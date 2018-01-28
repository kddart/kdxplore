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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.vistool.AbstractVisToolPanel;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.SimpleVisualisationTool;
import com.diversityarrays.kdxplore.vistool.VisToolData;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.kdxplore.vistool.VisualisationToolService.VisToolParams;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

public class ScatterPlotVisualisationTool extends SimpleVisualisationTool {

	private final PlotInfoProvider plotInfoProvider;
	
	private final SuppressionHandler suppressionHandler;
	
	public ScatterPlotVisualisationTool(VisToolParams params) { 
	    //SelectedValueStore svs, PlotInfoProvider pip, Supplier<TraitColorProvider> cpf, SuppressionHandler suppressionHandler) {
		super(params.selectedValueStore, params.traitColorProviderSupplier);
		this.plotInfoProvider = params.plotInfoProvider;
		this.suppressionHandler = params.suppressionHandler;
	}

	@Override
	public String getToolButtonName() {
			return Msg.TOOLNAME_SCATTER_PLOT();
	}

	@Override
	public Icon getToolIcon() {		
		return KDClientUtils.getIcon(ImageId.GRAPH_XY);
	}

	@Override
	public String getToolName() {
		return Msg.TOOLNAME_SCATTER_PLOT();
	}

	@Override
    public boolean supportsXandYaxes() {
        return true;
    }

	@Override
	protected AbstractVisToolPanel createVisToolPanel(VisToolData data) 
	{
		List<String> seriesNames = new ArrayList<String>();
		
		String plotTitle = buildPlotTitle(data.context.getTrial(), data.traitInstances, seriesNames);
			
		ScatterPlotPanel plotPanel = new ScatterPlotPanel(
				plotInfoProvider,
				getVisualisationToolId(),
				selectedValueStore,
				plotTitle, 
				data,
				colorProviderFactory,
				suppressionHandler);
		
		plotPanel.setPlotSpecimensToWatch(data.plotSpecimensToGraph);

		return plotPanel;
	}
	
	private int minDataSets = 2;
	private int maxDataSets = 0;


	@Override
	public int[] getDataRequirements() {
		return new int[]{minDataSets,maxDataSets};
	};

	
	@Override
	public List<Action> createActionsFor(CurationContext context,
			List<PlotOrSpecimen> plotSpecimens, // Not supported for ScatterPlot
			List<TraitInstance> checkedInstances,
			TraitInstance activeInstance,
			List<TraitInstance> selectedInstances) 
	{		
        List<Action> result = new ArrayList<>();
        
		List<TraitInstance> allNumericInstances = collectNumericInstances(context, ALLOW_CATEGORICAL, selectedInstances);

		boolean subPlotOk = VisToolUtil.allowSubplotTraits;
        List<TraitInstance> check = allNumericInstances;
        if (! VisToolUtil.allowSubplotTraits) {
            allNumericInstances = check.stream().filter(ti -> TraitLevel.PLOT == ti.trait.getTraitLevel()).collect(Collectors.toList());
        }

        final List<TraitInstance> numericInstances = allNumericInstances;
        
        boolean addedDisableAction = false;
        if (check.size() != numericInstances.size()) {
            addDisabledAction(result);
            addedDisableAction = true;
        }
		
		TraitNameStyle traitNameStyle = context.getTrial().getTraitNameStyle();

		if (activeInstance == null) {
			
			int nTraitInstances = numericInstances.size();
			if (nTraitInstances < 2) {
			    if (! addedDisableAction) {
    				Action action = new AbstractAction(Msg.ACTION_AT_LEAST_2_TRAIT_INSTANCES()) {
    					@Override
    					public void actionPerformed(ActionEvent e) {}
    				};
    //				action.setEnabled(false);
    				result.add(action);
			    }
			}
			else {
			    final List<JFrame> frames = new ArrayList<>();
			    doVariantPairs(context, numericInstances, traitNameStyle, frames, result);
//				doVariantsXwithMultipleY(context, numericInstances, traitNameStyle, frames, result);
				
				if (result.size() > 1) {
					final List<Action> allActions = new ArrayList<>(result);
					Action all = new AbstractAction(Msg.ACTION_ALL_N_SCATTER_PLOTS(result.size())) {
						@Override
						public void actionPerformed(ActionEvent e) {
							for (Action a : allActions) {
								a.actionPerformed(e);
							}
							VisToolUtil.staggerOpenedFrames(frames);
						}
						
					};
					result.add(all);
				}
			}
		}
		else {
		    if (! subPlotOk && TraitLevel.PLOT != activeInstance.trait.getTraitLevel()) {
		        // Already added?
		        if (! addedDisableAction) {
		            addDisabledAction(result);
		        }
		    }
		    else {
	            // there *IS* an activeInstance.
	            // So we "prefer" only offer it as the X 
	            // or as the singular Y (with the others as X)
	            //
	            // For Traits T1, T2, T3:
	            // Offer   X: Active, Y: T1
	            //                    ...
	            //         X: Active, Y: T1, T2, T3
	            //
	            //         X: T1, Y: Active
	            //                    ...
	            addPairsForActiveInstance(context, activeInstance, numericInstances, result);
		    }
		}
		
		return result;
	}

    public void addDisabledAction(List<Action> result) {
        String disabledForSubPlotName = Msg.ACTION_VISTOOL_DISABLED_FOR_LEVEL(getToolName(), TraitLevel.SPECIMEN.visible);
        Action action = new AbstractAction(disabledForSubPlotName) {
            @Override
            public void actionPerformed(ActionEvent e) {}
        };
        action.setEnabled(false);
        
        result.add(action);
    }

