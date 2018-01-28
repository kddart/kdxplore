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
package com.diversityarrays.kdxplore.heatmap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.PlotsByTraitInstance;
import com.diversityarrays.kdxplore.curate.SampleName;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressOption;
import com.diversityarrays.kdxplore.curate.SuppressionArgs;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever.TrialCoord;
import com.diversityarrays.kdxplore.vistool.AbstractVisToolPanel;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.VisToolData;
import com.diversityarrays.kdxplore.vistool.VisToolbarFactory;
import com.diversityarrays.kdxplore.vistool.VisToolbarFactory.ImageFormat;
import com.diversityarrays.kdxplore.vistool.VisualisationTool;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MsgBox;

import net.pearcan.heatmap.Gradient;
import net.pearcan.heatmap.GradientComboBoxRenderer;
import net.pearcan.heatmap.HeatMap;
import net.pearcan.heatmap.HeatMapModel;
import net.pearcan.heatmap.HeatMapPane;
import net.pearcan.heatmap.HeatMapRenderer;
import net.pearcan.heatmap.ValueModel;
import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.GBH;

@SuppressWarnings("nls")
public class HeatMapPanel<T extends Number> extends AbstractVisToolPanel {

	public static boolean DEBUG = Boolean.getBoolean(HeatMapPanel.class.getSimpleName()+".DEBUG");

    private final String TAB_CURATION = Msg.TAB_CURATION();

	private final String TAB_MESSAGES = Msg.TAB_MESSAGES();

	private final String NO_MARK = Msg.OPTION_DO_NOT_SHOW_MARK();
	
	static private int nextId = 1;
	
	private final JComboBox<Gradient> gradientComboBox;
	
	private String title;
	private final HeatMapPane<PlotOrSpecimen> heatMap;

	private final Integer windowId = nextId++;
	
	private final PropertyChangeListener heatMapPaneSelectionChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			String propertyName = event.getPropertyName();
			if (HeatMapPane.PROPERTY_SELECTION_CHANGE.equals(propertyName) && ! getSelectionChanging()) {
				updateSelectedPlots();
				fireSelectionStateChanged();
			}
		}
	};

	private final ItemListener gradientItemListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				Gradient g = (Gradient) e.getItem();
				heatMap.setGradient(g);
			}
		}
	};
	
	private final TraitInstance zTraitInstance;
	
	private final PlotInfoProvider plotInfoProvider;
	
	private final  MarkInfo markInfo;
	
	private final JTextArea messagesTextArea = new JTextArea();
	private final JTabbedPane tabbedPane = new JTabbedPane();
	
	private final CurationContext context;
	private final ValueRetriever<?> xValueRetriever;
	private final ValueRetriever<?> yValueRetriever;
	private final TraitInstanceValueRetriever<?> traitInstanceValueRetriever;

	private final boolean askAboutValueForUnscored;
	
	private final HeatMapRenderer<PlotOrSpecimen> diagonalDecorator = new HeatMapRenderer<PlotOrSpecimen>() {

	    int stripeGap = 8;

        @Override
        public void draw(HeatMap<PlotOrSpecimen> heatMap, 
                Graphics2D g2d, 
                int width, int height,
                boolean isSelected, Point pt, 
                PlotOrSpecimen cellContents) 
        {
            Color xorColor;
            Color color = heatMap.getColorByPosition().get(pt);
            if (color == null) {
                if (DEBUG) {
                    System.out.println("pt=" + pt.x+","+pt.y + " color==null");
                }
                xorColor = Color.BLACK;
            }
            else {
                int rgb = color.getRGB();
                if (DEBUG) {
                    System.out.println("pt=" + pt.x+","+pt.y + " color==" + Integer.toHexString(rgb));
                }
                rgb = (rgb ^ 0xffffffff) | 0xff000000;
                xorColor = new Color(rgb);
            }

            g2d.setXORMode(xorColor);
            for (int xi = 0; (xi+stripeGap) < width; xi += stripeGap) {
                g2d.drawLine(xi, height, xi + stripeGap, 0);
            }

            g2d.setPaintMode();
        }
	    
	};
	
	public HeatMapPanel(
			VisualisationTool vtool, 
			int unique,
			SelectedValueStore svs,
			PlotInfoProvider pip,
			String title, 
			
			HeatMapModelData<T> heatMapModelData,
			SuppressionHandler suppressionHandler) 
	{
		super(title, svs, vtool.getVisualisationToolId(), unique, Arrays.asList(heatMapModelData.zTraitInstance), suppressionHandler);

		context = heatMapModelData.context;
		xValueRetriever = heatMapModelData.xValueRetriever;
		yValueRetriever = heatMapModelData.yValueRetriever;
		
		askAboutValueForUnscored = ! ValueRetriever.isEitherOneXorY(xValueRetriever, yValueRetriever);
		
		traitInstanceValueRetriever = heatMapModelData.traitInstanceValueRetriever;
		this.zTraitInstance = heatMapModelData.zTraitInstance;
//		this.plotSpecimensByPoint = heatMapModelData.model.getCellLegend();
		
		this.plotInfoProvider = pip;
		this.title = title;
		
		if (heatMapModelData.plotPointsByMark.isEmpty()) {
			markInfo = null;
		}
		else {
			markInfo = new MarkInfo(Msg.LABEL_MARK_INFO_PLOT_TYPE(), heatMapModelData.plotPointsByMark);
		}
        
        this.heatMap = createHeatMap(heatMapModelData);
        
		this.heatMap.addPropertyChangeListener(HeatMapPane.PROPERTY_SELECTION_CHANGE,
				heatMapPaneSelectionChangeListener);
		
		messagesTextArea.setEditable(false);
		tabbedPane.addTab(TAB_MESSAGES, new JScrollPane(messagesTextArea));
		tabbedPane.addTab(TAB_CURATION, createCurationControls());
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				tabbedPane,
				heatMap);
		
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.0);
		add(splitPane, BorderLayout.CENTER);

		int cellValueCount = heatMap.getValueModel().getUniqueCellValueCount();
		Gradient[] gradients = Gradient.createBuiltins(cellValueCount);
		Gradient initial = null;
		for (Gradient g : gradients) {
			if (Gradient.RAINBOW_NAME.equals(g.getName())) {
				initial = g;
				break;
			}
		}
		
		gradientComboBox = new JComboBox<Gradient>(gradients);
		gradientComboBox.setRenderer(new GradientComboBoxRenderer());
		gradientComboBox.addItemListener(gradientItemListener);

		// gradientItemListener will to model.setGradient()
		gradientComboBox.setSelectedItem(initial);

		Box box = Box.createHorizontalBox();
		box.add(syncedOption);
		box.add(gradientComboBox);
		box.add(Box.createHorizontalGlue());
