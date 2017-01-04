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
package com.diversityarrays.kdxplore.trialmgr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.daldb.core.SystemUser;
import com.diversityarrays.kdsmart.KDSmartApplication;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.TrialChangeListener;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.data.DatabaseDataLoadResult;
import com.diversityarrays.kdxplore.data.DatabaseDataStub;
import com.diversityarrays.kdxplore.data.DatabaseDataUtils;
import com.diversityarrays.kdxplore.data.DatabaseLocation;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.OfflineDataChangeListener;
import com.diversityarrays.kdxplore.prefs.ExplorerProperties;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.prefs.PreferenceCollection;
import com.diversityarrays.kdxplore.services.AppInitContext;
import com.diversityarrays.kdxplore.services.KdxApp;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.kdxplore.trialmgr.trait.TagExplorerPanel;
import com.diversityarrays.kdxplore.trialmgr.trait.TraitExplorerPanel;
import com.diversityarrays.kdxplore.trials.TrialExplorerPanel;
import com.diversityarrays.ormlite.jdbc.DriverType;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.DALClientProvider;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.MessagePrinter;

public class TrialManagerPanel extends JPanel implements TrialExplorerManager, KdxApp {

	private static final String TAB_TRAITS = Msg.TAB_TRAITS();
	private static final String TAB_TRIALS = Msg.TAB_TRIALS();
	private static final String TAB_TAGS = Msg.TAB_TAGS();

	private static final String TAG = TrialManagerPanel.class.getSimpleName();

	private final JLabel databaseUrlLabel = new JLabel();
	
	private final BackgroundRunner backgroundRunner;

	private final OfflineData offlineData;
	
	private TrialChangeListener trialChangeListener = new TrialChangeListener() {

		@Override
		public void entityAdded(KDSmartDatabase db, Trial trial) {
			StringBuilder sb = new StringBuilder(" ++ "); //$NON-NLS-1$
			sb.append(trial.getTrialName());
			String a = trial.getTrialAcronym();
			if (a != null && ! a.isEmpty()) {
				sb.append(": ").append(a); //$NON-NLS-1$
			}
			messagesPanel.println(sb.toString());
		}

		@Override
		public void entityChanged(KDSmartDatabase db, Trial trial) { }

		@Override
		public void entitiesRemoved(KDSmartDatabase db, Set<Integer> ids) { }

		@Override
		public void listChanged(KDSmartDatabase db, int nChanges) { }
	};
	
	private final OfflineDataChangeListener offlineDataChangeListener = new OfflineDataChangeListener() {

		@Override
		public void trialUnitsAdded(Object source, int trialId) { }

		@Override
		public void offlineDataChanged(Object source, String reason, KdxploreDatabase oldDb, KdxploreDatabase newDb) {
			
			if (oldDb != null) {
				oldDb.removeEntityChangeListener(trialChangeListener);
			}
			if  (newDb != null) {
				newDb.addEntityChangeListener(trialChangeListener);
			}
			if (newDb != null) {
	            SystemUser loginUser = offlineData.getKddartReferenceData().getLoginUser();
	            String username = loginUser == null ? null : loginUser.getUserName();

	            String databaseUrl = offlineData.getDatabaseUrl();

	            try {
	                if (KdxploreDatabase.LOCAL_DATABASE_URL.equalsIgnoreCase(databaseUrl)) {
	                    clientProvider.setInitialClientUrl(null);
	                }
	                else {
	                    clientProvider.setInitialClientUrl(databaseUrl);
	                    explorerProperties.setCurrentDatabaseUrl(databaseUrl);
	                }

	                clientProvider.setInitialClientUsername(username);

	                explorerProperties.setCurrentDatabaseUsername(username);
	                
	                explorerProperties.save("switched offline data"); //$NON-NLS-1$
	            } catch (IOException e) {
	                e.printStackTrace();
	                messagesPanel.println("Unable to persist database change:"); //$NON-NLS-1$
	                messagesPanel.println(e.getMessage());
	            }
			}

			updateDatabaseUrlLabel();			
		}
	};

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final TrialExplorerPanel trialExplorerPanel;
	private final TraitExplorerPanel traitExplorerPanel;
	
