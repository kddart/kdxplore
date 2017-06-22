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
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.event.EventListenerList;

import com.diversityarrays.util.Check;
import com.diversityarrays.util.Origin;
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
        ORIGIN, // the four Corner icons
        // ones above here have dialog editors
        MINIMUM_CELL_COUNT,
        SPATIALS_REQUIRED,
        CONTENT,
        ;
    }

    static public enum Attribute {
        DIMENSION("RxC", Dimension.class, "Size"),
        POSITION("X,Y", Point.class, "Position"),
        BORDERS("Borders", String.class, "Borders"),
        ORIGIN("Origin", Origin.class, "Origin")
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
            case ORIGIN:
                return t.getOrigin();
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
    private int spatialChecksRequired;

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
        this.spatialChecksRequired = nSpatials;
    }

	public Set<WhatChanged> updateDesignParameters(
	        DesignParams designParams
//	        , Function<DesignEntry, E> contentFactory
	        )
    {
        Set<WhatChanged> whatChanged = new HashSet<>();

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

        if (designParams.nSpatials != spatialChecksRequired) {
            spatialChecksRequired = designParams.nSpatials;
            whatChanged.add(WhatChanged.SPATIALS_REQUIRED);
        }

//        if (designParams.manualMode) {
//            // Manual positioning - we don't have XY
//        }
//        else {
//
//            Optional<List<PositionedDesignEntry<DesignEntry>>> optEntries =
//                    designParams.getEntries(replicateNumber);
//
//            if (optEntries.isPresent()) {
//                Optional<PositionedDesignEntry<DesignEntry>> foundWithout = optEntries.get().stream()
//                    .filter(pde -> ! pde.getWhere().isPresent())
//                    .findFirst();
//                if (foundWithout.isPresent()) {
//                    throw new IllegalStateException(
//                            "Entry found without XYPos:" + foundWithout.get().getEntry().getEntryName());
//                }
//
//                Map<Point, E> map = optEntries.get().stream()
//                    .collect(Collectors.toMap(
//                            (pde) -> {
//                                Optional<XYPos> optxy = pde.getWhere();
//                                XYPos xy = optxy.orElse(new XYPos(-1,-1));
//                                return new Point(xy.x, xy.y);
//                                },
//                            (pde) -> contentFactory.apply(pde.getEntry())));
//
//                setContentUsingMap(map);
//
//                // Either of these could have changed, err on the side of caution
//                // and assume that BOTH have changed.
//                whatChanged.add(WhatChanged.POSITION);
//                whatChanged.add(WhatChanged.ENTRY_TYPES);
//            }
//        }
        return whatChanged;
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
        int index = side.ordinal();
        int oldCount = borderCountBySide[index];
        borderCountBySide[index] = Math.max(0, count);
        if (borderCountBySide[index] != oldCount) {
            fireChanges(WhatChanged.BORDER);
        }
    }

    public int getReplicateNumber() {
        return replicateNumber;
    }

    public boolean getCanEditSize() {
        return canEditSize;
    }

    public void setCanEditSize(boolean b) {
        canEditSize = b;
//        fireChanges();
    }

    public String getName() {
        return name;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setTemporaryColumnCount(int columnCount) {
        setColumnCount(columnCount, true);
    }
    public void setFinalColumnCount(int columnCount) {
        setColumnCount(columnCount, false);
    }

    private void setColumnCount(int columnCount, boolean temporary) {
        this.columnCount = columnCount;
        removeContentOutsideBounds(temporary);
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setTemporaryRowCount(int rowCount) {
        setRowCount(rowCount, true);
    }
    public void setFinalRowCount(int rowCount) {
        setRowCount(rowCount, false);
    }
    private void setRowCount(int rowCount, boolean temporary) {
        this.rowCount = rowCount;
        removeContentOutsideBounds(temporary);
    }

    private void removeContentOutsideBounds(boolean temporary) {
        if (! temporary) {
            List<Point> pointsToRemove = contentByPoint.keySet().stream()
                    .filter(pt -> pt.x >= columnCount || pt.y >= rowCount)
                    .collect(Collectors.toList());
                if (! pointsToRemove.isEmpty()) {
                    removeContentAt(pointsToRemove);
                }
        }
        fireChanges(WhatChanged.DIMENSION);

    }

    public Dimension getSize() {
        return new Dimension(columnCount, rowCount);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        fireChanges(WhatChanged.POSITION);
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        fireChanges(WhatChanged.POSITION);
    }

    public void setSpatialChecksRequired(int v) {
        this.spatialChecksRequired = v;
        fireChanges(WhatChanged.SPATIALS_REQUIRED);
    }
    public int getSpatialChecksRequired() {
        return spatialChecksRequired;
    }

    @Transient
    public Rectangle getRectangle() {
        return new Rectangle(x, y, columnCount, rowCount);
    }

    @Transient
    public boolean contains(Point pt) {
        return getRectangle().contains(pt);
    }

    public Optional<Integer> getFillerCount() {
        Integer result = null;
        if (minimumCellCount != null) {
            int excess = (columnCount * rowCount) - minimumCellCount;
            if (excess > 0) {
                result = excess;
            }
        }
        return Optional.ofNullable(result);
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public boolean isEmpty() {
        return contentByPoint.isEmpty();
    }

    public Map<Point, E> getContentByPoint() {
        return Collections.unmodifiableMap(contentByPoint);
    }

    public Collection<E> getAllContents() {
        return Collections.unmodifiableCollection(contentByPoint.values());
    }

    public void visitContentByPoint(BiPredicate<Point, E> visitor) {
        for (Map.Entry<Point, E> me : contentByPoint.entrySet()) {
            if (! visitor.test(me.getKey(), me.getValue())) {
                break;
            }
        }
    }

    private final Map<Point, E> contentByPoint = new HashMap<>();

    private Origin origin = Origin.LOWER_LEFT;

    public int getUserXcoord(int x) {
        return OriginCoordTransform.getUserXcoord(origin, x, columnCount);
    }

    public int getUserYcoord(int y) {
        return OriginCoordTransform.getUserYcoord(origin, y, rowCount);
    }

    public void clearContent(Predicate<E> discardThese) {
        Map<Point, E> oldContentByPoint = new HashMap<>(contentByPoint);
        Map<Point, E> newContent = null;
        if (discardThese != null) {
            newContent = contentByPoint.entrySet().stream()
                    // Note: inversion of the predicate
                    // i.e. we keep the ones we do NOT discard
                    .filter(e -> ! discardThese.test(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        setNewContent(oldContentByPoint, newContent);
    }

    private void setNewContent(Map<Point, E> oldContentByPoint, Map<Point, E> newContent) {
        contentByPoint.clear();
        if (newContent != null) {
            contentByPoint.putAll(newContent);
        }
        fireChanges(WhatChanged.CONTENT, oldContentByPoint);
    }

    public void removeContentAt(Collection<Point> points) {
        setContentAtPoints(null, points);
    }

    public Collection<Point> setContentUsingMap(Map<Point, E> newContentByPoint) {
        if (Check.isEmpty(newContentByPoint)) {
            return Collections.emptyList();
        }

        Map<Point, E> oldContentByPoint = new HashMap<>();
        newContentByPoint.entrySet().stream()
            .forEach(e -> setContentAt(e.getKey(), e.getValue(), oldContentByPoint));

        if (! oldContentByPoint.isEmpty()) {
            fireChanges(WhatChanged.CONTENT, oldContentByPoint);
        }
        return oldContentByPoint.keySet();
    }

    public Collection<Point> setContentAtPoints(E newContent, Collection<Point> points) {
        HashMap<Point, E> oldContentByPoint = new HashMap<>();
        for (Point pt : points) {
            setContentAt(pt, newContent, oldContentByPoint);
        }
        if (! oldContentByPoint.isEmpty()) {
            fireChanges(WhatChanged.CONTENT, oldContentByPoint);
        }
        return oldContentByPoint.keySet();
    }

    public Collection<Point> setContentAt(E newContent, Point point) {
        if (point != null) {
            HashMap<Point, E> oldContentByPoint = new HashMap<>();
            setContentAt(point, newContent, oldContentByPoint);
            if (! oldContentByPoint.isEmpty()) {
                fireChanges(WhatChanged.CONTENT, oldContentByPoint);
            }
            return oldContentByPoint.keySet();
        }
        return Collections.emptyList();
    }

    /**
     * Order of parameters is deliberately different to the public methods.
     * @param where
     * @param newContent
     * @param oldContentByPoint
     */
    private void setContentAt(
            Point where,
            E newContent,
            Map<Point, E> oldContentByPoint)
    {
        // Copy in case caller who supplied it changes it!
        Point pt = new Point(where);
        E oldContent = contentByPoint.remove(pt);
        if (newContent != null) {
            contentByPoint.put(pt, newContent);
        }

        if (newContent != null) {
            if (! newContent.equals(oldContent)) {
                oldContentByPoint.put(pt, oldContent);
            }
        }
        else {
            // newContent == null
            if (oldContent != null) {
                oldContentByPoint.put(pt, oldContent);
            }
        }
    }

    public int getContentCount(Predicate<E> onlyThese) {
        if (onlyThese == null) {
            return contentByPoint.size();
        }
        int result = (int) contentByPoint.values().stream().filter(onlyThese).count();
        return result;
    }

    @Override
    public int compareTo(PlantingBlock<?> o) {
        return this.getName().compareToIgnoreCase(o.getName());
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin o) {
        origin = o;
        fireChanges(WhatChanged.ORIGIN);
    }

    static public interface PlantingBlockChangeListener<E> extends EventListener {
        void blockChanged(PlantingBlock<E> source,
                WhatChanged whatChanged,
                Map<Point, E> oldContentByPoint);
    }

    private transient EventListenerList listenerList = new EventListenerList();

    public void addPlantingBlockChangeListener(PlantingBlockChangeListener<E> l) {
        listenerList.add(PlantingBlockChangeListener.class, l);
    }

    public void removePlantingBlockChangeListener(PlantingBlockChangeListener<E> l) {
        listenerList.remove(PlantingBlockChangeListener.class, l);
    }

    protected void fireChanges(WhatChanged whatChanged) {
        fireChanges(whatChanged, Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    protected void fireChanges(WhatChanged whatChanged, Map<Point, E> oldContentByPoint) {
        for (PlantingBlockChangeListener<E> l : listenerList.getListeners(PlantingBlockChangeListener.class)) {
            l.blockChanged(this, whatChanged, oldContentByPoint);
        }
    }
}
