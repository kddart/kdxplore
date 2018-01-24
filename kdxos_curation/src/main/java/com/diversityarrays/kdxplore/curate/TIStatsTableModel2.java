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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;

import net.pearcan.reflect.Feature;

class TIStatsTableModel2 extends AbstractTableModel implements TIStatsTableModel {

        static private final List<Feature> STATS_FEATURES = Feature.getAllExhibitFeatures(SimpleStatistics.class);

        static class ColumnInfo {
            public final String heading;
            public final Class<?> columnClass;
            ColumnInfo(String h, Class<?> cc) {
                heading = h;
                columnClass = cc;
            }
        }
        static public final String HEADING_INSTANCE = "Instance";

        public static final int[] TRAIT_INSTANCE_COLUMN_INDICES = { 1, 2, 3 };
        
        static private final TIStatsTableModel2.ColumnInfo[] NON_FEATURE_COLUMN_INFO = new TIStatsTableModel2.ColumnInfo[] {
            new ColumnInfo("Validation Rule Error", InvalidRuleException.class),
            new ColumnInfo(HEADING_INSTANCE, Integer.class),
            new ColumnInfo("Name", TraitInstance.class),
            new ColumnInfo("DataType", String.class) //TraitDataType.class)
        };
        
        private static final int N_NON_FEATURE_COLUMNS = NON_FEATURE_COLUMN_INFO.length;

        public static final int VALRULE_ERROR_COLUMN_INDEX = 1;
        public static final int TRAIT_INSTANCE_NAME_COLUMN_INDEX = 2;

        public static final int TRAIT_INSTANCE_DATATYPE_COLUMN_INDEX = 3;

        private List<TraitInstance> showingInstances = new ArrayList<>();
        private StatsData statsData;

        private final CurationData curationData;
        
        private final DeviceType deviceTypeForSamples;

        private final CurationDataChangeListener curationDataChangeListener = new CurationDataChangeListener() {
            @Override
            public void plotActivationChanged(Object source, boolean activated, List<Plot> plots)  {
                rePopulateModel();
            }

            boolean inSamplesChanged = false;
            @Override
            public void editedSamplesChanged(Object source, List<CurationCellId> curationCellIds) {
                if (inSamplesChanged) {
                    return;
                }
                
                inSamplesChanged = true;
                try {
                    rePopulateModel();
                }
                finally {
                    inSamplesChanged = false;
                }
            }
        };
        
