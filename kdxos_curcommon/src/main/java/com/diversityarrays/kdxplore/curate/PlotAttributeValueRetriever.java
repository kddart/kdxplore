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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;

public class PlotAttributeValueRetriever implements ValueRetriever<String> {
	
	private final String attributeName;
	private final PlotAttribute plotAttribute;
	
	private final String[] plotAttributeValues;
	
	public PlotAttributeValueRetriever(PlotAttribute pa, Set<String> values) {
		this.plotAttribute = pa;
		this.attributeName = plotAttribute.getPlotAttributeName();
		List<String> list = new ArrayList<>(values);
		Collections.sort(list);
		plotAttributeValues = list.toArray(new String[list.size()]);
	}
	
	@Override
	public int hashCode() {
	    return plotAttribute.hashCode();
	}
	
	@Override
	public String toString() {
	    return "PlotAttrVR[" + attributeName + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	// We need this because of the code in {@link com.diversityarrays.kdxplore.curate.CurationTableModel#initPlotAttributes()}
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (! (o instanceof PlotAttributeValueRetriever)) return false;
		PlotAttributeValueRetriever other = (PlotAttributeValueRetriever) o;
		return this.attributeName.equals(other.attributeName);
	}
	
	@Override
    public ValueType getValueType() {
	    return ValueType.PLOT_ATTRIBUTE;
	}
	
    @Override
    public boolean isPlotColumn() {
        return false;
    }

    @Override
    public boolean isPlotRow() {
        return false;
    }

	public String getAttributeName() {
		return attributeName;
	}
	
	public PlotAttribute getPlotAttribute() {
		return plotAttribute;
	}

	@Override
	public String getDisplayName() {
		return attributeName;
	}

	@Override
	public Class<String> getValueClass() {
		return String.class;
	}
	
	@Override
	public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
		Integer result = null;
		String v = getAttributeValue(infoProvider, pos, null);
		if (v != null) {
			result = Arrays.binarySearch(plotAttributeValues, v);
		}
		return result;
	}
	
	@Override
	public int getAxisZeroValue() {
		return 0; // zero is zero!
	}
	
	@Override
	public int getAxisValueCount() {
	    return plotAttributeValues.length;
	}
	
	@Override
	public com.diversityarrays.kdxplore.curate.ValueRetriever.TrialCoord getTrialCoord() {
		return TrialCoord.NONE;
	}

	@Override
	public String getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, String valueIfNull) {
		String v = infoProvider.getPlotAttributeValue(pos.getPlotId(), attributeName);
		return v==null ? valueIfNull : v;
	}

}
