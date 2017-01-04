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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.Attribute;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent.EventType;
import com.diversityarrays.kdxplore.ui.HelpUtils;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.OverrideCellRenderer;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.renderer.ColorCellRenderer;

@SuppressWarnings("nls")
public class PlantingBlockTablePanel<E> extends JPanel {

    public static final int DEFAULT_VISIBLE_ROW_COUNT = 4;

    private static final boolean ALLOW_RESIZE_SITE_BUTTON = Boolean.getBoolean("ALLOW_RESIZE_SITE_BUTTON");

    private final PlantingBlockTableModel<E> plantingBlockModel;

    private final JTable plantingBlockTable;

    private final Action changeBlockSizeAction = new AbstractAction(
            Attribute.DIMENSION.buttonText)
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            int vrow = plantingBlockTable.getSelectedRow();
            if (vrow >= 0) {
                int mrow = plantingBlockTable.convertRowIndexToModel(vrow);
                if (mrow >= 0) {
                    doChangePlantingBlockSize(null, mrow);
                }
            }
        }
    };
    private final JButton changeBlockSizeButton = new JButton(changeBlockSizeAction);

    private final Action changeBlockPositionAction = new AbstractAction(
            Attribute.POSITION.buttonText)
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            int vrow = plantingBlockTable.getSelectedRow();
            if (vrow >= 0) {
                int mrow = plantingBlockTable.convertRowIndexToModel(vrow);
                if (mrow >= 0) {
                    doChangePlantingBlockPosition(null, mrow);
                }
            }
        }
    };
    private final JButton changePositionButton = new JButton(changeBlockPositionAction);

    private final Action changeBlockBordersAction = new AbstractAction(
            Attribute.BORDERS.buttonText)
    {
        @Override
        public void actionPerformed(ActionEvent e) {
            int vrow = plantingBlockTable.getSelectedRow();
            if (vrow >= 0) {
                int mrow = plantingBlockTable.convertRowIndexToModel(vrow);
                if (mrow >= 0) {
                    doChangePlantingBlockBorders(null, mrow);
                }
            }
        }
    };
    private final JButton changeBordersButton = new JButton(changeBlockBordersAction);

    private final List<PlantingBlock<E>> selectedTrialBlocks = new ArrayList<>();

    private final Action resizeSiteAction = new AbstractAction("Resize Site") {
        @Override
        public void actionPerformed(ActionEvent e) {

            int allwid = plantingBlockModel.getPlantingBlocks().stream()
                    .mapToInt(PlantingBlock::getColumnCount)
                    .sum();

            int allhyt = plantingBlockModel.getPlantingBlocks().stream()
                        .mapToInt(tb -> tb.getRowCount())
                        .sum();

            if (Check.isEmpty(selectedTrialBlocks)) {
                fieldSizer.setDimension(allwid, allhyt);
                return;
            }

            int wid = selectedTrialBlocks.stream()
                    .mapToInt(tb -> tb.getColumnCount())
                    .sum();
            int hyt = selectedTrialBlocks.stream()
                    .mapToInt(tb -> tb.getRowCount())
                    .sum();
            String[] options = new String[] {
                    "All: WxH (" + allwid + "x" + allhyt + ")",
                    "Selected: WxH (" + wid + "x" + hyt + ")",
                    UnicodeChars.CANCEL_CROSS
            };
            IntConsumer choiceConsumer = new IntConsumer() {
                @Override
                public void accept(int choice) {
                    switch (choice) {
                    case 0:
                        fieldSizer.setDimension(allwid, allhyt);
                        break;
                    case 1:
                        fieldSizer.setDimension(wid, hyt);
                        break;
                    }
                }
            };
            HelpUtils.askOptionPopup(resizeSiteButton, "Change Site size to fit:", choiceConsumer, options);
        }

    };
    private JButton resizeSiteButton = new JButton(resizeSiteAction);

    private Action exchangePositions = new AbstractAction("Swap") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedTrialBlocks.size() >= 2) {
                PlantingBlock<E> b1 = selectedTrialBlocks.get(0);
                PlantingBlock<E> b2 = selectedTrialBlocks.get(1);

                Point p1 = new Point(b1.getX(), b1.getY());
                b1.setX(b2.getX());
                b1.setY(b2.getY());

                b2.setX(p1.x);
                b2.setY(p1.y);
                plantingBlockModel.blockChanged(WhatChanged.POSITION, b1, b2);
            }
        }
    };

    private final FieldSizer<E> fieldSizer;

    public PlantingBlockTablePanel(PlantingBlockTableModel<E> tbm,
            FieldSizer<E> fieldSizer)
    {
        super(new BorderLayout());

        this.plantingBlockModel = tbm;
        this.fieldSizer = fieldSizer;
        plantingBlockTable = new JTable(plantingBlockModel);

        GuiUtil.setVisibleRowCount(plantingBlockTable, DEFAULT_VISIBLE_ROW_COUNT);

        exchangePositions.putValue(Action.SHORT_DESCRIPTION, "Select 2 Reps to Swap positions");
        exchangePositions.setEnabled(false);

        resizeSiteAction.putValue(Action.SHORT_DESCRIPTION, "Select Reps to establish Field width &/or height");

        plantingBlockTable.setDefaultRenderer(Color.class, new ColorCellRenderer());
        plantingBlockTable.setDefaultRenderer(Dimension.class,
                new OverrideCellRenderer<>(
                        Dimension.class,
                        (d) -> String.format("%d x %d", d.width, d.height))
                );
        plantingBlockTable.setDefaultRenderer(Point.class,
                new OverrideCellRenderer<>(
                        Point.class,
                        (pt) -> String.format("%d , %d", pt.x, pt.y))
                );
        plantingBlockTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    e.consume();
                    Point pt = e.getPoint();
                    int vrow = plantingBlockTable.rowAtPoint(pt);
                    if (vrow >= 0) {
                        int mrow = plantingBlockTable.convertRowIndexToModel(vrow);
                        if (mrow >= 0) {
                            int vcol = plantingBlockTable.columnAtPoint(pt);
                            int mcol = plantingBlockTable.convertColumnIndexToModel(vcol);
                            if (mcol >= 0) {
                                Optional<Attribute> attr = plantingBlockModel.getAttributeFor(mcol);
                                if (attr.isPresent()) {
                                    switch (attr.get()) {
                                    case BORDERS:
                                        doChangePlantingBlockBorders(pt, mrow);
                                        break;
                                    case DIMENSION:
                                        doChangePlantingBlockSize(pt, mrow);
                                        break;
                                    case POSITION:
                                        doChangePlantingBlockPosition(pt, mrow);
                                        break;
                                    default:
                                        MsgBox.error(PlantingBlockTablePanel.this,
                                                "Unsupported Attribute: " + attr.get(),
                                                "Internal Error");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        plantingBlockTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    collectSelectedTrialBlocks();
                    updateTrialBlockTableActions();
                }
            }
        });

        TableTransferHandler tth = TableTransferHandler.initialiseForCopySelectAll(plantingBlockTable, true);
        plantingBlockTable.setTransferHandler(tth);

        plantingBlockTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    if (! handlerList.isEmpty()) {
                        List<Integer> rows = GuiUtil.getSelectedModelRows(plantingBlockTable);
                        fireHandlers(rows);
                    }
                }
            }
        });

        updateTrialBlockTableActions();

        Box controls = Box.createHorizontalBox();

        controls.add(new JButton(exchangePositions));
        if (ALLOW_RESIZE_SITE_BUTTON) {
            controls.add(Box.createHorizontalStrut(8));
            controls.add(resizeSiteButton);
        }

        controls.add(Box.createHorizontalGlue());
        controls.add(new JSeparator(JSeparator.VERTICAL));
        controls.add(Box.createHorizontalGlue());

        controls.add(new JLabel("Change:"));
        controls.add(changeBlockSizeButton);
        controls.add(changePositionButton);
        controls.add(changeBordersButton);
        controls.add(Box.createHorizontalGlue());

        JScrollPane scrollPane = new JScrollPane(plantingBlockTable);
        scrollPane.setTransferHandler(tth);

        add(controls, BorderLayout.NORTH);
        add(scrollPane);
    }

    protected void fireHandlers(List<Integer> list) {

        List<Integer> rows = list.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(list));

        @SuppressWarnings("unchecked")
        Consumer<List<Integer>>[] handlers = (Consumer<List<Integer>>[]) Array.newInstance(Consumer.class, handlerList.size());
        for (int index = handlerList.size(); --index >= 0; ) {
            handlers[index] = handlerList.get(index);
        }

        for (Consumer<List<Integer>> c : handlers) {
            c.accept(rows);
        }
    }

    private final List<Consumer<List<Integer>>> handlerList = new ArrayList<>();
    public void addPlantingBlockSelectionHandler(Consumer<List<Integer>> handler) {
        handlerList.add(handler);
    }
    public void removeTrialBlockSelectionHandler(Consumer<List<Integer>> handler) {
        handlerList.remove(handler);
    }

    private void collectSelectedTrialBlocks() {
        List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(plantingBlockTable);
        selectedTrialBlocks.clear();
        for (Integer rowIndex : selectedModelRows) {
            selectedTrialBlocks.add(plantingBlockModel.getItemAt(rowIndex));
        }
        exchangePositions.setEnabled(selectedTrialBlocks.size() == 2);

        fieldSizer.setSelectedBlocks(selectedTrialBlocks);
    }

    private void updateTrialBlockTableActions() {
        boolean enb = plantingBlockTable.getSelectedRowCount() > 0;

        changeBlockSizeAction.setEnabled(enb);
        changeBlockBordersAction.setEnabled(enb);
        changeBlockPositionAction.setEnabled(enb);
    }

    private void doChangePlantingBlockBorders(Point mousePoint, int mrow) {
        doChangeAttribute(Attribute.BORDERS,
                mousePoint!=null
                    ? Either.left(mousePoint)
                    : Either.right(changeBordersButton),
                mrow,
                (pb) -> new ChangePlantingBlockBordersDialog(
                        GuiUtil.getOwnerWindow(PlantingBlockTablePanel.this),
                        plantingBlockModel,
                        pb));
    }

    private void doChangePlantingBlockPosition(Point mousePoint, int mrow) {
        Point maxFieldCoord = fieldSizer.getMaxFieldCoordinate();
        doChangeAttribute(Attribute.POSITION,
                mousePoint!=null
                    ? Either.left(mousePoint)
                    : Either.right(changePositionButton),
                mrow,
                (pb) -> new ChangePlantingBlockPositionDialog(
                        GuiUtil.getOwnerWindow(PlantingBlockTablePanel.this),
                        plantingBlockModel,
                        pb,
                        maxFieldCoord));
    }

    private void doChangePlantingBlockSize(Point mousePoint, int mrow) {
        doChangeAttribute(Attribute.DIMENSION,
                mousePoint!=null
                    ? Either.left(mousePoint)
                    : Either.right(changeBlockSizeButton),
                mrow,
                (pb) -> new ChangePlantingBlockSizeDialog(
                        GuiUtil.getOwnerWindow(PlantingBlockTablePanel.this),
                        plantingBlockModel,
                        pb));
    }

    private final Map<Attribute, JDialog> dialogByAttribute = new HashMap<>();

    private void doChangeAttribute(Attribute attr,
            Either<Point, JComponent> mousePointOrComponent,
            int modelRow,
            Function<PlantingBlock<?>, JDialog> dialogSupplier)
    {
        JDialog dialog = dialogByAttribute.get(attr);
        if (dialog != null) {
            dialog.toFront();
        }
        else {
            PlantingBlock<?> pb = plantingBlockModel.getItemAt(modelRow);
            dialog = dialogSupplier.apply(pb);
            dialog.setAlwaysOnTop(true);
            if (mousePointOrComponent.isLeft()) {
                Point mousePoint = mousePointOrComponent.left();
                Point pt = plantingBlockTable.getLocationOnScreen();
                pt.x += mousePoint.x;
                pt.y += mousePoint.y;
                dialog.setLocation(pt);
            }
            else {
                dialog.setLocationRelativeTo(mousePointOrComponent.right());
            }
            dialogByAttribute.put(attr, dialog);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    dialogByAttribute.remove(attr);
                }
            });
            dialog.setVisible(true);
        }
    }


    public void doSelectBlock(PlantingBlockSelectionEvent<E> event) {
        if (event.plantingBlock != null && EventType.SELECT == event.eventType) {
            int index = plantingBlockModel.indexOf(event.plantingBlock);
            if (index >= 0) {
                int viewIndex = plantingBlockTable.convertRowIndexToView(index);
                if (viewIndex >= 0) {
                    boolean shifted = 0 != (MouseEvent.SHIFT_DOWN_MASK & event.mouseEvent.getModifiersEx());
                    if (shifted) {
                        plantingBlockTable.addRowSelectionInterval(viewIndex, viewIndex);
                    }
                    else {
                        plantingBlockTable.setRowSelectionInterval(viewIndex, viewIndex);
                    }
                }
            }
        }
    }
}
