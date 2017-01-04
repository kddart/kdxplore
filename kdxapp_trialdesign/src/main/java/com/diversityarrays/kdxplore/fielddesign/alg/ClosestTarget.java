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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.diversityarrays.util.Pair;

public class ClosestTarget {

    static private Comparator<Pair<?,Integer>> PAIR_COMPARATOR = new Comparator<Pair<?,Integer>>() {
        @Override
        public int compare(Pair<?, Integer> o1, Pair<?, Integer> o2) {
            return o1.second.compareTo(o2.second);
        }
    };

    static public List<ClosestTarget> collectClosestTargets(
            Collection<Point> allPoints, Random random, NamedPoint[] targets)
    {
        // First randomise the list so we don't bias based on position
        List<Pair<Point,Integer>> pointPairs = new ArrayList<>();
        for (Point pt : allPoints) {
            pointPairs.add(new Pair<>(pt, random.nextInt()));
        }
        Collections.sort(pointPairs, PAIR_COMPARATOR);

        List<ClosestTarget> result = new ArrayList<>();

        for (Pair<Point,Integer> pair : pointPairs) {
            Point pt = pair.first;
            NamedPoint closestTarget = findClosestTarget(pt, targets);
            result.add(new ClosestTarget(pt, closestTarget));
        }

        return result;
    }

    static private NamedPoint findClosestTarget(Point pt, NamedPoint[] targets) {
        double minDist = 0;
        NamedPoint closest = null;
        for (NamedPoint target : targets) {
            if (pt.equals(target.point)) {
                continue;
            }
            double dist = target.point.distanceSq(pt);
            if (closest == null) {
                closest = target;
                minDist = dist;
            }
            else if (dist < minDist) {
                closest = target;
                minDist = dist;
            }
        }
        return closest;
    }

    public final Point point;
    public final NamedPoint spatial;

    ClosestTarget(Point pt, NamedPoint sp) {
        point = pt;
        spatial = sp;
    }

    public NamedPoint getSpatial() {
        return spatial;
    }

    @Override
    public String toString() {
        return String.format("Closest to (%d,%d) is %s", //$NON-NLS-1$
                point.x, point.y,
                spatial.toString());
    }
}
