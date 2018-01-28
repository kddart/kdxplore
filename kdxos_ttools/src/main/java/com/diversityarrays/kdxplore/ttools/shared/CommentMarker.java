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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * Determines what colour triangle is drawn in the TRC of a Table cell.
 * @author brianp
 *
 */
@SuppressWarnings("nls")
public enum CommentMarker {
    /**
     * A Raw (device) sample exists that has a date/time
     * that is more recent than the current Curated value.
     */
    MORE_RECENT("More recent sample exists"),
    
    /**
     * Multiple device samples exist and they all
     * have the same value
     */
    MULTIPLE_WITH_ONE_VALUE("Multiple Samples, Same value"),
    /**
     * Multiple device samples exist and none of them is more
     * recent.
     */
    MULTIPLE_VALUES("Multiple Samples with different values");
    
    public final String toolTipText;
    CommentMarker(String ttt) {
        toolTipText = ttt;
    }
    
    static private final int EDGE_LEN = 11;
    
    static private Color MORE_RECENT_COLOUR = Color.RED;
    static private Color MULTI_SAMPLES_COLOUR =  Color.decode("#00ddFF");
    static private Color MULTI_ONE_VALUE_COLOUR = Color.GREEN;
    
    static private AlphaComposite TRANSPARENT = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    private static final Polygon MARKER_TRIANGLE;

    static {
        int npoints = 3;
        int[] xpoints = new int[npoints];
        int[] ypoints = new int[npoints];
        
        xpoints[0] = -EDGE_LEN;  ypoints[0] = 0;
        xpoints[1] = 0;          ypoints[1] = 0;
        xpoints[2] = 0;          ypoints[2] = EDGE_LEN;
        MARKER_TRIANGLE = new Polygon(xpoints, ypoints, npoints);
    }
    
    public void draw(JComponent comp, Graphics g) {
        Rectangle rect = comp.getBounds();
        switch (this) {
        case MORE_RECENT:
            g.translate(rect.width, 0);
            g.setColor(MORE_RECENT_COLOUR);
            g.fillPolygon(MARKER_TRIANGLE);
            g.translate(-rect.width, 0);
            break;
        case MULTIPLE_VALUES:
            g.translate(rect.width, 0);
            g.setColor(MULTI_SAMPLES_COLOUR);
            g.fillPolygon(MARKER_TRIANGLE);
            g.translate(-rect.width, 0);
            break;
        case MULTIPLE_WITH_ONE_VALUE:
            Graphics2D g2d = (Graphics2D) g;
            Composite save = g2d.getComposite();
            if (TRANSPARENT != null) {
                g2d.setComposite(TRANSPARENT);
            }
            
            g.translate(rect.width, 0);
            g.setColor(MULTI_ONE_VALUE_COLOUR);
            g.fillPolygon(MARKER_TRIANGLE);
            g.translate(-rect.width, 0);
            
            g2d.setComposite(save);
            
            break;
        default:
            break;
        }
    }
}