//		if (RunMode.getRunMode().isDeveloper()) {
//	        box.add(useBlankTilesOption);
//		}
        box.add(new JSeparator(JSeparator.VERTICAL));
        box.add(opacityLabel);
		box.add(unselectedOpacitySpinner);
		
		add(box, BorderLayout.SOUTH);
		
		opacityLabel.setToolTipText("Sets the opacity of unselected cells");
		unselectedOpacitySpinner.setToolTipText("Sets the opacity of unselected cells");
		
//		useBlankTilesOption.setSelected(heatMap.getUseBlankTiles());
		unselectedOpacityModel.setValue(heatMap.getUnselectedOpacity());
		
//		useBlankTilesOption.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                handleUseBlankTilesChanged();
//            }
//        });
		
		unselectedOpacityModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                handleOpacityChange();
            }
        });

        updateMessagesWithMissingOrBad(null, heatMapModelData);

        applyDecoratorsToHeatMap(heatMapModelData);
	}

//	private final JCheckBox useBlankTilesOption = new JCheckBox("Blanks");
	
//	private void handleUseBlankTilesChanged() {
//	    heatMap.setUseBlankTiles(useBlankTilesOption.isSelected());
//	}
	
	private void handleOpacityChange() {
	    float opacity = unselectedOpacityModel.getNumber().floatValue();
        heatMap.setUnselectedOpacity(opacity);
        System.out.println(String.format("Unselected Opacity: %.2f", opacity));
	}
	
	private final JLabel opacityLabel = new JLabel("Opacity:");
	private final SpinnerNumberModel unselectedOpacityModel = new SpinnerNumberModel(0.1, 0.05, 1.0, 0.05);
	private final NumberSpinner unselectedOpacitySpinner = new NumberSpinner(unselectedOpacityModel, "0.00");
	
    private HeatMapPane<PlotOrSpecimen> createHeatMap(HeatMapModelData<T> hmmd) {
        
        TrialLayout trialLayout = context.getTrial().getTrialLayout();
        
        boolean useGraphicsXaxis = true;
        if (TrialCoord.X == xValueRetriever.getTrialCoord()) {
            switch (trialLayout.getOrigin()) {
            case LOWER_LEFT:
                useGraphicsXaxis = true;
                break;
            case LOWER_RIGHT:
                useGraphicsXaxis = false;
                break;
            case UPPER_LEFT:
                useGraphicsXaxis = true;
                break;
            case UPPER_RIGHT:
                useGraphicsXaxis = false;
                break;
            }       
        }
        
        boolean useGraphicsYaxis = false;
        if (TrialCoord.Y == yValueRetriever.getTrialCoord()) {
            switch (trialLayout.getOrigin()) {
            case LOWER_LEFT:
                useGraphicsYaxis = false;
                break;
            case LOWER_RIGHT:
                useGraphicsYaxis = false;
                break;
            case UPPER_LEFT:
                useGraphicsYaxis = true;
                break;
            case UPPER_RIGHT:
                useGraphicsYaxis = true;
                break;
            }
        }

        HeatMapModel<PlotOrSpecimen> hmodel = (HeatMapModel<PlotOrSpecimen>) hmmd.model;
        ValueModel<PlotOrSpecimen, ? extends Number> vmodel = hmmd.valueModel;

        HeatMapPane<PlotOrSpecimen> result = new HeatMapPane<PlotOrSpecimen>(
                hmodel, 
                vmodel,
                useGraphicsYaxis,
                useGraphicsXaxis);

//        result.setDrawXAxisTitle(true);
        result.setDrawXLabels(true);
        
//        result.setDrawYAxisTitle(true);
        result.setDrawYLabels(true);

        result.setUnselectedOpacity(0.1f);
        updateHeatMapToolTipsAndOffsets(hmmd, result);

        return result;
	}

    private void updateMessagesWithMissingOrBad(String why, HeatMapModelData<T> heatMapModelData) {
        int tabIndex;
        
        String msg = VisToolData.createReportText(heatMapModelData.missingOrBad, heatMapModelData.suppressed);

        if (Check.isEmpty(msg)) {
            tabIndex = tabbedPane.indexOfTab(TAB_CURATION);
        }
        else {
            if (! Check.isEmpty(why)) {
                messagesTextArea.append("\n==== " + why);
            }
            messagesTextArea.append("\n");
            messagesTextArea.append(msg);
            tabIndex = tabbedPane.indexOfTab(TAB_MESSAGES);
        }
        tabbedPane.setSelectedIndex(tabIndex);
    }

    public void applyDecoratorsToHeatMap(HeatMapModelData<T> hmmd) {
        
        HeatMapModel<PlotOrSpecimen> model = hmmd.model;
        Map<Point, PlotOrSpecimen> plotSpecimensByPoint = model.getCellContentByPoint();

        Point[] decoratedPoints = null;
        
        Set<Integer> inactivePlotIds = plotInfoProvider.getPlots().stream()
            .filter(p -> ! p.isActivated())
            .map(Plot::getPlotId)
            .collect(Collectors.toSet());

		if (! inactivePlotIds.isEmpty()) {
			Predicate<Point> containsInactivePlotId = new Predicate<Point>() {
                @Override
                public boolean test(Point pt) {
                    PlotOrSpecimen pos = plotSpecimensByPoint.get(pt);
                    if (pos==null) {
                        return false;
                    }
                    return inactivePlotIds.contains(pos.getPlotId());
                }
			};
			Set<Point> points = plotSpecimensByPoint.keySet().stream()
                .filter(containsInactivePlotId)
                .collect(Collectors.toSet());
			if (! points.isEmpty()) {
				decoratedPoints = points.toArray(new Point[points.size()]);
			}
		}
		
        heatMap.setDecoratedPoints(diagonalDecorator, decoratedPoints);
    }
    
    private void updateHeatMapToolTipsAndOffsets(HeatMapModelData<T> hmmd, HeatMapPane<PlotOrSpecimen> hmp) {
        Map<Point,PlotOrSpecimen> tooltipIds = null;
        if (ValueRetriever.TrialCoord.X == xValueRetriever.getTrialCoord() 
                && 
                ValueRetriever.TrialCoord.Y == yValueRetriever.getTrialCoord()) 
        {
            tooltipIds = hmmd.model.getCellContentByPoint();
        }

        @SuppressWarnings("unchecked")
        HeatMapToolTipFactory toolTipFactory = new HeatMapToolTipFactory(
                hmmd.context, 
                zTraitInstance, 
                hmmd.model, 
                (ValueModel<PlotOrSpecimen, Number>) hmmd.valueModel, 
                hmmd.columnRowLabelOffset, 
                tooltipIds);

        hmp.setToolTipFactory(toolTipFactory);
        Point pt = hmmd.columnRowLabelOffset;
        hmp.setColumnRowLabelOffsets(pt.x, pt.y);
    }

	private final Action acceptSamplesAction = new AbstractAction(Vocab.ACTION_ACCEPT_TRAIT_VALUES()) {
		@Override
		public void actionPerformed(ActionEvent e) {			
			List<SampleName> samples = new ArrayList<SampleName>();
			for (TraitInstance ti : HeatMapPanel.this.traitInstances) {
                PlotsByTraitInstance psByTi = selectedValueStore.getSelectedPlotsForToolId(HeatMapPanel.this.toolPanelId);
                for (Plot plot : psByTi.getPlots(ti, plotInfoProvider)) {
					SampleName sampleName = new SampleName(plot.getPlotId(), ti.getInstanceNumber(), ti.getTraitId());
					samples.add(sampleName);		
				}
			}
			if (samples.size() > 0) {
				SuppressionArgs sargs = suppressionHandler.createSuppressionArgs(
				        askAboutValueForUnscored, samples, HeatMapPanel.this.traitInstances);
				suppressionHandler.setSamplesSuppressed(SuppressOption.ACCEPT, sargs, HeatMapPanel.this);
			}
		}
	};
	
	private final Action rejectSamplesAction = new AbstractAction(Vocab.ACTION_SUPPRESS_TRAIT_VALUES()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			List<SampleName> sampleNames = new ArrayList<SampleName>();							
			for (TraitInstance ti : HeatMapPanel.this.traitInstances) {
			    PlotsByTraitInstance psByTi = selectedValueStore.getSelectedPlotsForToolId(HeatMapPanel.this.toolPanelId);
				for (Plot plot : psByTi.getPlots(ti, plotInfoProvider)) {
					SampleName sampleName = new SampleName(plot.getPlotId(), ti.getInstanceNumber(), ti.getTraitId());
					sampleNames.add(sampleName);	
				}
			}				
			if (sampleNames.size() > 0) {
				SuppressionArgs sargs = suppressionHandler.createSuppressionArgs(
				        askAboutValueForUnscored, sampleNames, HeatMapPanel.this.traitInstances);
				suppressionHandler.setSamplesSuppressed(SuppressOption.REJECT, sargs, HeatMapPanel.this);
			}
		}
	};

	private final Action deActivatePlotsAction = new AbstractAction(Msg.ACTION_DEACTIVATE_PLOTS()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (! selectedPlots.isEmpty()) {
				plotInfoProvider.changePlotsActivation(false, selectedPlots);
				updateButtons();
			}
		}
	};
	
	private final Action reActivatePlotsAction = new AbstractAction(Msg.ACTION_ACTIVATE_PLOTS()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (! selectedPlots.isEmpty()) {
				plotInfoProvider.changePlotsActivation(true, selectedPlots);
                updateButtons();
			}
		}
	};

	private final List<Plot> selectedPlots = new ArrayList<>();
	
	private String markName;
	private final HeatMapRenderer<PlotOrSpecimen> markInfoRenderer = new HeatMapRenderer<PlotOrSpecimen>() {
        @Override
        public void draw(HeatMap<PlotOrSpecimen> hmap, Graphics2D g2d, int width, int height, boolean isSelected, Point xy, PlotOrSpecimen pos) {
            Color saved = g2d.getColor();

            int rgb = saved.getRGB();
            rgb = rgb ^ 0xffffffff;
            rgb = rgb | 0xff000000;
            Color xorColor = new Color(rgb);
            g2d.setXORMode(xorColor);
//            g2d.setXORMode(Color.WHITE);
//            g2d.setColor(drawColor);

            if (Check.isEmpty(markName)) {
                g2d.drawLine(0, 0, width, height);
                g2d.drawLine(width, 0, 0, height);
            }
            else {
                FontMetrics fm = g2d.getFontMetrics();
                int lineHeight = fm.getAscent() + fm.getDescent();
                g2d.drawString(markName, 0, lineHeight);
            }

            g2d.setPaintMode();
            g2d.setColor(saved);
        }
    };
	
	private JComponent createCurationControls() {
		
	    rejectSamplesAction.setEnabled(false);
		acceptSamplesAction.setEnabled(false);

		deActivatePlotsAction.setEnabled(false);
		reActivatePlotsAction.setEnabled(false);

		//                      | deactivate
		//   accept  suppress   | reactivate
		
        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel, 2,2,2,2);

        gbh.add(0,0, 1,1, GBH.NONE, 1,1, GBH.WEST, new JButton(acceptSamplesAction));
        gbh.add(1,0, 1,1, GBH.NONE, 1,1, GBH.WEST, new JButton(rejectSamplesAction));
