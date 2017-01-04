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
package com.diversityarrays.kdxplore.design;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.autocomplete.ComboBoxCellEditor;

import net.pearcan.ui.GuiUtil;

@SuppressWarnings("nls")
public class HeadingRoleTable<T> extends JTable {

    private JPopupMenu popupMenu;
    private final Map<JCheckBoxMenuItem,T> roleByMenuItem = new HashMap<>();

    private final MouseListener mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e) && 1==e.getClickCount()) {
                List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(HeadingRoleTable.this);
                if (! selectedModelRows.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    HeadingRoleTableModel<T> htm = (HeadingRoleTableModel<T>) getModel();

                    if (popupMenu == null) {
                        popupMenu = new JPopupMenu("Choose");
                    }
                    popupMenu.removeAll();
                    roleByMenuItem.clear();

                    for (final T t : htm.getRoleValues()) {
                        Action action = new AbstractAction(t.toString()) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                htm.setRoleValueAt(selectedModelRows, t);
                            }
                        };
                        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(action);
                        roleByMenuItem.put(menuItem, t);
                        popupMenu.add(menuItem);
                    }

                    if (selectedModelRows.size()==1 || htm.getAllRowsHaveSameRole(selectedModelRows)) {
                        T role = htm.getRoleAt(selectedModelRows.get(0));
                        for (JCheckBoxMenuItem mi : roleByMenuItem.keySet()) {
                            T menuRole = roleByMenuItem.get(mi);
                            boolean b = role==menuRole;
                            mi.setSelected(b);
                        }
                    }
                    else {
                        for (JCheckBoxMenuItem mi : roleByMenuItem.keySet()) {
                            mi.setSelected(false);
                        }
                    }
                    Point pt = e.getPoint();
                    popupMenu.show(e.getComponent(), pt.x, pt.y);
                }   
            }

        }
    };

    public HeadingRoleTable(HeadingRoleTableModel<T> model) {
        super(model);

        JComboBox<T> comboBox = new JComboBox<>(model.getRoleValues());
        TableCellEditor editor = new ComboBoxCellEditor(comboBox);
        setDefaultEditor(model.getRoleClass(), editor);

        addMouseListener(mouseAdapter);
        //            getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        //                @Override
        //                public void valueChanged(ListSelectionEvent e) {
        //                    // TODO Auto-generated method stub
        //                    
        //                }
        //            });
    }

}
