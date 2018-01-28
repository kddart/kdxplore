/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.ttools.shared;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.swing.JComponent;

import com.diversityarrays.util.Check;

@SuppressWarnings("nls")
public enum SampleIconType {
	NORMAL,
	
	// These are used for the Samples table
	RAW,
	DATABASE,
    CALCULATED,

    // These are used for FieldView
    INACTIVE_PLOT, 
	INACTIVE_PLOT_SELECTED;
    
    static private final Color DATABASE_BLUE = Color.decode("#00ddFF");
    
    static private final int EDGE_LEN = 11;
    static private final int BOX_HEIGHT = EDGE_LEN / 3 - 1;

    private static final Polygon WARNING_TRIANGLE;

//    public static final Polygon MARKER_TRIANGLE;

    static {
        int npoints = 3;
        int[] xpoints = new int[npoints];
        int[] ypoints = new int[npoints];
        xpoints[0] = EDGE_LEN/2; ypoints[0] = 0;
        xpoints[1] = 0;          ypoints[1] = EDGE_LEN;
        xpoints[2] = EDGE_LEN;   ypoints[2] = EDGE_LEN;
        WARNING_TRIANGLE = new Polygon(xpoints, ypoints, npoints);
    }
    
    public void draw(JComponent comp, Graphics g) {
        draw(comp, g, null);
    }
    
    public void draw(JComponent comp, Graphics g, String strForCalc) {
        switch (this) {
        case DATABASE:
            g.setColor(DATABASE_BLUE);
            g.fillRect(0, 0,           EDGE_LEN, BOX_HEIGHT);
            g.fillRect(0, BOX_HEIGHT+1, EDGE_LEN, BOX_HEIGHT);
            g.fillRect(0, BOX_HEIGHT+1 + BOX_HEIGHT+1, EDGE_LEN, BOX_HEIGHT);
            break;
        case RAW:
            g.setColor(Color.ORANGE);
            g.fillPolygon(WARNING_TRIANGLE);
            g.setColor(Color.BLACK);
            int x = EDGE_LEN / 2;
            g.drawLine(x, 2, x, EDGE_LEN-5);
            g.drawLine(x, EDGE_LEN-3, x, EDGE_LEN-1);
            x = x-1;
            g.drawLine(x, EDGE_LEN-2, x+2, EDGE_LEN-2);
            break;
        case INACTIVE_PLOT:
            g.setColor(Color.GREEN);
            g.setXORMode(Color.RED);
            drawDiagonals(g, comp);
            g.setPaintMode();
            break;
        case INACTIVE_PLOT_SELECTED:
            g.setColor(Color.GREEN);
            g.setXORMode(Color.RED);
            drawDiagonals(g, comp);
            g.setPaintMode();
            break;
        case CALCULATED:
            g.setColor(Color.DARK_GRAY);
            if (Check.isEmpty(strForCalc)) {
                g.fillRect(2, BOX_HEIGHT+1,               EDGE_LEN-2, BOX_HEIGHT);
                g.fillRect(2, BOX_HEIGHT+1 + BOX_HEIGHT+1, EDGE_LEN-2, BOX_HEIGHT);
            }
            else {
                FontMetrics fm = g.getFontMetrics();
                g.drawString(strForCalc, 0, fm.getMaxAscent());
            }
            break;
        case NORMAL:
            break;
        default:
            throw new RuntimeException("Unhandled SampleIconType: " + this); //$NON-NLS-1$
        }  
    }
    
    private void drawDiagonals(Graphics g, JComponent comp) {
        Rectangle r = comp.getBounds();
        int stripeGap = 16;
        // draw diagonals
        int ybot = 0 + r.height;
        for (int sx = 0; (sx+stripeGap) < r.width; sx += stripeGap) {
            g.drawLine(sx, ybot, sx+stripeGap, 0);
        }
    }
}
