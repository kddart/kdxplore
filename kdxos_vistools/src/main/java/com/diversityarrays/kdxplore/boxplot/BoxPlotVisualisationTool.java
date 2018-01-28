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
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.vistool.AbstractVisToolPanel;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.SimpleVisualisationTool;
import com.diversityarrays.kdxplore.vistool.VisToolData;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.kdxplore.vistool.VisualisationToolService.VisToolParams;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

public class BoxPlotVisualisationTool extends SimpleVisualisationTool {

	private final PlotInfoProvider plotInfoProvider;
	
	private final SuppressionHandler suppressionHandler;
	
	public BoxPlotVisualisationTool(VisToolParams params) {
		super(params.selectedValueStore, params.traitColorProviderSupplier);
		this.plotInfoProvider = params.plotInfoProvider;	
		this.suppressionHandler = params.suppressionHandler;
	}

	@Override
	protected AbstractVisToolPanel createVisToolPanel(VisToolData data) 
	{
		List<String> seriesNames = new ArrayList<String>();
		
		String plotTitle = buildPlotTitle(data.context.getTrial(), data.traitInstances, seriesNames);
		
		BoxPlotPanel plotPanel = new BoxPlotPanel(plotInfoProvider,
				getVisualisationToolId(),
				selectedValueStore,
				plotTitle, 
				data,
				colorProviderFactory,
				suppressionHandler);
		
		plotPanel.setPlotSpecimensToWatch(data.plotSpecimensToGraph);
		
		return plotPanel;
	}
	
	@Override
	public Either<String,List<JFrame>> getVisualisationDialogsUsingPlots(
			CurationContext context, 
			List<TraitInstance> selectedInstances, 
			List<PlotOrSpecimen> plotSpecimensToGraph)
	{
		VisToolData data = new VisToolData(context, plotSpecimensToGraph, selectedInstances);
		return getVisualisationDialogInternal(context, data);
	}
	
	@Override
	public List<Action> createActionsFor(
			CurationContext context,
			List<PlotOrSpecimen> plotSpecimens,
			List<TraitInstance> checkedInstances,
			TraitInstance activeInstance,
			List<TraitInstance> selectedInstances) 
	{
		List<Action> actions = new ArrayList<>();

		List<TraitInstance> allNumericInstances = collectNumericInstances(
		        context, NO_CATEGORICAL, selectedInstances);
		// Note: ignore activeInstance as that will either be one of the selectedInstances
		//       or, if it isn't a "numeric" then we won't be offering it.
		
        List<TraitInstance> check = allNumericInstances;
        if (! VisToolUtil.allowSubplotTraits) {
            allNumericInstances = check.stream().filter(ti -> TraitLevel.PLOT == ti.trait.getTraitLevel()).collect(Collectors.toList());
        }

        final List<TraitInstance> numericInstances = allNumericInstances;
        
		boolean doForPlots = ! Check.isEmpty(plotSpecimens);
		
        if (check.size() != numericInstances.size()) {
            String disabledForSubPlotName = Msg.ACTION_VISTOOL_DISABLED_FOR_LEVEL(getToolName(), TraitLevel.SPECIMEN.visible);
            Action action = new AbstractAction(disabledForSubPlotName) {
                @Override
                public void actionPerformed(ActionEvent e) {}
            };
//            action.setEnabled(false);
            actions.add(action);
        }

		for (TraitInstance ti : numericInstances) {
			String name = context.makeTraitInstanceName(ti);
			String name_0 = doForPlots 
			        ? Msg.ACTION_ALL_PLOTS_FOR_NAME(name) 
			        : name;
			actions.add(new AbstractAction(name_0) {
				@Override
				public void actionPerformed(ActionEvent e) {
					VisToolData data = new VisToolData(context, null, Arrays.asList(ti));
					createVisToolWindow(context, data);
				}
			});
			if (doForPlots) {
				actions.add(new AbstractAction(Msg.ACTION_N_PLOTS_FOR_NAME(name, plotSpecimens.size())) {
					@Override
					public void actionPerformed(ActionEvent e) {
						VisToolData data = new VisToolData(context, plotSpecimens, Arrays.asList(ti));
						createVisToolWindow(context, data);
					}
				});
			}
		}
		
		if (numericInstances.size() > 1) {
			String label = Msg.ACTION_ONE_CHART_WITH_N_TRAITS(numericInstances.size());
			Action action = new AbstractAction(label) {
				@Override
				public void actionPerformed(ActionEvent e) {
					VisToolData data = new VisToolData(context, null, numericInstances);
					createVisToolWindow(context, data);
				}
			};
			actions.add(action);
			
			Action allAction = new AbstractAction(Msg.ACTION_EACH_TRAIT_IN_OWN_CHART()) {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<JFrame> frames = new ArrayList<>();
					for (TraitInstance ti : numericInstances) {
						VisToolData data = new VisToolData(context, null, Arrays.asList(ti));
						JFrame frame = createVisToolWindow(context, data);
						frames.add(frame);
					}
					VisToolUtil.staggerOpenedFrames(frames);
				}
				
			};
			actions.add(allAction);
		}
		
//		Set<TraitInstance> set = new HashSet<>(numericSelectedInstances);
//		set.removeAll(numericCheckedInstances);
//			
//		if (! set.isEmpty() && numericSelectedInstances.size() > 1) {
//			String label = Msg.ACTION_ONE_CHART_WITH_N_SELECTED_TRAITS(numericSelectedInstances.size());
//			actions.add(new AbstractAction(label) {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					VisToolData data = new VisToolData(context, null, numericSelectedInstances);
//					createVisToolWindow(context, data);
//				}
//			});
//			
//			Action allAction = new AbstractAction(Msg.ACTION_EACH_SELECTED_TRAIT_IN_OWN_CHART()) {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					List<JFrame> frames = new ArrayList<>();
//					for (TraitInstance ti : numericSelectedInstances) {
//						VisToolData data = new VisToolData(context, null, Arrays.asList(ti));
//						JFrame frame = createVisToolWindow(context, data);
//						frames.add(frame);
//					}
//					VisToolUtil.staggerOpenedFrames(frames);
//				}				
//			};
//			actions.add(allAction);
//		}

		if (actions.isEmpty()) {
			Action action = new AbstractAction(Msg.ACTION_NO_DATA_TO_PLOT()) {
				@Override
				public void actionPerformed(ActionEvent e) {}
			};
//			action.setEnabled(false);
			actions.add(action);
		}

		return actions;
	}

	@Override
	public String getToolButtonName() {
		return Msg.TOOLNAME_BOX_PLOT();
	}

	@Override
	public Icon getToolIcon() {
		return KDClientUtils.getIcon(ImageId.GRAPH_BOXPLOT);
	}

	@Override
	public String getToolName() {		
		return Msg.TOOLNAME_BOX_PLOT();
	}

   @Override
    public boolean supportsXandYaxes() {
        return false;
    }	    

	private int minDataSets = 1;
	private int maxDataSets = 0;
	
	@Override
	public int[] getDataRequirements() {
		return new int[]{minDataSets,maxDataSets};
	}

}
