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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent.EventType;
import com.diversityarrays.kdxplore.ui.HelpUtils;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.UnicodeChars;

@SuppressWarnings("nls")
public class EditModeWidget<E> {

    public enum EditMode {
        CELL_TYPE("Site Details"), // Allows arrangement of PlantingBlocks and setting CellTypes
        REPLICATE_CONTENT("Replicate"), // Allows editing the ReplicateCellContent for PlantingBlocks
        ;

        public final String displayValue;
        EditMode(String s) {
            displayValue = s;
        }

        @Override
        public String toString() {
            return displayValue;
        }

        public String asBoldHtml() {
            return "<b>" + displayValue + "</b>";
        }
    }

    private static final String CLEAR_BORDERS = "Clear Field Cells";

    private final Map<EditMode, List<JComponent>> editModeComponents = new HashMap<>();

    private final Map<EditMode, JRadioButton> radioButtonByEditMode = new HashMap<>();
//    private FieldLayoutView<E> fieldLayoutView;
//    FieldView<E> fieldView;

    private JComboBox<FieldCellType> cellTypeCombo = new JComboBox<>(FieldCellType.values());

    private final Action clearBordersAction = new AbstractAction(CLEAR_BORDERS) {
        @Override
        public void actionPerformed(ActionEvent e) {

            List<String> options = new ArrayList<>();
            options.add("All");
            for (FieldCellType fct : FieldCellType.values()) {
                options.add(fct.toString());
            }
            options.add(UnicodeChars.CANCEL_CROSS);

            IntConsumer choiceConsumer = new IntConsumer() {
                @Override
                public void accept(int index) {
                    if (UnicodeChars.CANCEL_CROSS.equals(options.get(index))) {
                        return;
                    }

                    Predicate<FieldCell<?>> predicate = null;
                    if (index > 0) {
                        FieldCellType fcType = FieldCellType.values()[index-1];
                        predicate = new Predicate<FieldCell<?>>() {
                            @Override
                            public boolean test(FieldCell<?> cell) {
                                return fcType.equals(cell.getFieldCellType());
                            }
                        };
                    }
                    clearFieldCells.accept(predicate);
                }
            };

            HelpUtils.askOptionPopup(e, 10,10,
                    "", //"Confirm " + CLEAR_BORDERS, // + "?",
                    choiceConsumer , options.toArray(new String[options.size()]));
        }
    };

    private final JLabel editingBlockName = new JLabel();

    private EditMode editMode;
    private FieldCellType fieldCellType = FieldCellType.DEFAULT_FIELD_CELL_TYPE;

    private final Supplier<Either<Integer, PlantingBlock<E>>> supplySingleSelectedBlock;

    private final Consumer<PlantingBlock<E>> setEditingBlock;

    private final Consumer<Predicate<FieldCell<?>>> clearFieldCells;

    private final Box widgetComponent;

