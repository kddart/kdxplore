/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.
    
    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.kdxplore.boxplot;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.TreeBag;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.chartcommon.KDXChartMouseListener;
import com.diversityarrays.kdxplore.chartcommon.KDXploreChartPanel;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.PlotsByTraitInstance;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.curate.TraitColorProvider;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.vistool.CurationControls;
import com.diversityarrays.kdxplore.vistool.JFreeChartVisToolPanel;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.VisToolData;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.kdxplore.vistool.VisualisationToolId;
import com.diversityarrays.util.Check;

import net.pearcan.color.ColorPair;


public class BoxPlotPanel extends JFreeChartVisToolPanel {

    private static final String FONT_NAME_SANS_SERIF = "SansSerif"; //$NON-NLS-1$

    protected static final boolean DEBUG = Boolean.getBoolean(BoxPlotPanel.class.getSimpleName() + ".DEBUG"); //$NON-NLS-1$

    private final String TAB_CURATION = Msg.TAB_CURATION();

    private final String TAB_MESSAGES = Msg.TAB_MESSAGES();

	static private int nextId = 1;
	
	private JFreeChart chart = null;
	
	private KDXploreChartPanel chartPanel;
	
	private JCheckBox showOutliers = new JCheckBox(Msg.CBOX_OUTLIERS());
	private JCheckBox showMean = new JCheckBox(Msg.CBOX_MEAN());
	private JCheckBox showMedian = new JCheckBox(Msg.CBOX_MEDIAN());
		
