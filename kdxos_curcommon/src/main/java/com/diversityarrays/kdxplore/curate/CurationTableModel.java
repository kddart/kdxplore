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

import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.collections15.Predicate;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.beans.DartEntityBeanInfo;
import com.diversityarrays.kdxplore.beans.DartEntityBeanRegistry;
import com.diversityarrays.kdxplore.beans.DartEntityFeature;
import com.diversityarrays.kdxplore.beans.KddartBeanUtils;
import com.diversityarrays.kdxplore.curate.ValueRetriever.ValueType;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

@SuppressWarnings("nls")
public class CurationTableModel extends AbstractTableModel {
	
	static private boolean DEBUG = false;
	
	public static final String PROPERTY_TRAIT_INSTANCES = "traitInstancesChanged";

	static private final Predicate<ValueRetriever<?>> IS_TRAIT_INSTANCE = new Predicate<ValueRetriever<?>>() {
		@Override
		public boolean evaluate(ValueRetriever<?> vr) {
			return (vr instanceof TraitInstanceValueRetriever);
		}
	};
	
	static private final Predicate<ValueRetriever<?>> IS_NOT_TRAIT_INSTANCE = new Predicate<ValueRetriever<?>>() {
		@Override
		public boolean evaluate(ValueRetriever<?> vr) {
			return ! (vr instanceof TraitInstanceValueRetriever);
		}
		
	};
	
	


	static final Map<String,Boolean> CURATION_TRIAL_UNIT_COLUMN_WANTED;

	static {
		Map<String,Boolean> map = new LinkedHashMap<String,Boolean>();
		map.put("PlotId", DEBUG);
		map.put(Plot.COLNAME_PLANTING_DATE, false);
		
		CURATION_TRIAL_UNIT_COLUMN_WANTED = map;
	}
	
	private final CurationData curationData;

	private final Map<TraitInstance,TraitInstanceValueRetriever<?>> tiRetrieverByTraitInstance = new HashMap<>();
	
	private final List<ValueRetriever<?>> valueRetrievers = new ArrayList<>();
	private final List<DartEntityFeature<?>> showFeatures = new ArrayList<DartEntityFeature<?>>();
	
	private boolean readOnly;

	private CurationDataChangeListener curationDataChangeListener = new CurationDataChangeListener() {
		@Override
		public void plotActivationChanged(Object source, boolean activated, List<Plot> plots) {
			handlePlotActivationChanges(plots);
		}
		@Override
		public void editedSamplesChanged(Object source, List<CurationCellId> curationCellIds) {
			fireEditedSampleTableChanges(curationCellIds);
		}
	};

//	static class PlotAndPlant {
//        public final Plot plot;
//        public final int plantNumber;
//        public PlotAndPlant(Plot p, int n) {
//            plot = p;
//            plantNumber = n;
//        }
//	}
	
	private final List<PlotOrSpecimen> plotOrSpecimens = new ArrayList<>();
	private final Map<Integer,Plot> plotByPlotId = new HashMap<>();
    private final Map<Integer,Integer[]> plotOrSpecimenRowsByPlotId = new HashMap<>();
    private final Map<PlotOrSpecimen,Integer> rowByPlotOrSpecimen = new HashMap<>();
	
	public CurationTableModel(CurationData cd) {
		this(cd, false);
	}
	