    public EditModeWidget(FieldView<E> view, EditMode editMode,
            Supplier<Either<Integer, PlantingBlock<E>>> supplySingleSelectedBlock,
            Consumer<PlantingBlock<E>> setEditingBlock,
            Consumer<Predicate<FieldCell<?>>> clearFieldCells)
    {
        this.supplySingleSelectedBlock = supplySingleSelectedBlock;
        this.setEditingBlock = setEditingBlock;
        this.clearFieldCells = clearFieldCells;
        this.editMode = editMode;

        editingBlockName.setFont(editingBlockName.getFont().deriveFont(Font.BOLD));
        editingBlockName.setToolTipText("Name of the Replicate being edited");

        KDClientUtils.initAction(ImageId.BACKSPACE_24, clearBordersAction,
                "Clear Field Cells (by type)");

        // Celltype controls
        JLabel cellTypeLabel = new JLabel("Cell Type:");
        JButton clearBordersButton = new JButton(clearBordersAction);
        cellTypeCombo.setSelectedItem(fieldCellType);
        cellTypeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object item = cellTypeCombo.getSelectedItem();
                if (item instanceof FieldCellType) {
                    fieldCellType = (FieldCellType) item;
                }
            }
        });

        Orientation orientation;
        if (FieldCellType.values().length == 1) {
            orientation = Orientation.HORZ;
            editModeComponents.put(EditMode.CELL_TYPE,
                    Arrays.asList(clearBordersButton));
        }
        else {
            orientation = Orientation.VERT;
            editModeComponents.put(EditMode.CELL_TYPE,
                    Arrays.asList(cellTypeLabel, cellTypeCombo, clearBordersButton));
        }

        // Replicate controls
        editModeComponents.put(EditMode.REPLICATE_CONTENT,
                Arrays.asList(editingBlockName));

        ButtonGroup em_bg = new ButtonGroup();
        Map<JRadioButton, EditMode> editModeByRb = new HashMap<>();
        ActionListener emrbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditMode em = editModeByRb.get(e.getSource());
                setEditMode(em);
            }
        };

        widgetComponent = orientation.createBox();//Box.createVerticalBox();
        widgetComponent.setBorder(new TitledBorder("Edit"));
        for (EditMode em : EditMode.values()) {
            JRadioButton rb = new JRadioButton(em.displayValue, em==editMode);
            editModeByRb.put(rb, em);
            radioButtonByEditMode.put(em, rb);
            em_bg.add(rb);
            rb.addActionListener(emrbListener);

            if (Orientation.HORZ == orientation) {
                if (em_bg.getButtonCount() > 1) {
//                    widgetComponent.add(orientation.createGlue());
//                    widgetComponent.add(orientation.createJSeparator());
                    widgetComponent.add(Box.createHorizontalStrut(20));
                }
                widgetComponent.add(rb);
                if (EditMode.CELL_TYPE == em) {
                    editModeComponents.get(EditMode.CELL_TYPE).stream()
                        .forEach((c) -> widgetComponent.add(c));
                }
                else {
                    widgetComponent.add(editingBlockName);
                }
            }
            else {
                Box btnAndControls = Box.createHorizontalBox();
                btnAndControls.add(rb);
                if (EditMode.CELL_TYPE == em) {
                    editModeComponents.get(EditMode.CELL_TYPE).stream()
                        .forEach((c) -> btnAndControls.add(c));
                }
                else {
                    btnAndControls.add(editingBlockName);
                }
                btnAndControls.add(Box.createHorizontalGlue());

                widgetComponent.add(btnAndControls);
            }
        }
        if (Orientation.HORZ == orientation) {
            widgetComponent.add(orientation.createGlue());
        }

        setEditMode(editMode);
    }

    public JComponent getWidgetComponent() {
        return widgetComponent;
    }

    public void setEnableClearBorders(boolean b) {
        clearBordersAction.setEnabled(b);
    }

    public void setEditMode(EditMode selectedItem) {
        editMode = selectedItem;

        for (EditMode em : EditMode.values()) {
            for (JComponent c : editModeComponents.get(em)) {
                c.setEnabled(em==editMode);
            }
        }
        updateEditingBlock();
    }

    private void updateEditingBlock() {
        if (EditMode.REPLICATE_CONTENT == editMode) {
            Either<Integer, PlantingBlock<E>> either = supplySingleSelectedBlock.get();

            if (either.isRight()) {
                PlantingBlock<E> pb = either.right();
                editingBlockName.setText(pb.getName());

                setEditingBlock.accept(pb);
            }
            else {
                editingBlockName.setToolTipText(null);
                setEditingBlock.accept(null);

                int nSelected = either.left();

                new Toast(radioButtonByEditMode.get(EditMode.REPLICATE_CONTENT),
                        String.format("# Selected=%d (Select just one to edit it)", nSelected),
                        Toast.SHORT).show();
                editingBlockName.setText(null);
            }
        }
        else {
            editingBlockName.setText(null);
            setEditingBlock.accept(null);
        }
    }

    public void onBlockClicked(PlantingBlockSelectionEvent<E> e) {
        if (EventType.EDIT == e.eventType) {
            if (e.plantingBlock != null) {
                if (EditMode.REPLICATE_CONTENT == editMode) {
                    setEditingBlock.accept(e.plantingBlock);
                }
                else {
                    radioButtonByEditMode.get(EditMode.REPLICATE_CONTENT).doClick();
                }
            }
        }
    }

    public FieldCellType getFieldCellType() {
        return fieldCellType;
    }
}