        public TIStatsTableModel2(CurationData cd, StatsData initialStatsData, DeviceType dtForSamples) 
        {
            super();
            this.deviceTypeForSamples = dtForSamples;
            this.curationData = cd;
            
            //statsData = initialStatsData; // curationData.getStatsData(deviceTypeForSamples);

            rePopulateModel();
            
            curationData.addCurationDataChangeListener(curationDataChangeListener);

            curationData.addPropertyChangeListener(CurationData.PROPERTY_N_STD_DEV_FOR_OUTLIER, new PropertyChangeListener() {          
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    rePopulateModel();
                }
            });
        }
        
        public void rePopulateModel() {
            
            statsData = curationData.getStatsData(deviceTypeForSamples);

            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return showingInstances.size();
        }

        @Override
        public String getColumnName(int col) {
            if (col < N_NON_FEATURE_COLUMNS) {
                return NON_FEATURE_COLUMN_INFO[col].heading;
            }
            return getFeature(col).getHeading();
        }

        @Override
        public int getColumnCount() {
            return N_NON_FEATURE_COLUMNS + STATS_FEATURES.size();
        }

        @Override
        public Class<?> getColumnClass(int column) {        
            if (column < N_NON_FEATURE_COLUMNS) {
                return NON_FEATURE_COLUMN_INFO[column].columnClass;
            }
            Class<?> result = getFeature(column).getValueClass();
            return result;
        }
        
        private Feature getFeature(int col) {
            if (col < N_NON_FEATURE_COLUMNS) {
                return null;
            }
            int ci = col - N_NON_FEATURE_COLUMNS;
            return STATS_FEATURES.get(ci);
        }
        
        public TraitInstance getTraitInstanceAt(int rowIndex) {
            return showingInstances.get(rowIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TraitInstance traitInstance = getTraitInstanceAt(rowIndex);

            if (columnIndex < N_NON_FEATURE_COLUMNS) {
                switch (columnIndex) {
                case 0:
                    return statsData.getInvalidRuleByTraitInstance(traitInstance);
                case 1: 
                    return traitInstance.getInstanceNumber();
                case 2:
                    return traitInstance;
//                  return traitNameStyle.makeTraitInstanceName(traitInstance);
                case 3:
                    return traitInstance.trait.getTraitDataType().shortName;
                }
            }
            Object result = null;
            try {
                int ci = columnIndex - N_NON_FEATURE_COLUMNS;
                SimpleStatistics<?> stats = statsData.getStatistics(traitInstance);
                if (stats != null) {
                    result = STATS_FEATURES.get(ci).getValue(stats);
                }
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            
            return result;
        }

        // Start ItemSelectable

        @Override
        public Object[] getSelectedObjects() {
            List<TraitInstance> sel = getCheckedTraitInstances();
            return sel.toArray(new TraitInstance[sel.size()]);
        }

        @Override
        public void addItemListener(ItemListener l) {
            listenerList.add(ItemListener.class, l);
        }

        @Override
        public void removeItemListener(ItemListener l) {
            listenerList.remove(ItemListener.class, l);
        }

        // End ItemSelectable
        
        protected void fireItemSelected(TraitInstance ti) {
            ItemEvent event = null;
            for (ItemListener l : listenerList.getListeners(ItemListener.class)) {
                if (event == null) {
                    event = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, ti, ItemEvent.SELECTED);
                }
                l.itemStateChanged(event);
            }
        }
        
        protected void fireItemDeselected(TraitInstance ti) {
            ItemEvent event = null;
            for (ItemListener l : listenerList.getListeners(ItemListener.class)) {
                if (event == null) {
                    event = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, ti, ItemEvent.DESELECTED);
                }
                l.itemStateChanged(event);
            }
        }

        @Override
        public TableModel getTableModel() {
            return this;
        }

        @Override
        public SimpleStatistics<?> getStatsAt(int rowIndex) {
            TraitInstance ti = showingInstances.get(rowIndex);
            return statsData.getStatistics(ti);
        }

        @Override
        public int getTraitInstanceNameColumnIndex() {
            return TRAIT_INSTANCE_NAME_COLUMN_INDEX;
        }

        @Override
        public Integer getViewColumnIndex() {
            return null; // We don't support the View? column
        }

        @Override
        public int getValRuleErrorColumnIndex() {
            return VALRULE_ERROR_COLUMN_INDEX;
        }

        @Override
        public Trait getTrait(int rowIndex) {
            TraitInstance ti = showingInstances.get(rowIndex);
            return ti.trait;
        }

        @Override
        public List<TraitInstance> getTraitInstancesWithData() {
            // We only have the instances with data !
            return Collections.unmodifiableList(showingInstances);
        }

        @Override
        public Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance() {
            Map<TraitInstance,SimpleStatistics<?>> result = new HashMap<TraitInstance,SimpleStatistics<?>>();
            for (TraitInstance ti : showingInstances) {
                SimpleStatistics<?> stats = statsData.getStatistics(ti);
                if (stats != null) {
                    result.put(ti, stats);
                }
            }
            return result;
        }

        @Override
        public Map<Feature, Integer> getFeatureToColumnIndex() {
            Map<Feature,Integer> result = new LinkedHashMap<Feature, Integer>();
            int nc = getColumnCount();
            for (int i = 0; i < nc; ++i) {
                Feature f = getFeatureForColumn(i);
                if (f!=null) {
                    result.put(f,  i);
                }
            }
            return result;
        }
        
        private Feature getFeatureForColumn(int columnIndex) {
            Feature result = null;
            if (columnIndex >= N_NON_FEATURE_COLUMNS) {
                int ci = columnIndex - N_NON_FEATURE_COLUMNS;
                result = STATS_FEATURES.get(ci);
            }
            return result;
        }


        @Override
        public void changeTraitInstanceChoice(boolean choiceAdded, TraitInstance[] choices) {
            if (choiceAdded) {
                if (showingInstances.isEmpty()) {
                    Collections.addAll(showingInstances, choices);
                    fireTableDataChanged();
                    for (TraitInstance ti : showingInstances) {
                        fireItemSelected(ti);
                    }
                }
                else {
                    for (TraitInstance ti : choices) {
                        if (! showingInstances.contains(ti)) {
                            int row = showingInstances.size();
                            showingInstances.add(ti);
                            fireTableRowsInserted(row, row);
                            fireItemSelected(ti);
                        }
                    }
                }
            }
            else {
                for (TraitInstance ti : choices) {  
                    int row = showingInstances.indexOf(ti);
                    if (row >= 0) {
                        showingInstances.remove(ti);
                        fireTableRowsDeleted(row, row);
                        fireItemDeselected(ti);
                    }
                }
            }
        }

        @Override
        public String getValidationExpressionAt(int rowIndex) {
            TraitInstance ti = showingInstances.get(rowIndex);
            return statsData.getValidationExpressionFor(ti);
        }

        @Override
        public List<TraitInstance> getCheckedTraitInstances() {
            return new ArrayList<>(showingInstances);
        }

        @Override
        public List<TraitInstance> getAllTraitInstances(boolean allElseOnlyChecked) {
            // We ONLY have checked instances
            return new ArrayList<>(showingInstances);
        }

        @Override
        public int getTraitInstanceDatatypeColumnIndex() {
            return TRAIT_INSTANCE_DATATYPE_COLUMN_INDEX;
        }

        @Override
        public String getInstanceHeading() {
            return HEADING_INSTANCE;
        }

        @Override
        public int[] getTraitInstanceColumnIndices() {
            return TRAIT_INSTANCE_COLUMN_INDICES;
        }        
    }
