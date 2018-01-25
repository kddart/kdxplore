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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.table.BspAbstractTableModel;

public class TagChoooser {

    class TagLabelTableModel extends BspAbstractTableModel {

        private final Set<Integer> chosen = new HashSet<>();
        private final List<String> tagLabels;
        private Set<Integer> initialChoices = Collections.emptySet();
        private final Map<String, Integer> countByTagLabel;

        TagLabelTableModel(Map<String,Integer> map) {
            super("Include?", "Tag", "Count");
            this.countByTagLabel = map;
            this.tagLabels = new ArrayList<>(countByTagLabel.keySet());
            Collections.sort(tagLabels);
        }

        @Override
        public int getRowCount() {
            return tagLabels.size();
        }

        @Override
        public Class<?> getColumnClass(int col) {
            switch (col) {
            case 0: return Boolean.class;
            case 1: return String.class;
            case 2: return Integer.class;
            }
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && aValue instanceof Boolean) {
                if ((Boolean) aValue) {
                    if (chosen.add(rowIndex)) {
                        fireTableRowsUpdated(rowIndex, rowIndex);
                        updateApplyAction();
                    }
                }
                else {
                    if (chosen.remove(rowIndex)) {
                        fireTableRowsUpdated(rowIndex, rowIndex);
                        updateApplyAction();
                    }
                }
            }
        }
        private void updateApplyAction() {
            applyAction.setEnabled(! initialChoices.equals(chosen));
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return chosen.contains(rowIndex);
            case 1:
                return tagLabels.get(rowIndex);
            case 2:
                return countByTagLabel.get(tagLabels.get(rowIndex));
            }
            return null;
        }

        public void setChosenUsing(Set<String> previousChoices) {
            chosen.clear();
            for (String label : previousChoices) {
                int rowIndex = tagLabels.indexOf(label);
                if (rowIndex >= 0) {
                    chosen.add(rowIndex);
                }
            }
            // Disable until any changes
            applyAction.setEnabled(false);
            
            initialChoices = new HashSet<>(chosen);
            fireTableDataChanged();
        }

        public Set<String> getChoices() {
            return chosen.stream()
                    .map(r -> tagLabels.get(r))
                    .collect(Collectors.toSet());
        }

        public void chooseAll() {
            for (int i = getRowCount(); --i >= 0; ) {
                chosen.add(i);
            }
            updateApplyAction();
            fireTableDataChanged();
        }

        public void chooseNone() {
            chosen.clear();
            updateApplyAction();
            fireTableDataChanged();
        }

        public void chooseSome(List<Integer> rows) {
            for (Integer r : rows) {
                chosen.add(r);
            }
            updateApplyAction();
            fireTableDataChanged();
        }
    }

    private final TagLabelTableModel ttm;
    private final JTable tagLabelTable;
    
    private final Action chooseAll = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            ttm.chooseAll();
        }  
    };
    private final Action chooseNone = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            ttm.chooseNone();
        }  
    };
    private final Action chooseSome = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            ttm.chooseSome(GuiUtil.getSelectedModelRows(tagLabelTable));
        }  
    };
    private final Action applyAction = new AbstractAction("Apply") {
        @Override
        public void actionPerformed(ActionEvent e) {
            popup.hide();
            Set<String> choices = ttm.getChoices();
            onApply.accept(choices);
        }
    };
    private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
        @Override
        public void actionPerformed(ActionEvent e) {
            popup.hide();
            onApply.accept(null);
        }
    };

    private final JScrollPane scrollPane;

    private final JPanel contents = new JPanel(new BorderLayout());

    private Popup popup;
    private Consumer<Set<String>> onApply;

    public TagChoooser(Map<String,Integer> countByTagLabel) {
        ttm = new TagLabelTableModel(countByTagLabel);
        tagLabelTable = new JTable(ttm);
        tagLabelTable.setAutoCreateRowSorter(true);
        scrollPane = new JScrollPane(tagLabelTable);
        GuiUtil.setVisibleRowCount(tagLabelTable, Math.min(10, ttm.getRowCount()));

        KDClientUtils.initAction(ImageId.CHECK_ALL_SQUARE, chooseAll, "Choose All");
        KDClientUtils.initAction(ImageId.UNCHECK_ALL_SQUARE, chooseNone, "Choose None");
        KDClientUtils.initAction(ImageId.CHECK_SELECTED, chooseSome, "Choose Selected");
        
        tagLabelTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                chooseSome.setEnabled(tagLabelTable.getSelectedRowCount() > 0);
            }
        });
        
        Box btns = Box.createHorizontalBox();
        btns.add(new JButton(chooseAll));
        btns.add(new JButton(chooseNone));
        btns.add(new JButton(chooseSome));
        btns.add(Box.createHorizontalGlue());
        btns.add(new JButton(cancelAction));
        btns.add(new JButton(applyAction));

        contents.add(GuiUtil.createLabelSeparator("Choose Tags to Show"), BorderLayout.NORTH);
        contents.add(scrollPane, BorderLayout.CENTER);
        contents.add(btns, BorderLayout.SOUTH);
    }

    // onApply is called with null if cancelled else the selected labels
    public void showAsPopup(Component owner, Set<String> previousChoices,
            Consumer<Set<String>> onApply) {
        this.onApply = onApply;
        ttm.setChosenUsing(previousChoices);
        chooseSome.setEnabled(false);
        Point pt = owner.getLocationOnScreen();
        popup = PopupFactory.getSharedInstance().getPopup(owner, contents, pt.x, pt.y);
        popup.show();
    }
}
