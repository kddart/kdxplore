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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;
import java.util.IntSummaryStatistics;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.diversityarrays.kdxplore.fielddesign.EditModeWidget.EditMode;
import com.diversityarrays.kdxplore.fieldlayout.FieldSizer;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockTableModel;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;

import net.pearcan.color.ColorPair;

public class FieldLayoutEditPanel<E> extends JPanel implements FieldSizer<E> {

    private final PlantingBlockTableModel<E> plantingBlockModel;

    private final FieldView<E> fieldView;

    private FieldModelListener fieldModelListener = new FieldModelListener() {
        @Override
        public void fieldDimensionChanged(Object source) {
            FieldModel model = fieldView.getModel();
            setDimension(model.getColumnCount(), model.getRowCount());
        }
        @Override
        public void bordersChanged(Object source, Point pt) {
            if (editModeWidget != null) {
                editModeWidget.setEnableClearBorders(fieldView.getModel().getFieldCellCount() > 0);
            }
        }
    };

    boolean onlyGrey = true;

    private Function<PlantingBlock<E>, ColorPair> blockColorPairProvider = new Function<PlantingBlock<E>, ColorPair>() {
        @Override
        public ColorPair apply(PlantingBlock<E> tb) {
            if (onlyGrey) {
                return new ColorPair(DefaultPlantingBlockRenderer.DEFAULT_BLOCK_COLOR,
                        DefaultPlantingBlockRenderer.DEFAULT_BORDER_COLOR);
            }
            return plantingBlockModel.getColorPair(tb);
        }
    };

    private final FieldCellRenderer fieldCellRenderer = new DefaultFieldCellRenderer();
    private final PlantingBlockRenderer<E> plantingBlockRenderer;

    private final Consumer<PlantingBlockSelectionEvent<E>> doSelectBlock;
    private final Consumer<PlantingBlockSelectionEvent<E>> onBlockClicked = new Consumer<PlantingBlockSelectionEvent<E>>() {

        @Override
        public void accept(PlantingBlockSelectionEvent<E> e) {
            if (doSelectBlock != null) {
                doSelectBlock.accept(e);
            }

            if (editModeWidget == null) {
                fieldView.setEditingPlantingBlock(e.plantingBlock);
            }
            else {
                editModeWidget.onBlockClicked(e);
            }
        }
    };

    private final EditModeWidget<E> editModeWidget;


    private final ZoomAndDimensionWidget zoomAndDimensionWidget;

    private final AspectControlsWidget aspectControlsWidget;

    public FieldLayoutEditPanel(
            Dimension initialSize,
            PlantingBlockTableModel<E> tbm,
            Consumer<PlantingBlockSelectionEvent<E>> doSelectBlock,
            Function<E, Color> entryTypeColorSupplier)
    {
        this(initialSize,
                tbm, false, false,
                doSelectBlock, entryTypeColorSupplier);
    }

    private final Function<Point, FieldCell<?>> fieldCellFactory = new Function<Point, FieldCell<?>>() {
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public FieldCell<?> apply(Point pt) {
            // null check is in case of initialisation order problems
            // (FieldModel needs to be created before EditModeWidget)
            // OR
            // we don't create the editModeWidget because I'm not
            // ready for multiple FieldCellType-s
            FieldCellType type = editModeWidget==null
                    ? FieldCellType.DEFAULT_FIELD_CELL_TYPE
                    : editModeWidget.getFieldCellType();
            String content = type.name(); // fieldCellContentField.getText();
            return new FieldCell(pt, content, type);
      }
    };

    private IntSupplier minimumFieldSizeSupplier = new IntSupplier() {
        @Override
        public int getAsInt() {
            BinaryOperator<Dimension> accumulator = new BinaryOperator<Dimension>() {
                @Override
                public Dimension apply(Dimension t, Dimension u) {
                    int w = Math.max(t.width, u.width);
                    int h = Math.max(t.height, u.height);
                    return new Dimension(w, h);
                }
            };

            Dimension extent = plantingBlockModel.getPlantingBlocks().stream()
                .map(pb -> new Dimension( pb.getX() + pb.getColumnCount() - 1, pb.getY() + pb.getRowCount() - 1 ))
                .reduce(new Dimension(0,0), accumulator);

            IntSummaryStatistics stats = plantingBlockModel.getPlantingBlocks().stream()
                .collect(Collectors.summarizingInt(pb -> pb.getColumnCount() * pb.getRowCount()));

            int maxSize = stats.getMax();
            int maxExtent = extent.width * extent.height;

            return Math.max(maxSize, maxExtent);
        }
    };

