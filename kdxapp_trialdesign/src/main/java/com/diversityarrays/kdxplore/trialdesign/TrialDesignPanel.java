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
package com.diversityarrays.kdxplore.trialdesign;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.design.EntryCountChangeListener;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.EntryType.Variant;
import com.diversityarrays.kdxplore.fieldlayout.DesignParams;
import com.diversityarrays.kdxplore.fieldlayout.LocationsPanel;
import com.diversityarrays.kdxplore.fieldlayout.ManualLayoutPanel;
import com.diversityarrays.kdxplore.fieldlayout.SiteLocation;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ColorSupplier;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.MessagePrinter;

@SuppressWarnings("nls")
public class TrialDesignPanel extends JPanel {

    private final FileDrop fileDrop = new FileDrop() {
        @Override
        public void dropFiles(Component c, List<File> list, DropLocationInfo dli) {
            File file = list.get(0);
            SwingUtilities.invokeLater(() -> doImportFile(file));
        }
    };

    private FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

    private final TrialEntryTableModel entryTableModel = new TrialEntryTableModel();
    private JTable entriesTable = new JTable(entryTableModel);
    private final PromptScrollPane entriesScrollPane = new PromptScrollPane(entriesTable,
            "Drag/Drop CSV file here");
//"Drag/Drop Excel\nor CSV file");

    private final DefaultComboBoxModel<String> experimentModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> experimentCombo = new JComboBox<>(experimentModel);
    private Map<String, List<TrialEntry>> entriesByExperiment = Collections.emptyMap();
//    private final AlgorithmSelector algorithmSelector;

    private LocationsPanel locationsPanel;

    private JTabbedPane tabbedPane = new JTabbedPane();

    private Consumer<DesignParams> totalPlotsChanged = new Consumer<DesignParams>() {
        @Override
        public void accept(DesignParams dp) {
            Map<EntryType, Integer> countByType = getEntryTypeCountsRequired();
            locationsPanel.setDesignParams(dp, countByType);
        }
    };

    private ManualLayoutPanel manualLayoutPanel = new ManualLayoutPanel(
            ManualLayoutPanel.FOR_TRIAL, totalPlotsChanged);

    private final Function<EntryType, Color> colorSupplier = new ColorSupplier<>();

    private IntConsumer onLocationCountChanged = new IntConsumer() {
        @Override
        public void accept(int nLocations) {
        	System.out.println("TrialDesignPanel: onLocationCountChanged(" + nLocations + ")");
//            algorithmSelector.setLocationCount(nLocations);
        }
    };

    private final Consumer<SiteLocation> onLocationChanged = new Consumer<SiteLocation>() {
        @Override
        public void accept(SiteLocation loc) {
            new Toast(TrialDesignPanel.this, "Location Changed: " + loc.name, 1000).show();
        }
    };

    private JFileChooser fileChooser;
    private final Action loadEntriesFileAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (fileChooser == null) {
                fileChooser = Shared.getFileChooser(Shared.For.FILE_LOAD, Shared.CSV_FILE_FILTER, Shared.TXT_FILE_FILTER);
            }

            if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(TrialDesignPanel.this)) {
                File file = fileChooser.getSelectedFile();
                SwingUtilities.invokeLater(() -> doImportFile(file));
            }
        }
    };

    private final Action removeEntriesAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Integer> modelRows = GuiUtil.getSelectedModelRows(entriesTable);
            entryTableModel.removeEntriesAt(modelRows);
        }
    };
    private final Action addEntryAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            MsgBox.warn(TrialDesignPanel.this, "Not Yet Available", "Add Entry");
        }
    };

    private final JSplitPane mainSplitPane;

    private final ChangeListener tabChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            updateEntryTypesAvailable();
        }
    };

    private final BackgroundRunner backgroundRunner;

    private Supplier<List<TrialEntry>> trialEntriesSupplier = new Supplier<List<TrialEntry>>() {
        @Override
        public List<TrialEntry> get() {
            return entryTableModel.getEntries();
        }
    };

    private final MessagePrinter messagePrinter;

    public TrialDesignPanel(KdxPluginInfo pluginInfo) {
        super(new BorderLayout());

        messagePrinter = pluginInfo.getMessagePrinter();

        backgroundRunner = pluginInfo.getBackgroundRunner();
//        algorithmSelector = new AlgorithmSelector(backgroundRunner);

        experimentCombo.setVisible(false);

        KDClientUtils.initAction(ImageId.ADD_CSV_24, loadEntriesFileAction, "Import Entries");
        KDClientUtils.initAction(ImageId.MINUS_GOLD_24, removeEntriesAction, "Remove Entries");
        KDClientUtils.initAction(ImageId.PLUS_GOLD_24, addEntryAction, "Add Entry (disabled until next release)");

        addEntryAction.setEnabled(RunMode.getRunMode().isDeveloper());

        entriesTable.setTransferHandler(flth);
        entriesScrollPane.setTransferHandler(flth);

        entriesTable.setAutoCreateRowSorter(true);
        entriesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    removeEntriesAction.setEnabled(entriesTable.getSelectedRowCount() > 0);
                }
            }
        });
        removeEntriesAction.setEnabled(false);

        JPanel entriesPanel = new JPanel(new BorderLayout());

        Box box = Box.createHorizontalBox();
        box.add(experimentCombo);
        box.add(Box.createHorizontalGlue());
        box.add(new JButton(addEntryAction));
        box.add(Box.createHorizontalStrut(10));
        box.add(new JButton(removeEntriesAction));
        box.add(Box.createHorizontalGlue());
        box.add(new JButton(loadEntriesFileAction));

        entriesPanel.add(GuiUtil.createLabelSeparator("Entry List | Experiment:", box), BorderLayout.NORTH);
        entriesPanel.add(entriesScrollPane, BorderLayout.CENTER);

        locationsPanel = new LocationsPanel(
                colorSupplier,
                onLocationChanged,
                onLocationCountChanged,
                manualLayoutPanel,
                trialEntriesSupplier,
                (s) -> messagePrinter.println(s));

        experimentCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object item = experimentCombo.getSelectedItem();
                if (item == null) {

                }
                else {
                    String expName = item.toString();
                    List<TrialEntry> list = entriesByExperiment.get(expName);
                    entryTableModel.setEntries(list);

                    Set<String> locationNames = list.stream().map(TrialEntry::getLocation)
                        .filter(s -> ! Check.isEmpty(s))
                        .collect(Collectors.toSet());
                    locationsPanel.setLocationNames(locationNames);
                }
            }
        });
        entryTableModel.addEntryCountChangeListener(new EntryCountChangeListener() {
            @Override
            public void entryCountChanged(Object source) {
                manualLayoutPanel.setDesignEntries(entryTableModel.getEntries());

                updateEntryTypesAvailable();

                Set<String> locationNames = entryTableModel.getEntries().stream()
                    .filter(te -> ! Check.isEmpty(te.getLocation()))
                    .map(TrialEntry::getLocation)
                    .collect(Collectors.toSet());

                locationsPanel.setLocationNames(locationNames);
//                algorithmSelector.setEntries(entryTableModel.getEntries());
            }
        });

        tabbedPane.addTab("Manual Layout", manualLayoutPanel);
