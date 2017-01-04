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
package com.diversityarrays.kdxplore.trials;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.logging.Log;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.dalclient.DalException;
import com.diversityarrays.dalclient.DalResponseHttpException;
import com.diversityarrays.dalclient.http.DalHeader;
import com.diversityarrays.daldb.core.GeneralType;
import com.diversityarrays.db.DartSchemaHelper;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.TrialChangeListener;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.CreateItemException;
import com.diversityarrays.kdsmart.db.util.Util;
import com.diversityarrays.kdx2s.kdsimport.ImportTrialCsvDialog;
import com.diversityarrays.kdx2s.kdsimport.KdxploreDatabaseStub;
import com.diversityarrays.kdxplore.KDDartEntityFactory;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.SourceChoiceHandler;
import com.diversityarrays.kdxplore.SourceChoiceHandler.SourceChoice;
import com.diversityarrays.kdxplore.barcode.BarcodePreferences;
import com.diversityarrays.kdxplore.beans.DartEntityBeanRegistry;
import com.diversityarrays.kdxplore.beans.DartEntityFeature;
import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel.ImportFileGroup;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel.ImportType;
import com.diversityarrays.kdxplore.data.DatabaseDataUtils;
import com.diversityarrays.kdxplore.data.DeleteTrialException;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.OfflineDataChangeListener;
import com.diversityarrays.kdxplore.data.jdbc.KdxploreConfigException;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.data.kdx.SamplesSavedListener;
import com.diversityarrays.kdxplore.data.loading.MediaSampleRetriever;
import com.diversityarrays.kdxplore.data.loading.MultiTrialLoader;
import com.diversityarrays.kdxplore.data.loading.TrialLoadResult;
import com.diversityarrays.kdxplore.data.loading.TrialLoadResult.StoreResult;
import com.diversityarrays.kdxplore.data.refdata.MultimediaSourceMissingException;
import com.diversityarrays.kdxplore.data.loading.TrialLoaderResult;
import com.diversityarrays.kdxplore.data.loading.TrialStoreException;
import com.diversityarrays.kdxplore.importdata.ImportSourceChoiceDialog;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.prefs.PreferenceCollection;
import com.diversityarrays.kdxplore.services.KdxApp;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.kdxplore.services.SeedPrepHarvestService;
import com.diversityarrays.kdxplore.services.SeedPrepHarvestService.HarvestParams;
import com.diversityarrays.kdxplore.services.SeedPrepHarvestService.SeedPrepParams;
import com.diversityarrays.kdxplore.services.TrialDataEditorService;
import com.diversityarrays.kdxplore.services.TrialDataEditorService.CurationParams;
import com.diversityarrays.kdxplore.services.TrialDataEditorService.InitError;
import com.diversityarrays.kdxplore.services.TrialDataEditorService.TrialDataEditorResult;
import com.diversityarrays.kdxplore.trialmgr.ClientUrlChanger;
import com.diversityarrays.kdxplore.trialmgr.TrialExplorerManager;
import com.diversityarrays.kdxplore.trialmgr.trait.TraitExplorer;
import com.diversityarrays.kdxplore.trialtool.TrialToolUtils;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.kdxplore.upload.KddartUploadResult;
import com.diversityarrays.kdxplore.upload.TrialUploadHandler;
import com.diversityarrays.kdxservice.KDXDeviceService;
import com.diversityarrays.ormlite.jdbc.DriverType;
import com.diversityarrays.util.AbstractMsg;
import com.diversityarrays.util.DALClientProvider;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.PrintStreamMessageLogger;
import com.diversityarrays.util.RunMode;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.MessagePrinter;
import net.pearcan.util.StringUtil;

/**
 * Provides the main User Interface for displaying all of the Trials in a
 * database and the functions that may be performed on the group of entities.
 * Other details are available via:
 * <ul>
 * <li>TrialOverviewPanel</li>
 * <li>TrialDetailsPanel</li>
 * <li>TrialViewPanel</li>
 * </ul>
 * Note that we can probably move all of the <i>TrialViewPanel</i> back up into
 * the <i>TrialDetailsPanel</i> at some time in the future. The current
 * structure was arrived at when experimenting with alternate implementations of
 * each structural element but now that we have stabilised the structure we can
 * remove the extra layer.
 * 
 * <pre>
 *    +----------------------------+---------------------------+
 *    | +-- TrialOverviewPanel --+ | +-- TrialDetailsPanel --+ |
 *    | |    _________________   | | | (x) (x) (x) (x)       | |
 *    | |   [ list of Trials  ]  | | | +- TrialViewPanel --+ | |
 *    | |   [                 ]  | | | |  _______________  | | |
 *    | |   [                 ]  | | | | [ details of    ] | | |
 *    | |   [                 ]  | | | | [ selected      ] | | |
 *    | |   [                 ]  | | | | [ Trial         ] | | |
 *    | |   [_________________]  | | | | [_______________] | | |
 *    | |                        | | | |                   | | |
 *    | | - - - - - - - - - - -  | | | | - - - - - - - - - | | |
 *    | |    _________________   | | | |  _______________  | | |
 *    | |   [ Traits of       ]  | | | | [ Sample Groups ] | | |
 *    | |   [ selected Trial  ]  | | | | [ of selected   ] | | |
 *    | |   [                 ]  | | | | [ Trial         ] | |
 *    | |   [                 ]  | | | | [_______________] | | |
 *    | |   [_________________]  | | | |___________________| | |
 *    | |________________________| | |_______________________| |
 *    +----------------------------+---------------------------+
 * </pre>
 * 
 * @author brian
 * 
 */
public class TrialExplorerPanel extends JPanel {
    
    private static final int CONFIG_FLAG_SEEDPREP = 0x1;
    private static final int CONFIG_FLAG_HARVEST  = 0x2;
    private static final int CONFIG_FLAG_SEED_AND_HARV = CONFIG_FLAG_SEEDPREP | CONFIG_FLAG_HARVEST;

    private static final String SEEDPREP_HARVEST_SERVICE_IMPL_CLASSNAME = "com.diversityarrays.kdxplore.trialtools.SeedPrepHarvestServiceImpl"; //$NON-NLS-1$

    private static final String TAG = "TrialExplorer"; //$NON-NLS-1$

    private static final TraitNameStyle DEFAULT_DOWNLOAD_TRAIT_NAME_STYLE = TraitNameStyle.TWO_USCORE_DIGITS;

    private final String REMOVE_TRIAL = Msg.TITLE_REMOVE_TRIAL();

    private final String UPLOAD_TRIAL = Msg.TITLE_STORE_TRIAL();

    private final String EDIT_TRIAL = Msg.TITLE_EDIT_VIEW_TRIAL();

    private final String SEED_PREP = Msg.TITLE_PREPARE_FOR_PLANTING() + AbstractMsg.BETA_SUFFIX;

    private final String HARVEST_PREP = Msg.TITLE_HARVEST_PROCESSING() + AbstractMsg.BETA_SUFFIX;

    private final String REFRESH_TRIAL_INFO = Msg.TITLE_REFRESH_TRIAL_DATA();

    private final String ADD_TRIALS = Msg.TITLE_ADD_TRIALS();

//    private TrialUploadHandler trialUploadHandler;

    static private TraitNameStyle getDefaultTraitNameStyle() {
        return KdxploreConfig.getInstance().getModeList().contains("CIMMYT") //$NON-NLS-1$
                ? TraitNameStyle.NO_INSTANCES
                : TraitNameStyle.TRAILING_COLON;
    }

    static class JustLabelPanel extends JPanel {
        JustLabelPanel(String[] lines, Action... actions) {
            super(new BorderLayout());

            StringBuilder sb = new StringBuilder();
            for (String s : lines) {
                sb.append(s);
            }

            if (actions != null && actions.length > 0) {
                Box box = Box.createVerticalBox();
                box.add(new JLabel(sb.toString()));

                // Box buttons = Box.createHorizontalBox();
                // box.add(buttons);
                for (Action action : actions) {
                    // buttons.add(new JButton(action));
                    box.add(new JButton(action));
                }
                box.add(Box.createVerticalGlue());

                add(box, BorderLayout.CENTER);
            }
            else {
                add(new JLabel(sb.toString()), BorderLayout.CENTER);
            }
        }
    }

    private JSplitPane lrSplitPane;

    static enum NoTrialUnitOption {
        LOAD_FROM_DATABASE(Msg.ENUM_NO_TRIAL_UNIT_LOAD_FROM_DB()), EDIT_WITHOUT_TRIAL_UNITS(
                Msg.ENUM_NO_TRIAL_UNIT_EDIT_WITHOUT()),;

        public final String text;

