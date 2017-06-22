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

abstract public class TableColumnInfo<T> {
    private final String columnName;
    private final Class<?> columnClass;

    public TableColumnInfo(String hdg, Class<?> cls) {
        this.columnName = hdg;
        this.columnClass = cls;
    }

    @Override
    public String toString() { // for debugging
    	return "TCI[" + columnName + ": " + columnClass.getSimpleName() + "]";
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getColumnClass() {
        return columnClass;
    }

    public boolean isEditable(int rowIndex) {
        return false;
    }

    public void setColumnValue(int rowIndex, T t, Object aValue) {
        throw new UnsupportedOperationException();
    }

    abstract public Object getColumnValue(int rowIndex, T t);
}
