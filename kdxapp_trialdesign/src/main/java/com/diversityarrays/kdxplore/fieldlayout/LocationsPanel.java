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
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.PlantingBlockFactory;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.fielddesign.DefaultFieldModel;
import com.diversityarrays.kdxplore.fielddesign.FieldLayoutEditFrame;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.kdxplore.trialdesign.TrialEntryFile;
import com.diversityarrays.kdxplore.ui.HelpUtils;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.ui.widget.SeparatorPanel;

@SuppressWarnings("nls")
public class LocationsPanel extends JPanel {

    private LocationReplicatesTable replicatesTable;
    private LocationReplicatesTableModel replicatesTableModel;

    private final PromptScrollPane scrollPane;

    private final Action addAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = JOptionPane.showInputDialog(LocationsPanel.this, "Enter Name for new Location");
            if (! Check.isEmpty(name)) {
                Optional<SiteLocation> opt = replicatesTable.getLocationWithName(name);
                if (opt.isPresent()) {
                    MsgBox.warn(LocationsPanel.this, "That name already exists", "Can't Add Location");
                }
                else {
                    int oldCount = replicatesTable.getRowCount();

                    Optional<SiteLocation> opt_loc = replicatesTableModel.addLocation(name);
                    if (opt_loc.isPresent()) {
                        if (replicatesTable.getRowCount() > 0 && oldCount <= 0) {
                            // Auto-select the first one added
                            replicatesTable.getSelectionModel().setSelectionInterval(0, 0);
                        }
                    }
                    else {
                        MsgBox.error(LocationsPanel.this,
                                String.format(
                                        "ReplicatesTableModel thinks the location Site '%s' already exists",
                                        name),
                                "Internal Error");
                    }
                }
            }
        }
    };
    private final Action delAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Integer> selRows = GuiUtil.getSelectedModelRows(replicatesTable);
            if (selRows.isEmpty()) {
                return;
            }
            String option0 = selRows.size() == 1 ? "Delete !" : "Delete " + selRows.size() + " Locations";
            IntConsumer choiceConsumer = new IntConsumer() {
                @Override
                public void accept(int t) {
                    if (t == 0) {
                        replicatesTableModel.removeLocationsAt(selRows);
                    }
                }
            };
            HelpUtils.askOptionPopup(delButton, "", choiceConsumer, option0, UnicodeChars.CANCEL_CROSS);
        }
    };
    private final JButton delButton = new JButton(delAction);

    private final IntConsumer onLocationCountChanged;
    private PropertyChangeListener locationCountChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Object oldValue = evt.getOldValue();
            Object newValue = evt.getNewValue();
            if (oldValue instanceof Integer && newValue instanceof Integer) {
                onLocationCountChanged.accept((Integer) newValue);
            }
        }
    };

    private final Map<SiteLocation, FieldLayoutEditFrame> editorFrameByLocation = new HashMap<>();

    private final PlantingBlockFactory<ReplicateCellContent> blockFactory;

    private final Action openEditorAction = new AbstractAction("Edit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<SiteLocation> list = replicatesTable.getSelectedLocations();

            List<SiteLocation> failed = new ArrayList<>();

            GraphicsConfiguration gc = getGraphicsConfiguration(); //GuiUtil.getOwnerWindow(LocationsPanel.this).getGraphicsConfiguration();
            if (gc != null) {
                GraphicsDevice gDevice = gc.getDevice();
                final Point screenSize = new Point(
                        gDevice.getDisplayMode().getWidth(),
                        gDevice.getDisplayMode().getHeight());
                System.out.println("LocationsPanel: screenSize = " + screenSize.x + "x" + screenSize.y);
            }

            final int xyIncrement = 20;

            Point offset = null;
            if (list.size() > 1) {
                offset = gc.getBounds().getLocation();
            }

            for (SiteLocation loc : list) {
                openEditorForLocation(gc, offset, loc, (l) -> failed.add(l) );
                if (offset != null) {
                    offset.translate(xyIncrement, xyIncrement);
                }
            }

            if (! failed.isEmpty()) {
                String msg = failed.stream()
                    .map(l -> l.name)
                    .collect(Collectors.joining("\n  ", "Locations:\n", ""));
                MsgBox.warn(LocationsPanel.this, msg, "No Replicates Assigned");
            }
        }
    };
    private final Function<EntryType, Color> colorSupplier;
    private final Map<EntryType, Integer> entryTypeCounts = new HashMap<>();
    private final Supplier<TrialEntryFile> trialEntryFileSupplier;
    private final Consumer<String> messagePrinter;

    public LocationsPanel(
            Function<EntryType, Color> colorSupplier,
            Consumer<SiteLocation> onLocationChanged,
            IntConsumer onLocationCountChanged,
            PlantingBlockFactory<ReplicateCellContent> blockFactory,
            Supplier<TrialEntryFile> trialEntryFileSupplier,
            Consumer<String> messagePrinter
            )
    {
        super(new BorderLayout());

        this.colorSupplier = colorSupplier;
        this.onLocationCountChanged = onLocationCountChanged;
        this.blockFactory = blockFactory;
        this.trialEntryFileSupplier = trialEntryFileSupplier;
        this.messagePrinter = messagePrinter;

        replicatesTable = new LocationReplicatesTable(blockFactory, colorSupplier, onLocationChanged);
        replicatesTableModel = replicatesTable.getLocationReplicatesTableModel();

        if (this.onLocationCountChanged != null) {
            replicatesTableModel.addPropertyChangeListener(LocationReplicatesTableModel.PROPERTY_LOCATION_COUNT, locationCountChangeListener);
        }

        scrollPane = new PromptScrollPane(replicatesTable, "Locations go here");

        KDClientUtils.initAction(ImageId.PLUS_BLUE_24, addAction, "Add a Location");
        KDClientUtils.initAction(ImageId.MINUS_BLUE_24, delAction, "Remove Locations");
        KDClientUtils.initAction(ImageId.EDIT_BLUE_24, openEditorAction,
                "<HTML>Open Location Editor(s)<BR>(or double-click a Location row)");

        replicatesTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                addAction.setEnabled(replicatesTableModel.isEditable());
            }
        });

        replicatesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (2 == e.getClickCount()) {
                    Point point = e.getPoint();
                    int viewRow = replicatesTable.rowAtPoint(point);
                    if (viewRow >= 0) {
                        int modelRow = replicatesTable.convertRowIndexToModel(viewRow);
                        if (modelRow >= 0) {
                            e.consume();

                            SiteLocation loc = replicatesTableModel.getLocationAt(modelRow);

                            SwingUtilities.invokeLater(() ->
                                openEditorForLocation(
                                    loc,
                                    (l) -> MsgBox.warn(LocationsPanel.this,
                                            l.name, "No Replicates Assigned"))
                            );
                        }
                    }
                }
            }

        });
        replicatesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    updateActions();
                }
            }
        });
        updateActions();

        Box btns = Box.createHorizontalBox();
        btns.add(new JButton(openEditorAction));
        btns.add(Box.createHorizontalGlue());
        btns.add(new JButton(addAction));
        btns.add(Box.createHorizontalStrut(8));
        btns.add(delButton);

        SeparatorPanel header = GuiUtil.createLabelSeparator("Locations:", btns);
        if (RunMode.getRunMode().isDeveloper()) {
            new Toast((Component) null, "SHIFT click on Locations: to make 3", Toast.SHORT).show();
            header.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (0 != (MouseEvent.SHIFT_MASK & e.getModifiers())) {
                        if (replicatesTableModel.getRowCount() > 0) {
                            return;
                        }

                        // Shift click on "Locations" header adds 3 locations (for testing)
                        e.consume();

                        List<SiteLocation> list = new ArrayList<>();
                        for (int i = 0; i < 3; ++i) {
                            SiteLocation loc = new SiteLocation("Location#" + (i+1),
                                    DefaultFieldModel.INITIAL_SIDE, DefaultFieldModel.INITIAL_SIDE);
                            list.add(loc);
                        }
                        replicatesTableModel.setLocations(list);
                    }
                }
            });
        }

        GuiUtil.setVisibleRowCount(replicatesTable, 5);
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void updateActions() {
        int nSelectedRows = replicatesTable.getSelectedRowCount();
        openEditorAction.setEnabled(nSelectedRows > 0);
        delAction.setEnabled(replicatesTableModel.isEditable() && nSelectedRows > 0);
    }

    static private Function<String,SiteLocation> CREATE_FROM_NAME = new Function<String,SiteLocation>() {
        @Override
        public SiteLocation apply(String n) {
            return new SiteLocation(n, 0, 0);
        }
    };

    public void setLocationNames(Set<String> locationNames) {
        List<SiteLocation> locs = locationNames.stream()
            .map(n -> CREATE_FROM_NAME.apply(n))
            .collect(Collectors.toList());
        replicatesTableModel.setLocations(locs);
        replicatesTableModel.setEditable(locs.isEmpty());
    }

    public boolean isManualMode() {
        return replicatesTableModel.isEditable();
    }

    public void setDesignParams(DesignParams designParams, Map<EntryType, Integer> countByType) {

        setEntryTypeCounts(countByType);

        Map<SiteLocation, Set<WhatChanged>> changes =
                replicatesTableModel.setDesignParams(designParams, entryTypeCounts);

        for (SiteLocation loc : changes.keySet()) {
            Set<WhatChanged> whatChanged = changes.get(loc);
            if (! whatChanged.isEmpty()) {
                FieldLayoutEditFrame frame = editorFrameByLocation.get(loc);
                if (frame != null) {
                    frame.onDesignParamsChanged(designParams);
                }
            }
        }

//        changedLocations.stream()
//            .map(loc -> editorFrameByLocation.get(loc))
//            .filter(f -> f != null)
//            .forEach(f -> f.onDesignParamsChanged(designParams));
    }

    public void setEntryTypeCounts(Map<EntryType, Integer> countByType) {
        this.entryTypeCounts.clear();
        this.entryTypeCounts.putAll(countByType);
    }

    private void openEditorForLocation(SiteLocation loc,
            Consumer<SiteLocation> onFailed)
    {
        openEditorForLocation(getGraphicsConfiguration(),  null, loc, onFailed);
    }

    private void openEditorForLocation(GraphicsConfiguration gc,
            Point optionalOffset,
            SiteLocation loc,
            Consumer<SiteLocation> onFailed)
    {
        FieldLayoutEditFrame frame = editorFrameByLocation.get(loc);
        if (frame != null) {
            frame.toFront();
        }
        else {
            Optional<Set<ReplicateDetailsModel>> optional = replicatesTableModel.getReplicateModelsForLocation(loc);
            if (! optional.isPresent() || optional.get().isEmpty()) {
                onFailed.accept(loc);
                return;
            }

            frame = new FieldLayoutEditFrame(
                    gc,
                    loc,
                    optional.get(),
                    blockFactory,
                    colorSupplier,
                    entryTypeCounts,
                    trialEntryFileSupplier,
                    messagePrinter);
            if (optionalOffset == null) {
                // Centre on screen
                frame.setLocationRelativeTo(null);
            }
            else {
                // Specific position
                frame.setLocation(optionalOffset);
            }
            editorFrameByLocation.put(loc, frame);

            final FieldLayoutEditFrame f_frame = frame;
            f_frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    f_frame.removeWindowListener(this);
                    editorFrameByLocation.remove(f_frame.getSiteLocation());
                }
            });
            f_frame.setVisible(true);
        }
    }
}
