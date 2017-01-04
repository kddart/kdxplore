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

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("nls")
public class FarthestFirst extends AbstractIterableAlg {

    private static final boolean DEBUG = Boolean.getBoolean(FarthestFirst.class.getSimpleName() + ".DEBUG");

    private int nClusters;

    class IterationData {

        class Centroid {
            public final NamedPoint namedPoint;
            double minDistance;
            List<Centroid> belongsTo;

            Centroid(NamedPoint pt) {
                namedPoint = pt;
                minDistance = Double.MAX_VALUE;

                if (pt.fixed || fixedPoints.containsKey(pt.point)) {
                    belongsTo = selectedCentroids;
                }
                else {
                    belongsTo = unSelectedCentroids;
                }
                belongsTo.add(this);
            }

            public Centroid(Centroid c) {
                namedPoint = c.namedPoint;
                minDistance = c.minDistance;
                belongsTo = c.isSelected() ? selectedCentroids : unSelectedCentroids;
                belongsTo.add(this);
            }

            @Override
            public String toString() {
                return String.format("Centroid[%s  %s  %.3f]",
                        namedPoint.toString(),
                        ((belongsTo == selectedCentroids) ? "SEL]" : "UNSEL]"),
                        minDistance);
            }

            public boolean isSelected() {
                return belongsTo == selectedCentroids;
            }

            public void setSelected(boolean b) {
                if (b) {
                    if (belongsTo == unSelectedCentroids) {
                        unSelectedCentroids.remove(this);
                        selectedCentroids.add(this);
                        belongsTo = selectedCentroids;
                    }
                    else if (belongsTo == selectedCentroids) {
                        selectedCentroids.remove(this);
                        unSelectedCentroids.add(this);
                        belongsTo = unSelectedCentroids;
                    }
                }
            }
        }

        final List<Centroid> unSelectedCentroids;
        final List<Centroid> selectedCentroids;
        public final int nClusters;

        public IterationData(int nClusters, List<NamedPoint> points) {
            this.nClusters = nClusters;
            unSelectedCentroids = new ArrayList<>(points.size());
            selectedCentroids = new ArrayList<>(nClusters);

            for (NamedPoint pt : points) {
                new Centroid(pt);
            }
        }

        public IterationData(IterationData payload) {
            this.nClusters = payload.nClusters;

            unSelectedCentroids = new ArrayList<>(payload.unSelectedCentroids.size());
            selectedCentroids = new ArrayList<>(nClusters);

            for (Centroid c : payload.unSelectedCentroids) {
                new Centroid(c);
            }

            for (Centroid c : payload.selectedCentroids) {
                new Centroid(c);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("IterationData[");
            sb.append("nSel=").append(selectedCentroids.size());
            for (Centroid c : selectedCentroids) {
                sb.append("\n\t").append(c);
            }
            sb.append(']');
            return sb.toString();
        }

        public Centroid getUnselected(int index) {
            return unSelectedCentroids.get(index);
        }

        public Centroid farthestAway() {
            double maxDistance = -1.0;
            Centroid result = null;
            for (Centroid c : unSelectedCentroids) {
                if (maxDistance < c.minDistance) {
                    maxDistance = c.minDistance;
                    result = c;
                }
            }
            return result;
        }

        public void updateMinDistance(Centroid centre) {
            for (Centroid c : unSelectedCentroids) {
                double d = centre.namedPoint.distanceSq(c.namedPoint);
                if (d < c.minDistance) {
                    c.minDistance = d;
                }
            }
        }
    }

