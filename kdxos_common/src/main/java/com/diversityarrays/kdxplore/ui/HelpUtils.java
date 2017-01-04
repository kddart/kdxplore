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
package com.diversityarrays.kdxplore.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableModel;

import com.diversityarrays.util.Check;

import net.pearcan.io.IOUtil;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.table.BspAbstractTableModel;

public class HelpUtils {

    private static final int DEFAULT_MAX_VIEW = 10;

    static public Color PALE_YELLOW = Color.decode("#ffff66"); //$NON-NLS-1$

    static public JDialog getHelpDialog(
            Window owner, 
            String title,
            Class<?> resourceClass, String resourceName) 
    {
        JDialog helpDialog = new JDialog(owner,  title, ModalityType.MODELESS);

        helpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        String helpText = getHelpText(resourceClass, resourceName);

        JLabel label = new JLabel(helpText);
        label.setBackground(PALE_YELLOW);
        label.setOpaque(true);
        
        JScrollPane contentPane = new JScrollPane(label);

        contentPane.setBorder(
                BorderFactory.createCompoundBorder(new LineBorder(Color.BLUE),
                        new EmptyBorder(4, 4, 4, 4)));
        helpDialog.setContentPane(contentPane);

        helpDialog.pack();
        
        return helpDialog;
    }
    
    static public String getHelpText(Class<?> resourceClass, String resourceName) {
        String result = "Sorry - missing help resource: " + resourceName; //$NON-NLS-1$

        InputStream is = resourceClass.getResourceAsStream(resourceName);
        
        if (is != null) {
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            try {
                br = new BufferedReader(new InputStreamReader(is));
                String line;
                while (null != (line = br.readLine())) {
                    sb.append(line).append("\n"); //$NON-NLS-1$
                }
            }
            catch (IOException ignore) {
            }
            finally {
                IOUtil.closeQuietly(br);
            }
            result = sb.toString();
        }
        return result;
    }

    static public void askOptionPopup(
            ActionEvent actionEvent,
            IntConsumer choiceConsumer,
            String ... options)
    {
        askOptionPopup(actionEvent,
                null,
                choiceConsumer, options);
    }
    
    static public void askOptionPopup(
            ActionEvent actionEvent,
            String title,
            IntConsumer choiceConsumer,
            String ... options)
    {
        askOptionPopup(actionEvent, 20,20,
                title,
                choiceConsumer, options);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static public <T,U> JScrollPane makeTableInScrollPane(String heading1, String heading2, Collection<T> items, Function<T,U> namer) {
        
        List<T> list;
        if (items instanceof List) {
            list = (List) items;
        }
        else {
            list = new ArrayList<>(items);
        }
        
//        Map<T, U> map = list.stream().collect(Collectors.toMap(Function.identity(), namer));
        
        TableModel tableModel = new BspAbstractTableModel(heading1, heading2) {
            @Override
            public int getRowCount() {
                return list.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                T item = list.get(rowIndex);
                switch (columnIndex) {
                case 0: return item;
                case 1: return namer.apply(item);
                }
                // TODO Auto-generated method stub
                return null;
            }
        };
        
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        GuiUtil.setVisibleRowCount(table, DEFAULT_MAX_VIEW);
        
        return new JScrollPane(table);
    }

    static public <T,U> JScrollPane makeListInScrollPane(Collection<T> items, Function<T,U> namer) {
        return makeListInScrollPane(items, namer, DEFAULT_MAX_VIEW);
    }
    
    static public <T,U> JScrollPane makeListInScrollPane(Collection<T> items, Function<T,U> namer, int maxView) {
        DefaultListModel<U> listModel = new DefaultListModel<>();
        items.stream().forEach(item -> listModel.addElement(namer.apply(item)));
        JList<U> list = new JList<>(listModel);
        if (maxView > 0) {
            list.setVisibleRowCount(Math.min(maxView, listModel.size()));
        }
        return new JScrollPane(list);
    }

    static public void askOptionPopup(
            ActionEvent actionEvent,
            int x, int y,
            String title,
            IntConsumer choiceConsumer,
            String ... options)
    {        
        Component invoker = null;
        if (actionEvent.getSource() instanceof Component) {
            invoker = (Component) actionEvent.getSource();
        }
        askOptionPopup(invoker, x,y,
                title,
                choiceConsumer, options);
    }
    
    static public void askOptionPopup(Component invoker,
            String title,
            IntConsumer choiceConsumer,
            String ... options)
    {
        askOptionPopup(invoker,  20, 20,
                title,
                choiceConsumer, options);
    }

    static public void askOptionPopup(
            Component invoker, int x, int y,
            String title,
            IntConsumer choiceConsumer,
            String ... options)
    {
        JPopupMenu popupMenu = new JPopupMenu();
        if (! Check.isEmpty(title)) {
            popupMenu.add(title);
            popupMenu.addSeparator();
        }
        int index = -1;
        for (String opt : options) {
            final int choiceIndex = ++index;
            Action action = new AbstractAction(opt) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    choiceConsumer.accept(choiceIndex);
                }
            };
            popupMenu.add(action);
        }
        popupMenu.show(invoker, x, y);
    }
    
    private HelpUtils() {}
}
