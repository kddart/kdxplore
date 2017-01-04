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
package com.diversityarrays.kdxplore.prefs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.l2fprod.common.swing.renderer.ColorCellRenderer;

// TODO try this instead of PreferenceTreePanel
public class PreferenceTreeTablePanel extends JPanel {

    static public class PrefNode {

        final String name;
        final Map<String,PrefNode> nodeByName = new HashMap<>();
        final List<PrefNode> children = new ArrayList<>();
        public KdxPreference<?> preference;
        
        PrefNode(String n) {
            this.name = n;
        }
        
        public void addChild(PrefNode child) {
            if (nodeByName.containsKey(child.name)) {
                throw new RuntimeException("Coding error"); //$NON-NLS-1$
            }
            nodeByName.put(child.name, child);
            children.add(child);
        }
        
        
        static public PrefNode buildPrefNodeTree(KdxPreference<?>[] preferences) {
            
            PrefNode root = new PrefNode("root"); //$NON-NLS-1$
            
            Set<String> paths = new HashSet<>();
            
            KdxplorePreferences kdxPrefs = KdxplorePreferences.getInstance();

            for (PreferenceCollection prefcoll : kdxPrefs.getPreferenceCollections()) {
                for (KdxPreference<?> pref : prefcoll.getKdxPreferences()) {
                    String[] keyParts = pref.key.split("/"); //$NON-NLS-1$                    
                    if (paths.add(pref.key)) {
                        List<String> path = new ArrayList<>();
                        for (String kp : keyParts) {
                            String name = KdxplorePreferences.BRANCH_NAME_BY_PATH_COMPONENT.get(kp);
                            if (name == null) {
                                name = kp.toUpperCase();
                            }
                            path.add(name);
                        }
                        
                        PrefNode findNode = findPrefNode(root, path);
                        if (findNode.preference != null) {
                            throw new RuntimeException("Already an object at " + pref.key+ ": "+ findNode.preference); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        findNode.preference = pref;
                    }
                    else {
                        throw new RuntimeException("Duplicate preference key: " + pref.key); //$NON-NLS-1$
                    }
                }
            }
//            for (KdxPreference<?> pref : preferences) {
//                String[] keyParts = pref.key.split("/"); //$NON-NLS-1$
//                if (! kdxPrefs.isSectionSupported(keyParts[0])) {
//                    continue;
//                }
//                
//                if (paths.add(pref.key)) {
//                    List<String> path = new ArrayList<>();
//                    for (String kp : keyParts) {
//                        String name = KdxplorePreferences.BRANCH_NAME_BY_PATH_COMPONENT.get(kp);
//                        if (name == null) {
//                            name = kp.toUpperCase();
//                        }
//                        path.add(name);
//                    }
//                    
//                    PrefNode findNode = findPrefNode(root, path);
//                    if (findNode.preference != null) {
//                        throw new RuntimeException("Already an object at " + pref.key+ ": "+ findNode.preference); //$NON-NLS-1$ //$NON-NLS-2$
//                    }
//                    findNode.preference = pref;
//                }
//                else {
//                    throw new RuntimeException("Duplicate preference key: " + pref.key); //$NON-NLS-1$
//                }
//            }
            
            return root;
        }
        
        static private PrefNode findPrefNode(PrefNode node, List<String> path) {
            String lookingFor = path.get(0);
            

            PrefNode found = null;
            for (PrefNode child : node.children) {
                if (lookingFor.equals(child.name)) {
                    found = child;
                    break;
                }
            }
            
            if (found == null) {
                // Need to create a new one
                switch (path.size()) {
                case 0:
                    throw new IllegalStateException("Coding error"); //$NON-NLS-1$
                case 1:
                    // doing it now!
                    PrefNode newNode = new PrefNode(lookingFor);
                    node.addChild(newNode);
                    return newNode;
                case 2:
                    found = new PrefNode(lookingFor);
                    node.addChild(found);
                    break;
                }
            }
            else {
                if (path.size() == 1) {
                    return found;
                }
            }
            return findPrefNode(found, path.subList(1, path.size()));

        }


        
    }
	
	static class PrefsTreeTableModel extends AbstractTreeTableModel {

		private final static String[] COLUMN_NAMES = {
			Msg.COLHDG_PREFERENCE(), Msg.COLHDG_VALUE()
		};

		private final KdxPreference<?>[] preferences;