    // TODO redefine this using functional approach
    static private void iterateUsingInset(Dimension wh, int inset,
            Consumer<Point> visitor)
    {
        int width = wh.width;
        int height = wh.height;

        int min_x = 0 + inset;
        int max_x = width - inset;

        int min_y = 0 + inset;
        int max_y = height - inset;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if ((x >= min_x && x < max_x) && (y >= min_y && y < max_y)) {
                    Point pt = new Point(x, y);
                    visitor.accept(pt);
                }
            }
        }

    }

    static public int getPointCountAvailable(Dimension wh, int inset) {
        int[] result = new int[1];
        iterateUsingInset(wh, inset, (pt) -> ++result[0]);
        return result[0];
    }

    private int inset;
    private List<Point> allPoints;

    public FarthestFirst(int wid, int hyt, long seed, int inset) {
        super("FarthestFirst", wid, hyt, seed);
        this.inset = inset;
    }

    @Override
    public StepState start(int nPoints) {

        nameMaker.reset();

        int maxPoints = width * height;

        nClusters = Math.min(nPoints + fixedPoints.size(), maxPoints);

        List<NamedPoint> pointsToUse = new ArrayList<>();

        iterateUsingInset(
                new Dimension(width, height),
                inset,

                (pt) -> {
                    if (DEBUG) ps.println("Using " + pointToString(pt));
                    NamedPoint npt = fixedPoints.get(pt);
                    if (npt == null) {
                        npt = new NamedPoint(nameMaker.get(), pt, false);
                    }
                    pointsToUse.add(npt);
                }
        );

        allPoints = pointsToUse.stream()
                .map(np -> np.point)
                .collect(Collectors.toList());
        IterationData iterationData = new IterationData(nClusters, pointsToUse);

        // If none yet then put one in, else the fixed points are the initial state
        if (iterationData.selectedCentroids.isEmpty()) {
            int index = random.nextInt(pointsToUse.size());
            IterationData.Centroid centre = iterationData.getUnselected(index);
            centre.setSelected(true);
            if (DEBUG) ps.println(String.format("First centre is at %s", centre.namedPoint.toString()));
            iterationData.updateMinDistance(centre);
        }


        List<NamedPoint> list = iterationData.selectedCentroids.stream()
                .map(c -> c.namedPoint)
                .collect(Collectors.toList());

        NamedPoint[] spatials = list.toArray(new NamedPoint[list.size()]);

        currentState = MyStepState.create(null, iterationData, allPoints,
                random, spatials);
        return currentState;
    }

    static class MyStepState extends StepState {

        final IterationData payload;

        static public MyStepState create(StepState previous,
                IterationData payload,
                Collection<Point> allPoints,
                Random random,
                NamedPoint[] spatials)
        {
            Set<NamedPoint> set = payload.selectedCentroids.stream()
                    .map(c -> c.namedPoint)
                    .collect(Collectors.toSet());

            Collection<PointsForSpatial> p4s =
                    PointsForSpatial.makeGroupsBySpatial(allPoints, random, spatials);

            return new MyStepState(payload, previous, set, p4s);
        }

        private MyStepState(IterationData payload,
                StepState previous,
                Set<NamedPoint> spatials,
                Collection<PointsForSpatial> pointsForSpatial) {
            super(previous, spatials, pointsForSpatial);
            this.payload = payload;
        }
    }

    @Override
    public Optional<StepState> step() {

        if (currentState == null) {
            throw new IllegalStateException("start() was not called");
        }
        MyStepState current = (MyStepState) currentState;
        if (current.payload.selectedCentroids.size() >= nClusters) {
            return Optional.empty();
        }

        IterationData iterationData = new IterationData(current.payload);

        IterationData.Centroid farthest = iterationData.farthestAway();
        farthest.setSelected(true);
        iterationData.updateMinDistance(farthest);

        Set<NamedPoint> clusterPoints = getClusterPoints();
        NamedPoint[] spatials = clusterPoints.toArray(new NamedPoint[clusterPoints.size()]);

        MyStepState mss = MyStepState.create(currentState, iterationData,
                allPoints, random, spatials);
        currentState = mss;

        if (DEBUG) ps.println(String.format("next centre is at %s", farthest.toString()));

        return Optional.of(mss);
    }

    @Override
    public boolean canUndo() {
        return false;
    }

    @Override
    public StepState undo() {
        throw new UnsupportedOperationException("undo");
    }

    @Override
    public boolean canRedo() {
        return false;
    }

    @Override
    public StepState redo() {
        throw new UnsupportedOperationException("redo");
    }

    @Override
    public Set<NamedPoint> getClusterPoints() {
        if (currentState == null) {
            throw new IllegalStateException("start not called");
        }

        MyStepState mss = (MyStepState) currentState;
        return mss.payload.selectedCentroids.stream()
            .map(c -> c.namedPoint)
            .collect(Collectors.toSet());
    }
}
