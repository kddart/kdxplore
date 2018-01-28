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

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;

public class HeatMapPanelParameters {
	
	public final String xAxisPositionName;
	public final String yAxisPositionName;
	public final TraitInstance traitInstance;

	private final PlotInfoProvider plotInfoProvider;
	
	HeatMapPanelParameters(String x, String y, TraitInstance ti, PlotInfoProvider pip) {
		this.xAxisPositionName = x;
		this.yAxisPositionName = y;
		this.traitInstance = ti;
		plotInfoProvider = pip;
	}
	
	public Plot getPlot(int plotId) {
		return plotInfoProvider.getPlotByPlotId(plotId);
	}
	
	@Override
	public int hashCode() {
		return xAxisPositionName.hashCode() * 11 + yAxisPositionName.hashCode() + 3 + traitInstance.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (! (o instanceof HeatMapPanelParameters)) return false;
		HeatMapPanelParameters other = (HeatMapPanelParameters) o;
		
		return (this.xAxisPositionName.equals(other.xAxisPositionName))
				&&
			(this.yAxisPositionName.equals(other.yAxisPositionName))
				&&
			this.traitInstance.equals(other.traitInstance);
	}
}
