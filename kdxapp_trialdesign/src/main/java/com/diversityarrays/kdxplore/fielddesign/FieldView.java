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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockTableModel;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;

import net.pearcan.color.ColorPair;

/**
 * A <code>FieldView</code> is a rectangular (for now) grid of <code>FieldCell</code>s.
 * <p>
 * The <code>FieldCell</code>s are rendered using a <code>FieldCellRenderer</code>
 * @author brianp
 *
 * @param <E> identifies the type of content for the cells in the PlantingBlocks
 */
@SuppressWarnings("nls")
public class FieldView<E> extends JComponent implements Scrollable {

    public static final float MINIMUM_OPACITY = 0.1f;
    public static final float MAXIMUM_OPACITY = 0.9f;

    static public enum BlankTile {
        LIGHT("blank_tile_light.png"),
        MEDIUM("blank_tile_medium.png"),
        DARK("blank_tile_dark.png"),
        ;

        public final String resourceName;
        BlankTile(String rn) {
            resourceName = rn;
        }
    }

    public static final String PROPERTY_DRAWING_ENTRIES = "drawingEntries";

    private static final int INITIAL_CELL_WH = 20;

    private static final Color DARK_GREEN = Color.decode("#006600"); //$NON-NLS-1$


    public static final String PROPERTY_PLANTING_BLOCK_MODEL = "plantingBlockModel";
    private static final Color ACTIVE_BLOCK_BORDER_COLOR = Color.RED;
    private static final float ACTIVE_BLOCK_BORDER_WIDTH = 3.0f;

    private static final Color SELECTED_BLOCK_BORDER_COLOR = Color.BLUE;
    private static final float SELECTED_BLOCK_BORDER_WIDTH = 3.0f;
    private static final float NORMAL_BLOCK_BORDER_WIDTH = 1.0f;

    public static final String PROPERTY_FIELD_VIEW_COLUMN_HEADER = "fieldViewColumnHeader";
    public static final String PROPERTY_FIELD_VIEW_ROW_HEADER = "fieldViewRowHeader";

    public static final String PROPERTY_CELL_DIMENSION = "cellDimension";

    public static final String PROPERTY_MODEL = "model";

    public static final String PROPERTY_BLANK_TILE = "blankTile";

    public static final String PROPERTY_VISIBLE_COLUMN_COUNT = "visibleColumnCount";

    public static final String PROPERTY_VISIBLE_ROW_COUNT = "visibleRowCount";

    public static final String PROPERTY_CELL_SHAPE = "cellShape";

    private static final boolean DEBUG = Boolean.getBoolean("FieldView.DEBUG");
    private static final boolean DEBUG_SCROLLABLE = Boolean.getBoolean("FieldView.DEBUG_SCROLLABLE");

    private static final int MINIMUM_CELL_WIDTH = 4;
    private static final int MAXIMUM_CELL_WIDTH = 200;

    private static final int MINIMUM_CELL_HEIGHT = 4;
    private static final int MAXIMUM_CELL_HEIGHT = 200;

    // Use this to paint FieldCells when there is no FieldCellRenderer assigned
    private static final boolean PAINT_FIELD_CELLS = Boolean.getBoolean(
            FieldView.class.getName()+".PAINT_FIELD_CELLS");

    private FieldModel model;
    private PlantingBlockTableModel<E> plantingBlockTableModel;

    private CellShape cellShape = CellShape.SQUARE;
    private Dimension cellDimension = cellShape.getDimensionForSide(INITIAL_CELL_WH);

    private ColumnHeader<E> fieldViewColumnHeader;
    private RowHeader<E> fieldViewRowHeader;

    private final int minimumCellWidth = MINIMUM_CELL_WIDTH;
    private final int minimumCellHeight = MINIMUM_CELL_HEIGHT;

    private BlankTile blankTile; // = BlankTile.DARK;
    private BufferedImage blankTileImage;

//    private JViewport viewport;
    private Container viewportParent;
    private ComponentListener viewportResizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            if (DEBUG) {
                System.out.println("FieldView.viewportResized");
            }
            paintOffset = null;
            establishPreferredViewportSize();
        }
    };


    private final FieldModelListener fieldModelListener = new FieldModelListener() {
        @Override
        public void fieldDimensionChanged(Object source) {
            recomputeSizes();

            establishMinimumAndPreferredSize();
            resizeAndRepaint();
        }

        @Override
        public void bordersChanged(Object source, Point pt) {
            repaint();
        }
    };

