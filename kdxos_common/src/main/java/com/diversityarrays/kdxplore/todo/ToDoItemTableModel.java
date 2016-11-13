/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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
package com.diversityarrays.kdxplore.todo;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.pearcan.reflect.ExhibitBeanInfo;

public class ToDoItemTableModel extends AbstractTableModel {
	
	private static final Comparator<TodoItem> PRIORITISED_SEARCH_COMPARATOR = new Comparator<TodoItem>() {
		@Override
		public int compare(TodoItem o1, TodoItem o2) {
			int diff = o1.getPriority().compareTo(o2.getPriority());
			if (diff==0) {
				diff = o1.getName().compareTo(o2.getName());
				if (diff==0) {
					diff = o1.getDescription().compareTo(o2.getDescription());
					if (diff==0) {
						diff = o1.getSeq().compareTo(o2.getSeq());
					}
				}
			}
			return diff;
		}
	};
	
	private static final Comparator<TodoItem> UNPRIORITISED_SEARCH_COMPARATOR = new Comparator<TodoItem>() {
		@Override
		public int compare(TodoItem o1, TodoItem o2) {
			int diff = o1.getName().compareTo(o2.getName());
			if (diff==0) {
				diff = o1.getDescription().compareTo(o2.getDescription());
				if (diff==0) {
					diff = o1.getSeq().compareTo(o2.getSeq());
				}
			}
			return diff;
		}
	};
	
	static private ExhibitBeanInfo BEAN_INFO = new ExhibitBeanInfo(TodoItem.class, null, null);
	
//	static public ToDoItemPrioritisedTableModel create(boolean usePriority) {
//		
//	}

	private List<TodoItem> items = new ArrayList<TodoItem>();

	private Comparator<TodoItem> searchComparator;

	private final PropertyDescriptor[] propertyDescriptors;
	private final int doneIndex;
	
	public ToDoItemTableModel(boolean usePriority) {
		searchComparator = usePriority ? PRIORITISED_SEARCH_COMPARATOR : UNPRIORITISED_SEARCH_COMPARATOR;
		List<PropertyDescriptor> pdlist = new ArrayList<PropertyDescriptor>();
		
		int di = -1;
		
		for (PropertyDescriptor pd : BEAN_INFO.getPropertyDescriptors()) {
			if (TodoItem.DISPLAY_NAME_DONE.equals(pd.getDisplayName())) {
				di = pdlist.size();
			}

			if (usePriority || ! TodoItem.DISPLAY_NAME_PRIORITY.equals(pd.getDisplayName())) {
				pdlist.add(pd);
			}
			
		}
		
		if (di < 0) {
			throw new RuntimeException("Didn't find a property with display name '"+TodoItem.DISPLAY_NAME_DONE+"'");
		}
		doneIndex = di;
		
		this.propertyDescriptors = pdlist.toArray(new PropertyDescriptor[pdlist.size()]);
	}
	

	public void clear() {
		items.clear();
		fireTableDataChanged();
	}

	
	public TodoItem removeTodoItem(Object id) {
		int rowIndex = -1;
		for (int r = items.size(); --r >= 0; ) {
			if (items.get(r).getIdentifier().equals(id)) {
				rowIndex = r;
				break;
			}
		}
		
		TodoItem result = null;
		if (rowIndex >= 0) {
			result = items.remove(rowIndex);
			fireTableRowsDeleted(rowIndex, rowIndex);
		}
		return result;
	}
	
	public void addToDoItemListener(ToDoItemListener l) {
		listenerList.add(ToDoItemListener.class, l);
	}
	
	public void removeToDoItemListener(ToDoItemListener l) {
		listenerList.remove(ToDoItemListener.class, l);
	}
	
	protected void fireToDoItemRemoved(TodoItem item) {
		for (ToDoItemListener l : listenerList.getListeners(ToDoItemListener.class)) {
			l.toDoItemRemoved(this, item);
		}
	}

	@Override
	public int getRowCount() {
		return items.size();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex==doneIndex;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex==doneIndex && aValue instanceof Boolean) {
			TodoItem removedItem = items.remove(rowIndex);
			fireTableRowsDeleted(rowIndex, rowIndex);
			fireToDoItemRemoved(removedItem);
		}
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return propertyDescriptors[col].getPropertyType();
	}
	
	@Override
	public String getColumnName(int column) {
		return propertyDescriptors[column].getDisplayName();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		PropertyDescriptor pd = propertyDescriptors[columnIndex];
		TodoItem item = items.get(rowIndex);
		
		try {
			return pd.getReadMethod().invoke(item);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addItem(TodoItem item) {
		int pos = Collections.binarySearch(items, item, searchComparator);
		if (pos < 0) {
			pos = - (pos + 1);
		}
		items.add(pos, item);
		fireTableRowsInserted(pos, pos);
	}

	@Override
	public int getColumnCount() {
		return propertyDescriptors.length;
	}

}
