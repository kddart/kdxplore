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
package com.diversityarrays.kdxplore.vistool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.event.EventListenerList;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.util.Check;

abstract public class AbstractVisualisationTool implements VisualisationTool {

	protected static final Comparator<TraitInstance> TRAIT_INSTANCE_COMPARATOR = new Comparator<TraitInstance>() {
			@Override
			public int compare(TraitInstance o1, TraitInstance o2) {
				int diff = o1.getTraitName().compareTo(o2.getTraitName());
				if (diff == 0) {
					diff = Integer.compare(o1.getInstanceNumber(), o2.getInstanceNumber());
				}
				return diff;
			}
		};

	protected final SelectedValueStore selectedValueStore;
	
	private final VisualisationToolId<?> uniqueId;
	
	protected final EventListenerList listenerList = new EventListenerList();

	protected final Bag<String> chartCountByTrialName = new HashBag<>();
	
	public AbstractVisualisationTool(SelectedValueStore svs) {
		this.uniqueId = new VisualisationToolId<>(this.getClass());
		this.selectedValueStore = svs;
	}
	
//	protected void initialiseDataSetsAndTraitInstances(CurationContext context, 
//			List<TraitInstance> selectedInstances, 
//			List<Plot> plotsToGraph,
//			List<TraitInstance> traitInstances,
//			List<List<Comparable<?>>> dataSets) 
//	{
//		Date trialPlantingDate = context.getTrial().getTrialPlantingDate();
//
//		for (TraitInstance ti : selectedInstances) {
//			
//			TraitValidationProcessor<?> tvp = TraitHelper.createTraitValidationProcessor(ti.trait, trialPlantingDate);
//			if (tvp instanceof NumericTraitValidationProcessor) {
//				
//				traitInstances.add(ti);
//
//				if (plotsToGraph != null) {
//					List<Comparable<?>> sampleValues = context.getSampleValuesForPlots(ti, plotsToGraph);
//					dataSets.add(sampleValues);
//				}
//				else {				
//					List<Comparable<?>> sampleValues = context.getSampleValues(ti);
//					dataSets.add(sampleValues);
//				}
//			}
//		}
//	}

	@Override
	final public VisualisationToolId<?> getVisualisationToolId() {
		return uniqueId;
	}

	@Override
	public void addVisToolListener(VisToolListener l) {
		listenerList.add(VisToolListener.class, l);
	}
	
	@Override
	public void removeVisToolListener(VisToolListener l) {
		listenerList.remove(VisToolListener.class, l);
	}
	
	protected void fireVisToolPanelCreated(JFrame frame, VisToolPanel visToolPanel) {
		for (VisToolListener vtl : listenerList.getListeners(VisToolListener.class)) {
			vtl.visToolPanelCreated(this, frame, visToolPanel);
		}
	}
	
	protected void fireVisToolPanelClosed(JFrame frame, VisToolPanel visToolPanel) {
		for (VisToolListener vtl : listenerList.getListeners(VisToolListener.class)) {
			vtl.visToolPanelClosed(this, frame, visToolPanel);
		}
	}

	protected String buildPlotTitle(Trial trial, List<TraitInstance> traitInstances, List<String> seriesNames) {
		TraitNameStyle traitNameStyle = trial.getTraitNameStyle();
		StringBuilder sb = new StringBuilder();
		sb.append(Msg.PLOT_TITLE_TOOLNAME_OF(getToolName()));
		String sep = ""; //$NON-NLS-1$
		for (TraitInstance ti : traitInstances) {
			String traitInstanceName = traitNameStyle.makeTraitInstanceName(ti);

			seriesNames.add(traitInstanceName);
			sb.append(sep).append(traitInstanceName);
			sep = ", "; //$NON-NLS-1$
		}
	
		String trialName = trial.getTrialAcronym();
		if (Check.isEmpty(trialName)) {
			trialName = trial.getTrialName();
		}
		chartCountByTrialName.add(trialName);
		int chartCount = chartCountByTrialName.getCount(trialName);
		
		sb.append(" : ").append(trialName) //$NON-NLS-1$
			.append("(").append(chartCount).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		
		String plotTitle = sb.toString();
		return plotTitle;
	}

    static protected boolean ALLOW_CATEGORICAL = true;
    static protected boolean NO_CATEGORICAL = false;
	
	protected List<TraitInstance> collectNumericInstances(CurationContext context, boolean allowCategorical, List<TraitInstance> input) {
		List<TraitInstance> result = new ArrayList<>();
		
		if (input != null) {
			Map<TraitInstance, SimpleStatistics<?>> statsByTraitInstance = context.getStatsByTraitInstance();
			
			for (TraitInstance ti : input) {
				SimpleStatistics<?> statistics = statsByTraitInstance.get(ti);
				
				if (statistics==null || statistics.getValidCount() <= 0) {
					continue;
				}
				
				switch (ti.trait.getTraitDataType()) {
				case CALC:
				case INTEGER:
				case DECIMAL:
				case ELAPSED_DAYS:
					result.add(ti);
					break;
					
				case CATEGORICAL:
				    if (allowCategorical) {
	                    result.add(ti);
				    }
				    break;
				case DATE:
				    result.add(ti);
                    break;

				case TEXT:
				default:
					break;
				}
			}
		}
		return result;
	}

}