//    private Color gridColor = Color.LIGHT_GRAY;

    private TableModelListener plantingBlockTableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            recomputeSizes();
        }
    };

    private FieldViewFunctions<E> fieldViewFunctions = new FieldViewFunctions<E>() {

        @Override
        public void setCursor(Cursor c) {
            FieldView.this.setCursor(c);
        }

        @Override
        public Graphics getGraphics() {
            return FieldView.this.getGraphics();
        }

        @Override
        public void repaint() {
            FieldView.this.repaint();
        }

        @Override
        public void blockPositionChanged(PlantingBlock<E> block) {
            plantingBlockTableModel.blockChanged(WhatChanged.POSITION, block);
        }

        @Override
        public boolean isAnyBlockContaining(Point pt, PlantingBlock<E> exceptFor) {
            return FieldView.this.isAnyBlockContaining(pt, exceptFor);
        }

        @Override
        public List<PlantingBlock<E>> findBlocksContaining(Point pt, PlantingBlock<E> exceptFor) {
            return FieldView.this.findBlocksContaining(pt, exceptFor);
        }

        @Override
        public Optional<PlantingBlock<E>> findFirstPlantingBlock(Predicate<PlantingBlock<E>> predicate) {
            return plantingBlockTableModel.getPlantingBlocks().stream()
                .filter(predicate)
                .findFirst();
        }

        @Override
        public Point viewToModel(Point pt) {
            return FieldView.this.viewToModel(pt);
        }

        @Override
        public Point modelToView(Point pt) {
            return FieldView.this.modelToView(pt);
        }

        @Override
        public boolean isAnyOtherBlockIntersecting(PlantingBlock<E> block) {
            if (plantingBlockTableModel == null) {
                return false;
            }
            return plantingBlockTableModel.isAnyOtherBlockIntersecting(block);
        }

        @Override
        public void blockEntryTypesChanged(PlantingBlock<E> block) {
            plantingBlockTableModel.blockChanged(WhatChanged.ENTRY_TYPES, block);
        }
    };

    private final FieldViewMouseListener<E> fieldViewMouseListener;

    private Predicate<FieldCell<?>> fieldCellSelectedPredicate;
    private FieldCellRenderer fieldCellRenderer;
    private PlantingBlockRenderer<E> plantingBlockRenderer;

    private final Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked;

    public FieldView(FieldModel m,
            PlantingBlockTableModel<E> tm,
            Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked)
    {
        setOpaque(true);
        setBackground(Color.GRAY);
        setForeground(Color.LIGHT_GRAY);

        setModel(m);

        this.onBlockClicked = onBlockClicked;
        fieldViewMouseListener = new FieldViewMouseListener<>(model, fieldViewFunctions, onBlockClicked);

        initializeLocalVars();

        setPlantingBlockTableModel(tm);

//        addPropertyChangeListener("font", fontChangeListener);

        addMouseListener(fieldViewMouseListener);
        addMouseMotionListener(fieldViewMouseListener);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize("componentResized");
            }
        });

        addHierarchyListener(new HierarchyListener() {
//            boolean everShown;

            @Override
            public void hierarchyChanged(HierarchyEvent e) {

//                if (! everShown) {
//                    everShown = true;
//                    updateMinimumCellDimension();
//                }

                Container container = e.getChangedParent();
                if (container == null) {
                    if (viewportParent != null) {
                        viewportParent.removeComponentListener(viewportResizeListener);
                    }
                    viewportParent = null;
                }
                else {
                    if (container instanceof JViewport) {
                        if (viewportParent != null) {
                            viewportParent.removeComponentListener(viewportResizeListener);
                        }
                        JViewport viewport = (JViewport) container;
                        viewportParent = viewport.getParent();
                        if (viewportParent != null) {
                            viewportParent.addComponentListener(viewportResizeListener);
                        }
                    }
                }

                if (DEBUG_SCROLLABLE) {
                    StringBuilder sb = new StringBuilder("FieldView.hierarchyChanged: ");
                    switch (e.getID()) {
                    case HierarchyEvent.HIERARCHY_CHANGED:
                        sb.append("HIERARCHY_CHANGED");
                        break;
                    case HierarchyEvent.ANCESTOR_MOVED:
                        sb.append("ANCESTOR_MOVED");
                        break;
                    case HierarchyEvent.ANCESTOR_RESIZED:
                        sb.append("ANCESTOR_RESIZED");
                        break;
                    default:
                        sb.append("ID=").append(e.getID());
                        break;
                    }
                    if (container == null) {
                        sb.append("\tparent=null");
                    }
                    else {
                        sb.append("\tparent= (").append(container.getClass().getName()).append(')');
                    }

                    long changeFlags = e.getChangeFlags();
                    if (changeFlags != 0) {
                        sb.append(", changeFlags=").append(Long.toHexString(changeFlags));
                        if (0 != (changeFlags & HierarchyEvent.PARENT_CHANGED)) {
                            sb.append(' ').append("PARENT");
                        }
                        if (0 != (changeFlags & HierarchyEvent.DISPLAYABILITY_CHANGED)) {
                            sb.append(' ').append("DISPLAYABILITY");
                        }
                        if (0 != (changeFlags & HierarchyEvent.SHOWING_CHANGED)) {
                            sb.append(' ').append("SHOWING");
                        }
                    }

                    System.out.println(sb.toString());

                }
            }
        });
    }

    private PlantingBlockEditMouseListener<E> plantingBlockEditMouseListener;

    private boolean drawingEntries = true;

    public boolean isDrawingEntries() {
        return drawingEntries;
    }

    public void setDrawingEntries(boolean draw) {
        boolean oldValue = drawingEntries;
        this.drawingEntries = draw;
        if (plantingBlockRenderer != null) {
            plantingBlockRenderer.setDrawingEntries(drawingEntries);
        }
        repaint();
        firePropertyChange(PROPERTY_DRAWING_ENTRIES, oldValue, drawingEntries);
    }

    public boolean isEditingPlantingBlock() {
        return plantingBlockEditMouseListener != null;
    }

    public void setEditingPlantingBlock(PlantingBlock<E> tb) {
        if (tb == null) {
            if (plantingBlockEditMouseListener != null) {
                removeMouseListener(plantingBlockEditMouseListener);
                removeMouseMotionListener(plantingBlockEditMouseListener);

                plantingBlockEditMouseListener = null;

                addMouseListener(fieldViewMouseListener);
                addMouseMotionListener(fieldViewMouseListener);
            }
            else {
                // nothing to change
            }
        }
        else {
            if (plantingBlockEditMouseListener != null) {
                removeMouseListener(plantingBlockEditMouseListener);
                removeMouseMotionListener(plantingBlockEditMouseListener);
            }
            else {
                removeMouseListener(fieldViewMouseListener);
                removeMouseMotionListener(fieldViewMouseListener);
            }

            plantingBlockEditMouseListener = new PlantingBlockEditMouseListener<>(tb, fieldViewFunctions, onBlockClicked);
            plantingBlockEditMouseListener.setContentFactory(contentFactory);

            addMouseListener(plantingBlockEditMouseListener);
            addMouseMotionListener(plantingBlockEditMouseListener);
        }

        if (plantingBlockRenderer != null) {
            if (! drawingEntries) {
                setDrawingEntries(true);
            }
        }
        repaint();
    }

    private BiFunction<PlantingBlock<E>, Point, E> contentFactory;

    public void setContentFactory(BiFunction<PlantingBlock<E>, Point, E> f) {
        contentFactory = f;;
        if (plantingBlockEditMouseListener != null) {
            plantingBlockEditMouseListener.setContentFactory(contentFactory);
        }
    }

    public void setPlantingBlockRenderer(PlantingBlockRenderer<E> r) {
        plantingBlockRenderer = r;
        if (plantingBlockRenderer != null) {
            plantingBlockRenderer.setDrawingEntries(drawingEntries);
        }
        repaint();
    }

    public void setFieldCellRenderer(FieldCellRenderer r) {
        fieldCellRenderer = r;
        repaint();
    }

    public void setFieldCellSelectedPredicate(Predicate<FieldCell<?>> predicate) {
        fieldCellSelectedPredicate = predicate;
        repaint();
    }

    protected void handleResize(String fromWhere) {
        if (DEBUG) System.out.println("==== handleResize(" + fromWhere + ")");

        paintOffset = null;
        establishPreferredViewportSize();
        recomputeSizes();
        revalidate();
    }

    public void autoFitCellDimension() {

        Dimension size = getBounds().getSize();

        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            Container grandParent = parent.getParent();
            if (grandParent instanceof JScrollPane) {
                Dimension vsize = getViewportSize();
                size = grandParent.getBounds().getSize();
                Insets insets = grandParent.getInsets();
                size.width -= (insets.left + insets.right);
                size.height -= (insets.top + insets.bottom);
                System.out.println(String.format("vsize=%d x %d\tsize=%d x %d",
                        vsize.width, vsize.height, size.width, size.height));
            }
        }

        int nWide = Math.max(minimumCellWidth, Math.floorDiv(size.width, model.getColumnCount()));
        int nHigh = Math.max(minimumCellHeight, Math.floorDiv(size.height, model.getRowCount()));

        int n = nWide * model.getColumnCount();
        while (n >= size.width) {
            --nWide;
            n = nWide * model.getColumnCount();
        }

        n = nHigh * model.getRowCount();
        while (n >= size.height) {
            --nHigh;
            n = nHigh * model.getRowCount();
        }

        setCellDimension(nWide, nHigh);
    }

    public void setPlantingBlockTableModel(PlantingBlockTableModel<E> pbtm) {
        if (this.plantingBlockTableModelListener != pbtm) {
            PlantingBlockTableModel<E> oldValue = plantingBlockTableModel;
            if (plantingBlockTableModel != null) {
                plantingBlockTableModel.removeTableModelListener(plantingBlockTableModelListener);
            }
            this.plantingBlockTableModel = pbtm;
            if (plantingBlockTableModel != null) {
                plantingBlockTableModel.addTableModelListener(plantingBlockTableModelListener);
            }
            firePropertyChange(PROPERTY_PLANTING_BLOCK_MODEL, oldValue, plantingBlockTableModel);
            repaint();
        }
    }

    protected ColumnHeader<E> createFieldViewColumnHeader() {
        return null;
    }

    protected RowHeader<E> createFieldViewRowHeader() {
        return null;
    }

    public FieldModel getModel() {
        return model;
    }

    protected void initializeLocalVars() {
        visibleColumnCount = 0;
        visibleRowCount = 0;
//        setBlankTile(BlankTile.DARK);
        setFieldViewColumnHeader(createFieldViewColumnHeader());
        setFieldViewRowHeader(createFieldViewRowHeader());
        establishMinimumAndPreferredSize();
//        establishPreferredViewportSize();
    }

    public void setBlankTile(BlankTile value) {
        BlankTile old = blankTile;
        blankTile = value;

        if (blankTile == null) {
            blankTileImage = null;
        }
        else {
            InputStream is = FieldView.class.getResourceAsStream(blankTile.resourceName);
            if (is == null) {
                System.err.println("Missing resource: " + blankTile.resourceName);
                blankTileImage = null;
            }
            else {
                try {
                    blankTileImage = ImageIO.read(is);
                }
                catch (IOException e) {
                    System.err.println("Resource error: " + blankTile.resourceName
                            + " (" + e.getMessage() + ")");
                }
            }
        }

        if (old != blankTile) {
            firePropertyChange(PROPERTY_BLANK_TILE, old, blankTile);
            repaint();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        configureEnclosingScrollPane();
    }

    protected void configureEnclosingScrollPane() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            JViewport port = (JViewport) parent;
            Container gp = port.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane)gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null ||
                        SwingUtilities.getUnwrappedView(viewport) != this) {
                    return;
                }
                scrollPane.setColumnHeaderView(getFieldViewColumnHeader());
                scrollPane.setRowHeaderView(getFieldViewRowHeader());
