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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.Transient;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

import net.pearcan.io.IOUtil;

/**
 * PlantingBlock-s are uniquely identified by their name.
 * <p>
 * They have content
 * @author brianp
 *
 */
@SuppressWarnings("nls")
public class PlantingBlock<E> implements Comparable<PlantingBlock<?>> {

    private static final Map<Heading, Field> FIELD_BY_HEADING;

    static {
        Map<Heading,Field> map = new HashMap<>();
        for (Heading h : Heading.values()) {
            try {
                if (h.fieldName == null) {
                    continue;
                }
                Field fld = PlantingBlock.class.getDeclaredField(h.fieldName);
                if (int.class == fld.getType()
                        || String.class == fld.getType()
                        || Integer.class == fld.getType())
                {
                    fld.setAccessible(true);
                }
                else {
                    throw new RuntimeException(
                            "Unsupported field class for: " + fld.getName() + "=" + fld.getType());
                }
                map.put(h, fld);
            }
            catch (NoSuchFieldException | SecurityException e) {
                throw new RuntimeException(
                        "ERROR in enum " + Heading.class.getName() + "." + h.name(),
                        e);
            }
        }
        FIELD_BY_HEADING = map;
    }

    static public enum WhatChanged {
        POSITION,  // X,Y
        DIMENSION, // width x height
        ENTRY_TYPES,
        BORDER,     // N S E W

        // ones above here have editors
        MINIMUM_CELL_COUNT,
        SPATIALS_REQUIRED
    }

    static public enum Attribute {
        DIMENSION("RxC", Dimension.class, "Size"),
        POSITION("X,Y", Point.class, "Position"),
        BORDERS("Borders", String.class, "Borders"),
        ;

        public final Class<?> valueClass;
        public final String displayValue;
        public final String buttonText;
        Attribute(String d, Class<?> cls, String bt) {
            displayValue = d;
            valueClass = cls;
            buttonText = bt;
        }

        public Object getColumnValue(PlantingBlock<?> t) {
            switch (this) {
            case BORDERS:
                int[] counts = t.getBorderCountBySide();
                StringBuilder sb = new StringBuilder();
                String sep = "";
                for (Side side : Side.values()) {
                    int count = counts[side.ordinal()];
                    if (count > 0) {
                        sb.append(sep).append(side.displayName);
                        sep = " ";
                        if (count > 1) {
                            sb.append(':').append(count);
                        }
                    }
                }
                return sb.length() <= 0 ? "-none-" : sb.toString();
            case DIMENSION:
                return new Dimension(t.getRowCount(), t.getColumnCount());
            case POSITION:
                return new Point(t.getX(), t.getY());
            default:
                break;
            }
            return null;
        }
    }

