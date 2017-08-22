/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.
    
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

import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFrame;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.util.Either;

public interface VisualisationTool {

	/**
	 * Each tool has a unique id we can use to identify it
	 * @return
	 */
	VisualisationToolId<?> getVisualisationToolId();
	
	String getToolName();
	String getToolButtonName();
	Icon getToolIcon();
	
	/**
	 * Return null or empty if the tool does its own dataSet prompting.
	 * In that situation, getVisualisationDialog()
	 * @return
	 */
	int[] getDataRequirements();

	/**
	 * 
	 * @param context
	 * @param selectedInstances null if getDataRequirements() returns null
	 * @return String error or the created JFrames
	 */
	Either<String,List<JFrame>> getVisualisationDialogs(CurationContext context, List<TraitInstance> selectedInstances);
	
//	void addSelectionChangeListener(SelectionChangeListener l);
//	void removeSelectionChangeListener(SelectionChangeListener l);

//	void showSelection(Boolean show);

	/**
	 * @param context
	 * @param selectedInstances
	 * @param plotsToGraph
	 * @return
	 */
	Either<String,List<JFrame>> getVisualisationDialogsUsingPlots(CurationContext context,
			List<TraitInstance> selectedInstances, List<PlotOrSpecimen> plotSpecimensToGraph);
	
	void addVisToolListener(VisToolListener l);
	void removeVisToolListener(VisToolListener l);
	

	/**
	 * Each tool can decide if it will provide "quick pick" actions appropriate for the selection of TraitInstances.
	 * @param chosen
	 * @return null or an array of Actions that will create an instance of the tool.
	 */
	List<Action> createActionsFor(
			CurationContext context, 
			List<PlotOrSpecimen> plotSpecimens,
			List<TraitInstance> checkedInstances,
			TraitInstance activeInstance,
			List<TraitInstance> selectedInstances);

    boolean supportsXandYaxes();

}