    public FieldLayoutEditPanel(
            Dimension initialSize,
            PlantingBlockTableModel<E> tbm,
            boolean showSizeControls,
            boolean allowAutoSize,
            Consumer<PlantingBlockSelectionEvent<E>> doSelectBlock,
            Function<E, Color> entryTypeColorSupplier)
    {
        super(new BorderLayout());

        this.plantingBlockModel = tbm;
        this.doSelectBlock = doSelectBlock;

        plantingBlockRenderer = new DefaultPlantingBlockRenderer<>(
                blockColorPairProvider,
                entryTypeColorSupplier);

        FieldModel fieldModel = new DefaultFieldModel(fieldCellFactory);
        fieldModel.setColumnRowCount(initialSize.width, initialSize.height);
        fieldView = new FieldView<>(fieldModel,
                plantingBlockModel,
                onBlockClicked);
        fieldView.setPlantingBlockRenderer(plantingBlockRenderer);
        fieldView.setFieldCellRenderer(fieldCellRenderer);

//        if (FieldCellType.values().length > 1) {
            editModeWidget = new EditModeWidget<>(fieldView, EditMode.CELL_TYPE,
                    () -> fieldView.getSingleSelectedBlock(),
                    (pb) -> fieldView.setEditingPlantingBlock(pb),
                    (predicate) -> fieldView.getModel().clearFieldCells(predicate));
//        }
//        else {
//            editModeWidget = null;
//        }

        aspectControlsWidget = new AspectControlsWidget(fieldView, Orientation.VERT);

// this gives a loop
//        fieldView.addPositionChangedListener(fieldViewPositionChangedListener);

        fieldView.getModel().addFieldModelListener(fieldModelListener);

        Box vertToolBox = Box.createVerticalBox();
        zoomAndDimensionWidget = new ZoomAndDimensionWidget(fieldView,
                Orientation.VERT,
                showSizeControls,
                allowAutoSize,
                minimumFieldSizeSupplier);

        vertToolBox.add(aspectControlsWidget.getWidgetComponent());
        vertToolBox.add(zoomAndDimensionWidget.getWidgetComponent());
//        vertToolBox.add(Orientation.VERT.createJSeparator());
        vertToolBox.add(Box.createVerticalGlue());

        if (editModeWidget != null) {
            Box horzToolbox = Box.createHorizontalBox();
            horzToolbox.add(Box.createHorizontalGlue());
            horzToolbox.add(editModeWidget.getWidgetComponent());
            add(horzToolbox, BorderLayout.NORTH);
        }

        add(new JScrollPane(fieldView), BorderLayout.CENTER);
        add(vertToolBox, BorderLayout.EAST);
    }

    public JComponent getEditModeWidgetComponent() {
        return editModeWidget.getWidgetComponent();
    }


    // = = = = = = = = = FieldSizer

    @Override
    public void setDimension(int w, int h) {
        zoomAndDimensionWidget.setFieldSize(new Dimension(w, h));
    }

    @Override
    public void setSelectedBlocks(Collection<PlantingBlock<E>> blocks) {

        // TODO review the logic below
        if (Check.isEmpty(blocks)) {
            if (fieldView.isEditingPlantingBlock()) {
                // Don't change the status
            }
            else {
            }
        }
        else {
            if (fieldView.isEditingPlantingBlock() && blocks.size() == 1) {

            }
        }

        fieldView.setSelectedBlocks(blocks);
    }

    /**
     * @return Either.right== the single selected block or Either.left==number of blocks
     */
    public Either<Integer, PlantingBlock<E>> getSingleSelectedBlock() {
        return fieldView.getSingleSelectedBlock();
    }

    public void setCellShape(CellShape cellShape) {
        fieldView.setCellShape(cellShape);
    }

    public CellShape getCellShape() {
        return fieldView.getCellShape();
    }

    public void setFieldDimension(int nCols, int nRows) {
        fieldView.getModel().setColumnRowCount(nCols, nRows);
    }

    public FieldView<E> getFieldView() {
        return fieldView;
    }

    public void setContentFactory(BiFunction<PlantingBlock<E>, Point, E> f) {
        fieldView.setContentFactory(f);
    }

    @Override
    public Point getMaxFieldCoordinate() {
        return fieldView.getMaxFieldCoordinate();
    }

}
