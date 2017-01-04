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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.function.Predicate;

public interface FieldModel {

    void addFieldModelListener(FieldModelListener l);
    void removeFieldModelListener(FieldModelListener l);

    void setColumnCount(int c);
    int getColumnCount();

    void setRowCount(int r);
    int getRowCount();

    Dimension getSize();

    int getFieldCellCount();
    /**
     * Clear the field cells that match the predicate.
     * @param predicate
     */
    void clearFieldCells(Predicate<FieldCell<?>> predicate);

    void toggleFieldCell(Point location);
    FieldCell<?> removeFieldCell(Point location);
    boolean addFieldCell(Point location);

    List<FieldCell<?>> getFieldCells();
    void visitFieldCells(Predicate<FieldCell<?>> visitor);
    void setColumnRowCount(int nCols, int nRows);


}
