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

import java.util.function.Function;

import javax.swing.table.DefaultTableCellRenderer;

public class OverrideCellRenderer<T> extends DefaultTableCellRenderer {
    public enum Horizontal {
        LEFT(DefaultTableCellRenderer.LEFT),
        CENTER(DefaultTableCellRenderer.CENTER),
        RIGHT(DefaultTableCellRenderer.RIGHT);

        final int alignment;
        Horizontal(int c) {
            alignment = c;
        }
    }
    public enum Vertical {
        TOP(DefaultTableCellRenderer.NORTH),
        MIDDLE(DefaultTableCellRenderer.CENTER),
        BOTTOM(DefaultTableCellRenderer.SOUTH),
        ;
        final int alignment;
        Vertical(int c) {
            alignment = c;
        }
    }
    private final Function<T, String> valueTransform;
    private final Class<? extends T> valueClass;

    public OverrideCellRenderer(Class<? extends T> tclass, Function<T,String> valueTransform) {
        this(Horizontal.CENTER, tclass, valueTransform);
    }

    public OverrideCellRenderer(OverrideCellRenderer.Horizontal horz, Class<? extends T> tclass, Function<T,String> valueTransform) {
        super();
        setHorizontalAlignment(horz.alignment);
        this.valueClass = tclass;
        this.valueTransform = valueTransform;
    }
    @Override
    protected void setValue(Object value) {
        if (value != null && valueClass.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T t = (T) value;
            setText(valueTransform.apply(t));
            return;
        }
        // TODO Auto-generated method stub
        super.setValue(value);
    }


}
