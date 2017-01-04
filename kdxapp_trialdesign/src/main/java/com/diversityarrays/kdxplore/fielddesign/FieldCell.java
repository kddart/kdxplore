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

import java.awt.Point;

/**
 * An immutable object that store what is in each cell in a FieldView.
 * @author brianp
 *
 * @param <T> identifies the content type of the PlantingBlocks in the FieldView.
 */
public class FieldCell<T> {

    private final FieldCellType fieldCellType;
    /**
     * Note: do NOT store Point and/or return it in getLocation()
     * because Point is mutable!
     */
    private final int locationX;
    private final int locationY;

    /**
     * Content <i>should</i> be immutable - but we can't enforce that.
     */
    private final T content;

    public FieldCell(Point pt, T c, FieldCellType type) {
        this.content = c;
        this.locationX = pt.x;
        this.locationY = pt.y;
        this.fieldCellType = type;
    }

    /**
     * Return a new FieldCell with the location changed to
     * @param pt
     * @return
     */
    public FieldCell<T> moveTo(Point pt) {
        return new FieldCell<>(pt, content, fieldCellType);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if  (this==o) return true;
        if (! (o instanceof FieldCell)) return false;
        FieldCell<?> other = (FieldCell<?>) o;
        return (this.locationX == other.locationX)
                &&
                (this.locationY == other.locationY)
                &&
                (this.content.equals(other.content));
    }

    @Override
    public String toString() {
        return String.format("FieldCell[ (%d,%d) %s]", locationX, locationY, content.toString()); //$NON-NLS-1$
    }

    public FieldCellType getFieldCellType() {
        return fieldCellType;
    }

    public Point getLocation() {
        return new Point(locationX, locationY);
    }

    public T getContent() {
        return content;
    }
}