	public CurationTableModel(CurationData cd, boolean readOnly) {
		
		curationData = cd;
		this.readOnly = readOnly;

		// =======================================================================
		// - - - - - BEGIN: building plotOrSpecimens and the various cross-indexes
		plotOrSpecimens.clear();
		plotByPlotId.clear();
        rowByPlotOrSpecimen.clear();

        Map<Integer,List<Integer>> ppRowsByPlotId = new HashMap<>();
		for (Plot plot : curationData.getPlots()) {
		    plotByPlotId.put(plot.getPlotId(), plot);
		    for (Integer psnum : plot.getSpecimenNumbers(PlotOrSpecimen.INCLUDE_PLOT)) {
                PlotOrSpecimen pos = plot.getPlotOrSpecimen(psnum);
		        plotOrSpecimens.add(pos);
		    }
		}

		Optional<PlotOrSpecimen> opt_pos = plotOrSpecimens.stream()
		        .filter(pos -> PlotOrSpecimen.isSpecimenNumberForSpecimen(pos.getSpecimenNumber()))
		        .findFirst();

		PlotOrSpecimenComparator comp = new PlotOrSpecimenComparator(
		        curationData.getTrial(), plotByPlotId, opt_pos.isPresent());
		if (! comp.isNoOperation()) {
	        Collections.sort(plotOrSpecimens, comp);
		}

		int rowIndex = -1;
		for (PlotOrSpecimen pos : plotOrSpecimens) {
		    ++rowIndex;
		    rowByPlotOrSpecimen.put(pos, rowIndex);
            List<Integer> list = ppRowsByPlotId.get(pos.getPlotId());
            if (list == null) {
                list = new ArrayList<>();
                ppRowsByPlotId.put(pos.getPlotId(), list);
            }
            list.add(rowIndex);
		}

		plotOrSpecimenRowsByPlotId.clear();
		for (Integer plotId : ppRowsByPlotId.keySet()) {
		    List<Integer> rowIndices = ppRowsByPlotId.get(plotId);
		    plotOrSpecimenRowsByPlotId.put(plotId, 
		            rowIndices.toArray(new Integer[rowIndices.size()]));
		}
		// - - - - - END: building plotOrSpecimens and the various cross-indexes
        // =====================================================================

		curationData.addCurationDataChangeListener(curationDataChangeListener);
		
		valueRetrievers.clear();

		DartEntityBeanInfo<Plot> beanInfo = DartEntityBeanRegistry.PLOT_BEAN_INFO;
		Map<String,DartEntityFeature<?>> map = KddartBeanUtils.makeLookupByColumnName(beanInfo);
		
		for (String columnName : CURATION_TRIAL_UNIT_COLUMN_WANTED.keySet()) {
			DartEntityFeature<?> f = map.get(columnName);
			if (f==null) {
				throw new RuntimeException("Missing feature for columnName='"+columnName+"'");
			}
			
			if (CURATION_TRIAL_UNIT_COLUMN_WANTED.get(columnName)) {
				showFeatures.add(f);
			}
		}

//		Set<String> plotTypes = new HashSet<>();
//		for (Plot plot : cd.getPlots()) {
//			String plotType = plot.getPlotType();
//			if (plotType != null && ! plotType.isEmpty()) {
//				plotTypes.add(plotType);
//			}
//		}
//		if (! plotTypes.isEmpty()) {
//			valueRetrievers.add(new PlotTypeValueRetriever(plotTypes));
//		}

//		Trial trial = curationData.getTrial();
//		valueRetrievers.addAll(ValueRetrieverFactory.getPlotIdentValueRetrievers(trial));
		
//		initPlotAttributes();
	}

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(String property, PropertyChangeListener l) {
		propertyChangeSupport.addPropertyChangeListener(property, l);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener l) {
		propertyChangeSupport.removePropertyChangeListener(property, l);
	}
	
	private void initPlotAttributes(List<ValueRetriever<?>> vrList) {

		List<ValueRetriever<?>> onlyTraits = valueRetrievers.stream()
		        .filter(vr -> vr instanceof TraitInstanceValueRetriever)
		        .collect(Collectors.toList());

		valueRetrievers.clear();

		valueRetrievers.addAll(vrList);
		valueRetrievers.addAll(onlyTraits);

		// After refreshing the list based on the users requirements, we re-sort
		// so that PlotIdentifiers come before PlotAttributes before Traits.
		
		// But first we build this so that the order the user selected them in
		// is preserved.
		final Map<ValueRetriever<?>,Integer> posByValueRetriever = new HashMap<>();
		int pos = 0;
		for (ValueRetriever<?> vr : valueRetrievers) {
			posByValueRetriever.put(vr, pos);
			++pos;
		}
		
		Collections.sort(valueRetrievers, new Comparator<ValueRetriever<?>>() {
			@Override
			public int compare(ValueRetriever<?> lhs, ValueRetriever<?> rhs) {
				int diff = lhs.getValueType().compareTo(rhs.getValueType());
				if (diff == 0) {
					// preserve the user's chosen order
					diff = posByValueRetriever.get(lhs).compareTo(posByValueRetriever.get(rhs));
				}
				return diff;
			}
		});

		fireTableStructureChanged();
	}
	