        NoTrialUnitOption(String q) {
            this.text = q;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private Action uploadTrialAction = new AbstractAction(UPLOAD_TRIAL) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final Trial trial = trialOverviewPanel.getSelectedTrial();
            if (trial != null) {
                JFrame frame = windowOpener.getWindowByIdentifier(trial
                        .getTrialId());
                if (frame != null) {
                    MsgBox.error(TrialExplorerPanel.this,
                            "You are still editing that trial",
                            UPLOAD_TRIAL);
                    GuiUtil.restoreFrame(frame);
                }
                else {
                    // In CIMMYT mode we prevent a "locally created" Trial from
                    // being uploaded; but provide a hack override in case we
                    // need it :-).
                    if (KdxploreConfig.getInstance().getModeList().contains("CIMMYT")) {
                        if (null == trial.getIdDownloaded()) {
                            if (!allowUploadOfLocalTrial()) {
                                // stderr log so those in the know can figure
                                // out where to put the override
                                System.err.println(
                                        "kdxplore-extra.properties/" + ALLOW_UPLOAD_OF_LOCAL_TRIAL);
                                MsgBox.error(TrialExplorerPanel.this,
                                        "Trial is not from the database and so cannot be uplaoded",
                                        UPLOAD_TRIAL);
                            }
                        }
                    }
                    doUploadTrial(trial);
                }
            }
        }

        private final String ALLOW_UPLOAD_OF_LOCAL_TRIAL = "allowUploadOfLocalTrial";

        private boolean allowUploadOfLocalTrial() {
            boolean result = false;
            File home = new File(System.getProperty("user.home"));
            File propsFile = new File(home, "kdxplore-extra.properties");
            if (propsFile.exists()) {
                Properties properties = new Properties();
                try {
                    properties.load(new FileReader(propsFile));
                    result = Boolean
                            .parseBoolean(properties.getProperty(ALLOW_UPLOAD_OF_LOCAL_TRIAL));
                }
                catch (IOException ignore) {
                }
                ;
            }
            return result;
        }
    };

    private Action editTrialAction = new AbstractAction(EDIT_TRIAL) {
        @Override
        public void actionPerformed(ActionEvent ee) {
            final Trial trial = trialOverviewPanel.getSelectedTrial();
            if (trial != null) {

                JFrame frame = windowOpener.getWindowByIdentifier(trial.getTrialId());

                if (frame != null) {
                    GuiUtil.restoreFrame(frame);
                }
                else {
                    try {
                        doEdit(trial);
                    }
                    catch (IOException e) {
                        MsgBox.error(TrialExplorerPanel.this, e.getMessage(), EDIT_TRIAL);
                    }
                }
            }
        }
    };

