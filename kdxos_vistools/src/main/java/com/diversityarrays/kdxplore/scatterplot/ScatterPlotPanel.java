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
package com.diversityarrays.kdxplore.scatterplot;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.TreeBag;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.Annotation;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
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
import net.pearcan.ui.desktop.DesktopObject;

@SuppressWarnings("nls")
public class ScatterPlotPanel extends JFreeChartVisToolPanel implements DesktopObject {

	private static final boolean DEBUG = Boolean.getBoolean(ScatterPlotPanel.class.getSimpleName() + ".DEBUG"); //$NON-NLS-1$

    static private int nextId = 1;
	
	private final KDXploreChartPanel chartPanel = new KDXploreChartPanel(null);

	private final JSplitPane splitPane;
	private final JTextArea reportTextArea;
	
	private final TraitInstance xInstance;

	private XYSeriesCollection currentDataSet;
	
	private String xAxisName = null;
	private String yAxisName = null;
	
    private final NumberToTraitValue xNumberToTraitValue = new NumberToTraitValue();
    
    private final Map<Integer, NumberToTraitValue> numberToTraitValueBySeriesIndex = new HashMap<>();
	
	private SpinnerNumberModel minxModel = new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0);
	private SpinnerNumberModel maxxModel = new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0);
	
	private SpinnerNumberModel minyModel = new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0);
	private SpinnerNumberModel maxyModel = new SpinnerNumberModel(0.0, 0.0, 0.0, 1.0);
	
	private final CurationControls curationControls;
	
	private JSpinner minxSpinner = new JSpinner(minxModel);
	private JSpinner maxxSpinner = new JSpinner(maxxModel);
	
	private JSpinner minySpinner = new JSpinner(minyModel);
	private JSpinner maxySpinner = new JSpinner(maxyModel);
	
	private Double dataxMax = null;
	private Double dataxMin = null;
	
	private Double datayMax = null;
	private Double datayMin = null;
	
	private final Map<String,TraitInstance> valueInstanceByTraitIdAndNumber = new HashMap<>();
		
	@Override
	public void addMouseListener(MouseListener mouseListener){
		if (chartPanel != null) {
			chartPanel.addMouseListener(mouseListener);
		}
	}
	
	private final KDXChartMouseListener chartMouseListener = new KDXChartMouseListener() {

		@Override
		public void chartMouseClicked(ChartMouseEvent cme) {
			mouseDownPoint = null;
			mouseUpPoint = null;
			setSelectedTraitAndMeasurementsToNull();
			removeAnnotations();
			stillChanging = false;
		}

		@Override
		public void chartMouseMoved(ChartMouseEvent cme) {
		}

		@Override
		public void chartMouseSelected(ChartMouseEvent event) {
			clearExternallySelectedPlots();
			
			setXYValues(event);
			drawRectangle();
			if (mouseDownPoint != null && mouseUpPoint != null) {
				buildMinMaxPoints();
				setSelectedTraitAndMeasurements();
			}	
		}

		@Override
		public void chartMouseSelectedReleased(ChartMouseEvent event) {	
			stillChanging = false;
		}

		@Override
		public void chartMouseZoomingReleased(ChartMouseEvent event) {
			redrawSelectedPlots();
		}
	};
	
	private final Supplier<TraitColorProvider> colorProviderFactory;

	private final List<TraitInstance> valueInstances = new ArrayList<TraitInstance>();
	private final Map<TraitInstance,TraitInstanceValueRetriever<?>> tivrByTi;
	private final List<PlotOrSpecimen> plotSpecimens;

	private final PlotInfoProvider plotInfoProvider;

    private final String tabMessages;
    private final String tabCuration;

	public ScatterPlotPanel(
			PlotInfoProvider infoProvider,
			VisualisationToolId<?> vtoolId,
			SelectedValueStore svs,
			String title, 
			VisToolData data,
			Supplier<TraitColorProvider> colorProviderFactory,
			SuppressionHandler sh) 
	{
		super(title, vtoolId, svs, nextId++, data.traitInstances, data.context.getTrial(), sh);

		this.plotInfoProvider = infoProvider;

		if (data.plotSpecimensToGraph==null) {
		    plotSpecimens = new ArrayList<>();
		    VisToolUtil.collectPlotSpecimens(plotInfoProvider.getPlots(),
		            new Consumer<PlotOrSpecimen>() {
                        @Override
                        public void accept(PlotOrSpecimen pos) {
                            plotSpecimens.add(pos);
                        }
                    });
		}
		else {
		    plotSpecimens = data.plotSpecimensToGraph;			
		}

		List<List<Comparable<?>>> instanceValuesList = new ArrayList<>();
		
		this.colorProviderFactory = colorProviderFactory;

        Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
            @Override
            public List<KdxSample> apply(TraitInstance ti) {
                return infoProvider.getSampleMeasurements(ti);
            }
        };
		tivrByTi = VisToolUtil.buildTraitInstanceValueRetrieverMap(trial, traitInstances, sampleProvider);

		int plotLength = instanceValuesList.size();
		if(plotLength<3){
			plotLength = 3;
		}

		xInstance = traitInstances.get(0);	
		TraitInstance firstValueInstance = null;

		for (int i = 1; i < traitInstances.size(); i++) {			
			TraitInstance ti  = traitInstances.get(i);
			if (firstValueInstance==null) {
				firstValueInstance = ti;
			}
			valueInstances.add(ti);
			valueInstanceByTraitIdAndNumber.put(InstanceIdentifierUtil.getInstanceIdentifier(ti), ti);
		}

		xAxisName = traitNameStyle.makeTraitInstanceName(xInstance);

		if (traitInstances.size() == 2) {
			yAxisName = traitNameStyle.makeTraitInstanceName(firstValueInstance);
		} else {
			yAxisName = "Sample Measurement Value";
		}

		chartPanel.addChartMouseListener(chartMouseListener);

        Bag<String> missingOrBad = new TreeBag<String>();
        Bag<String> suppressed = new TreeBag<String>();

		generateChart(true, missingOrBad, suppressed);

		ChangeListener listener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!stillChanging) {
					clearExternallySelectedPlots();

					setXYValues();
					drawRectangle();
					if (mouseDownPoint != null && mouseUpPoint != null) {
						buildMinMaxPoints();
						setSelectedTraitAndMeasurements();
					}	
				}
			}
		};
        
		minxSpinner.addChangeListener(listener);
		minySpinner.addChangeListener(listener);
		maxxSpinner.addChangeListener(listener);
		maxySpinner.addChangeListener(listener);
		
        stillChanging = true;
        minxModel.setValue(dataxMin);
        maxxModel.setValue(dataxMax);
        
        minyModel.setValue(datayMin);
        maxyModel.setValue(datayMax);
        
        minxModel.setMinimum(dataxMin);
        minxModel.setMaximum(dataxMax);      
        minyModel.setMinimum(datayMin);
        minyModel.setMaximum(datayMax);	
        
        maxxModel.setMinimum(dataxMin);
        maxxModel.setMaximum(dataxMax);      
        maxyModel.setMinimum(datayMin);
        maxyModel.setMaximum(datayMax);	
        stillChanging = false;
        
        Box hbox = Box.createHorizontalBox();
        hbox.add(syncedOption);
        
        addSpinners(hbox, minxSpinner, minySpinner);        
        hbox.add(new JLabel(" " + Msg.LABEL_MIN_TO_MAX_SEPARATOR() + " " )); //$NON-NLS-1$ //$NON-NLS-2$
        addSpinners(hbox, maxxSpinner, maxySpinner);
        
        
        List<TraitInstance> curationControlInstances = traitInstances;
        curationControlInstances.remove(xInstance);
        
        curationControls = new CurationControls(
                true, // askAboutValueForUnscored
        		suppressionHandler, 
        		selectedValueStore, 
//        		plotInfoProvider, 
        		toolPanelId, 
        		null,
        		traitNameStyle, 
        		curationControlInstances
        		);

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);

        tabMessages = Msg.TAB_MESSAGES();
        tabCuration = Msg.TAB_CURATION();
        tabbedPane.addTab(tabMessages, new JScrollPane(reportTextArea));
        tabbedPane.addTab(tabCuration, curationControls);
        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                tabbedPane, 
                chartPanel);

        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.0);
        add(splitPane, BorderLayout.CENTER);
        add(hbox, BorderLayout.SOUTH);

        splitPane.repaint(); // TODO why is this here?
        
        String msg = VisToolData.createReportText(missingOrBad, suppressed);
		if (Check.isEmpty(msg)) {
		    tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(tabCuration));
		}
		else {
            reportTextArea.setText(msg);
            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(tabMessages));
		}

		setPreferredSize(new Dimension(600,500));
	}
	

	private void addSpinners(Box hbox, JSpinner xSpinner, JSpinner ySpinner) {
        hbox.add(new JLabel(Msg.LABEL_X_COMMA_Y()));
        hbox.add(xSpinner);
        hbox.add(new JLabel(",")); //$NON-NLS-1$
        hbox.add(ySpinner);
    }

    private XYShapeAnnotation intervalAnnotation = null;
	private Point2D mouseDownPoint = null;
	private Point2D mouseUpPoint = null;
	
	@Override
	protected void updateSyncedOption() {
		if (getSyncWhat().isSync()) {
			if (intervalAnnotation != null) {
				addRangeMarker(intervalAnnotation);

				curationControls.setSyncedState(true);
				
				if (mouseDownPoint != null && mouseUpPoint != null) {
					setSelectedTraitAndMeasurements();
				}
			}
		} else {		
		    selectedValueStore.setSelectedPlots(toolPanelId, null);

			fireSelectionStateChanged();
			((XYPlot) getChart().getPlot()).clearAnnotations();
			curationControls.setSyncedState(false);
		}
	}

	private void removeAnnotations() {
		((XYPlot) chart.getPlot()).clearAnnotations();
	}

	private double minChartX = Double.MAX_VALUE;
	private double maxChartX = Double.MIN_VALUE;
	
	private double minChartY = Double.MAX_VALUE;
	private double maxChartY = Double.MIN_VALUE;

	private void buildMinMaxPoints() {
				
		double dn = mouseDownPoint.getX();
		double up = mouseUpPoint.getX();
		
		minChartX = Math.min(dn, up);
		maxChartX = Math.max(dn, up);

		dn = mouseDownPoint.getY();
		up = mouseUpPoint.getY();

		minChartY = Math.min(dn, up);
		maxChartY = Math.max(dn, up);
	}
	
	private void drawRectangle() {
		if (mouseDownPoint != null && mouseUpPoint != null) {
			
			double dn = mouseDownPoint.getX();
			double up = mouseUpPoint.getX();
			
			double x0 = Math.min(dn, up);
			double x1 = Math.max(dn, up);

			dn = mouseDownPoint.getY();
			up = mouseUpPoint.getY();

			double y0 = Math.min(dn, up);
			double y1 = Math.max(dn, up);
			
			removeAnnotations();
			Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0);				
			Paint paint = Color.BLACK;
			
			Rectangle2D rect = new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0);				
			intervalAnnotation = new XYShapeAnnotation(rect,stroke, paint);				
			
			addRangeMarker(intervalAnnotation);
			getChartPanel().repaint();
		}
	}

	private void setSelectedTraitAndMeasurements() {
		
		boolean sync = getSyncWhat().isSync();
		
		if (sync) {
			Map<PlotOrSpecimen,List<TraitInstance>> traitInstancesByPlot = new HashMap<>();
	
			Set<PlotOrSpecimen> plotsInXrange = new HashSet<>();
	
			TraitInstanceValueRetriever<?> x_tivr = tivrByTi.get(xInstance);
			
			for (PlotOrSpecimen pos : plotSpecimens) {
	
				TraitValue xTraitValue = x_tivr.getAttributeValue(plotInfoProvider, pos, null);
				if (xTraitValue == null || ! (xTraitValue.comparable instanceof Number)) {
					continue;
				}
				double x = ((Number) xTraitValue.comparable).doubleValue();
				if (minChartX <= x && x <= maxChartX) {
					plotsInXrange.add(pos);
				}
				
				for (TraitInstance valueInstance : valueInstances) {
	
					TraitInstanceValueRetriever<?> y_tivr = tivrByTi.get(valueInstance);
					
					TraitValue yTraitValue = y_tivr.getAttributeValue(plotInfoProvider, pos, null);
					if (yTraitValue == null || ! (yTraitValue.comparable instanceof Number)) {
						continue;
					}
					
					double y = ((Number) yTraitValue.comparable).doubleValue();
					if (minChartY <= y && y < maxChartY) {
						List<TraitInstance> list = traitInstancesByPlot.get(pos);
						if ( list == null) {
							list = new ArrayList<>();
							traitInstancesByPlot.put(pos, list);
						}
						list.add(valueInstance);
					}
				}
	
			}
	
			PlotsByTraitInstance plotsByTi = new PlotsByTraitInstance();
			for (PlotOrSpecimen pos : plotsInXrange) {
				if (usedPlotSpecimens.isEmpty() || usedPlotSpecimens.contains(pos)) {
					List<TraitInstance> list = traitInstancesByPlot.get(pos);
					if (list != null ){
						plotsByTi.addTraitInstances(pos, list);
					}
				}
			}
			selectedValueStore.setSelectedPlots(toolPanelId, plotsByTi);
		}
		
		fireSelectionStateChanged();
		curationControls.updateButtons();
	}

	private JFreeChart chart = null;

	public JFreeChart getChart() {
		return chart;
	}
	
	private void generateChart(boolean recreateDataSet, Bag<String> missingOrBad, Bag<String> suppressed){

	    if (recreateDataSet) {
	        currentDataSet = createSampleDataSet(missingOrBad, suppressed);
	    }
	    
	    XYSeriesCollection dataset = currentDataSet;
	    
		PlotOrientation orientation = PlotOrientation.VERTICAL; 
		boolean show = true; 
		boolean toolTips = true;
		boolean urls = true; 

		chart = ChartFactory.createScatterPlot(getTitle(), xAxisName, yAxisName, 
				dataset, orientation, show, toolTips, urls);

		if (DEBUG) {
		    System.out.println("Generated new ScatterPlot"); //$NON-NLS-1$
		}

		TraitColorProvider traitColorProvider = colorProviderFactory.get();
		
		XYPlot xyPlot = (XYPlot) chart.getPlot();
        XYItemRenderer xyr = xyPlot.getRendererForDataset(dataset);

		boolean anyDisplayValues = false;
		if (! xNumberToTraitValue.numberToTraitValue.isEmpty()) {
		    anyDisplayValues = true;
		}
		else {
		    for (NumberToTraitValue n2tv : numberToTraitValueBySeriesIndex.values()) {
		        if (! n2tv.numberToTraitValue.isEmpty()) {
		            anyDisplayValues = true;
		            break;
		        }
		    }
		}
		if (anyDisplayValues) {
	        xyr.setBaseToolTipGenerator(new MyXYToolTipGenerator());
		}  

		for (TraitInstance ti : traitInstances) {
			ColorPair colorPair = traitColorProvider.getTraitInstanceColor(ti);
			if (colorPair != null) {
				if (DEBUG) {
				    System.out.println("Got a color back for: " + InstanceIdentifierUtil.getInstanceIdentifier(ti)); //$NON-NLS-1$
				}
				
				String validName = traitNameStyle.makeTraitInstanceName(ti);
				if (seriesCountByTraitName.get(validName) != null) {
					xyr.setSeriesPaint(seriesCountByTraitName.get(validName), colorPair.getBackground());
				}		
			}
		}
		xyPlot.setRenderer(xyr);
		
		chartPanel.setChart(chart);
		
        dataxMin = xyPlot.getDomainAxis().getLowerBound();
        dataxMax = xyPlot.getDomainAxis().getUpperBound();
		
        datayMin = xyPlot.getRangeAxis().getLowerBound();
        datayMax = xyPlot.getRangeAxis().getUpperBound();
	}
	
	class NumberToTraitValue {
	    
	    public final Map<Number, String> numberToTraitValue = new HashMap<>();
        private TraitInstanceValueRetriever<?> tivr;
        private boolean numeric;

        public NumberToTraitValue() {
            this(null);
        }
        public NumberToTraitValue(TraitInstanceValueRetriever<?> tivr) {
            setTraitInstanceValueRetriever(tivr);
        }

        public void setTraitInstanceValueRetriever(TraitInstanceValueRetriever<?> tivr) {
            this.tivr = tivr;
            if (tivr != null) {
                Class<?> valueClass = tivr.getComparableValueClass();
                if (Number.class.isAssignableFrom(valueClass)) {
                    numeric = true;
                }
                else if (tivr.supportsGetAxisValue()) {
                    numeric = false;
                }
                else {
                    throw new RuntimeException("Unsupported value class: " + valueClass.getName());
                }
            }
        }

        public String getTraitDisplayValue(Number number) {
            return numberToTraitValue.get(number);
        }

        public void clear() {
            numberToTraitValue.clear();
        }
        
        public Number getAxisValue(PlotOrSpecimen pos, Bag<String> missingOrBad, Bag<String> suppressed) {
            Number axisValue = null;
            TraitValue traitValue = tivr.getAttributeValue(plotInfoProvider, pos, null);
            
            if (traitValue == null) {
                missingOrBad.add(tivr.getDisplayName());
            }
            else if (traitValue.suppressed) {
                suppressed.add(tivr.getDisplayName());
            }
            else {
                if (traitValue.comparable instanceof Number) {
                    axisValue = (Number) traitValue.comparable;
                }
                else if (! numeric) {
                    axisValue = tivr.getAxisValue(plotInfoProvider, pos);
                }
                
                if (axisValue != null) {
                    numberToTraitValue.put(axisValue, traitValue.displayValue);
                }
                else {
                    missingOrBad.add(tivr.getDisplayName());
                }
            }
//            if (traitValue != null) {
//                if (traitValue.comparable instanceof Number) {
//                    axisValue = (Number) traitValue.comparable;
//                }
//                else if (! numeric) {
//                    axisValue = tivr.getAxisValue(plotInfoProvider, pos);
//                }
//
//                if (axisValue != null) {
//                    numberToTraitValue.put(axisValue, traitValue.displayValue);
//                }
//            }
            return axisValue;
        }
	}
	
    class MyXYToolTipGenerator implements XYToolTipGenerator {
        @Override
        public String generateToolTip(XYDataset dataset, int series, int item) {
            Number x = dataset.getX(series, item);
            Number y = dataset.getY(series, item);
            
            StringBuilder sb = new StringBuilder();
            String xdv = null;
            if (x != null) {
                xdv = xNumberToTraitValue.getTraitDisplayValue(x);
                if (xdv != null) {
                    sb.append("X:").append(xdv);
                }
            }
            
            String ydv = null;
            if (y != null) {
                NumberToTraitValue yNumberToTraitValue = numberToTraitValueBySeriesIndex.get(series);
                if (yNumberToTraitValue != null) {
                    ydv = yNumberToTraitValue.getTraitDisplayValue(y);
                    if (ydv != null) {
                        if (sb.length() > 0) { 
                            sb.append(", ");
                        }
                        sb.append("Y:").append(ydv);
                    }
                }
            }

            return sb.toString();
        }
    };
	
	private final List<String> usedInstanceNames = new ArrayList<String>();
	
	Map<String,Double[]> selectedValuesMinMax = null;
	
	public Map<String, Double[]> getSelectedData(){
		return selectedValuesMinMax == null ? new HashMap<String,Double[]>() : selectedValuesMinMax ;
	}
	
	private Map<String,JCheckBox> parameters = new HashMap<String,JCheckBox>();

	//keeping this here just in case we want it later.
	@SuppressWarnings("unused")
	private JPanel getControlPanel(){
	    
		List<String> pList = Arrays.asList(
		        Msg.CBOX_MEAN(),
		        Msg.CBOX_MEDIAN(),
		        Msg.CBOX_OUTLIERS());
		JPanel cPanel = new JPanel();
		for(String s: pList) {
		    ActionListener checkBoxActionListener = new ActionListener()
		    {
		        @Override
		        public void actionPerformed(ActionEvent e) {
		            redoGenerateChart(s, false);
		        }
		    };
			JCheckBox jcb = new JCheckBox(s);
			jcb.setSelected(true);
			jcb.addActionListener(checkBoxActionListener);			
			parameters.put(s, jcb);	
			cPanel.add(jcb, BorderLayout.CENTER);
		}
		
		return cPanel;
	}
	

	protected void redoGenerateChart(String why, boolean recreateDataSet) {
        Bag<String> missingOrBad = new TreeBag<String>();
        Bag<String> suppressed = new TreeBag<String>();

	    generateChart(recreateDataSet, missingOrBad, suppressed);
	    
	    String msg = VisToolData.createReportText(missingOrBad, suppressed);
	    if (! Check.isEmpty(msg)) {
	        if (! Check.isEmpty(why)) {
	            reportTextArea.append("\n===== " + why);
	            reportTextArea.append("\n");
	        }
	        reportTextArea.append(msg);
	        tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(tabMessages));
	    }
    }

    private XYSeriesCollection createSampleDataSet(Bag<String> missingOrBad, Bag<String> suppressed) {
		
        usedInstanceNames.clear();
        seriesCountByTraitName.clear();

        final XYSeriesCollection dataSets =  new XYSeriesCollection();

        TraitInstanceValueRetriever<?> x_tivr = tivrByTi.get(xInstance);
        
        String xInstanceName = traitNameStyle.makeTraitInstanceName(xInstance);

        xNumberToTraitValue.setTraitInstanceValueRetriever(x_tivr);
        xNumberToTraitValue.clear();
        numberToTraitValueBySeriesIndex.clear();
        Map<Integer, Number> xValueByPlotId = new HashMap<>();
        int count = 0;
        for (TraitInstance instance : valueInstances) {
            
            TraitInstanceValueRetriever<?> y_tivr = tivrByTi.get(instance);

            NumberToTraitValue yNumberToTraitValue = new NumberToTraitValue(y_tivr);
            numberToTraitValueBySeriesIndex.put(count, yNumberToTraitValue);

        	
        	String instanceName = traitNameStyle.makeTraitInstanceName(instance);
        	usedInstanceNames.add(instanceName);
        	seriesCountByTraitName.put(instanceName, count);
        	
        	XYSeries series = new XYSeries(instanceName, true, true);

        	if (DEBUG) {
        	    System.out.println("--START ScatterPlot: " + xInstanceName + " BY " + instanceName); //$NON-NLS-1$ //$NON-NLS-2$
        	}
        	
        	for (PlotOrSpecimen pos : plotSpecimens) {
        	    Plot plot = plotInfoProvider.getPlotByPlotId(pos.getPlotId());
        	    if (plot == null) {
        	        continue;
        	    }
                if (! plot.isActivated()) {
                    continue;
                }

        		int plotId = plot.getPlotId();
        		
        		Number xValue;
        		if (count == 0) {
                    xValue = xNumberToTraitValue.getAxisValue(plot, missingOrBad, suppressed);
                    if (xValue == null) {
                        continue;
                    }
                    xValueByPlotId.put(plotId, xValue);
        		}
        		else {
        		    xValue = xValueByPlotId.get(plotId);
        		    if (xValue == null) {
        		        continue;
        		    }
        		}
 
        		Number yValue = yNumberToTraitValue.getAxisValue(plot, missingOrBad, suppressed);
                if (yValue == null) {
                    continue;
                }
                
        		if  (DEBUG) {
        		    System.out.println("\t" + xValue + " , " + yValue); //$NON-NLS-1$ //$NON-NLS-2$
        		}
        		
				XYDataItem dataItem = new XYDataItem(xValue, yValue);
				series.add(dataItem);
        	}
        	if (DEBUG) {
        	    System.out.println("--END-- ScatterPlot: " + instanceName); //$NON-NLS-1$
        	}


        	dataSets.addSeries(series);
        	count++;
        }

	    return dataSets;
	}
	
	private Map<String, Integer> seriesCountByTraitName = new HashMap<String, Integer>();

	public void setChartSelectionListener(
			ChartMouseListener chartSelectionListener) {
		getChartPanel().addChartMouseListener(chartSelectionListener);
	}

	public void addRangeMarker(XYAnnotation intervalMarker) {
		((XYPlot) getChart().getPlot()).addAnnotation(intervalMarker);
	}
	
	private void setSelectionRange(double minx, double miny,
			double maxx, double maxy) {
		
		dataxMin = minx;
		dataxMax = maxx;
		datayMin = miny;
		datayMax = maxy;
		
        minxModel.setValue(dataxMin);
        maxxModel.setValue(dataxMax);      
        minyModel.setValue(datayMin);
        maxyModel.setValue(datayMax);		
                
	}
	
	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	private void setSelectedTraitAndMeasurementsToNull() {
	    selectedValueStore.setSelectedPlots(toolPanelId, null);
		fireSelectionStateChanged();
		curationControls.updateButtons();
	}
	
	private List<Point2D> selectedPoints = new ArrayList<>();

    private final JTabbedPane tabbedPane = new JTabbedPane();

	@SuppressWarnings("unchecked")
	public void redrawSelectedPlots() {

		if (syncedOption.getSyncWhat().isSync() && ! getSelectionChanging()) {
			XYPlot xyplot = getChart().getXYPlot();

//			curationControls.setSyncedState(true);
			
			Double yscale = (xyplot.getRangeAxis().getUpperBound() - xyplot.getRangeAxis().getLowerBound()) / 50;
			Double xscale = (xyplot.getDomainAxis().getUpperBound() - xyplot.getDomainAxis().getLowerBound()) / 50;

			Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0);				
			Paint paint = Color.RED;

			for (Annotation anno : (List<Annotation>) xyplot.getAnnotations()) {			
				if (anno != intervalAnnotation && anno instanceof XYShapeAnnotation) {
					xyplot.removeAnnotation((XYShapeAnnotation) anno);
				}
			}

			for (Point2D point : selectedPoints) {
				double x = point.getX();
				double y = point.getY();

				double y1 = y - yscale;
				double y2 = y + yscale;
				double x1 = x - xscale;
				double x2 = x + xscale;

				Ellipse2D oval = new Ellipse2D.Double(x1, y1, x2 - x1, y2 - y1);

				XYShapeAnnotation lineanno0 = new XYShapeAnnotation(oval, stroke, paint);	
				addRangeMarker(lineanno0);			
			}
		}
		curationControls.updateButtons();
	}
	
	private void clearExternallySelectedPlots() {
		selectedPoints.clear();
		redrawSelectedPlots();
	}

	private void setXYValues() {

		List<Double> xs = new ArrayList<Double>();
		xs.add(minxModel.getNumber().doubleValue());
		xs.add(maxxModel.getNumber().doubleValue());

		List<Double> ys = new ArrayList<Double>();
		ys.add(minyModel.getNumber().doubleValue());
		ys.add(maxyModel.getNumber().doubleValue());		

		Collections.sort(xs);
		Collections.sort(ys);

		mouseDownPoint = new Point2D.Double(xs.get(0), ys.get(0));
		mouseUpPoint = new Point2D.Double(xs.get(1), ys.get(1));
	}
	
	private void setXYValues(ChartMouseEvent cme) {

		double pointX = cme.getTrigger().getPoint().x;
		double pointY = cme.getTrigger().getPoint().y;

		Rectangle2D plotArea = getChartPanel().getScreenDataArea();
		XYPlot plot = (XYPlot) getChart().getPlot(); 
		Double chartX = plot.getDomainAxis().java2DToValue(pointX, plotArea, plot.getDomainAxisEdge());
		Double chartY = plot.getRangeAxis().java2DToValue(pointY, plotArea, plot.getRangeAxisEdge());

		if (! stillChanging) {
			mouseDownPoint = new Point2D.Double( chartX, chartY );
			stillChanging = true;
		}
		else {
			mouseUpPoint = new Point2D.Double( chartX, chartY );
		}

		if (mouseDownPoint != null && mouseUpPoint != null) {
			
			double dn = mouseDownPoint.getX();
			double up = mouseUpPoint.getX();
			
			double x0 = Math.min(dn, up);
			double x1 = Math.max(dn, up);

			dn = mouseDownPoint.getY();
			up = mouseUpPoint.getY();

			double y0 = Math.min(dn, up);
			double y1 = Math.max(dn, up);
			
			setSelectionRange(x0, y0, x1, y1);
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

		intervalAnnotation = null;
		selectedPoints.clear();

		XYPlot xyplot = getChart().getXYPlot();

		Double yscale = (xyplot.getRangeAxis().getUpperBound() - xyplot.getRangeAxis().getLowerBound()) / 50;
		Double xscale = (xyplot.getDomainAxis().getUpperBound() - xyplot.getDomainAxis().getLowerBound()) / 50;

		removeAnnotations();
		Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3}, 0);				
		Paint paint = Color.RED;

		xyplot.clearRangeMarkers();
		
		TraitInstanceValueRetriever<?> x_tivr = tivrByTi.get(xInstance);

		double[] xy = new double[2];
		
		for (TraitInstance ti : plotsByTraitInstance.getTraitInstances()) {

			if (traitInstances.contains(ti) && ! xInstance.equals(ti)) {
				
				TraitInstanceValueRetriever<?> tivr = tivrByTi.get(ti);
				
				for (PlotOrSpecimen pos : plotsByTraitInstance.getPlotSpecimens((ti))) {
					if (usedPlotSpecimens.isEmpty() || usedPlotSpecimens.contains(pos)) {

						xy = getXY(x_tivr, tivr, pos, xy);

						if (xy != null) {
							double x = xy[0];
							double y = xy[1];
							
							double y1 = y - yscale;
							double y2 = y + yscale;
							double x1 = x - xscale;
							double x2 = x + xscale;

							selectedPoints.add(new Point2D.Double(x,y));

							Ellipse2D oval = new Ellipse2D.Double(x1, y1, x2 - x1, y2 - y1);

							//					Line2D line0 = new Line2D.Double(x1, y2, x2, y1);
							//					Line2D line1 = new Line2D.Double(x1, y1, x2, y2);
							//					
							XYShapeAnnotation lineanno0 = new XYShapeAnnotation(oval, stroke, paint);	
							//					
							//					XYShapeAnnotation lineanno0 = new XYShapeAnnotation(line0, stroke, paint);	
							//					XYShapeAnnotation lineanno1 = new XYShapeAnnotation(line1, stroke, paint);

							addRangeMarker(lineanno0);
							//					plotPanel.addRangeMarker(lineanno1);

						}			
					}
				}
			}

			getChartPanel().repaint();
		}
		curationControls.updateButtons(false);
	}

	private double[] getXY(TraitInstanceValueRetriever<?> x_tivr, TraitInstanceValueRetriever<?> y_tivr, PlotOrSpecimen pos, double[] result) {
		TraitValue xTraitValue = x_tivr.getAttributeValue(plotInfoProvider, pos, null);
		if (xTraitValue == null) {
			return null;
		}
		Comparable<?> x_comp = xTraitValue.comparable;
		if (! (x_comp instanceof Number)) {
			return null;
		}
		double x = ((Number) x_comp).doubleValue();
		
		TraitValue yTraitValue = y_tivr.getAttributeValue(plotInfoProvider, pos, null);
		if (yTraitValue == null) {
			return null;
		}
		Comparable<?> y_comp = yTraitValue.comparable;
		if (y_comp == null) {
			return null;
		}
		if (! (y_comp instanceof Number)) {
			return null;
		}
		
		if (result==null || result.length < 2) {
			result = new double[2];
		}
		result[0] = x;
		result[1] = ((Number) y_comp).doubleValue();
		return result;
	}

	@Override
	protected JFreeChart getJFreeChart() {
		return chart;
	}
	
	@Override
	public boolean refreshData() {
	    redoGenerateChart("Refresh Data", true);
		curationControls.updateButtons();
		return true;
	}
}