	// FIXME use this somewhere
	public boolean isReadOnly() {
		return readOnly;
		// I remembered! it is in case we want to stop any editing; For example:
		//  The user has access to READ the data but is not allowed to alter it
		//  - this might be due to database access rights, or the Trial is "CLOSED"
		//  but the Curation U/I is still a useful data exploration tool.
	}
	
	public void addTraitInstance(TraitInstance ti) {
		int count = valueRetrievers.size();
		addTraitInstanceImpl(ti, true);
		int newCount = valueRetrievers.size();
		if (newCount != count) {
			propertyChangeSupport.firePropertyChange(PROPERTY_TRAIT_INSTANCES, count, newCount);
		}
		
		for (Consumer<Void> consumer : traitChangeListeners) {
			consumer.accept(null);
		}
	}
	
	private boolean addTraitInstanceImpl(TraitInstance ti, boolean fire) {
		boolean result = false;
		if (! tiRetrieverByTraitInstance.containsKey(ti)) {
			TraitInstanceValueRetriever<?> tir;
			try {
                tir = TraitInstanceValueRetriever.getValueRetriever(
                        curationData.getTrial(), 
                        ti, 
                        curationData.getSampleProvider());
				tiRetrieverByTraitInstance.put(ti, tir);
				valueRetrievers.add(tir);
				if (fire) {
					fireTableStructureChanged();
				}
				result = true;
			} catch (InvalidRuleException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public void removeTraitInstance(TraitInstance ti) {
		TraitInstanceValueRetriever<?> tir = tiRetrieverByTraitInstance.remove(ti);
		if (tir != null) {
			int count = valueRetrievers.size();
			
			valueRetrievers.remove(tir);
			fireTableStructureChanged();

			propertyChangeSupport.firePropertyChange(PROPERTY_TRAIT_INSTANCES, count, valueRetrievers.size());
		}
	}

	@Override
	public int getColumnCount() {
		return showFeatures.size() + valueRetrievers.size();
	}
	
	public Integer getColumnIndexForTraitInstance(TraitInstance ti) {
		Integer result = null;
		if (ti != null) {
			int index = showFeatures.size();
			for (ValueRetriever<?> vr : valueRetrievers) {
				if (vr instanceof TraitInstanceValueRetriever) {
					TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
					if (ti.equals(tivr.traitInstance)) {
						result = index;
						break;
					}
				}
				++index;
			}
		}
		return result;
	}
	
	public String getStringNumberFormatAt(int column) {
		String result = null;
		TraitInstance ti = getTraitInstanceAt(column);
		if (ti != null) {
			TraitInstanceValueRetriever<?> tivr = tiRetrieverByTraitInstance.get(ti);
			if (tivr != null) {
				result = tivr.getStringNumberFormat();
			}
		}
		return result;
	}

	public TraitInstance getTraitInstanceAt(int column) {
		TraitInstance result = null;
		int vrIndex = column - showFeatures.size();
		if (vrIndex >= valueRetrievers.size()) {
			return null;
		}
		if (vrIndex >= 0) {
			ValueRetriever<?> vr = valueRetrievers.get(vrIndex);
			if (vr instanceof TraitInstanceValueRetriever) {
				result = ((TraitInstanceValueRetriever<?>) vr).getTraitInstance();
			}
		}
		return result;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		Class<?> result = null;
		if (column < showFeatures.size()) {
			result = String.class;
		}
		else {
			int vrIndex = column - showFeatures.size();
			if (vrIndex >= valueRetrievers.size()) {
				return Object.class;
			}
			if (vrIndex < this.getColumnCount()) {
				ValueRetriever<?> vr = valueRetrievers.get(vrIndex);
				result = vr.getValueClass();
			}
		}
		return result;
	}
	
//	static public enum ColumnType {
//	    DART_ENTITY_FEATURE(null),
//
//	    PLOT_POSITION(ValueType.PLOT_POSITION),
//	    PLOT_TYPE(ValueType.PLOT_TYPE),
//        SPECIMEN_NUMBER(ValueType.SPECIMEN_NUMBER),
//        ATTACHMENT_COUNT(ValueType.ATTACHMENT_COUNT),
//        PLOT_NOTE(ValueType.PLOT_NOTE),
//        PLOT_TAGS,
//	    PLOT_ATTRIBUTE,
//	    TRAIT_INSTANCE,
//
//	    UNKNOWN,
//	    ;
//	    
//	    public final ValueType valueType;
//	    ColumnType(ValueType vt) {
//	        valueType = vt;
//	    }
//	}
	
	public ValueType getColumnValueType(int column) {
	    if (column < showFeatures.size()) {
	        return null; // ColumnType.DART_ENTITY_FEATURE;
	    }
	    int vrIndex = column - showFeatures.size();
	    if (vrIndex < valueRetrievers.size()) {
	        ValueRetriever<?> vr = valueRetrievers.get(vrIndex);
	        return vr.getValueType();
	    }
	    return null;
	}
	
	@Override
	public String getColumnName(int column) {
		if (column < showFeatures.size()) {
			return showFeatures.get(column).getColumnName();
		}
		int vrIndex = column - showFeatures.size();
		return valueRetrievers.get(vrIndex).getDisplayName();
	}

	public List<TraitInstance> getTraitInstances() {
		List<TraitInstance> result = new ArrayList<>();
		for (ValueRetriever<?> vr : valueRetrievers) {
			if (vr instanceof TraitInstanceValueRetriever) {
				TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
				result.add(tivr.traitInstance);
			}
		}
		return result;
	}
	
	private boolean usePlotIdentOption = true;
	public String getPlotName(int rowIndex) {

		String result;
		
		Trial trial = curationData.getTrial();
		
		Plot plot = getPlotAtRowIndex(rowIndex);
		if (usePlotIdentOption) {
			PlotIdentOption pio = trial.getPlotIdentOption();
			result = pio.createPositionDescription(trial, "/", plot, ":");
		}
		else {
			StringBuilder sb = new StringBuilder(trial.getNameForPlot());
			String sep = "/";
			for (DartEntityFeature<?> feature : showFeatures) {
				Object value = feature.readValue(plot);
				if (value != null) {
					sb.append(sep).append(value);
					sep = ":";
				}
			}
			result = sb.toString();
		}

		PlotOrSpecimen pos = getPlotOrSpecimenAtRowIndex(rowIndex);
		if (! pos.isPlot()) {
		    result = result + "#" + pos.getSpecimenNumber();
		}
		return result;
	}

	@Override
    public int getRowCount() {
	    return plotOrSpecimens.size();
    }

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object result = null;
		
		Point pt = new Point(columnIndex, rowIndex);
		result = temporaryValues.get(pt);
		if (result == null) {
		    if (columnIndex < showFeatures.size()) {
		        Plot plot = getPlotAtRowIndex(rowIndex);
		        DartEntityFeature<?> feature = showFeatures.get(columnIndex);
		        result = feature.readValue(plot);
		    }
		    else {
		        int vfIndex = columnIndex - showFeatures.size();
		        if (vfIndex < valueRetrievers.size()) {
	                ValueRetriever<?> vr = valueRetrievers.get(vfIndex);
	                PlotOrSpecimen pos = plotOrSpecimens.get(rowIndex);
	                result = vr.getAttributeValue(curationData, pos, null);
		        }		       
		    }
		}
		return result;
	}
	
	private Map<Point,Comparable<?>> temporaryValues = new HashMap<>();
	
	public boolean isTemporaryValue(int rowIndex, int columnIndex) {
//	    TraitInstance ti = getTraitInstanceAt(columnIndex);
//	    if (ti==null) {
//	        return false;
//	    }
//	    if (rowIndex < 0 || rowIndex >= plotOrSpecimens.size()) {
//	        return false;
//	    }
//	    PlotOrSpecimen pos = plotOrSpecimens.get(rowIndex);
        return temporaryValues.containsKey(new Point(columnIndex, rowIndex));
    }
	

    public void setTemporaryValue(CurationCellId ccid, Comparable<?> value) {
        
        Predicate<PlotOrSpecimen> predicate = new Predicate<PlotOrSpecimen>() {
            @Override
            public boolean evaluate(PlotOrSpecimen pos) {
                return ccid.specimenNumber==pos.getSpecimenNumber() && ccid.plotId==pos.getPlotId();
            }
            
        };
        
        int foundRow = -1;
        for (int row = 0; row < plotOrSpecimens.size(); ++row) {
            PlotOrSpecimen pos = plotOrSpecimens.get(row);
            if (predicate.evaluate(pos)) {
                foundRow = row;
                break;
            }
        }
        
        if (foundRow >= 0) {
            Integer column = getColumnIndexForTraitInstance(ccid.traitInstance);
            if (column != null) {
                Point pt = new Point(column, foundRow);
                if (value == null) {
                    temporaryValues.remove(pt);
                }
                else {
                    temporaryValues.put(pt, value);     
                }
                
                this.fireTableCellUpdated(foundRow, column);
            }
        }
    }

    public void clearTemporaryValues(/*TraitInstance ti */) {     

        if (temporaryValues != null) {
            if (! temporaryValues.isEmpty()) {
                for (Point pt : temporaryValues.keySet()) {
                    this.fireTableCellUpdated(pt.y, pt.x);
                }
                temporaryValues.clear();
            }
        }
//
//        Integer columnIndex = getColumnIndexForTraitInstance(ti);
//        if (columnIndex == null) {
//            if (temporaryValues != null) {
//                temporaryValues.clear();
//            }
//        }
//        else {
//            if (temporaryValues != null) {
//                if (! temporaryValues.isEmpty()) {
//                    for (Point pt : temporaryValues.keySet()) {
//                        this.fireTableCellUpdated(pt.y, pt.x);
//                    }
//                    temporaryValues.clear();
//                }
//            }
//        }
    }
	
	public CurationCellValue getCurationCellValue(int rowIndex, TraitInstance traitInstance) {
		return curationData.getCurationCellValue(getPlotOrSpecimenAtRowIndex(rowIndex), traitInstance);
	}
	
	public List<CurationCellValue> getCurationCellValuesForPlot(Integer plotId) {
		List<CurationCellValue> result = new ArrayList<CurationCellValue>();

		int rowCount = getRowCount();
		for (Integer rowIndex : getRowsForPlotId(plotId, TraitLevel.PLOT)) {
			if (rowIndex >= 0 && rowIndex < rowCount) {
				for (ValueRetriever<?> vr : valueRetrievers) {
					if (vr instanceof TraitInstanceValueRetriever) {
						TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
						TraitInstance ti = tivr.getTraitInstance();
                        PlotOrSpecimen pos = getPlotOrSpecimenAtRowIndex(rowIndex);
						result.add(curationData.getCurationCellValue(pos, ti));
					}
				}
			}
		}
		return result;
	}
	
	public List<CurationCellValue> getCurationCellValuesForPlotSpecial(Integer plotId, List<TraitInstance> traitInstances) {
		List<CurationCellValue> result = new ArrayList<CurationCellValue>();
		List<TraitInstance> alreadyAdded = new ArrayList<TraitInstance>();

		for (TraitInstance ti : traitInstances) {
			if (this.getColumnIndexForTraitInstance(ti) == null) {
				this.addTraitInstanceImpl(ti, false);
			} else {
				alreadyAdded.add(ti);
			}
		}

		int rowCount = getRowCount();
		for (Integer rowIndex : getRowsForPlotId(plotId, TraitLevel.PLOT)) {
			if (rowIndex >= 0 && rowIndex < rowCount) {
				for (ValueRetriever<?> vr : valueRetrievers) {
					if (vr instanceof TraitInstanceValueRetriever) {
						TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
						TraitInstance ti = tivr.getTraitInstance();
						PlotOrSpecimen pos = getPlotOrSpecimenAtRowIndex(rowIndex);
						result.add(curationData.getCurationCellValue(pos, ti));
					}
				}
			}
		}
		
		for (TraitInstance ti : traitInstances) {
			if (!alreadyAdded.contains(ti)) {
				this.removeTraitInstance(ti);
			}
		}
		
		return result;
	}
	
	public CurationCellValue getCurationCellValueForTraitInstanceInPlot(TraitInstance ti, Plot plot) {
		CurationCellValue result = null;
		for (ValueRetriever<?> vr : valueRetrievers) {
			if (vr instanceof TraitInstanceValueRetriever) {
				TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
				if (ti == tivr.getTraitInstance()) {
					result = curationData.getCurationCellValue(plot, ti);
					break;
				}
			}
		}
		return result;
	}

	public PlotOrSpecimen getPlotOrSpecimenAtRowIndex(int rowIndex) {
		return plotOrSpecimens.get(rowIndex);
	}
	
	// FIXME check all uses of this method in case they are on longer "correct" for sub-plot editing
	public Plot getPlotAtRowIndex(int rowIndex) {
	    return plotByPlotId.get(getPlotOrSpecimenAtRowIndex(rowIndex).getPlotId());
	}
	
//    public PlotOrSpecimen getPlotPlantAtRowIndex(int rowIndex) {
//        return plotPlants.get(rowIndex);
//    }

	
	private boolean usingOnlyUnedited;
	public void setUsingOnlyUnedited(boolean b) {
		usingOnlyUnedited = b;
	}
	
	private void handlePlotActivationChanges(List<Plot> plots) {
		if (usingOnlyUnedited) {
			// the plots MAY include edited ones - we don't know
			fireTableDataChanged();
		}
		else {
			for (Plot plot : plots) {
				Integer[] rows = getRowsForPlotId(plot.getPlotId(), TraitLevel.PLOT);
				for (Integer row : rows) {
					fireTableRowsUpdated(row, row);
				}
			}
		}
	}

	private void fireEditedSampleTableChanges(List<CurationCellId> curationCellIds) {
		if (usingOnlyUnedited) {
			// edited samples have changed but we are only viewing the unedited ones
			// so the view doesn't contain the sample we have just changed as it
			// will be filtered out.
			//
			// If we try to do "fireTableCellUpdated" the table repaint will
			// attempt to use the ccvModelRow but that will give an IndexOutOfBoundsException
			// because it won't find a viewRow that corresponds to it
			fireTableDataChanged();
		}
		else {
			Set<TraitInstance> notFound = new HashSet<>();
			// cache the results locally
			Map<TraitInstance,Integer> columnIndexByTraitInstance = new HashMap<>();
			for (CurationCellId ccid : curationCellIds) {
			    // If we've already looked and haven't found it then don't look again
				if (! notFound.contains(ccid.traitInstance)) {
				    // Check cache and populate if not found
					Integer columnIndex = columnIndexByTraitInstance.get(ccid.traitInstance);
					if (columnIndex == null) {
						columnIndex = getColumnIndexForTraitInstance(ccid.traitInstance);
						if (columnIndex == null) {
							notFound.add(ccid.traitInstance);
						}
						else {
							columnIndexByTraitInstance.put(ccid.traitInstance, columnIndex);
						}
					}
					
					if (columnIndex != null) {
					    // Note: this may discover more rows than we expect
					    //       because for SubPlot traits we get the
					    //       get all of the 
                        Integer[] rows = getRowsForPlotId(ccid.plotId, TraitLevel.PLOT);
                        for (Integer row : rows) {
                            fireTableCellUpdated(row, columnIndex);
                        }
					}
				}
			}
		}
	}
	
	public void getKdxSamples(TraitInstance ti, Consumer<KdxSample> consumer) {
	    curationData.getKdxSamples(ti, consumer);
	}

	public List<KdxSample> getSampleMeasurements(TraitInstance ti) {
		return curationData.getSampleMeasurements(ti);
	}
	
    public Map<Plot,Map<Integer,KdxSample>> getEditStateSamplesByPlot(TraitInstance ti, Predicate<Plot> plotFilter) {
        return curationData.getEditStateSamplesByPlot(ti, plotFilter);
    }

    public Map<PlotOrSpecimen,KdxSample> getEditStateSamplesByPlotOrSpecimen(TraitInstance ti, Predicate<Plot> plotFilter) {
        return curationData.getEditStateSamplesByPlotOrSpecimen(ti, plotFilter);
    }
	
	public boolean hasAnyUncurated(int rowIndex) {
		int base = showFeatures.size();
		for (int vfIndex = valueRetrievers.size(); --vfIndex >= 0; ) {
			ValueRetriever<?> vr = valueRetrievers.get(vfIndex);
			if (! (vr instanceof TraitInstanceValueRetriever)) {
				continue;
			}
			int columnIndex = base + vfIndex;			
			TraitInstance ti = getTraitInstanceAt(columnIndex);

			if (ti != null ) {
				PlotOrSpecimen pos = getPlotOrSpecimenAtRowIndex(rowIndex); 
				if (pos != null) {
					CurationCellId ccid = new CurationCellId(pos, ti);
					KdxSample ed = curationData.getEditedSample(ccid);
					if (ed == null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public CurationCellValue getCurationCellValue(int rowIndex, int column) {
		CurationCellValue result = null;
		TraitInstance ti = getTraitInstanceAt(column);

		if (ti != null ) {
			result = curationData.getCurationCellValue(getPlotOrSpecimenAtRowIndex(rowIndex), ti);
		}
		return result;
	}
	
	public CurationCellValue getCurationCellValue(Plot plot, TraitInstance ti) {
		CurationCellValue result = null;

		if (ti != null && null != plot) {
			result = curationData.getCurationCellValue(plot, ti);

		}
		return result;
	}

	public KdxSample getSample(int row, int column) {
		KdxSample result = null;
		
		TraitInstance ti = getTraitInstanceAt(column);
		if (ti != null ) {
			PlotOrSpecimen pos = getPlotOrSpecimenAtRowIndex(row); 
			if (pos != null) {
				result = curationData.getEditStateSampleFor(ti, pos);
			}
		}
		return result;
	}
	
	static private final Integer[] NO_ROWS = new Integer[0];
	public Integer[] getRowsForPlotId(int wantedPlotId, TraitLevel levelWanted) {
        Integer[] result = plotOrSpecimenRowsByPlotId.get(wantedPlotId);
        if (result == null) {
            result = NO_ROWS;
        }
	    if (TraitLevel.PLOT != levelWanted && result.length > 0) {
	        List<Integer> list = new ArrayList<>(result.length);
	        for (Integer row : result) {
	            PlotOrSpecimen pos = plotOrSpecimens.get(row);
	            if (pos.isSpecimen()) {
	                list.add(row);
	            }
	        }
	        if (list.isEmpty()) {
	            result = NO_ROWS;
	        }
	        else {
	            result = list.toArray(new Integer[list.size()]);
	        }
	    }
	    return result;
	}
	
	public Optional<Integer> getRowForPlotOrSpecimen(PlotOrSpecimen pos) {
	    Integer row = rowByPlotOrSpecimen.get(pos);
	    return row==null ? Optional.empty() : Optional.of(row);
	}

	public void setSelectedPlotAttributes(List<ValueRetriever<?>> vrList) {
		initPlotAttributes(vrList);
	}

	public boolean hasAnyTraitInstanceColumns() {
		for (int vfIndex = valueRetrievers.size(); --vfIndex >= 0; ) {
			ValueRetriever<?> vr = valueRetrievers.get(vfIndex);
			if (vr instanceof TraitInstanceValueRetriever) {
				return true;
			}
		}
		return false;
	}

	public List<Integer> getTraitInstanceColumns() {
		return collectColumnIndices(IS_TRAIT_INSTANCE);
	}

	public List<Integer> getNonTraitInstanceColumns() {
		return collectColumnIndices(IS_NOT_TRAIT_INSTANCE);
	}

	protected List<Integer> collectColumnIndices(Predicate<ValueRetriever<?>> predicate) {
		List<Integer> result = new ArrayList<>();
		for (int vfIndex = valueRetrievers.size(); --vfIndex >= 0; ) {
			ValueRetriever<?> vr = valueRetrievers.get(vfIndex);
			if (predicate.evaluate(vr)) {
				result.add(vfIndex);
			}
		}
		return result;
	}
	
	public boolean hasAnyScores(int rowIndex) {
		for (int vfIndex = valueRetrievers.size(); --vfIndex >= 0; ) {
			PlotOrSpecimen pos = getPlotOrSpecimenAtRowIndex(rowIndex);
			ValueRetriever<?> vr = valueRetrievers.get(vfIndex);
			if (vr instanceof TraitInstanceValueRetriever) {
				TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
				Comparable<?> comp = tivr.getAttributeValue(curationData, pos, null);
				if (comp != null) {
					if (! (comp instanceof TraitValue)) {
						return true;
					}
					TraitValue traitValue = (TraitValue) comp;
					if (TraitValueType.UNSET != traitValue.traitValueType) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean hasPlotType() {
		for (ValueRetriever<?> vr : valueRetrievers) {
			if (vr instanceof PlotTypeValueRetriever) {
				return true;
			}
		}
		return false;
	}

	List<Consumer<Void>> traitChangeListeners = new ArrayList<>();
	
	public void addTraitChangeListener(Consumer<Void> consumer) {
		traitChangeListeners.add(consumer);
	}

}