	private SpinnerNumberModel minSpinnerModel = new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0);
	private SpinnerNumberModel maxSpinnerModel = new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0);
	
	private JSpinner minSpinner = new JSpinner(minSpinnerModel);
	private JSpinner maxSpinner = new JSpinner(maxSpinnerModel);

	private final Map<TraitInstance,TraitInstanceValueRetriever<?>> tivrByTi;
	
	private CurationControls curationControls;

	private final JTabbedPane messagesAndCurationTabbedPane;
	
	private final JTextArea reportTextArea = new JTextArea();
	private final JSplitPane splitPane;	
	private final PlotInfoProvider plotInfoProvider;
	private final Supplier<TraitColorProvider> colorProviderFactory;
	private final List<PlotOrSpecimen> plotSpecimens;

	private final int selectedPlotSpecimenCount;
	
	private final int maxNumberOfDecimalPlaces;
	
	private final Double[] overallMinMax = new Double[2];


	private final ChangeListener spinnerChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (! stillChanging) {
				minSelectedY = minSpinnerModel.getNumber().doubleValue();
				maxSelectedY = maxSpinnerModel.getNumber().doubleValue();

				if (DEBUG) {
	                System.out.println("yRange: " + minSelectedY + " .. " + maxSelectedY);  //$NON-NLS-1$//$NON-NLS-2$
				}

				drawRectangle();

				setSelectedTraitAndMeasurements();
			}
		}
	};	

	public BoxPlotPanel(
			PlotInfoProvider pip,
			VisualisationToolId<?> vtoolId,
			SelectedValueStore svs,
			String title, 
			VisToolData data,
			Supplier<TraitColorProvider> colorProviderFactory,
			SuppressionHandler suppressionHandler) 
	{
		super(title, vtoolId, svs, nextId++, data.traitInstances, data.context.getTrial(), suppressionHandler);

		int ndecs = 0;
		for (TraitInstance ti : traitInstances) {
		    String vr = ti.trait.getTraitValRule();
		    if (! Check.isEmpty(vr)) {
	            try {
	                ValidationRule vrule = ValidationRule.create(vr);
	                ndecs = Math.max(ndecs, vrule.getNumberOfDecimalPlaces());
	            } catch (UnsupportedOperationException | InvalidRuleException snh) {
	                throw new RuntimeException(ti.trait.getTraitValRule(), snh); 
	            }
		    }
		}
		this.maxNumberOfDecimalPlaces = ndecs;

		Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
            @Override
            public List<KdxSample> apply(TraitInstance ti) {
                return pip.getSampleMeasurements(ti);
            }
        };
        tivrByTi = VisToolUtil.buildTraitInstanceValueRetrieverMap(trial, traitInstances, sampleProvider);
		
		this.plotInfoProvider = pip;
		this.colorProviderFactory = colorProviderFactory;

		if (Check.isEmpty(data.plotSpecimensToGraph)) {
		    plotSpecimens = new ArrayList<>();
		    VisToolUtil.collectPlotSpecimens(plotInfoProvider.getPlots(),
		            new Consumer<PlotOrSpecimen>() {
                        @Override
                        public void accept(PlotOrSpecimen pos) {
                            plotSpecimens.add(pos);
                        }
		    });
		    selectedPlotSpecimenCount = 0;
		}
		else {
			plotSpecimens = data.plotSpecimensToGraph;			
			selectedPlotSpecimenCount = plotSpecimens.size();
		}
		
        JComponent controlsOrLabel;

        String messageLine = selectedPlotSpecimenCount <= 0 
                ? null 
                : Msg.MSG_ONLY_FOR_N_PLOTS(selectedPlotSpecimenCount);

        if (traitInstances.size() == 1) {
            curationControls = new CurationControls(
                    true, // askAboutValueForUnscored
                    suppressionHandler,
                    selectedValueStore,
                    toolPanelId,
                    messageLine,
                    traitNameStyle, 
                    Arrays.asList(traitInstances.get(0)));
            curationControls.setBorder(new EmptyBorder(2, 4, 2, 4));

            controlsOrLabel = curationControls;
        }
        else {
            StringBuilder sb = new StringBuilder("<HTML>"); //$NON-NLS-1$
            if (messageLine != null) {
                sb.append(messageLine).append("<br>"); //$NON-NLS-1$
            }
            sb.append(Msg.HTML_CURATION_NOT_AVAILABLE_WITH_MULTIPLE_TRAITS());
            controlsOrLabel = new JLabel(sb.toString());
        }
        
        messagesAndCurationTabbedPane = new JTabbedPane();
        messagesAndCurationTabbedPane.addTab(TAB_MESSAGES, new JScrollPane(reportTextArea));
        messagesAndCurationTabbedPane.addTab(TAB_CURATION, controlsOrLabel);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                messagesAndCurationTabbedPane,
                new JLabel()); // placeholder
        
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.0);
        
        Box controls = generateControls();

        add(controlsOrLabel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
		
		generateGraph(Why.INITIAL);

		minSpinnerModel.addChangeListener(spinnerChangeListener);
		maxSpinnerModel.addChangeListener(spinnerChangeListener);

		NumberEditor formatterMin;
		NumberEditor formatterMax;

		stillChanging = true;
		try {
			if (maxNumberOfDecimalPlaces <= 0) {
				// integer
				minSpinnerModel.setStepSize(1);
				maxSpinnerModel.setStepSize(1);
				
				formatterMin = new JSpinner.NumberEditor(minSpinner, "0"); //$NON-NLS-1$
				formatterMax = new JSpinner.NumberEditor(maxSpinner, "0"); //$NON-NLS-1$
			}
			else {
				double stepSize = Math.pow(10, -maxNumberOfDecimalPlaces);
	
				minSpinnerModel.setStepSize(stepSize);
				maxSpinnerModel.setStepSize(stepSize);
				
				StringBuilder sb = new StringBuilder("0."); //$NON-NLS-1$
				for (int i = maxNumberOfDecimalPlaces; --i >= 0; ) {
					sb.append("0"); //$NON-NLS-1$
				}
				String fmt = sb.toString();
				formatterMin = new JSpinner.NumberEditor(minSpinner, fmt);
				formatterMax = new JSpinner.NumberEditor(maxSpinner, fmt);
			}
		}
		finally {
			stillChanging = false;
		}
		
		formatterMin.setEnabled(true);
		formatterMax.setEnabled(true);
		
		minSpinner.setEditor(formatterMin);
		maxSpinner.setEditor(formatterMax);
		
		minSpinner.setBorder(new EmptyBorder(3, 5, 3, 5));
		maxSpinner.setBorder(new EmptyBorder(3, 5, 3, 5));

		setSpinnerRanges();
	}
	
	private void setSpinnerRanges() {
		
		if (overallMinMax[0] == null) {
			overallMinMax[0] = 0.0;
			overallMinMax[1] = 0.0;
		}

		Comparable<?> minValue;
		Comparable<?> maxValue;
		
		if (maxNumberOfDecimalPlaces <= 0) {
			minValue = overallMinMax[0].intValue();
			maxValue = overallMinMax[1].intValue();
		}
		else {
			minValue = overallMinMax[0];
			maxValue = overallMinMax[1];
		}
		
		try {
			stillChanging = true;

			minSpinnerModel.setMinimum(minValue);
			minSpinnerModel.setMaximum(maxValue);

			maxSpinnerModel.setMinimum(minValue);
			maxSpinnerModel.setMaximum(maxValue);
			
			minSpinnerModel.setValue(minValue);
			maxSpinnerModel.setValue(maxValue);
		}
		finally {
			stillChanging = false;
		}

		if (DEBUG) {
	        System.out.println(
	                String.format("minSpinnerModel: [%s .. %s]\tstepSize=%s",  //$NON-NLS-1$
	                        minSpinnerModel.getMinimum().toString(), 
	                        minSpinnerModel.getMaximum().toString(),
	                        minSpinnerModel.getStepSize().toString()));
	        System.out.println(
	                String.format("minSpinnerModel: [%s .. %s]\tstepSize=%s", //$NON-NLS-1$
	                        maxSpinnerModel.getMinimum().toString(),
	                        maxSpinnerModel.getMaximum().toString(),
	                        maxSpinnerModel.getStepSize().toString()));
		}
		 
	}

	private void setSelectedRange(double min, double max) {
		if (min <= max) {
			minSpinnerModel.setValue(min);
			maxSpinnerModel.setValue(max);
		}
		else {
			minSpinnerModel.setValue(max);
			maxSpinnerModel.setValue(min);
		}
	}

	private Box generateControls() {

		for (JCheckBox jcb : Arrays.asList(showOutliers, showMean, showMedian)) { 
		    ActionListener optionsActionListener = new ActionListener() {
		        @Override
		        public void actionPerformed(ActionEvent e) {
		            generateGraph(Why.OPTION_CHANGED);
		            setSpinnerRanges();
		        }           
		    };
			jcb.addActionListener(optionsActionListener);
			jcb.setSelected(true);
		}
		
		JLabel infoLabel = new JLabel(Msg.LABEL_SHOW_PARAMETERS());
		infoLabel.setBorder(new EmptyBorder(3,3,3,3));
		
		Box hbox = Box.createHorizontalBox();
		hbox.add(syncedOption);
		hbox.add(minSpinner);
		hbox.add(new JLabel(" - ")); //$NON-NLS-1$
		hbox.add(maxSpinner);
		hbox.add(infoLabel);
		hbox.add(showOutliers);
		hbox.add(showMean);
		hbox.add(showMedian);

		return hbox;
	}
	
	enum Why {
        INITIAL("Initial"), 
        OPTION_CHANGED("Option Changed"), 
        REFRESH_DATA("Refresh Data"),
        ;
        public final String displayValue;
        Why(String s) {
            displayValue = s;
        }
        
        @Override
        public String toString() {
            return displayValue;
        }

        public boolean needsReport() {
            return this != OPTION_CHANGED;
        }

        public boolean needsLeadingNewline() {
            return this != INITIAL;
        }
	}

	private void generateGraph(Why why) {

		overallMinMax[0] = null;
		overallMinMax[1] = null;

		if (chartPanel != null) {
			for (EventListener cml : chartPanel.getListeners(ChartMouseListener.class)) {
				chartPanel.removeChartMouseListener((ChartMouseListener) cml);
			}
		}
		
		Bag<String> missingOrBad = new TreeBag<>();
		Bag<String> suppressed = new TreeBag<>();

		final BoxAndWhiskerCategoryDataset dataset = createSampleDataSet(
				missingOrBad, suppressed, overallMinMax);

        final CategoryAxis xAxis = new CategoryAxis(Msg.AXIS_LABEL_TRAIT_INSTANCE());
        final NumberAxis yAxis = new NumberAxis(Msg.AXIS_LABEL_SAMPLE_VALUE());
        yAxis.setAutoRangeIncludesZero(false);
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setMaximumBarWidth(.35);
//        renderer.setItemMargin(.1);
        renderer.setFillBox(true);
        renderer.setUseOutlinePaintForWhiskers(true); 
        renderer.setOutliersVisible(showOutliers.isSelected());
        renderer.setMeanVisible(showMean.isSelected());
        renderer.setMedianVisible(showMedian.isSelected());
//        renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        CategoryPlot boxplot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		
        chart = new JFreeChart(getTitle() ,new Font(FONT_NAME_SANS_SERIF, Font.BOLD, 14),boxplot,true);
//        dataMin = ((CategoryPlot) chart.getPlot()).getRangeAxis().getLowerBound();
//        dataMax = ((CategoryPlot) chart.getPlot()).getRangeAxis().getUpperBound();
 
		CategoryItemRenderer catr = ((CategoryPlot) chart.getPlot()).getRendererForDataset(dataset);
		TraitColorProvider traitColorProvider = colorProviderFactory.get();
		for (TraitInstance ti : traitInstances) {
			ColorPair colorPair = traitColorProvider.getTraitInstanceColor(ti);
			if (colorPair != null) {
				
				String validName = traitNameStyle.makeTraitInstanceName(ti);
				
				if (seriesCountByTraitName.get(validName) != null) {
					catr.setSeriesPaint(seriesCountByTraitName.get(validName), colorPair.getBackground());
				}		
			}
		}
//		((CategoryPlot) chart.getPlot()).setRenderer(catr);
        
        chartPanel = new KDXploreChartPanel(chart);
        
        chartPanel.addChartMouseListener(chartMouseListener);
        
        // TODO check if we should be checking syncedOption
        if (marker != null && syncedOption.getSyncWhat().isSync()) {
    		((CategoryPlot) getChart().getPlot()).addRangeMarker(marker);
//			curationControls.setSyncedState(true);
        }

        if (why.needsReport()) {
            String msg = VisToolData.createReportText(missingOrBad, suppressed);
            if (! Check.isEmpty(msg)) {
                if (why.needsLeadingNewline()) {
                    reportTextArea.append("\n");
                }
                reportTextArea.append("==== ");
                reportTextArea.append(why.displayValue);
                reportTextArea.append("\n");
                reportTextArea.append(msg);
            }
        }

        splitPane.setRightComponent(chartPanel);
		
		this.updateUI();
		this.repaint();
	}

    boolean bol = true;
	
	private Marker marker = null;

	public void addRangeMarker(Marker intervalMarker) {
		marker = intervalMarker;
		((CategoryPlot) getChart().getPlot()).addRangeMarker(intervalMarker);	
	}
	

	private BoxAndWhiskerCategoryDataset createSampleDataSet(
			Bag<String> missingOrBad,
			Bag<String> suppressed,
			Double[] minMax) 
	{
		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		seriesCountByTraitName.clear();

		minMax[0] = null;
		minMax[1] = null;
		int count = 0;
		for (TraitInstance instance : traitInstances) {

			TraitInstanceValueRetriever<?> y_tivr = tivrByTi.get(instance);    	
			String instanceName = traitNameStyle.makeTraitInstanceName(instance);

			List<Double> data = new ArrayList<Double>();
			
			for (PlotOrSpecimen pos : plotSpecimens) {
			    Plot plot = plotInfoProvider.getPlotByPlotId(pos.getPlotId());
			    if (plot==null || ! plot.isActivated()) {
			        continue;
			    }

				TraitValue yTraitValue = y_tivr.getAttributeValue(plotInfoProvider, plot, null);
				if (yTraitValue == null || ! (yTraitValue.comparable instanceof Number)) {
					missingOrBad.add(instanceName);
					continue;
				} else if (yTraitValue.suppressed) {
					// TODO count suppressed
					suppressed.add(instanceName);
					continue;
				}
				double y = ((Number) yTraitValue.comparable).doubleValue();
				data.add(y);
				
				if (minMax[0] == null) {
					minMax[0] = y;
					minMax[1] = y;
				}
				else {
					minMax[0] = Math.min(minMax[0], y);
					minMax[1] = Math.max(minMax[1], y);
				}
			}

			seriesCountByTraitName.put(instanceName, count);
			
			String columnKey = ""; // TODO use something better? //$NON-NLS-1$
			dataset.add(data, instanceName, columnKey);
        	count++;
        }		
		
		return dataset;
	}

	private Map<String, Integer> seriesCountByTraitName = new HashMap<String, Integer>();
	
	public ChartPanel boxPlot() {
		return chartPanel;
	}
	
	public JFreeChart getChart() {
		return chart;
	}

	public List<TraitInstance> getTraitInstances() {
		return traitInstances;
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}
	
	// - - - - -
	
	private Double minSelectedY = null;
	private Double maxSelectedY = null;
	private Marker intervalMarker = null;
	
	private final KDXChartMouseListener chartMouseListener = new KDXChartMouseListener() {
		@Override
		public void chartMouseClicked(ChartMouseEvent cme) {
		    if (DEBUG) {
	            System.out.println("BoxPlotPanel: chartMouseClicked"); //$NON-NLS-1$
		    }
			minSelectedY = null; // overallMinMax[0];
			maxSelectedY = null; // overallMinMax[1];
			
			stillChanging = true;
			try {
				minSpinnerModel.setValue(overallMinMax[0]);
				maxSpinnerModel.setValue(overallMinMax[1]);
				
				((CategoryPlot) getChart().getPlot()).clearRangeMarkers();
				
				if (minSelectedY != null && maxSelectedY != null) {				
					setSelectedRange(minSelectedY, maxSelectedY);
				}
				setSelectedTraitAndMeasurements();
			}
			finally {
				stillChanging = false;
			}
		}
		
		@Override
		public void chartMouseMoved(ChartMouseEvent cme) {
//			System.out.println("BoxPlotPanel: chartMouseMoved"); // TODO  something 
		}
		
		@Override
		public void chartMouseSelected(ChartMouseEvent event) {
			if (DEBUG) {
			    System.out.println("BoxPlotPanel: chartMouseSelected"); //$NON-NLS-1$
			}

			stillChanging = true;

			double pointY = event.getTrigger().getPoint().y;

			//TODO - find selected trait in graph when selecting from mouse X values
			
			Rectangle2D plotArea = getChartPanel().getScreenDataArea();
			CategoryPlot plot = (CategoryPlot) getChart().getPlot(); 
			Double chartY = plot.getRangeAxis().java2DToValue(pointY, plotArea, plot.getRangeAxisEdge());
			
			if (chartY != null) {
				if (minSelectedY == null) {
					minSelectedY = chartY;
					maxSelectedY = chartY;
				}
				else {
					minSelectedY = Math.min(minSelectedY, chartY);
					maxSelectedY = Math.max(maxSelectedY, chartY);
				}
			}
//			if (! stillChanging) {
//				minSelectedY = chartY;
//				stillChanging = true;
//			}
//			else {
//				maxSelectedY = chartY;
//			}

//			 TODO resinstate if required
//			if (maxSelectedY != null && minSelectedY != null) {
//				setSelectedRange(minSelectedY, maxSelectedY);
//			}
			
			drawRectangle();
			
		}
		@Override
		public void chartMouseSelectedReleased(ChartMouseEvent event) {
			if (DEBUG) {
			    System.out.println("BoxPlotPanel: chartMouseSelectedReleased"); //$NON-NLS-1$
			}

			stillChanging = false;

			if (minSelectedY != null && maxSelectedY != null) {
				
				setSelectedRange(minSelectedY, maxSelectedY);

				setSelectedTraitAndMeasurements();
			}
		}

		@Override
		public void chartMouseZoomingReleased(ChartMouseEvent event) {
			if (DEBUG) {
			    System.out.println("BoxPlotPanel: chartMouseZoomingReleased"); //$NON-NLS-1$
			}
		}
	};
	
	private void drawRectangle() {
		if (minSelectedY != null && maxSelectedY != null) {		

			double min = minSelectedY;
			double max = maxSelectedY;
			if (min > max) {
				min = maxSelectedY;
				max = minSelectedY;
			}
			
			((CategoryPlot) getChart().getPlot()).clearRangeMarkers();

			intervalMarker = new IntervalMarker(min, max);
			intervalMarker.setAlpha(0.3f);
			intervalMarker.setPaint(Color.RED);	
			addRangeMarker(intervalMarker);

			getChartPanel().repaint();
		}
	}

	
	private void setSelectedTraitAndMeasurements() {
		boolean sync = getSyncWhat().isSync();
		
		PlotsByTraitInstance plotsByTi = sync ? new PlotsByTraitInstance() : null;

		List<KdxSample> selectedSamples = new ArrayList<>();

		if (minSelectedY != null && maxSelectedY != null) {
		    
			for (Plot plot : plotInfoProvider.getPlots()) {
	            
			    Consumer<KdxSample> visitor = new Consumer<KdxSample>() {
	                @Override
	                public void accept(KdxSample s) {
	                    TraitInstance ti = plotInfoProvider.getTraitInstanceForSample(s);
	                    if (checkIfSelected(s, minSelectedY, maxSelectedY) == AxisType.Y) {
	                        selectedSamples.add(s);
	                        
	                        if (plotsByTi != null) {
	                            plotsByTi.addPlot(ti, plot);
	                        }
	                    }
	                }
	            };

	            if (usedPlotSpecimens.isEmpty() || usedPlotSpecimens.contains(plot)) {
				    plotInfoProvider.visitSamplesForPlotOrSpecimen(plot, visitor);
				}
			}
		}

		// TODO review not do it when empty
		if (plotsByTi!=null) { // && ! plotsByTi.isEmpty()) {
			selectedValueStore.setSelectedPlots(toolPanelId, plotsByTi);
		}

		
		if (curationControls!=null) {
			curationControls.setSamples(selectedSamples);
			int index = messagesAndCurationTabbedPane.indexOfTab(TAB_CURATION);
			if (index >= 0) {
			    messagesAndCurationTabbedPane.setSelectedIndex(index);
			}
			// Note that curationControls is only non-null if we have ONE TraitInstance
			// so all of the selectedSampled must be for that TraitInstance	
			curationControls.updateButtons();
		}

		fireSelectionStateChanged();
	}
	
	private AxisType checkIfSelected(Sample sample, double min, double max) {

		if (sample.getTraitValue() == null) {
			return AxisType.Neither;
		}

		int sampleTraitId = sample.getTraitId();
		int sampleInstanceNumber = sample.getTraitInstanceNumber();

		double ymin = min;
		double ymax = max;
		if (min > max) {
			ymin = max;
			ymax = min;
		}
		for (TraitInstance ti : traitInstances) {
			if (sampleTraitId == ti.getTraitId() && sampleInstanceNumber == ti.getInstanceNumber()) {
				try {
					String tv = sample.getTraitValue();
					if (tv != null && ! tv.isEmpty()) {
						double traitValue = Double.parseDouble(tv);	
						if (ymin <= traitValue  && traitValue <= ymax) {
							return AxisType.Y;
						}	
					}
				}
				catch (NumberFormatException ignore) { }
				
				// We found the matching TraitInstance so no need to continue searching
				break;
			}
		}

		return AxisType.Neither;
	}

	// === VisToolPanel

	@Override
	protected void updateSyncedOption() {
		if (intervalMarker != null) {
			if (getSyncWhat().isSync()) {
				addRangeMarker(intervalMarker);

				curationControls.setSyncedState(true);
				
				if (minSelectedY != null && maxSelectedY != null) {
					setSelectedTraitAndMeasurements();
				}
			}
			else {
				((CategoryPlot) getChart().getPlot()).clearRangeMarkers();	

				curationControls.setSyncedState(false);
				
				selectedValueStore.setSelectedPlots(toolPanelId, null);
				fireSelectionStateChanged();
			}
		}
	}
	
	@Override
	public void plotActivationsChanged(boolean activated, List<Plot> plots) {
		// This will affect the values seen
		updateRefreshButton();
		curationControls.updateButtons();
	}
	
	@Override
	public void editedSamplesChanged() {
		// This will affect the values seen
		updateRefreshButton();
		curationControls.updateButtons();
	}

	@Override
	public void updateSelectedSamples() {

		PlotsByTraitInstance plotsByTraitInstance = selectedValueStore.getSyncedPlotsByTrait();

		intervalMarker = null;

		CategoryPlot categoryPlot = ((CategoryPlot) getChart().getPlot());
		
		categoryPlot.clearRangeMarkers();
		
		double min = categoryPlot.getRangeAxis().getLowerBound();
		double max = categoryPlot.getRangeAxis().getUpperBound();	

		for (TraitInstance ti : plotsByTraitInstance.getTraitInstances()) {
			if (traitInstances.contains(ti)) {
			    String tiIdentifier = InstanceIdentifierUtil.getInstanceIdentifier(ti);
			    
//			    Set<Plot> plots = plotsByTraitInstance.getPlotSpecimens(ti).stream()
//			        .map(pos -> plotInfoProvider.getPlotByPlotId(pos.getPlotId()))
//			        .collect(Collectors.toSet());
			    
				for (PlotOrSpecimen pos : plotsByTraitInstance.getPlotSpecimens(ti)) {
					if (usedPlotSpecimens.isEmpty() || usedPlotSpecimens.contains(pos)) {
					    
					    Consumer<KdxSample> visitor = new Consumer<KdxSample>() {
                            @Override
                            public void accept(KdxSample sample) {
                                if (InstanceIdentifierUtil.getInstanceIdentifier(sample).equals(tiIdentifier)) {
                                    if (checkIfSelected(sample , min, max) == AxisType.Y) {

                                        intervalMarker = new IntervalMarker(Double.parseDouble(sample.getTraitValue()) - 0.1 ,Double.parseDouble(sample.getTraitValue()) + 0.1);
                                        intervalMarker.setAlpha(0.3f);
                                        intervalMarker.setPaint(Color.RED); 
                                        Stroke stroke = new BasicStroke();
                                        intervalMarker.setOutlinePaint(Color.RED);
                                        intervalMarker.setOutlineStroke(stroke);
                                        addRangeMarker(intervalMarker);     
                                    }
                                }
                            }
                        };
                        plotInfoProvider.visitSamplesForPlotOrSpecimen(pos, visitor);					    
					}
				}
			}
		}

		getChartPanel().repaint();	

		if (null != curationControls) {
			curationControls.updateButtons(false);
		}
	}

	@Override
	protected JFreeChart getJFreeChart() {
		return chart;
	}
	
	@Override
	public boolean refreshData() {
	    generateGraph(Why.REFRESH_DATA);
	    setSpinnerRanges();
		curationControls.updateButtons();
		return true;
	}

}