//        if (! askAboutValueForUnscored) {
//            // TODO i18n
//            gbh.add(0,1, 2,1, GBH.HORZ, 1,1, GBH.CENTER, "Unscored samples ignored");
//        }
        
        if (xValueRetriever.isPlotColumn() && yValueRetriever.isPlotRow()) {
            gbh.add(2,0, 1,2, GBH.VERT, 1,1, GBH.CENTER, new JSeparator(JSeparator.VERTICAL));
            gbh.add(3,0, 1,1, GBH.NONE, 1,1, GBH.EAST, new JButton(deActivatePlotsAction));
            gbh.add(3,1, 1,1, GBH.NONE, 1,1, GBH.EAST, new JButton(reActivatePlotsAction));
        }

		if (markInfo != null) {
			JComboBox<String> combo = new JComboBox<>();
			combo.addItem(NO_MARK);
			for (String s : markInfo.plotPointsByMark.keySet()) {
				combo.addItem(s);
			}
			combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<Point> points = null;
                    Object item = combo.getSelectedItem();
                    if (item != null && ! NO_MARK.equals(item)) {
                        points = markInfo.plotPointsByMark.get(item);
                        markName = item.toString();
                    }

                    if (Check.isEmpty(points)) {
                        heatMap.removeDecoratedPoints(markInfoRenderer);
                    }
                    else {
                        heatMap.setDecoratedPoints(markInfoRenderer, points.toArray(new Point[points.size()]));
                    }
                }
            });
			
			Box newBox = Box.createHorizontalBox();
			newBox.add(new JLabel(markInfo.label));
			newBox.add(combo);
			
			gbh.add(0,2, 4,1, GBH.HORZ, 1,1, GBH.CENTER, newBox);
		}
		
		return panel;
	}


	private void updateSelectedPlots() {

		selectedPlots.clear();

		for (PlotOrSpecimen pos  : getSelectedPlotIds()) {
			Plot plot = plotInfoProvider.getPlotByPlotId(pos.getPlotId());
			selectedPlots.add(plot);
		}			

		PlotsByTraitInstance plotsByTi = new PlotsByTraitInstance();
		for (Plot plot : selectedPlots) {
		    plotsByTi.addPlot(zTraitInstance, plot);
		}

		selectedValueStore.setSelectedPlots(toolPanelId, plotsByTi);
		
		updateButtons();
	}

	public HeatMapPane<PlotOrSpecimen> getHeatMap() {
		return heatMap;
	}
	
	@Override
	public void plotActivationsChanged(boolean activated, List<Plot> plots) {

	    HeatMapModel<PlotOrSpecimen> model = heatMap.getModel();

	    Map<Point, PlotOrSpecimen> plotSpecimensByPoint = model.getCellContentByPoint();
		Set<Point> points = new HashSet<>();
		for (Plot plot : plotInfoProvider.getPlots()) {
			if (! plot.isActivated()) {
				for (Point pt : plotSpecimensByPoint.keySet()) {
                    PlotOrSpecimen pos = plotSpecimensByPoint.get(pt);
//					for (PlotOrSpecimen pos : plotSpecimensByPoint.get(pt)) {
						if (plot.getPlotId() == pos.getPlotId()) {
							points.add(pt);
						}
//					}
				}
			}
		}
		
		Point[] array = points.toArray(new Point[points.size()]);
        heatMap.setDecoratedPoints(diagonalDecorator, array);
        
		updateRefreshButton();
	}
	
	@Override
	public void editedSamplesChanged() {
		updateRefreshButton();
	}


	@Override
	public void updateSelectedSamples() {
		PlotsByTraitInstance plotsByTraitInstance = selectedValueStore.getSyncedPlotsByTrait();
		if (plotsByTraitInstance != null) {

			selectedPlots.clear();
			
			Set<Point> points = new HashSet<Point>();
			
			Set<Plot> plots = null;
            switch (getSyncWhat()) {
			case DONT_SYNC:
				break;
			case SYNC_ALL:
                plots = plotsByTraitInstance.getPlots(null, plotInfoProvider);
				break;
			case TRAIT_ONLY:
				plots = plotsByTraitInstance.getPlots(zTraitInstance, plotInfoProvider);
				break;
			default:
				break;
			}

			if (! Check.isEmpty(plots)) {
			    Map<Point, PlotOrSpecimen> plotSpecimensByPoint = heatMap.getModel().getCellContentByPoint();
				for (Plot plot : plots) {
					for (Point pt : plotSpecimensByPoint.keySet()) {
                        PlotOrSpecimen pos = plotSpecimensByPoint.get(pt);
//						for (PlotOrSpecimen pos : plotSpecimensByPoint.get(pt)) {
							if (plot.getPlotId() == pos.getPlotId()) {
								points.add(pt);
								break;
							}
//						}
					}
				}
			}
			
			stillChanging = true;
			try {
				heatMap.setSelectedPoints(points);
			}
			finally {
				stillChanging = false;
			}				

		}
		
//		updateButtons();
		repaint();
	}
	
	private void updateButtons() {
		boolean samples = selectedPlots.size() > 0;
		boolean someActive = false;
		boolean someInactive = false;
		
		for (Plot plot : selectedPlots) {
			if (plot.isActivated()) {
				someActive = true;
				if (someInactive) {
				    break;
				}
			} else {
				someInactive = true;
				if (someActive) {
				    break;
				}
			}
		}
		
		acceptSamplesAction.setEnabled(samples);
		rejectSamplesAction.setEnabled(samples);
		deActivatePlotsAction.setEnabled(someActive);
		reActivatePlotsAction.setEnabled(someInactive);
	}

	private Set<PlotOrSpecimen> getSelectedPlotIds(){
			
		Set<Point> selectedPoints = heatMap.getSelectedPoints();
		Map<Point, PlotOrSpecimen> plotSpecimensByPoint = heatMap.getModel().getCellContentByPoint();
		List<PlotOrSpecimen> list = plotSpecimensByPoint.keySet().stream()
		    .filter(pt -> selectedPoints.contains(pt))
		    .map(pt -> plotSpecimensByPoint.get(pt))
		    .collect(Collectors.toList());

		Set<PlotOrSpecimen> result = new HashSet<>();
		for (PlotOrSpecimen posArray : list) {
		    Collections.addAll(result, posArray);
		}

		return result;
	}
	
	// DesktopObject

	@Override
	public JPanel getJPanel() {
		return this;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public JMenuBar getJMenuBar() {
		return null;
	}


	private Closure<File> snapshotter = new Closure<File>() {
		@Override
		public void execute(File xfile) {
			File file = xfile;
			ImageFormat imageFormat = VisToolbarFactory.getImageFormatName(file);
			if (imageFormat == null) {
				imageFormat = ImageFormat.PNG;
				file = new File(file.getParent(), file.getName() + imageFormat.suffix);
			}
			
			Dimension sz = heatMap.getSize();
			BufferedImage img = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics g = img.getGraphics();
			heatMap.paint(g);
			g.dispose();
			
			try {
				ImageIO.write(img, imageFormat.formatName, file);
			} catch (IOException e) {
				MsgBox.error(HeatMapPanel.this,  e.getMessage(), Msg.ERRTITLE_UNABLE_TO_SNAPSHOT());
			}
		}
	};

	@Override
	public void doPostOpenActions() {
	}

	@Override
	public boolean canClose() {
		return true;
	}

	@Override
	public boolean isClosable() {
		return true;
	}

	@Override
	public Object getWindowIdentifier() {
		return windowId;
	}

	@Override
	protected void updateSyncedOption() {
		updateSelectedSamples();
	}
	
	@Override
	protected Closure<File> getSnapshotter() {
		return snapshotter;
	}
	
	@Override
	public boolean refreshData() {
	    // FIXME in a SubPlot variant of HeatMapPanel, use a SubPlot variant
	    //       of HeatMapModelData as well
		HeatMapModelData<T> hmmd = new HeatMapModelData<T>(
				context,
				xValueRetriever,
				yValueRetriever,
				traitInstanceValueRetriever);
		
		updateMessagesWithMissingOrBad("Refresh Data", hmmd);
		applyDecoratorsToHeatMap(hmmd);   

		heatMap.setModel(hmmd.model);
		
        updateHeatMapToolTipsAndOffsets(hmmd, heatMap);

		return true;
	}
}
