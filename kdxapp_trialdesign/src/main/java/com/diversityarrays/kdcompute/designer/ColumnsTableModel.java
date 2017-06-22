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
package com.diversityarrays.kdcompute.designer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public abstract class ColumnsTableModel<T> extends AbstractTableModel implements Iterable<T> {
    
    protected String[] columnNames;

    protected final List<T> list = new ArrayList<>();
    
    public ColumnsTableModel(String ... columnNames) {
        this.columnNames = columnNames;
    }
    
    public void replaceWith(Collection<T> others) {
        list.clear();
        list.addAll(others);
        fireTableDataChanged();
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }
    
    public void add(T t) {
        int row = list.size();
        list.add(t);
        fireTableRowsInserted(row, row);
    }
    
    /**
     * Return a copy of the list.
     * @return
     */
    public List<T> getAll() {
        return new ArrayList<>(list);
    }
    
    public T get(int rowIndex) {
        return list.get(rowIndex);
    }
    
    @Override
    final public int getRowCount() {
        return list.size();
    }
    
    @Override
    final public int getColumnCount() {
        return columnNames.length;
    }
    @Override
    final public String getColumnName(int column) {
        return columnNames[column];
    }

    public void removeRows(List<Integer> modelRows) {            
        Collections.sort(modelRows, Collections.reverseOrder());
        for (Integer row : modelRows) {
            int rowIndex = row.intValue();
            list.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }
    
    
}
