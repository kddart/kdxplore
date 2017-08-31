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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.util.BooleanRenderer;
import com.diversityarrays.util.FilterTextField;
import com.diversityarrays.util.OkCancelDialog;

import net.pearcan.dnd.TableTransferHandler;

public class AddTraitsDialog extends OkCancelDialog {

    static List<Integer> parseInstanceNumbers(String spec) {
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

        private final AddTraitsTableModel tableModel;
	    private final JTable table;

	    private final SpinnerEditor spinnerEditor = new SpinnerEditor();
	    
	    private boolean cancelled = true;

	    private FilterTextField filterText = new FilterTextField(Msg.PROMPT_ENTER_FILTER_STRING());

	    private final JLabel selectedInfo = new JLabel();

	    public AddTraitsDialog(Window owner, Trial trial, Map<Trait,Integer> traits) {
	        super(owner, Msg.TITLE_ADD_TRAITS_TO_TRIAL(trial.getTrialName()));

	        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

	        tableModel = new AddTraitsTableModel(traits, TraitNameStyle.NO_INSTANCES != trial.getTraitNameStyle());
	        table = new JTable(tableModel);
	        table.setAutoCreateRowSorter(true);
	        table.setDefaultRenderer(Boolean.class, new BooleanRenderer());
	        table.setTransferHandler(TableTransferHandler.initialiseForCopySelectAll(table, true));

	        table.setDefaultEditor(Integer.class, spinnerEditor);
	        
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
