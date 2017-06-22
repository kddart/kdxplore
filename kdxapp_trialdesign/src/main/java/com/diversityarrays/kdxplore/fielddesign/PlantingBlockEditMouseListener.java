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
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent.EventType;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

@SuppressWarnings("nls")
public class PlantingBlockEditMouseListener<E> implements MouseListener, MouseMotionListener {

    enum State {
        OUT, IN, IN_DRAGGING;
    }

    static private Cursor makeOverBlockCursor() {
        Image image = KDClientUtils.getImage(ImageId.EDIT_GOLD_24);
        Point hotspot = new Point(0, image.getHeight(null) - 1);
        String name = "FieldEdit";
        return Toolkit.getDefaultToolkit().createCustomCursor(image, hotspot, name);
    }

    private static final boolean DEBUG = Boolean.getBoolean(
            PlantingBlockEditMouseListener.class.getSimpleName() + ".DEBUG");

    private State state = State.OUT;

    private Cursor normalCursor = Cursor.getDefaultCursor();
    private Cursor notOverBlockCursor = normalCursor;

    // private Cursor overBlockCursor =
    // Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Cursor overBlockCursor = makeOverBlockCursor();
    private Cursor overOtherBlockCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private Cursor erasingCursor = KDClientUtils.getEraserCursor(false);

    private final PlantingBlock<E> editingBlock;

    private final FieldViewFunctions<E> fieldViewFunctions;

    private Point mouseDownViewPoint;
    private Point prevMoveViewPoint;
    private Graphics2D mouseg;
    private Stroke stroke = new BasicStroke(2);
    private Color xorColor = Color.CYAN;

    private Point mouseDownModelPoint;

    private final Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked;

    private BiFunction<PlantingBlock<E>, Point, E> contentFactory;

    public PlantingBlockEditMouseListener(PlantingBlock<E> trialBlock,
            FieldViewFunctions<E> fieldViewFunctions,
            Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked) {
        this.editingBlock = trialBlock;
        this.fieldViewFunctions = fieldViewFunctions;
        this.onBlockClicked = onBlockClicked;
    }

    public void setContentFactory(BiFunction<PlantingBlock<E>, Point, E> f) {
        contentFactory = f;
    }

    private Point viewToModel(Point pt) {
        return fieldViewFunctions.viewToModel(pt);
    }

