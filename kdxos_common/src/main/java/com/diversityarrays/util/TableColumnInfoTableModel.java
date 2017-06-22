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
package com.diversityarrays.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.table.AbstractTableModel;

public class TableColumnInfoTableModel<T> extends AbstractTableModel {

    private final List<TableColumnInfo<T>> infos = new ArrayList<>();

    private final List<T> items = new ArrayList<>();
    public TableColumnInfoTableModel() { }

    public void setTableColumnInfo(Collection<TableColumnInfo<T>> coll) {
        infos.clear();
        if (coll != null) {
            infos.addAll(coll);
        }
        fireTableStructureChanged();
    }

    public void addTableColumnInfo(TableColumnInfo<T> tci) {
        infos.add(tci);
        fireTableStructureChanged();
    }

    public void replaceTableColumnInfo(int index, TableColumnInfo<T> tci) {
        infos.set(index, tci);
        fireTableStructureChanged();
    }

    public Optional<Integer> findTableColumnInfoIndex(Predicate<TableColumnInfo<T>> filter) {
        Integer result = null;
        for (int index = infos.size(); --index >= 0; ) {
            if (filter.test(infos.get(index))) {
                result = index;
                break;
            }
        }
        return Optional.ofNullable(result);
    }

    public void removeTableColumnInfo(TableColumnInfo<T> tci) {
        if (infos.remove(tci)) {
            fireTableStructureChanged();
        }
    }

    public TableColumnInfo<T> getTableColumnInfo(int columnIndex) {
        return infos.get(columnIndex);
    }

    public void setItems(Collection<T> coll) {
        items.clear();
        if (coll != null) {
            items.addAll(coll);
        }
        fireTableDataChanged();
    }

    public Stream<T> getItemStream() {
        return items.stream();
    }

    public T getItemAt(int rowIndex) {
        return items.get(rowIndex);
    }

    public int indexOfItem(T item) {
        return items.indexOf(item);
    }

    @Override
    public int getRowCount() {
        return items==null ? 0 : items.size();
    }

    @Override
    public int getColumnCount() {
        return infos==null ? 0 : infos.size();
    }

    @Override
    public String getColumnName(int column) {
        return infos.get(column).getColumnName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return infos.get(columnIndex).getColumnClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return infos.get(columnIndex).getColumnValue(rowIndex, getItemAt(rowIndex));
    }
}
