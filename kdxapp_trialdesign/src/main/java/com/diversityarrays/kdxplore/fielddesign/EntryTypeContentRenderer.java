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
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.function.Function;

import com.diversityarrays.kdxplore.design.EntryType;

public class EntryTypeContentRenderer implements ContentRenderer<EntryType> {

    private Function<EntryType, Color> entryTypeColorProvider;

    public EntryTypeContentRenderer() {
        this(null);
    }

    public EntryTypeContentRenderer(Function<EntryType, Color> provider) {
        this.entryTypeColorProvider = provider;
    }

    @Override
    public void setContentColorProvider(Function<EntryType, Color> provider) {
        entryTypeColorProvider = provider;
    }

    @Override
    public Function<EntryType, Color> getContentColorProvider() {
        return entryTypeColorProvider;
    }

    @Override
    public void draw(Graphics2D g2d, 
            Composite transparent, 
            Point p, 
            EntryType entryType, 
            int cellWidth, 
            int cellHeight,
            boolean preserveColor)
    {
//        // convert to view offsets
//        Point vp = new Point(p.x * cellWidth, p.y * cellHeight);
//
        Color c;
        if (entryTypeColorProvider != null) {
            c = entryTypeColorProvider.apply(entryType);
        }
        else {
            int hc = entryType==null ? 0 : entryType.getName().hashCode();
            c = new Color(hc & 0x00ffffff);
        }

        Color save = preserveColor ? g2d.getColor() : null;
        g2d.setColor(c);
        g2d.fillRect(p.x, p.y, cellWidth, cellHeight);
        if (save != null) {
            g2d.setColor(save);
        }
    }
}
