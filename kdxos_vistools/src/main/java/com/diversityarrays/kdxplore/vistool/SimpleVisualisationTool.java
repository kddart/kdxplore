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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.JFrame;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.TraitColorProvider;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.util.Either;

/**
 * Superclass for VisualisationTools that can use the standard
 * createVisToolWindow() and/or getVisualisationDialogInternal().
 * <p>
 * Current examples are ScatterPlot and BoxPlot but others may come along.
 * The HeatMapVisualisationTool is a more complex animal (for now).
 * @author brianp
 */
public abstract class SimpleVisualisationTool extends AbstractVisualisationTool {
	
	protected final Supplier<TraitColorProvider> colorProviderFactory;

	public SimpleVisualisationTool(SelectedValueStore svs, Supplier<TraitColorProvider> colorProviderFactory) {
		super(svs);
		this.colorProviderFactory = colorProviderFactory;
	}

	abstract protected AbstractVisToolPanel createVisToolPanel(VisToolData data);
	
	protected JFrame createVisToolWindow(CurationContext context, VisToolData data) 
	{
		final AbstractVisToolPanel plotPanel = createVisToolPanel(data);	
		
		final JFrame frame = context.addVisualisationToolUI(plotPanel);
		frame.setTitle(plotPanel.getTitle());
		
		CurationDataChangeListener cdcl = new CurationDataChangeListener() {
			@Override
			public void plotActivationChanged(Object source, boolean activated, List<Plot> plots) {
				plotPanel.plotActivationsChanged(activated, plots);
			}
			
			@Override
			public void editedSamplesChanged(Object source, List<CurationCellId> curationCellIds) {
				plotPanel.editedSamplesChanged();
			}
		};
		context.getPlotInfoProvider().addCurationDataChangeListener(cdcl);
		
		fireVisToolPanelCreated(frame, plotPanel);

		frame.pack();		
//		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				frame.removeWindowListener(this);
				context.getPlotInfoProvider().removeCurationDataChangeListener(cdcl);

				fireVisToolPanelClosed(frame, plotPanel);				
			}
		});
		
		return frame;
	}
	
	protected String getTraitInstancesErrorMessage(Collection<TraitInstance> traitInstances) {
		if (traitInstances.isEmpty()) {
			return Msg.MSG_NO_INSTANCES_WITH_NUMERIC_DATA();
		}
		return null;
	}
	
	protected Either<String,List<JFrame>> getVisualisationDialogInternal(
			CurationContext context,
			VisToolData data)
	{
		String errmsg = getTraitInstancesErrorMessage(data.traitInstances);
		if (errmsg != null) {
			return Either.left(errmsg);
		}

		JFrame frame = createVisToolWindow(context, data);
		return Either.right(Arrays.asList(frame));
	}

	@Override
	public Either<String,List<JFrame>> getVisualisationDialogs(CurationContext context, List<TraitInstance> selectedInstances) {
		
		Map<TraitInstance, SimpleStatistics<?>> statsByTraitInstance = context.getStatsByTraitInstance();
	
		if (! VisToolUtil.allowSubplotTraits) {
		    Set<TraitInstance> set = new HashSet<>(statsByTraitInstance.keySet());
		    for (TraitInstance ti : set) {
		        if (TraitLevel.PLOT != ti.trait.getTraitLevel()) {
		            statsByTraitInstance.remove(ti);
		        }
		    }
		}
		
		if (statsByTraitInstance.isEmpty() || selectedInstances.isEmpty()) {
			return Either.left(Msg.MSG_NO_INPUT_DATA());
		}
	
		VisToolData data = new VisToolData(context, null, selectedInstances);
	
		return getVisualisationDialogInternal(context, data);
	}

	@Override
	public Either<String,List<JFrame>> getVisualisationDialogsUsingPlots(CurationContext context, List<TraitInstance> selectedInstances,
			List<PlotOrSpecimen> plotSpecimensToGraph)
	{
		VisToolData data = new VisToolData(context, plotSpecimensToGraph, selectedInstances);
	
		return getVisualisationDialogInternal(context, data);
	}

}
