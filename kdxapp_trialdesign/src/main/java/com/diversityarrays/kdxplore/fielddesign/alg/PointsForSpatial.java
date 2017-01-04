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
package com.diversityarrays.kdxplore.fielddesign.alg;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@SuppressWarnings("nls")
public class PointsForSpatial {

    static public Collection<PointsForSpatial> makeGroupsBySpatial(
            Collection<Point> allPoints,
            Random random,
            NamedPoint[] spatials)
    {
        // For each Point, what is the closest target to it?
        List<ClosestTarget> closestTargets = ClosestTarget.collectClosestTargets(
                allPoints, random, spatials);

        Map<NamedPoint, PointsForSpatial> groupsBySpatial = new HashMap<>();
        for (ClosestTarget ct : closestTargets) {
            NamedPoint spatial = ct.spatial;
            PointsForSpatial p4s = groupsBySpatial.get(spatial);
            if (p4s == null) {
                p4s = new PointsForSpatial(spatial);
                groupsBySpatial.put(spatial, p4s);
            }
            p4s.addPoint(ct.point);
        }
        return groupsBySpatial.values();
    }


    public final NamedPoint spatial;
    private NamedPoint centroid;
    private final List<Point> points = new ArrayList<>();
    PointsForSpatial(NamedPoint s) {
        spatial = s;
    }

    public void addPoint(Point point) {
        centroid = null;
        points.add(point);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("PointsForSpatial[ %s : ",
                spatial.toString()));

        for (Point pt : points) {
            sb.append(String.format(" (%d,%d)", pt.x, pt.y));
        }
        return sb.toString();
    }

    public NamedPoint getCentroid(Random random) {
        if (points.size() <= 1) {
            return null;
        }

        if (centroid != null) {
            return centroid;
        }
        Integer xsum = points.stream().collect(Collectors.summingInt(pt -> pt.x));
        Integer ysum = points.stream().collect(Collectors.summingInt(pt -> pt.y));

        double dx = (1.0 * xsum / points.size());
        double dy = (1.0 * ysum / points.size());

        int x = -1;
        int y = -1;

        int dispatch = random.nextInt(4);
        dispatch = 2;
        switch (dispatch) {
        case 0:
            x = (int) Math.floor(dx);
            y = (int) Math.floor(dy);
            break;
        case 1:
            x = (int) Math.floor(dx);
            y = (int) Math.ceil(dy);
            break;
        case 2:
            x = (int) Math.ceil(dx);
            y = (int) Math.floor(dy);
            break;
        case 3:
            x = (int) Math.ceil(dx);
            y = (int) Math.ceil(dy);
            break;
        }

        if (x < 0 || y < 0) {
            throw new RuntimeException("logic error");
        }

        centroid = new NamedPoint(spatial.name, new Point(x, y), false);
        return centroid;
    }
}