    public void addPairsForActiveInstance(CurationContext context, TraitInstance activeInstance,
            final List<TraitInstance> numericInstances, List<Action> result) {
        List<TraitInstance> yInstances = new ArrayList<>(numericInstances);
        yInstances.remove(activeInstance);
        
        if (yInstances.isEmpty()) {
        	Action action = new AbstractAction(Msg.ACTION_AT_LEAST_2_TRAIT_INSTANCES()) {
        		@Override
        		public void actionPerformed(ActionEvent e) {}
        	};
//				action.setEnabled(false);
        	result.add(action);
        }
        else {
        	List<JFrame> frames = new ArrayList<>();
        	
        	String activeName = context.makeTraitInstanceName(activeInstance);

        	for (TraitInstance yInstance : yInstances) {
        	    String yName = context.makeTraitInstanceName(yInstance);
        		String label = Msg.ACTION_X_AXIS_NAME_Y_AXIS_NAME(activeName, yName);
        		Action action = new AbstractAction(label) {
        			@Override
        			public void actionPerformed(ActionEvent e) {
        				VisToolData data = new VisToolData(context, 
        						null, 
        						Arrays.asList(activeInstance, yInstance));
        				frames.add(createVisToolWindow(context, data));
        			}
        		};
        		result.add(action);
        	}
        					
        	if (yInstances.size() > 1) {
        		final List<Action> allActions = new ArrayList<>(result);
        		Action all = new AbstractAction(Msg.ACTION_ALL_Y_WITH_X_AXIS_NAME(yInstances.size(), activeName)) {
        			@Override
        			public void actionPerformed(ActionEvent e) {
        				for (Action a : allActions) {
        					a.actionPerformed(e);
        				}
        				VisToolUtil.staggerOpenedFrames(frames);
        			}
        		};
        		result.add(all);

        		// Add a "divider"
        		result.add(new AbstractAction("-----") { //$NON-NLS-1$
        			@Override
        			public void actionPerformed(ActionEvent e) {}
        		});
        		result.get(result.size() - 1).setEnabled(false);
        	}
        	
        	// Now for active as the Y, 
        	for (TraitInstance ti : yInstances) {
        	    String xName = context.makeTraitInstanceName(ti);
        	    
        		String label = Msg.ACTION_X_AXIS_NAME_Y_AXIS_NAME(xName, activeName);
        		Action action = new AbstractAction(label) {
        			@Override
        			public void actionPerformed(ActionEvent e) {
        				VisToolData data = new VisToolData(context, null, 
        						Arrays.asList(ti, activeInstance));
        				frames.add(createVisToolWindow(context, data));
        			}
        		};
        		result.add(action);
        	}
        }
    }
	
    private void doVariantPairs(CurationContext context,
            final List<TraitInstance> traitInstances,
            TraitNameStyle traitNameStyle, 
            List<JFrame> frames,
            List<Action> result)
    {
//        int nTraitInstances = traitInstances.size();
        // For Traits: T1, T2, T3
        // Offer to make:
        //  X: T1    Y: T2
        //  X: T1    Y: T3
        //
        //  X: T2    Y: T1
        //  X: T2    Y: T3
        
        //  X: T3    Y: T1
        //  X: T3    Y: T2

        for (TraitInstance xInstance : traitInstances) {
            for (TraitInstance yInstance : traitInstances) {
                if (xInstance != yInstance) {
                    String xName = context.makeTraitInstanceName(xInstance);
                    String yName = context.makeTraitInstanceName(yInstance);
                    String label;
                    label = Msg.ACTION_X_AXIS_NAME_Y_AXIS_NAME(xName, yName);
                    Action action = new AbstractAction(label) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            VisToolData data = new VisToolData(context, null, Arrays.asList(xInstance, yInstance));
                            frames.add(createVisToolWindow(context, data));
                        }
                    };
                    result.add(action);
                }
            }
        }
    }

    private void doVariantsXwithMultipleY(CurationContext context,
            final List<TraitInstance> numericInstances,
            TraitNameStyle traitNameStyle,
            List<JFrame> frames,
            List<Action> result)
    {
        int nTraitInstances = numericInstances.size();
        // For Traits: T1, T2, T3
        // Offer to make:
        //  X: T1    Y: T2, T3
        //  X: T2    Y: T1, T3
        //  X: T3    Y: T1, T2
        // At least two exist...
        
        // Each variant list is:   X-instance, Y-instances
        List<List<TraitInstance>> scatterPlotList = new ArrayList<>();
        for (int index = 0; index < nTraitInstances; ++index) {
        	List<TraitInstance> variant = new ArrayList<TraitInstance>();
        	
        	variant.add(numericInstances.get(index));
        	for (int other = 0; other < nTraitInstances; ++other) {
        		if (other != index) {
        			variant.add(numericInstances.get(other));
        		}
        	}
        	
        	if  (variant.size() >= 2) {
                scatterPlotList.add(variant);
        	}
        }

        // Each variant list is:   X-instance, Y-instances
        for (final List<TraitInstance> scatterPlot : scatterPlotList) {
        	TraitInstance xInstance = scatterPlot.get(0);
        	String xName = context.makeTraitInstanceName(xInstance);
        	String label;
        	if (scatterPlot.size()==2) {
        		String yName = context.makeTraitInstanceName(scatterPlot.get(1));
        		label = Msg.ACTION_X_AXIS_NAME_Y_AXIS_NAME(xName, yName);
        	}
        	else {
        		label = Msg.ACTION_X_AXIS_NAME_Y_N_TRAITS(xName, scatterPlot.size() - 1);
        	}
        	Action action = new AbstractAction(label) {
        		@Override
        		public void actionPerformed(ActionEvent e) {
        			VisToolData data = new VisToolData(context, null, scatterPlot);
        			frames.add(createVisToolWindow(context, data));
        		}
        	};
        	result.add(action);
        }
    }
}

