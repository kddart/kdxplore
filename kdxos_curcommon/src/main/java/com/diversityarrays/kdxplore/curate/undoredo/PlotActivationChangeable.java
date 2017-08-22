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
package com.diversityarrays.kdxplore.curate.undoredo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.util.RunMode;

public class PlotActivationChangeable implements Changeable<PlotAndSampleChanger> {
	
	private final boolean plotsWereActivated;
	public final Date date = new Date();

	private final Map<Plot,Date> previousDeactivationDateByPlot = new HashMap<>();
	
	public PlotActivationChangeable(boolean activated) {
		this.plotsWereActivated = activated;
	}
	
	public Collection<Plot> getPlots() {
		return previousDeactivationDateByPlot.keySet();
	}
	
	public void addPlot(Plot p) {
		previousDeactivationDateByPlot.put(p, p.getWhenDeactivated());
		p.setWhenDeactivated(plotsWereActivated ? null : date);
	}

	@Override
	public Object getOldValue() {
		StringBuilder sb = new StringBuilder();
		sb.append(plotsWereActivated ? "Activated" : "Deactivated");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		sb.append(" on ").append(df.format(date));
		sb.append(":");

		boolean dev = RunMode.getRunMode().isDeveloper();

		for (Plot p : previousDeactivationDateByPlot.keySet()) {
			sb.append(" ").append(p.getPlotId());
			if (dev) {
				sb.append("@").append(p.getPlotColumn()).append(',').append(p.getPlotRow());
			}
		}
		return sb.toString();
	}

	@Override
	public Object getNewValue() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		if (plotsWereActivated) {
			return previousDeactivationDateByPlot.size() + " Plots Activated on " + df.format(date);
		}
		return previousDeactivationDateByPlot.size() + " Plots Deactivated on " + df.format(date);
	}

	@Override
	public void redo(PlotAndSampleChanger pasm) throws Exception {
		if (plotsWereActivated) {
			// Redo the activation
			pasm.redoPlotActivation(previousDeactivationDateByPlot.keySet());
		}
		else {
			// Redo the deactivation
			pasm.redoPlotDeactivation(previousDeactivationDateByPlot.keySet(), date);
		}
	}

	@Override
	public void undo(PlotAndSampleChanger pasm) throws Exception {
		pasm.undoPlotDeactivation(previousDeactivationDateByPlot);
	}

	@Override
	public String getInfo() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		StringBuilder sb = new StringBuilder();
		sb.append(plotsWereActivated ? "Activated" : "Deactivated");
		sb.append(" on ").append(df.format(date));
		sb.append(":");
		for (Plot p : previousDeactivationDateByPlot.keySet()) {
			sb.append(" ").append(p.getPlotId());
		}
		return sb.toString();
	}
	
}
