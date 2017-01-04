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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.fieldlayout.Side;
import com.diversityarrays.util.Pair;

/**
 *
 * @author brianp
 *
 */
@SuppressWarnings("nls")
public class LloydsAlg extends AbstractIterableAlg {

    private int nSpatials;

    private List<Point> allPoints;

    public LloydsAlg(int wid, int hyt) {
        this(wid, hyt, 0);
    }

    public LloydsAlg(int wid, int hyt, long seed) {
        super("LloydsAlg", wid, hyt, seed);
    }

    private void initAllPoints(int nTargets) {

        nameMaker.reset();

        if (nTargets <= 0) {
            throw new IllegalArgumentException("nPoints must be > 0");
        }
        int maxPoints = width * height;
        if (nTargets > maxPoints) {
            throw new IllegalArgumentException(String.format("nTargets must be <= %d (== %d * %d)",
                    maxPoints, width, height));
        }
        this.nSpatials = nTargets;

//        NameMaker nameMaker = new NameMaker('a');
        allPoints = new ArrayList<>();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                allPoints.add(new Point(x, y));
            }
        }
    }

    public StepState startWith(int nPoints, Set<NamedPoint> points) {

        initAllPoints(nPoints);

        return startImpl(points);
    }

    private StepState startImpl(Set<NamedPoint> spatialSet) {

        redoStack.clear();

        Collection<NamedPoint> fps = fixedPoints.values();
        NamedPoint[] spatials = fps.toArray(new NamedPoint[fps.size()]);
        Collection<PointsForSpatial> coll = PointsForSpatial.makeGroupsBySpatial(
                allPoints, random, spatials);
        currentState = new StepState(null, spatialSet, coll);

        return currentState;

    }

    @Override
    public Set<NamedPoint> getClusterPoints() {
        if (currentState == null) {
            throw new IllegalStateException("start not called");
        }
        Set<NamedPoint> result = new HashSet<>(currentState.spatials.length);
        Collections.addAll(result, currentState.spatials);
        return result;
    }

    @Override
    public StepState start(int nTargets) {

        if (! fixedPoints.isEmpty() && nTargets >= fixedPoints.size()) {
            throw new IllegalArgumentException("already have " + fixedPoints.size() + " fixed points");
        }

        initAllPoints(nTargets);

        Set<NamedPoint> points = new HashSet<>();
        points = new HashSet<>();
        if (nTargets == 1) {
            Point pt = allPoints.get(allPoints.size() / 2);
            if (! fixedPoints.isEmpty()) {
                // Find the points furthest from the fixed points
                Set<Point> furthest = new HashSet<>();
                List<Pair<Point, Double>> pointDistances = new ArrayList<>();
                for (Point p : allPoints) {
                    double sum = 0;
                    for (Point fp : fixedPoints.keySet()) {
                        sum += manhattanDistance(p, fp);
                    }
                    pointDistances.add(new Pair<>(p, sum));
                }
                Comparator<Pair<Point, Double>> reverseComp = new Comparator<Pair<Point, Double>>() {
                    @Override
                    public int compare(Pair<Point, Double> o1, Pair<Point, Double> o2) {
                        return o2.second.compareTo(o1.second);
                    }
                };
                Collections.sort(pointDistances, reverseComp);
                pt = pointDistances.get(0).first;
            }
            points.add(new NamedPoint(nameMaker.get(), pt, false));
        }
        else {
            List<Point> choices = new ArrayList<>(allPoints);
            choices.removeAll(fixedPoints.keySet());
            for (int i = this.nSpatials; --i >= 0; ) {
                int index = random.nextInt(choices.size());
                points.add(new NamedPoint(nameMaker.get(), choices.remove(index), false));
            }
        }

        return startImpl(points);
    }

    @Override
    public Optional<StepState> step() {

        long nanos = System.nanoTime();

        Collection<PointsForSpatial> pointsForSpatial = PointsForSpatial.makeGroupsBySpatial(
                allPoints, random, currentState.spatials);

        ps.println("--- Generation: " + currentState.generation);

        List<Point> collisions = new ArrayList<>();
        List<Pair<Point, Point>> changedPoints = new ArrayList<>();
        Set<NamedPoint> newSpatials = movePointsAndCollectCollisions(pointsForSpatial, collisions, changedPoints);

        long elapsedNanos = System.nanoTime() - nanos;

        printChangedPoints(changedPoints);

        if (collisions.isEmpty()) {
            ps.println(String.format("  No collisions: in %.3fms", elapsedNanos / 1_000_000.0));
        }
        else {
            long collisionNanos = System.nanoTime();
            Set<Point> spatialPoints = newSpatials.stream().map(np -> np.point).collect(Collectors.toSet());
            resolveCollisions(spatialPoints, collisions);
            collisionNanos = System.nanoTime() - collisionNanos;
            ps.println(String.format("\t resolved collisions: in %.3fms", collisionNanos / 1_000_000.0));
        }

        if (changedPoints.isEmpty()) {
            return Optional.empty();
        }

        currentState = new StepState(currentState, newSpatials, pointsForSpatial);

        redoStack.clear();
        return Optional.of(currentState);
    }

