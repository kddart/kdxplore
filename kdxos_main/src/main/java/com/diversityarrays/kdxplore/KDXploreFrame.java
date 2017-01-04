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
package com.diversityarrays.kdxplore;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.prefs.ExplorerProperties;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferenceEditor;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.services.AppInitContext;
import com.diversityarrays.kdxplore.services.BackupProvider;
import com.diversityarrays.kdxplore.services.KdxApp;
import com.diversityarrays.kdxplore.services.KdxApp.AfterUpdateResult;
import com.diversityarrays.kdxplore.services.KdxApp.DevelopmentState;
import com.diversityarrays.kdxplore.services.KdxAppService;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.ui.BrandingImageComponent;
import com.diversityarrays.ui.LoginUrlsProvider;
import com.diversityarrays.ui.PropertiesLoginUrlsProvider;
import com.diversityarrays.update.UpdateCheckContext;
import com.diversityarrays.update.UpdateCheckRequest;
import com.diversityarrays.update.UpdateDialog;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ConnectDisconnectActions;
import com.diversityarrays.util.DALClientProvider;
import com.diversityarrays.util.DefaultDALClientProvider;
import com.diversityarrays.util.DefaultUncaughtExceptionHandler;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.PrintStreamMessageLogger;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.VerticalLabelUI;

import net.pearcan.application.ApplicationFolder;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.DefaultDesktopSupport;
import net.pearcan.ui.desktop.DefaultFrameWindowOpener;
import net.pearcan.ui.desktop.DesktopSupport;
import net.pearcan.ui.desktop.MacApplication;
import net.pearcan.ui.desktop.MacApplicationException;
import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.ui.widget.MessagesPanel;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.MemoryUsageMonitor;
import net.pearcan.util.MessagePrinter;
import net.pearcan.util.Util;

public class KDXploreFrame extends JFrame {

    private static final String OFFLINE_DATA_APP_SERVICE_CLASS_NAME = "com.diversityarrays.kdxplore.offline.OfflineDataAppService";

    private static final String TAG = KDXploreFrame.class.getSimpleName();

    static private final Dimension MINIMUM_SIZE = new Dimension(800, 600);

    private static final String CARD_KDXAPPS = "kdxapps"; //$NON-NLS-1$

    private final String displayVersion;

    private MessagesPanel messagesPanel = new MessagesPanel(Msg.HDG_MESSAGES(), true);