	private final MessagePrinter messagesPanel;

	private final DALClientProvider clientProvider;

	private final ExplorerProperties explorerProperties;

	private final File userDataFolder;
	
	private final ImageIcon linkedIcon;

	private final ImageIcon unlinkedIcon;

	private final ClientUrlChanger clientUrlChanger = new ClientUrlChanger() {
		
		@Override
		public void setIgnoreClientUrlChanged(boolean b) {
			ignoreClientUrlChanged = b;
		}
		
		@Override
		public void clientUrlChanged() {
			updateDatabaseUrlLabel();
		}
	};


	private final DriverType driverType;
	private final TagExplorerPanel tagExplorerPanel;
	private final ImageIcon barcodeIcon;
	
    private final Consumer<Void> trialsLoadedConsumer = new Consumer<Void>() {
        @Override
        public void accept(Void arg0) {
            if (traitExplorerPanel != null) {
                traitExplorerPanel.refreshTraitsTable();
            }
        }
    };
    
    private final Consumer<Collection<Trait>> traitRemovalHandler = new Consumer<Collection<Trait>>() {
        @Override
        public void accept(Collection<Trait> traitsToRemove) {
            if (traitExplorerPanel != null) {
                traitExplorerPanel.removeTraits(traitsToRemove);
            }
        }
    };

	
	public TrialManagerPanel(KdxPluginInfo pluginInfo) 
	throws IOException 
	{
		super(new BorderLayout());
		
		this.messagesPanel = pluginInfo.getMessagePrinter();
		this.backgroundRunner = pluginInfo.getBackgroundRunner();
		this.userDataFolder = pluginInfo.getUserDataFolder();
		this.clientProvider = pluginInfo.getClientProvider();
		this.offlineData = pluginInfo.getSingletonSharedResource(OfflineData.class);
		
        KDSmartApplication.getInstance().setMessagePrinter(pluginInfo.getMessagePrintStream());
		
        this.driverType = DriverType
                .getDriverTypeFromSystemProperties(DriverType.H2);

		offlineData.addOfflineDataChangeListener(offlineDataChangeListener);

		linkedIcon = KDClientUtils.getIcon(ImageId.CONNECTED_24);
		unlinkedIcon = KDClientUtils.getIcon(ImageId.DISCONNECTED_24);		
		barcodeIcon = KDClientUtils.getIcon(ImageId.BARCODE_PAGE);

		updateDatabaseUrlLabel();
		
		this.clientProvider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				handleClientChanged();
			}
		});

		explorerProperties = ExplorerProperties.getInstance();
		
		PreferenceCollection pc = TrialManagerPreferences.getInstance()
				.getPreferenceCollection(this, Msg.APPNAME_TRIAL_MANAGER());
		KdxplorePreferences.getInstance().addPreferenceCollection(pc);

		TraitValue.DISPLAY_DATE_DIFF_AS_NDAYS = explorerProperties.getDisplayElapsedDaysAsCount();		

		ExplorerServices explorerServices = new ExplorerServices(
		        pluginInfo, 
		        offlineData
//		        , clientProvider
		        );
		
        trialExplorerPanel = new TrialExplorerPanel(
                this, // KdxApp
		        pluginInfo,
				explorerServices.getKdxDeviceService(),
				this, // as TrialExplorerManager
				offlineData,
				driverType,
				barcodeIcon,
				clientUrlChanger,
				trialsLoadedConsumer,
				traitRemovalHandler);

		tabbedPane.addTab(TAB_TRIALS, KDClientUtils.getIcon(ImageId.KDS_TRIALS), trialExplorerPanel);

		Transformer<Trial, Boolean> checkIfEditorIsActive = new Transformer<Trial, Boolean>() {
			@Override
			public Boolean transform(Trial trial) {
				return trialExplorerPanel.isEditorActiveForTrial(trial);
			}
		};
		traitExplorerPanel = new TraitExplorerPanel(
				messagesPanel, 
				offlineData,
				clientProvider,
//				uploadHandler,
				backgroundRunner,
				barcodeIcon, 
				checkIfEditorIsActive);
		
		tabbedPane.addTab(TAB_TRAITS, KDClientUtils.getIcon(ImageId.KDS_TRAITS), traitExplorerPanel);
		
		tagExplorerPanel = new TagExplorerPanel(offlineData);
		tabbedPane.addTab(TAB_TAGS, KDClientUtils.getIcon(ImageId.BLACK_TAG), tagExplorerPanel);
		
		
		// Now tie together those panels that need to talk to each other
		trialExplorerPanel.setTraitExplorer(traitExplorerPanel);
		
		Box box = Box.createHorizontalBox();
		
		explorerServices.addActions(box);
		box.add(Box.createHorizontalGlue());
		box.add(databaseUrlLabel);
		box.add(Box.createHorizontalGlue());
		Action action = offlineData.getOfflineDataAction();
		if (action != null) {
		    JButton button = new JButton(pluginInfo.getKdxploreIcon());
		    button.addActionListener(new ActionListener() {                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (RunMode.getRunMode().isDeveloper()) {
                        if (0 != (ActionEvent.SHIFT_MASK & e.getModifiers())) {
                            openJdbcExplorer();
                            return;
                        }
                    }
                    action.actionPerformed(e);
                }
            });
		    box.add(button);
		}
		
		add(box, BorderLayout.NORTH);
		add(tabbedPane, BorderLayout.CENTER);
	}

    private void openJdbcExplorer() {
        MsgBox.info(TrialManagerPanel.this, "Not Yet Ready", "Explore Database");
//        KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
//        if (kdxdb != null) {
//            if (kdxdb instanceof JdbcKDXploreDatabase) {
//                
//            }
//        }
    }
    
    // ---- KdxApp ---

    @Override
    public DevelopmentState getDevelopmentState() {
        if (KdxploreConfig.getInstance().isBeta()) {
            return DevelopmentState.BETA;
        }
        return DevelopmentState.PRODUCTION;
    }

	@Override
	public JButton getDefaultButton() {
	    return trialExplorerPanel.getAddDatabaseTrialsButton();
	}
	   
    public OfflineData getOfflineData() {
		return offlineData;
	}
	
	private void handleClientChanged() {

		// updateLoadTrialUnitsAction();

		if (ignoreClientUrlChanged) {
			if (clientProvider.isClientAvailable()) {
				DALClient client = clientProvider.getDALClient(null);
				maybeChangeSavedUsername(client.getUserName());
			}
			return;
		}
		
		KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
        if (kdxdb != null) {
            if (clientProvider.isClientAvailable()) {
                DALClient client = clientProvider.getDALClient(null);
                try {
                    kdxdb.setUrlIfCurrentIsLocal(client.getBaseUrl());
                }
                catch (IOException e) {
                    Shared.Log.w(TAG, "handleClientChanged", e); //$NON-NLS-1$
                }
            }
        }

		updateDatabaseUrlLabel();
		// new
		// Exception("==== NOT AN ERROR: INFORMATION ONLY: handleClientChanged: ignoreClientUrlChanged="+ignoreClientUrlChanged).printStackTrace();

	}

	private void maybeChangeSavedUsername(String username) {
		if (! username.equalsIgnoreCase(explorerProperties.getCurrentDatabaseUsername())) {
			try {
				explorerProperties.setCurrentDatabaseUsername(username);
				explorerProperties.save("username changed"); //$NON-NLS-1$
			} catch (IOException e) {
				Shared.Log.w(TAG, 
				        String.format("maybeChangeSavedUsername(%s)  Problem saving username change",  //$NON-NLS-1$
				                username));
			}
		}
	}


	/**
	 * @param dbstub
	 * @param onFinish
	 */
	private void loadOfflineDataAsync(
	        DatabaseDataStub dbstub,
	        final Closure<DatabaseDataLoadResult> onFinish) 
	{
	    String msg = OfflineData.LOADING_OFFLINE_REFERENCE_DATA_FOR(dbstub.dburl);
        messagesPanel.println(msg);

		File dbDataDir = dbstub.getDatabaseDirectory(userDataFolder);
		DatabaseLocation databaseLocation = OfflineData.createFor(driverType, dbDataDir);
		offlineData.loadDatabaseDataAsync(msg, dbstub, databaseLocation, backgroundRunner, onFinish);
	}

	private void updateDatabaseUrlLabel() {
		Icon icon = unlinkedIcon;
		String clientUserName = null;
		if (clientProvider.isClientAvailable()) {
			DALClient client = clientProvider.getDALClient(null);
			if (client.isLoggedIn()) {
				clientUserName = client.getUserName();
				icon = linkedIcon;

			}
		}
		databaseUrlLabel.setIcon(icon);

		String dburl = offlineData.getDatabaseUrl();
		if (Check.isEmpty(dburl)) {
			databaseUrlLabel.setToolTipText(Msg.TOOLTIP_NO_CURRENT_KDDART_CONNECTION());
			databaseUrlLabel.setText(com.diversityarrays.kdxplore.offline.Msg.MSG_NO_OFFLINE_DATA());
		} else {
		    StringBuilder sb = new StringBuilder();
            if (clientUserName == null) {
                sb.append(Msg.MSG_NOT_LOGGED_IN());
            } else {
                sb.append(Msg.MSG_CONNECTED_AS_USER(clientUserName));
            }
		    
		    String label = offlineData.getDatabaseNickname();
		    if (Check.isEmpty(label)) {
		        label = dburl;
		    }
		    else {
		        sb.append(" to ").append(dburl);
		    }
		    
		    databaseUrlLabel.setText(label);
            databaseUrlLabel.setToolTipText(sb.toString());
		}
	}

	private boolean ignoreClientUrlChanged;

	@Override
	public void showTraitsInTraitExplorer(List<Trait> traits) {
		traitExplorerPanel.setSelectedTraits(traits);
		
		int index = tabbedPane.indexOfTab(TAB_TRAITS);
		if (index >= 0) {
			tabbedPane.setSelectedIndex(index);
		}
	}
	
	// - - - - - - - -
	
	@Override
	public void shutdown() {
        KdxploreDatabase kdxploreDatabase = offlineData.getKdxploreDatabase();
        if (kdxploreDatabase != null) {
            kdxploreDatabase.shutdown();
        }
	}
	
	@Override
	public AfterUpdateResult initialiseAppAfterUpdateCheck(AppInitContext initContext) {
	    
	    final AfterUpdateResult[] initResult = new AfterUpdateResult[1];
	    initResult[0] = AfterUpdateResult.OK;

        trialExplorerPanel.doPostOpenOperations();

        String currentDatabaseUrl = explorerProperties.getCurrentDatabaseUrl();

        if (! KdxploreDatabase.LOCAL_DATABASE_URL.equalsIgnoreCase(currentDatabaseUrl)) {
            clientProvider.setInitialClientUrl(currentDatabaseUrl);
        }
        
        clientProvider.setInitialClientUsername(explorerProperties.getCurrentDatabaseUsername());

        if (currentDatabaseUrl == null) {
            currentDatabaseUrl = KdxploreDatabase.LOCAL_DATABASE_URL;
        }
        
        Closure<DatabaseDataLoadResult> loadDataErrorCompleteHandler = new Closure<DatabaseDataLoadResult>() {
            @Override
            public void execute(DatabaseDataLoadResult result) {

                Throwable problem = result.cause;

                if (problem == null) {
                    
                    // If user is logged in it may be to another database
                    // so ensure we don't get confused by shutting down the extant connection.
                    if (clientProvider.isClientAvailable()) {
                        DALClient client = clientProvider.getDALClient();
                        if (! client.getBaseUrl().equalsIgnoreCase(result.dbUrl)) {
                            clientProvider.logout();
                        }
                    }
                    
                    String initialTabName = TAB_TRIALS;
                    if (KdxploreConfig.getInstance().getModeList().contains("CIMMYT")
                            &&
                        trialExplorerPanel.getTrialCount() <= 0 
                            &&
                        traitExplorerPanel.getTraitCount() <= 0) 
                    {
                        // This will suggest that the user loads traits first.
                        initialTabName = TAB_TRAITS;
                        // The reason for this in CIMMYT mode is that because the
                        // user isn't able to get the Traits from the database
                        // they really should load the Traits first.
                        // 
                    }
                    int index = tabbedPane.indexOfTab(initialTabName);
                    tabbedPane.setSelectedIndex(index);
                }
                else {
                    problem.printStackTrace();
                    messagesPanel.println(problem);

                    Throwable lastCause = null;
                    Throwable cause = problem;
                    while (cause != null) {
                        lastCause = cause;
                        cause = cause.getCause();
                    }

                    if (lastCause != null) {
                        Shared.Log.e(TAG, "Initialisation Failed", lastCause); //$NON-NLS-1$
                    }

                    // Replace all extant tabs with the Error tab.
                    for (int tabIndex = tabbedPane.getTabCount(); --tabIndex >= 0; ) {
                        tabbedPane.remove(tabIndex);
                    }
                    
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    result.cause.printStackTrace(pw);
                    pw.close();
                    JTextArea ta = new JTextArea(sw.toString());
                    ta.setEditable(false);
                    tabbedPane.addTab(Msg.TAB_INIT_ERROR(), new JScrollPane(ta));

                    initResult[0] = AfterUpdateResult.FAIL_IF_ALL;
                    
                    String errmsg = lastCause==null ? "" : lastCause.getMessage(); //$NON-NLS-1$
                    GuiUtil.errorMessage(
                            TrialManagerPanel.this,
                            Msg.ERRMSG_CHECK_MESSSAGES_URL_CAUSE(result.dbUrl, errmsg),
                            OfflineData.LOADING_OFFLINE_REFERENCE_DATA_ERROR());
                }
            }
        };

        List<DatabaseDataStub> databaseStubs = DatabaseDataUtils.collectDatabaseDirectories(userDataFolder, driverType);

        DatabaseDataStub chosen = null;
        if (databaseStubs.isEmpty()) {
            // This is a brand new one.
            chosen = DatabaseDataStub.create(driverType, userDataFolder, currentDatabaseUrl);
//          Path dirPath = Paths.get(defaultKdxploreDatabaseLocation.databaseDirectory.getPath());
//          boolean isDefaultDatabase = defaultKdxploreDatabaseLocation.databaseDirectory.equals(dirPath.toFile());
//          chosen = new DatabaseDataStub(isDefaultDatabase, defaultKdxploreDatabaseLocation.driverType, dirPath, currentDatabaseUrl, null);
        }
        else {
            if (databaseStubs.size() == 1) {
                chosen = databaseStubs.get(0);
            }
            else {
                DatabaseDataStub[] selectionValues = databaseStubs.toArray(new DatabaseDataStub[databaseStubs.size()]);
                for (DatabaseDataStub dds : selectionValues) {
                    if (currentDatabaseUrl.equalsIgnoreCase(dds.dburl)) {
                        chosen = dds;
                        break;
                    }
                }
                if (chosen==null) {
                    chosen = selectionValues[0];
                }
                Object answer = JOptionPane.showInputDialog(TrialManagerPanel.this, 
                        Msg.MSG_CHOOSE_DATABASE_TO_OPEN_DEFAULT(chosen.toString()), 
                        Msg.TITLE_MULTIPLE_DATABASES_FOUND(),
                        JOptionPane.QUESTION_MESSAGE, 
                        null,
                        selectionValues, chosen);
                if (answer instanceof DatabaseDataStub) {
                    chosen = (DatabaseDataStub) answer;
                }
            }
        }
        
        if (chosen == null) {
            initResult[0] = AfterUpdateResult.FAIL_IF_ALL;
//            System.exit(0);
        }
        else {
            loadOfflineDataAsync(chosen, loadDataErrorCompleteHandler);   
            trialExplorerPanel.initialiseUploadHandler(offlineData);
        }
        
        return initResult[0];
    }

    @Override
    public Component getUIComponent() {
        return this;
    }

    @Override
    public int getInitialisationOrder() { 
        return OfflineData.AppOrder.TRIAL_MANAGER.initOrder;
    }

    @Override
    public int getDisplayOrder() {
        return OfflineData.AppOrder.TRIAL_MANAGER.displayOrder;
        // "Just after WelcomeApp"
    }
    
    @Override
    public String getAppName() {
        return Msg.APPNAME_TRIAL_MANAGER();
    }
}