//    private Collection<PointsForSpatial> makeGroupsBySpatial() {
//        // For each Point, what is the closest target to it?
//        List<ClosestTarget> closestTargets = ClosestTarget.collectClosestTargets(
//                allPoints, random, currentState.spatials);
//
//        Map<Point, PointsForSpatial> groupsBySpatial = new HashMap<>();
//        for (ClosestTarget ct : closestTargets) {
//            Point spatial = ct.spatial;
//            PointsForSpatial p4s = groupsBySpatial.get(spatial);
//            if (p4s == null) {
//                p4s = new PointsForSpatial(spatial);
//                groupsBySpatial.put(spatial, p4s);
//            }
//            p4s.addPoint(ct.point);
//        }
//        return groupsBySpatial.values();
//    }

    private void printChangedPoints(List<Pair<Point, Point>> changedPoints) {
        if (changedPoints.isEmpty()) {
            ps.println("  No changed points");
        }
        else {
            ps.println("  " + changedPoints.size() + " changed points:");
            StringBuilder sb = new StringBuilder();
            String sep = "\t";
            for (Pair<Point, Point> pair : changedPoints) {
                sb.append(sep).append(String.format("%s => %s",
                        pointToString(pair.first), pointToString(pair.second)) );
                if (sb.length() > 40) {
                    ps.println(sb.toString());
                    sep = "\t";
                    sb = new StringBuilder();
                }
                else {
                    sep = ",  ";
                }
            }
            if (sb.length() > 0) {
                ps.println(sb.toString());
            }
        }
    }

    float relax = 1.9f;

    private Set<NamedPoint> movePointsAndCollectCollisions(
            Collection<PointsForSpatial> pointsForSpatial,
            List<Point> collisions,
            List<Pair<Point, Point>> changedPoints)
    {
        Set<NamedPoint> result = new LinkedHashSet<>();

        // First, retain the fixed points
        for (PointsForSpatial p4s : pointsForSpatial) {
            if (p4s.spatial.fixed) {
                result.add(p4s.spatial);
            }
        }

        for (PointsForSpatial p4s : pointsForSpatial) {
            if (p4s.spatial.fixed) {
                continue; // already done above
            }

            NamedPoint centroid = p4s.getCentroid(random);
            if (centroid == null) {
                if (! result.add(p4s.spatial)) {
                    // collision
                    collisions.add(p4s.spatial.point);
                }
            }
            else {
                NamedPoint sp = p4s.spatial;

                int new_x = sp.point.x + (int) ((centroid.point.x - sp.point.x) * relax);
                if (new_x < 0 || new_x >= width) {
                    new_x = centroid.point.x;
                }
                int new_y = sp.point.y + (int) ((centroid.point.y - sp.point.y) * relax);
                if (new_y < 0 || new_y >= height) {
                    new_y = centroid.point.y;
                }
                Point new_sp = new Point(new_x, new_y);

                NamedPoint npt = new NamedPoint(sp.name, new_sp, false);
                if (result.add(npt)) {
                    if (! new_sp.equals(sp.point)) {
                        changedPoints.add(new Pair<>(sp.point, new_sp));
                    }
                }
                else {
                    // collision
                    collisions.add(new_sp);
                }
            }
        }
        return result;
    }

//    static private Comparator<Pair<Point,Integer>> PAIR_COMPARATOR = new Comparator<Pair<Point,Integer>>() {
//        @Override
//        public int compare(Pair<Point, Integer> o1, Pair<Point, Integer> o2) {
//            return o1.second.compareTo(o2.second);
//        }
//    };

//    private List<ClosestTarget> collectClosestTargets() {
//
//        List<Pair<Point,Integer>> pointPairs = new ArrayList<>();
//        for (Point pt : allPoints) {
//            pointPairs.add(new Pair<>(pt, random.nextInt()));
//        }
//        Collections.sort(pointPairs, PAIR_COMPARATOR);
//
//        List<ClosestTarget> result = new ArrayList<>();
//
//        for (Pair<Point,Integer> pair : pointPairs) {
//            Point pt = pair.first;
//            Point closest = findClosestSpatial(pt);
//            result.add(new ClosestTarget(pt, closest));
//        }
//
//        return result;
//    }

