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
package com.diversityarrays.kdxplore.trials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.TableColumnInfo;

class AddTraitsTableModel extends AbstractTableModel {

	        private static final String ONE = "0"; //$NON-NLS-1$
//            private final Set<Trait> chosen = new HashSet<>();
//            private final Map<Trait, String> instancesToCreateByTrait = new HashMap<>();
            private final Map<Trait, Integer> nInstanceTobeCreatedTrait = new TreeMap<>();

            private final Map<Trait, Integer> currentInstances = new TreeMap<>();

            
	        private List<Trait> allTraits;

	        private List<Trait> filteredTraits = new ArrayList<>();

            private final List<TableColumnInfo<Trait>> columnInfos = new ArrayList<>();

            private int editableInstanceNumbersColumnIndex = -1;
            private int instanceNumberListColumnIndex = -1;

	        AddTraitsTableModel(Map<Trait,Integer> instancesByTrait, boolean showInstanceCount) {

	        	currentInstances.putAll(instancesByTrait);
	        	
	            allTraits = new ArrayList<>(instancesByTrait.keySet());
	            Collections.sort(allTraits);
//
//	            String spec = ONE;
//	            List<Integer> instanceNumbers = AddTraitsDialog.parseInstanceNumbers(spec);
//	            for (Trait t : allTraits) {
////                    instancesToCreateByTrait.put(t, spec);
//                    nInstanceTobeCreatedTrait.put(t, instanceNumbers);
//	            }

	            // NOTE: the "chosen" is assumed to be 0 by isCellEditable()
//	            columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_ADD(), Boolean.class) {
//                    @Override
//                    public Object getColumnValue(int rowIndex, Trait t) {
//                        return chosen.contains(t);
//                    }
//                });

//	            if (showInstanceCount) {
                    editableInstanceNumbersColumnIndex = columnInfos.size();
                    columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_INSTANCE_NUMBERS(), String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, Trait t) {
//                            return instancesToCreateByTrait.get(t);
                        	return currentInstances.get(t);                  	
                        }
                    });

                    instanceNumberListColumnIndex = columnInfos.size();
                    columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_N_INSTANCES_TO_CREATE(), Integer.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, Trait t) {
                                Integer toCreate = nInstanceTobeCreatedTrait.get(t);
                                if (toCreate == null) {
                                	toCreate = 0;
                                }
                                
                                return toCreate;

                        }
                    });
//                };
                columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_TRAIT(), String.class) {
                    @Override
                    public Object getColumnValue(int rowIndex, Trait t) {
                        return t.getTraitName();
                    }
                });
                columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_TRAIT_UNIT(), String.class) {
                    @Override
                    public Object getColumnValue(int rowIndex, Trait t) {
                        return t.getTraitUnit();
                    }
                });

                Set<TraitLevel> levels = allTraits.stream().map(Trait::getTraitLevel).collect(Collectors.toSet());
                if (levels.size() > 1) {
                    columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_LEVEL(), String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, Trait t) {
                            return t.getTraitLevel().visible;
                        }
                    });
                }
                columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_DESCRIPTION(), String.class) {
                    @Override
                    public Object getColumnValue(int rowIndex, Trait t) {
                        return t.getTraitDescription();
                    }
                });

	            applyFilter(null);
	        }

	        public List<Integer> getColumnIndicesToResize() {
	            List<Integer> result = new ArrayList<>();
	            result.add(0); // the "checkbox" column
	            if (editableInstanceNumbersColumnIndex > 0) {
	                result.add(editableInstanceNumbersColumnIndex);
	            }
	            if (instanceNumberListColumnIndex > 0) {
	                result.add(instanceNumberListColumnIndex);
	            }
	            return result;
	        }

	        public int getEditableInstanceNumbersColumnIndex() {
	            return editableInstanceNumbersColumnIndex;
	        }

	        public int getInstanceNumberListColumnIndex() {
	            return instanceNumberListColumnIndex;
	        }

	        public int getChosenCount() {
//	            return chosen.size();
	        	return this.nInstanceTobeCreatedTrait.keySet().size();
	        }

            public void applyFilter(String filter) {

                if (Check.isEmpty(filter)) {
                    setFilteredTraits(allTraits);
                }
                else {
                    String loFilter = filter.toLowerCase();

                    Predicate<Trait> predicate = new Predicate<Trait>() {
                        @Override
                        public boolean test(Trait t) {
                            if (t.getTraitName().toLowerCase().contains(loFilter)) {
                                return true;
                            }
                            String desc = t.getTraitDescription();
                            if (desc != null && desc.toLowerCase().contains(loFilter)) {
                                return true;
                            }
                            String alias = t.getTraitAlias();
                            if (alias != null && alias.toLowerCase().contains(loFilter)) {
                                return true;
                            }
                            return false;
                        }
                    };

                    setFilteredTraits(allTraits.stream().filter(predicate)
                            .collect(Collectors.toList()));
                }
            }

            private void setFilteredTraits(List<Trait> list) {
                filteredTraits = list;
                fireTableDataChanged();
            }

            @Override
            public int getRowCount() {
                return filteredTraits.size();
            }

            @Override
            public int getColumnCount() {
                return columnInfos.size();
            }

            @Override
            public String getColumnName(int column) {
                return columnInfos.get(column).getColumnName();
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return columnInfos.get(col).getColumnClass();
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;// || columnIndex == editableInstanceNumbersColumnIndex;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            	if (1 == columnIndex) {
            		Trait trait = filteredTraits.get(rowIndex);
            		if (trait == null) {
            			return;
            		}

            		if (aValue instanceof Integer) {
            			Integer value = (Integer) aValue;

            			if (value > 0) {            				
            				nInstanceTobeCreatedTrait.put(trait, value);            				
            				fireTableCellUpdated(rowIndex, instanceNumberListColumnIndex);
            			}
            		}
            	}

