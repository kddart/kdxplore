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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent.EventType;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

@SuppressWarnings("nls")
public class FieldViewMouseListener<E> implements MouseListener, MouseMotionListener {

    static public String asString(Rectangle r) {
        return String.format("[%d,%d  %d x %d]", r.x, r.y, r.width, r.height);
    }

    enum State {
        OUT,
        IN,
        IN_DRAGGING;
    }


    static private Cursor makeDrawingCellTypeBlockCursor(boolean small) {
        return KDClientUtils.getCursor(ImageId.EDIT_BLUE_24, small, "FieldEdit",
                (sz) -> new Point(0, sz.height-1));
    }

    private static final boolean DEBUG = Boolean.getBoolean("FieldViewMouseListener.DEBUG");
    private State state = State.OUT;

    private Cursor normalCursor = Cursor.getDefaultCursor();



    private Cursor drawingCellTypeCursor = makeDrawingCellTypeBlockCursor(false);
    private Cursor erasingCursor = KDClientUtils.getEraserCursor(false);

    private Cursor notOverBlockCursor = makeDrawingCellTypeBlockCursor(true);
    private Cursor overBlockCursor = KDClientUtils.getOpenHandCursor(false); // Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); // CROSSHAIR_CURSOR); //Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    private Cursor draggingBlockCursor = KDClientUtils.getClosedHandCursor(false); // Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

    private final FieldModel model;

    private Point mouseDownViewPoint;
    private Point mouseDownModelPoint;
    private boolean activeStartedOverOthers;

    // box drawing bits
    private Point prevMoveViewPoint;
    private Graphics2D mouseg;
    private Stroke stroke = new BasicStroke(2);
    private Color xorColor = Color.CYAN;

    private PlantingBlock<E> activePlantingBlock;

    private final FieldViewFunctions<E> fieldViewFunctions;

    // block, doubleClick
    private final Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked;
    private Composite opaque;

    public FieldViewMouseListener(
            FieldModel model,
            FieldViewFunctions<E> fieldViewFunctions,
            Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked)
    {
        this.model = model;
        this.fieldViewFunctions = fieldViewFunctions;
        this.onBlockClicked = onBlockClicked;
    }

    public PlantingBlock<E> getActivePlantingBlock() {
        return activePlantingBlock;
    }

