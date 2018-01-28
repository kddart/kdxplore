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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.curate.PlotAttributeValuesIterator;
import com.diversityarrays.kdxplore.curate.PlotInformation;

public class TraitInstanceData /*implements PlotInfoProvider */ {

	public final Trial trial;
	public final TraitInstance traitInstance;
	private final List<PlotAttribute> plotAttributes;
	
	private final Map<Integer,PlotInformation> plotInfoByPlotId = new HashMap<>();
	private final Map<Integer,Sample> sampleByPlotId = new HashMap<>();
	private final List<Plot> plots;

	public TraitInstanceData(Trial trial, 
			TraitInstance traitInstance, 
			List<PlotAttribute> plotAttributes,
			List<Plot> plots) 
	{
		this.trial = trial;
		this.traitInstance = traitInstance;
		this.plotAttributes = plotAttributes;
		this.plots = plots;
	}
	
	public List<PlotAttribute> getPlotAttributes() {
		return Collections.unmodifiableList(plotAttributes);
	}
	
	public void addPlotInformation(PlotInformation pi) {
		plotInfoByPlotId.put(pi.getPlotId(), pi);
	}
	
	public void addSamples(Collection<Sample> samples) {
		for (Sample sample : samples) {
			addSample(sample);
		}
	}

	public void addSample(Sample sample) {
		int tin = sample.getTraitInstanceNumber();
		if (traitInstance.getInstanceNumber() == tin) {
			sampleByPlotId.put(sample.getPlotId(), sample);
		}
	}

//	@Override
	public Sample getSampleForTraitInstance(int plotId, TraitInstance ti) {
		Sample result = null;
		if (ti != null && ti.getTraitInstanceId() == traitInstance.getTraitInstanceId()) {
			result = sampleByPlotId.get(plotId);
		}
		return result;
	}

//	@Override
	public Plot getPlotByPlotId(int plotId) {
		PlotInformation pi = plotInfoByPlotId.get(plotId);
		return pi==null ? null : pi.plot;
	}

//	@Override
	public String getPlotAttributeValue(int plotId, String attributeName) {
		PlotInformation pi =  plotInfoByPlotId.get(plotId);
		return pi==null ? null : pi.getAttributeValue(attributeName);
	}

	public List<Plot> getPlots() {
		List<Plot> result = new ArrayList<>();
		for (PlotInformation pi : plotInfoByPlotId.values()) {
			result.add(pi.plot);
		}
		Comparator<Plot> comp = getPlotComparator(trial.getPlotIdentOption());
		Collections.sort(result, comp);
		return result;
	}
	
	static private Comparator<Plot> getPlotComparator(PlotIdentOption pio) {
		// This "default" is basically random as it depends on the database id.
		Comparator<Plot> result = new Comparator<Plot>() {
			@Override
			public int compare(Plot o1, Plot o2) {
				return Integer.compare(o1.getPlotId(), o2.getPlotId());
			}
			
		};
		switch (pio) {
		case NO_X_Y_OR_PLOT_ID:
			break;
		case PLOT_ID:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					return o1.getUserPlotId().compareTo(o2.getUserPlotId());
				}				
			};
			break;
		case PLOT_ID_THEN_X:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					int diff = o1.getUserPlotId().compareTo(o2.getUserPlotId());
					if (diff == 0) {
						diff = Integer.compare(o1.getPlotColumn(), o2.getPlotColumn());
					}
					return diff;
				}				
			};
			break;
		case PLOT_ID_THEN_XY:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					int diff = o1.getUserPlotId().compareTo(o2.getUserPlotId());
					if (diff == 0) {
						diff = Integer.compare(o1.getPlotColumn(), o2.getPlotColumn());
						if (diff == 0) {
							diff = Integer.compare(o1.getPlotRow(), o2.getPlotRow());
						}
					}
					return diff;
				}				
			};
			break;
		case PLOT_ID_THEN_Y:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					int diff = o1.getUserPlotId().compareTo(o2.getUserPlotId());
					if (diff == 0) {
						diff = Integer.compare(o1.getPlotRow(), o2.getPlotRow());
					}
					return diff;
				}				
			};
			break;
		case PLOT_ID_THEN_YX:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					int diff = o1.getUserPlotId().compareTo(o2.getUserPlotId());
					if (diff == 0) {
						diff = Integer.compare(o1.getPlotRow(), o2.getPlotRow());
						if (diff == 0) {
							diff = Integer.compare(o1.getPlotColumn(), o2.getPlotColumn());
						}
					}
					return diff;
				}				
			};
			break;
		case X_THEN_Y:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					int diff = Integer.compare(o1.getPlotColumn(), o2.getPlotColumn());
					if (diff == 0) {
						diff = Integer.compare(o1.getPlotRow(), o2.getPlotRow());
					}
					return diff;
				}				
			};
			break;
		case Y_THEN_X:
			result = new Comparator<Plot>() {
				@Override
				public int compare(Plot o1, Plot o2) {
					int diff = Integer.compare(o1.getPlotRow(), o2.getPlotRow());
					if (diff == 0) {
						diff = Integer.compare(o1.getPlotColumn(), o2.getPlotColumn());
					}
				return diff;
				}				
			};
			break;
		default:
			break;
		
		}
		return result;
	}

//	@Override
	public Iterator<String> getPlotAttributeValuesIterator(String attributeName) {
		return new PlotAttributeValuesIterator(attributeName, plots);
	}
	

}