//                if (0 == columnIndex) {
//                    // Changing "chosen"
//                    if (aValue instanceof Boolean) {
//                        Trait trait = filteredTraits.get(rowIndex);
//                        if ((Boolean) aValue) {
//                            chosen.add(trait);
//                            fireTableCellUpdated(rowIndex, 0);
//                            String spec = instancesToCreateByTrait.get(trait);
//                            if (Check.isEmpty(spec)) {
//                                spec = ONE;
//                            }
//                            List<Integer> list = AddTraitsDialog.parseInstanceNumbers(spec);
//                            if (list.isEmpty()) {
//                                spec = ONE;
//                                list = AddTraitsDialog.parseInstanceNumbers(spec);
//                            }
//                            instancesToCreateByTrait.put(trait, spec);
//                            nInstanceTobeCreatedTrait.put(trait, list);
//
//                            if (editableInstanceNumbersColumnIndex >= 0) {
//                                fireTableCellUpdated(rowIndex, editableInstanceNumbersColumnIndex);
//                            }
//
//                            if (instanceNumberListColumnIndex >= 0) {
//                                fireTableCellUpdated(rowIndex, instanceNumberListColumnIndex);
//                            }
//                        }
//                        else {
//                            chosen.remove(trait);
//                            fireTableCellUpdated(rowIndex, 0);
            	//                        }
            	//                    }
            	//                }
            	//                else
//            	if (1 == columnIndex) {
//            		if (aValue instanceof String) {
//            			String spec = (String) aValue;
//            			Trait trait = filteredTraits.get(rowIndex);
//
//            			List<Integer> instanceNumbers = AddTraitsDialog.parseInstanceNumbers(spec);
//            			if (instanceNumbers.isEmpty()) {
//            			}
//            			else {
////            				chosen.add(trait);
////            				instancesToCreateByTrait.put(trait, spec);
//            				nInstanceTobeCreatedTrait.put(trait, instanceNumbers);
//            				
//            				fireTableCellUpdated(rowIndex, 0);
//            				if (editableInstanceNumbersColumnIndex > 0) {
//            					fireTableCellUpdated(rowIndex, editableInstanceNumbersColumnIndex);
//            				}
//            				if (instanceNumberListColumnIndex > 0) {
//            					fireTableCellUpdated(rowIndex, instanceNumberListColumnIndex);
//            				}
//                        }
//                    }
//                    // Changing instance count - but this may also change "chosen"
//                    if (aValue instanceof Integer) {
//                        Trait trait = filteredTraits.get(rowIndex);
//                        int nInstances = (Integer) aValue;
//                        if (nInstances <= 0) {
//                            // If less than zero then "not chosen" - but don't change the number
//                            if (chosen.remove(trait)) {
//                                fireTableCellUpdated(rowIndex, 0);
//                            }
//                        }
//                        else {
//                            // greater than zero
//                            instanceCountByTrait.put(trait, nInstances);
//                            if (chosen.add(trait)) {
//                                fireTableCellUpdated(rowIndex, 0);
//                            }
//                        }
//                        fireTableCellUpdated(rowIndex, instanceCountEditableColumnIndex);
//                    }
//                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Trait trait = filteredTraits.get(rowIndex);

                return columnInfos.get(columnIndex).getColumnValue(rowIndex, trait);
            }

            public Map<Trait, List<Integer>> getInstancesToCreateByTrait() {
            	
                return nInstanceTobeCreatedTrait.keySet().stream().collect(
                        Collectors.toMap(Function.identity(), t -> 
                        IntStream.range(1, nInstanceTobeCreatedTrait.get(t) == null ? 1 : nInstanceTobeCreatedTrait.get(t) + 1)
                        .boxed()
                        .collect(Collectors.toList())));
                
            }
	    }
