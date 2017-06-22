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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.util.BooleanRenderer;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.FilterTextField;
import com.diversityarrays.util.OkCancelDialog;
import com.diversityarrays.util.TableColumnInfo;

import net.pearcan.dnd.TableTransferHandler;

public class AddTraitsDialog extends OkCancelDialog {

    static private List<Integer> parseInstanceNumbers(String spec) {
        Pattern minToMax = Pattern.compile("^([1-9][0-9]*)-([1-9][0-9]*)$"); //$NON-NLS-1$
        Pattern oneToMax = Pattern.compile("^-([1-9][0-9]*)$"); //$NON-NLS-1$
        Set<Integer> set = new HashSet<>();
        String[] specs = spec.split(","); //$NON-NLS-1$
        for (String s : specs) {
            s = s.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                Matcher m = minToMax.matcher(s);
                if (m.matches()) {
                    int min = Integer.parseInt(m.group(1));
                    int max = Integer.parseInt(m.group(2));
                    for (int i = min; i <= max; ++i) {
                        set.add(i);
                    }
                }
                else {
                    m = oneToMax.matcher(s);
                    if (m.matches()) {
                        int min = 1;
                        int max = Integer.parseInt(m.group(1));
                        for (int i = min; i <= max; ++i) {
                            set.add(i);
                        }
                    }
                    else {
                        set.add(Integer.valueOf(s));
                    }
                }
            }
            catch (NumberFormatException e) {
            }
        }
        if (set.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>(set);
        Collections.sort(result);
        return result;
    }

        static class AddTraitsTableModel extends AbstractTableModel {

	        private static final String ONE = "1"; //$NON-NLS-1$
            private final Set<Trait> chosen = new HashSet<>();
            private final Map<Trait, String> instancesToCreateByTrait = new HashMap<>();
            private final Map<Trait, List<Integer>> nInstanceTobeCreatedTrait = new TreeMap<>();

	        private List<Trait> allTraits;

	        private List<Trait> filteredTraits = new ArrayList<>();


            private final List<TableColumnInfo<Trait>> columnInfos = new ArrayList<>();

            private int editableInstanceNumbersColumnIndex = -1;
            private int instanceNumberListColumnIndex = -1;

	        AddTraitsTableModel(List<Trait> list, boolean showInstanceCount) {

	            allTraits = new ArrayList<>(list);
	            Collections.sort(allTraits);

	            String spec = ONE;
	            List<Integer> instanceNumbers = parseInstanceNumbers(spec);
	            for (Trait t : allTraits) {
                    instancesToCreateByTrait.put(t, spec);
                    nInstanceTobeCreatedTrait.put(t, instanceNumbers);
	            }


	            // NOTE: the "chosen" is assumed to be 0 by isCellEditable()
	            columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_ADD(), Boolean.class) {
                    @Override
                    public Object getColumnValue(int rowIndex, Trait t) {
                        return chosen.contains(t);
                    }
                });