//        if (RunMode.getRunMode().isDeveloper()) {
//            tabbedPane.addTab("Trial Algorithm", algorithmSelector);
//        }
        tabbedPane.addChangeListener(tabChangeListener);
        updateEntryTypesAvailable();

        JSplitPane designsAndLocations = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                tabbedPane, locationsPanel);
        designsAndLocations.setResizeWeight(0.5);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                entriesPanel,
                designsAndLocations); // designAndMapsPane);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setResizeWeight(0.17);

        add(mainSplitPane, BorderLayout.CENTER);
    }

    private void doImportFile(File file) {

        if (Shared.XLS_FILE_FILTER.accept(file)) {
            if (! RunMode.getRunMode().isDeveloper()) {
                MsgBox.info(this,
                        "Sorry - importing from Excel isn't supported yet",
                        "Load Entries");
                return;
            }
        }

        TrialEntryFileImportDialog dlg = new TrialEntryFileImportDialog(
                GuiUtil.getOwnerWindow(this),
                "File Import for Trial Entries",
                file);
        dlg.setVisible(true);

        if (dlg.entryFile != null) {
            entriesByExperiment = dlg.entryFile.getEntries().stream()
                .collect(Collectors.groupingBy(TrialEntry::getExperimentName));

            entryTableModel.setEntryFile(dlg.entryFile);

            Set<String> experimentNames = new TreeSet<>(entriesByExperiment.keySet());

            experimentModel.removeAllElements();
            if (experimentNames.isEmpty()) {
                experimentCombo.setVisible(false);
                entryTableModel.setEntries(dlg.entryFile.getEntries());
            }
            else {
                for (String expName : experimentNames) {
                    experimentModel.addElement(expName);
                }
                experimentCombo.setSelectedIndex(0);
                experimentCombo.setVisible(true);
            }
        }
    }

    private void updateEntryTypesAvailable() {
        Map<EntryType, Integer> countByType = getEntryTypeCountsRequired();
        locationsPanel.setEntryTypeCounts(countByType);
    }

    private boolean getIsManual() {
        boolean isManual = false;
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0) {
            Component c = tabbedPane.getComponentAt(index);
            isManual = manualLayoutPanel == c;
        }
        return isManual;
    }

    private Map<EntryType, Integer> getEntryTypeCountsRequired() {

        Map<EntryType, List<TrialEntry>> map = entryTableModel.getEntries().stream()
        		.filter(entry -> entry.getEntryType() != null)
        		.collect(Collectors.groupingBy(TrialEntry::getEntryType));

        Map<EntryType, Integer> countByType = map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e-> e.getValue().size()));

        if (getIsManual()) {
            int nSpatialsRequired = manualLayoutPanel.getSpatialChecksCountPerReplicate();
            if (nSpatialsRequired > 0) {

                String spatialName = TrialDesignPreferences.getInstance().getSpatialEntryName();

                Optional<EntryType> opt = countByType.keySet().stream()
                        .filter(e -> e.getName().equalsIgnoreCase(spatialName))
                        .findFirst();

                if (! opt.isPresent()) {
                    EntryType spatial = new EntryType(spatialName, Variant.SPATIAL);
                        countByType.put(spatial, nSpatialsRequired);
                }
            }
        }
        return countByType;
    }
}