    private Action seedPrepAction = new AbstractAction(SEED_PREP) {
        @Override
        public void actionPerformed(ActionEvent ee) {
            final Trial trial = trialOverviewPanel.getSelectedTrial();
            if (trial != null) {
                String why = TrialToolUtils.getErrorIfNotValidForProcessing(trial);
                if (null != why) {
                    MsgBox.error(TrialExplorerPanel.this,
                            "Trial is not valid for this function"
                                    + "\n(" + why + ")",
                            SEED_PREP);
                    return;
                }

                try {
                    if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(TrialExplorerPanel.this, 
                            "This is Pre-Production Software\nContinue?", 
                            SEED_PREP, JOptionPane.OK_CANCEL_OPTION)) {
                        doSeedPrep(trial);
                    }
                }
                catch (IOException e) {
                    MsgBox.error(TrialExplorerPanel.this,
                            e.getMessage(), SEED_PREP);
                }
            }
        }
    };

    private SeedPrepHarvestService seedPrepHarvestService;

    private JPopupMenu harvestPopupMenu;
    private Action harvestAction = new AbstractAction(HARVEST_PREP) {
        @Override
        public void actionPerformed(ActionEvent ee) {
            final Trial trial = trialOverviewPanel.getSelectedTrial();
            if (trial != null) {
                String why = TrialToolUtils.getErrorIfNotValidForProcessing(trial);
                if (null != why) {
                    MsgBox.error(TrialExplorerPanel.this,
                            "Trial is not valid for this function"
                                    + "\n(" + why + ")",
                            HARVEST_PREP);
                    return;
                }

                if (harvestPopupMenu == null) {
                    harvestPopupMenu = new JPopupMenu("Harvest");
                    harvestPopupMenu
                            .add(new AbstractAction("Harvest Trial" + AbstractMsg.BETA_SUFFIX) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            MsgBox.warn(TrialExplorerPanel.this, "Sorry - not yet available",
                                    "Trial Harvest");
                        }
                    });
                    harvestPopupMenu
                            .add(new AbstractAction("Harvest Nursery" + AbstractMsg.BETA_SUFFIX) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(TrialExplorerPanel.this, 
                                        "This is Pre-Production Software\nContinue?", 
                                        HARVEST_PREP, JOptionPane.OK_CANCEL_OPTION)) {
                                    startHarvest(trial);
                                }
                            }
                            catch (IOException err) {
                                MsgBox.error(TrialExplorerPanel.this,
                                        err.getMessage(), HARVEST_PREP);
                            }
                        }
                    });
                }

                Component comp = null;
                Object src = ee.getSource();
                if (src instanceof Component) {
                    comp = (Component) src;
                    // TODO get x,y from Component
                }
                harvestPopupMenu.show(comp, 20, 20);
            }
        }
    };

    private void doSeedPrep(Trial trial) throws IOException {

        Object windowIdent = SeedPrepHarvestService
                .createSeedPrepWindowIdentifier(trial.getIdDownloaded());

        // NOTE: MUST use the same object that
        // SeedPreparationWizardPanel.getWindowIdentifier() provides
        JFrame frame = windowOpener.getWindowByIdentifier(windowIdent);

        if (frame != null) {
            GuiUtil.restoreFrame(frame);
            return;
        }

        String title = "Seed Prep: " + trial.getTrialName() + AbstractMsg.ALPHA_SUFFIX;

        if (seedPrepHarvestService == null) {
            MsgBox.error(TrialExplorerPanel.this, "No SeedPrepService", title);
        }
        else {
            SeedPrepParams params = new SeedPrepParams();

            params.title = title;
            params.trial = trial;
            params.offlineData = offlineData;
            params.clientProvider = clientProvider;
            params.dartSchemaHelper = dartSchemaHelper;
            params.messageLogger = messageLogger;
            params.component = TrialExplorerPanel.this;
            params.windowOpener = windowOpener;
            // params.trialDesignEditor = null;
            params.deviceService = kdxDeviceService;

            seedPrepHarvestService.createSeedPrepUserInterface(backgroundRunner, params);
        }
    }

    private Trial previousHarvestTrial = null;

    private boolean sameHarvest(Trial trial) {
        boolean result = previousHarvestTrial != null
                ? previousHarvestTrial.getTrialId() == trial.getTrialId() : false;
        previousHarvestTrial = trial;
        return result;
    }

    private void startHarvest(Trial trial) throws IOException {

        Object windowId = SeedPrepHarvestService
                .createHarvestWindowIdentifier(trial.getIdDownloaded());

        // NOTE: MUST use the same object that
        // HarvestPanel.getWindowIdentifier() provides
        JFrame frame = windowOpener.getWindowByIdentifier(windowId);
        if (frame != null && sameHarvest(trial)) {
            GuiUtil.restoreFrame(frame);
            return;
        }

        String title = "Harvest: " + trial.getTrialName() + AbstractMsg.ALPHA_SUFFIX;

        if (seedPrepHarvestService == null) {
            MsgBox.error(TrialExplorerPanel.this, "No HarvestService", title);
        }
        else {
            HarvestParams params = new HarvestParams();

            params.title = title;
            params.trial = trial;
            params.offlineData = offlineData;
            params.deviceService = kdxDeviceService;
            params.clientProvider = clientProvider;
            params.dartSchemaHelper = dartSchemaHelper;
            params.messageLogger = messageLogger;
            params.component = TrialExplorerPanel.this;
            params.windowOpener = windowOpener;

            seedPrepHarvestService.createHarvestUserInterface(backgroundRunner, params);
        }
    }

    private void doEdit(Trial trial) throws IOException {

        int nTrialUnits = offlineData.getPlotCount(trial.getTrialId());

        if (nTrialUnits <= 0) {

            List<NoTrialUnitOption> options = new ArrayList<>(
                    Arrays.asList(NoTrialUnitOption.values()));

            PlotIdentSummary pis = trial.getPlotIdentSummary();
            if (pis == null || (!pis.hasXandY() && pis.plotIdentRange.isEmpty())) {
                options.remove(NoTrialUnitOption.EDIT_WITHOUT_TRIAL_UNITS);
            }

            NoTrialUnitOption[] use = options.toArray(new NoTrialUnitOption[options.size()]);

            NoTrialUnitOption answer = GuiUtil.choose(TrialExplorerPanel.this,
                    EDIT_TRIAL, Msg.MSG_NO_PLOTS_AVAILABLE_HOW_PROCEED(), null,
                    "Cancel", NoTrialUnitOption.LOAD_FROM_DATABASE,
                    use);

            if (answer != null) {
                switch (answer) {
                case EDIT_WITHOUT_TRIAL_UNITS:
                    doEditTrial(trial);
                    break;

                case LOAD_FROM_DATABASE:

                    doCollectTrialInfoAfterClientCheck(trial);

                    break;
                default:
                    throw new RuntimeException("Unhandled option: " + answer);
                }
            }
        }
        else {
            // Yes, we have trial units
            doEditTrial(trial);
        }
    }

    private void doCollectTrialInfoAfterClientCheck(final Trial trial) {
        Closure<TrialLoadResult> onFinish = new Closure<TrialLoadResult>() {
            @Override
            public void execute(TrialLoadResult lr) {
                handleTrialDataLoadResult(EDIT_TRIAL, lr);
            }
        };

        collectTrialInfoAfterClientCheck(EDIT_TRIAL, trial, onFinish);
    }

    private final Supplier<List<Trial>> trialListSupplier = new Supplier<List<Trial>>() {
        @Override
        public List<Trial> get() {
            return trialOverviewPanel.getAllTrials();
        }
    };

    private final Action removeTrialAction = new AbstractAction(REMOVE_TRIAL) {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Trial> trialsToRemove = trialOverviewPanel.getSelectedTrials();
            if (!trialsToRemove.isEmpty()) {
                StringBuilder sb = new StringBuilder("Trial(s) to remove:");

                for (Trial t : trialsToRemove) {
                    sb.append('\n').append("Trial#").append(t.getTrialId())
                            .append(": ").append(t.getTrialName());
                }

                String title = "Confirm " + REMOVE_TRIAL;
                
                boolean deleteTraits = false;
                if (0 != (ActionEvent.SHIFT_MASK & e.getModifiers())) {
                    deleteTraits = true;
                    title = title + " (& Traits)";
                }

                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                        TrialExplorerPanel.this,
                        sb.toString(),
                        title, JOptionPane.YES_NO_OPTION)) 
                {
                    Set<Trait> allTraits = new HashSet<>();
                    for (Trial t : trialsToRemove) {
                        try {
                            Set<Trait> trialTraits = null;
                            try {
                                trialTraits = offlineData.getTraits(t);
                            }
                            catch (IOException e1) {
                                Shared.Log.w(TAG, 
                                        "removeTrial: unable to get Traits for '" + t.getTrialName() + "'", 
                                        e1);
                            }
                            
                            offlineData.removeTrial(t);
                            // If Trial was removed then we will try to remove the traits
                            if (trialTraits != null) {
                                allTraits.addAll(trialTraits);
                            }
                            messagePrinter.println("Removed: " + t.getTrialName());
                        }
                        catch (DeleteTrialException dte) {
                            dte.printStackTrace();
                            messagePrinter.println("Failed: " + t.getTrialName());
                            messagePrinter.println(dte.getMessage());
                        }
                    }
                    
                    trialsChangedConsumer.accept(null);
                    
                    if (deleteTraits && ! allTraits.isEmpty()) {
                        traitRemovalHandler.accept(allTraits);
                    }
                }
            }
        }
    };

    private final Action addTrialsAction = new AbstractAction(ADD_TRIALS) {
        @Override
        public void actionPerformed(ActionEvent e) {

            String databaseUrl = offlineData.getDatabaseUrl();

            if (databaseUrl != null) {

                if (!KdxploreDatabase.LOCAL_DATABASE_URL.equalsIgnoreCase(databaseUrl)) {
                    try {
                        URL url = new URL(databaseUrl);
                        String protocol = url.getProtocol();
                        if ("file".equals(protocol)) {
                            MsgBox.warn(TrialExplorerPanel.this,
                                    "Can't connect to '" + databaseUrl + "'"
                                            + "\nYou must be using a test area.",
                                    ADD_TRIALS);
                            return;
                        }

                    }
                    catch (MalformedURLException e1) {
                        MsgBox.error(TrialExplorerPanel.this, e1, "Unable to Add Trials");
                        return;
                    }
                }
            }

            SourceChoiceHandler sourceChoiceHandler = new SourceChoiceHandler() {
                @Override
                public void handleSourceChosen(SourceChoice choice) {
                    switch (choice) {
                    case DATABASE:
                        loadFromKddartDatabase();
                        break;
                    case KDX:
                    case XLS:
                        loadUsingKdxOrXlsFiles(choice);
                        break;
                    case CSV:
                        TraitNameStyle tns = getDefaultTraitNameStyle();
                        ImportTrialCsvDialog dlg = new ImportTrialCsvDialog(
                                GuiUtil.getOwnerWindow(TrialExplorerPanel.this),
                                databaseDeviceIdentifier,
                                tns,
                                deviceInfoProvider,
                                offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase(),
                                trialsChangedConsumer);
                        dlg.setVisible(true);
                        break;
                    default:
                        break;
                    }
                }
            };

            List<SourceChoice> choices = new ArrayList<>();
            Collections.addAll(choices, SourceChoice.values());

            if (KdxploreConfig.getInstance().getModeList().contains("CIMMYT")) {
                
                // No longer supporting XLS (vis BMS XLS - need to re-enable when we
                // finish doing the Excel import code)
                if (RunMode.getRunMode().isDeveloper()) {
                    new Toast(TrialExplorerPanel.this, "XLS Temporarily Enabled", Toast.SHORT).show();
                }
                else {
                    choices.remove(SourceChoice.XLS);
                }
                
//                // At CIMMYT we want to discourage use of CSV files.
//                choices.remove(SourceChoice.CSV);

                // And we don't allow from database (yet)
                choices.remove(SourceChoice.DATABASE);
            }

            SourceChoiceHandler.Util
                    .showSourceChoicePopup(addDatabaseTrialsButton, 0, 0,
                            "Select source for Trial data",
                            sourceChoiceHandler,
                            choices.toArray(new SourceChoice[choices.size()]));
        }
    };

    private final JButton addDatabaseTrialsButton;

    private void askForTrialsThenDownload(DALClient client) {

        try {

            Transformer<BackgroundRunner, TrialSearchOptionsPanel> searchOptionsPanelFactory = new Transformer<BackgroundRunner, TrialSearchOptionsPanel>() {
                @Override
                public TrialSearchOptionsPanel transform(BackgroundRunner br) {
                    return TrialSelectionSearchOptionsPanel.create(br);
                }
            };

            final TrialSelectionDialog trialSelectionDialog = new TrialSelectionDialog(
                    GuiUtil.getOwnerWindow(TrialExplorerPanel.this),
                    "Choose Trials to Download", client, offlineData.getTrials(),
                    searchOptionsPanelFactory);

            trialSelectionDialog.setVisible(true);

            if (trialSelectionDialog.trialRecords != null) {
                Closure<TrialLoaderResult> onTrialsAdded = new Closure<TrialLoaderResult>() {
                    @Override
                    public void execute(TrialLoaderResult trialLoaderResult) {
                        handleTrialDataLoaded(trialLoaderResult);
                    }
                };

                boolean wantSpecimens = getSpecimensRequired();
                
              	Exception error = null;
            	Optional<Optional<GeneralType>> opt_opt_mmt = Optional.empty();
            	try {
    	            opt_opt_mmt = MediaSampleRetriever.getCuratedSamplesMultimediaType(
    	                    TrialExplorerPanel.this, offlineData.getKddartReferenceData());	            
            	}
            	catch (MultimediaSourceMissingException e) {
            		error = e;
            	}

            	if (! opt_opt_mmt.isPresent()) {
            		MediaSampleRetriever.showMissingMultimedia(TrialExplorerPanel.this, error, "Can't upload");
                    return;
                }

                MultiTrialLoader multiTrialLoader = new MultiTrialLoader(
                        TrialExplorerPanel.this,
                        "Saving Trials for Offline access", true,
                        client,
                        dartSchemaHelper,
                        wantSpecimens,
                        pluginInfo.getBackgroundRunner(),
                        pluginInfo.getBackgroundRunner(),
                        pluginInfo.getMessageLogger(),
                        offlineData.getKdxploreDatabase(),
                        opt_opt_mmt.get(),
                        DEFAULT_DOWNLOAD_TRAIT_NAME_STYLE,
                        trialSelectionDialog.trialRecords,
                        onTrialsAdded);

                backgroundRunner.runBackgroundTask(multiTrialLoader);
            }
        }
        catch (IOException e) {
            MsgBox.error(TrialExplorerPanel.this, e.getMessage(), "Problem Getting Trials");
        }
    }

    private boolean getSpecimensRequired() {
        KdxploreConfig config = KdxploreConfig.getInstance();
        return 0 != (CONFIG_FLAG_SEED_AND_HARV & config.getFlags());
    }