	            if (showInstanceCount) {
                    editableInstanceNumbersColumnIndex = columnInfos.size();
                    columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_INSTANCE_NUMBERS(), String.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, Trait t) {
                            return instancesToCreateByTrait.get(t);
                        }
                    });

                    instanceNumberListColumnIndex = columnInfos.size();
                    columnInfos.add(new TableColumnInfo<Trait>(Msg.COLHDG_N_INSTANCES_TO_CREATE(), Integer.class) {
                        @Override
                        public Object getColumnValue(int rowIndex, Trait t) {
                            if (chosen.contains(t)) {
                                List<Integer> list = nInstanceTobeCreatedTrait.get(t);
                                return list==null ? 0 : list.size();
                            }
                            return null;
                        }
                    });
                };
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
	            return chosen.size();
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
                return columnIndex == 0 || columnIndex == editableInstanceNumbersColumnIndex;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (0 == columnIndex) {
                    // Changing "chosen"
                    if (aValue instanceof Boolean) {
                        Trait trait = filteredTraits.get(rowIndex);
                        if ((Boolean) aValue) {
                            chosen.add(trait);
                            fireTableCellUpdated(rowIndex, 0);
                            String spec = instancesToCreateByTrait.get(trait);
                            if (Check.isEmpty(spec)) {
                                spec = ONE;
                            }
                            List<Integer> list = parseInstanceNumbers(spec);
                            if (list.isEmpty()) {
                                spec = ONE;
                                list = parseInstanceNumbers(spec);
                            }
                            instancesToCreateByTrait.put(trait, spec);
                            nInstanceTobeCreatedTrait.put(trait, list);

                            if (editableInstanceNumbersColumnIndex >= 0) {
                                fireTableCellUpdated(rowIndex, editableInstanceNumbersColumnIndex);
                            }

                            if (instanceNumberListColumnIndex >= 0) {
                                fireTableCellUpdated(rowIndex, instanceNumberListColumnIndex);
                            }
                        }
                        else {
                            chosen.remove(trait);
                            fireTableCellUpdated(rowIndex, 0);
                        }
                    }
                }
                else if (editableInstanceNumbersColumnIndex == columnIndex) {
                    if (aValue instanceof String) {
                        String spec = (String) aValue;
                        Trait trait = filteredTraits.get(rowIndex);

                        List<Integer> instanceNumbers = parseInstanceNumbers(spec);
                        if (instanceNumbers.isEmpty()) {
                        }
                        else {
                            chosen.add(trait);
                            instancesToCreateByTrait.put(trait, spec);
                            nInstanceTobeCreatedTrait.put(trait, instanceNumbers);
                            fireTableCellUpdated(rowIndex, 0);
                            if (editableInstanceNumbersColumnIndex > 0) {
                                fireTableCellUpdated(rowIndex, editableInstanceNumbersColumnIndex);
                            }
                            if (instanceNumberListColumnIndex > 0) {
                                fireTableCellUpdated(rowIndex, instanceNumberListColumnIndex);
                            }
                        }
                    }
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
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Trait trait = filteredTraits.get(rowIndex);

                return columnInfos.get(columnIndex).getColumnValue(rowIndex, trait);
            }

            public Map<Trait, List<Integer>> getInstancesToCreateByTrait() {
                return chosen.stream().collect(
                        Collectors.toMap(Function.identity(), t -> nInstanceTobeCreatedTrait.get(t)));
            }
	    }

	    private final AddTraitsTableModel tableModel;
	    private final JTable table;

	    private boolean cancelled = true;

	    private FilterTextField filterText = new FilterTextField(Msg.PROMPT_ENTER_FILTER_STRING());

	    private final JLabel selectedInfo = new JLabel();

	    public AddTraitsDialog(Window owner, Trial trial, List<Trait> traits) {
	        super(owner, Msg.TITLE_ADD_TRAITS_TO_TRIAL(trial.getTrialName()));

	        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

	        tableModel = new AddTraitsTableModel(traits, TraitNameStyle.NO_INSTANCES != trial.getTraitNameStyle());
	        table = new JTable(tableModel);
	        table.setAutoCreateRowSorter(true);
	        table.setDefaultRenderer(Boolean.class, new BooleanRenderer());
	        table.setTransferHandler(TableTransferHandler.initialiseForCopySelectAll(table, true));

	        initialiseGui();

	        filterText.addFilterChangeHandler((s) -> applyFilter(s));

	        getOkAction().setEnabled(false);

	        tableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    int count = tableModel.getChosenCount();
                    getOkAction().setEnabled(count > 0);
                    selectedInfo.setText("Selected: " + count);
                }
            });

	        initialiseRenderer(tableModel.getInstanceNumberListColumnIndex(), Color.RED);
	        initialiseRenderer(tableModel.getEditableInstanceNumbersColumnIndex(), Color.BLUE);

	        setLocationRelativeTo(owner);
	    }

	    private void initialiseRenderer(int modelColumnIndex, Color color) {
	        if (modelColumnIndex < 0) {
	            return;
	        }

	        int viewColumnIndex = table.convertColumnIndexToView(modelColumnIndex);
	        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
	        renderer.setHorizontalAlignment(JLabel.CENTER);
	        if (color != null) {
	            renderer.setForeground(color);
	        }
	        table.getColumnModel().getColumn(viewColumnIndex).setCellRenderer(renderer);;
        }

        private void applyFilter(String text) {
	        tableModel.applyFilter(text);
        }

        public Map<Trait, List<Integer>> getTraitAndInstanceNumber() {
	        if (cancelled) {
	            return null;
	        }
	        return tableModel.getInstancesToCreateByTrait();
	    }

        @Override
        protected void constructContentPane() {
            super.constructContentPane();

            Box filterBox = Box.createHorizontalBox();
            filterBox.add(new JLabel(Msg.LABEL_FILTER()));
            filterBox.add(filterText);

            if (tableModel.getEditableInstanceNumbersColumnIndex() >= 0) {
                JLabel label = new JLabel(Msg.HTML_DOUBLE_CLICK_TO_CHANGE_INSTANCE_COUNT());
                label.setHorizontalAlignment(JLabel.LEFT);
                Box box = Box.createHorizontalBox();
                box.add(label);
                Box top = Box.createVerticalBox();
                top.add(filterBox);
                top.add(box);
                getContentPane().add(top, BorderLayout.NORTH);
            }
            else {
                getContentPane().add(filterBox, BorderLayout.NORTH);
            }
        }

        @Override
        protected void doPostOpenInitialisation() {
//            GuiUtil.initialiseTableColumnWidths(table);
            TableColumnModel tcm = table.getColumnModel();
            List<Integer> indices = tableModel.getColumnIndicesToResize();
            for (Integer cindex : indices) {
                TableColumn tc = tcm.getColumn(cindex);
                Object hv = tc.getHeaderValue();
                TableCellRenderer hr = tc.getCellRenderer();
                if (hr==null) {
                    hr = table.getTableHeader().getDefaultRenderer();
                }
                Component c = hr.getTableCellRendererComponent(table, hv, false, false, 0, cindex);
                int w = c.getPreferredSize().width;
                tc.setPreferredWidth(w);
            }
            super.doPostOpenInitialisation();
        }

        @Override
        protected void addExtraBottomComponents(Box box) {
            box.add(selectedInfo);
        }

        @Override
        protected Component createMainPanel() {
            return new JScrollPane(table);
        }

        @Override
        protected boolean handleCancelAction() {
            cancelled = true;
            return true;
        }

        @Override
        protected boolean handleOkAction() {
            cancelled = false;
            return true;
        }
	}
