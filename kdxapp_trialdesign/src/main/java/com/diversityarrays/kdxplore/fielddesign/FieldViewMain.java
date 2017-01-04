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
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.EntryType.Variant;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.fieldlayout.DesignParametersPanel;
import com.diversityarrays.kdxplore.fieldlayout.LocationEditPanel;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockTableModel;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockTablePanel;
import com.diversityarrays.kdxplore.fieldlayout.ReplicateCellContent;
import com.diversityarrays.kdxplore.fieldlayout.SiteLocation;
import com.diversityarrays.kdxplore.fieldlayout.TrialEntryAssignmentDataProvider;
import com.diversityarrays.kdxplore.trialdesign.TrialEntry;
import com.diversityarrays.util.ColorSupplier;

import net.pearcan.ui.StatusInfoLine;
import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.GBH;

@SuppressWarnings("nls")
public class FieldViewMain extends JFrame {

//    private static final String CLEAR_BORDERS = "Clear Borders";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FieldViewMain().setVisible(true);
            }
        });
    }

    private Action addBlockAction = new AbstractAction("+") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Set<Integer> repNumberSet = plantingBlockTableModel.getPlantingBlocks().stream()
                    .map(tb -> Integer.valueOf(tb.getReplicateNumber()))
                .collect(Collectors.toSet());

            int modelBlockCount = plantingBlockTableModel.getRowCount();

            int useReplicateNumber = 0;
            for (int r = modelBlockCount + 1; --r >= 1; ) {
                if (! repNumberSet.contains(r)) {
                    useReplicateNumber = r;
                    break;
                }
            }

            if (useReplicateNumber <= 0) {
                Optional<Integer> opt = repNumberSet.stream().collect(Collectors.maxBy(Integer::compareTo));
                if (opt.isPresent()) {
                    useReplicateNumber = opt.get() + 1;
                }
                else {
                    useReplicateNumber = 1;
                }
            }
            PlantingBlock<ReplicateCellContent> tb = new PlantingBlock<>(
                    useReplicateNumber,
                    "Rep#" + useReplicateNumber,
                    4, 4,
                    0);
            plantingBlockTableModel.addOne(tb);
        }
    };
    private Action removeBlockAction = new AbstractAction("-") {
        @Override
        public void actionPerformed(ActionEvent e) {
            plantingBlockTableModel.removeRows(selectedRowIndices);
        }
    };

    private Map<EntryType, Integer> entryTypeCounts = Arrays.asList("Check", "Dry", "Wet").stream()
            .map(s -> new EntryType(s, Variant.CHECK))
            .collect(Collectors.toMap(Function.identity(), x -> 0));

