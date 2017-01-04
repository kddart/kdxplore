package com.diversityarrays.kdxplore.fielddesign.alg;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.diversityarrays.util.ListByOne;
import com.diversityarrays.util.Pair;

@SuppressWarnings("nls")
public class TestUseIterableAlg {

    public static void main(String[] args) {

        int width = 100;
        int height = 25;
        double sPercent = 4.0;
        int nSpatials = (int) ((width * height) * sPercent / 100.0);
        Supplier<IterableAlg> supplier = new Supplier<IterableAlg>() {
            @Override
            public IterableAlg get() {
                return new FarthestFirst(width, height, System.currentTimeMillis(), 1);
            }
        };

        System.out.println(String.format("Looking for %d spatials in %d by %d",
                nSpatials, width, height));

        long millis = System.currentTimeMillis();
        Pair<Dimension, List<Point>> result = doOne(supplier, nSpatials);
        millis = System.currentTimeMillis() - millis;

        Dimension size = result.first;
        System.out.println(String.format("Field %d x %d (%d plots) took %d millis",
                size.width, size.height, (size.width * size.height), millis));

//        List<Point> points = result.second;
//        Collections.sort(points, new Comparator<Point>() {
//            @Override
//            public int compare(Point o1, Point o2) {
//                int diff;
//                diff = Integer.compare(o1.y, o2.y);
//                if (diff == 0) {
//                    diff = Integer.compare(o1.x, o2.x);
//                }
//                return diff;
//            }
//
//        });

        ListByOne<Integer, Integer> colsByRow = new ListByOne<>(new TreeMap<Integer, List<Integer>>());
        result.second.stream().forEach(p -> colsByRow.addKeyValue(p.y, p.x));

//            .forEach((pt) -> System.out.println(String.format("\t%2d,%2d", pt.x, pt.y)));

        colsByRow.entrySet().stream().forEach((entry) -> {
            System.out.print(String.format("  %2d :", entry.getKey()));
            List<Integer> columns = entry.getValue();
            Collections.sort(columns);
            System.out.println(columns.stream().map(String::valueOf)
                        .collect(Collectors.joining(", ", " ", "")));
        });
    }


    static private Pair<Dimension, List<Point>> doOne(
            Supplier<IterableAlg> algSupplier,
            int nPoints)
    {
        IterableAlg alg = algSupplier.get();

        StepState ss = alg.start(nPoints);

        Optional<StepState> opt;
        while ((opt = alg.step()).isPresent()) {
            ss = opt.get();
        }
        System.out.println(
                String.format("%s stopped at generation %d", alg.getName(), ss.generation));
        Set<NamedPoint> clusterPoints = alg.getClusterPoints();

        List<Point> points = clusterPoints.stream().map(np -> np.point)
            .collect(Collectors.toList());

        Dimension size = new Dimension(alg.getWidth(), alg.getHeight());

        return new Pair<>(size, points);
    }
}