//                // configure the scrollpane for any LAF dependent settings
//                configureEnclosingScrollPaneUI();
            }
        }
    }

    @Override
    public void removeNotify() {
        unconfigureEnclosingScrollPane();
        super.removeNotify();
    }

    protected void unconfigureEnclosingScrollPane() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            JViewport port = (JViewport) parent;
            Container gp = port.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane)gp;
                // Make certain we are the viewPort's view and not, for
                // example, the rowHeaderView of the scrollPane -
                // an implementor of fixed columns might do this.
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null ||
                        SwingUtilities.getUnwrappedView(viewport) != this) {
                    return;
                }
                scrollPane.setColumnHeaderView(null);
                // remove ScrollPane corner if one was added by the LAF
                Component corner =
                        scrollPane.getCorner(JScrollPane.UPPER_TRAILING_CORNER);
                if (corner instanceof UIResource){
                    scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER,
                            null);
                }
            }
        }
    }

    public RowHeader<E> getFieldViewRowHeader() {
        return fieldViewRowHeader;
    }

    public void setFieldViewRowHeader(RowHeader<E> newHeader) {
        if (this.fieldViewRowHeader != newHeader) {
            RowHeader<E> old = this.fieldViewRowHeader;
            if (old != null) {
                old.setFieldView(null);
            }
            this.fieldViewRowHeader = newHeader;
            if (this.fieldViewRowHeader != null) {
                this.fieldViewRowHeader.setFieldView(this);
            }
            firePropertyChange(PROPERTY_FIELD_VIEW_ROW_HEADER, old, this.fieldViewRowHeader);
        }
    }

    public ColumnHeader<E> getFieldViewColumnHeader() {
        return fieldViewColumnHeader;
    }

    public void setFieldViewColumnHeader(ColumnHeader<E> newHeader) {
        if (this.fieldViewColumnHeader != newHeader) {
            ColumnHeader<E> old = this.fieldViewColumnHeader;
            if (old != null) {
                old.setFieldView(null);
            }
            this.fieldViewColumnHeader = newHeader;
            if (this.fieldViewColumnHeader != null) {
                this.fieldViewColumnHeader.setFieldView(this);
            }
            firePropertyChange(PROPERTY_FIELD_VIEW_COLUMN_HEADER, old, this.fieldViewColumnHeader);
        }
    }

    static public class ColumnHeader<E> extends JComponent {

        public void setFieldView(FieldView<E> view) {

        }
    }

    static public class RowHeader<E> extends JComponent {

        public void setFieldView(FieldView<E> view) {

        }
    }

    private void establishMinimumAndPreferredSize() {
//        int minWid = getCellWidth() * model.getColumnCount();
//        int minHyt = getCellHeight() * model.getRowCount();

//        Dimension d = new Dimension(minWid, minHyt);
        Dimension d = getViewportSize();
        setPreferredScrollableViewportSize(d);
        setMinimumSize(d);
        setPreferredSize(d);
    }

    private void establishPreferredViewportSize() {
        Dimension d = getViewportSize();
        if (DEBUG) {
            System.out.println(String.format("establishPreferredViewportSize: %d x %d", d.width, d.height));
        }
        setPreferredScrollableViewportSize(d);
        setPreferredSize(d);
    }

    private Dimension getViewportSize() {

        int nCols = (visibleColumnCount <= 0) ? model.getColumnCount() : visibleColumnCount;
        int nRows = (visibleRowCount    <= 0) ? model.getRowCount()    : visibleRowCount;
        Dimension size = new Dimension(getCellWidth() * nCols, getCellHeight() * nRows);

        Dimension result;
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            result = parent.getSize();
            int wid = Math.max(size.width, result.width);
            int hyt = Math.max(size.height, result.height);
            result = new Dimension(wid, hyt);
        }
        else {
            result = size;
        }

        return result;
    }

    protected Dimension getZoomDimension(boolean zoomIn) {
        Dimension newDim;

        int cellWidth = cellDimension.width;
        int cellHeight = cellDimension.height;
        if (zoomIn) {
            newDim = new Dimension(cellWidth + (cellWidth / 2), cellHeight + (cellHeight / 2));
            if (newDim.width >= MAXIMUM_CELL_WIDTH) {
                return null;
            }
            if (newDim.height >= MAXIMUM_CELL_HEIGHT) {
                return null;
            }
            return newDim;
        }

        newDim = new Dimension(Math.floorDiv(cellWidth * 2, 3), Math.floorDiv(cellHeight * 2, 3));
        if (newDim.width < minimumCellWidth) {
            return null;
        }
        if  (newDim.height < minimumCellHeight) {
            return null;
        }
        return newDim;
    }

    public boolean canZoomIn() {
        return null != getZoomDimension(true);
    }

    public void zoomIn() {
        Dimension d = getZoomDimension(true);
        if (d != null) {
            setCellDimension(d.width, d.height);
        }
    }

    public boolean canZoomOut() {
        return null != getZoomDimension(false);
    }

    public void zoomOut() {
        Dimension d = getZoomDimension(false);
        if (d != null) {
            setCellDimension(d.width, d.height);
        }
    }

    public CellShape getCellShape() {
        return cellShape;
    }

    public void setCellShape(CellShape newShape) {
        if (newShape == null) {
            throw new IllegalArgumentException("cellShape cannot be null");
        }
        if (this.cellShape == newShape) {
            return;
        }
        CellShape oldShape = this.cellShape;
        switch (newShape) {
        case LANDSCAPE:
            if (CellShape.PORTRAIT == oldShape) {
                cellShape = newShape;
                setCellDimension(cellDimension.height, cellDimension.width); // swap
            }
            else if (CellShape.SQUARE == oldShape) {
                cellShape = newShape;
                setCellWidth(cellDimension.width + cellDimension.width / 2);
            }
            break;
        case PORTRAIT:
            if (CellShape.LANDSCAPE == oldShape) {
                cellShape = newShape;
                setCellDimension(cellDimension.height, cellDimension.width); // swap
            }
            else if (CellShape.SQUARE == oldShape) {
                cellShape = newShape;
                setCellHeight(cellDimension.height + cellDimension.height / 2);
            }
            break;
        case SQUARE:
            if (CellShape.LANDSCAPE == oldShape ||  CellShape.PORTRAIT == oldShape) {
                cellShape = newShape;
                int min = Math.min(cellDimension.width, cellDimension.width);
                setCellDimension(min, min);
            }
            break;
        default:
            throw new RuntimeException("Unhandled CellShape: " + newShape);
        }
        firePropertyChange(PROPERTY_CELL_SHAPE, oldShape, cellShape);
    }

    public int getCellWidth() {
        return cellDimension.width;
    }

    public int getCellHeight() {
        return cellDimension.height;
    }

    public void setCellWidth(int width) {

        Dimension oldDimension = cellDimension;
        cellDimension = new Dimension(Math.max(1, width), cellDimension.height);
        if (! cellDimension.equals(oldDimension)) {
            establishMinimumAndPreferredSize();
        }
        recomputeSizes();

        resizeAndRepaint();

        firePropertyChange(PROPERTY_CELL_DIMENSION,
                oldDimension,
                getCellDimension());
    }

    public Dimension getCellDimension() {
        return new Dimension(this.cellDimension);
    }

    public void setCellDimension(int width, int height) {

        Dimension oldDimension = cellDimension;
        cellDimension = new Dimension(Math.max(1, width), Math.max(1, height));

        if (! cellDimension.equals(oldDimension)) {
            establishMinimumAndPreferredSize();
        }

        recomputeSizes();

        resizeAndRepaint();

        firePropertyChange(PROPERTY_CELL_DIMENSION,
                oldDimension,
                getCellDimension());
    }

    public void setCellHeight(int height) {

        Dimension oldDimension = cellDimension;
        cellDimension = new Dimension(cellDimension.width, Math.max(1, height));
        if (! cellDimension.equals(oldDimension)) {
            establishMinimumAndPreferredSize();
        }
        recomputeSizes();

        resizeAndRepaint();

        firePropertyChange(PROPERTY_CELL_DIMENSION,
                oldDimension,
                getCellDimension());
    }

    public void setModel(FieldModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("Model must be non-null");
        }

        if (model != newModel) {
            FieldModel oldModel = model;
            if (model != null) {
                model.removeFieldModelListener(fieldModelListener);
            }
            model = newModel;
            model.addFieldModelListener(fieldModelListener);

            establishMinimumAndPreferredSize();
            firePropertyChange(PROPERTY_MODEL, oldModel, model);
        }