//    private Predicate<File> fileAcceptable = new Predicate<File>() {
//        @Override
//        public boolean test(File t) {
//            return false;
//        }
//    };
//    private Consumer<File> importHandler = new Consumer<File>() {
//        @Override
//        public void accept(File t) {
//            MsgBox.warn(FieldViewMain.this, "Not Available in Test Harness", "Import File");
//        }
//    };

    private final DesignParametersPanel paramsPanel = new DesignParametersPanel(true);

    private final Function<EntryType, Color> colorSupplier = new ColorSupplier<>();

    private final LocationEditPanel locationEditPanel;

    private StatusInfoLine statusInfoLine = new StatusInfoLine(true);

    private PlantingBlockTableModel<ReplicateCellContent> plantingBlockTableModel;
    protected List<Integer> selectedRowIndices;

    @SuppressWarnings("unchecked")
    FieldViewMain() {
        super("Field View Main");

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Set<ReplicateDetailsModel> dataModels = Collections.emptySet();
        TrialEntryAssignmentDataProvider dataProvider = new TrialEntryAssignmentDataProvider() {

            @Override
            public List<TrialEntry> getTrialEntries() {
                return Collections.emptyList();
            }

            @Override
            public Collection<ReplicateDetailsModel> getAllReplicateDetailModels() {
                return dataModels;
            }
        };
        locationEditPanel = new LocationEditPanel(
                (s) -> { System.out.println("Location Name changed to: '" + s.name + "'"); },
                new SiteLocation("Site-A", 20, 10, true),
                dataModels,
                entryTypeCounts,
//                blockFactory,
                colorSupplier,
                dataProvider,
                (s) -> System.out.println(s));

        try {
            Field field = locationEditPanel.getClass().getDeclaredField("plantingBlockTablePanel");
            if (! PlantingBlockTablePanel.class.isAssignableFrom(field.getType())) {
                throw new RuntimeException(locationEditPanel.getClass().getName() + "." + field.getName()
                        + " is not a PlantingBlockTablePanel");
            }
            field.setAccessible(true);
            PlantingBlockTablePanel<?> blockTablePanel = (PlantingBlockTablePanel<?>) field.get(locationEditPanel);

            Field tbtmField = blockTablePanel.getClass().getDeclaredField("plantingBlockModel");

            if (! PlantingBlockTableModel.class.isAssignableFrom(tbtmField.getType())) {
                throw new RuntimeException(blockTablePanel.getClass().getName() + "." + tbtmField.getName()
                + " is not a PlantingBlockTableModel");
            }
            tbtmField.setAccessible(true);
            plantingBlockTableModel = (PlantingBlockTableModel<ReplicateCellContent>) tbtmField.get(blockTablePanel);

            blockTablePanel.addPlantingBlockSelectionHandler(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> rows) {
                    selectedRowIndices = rows;
                    removeBlockAction.setEnabled(! selectedRowIndices.isEmpty());
                }
            });
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        removeBlockAction.setEnabled(false);

        Box buttons = Box.createHorizontalBox();
        buttons.add(new JButton(addBlockAction));
        buttons.add(new JButton(removeBlockAction));
        buttons.add(Box.createHorizontalGlue());

        Container cp = getContentPane();
        cp.add(paramsPanel, BorderLayout.NORTH);
        cp.add(locationEditPanel, BorderLayout.CENTER);
        cp.add(statusInfoLine, BorderLayout.SOUTH);
        pack();
    }

    static class FieldViewModelControls extends JPanel {

        static private final int MAX_ROWCOL = 200;
        static private final int MAX_WIDHYT = 200;

        static private final String CELL_NAME = "Plot";

        private final FieldModel fieldModel;
        private final FieldView<ReplicateCellContent> fieldView;

        private final SpinnerNumberModel rowCountModel = new SpinnerNumberModel(1, 1, MAX_ROWCOL, 1);
        private final SpinnerNumberModel colCountModel = new SpinnerNumberModel(1, 1, MAX_ROWCOL, 1);
        private final SpinnerNumberModel cellWidModel = new SpinnerNumberModel(1, 1, MAX_WIDHYT, 1);
        private final SpinnerNumberModel cellHytModel = new SpinnerNumberModel(1, 1, MAX_WIDHYT, 1);

        private final Action resizeAction = new AbstractAction("AutoFit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldView.autoFitCellDimension();
            }
        };

        private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (FieldView.PROPERTY_MODEL.equals(propertyName)) {
                    try {
                        changingModelDimension = true;
                        rowCountModel.setValue(fieldModel.getRowCount());
                        colCountModel.setValue(fieldModel.getColumnCount());
                    }
                    finally {
                        changingModelDimension = false;
                    }
                }
                else if (FieldView.PROPERTY_CELL_DIMENSION.equals(propertyName)) {
                    try {
                        changingCellDimension = true;
                        cellWidModel.setValue(fieldView.getCellWidth());
                        cellHytModel.setValue(fieldView.getCellHeight());
                    }
                    finally {
                        changingCellDimension = false;
                    }
                }
            }
        };

        private boolean changingModelDimension;
        private final ChangeListener rowCountChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (! changingModelDimension) {
                    fieldModel.setRowCount(rowCountModel.getNumber().intValue());
                }
            }
        };
        private final ChangeListener colCountChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (! changingModelDimension) {
                    fieldModel.setColumnCount(colCountModel.getNumber().intValue());
                }
            }
        };

        private boolean changingCellDimension;
        private final ChangeListener cellWidthChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (! changingCellDimension) {
                    fieldView.setCellWidth(cellWidModel.getNumber().intValue());
                }
            }
        };
        private final ChangeListener cellHeightChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (! changingCellDimension) {
                    fieldView.setCellHeight(cellHytModel.getNumber().intValue());
                }
            }
        };

        private final SpinnerNumberModel opacityModel = new SpinnerNumberModel(0.5, 0.05, 1.0, 0.05);
        private final NumberSpinner opacitySpinner = new NumberSpinner(opacityModel, "0.00");

        public FieldViewModelControls(FieldView<ReplicateCellContent> v) {
            this.fieldView = v;
            this.fieldModel = fieldView.getModel();

            this.fieldView.addPropertyChangeListener(FieldView.PROPERTY_MODEL, propertyChangeListener);

            rowCountModel.setValue(fieldModel.getRowCount());
            colCountModel.setValue(fieldModel.getColumnCount());

            cellWidModel.setValue(fieldView.getCellWidth());
            cellHytModel.setValue(fieldView.getCellHeight());

            opacityModel.setValue(fieldView.getPlantingBlockOpacity());
            System.out.println(String.format("OpacityModel(%.2f, %.2f, %.2f, %.2f)",
                    opacityModel.getValue(),
                    opacityModel.getMinimum(),
                    opacityModel.getMaximum(),
                    opacityModel.getStepSize()));

            opacityModel.addChangeListener((e) -> fieldView.setPlantingBlockOpacity(opacityModel.getNumber().floatValue()));

            rowCountModel.addChangeListener(rowCountChangeListener);
            colCountModel.addChangeListener(colCountChangeListener);
            cellHytModel.addChangeListener(cellHeightChangeListener);
            cellWidModel.addChangeListener(cellWidthChangeListener);

            GBH gbh = new GBH(this, 2,2,0,0);
            int y = 0;

            gbh.add(0,y, 2,1, GBH.NONE, 1,1, GBH.CENTER, "Model Settings");
            gbh.add(2,y, 1,1, GBH.VERT, 1,1, GBH.CENTER, new JSeparator(JSeparator.VERTICAL));
            gbh.add(3,y, 2,1, GBH.NONE, 1,1, GBH.CENTER, "View Settings");
            ++y;

            gbh.add(0,y, 5,1, GBH.HORZ, 0.5,1, GBH.CENTER, new JSeparator(JSeparator.HORIZONTAL));
            ++y;

            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "# rows");
            gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new NumberSpinner(rowCountModel, "0"));
            gbh.add(2,y, 1,1, GBH.VERT, 1,1, GBH.CENTER, new JSeparator(JSeparator.VERTICAL));
            gbh.add(3,y, 1,1, GBH.NONE, 0,1, GBH.EAST, CELL_NAME + " Height");
            gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new NumberSpinner(cellHytModel, "0"));
            ++y;

            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "# columns");
            gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new NumberSpinner(colCountModel, "0"));
            gbh.add(2,y, 1,1, GBH.VERT, 1,1, GBH.CENTER, new JSeparator(JSeparator.VERTICAL));
            gbh.add(3,y, 1,1, GBH.NONE, 0,1, GBH.EAST, CELL_NAME + " Width");
            gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new NumberSpinner(cellWidModel, "0"));
            ++y;

            gbh.add(3,y, 1,1, GBH.NONE, 0,1, GBH.CENTER, new JButton(resizeAction));
            ++y;

            gbh.add(3,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Opacity");
            gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.WEST, opacitySpinner);
            ++y;

        }
    }
}
