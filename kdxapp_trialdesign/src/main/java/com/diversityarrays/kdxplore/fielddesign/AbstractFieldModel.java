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

import javax.swing.event.EventListenerList;

abstract public class AbstractFieldModel implements FieldModel {

    protected final EventListenerList listenerList = new EventListenerList();

    protected void fireFieldDimensionChanged() {
        for (FieldModelListener l : listenerList.getListeners(FieldModelListener.class)) {
            l.fieldDimensionChanged(this);
        }
    }

    protected void fireBordersChanged(Point pt) {
        Point point = pt == null ? null : new Point(pt);
        for (FieldModelListener l : listenerList.getListeners(FieldModelListener.class)) {
            l.bordersChanged(this, point);
        }
    }

    @Override
    public void addFieldModelListener(FieldModelListener l) {
        listenerList.add(FieldModelListener.class, l);
    }

    @Override
    public void removeFieldModelListener(FieldModelListener l) {
        listenerList.remove(FieldModelListener.class, l);
    }
}
