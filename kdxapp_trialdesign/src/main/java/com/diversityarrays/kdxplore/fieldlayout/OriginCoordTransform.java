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
package com.diversityarrays.kdxplore.fieldlayout;

import java.awt.Point;

import com.diversityarrays.util.Origin;

public class OriginCoordTransform {

    static private Point ORIGIN_OFFSET = new Point(1, 1);

    static public int getUserXcoord(Origin origin, int x, int xmax) {
        int xx;
        switch (origin) {
        case UPPER_LEFT:
        case LOWER_LEFT:
            xx = x;
            break;
        case LOWER_RIGHT:
        case UPPER_RIGHT:
            xx = (xmax-1) - x;
            break;
        default:
            throw new IllegalStateException();
        }
        return ORIGIN_OFFSET.x + xx;
    }

    static public int getUserYcoord(Origin origin, int y, int maxy) {
        int yy;
        switch (origin) {
        case UPPER_RIGHT:
        case UPPER_LEFT:
            yy = y;
            break;
        case LOWER_RIGHT:
        case LOWER_LEFT:
            yy = (maxy-1) - y;
            break;
        default:
            throw new IllegalStateException();
        }
        return ORIGIN_OFFSET.y + yy;
    }
}