//        setPreferredSize(d);
    }

    protected void resizeAndRepaint() {
        paintOffset = null;
        revalidate();
        repaint();
    }

    private Rectangle paintOffset = null;

    @Override
    public void invalidate() {
        if (DEBUG) {
            System.out.println("FieldView.invalidate");
        }
        super.invalidate();
        paintOffset = null;
    }

    public Rectangle getPaintOffset() {
        if (paintOffset == null) {
            Dimension size = getSize();
            Container parent = SwingUtilities.getUnwrappedParent(this);
            if (parent instanceof JViewport) {
                Dimension parentSize = parent.getSize();
                int xoff = Math.max(0, (parentSize.width - size.width) / 2);
                int yoff = Math.max(0, (parentSize.height - size.height) / 2);
                paintOffset = new Rectangle(xoff, yoff, size.width, size.height);
            }
            else {
                paintOffset = new Rectangle(0, 0, size.width, size.height);
            }
        }
        return paintOffset;
    }

    private Rectangle getFieldRectangle() {
        int wid = model.getColumnCount() * getCellWidth();
        int hyt = model.getRowCount() * getCellHeight();

//        Rectangle bb = parentBounds==null ? bounds : parentBounds;

        Rectangle bb = getPaintOffset(); // bounds; // parentBounds==null ? bounds : parentBounds;
        int xbase = wid <= bb.width  ? (bb.width - wid)  / 2 : 0;
        int ybase = hyt <= bb.height ? (bb.height - hyt) / 2 : 0;

        return new Rectangle(xbase, ybase, wid, hyt);
    }

    private Point getDrawingOrigin() {
        int wid = model.getColumnCount() * getCellWidth();
        int hyt = model.getRowCount() * getCellHeight();

//        Rectangle bb = parentBounds==null ? bounds : parentBounds;

        Rectangle bb = getPaintOffset(); // bounds; // parentBounds==null ? bounds : parentBounds;
        int xbase = wid <= bb.width  ? (bb.width - wid)  / 2 : 0;
        int ybase = hyt <= bb.height ? (bb.height - hyt) / 2 : 0;

        return new Point(xbase, ybase);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (! isOpaque()) {
            super.paintComponent(g);
        }

        Rectangle bounds = getBounds();
//        Rectangle clipBounds = g.getClipBounds();
//        Rectangle parentBounds = null;
//        Container parent = SwingUtilities.getUnwrappedParent(this);
//        if (parent instanceof JViewport) {
//            parentBounds = parent.getBounds();
//        }
        g.setColor(getBackground());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        if (blankTileImage != null) {
            int imWid = blankTileImage.getWidth();
            int imHyt = blankTileImage.getHeight();

            for (int im_x = bounds.x; im_x < bounds.width; im_x += imWid) {
                int draw_wid = imWid;
                if ((im_x + imWid) > bounds.width) {
                    draw_wid = bounds.width - (im_x + imWid);
                }

                for (int im_y = bounds.y; im_y < bounds.height; im_y += imHyt) {
                    int draw_hyt = imHyt;
                    if ((im_y + imHyt) > bounds.height) {
                        draw_hyt = bounds.height - (im_y + imHyt);
                    }

                    g.drawImage(blankTileImage, im_x, im_y, draw_wid, draw_hyt, null);
                }
            }
        }

        g.setColor(getForeground());
//        g.setColor(gridColor);
        int cw = getCellWidth();
        int ch = getCellHeight();

        int wid = model.getColumnCount() * cw;
        int hyt = model.getRowCount() * ch;

        Point origin = getDrawingOrigin();
        int xOrigin = origin.x;
        int yOrigin = origin.y;
        if (DEBUG) {
            System.out.println(String.format("\tcw,ch=%d,%d\twid,hyt=%d,%d\txbase,ybase=%d,%d",
                    cw,ch,  wid,hyt,  xOrigin,yOrigin));
        }

        for (int x = 0; x <= wid; x += cw ) {
            g.drawLine(xOrigin + x, yOrigin + 0, xOrigin + x, yOrigin + hyt);
        }

        for (int y = 0; y <= hyt; y += ch) {
            g.drawLine(xOrigin + 0, yOrigin + y, xOrigin + wid, yOrigin + y);
        }

        paintFieldCells((Graphics2D) g, origin);

        paintTriaBlocks((Graphics2D) g, origin);
    }

    private void paintFieldCells(Graphics2D g2d, Point origin) {
        if (model.getFieldCellCount() > 0) {
            int cw = getCellWidth();
            int ch = getCellHeight();

            Color save = g2d.getColor();

            Predicate<FieldCell<?>> visitor = null;
            if (fieldCellRenderer == null) {
                if (PAINT_FIELD_CELLS) {
                    g2d.setColor(DARK_GREEN);

                    visitor = new Predicate<FieldCell<?>>() {
                        @Override
                        public boolean test(FieldCell<?> cell) {
                            if (cell != null) {
                                Point pt = modelToView(cell.getLocation(), origin);
                                g2d.fillRect(pt.x, pt.y, cw, ch);
                            }
                            return true;
                        }
                    };
                }
            }
            else {
                visitor = new Predicate<FieldCell<?>>() {
                    @Override
                    public boolean test(FieldCell<?> cell) {
                        if (cell != null) {
                            Point pt = modelToView(cell.getLocation(), origin);
                            boolean isSelected = fieldCellSelectedPredicate==null
                                    ? false
                                    : fieldCellSelectedPredicate.test(cell);
                            fieldCellRenderer.draw(g2d, FieldView.this, cell, pt, cw, ch, isSelected);
                        }
                        return true;
                    }
                };
            }

            if (visitor != null) {
                model.visitFieldCells(visitor);
            }

            g2d.setColor(save);
        }
    }

    private float plantingBlockOpacity = 0.7f;
    private final Set<PlantingBlock<E>> selectedBlocks = new HashSet<>();

    public void addFieldViewSelectionListener(FieldViewSelectionListener l) {
        listenerList.add(FieldViewSelectionListener.class, l);
    }

    public void removeFieldViewSelectionListener(FieldViewSelectionListener l) {
        listenerList.remove(FieldViewSelectionListener.class, l);
    }

    protected void fireSelectionChanged() {
        for (FieldViewSelectionListener l : listenerList.getListeners(FieldViewSelectionListener.class)) {
            l.selectionChanged(this);
        }
    }

    public void setSelectedBlocks(Collection<PlantingBlock<E>> blocks) {
        selectedBlocks.clear();
        selectedBlocks.addAll(blocks);

        repaint();
        fireSelectionChanged();
    }

    /**
     * @return Either.right== the single selected block or Either.left==number of blocks
     */
    public Either<Integer, PlantingBlock<E>> getSingleSelectedBlock() {
        if (selectedBlocks.size() == 1) {
            for (PlantingBlock<E> tb : selectedBlocks) {
                return Either.right(tb);
            }
        }
        return Either.left(selectedBlocks.size());
    }

    static private final PlantingBlock<?>[] NO_BLOCKS = new PlantingBlock[0];

    public PlantingBlock<?>[] getSelectedBlocks() {
        if (Check.isEmpty(selectedBlocks)) {
            return NO_BLOCKS;
        }
        return selectedBlocks.toArray(new PlantingBlock[selectedBlocks.size()]);
    }

    private void paintTriaBlocks(Graphics2D g2d, Point xyBase) {
        if (plantingBlockTableModel == null || plantingBlockTableModel.getRowCount() <= 0) {
            return;
        }
        AlphaComposite transparent = null;
        transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, plantingBlockOpacity);
        Composite save = g2d.getComposite();

        FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getMaxAscent() + fm.getMaxDescent();

        PlantingBlock<E> activeBlock;
        if (plantingBlockEditMouseListener != null) {
            activeBlock = plantingBlockEditMouseListener.getPlantingBlock();
        }
        else {
            activeBlock = fieldViewMouseListener.getActivePlantingBlock();
        }

        for (PlantingBlock<E> tb : plantingBlockTableModel.getBlocksInDecreasingSize()) {
            if (activeBlock != tb) {
                paintOneBlock(g2d, tb, false, transparent, save, lineHeight);
            }
        }
        // Paint the active block last so it is "on top" of the others
        if (activeBlock != null) {
            paintOneBlock(g2d, activeBlock, true, transparent, save, lineHeight);
        }

        g2d.setComposite(save);
    }

    private void paintOneBlock(Graphics2D g2d,
            PlantingBlock<E> tb, boolean isActiveBlock,
            Composite transparent,
            Composite opaque,
            int lineHeight)
    {
        Rectangle rect = rectangleByTrial.get(tb);
        if (rect == null) {
            return;
        }

        Predicate<PlantingBlock<?>> pred = new Predicate<PlantingBlock<?>>() {
            @Override
            public boolean test(PlantingBlock<?> b) {
                if (tb != b) {
                    Rectangle r = rectangleByTrial.get(b);
                    if (r != null && rect.intersects(r)) {
                        return true;
                    }
                }
                return false;
            }
        };
        Optional<PlantingBlock<E>> opt_tb = plantingBlockTableModel.getPlantingBlocks().stream()
            .filter(pred)
            .findFirst();

        boolean intersectsOtherBlocks = opt_tb.isPresent();

        if (plantingBlockRenderer == null) {
            ColorPair cp = plantingBlockTableModel.getColorPair(tb);
            if (cp != null) {
                g2d.setComposite(transparent);
                g2d.setColor(cp.getBackground());
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);

                g2d.setComposite(opaque);
                if (isActiveBlock) {
//                    g2d.setComposite(opaque);
                    g2d.setColor(ACTIVE_BLOCK_BORDER_COLOR);
                    g2d.setStroke(new BasicStroke(ACTIVE_BLOCK_BORDER_WIDTH));
                    g2d.draw(new Rectangle2D.Float(rect.x, rect.y, rect.width, rect.height));
                }
                else {
                    boolean isSelected = selectedBlocks.contains(tb);
                    Color fg = isSelected ? SELECTED_BLOCK_BORDER_COLOR : cp.getForeground();
                    float strokeWidth = isSelected ? SELECTED_BLOCK_BORDER_WIDTH : NORMAL_BLOCK_BORDER_WIDTH;
//                    g2d.setComposite(transparent);
                    g2d.setColor(fg);
                    g2d.setStroke(new BasicStroke(strokeWidth));
                    g2d.draw(new Rectangle2D.Float(rect.x, rect.y, rect.width, rect.height));
                }
//                g2d.drawRect(r.x, r.y, r.width, r.height);
            }
            else {
                g2d.setComposite(transparent);
                g2d.setColor(Color.PINK);
                g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
            }

            Color nameColor = intersectsOtherBlocks ? Color.RED : Color.BLACK;

            g2d.setComposite(opaque);

            Color saveColor = g2d.getColor();
            g2d.setColor(nameColor);
            int sx = rect.x + 2;
            int sy = Math.max(rect.y + rect.height - lineHeight, lineHeight);
            g2d.drawString(tb.getName(), sx, sy);
            g2d.setColor(saveColor);
        }
        else {
            boolean isSelected = selectedBlocks.contains(tb);

            g2d.translate(rect.x, rect.y);
            plantingBlockRenderer.draw(g2d,
                    transparent,
                    lineHeight,
                    this,
                    tb,
                    rect.width,
                    rect.height,
                    isActiveBlock,
                    isSelected,
                    intersectsOtherBlocks);
            g2d.translate(- rect.x, - rect.y);
        }
    }

    private Map<PlantingBlock<?>,Rectangle> rectangleByTrial = new HashMap<>();

    protected void recomputeSizes() {

        paintOffset = null;

        if (DEBUG) System.out.println("recomputeSizes: " + model.getRowCount() + " blocks");

        int maxX = model.getRowCount();
        int maxY = model.getColumnCount();

        if (plantingBlockTableModel != null) {
            for (PlantingBlock<?> tb : plantingBlockTableModel) {
                int x = tb.getX() + tb.getColumnCount() - 1;
                int y = tb.getY() + tb.getRowCount() - 1;

                if (DEBUG) System.out.println(tb.getName() + ": x,y == " + x + "," + y);

                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        // Now we know how "big" we need to be.

        Point origin  = getDrawingOrigin(); // new Point(0,0);

        int cw = getCellWidth();

        int ch = getCellHeight();

        if (DEBUG) System.out.println(String.format("recomputeSizes: cell dimension=[%d x %d]", cw, ch));

        rectangleByTrial.clear();
        for (PlantingBlock<?> tb : plantingBlockTableModel) {

            int x = tb.getX() * cw;
            int y = tb.getY() * ch;

            int w = tb.getColumnCount() * cw;
            int h = tb.getRowCount() * ch;


            Rectangle r = new Rectangle(origin.x + x, origin.y + y, w, h);

            rectangleByTrial.put(tb, r);
        }

        repaint();
    }

    public Point viewToModel(Point pt) {
        Point origin  = getDrawingOrigin();
        return new Point(
                (pt.x - origin.x) / getCellWidth(),
                (pt.y - origin.y) / getCellHeight());
    }

//    public Point viewToModelCeil(Point pt) {
//        Point origin  = getDrawingOrigin();
//
//        int cw = getCellWidth();
//        int ch = getCellHeight();
//
//        return new Point(
//                Math.floorDiv(pt.x - origin.x + (cw - 1), cw),
//                Math.floorDiv(pt.y - origin.y + (ch - 1), ch) );
//    }

    public Point modelToView(Point pt) {
        return modelToView(pt, null);
    }

    public Point modelToView(Point pt, Point origin) {
        if (origin == null) {
            origin  = getDrawingOrigin();
        }
        Point result = new Point(
                origin.x + (pt.x * getCellWidth()),
                origin.y + (pt.y * getCellHeight()));
//        if (false && DEBUG) {
//            System.out.println(
//                    String.format("IN=(%d,%d)  Z=(%d,%d)  WH=%d x %d   OUT=(%d,%d)", pt.x, pt.y, origin.x, origin.y, cellWidth , cellHeight, result.x, result.y));
//        }
        return result;
    }

    private boolean isAnyBlockContaining(Point viewPoint, PlantingBlock<?> exceptFor) {

        if (DEBUG) {
            System.out.println(String.format(
                    "isAnyBlockContaining( (%d,%d), %s)",
                    viewPoint.x, viewPoint.y, (exceptFor==null ? "null" : exceptFor.getName())));
        }
        if (plantingBlockTableModel == null) {
            return false;
        }

        Rectangle fr = getFieldRectangle();
        if (! fr.contains(viewPoint)) {
            return false;
        }

        Predicate<PlantingBlock<?>> containsPoint = new Predicate<PlantingBlock<?>>() {
            @Override
            public boolean test(PlantingBlock<?> tb) {
                if (tb == exceptFor) {
                    return false;
                }
                Rectangle tr = rectangleByTrial.get(tb);
                if (tr == null) {
                    return false;
                }

                boolean result = tr.contains(viewPoint);
                if (DEBUG) {
                    System.out.println(String.format(
                            "\t test( (%d,%d) in [%d,%d - %d,%d] is %s",
                                viewPoint.x, viewPoint.y,
                                tr.x, tr.y, (tr.x + tr.width-1), (tr.y + tr.height-1),
                                (result ? "YES" : "NO")));
                }
                return result;
            }
        };

        Optional<PlantingBlock<E>> found = plantingBlockTableModel.getPlantingBlocks().stream()
                .filter(containsPoint)
                .findFirst();
        return found.isPresent();
    }

    private List<PlantingBlock<E>> findBlocksContaining(Point viewPoint, PlantingBlock<E> exceptFor) {

        if (DEBUG) {
            System.out.println(String.format(
                    "findBlocksContaining( (%d,%d), %s)",
                    viewPoint.x, viewPoint.y, (exceptFor==null ? "null" : exceptFor.getName())));
        }

        if (plantingBlockTableModel == null) {
            return Collections.emptyList();
        }

//        Rectangle fr = getFieldRectangle();
//        if (! fr.contains(viewPoint)) {
//            return Collections.emptyList();
//        }
//
//        // Ok - we are somewhere in the area that contains Trial Blocks

        Predicate<PlantingBlock<E>> containsPoint = new Predicate<PlantingBlock<E>>() {
            @Override
            public boolean test(PlantingBlock<E> tb) {
                if (tb == exceptFor) {
                    return false;
                }
                Rectangle tr = rectangleByTrial.get(tb);
                if (tr == null) {
                    return false;
                }

                boolean result = tr.contains(viewPoint);
                if (DEBUG) {
                    System.out.println(String.format(
                            "\t test( (%d,%d) in [%d,%d - %d,%d] is %s",
                                viewPoint.x, viewPoint.y,
                                tr.x, tr.y, (tr.x + tr.width-1), (tr.y + tr.height-1),
                                (result ? "YES" : "NO")));
                }

                return result;
            }
        };

        List<PlantingBlock<E>> found = plantingBlockTableModel.getPlantingBlocks().stream()
            .filter(containsPoint)
            .collect(Collectors.toList());

        return found;
    }



    // =====================================

    private int visibleRowCount = 0;

    private int visibleColumnCount = 0;

    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public void setVisibleRowCount(int visibleRowCount) {
        int oldValue = this.visibleRowCount;
        this.visibleRowCount = Math.max(0, visibleRowCount);

        establishPreferredViewportSize();

        firePropertyChange(PROPERTY_VISIBLE_ROW_COUNT, oldValue, visibleRowCount);
    }

    public int getVisibleColumnCount() {
        return visibleColumnCount;
    }

    public void setVisibleColumnCount(int visibleColumnCount) {
        int oldValue = this.visibleColumnCount;
        this.visibleColumnCount = Math.max(0, visibleColumnCount);

        establishPreferredViewportSize();

        firePropertyChange(PROPERTY_VISIBLE_COLUMN_COUNT, oldValue, visibleColumnCount);
    }

    public void setVisibleSize(int columnCount, int rowCount) {
        int oldColumnCount = this.visibleColumnCount;
        int oldRowCount = this.visibleRowCount;

        this.visibleColumnCount = Math.max(0, visibleColumnCount);
        this.visibleRowCount = Math.max(0, visibleRowCount);

        establishPreferredViewportSize();

        firePropertyChange(PROPERTY_VISIBLE_COLUMN_COUNT, oldColumnCount, visibleColumnCount);
        firePropertyChange(PROPERTY_VISIBLE_ROW_COUNT, oldRowCount, visibleRowCount);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension result = super.getPreferredSize();
//        if (false && DEBUG) {
//            System.out.println("getPreferredSize: " + (result==null ? "null" : result.width + "x" + result.height));
//        }
        return result;
    }


    // cell bounds for the cell at col,row
    public Rectangle getCellBounds(int col, int row) {
        return getCellBounds(col, row, getBounds());
    }

    public Rectangle getCellBounds(int col, int row, Rectangle bounds) {

        if (bounds == null) {
            bounds = getBounds();
        }
        int cw = getCellWidth();
        int ch = getCellHeight();

        int wid = model.getColumnCount() * cw;
        int hyt = model.getRowCount() * ch;

        int xbase = wid <= bounds.width  ? (bounds.width - wid)  / 2 : 0;
        int ybase = hyt <= bounds.height ? (bounds.height - hyt) / 2 : 0;

        Rectangle result = new Rectangle(xbase + col * cw, ybase + row * ch, cw, ch);
        if (DEBUG) {
            System.out.println(String.format(
                    "getCellBounds(%d,%d): [%d,%d  %dx%d]",
                    col, row,
                    result.x, result.y, result.width, result.height));
        }
        return result;
    }



    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // Scrollable

    Dimension preferredViewportSize;

    public void setPreferredScrollableViewportSize(Dimension size) {
        preferredViewportSize = size;
        if (DEBUG_SCROLLABLE) {
            System.out.println("setPreferredScrollableViewportSize: " + preferredViewportSize);
        }
        invalidate();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        if (DEBUG_SCROLLABLE) {
            System.out.println("getPreferredViewportSize: " + preferredViewportSize);
        }
        return preferredViewportSize;
    }

//    private Dimension getAlternatePSVS() {
//        Insets insets = getInsets();
//        int dx = insets.left + insets.right;
//        int dy = insets.top + insets.bottom;
//
//        int cellWidth = getModel().getCellWidth();
//        int cellHeight = getModel().getCellHeight();
//
//        if ((cellWidth > 0) && (cellHeight > 0)) {
//            int width = (visibleColumnCount * cellWidth) + dx;
//            int height = (visibleRowCount * cellHeight) + dy;
//            return new Dimension(width, height);
//        }
//
//        if (getModel().getRowCount() > 0 && getModel().getColumnCount() > 0) {
//            int width = getPreferredSize().width;
//            int height;
//            Rectangle r = getCellBounds(0, 0);
//            if (r != null) {
//                height = (visibleRowCount * r.height) + dy;
//            }
//            else {
//                // Will only happen if UI null, shouldn't matter what we return
//                height = 1;
//            }
//            return new Dimension(width, height);
//        }
//
//        cellWidth = (cellWidth > 0) ? cellWidth : 256;
//        cellHeight = (cellHeight > 0) ? cellHeight : 16;
//        return new Dimension(cellWidth, cellHeight * visibleRowCount);
//    }


    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = (orientation == SwingConstants.HORIZONTAL) ? visibleRect.x : visibleRect.y;
        int maxUnitIncrement = (orientation == SwingConstants.HORIZONTAL) ? getCellWidth() : getCellHeight();

        // Return the number of pixels between currentPosition
        // and the nearest tick mark in the indicated direction.
        int result;
        if (direction < 0) {
            int newPosition = currentPosition
                    - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
            result = (newPosition == 0) ? maxUnitIncrement : newPosition;
        } else {
            result = ((currentPosition / maxUnitIncrement) + 1)
                    * maxUnitIncrement - currentPosition;
        }

        if (DEBUG_SCROLLABLE) {
            System.out.println(String.format(
                    "getScrollableUnitIncrement([%d,%d %dx%d] , %s, %s) = %d",
                    visibleRect.x, visibleRect.y, visibleRect.width, visibleRect.height,
                    (orientation==SwingConstants.HORIZONTAL ? "HORZ" : "VERT"),
                    (direction < 0 ? "UP/LEFT" : "DOWN/RIGHT"),
                    result));
        }

        return result;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        int result;
        if (orientation == SwingConstants.HORIZONTAL)
            result = visibleRect.width - getCellWidth();
        else
            result = visibleRect.height - getCellHeight();
        if (DEBUG_SCROLLABLE) {
            System.out.println(String.format(
                    "getScrollableBlockIncrement([%d,%d %dx%d] , %s, %s) = %d",
                    visibleRect.x, visibleRect.y, visibleRect.width, visibleRect.height,
                    (orientation==SwingConstants.HORIZONTAL ? "HORZ" : "VERT"),
                    (direction < 0 ? "UP/LEFT" : "DOWN/RIGHT"),
                    result));
        }
        return result;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        boolean result = false;
        if (getVisibleColumnCount() <= 0) {
            Container parent = SwingUtilities.getUnwrappedParent(this);
            if (parent instanceof JViewport) {
                int nCols = (visibleColumnCount <= 0) ? model.getColumnCount() : visibleColumnCount;
                int nRows = (visibleRowCount    <= 0) ? model.getRowCount()    : visibleRowCount;
                Dimension size = new Dimension(getCellWidth() * nCols, getCellHeight() * nRows);

                result = parent.getWidth() > size.width;
//                result = parent.getWidth() > getPreferredSize().width;
            }
//            result = defaultTrackWidthHeight;
        }
        else {
            Container parent = SwingUtilities.getUnwrappedParent(this);
            if (parent instanceof JViewport) {
                result = parent.getWidth() > getPreferredSize().width;
            }
        }
        if (DEBUG_SCROLLABLE) {
            System.out.println("getScrollableTracksViewportWidth: " + result);
        }
        return result;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        boolean result = false;
        if (getVisibleRowCount() <= 0) {
            Container parent = SwingUtilities.getUnwrappedParent(this);
            if (parent instanceof JViewport) {
                result = parent.getHeight() > getPreferredSize().height;
            }
            //result = defaultTrackWidthHeight;
        }
        else {
            Container parent = SwingUtilities.getUnwrappedParent(this);
            if (parent instanceof JViewport) {
                result = parent.getHeight() > getPreferredSize().height;
            }
        }
        if (DEBUG_SCROLLABLE) {
            System.out.println("getScrollableTracksViewportHeight: " + result);
        }
        return result;
    }


    // ===== Listeners
    public void addPositionChangedListener(PositionChangedListener l) {
        plantingBlockTableModel.addPositionChangedListener(l);
    }
    public void removePositionChangedListener(PositionChangedListener l) {
        plantingBlockTableModel.removePositionChangedListener(l);
    }

    public float getPlantingBlockOpacity() {
        return plantingBlockOpacity;
    }
    public void setPlantingBlockOpacity(float f) {
        plantingBlockOpacity = Math.min(MAXIMUM_OPACITY, Math.max(MINIMUM_OPACITY, f));
        repaint();
    }

    public Point getMaxFieldCoordinate() {
        return new Point(model.getColumnCount(), model.getRowCount());
    }

//    private void updateMinimumCellDimension() {
//        FontMetrics fm = getFontMetrics(getFont());
//        minimumCellWidth = Math.max(MINIMUM_CELL_WIDTH, fm.stringWidth("M"));
//        minimumCellHeight = Math.max(MINIMUM_CELL_HEIGHT, fm.getMaxAscent() + fm.getMaxDescent());
//        if (DEBUG) {
//            System.out.println(String.format("updateMinimumCellDimension: %d x %d", minimumCellWidth, minimumCellHeight));
//        }
//    }
}
