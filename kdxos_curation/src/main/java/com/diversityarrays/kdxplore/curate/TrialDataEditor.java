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
package com.diversityarrays.kdxplore.curate;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.imageio.spi.ServiceRegistry;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.daldb.core.GeneralType;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle.Prefix;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.Shared.Log;
import com.diversityarrays.kdxplore.TitledTablePanelWithResizeControls;
import com.diversityarrays.kdxplore.curate.data.TraitHelper.ValueFactory;
import com.diversityarrays.kdxplore.curate.fieldview.FieldLayoutView;
import com.diversityarrays.kdxplore.curate.fieldview.FieldViewSelectionModel;
import com.diversityarrays.kdxplore.curate.fieldview.InterceptFieldLayoutView;
import com.diversityarrays.kdxplore.curate.undoredo.ChangeManager;
import com.diversityarrays.kdxplore.curate.undoredo.PlotAndSampleChanger;
import com.diversityarrays.kdxplore.data.EntityUtils;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.dal.SampleType;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.data.kdx.UnsavedChangesListener;
import com.diversityarrays.kdxplore.data.tool.LookupRecord;
import com.diversityarrays.kdxplore.exportdata.CurationDataExporter;
import com.diversityarrays.kdxplore.exportdata.CurationExportHelper;
import com.diversityarrays.kdxplore.exportdata.ExportWhatDialog;
import com.diversityarrays.kdxplore.exportdata.WhichTraitInstances;
import com.diversityarrays.kdxplore.field.FieldLayoutTableModel;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.ui.CellSelectableTable;
import com.diversityarrays.kdxplore.ui.CellSelectionListener;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.kdxplore.vistool.VisualisationTool;
import com.diversityarrays.kdxplore.vistool.VisualisationToolActionListener;
import com.diversityarrays.kdxplore.vistool.VisualisationToolController;
import com.diversityarrays.kdxplore.vistool.VisualisationToolId;
import com.diversityarrays.kdxplore.vistool.VisualisationToolService;
import com.diversityarrays.kdxplore.vistool.VisualisationToolService.VisToolParams;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MessageLogger;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.ReportIssueAction;
import com.diversityarrays.util.RunMode;

import net.pearcan.color.ColorPair;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.StatusInfoLine;
import net.pearcan.ui.desktop.DesktopObject;
import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.ui.desktop.WindowsMenuManager;
import net.pearcan.ui.widget.MessagesPanel;
import net.pearcan.util.StringUtil;

public class TrialDataEditor extends JPanel implements DesktopObject {

    private static final String PROBLEM = "Problem";

    private static final String TAB_SAMPLE_DATA = "Sample Data";

    private static final String TAB_TRAIT_VALUE = "Trait Value";

    // private static final String UNDOCK_ACTION_TITLE = "\u25a3 - Undock"; //
    // // Black // square
          // inside
          // white

    private static final String TAB_SAMPLES = "Curation Samples";

    private static final String PHRASE_SHOWING = Msg.PHRASE_SHOWING();
    private static final String PHRASE_SHOWING_ALL = Msg.PHRASE_SHOWING_ALL();

    private static final String CARD_READ_ONLY = "CARD_READ_ONLY"; //$NON-NLS-1$

    private static final String CARD_EDIT_CELL = "CARD_EDIT_CELL"; //$NON-NLS-1$

    private static final String CARD_NO_CELL = "CARD_NO_CELL"; //$NON-NLS-1$

    private static final String CARD_CALC = "CARD_CALC"; //$NON-NLS-1$

    private static final String WARNING_FLAG = "======= WARNING ========";

    private static final String SAMPLES_TABLE_NAME = "Samples Table";

    private static final String TAG = TrialDataEditor.class.getSimpleName();

    public static enum HowStarted {
        UNDO, REDO, CURATION_DATA_CHANGED, SAMPLE_TABLE_SELECTION_CHANGED, FIELD_VIEW_SELECTION_CHANGED, FIELD_VIEW_CHANGED_ACTIVE_TRAIT_INSTANCE, VISTOOL_SELECTION
    }

    static public TrialDataEditor createTrialDataEditor(
            OfflineData offlineData,
            CurationData curationData,
            WindowOpener<JFrame> windowOpener,
            MessageLogger messageLogger)
                    throws IOException {
        IntFunction<Trait> traitProvider = EntityUtils.createTraitProvider(offlineData,
                curationData.getTrial());

        SampleType[] sampleTypes;

        List<LookupRecord<GeneralType>> sampleTypeRecords = offlineData.getKddartReferenceData()
                .getSampleTypes();

        Optional<LookupRecord<GeneralType>> ff = sampleTypeRecords.stream().filter(lr -> lr.getSource() == null).findFirst();
        if (ff.isPresent()) {
        	// At least one with null source
            sampleTypes = new SampleType[0];

        }
        else {
        	// All have source
            sampleTypes = new SampleType[sampleTypeRecords.size()];
            int i = 0;
            for (LookupRecord<GeneralType> lr : sampleTypeRecords) {
                GeneralType gt = lr.getSource();
                if (gt == null) {
                	throw new RuntimeException("LookupRecord.source is null !");
//                    // TODO fix this hack properly
//                    gt = KDDartEntityFactory.Util.getInstance().newGeneralType();
//                    gt.setClassValue(GeneralTypeClass.SAMPLE.classValue);
//                    gt.setIsTypeActive(true);
//                    gt.setTypeId(lr.id);
//                    gt.setTypeName(lr.nameValue);
//                    gt.setTypeNote(lr.noteValue);
                }
                sampleTypes[i] = new SampleType(gt);
                ++i;
            }
        }

        TrialDataEditor editor = new TrialDataEditor(curationData,
                windowOpener,
                messageLogger,
                offlineData.getKdxploreDatabase(),
                traitProvider,
                sampleTypes);

        return editor;
    }

    private final SelectedValueStore selectedValueStore;

    private CurationData curationData;

    private CardLayout cardLayout = new CardLayout();
    private JPanel cardLayoutPanel = new JPanel(cardLayout);

    private final CurationCellEditor curationCellEditor;

    private final CurationTableModel curationTableModel;
    private final CurationTable curationTable;

    private final InterceptFieldLayoutView fieldLayoutView;

    private final Supplier<TraitColorProvider> colorProviderFactory = new Supplier<TraitColorProvider>() {
        @Override
        public TraitColorProvider get() {
            return curationData.getTraitColorProvider();
        }
    };
    private final PlotCellChoicesPanel plotCellChoicesPanel;

    private final SuppressionHandler suppressionHandler;

    private final JTabbedPane mainTabbedPane = new JTabbedPane();

    private JSplitPane leftSplit;
    private JSplitPane leftAndRightSplit;
    private JSplitPane mainVerticalSplit;

    private final Font smallFont;

    private final MessageLogger messageLogger;
    private final Set<Integer> missingTraitIds = new HashSet<>();

    private final StatusInfoLine statusInfoLine = new StatusInfoLine();

    private final IntFunction<Trait> traitProvider;

    private WindowOpener<JFrame> windowOpener;

    // private Icon fieldViewIcon;
    private Icon samplesTableIcon;

    // private Closure<JComponent> restoreClosure = new Closure<JComponent>() {
    // @Override
    // public void execute(JComponent c) {
    //// if (fieldLayoutViewPanel == c) {
    //// trialViewTabbedPane.insertTab(TAB_FIELD, fieldViewIcon,
    // fieldLayoutViewPanel,
    //// TOOLTIP_FIELD_VIEW, TAB_INDEX_FIELD_VIEW);
    //// undockViewAction.setEnabled(true);
    //// }
    //
    //
    //// if (curationTablePanel == c) {
    //// trialViewTabbedPane.insertTab(TAB_SAMPLES, samplesTableIcon,
    // curationTablePanel,
    //// TOOLTIP_SAMPLES_TABLE, 0); // TAB_INDEX_SAMPLES_TABLE);
    //// undockViewAction.setEnabled(true);
    //// }
    // }
    // };

    // private PropertyChangeListener redockPCL = new PropertyChangeListener() {
    // @Override
    // public void propertyChange(PropertyChangeEvent evt) {
    // JFrame undockFrame = (JFrame) evt.getNewValue();
    // if (undockFrame==null) {
    // // has been undocked...
    // undockViewAction.setEnabled(true);
    // }
    // }
    // };
    // // Called when the panel has been successfully undocked.
    // private Closure<JFrame> undockedFrameHandler = new Closure<JFrame>() {
    // @Override
    // public void execute(JFrame undockFrame) {
    // Window w = GuiUtil.getOwnerWindow(TrialDataEditor.this);
    // UndockedPanelManager.handleUndock(w, undockFrame);
    // undockViewAction.setEnabled(false);
    // w.repaint();
    //
    // undockedPanelManager.removePropertyChangeListener(UndockedPanelManager.REDOCKED,
    // redockPCL);
    // undockedPanelManager = null;
    // }
    // };

    // private Action undockViewAction = new AbstractAction(UNDOCK_ACTION_TITLE)
    // {
    // @Override
    // public void actionPerformed(ActionEvent e) {
    // if (undockedPanelManager != null) {
    // return;
    // }
    //
    // Pair<String,JComponent> pair = getActiveTabComponent(false);
    // if (pair != null) {
    // undockedPanelManager = new UndockedPanelManager(pair.second,
    // restoreClosure);
    // undockedPanelManager.addPropertyChangeListener(UndockedPanelManager.REDOCKED,
    // redockPCL);
    // if (undockedPanelManager.undock(pair.first, undockedFrameHandler)) {
    // // ? to anything?
    //// undockViewAction.setEnabled(false); // don in the undockedFrameHandler
    // }
    // }
    // }
    //
    // @Override
    // public void setEnabled(boolean b) {
    // super.setEnabled(b);
    // for (JButton btn : undockButtons) {
    // btn.setEnabled(b);;
    // }
    // }
    // };
    // private final List<JButton> undockButtons = new ArrayList<>();
    //
    // private UndockedPanelManager undockedPanelManager;

    private AbstractCurationTableCellRenderer curationCellRenderer;

    private final TraitsAndInstances/* Panel */ traitsAndInstancesPanel;

    private final ChangeManager<PlotAndSampleChanger> changeManager = new ChangeManager<>();

    private String tAndIpanelLabel;

    private RowSorterListener rowSorterListener = new RowSorterListener() {
        @Override
        public void sorterChanged(RowSorterEvent e) {
            curationTableSelectionModel.handleSorterChanged(curationTable, e);
        }
    };

    private final CurationTableSelectionModel curationTableSelectionModel;

    private final Map<TraitInstance, TraitInstanceValueRetriever<?>> tivrByTi;

    private String cellSelectionBusy = null;

