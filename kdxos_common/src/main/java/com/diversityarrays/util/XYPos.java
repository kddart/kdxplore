/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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

/**
 * This is so that we can use VisitOrder2D in android - but it doesn't have
 * java.awt.Point - and android.graphics.Point is the only other simple x,y pair.
 * @author brian
 *
 */
public class XYPos {
	
	public final int x;
	public final int y;
	
	private final int hashCode;
	
	public XYPos(int x, int y) {
		this.x = x;
		this.y = y;
		
		hashCode = new Integer(x).hashCode() * 17 + new Integer(y).hashCode();
	}
	
	@Override
	public int hashCode() {
		return hashCode; 
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (! (o instanceof XYPos)) return false;
		XYPos other = (XYPos) o;
		return this.x==other.x && this.y==other.y;
	}
	
	@Override
	public String toString() {
		return x+","+y; //$NON-NLS-1$
	}
}

