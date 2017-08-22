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
package com.diversityarrays.kdxplore.curate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;

public class PlotTypeValueRetriever implements ValueRetriever<String> {

	private final List<String> plotTypes;
	public PlotTypeValueRetriever(Set<String> allPlotTypes) {
		plotTypes = new ArrayList<>(allPlotTypes);
		Collections.sort(plotTypes);
	}
	
	@Override
	public int hashCode() {
	    return plotTypes.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
	    if (this==o) return true;
	    if (! (o instanceof PlotTypeValueRetriever)) return false;
	    PlotTypeValueRetriever other = (PlotTypeValueRetriever) o;
	    return this.plotTypes.equals(other.plotTypes);
	}
	
	@Override
	public String toString() {
	    return "PlotTypeVR"; //$NON-NLS-1$
	}
	
	@Override
    public ValueType getValueType() {
	    return ValueType.PLOT_TYPE;
	}

	@Override
    public boolean isPlotColumn() {
        return false;
    }

    @Override
    public boolean isPlotRow() {
        return false;
    }


	@Override
	public String getDisplayName() {
		return "Plot Type";
	}

	@Override
	public com.diversityarrays.kdxplore.curate.ValueRetriever.TrialCoord getTrialCoord() {
		return TrialCoord.NONE;
	}

	@Override
	public Class<String> getValueClass() {
		return String.class;
	}

	@Override
	public String getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, String valueIfNull) {
		Plot plot = infoProvider.getPlotByPlotId(pos.getPlotId());
		return plot==null ? null : plot.getPlotType();
	}

	@Override
	public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
		Integer result = null;
		String value = getAttributeValue(infoProvider, pos, null);
		if (value != null) {
			int idx = plotTypes.indexOf(value);
			if (idx >= 0) {
				result = idx;
			}
		}
		return result;
	}

	@Override
	public int getAxisZeroValue() {
		return 0;
	}

	@Override
	public int getAxisValueCount() {
	    return plotTypes.size();
	}
}