		PrefsTreeTableModel() {
		    List<KdxPreference<?>> prefs = KdxplorePreferences.getInstance().getKdxPreferences();
			preferences = prefs.toArray(new KdxPreference<?>[prefs.size()]);
			root = PrefNode.buildPrefNodeTree(preferences);
		}
		
		@Override
		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

//		@Override
//		public Class<?> getColumnClass(int column) {
//			// TODO Auto-generated method stub
//			return super.getColumnClass(column);
//		}

		@Override
		public String getColumnName(int column) {
			return COLUMN_NAMES[column];
		}

		@Override
		public boolean isCellEditable(Object obj, int column) {
			PrefNode node = (PrefNode) obj;
			if (node.preference==null) {
				return false;
			}
			if (Number.class.isAssignableFrom(node.preference.valueClass)) {
				return true;
			}
			if (Boolean.class.isAssignableFrom(node.preference.valueClass)) {
				return true;
			}
			return false;
		}
		
		

		@Override
		public Object getValueAt(Object obj, int column) {
			PrefNode node = (PrefNode) obj;
			if (node.preference == null) {
				switch (column) {
				case 0:
					return node.name;
				case 1:
					return Msg.NODENAME_OPTIONS_COUNT(node.children.size());
				}
			}
			else {
				switch (column) {
				case 0:
					return Msg.getMessageIdText(node.preference.messageId);
				case 1:
					return KdxplorePreferences.getInstance().getPreferenceValue(node.preference);
				}
			}
			return null;
		}

		@Override
		public Object getChild(Object parent, int index) {
			if (parent instanceof PrefNode) {
				return ((PrefNode) parent).children.get(index);
			}
			return null;
		}

		@Override
		public int getChildCount(Object parent) {
			if (parent instanceof PrefNode) {
				return ((PrefNode) parent).children.size();
			}
			return 0;
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			PrefNode node = (PrefNode) parent;
			return node.children.indexOf(child);
		}

		@Override
		public boolean isLeaf(Object node) {
			return super.isLeaf(node);
		}		
	}

    public static final boolean DEBUG = Boolean.getBoolean("PreferenceTreeTablePanel.DEBUG"); //$NON-NLS-1$

	// This uses PrefNode
	private TreeTableModel treeTableModel = new PrefsTreeTableModel();
	private final JXTreeTable treeTable = new JXTreeTable(treeTableModel);
	private PrefCellRenderer cellRenderer = new PrefCellRenderer();
	private TableCellEditor cellEditor = new PrefCellEditor();
	

	PreferenceTreeTablePanel() {
		super(new BorderLayout());
		
		treeTable.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);
		treeTable.getColumnModel().getColumn(1).setCellEditor(cellEditor);
		add(new JScrollPane(treeTable), BorderLayout.CENTER);

	}
	



	class PrefCellEditor implements TableCellEditor {
		
		@SuppressWarnings("rawtypes")
        private JComboBox comboBox = new JComboBox();
		private JCheckBox checkBox = new JCheckBox();
		private JTextField textField = new JTextField();
		
		private DefaultCellEditor combo = new DefaultCellEditor(comboBox);
		private DefaultCellEditor check = new DefaultCellEditor(checkBox);
		private DefaultCellEditor text = new DefaultCellEditor(textField);
		
		DefaultCellEditor current;
		
		private EventListenerList listenerList = new EventListenerList();
		private CellEditorListener cellEditorListener = new CellEditorListener() {
			
			@Override
			public void editingStopped(ChangeEvent e) {
				for (CellEditorListener l : listenerList.getListeners(CellEditorListener.class)) {
					l.editingStopped(e);
				}
			}
			
			@Override
			public void editingCanceled(ChangeEvent e) {
				for (CellEditorListener l : listenerList.getListeners(CellEditorListener.class)) {
					l.editingCanceled(e);
				}
			}
		};
		
		PrefCellEditor() {
			combo.addCellEditorListener(cellEditorListener);
			check.addCellEditorListener(cellEditorListener);
			text.addCellEditorListener(cellEditorListener);

			if (DEBUG) {
	            System.out.println("combo=" + combo); //$NON-NLS-1$
	            System.out.println("check=" + check); //$NON-NLS-1$
	            System.out.println("text=" + text); //$NON-NLS-1$
			}			
		}
		
