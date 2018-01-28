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

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetrieverFactory;
import com.diversityarrays.kdxplore.curate.data.TraitHelper;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.heatmap.HeatMapParamsDialog.VisToolOpenClose;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.vistool.AbstractVisToolPanel;
import com.diversityarrays.kdxplore.vistool.AbstractVisualisationTool;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.VisToolPanel;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.kdxplore.vistool.VisualisationToolService.VisToolParams;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;

public class HeatMapVisualisationTool extends AbstractVisualisationTool {

	@Override
	public String getToolName() {
		return Msg.TOOLNAME_HEATMAP();
	}
	
    @Override
    public boolean supportsXandYaxes() {
        return true;
    }
    
	@Override
	public Icon getToolIcon() {
		return KDClientUtils.getIcon(ImageId.GRAPH_HEATMAP);
	}
	
	@Override
	public String getToolButtonName() {
		return Msg.TOOLNAME_HEATMAP();
	}

	// TODO remove this
//	private final PlotInfoProvider plotInfoProvider;

	private VisToolOpenClose visToolCloser = new VisToolOpenClose() {		

		@Override
		public void visToolCreated(JFrame frame, VisToolPanel panel) {
			fireVisToolPanelCreated(frame, panel);
		}

		@Override
		public void visToolClosed(JFrame frame, VisToolPanel panel) {
			fireVisToolPanelClosed(frame, panel);
		}
	};
	
	private final SuppressionHandler suppressionHandler;
	
	public HeatMapVisualisationTool(VisToolParams params) {
		super(params.selectedValueStore);
		this.suppressionHandler = params.suppressionHandler;
	}
	