    private AboutPage aboutPage;
    private Action aboutAction = new AbstractAction(Msg.ACTION_ABOUT()) {
        @Override
        public void actionPerformed(ActionEvent ev) {

            if (aboutPage != null) {
                aboutPage.toFront();
                return;
            }

            aboutPage = new AboutPage(Msg.TITLE_PREFIX_ABOUT(getTitle()),
                    KDXploreFrame.this,
                    applicationFolder.getApplicationName(),
                    displayVersion);
            aboutPage.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    aboutPage.removeWindowListener(this);
                    aboutPage = null;
                }
            });
            aboutPage.setLocationRelativeTo(KDXploreFrame.this);
            aboutPage.setVisible(true);
        }
    };

    private final String onlineHelpUrl;
    private Action onlineHelpAction = new AbstractAction(Msg.ACTION_ONLINE_HELP()) {
        @Override
        public void actionPerformed(ActionEvent e) {
        	if (Check.isEmpty(onlineHelpUrl)) {
        		MsgBox.warn(KDXploreFrame.this, "Sorry - no help URL available", getTitle());
        		return;
        	}
            Desktop dt = Desktop.getDesktop();
            try {
                dt.browse(new URI(onlineHelpUrl));
            }
            catch (IOException | URISyntaxException e1) {
                MsgBox.error(KDXploreFrame.this,
                        "Unable to open online help at\n" + onlineHelpUrl,
                        getTitle());
            }
        }
    };

    private final DefaultFrameWindowOpener frameWindowOpener;

    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    private JTabbedPane kdxAppTabs;

    private final BackgroundPanel backgroundPanel = new BackgroundPanel();

    private Image iconImageBig;
    private Image iconImageSmall;

    private final Image iconImage;
    private final ImageIcon kdxploreIcon;

    private final String baseTitle;

    private final LoginUrlsProvider loginUrlsProvider;
    private final DALClientProvider clientProvider;

    private final ConnectDisconnectActions connectDisconnectActions;

    private JSplitPane splitPane;

    private final ApplicationFolder applicationFolder;
    private final File userDataFolder;

    private final Closure<UpdateCheckContext> updateChecker;

    private LogoImagesLabel logoImagesLabel;

    private final PrintStreamMessageLogger messageLogger = new PrintStreamMessageLogger(
            messagesPanel.getPrintStream());


    private final int versionCode;
    private final String version;

    static private final Consumer<DALClientProvider> NO_OP_CONSUMER = new Consumer<DALClientProvider>() {
        @Override
        public void accept(DALClientProvider t) { }
    };
    private final Function<DALClientProvider, Consumer<DALClientProvider>> connectIntercept = new Function<DALClientProvider, Consumer<DALClientProvider>>() {

        @Override
        public Consumer<DALClientProvider> apply(DALClientProvider provider) {
            Consumer<DALClientProvider> result;
            if (provider.getCanChangeUrl()) {
                // Just let it continue
                result = NO_OP_CONSUMER;
            }
            else {
                // We will allow change temporarily.
                provider.setCanChangeUrl(true);
                result = new Consumer<DALClientProvider>() {
                    @Override
                    public void accept(DALClientProvider provider) {
                        provider.setCanChangeUrl(false);
                    }
                };
            }
            return result;
        }
    };

    KDXploreFrame(ApplicationFolder appFolder, String title, int versionCode, String version, Closure<UpdateCheckContext> updateChecker)
            throws IOException {
        super(title);

        this.applicationFolder = appFolder;
        this.baseTitle = title;
        this.versionCode = versionCode;
        this.version = version;
        this.updateChecker = updateChecker;

        KdxploreConfig config = KdxploreConfig.getInstance();
        this.onlineHelpUrl = config.getOnlineHelpUrl();

        String supportEmail = config.getSupportEmail();
	if (Check.isEmpty(supportEmail)) {
	    supportEmail = "someone@somewhere";
	}

        DefaultUncaughtExceptionHandler eh = new DefaultUncaughtExceptionHandler(this,
                appFolder.getApplicationName() + "_Error", //$NON-NLS-1$
                supportEmail,
                version + "(" + versionCode + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        Thread.setDefaultUncaughtExceptionHandler(eh);
        MsgBox.DEFAULT_PROBLEM_REPORTER = eh;

        this.userDataFolder = applicationFolder.getUserDataFolder();

        this.loginUrlsProvider = new PropertiesLoginUrlsProvider(CommandArgs.getPropertiesFile(applicationFolder));

        displayVersion = RunMode.getRunMode().isDeveloper()
                ? version + "-dev" //$NON-NLS-1$
                : version;

        List<? extends Image> iconImages = loadIconImages();
        iconImage = iconImageBig != null ? iconImageBig : iconImageSmall;
        kdxploreIcon = new ImageIcon(iconImageSmall != null ? iconImageSmall : iconImageBig);
        setIconImages(iconImages);

        if (Util.isMacOS()) {
            try {
                System.setProperty("apple.laf.useScreenMenuBar", "true"); //$NON-NLS-1$ //$NON-NLS-2$

                macapp = new MacApplication(null);
                macapp.setAboutHandler(aboutAction);
                macapp.setQuitHandler(exitAction);
                if (iconImage != null) {
                    macapp.setDockIconImage(iconImage);
                }
                macapp.setPreferencesHandler(settingsAction);
                macapp.setAboutHandler(aboutAction);
                macapp.setQuitHandler(exitAction);
            }
            catch (MacApplicationException e) {
                macapp = null;
                Shared.Log.w(TAG, "isMacOS", e); //$NON-NLS-1$
            }
        }

        if (iconImage != null) {
            this.setIconImage(iconImage);
        }

        clientProvider = new DefaultDALClientProvider(this, loginUrlsProvider,
                createBrandingImageComponent());
        clientProvider.setCanChangeUrl(false);
        clientProvider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                handleClientChanged();
            }
        });

        connectDisconnectActions = new ConnectDisconnectActions(clientProvider,
                Msg.TOOLTIP_CONNECT_TO_DATABASE(),
                Msg.TOOLTIP_DISCONNECT_FROM_DATABASE());
        connectDisconnectActions.setConnectIntercept(connectIntercept);

        desktopSupport = new DefaultDesktopSupport(KDXploreFrame.this, baseTitle);

        frameWindowOpener = new DefaultFrameWindowOpener(desktopSupport) {
            @Override
            public Image getIconImage() {
                return iconImage;
            }
        };
        frameWindowOpener.setOpenOnSameScreenAs(KDXploreFrame.this);

        setGlassPane(desktopSupport.getBlockingPane());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(windowListener);

        initialiseKdxApps();

        JMenuBar mb = buildMenuBar();

        if (iconImage != null) {
            backgroundPanel.setBackgroundImage(iconImage);
        }
        else {
            backgroundPanel.setBackgroundImage(KDClientUtils.getImage(ImageId.DART_LOGO_128x87));
        }
        backgroundPanel.setBackground(Color.decode("0xccffff")); //$NON-NLS-1$

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cardPanel, messagesPanel);
        splitPane.setResizeWeight(0.8);

        // = = = = = =

        Container cp = getContentPane();
        cp.add(splitPane, BorderLayout.CENTER);

        statusInfoLine.setHorizontalAlignment(SwingConstants.LEFT);
        logoImagesLabel = new LogoImagesLabel();
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusInfoLine, BorderLayout.CENTER);
        bottom.add(logoImagesLabel, BorderLayout.EAST);
        cp.add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                removeWindowListener(this);
                logoImagesLabel.startCycling();
            }
        });

        showCard(CARD_KDXAPPS);

        setJMenuBar(mb);

        pack();

        Dimension size = getSize();
        boolean changed = false;
        if (size.width < MINIMUM_SIZE.width) {
            size.width = MINIMUM_SIZE.width;
            changed = true;
        }
        if (size.height < MINIMUM_SIZE.height) {
            size.height = MINIMUM_SIZE.height;
            changed = true;
        }
        if (changed) {
            setSize(size);
        }

        setLocationRelativeTo(null);

        final MemoryUsageMonitor mum = new MemoryUsageMonitor();
        mum.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                statusInfoLine.setText(mum.getMemoryUsage());
            }
        });
    }

    // Backup KdxApps are those that return true for canBackupDatabase().
    // They are separate from active Apps because some may NOT have a user interface
    // in the normal case.
    private final List<BackupProvider> backupProviders = new ArrayList<>();

    // Active KdxApps are those that are 'wanted' and also have a uiComponent.
    private final List<KdxApp> allKdxApps = new ArrayList<>();

    private final Map<Component, KdxApp> appByComponent = new HashMap<>();

    private ChangeListener kdxAppTabsChangeListener = new ChangeListener() {
        KdxApp previousApp = null;
        @Override
        public void stateChanged(ChangeEvent e) {
            Component comp = kdxAppTabs.getSelectedComponent();
            if (comp != null) {
                KdxApp app = appByComponent.get(comp);
                if (app != null) {

                    getRootPane().setDefaultButton(app.getDefaultButton());

                    JMenuBar mbar = getJMenuBar();
                    boolean mbarChanged = false;
                    if (previousApp != null) {
                        List<JMenu> menus = previousApp.getAppMenus();
                        if (! Check.isEmpty(menus)) {
                            for (JMenu m : menus) {
                                mbar.remove(m);
                                mbarChanged = true;
                            }
                        }

                        previousApp.setActive(false);
                    }
                    app.setActive(true);

                    List<JMenu> menus = app.getAppMenus();
                    if (! Check.isEmpty(menus)) {
                        int pos = 0;
                        if (mbar.getMenuCount() > 0) {
                            pos = 1;
                        }
                        for (JMenu m : menus) {
                            mbar.add(m, pos);
                            ++pos;
                            mbarChanged = true;
                        }
                    }

                    if (mbarChanged) {
                        mbar.repaint();
                    }

                    previousApp = app;
                }
            }
        }
    };

    private void initialiseKdxApps() throws IOException {

        String[] classNames = KdxploreConfig.getInstance().getMainPluginClassNames();
        if (classNames != null && classNames.length > 0) {
            List<String> classNamesToLoad = new ArrayList<>();
            Collections.addAll(classNamesToLoad, classNames);
            if (! classNamesToLoad.contains(OFFLINE_DATA_APP_SERVICE_CLASS_NAME)) {
                classNamesToLoad.add(0, OFFLINE_DATA_APP_SERVICE_CLASS_NAME);
                classNames = classNamesToLoad.toArray(new String[classNamesToLoad.size()]);
            }
        }

        Map<KdxApp, Component> componentByApp = collectKdxApps(classNames);

        appByComponent.clear();
        for (KdxApp app : componentByApp.keySet()) {
            Component comp = componentByApp.get(app);
            if  (comp != null) {
                appByComponent.put(comp, app);
            }
        }

        allKdxApps.clear();
        allKdxApps.addAll(componentByApp.keySet());

        // Initialise the apps in initialisation order.
        allKdxApps.sort(Comparator.comparing(KdxApp::getInitialisationOrder));


        // And while we're initialising them we collect
        // those that can perform a databaseBackup (i.e. have a BackupProvider).
        backupProviders.clear();
        List<KdxApp> wantedAppsWithUi = new ArrayList<>();

        for (KdxApp app : allKdxApps) {
            BackupProvider bp = app.getBackupProvider();
            if (bp != null) {
                backupProviders.add(bp);
            }

            /**
             * See {@link com.diversityarrays.kdxplore.prefs.KdxplorePreferences#SHOW_ALL_APPS}
             */
            if (appIsWanted(app)) {
                try {
                    app.initialiseAppBeforeUpdateCheck(appInitContext);
                }
                catch (Exception e) {
                    String msg = Msg.MSG_KDXAPP_INIT_PROBLEM(app.getAppName());
                    Shared.Log.w(TAG, msg, e);
                    messagesPanel.println(msg);
                    messagesPanel.println(e.getMessage());
                }
            }

            if (appIsWanted(app) && null != componentByApp.get(app)) {
                wantedAppsWithUi.add(app);
            }
        }

        // - - - - - - - - - - - - - - - - - - - - -
        // Display the apps in display order.
        wantedAppsWithUi.sort(Comparator.comparing(KdxApp::getDisplayOrder));
        backupProviders.sort(Comparator.comparing(BackupProvider::getDisplayOrder));

        switch (wantedAppsWithUi.size()) {
        case 0:
            JLabel label = new JLabel(Msg.MSG_NO_KDXPLORE_APPS_AVAILABLE());
            label.setHorizontalAlignment(JLabel.CENTER);
            cardPanel.add(label, CARD_KDXAPPS);
            break;
        case 1:
            KdxApp kdxApp = wantedAppsWithUi.get(0);
            Component uiComponent = componentByApp.get(kdxApp);

            Component appComponent = makeComponentForTab(kdxApp, uiComponent);
            cardPanel.add(appComponent, CARD_KDXAPPS);

            getRootPane().setDefaultButton(kdxApp.getDefaultButton());

            String msg = Msg.MSG_SHOWING_KDXAPP(kdxApp.getAppName());
            messagesPanel.println(msg);
            System.err.println(msg
                    + " uiClass=" //$NON-NLS-1$
                    + uiComponent.getClass().getName());
            break;
        default:
            kdxAppTabs = new JTabbedPane(JTabbedPane.LEFT);
            cardPanel.add(kdxAppTabs, CARD_KDXAPPS);
            Bag<String> tabsSeen = new HashBag<>();
            for (KdxApp app : wantedAppsWithUi) {
                Component ui = componentByApp.get(app);

                String tabName = app.getAppName();
                DevelopmentState devState = app.getDevelopmentState();
                switch (devState) {
                case ALPHA:
                    tabName = tabName + " (\u03b1)"; // TODO move to UnicodeChars
                    break;
                case BETA:
                    tabName = tabName + " (\u03b2)"; // TODO move to UnicodeChars
                    break;
                case PRODUCTION:
                    break;
                default:
                    tabName = tabName + " " + devState.name();
                    break;
                }
                tabsSeen.add(tabName);
                int count = tabsSeen.getCount(tabName);
                if (count > 1) {
                    tabName = tabName + "_" + count; //$NON-NLS-1$
                }

                Component tabComponent = makeComponentForTab(app, ui);
                kdxAppTabs.addTab(tabName, tabComponent);
                if (macapp == null) {
                    int index = kdxAppTabs.indexOfTab(tabName);
                    if (index >= 0) {
                        JLabel tabLabel = new JLabel(tabName);
                        tabLabel.setBorder(new EmptyBorder(2,2,2,2));
                        tabLabel.setUI(new VerticalLabelUI(VerticalLabelUI.UPWARDS));
                        kdxAppTabs.setTabComponentAt(index, tabLabel);
                    }
                }
                messagesPanel.println(Msg.MSG_SHOWING_KDXAPP(tabName));
            }

            kdxAppTabs.addChangeListener(kdxAppTabsChangeListener);
            kdxAppTabs.setSelectedIndex(0);
            break;
        }
    }

    private Component makeComponentForTab(KdxApp kdxApp, Component uiComponent) {
        Component appComponent = uiComponent;
        DevelopmentState devState = kdxApp.getDevelopmentState();
        if (devState.getShouldShowHeading()) {
            JLabel heading = new JLabel(devState.getHeadingText(), JLabel.CENTER);
            heading.setOpaque(true);
            heading.setForeground(devState.getHeadingFontColor());
            heading.setBackground(Color.LIGHT_GRAY);
            heading.setFont(heading.getFont().deriveFont(Font.BOLD));

            JPanel p = new JPanel(new BorderLayout());
            p.add(heading, BorderLayout.NORTH);
            //Color.DARK_GRAY, Font.BOLD, Color.LIGHT_GRAY, Color.GRAY
//            p.add(GuiUtil.createLabelSeparator(devState.getHeadingText(), fontColor, Font.BOLD, Color.LIGHT_GRAY, Color.GRAY),
//                    BorderLayout.NORTH);
            p.add(uiComponent, BorderLayout.CENTER);
            appComponent = p;
        }
        return appComponent;
    }

    private Set<String> appNamesToHide = null;
    private boolean appIsWanted(KdxApp app) {

        if (appNamesToHide == null) {
            appNamesToHide = new HashSet<>();
            if (! KdxplorePreferences.getInstance().getShowAllApps()) {
                appNamesToHide.addAll(ExplorerProperties.getInstance().getAppNamesToHide());
            }
        }

        return ! appNamesToHide.contains(app.getAppName());
    }

    private Map<KdxApp, Component> collectKdxApps(String[] classNames) throws IOException {

        Map<KdxApp, Component> result = new HashMap<>();

        BiConsumer<String, Either<Throwable, KdxAppService>> onServiceFound = new BiConsumer<String, Either<Throwable, KdxAppService>>() {
            @Override
            public void accept(String className, Either<Throwable, KdxAppService> either) {
                Throwable error = null;
                if (either.isRight()) {
                    KdxAppService kdxAppService = either.right();
                    if (kdxAppService != null) {
                        try {
                            KdxApp kdxApp = kdxAppService.createKdxApp(pluginInfo);
                            Component uiComponent = kdxApp.getUIComponent();
                            result.put(kdxApp, uiComponent);
                        }
                        catch (Exception | NoClassDefFoundError e) {
                            error = e;
                        }
                    }
                }
                else {
                    error = either.left();
                }

                if (error != null) {
                    String msg = Msg.MSG_PROBLEM_GETTING_KDXAPP(className);
                    Shared.Log.w(TAG, msg, error);
                    messagesPanel.println(msg);
                    messagesPanel.println(error);
                }
            }
        };

        Shared.detectServices(KdxAppService.class, onServiceFound, classNames);

        return result;
    }

    static class LogoImagesLabel extends JLabel implements Runnable {

        static class LogoImageData {
            public final String menuName;
            public final ImageIcon imageIcon;
            public final String url;

            LogoImageData(String menuName, String imageResourceName, String url) {
                this.menuName = menuName;
                this.url = url;

                ImageIcon icon = null;
                InputStream is = LogoImagesLabel.class.getResourceAsStream(imageResourceName);
                if (is != null) {
                    try {
                        icon = new ImageIcon(ImageIO.read(is));
                    }
                    catch (IOException e) {
                        Shared.Log.w("LogoImageData", //$NON-NLS-1$
                                "ctor: Missing resource: " + imageResourceName, e); //$NON-NLS-1$
                    }
                    finally {
                        try {
                            is.close();
                        }
                        catch (IOException ignore) {
                        }
                    }
                }

                imageIcon = icon;
            }
        }

        static private final LogoImageData[] LOGOS = {
                new LogoImageData("Diversity Arrays Technology",  //$NON-NLS-1$
                        "DArT-53x30.png", //$NON-NLS-1$
                        "http://www.diversityarrays.com/"), //$NON-NLS-1$
                new LogoImageData("www.kddart.org",  //$NON-NLS-1$
                        "KDDart-53x30.png",  //$NON-NLS-1$
                        "http://www.kddart.org/"), //$NON-NLS-1$
        };

        private boolean started = false;

        private JPopupMenu popupMenu;
        private final MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (popupMenu == null) {
                    popupMenu = new JPopupMenu();
                    for (LogoImageData lid : LOGOS) {
                        Action action = new AbstractAction(lid.menuName) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    Util.openUrl(lid.url);
                                }
                                catch (IOException ignore) {
                                }
                            }
                        };
                        popupMenu.add(action);
                    }
                }
                popupMenu.show(LogoImagesLabel.this, e.getX(), e.getY());
            }
        };

        static private final long IMAGE_MILLIS = 10000;

        static private final long OPACITY_MILLIS = 80;

        private int showingIndex;

        private int overlayIndex = 0;
        private final List<BufferedImage> overlays = new ArrayList<>();

        LogoImagesLabel() {
            for (float op = 1.0f; op >= 0.0f; op -= 0.1f) {
                BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) bi.getGraphics();
                AlphaComposite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        op);
                g.setComposite(transparent);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, 1, 1);
                g.dispose();
                overlays.add(bi);
            }
            overlayIndex = overlays.size() - 1;
            showNext(0);

            addMouseListener(mouseListener);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(Msg.MSG_CLICK_TO_VISIT_WEBSITE());
        }

        private void showNext(int index) {
            showingIndex = index;
            setIcon(LOGOS[showingIndex].imageIcon);
        }

        public void startCycling() {
            if (!started && LOGOS.length > 1) {
                started = true;
                Thread t = new Thread(this);
                t.setDaemon(true);
                t.start();
            }
        }

        @Override
        public void run() {
            try {
                Thread.sleep(IMAGE_MILLIS / 10);
            }
            catch (InterruptedException ignore) {
            }
            while (true) {
                try {
                    overlayIndex = (overlayIndex + 1) % overlays.size();
                    if (overlayIndex == 0) {
                        Thread.sleep(IMAGE_MILLIS);
                        int index = (showingIndex + 1) % LOGOS.length;
                        showNext(index);
                    }
                    else {
                        repaint();
                        Thread.sleep(OPACITY_MILLIS);
                    }
                }
                catch (InterruptedException ignore) {
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (overlays != null) {
                BufferedImage bi = overlays.get(overlayIndex);
                Rectangle bounds = getBounds();
                g.drawImage(bi, 0, 0, bounds.width, bounds.height, null);
            }
        }
    }

    private BrandingImageComponent createBrandingImageComponent() {
        BrandingImageComponent bic = null;

        if (iconImage != null) {
            bic = new BrandingImageComponent(iconImage);
            bic.setBorder(new EmptyBorder(4, 4, 4, 4));
            bic.setBackground(Color.decode("#ffe74a")); //$NON-NLS-1$
        }

        return bic;
    }

    private List<? extends Image> loadIconImages() {
        List<Image> result = new ArrayList<Image>();

        iconImageBig = KDClientUtils.getImage(ImageId.KDXPLORE_48);
        if (iconImageBig != null) {
            result.add(iconImageBig);
        }

        iconImageSmall = KDClientUtils.getImage(ImageId.KDXPLORE_24);
        if (iconImageSmall != null) {
            result.add(iconImageSmall);
        }
        return result;
    }

    private void showCard(String name) {
        cardLayout.show(cardPanel, name);
    }

    static private class BackgroundPanel extends JPanel {

        private Image backgroundImage;
        private int backgroundWidth = -1;
        private int backgroundHeight = -1;

        public BackgroundPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        public void setBackgroundImage(Image img) {
            backgroundImage = img;
            if (backgroundImage == null) {
                backgroundWidth = -1;
                backgroundHeight = -1;
            }
            else {
                backgroundWidth = backgroundImage.getWidth(null);
                backgroundHeight = backgroundImage.getHeight(null);
            }
        }

        @Override
        public void paint(Graphics g) {
            if (backgroundImage != null) {
                Rectangle vr = getVisibleRect();

                Color save = g.getColor();
                g.setColor(getBackground());
                g.fillRect(0, 0, vr.width, vr.height);
                g.setColor(save);
                if (backgroundWidth > 0 && backgroundHeight > 0) {
                    int x = Math.max(0, (vr.width - backgroundWidth) / 2);
                    int y = Math.max(0, (vr.height - backgroundHeight) / 2);
                    g.drawImage(backgroundImage, x, y, null);
                }
                else {
                    g.drawImage(backgroundImage, vr.x, vr.y, vr.width,
                            vr.height, this);
                }
            }
            super.paint(g);
        }
    }


    private void handleClientChanged() {

        if (clientProvider.isClientAvailable()) {
            DALClient client = clientProvider.getDALClient();

            StringBuilder sb = new StringBuilder(baseTitle);
            sb.append(": ").append(client.getBaseUrl()) //$NON-NLS-1$
                    .append(" ").append(client.getUserName()); //$NON-NLS-1$

            if (client.isInAdminGroup()) {
                sb.append(" **"); //$NON-NLS-1$
            }
            sb.append('/')
                    .append(client.getGroupName());

            String newTitle = sb.toString();

            setTitle(newTitle);
            messagesPanel.println(Msg.MSG_LOGGED_IN(newTitle));
            messagesPanel.println("DAL: " + clientProvider.getClientVersion()); //$NON-NLS-1$
        }
        else {
            messagesPanel.println(Msg.MSG_LOGGED_OUT());

            setTitle(baseTitle);
        }

    }

    private Action settingsAction = new AbstractAction(Msg.ACTION_PREFERENCES()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame frame = frameWindowOpener.getWindowByIdentifier(KdxplorePreferences.class);
            if (frame == null) {
                frame = frameWindowOpener.addDesktopObject(
                        new KdxplorePreferenceEditor(Msg.TITLE_KDXPLORE_PREFERENCES(applicationFolder.getApplicationName())));
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
            }
            else {
            	frame.toFront();
            }
        }
    };

    private Action updateAction = new AbstractAction(Msg.ACTION_CHECK_FOR_UPDATES()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (updateChecker == null || Check.isEmpty(KdxploreConfig.getInstance().getUpdatebaseUrl())) {
                MsgBox.warn(KDXploreFrame.this, Msg.MSG_NO_UPDATE_AVAILABLE(), getTitle());
            }
            else {
                String title = Msg.TITLE_UPDATE_CHECK(getTitle());
                String url = KdxploreConfig.getInstance().getUpdatebaseUrl();
                if (Check.isEmpty(url)) {
                    MsgBox.warn(KDXploreFrame.this, Msg.MSG_NO_UPDATE_URL(), title);
                }
                else {
                    UpdateCheckContext ctx = createUpdateCheckContext(url);
                    updateChecker.execute(ctx);
                }
            }
        }
    };

    private final DesktopSupport desktopSupport;

    private Action exitAction = new AbstractAction(Msg.ACTION_EXIT()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            // boolean quit = JOptionPane.YES_OPTION == JOptionPane
            // .showConfirmDialog(KDXploreFrame.this, "Are you sure?",
            // "Quit", JOptionPane.YES_NO_OPTION);
            boolean quit = true;
            if (quit) {
                if (macapp != null) {
                    System.out.println(e.getClass());
                }
                System.exit(0);
            }
        }
    };

    private MacApplication macapp;

    private JMenuBar buildMenuBar() {

        RunMode runMode = RunMode.getRunMode();

        JMenuBar mb = new JMenuBar();

        mb.add(createMainMenu(runMode));

        if (! backupProviders.isEmpty()) {
            String toolsMenuLabel = Msg.MENU_TOOLS();
            JMenu toolsMenu = new JMenu(toolsMenuLabel);
            mb.add(toolsMenu);
            for (Action a : pluginInfo.getToolsMenuActions()) {
                toolsMenu.add(a);
            }

            Action backupAction = new AbstractAction(Msg.MENUITEM_BACKUP_DB()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doBackupDatabase();
                }
            };
            backupAction.putValue(Action.SMALL_ICON, KDClientUtils.getIcon(ImageId.DB_BACKUP));

            toolsMenu.add(backupAction);
            boolean seen = false;
            for (BackupProvider bp : backupProviders) {
                Action a = bp.getOfflineDataAction();
                if (a != null) {
                    if (! seen) {
                        seen = true;
                        toolsMenu.add(new JSeparator());
                    }
                    toolsMenu.add(a);
                }
            }
        }

        mb.add(frameWindowOpener.getWindowsMenu());
        mb.add(createHelpMenu());

        return mb;
    }

    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu(Msg.MENU_HELP());
        helpMenu.add(aboutAction);
        if (! Check.isEmpty(onlineHelpUrl) && Desktop.isDesktopSupported()) {
            helpMenu.add(onlineHelpAction);
        }
        return helpMenu;
    }

    private JMenu createMainMenu(RunMode runMode) {
        JMenu mainMenu = new JMenu(Msg.MENU_FILE());

        mainMenu.add(connectDisconnectActions.connectAction);
        mainMenu.add(connectDisconnectActions.disconnectAction);

        mainMenu.addSeparator();
        KDClientUtils.initAction(ImageId.SETTINGS_24, settingsAction, Msg.TOOLTIP_CHANGE_PREFS(), true);
        mainMenu.add(settingsAction);

        mainMenu.addSeparator();

        mainMenu.add(updateAction);

        mainMenu.addSeparator();

        mainMenu.add(exitAction);
        return mainMenu;
    }

    private JLabel statusInfoLine = new JLabel();

    private WindowAdapter windowListener = new WindowAdapter() {

        @Override
        public void windowClosing(WindowEvent e) {
            JFrame[] openWindows = frameWindowOpener.getWrappingWindows();
            int nWindows = openWindows.length;

            if (nWindows <= 0) {
                if (RunMode.getRunMode().isDeveloper()) {
                    System.exit(0);
                }

                boolean closeWithoutAsking = KdxplorePreferences.getInstance().getCloseKdxploreWithoutAsking();
                if (closeWithoutAsking) {
                    System.exit(0);
                }

                JCheckBox dontAskAgain = new JCheckBox(Msg.OPTION_CLOSE_WITHOUT_ASKING_IN_FUTURE());
                Box box = Box.createVerticalBox();
                box.add(new JLabel(Msg.MSG_DO_YOU_REALLY_WANT_TO_CLOSE()));
                box.add(dontAskAgain);

                int answer = JOptionPane.showConfirmDialog(KDXploreFrame.this,
                        box, getTitle(),
                        JOptionPane.YES_NO_OPTION);
                if (dontAskAgain.isSelected()) {
                    KdxplorePreferences.getInstance().saveCloseKdxploreWithoutAsking(true);
                }

                if (JOptionPane.YES_OPTION == answer) {
                    System.exit(0);
                }
            }
            else if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(KDXploreFrame.this,
                    Msg.MSG_YOU_STILL_HAVE_ACTIVITIES_ARE_YOU_SURE(nWindows),
                    getTitle(), JOptionPane.YES_NO_OPTION)) {
                try {
                    for (JFrame f : openWindows) {
                        f.dispose();
                    }
                }
                finally {
                    System.exit(0);
                }
            }
            else {
                GuiUtil.restoreFrame(openWindows[0]);
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {

            splitPane.setDividerLocation(0.8);

            KdxploreConfig config = KdxploreConfig.getInstance();
            String url = config.getUpdatebaseUrl();
            if (url != null && !url.isEmpty()) {

                // TODO: make this happen each 5 days in a "quiet" fashion
                UpdateCheckRequest request = new UpdateCheckRequest(KDXploreFrame.this,
                        versionCode,
                        version,
                        false,
                        url);

                UpdateCheckContext ctx = createUpdateCheckContext(url);

                UpdateDialog updateDialog = new UpdateDialog(request,
                        ctx,
                        KdxConstants.getVersionInfo());

                updateDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        doPostUpdateCheckAppInit();
                    }
                });
                updateDialog.setVisible(true);
            }
            else {
                doPostUpdateCheckAppInit();
            }
        }

        @Override
        public void windowClosed(WindowEvent e) {
            clientProvider.logout();

            // Note: any errors here we don't try to recover from.
            for (KdxApp app : allKdxApps) {
            	app.shutdown();
            }
            KdxplorePreferences.getInstance().saveOnShutdown();

        }
    };


    private final AppInitContext appInitContext = new AppInitContext() {

        final Map<String, Object> map = new HashMap<>();

        @Override
        public JFrame getKdxploreFrame() {
            return KDXploreFrame.this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getContextValue(Class<T> tclass, String key) {
            T result = null;

            Object tmp = map.get(key);
            if (tmp != null) {
                if (tclass.isAssignableFrom(tmp.getClass())) {
                    result = (T) tmp;
                }
            }
            return result;
        }

        @Override
        public <T> void setContextValue(Class<T> tclass, String key, T value) {
            T tmp = getContextValue(tclass, key);
            if (tmp != null) {
                if (! tclass.isAssignableFrom(tmp.getClass())) {
                    throw new IllegalStateException(
                            "Incompatible context value, expecting: " + tclass.getName() //$NON-NLS-1$
                            + " got: " + tmp.getClass().getName()); //$NON-NLS-1$
                }
            }
            map.put(key, value);
        }
    };

    private final KdxPluginInfo pluginInfo = new KdxPluginInfo() {

        @Override
        public JFrame getKdxploreFrame() {
            return KDXploreFrame.this;
        }

        @Override
        public WindowOpener<JFrame> getWindowOpener() {
            return frameWindowOpener;
        }

        @Override
        public File getUserDataFolder() {
            return userDataFolder;
        }

        @Override
        public MessagePrinter getMessagePrinter() {
            return messagesPanel;
        }

    	@Override
    	public PrintStream getMessagePrintStream() {
    	    return messagesPanel.getPrintStream();
    	}

        @Override
        public PrintStreamMessageLogger getMessageLogger() {
            return messageLogger;
        }

        @Override
        public ImageIcon getKdxploreIcon() {
            return kdxploreIcon;
        }

        @Override
        public DALClientProvider getClientProvider() {
            return clientProvider;
        }

        @Override
        public BackgroundRunner getBackgroundRunner() {
            return desktopSupport;
        }

        final List<Action> allToolsMenuActions = new ArrayList<>();
        @Override
        public void addToolsMenuActions(Action ... actions) {
            Collections.addAll(allToolsMenuActions, actions);
        }

        @Override
        public Action[] getToolsMenuActions() {
            return allToolsMenuActions.toArray(new Action[allToolsMenuActions.size()]);
        }

        final Map<KdxApp, List<JMenu>> menusByApp = new LinkedHashMap<>();

        @Override
        public Set<KdxApp> getAppsWithMenus() {
            return menusByApp.keySet();
        }

        @Override
        public List<JMenu> getMainMenuActions(KdxApp app) {
            List<JMenu> result = menusByApp.get(app);
            if (result == null) {
                result = Collections.emptyList();
            }
            else {
                result = Collections.unmodifiableList(result);
            }
            return result;
        }

        @Override
        public void addMenus(KdxApp app, List<JMenu> menus) {
            List<JMenu> list = menusByApp.get(app);
            if (list == null) {
                list = new ArrayList<>();
                menusByApp.put(app, list);
            }
            list.addAll(menus);
        }

        @Override
        public void removeAppFromView(KdxApp kdxApp) {
            if (kdxAppTabs == null) {
            	cardPanel.remove(kdxApp.getUIComponent());
                JLabel label = new JLabel(Msg.MSG_NO_KDXPLORE_APPS_AVAILABLE());
                label.setHorizontalAlignment(JLabel.CENTER);
                cardPanel.add(label, CARD_KDXAPPS);
            }
            else {
                final Component ui = kdxApp.getUIComponent();
                if (ui != null) {
                    int foundTabIndex = -1;
                    for (int tabIndex = kdxAppTabs.getTabCount(); --tabIndex >= 0; ) {
                        Component tabComponent = kdxAppTabs.getComponentAt(tabIndex);
                        if (tabComponent == ui) {
                            foundTabIndex = tabIndex;
                            break;
                        }
                    }

                    if (foundTabIndex >= 0) {
                        kdxAppTabs.removeTabAt(foundTabIndex);
                        if (kdxAppTabs.getTabCount() == 1) {
                            Component lastUi = kdxAppTabs.getComponentAt(0);
                            cardPanel.remove(kdxAppTabs);
                            cardPanel.add(lastUi, CARD_KDXAPPS);
                            cardLayout.show(cardPanel, CARD_KDXAPPS);
                            kdxAppTabs = null;
                        }
                    }
                }
                // TODO consider doing shutdown on the App and removing from activeKdxApps
            }
        }

        @Override
        public void appWantsToHide(KdxApp kdxApp) {
            ExplorerProperties.getInstance().addAppNameToHide(kdxApp.getAppName());
            KdxplorePreferences.getInstance().setShowAllApps(false);
        }

        @Override
        public <R> R setSingletonSharedResource(Class<? extends R> rClass, R resource) {
            R previous = getSingletonSharedResource(rClass);
            sharedResources.put(rClass, resource);
            return previous;
        }

        private final Map<Class<?>,Object> sharedResources = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <R> R getSingletonSharedResource(Class<R> rClass) {
            Object obj = sharedResources.get(rClass);
            if (obj !=null && rClass.isAssignableFrom(obj.getClass())) {
                return (R) obj;
            }
            return null;
        }
    };

    private void doPostUpdateCheckAppInit() {
        int nFailed = 0;

        for (KdxApp app : allKdxApps) {
            AfterUpdateResult check = app.initialiseAppAfterUpdateCheck(appInitContext);
            switch (check) {
            case ABORT:
                dispose();
                break;
            case FAIL_IF_ALL:
                ++nFailed;
                break;
            case OK:
                break;
            }
        }

        if (nFailed > 0 && nFailed == allKdxApps.size()) {
            dispose();
        }
    }

    private void doBackupDatabase() {

        switch (backupProviders.size()) {
        case 0:
            MsgBox.warn(this, Msg.MSG_NO_DB_BACKUP_APPS_AVAILABLE(), getTitle());
            return;

        case 1:
            backupProviders.get(0).doDatabaseBackup(this);
            break;

        default:
            Map<String, BackupProvider> bpByName = backupProviders.stream()
                .collect(Collectors.toMap(BackupProvider::getBackupProviderName, Function.identity()));

            String[] choices = backupProviders.stream()
                .map(BackupProvider::getBackupProviderName)
                .collect(Collectors.toList())
                .toArray(new String[backupProviders.size()]);

            Object choice = JOptionPane.showInputDialog(this,
                    Msg.MSG_SELECT_APP_FOR_BACKUP(),
                    Msg.TITLE_BACKUP_DATABASE(),
                    JOptionPane.QUESTION_MESSAGE, null,
                    choices, choices[0]);

            if (choice != null) {
                BackupProvider bp = bpByName.get(choice);
                bp.doDatabaseBackup(this);
            }
            break;
        }
    }

    protected UpdateCheckContext createUpdateCheckContext(String url) {

        UpdateCheckContext ctx = new UpdateCheckContext() {
            @Override
            public Window getWindow() {
                return KDXploreFrame.this;
            }

            @Override
            public PrintStream getPrintStream() {
                return messagesPanel.getPrintStream();
            }

            @Override
            public String getBaseUrl() {
                return url;
            }

            @Override
            public void backupDatabase() {
                doBackupDatabase();
            }

            @Override
            public boolean canBackupDatabase() {
                return ! backupProviders.isEmpty();
            }
        };

        return ctx;
    }
}
