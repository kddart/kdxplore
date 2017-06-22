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
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.ui.table.BspAbstractTableModel;

public class ChangeTraitScoringOrderDialog extends JDialog {
    
    private final Action applyAction = new AbstractAction(UnicodeChars.CONFIRM_TICK) {
        @Override
        public void actionPerformed(ActionEvent e) {
            newTraitOrder = traits;
            dispose();
        }
    };

    private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
        @Override
        public void actionPerformed(ActionEvent e) {
            newTraitOrder = null;
            dispose();
        }
    };
    
    private final Action moveUpAction = new AbstractAction(UnicodeChars.TRIANGLE_UP) {
        @Override
        public void actionPerformed(ActionEvent e) {
            int rowUp = selectedRow - 1;
            Trait t = traits.remove(selectedRow);
            traits.add(rowUp, t);
            tableModel.fireTableRowsUpdated(rowUp, selectedRow);
            table.getSelectionModel().setSelectionInterval(rowUp, rowUp);
        }
    };

    private final Action moveDownAction = new AbstractAction(UnicodeChars.TRIANGLE_DN) {
        @Override
        public void actionPerformed(ActionEvent e) {
            int rowDown = selectedRow + 1;
            Trait t = traits.remove(rowDown);
            traits.add(selectedRow, t);
            tableModel.fireTableRowsUpdated(selectedRow, rowDown);
            table.getSelectionModel().setSelectionInterval(rowDown, rowDown);
        }
    };

    public List<Trait> newTraitOrder = null;
    
    private final List<Trait> traits;// = new LinkedList<>();
    
    private final BspAbstractTableModel tableModel = new BspAbstractTableModel("Order", "Trait") {

        @Override
        public int getRowCount() {
            return traits.size();
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
            case 0: return Integer.class;
            case 1: return String.class;
            }
            return Object.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0: return rowIndex+1;
            case 1: return traits.get(rowIndex).getTraitName();
            }
            return null;
        }
    };
    
    private final JTable table = new JTable(tableModel);

    private int selectedRow;
    
    public ChangeTraitScoringOrderDialog(Window owner, List<Trait> inputTraits) {
        super(owner, "Change Scoring Order", ModalityType.APPLICATION_MODAL);
        
        traits = new LinkedList<>(inputTraits);
//        for (Trait t : inputTraits) {
//            if (TraitDataType.CALC != t.getTraitDataType()) {
//                traits.add(t);
//            }
//        }
        
        KDClientUtils.initAction(ImageId.ARROW_UP, moveUpAction, "Move Trait Up");
        KDClientUtils.initAction(ImageId.ARROW_DOWN, moveDownAction, "Move Trait Down");
        
        table.setAutoCreateRowSorter(false);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    updateUpDownActions();
                }
            }
        });
        updateUpDownActions();
        
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(new JButton(cancelAction));
        box.add(new JButton(applyAction));

        Box side = Box.createVerticalBox();
//        side.add(Box.createVerticalGlue());
        side.add(new JButton(moveUpAction));
        side.add(new JButton(moveDownAction));
        side.add(Box.createVerticalGlue());
       
        Container cp = getContentPane();
        
        cp.add(new JScrollPane(table), BorderLayout.CENTER);
        cp.add(box, BorderLayout.SOUTH);
        
        cp.add(side, BorderLayout.EAST);
        pack();
    }

    private void updateUpDownActions() {
        selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            moveUpAction.setEnabled(selectedRow > 0);
            moveDownAction.setEnabled(selectedRow < tableModel.getRowCount()-1);
        }
        else {
            moveUpAction.setEnabled(false);
            moveDownAction.setEnabled(false);
        }
    }

}