    private final CellSelectionListener fieldViewCellSelectionListener = new CellSelectionListener() {

        @Override
        public void handleChangeEvent(EventType eventType, ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }

            if (cellSelectionBusy != null) {
                Shared.Log.d(TAG,
                        "fieldViewCellSelectionListener.handleChangeEvent: ***** LOOPED doing " //$NON-NLS-1$
                                + cellSelectionBusy);
            }
            else {
                Shared.Log.d(TAG,
                        "fieldViewCellSelectionListener.handleChangeEvent: BEGIN " //$NON-NLS-1$
                                + eventType.name());

                cellSelectionBusy = "fieldViewCellSelectionListener.handleChangeEvent " //$NON-NLS-1$
                        + eventType.name();
                try {
                    startEdit(HowStarted.FIELD_VIEW_SELECTION_CHANGED);
                }
                finally {
                    cellSelectionBusy = null;
                    Shared.Log.d(TAG,
                            "fieldViewCellSelectionListener.handleChangeEvent: END " //$NON-NLS-1$
                                    + eventType.name());
                }
            }
        }
    };

    private final CellSelectionListener curationTableCellSelectionListener = new CellSelectionListener() {

        @Override
        public void handleChangeEvent(EventType eventType, ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }

            if (cellSelectionBusy != null) {
                Shared.Log.d(TAG,
                        "curationTableCellSelectionListener.handleChangeEvent: ***** LOOPED doing " //$NON-NLS-1$
                                + cellSelectionBusy);
            }
            else {
                Shared.Log.d(TAG,
                        "curationTableCellSelectionListener.handleChangeEvent: BEGIN " //$NON-NLS-1$
                                + eventType.name());

                cellSelectionBusy = "curationTableCellSelectionListener.handleChangeEvent " //$NON-NLS-1$
                        + eventType.name();
                try {
                    fieldLayoutView.clearSelection();

                    // selectedValueStore.clearSelectedPlotsFor(curationTable.getName());

                    PlotsByTraitInstance pByTi = getPlotsSelectedInTable();
                    if (! pByTi.isEmpty()) {
                        valuesTabbedPane.setSelectedIndex(valuesTabbedPane.indexOfTab(TAB_TRAIT_VALUE));
                    }
                    selectedValueStore.setSelectedPlots(curationTable.getName(), pByTi);

                    String toolId = ""; //$NON-NLS-1$
                    toolController.updateSelectedSamplesExceptFor(toolId);
                    // FIXME consider not doing clearSelection above but as part
                    // of updateSelectedMeasurements
                    fieldLayoutView.updateSelectedMeasurements(
                            "TrialDataEditor.curationTableCellSelectionListener.handleChangeEvent"); //$NON-NLS-1$

                    curationTableSelectionModel.updateSampleSelection();

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            curationTable.repaint();
                            fieldLayoutView.repaint();
                        }
                    });

                    startEdit(HowStarted.SAMPLE_TABLE_SELECTION_CHANGED);
                }
                finally {
                    cellSelectionBusy = null;
                    Shared.Log.d(TAG,
                            "curationTableCellSelectionListener.handleChangeEvent: END " //$NON-NLS-1$
                                    + eventType.name());
                }
            }
        }
    };

    private final Closure<Void> refreshFieldLayoutView = new Closure<Void>() {
        @Override
        public void execute(Void v) {
            startEdit(HowStarted.CURATION_DATA_CHANGED);
        }
    };

    private final CurationMenuProvider curationMenuProvider;

    private final JSplitPane statsAndSamplesSplit;

    static private int UNIQUE_CURATION_TABLE_ID = 0;

    public TrialDataEditor(CurationData cd,
            WindowOpener<JFrame> windowOpener,
            MessageLogger messageLogger,
            KdxploreDatabase kdxdb,
            IntFunction<Trait> traitProvider,
            SampleType[] sampleTypes)
                    throws IOException {
        super(new BorderLayout());

        this.traitProvider = traitProvider;
        this.windowOpener = windowOpener;

        this.curationData = cd;
        this.curationData.setChangeManager(changeManager);
        this.curationData.setKDSmartDatabase(kdxdb.getKDXploreKSmartDatabase());

        inactiveTagFilterIcon = KDClientUtils.getIcon(ImageId.TAG_FILTER_24);
        activeTagFilterIcon = KDClientUtils.getIcon(ImageId.TAG_FILTER_ACTIVE_24);

        inactivePlotOrSpecimenFilterIcon = KDClientUtils.getIcon(ImageId.FILTER_PLOT_SPEC_INACTIVE);
        activePlotFilterIcon = KDClientUtils.getIcon(ImageId.FILTER_PLOT_ACTIVE);
        activeSpecimenFilterIcon = KDClientUtils.getIcon(ImageId.FILTER_SPEC_ACTIVE);

        updatePlotSpecimenIcon();

        TraitColorProvider traitColourProvider = new TraitColorProvider(false);
        this.curationData.setTraitColorProvider(traitColourProvider);

        curationData.addCurationDataChangeListener(new CurationDataChangeListener() {
            @Override
            public void plotActivationChanged(Object source, boolean activated, List<Plot> plots) {
                updateRowFilter();
                if (toolController != null) {
                    toolController.plotActivationsChanged(activated, plots);
                }
            }

            @Override
            public void editedSamplesChanged(Object source, List<CurationCellId> curationCellIds) {
                if (toolController != null) {
                    toolController.editedSamplesChanged();
                }
            }
        });

        this.selectedValueStore = new SelectedValueStore(curationData.getTrial().getTrialName());

        this.messageLogger = messageLogger;

        smallFont = KDClientUtils.makeSmallFont(this);

        // undockViewAction.putValue(Action.SHORT_DESCRIPTION, "Click to undock
        // this view");

        KDClientUtils.initAction(ImageId.HELP_24, curationHelpAction, Msg.TOOLTIP_HELP_DATA_CURATION(), false);

        KDClientUtils.initAction(ImageId.SAVE_24, saveChangesAction, Msg.TOOLTIP_SAVE_CHANGES(), true);
        KDClientUtils.initAction(ImageId.EXPORT_24, exportCuratedData, Msg.TOOLTIP_EXPORT(), true);

        KDClientUtils.initAction(ImageId.UNDO_24, undoAction, Msg.TOOLTIP_UNDO(), true);
        KDClientUtils.initAction(ImageId.REDO_24, redoAction, Msg.TOOLTIP_REDO(), true);

        KDClientUtils.initAction(ImageId.FIELD_VIEW_24, showFieldViewAction, Msg.TOOLTIP_FIELD_VIEW(), false);

        KDClientUtils.initAction(ImageId.GET_TRIALINFO_24, importCuratedData, Msg.TOOLTIP_IMPORT_DATA(), true);

        Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
            @Override
            public List<KdxSample> apply(TraitInstance ti) {
                return curationData.getSampleMeasurements(ti);
            }
        };
        tivrByTi = VisToolUtil.buildTraitInstanceValueRetrieverMap(
                curationData.getTrial(),
                curationData.getTraitInstances(),
                sampleProvider);

        // = = = = = = = =

        boolean readOnly = false;
        // FIXME work out if the Trial can be edited or not
        curationTableModel = new CurationTableModel(curationData, readOnly);
        // See FIXME comment in CurationTableModel.isReadOnly()

        curationTableSelectionModel = new CurationTableSelectionModelImpl(selectedValueStore,
                curationTableModel);

        curationTableSelectionModel
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        curationTable = new CurationTable("CurationTable-" + (++UNIQUE_CURATION_TABLE_ID), //$NON-NLS-1$
                curationTableModel,
                curationTableSelectionModel);
        curationTable.setCellSelectionEnabled(true);
        curationTable.setAutoCreateRowSorter(true);

        // = = = = = = = =
        this.sampleSourcesTablePanel = new SampleSourcesTablePanel(curationData, curationTableModel, handler);

        @SuppressWarnings("unchecked")
        TableRowSorter<CurationTableModel> rowSorter = (TableRowSorter<CurationTableModel>) curationTable
                .getRowSorter();
        rowSorter.setSortsOnUpdates(true);
        rowSorter.addRowSorterListener(rowSorterListener);

        curationCellRenderer = new CurationTableCellRenderer(
                curationTableModel,
                colorProviderFactory,
                curationTableSelectionModel);

        curationTable.setDefaultRenderer(Object.class, curationCellRenderer);
        curationTable.setDefaultRenderer(Integer.class, curationCellRenderer);
        curationTable.setDefaultRenderer(String.class, curationCellRenderer);
        curationTable.setDefaultRenderer(CurationCellValue.class, curationCellRenderer);
        curationTable.setDefaultRenderer(Double.class, curationCellRenderer);
        curationTable.setDefaultRenderer(TraitValue.class, curationCellRenderer);

        // If either the rows selected change or the columns selected change
        // then we
        // need to inform the visualisation tools.
        curationTable.getSelectionModel()
                .addListSelectionListener(curationTableCellSelectionListener);
        curationTable.getColumnModel().addColumnModelListener(curationTableCellSelectionListener);

        fieldLayoutView = new InterceptFieldLayoutView();

        this.curationCellEditor = new CurationCellEditorImpl(
                curationTableModel,
                fieldLayoutView,
                curationData,
                refreshFieldLayoutView,
                kdxdb,
                traitProvider,
                sampleTypes);

        SuppressionInfoProvider suppressionInfoProvider = new SuppressionInfoProvider(
                curationData,
                curationTableModel,
                curationTable);

        suppressionHandler = new SuppressionHandler(curationContext, curationData,
                curationCellEditor, suppressionInfoProvider);

        loadVisualisationTools();

        curationMenuProvider = new CurationMenuProvider(
                curationContext,
                curationData,
                messages,
                visualisationTools,
                suppressionHandler);

        curationMenuProvider.setSuppressionInfoProvider(suppressionInfoProvider);

        fieldLayoutView.addTraitInstanceSelectionListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (ItemEvent.SELECTED == e.getStateChange()) {
                    TraitInstance traitInstance = fieldLayoutView.getActiveTraitInstance(true);
                    if (traitInstance == null) {
                        curationCellEditor.setCurationCellValue(null);
                    }
                    else {
                        // startEdit(HowStarted.FIELD_VIEW_CHANGED_ACTIVE_TRAIT_INSTANCE);
                    }
                    fieldLayoutView.updateSamplesSelectedInTable();
                }
            }
        });

        // = = = = = = =

        curationData.addUnsavedChangesListener(new UnsavedChangesListener() {
            @Override
            public void unsavedChangesExist(Object source, int nChanges) {
                int unsavedCount = curationData.getUnsavedChangesCount();
                saveChangesAction.setEnabled(unsavedCount > 0);

                if (unsavedCount > 0) {
                    statusInfoLine.setMessage("Unsaved changes: " + unsavedCount);
                }
                else {
                    statusInfoLine.setMessage("No Unsaved changes");
                }
            }
        });

        // curationData.addEditedSampleChangeListener(new ChangeListener() {
        // @Override
        // public void stateChanged(ChangeEvent e) {
        // handleEditedSampleChanges();
        // }
        // });
        saveChangesAction.setEnabled(false);
        changeManager.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateUndoRedoActions();
            }
        });
        undoAction.setEnabled(false);
        redoAction.setEnabled(false);

        // = = = = = = = =

        // TODO provide one of these for each relevant device type
        DeviceType deviceTypeForSamples = null;

        StatsData statsData = curationData.getStatsData(deviceTypeForSamples);

        TIStatsTableModel statsTableModel = new TIStatsTableModel2(curationData, statsData, deviceTypeForSamples);

        traitColourProvider.generateColorMap(statsData.getTraitInstancesWithData());

        Set<Integer> instanceNumbers = statsData.getInstanceNumbers();
        String nonHtmlLabel;
        if (instanceNumbers.size() > 1) {
            tAndIpanelLabel = "<HTML><i>Plot Info</i> &amp; Trait Instances";
            nonHtmlLabel = "Plot Info & Trait Instances";
        }
        else {
            tAndIpanelLabel = "<HTML><i>Plot Info</i> &amp; Traits";
            nonHtmlLabel = "Plot Info & Traits";
        }

        traitsAndInstancesPanel = new TraitsAndInstancesPanel2(
                curationContext,
                smallFont,
                statsTableModel,
                instanceNumbers.size() > 1,
                statsData.getInvalidRuleCount(),
                nonHtmlLabel,
                curationMenuProvider,
                outlierConsumer);

        traitsAndInstancesPanel.addTraitInstanceStatsItemListener(new ItemListener() {
            boolean busy;

            @Override
            public void itemStateChanged(ItemEvent e) {
                // NOTE: we want to process both SELECTED and DESELECTED
                // variants
                if (busy) {
                    Shared.Log.d(TAG, "***** LOOPED in traitsAndInstancesPanel.ItemListener"); //$NON-NLS-1$
                }
                else {
                    Shared.Log.d(TAG, "traitsAndInstancesPanel.ItemListener BEGIN"); //$NON-NLS-1$
                    busy = true;
                    try {
                        updateViewedTraitInstances(e); // !!!!!
                    }
                    finally {
                        busy = false;
                        Shared.Log.d(TAG, "traitsAndInstancesPanel.ItemListener END\n"); //$NON-NLS-1$
                    }
                }
            }
        });

        // = = = = = = = =

        plotCellChoicesPanel = new PlotCellChoicesPanel(curationContext,
                curationData,
                deviceTypeForSamples,
                tAndIpanelLabel, curationMenuProvider, colorProviderFactory);

        // plotCellChoicesPanel.setData(
        // curationData.getPlotAttributes(),
        // traitsAndInstancesPanel.getTraitInstancesWithData());

        plotCellChoicesPanel.addPlotCellChoicesListener(new PlotCellChoicesListener() {
            @Override
            public void traitInstanceChoicesChanged(Object source,
                    boolean choiceAdded, TraitInstance[] choice,
                    Map<Integer, Set<TraitInstance>> traitInstancesByTraitId) {
                traitsAndInstancesPanel.changeTraitInstanceChoice(choiceAdded, choice);
            }

            @Override
            public void plotAttributeChoicesChanged(Object source, List<ValueRetriever<?>> vrList) {
                curationTableModel.setSelectedPlotAttributes(vrList);
            }
        });

        // = = = = = = = =

        statsAndSamplesSplit = createStatsAndSamplesTable(traitsAndInstancesPanel.getComponent());

        mainTabbedPane.addTab(TAB_SAMPLES, samplesTableIcon, statsAndSamplesSplit,
                Msg.TOOLTIP_SAMPLES_TABLE());

        // = = = = = = = =

        checkForInvalidTraits();

        leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createCurationCellEditorComponent(),
                plotCellChoicesPanel);
        leftSplit.setResizeWeight(0.5);
        // traitToDoTaskPaneContainer);

        // rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        // traitsAndInstancesPanel, fieldViewAndPlots);
        // rightSplit.setResizeWeight(0.5);
        // rightSplit.setOneTouchExpandable(true);

        leftAndRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftSplit,
                mainTabbedPane);
        leftAndRightSplit.setOneTouchExpandable(true);

        mainVerticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                messages,
                leftAndRightSplit);
        mainVerticalSplit.setOneTouchExpandable(true);
        mainVerticalSplit.setResizeWeight(0.0);

        add(statusInfoLine, BorderLayout.NORTH);
        add(mainVerticalSplit, BorderLayout.CENTER);

        fieldLayoutView.addCellSelectionListener(fieldViewCellSelectionListener);

        curationTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)
                        && 1 == me.getClickCount()) {
                    me.consume();
                    displayPopupMenu(me);
                }

            }
        });
    }

    private final Transformer<TraitInstance, String> tiNameProvider = new Transformer<TraitInstance, String>() {
        @Override
        public String transform(TraitInstance ti) {
            Trial trial = curationData.getTrial();
            return trial.getTraitNameStyle().makeTraitInstanceName(ti);
        }
    };

    @SuppressWarnings("rawtypes")
    private final MutableComboBoxModel fieldViewTraitInstanceComboBoxModel = new DefaultComboBoxModel();

    private final FieldLayoutTableModel fieldLayoutTableModel = new FieldLayoutTableModel();
    private static int UNIQUE_TABLE_ID = 0;

    private final CellSelectableTable fieldLayoutTable = new CellSelectableTable(
            "FieldLayoutTable-" + (++UNIQUE_TABLE_ID), //$NON-NLS-1$
            fieldLayoutTableModel,
            FieldLayoutView.Util.DEFAULT_RESIZE_ALL);

    private FieldViewSelectionModel fieldViewSelectionModel;

    private void ensureFieldViewSelectionModel() {
        if (fieldViewSelectionModel == null) {
            fieldViewSelectionModel = new FieldViewSelectionModel(
                    curationData,
                    fieldLayoutTable,
                    fieldLayoutTableModel,
                    selectedValueStore);
            fieldLayoutTable.setSelectionModel(fieldViewSelectionModel);
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureFieldViewComboBoxModel() {
        if (fieldViewTraitInstanceComboBoxModel.getSize() <= 0) {
            fieldViewTraitInstanceComboBoxModel.addElement(
                    TraitInstanceCellRenderer.TRAIT_TO_EDIT);
        }
    }

    private void doDisplayFieldOverview() {

        ensureFieldViewComboBoxModel();
        ensureFieldViewSelectionModel();

        Window window = GuiUtil.getOwnerWindow(TrialDataEditor.this);

        fieldLayoutView.openOverview(window,
                "Field Overview",
                showFieldViewButton,
                fieldViewTraitInstanceComboBoxModel,
                curationData,
                tiNameProvider,
                fieldLayoutTable,
                fieldViewSelectionModel,
                fieldLayoutTableModel,
                curationTableModel);
    }

    private final Closure<String> selectionClosure = new Closure<String>() {
        @Override
        public void execute(String toolId) {
            toolController.updateSelectedSamplesExceptFor(toolId);
            curationTableSelectionModel.updateSampleSelection();
        }
    };

    private void doDisplayFieldView() {

        ensureFieldViewComboBoxModel();
        ensureFieldViewSelectionModel();

        fieldLayoutView.openFieldLayoutView(
                TrialDataEditor.this,
                Msg.TITLE_FIELD_VIEW(),
                fieldViewTraitInstanceComboBoxModel,
                curationData,
                curationTableModel,
                selectedValueStore,
                plotCellChoicesPanel,
                null,
                smallFont,
                curationHelpAction,
                messages,
                selectionClosure,
                curationContext,
                curationMenuProvider,

        fieldLayoutTableModel,
                fieldLayoutTable,
                fieldViewSelectionModel,

        null // makeUndockButton()
        );

        fieldLayoutView.initialiseAfterOpening();
    }

    // private Pair<String,JComponent> getActiveTabComponent(boolean
    // forMenuItem) {
    //
    // int tabIndex = mainTabbedPane.getModel().getSelectedIndex();
    // String tabTitle = mainTabbedPane.getTitleAt(tabIndex);
    // Component c = mainTabbedPane.getComponentAt(tabIndex); //
    // trialViewTabbedPane.getTabComponentAt(tabIndex);
    //
    // if (c == fieldLayoutView.getPanel() || c==curationTablePanel) {
    // String title = tabTitle;
    // if (! forMenuItem) {
    // title = tabTitle + " - " + curationData.getTrial().getTrialName();
    // //$NON-NLS-1$
    // }
    // return new Pair<>(title, (JComponent) c);
    // }
    // return null;
    // }

    private void displayPopupMenu(MouseEvent me) {
        List<PlotOrSpecimen> plotSpecimens = new ArrayList<>();
        List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(curationTable);
        for (Integer modelRow : selectedModelRows) {
            PlotOrSpecimen pos = curationTableModel.getPlotOrSpecimenAtRowIndex(modelRow);
            if (pos != null) {
                plotSpecimens.add(pos);
            }
        }

        List<TraitInstance> selectedInstances = new ArrayList<>();

        int[] viewColumns = curationTable.getSelectedColumns();
        for (int vcol : viewColumns) {
            int mcol = curationTable.convertColumnIndexToModel(vcol);
            if (mcol >= 0) {
                TraitInstance ti = curationTableModel.getTraitInstanceAt(mcol);
                if (ti != null) {
                    selectedInstances.add(ti);
                }
            }
        }

        List<TraitInstance> checkedInstances = traitsAndInstancesPanel.getCheckedTraitInstances();

        curationMenuProvider.showSampleTableToolMenu(me,
                plotSpecimens,
                checkedInstances,
                selectedInstances);
    }

    // private JButton makeUndockButton() {
    // JButton btn = new JButton("\u25a3");
    // btn.addActionListener(undockViewAction);
    // btn.setFont(btn.getFont().deriveFont(Font.BOLD));
    // btn.setForeground(Color.BLUE);
    //
    // undockButtons.add(btn);
    // return btn;
    // }

    private void loadVisualisationTools() {

        List<VisualisationTool> list = new ArrayList<>();

        VisToolParams params = new VisToolParams(
                selectedValueStore,
                curationData,
                colorProviderFactory,
                suppressionHandler);

        Shared.Log.d(TAG, "Visualisation Plugins: "); //$NON-NLS-1$

        boolean useHardcoded = Boolean.getBoolean("USE_HARDCODED_VISTOOLS"); //$NON-NLS-1$
        if (useHardcoded) {
            // TODO remove hard coded VisualisationTool generation once dynamic
            // loading works

            String[] vtoolClassNames = {
                    "com.diversityarrays.kdxplore.heatmap.HeatMapVisualisationTool", //$NON-NLS-1$
                    "com.diversityarrays.kdxplore.boxplot.BoxPlotVisualisationTool", //$NON-NLS-1$
                    "com.diversityarrays.kdxplore.scatterplot.ScatterPlotVisualisationTool" //$NON-NLS-1$
            };

            for (String className : vtoolClassNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (VisualisationTool.class.isAssignableFrom(clazz)) {
                        @SuppressWarnings("unchecked")
                        Class<VisualisationTool> vtoolClass = (Class<VisualisationTool>) clazz;
                        Constructor<VisualisationTool> ctor = vtoolClass.getConstructor(VisToolParams.class);
                        VisualisationTool vtool = ctor.newInstance(params);
                        list.add(vtool);
                        Shared.Log.i(TAG, vtool.toString());
                    }
                    else {
                        Shared.Log.e(TAG, "VisualisationTool.class not assignable for instance of " + className);
                    }
                }
                catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                        | IllegalAccessException | InvocationTargetException e)
                {
                    Shared.Log.e(TAG, "Unable to get VisualisationTool for " + className, e);
                }
            }
        }
        else {

            Iterator<VisualisationToolService> it = ServiceRegistry
                    .lookupProviders(VisualisationToolService.class);
            while (it.hasNext()) {
                try {
                    VisualisationToolService vtoolService = it.next();
                    VisualisationTool vtool = vtoolService.createVisualisationTool(params);
                    list.add(vtool);
                    System.out.println(vtool.toString());
                }
                catch (java.util.ServiceConfigurationError error) {
                    Shared.Log.e(TAG, "VisualisationToolService error", error);
                    messages.printErrorLn("VisualisationToolService error: ", error);
                }
            }

        }

        visualisationTools = list.toArray(new VisualisationTool[list.size()]);

        toolController = new VisualisationToolController(selectedValueStore, visualisationTools);
        toolController.addSelectionChangeListener(visToolSelectionChangeListener);

        for (VisualisationTool tool : visualisationTools) {
            visualisationToolById.put(tool.getVisualisationToolId(), tool);
        }
    }

    private void updateUndoRedoActions() {
        undoAction.setEnabled(getUndoCount() > 0);
        redoAction.setEnabled(getRedoCount() > 0);
    }

    private int getUndoCount() {
        return changeManager.getUndoCount();
    }

    private int getRedoCount() {
        return changeManager.getRedoCount();
    }

    private void doSaveChanges() throws IOException {
        int saveCount = curationData.saveEditedSampleChanges();
        statusInfoLine.setMessage("Saved: " + saveCount);
    }

    private PlotsByTraitInstance getPlotsSelectedInTable() {
        int[] vcols = curationTable.getSelectedColumns();
        int[] vrows = curationTable.getSelectedRows();

        PlotsByTraitInstance result = new PlotsByTraitInstance();

        for (int c = 0; c < vcols.length; c++) {
            int mcol = curationTable.convertColumnIndexToModel(vcols[c]);
            if (mcol >= 0) {
                TraitInstance ti = curationTableModel.getTraitInstanceAt(mcol);
                List<PlotOrSpecimen> plots = new ArrayList<>();
                if (ti != null) {
                    for (int i = 0; i < vrows.length; i++) {
                        int mrow = curationTable.convertRowIndexToModel(vrows[i]);
                        if (mrow >= 0) {
                            PlotOrSpecimen plot = curationTableModel.getPlotOrSpecimenAtRowIndex(mrow);
                            if (plot != null) {
                                plots.add(plot);
                            }
                        }
                    }
                    result.addPlots(ti, plots);
                }
            }
        }
        return result;
    }

    private Consumer<List<OutlierSelection>> outlierConsumer = new Consumer<List<OutlierSelection>>() {
        @Override
        public void accept(List<OutlierSelection> t) {
            setOutliersSelected(t);
        }
    };

    private void setOutliersSelected(List<OutlierSelection> outlierSelections) {

        PlotsByTraitInstance plotsByTi = new PlotsByTraitInstance();

        for (OutlierSelection outlierSelection : outlierSelections) {

            if (! outlierSelection.traitInstance.trait.getTraitDataType().isNumeric()) {
                continue;
            }

            List<PlotOrSpecimen> plotSpecimens = new ArrayList<>();
            for (int i = 0; i < curationTableModel.getRowCount(); i++) {
                PlotOrSpecimen pos = curationTableModel.getPlotOrSpecimenAtRowIndex(i);
                if (pos != null) {
                    plotSpecimens.add(pos);
                }
            }

            List<PlotOrSpecimen> plotsToUse = new ArrayList<>();
            for (PlotOrSpecimen pos : plotSpecimens) {
                Sample sample = curationData.getSampleForTraitInstance(pos,
                        outlierSelection.traitInstance);

                if (sample != null && sample.hasBeenScored() && sample.getTraitValue() != null) {
                    try {
                        Double value = Double.parseDouble(sample.getTraitValue());
                        if (value >= outlierSelection.maxMin || value <= outlierSelection.minMax) {
                            plotsToUse.add(pos);
                        }
                    }
                    catch (NumberFormatException e) {
                        String name = curationData.getTrial().getTraitNameStyle()
                                .makeTraitInstanceName(outlierSelection.traitInstance, Prefix.EXCLUDE);
                        System.err.println(name + ": invalid sample value [" + sample.getTraitValue() + "]");
                    }
                }
            }

            plotsByTi.addPlots(outlierSelection.traitInstance, plotsToUse);
        }

        try {
            fieldLayoutView.clearSelection();

            selectedValueStore.setSelectedPlots("Stats", plotsByTi);
            toolController.updateSelectedSamplesExceptFor("Stats");
            curationTableSelectionModel.updateSampleSelection();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    curationTable.repaint();
                    fieldLayoutView.repaint();
                }
            });

            startEdit(HowStarted.SAMPLE_TABLE_SELECTION_CHANGED);
        }
        finally {
            cellSelectionBusy = null;
            Shared.Log.d(TAG,
                    "curationTableCellSelectionListener.handleChangeEvent: END "); //$NON-NLS-1$
        }
    }

    // TODO add checks for invalid values
    // TODO check for no SampleTypes
    private void checkForInvalidTraits() {
        SampleGroup db_smdata = curationData.getDatabaseSampleGroup();
        if (db_smdata == null) {
            return;
        }
        Bag<Integer> traitIdCountsFromDatabase = new HashBag<>();
        for (KdxSample sm : db_smdata.getSamples()) {
            traitIdCountsFromDatabase.add(sm.getTraitId());
        }

        Set<Integer> errorTraitIds = new HashSet<>(traitIdCountsFromDatabase.uniqueSet());

        Set<Integer> traitIds = CurationData.getTraitIds(curationData);
        errorTraitIds.removeAll(traitIds);

        if (!errorTraitIds.isEmpty()) {
            for (Integer id : errorTraitIds) {
                if (id == null) {
                    continue;
                }
                Trait trait = this.traitProvider.apply(id);
                if (trait == null) {
                    trait = new Trait();
                    trait.setTraitId(id);
                    trait.setTraitName("Unknown Trait#" + id);
                }
                errorTraitsById.put(id, trait);
            }
        }
    }

    private void collectSelectedPlotsAndColumns(List<PlotOrSpecimen> selectedPlots,
            Set<TraitInstance> selectedTraitInstances) {
        selectedPlots.clear();
        selectedTraitInstances.clear();

        Map<Integer, TraitInstance> viewColumnToTraitInstance = new HashMap<>();

        for (Integer mcol : curationTableModel.getTraitInstanceColumns()) {
            int vcol = curationTable.convertColumnIndexToView(mcol);
            if (vcol >= 0) {
                TraitInstance ti = curationTableModel.getTraitInstanceAt(mcol);
                if (ti != null) {
                    viewColumnToTraitInstance.put(vcol, ti);
                    Log.d(TAG, "collectSelectedPlotsAndColumns: " + vcol + " => " + ti); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        for (Integer modelRow : GuiUtil.getSelectedModelRows(curationTable)) {
            selectedPlots.add(curationTableModel.getPlotOrSpecimenAtRowIndex(modelRow));

            int viewRow = curationTable.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                for (Integer checkViewColumn : viewColumnToTraitInstance.keySet()) {
                    if (curationTable.isCellSelected(viewRow, checkViewColumn)) {
                        selectedTraitInstances.add(viewColumnToTraitInstance.get(checkViewColumn));
                        System.out.println("Selected: " + checkViewColumn + "," + viewRow); //$NON-NLS-2$
                    }
                }
            }
        }
    }

    private void updateViewedTraitInstances(ItemEvent e) {
        TraitInstance traitInstance = (TraitInstance) e.getItem();

        List<PlotOrSpecimen> selectedPlotSpecimens = new ArrayList<>();
        Set<TraitInstance> selectedTraitInstances = new HashSet<>();

        // works for no column movement, works for column movement
        collectSelectedPlotsAndColumns(selectedPlotSpecimens, selectedTraitInstances);

        Map<Integer, SortKey> sortKeyByColumn = new LinkedHashMap<>();
        Map<SortKey, String> cnameBySortKey = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        TableRowSorter<CurationTableModel> rowSorter = (TableRowSorter<CurationTableModel>) curationTable
                .getRowSorter();
        if (rowSorter != null) {
            // Make a copy because the one we get back is not modifiable!
            for (SortKey skey : rowSorter.getSortKeys()) {
                int columnIndex = skey.getColumn();

                sortKeyByColumn.put(columnIndex, skey);

                cnameBySortKey.put(skey, curationTableModel.getColumnName(columnIndex));
            }
        }

        // final Integer columnIndexForTraitInstance;

        boolean add = ItemEvent.SELECTED == e.getStateChange();
        if (add) {
            curationTableModel.addTraitInstance(traitInstance);
            plotCellChoicesPanel.addSelectedTraitInstance(traitInstance);

            fieldLayoutView.addTraitInstance(traitInstance);
            boolean found = false;
            for (int index = fieldViewTraitInstanceComboBoxModel.getSize(); --index >= 0;) {
                if (traitInstance == fieldViewTraitInstanceComboBoxModel.getElementAt(index)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                addToCombo(traitInstance);
            }

            Integer columnIndexForTraitInstance = curationTableModel
                    .getColumnIndexForTraitInstance(traitInstance);

            messages.println(
                    "Added " + curationTableModel.getColumnName(columnIndexForTraitInstance));
        }
        else {
            selectedTraitInstances.remove(traitInstance);

            Integer columnIndexForTraitInstance = curationTableModel
                    .getColumnIndexForTraitInstance(traitInstance);
            if (traitInstance.trait != null) {
                messages.println("Removed "
                        + traitInstance.trait.getTraitName()
                        + " [" + columnIndexForTraitInstance + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            curationTableModel.removeTraitInstance(traitInstance);
            plotCellChoicesPanel.removeSelectedTraitInstance(traitInstance);

            fieldLayoutView.removeTraitInstance(traitInstance);
            fieldViewTraitInstanceComboBoxModel.removeElement(traitInstance);

            SortKey removed = sortKeyByColumn.remove(columnIndexForTraitInstance);
            if (removed != null) {
                cnameBySortKey.remove(removed);
            }
        }

        if (rowSorter != null) {

            List<SortKey> validated = new ArrayList<>();

            int nColumns = curationTableModel.getColumnCount();

            for (Integer columnIndex : sortKeyByColumn.keySet()) {
                SortKey skey = sortKeyByColumn.get(columnIndex);
                if (columnIndex < nColumns) {
                    validated.add(skey);
                }
            }

            if (!cnameBySortKey.isEmpty()) {
                String msg = cnameBySortKey.keySet().stream()
                        .map(skey -> cnameBySortKey.get(skey) + ": " + skey.getSortOrder()) //$NON-NLS-1$
                        .collect(Collectors.joining("\n ", //$NON-NLS-1$
                                "Restoring sort order:",
                                "")); //$NON-NLS-1$
                messages.println(msg);
            }
            rowSorter.setSortKeys(validated);
        }

        if (!selectedPlotSpecimens.isEmpty()) {
            reapplySelection(selectedPlotSpecimens, selectedTraitInstances);
        }

        List<TraitInstance> checkedTraitInstances = null;
        if (curationTableModel.hasAnyTraitInstanceColumns()) {
            checkedTraitInstances = traitsAndInstancesPanel.getCheckedTraitInstances();
        }
        sampleSourcesTablePanel.updateTraitInstancesCombo(checkedTraitInstances);
    }

    @SuppressWarnings("unchecked")
    private void addToCombo(TraitInstance traitInstance) {
        fieldViewTraitInstanceComboBoxModel.addElement(traitInstance);
    }

    private void reapplySelection(
            List<PlotOrSpecimen> selectedPlotSpecimens,
            Set<TraitInstance> selectedTraitInstances)
    {
        boolean anySubPlot = false;
        List<Integer> selectedModelColumns = new ArrayList<>();
        for (TraitInstance ti : selectedTraitInstances) {
            if (TraitLevel.PLOT != ti.trait.getTraitLevel()) {
                anySubPlot = true;
            }
            Integer columnIndex = curationTableModel.getColumnIndexForTraitInstance(ti);
            if (columnIndex != null) {
                selectedModelColumns.add(columnIndex);
            }
        }

        boolean nonTraitColumns = false;
        if (selectedModelColumns.isEmpty()) {
            nonTraitColumns = true;
            selectedModelColumns.addAll(curationTableModel.getNonTraitInstanceColumns());
        }

        ListSelectionModel lsm = curationTable.getSelectionModel();

        try {
            curationTable.clearSelection();
            lsm.setValueIsAdjusting(true);

            // lsm.clearSelection(); above line already does this

            // For the curation table ...
            int countRowsFound = 0;

            if (anySubPlot) {
                // get all of the rows?
              Set<Integer> plotIds = selectedPlotSpecimens.stream()
                      .map(PlotOrSpecimen::getPlotId)
                      .collect(Collectors.toSet());
                for (Integer plotId : plotIds) {
                    Integer[] modelRows = curationTableModel.getRowsForPlotId(plotId, TraitLevel.PLOT);
                    for (Integer modelRow : modelRows) {
                        int viewRow = curationTable.convertRowIndexToView(modelRow);
                        if (viewRow >= 0) {
                          lsm.addSelectionInterval(viewRow, viewRow);
                        }
                    }
                }
            }
            else {
                for (PlotOrSpecimen pos : selectedPlotSpecimens) {
                    Optional<Integer> opt = curationTableModel.getRowForPlotOrSpecimen(pos);
                    if (opt.isPresent()) {
                        Integer modelRow = opt.get();
                        int viewRow = curationTable.convertRowIndexToView(modelRow);
                        if (viewRow >= 0) {
                            ++countRowsFound;
                            lsm.addSelectionInterval(viewRow, viewRow);
                        }
                    }
                }
            }
//            Set<Integer> plotIds = selectedPlotSpecimens.stream().map(PlotOrSpecimen::getPlotId)
//                    .collect(Collectors.toSet());
//            for (Integer plotId : plotIds) {
//                Integer[] modelRows = curationTableModel.getRowsForPlotId(plotId);
//                for (Integer modelRow : modelRows) {
//                    int viewRow = curationTable.convertRowIndexToView(modelRow);
//                    if (viewRow >= 0) {
//                        lsm.addSelectionInterval(viewRow, viewRow);
//                    }
//                }
//            }

            StringBuilder msg = new StringBuilder();
            if (!selectedModelColumns.isEmpty()) {
                List<Integer> sorted = new ArrayList<>(selectedModelColumns);
                Collections.sort(sorted);
                for (Integer mcol : sorted) {
                    int vcol = curationTable.convertColumnIndexToView(mcol);
                    if (vcol >= 0) {
                        curationTable.addColumnSelectionInterval(vcol, vcol);
                    }
                }

                if (! nonTraitColumns) {
                    if (countRowsFound <= 0) {
                        msg.append("\tSamples Table: selected ").append(selectedModelColumns.size())
                        .append(" Traits");
                    }
                    else {
                        msg.append("\tSamples Table: selected ").append(countRowsFound)
                            .append(" for ").append(selectedModelColumns.size())
                            .append(" Traits");
                    }
//                    msg.append("\tSamples Table: selected ").append(selectedModelColumns.size())
//                            .append(" Traits");
//                    if (!plotIds.isEmpty()) {
//                        msg.append(" in ").append(plotIds.size()).append(" Plots");
//                    }
                }
                else if (countRowsFound > 0) {
                    msg.append("\tSamples Table: selected ").append(countRowsFound);
                }
//                else if (!plotIds.isEmpty()) {
//                    msg.append("\tSamples Table: selected ").append(plotIds.size())
//                            .append(" Plots");
//                }
            }

//            if (!plotIds.isEmpty()) {
//                if (msg.length() > 0)
//                    msg.append('\n');
//                msg.append("\tField View: selected ").append(plotIds.size()).append(" Plots");
//            }

            if (msg.length() > 0) {
                messages.println(msg);
            }

            Set<Plot> plots = selectedPlotSpecimens.stream()
                    .map(pos -> curationData.getPlotByPlotId(pos.getPlotId()))
                    .collect(Collectors.toSet());
            fieldLayoutView.setSelectedPlots(new ArrayList<>(plots));
        }
        finally {
            lsm.setValueIsAdjusting(false);
            curationTable.repaint();
        }
    }

    private JTabbedPane valuesTabbedPane = new JTabbedPane();

    private Consumer<TraitInstance> handler = new Consumer<TraitInstance>() {

        @Override
        public void accept(TraitInstance selected) {
            List<CurationCellValue> result = new ArrayList<>();

            SelectedInfo selectedInfo = createSelectedInfoFromCurationTableModel(null);

            PlotsByTraitInstance plotsByTi = selectedInfo.plotsByTraitInstance;

            if (selected == null) {
                for (TraitInstance ti : curationTableModel.getTraitInstances()) {
                    for (PlotOrSpecimen pos : plotsByTi.getPlotSpecimens(ti)) {
                        result.add(curationData.getCurationCellValue(pos, ti));
                    }
                }
            }
            else {
                for (TraitInstance ti : curationTableModel.getTraitInstances()) {
                    if (selected == ti) {
                        for (PlotOrSpecimen pos : plotsByTi.getPlotSpecimens(ti)) {
                            result.add(curationData.getCurationCellValue(pos, ti));
                        }
                    }
                    else {
                        plotsByTi.remove(ti);
                    }
                }
            }

            if (!result.isEmpty()) {
                SampleGroup group = sampleSourcesTablePanel.getSelectedSampleGroup();
                if (group != null) {
                    curationCellEditor.setAllValuesToAccepted(result, group);
                }
            }
        }
    };

    /**
     * Show all of the
     */
    private final SampleSourcesTablePanel sampleSourcesTablePanel;

    private JComponent createCurationCellEditorComponent() {

        cardLayoutPanel.add(new JLabel(
                "<HTML>"
                        + "Select Values for a single Column in "
                        + "<b><i>" + SAMPLES_TABLE_NAME + "</i></b><br/>"
                        + "(after choosing <b><i>"
                        + "Trait Instances"
                        + "</i></b>)",
                SwingUtilities.CENTER),
                CARD_NO_CELL);

        cardLayoutPanel.add(new JLabel(
                "<HTML>Calculated Traits cannot be curated", SwingUtilities.CENTER),
                CARD_CALC);
        cardLayoutPanel.add(new JLabel(
                "<HTML>Trial is Complete<br>Editing is disabled</HTML>", SwingUtilities.CENTER),
                CARD_READ_ONLY);
        cardLayoutPanel.add(curationCellEditor.getComponent(),
                CARD_EDIT_CELL);

        valuesTabbedPane.addTab(TAB_TRAIT_VALUE, cardLayoutPanel);
        valuesTabbedPane.addTab(TAB_SAMPLE_DATA, sampleSourcesTablePanel);

        return valuesTabbedPane;
    }

    enum OnlyPlotOrSpecimen {
        ALL(TraitLevel.PLOT.visible + " & " + TraitLevel.SPECIMEN),
        ONLY_PLOTS("Only " + TraitLevel.PLOT),
        ONLY_SPECIMENS("Only " + TraitLevel.SPECIMEN),;
        public final String displayName;

        OnlyPlotOrSpecimen(String s) {
            displayName = s;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private boolean onlyRowsWithScores;
    private final JCheckBox onlyRowsWithScoresCheckbox = new JCheckBox("Only Scored",
            onlyRowsWithScores);

    private boolean onlyUncurated;
    private final JCheckBox onlyUneditedCheckbox = new JCheckBox("Un-curated", onlyUncurated);

    private OnlyPlotOrSpecimen onlyPlotOrSpecimen = OnlyPlotOrSpecimen.ALL;
    private final Map<JRadioButtonMenuItem, OnlyPlotOrSpecimen> oposByRb = new HashMap<>();
    private final ActionListener oposActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            onlyPlotOrSpecimen = oposByRb.get(e.getSource());

            updatePlotSpecimenIcon();

            updateRowFilter();
        }
    };
    private Action plotOrSpecimenFilterAction = new AbstractAction() {
        JPopupMenu popupMenu;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (popupMenu == null) {
                popupMenu = new JPopupMenu();
                ButtonGroup bg = new ButtonGroup();
                for (OnlyPlotOrSpecimen opos : OnlyPlotOrSpecimen.values()) {
                    JRadioButtonMenuItem mi = new JRadioButtonMenuItem(
                            opos.displayName, onlyPlotOrSpecimen == opos);
                    bg.add(mi);
                    mi.addActionListener(oposActionListener);
                    oposByRb.put(mi, opos);
                    popupMenu.add(mi);
                }
            }

            popupMenu.show(plotOrSpecimenFilterButton, 0, 0);
        }
    };
    private JButton plotOrSpecimenFilterButton = new JButton(plotOrSpecimenFilterAction);

    private final Set<String> selectedTagLabels = new HashSet<>();
    private final Consumer<Set<String>> onApplyTagLabelsSelected = new Consumer<Set<String>>() {
        @Override
        public void accept(Set<String> set) {
            if (set != null) {
                selectedTagLabels.clear();
                selectedTagLabels.addAll(set);
                tagLabelFiltersAction.putValue(Action.LARGE_ICON_KEY,
                        selectedTagLabels.isEmpty() ? inactiveTagFilterIcon : activeTagFilterIcon);
                updateRowFilter();
            }
        }
    };

    private Action tagLabelFiltersAction = new AbstractAction() {
        TagChoooser tagChooser;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (tagChooser == null) {
                tagChooser = new TagChoooser(curationData.getCountByTagLabel());
            }
            tagChooser.showAsPopup(tagLabelFiltersButton, selectedTagLabels, onApplyTagLabelsSelected);
        }
    };
    private final JButton tagLabelFiltersButton = new JButton(tagLabelFiltersAction);

    private boolean hideInactive = false;
    private final JCheckBox hideInactivePlots = new JCheckBox("Hide Inactive Plots", hideInactive);

    private final RowFilter<CurationTableModel, Integer> plotFilter = new RowFilter<CurationTableModel, Integer>() {
        @Override
        public boolean include(
                javax.swing.RowFilter.Entry<? extends CurationTableModel, ? extends Integer> entry) {
            int modelRow = entry.getIdentifier();

            CurationTableModel model = entry.getModel();

            boolean result = true;

            if (hideInactive) {
                Plot plot = model.getPlotAtRowIndex(modelRow);
                if (!plot.isActivated()) {
                    result = false;
                }
            }

            if (result && (OnlyPlotOrSpecimen.ALL != onlyPlotOrSpecimen)) {
                PlotOrSpecimen pos = model.getPlotOrSpecimenAtRowIndex(modelRow);
                if (pos != null) {
                    switch (onlyPlotOrSpecimen) {
                    case ALL:
                        break;
                    case ONLY_PLOTS:
                        if (!pos.isPlot()) {
                            result = false;
                        }
                        break;
                    case ONLY_SPECIMENS:
                        if (pos.isPlot()) {
                            result = false;
                        }
                        break;
                    default:
                        break;
                    }
                }
            }

            if (result && onlyRowsWithScores) {
                if (!model.hasAnyScores(modelRow)) {
                    result = false;
                }
            }

            if (result && onlyUncurated) {
                if (!model.hasAnyUncurated(modelRow)) {
                    result = false;
                }
            }

            if (result && !selectedTagLabels.isEmpty()) {
                Plot plot = model.getPlotAtRowIndex(modelRow);
                if (plot != null) {
                    Optional<Tag> anyMatches = plot.getTagsBySampleGroup().values()
                            .stream()
                            .flatMap(l -> l.stream())
                            .filter(tag -> selectedTagLabels.contains(tag.getLabel()))
                            .findFirst();
                    if (!anyMatches.isPresent()) {
                        result = false;
                    }
                }
            }

            return result;
        }
    };

    private final ActionListener filtersActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateRowFilter();
        }
    };

    private final PropertyChangeListener traitInstancesChanged = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            onlyRowsWithScoresCheckbox.setEnabled(curationTableModel.hasAnyTraitInstanceColumns());
            updateRowFilter();
        }
    };

    private TitledTablePanelWithResizeControls curationTablePanel;

    private Action curationHelpAction = new AbstractAction("Help") {

        private JFrame legendFrame;

        private WindowListener windowListener = new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                legendFrame.removeWindowListener(this);
                legendFrame = null;
            }
        };

        private Action closeAction = new AbstractAction("X") {
            @Override
            public void actionPerformed(ActionEvent e) {
                legendFrame.dispose();
            }
        };

        @Override
        public void actionPerformed(ActionEvent e) {

            if (legendFrame != null) {
                legendFrame.setVisible(true);
                legendFrame.toFront();
            }
            else {
                legendFrame = new JFrame("Quick Help");
                legendFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                legendFrame.setAlwaysOnTop(true);

                JLabel label = new JLabel(getEditingHelp());
                label.setBorder(new EmptyBorder(2, 2, 2, 2));
                legendFrame.getContentPane().add(new JScrollPane(label), BorderLayout.CENTER);
                Box box = Box.createHorizontalBox();
                box.add(new JButton(closeAction));
                box.add(Box.createHorizontalGlue());
                legendFrame.add(box, BorderLayout.SOUTH);

                legendFrame.pack();

                legendFrame.addWindowListener(windowListener);

                GuiUtil.centreOnOwner(legendFrame);
                Object src = e.getSource();
                if (src instanceof Component) {
                    legendFrame.setLocationRelativeTo((Component) src);
                }
                legendFrame.setVisible(true);
            }
        }
    };

    private final ImageIcon inactivePlotOrSpecimenFilterIcon;
    private final ImageIcon activePlotFilterIcon;
    private final ImageIcon activeSpecimenFilterIcon;

    private final ImageIcon inactiveTagFilterIcon;
    private final ImageIcon activeTagFilterIcon;

    private void updateRowFilter() {

        @SuppressWarnings("unchecked")
        TableRowSorter<CurationTableModel> rowSorter = (TableRowSorter<CurationTableModel>) curationTable
                .getRowSorter();

        onlyRowsWithScores = onlyRowsWithScoresCheckbox.isSelected();

        boolean onlyUneditedBefore = onlyUncurated;
        onlyUncurated = onlyUneditedCheckbox.isSelected();

        @SuppressWarnings("unused")
        boolean hideInactiveBefore = hideInactive;
        hideInactive = hideInactivePlots.isSelected();

        JLabel label = curationTablePanel.separator.getLabel();
        String msg;
        if (isAnyFilterActive()) {
            rowSorter.setRowFilter(plotFilter);
            label.setForeground(Color.RED);
            msg = PHRASE_SHOWING + curationTable.getRowCount() + " of "
                    + curationTableModel.getRowCount();
        }
        else {
            rowSorter.setRowFilter(null);
            label.setForeground(Color.DARK_GRAY);

            msg = PHRASE_SHOWING_ALL + +curationTable.getRowCount();
        }

        curationTable.setRowSorter(rowSorter);

        if (onlyUneditedBefore != onlyUncurated) {
            curationTableModel.setUsingOnlyUnedited(onlyUncurated);
        }

        // Allow filter to be applied before updating
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                label.setToolTipText(msg);
                label.setText(msg);
            }
        });
    }

    private boolean isAnyFilterActive() {
        return onlyRowsWithScores
                || onlyUncurated
                || hideInactive
                || !selectedTagLabels.isEmpty()
                || OnlyPlotOrSpecimen.ALL != onlyPlotOrSpecimen;
    }

    private void updatePlotSpecimenIcon() {
        ImageIcon icon = null;
        switch (onlyPlotOrSpecimen) {
        case ALL:
            icon = inactivePlotOrSpecimenFilterIcon;
            break;
        case ONLY_PLOTS:
            icon = activePlotFilterIcon;
            break;
        case ONLY_SPECIMENS:
            icon = activeSpecimenFilterIcon;
            break;
        default:
            break;
        }
        if (icon != null) {
            plotOrSpecimenFilterAction.putValue(Action.LARGE_ICON_KEY, icon);
        }
    }

    protected String getEditingHelp() {
        StringBuilder sb = new StringBuilder("<HTML>"); //$NON-NLS-1$

        sb.append(Msg.HTML_EDITING_HELP(
                StringUtil.htmlEscape(tAndIpanelLabel)));
        return sb.toString();
    }

    private JSplitPane createStatsAndSamplesTable(JComponent top) {

        curationTableModel.addPropertyChangeListener(CurationTableModel.PROPERTY_TRAIT_INSTANCES,
                traitInstancesChanged);

        onlyUneditedCheckbox.addActionListener(filtersActionListener);

        hideInactivePlots.addActionListener(filtersActionListener);

        if (onlyRowsWithScoresCheckbox != null) {
            onlyRowsWithScoresCheckbox.addActionListener(filtersActionListener);
            onlyRowsWithScoresCheckbox.setEnabled(curationTableModel.hasAnyTraitInstanceColumns());
        }

        JComponent tagFilters = null;
        if (!curationData.getSortedTagLabels().isEmpty()) {
            tagFilters = tagLabelFiltersButton;
        }
        JComponent posFilters = null;
        if (curationData.hasAnySpecimens()) {
            updatePlotSpecimenIcon();
            posFilters = plotOrSpecimenFilterButton;
        }

        plotOrSpecimenFilterAction.putValue(Action.LARGE_ICON_KEY, inactivePlotOrSpecimenFilterIcon);
        plotOrSpecimenFilterAction.putValue(Action.SHORT_DESCRIPTION, "Filter on Plot/Specimen");

        tagLabelFiltersAction.putValue(Action.LARGE_ICON_KEY, inactiveTagFilterIcon);
        tagLabelFiltersAction.putValue(Action.SHORT_DESCRIPTION, "Filter on Tags");

        curationTablePanel = new TitledTablePanelWithResizeControls(SAMPLES_TABLE_NAME,
                curationTable,
                smallFont,
                hideInactivePlots, onlyUneditedCheckbox, onlyRowsWithScoresCheckbox,
                tagFilters, posFilters,
                new JButton(curationHelpAction));

        // curationTablePanel.scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER,
        // makeUndockButton());

        Transformer<TraitInstance, Color> traitInstanceColorProvider = new Transformer<TraitInstance, Color>() {
            @Override
            public Color transform(TraitInstance ti) {
                ColorPair cp = colorProviderFactory.get().getTraitInstanceColor(ti);
                return cp.getBackground();
            }
        };
        markerPanelManager = new MarkerPanelManager(
                curationData.getTrial(),
                curationTableModel,
                curationTable,
                curationTablePanel,
                traitInstanceColorProvider,
                curationTableSelectionModel);

        // fieldViewIcon = KDClientUtils.getIcon(ImageId.FIELD_VIEW_24);
        samplesTableIcon = KDClientUtils.getIcon(ImageId.TABLE_24);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, curationTablePanel);
        splitPane.setResizeWeight(0.3);
        splitPane.setOneTouchExpandable(true);

        return splitPane;
    }

    // private JComponent createFieldViewAndPlotsTable() {
    //
    // curationTableModel.addPropertyChangeListener(CurationTableModel.PROPERTY_TRAIT_INSTANCES,
    // traitInstancesChanged);
    //
    // onlyUneditedCheckbox.addActionListener(filtersActionListener);
    //
    // hideInactivePlots.addActionListener(filtersActionListener);
    //
    // if (onlyRowsWithScoresCheckbox != null) {
    // onlyRowsWithScoresCheckbox.addActionListener(filtersActionListener);
    // onlyRowsWithScoresCheckbox.setEnabled(curationTableModel.hasAnyTraitInstanceColumns());
    // }
    //
    // JComponent tmp = onlyRowsWithScoresCheckbox==null ? new JLabel("_") :
    // onlyRowsWithScoresCheckbox;
    // curationTablePanel = new
    // TitledTablePanelWithResizeControls(SAMPLES_TABLE_NAME,
    // curationTable,
    // smallFont,
    // hideInactivePlots, onlyUneditedCheckbox, tmp,
    // new JButton(curationHelpAction));
    //
    //// curationTablePanel.scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER,
    // makeUndockButton());
    //
    // Transformer<TraitInstance,Color> traitInstanceColorProvider = new
    // Transformer<TraitInstance,Color>() {
    // @Override
    // public Color transform(TraitInstance ti) {
    // ColorPair cp = colorProviderFactory.create().getTraitInstanceColor(ti);
    // return cp.getBackground();
    // }
    // };
    // markerPanelManager = new MarkerPanelManager(
    // curationData.getTrial(),
    // curationTableModel,
    // curationTable,
    // curationTablePanel,
    // traitInstanceColorProvider,
    // curationTableSelectionModel);
    //
    // fieldViewIcon = KDClientUtils.getIcon(ImageId.FIELD_LAYOUT_24);
    // samplesTableIcon = KDClientUtils.getIcon(ImageId.TABLE_24);
    //
    //// trialViewTabbedPane.insertTab(TAB_FIELD, fieldViewIcon,
    // fieldLayoutViewPanel,
    //// TOOLTIP_FIELD_VIEW, TAB_INDEX_FIELD_VIEW);
    //
    // mainTabbedPane.insertTab(TAB_SAMPLES, samplesTableIcon,
    // curationTablePanel,
    // TOOLTIP_SAMPLES_TABLE, 0); // TAB_INDEX_SAMPLES_TABLE);
    //
    //
    // return mainTabbedPane;
    // }

    @SuppressWarnings("unused")
    private MarkerPanelManager markerPanelManager;

    private String buildDatabaseTraitsWarning() {
        List<Trait> traits = new ArrayList<>(errorTraitsById.values());
        Collections.sort(traits, new Comparator<Trait>() {
            @Override
            public int compare(Trait o1, Trait o2) {
                return o1.getTraitName().toLowerCase().compareTo(o2.getTraitName().toLowerCase());
            }
        });

        StringBuilder sb = new StringBuilder(
                "Traits in Database Samples that are not in the Trial:");
        for (Trait trait : traits) {
            sb.append("\nID=").append(trait.getTraitId()).append(": ").append(trait.getTraitName());
        }

        return sb.toString();
    }

    @Override
    public JPanel getJPanel() {
        return this;
    }

    @Override
    public String getTitle() {
        return "Curating: " + curationData.getTrial().getTrialName();
    }

    private Action showWarnings = new AbstractAction("Show Warnings") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StringBuilder msg = new StringBuilder();
            if (!missingTraitIds.isEmpty()) {
                msg.append(buildMissingTraitsMessage());
            }
            if (!errorTraitsById.isEmpty()) {
                if (!missingTraitIds.isEmpty()) {
                    msg.append("\n-----");
                }
                msg.append(buildDatabaseTraitsWarning());
            }
            GuiUtil.infoMessage(TrialDataEditor.this, msg, getTitle());
        }
    };

    private JMenu viewMenu = new JMenu(Msg.MENU_VIEW());

    private JMenu fileMenu = new JMenu(Msg.MENU_FILE());

    private JMenu editMenu = new JMenu(Msg.MENU_EDIT());

    private final Action displayOverviewAction = new AbstractAction(Msg.ACTION_OVERVIEW()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!RunMode.getRunMode().isDeveloper()) {
                MsgBox.info(TrialDataEditor.this, "Temporarily Disabled", "Show Overview");
                return;
            }
            doDisplayFieldOverview();
        }
    };

    private final Action displayFieldViewAction = new AbstractAction(Msg.ACTION_FIELD_VIEW()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            doDisplayFieldView();
        }
    };

    private JPopupMenu fieldViewPopup;
    private Action showFieldViewAction = new AbstractAction(Msg.TITLE_FIELD_VIEW()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (fieldViewPopup == null) {
                displayOverviewAction.setEnabled(RunMode.getRunMode().isDeveloper());
                fieldViewPopup = new JPopupMenu("Choose Field View");
                fieldViewPopup.add(new JMenuItem(displayOverviewAction));
                fieldViewPopup.add(new JMenuItem(displayFieldViewAction));
            }

            int x = 0;
            int y = 0;
            fieldViewPopup.show(showFieldViewButton, x, y);
        }
    };
    private final JButton showFieldViewButton = new JButton(showFieldViewAction);

    private Action undoAction = new AbstractAction("Undo") {
        @Override
        public void actionPerformed(ActionEvent e) {
            String msg = curationData.undoChanges();
            messages.println(msg);

            startEdit(HowStarted.UNDO);
        }

    };

    private Action redoAction = new AbstractAction("Redo") {
        @Override
        public void actionPerformed(ActionEvent e) {
            String msg = curationData.redoChanges();
            messages.println(msg);

            startEdit(HowStarted.REDO);
        }
    };

    private Action exportCuratedData = new AbstractAction("Export ...") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doExportCuratedData();
        }
    };
    private Action importCuratedData = new AbstractAction("Import Data...") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doDisabledForTesting("Import Curated Data");
        }
    };

    private Action saveChangesAction = new AbstractAction("Save Changes") {
        @Override
        public void actionPerformed(ActionEvent ae) {
            try {
                doSaveChanges();
                saveChangesAction.setEnabled(false);
            }
            catch (IOException e) {
                messages.println("Problem during save:");
                messages.println(e);
                JOptionPane.showMessageDialog(TrialDataEditor.this,
                        "Database Error: " + e.getMessage(),
                        "Save Changes",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    };

    private Map<Integer, Trait> errorTraitsById = new HashMap<>();

    private final MessagesPanel messages = new MessagesPanel("Curation Messages", true);

    private JMenu windowsMenu = new JMenu("Graphs & Plots") {

        @Override
        public JMenuItem add(JMenuItem menuItem) {
            // TODO Auto-generated method stub
            return super.add(menuItem);
        }

        @Override
        public JMenuItem add(Action a) {
            // TODO Auto-generated method stub
            return super.add(a);
        }

    };

    private JMenu problemMenu = new JMenu(PROBLEM);

    private WindowsMenuManager windowsMenuManager = new WindowsMenuManager(windowsMenu);

    private WindowListener plotTitleUncounter = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
            Window w = e.getWindow();
            w.removeWindowListener(this);

            if (w instanceof JFrame) {
                JFrame f = (JFrame) w;
                String title = f.getTitle();
                if (title != null) {
                    int pos = title.lastIndexOf(':');
                    if (pos > 0) {
                        title = title.substring(0, pos);
                    }
                    plotTitleCounts.remove(title);
                }
                toolFrames.remove(f);
                refreshToolAll();
            }
        }
    };
    private Bag<String> plotTitleCounts = new HashBag<>();

    private List<JFrame> toolFrames = new ArrayList<>();

    private void refreshToolAll() {
        if (toolFrames.size() == 0) {
            showAll.setEnabled(false);
            closeAll.setEnabled(false);
        }
        else {
            showAll.setEnabled(true);
            closeAll.setEnabled(true);
        }
    }

    private void doExportCuratedData() {
        int selectedTraitInstanceCount = traitsAndInstancesPanel.getCheckedTraitInstances().size();

        int selectedPlotAttributeCount = plotCellChoicesPanel.getPlotAttributes(false).size();

        ExportWhatDialog dlg = new ExportWhatDialog(GuiUtil.getOwnerWindow(TrialDataEditor.this),
                "Export Curated Data",
                curationData.getTrial(),
                curationTableModel.getRowCount(),
                GuiUtil.getSelectedModelRows(curationTable),
                selectedTraitInstanceCount,
                selectedPlotAttributeCount);
        dlg.setVisible(true);

        if (dlg.exportOptions != null) {
            CurationExportHelper helper = new CurationExportHelper() {
                @Override
                public boolean hasPlotType(boolean bmsMode) {
                    if (bmsMode) {
                        // Independent of what the user has chosen
                        return curationData.getHasPlotType();
                    }
                    return curationTableModel.hasPlotType();
                }

                @Override
                public List<PlotAttribute> getPlotAttributes(boolean allElseSelected) {
                    return plotCellChoicesPanel.getPlotAttributes(allElseSelected);
                }

                @Override
                public List<TraitInstance> getTraitInstances(WhichTraitInstances which) {
                    return plotCellChoicesPanel.getTraitInstances(which);
                }

                @Override
                public Plot getPlotByRowIndex(int rowIndex) {
                    return curationTableModel.getPlotAtRowIndex(rowIndex);
                }

                @Override
                public Sample getSampleForPlotAndTraitInstance(PlotOrSpecimen pos,
                        TraitInstance ti) {
                    return curationData.getSampleForTraitInstance(pos, ti);
                }

                @Override
                public Trait getTrait(int traitId) {
                    return traitProvider.apply(traitId);
                }

                @Override
                public List<KdxSample> getSampleMeasurements(TraitInstance ti) {
                    return curationData.getSampleMeasurements(ti);
                }
            };

            CurationDataExporter exporter = new CurationDataExporter(
                    curationData.getTrial(),
                    curationData.getTrialAttributes(),
                    helper,
                    TrialDataEditor.this,
                    messages);
            exporter.exportCurationData(dlg.exportOptions);
        }
    }

    private JFrame addGraphOrPlotPanel(DesktopObject dobj) {
        JFrame frame = windowOpener.addDesktopObject(dobj);
        JMenuBar mbar = new JMenuBar();
        frame.setJMenuBar(getJMenuBar());
        toolFrames.add(frame);

        frame.addWindowListener(plotTitleUncounter);

        Dimension size = this.getSize();
        size.width = Math.max(size.width / 2, 640);
        size.height = Math.max(size.height / 2, 640);
        frame.setSize(size);
        frame.setLocationRelativeTo(null);

        windowsMenuManager.addWindow(frame, dobj.getTitle());
        refreshToolAll();
        return frame;
    }

    private AbstractAction showAll = new AbstractAction("Show All") {
        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (JFrame frame : toolFrames) {
                        frame.toFront();
                        frame.repaint();
                    }
                }
            });
        }
    };

    boolean justCloseThem = true;
    private AbstractAction closeAll = new AbstractAction("Close All") {
        @Override
        public void actionPerformed(ActionEvent e) {

            if (justCloseThem) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (JFrame frame : toolFrames) {
                            frame.dispose();
                        }
                    }
                });
                return;
            }

            JDialog dlg = new JDialog(GuiUtil.getOwnerWindow(TrialDataEditor.this),
                    "Close All Graphs/Plots",
                    ModalityType.APPLICATION_MODAL);

            dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dlg.setAlwaysOnTop(true);

            Action yes = new AbstractAction("Yes") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dlg.dispose();
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (JFrame frame : toolFrames) {
                                frame.dispose();
                            }
                        }
                    });
                }
            };

            List<JFrame> framesToRestore = new ArrayList<>();
            for (JFrame frame : toolFrames) {
                int state = frame.getState();
                if (JFrame.NORMAL == state) {
                    framesToRestore.add(frame);
                    frame.setState(JFrame.ICONIFIED);
                }
            }

            Action no = new AbstractAction("No") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dlg.dispose();
                    for (JFrame frame : framesToRestore) {
                        frame.setState(JFrame.NORMAL);
                    }
                }
            };

            JButton yesButton = new JButton(yes);
            JButton noButton = new JButton(no);

            JOptionPane optionPane = new JOptionPane(
                    "Do you really want to close all Graphs/Plots?",
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null,
                    new JButton[] { yesButton, noButton }, yesButton);

            Container cp = dlg.getContentPane();
            cp.add(optionPane, BorderLayout.CENTER);
            dlg.pack();
            dlg.setLocationRelativeTo(null);

            dlg.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    dlg.removeWindowListener(this);
                    dlg.toFront();
                }
            });
            dlg.setVisible(true);

        }
    };


    // private JMenuItem undockMenuItem;

    private JMenuBar menuBar;
    @Override
    public JMenuBar getJMenuBar() {

        if (menuBar == null) {

            JMenuItem menuItem;
    //        JMenu windowsMenu = new JMenu("Graphs & Plots");
            showAll.setEnabled(false);
            menuItem = windowsMenu.add(showAll);
            menuItem.setMnemonic('B');
            menuItem.setAccelerator(
                    KeyStroke.getKeyStroke('B', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            closeAll.setEnabled(false);
            windowsMenu.add(closeAll);
            windowsMenu.addSeparator();

            JMenu fileMenu = new JMenu(Msg.MENU_FILE());
            menuItem = fileMenu.add(saveChangesAction);
            menuItem.setMnemonic('S');
            menuItem.setAccelerator(
                    KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            menuItem = fileMenu.add(exportCuratedData);
            // menuItem.setMnemonic('T');
            // menuItem.setAccelerator(KeyStroke.getKeyStroke('T',
            // Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
            fileMenu.addSeparator();

            fileMenu.add(importCuratedData);

            importCuratedData.setEnabled(true);

            JMenu editMenu = new JMenu(Msg.MENU_EDIT());
            menuItem = editMenu.add(undoAction);
            menuItem.setMnemonic('Z');
            menuItem.setAccelerator(
                    KeyStroke.getKeyStroke('Z', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            menuItem = editMenu.add(redoAction);
            menuItem.setMnemonic('Y');
            menuItem.setAccelerator(
                    KeyStroke.getKeyStroke('Y', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            // undockMenuItem = viewMenu.add(undockViewAction);

            showWarnings.setEnabled(false);
            JMenu viewMenu = new JMenu(Msg.MENU_VIEW());
            viewMenu.add(showWarnings);

            problemMenu.add(new ReportIssueAction(this));

            menuBar = new JMenuBar();
            menuBar.add(fileMenu);
            menuBar.add(editMenu);
            menuBar.add(viewMenu);
            menuBar.add(windowsMenu);
            menuBar.add(problemMenu);
        }

        return menuBar;
    }

    private void doDisabledForTesting(String toolTitle) {
        VisualisationToolActionListener.doDisabledForTesting(TrialDataEditor.this, getTitle(),
                toolTitle);
    }

    private JToolBar toolBar = null;

    private JButton makeToolbarButton(Action action) {
        JButton btn = new JButton(action);
        if (btn.getIcon() != null) {
            btn.setText(null);
        }
        return btn;
    }

    @Override
    public JToolBar getJToolBar() {

        if (toolBar == null) {

            JToolBar tbar = new JToolBar("Curation Toolbar");
            tbar.add(makeToolbarButton(saveChangesAction));
            tbar.add(makeToolbarButton(exportCuratedData));

            tbar.addSeparator();

            tbar.add(makeToolbarButton(undoAction));
            tbar.add(makeToolbarButton(redoAction));

            tbar.addSeparator();

            for (JButton button : createVisualisationButtons()) {
                button.setText(null);
                tbar.add(button);
            }

            tbar.addSeparator();

            tbar.add(showFieldViewButton);

            tbar.add(Box.createGlue());
            toolBar = tbar;
        }

        return toolBar;
    }

    private List<JButton> createVisualisationButtons() {

        List<JButton> visButtons = new ArrayList<>();

        for (final VisualisationTool tool : visualisationTools) {

            JButton button = new JButton();
            button.setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2),
                    BorderFactory.createBevelBorder(BevelBorder.RAISED)));
            Icon icon = tool.getToolIcon();
            if (icon != null) {
                button.setIcon(icon);
            }
            else {
                button.setName(tool.getToolButtonName());
            }

            boolean onlyForDeveloper = false;

            VisualisationToolActionListener actionListener = new VisualisationToolActionListener(
                    TrialDataEditor.this,
                    getTitle(),
                    onlyForDeveloper,
                    curationContext,
                    tool);

            button.addActionListener(actionListener);
            visButtons.add(button);
            button.setText(tool.getToolName());
        }
        return visButtons;
    }

    private VisualisationToolController toolController;

    private SelectionChangeListener visToolSelectionChangeListener = new SelectionChangeListener() {
        @Override
        public void selectionChanged(String toolInstanceId) {
            handleVisToolSelectionChanged(toolInstanceId);
            startEdit(HowStarted.VISTOOL_SELECTION);
        }
    };

    /**
     * We want the Samples that were selected in the VisualisationTool.
     *
     * @param toolId
     * @param toolInstanceId
     */
    private void handleVisToolSelectionChanged(String toolInstanceId) {

        curationTable.clearSelection();
        fieldLayoutView.clearSelection();

        toolController.updateSelectedSamplesExceptFor(toolInstanceId);

        curationTableSelectionModel.updateSampleSelection();
        fieldLayoutView.updateSelectedMeasurements("TrialDataEditor.updateSampleSelectionExceptFor("
                + toolInstanceId + ")");
        if (SwingUtilities.isEventDispatchThread()) {
            curationTable.repaint();
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    curationTable.repaint();
                }
            });
        }
    }

    private CurationContext curationContext = new CurationContext() {

        private Window dialogOwnerWindow;

        @Override
        public Trial getTrial() {
            return curationData.getTrial();
        }

        @Override
        public PlotInfoProvider getPlotInfoProvider() {
            return curationData;
        }

        @Override
        public TraitValue getTraitValue(TraitInstance traitInstance, PlotOrSpecimen pos) {
            TraitValue result = null;

            TraitInstanceValueRetriever<?> tivr = tivrByTi.get(traitInstance);
            if (tivr != null) {
                result = tivr.getAttributeValue(curationData, pos, null);
            }

            return result;
        }

        @Override
        public Map<PlotAttribute, Set<String>> getPlotAttributesAndValues() {
            return curationData.getPlotAttributesAndValues();
        }

        @Override
        public IntFunction<Trait> getTraitProvider() {
            return traitProvider;
        }

        @Override
        public Window getDialogOwnerWindow() {
            if (dialogOwnerWindow == null) {
                dialogOwnerWindow = GuiUtil.getOwnerWindow(TrialDataEditor.this);
            }
            return dialogOwnerWindow;
        }

        @Override
        public JFrame addVisualisationToolUI(DesktopObject dobj) {
            return addGraphOrPlotPanel(dobj);
        }

        @Override
        public void errorMessage(VisualisationTool tool, String message) {
            GuiUtil.errorMessage(getDialogOwnerWindow(), message, tool.getToolName());
        }

        @Override
        public Map<Plot,Map<Integer,KdxSample>> getPlotSampleMeasurements(TraitInstance traitInstance, Predicate<Plot> plotFilter)
        {
            return curationTableModel.getEditStateSamplesByPlot(traitInstance, plotFilter);
        }

        @Override
        public Map<PlotOrSpecimen, KdxSample> getSampleMeasurements(TraitInstance traitInstance,
                Predicate<Plot> plotFilter) {
            return curationTableModel.getEditStateSamplesByPlotOrSpecimen(traitInstance, plotFilter);
        }

        @Override
        public List<CurationCellValue> getCurationCellValuesForPlot(PlotOrSpecimen pos) {
            return curationTableModel.getCurationCellValuesForPlot(pos.getPlotId());
        }

        @Override
        public Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance() {
            return plotCellChoicesPanel.getStatsByTraitInstance();
            // return traitsAndInstancesPanel.getStatsByTraitInstance();
        }

//        @Override
//        public String getTraitInstanceName(TraitInstance traitInstance) {
//            Trait trait = traitProvider.apply(traitInstance.getTraitId());
//            TraitNameStyle traitNameStyle = curationData.getTrial().getTraitNameStyle();
//            return traitNameStyle.makeTraitInstanceName(traitInstance, trait);
//        }

        @Override
        public TraitColorProvider getTraitColorProvider() {
            return curationData.getTraitColorProvider();
        }

        @Override
        public String makeTraitInstanceName(TraitInstance ti) {
            return getTrial().getTraitNameStyle()
                    .makeTraitInstanceName(ti,
                            showAliasForTraits,
                            TraitNameStyle.Prefix.select(showTraitLevelPrefix));
        }

        boolean showAliasForTraits;
        @Override
        public boolean getShowAliasForTraits() {
            return showAliasForTraits;
        }
        @Override
        public void setShowAliasForTraits(boolean b) {
            boolean old = showAliasForTraits;
            showAliasForTraits = b;
            support.firePropertyChange(PROP_SHOW_ALIAS, old, showAliasForTraits);
        }

        boolean showTraitLevelPrefix = true;
        @Override
        public boolean getShowTraitLevelPrefix() {
            return showTraitLevelPrefix;
        }
        @Override
        public void setShowTraitLevelPrefix(boolean b) {
            boolean old = showTraitLevelPrefix;
            showTraitLevelPrefix = b;
            support.firePropertyChange(PROP_SHOW_TRAIT_LEVEL, old, showTraitLevelPrefix);
        }

        PropertyChangeSupport support = new PropertyChangeSupport(this);
        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }
        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener(listener);
        }

    };

    @SuppressWarnings("rawtypes")
    private Map<VisualisationToolId, VisualisationTool> visualisationToolById = new LinkedHashMap<>();

    private VisualisationTool[] visualisationTools;

    static class SampleMeasurementComparator implements Comparator<KdxSample> {

        Map<String, Integer> viewRowByPosIdent;

        SampleMeasurementComparator(Map<String, Integer> viewRowByPosIdent) {
            this.viewRowByPosIdent = viewRowByPosIdent;
        }

        @Override
        public int compare(KdxSample sm1, KdxSample sm2) {
            Integer vr1 = sm1 == null
                    ? null
                    : viewRowByPosIdent.get(InstanceIdentifierUtil.getPlotSpecimenIdentifier(sm1));
            Integer vr2 = sm2 == null
                    ? null
                    : viewRowByPosIdent.get(InstanceIdentifierUtil.getPlotSpecimenIdentifier(sm2));

            if (vr1 == null) {
                return (vr2 == null) ? 0 : -1;
            }

            if (vr2 == null) {
                return 1;
            }
            return vr1.compareTo(vr2);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static private Comparable minimumOf(Comparable a, Comparable b) {
        Comparable result = a;
        if (a == null) {
            if (b != null) {
                result = b;
            }
        }
        else if (b != null && b.compareTo(a) < 0) {
            result = b;
        }
        return result;
    }

    /**
     * Returns into (x,y)OutputValues and the pair of minima for x,y as the
     * result.
     *
     * @param xSamples
     *            input samples for X
     * @param xValueFactory
     *            value generator for X
     * @param ySamples
     *            input samples for Y
     * @param yValueFactory
     *            value generator for Y
     * @param xOutputValues
     * @param yOutputValues
     * @return an array of the minimums
     */
    // TODO use this for plotting data
    private Comparable<?>[] collectSampleValuePairsByCurationTableVisibleRowOrder(
            List<KdxSample> xSamples, ValueFactory<?> xValueFactory,
            List<KdxSample> ySamples, ValueFactory<?> yValueFactory,
            List<Comparable<?>> xOutputValues, List<Comparable<?>> yOutputValues) {

        // The sort order is by the current order of the PlotOrSpecimen-s in the curationTableModel

        // Need to sort by Plot#Specimen

        final Map<String, Integer> viewRowByPosIdent = new HashMap<>();
        for (int vrow = curationTable.getRowCount(); --vrow >= 0;) {
            int mrow = curationTable.convertRowIndexToModel(vrow);
            if (mrow >= 0) {
                PlotOrSpecimen pos = curationTableModel.getPlotOrSpecimenAtRowIndex(mrow);
                String posIdent = InstanceIdentifierUtil.getPlotSpecimenIdentifier(pos);
                viewRowByPosIdent.put(posIdent, vrow);
            }
        }

        Comparator<KdxSample> comparator = new SampleMeasurementComparator(viewRowByPosIdent);
        // First we get both lists in the order specified by the
        // curationTableModel's sort order.
        Collections.sort(xSamples, comparator);
        Collections.sort(ySamples, comparator);

        // List<Pair> values = new ArrayList<Pair>();

        // Next we align each pair of samples - but some may
        // have a missing X and some may have a missing Y.
        // We can only determine this by looking up their respective
        // visible row indices and seeing which one comes first.

        // While one's position (visible row index) is less than
        // the other's - we advance the lesser till we catch up.

        Iterator<KdxSample> xiter = xSamples.iterator();
        Iterator<KdxSample> yiter = ySamples.iterator();

        KdxSample x = xiter.hasNext() ? xiter.next() : null;
        KdxSample y = yiter.hasNext() ? yiter.next() : null;

        Comparable<?> xmin = null;
        Comparable<?> ymin = null;

        while (x != null && y != null) {
            int diff = comparator.compare(x, y);

            Comparable<?> xvalue = null;
            Comparable<?> yvalue = null;
            if (diff < 0) {
                // X is less, advance it after assigning a null Y
                xvalue = xValueFactory.getTraitValue(x);
                x = xiter.hasNext() ? xiter.next() : null;
            }
            else if (diff == 0) {
                xvalue = xValueFactory.getTraitValue(x);
                x = xiter.hasNext() ? xiter.next() : null;

                yvalue = xValueFactory.getTraitValue(y);
                y = yiter.hasNext() ? yiter.next() : null;
            }
            else {
                yvalue = xValueFactory.getTraitValue(y);
                y = yiter.hasNext() ? yiter.next() : null;
            }

            xmin = minimumOf(xmin, xvalue);
            ymin = minimumOf(ymin, yvalue);

            xOutputValues.add(xvalue);
            yOutputValues.add(yvalue);
            // values.add(new XYPair(xvalue, yvalue));
        }

        // Only one of these two will activate
        while (y != null) {
            Comparable<?> yvalue = yValueFactory.getTraitValue(y);
            xOutputValues.add(null);
            yOutputValues.add(yvalue);

            // xmin = minimumOf(xmin, xvalue);
            ymin = minimumOf(ymin, yvalue);

            // values.add(new XYPair(null, yvalue));
            y = yiter.hasNext() ? yiter.next() : null;
        }

        while (x != null) {
            Comparable<?> xvalue = xValueFactory.getTraitValue(x);
            xOutputValues.add(xvalue);
            yOutputValues.add(null);

            xmin = minimumOf(xmin, xvalue);
            // ymin = minimumOf(ymin, yvalue);

            // values.add(new XYPair(xvalue, null));
            x = xiter.hasNext() ? xiter.next() : null;
        }

        return new Comparable[] { xmin, ymin };
    }

    // /**
    // * Returns into xOutputValues the numerics.
    // * @param xSamples comes back modified into the correct sort order
    // * @param xValueFactory
    // * @param xOutputValues
    // * @return the minimum
    // */
    // @SuppressWarnings({ "rawtypes" })
    // private Comparable<?> collectSampleValuesByCurationTableVisibleRowOrder(
    // List<SampleMeasurement> xSamples, ValueFactory<?> xValueFactory,
    // List<Comparable> xOutputValues)
    // {
    //
    // Comparable xmin = null;
    // for (SampleMeasurement sm : xSamples) {
    // Comparable xvalue = xValueFactory.getTraitValue(sm);
    // xOutputValues.add(xvalue);
    // xmin = minimumOf(xmin, xvalue);
    // }
    // return xmin;
    // }

    private String buildMissingTraitsMessage() {
        StringBuilder sb = new StringBuilder(
                "Trait Ids in the Trial that are not in the Database:");
        int nPerLine = 20;
        for (Integer id : missingTraitIds) {
            if (nPerLine == 20) {
                sb.append('\n');
            }
            else {
                sb.append(' ');
            }
            sb.append(id);
            if (--nPerLine <= 0) {
                nPerLine = 20;
            }
        }
        String msg = sb.toString();
        return msg;
    }

    @Override
    public void doPostOpenActions() {

        plotCellChoicesPanel.doPostOpenActions();

        fieldLayoutView.doPostOpenActions();

        double dividerLocation = 0.35;
        // [ TraitValue & SampleData ] | [ Plot Attributes and Traits ]
        leftSplit.setDividerLocation(0.5);
        // [ Traits & Instances ] | [ FieldView & PlotTable ]
        statsAndSamplesSplit.setDividerLocation(dividerLocation);

        leftAndRightSplit.setDividerLocation(0.35);

        // Everything || messages
        mainVerticalSplit.setDividerLocation(0.12);

        // updateUndockMenuItem();

        List<String> warnings = new ArrayList<>();

        if (!missingTraitIds.isEmpty()) {
            String msg = buildMissingTraitsMessage();
            messages.println(WARNING_FLAG);
            messages.println(msg);
            messageLogger.w(getTitle(), msg);

            warnings.add(missingTraitIds.size() + " missing Traits");
        }

        if (!errorTraitsById.isEmpty()) {
            String msg = buildDatabaseTraitsWarning();

            messages.println(WARNING_FLAG);
            messages.println(msg);

            warnings.add(errorTraitsById.size() + " database samples for Traits not in the Trial");
        }

        if (!warnings.isEmpty()) {
            statusInfoLine.setError(
                    "There are " + StringUtil.join(" and ", warnings) + ". Use View/Show Warnings");

            showWarnings.setEnabled(true);
        }

        if (hideInactivePlots.isSelected()) {
            updateRowFilter();
        }

        if (anySubplotSamples()) {
            MsgBox.warn(this,
                    "Subplot Measurement Editing is currently Experimental"
                    + "\nIf you encounter issues, please report them via the link in the "
                            + PROBLEM + " menu"
                    , getTitle());
        }
    }

    private boolean anySubplotSamples() {
        if (anySubplotSamples(curationData.getDatabaseSampleGroup())) {
            return true;
        }
        if (anySubplotSamples(curationData.getEditedSampleGroup())) {
            return true;
        }
        for (SampleGroup sg : curationData.getDeviceSampleGroups()) {
            if (anySubplotSamples(sg)) {
                return true;
            }
        }
        return false;
    }

    private boolean anySubplotSamples(SampleGroup sg) {
        if (sg == null) {
            return false;
        }
        int plotSpecimenNumber = PlotOrSpecimen.ORGANISM_NUMBER_IS_PLOT;
        Optional<KdxSample> subPlotSample = sg.getSamples().stream()
                .filter(s -> s.getSpecimenNumber() != plotSpecimenNumber)
                .findFirst();
        return subPlotSample.isPresent();
    }

    // private void updateUndockMenuItem() {
    // if (undockMenuItem != null) {
    // Pair<String,JComponent> pair = getActiveTabComponent(true);
    // if (pair != null) {
    // undockMenuItem.setText(UNDOCK_ACTION_TITLE + " " + pair.first);
    // }
    // undockMenuItem.setEnabled(pair != null);
    // }
    // }

    @Override
    public boolean isClosable() {
        return true;
    }

    public Integer getTrialId() {
        return curationData.getTrialId();
    }

    @Override
    public boolean canClose() {
        boolean result = true;
        if (isSaveRequired()) {
            result = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(TrialDataEditor.this,
                    "You have unsaved changes\nDo you really want to close?",
                    getTitle(), JOptionPane.YES_NO_OPTION);
        }
        return result;
    }

    private boolean isSaveRequired() {
        return curationData.getUnsavedChangesCount() > 0;
    }

    @Override
    public Object getWindowIdentifier() {
        return curationData.getTrialId();
    }

    private Map<TraitInstance, List<Integer>> getSelectedModelRowsByTraitInstance(
            HowStarted howStarted) {

        int[] mcols;
        int[] mrows;

        switch (howStarted) {
        case FIELD_VIEW_CHANGED_ACTIVE_TRAIT_INSTANCE:
        case FIELD_VIEW_SELECTION_CHANGED:
            TraitInstance activeTraitInstance = fieldLayoutView.getActiveTraitInstance(true);
            Integer columnIndex = curationTableModel
                    .getColumnIndexForTraitInstance(activeTraitInstance);
            if (columnIndex == null) {
                // activeTraitInstance==null or no mapping current
                // convert this to all of the non-TraitInstance columns
                List<Integer> nonTiColumns = curationTableModel.getNonTraitInstanceColumns();
                mcols = new int[nonTiColumns.size()];
                for (int i = nonTiColumns.size(); --i >= 0;) {
                    mcols[i] = nonTiColumns.get(i);
                }
            }
            else {
                mcols = new int[] { columnIndex };
            }

            List<Plot> selectedPlots = fieldLayoutView.getFieldViewSelectedPlots();
            if (selectedPlots.isEmpty()) {
                mrows = new int[0];
            }
            else {
                // TODO check if
                List<Integer> rows = new ArrayList<>();
                // Find the rows for the plots
                for (Plot plot : selectedPlots) {
                    // FieldView only gives us the plots (unless we can somehow use the activeTraitInstance
                    Integer[] rowsForPlotId = curationTableModel.getRowsForPlotId(plot.getPlotId(), TraitLevel.PLOT);
                    Collections.addAll(rows, rowsForPlotId);
                }
                mrows = new int[rows.size()];
                for (int i = rows.size(); --i >= 0;) {
                    mrows[i] = rows.get(i);
                }
            }
            break;

        case UNDO:
        case REDO:
        case CURATION_DATA_CHANGED:
        case SAMPLE_TABLE_SELECTION_CHANGED:
            mcols = curationTable.getSelectedColumns();
            mrows = curationTable.getSelectedRows();
            for (int i = 0; i < mcols.length; ++i) {
                mcols[i] = curationTable.convertColumnIndexToModel(mcols[i]);
            }
            for (int i = 0; i < mrows.length; ++i) {
                mrows[i] = curationTable.convertRowIndexToModel(mrows[i]);
            }
            break;

        case VISTOOL_SELECTION:
            mcols = curationTableSelectionModel.getToolSelectedModelColumns();
            mrows = curationTableSelectionModel.getToolSelectedModelRows();
            break;
        default:
            throw new RuntimeException("Unhandled option: " + howStarted);
        }

        // The view rows correspond to the Plots which are in the modelRows
        int modelRowCount = curationTableModel.getRowCount();

        // The TraitInstances are in the columns.
        // Check each column against the current visible column count
        // in case of races between "selection changes" and other issues.
        int tableColumnCount = curationTable.getColumnCount();

        Map<TraitInstance, List<Integer>> selectedRowsByTraitInstance = new HashMap<>();

        for (int mcol : mcols) {
            if (0 <= mcol && mcol < tableColumnCount) {
                TraitInstance ti = curationTableModel.getTraitInstanceAt(mcol);
                if (ti != null) {
                    for (int mrow : mrows) {
                        if (0 <= mrow && mrow < modelRowCount) {
                            List<Integer> list = selectedRowsByTraitInstance.get(ti);
                            if (list == null) {
                                list = new ArrayList<>();
                                selectedRowsByTraitInstance.put(ti, list);
                            }
                            list.add(mrow);
                        }
                    }
                }
            }
        }

        return selectedRowsByTraitInstance;
    }

    private Map<TraitInstance, List<Integer>> getModelRowsByTraitInstance() {
        // The view rows correspond to the Plots which are in the modelRows
        int modelRowCount = curationTableModel.getRowCount();
        Map<TraitInstance, List<Integer>> selectedRowsByTraitInstance = new HashMap<>();

        for (TraitInstance ti : this.traitsAndInstancesPanel.getTraitInstances(true)) {
            if (ti != null) {
                for (int mrow = 0; mrow < modelRowCount; mrow++) {
                    List<Integer> list = selectedRowsByTraitInstance.get(ti);
                    if (list == null) {
                        list = new ArrayList<>();
                        selectedRowsByTraitInstance.put(ti, list);
                    }
                    list.add(mrow);
                }
            }
        }

        return selectedRowsByTraitInstance;
    }

    private HowStarted alreadyStarted = null;

    private void startEdit(HowStarted howStarted) { // FIXME this needs to be
                                                    // provided with the source
                                                    // that triggered it!

        if (alreadyStarted != null) {
            Shared.Log.d(TAG,
                    "startEdit(" + howStarted + ") : ***** LOOPING while " + alreadyStarted);
            return;
        }

        alreadyStarted = howStarted;
        try {
            Shared.Log.d(TAG, "=============== startEdit: BEGIN " + howStarted);
            startEditInternal(howStarted);
        }
        finally {
            alreadyStarted = null;
            Shared.Log.d(TAG, "=============== startEdit: END " + howStarted + "\n");
        }
    }

    private void startEditInternal(HowStarted howStarted) {
        // FIXME and that should be used to determine the "fieldViewActive" as
        // otherwise we think we're in the sample table
        // FIXME but in fact the FieldView got a setSelectedPoints
        int tabbedIndex = mainTabbedPane.getSelectedIndex();

        // boolean fieldViewActive = fieldLayoutViewPanel ==
        // trialViewTabbedPane.getComponentAt(tabbedIndex);
        boolean fieldViewActive;
        switch (howStarted) {
        case CURATION_DATA_CHANGED:
            fieldViewActive = false;
            break;
        case FIELD_VIEW_CHANGED_ACTIVE_TRAIT_INSTANCE:
            // Could have come from Overview
            fieldViewActive = fieldLayoutView.getPanel() == mainTabbedPane
                    .getComponentAt(tabbedIndex);
            break;
        case FIELD_VIEW_SELECTION_CHANGED:
            // Hmmm. Could be triggered by add/remove of a TraitInstance
            // which does a setSelection() on the field view
            fieldViewActive = true;
            break;
        case SAMPLE_TABLE_SELECTION_CHANGED:
            fieldViewActive = false;
            break;
        case REDO:
        case UNDO:
            fieldViewActive = fieldLayoutView.getPanel() == mainTabbedPane
                    .getComponentAt(tabbedIndex);
            break;
        case VISTOOL_SELECTION:
            fieldViewActive = fieldLayoutView.getPanel() == mainTabbedPane
                    .getComponentAt(tabbedIndex);
            break;
        default:
            throw new RuntimeException("Option not handled: " + howStarted);
        }

        Shared.Log.d(TAG, "    startEditInternal "
                + (fieldViewActive ? "FOR FieldView" : "FOR SampleTable") + ": " + howStarted);

        Map<TraitInstance, List<CurationCellValue>> ccvsByTraitInstance;

        // boolean isCalc = false;
        // TODO - Will we ever implement this?

        // List<CurationCellValue> ccvs;
        boolean multipleColumns = false;

        if (fieldViewActive) {

            SelectedInfo selectedInfo = fieldLayoutView.createFromFieldView();

            PlotsByTraitInstance plotsByTi = selectedInfo.plotsByTraitInstance;

            // Make sure the CurationTable knows what is selected in the FieldView
            selectedValueStore.setSelectedPlots(fieldLayoutView.getStoreId(), plotsByTi);

            curationTableSelectionModel.updateSampleSelection();

            ccvsByTraitInstance = selectedInfo.ccvsByTraitInstance;
        }
        else {
            // Samples Table active

            SelectedInfo selectedInfo = createSelectedInfoFromCurationTableModel(howStarted);

            PlotsByTraitInstance plotsByTi = selectedInfo.plotsByTraitInstance;

            selectedValueStore.setSelectedPlots(curationTable.getName(), plotsByTi);

            // Shared.Log.d(TAG,
            // " startEditInternal: calling
            // fieldLayoutView.refreshSelectedMeasurements(...)");
            fieldLayoutView.refreshSelectedMeasurements(
                    "TrialDataEditor.startEditInternal(" + howStarted + ")");
            // FIXME above triggers data loading

            ccvsByTraitInstance = selectedInfo.ccvsByTraitInstance;
        }

        TraitInstance traitInstance = null;
        List<CurationCellValue> ccvs = null;

        if (!ccvsByTraitInstance.isEmpty()) {
            // Only get the first
            for (TraitInstance ti : ccvsByTraitInstance.keySet()) {
                traitInstance = ti;
                ccvs = ccvsByTraitInstance.get(ti);
                break;
            }
        }

        String cardName = CARD_NO_CELL;
        if (traitInstance == null) {
        }
        else {
            curationTableModel.clearTemporaryValues(/* traitInstance */);

            fieldLayoutView.clearTemporaryValues(ccvs);

            if (TraitDataType.CALC == traitInstance.getTraitDataType()) {
                cardName = CARD_CALC;
                // FIXME below triggers data loading
                // fieldView.refreshSelectedMeasurements(...)
                curationCellEditor.setCurationCellValue(null);
            }
            else {
                cardName = CARD_EDIT_CELL;
                // FIXME below triggers data loading
                // fieldView.refreshSelectedMeasurements(...)
                curationCellEditor.setCurationCellValue(ccvs);
            }
        }

        cardLayout.show(cardLayoutPanel, cardName);
    }

    // HowStarted == null : when called from "Set Multiple Values Action"
    // != null : when called to startEditing
    private SelectedInfo createSelectedInfoFromCurationTableModel(HowStarted howStarted) {

        SelectedInfo result = new SelectedInfo();

        // We are operating on the Samples Table.
        Map<TraitInstance, List<Integer>> modelRowsByTraitInstance = new HashMap<>();
        if (howStarted == null) {
            // Called from setMultipleValuesAction
            modelRowsByTraitInstance = getModelRowsByTraitInstance();
        }
        else {
            // Called from startEdit - user has
            modelRowsByTraitInstance = getSelectedModelRowsByTraitInstance(howStarted);
        }

        for (TraitInstance target : modelRowsByTraitInstance.keySet()) {
            Integer modelColumn = curationTableModel.getColumnIndexForTraitInstance(target);
            if (modelColumn != null) {
                for (Integer modelRow : modelRowsByTraitInstance.get(target)) {
                    if (modelRow >= 0) {
                        Plot plot = curationTableModel.getPlotAtRowIndex(modelRow);
                        if (plot != null) {
                            result.addPlot(target, plot);

                            CurationCellValue ccv = curationData.getCurationCellValue(plot, target);
                            result.addCcv(target, ccv);
                        }

                    }
                }
            }
        }
        return result;
    }

    public void pushDownAllGraphAndPlotFrames() {
        for (JFrame f : toolFrames) {
            f.setAlwaysOnTop(false);
        }
    }

}
