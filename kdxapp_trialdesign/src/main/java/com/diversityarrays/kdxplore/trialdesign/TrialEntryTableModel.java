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
package com.diversityarrays.kdxplore.trialdesign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdxplore.design.EntryCountChangeListener;
import com.diversityarrays.kdxplore.design.EntryFactor;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.TableColumnInfo;

@SuppressWarnings("nls")
public class TrialEntryTableModel extends AbstractTableModel {

//    static abstract class EntryInfo<T> {
//        public final Class<T> columnClass;
//        public final String columnName;
//        EntryInfo(Class<T> cc, String cn) {
//            columnClass = cc;
//            columnName = cn;
//        }
//        abstract T getColumnValue(TrialEntry te);
//    }

    private TrialEntryFile entryFile;
    private List<TableColumnInfo<TrialEntry>> entryInfos = new ArrayList<>(); // BASIC_ENTRY_INFO);
    private final List<TrialEntry> entries = new ArrayList<>();
    private final List<EntryFactor> entryFactors = new ArrayList<>();

    private Integer plotTypeColumnIndex = null;

    public TrialEntryTableModel() {
    }

    public void setEntryFile(TrialEntryFile tef) {
        entryFile = tef;

        setEntries(tef.getEntries());

        entryFactors.clear();
        entryFactors.addAll(tef.getEntryFactors());
        plotTypeColumnIndex = null;

        fireTableStructureChanged();
        fireEntryCountChanged();
    }

    public List<TrialEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setEntries(List<TrialEntry> list) {
        entries.clear();
        if (list != null) {
            entries.addAll(list);
        }

        entryInfos = new ArrayList<>(); // BASIC_ENTRY_INFO);
//        entryInfos = new ArrayList<>(BASIC_ENTRY_INFO);

        Map<TrialHeading, String> userHeadings = entryFile.getUserHeadings();
        String entryIdHeading = userHeadings.get(TrialHeading.ENTRY_ID);
        if (Check.isEmpty(entryIdHeading)) {
            entryIdHeading = TrialHeading.ENTRY_ID.display;
        }
        entryInfos.add(new TableColumnInfo<TrialEntry>(entryIdHeading, Integer.class) {
            @Override
            public Object getColumnValue(int rowIndex, TrialEntry te) {
                return te.getEntryId();
            }
        });

        String plotTypeHeading = userHeadings.get(TrialHeading.ENTRY_TYPE);
        if (Check.isEmpty(plotTypeHeading)) {
            plotTypeColumnIndex = null;
        }
        else {
            plotTypeColumnIndex = entryInfos.size();

            entryInfos.add(new TableColumnInfo<TrialEntry>(plotTypeHeading, EntryType.class) {
                @Override
                public Object getColumnValue(int rowIndex, TrialEntry te) {
                    return te.getEntryType();
                }
            });
        }

        BiConsumer<TrialHeading, String> action = new BiConsumer<TrialHeading, String>() {
            @Override
            public void accept(TrialHeading th, String userHeading) {
                switch (th) {
                case DONT_USE:
                    break;
                case ENTRY_ID:
                    // already done (mandatory first column)
                    break;
                case ENTRY_TYPE:
                    // already done (optional second column)
                    break;
                case FACTOR:
                    // done separately
                    break;

                case ENTRY_NAME:
                    entryInfos.add(new TableColumnInfo<TrialEntry>(userHeading, String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, TrialEntry te) {
                            return te.getEntryName();
                        }
                    });
                    break;

                case EXPERIMENT:
                    entryInfos.add(new TableColumnInfo<TrialEntry>(userHeading, String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, TrialEntry te) {
                            return te.getExperimentName();
                        }
                    });
                    break;

                case LOCATION:
                    entryInfos.add(new TableColumnInfo<TrialEntry>(userHeading, String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, TrialEntry te) {
                            return te.getLocation();
                        }
                    });
                    break;

                case NESTING:
                    entryInfos.add(new TableColumnInfo<TrialEntry>(userHeading, String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, TrialEntry te) {
                            return te.getNesting();
                        }
                    });
                    break;

                default:
                    entryInfos.add(new TableColumnInfo<TrialEntry>(userHeading, String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, TrialEntry te) {
                            return "?unsupported TrialHeading: " + th;
                        }
                    });
                    break;
                }
            }
        };
        userHeadings.entrySet().stream()
            .forEach(e -> action.accept(e.getKey(), e.getValue()));

        fireTableStructureChanged();
        fireEntryCountChanged();
    }

    public void addEntryCountChangeListener(EntryCountChangeListener l) {
        listenerList.add(EntryCountChangeListener.class, l);
    }
    public void removeEntryCountChangeListener(EntryCountChangeListener l) {
        listenerList.remove(EntryCountChangeListener.class, l);
    }
    protected void fireEntryCountChanged() {
        for (EntryCountChangeListener l : listenerList.getListeners(EntryCountChangeListener.class)) {
            l.entryCountChanged(this);
        }
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }
    @Override
    public int getColumnCount() {
        return entryInfos.size() +
                (entryFile==null ? 0 : entryFile.getEntryFactorCount());
    }

    @Override
    public String getColumnName(int column) {
        if (column < entryInfos.size()) {
            return entryInfos.get(column).getColumnName();
        }
        if (entryFile==null) {
            return super.getColumnName(column);
        }

        int factorIndex = column - entryInfos.size();
        if (factorIndex < entryFile.getEntryFactorCount()) {
            EntryFactor ef = entryFile.getEntryFactor(factorIndex);
            return ef.toString();
        }
        return super.getColumnName(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex < entryInfos.size()) {
            return entryInfos.get(columnIndex).getColumnClass();
        }
        return String.class;
    }
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return plotTypeColumnIndex!=null && plotTypeColumnIndex.intValue()==columnIndex;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (plotTypeColumnIndex == null) {
            return;
        }
        if (plotTypeColumnIndex.intValue() == columnIndex) {
            return;
        }
        if (aValue instanceof EntryType) {
            EntryType et = (EntryType) aValue;
            TrialEntry te = entries.get(rowIndex);
            te.setEntryType(et);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TrialEntry te = entries.get(rowIndex);
        if (columnIndex < entryInfos.size()) {
            return entryInfos.get(columnIndex).getColumnValue(rowIndex, te);
        }

        if (entryFile==null) {
            return null;
        }
        int factorIndex = columnIndex - entryInfos.size();
        EntryFactor entryFactor = entryFile.getEntryFactor(factorIndex);
        Optional<String> opt = te.getFactorValue(entryFactor);
        return opt.orElse("");
    }

    public TrialEntry getEntryAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= entries.size()) {
            return null;
        }
        return entries.get(rowIndex);
    }

    public void removeEntriesAt(List<Integer> modelRows) {
        List<TrialEntry> list = modelRows.stream().map(r -> getEntryAt(r))
            .filter(e -> e != null)
            .collect(Collectors.toList());
        boolean anyRemoved = false;
        for (TrialEntry te : list) {
            int row = entries.indexOf(te);
            if (row >= 0) {
                anyRemoved = true;
                entries.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        if (anyRemoved) {
            fireEntryCountChanged();
        }
    }

}