//    private Point findClosestSpatial(Point pt) {
//        double minDist = 0;
//        Point closest = null;
//        for (Point spatial : currentState.spatials) {
//            if (pt.equals(spatial)) {
//                continue;
//            }
//            double dist = spatial.distanceSq(pt);
//            if (closest == null) {
//                closest = spatial;
//                minDist = dist;
//            }
//            else if (dist < minDist) {
//                closest = spatial;
//                minDist = dist;
//            }
//        }
//        return closest;
//    }

    private boolean inMyRectangle(Point tmp) {
        if (tmp.x < 0 || tmp.x >= width) {
            return false;
        }
        if (tmp.y < 0 || tmp.y >= height) {
            return false;
        }
        return true;
    }

    private void resolveCollisions(Set<Point> newSpatials, List<Point> collisions) {
        ps.println("# collisions=" + collisions.size());
        ps.println(collisions.stream().map(pt -> pt.toString())
            .collect(Collectors.joining(" : ", "\t", "")));

        for (Point collisionPoint : collisions) {
            ps.println(String.format("Moving collision: (%s)", pointToString(collisionPoint)));

            // We need to find a place to put the collision,
            // First try the N,S,E,W sides (but in a random order);
            // If we can't put it there, then try the Corners (again in a random order)
            Point foundFreePosition = null;
            Side foundSide = null;

            List<Side> sides = new ArrayList<>();
            Collections.addAll(sides,Side.values());

            while (foundFreePosition == null && ! sides.isEmpty()) {
                int index = random.nextInt(sides.size());
                Side side = sides.remove(index);
                Point new_pt = new Point(collisionPoint.x + side.xinc, collisionPoint.y + side.yinc);
                if (inMyRectangle(new_pt)) {
                    if (newSpatials.add(new_pt)) {
                        foundFreePosition = new_pt;
                        foundSide = side;
                    }
                }
            }

            if (foundFreePosition != null) {
                ps.println(String.format("\tMOVED to Side.%s = (%s)",
                        foundSide.name(), pointToString(foundFreePosition)));
            }
            else {
                // Didn't find one on a Side, let's try the Corners
                Corner foundCorner = null;
                List<Corner> corners = new ArrayList<>();
                Collections.addAll(corners, Corner.values());
                while (foundFreePosition == null && ! corners.isEmpty()) {
                    int index = random.nextInt(corners.size());
                    Corner corner = corners.remove(index);

                    Point new_pt = new Point(collisionPoint.x + corner.xinc, collisionPoint.y + corner.yinc);

                    if (inMyRectangle(new_pt)) {
                        if (newSpatials.add(new_pt)) {
                            foundFreePosition = new_pt;
                            foundCorner = corner;
                        }
                    }
                }

                if (foundFreePosition != null) {
                    ps.println(String.format("\tMOVED to Corner.%s = (%s)",
                            foundCorner.name(), pointToString(foundFreePosition)));
                }
                else {
                    // Didn't find it in a Corner either!
                    // For now, just pick any other spot at random
                    // TODO look further using Manhattan distance until we find one

                    Optional<Integer> opt_max = allPoints.stream().map(pt -> manhattanDistance(collisionPoint, pt))
                        .collect(Collectors.maxBy(Integer::compareTo));
                    if (opt_max.isPresent()) {
                        int maxUseDistance = opt_max.get();
                        for (int useDistance = 2; useDistance <= maxUseDistance; ++useDistance) {
                            Optional<Point> found = findFirstFreeAtDistance(2, collisionPoint, newSpatials);
                            if (found.isPresent()) {
                                foundFreePosition = found.get();
                                ps.println(String.format("\t  found (%s) at manhattanDistance=%d",
                                        pointToString(foundFreePosition), useDistance));
                                break;
                            }
                        }
                    }

                    if  (foundFreePosition == null) {
                        // Still failed! Just get one at random
                        List<Point> all = new ArrayList<>(allPoints);
                        while (foundFreePosition == null) {
                            int index = random.nextInt(all.size());
                            Point new_pt = all.remove(index);
                            if (newSpatials.add(new_pt)) {
                                foundFreePosition = new_pt;
                                break;
                            }
                        }
                    }

                    ps.println(String.format("\tMOVED to Random = (%s)",
                            pointToString(foundFreePosition)));
                }
            }

        }
    }

    private Optional<Point> findFirstFreeAtDistance(int useDistance, Point collisionPoint, Set<Point> newSpatials) {
        List<Point> pointsAtUseDistance = new ArrayList<>();
        for (Point pt : allPoints) {
            if (! collisionPoint.equals(pt)) {
                int dist = manhattanDistance(collisionPoint, pt);
                if (dist == useDistance) {
                    pointsAtUseDistance.add(pt);
                }
            }
        }

        while (! pointsAtUseDistance.isEmpty()) {
            int index = random.nextInt(pointsAtUseDistance.size());
            Point pt = pointsAtUseDistance.remove(index);
            if (newSpatials.add(pt)) {
                return Optional.of(pt);
            }
        }
        return Optional.empty();
    }

}
