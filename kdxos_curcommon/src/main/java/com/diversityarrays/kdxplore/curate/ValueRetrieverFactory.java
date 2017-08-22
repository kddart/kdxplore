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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

public class ValueRetrieverFactory {
	
	static public Pair<ValueRetriever<?>,ValueRetriever<?>> getXyValueRetrievers(Trial trial) {
		ValueRetriever<?> xValueRetriever = null;
		ValueRetriever<?> yValueRetriever = null;
		
		
		switch (trial.getPlotIdentOption()) {

		case PLOT_ID_THEN_XY:
		case PLOT_ID_THEN_YX:
		case X_THEN_Y:
		case Y_THEN_X:
			PlotIdentSummary pis = trial.getPlotIdentSummary();
			if (! pis.xColumnRange.isEmpty()) {
				xValueRetriever = new PlotColumnValueRetriever(trial);
			}
			if (! pis.yRowRange.isEmpty()) {
				yValueRetriever = new PlotRowValueRetriever(trial);
			}

			break;

		case NO_X_Y_OR_PLOT_ID:
		case PLOT_ID:
		case PLOT_ID_THEN_X:
		case PLOT_ID_THEN_Y:
		default:
			break;
		
		}
		
		return new Pair<>(xValueRetriever, yValueRetriever);
	}

	static public ValueRetriever<Integer> getAttachmentCountValueRetriever(String displayName, int maxAttachmentCount) {
	    return new AttachmentCountValueRetriever(displayName, maxAttachmentCount);
	}
	
	static public ValueRetriever<Integer> getSpecimenNumberValueRetriever(String displayName, int maxSpecimenNumber) {
	    return new SpecimenNumberValueRetriever(displayName, maxSpecimenNumber);
	}

	static public List<ValueRetriever<?>> getPlotIdentValueRetrievers(Trial trial) {
		
		List<ValueRetriever<?>> result = new ArrayList<>();
		
		PlotIdentSummary pis = trial.getPlotIdentSummary();
		
		PlotIdentOption pio = trial.getPlotIdentOption();
		if (pio.isUsingPlotId() && ! pis.plotIdentRange.isEmpty()) {
			result.add(new PlotIdValueRetriever(trial));
		}
		
		switch (trial.getPlotIdentOption()) {
		case NO_X_Y_OR_PLOT_ID:
			break;
		case PLOT_ID:
			break;
		case PLOT_ID_THEN_X:
			if (! pis.xColumnRange.isEmpty()) {
				result.add(new PlotColumnValueRetriever(trial));
			}
			break;
		case PLOT_ID_THEN_XY:
			if (! pis.xColumnRange.isEmpty()) {
				result.add(new PlotColumnValueRetriever(trial));
			}
			if (! pis.yRowRange.isEmpty()) {
				result.add(new PlotRowValueRetriever(trial));
			}
			break;
		case PLOT_ID_THEN_Y:
			if (! pis.yRowRange.isEmpty()) {
				result.add(new PlotRowValueRetriever(trial));
			}
			break;
		case PLOT_ID_THEN_YX:
			if (! pis.yRowRange.isEmpty()) {
				result.add(new PlotRowValueRetriever(trial));
			}
			if (! pis.xColumnRange.isEmpty()) {
				result.add(new PlotColumnValueRetriever(trial));
			}
			break;
		case X_THEN_Y:
			if (! pis.xColumnRange.isEmpty()) {
				result.add(new PlotColumnValueRetriever(trial));
			}
			if (! pis.yRowRange.isEmpty()) {
				result.add(new PlotRowValueRetriever(trial));
			}
			break;
		case Y_THEN_X:
			if (! pis.yRowRange.isEmpty()) {
				result.add(new PlotRowValueRetriever(trial));
			}
			if (! pis.xColumnRange.isEmpty()) {
				result.add(new PlotColumnValueRetriever(trial));
			}
			break;
		default:
			break;		
		}
		
		return result;
	}
	
	static abstract private class AbstractPXYRetriever implements ValueRetriever<Integer> {
		
		private String name;
		private int minimum;
		private int valueCount;
		private TrialCoord trialCoord;

		protected AbstractPXYRetriever(String name, TrialCoord tc, int minumum, int valueCount) {
			this.name = name;
			this.trialCoord = tc;
			this.minimum = minumum;
			this.valueCount = valueCount;
		}
		
		@Override
		final public int hashCode() {
		    return trialCoord.hashCode();
		}
		
		@Override
		final public boolean equals(Object o) {
		    if (this==o) return true;
		    if (! (o instanceof AbstractPXYRetriever)) return false;
		    AbstractPXYRetriever other = (AbstractPXYRetriever) o;
		    return this.trialCoord.equals(other.trialCoord);
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
		final public String getDisplayName() {
			return name;
		}
		
		@Override
		final public com.diversityarrays.kdxplore.curate.ValueRetriever.TrialCoord getTrialCoord() {
			return trialCoord;
		}
		
		@Override
		final public Class<Integer> getValueClass() {
			return Integer.class;
		}
		
		@Override
		final public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
			Integer result = getAttributeValue(infoProvider, pos, null);
			if (result != null) {
				result = result - minimum;
			}
			return result;
		}
		