    /**
     * Read a line-by-line of file of TrialBlock data.
     * @param file
     * @param ps
     * @return the separator used in the file and the TrialBlocks found.
     * @throws IOException
     */
    static public Pair<String,List<PlantingBlock<?>>> readFromCsvFile(
            File file,
            PrintStream ps)
    throws IOException {

        String separator = null;
        List<PlantingBlock<?>> result = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            String[] parts;
            Map<Integer, Heading> headingByColumnIndex = null;
            int lineNumber = 0;
            while (null != (line = br.readLine())) {
                ++lineNumber;

                // Ignore blanks and comments
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (separator == null) {
                    parts = line.split("\t");
                    if (parts.length > 1) {
                        separator = "\t";
                    }
                    else {
                        parts = line.split(",");
                        separator = ",";
                    }

                    headingByColumnIndex = findHeadingMapping(parts);
                }
                else {
                    parts = line.split(separator);
                    @SuppressWarnings("rawtypes")
                    PlantingBlock<?> tr = new PlantingBlock();
                    for (Integer columnIndex : headingByColumnIndex.keySet()) {
                        if (columnIndex < parts.length) {
                            Heading hdg = headingByColumnIndex.get(columnIndex);
                            String value = parts[columnIndex];
                            Field fld = FIELD_BY_HEADING.get(hdg);
                            try {
                                if (int.class == fld.getType() || Integer.class == fld.getType()) {
                                    try {
                                        Integer ivalue = new Integer(value);
                                        fld.set(tr, ivalue);
                                    }
                                    catch (NumberFormatException e) {
                                        ps.println(String.format(
                                                "Invalid value %s in line#%d column#%d",
                                                value, lineNumber, (columnIndex + 1)));
                                        tr = null;
                                        break;
                                    }
                                }
                                else if (String.class == fld.getType()) {
                                    fld.set(tr, value);
                                }
                            }
                            catch (IllegalArgumentException | IllegalAccessException e) {
                                throw new IOException(e);
                            }
                        }
                        else {
                            tr = null;
                            ps.println("%Ignoring line#" + lineNumber + ": too few data columns");
                            break;
                        }
                    }
                    if (tr != null) {
                        result.add(tr);
                    }
                }
            }
        }
        finally {
            IOUtil.closeQuietly(br);
        }
        return new Pair<>(separator, result);
    }

    public static Map<Integer, Heading> findHeadingMapping(String[] parts) throws IOException {
        Map<Integer, Heading> headingByColumnIndex = new HashMap<>();

        Set<Heading> toFind = new HashSet<>(Arrays.asList(Heading.values()));

        for (int columnIndex = 0; columnIndex < parts.length; ++columnIndex) {
            String p = parts[columnIndex];
            Heading found = null;
            for (Heading h : toFind) {
                if (h.csvHeading.equalsIgnoreCase(p)) {
                    found = h;
                    break;
                }
            }
            if (found != null) {
                headingByColumnIndex.put(columnIndex, found);
                toFind.remove(found);
                if (toFind.isEmpty()) {
                    break;
                }
            }
        }

        if (! toFind.isEmpty()) {
            String msg = toFind.stream()
                       .map(h -> h.csvHeading)
                       .collect(Collectors.joining(","));
            throw new IOException("Missing headings: " + msg);
        }
        return headingByColumnIndex;
    }

    private final String name;
    private final int replicateNumber;
    private boolean canEditSize;
    private int columnCount;
    private int rowCount;
    private int x;
    private int y;
    private int spatialChecksCount;

    private E content;

    private Integer minimumCellCount;

    private final int[] borderCountBySide = new int[Side.values().length];

    // Can't create one without a name
    private PlantingBlock() {
        name = null;
        replicateNumber = 0;
    }

    public PlantingBlock(int replicateNumber, String name, DesignParams designParams) {
        this(replicateNumber, name,
                designParams.width, designParams.height,
                0, 0,
                designParams.plotsPerReplicate,
                designParams.nSpatials);
    }

    public Set<WhatChanged> updateDesignParameters(DesignParams designParams) {

//        new Exception("CHECK ME").printStackTrace();
        Set<WhatChanged> whatChanged = new HashSet<>();
//        boolean changed = false;

        if (designParams.width != columnCount || designParams.height != rowCount) {
            // They may have changed but we might already be large enough
            int currentPlotCount = columnCount * rowCount;
            int dpPlotCount = designParams.width * designParams.height;
            if (dpPlotCount > currentPlotCount) {
                int oldColumnCount = columnCount;
                int oldRowCount = rowCount;

                columnCount = Math.max(columnCount, designParams.width);
                rowCount = Math.max(rowCount, designParams.height);

                if (columnCount != oldColumnCount || rowCount != oldRowCount) {
                    whatChanged.add(WhatChanged.DIMENSION);
                }
            }
        }

        if (minimumCellCount==null) {
            if (designParams.plotsPerReplicate > 0) {
                minimumCellCount = designParams.plotsPerReplicate;
                whatChanged.add(WhatChanged.MINIMUM_CELL_COUNT);
            }
        }
        else if (minimumCellCount != designParams.plotsPerReplicate) {
            minimumCellCount = designParams.plotsPerReplicate;
            whatChanged.add(WhatChanged.MINIMUM_CELL_COUNT);
        }

        if (designParams.nSpatials != spatialChecksCount) {
            spatialChecksCount = designParams.nSpatials;
            whatChanged.add(WhatChanged.SPATIALS_REQUIRED);
        }

        return whatChanged;
    }

    public PlantingBlock(int replicateNumber, String name, int nCols, int nRows, int nSpatials) {
        this(replicateNumber, name,
                nCols, nRows,
                0, 0,
                null,
                nSpatials);
    }

    private PlantingBlock(int replicateNumber, String name,
            int nCols, int nRows,
            int x, int y,
            Integer mincc,
            int nSpatials)
    {
        this.name = name;
        this.replicateNumber = replicateNumber;
        this.columnCount = nCols;
        this.rowCount = nRows;
        this.x = x;
        this.y = y;
        this.minimumCellCount = mincc;
        this.spatialChecksCount = nSpatials;
    }

    public E getContent() {
        return content;
    }
    public void setContent(E e) {
        this.content = e;
    }

    public Integer getMinimumCellCount() {
        return minimumCellCount;
    }

    @Override
    public String toString() {
        return name + "[" + columnCount + "x" + rowCount + "] @" + x + "," + y;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (! (o instanceof PlantingBlock)) return false;
        return this.name.equals(((PlantingBlock<?>) o).name);
    }

    public int getBorderCount(Side side) {
        return borderCountBySide[side.ordinal()];
    }

