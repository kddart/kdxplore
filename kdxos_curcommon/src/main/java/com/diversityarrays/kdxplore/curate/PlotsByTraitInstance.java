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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;

@SuppressWarnings("nls")
public class PlotsByTraitInstance {
	
	public static final PlotsByTraitInstance EMPTY = new PlotsByTraitInstance();

	private final Map<TraitInstance,Set<PlotOrSpecimen>> plotSpecimensByTraitInstance = new HashMap<>();
	
	public PlotsByTraitInstance() {
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("PlotsByTraitInstance:");
		for (TraitInstance ti : plotSpecimensByTraitInstance.keySet()) {
			sb.append("\n\t(");
			if (ti.trait==null) {
				sb.append("TR#").append(ti.getTraitId());
			}
			else {
				sb.append(ti.trait.getAliasOrName());
			}
			sb.append('_').append(ti.getInstanceNumber());
			sb.append(": ").append(plotSpecimensByTraitInstance.get(ti).size());
			sb.append(" )");
		}
		sb.append("\n- - - - -\n");
		return sb.toString(); // for debugging
	}

	public void collectPlotsInto(TraitInstance ti, List<PlotOrSpecimen> result) {
		Set<PlotOrSpecimen> set = plotSpecimensByTraitInstance.get(ti);
		if (set != null) {
			result.addAll(set);
		}
	}
	
	public Set<Plot> getPlots(TraitInstance ti, PlotInfoProvider plotInfoProvider) {
	    if (ti == null) {
	        return plotSpecimensByTraitInstance.values().stream()
	                .flatMap(set -> set.stream())
	                .map(pos -> plotInfoProvider.getPlotByPlotId(pos.getPlotId()))
	                .collect(Collectors.toSet());
	    }

	    return getPlotSpecimens(ti).stream()
            .map(pos -> plotInfoProvider.getPlotByPlotId(pos.getPlotId()))
            .collect(Collectors.toSet());
	}

	/**
	 * If null, return the union of all Plots for all TraitInstances.
	 * @param ti
	 * @return
	 */
	public Set<PlotOrSpecimen> getPlotSpecimens(TraitInstance ti) {
		Set<PlotOrSpecimen> result;
		if (ti == null) {
			result = new HashSet<>();
			for (Collection<PlotOrSpecimen> coll : plotSpecimensByTraitInstance.values()) {
				result.addAll(coll);
			}
		}
		else {
			result = plotSpecimensByTraitInstance.get(ti);
			if (result == null) {
				result = Collections.emptySet();
			}
			else {
				result = Collections.unmodifiableSet(result);
			}
		}
		return result;
	}

	public void addPlot(TraitInstance ti, PlotOrSpecimen plot) {
		Set<PlotOrSpecimen> set = plotSpecimensByTraitInstance.get(ti);
		if (set==null) {
			set = new HashSet<>();
			plotSpecimensByTraitInstance.put(ti, set);
		}
		set.add(plot);
	}

	public Set<TraitInstance> getTraitInstances() {
		return plotSpecimensByTraitInstance.keySet();
	}

	public void addPlots(TraitInstance ti, Collection<PlotOrSpecimen> plots) {
		Set<PlotOrSpecimen> set = plotSpecimensByTraitInstance.get(ti);
		if (set==null) {
			set = new HashSet<>();
			plotSpecimensByTraitInstance.put(ti, set);
		}
		set.addAll(plots);
	}
	

	public void addTraitInstances(PlotOrSpecimen plot, Iterable<TraitInstance> list) {
		for (TraitInstance ti : list) {
			addPlot(ti, plot);
		}
	}	

	public boolean isEmpty() {
		return plotSpecimensByTraitInstance.isEmpty();
	}

	public Collection<Set<PlotOrSpecimen>> getPlotSpecimenLists() {
		return plotSpecimensByTraitInstance.values();
	}

	public boolean contains(TraitInstance ti, Plot plot) {
		boolean result = false;
		Set<PlotOrSpecimen> set = plotSpecimensByTraitInstance.get(ti);
		if (set != null) {
			result = set.contains(plot);
		}
		return result;
	}

	public void remove(TraitInstance ti) {
	    plotSpecimensByTraitInstance.remove(ti);
	}

}
