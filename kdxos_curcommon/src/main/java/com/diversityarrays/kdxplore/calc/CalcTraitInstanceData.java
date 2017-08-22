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
package com.diversityarrays.kdxplore.calc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class CalcTraitInstanceData {
	
	public final TraitInstance traitInstance;
	public final String name;
	public final Map<Integer,KdxSample> sampleByPlotId = new HashMap<>();

	public CalcTraitInstanceData(TraitInstance ti, String tiName) {
		this.traitInstance = ti;
		this.name = tiName;
	}
	
	public void refreshData(CalcContextDataProvider ccdp) {
		sampleByPlotId.clear();
		for (KdxSample s : ccdp.getSampleMeasurements(traitInstance)) {
			sampleByPlotId.put(s.getPlotId(), s);
		}
	}
	
	public KdxSample getSample(Integer plotId) {
		return sampleByPlotId.get(plotId);
	}

	public void addPlotIdsTo(Set<Integer> plotIds) {
		plotIds.addAll(sampleByPlotId.keySet());
	}

	@Override
	public String toString() {
		return name + " (" + sampleByPlotId.size() + " samples)";
	}
	
}