//    public Map<Side,Integer> getBorderCountBySideAsMap() {
//        Map<Side,Integer> result = new HashMap<>();
//        for (Side side : Side.values()) {
//            result.put(side, borderCountBySide[side.ordinal()]);
//        }
//        return result;
//    }

    public int[] getBorderCountBySide() {
        return getBorderCountBySide(null);
    }

    public int[] getBorderCountBySide(int[] result) {
        if (result == null || result.length < borderCountBySide.length) {
            result = new int[borderCountBySide.length];
        }
        System.arraycopy(borderCountBySide, 0, result, 0, result.length);
        return result;
    }

    public void setBorder(Side side, int count) {
        borderCountBySide[side.ordinal()] = Math.max(0, count);
    }

    public int getReplicateNumber() {
        return replicateNumber;
    }

    public boolean getCanEditSize() {
        return canEditSize;
    }

    public void setCanEditSize(boolean b) {
        canEditSize = b;
    }

    public String getName() {
        return name;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public Dimension getSize() {
        return new Dimension(columnCount, rowCount);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSpatialChecksCount(int v) {
        this.spatialChecksCount = v;
    }
    public int getSpatialChecksCount() {
        return spatialChecksCount;
    }

    @Transient
    public Rectangle getRectangle() {
        return new Rectangle(x, y, columnCount, rowCount);
    }

    @Transient
    public boolean contains(Point pt) {
        return getRectangle().contains(pt);
    }

    public Integer getFillerCount() {
        Integer result = null;
        if (minimumCellCount != null) {
            int excess = (columnCount * rowCount) - minimumCellCount;
            if (excess > 0) {
                result = excess;
            }
        }
        return result;
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public Map<Point, E> getContentByPoint() {
        return Collections.unmodifiableMap(contentByPoint);
    }

    public Collection<E> getAllContents() {
        return contentByPoint.values();
    }

    public void visitContentByPoint(BiPredicate<Point, E> visitor) {
        for (Map.Entry<Point, E> me : contentByPoint.entrySet()) {
            if (! visitor.test(me.getKey(), me.getValue())) {
                break;
            }
        }
    }

    private final Map<Point, E> contentByPoint = new HashMap<>();

    public void clearContent() {
        contentByPoint.clear();
    }

    public void clearEntryTypesOnly() {

        Map<Point, E> newContent = new HashMap<>();

        BiPredicate<Point, E> visitor = new BiPredicate<Point, E>() {
            @Override
            public boolean test(Point pt, E e) {
                newContent.put(pt, e);
                return true;
            }
        };

        for (Map.Entry<Point, E> me : contentByPoint.entrySet()) {
            if (! visitor.test(me.getKey(), me.getValue())) {
                break;
            }
        }
        contentByPoint.clear();
        contentByPoint.putAll(newContent);
    }

//    public Map<EntryType, Integer> getEntryTypeCounts() {
//        return contentByPoint.entrySet().stream()
//            .map(me -> me.getValue().first)
//            .filter(e -> e != null)
//            .collect(Collectors.toMap(Function.identity(),
//                (t) -> new Integer(1),
//                (a,b) -> a+b));
//    }

    public boolean setContentAt(Point modelPoint, E newContent) {
        Point pt = new Point(modelPoint);
        boolean contentChanged = false;
        E oldContent = contentByPoint.remove(pt);

        if (newContent != null) {
            contentByPoint.put(pt, newContent);
            contentChanged = ! newContent.equals(oldContent);
        }
        else {
            // newContent is null
            contentChanged = oldContent != null;
        }
        return contentChanged;
    }

    public List<Point> setContentUsing(Map<Point, E> newContentByPoint) {
        if (Check.isEmpty(newContentByPoint)) {
            return Collections.emptyList();
        }

        List<Point> changed = new ArrayList<>();
        for (Point pt : newContentByPoint.keySet()) {
            setContentAt(pt, newContentByPoint.get(pt), changed);
        }
        return changed;
    }

    public List<Point> setContentAtPoints(E newContent, Point ... points) {
        if (points == null || points.length <= 0) {
            return Collections.emptyList();
        }

        List<Point> changed = new ArrayList<>();
        for (Point pt : points) {
            setContentAt(pt, newContent, changed);
        }
        return changed;
    }

    public List<Point> addContentAt(E newContent, Point ... points) {

        List<Point> changed = new ArrayList<>();

        if (points != null) {
            for (Point pt : points) {
                setContentAt(pt, newContent, changed);
            }
        }
        return changed;
    }

    private void setContentAt(
            Point modelPoint,
            E newContent,
            List<Point> changed)
    {
        // Copy in case caller who supplied it changes it!
        Point pt = new Point(modelPoint);
        E oldContent = contentByPoint.remove(pt);
        if (newContent != null) {
            contentByPoint.put(pt, newContent);
        }

        if (newContent != null) {
            if (! newContent.equals(oldContent)) {
                changed.add(pt);
            }
        }
        else {
            // newContent == null
            if (oldContent != null) {
                changed.add(pt);
            }
        }
    }

    public int getContentCount() {
        return contentByPoint.size();
    }

    @Override
    public int compareTo(PlantingBlock<?> o) {
        return this.getName().compareToIgnoreCase(o.getName());
    }
}
