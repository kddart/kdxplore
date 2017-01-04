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

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import javax.swing.table.AbstractTableModel;

public class DataPreviewTableModel extends AbstractTableModel {

    private static final int INITIAL_COLUMN_COUNT = 4;

    int columnCount = INITIAL_COLUMN_COUNT;
    List<String[]> rowsOfCellValues = new ArrayList<>();
    
    private boolean useLettersForColumnNames;
    
    public DataPreviewTableModel() {
        this(false);
    }

    public DataPreviewTableModel(boolean useLetters) {
        this.useLettersForColumnNames = useLetters;
    }
    
    public void setUseLettersForColumnNames(boolean b) {
        this.useLettersForColumnNames = b;
        fireTableStructureChanged();
    }

    public void clear() {
        rowsOfCellValues.clear();
        columnCount = INITIAL_COLUMN_COUNT;
        fireTableDataChanged();
    }
    
    public void setData(List<String[]> data) {
        rowsOfCellValues.clear();
        rowsOfCellValues.addAll(data);
        OptionalInt opt = rowsOfCellValues.stream()
            .mapToInt(r -> r.length)
            .max();
        if (opt.isPresent()) {
            columnCount = Math.max(INITIAL_COLUMN_COUNT, opt.getAsInt());
        }
        else {
            columnCount = INITIAL_COLUMN_COUNT;
        }
        fireTableStructureChanged();
    }
    
    @Override
    public int getRowCount() {
        return rowsOfCellValues.size();
    }

    @Override
    public int getColumnCount() {
        return columnCount + 1;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "__"; //$NON-NLS-1$ // TODO i18n
        }
        if (useLettersForColumnNames) {
            return super.getColumnName(column-1);
        }
        return Integer.toString(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex==0) {
            return Integer.class;
        }
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex==0) {
            return rowIndex+1;
        }
        
        String[] cellValues = rowsOfCellValues.get(rowIndex);
        int index = columnIndex - 1;
        if (cellValues!=null && index < cellValues.length) {
            return cellValues[index];
        }
        return null;
    }
}
