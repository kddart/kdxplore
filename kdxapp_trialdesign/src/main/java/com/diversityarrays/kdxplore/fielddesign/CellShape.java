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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Specifies the shape to draw individual cells in a FieldView.
 * @author brianp
 *
 */
public enum CellShape {
    SQUARE,
    PORTRAIT,
    LANDSCAPE
    ;

    public Dimension getDimensionForSide(int side) {
        switch (this) {
        case LANDSCAPE:
            return new Dimension(side + side / 2, side);
        case PORTRAIT:
            return new Dimension(side, side + side / 2);
        case SQUARE:
        default:
            return new Dimension(side, side);
        }
    }

    public BufferedImage[] createImages(int wh, Color unselColor, Color selColor) {
        Point b_offset = new Point(0, 0);
        Point s_offset;
        Dimension size;
        int wh_inc = wh / 2;
        int of_inc = 3; // wh_inc / 2;// / 4;
        switch (this) {
        case LANDSCAPE:
            size = new Dimension(wh + wh_inc, wh);
            s_offset = new Point(0, 0);
            break;
        case PORTRAIT:
            size = new Dimension(wh, wh + wh_inc);
            s_offset = new Point(of_inc, 0);
            break;
        case SQUARE:
            size = new Dimension(wh, wh);
            s_offset = new Point(of_inc, 0);
            break;
        default:
            throw new RuntimeException("Unsupported: " + this.name()); //$NON-NLS-1$
        }

        Color base = Color.GRAY;
        Graphics g;

        int width = size.width;
        int height = size.height;

        BufferedImage unsel = new BufferedImage(width + s_offset.x, height + s_offset.y, BufferedImage.TYPE_4BYTE_ABGR);
        g = unsel.getGraphics();
        g.setColor(unselColor);
//        drawRaisedBevel(g, b_offset, width, height, base);
        g.drawRect(s_offset.x + 4, 4, width-5, height-5);
        g.dispose();

        BufferedImage sel = new BufferedImage(width + s_offset.x, height + s_offset.y, BufferedImage.TYPE_4BYTE_ABGR);
        g = sel.getGraphics();
        g.setColor(selColor);
//        drawLoweredBevel(g, b_offset, width, height, base);
        g.drawRect(s_offset.x + 4, 4, width-5, height-5);
        g.fillRect(s_offset.x + 4, 4, width-5, height-5);
        g.dispose();


        return new BufferedImage[] { unsel, sel };
    }

    static private void drawLoweredBevel(Graphics g,
            Point offset,
            int width, int height, Color base)
    {
        int x = offset.x;
        int y = offset.y;

        Color oldColor = g.getColor();
        int h = height;
        int w = width;

        g.translate(x, y);

        g.setColor(getHighlightOuterColor(base));
        g.drawLine(0, 0, 0, h-2);
        g.drawLine(1, 0, w-2, 0);

        g.setColor(getHighlightInnerColor(base));
        g.drawLine(1, 1, 1, h-3);
        g.drawLine(2, 1, w-3, 1);

        g.setColor(getShadowOuterColor(base));
        g.drawLine(0, h-1, w-1, h-1);
        g.drawLine(w-1, 0, w-1, h-2);

        g.setColor(getShadowInnerColor(base));
        g.drawLine(1, h-2, w-2, h-2);
        g.drawLine(w-2, 1, w-2, h-3);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    static private void drawRaisedBevel(Graphics g,
            Point offset,
            int width, int height, Color base)
    {
        int x = offset.x;
        int y = offset.y;

        Color oldColor = g.getColor();
        int h = height;
        int w = width;

        g.translate(x, y);

        g.setColor(getHighlightOuterColor(base));
        g.drawLine(0, 0, 0, h-2);
        g.drawLine(1, 0, w-2, 0);

        g.setColor(getHighlightInnerColor(base));
        g.drawLine(1, 1, 1, h-3);
        g.drawLine(2, 1, w-3, 1);

        g.setColor(getShadowOuterColor(base));
        g.drawLine(0, h-1, w-1, h-1);
        g.drawLine(w-1, 0, w-1, h-2);

        g.setColor(getShadowInnerColor(base));
        g.drawLine(1, h-2, w-2, h-2);
        g.drawLine(w-2, 1, w-2, h-3);

        g.translate(-x, -y);
        g.setColor(oldColor);

    }

    private static Color getShadowInnerColor(Color base) {
//        Color shadow = getShadowInnerColor();
        return base.darker();
    }

    private static Color getShadowOuterColor(Color base) {
//        Color shadow = Color.GRAY; //SHADOW_OUTER_COLOR;
        return base.darker().darker();
    }

    private static Color getHighlightInnerColor(Color base) {
        return base.brighter();
    }

    private static Color getHighlightOuterColor(Color base) {
        return base.brighter().brighter();
    }
}
