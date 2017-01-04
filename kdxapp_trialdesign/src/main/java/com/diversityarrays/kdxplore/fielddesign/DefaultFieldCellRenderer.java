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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.function.Function;

public class DefaultFieldCellRenderer implements FieldCellRenderer {


    private Function<FieldCell<?>, Color> colorProvider;

    public DefaultFieldCellRenderer() {
        this(null);
    }

    public DefaultFieldCellRenderer(Function<FieldCell<?>, Color> provider) {
        colorProvider = provider;
    }

    @Override
    public void draw(Graphics2D g2d, FieldView<?> view,
            FieldCell<?> cell,
            Point pt, int width, int height,
            boolean isSelected)
    {
        Color save = g2d.getColor();

        Color c = null;
        if (colorProvider != null) {
            c = colorProvider.apply(cell);
        }
        else {
            FieldCellType fct = cell.getFieldCellType();
            if (fct != null) {
                c = fct.defaultColor;
            }
        }

        if (c == null) {
            c = FieldCellType.ERROR_PLOT_COLOR;
        }
        g2d.setColor(c);

        g2d.fillRect(pt.x, pt.y, width, height);

        g2d.setColor(save);
    }
}
