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
package com.diversityarrays.kdxplore.field;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.util.ImageId;

public class KdxploreFieldLayout<E> extends FieldLayout<E> {

	public KdxploreFieldLayout(Class<E> tclass, ImageId imageId, int xsize, int ysize) {
		super(tclass, imageId, xsize, ysize);
	}

	// THIS CODE NEEDS TO BE USED ON JavaSE
	@Override
	protected void updateItemAtPoint(E e, int x, int y) {
		Point pt = new Point(x,y);
		if (e==null) {
			itemAtPoint.remove(pt);
		}
		else {
			itemAtPoint.put(pt, e);
		}
	}

	private final Map<Point,E> itemAtPoint = new HashMap<>();

	public List<E> getItemsAt(List<Point> points) {
		List<E> result = new ArrayList<>(points.size());
		for (Point pt : points) {
			E item = itemAtPoint.get(pt);
			if (item != null) {
				result.add(item);
			}
		}
		return result;
	}

	public List<Point> getPointsForItems(Collection<E> items) {
		List<Point> result = new ArrayList<>();
		for (Point pt : itemAtPoint.keySet()) {
			E item = itemAtPoint.get(pt);
			if (items.contains(item)) {
				result.add(pt);
			}
		}
		return result;
	}

	public Point getPointForItem(E target) {
		for (Point pt : itemAtPoint.keySet()) {
			E item = itemAtPoint.get(pt);
			if (item.equals(target)) {
				return pt;
			}
		}
		return null;
	}
}