		@Override
		public int getAxisZeroValue() {
			return minimum;
		}

		@Override
		public int getAxisValueCount() {
		    return valueCount;
		}
	}
	
	static private class PlotIdValueRetriever extends AbstractPXYRetriever {
		
		public PlotIdValueRetriever(Trial trial) {
			super(trial.getNameForPlot(), TrialCoord.PLOT_ID, 
                    trial.getPlotIdentSummary().plotIdentRange.getMinimum(),
                    trial.getPlotIdentSummary().plotIdentRange.getRangeSize());
		}
		
		@Override
		public String toString() {
		    return "PlotIdVR";
		}

		@Override
		public ValueType getValueType() {
		    return ValueType.USER_PLOT_ID;
		}

		@Override
		public Integer getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, Integer valueIfNull) {
			Integer result = valueIfNull;
			Plot plot = infoProvider.getPlotByPlotId(pos.getPlotId());
			if (plot != null) {
				result = plot.getUserPlotId();
			}
			return result;
		}
	};
	
	static private class PlotColumnValueRetriever extends AbstractPXYRetriever {
		
		public PlotColumnValueRetriever(Trial trial) {
			super(trial.getNameForColumn(), 
			        TrialCoord.X, 
			        trial.getPlotIdentSummary().xColumnRange.getMinimum(),
			        trial.getPlotIdentSummary().xColumnRange.getRangeSize());
		}
		
		@Override
		public String toString() {
		    return "PlotColumnVR";
		}

		@Override
        public ValueType getValueType() {
            return ValueType.X_COLUMN;
        }

		@Override
	    public boolean isPlotColumn() {
	        return true;
	    }

		@Override
		public Integer getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, Integer valueIfNull) {
			Integer result = valueIfNull;
			Plot plot = infoProvider.getPlotByPlotId(pos.getPlotId());
			if (plot != null) {
				result = plot.getPlotColumn();
			}
			return result;
		}
		
	};
	
	static private class PlotRowValueRetriever extends AbstractPXYRetriever {
		
		public PlotRowValueRetriever(Trial trial) {
			super(trial.getNameForRow(), TrialCoord.Y, 
			        trial.getPlotIdentSummary().yRowRange.getMinimum(),
			        trial.getPlotIdentSummary().yRowRange.getRangeSize());
		}
		
		@Override
		public String toString() {
		    return "PlotRowVR";
		}

        @Override
        public ValueType getValueType() {
            return ValueType.Y_ROW;
        }

        @Override
	    public boolean isPlotRow() {
	        return true;
	    }

		@Override
		public Integer getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, Integer valueIfNull) {
			Integer result = valueIfNull;
			Plot plot = infoProvider.getPlotByPlotId(pos.getPlotId());
			if (plot != null) {
				result = plot.getPlotRow();
			}
			return result;
		}
	}
	
	static public ValueRetriever<?> getPlotNoteValueRetriever() {
        return new PlotNoteValueRetriever();
    }
	
	static private class PlotNoteValueRetriever implements ValueRetriever<String> {
	    
	    PlotNoteValueRetriever() {
	    }
	    
	    @Override
	    public String toString() {
	        return "PlotNoteVR";
	    }

        @Override
        public ValueType getValueType() {
            return ValueType.PLOT_NOTE;
        }

        @Override
        public String getDisplayName() {
            return "Plot Note";
        }

        @Override
        public TrialCoord getTrialCoord() {
            return TrialCoord.NONE;
        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public String getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos,
                String valueIfNull) {
            Plot plot = infoProvider.getPlotByPlotId(pos.getPlotId());
            String note = plot==null ? null : plot.getNote();
            return note==null ? "" : note;
        }

        @Override
        public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
            return null;
        }

        @Override
        public int getAxisZeroValue() {
            return 0;
        }

        @Override
        public int getAxisValueCount() {
            return 0;
        }

        @Override
        public boolean isPlotColumn() {
            return false;
        }

        @Override
        public boolean isPlotRow() {
            return false;
        }
	}
	
	static public ValueRetriever<?> getPlotTagsValueRetriever(List<String> tagsInOrder, String joiner) {
	    return new PlotTagsValueRetriever(tagsInOrder, joiner);
	}

	static private class PlotTagsValueRetriever implements ValueRetriever<String> {
	    
	    private List<String> tagsInOrder;
        private final String joiner;

        public PlotTagsValueRetriever(List<String> tagsInOrder, String joiner) {
	        this.tagsInOrder = tagsInOrder;
	        this.joiner = joiner;
	    }
        
        @Override
        public int hashCode() {
            return tagsInOrder.hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this==o) return true;
            if (! (o instanceof PlotTagsValueRetriever)) return false;
            PlotTagsValueRetriever other = (PlotTagsValueRetriever) o;
            return this.tagsInOrder.equals(other.tagsInOrder);
        }

	    @Override
	    public String toString() {
	        return "PlotTagsVR";
	    }

        @Override
        public ValueType getValueType() {
            return ValueType.PLOT_TAGS;
        }

        @Override
        public String getDisplayName() {
            return "Tags";
        }

        @Override
        public TrialCoord getTrialCoord() {
            return TrialCoord.NONE;
        }

        @Override
        public Class<String> getValueClass() {
            return String.class;
        }

        @Override
        public String getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, String valueIfNull) {
            
            SortedSet<String> labels = null;
            if (pos != null) {
                Map<Integer, List<Tag>> map = pos.getTagsBySampleGroup();
                if (! map.isEmpty()) {
                    labels = new TreeSet<>(pos.getTagsBySampleGroup().values()
                            .stream()
                            .flatMap(l -> l.stream())
                            .map(tag -> tag.getLabel())
                            .collect(Collectors.toSet()));
                }
            }
            if (Check.isEmpty(labels)) {
                return "";
            }
            return labels.stream().collect(Collectors.joining(joiner));
        }

        @Override
        public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
            
            Collection<String> labels = null;
            if (pos != null) {
                Map<Integer, List<Tag>> map = pos.getTagsBySampleGroup();
                if (! map.isEmpty()) {
                    labels = map.entrySet()
                            .stream()
                            .flatMap(e -> e.getValue().stream())
                            .map(tag -> tag.getLabel())
                            .collect(Collectors.toSet());
//                            .collect(supplier, accumulator, combiner);
                }
            }
            String first = null;
            if (! Check.isEmpty(labels)) {
                if (labels instanceof SortedSet || labels.size()==1) {
                    for (String s : labels) {
                        first = s;
                        break;
                    }
                }
                else {
                    List<String> list = new ArrayList<>(labels);
                    Collections.sort(list);
                    first = list.get(0);
                }
            }
            int idx = tagsInOrder.indexOf(first);
            return idx + 1;
        }

        @Override
        public int getAxisZeroValue() {
            return 0;
        }

        @Override
        public int getAxisValueCount() {
            return tagsInOrder.size() + 1;
        }

        @Override
        public boolean isPlotColumn() {
            return false;
        }

        @Override
        public boolean isPlotRow() {
            return false;
        }
	}
	
	static private class SpecimenNumberValueRetriever implements ValueRetriever<Integer> {
	    
	    private final String displayName;
	    private final int maxSpecimenNumber;

	    SpecimenNumberValueRetriever(String displayName, int maxSpecimenNumber) {
	        this.displayName = displayName;
	        this.maxSpecimenNumber = maxSpecimenNumber;
	    }

	    @Override
	    public ValueType getValueType() {
	        return ValueType.SPECIMEN_NUMBER;
	    }

	    @Override
	    public String getDisplayName() {
	        return displayName;
	    }

	    @Override
	    public TrialCoord getTrialCoord() {
	        return TrialCoord.NONE;
	    }

	    @Override
	    public Class<Integer> getValueClass() {
	        return Integer.class;
	    }

	    @Override
	    public Integer getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, Integer valueIfNull) {
	        if (PlotOrSpecimen.ORGANISM_NUMBER_IS_PLOT==pos.getSpecimenNumber()) {
	            return null;
	        }
	        return pos.getSpecimenNumber();
	    }

	    @Override
	    public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
	        return pos.getSpecimenNumber();
	    }

	    @Override
	    public int getAxisZeroValue() {
	        return 0;
	    }

	    @Override
	    public int getAxisValueCount() {
	        return maxSpecimenNumber + 1;
	    }

	    @Override
	    public boolean isPlotColumn() {
	        return false;
	    }

	    @Override
	    public boolean isPlotRow() {
	        return false;
	    }
	}
	
	static private class AttachmentCountValueRetriever implements ValueRetriever<Integer> {

	    private final String displayName;
	    private int maxAttachmentCount;
	    
	    AttachmentCountValueRetriever(String displayName, int maxAttachmentCount) {
	        this.displayName = displayName;
	        this.maxAttachmentCount = maxAttachmentCount;
	    }

	    @Override
	    public ValueRetriever.ValueType getValueType() {
	        return ValueType.ATTACHMENT_COUNT;
	    }

	    @Override
	    public String getDisplayName() {
	        return displayName;
	    }

	    @Override
	    public ValueRetriever.TrialCoord getTrialCoord() {
	        return TrialCoord.NONE;
	    }

	    @Override
	    public Class<Integer> getValueClass() {
	        return Integer.class;
	    }

	    @Override
	    public Integer getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, Integer valueIfNull) {
	        return pos.getMediaFileCount();
	    }

	    @Override
	    public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
	        return pos.getMediaFileCount();
	    }

	    @Override
	    public int getAxisZeroValue() {
	        return 0;
	    }

	    @Override
	    public int getAxisValueCount() {
	        return maxAttachmentCount + 1;
	    }

	    @Override
	    public boolean isPlotColumn() {
	        return false;
	    }

	    @Override
	    public boolean isPlotRow() {
	        return false;
	    }
	}
}