//    private void refreshTrialsFromServer(Collection<Trial> newTrialList) {
//        try {
//            for (Trial tp : offlineData.getTrials()) {
//                if (newTrialList.contains(tp)) {
//                    retrieveTrialDataFromServer(tp, true);
//                }
//            }
//        }
//        catch (IOException e) {
//            MsgBox.error(TrialExplorerPanel.this, e.getMessage(), "Problem Getting Trials");
//        }
//    }

    private void loadFromKddartDatabase() {
        try {
            List<Trial> trialList = offlineData.getTrials();

            DALClient client = null;
            // clientUrlChanger.setIgnoreClientUrlChanged(true);

            // FIXME - move this code out of trialmgr into the OfflineDataApp
            // so that it is all managed centrally.
            // This will mean changes to the places that track the URL.
            // SHould probably also move the Offfline data management to that
            // KdxApp too.

            client = clientProvider.getDALClient(ADD_TRIALS);
            if (client == null) {
                return; // user cancelled
            }

            final UrlChangeConfirmation confirmation = confirmChangedClientIsOk(
                    client, ADD_TRIALS, WILL_NOT_CANCEL_PENDING);

            switch (confirmation) {
            case CHANGE_DENIED:
                return;

            case NEW_DATABASE:
            case CHANGE_APPROVED:
                initClientLog(client);

                Closure<DALClient> onLoadComplete = new Closure<DALClient>() {
                    @Override
                    public void execute(DALClient dalClient) {
                        clientUrlChanger.clientUrlChanged();

                        if (dalClient == null) {
                            Shared.Log.w(TAG,
                                    "loadFromKddartDatabase: " + ADD_TRIALS //$NON-NLS-1$
                                            + ": Failed to change client"); //$NON-NLS-1$
                        }
                        else {
                            askForTrialsThenDownload(dalClient);
                        }
                    }
                };
                loadDatabaseDataFromKddart(ADD_TRIALS, confirmation, client, onLoadComplete);
                break;

            case NO_CHANGE_SAME_URL:
                initClientLog(client);
                doAddTrials(client, trialList);
                break;
            default:
                throw new RuntimeException("Unhandled value " + confirmation);
            }

        }
        catch (IOException err) {
            MsgBox.error(TrialExplorerPanel.this, err.getMessage(), "Problem Getting Trials");
        }
    }

    private void doAddTrials(DALClient dalClient, List<Trial> trialList) {
        askForTrialsThenDownload(dalClient);
        // This is called at the wrong time because askForTrialsThenDownload()
        // runs a Background Task
        // try {
        // List<Trial> newTrialList = offlineData.getTrials();
        // for (Trial tp : trialList) {
        // if (newTrialList.contains(tp)) {
        // newTrialList.remove(tp);
        // }
        // }
        // refreshTrialsFromServer(newTrialList);
        // }
        // catch (IOException e) {
        // MsgBox.error(TrialExplorerPanel.this, e.getMessage(), "Problem
        // Getting Trials");
        // }
    }

    private void initClientLog(DALClient client) {
        if (Boolean.getBoolean("log_dalclient")) {
            Log log = org.apache.commons.logging.LogFactory.getLog("dalclient");
            log.info("==== Starting log_dalclient ====");
            client.setLog(log);
        }
    }

    private void loadUsingKdxOrXlsFiles(SourceChoice sourceChoice) {
        KdxploreDatabase db = offlineData.getKdxploreDatabase();
        try {
            ImportSourceChoiceDialog dlg = new ImportSourceChoiceDialog(
                    sourceChoice,
                    GuiUtil.getOwnerWindow(TrialExplorerPanel.this),
                    db,
                    messagePrinter,
                    onTrialsLoaded,
                    backgroundRunner);
            dlg.setVisible(true);
        }
        catch (IOException | KdxploreConfigException e) {
            MsgBox.error(TrialExplorerPanel.this, e, "Unable to Load KDX/XLS files");
        }
    }

    /**
     * Invoked when MultiTrialLoader has completed - whether or not it was
     * cancelled.
     * 
     * @param trialLoaderResult
     */
    private void handleTrialDataLoaded(TrialLoaderResult trialLoaderResult) {

        Map<Trial, TrialLoadResult> trialLoadResultByTrial = trialLoaderResult.trialLoadResultByTrial;
        List<String> errors = new ArrayList<>();

        KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();

        for (Trial trial : trialLoadResultByTrial.keySet()) {
            messagePrinter.println("Trial " + trial.getTrialName());

            TrialLoadResult trialLoadResult = trialLoadResultByTrial.get(trial);
            if (trialLoadResult.cause == null) {
                try {
                    messagePrinter.println("==== Load Result for '" + trial.getTrialName() + "' ====");

                    StoreResult storeResult = trialLoadResult.storeChangesInDatabase(kdxdb, autoUpdate);
                    if (! storeResult.warnings.isEmpty()) {
                        messagePrinter.println("--- Warnings ---");
                        for (String warning : storeResult.warnings) {
                            messagePrinter.println(warning);
                        }
                        messagePrinter.println("- - - - - - - - -");
                    }

                    if (storeResult.problems.isEmpty()) {
                        SampleGroup sampleGroup = trialLoadResult.sampleGroup;
                        try {
                            int nSamples = kdxdb.getSampleCount(sampleGroup);
                            messagePrinter.println("Retrieved " + nSamples + " Samples");
                        }
                        catch (IOException e) {
                            messageLogger.w(TAG, "handleTrialDataLoaded: " + trial.getTrialName(), e);

                            messagePrinter.println("No Samples for: " + trial.getTrialName());
                            messagePrinter.println(e.getMessage());
                        }
                    }
                    else {
                        errors.addAll(storeResult.problems);
                    }
                }
                catch (TrialStoreException e1) {
                    for (Pair<com.diversityarrays.daldb.core.Trait, String> pair : trialLoadResult.traitLoadErrors) {
                        com.diversityarrays.daldb.core.Trait dalTrait = pair.first;
                        String errmsg = pair.second;
                        errors.add(trial.getTrialName() + ": " + dalTrait.getTraitName() + ": "
                                + errmsg);
                    }
                }
                catch (IOException | CreateItemException e1) {
                    messageLogger.w(TAG,
                            "Error for '" + trial.getTrialName() + "'",
                            e1);
                    errors.add(trial.getTrialName() + ": " + e1.getMessage());
                }
            }
            else {
                messageLogger.w(TAG,
                        "Error for '" + trial.getTrialName() + "'",
                        trialLoadResult.cause);
                errors.add(trial.getTrialName() + ": " + trialLoadResult.cause.getMessage());

                if (trialLoadResult.hasAnyDownloadErrors()
                        || trialLoadResult.hasAnyDownloadWarnings()) {
                    reportDownloadErrorsAndWarnings(ADD_TRIALS, trialLoadResult);
                }
            }
        }

        trialOverviewPanel.refreshTrialTraits();

        if (!errors.isEmpty()) {
            MsgBox.error(TrialExplorerPanel.this,
                    errors.stream().collect(Collectors.joining("\n")), //$NON-NLS-1$
                    "Problems Loading Trials - Check Messages");
        }
    }

    private void retrieveCurrentlySelectedTrialData() {
        Trial trial = trialOverviewPanel.getSelectedTrial();
        retrieveTrialDataFromServer(trial, false);
    }

    private boolean autoUpdate = TrialLoadResult.AUTO_UPDATE_DATA_TYPE_OR_VAL_RULE;

    private void retrieveTrialDataFromServer(Trial trial, boolean newTrials) {

        if (!autoUpdate) {
            throw new RuntimeException("Not auto-updating not yet supported"); //$NON-NLS-1$
        }

        if (trial == null) {
            messagePrinter
                    .println("INTERNAL ERROR: trial==null in collectTrialInfo"); //$NON-NLS-1$
            // We shouldn't have got here !
            return;
        }

        Closure<TrialLoadResult> onFinish = new Closure<TrialLoadResult>() {
            @Override
            public void execute(TrialLoadResult lr) {
                handleTrialDataLoadResult(REFRESH_TRIAL_INFO, lr);
            }
        };

        collectTrialInfoAfterClientCheck(REFRESH_TRIAL_INFO, trial, onFinish);
    }

    private void handleTrialDataLoadResult(final String fromWhere, final TrialLoadResult lr) {
        if (lr.cause != null) {
            lr.cause.printStackTrace();
            messagePrinter.println(lr.cause.getMessage());
        }
        else {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        handleTrialDataLoadResult(fromWhere, lr);
                    }
                });
                return;
            }

            if (lr.hasAnyDownloadErrors() || lr.hasAnyDownloadWarnings()) {
                reportDownloadErrorsAndWarnings(fromWhere, lr);
                return;
            }

            // FIXME loadTrialData: need to store lr.plots into the database.
            // But this needs to first check if the plots already exist because
            // this method
            // can be called either on the initial Trial load or from
            // "refreshTrialData".
            // The latter could be quite complex unless we just discard all of
            // the
            // Plots. But this could then cause a problem because we might have
            // other records
            // (e.g. Samples) that refer to the plotIds of the extant plots.

            // FIXME loadTrialData: need to resolve the List<core.Trait> against
            // the kdsmart Traits
            // in the Trait table in kdxploreDatabase.

            try {
                storeToDatabase(fromWhere, lr);
            }
            catch (IOException e) {
                MsgBox.error(TrialExplorerPanel.this,
                        e.getMessage(), fromWhere);
            }
            catch (CreateItemException e) {
                MsgBox.error(TrialExplorerPanel.this,
                        e.getMessage(), fromWhere);
            }
            catch (TrialStoreException e) {
                StringBuilder sb = new StringBuilder("<HTML>Trait Load Problems:<ul>");
                for (Pair<com.diversityarrays.daldb.core.Trait, String> pair : lr.traitLoadErrors) {
                    com.diversityarrays.daldb.core.Trait dalTrait = pair.first;

                    sb.append("<li>").append(StringUtil.htmlEscape(dalTrait.getTraitName()))
                            .append("<br>")
                            .append(StringUtil.htmlEscape(pair.second))
                            .append("</li>");
                }
                sb.append("</ul>");

                JLabel msg = new JLabel(sb.toString());
                JScrollPane sp = new JScrollPane(msg);
                JOptionPane.showMessageDialog(TrialExplorerPanel.this, sp,
                        "Trial Load: " + fromWhere, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void storeToDatabase(final String fromWhere, final TrialLoadResult lr)
            throws IOException, CreateItemException, TrialStoreException {
        KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();

        // TODO think about not saving these Traits in the
        // storeChangesInDatabase method
        // but confirm with the user after showing the impact of the changes.

        StoreResult storeResult = lr.storeChangesInDatabase(kdxdb, autoUpdate);
        if (storeResult.problems.isEmpty()) {
            if (!storeResult.whatChangedByKdsTrait.isEmpty()) {
                messagePrinter
                        .println("Traits changed: " + storeResult.whatChangedByKdsTrait.size());
                for (Trait trait : storeResult.whatChangedByKdsTrait.keySet()) {
                    messagePrinter.println(trait.getTraitName() + ": "
                            + storeResult.whatChangedByKdsTrait.get(trait).toString());
                }
            }

            offlineData.trialDataUpdated(lr.trial, lr.sampleGroup);

            int nTrialUnits = offlineData.getPlotCount(lr.trial.getTrialId());

            messagePrinter.println(Msg.MSG_RETRIEVED_N_PLOTS(nTrialUnits));
            if (nTrialUnits <= 0) {
                MsgBox.error(
                        TrialExplorerPanel.this,
                        getTrialPromptMessage(
                                null,
                                Msg.MSG_NO_PLOTS_IN_DATABASE()),
                        fromWhere);
            }
        }
        else {
            String msg = storeResult.problems.stream()
                    .collect(Collectors.joining("\n", "Problems encountered while saving", ""));
            MsgBox.error(TrialExplorerPanel.this, msg, fromWhere);
        }
    }



    private void reportDownloadErrorsAndWarnings(String fromWhere, TrialLoadResult lr) {
        String report = TrialLoadResult.collectHtmlReport(lr);
        JLabel msg = new JLabel(report);
        JScrollPane sp = new JScrollPane(msg);
        JOptionPane.showMessageDialog(TrialExplorerPanel.this, sp,
                "Trial Load: " + fromWhere, JOptionPane.ERROR_MESSAGE);
    }

    private Action refreshTrialInfoAction = new AbstractAction(
            REFRESH_TRIAL_INFO) {
        @Override
        public void actionPerformed(ActionEvent e) {
            retrieveCurrentlySelectedTrialData();
        }
    };

    private final MessagePrinter messagePrinter;

    private final DALClientProvider clientProvider;

    private TrialChangeListener trialChangeListener = new TrialChangeListener() {

        @Override
        public void entityAdded(KDSmartDatabase db, Trial t) {
        }

        @Override
        public void entityChanged(KDSmartDatabase db, Trial trial) {
            System.out.println("TrialExplorerPanel.trialChanged: " + trial.getTrialName());
            // FIXME check if we need to refresh something
        }

        @Override
        public void entitiesRemoved(KDSmartDatabase db, Set<Integer> ids) {
            try {
                if (offlineData.getTrials().isEmpty()) {
                    currentTrialCardLayout.show(currentTrialCardPanel,
                            CARD_NO_TRIALS_LOADED);
                }
            }
            catch (IOException e) {
                MsgBox.error(TrialExplorerPanel.this, e.getMessage(),
                        "Problem Getting Trials");
            }
        }

        @Override
        public void listChanged(KDSmartDatabase db, int nChanges) {
            // FIXME check if our Trial is still in the list
        }
    };

    private final OfflineDataChangeListener offlineDataChangeListener = new OfflineDataChangeListener() {

        @Override
        public void trialUnitsAdded(Object source, int trialId) {
            Trial selectedTrial = trialOverviewPanel.getSelectedTrial();

            if (selectedTrial != null
                    && selectedTrial.getTrialId() == trialId) {
                updateCurrentTrial();
            }
        }

        @Override
        public void offlineDataChanged(Object source, String reason,
                KdxploreDatabase oldDb, KdxploreDatabase newDb) {
            if (oldDb != null) {
                oldDb.removeEntityChangeListener(trialChangeListener);
            }
            if (newDb != null) {
                newDb.addEntityChangeListener(trialChangeListener);

                try {
                    databaseDeviceIdentifier = newDb.getDatabaseDeviceIdentifier();

//                    if (!KdxploreConfig.getInstance().isCIMMYTmode()) {
//                        addTrialsAction.setEnabled(true);
//                    }

                    if (offlineData.getTrials().isEmpty()) {
                        currentTrialCardLayout.show(currentTrialCardPanel,
                                CARD_NO_TRIALS_LOADED);
                    }
                    else {
                        if (trialDetailsPanel.getCurrentTrial() == null) {
                            currentTrialCardLayout.show(currentTrialCardPanel,
                                    CARD_NO_TRIAL_SELECTED);
                        }
                        else {
                            currentTrialCardLayout.show(currentTrialCardPanel,
                                    CARD_TRIAL);
                        }
                    }
                }
                catch (IOException | KdxploreConfigException e) {
                    MsgBox.error(TrialExplorerPanel.this, e.getMessage(),
                            "Problem Getting Trials");
                }
            }
        }

    };

    private final TrialOverviewPanel trialOverviewPanel;

    private final File userDataFolder;

    private final DartSchemaHelper dartSchemaHelper = new DartSchemaHelper(
            KDDartEntityFactory.Util.getInstance().newCoreSchema());

    private KdxploreDatabaseStub deviceInfoProvider = new KdxploreDatabaseStub() {
        @Override
        public void saveSampleGroup(SampleGroup sampleGroup) throws IOException {
            offlineData.getKdxploreDatabase().saveSampleGroup(sampleGroup);
        }

        @Override
        public Sample createSample(SampleGroup sampleGroup) {
            KdxSample sample = new KdxSample();
            sample.setSampleGroupId(sampleGroup.getSampleGroupId());
            return sample;
        }

        @Override
        public List<DeviceIdentifier> getDeviceIdentifiers() throws IOException {
            return offlineData.getKdxploreDatabase().getDeviceIdentifiers();
        }

        @Override
        public void saveDeviceIdentifier(DeviceIdentifier deviceIdentifier) throws IOException {
            offlineData.getKdxploreDatabase().saveDeviceIdentifier(deviceIdentifier);
        }
    };

    private final Closure<List<Trial>> onTrialsLoaded = new Closure<List<Trial>>() {
        @Override
        public void execute(List<Trial> newTrials) {
            trialOverviewPanel.handleTrialsLoaded(newTrials);
            trialsChangedConsumer.accept(null);
            // trialDetailsPanel.handleTrialsLoaded(newTrials);
        }
    };

    private final FileDrop fileDrop = new FileDrop() {
        @Override
        public void dropFiles(Component component, List<File> files,
                DropLocationInfo dli) {

            Closure<String> reportError = new Closure<String>() {
                @Override
                public void execute(String s) {
                    messagePrinter.println(s);
                }

            };

            Map<ImportType, ImportFileGroup> groupByType = FileImportTableModel.classifyFiles(files,
                    reportError);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    doImportUsing(groupByType);
                }
            });
        }
    };

    private void doImportUsing(Map<ImportType, ImportFileGroup> groupByType) {

        ImportFileGroup importFileGroup = null;
        switch (groupByType.size()) {
        case 0:
            MsgBox.warn(TrialExplorerPanel.this, "None of the file types are supported",
                    "Import Trial");
            return;
        case 1:
            for (ImportFileGroup g : groupByType.values()) {
                importFileGroup = g;
                break;
            }
            break;
        default:
            String msg = groupByType.keySet().stream()
                    .map(ImportType::name).collect(Collectors.joining(",",
                            "Only a single file type at a time:\n", ""));
            MsgBox.warn(TrialExplorerPanel.this, msg, "Import Trial");
            return;
        }

        // Should only be one now !
        String errmsg = null;
        switch (importFileGroup.importType) {
        case KDX:
            errmsg = doImportUsing(SourceChoice.KDX, importFileGroup);
            break;
        case CSV:
            errmsg = doImportUsing(SourceChoice.CSV, importFileGroup);
            break;
        case BMS_EXCEL:
        case KDXPLORE_EXCEL:
            errmsg = doImportUsing(SourceChoice.XLS, importFileGroup);
            break;
        default:
            errmsg = "Use .CSV, .XLS or .KDX files (but not all at once)";
            break;
        }

        if (errmsg != null) {
            MsgBox.error(this,
                    errmsg,
                    "Import Files");
        }
    }

    private String doImportUsing(SourceChoice sourceChoice, ImportFileGroup importFileGroup) {

        if (sourceChoice != null) {
            switch (sourceChoice) {
            case CSV:
                List<File> csvFiles = importFileGroup.files;
                if (csvFiles.size() > 1) {
                    return "Please use 1 CSV file at a time";
                }

                final File csvFile = csvFiles.get(0);
                try {
                    String trialName = Util.extractTrialNameFromFileName(csvFile.getName());

                    KDSmartDatabase kdsdb = offlineData.getKdxploreDatabase()
                            .getKDXploreKSmartDatabase();
                    Trial trial = kdsdb.findTrialByName(trialName);
                    if (trial != null) {
                        return "Trial already exists with the name:\n" + trialName;
                    }
                }
                catch (IOException e) {
                    return "Error: " + e.getMessage();
                }

                TraitNameStyle tns = getDefaultTraitNameStyle();

                ImportTrialCsvDialog dlg = new ImportTrialCsvDialog(
                        GuiUtil.getOwnerWindow(TrialExplorerPanel.this),
                        databaseDeviceIdentifier,
                        tns,
                        deviceInfoProvider,
                        offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase(),
                        trialsChangedConsumer);

                dlg.setImportFile(csvFile);
                dlg.setVisible(true);
                break;

            case KDX:
            case XLS:
                try {
                    ImportSourceChoiceDialog iscdlg = new ImportSourceChoiceDialog(
                            sourceChoice,
                            GuiUtil.getOwnerWindow(TrialExplorerPanel.this),
                            offlineData.getKdxploreDatabase(),
                            messagePrinter,
                            onTrialsLoaded,
                            backgroundRunner);
                    iscdlg.addFiles(importFileGroup.files);
                    iscdlg.setVisible(true);
                }
                catch (IOException | KdxploreConfigException e) {
                    MsgBox.error(TrialExplorerPanel.this, e, "Unable to Load files");
                    return null;
                }
                break;

            case DATABASE:
            default:
                break;
            }
        }
        return null;
    }

    private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

    private final WindowOpener<JFrame> windowOpener;

    private final ActionListener trialOverviewActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (TrialOverviewPanel.EDIT_TRIAL_COMMAND.equals(e.getActionCommand())) {
                Trial trial = trialOverviewPanel.getSelectedTrial();
                if (trial != null) {
                    editTrialAction.actionPerformed(null);
                }
            }
        }
    };

    private final PrintStreamMessageLogger messageLogger;

    static private final String CARD_TRIAL = "CARD_TRIAL";
    static private final String CARD_NO_TRIALS_LOADED = "CARD_NO_TRIALS_LOADED";
    static private final String CARD_NO_TRIAL_SELECTED = "CARD_NO_TRIAL_SELECTED";
    private static final String CARD_ERRORS_GETTING_TRIAL_DATA = "CARD_TRIAL_DATA_ERRORS";

    private final CardLayout currentTrialCardLayout = new CardLayout();
    private final JPanel currentTrialCardPanel = new JPanel(
            currentTrialCardLayout);

    private final JLabel errorsGettingTrialData = new JLabel();

    private final TrialDetailsPanel trialDetailsPanel;
    private final BackgroundRunner backgroundRunner;
    private final OfflineData offlineData;

    private final ClientUrlChanger clientUrlChanger;

    private final Closure<List<Trial>> onTrialSelected = new Closure<List<Trial>>() {
        @Override
        public void execute(List<Trial> trials) {
            if (trials.isEmpty()) {
                // trialDetailsPanel.setSelectedTrial(null);
                removeTrialAction.setEnabled(false);
            }
            else {
                // trialDetailsPanel.setSelectedTrial(trials.get(0));
                removeTrialAction.setEnabled(true);
            }
        }
    };
    
    private final TrialExplorerManager trialExplorerManager;

    private final DriverType driverType;

    private DeviceIdentifier databaseDeviceIdentifier;

    private Transformer<Trial, Boolean> checkIfEditorActive = new Transformer<Trial, Boolean>() {
        @Override
        public Boolean transform(Trial trial) {
            JFrame frame = windowOpener.getWindowByIdentifier(trial.getTrialId());
            return frame != null;
        }
    };

    private final KDXDeviceService kdxDeviceService;

    private final Consumer<Void> trialsChangedConsumer;
    private final Consumer<Collection<Trait>> traitRemovalHandler;

    private final KdxApp kdxApp;

    private final KdxPluginInfo pluginInfo;
    
    private final Consumer<Trial> onTraitInstancesRemoved = new Consumer<Trial>() {
        @Override
        public void accept(Trial t) {
            trialOverviewPanel.refreshTrialTraits();
        }
    };

    public TrialExplorerPanel(
            KdxApp app,
            KdxPluginInfo pluginInfo,
            KDXDeviceService deviceService,
            TrialExplorerManager manager,
            OfflineData offlineData,
            DriverType dType,
            ImageIcon barcodeIcon,
            ClientUrlChanger clientUrlChanger,
            Consumer<Void> trialsChangedConsumer, 
            Consumer<Collection<Trait>> traitRemovalHandler)
    {
        super(new BorderLayout());

        this.kdxApp = app;
        this.messagePrinter = pluginInfo.getMessagePrinter();
        this.messageLogger = pluginInfo.getMessageLogger();
        this.windowOpener = pluginInfo.getWindowOpener();
        this.backgroundRunner = pluginInfo.getBackgroundRunner();
        this.clientProvider = pluginInfo.getClientProvider();
        this.userDataFolder = pluginInfo.getUserDataFolder();

        this.kdxDeviceService = deviceService;
        this.trialsChangedConsumer = trialsChangedConsumer;
        this.traitRemovalHandler = traitRemovalHandler;

        Predicate<SeedPrepHarvestService> onHarvestFound = new Predicate<SeedPrepHarvestService>() {
            @Override
            public boolean test(SeedPrepHarvestService t) {
                seedPrepHarvestService = t;
                return false;
            }
        };

        Shared.detectServices(SeedPrepHarvestService.class, onHarvestFound,
                SEEDPREP_HARVEST_SERVICE_IMPL_CLASSNAME);
        if (this.seedPrepHarvestService != null) {
            PreferenceCollection pc = seedPrepHarvestService.getPreferenceCollection(kdxApp);
            if (pc != null) {
                KdxplorePreferences.getInstance().addPreferenceCollection(pc);
            }
        }

        PreferenceCollection pc = BarcodePreferences.getInstance().getPreferenceCollection(app,
                "Barcode");
        if (pc != null) {
            KdxplorePreferences.getInstance().addPreferenceCollection(pc);
        }

        this.trialExplorerManager = manager;
        this.clientUrlChanger = clientUrlChanger;
        this.driverType = dType;
        this.offlineData = offlineData;
        this.pluginInfo = pluginInfo;

        this.trialOverviewPanel = new TrialOverviewPanel("Trials Available",
                offlineData,
                trialExplorerManager,
                flth,
                messagePrinter,
                onTrialSelected);

        this.trialDetailsPanel = new TrialDetailsPanel(windowOpener,
                messagePrinter,
                backgroundRunner,
                offlineData, editTrialAction, seedPrepAction, harvestAction, uploadTrialAction,
                refreshTrialInfoAction,
                barcodeIcon,
                checkIfEditorActive,
                onTraitInstancesRemoved);

//        addTrialsAction.setEnabled(false);

        currentTrialCardPanel.add( // NOTE: the null introduces a spacer
                new JustLabelPanel(
                        new String[] { Msg.HTML_NO_TRIALS_LOADED() },
                        addTrialsAction),
                CARD_NO_TRIALS_LOADED);
        currentTrialCardPanel.add(new JustLabelPanel(
                new String[] { Msg.HTML_NO_TRIAL_SELECTED() }),
                CARD_NO_TRIAL_SELECTED);
        currentTrialCardPanel.add(trialDetailsPanel, CARD_TRIAL);
        currentTrialCardPanel.add(errorsGettingTrialData,
                CARD_ERRORS_GETTING_TRIAL_DATA);

        offlineData.addOfflineDataChangeListener(offlineDataChangeListener);

        // KDClientUtils.initAction(ImageId.EXPAND_ALL, expandAllAction,
        // "Expand All");
        // KDClientUtils.initAction(ImageId.COLLAPSE_ALL, collapseAllAction,
        // "Collapse All");

        KDClientUtils.initAction(ImageId.TRASH_24, removeTrialAction,
                "<HTML>Remove Trial from Offline storage<BR>(Shift-Click to also remove Traits)"); // TODO i18n
        removeTrialAction.setEnabled(false);

        KDClientUtils.initAction(ImageId.ADD_TRIALS_24, addTrialsAction,
                ADD_TRIALS);
        addDatabaseTrialsButton = new JButton(addTrialsAction);

        KDClientUtils.initAction(ImageId.EDIT_BLUE_24, editTrialAction,
                "Edit current Trial"); // TODO i18n
        editTrialAction.setEnabled(false);

        KDClientUtils.initAction(ImageId.UPLOAD_24, uploadTrialAction,
                "Store Trial in Database"); // TODO i18n
        uploadTrialAction.setEnabled(false);

        KDClientUtils.initAction(ImageId.GET_TRIALINFO_24,
                refreshTrialInfoAction, REFRESH_TRIAL_INFO + " from Database"); // TODO
                                                                                // i18n
        refreshTrialInfoAction.setEnabled(false);

        KdxploreConfig config = KdxploreConfig.getInstance();

        KDClientUtils.initAction(ImageId.SEED_PREP_24, seedPrepAction,
                "Seed Preparation Wizard" + AbstractMsg.BETA_SUFFIX); // TODO
                                                                      // i18n
        seedPrepAction.setEnabled(0 != (CONFIG_FLAG_SEEDPREP & config.getFlags()));

        KDClientUtils.initAction(ImageId.HARVEST_WHEAT_24, harvestAction,
                "Harvest Wizard" + AbstractMsg.BETA_SUFFIX); // TODO i18n
        harvestAction.setEnabled(0 != (CONFIG_FLAG_HARVEST & config.getFlags()));

        @SuppressWarnings("rawtypes")
        DartEntityFeature[] descriptors = DartEntityBeanRegistry.TRIAL_BEAN_INFO
                .findDescriptors(Trial.COLNAME_TRIAL_NAME,
                        Trial.COLNAME_TRIAL_ACRONYM,
                        Trial.COLNAME_ORGANISM_TYPE, Trial.COLNAME_TRIAL_LAYOUT);

        // trialOverviewPanel.initialiseStructure(descriptors);

        trialOverviewPanel.setTransferHandler(flth);

        trialOverviewPanel.addActionListener(trialOverviewActionListener);

        trialOverviewPanel
                .addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            updateCurrentTrial();
                        }
                    }
                });

        // trialListSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        // new JScrollPane(trialOverviewPanel),
        // currentTrialCardPanel);
        // trialListSplit.setResizeWeight(0.7);
        // trialListSplit.setOneTouchExpandable(true);

        Box leftButtons = Box.createHorizontalBox();
        leftButtons.add(addDatabaseTrialsButton);

        leftButtons.add(Box.createHorizontalGlue());

        leftButtons.add(new JButton(removeTrialAction));

        // TODO enable this after fixing the
        // use of SampleGroup in the wizard
        // importCsvAction.setEnabled(false);
        // leftButtons.add(new JButton(importCsvAction));
        // leftButtons.add(Box.createHorizontalGlue());

        JPanel left = new JPanel(new BorderLayout());
        left.add(leftButtons, BorderLayout.NORTH);
        left.add(trialOverviewPanel, BorderLayout.CENTER);

        // trialTable.setDefaultRenderer(Integer.class, new
        // NumberCellRenderer());
        // trialTable.setDefaultRenderer(String.class, new
        // StringCellRenderer(trialTableModel));

        lrSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left,
                createCurrentTrialPanel());
        lrSplitPane.setResizeWeight(0.5);
        lrSplitPane.setOneTouchExpandable(true);

        add(lrSplitPane, BorderLayout.CENTER);

        // GuiUtil.setVisibleRowCount(todoTable, 6);

    }

    private void doUploadTrial(Trial trial) {
        if (KdxploreConfig.getInstance().getModeList().contains("CIMMYT")) {
            MsgBox.info(TrialExplorerPanel.this, "Disabled until SIU is ready", UPLOAD_TRIAL);
        }
        else {
            
        	Exception error = null;
        	Optional<Optional<GeneralType>> opt_opt_mmt = Optional.empty();
        	try {
	            opt_opt_mmt = MediaSampleRetriever.getCuratedSamplesMultimediaType(
	                    TrialExplorerPanel.this, offlineData.getKddartReferenceData());	            
        	}
        	catch (MultimediaSourceMissingException e) {
        		error = e;
        	}

        	if (! opt_opt_mmt.isPresent()) {
        		MediaSampleRetriever.showMissingMultimedia(TrialExplorerPanel.this, error, "Can't upload");
                return;
            }
            
            TrialUploadHandler trialUploadHandler = new TrialUploadHandler(
                    offlineData.getKdxploreDatabase(), clientProvider, dartSchemaHelper, opt_opt_mmt.get());
            KddartUploadResult<com.diversityarrays.daldb.core.Trial> result = 
                    trialUploadHandler.uploadTrial(trial);

            JOptionPane.showMessageDialog(this, result.getMessage());

            System.out.println(result.getErrors());
            System.out.println(result.getMessage());
        }
    }

    private final SamplesSavedListener samplesSavedListener = new SamplesSavedListener() {

        @Override
        public void samplesSaved(Object source, Trial trial, int[] sampleGroupIds,
                Plot[] changedPlots) {
            trialDetailsPanel.updateTrial(trial, sampleGroupIds, changedPlots);
        }

    };

    private void doEditTrial(Trial trial) {

        final String msgTitle = EDIT_TRIAL + ": " + trial.getTrialName();

        // TODO detect when multiple found and ignore all but the first
        Predicate<TrialDataEditorService> onServiceFound = new Predicate<TrialDataEditorService>() {
            @Override
            public boolean test(TrialDataEditorService service) {
                handleServiceDetected(msgTitle, trial, service);
                return false; // we only want the first
            }
        };

        if (0 == Shared.detectServices(TrialDataEditorService.class, onServiceFound,
                "com.diversityarrays.kdxplore.curate.TrialDataEditorServiceImpl")) // Class
                                                                                   // Name
                                                                                   // is
                                                                                   // for
                                                                                   // developer
                                                                                   // debugging
        {
            MsgBox.error(TrialExplorerPanel.this, "No TrialDataEditorService", msgTitle);
        }
    }

    private void handleServiceDetected(String msgTitle, Trial trial,
            TrialDataEditorService service) {
        CurationParams params = new CurationParams();
        params.title = msgTitle;
        params.trial = trial;
        params.offlineData = offlineData;
        params.messageLogger = messageLogger;
        params.component = TrialExplorerPanel.this;
        params.windowOpener = windowOpener;

        long start = System.currentTimeMillis();

        Consumer<Either<InitError, TrialDataEditorResult>> onComplete = new Consumer<Either<InitError, TrialDataEditorResult>>() {
            @Override
            public void accept(Either<InitError, TrialDataEditorResult> either) {

                long elapsed = System.currentTimeMillis() - start;
                Shared.Log.i(TAG, "doEditTrial.generateResult: Time to load trialData for trialId#"
                        + params.trial.getTrialId() + "=" + elapsed + " ms");

                if (either.isRight()) {
                    TrialDataEditorResult result = either.right();

                    result.curationData.addSamplesSavedListener(samplesSavedListener);

                    result.frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            result.frame.removeWindowListener(this);

                            result.curationData.removeSamplesSavedListener(samplesSavedListener);

                            Window ownerWindow = GuiUtil.getOwnerWindow(TrialExplorerPanel.this);
                            if (ownerWindow != null) {
                                ownerWindow.toFront();
                                ownerWindow.repaint();
                            }
                        }
                    });

                    GuiUtil.ensureMaximized(result.frame);
                }
                else {
                    InitError initError = either.left();
                    if (initError.throwable == null) {
                        messageLogger.e(TAG, params.title);
                        MsgBox.error(TrialExplorerPanel.this,
                                initError.message,
                                trial.getTrialName() + ": Unable to create editor");
                    }
                    else {
                        Throwable error = initError.throwable;
                        messageLogger.e(TAG, params.title, error);
                        MsgBox.error(TrialExplorerPanel.this,
                                error.getMessage(),
                                trial.getTrialName() + ": Unable to create editor");
                    }
                }
            }
        };
        service.createUserInterface(backgroundRunner, params, onComplete);
    }

    private static final boolean WILL_CANCEL_PENDING = true;
    private static final boolean WILL_NOT_CANCEL_PENDING = false;

    /**
     * Called because the user changed the client's URL by logging in to a
     * different database.
     * 
     * @param why
     * @param client
     * @param onLoadComplete
     */
    private void loadDatabaseDataFromKddart(
            final String prefix,
            UrlChangeConfirmation confirmation,
            final DALClient client, final Closure<DALClient> onLoadComplete) {
        String why = prefix + confirmation.reason;

        String dbfilename = KdxploreDatabase.LOCAL_DATABASE_URL;
        switch (confirmation) {
        case NEW_DATABASE:
            dbfilename = KdxploreDatabase.LOCAL_DATABASE_URL;
            break;
        case CHANGE_APPROVED:
        case NO_CHANGE_SAME_URL:
            dbfilename = DatabaseDataUtils.normaliseUrlForFilename(client.getBaseUrl());
            break;
        case CHANGE_DENIED:
            throw new IllegalStateException("loadDatabaseDataFromKddart with " + confirmation);
        }

        final File dbdataDirectory = new File(userDataFolder, dbfilename);

        Closure<Throwable> onFailure = new Closure<Throwable>() {
            @Override
            public void execute(Throwable t) {
                messagePrinter.println("Problem loading database from: "
                        + dbdataDirectory.getPath());
                messagePrinter.println(t.getMessage());

                MsgBox.error(TrialExplorerPanel.this, t, why);
                onLoadComplete.execute(null);
            }
        };

        Closure<DALClient> onSuccess = new Closure<DALClient>() {
            @Override
            public void execute(DALClient dalClient) {
                onLoadComplete.execute(dalClient);
            }
        };

        offlineData.loadDatabaseDataFromKddart(
                "Loading Reference Data from KDDart", driverType, dbdataDirectory, client,
                messageLogger, backgroundRunner, onFailure, onSuccess);
    }

    public JButton getAddDatabaseTrialsButton() {
        return addDatabaseTrialsButton;
    }

    public void doPostOpenOperations() {
        // if (todoTableModel.getRowCount()>0) {
        // todoTaskPane.setCollapsed(false);
        // }

        // trialListSplit.setDividerLocation(0.7);
        // lrSplitPane.setDividerLocation(0.5);
    }

    private void updateCurrentTrial() {

        Trial trial = trialOverviewPanel.getSelectedTrial();
        if (trial == null) {
            if (trialOverviewPanel.hasNoTrials()) {
                currentTrialCardLayout.show(currentTrialCardPanel,
                        CARD_NO_TRIALS_LOADED);
            }
            else {
                currentTrialCardLayout.show(currentTrialCardPanel,
                        CARD_NO_TRIAL_SELECTED);
            }
        }
        else {
            trialDetailsPanel.setSelectedTrial(trial);
            currentTrialCardLayout.show(currentTrialCardPanel, CARD_TRIAL);
        }
    }

    private Component createCurrentTrialPanel() {

        JPanel result = new JPanel(new BorderLayout());
        result.add(GuiUtil.createLabelSeparator("Trial Details"),
                BorderLayout.NORTH);
        result.add(currentTrialCardPanel, BorderLayout.CENTER);

        return result;
    }

    private void collectTrialInfoAfterClientCheck(
            final String doWhat,
            Trial trial,
            final Closure<TrialLoadResult> onFinish) {
        DALClient client = null;
        clientUrlChanger.setIgnoreClientUrlChanged(true);

        client = clientProvider.getDALClient(doWhat);
        if (client == null) {
            clientUrlChanger.setIgnoreClientUrlChanged(false);
            return; // user cancelled
        }

        final UrlChangeConfirmation confirm = confirmChangedClientIsOk(client,
                doWhat, WILL_CANCEL_PENDING);

        switch (confirm) {
        case CHANGE_DENIED:
            // User said "no"
            clientUrlChanger.setIgnoreClientUrlChanged(false);
            return;

        case CHANGE_APPROVED:
        case NEW_DATABASE:
            // Database changed - so we can't collect the TrialInfo because
            // the trial is from a different database !
            Closure<DALClient> onLoadComplete = new Closure<DALClient>() {
                @Override
                public void execute(DALClient dalClient) {
                    clientUrlChanger.setIgnoreClientUrlChanged(false);
                    clientUrlChanger.clientUrlChanged();

                    // Note: use of WILL_CANCEL_PENDING parameter above
                    // has already informed use of the Trial load cancel.
                    // MsgBox.info(
                    // TrialExplorerPanel.this,
                    // "Database was changed so Trial Information retrieval was
                    // cancelled",
                    // REFRESH_TRIAL_INFO);
                }
            };

            loadDatabaseDataFromKddart(doWhat, confirm, client, onLoadComplete);
            break;

        case NO_CHANGE_SAME_URL:
            // Database not changed.
            clientUrlChanger.setIgnoreClientUrlChanged(false);
            try {
                collectTrialInfo(client, trial, onFinish);
            }
            catch (KdxploreConfigException | IOException e) {
                MsgBox.error(
                        TrialExplorerPanel.this,
                        e,
                        REFRESH_TRIAL_INFO);
            }
            break;

        default:
            throw new RuntimeException("Unhandled value " + confirm);
        }
    }

    static private enum UrlChangeConfirmation {
        NEW_DATABASE("New Database"), NO_CHANGE_SAME_URL("Same URL"), CHANGE_DENIED(
                "Denied"), CHANGE_APPROVED("Database Change Approved");

        public final String reason;

        UrlChangeConfirmation(String reason) {
            this.reason = reason;
        }
    }

    /**
     * If we don't have current offline data, just continue. If we do, then did
     * the client change URL to one different to that of the offline data. If it
     * changed, check if that's what the user wanted to do. Because if it is
     * then we have to clear out the current offline data.
     * 
     * @param client
     * @param doWhat
     * @param willCancelPending
     * @return
     */
    private UrlChangeConfirmation confirmChangedClientIsOk(DALClient client,
            String doWhat, boolean willCancelPending) {

        UrlChangeConfirmation result;

        String databaseUrl = offlineData.getDatabaseUrl();
        if (databaseUrl == null || KdxploreDatabase.LOCAL_DATABASE_URL.equals(databaseUrl)) {
            result = UrlChangeConfirmation.NEW_DATABASE;
        }
        else {
            String baseUrl = client.getBaseUrl();

            if (baseUrl.equalsIgnoreCase(databaseUrl)) {
                result = UrlChangeConfirmation.NO_CHANGE_SAME_URL;
            }
            else {

                StringBuilder sb = new StringBuilder();
                sb.append("Old database: ")
                        .append(databaseUrl)
                        .append("\nNew database: ")
                        .append(baseUrl)
                        .append("\nAnswering YES will load different Offline data.");

                if (willCancelPending) {
                    sb.append("\n(and cancel your pending command to " + doWhat
                            + ")");
                }

                sb.append("\nDo you still wish to proceed?");

                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                        TrialExplorerPanel.this, sb.toString(),
                        "Database URL changed", JOptionPane.YES_NO_OPTION)) {
                    result = UrlChangeConfirmation.CHANGE_APPROVED;
                }
                else {
                    clientProvider.logout();
                    result = UrlChangeConfirmation.CHANGE_DENIED;
                }
            }
        }
        return result;
    }

    private void collectTrialInfo(
            final DALClient client,
            final Trial trial,
            final Closure<TrialLoadResult> onFinish)
                    throws KdxploreConfigException, IOException {

        Closure<Throwable> errorConsumer = new Closure<Throwable>() {
            @Override
            public void execute(Throwable cause) {

                String errmsg = cause.getMessage();

                Object optionMessage;

                if (cause instanceof DalException) {
                    Either<String, DalException> check = TrialLoadResult.checkPermissionDenied((DalException) cause);
                    if (check.isRight()) {
                        DalException dalException = check.right();
                        if (dalException instanceof DalResponseHttpException) {
                            DalResponseHttpException he = (DalResponseHttpException) dalException;

                            for (DalHeader h : he.responseInfo.headers) {
                                if ("content-type".equalsIgnoreCase(h.getName())) {
                                    String contentType = h.getValue();
                                    if (contentType.startsWith("text/html")) {
                                        String html = he.responseInfo.serverResponse;
                                        if (html != null && !html.isEmpty()) {
                                            String plainText = net.pearcan.util.Util
                                                    .htmlToPlainText(html);
                                            if (plainText != null) {
                                                errmsg = plainText;
                                            }

                                            if (!html.startsWith("<HTML>")
                                                    && !html.startsWith("<html>")) {
                                                html = "<HTML>" + html;
                                            }
                                            optionMessage = new JLabel(html);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        optionMessage = check.left();
                    }
                }
                else {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    cause.printStackTrace(pw);
                    pw.close();
                    optionMessage = sw.toString();
                }

                trialOverviewPanel.setPayloadErrorMessage(trial, errmsg);
                messagePrinter.println(errmsg);

                MsgBox.error(TrialExplorerPanel.this,
                        cause,
                        "Unable to get data for '" + trial.getTrialName() + "'");
            }
        };

        boolean wantSpecimens = getSpecimensRequired();

        Date today = new Date();
        offlineData.collectTrialInfo(client, backgroundRunner,
                dartSchemaHelper, wantSpecimens,
                errorConsumer, messageLogger, trial, today,
                onFinish);
    }

    static public String getTrialPromptMessage(Trial trial, String base) {
        if (trial == null) {
            return base;
        }
        return base + "\nTrial Id: " + trial.getTrialId() + "\nTrial Name: "
                + trial.getTrialName();
    }

    public int getTrialCount() {
        return trialOverviewPanel.getTrialCount();
    }

    public boolean isEditorActiveForTrial(Trial trial) {
        return checkIfEditorActive.transform(trial);
    }

    public void setTraitExplorer(TraitExplorer traitExplorer) {
        trialOverviewPanel.setTraitExplorer(traitExplorer);
    }

    /**
     * @param offlineData
     * 
     */
    public void initialiseUploadHandler(OfflineData offlineData) {
//        trialUploadHandler = new TrialUploadHandler(offlineData, clientProvider, dartSchemaHelper);
    }

}