	@Override
	public Either<String,List<JFrame>> getVisualisationDialogs(CurationContext context, List<TraitInstance> selectedInstances) 
	{
		Map<TraitInstance,SimpleStatistics<?>> statsByTraitInstance = context.getStatsByTraitInstance();

		if (! VisToolUtil.allowSubplotTraits) {
		    Set<TraitInstance> set = new HashSet<>(statsByTraitInstance.keySet());
		    for (TraitInstance ti : set) {
		        if (TraitLevel.PLOT != ti.trait.getTraitLevel()) {
		            statsByTraitInstance.remove(ti);
		        }
		    }
		}
		
		// First check = before we remove non-numerics
		if (statsByTraitInstance.isEmpty()) {
			return Either.left(Msg.MSG_NO_INSTANCES_WITH_NUMERIC_DATA());
		}

		List<JFrame> frames = new ArrayList<>();
		// See getMenuBuilderRequirementsBySource()
		if (selectedInstances != null) {

			Trial trial = context.getTrial();

            Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
                @Override
                public List<KdxSample> apply(TraitInstance ti) {
                    return context.getPlotInfoProvider().getSampleMeasurements(ti);
                }
            };

			List<TraitInstanceValueRetriever<?>> values = new ArrayList<>();
			for (TraitInstance ti : selectedInstances) {
			    if (! VisToolUtil.allowSubplotTraits) {
			        if (TraitLevel.PLOT != ti.trait.getTraitLevel()) {
			            continue;
			        }
			    }
				try {
                    TraitInstanceValueRetriever<?> tivr = TraitInstanceValueRetriever.getValueRetriever(trial, ti, sampleProvider);
					values.add(tivr);
				} catch (InvalidRuleException ignore) {
				}
			}
			
			if (values.isEmpty()) {
				return Either.left(Msg.ERRMSG_NO_VALUE_DATA_FOR_HEATMAP());
			}
			
			ValueRetriever<?> xvr = null;
			ValueRetriever<?> yvr = null;
			
			List<ValueRetriever<?>> valueRetrievers = ValueRetrieverFactory.getPlotIdentValueRetrievers(trial);
			for (ValueRetriever<?> vr : valueRetrievers) {
				switch (vr.getTrialCoord()) {
				case NONE:
					break;
				case PLOT_ID:
					break;
				case X:
					xvr = vr;
					break;
				case Y:
					yvr = vr;
					break;
				}
			}
			
			if (xvr==null || yvr ==null) {
				return Either.left(Msg.ERRMSG_NO_X_AND_Y_FOR_HEATMAP());
			}

			HeatMapPanelFactory factory = new HeatMapPanelFactory(this, suppressionHandler);

			for (TraitInstanceValueRetriever<?> tivr : values) {
				
				@SuppressWarnings("rawtypes")
                Pair<HeatMapPanelParameters, HeatMapPanel> pair = 
						factory.createHeatMap(context, 
								selectedValueStore, 
								HeatMapParamsDialog.getNextId(), 
								xvr, yvr, 
								tivr);
				
				HeatMapPanelParameters params = pair.first;
				AbstractVisToolPanel heatMapPanel = pair.second;

				final JFrame frame = context.addVisualisationToolUI(heatMapPanel);

				visToolCloser.visToolCreated(frame, heatMapPanel);
				
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						frame.removeWindowListener(this);

						heatMapPanelParamsByFrame.remove(frame);

						visToolCloser.visToolClosed(frame, heatMapPanel);
					}
				});
				
				heatMapPanelParamsByFrame.put(frame, params);
				
				frames.add(frame);
			}
			
			return Either.right(frames);
		}

		// We only want the TraitInstances with Integer or Double datatypes
		
		List<TraitInstance> list = new ArrayList<TraitInstance>(statsByTraitInstance.keySet());
			
		for (TraitInstance ti : list) {
			Class<? extends Comparable<?>> xClass = TraitHelper.getTraitDataTypeValueClass(ti.trait);
			if (! Number.class.isAssignableFrom(xClass)) {
				statsByTraitInstance.remove(ti);
			}
		}

		// First check = before we remove non-numerics
		if (statsByTraitInstance.isEmpty()) {
			// TODO allow non-numeric and add support to HeatMap
			return Either.left(Msg.MSG_NO_INSTANCES_WITH_NUMERIC_DATA());
		}
		
		HeatMapParamsDialog dialog = new HeatMapParamsDialog(
				HeatMapVisualisationTool.this,
				context, 
				selectedValueStore,
				statsByTraitInstance,
				heatMapPanelParamsByFrame,
				visToolCloser,
				suppressionHandler);

		dialog.setVisible(true);
			
		return Either.right(null);		
	}

	private final Map<JFrame,HeatMapPanelParameters> heatMapPanelParamsByFrame = new LinkedHashMap<JFrame, HeatMapPanelParameters>();

	@Override
	public int[] getDataRequirements() {
		// self contained data handling
		return null;
	}


	@Override
	public Either<String,List<JFrame>> getVisualisationDialogsUsingPlots(CurationContext context,
			List<TraitInstance> selectedInstances, List<PlotOrSpecimen> plotSpecimens) 
	{
		// TODO Auto-generated method stub
		return Either.right(null);
	}

	@Override
	public List<Action> createActionsFor(CurationContext context,
	        List<PlotOrSpecimen> plotSpecimens, // Not supported for HeatMap
			List<TraitInstance> notCheckedInstances,
			TraitInstance activeInstance,
			List<TraitInstance> selectedInstances) 
	{
		// activeInstance is one of the checkedInstances so don't do anything special for it
		Trial trial = context.getTrial();
		
		List<TraitInstance> allNumericInstances = collectNumericInstances(context, ALLOW_CATEGORICAL, selectedInstances);
		
		Pair<ValueRetriever<?>,ValueRetriever<?>> xyvr = ValueRetrieverFactory.getXyValueRetrievers(trial);
		
		ValueRetriever<?> xValueRetriever = xyvr.first;
		ValueRetriever<?> yValueRetriever = xyvr.second;
		
		if (xValueRetriever == null) {
			String name = (yValueRetriever==null) 
			        ? Msg.ACTION_NO_X_OR_Y_AVAILABLE() 
			        : Msg.ACTION_NO_X_AVAILABLE();
			Action action = new AbstractAction(name) {
				@Override
				public void actionPerformed(ActionEvent e) {
				    MsgBox.info(context.getDialogOwnerWindow(),
				            Msg.MSG_USE_BUTTON_ON_TOOLBAR(),
				            Msg.TOOLNAME_HEATMAP());
				}
			};
			return Arrays.asList(action);
		}

		if (yValueRetriever == null) {
			String name = Msg.ACTION_NO_Y_AVAILABLE();
			Action action = new AbstractAction(name) {
				@Override
				public void actionPerformed(ActionEvent e) { 
                    MsgBox.info(context.getDialogOwnerWindow(),
                            Msg.MSG_USE_BUTTON_ON_TOOLBAR(), 
                            Msg.TOOLNAME_HEATMAP());
				}
			};
			return Arrays.asList(action);
		}
		
		Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
            @Override
            public List<KdxSample> apply(TraitInstance ti) {
                return context.getPlotInfoProvider().getSampleMeasurements(ti);
            }
        };
        
        List<TraitInstance> check = allNumericInstances;
        if (! VisToolUtil.allowSubplotTraits) {
            allNumericInstances = check.stream().filter(ti -> TraitLevel.PLOT == ti.trait.getTraitLevel()).collect(Collectors.toList());
        }
        Map<TraitInstance, TraitInstanceValueRetriever<?>> tivrByTi = 
				VisToolUtil.buildTraitInstanceValueRetrieverMap(trial, allNumericInstances, sampleProvider);

        String disabledForSubPlotName = Msg.ACTION_VISTOOL_DISABLED_FOR_LEVEL(getToolName(), TraitLevel.SPECIMEN.visible);

		if (tivrByTi.isEmpty()) {
            String name = Msg.ERRMSG_NO_VALUE_DATA_FOR_HEATMAP();
		    if (! check.isEmpty()) {
                name = disabledForSubPlotName;
		    }
			Action action = new AbstractAction(name) {
				@Override
				public void actionPerformed(ActionEvent e) { }
			};
			return Arrays.asList(action);
		}
		
        List<Action> result = new ArrayList<>();

        boolean addedDisabledAction = false;
        if (check.size() != allNumericInstances.size()) {
            Action action = new AbstractAction(disabledForSubPlotName) {
                @Override
                public void actionPerformed(ActionEvent e) {}
            };
            action.setEnabled(false);
		    result.add(action);
		    
		    addedDisabledAction = true;
		}

		final HeatMapPanelFactory factory = new HeatMapPanelFactory(this, suppressionHandler);
		
		for (final TraitInstanceValueRetriever<?> tivr : tivrByTi.values()) {
			TraitInstance ti =  tivr.getTraitInstance();
			String name = context.makeTraitInstanceName(ti);
			
			String title = Msg.ACTION_X_Y_WITH_NAME(name);
			Action action = new AbstractAction(title) {
				@Override
				public void actionPerformed(ActionEvent e) {
					@SuppressWarnings("rawtypes")
                    Pair<HeatMapPanelParameters, HeatMapPanel> pair = factory.createHeatMap(
							context, 
							selectedValueStore,
							nextChartId++, 
							xValueRetriever, 
							yValueRetriever, 
							tivr);

					HeatMapPanelParameters hmpp = pair.first;
					final AbstractVisToolPanel heatMapPanel = pair.second;
					
					final JFrame frame = context.addVisualisationToolUI(heatMapPanel);
					frame.setTitle(heatMapPanel.getTitle());
					
					CurationDataChangeListener cdcl = new CurationDataChangeListener() {
						@Override
						public void plotActivationChanged(Object source, boolean activated, List<Plot> plots) {
							heatMapPanel.plotActivationsChanged(activated, plots);
						}
						
						@Override
						public void editedSamplesChanged(Object source, List<CurationCellId> curationCellIds) {
							heatMapPanel.editedSamplesChanged();
						}
					};
					context.getPlotInfoProvider().addCurationDataChangeListener(cdcl);
					
					fireVisToolPanelCreated(frame, heatMapPanel);
					
					heatMapPanelParamsByFrame.put(frame, hmpp);
					frame.addWindowListener(new WindowAdapter() {
						@Override
						public void windowClosed(WindowEvent e) {
							frame.removeWindowListener(this);
							context.getPlotInfoProvider().removeCurationDataChangeListener(cdcl);
							heatMapPanelParamsByFrame.remove(frame);

							fireVisToolPanelClosed(frame, heatMapPanel);
						}
					});
				}
				
			};
			result.add(action);
		}
		
		if (result.size() > 1 && ! addedDisabledAction) {
			final List<Action> allActions = new ArrayList<>(result);
			Action all = new AbstractAction(Msg.ACTION_ALL_TRAITS()) {
				@Override
				public void actionPerformed(ActionEvent e) {
					Set<JFrame> before = new HashSet<>(heatMapPanelParamsByFrame.keySet());
					for (Action a : allActions) {
						a.actionPerformed(e);
					}
					
					List<JFrame> frames = new ArrayList<>();
					for (JFrame frame : heatMapPanelParamsByFrame.keySet()) {
						if (! before.contains(frame)) {
							frames.add(frame);
						}
					}

					VisToolUtil.staggerOpenedFrames(frames);
				}				
			};
			result.add(all);
		}
		return result;
	}

	
	static private int nextChartId = 1;
}
