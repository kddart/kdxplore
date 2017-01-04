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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("nls")
public class DefaultFieldModel extends AbstractFieldModel {

    static public final int INITIAL_SIDE = 10;

    private int rowCount = INITIAL_SIDE;
    private int columnCount = INITIAL_SIDE;

    private Function<Point,FieldCell<?>> borderFactory;
    public DefaultFieldModel(Function<Point,FieldCell<?>> borderFactory) {
        if (borderFactory == null) {
            throw new IllegalArgumentException("borderFactory must not be null");
        }
        this.borderFactory = borderFactory;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public void setRowCount(int r) {
        if (r <= 0) {
            throw new IllegalArgumentException("rowCount must be greater than 0");
        }
        boolean changed = rowCount != r;
        this.rowCount = r;
        if (changed) {
            fireFieldDimensionChanged();
        }
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public void setColumnCount(int c) {
        if (c <= 0) {
            throw new IllegalArgumentException("columnCount must be greater than 0");
        }
        boolean changed = columnCount != c;
        this.columnCount = c;
        if (changed) {
            fireFieldDimensionChanged();
        }
    }

    @Override
    public Dimension getSize() {
        return new Dimension(columnCount, rowCount);
    }

    @Override
    public void setColumnRowCount(int c, int r) {
        if (c <= 0) {
            throw new IllegalArgumentException("columnCount must be greater than 0");
        }
        if (r <= 0) {
            throw new IllegalArgumentException("rowCount must be greater than 0");
        }
        if (columnCount != c || rowCount != r) {
            this.columnCount = c;
            this.rowCount = r;
            fireFieldDimensionChanged();
        }
    }

    private final Map<Point, FieldCell<?>> fieldCellsByLocation = new HashMap<>();
    @Override
    public int getFieldCellCount() {
        return fieldCellsByLocation.size();
    }

    @Override
    public List<FieldCell<?>> getFieldCells() {
        return new ArrayList<>(fieldCellsByLocation.values());
    }

    @Override
    public void visitFieldCells(Predicate<FieldCell<?>> visitor) {
        for (FieldCell<?> cell : fieldCellsByLocation.values()) {
            if (! visitor.test(cell)) {
                break;
            }
        }
    }

    @Override
    public void clearFieldCells(Predicate<FieldCell<?>> predicate) {
        if (predicate == null) {
            fieldCellsByLocation.clear();
        }
        else {
            Map<Point, FieldCell<?>> map = fieldCellsByLocation.entrySet().stream()
                    .filter(e -> ! predicate.test(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            fieldCellsByLocation.clear();
            fieldCellsByLocation.putAll(map);
        }
        fireBordersChanged(null);
    }

    @Override
    public void toggleFieldCell(Point location) {
        if (removeFieldCell(location, false) != null) {
            fireBordersChanged(location);
        }
        else {
            // No cell was at that location, put one there
            addFieldCell(location, true);
        }
    }

    @Override
    public FieldCell<?> removeFieldCell(Point b) {
        return removeFieldCell(b, true);
    }

    protected FieldCell<?> removeFieldCell(Point pt, boolean fire) {
        FieldCell<?> removed = fieldCellsByLocation.remove(pt);
        if (fire && removed != null) {
            fireBordersChanged(pt);
        }
        return removed;
    }

    public void addFieldCell(FieldCell<?> cell) {
        Point cellLocation = cell.getLocation();
        FieldCell<?> oldCell = fieldCellsByLocation.get(cellLocation);
        if (oldCell == null) {
            fieldCellsByLocation.put(cellLocation, cell);
            fireBordersChanged(cellLocation);
        }
        else {

        }
    }

    @Override
    public boolean addFieldCell(Point location) {
        return addFieldCell(location, true);
    }

    protected boolean addFieldCell(Point location, boolean fire) {

        FieldCell<?> oldCell = fieldCellsByLocation.get(location);
        // If already have something there don't do anything.
        if (oldCell == null) {
            // a brand new one
            FieldCell<?> newCell = borderFactory.apply(location);
            if (newCell != null) {
                // only if the factory was successful
                fieldCellsByLocation.put(location, newCell);
                if (fire) {
                    fireBordersChanged(location);
                }
                return true;
            }
            System.err.println("DefaultFieldModel.addBorder: factory returned null");
        }
        return false;
    }

}