    private Point viewToModel(Point pt) {
        return fieldViewFunctions.viewToModel(pt);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (DEBUG) System.out.println("mouseClicked: activePlantingBlock=" + activePlantingBlock);

        if (SwingUtilities.isLeftMouseButton(e) && 2 == e.getClickCount()) {
            Point viewPoint = e.getPoint();
            Point modelPoint = viewToModel(viewPoint);

            Predicate<PlantingBlock<E>> predicate = new Predicate<PlantingBlock<E>>() {
                @Override
                public boolean test(PlantingBlock<E> b) {
                    return b.contains(modelPoint);
                }
            };
            Optional<PlantingBlock<E>> optional = fieldViewFunctions.findFirstPlantingBlock(predicate);
            if (optional.isPresent()) {
                PlantingBlockSelectionEvent<E> event = new PlantingBlockSelectionEvent<>(
                        EventType.EDIT, e, optional.get());
                onBlockClicked.accept(event);
            }
            else {
                model.toggleFieldCell(modelPoint);
                fieldViewFunctions.repaint();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseDownViewPoint = e.getPoint();
        mouseDownModelPoint = viewToModel(mouseDownViewPoint);

        prevMoveViewPoint = null;
        mouseg = (Graphics2D) fieldViewFunctions.getGraphics();
        opaque = mouseg.getComposite();

        if (activePlantingBlock == null) {
            // A block is NOT active so we are drawing or erasing cells
            if (SwingUtilities.isRightMouseButton(e)) {
                fieldViewFunctions.setCursor(erasingCursor);
            }
            else {
                fieldViewFunctions.setCursor(drawingCellTypeCursor);
            }
        }
        else {
            // If not shifted then don't allow colliding
            activeStartedOverOthers =
                    fieldViewFunctions.isAnyOtherBlockIntersecting(activePlantingBlock);
            if (DEBUG) System.out.println("mousePressed: activeStartedOverOthers=" + activeStartedOverOthers);
            fieldViewFunctions.setCursor(draggingBlockCursor);

            if (onBlockClicked != null) {
                onBlockClicked.accept(new PlantingBlockSelectionEvent<>(
                        EventType.SELECT, e, activePlantingBlock));
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (prevMoveViewPoint != null) {
            Composite saveComposite = mouseg.getComposite();
            mouseg.setComposite(opaque);
            mouseg.setXORMode(xorColor);
            try {
                drawBox(); // undo last
            }
            finally {
                mouseg.setPaintMode();
                mouseg.setComposite(saveComposite);
            }
        }

        prevMoveViewPoint = null;
        mouseg.dispose();
        mouseg = null;

        if (mouseDownModelPoint != null && activePlantingBlock == null) {
            Point mouseUpModelPoint = fieldViewFunctions.viewToModel(e.getPoint());

            int xmin = Math.min(mouseDownModelPoint.x, mouseUpModelPoint.x);
            int xmax = Math.max(mouseDownModelPoint.x, mouseUpModelPoint.x);

            int ymin = Math.min(mouseDownModelPoint.y, mouseUpModelPoint.y);
            int ymax = Math.max(mouseDownModelPoint.y, mouseUpModelPoint.y);

            if (DEBUG) {
                System.out.println(String.format("TBEML.mouseReleased: mouseUpModelPoint=(%d,%d)\n\tscanning x[%d - %d]  and  y[%d - %d]",
                        mouseUpModelPoint.x, mouseUpModelPoint.y,
                        xmin, xmax, ymin, ymax));
            }

            for (int x = xmin; x <= xmax; ++x) {
                for (int y = ymin; y <= ymax; ++y) {
                    Point modelPoint = new Point(x, y);
                    Point viewPoint = fieldViewFunctions.modelToView(modelPoint);

                    if (! fieldViewFunctions.isAnyBlockContaining(viewPoint, null)) {
                        possiblyChangeBorder(e, modelPoint);
                    }
                }
            }
        }

        mouseDownViewPoint = null;
        mouseDownModelPoint = null;
//        activePlantingBlock = null;
        if (DEBUG) System.out.println("mouseReleased: activePlantingBlock IS NOW null");
        fieldViewFunctions.repaint();

        fieldViewFunctions.setCursor(notOverBlockCursor);

        showOverBlockStateAtViewPoint(e.getPoint());
    }

    private void drawBox() {
        if (prevMoveViewPoint == null) {
            return;
        }
        int x = Math.min(mouseDownViewPoint.x, prevMoveViewPoint.x);
        int y = Math.min(mouseDownViewPoint.y, prevMoveViewPoint.y);
        int width = Math.abs(mouseDownViewPoint.x - prevMoveViewPoint.x);
        int height = Math.abs(mouseDownViewPoint.y - prevMoveViewPoint.y);
        if (width > 0 || height > 0) {
            mouseg.setStroke(stroke);
            mouseg.drawRect(x, y, width, height);
        }
    }

    private void showOverBlockStateAtViewPoint(Point pt) {
        if (DEBUG) System.out.println("showOverBlockState: " + pt.x + "," + pt.y);

        if (fieldViewFunctions.isAnyBlockContaining(pt, null)) {
            if (DEBUG) System.out.println("CURSOR: over block");
            fieldViewFunctions.setCursor(overBlockCursor);
        }
        else {
            if (DEBUG) System.out.println("CURSOR: not over block");
            fieldViewFunctions.setCursor(notOverBlockCursor);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        xorColor = Color.PINK;
        stroke = new BasicStroke(4);
        switch (state) {
        case IN:
            break;
        case IN_DRAGGING:
            break;
        case OUT:
            state = State.IN;
            break;
        }
        // Don't worry about setting active - mouseMove soon will do that
    }

    @Override
    public void mouseExited(MouseEvent e) {
        switch (state) {
        case IN:
        case IN_DRAGGING:
            state = State.OUT;
            fieldViewFunctions.setCursor(normalCursor);
            break;
        case OUT:
            break;
        }
        activePlantingBlock = null;
        if (DEBUG) System.out.println("mouseExited: activePlantingBlock IS NOW null");

        fieldViewFunctions.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (activePlantingBlock == null) {
            Composite saveComposite = mouseg.getComposite();
            mouseg.setComposite(opaque);
            mouseg.setXORMode(xorColor);
            try {
                if (prevMoveViewPoint != null) {
                    drawBox();
                }
                prevMoveViewPoint = e.getPoint();
                drawBox();
            }
            finally {
                mouseg.setPaintMode();
                mouseg.setComposite(saveComposite);
            }
        }
        else {
            Point viewPoint = e.getPoint();
            Point modelPoint = viewToModel(viewPoint);

            // Will be -1, 0, 1
            int xViewInc = (int) Math.signum(viewPoint.x - mouseDownViewPoint.x);

            // Will be -1, 0, 1
            int yViewInc = (int) Math.signum(viewPoint.y - mouseDownViewPoint.y);

            if (xViewInc != 0 || yViewInc != 0) {
                // Will be -1, 0, 1
                int xModelInc = (int) Math.signum(modelPoint.x - mouseDownModelPoint.x);
                // Will be -1, 0, 1
                int yModelInc = (int) Math.signum(modelPoint.y - mouseDownModelPoint.y);

                boolean changed = possiblyMoveActiveBlock(e, xModelInc, yModelInc);

                if (changed) {
                    mouseDownModelPoint = modelPoint;
                    fieldViewFunctions.blockPositionChanged(activePlantingBlock);
                    if (DEBUG) {
                        System.out.println(String.format("*** MOVE %s by %d,%d \t TO %d,%d\n\tmoved mouseDownGridPoint to %d,%d",
                                activePlantingBlock.toString(), xModelInc, yModelInc,
                                activePlantingBlock.getX(), activePlantingBlock.getY(),
                                mouseDownModelPoint.x, mouseDownModelPoint.y));
                    }
                }
            }
        }
    }

//    void xx(MouseEvent e) {
//        Point viewPoint = e.getPoint();
//        Point modelPoint = viewToModel(viewPoint);
//
//
//        // Will be -1, 0, 1
//        int xViewInc = (int) Math.signum(viewPoint.x - mouseDownViewPoint.x);
//
//        // Will be -1, 0, 1
//        int yViewInc = (int) Math.signum(viewPoint.y - mouseDownViewPoint.y);
//
//        if (xViewInc != 0 || yViewInc != 0) {
//            // At least one movement is far enough
//            if (activePlantingBlock == null) {
//                if (! fieldViewFunctions.isAnyBlockContaining(viewPoint, null)) {
//                    possiblyChangeBorder(e, modelPoint);
//                }
//            }
//            else {
//                // Will be -1, 0, 1
//                int xModelInc = (int) Math.signum(modelPoint.x - mouseDownModelPoint.x);
//                // Will be -1, 0, 1
//                int yModelInc = (int) Math.signum(modelPoint.y - mouseDownModelPoint.y);
//
//                boolean changed = possiblyMoveActiveBlock(e, xModelInc, yModelInc);
//
//                if (changed) {
//                    mouseDownModelPoint = modelPoint;
//                    fieldViewFunctions.blockPositionChanged(activePlantingBlock);
//
//                    System.out.println(String.format("*** MOVE %s by %d,%d \t TO %d,%d\n\tmoved mouseDownGridPoint to %d,%d",
//                            activePlantingBlock.toString(), xModelInc, yModelInc,
//                            activePlantingBlock.getX(), activePlantingBlock.getY(),
//                            mouseDownModelPoint.x, mouseDownModelPoint.y));
//                }
//            }
//        }
//    }

    private boolean possiblyMoveActiveBlock(MouseEvent e, int xinc, int yinc) {

        boolean changed = false;

        Point activeBlockPoint = new Point(activePlantingBlock.getX(), activePlantingBlock.getY());

        boolean canMoveX = false;
        if (xinc != 0) {
            int ax = activeBlockPoint.x + xinc;

            if (xinc < 0 && ax >= 0) {
                if (DEBUG) System.out.println("\tX move LEFT  is OK");
                canMoveX = true;
            }
            else if (xinc > 0 && (ax + activePlantingBlock.getColumnCount() - 1) < model.getColumnCount()) {
                if (DEBUG) System.out.println("\tX move RIGHT is OK");
                canMoveX = true;
            }
            else {
                if (DEBUG) System.out.println("\tmove HORIZ is NOT ok");
            }

            if (canMoveX) {
                // It fits
                activeBlockPoint.x = ax;
            }
        }

        boolean canMoveY = false;
        if (yinc != 0) {
            int ay = activeBlockPoint.y + yinc;

            if (yinc < 0 && ay >= 0) {
                if (DEBUG) System.out.println("\tY move  UP  is OK");
                canMoveY = true;
            }
            else if (yinc > 0 && (ay + activePlantingBlock.getRowCount() - 1) < model.getRowCount()) {
                if (DEBUG) System.out.println("\tY move DOWN is OK");
                canMoveY = true;
            }
            else {
                if (DEBUG) System.out.println("\tmove VERTIC is NOT ok");
            }

            if (canMoveY) {
//                        System.out.println("moving activeBlock Y from " + activeBlock.getY() + " to " + ay);
                activeBlockPoint.y = ay;
            }
        }


        if (canMoveX || canMoveY) {

            boolean collide = false;

            boolean shifted = 0 != (MouseEvent.SHIFT_DOWN_MASK & e.getModifiersEx());

            if (! shifted && ! activeStartedOverOthers) {
                Rectangle moveTo = new Rectangle(activeBlockPoint.x, activeBlockPoint.y,
                        activePlantingBlock.getColumnCount(), activePlantingBlock.getRowCount());

                if (DEBUG) {
                    System.out.println(String.format("Looking for collision: %d,%d  %d x %d",
                            moveTo.x, moveTo.y,  moveTo.width, moveTo.height));
                }
                Predicate<PlantingBlock<E>> predicate = new Predicate<PlantingBlock<E>>() {
                    @Override
                    public boolean test(PlantingBlock<E> tb) {
                        if (tb == activePlantingBlock) {
                            return false;
                        }
                        Rectangle r = tb.getRectangle();
                        boolean result = moveTo.intersects(r);
                        if  (DEBUG) {
                            System.out.println(String.format("\tcheck %s intersects %s = %s",
                                    FieldViewMouseListener.asString(moveTo),
                                    FieldViewMouseListener.asString(r),
                                    result ? "YES" : "NO"));
                        }
                        return result;
                    }
                };
                Optional<PlantingBlock<E>> opt = fieldViewFunctions.findFirstPlantingBlock(predicate);
                collide = opt.isPresent();
            }

            if (collide) {
                if (++nCollisions > 5) {
                    if (! shown) {
                        shown = true;
                        new Toast(e.getPoint(), "Hold down SHIFT to override collision", 1000).show();
                    }
                }
                else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            else {
                nCollisions = 0;
                shown = false;
                activePlantingBlock.setX(activeBlockPoint.x);
                activePlantingBlock.setY(activeBlockPoint.y);
                fieldViewFunctions.blockPositionChanged(activePlantingBlock);
                changed = true;
            }
        }
        return changed;
    }

    private boolean shown = false;
    private int nCollisions = 0;


    private void possiblyChangeBorder(MouseEvent e, Point modelPoint) {

        int x = modelPoint.x;
        int y = modelPoint.y;

        int nCellsWide = model.getColumnCount();
        int nCellsHigh = model.getRowCount();

        if (x >= 0 && x < nCellsWide && y >= 0 && y < nCellsHigh) {
            if (SwingUtilities.isRightMouseButton(e)) {
                model.removeFieldCell(modelPoint);
//                model.removeFieldCell(new Point(x, y));
            }
            else {
                model.addFieldCell(modelPoint);
//                model.addFieldCell(new Point(x, y));
            }
        }
    }



    @Override
    public void mouseMoved(MouseEvent e) {
        Point pt = e.getPoint();

        if (mouseDownViewPoint == null) {
            // Mouse is NOT down, show ability to select by changing cursor

            PlantingBlock<E> smallest = null;

            List<PlantingBlock<E>> found = fieldViewFunctions.findBlocksContaining(pt, null);
            if (found.isEmpty()) {
                if (DEBUG)  System.out.println("\tmouseMoved: drawingCellCursor");
                fieldViewFunctions.setCursor(notOverBlockCursor);
            }
            else {
                if (DEBUG) System.out.println("\tmouseMoved: overBlockCursor");
                fieldViewFunctions.setCursor(overBlockCursor);

                // But we need to figure out which block.
                // Look for the smallest one - this lets us move the smaller ones
                // out from under the bigger ones.
                int smallestSize = Integer.MAX_VALUE;
                for (PlantingBlock<E> tb : found) {
                    int tbSize = tb.getRowCount() * tb.getColumnCount();
                    if (smallest==null || tbSize < smallestSize) {
                        smallest = tb;
                        smallestSize = tbSize;
                    }
                }
            }

            if (activePlantingBlock != smallest) {
                activePlantingBlock = smallest;

                fieldViewFunctions.repaint();
            }

            if (DEBUG) System.out.println("\tactiveBlock=" + activePlantingBlock);

            // We already did the cursor above
//            showOverBlockState(pt);
        }
    }

}