		private String getWho(Object src) {
			String result = src.toString();
			if (src == combo) {
				result = "combo"; //$NON-NLS-1$
			}
			else if (src==check) {
				result = "check"; //$NON-NLS-1$
			}
			else if (src==text) {
				result = "text"; //$NON-NLS-1$
			}
			return result;
		}

		@Override
		public Object getCellEditorValue() {
			Object result = current.getCellEditorValue();
			if (DEBUG) {
			    System.out.println(getWho(current) + ".getCellEditorValue=" + result); //$NON-NLS-1$
			}
			return result;
		}

		@Override
		public boolean isCellEditable(EventObject anEvent) {
			boolean result = false;
			Class<?> vc = null;
			if (anEvent instanceof MouseEvent) {
				MouseEvent me = (MouseEvent) anEvent;
				int rowAtPoint = treeTable.rowAtPoint(me.getPoint());
				if  (rowAtPoint >= 0) {
					TreePath path = treeTable.getPathForRow(rowAtPoint);
					Object lpc = path.getLastPathComponent();
					if (lpc instanceof PrefNode) {
						PrefNode prefNode = (PrefNode) lpc;
						if (prefNode.preference != null) {
							vc = prefNode.preference.valueClass;
							if  (Boolean.class == vc) {
								result = true;
							}
						}
					}
				}
			}
			return result;
		}

		@Override
		public boolean shouldSelectCell(EventObject anEvent) {
			boolean result = current.shouldSelectCell(anEvent);
			if (DEBUG) {
			    System.out.println(getWho(current) + ".shouldSelectCell=" + result); //$NON-NLS-1$
			}
			return result;
		}

		@Override
		public boolean stopCellEditing() {
			boolean result = current.stopCellEditing();
			if (DEBUG) {
			    System.out.println(getWho(current) + ".stopCellEditing=" + result); //$NON-NLS-1$
			}
			return result;
		}

		@Override
		public void cancelCellEditing() {
			if (DEBUG) {
			    System.out.println(getWho(current) + ".cancelCellEditing"); //$NON-NLS-1$
			}
			current.cancelCellEditing();
		}

		
		@Override
		public void addCellEditorListener(CellEditorListener l) {
			listenerList.add(CellEditorListener.class, l);
		}

		@Override
		public void removeCellEditorListener(CellEditorListener l) {
			listenerList.remove(CellEditorListener.class, l);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) 
		{
			if (value == null) {
				throw new RuntimeException("NULL value"); //$NON-NLS-1$
			}
			
			if (! (table instanceof JXTreeTable)) {
				throw new RuntimeException("Unsupported table class=" + table.getClass().getName()); //$NON-NLS-1$
			}
			JXTreeTable treeTable = (JXTreeTable) table;
			
			int modelRow = table.convertRowIndexToModel(row);
			int modelColumn = table.convertColumnIndexToModel(column);
			
			TreePath pathForRow = treeTable.getPathForRow(row);
			Object lpc = pathForRow.getLastPathComponent();
			if (! (lpc instanceof PrefNode)) {
				throw new RuntimeException("Should not be here"); //$NON-NLS-1$
			}
			PrefNode prefNode = (PrefNode) lpc;

			System.out.println(prefNode.preference);
			
			
			if (value instanceof Boolean) {
				checkBox.setSelected((Boolean) value);
				current = check;
			}
			else if (value instanceof Enum) {
                Class c = ((Enum) value).getDeclaringClass();
				Object[] enumConstants = c.getEnumConstants();
				comboBox.setModel(new DefaultComboBoxModel(enumConstants));
				comboBox.setSelectedItem(value);
				current = combo;
				return combo.getTableCellEditorComponent(treeTable, value, isSelected, modelRow, modelColumn);
			}
			else {
				textField.setText(value.toString());
				current = text;
			}
			if (DEBUG) {
			    System.out.println("getTableCellEditorComponent:  " + getWho(current)); //$NON-NLS-1$
			}
			return current.getTableCellEditorComponent(treeTable, value, isSelected, modelRow, modelColumn);
		}
	}
	
	class PrefCellRenderer extends DefaultTableCellRenderer {
		
		ColorCellRenderer colorCellRenderer = new ColorCellRenderer();

		PrefCellRenderer() {
			setHorizontalAlignment(CENTER);
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{			
			if (value != null) {
				if (value instanceof Color) {
					return colorCellRenderer.getTableCellRendererComponent
							(table, value, isSelected, hasFocus, row, column);
				}
			}
			
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);
		}
		
	}
}