    // === Mouse Listener ===
    @Override
    public void mouseClicked(MouseEvent e) {
        Point viewPoint = e.getPoint();
        Point modelPoint = viewToModel(viewPoint);

        Predicate<PlantingBlock<E>> predicate = new Predicate<PlantingBlock<E>>() {
            @Override
            public boolean test(PlantingBlock<E> b) {
                return (b != editingBlock) && b.contains(modelPoint);
            }
        };
        Optional<PlantingBlock<E>> optional = fieldViewFunctions
                .findFirstPlantingBlock(predicate);

        PlantingBlock<E> otherBlock = optional.isPresent() ? optional.get() : null;

        if (1 == e.getClickCount()) {
            if (otherBlock != null) {
                // Do nothing
                PlantingBlockSelectionEvent<E> event = new PlantingBlockSelectionEvent<>(
                        EventType.EDIT, e, otherBlock);
                PlantingBlock<E> old = editingBlock;
                onBlockClicked.accept(event);
                if (old != editingBlock) {
                    fieldViewFunctions.repaint();
                }
            }
            else if (contentFactory != null) {
                if (DEBUG) {
                    System.out
                            .println(String.format("TBEML.mouseClicked at view: %d,%d  model=%d,%d",
                                    viewPoint.x, viewPoint.y, modelPoint.x, modelPoint.y));
                }

                if (editingBlock.contains(modelPoint)) {
                    boolean unset = SwingUtilities.isRightMouseButton(e);

                    Point p = editingBlock.getLocation();
                    modelPoint.translate(-p.x, -p.y);

                    E newContent = unset ? null : contentFactory.apply(editingBlock, modelPoint);
                    if (DEBUG) {
                        System.out.println(String.format("\tsetEntryTypeAt: (%d,%d) = %s\tunset=%s",
                                modelPoint.x, modelPoint.y,
                                newContent == null ? "null" : newContent.toString(),
                                unset ? "true" : "false"));
                    }

                    editingBlock.setContentAt(newContent, modelPoint);
                    fieldViewFunctions.blockEntryTypesChanged(editingBlock);
                }
            }
        }
        else if (2 == e.getClickCount()) {
            PlantingBlockSelectionEvent<E> event = new PlantingBlockSelectionEvent<>(
                    EventType.EDIT, e, otherBlock);
            PlantingBlock<E> old = editingBlock;
            onBlockClicked.accept(event);
            if (old != editingBlock) {
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
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (prevMoveViewPoint != null) {
            drawBox(); // undo last
        }

        prevMoveViewPoint = null;
        mouseg.dispose();
        mouseg = null;

        if (mouseDownModelPoint != null) {
            boolean unset = SwingUtilities.isRightMouseButton(e);

            Point mouseUpModelPoint = fieldViewFunctions.viewToModel(e.getPoint());
            // Point mouseUpModelPoint =
            // fieldViewFunctions.viewToModelCeil(e.getPoint());

            int xmin = Math.min(mouseDownModelPoint.x, mouseUpModelPoint.x);
            int xmax = Math.max(mouseDownModelPoint.x, mouseUpModelPoint.x);

            int ymin = Math.min(mouseDownModelPoint.y, mouseUpModelPoint.y);
            int ymax = Math.max(mouseDownModelPoint.y, mouseUpModelPoint.y);

            if (DEBUG) {
                System.out.println(String.format(
                        "TBEML.mouseReleased: mouseUpModelPoint=(%d,%d)\n\tscanning x[%d - %d]  and  y[%d - %d]",
                        mouseUpModelPoint.x, mouseUpModelPoint.y,
                        xmin, xmax, ymin, ymax));
            }

            boolean anyChanged = false;
            for (int x = xmin; x <= xmax; ++x) {
                for (int y = ymin; y <= ymax; ++y) {
                    Point modelPoint = new Point(x, y);
                    if (editingBlock.contains(modelPoint)) {

                        if (DEBUG) System.out.println(String.format("\tFOUND in block: (%d,%d)", x, y));

                        Point p = editingBlock.getLocation();
                        modelPoint.translate(-p.x, -p.y);

                        E newContent = null;
                        if (!unset && contentFactory != null) {
                            newContent = contentFactory.apply(editingBlock, modelPoint);
                        }

                        if (!editingBlock.setContentAt(newContent, modelPoint).isEmpty()) {
                            anyChanged = true;
                        }
                    }
                    else if (DEBUG) {
                        System.out.println(String.format("\tNOT in block: (%d,%d)", x, y));
                    }
                }
            }

            if (anyChanged) {
                fieldViewFunctions.blockEntryTypesChanged(editingBlock);
            }
        }

        mouseDownViewPoint = null;
        mouseDownModelPoint = null;

        fieldViewFunctions.repaint();
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

    @Override
    public void mouseEntered(MouseEvent e) {
        switch (state) {
        case IN:
        case IN_DRAGGING:
            break;
        case OUT:
            state = State.IN;
            break;
        default:
            break;
        }
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
        default:
            break;
        }
        fieldViewFunctions.repaint();
    }

    // ==== MouseMotionListener ====

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseg.setXORMode(xorColor);
        if (prevMoveViewPoint != null) {
            drawBox();
        }
        prevMoveViewPoint = e.getPoint();
        drawBox();
    }

    // private void xx(MouseEvent e) {
    // Point viewPoint = e.getPoint();
    // Point modelPoint = viewToModel(viewPoint);
    //
    // if (DEBUG) {
    // System.out.println(String.format("TBEML.mouseDragged at view: %d,%d
    // model=%d,%d\tmouseDownModelPoint=%s",
    // viewPoint.x, viewPoint.y, modelPoint.x, modelPoint.y,
    // mouseDownModelPoint==null ? "<null>" : String.format("(%d,%d)",
    // mouseDownModelPoint.x, mouseDownModelPoint.y)));
    // }
    //
    // if (mouseDownModelPoint == null) {
    // return;
    // }
    //
    // // Will be -1, 0, 1
    // int xModelInc = (int) Math.signum(modelPoint.x - mouseDownModelPoint.x);
    //
    // // Will be -1, 0, 1
    // int yModelInc = (int) Math.signum(modelPoint.y - mouseDownModelPoint.y);
    //
    // if (DEBUG) {
    // System.out.println(String.format("\txyModelInc: %d,%d", xModelInc,
    // yModelInc));
    // }
    // if (xModelInc != 0 || yModelInc != 0) {
    // // At least one movement is far enough
    // if (trialBlock.contains(modelPoint)) {
    //
    // boolean unset = SwingUtilities.isRightMouseButton(e);
    //// boolean shifted = 0 != (MouseEvent.SHIFT_DOWN_MASK &
    // e.getModifiersEx());
    // if (DEBUG) {
    // System.out.println(String.format("\tsetEntryTypeAt: (%d,%d) =
    // %s\tunset=%s",
    // modelPoint.x, modelPoint.y,
    // entryType==null ? "null" : entryType.toString(),
    // unset ? "true" : "false"));
    // }
    //
    // Point p = trialBlock.getLocation();
    // modelPoint.translate(-p.x, -p.y);
    // trialBlock.setEntryTypeAt(modelPoint, unset ? null : entryType);
    // fieldViewFunctions.blockEntryTypesChanged(trialBlock);
    // }
    // }
    // }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point viewPoint = e.getPoint();
        Point modelPoint = viewToModel(viewPoint);

        if (mouseDownViewPoint == null) {
            // Mouse is NOT down, show ability to select by changing cursor
            if (editingBlock.contains(modelPoint)) {
                fieldViewFunctions.setCursor(overBlockCursor);
            }
            else if (overOtherBlockCursor!=null && fieldViewFunctions.isAnyBlockContaining(viewPoint, editingBlock)) {
                fieldViewFunctions.setCursor(overOtherBlockCursor);
            }
            else {
                fieldViewFunctions.setCursor(notOverBlockCursor);
            }
        }
    }

    public PlantingBlock<E> getPlantingBlock() {
        return editingBlock;
    }
}
