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

import java.util.Collection;
import java.util.Iterator;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;

public class PlotAttributeValuesIterator implements Iterator<String> {

	private Iterator<Plot> plotIterator;
	private Plot plot;
	private final String attributeName;
	private String nextAttributeValue;

	public PlotAttributeValuesIterator(String attrName, Collection<Plot> plots) {
		this.attributeName = attrName;
		
		plotIterator = plots.iterator();
		initialiseNext();
	}

	public void initialiseNext() {
		while (plotIterator.hasNext()) {
			plot = plotIterator.next();
			for (PlotAttributeValue pa : plot.plotAttributeValues) {
				if (attributeName.equals(pa.plotAttributeName)) {
					nextAttributeValue = pa.getAttributeValue();
					break;
				}
			}
			if (nextAttributeValue != null) {
				break;
			}
		}
	}

	@Override
	public boolean hasNext() {
		return nextAttributeValue != null;
	}

	@Override
	public String next() {
		String result = nextAttributeValue;
		initialiseNext();
		return result;
	}

}
