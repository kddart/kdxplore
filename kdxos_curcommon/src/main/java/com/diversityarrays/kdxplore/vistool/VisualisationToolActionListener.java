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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.data.TraitHelper;
import com.diversityarrays.kdxplore.vistool.AskForTraitInstances;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.ui.GuiUtil;

public class VisualisationToolActionListener implements ActionListener {
	
	static public void doDisabledForTesting(Component parent, String title, String toolTitle) {
		GuiUtil.infoMessage(parent, "Sorry, "+toolTitle+"\nis temporarily disabled", title);
	}
		
	private final JComponent parentComponent;
	private final String title;
	private final boolean onlyForDeveloper;
	private final CurationContext curationContext;
	private final VisualisationTool tool;

	private Closure<List<TraitInstance>> onInstancesChosen = new Closure<List<TraitInstance>>() {
		@Override
		public void execute(List<TraitInstance> instances) {

			Either<String,List<JFrame>> either = tool.getVisualisationDialogs(curationContext, instances);

			if (either.isRight()) {
				VisToolUtil.staggerOpenedFrames(either.right());
			}
			else {
				curationContext.errorMessage(tool, either.left());
			}			
		}
	};
	
	public VisualisationToolActionListener(JComponent parentComponent, 
			String title,
			boolean onlyForDeveloper,
			CurationContext curationContext,
			VisualisationTool tool) 
	{
		this.parentComponent = parentComponent;
		this.title = title;
		this.onlyForDeveloper = onlyForDeveloper;
		this.curationContext = curationContext;
		this.tool = tool;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
	    boolean developerRunMode = RunMode.getRunMode().isDeveloper();
		if (onlyForDeveloper) {
			if (! developerRunMode) {
				doDisabledForTesting(parentComponent, title, tool.getToolName());
				return;
			}
		}
		
        boolean allowSubPlots = developerRunMode ||
                (0 != (ActionEvent.SHIFT_MASK & e.getModifiers()));

		int[] dataSets = tool.getDataRequirements();
		if (dataSets == null || dataSets.length <= 0) {
		    VisToolUtil.allowSubplotTraits = allowSubPlots;
		    try {
    			Either<String,List<JFrame>> either = tool.getVisualisationDialogs(curationContext, null);
    			if (either.isRight()) {
    				VisToolUtil.staggerOpenedFrames(either.right());
    			}
    			else {
    				curationContext.errorMessage(tool, either.left());
    			}
		    }
		    finally {
		        
		    }
		}
		else {
			boolean xAndYaxes = tool.supportsXandYaxes();
			askForManyTraitInstancesWithData(parentComponent, 
					tool.getToolName(), 
					xAndYaxes,
					dataSets, 
					allowSubPlots,
					onInstancesChosen);
		}
	}
	
	private void askForManyTraitInstancesWithData(
			JComponent parentComponent,
			String dialogTitle, 
			boolean xAndYaxes,
			int[] minMaxDataSets, 
			boolean shiftDown, 
			Closure<List<TraitInstance>> onInstancesChosen) 
	{
	    boolean allowSubPlot = shiftDown || RunMode.getRunMode().isDeveloper();
	    
		Map<TraitInstance,SimpleStatistics<?>> statsByTraitInstance = new TreeMap<>(TraitHelper.COMPARATOR);
		
		boolean anyTraitInstances = false;
		for (Map.Entry<TraitInstance, SimpleStatistics<?>> me : curationContext.getStatsByTraitInstance().entrySet()) {
		    anyTraitInstances = true;

			TraitInstance ti = me.getKey();
			if (allowSubPlot || TraitLevel.PLOT == ti.trait.getTraitLevel()) {
	            SimpleStatistics<?> ss = me.getValue();
	            if (ss.getValidCount() > 0) {
	                statsByTraitInstance.put(ti, ss);
	            }
			}
		}

		String msg;
		switch (statsByTraitInstance.size()) {
		case 0:
		    if (! anyTraitInstances || allowSubPlot) {
	                msg = "No Trait Instances appear to have valid data.";
		    } 
		    else {
		        msg = "SubPlot visualisation is not yet available";
		    }
		    MsgBox.warn(parentComponent, msg, title);
			return;

		case 1:
			MsgBox.warn(parentComponent, 
					"Only 1 Trait Instance has valid data.",
					title);
			return;

		default:
			break;
		}
		
		Window owner = GuiUtil.getOwnerWindow(parentComponent);
		
		AskForTraitInstances dlg = new AskForTraitInstances(
				owner, 
				dialogTitle, 
				xAndYaxes,
				"Continue", 
				curationContext.getTrial().getTraitNameStyle(),
				statsByTraitInstance, 
				minMaxDataSets, 
				onInstancesChosen);
		dlg.setVisible(true);
	}


}
