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

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.prefs.ExplorerProperties;
import com.diversityarrays.kdxplore.prefs.KdxPreference;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.update.UpdateCheckContext;
import com.diversityarrays.update.UpdateCheckRequest;
import com.diversityarrays.update.UpdateDialog;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ClassPathExtender;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.application.ApplicationFolder;
import net.pearcan.application.ApplicationFolders;
import net.pearcan.ui.GuiUtil;

public class KDXplore {

    // ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **
    // ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **
    // ** ** ** ** ** Check that these have the correct content for the release

    private static final String KDXPLORE_APP_NAME = "KDXplore_OS"; //$NON-NLS-1$
    static private final int REQD_LIB_COUNT = 16;

    // 1.0.x: beta

    public static void initializeFontSize() {
        String fontSizeParam = "150"; //$NON-NLS-1$ //
                                      //$NON-NLS-1$ System.getProperty("myapp.fontSize");
        if (fontSizeParam != null) {
            float multiplier = Integer.parseInt(fontSizeParam) / 100.0f;
            setUIfontSize(multiplier);
        }
    }

    static public void setUIfontSize(float multiplier) {
        UIDefaults defaults = UIManager.getDefaults();
        for (Enumeration<?> e = defaults.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font) {
                Font font = (Font) value;
                int newSize = Math.round(font.getSize() * multiplier);
                if (value instanceof FontUIResource) {
                    defaults.put(key,
                            new FontUIResource(font.getName(), font.getStyle(),
                                    newSize));
                }
                else {
                    defaults.put(key, new Font(font.getName(), font.getStyle(),
                            newSize));
                }
            }
            else if (value instanceof Integer) {
                if ("Tree.rowHeight".equals(key)) { //$NON-NLS-1$
                    // System.out.println(key+": "+value);
                    Integer rh = (Integer) value;
                    rh = (int) (rh * multiplier * 1.4);
                    defaults.put(key, rh);
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        mainImpl(args, null, new Closure<UpdateCheckContext>() {
            @Override
            public void execute(UpdateCheckContext context) {
                try {
                    String url = context.getBaseUrl();
                    if (!Check.isEmpty(url)) {
                        UpdateCheckRequest request = new UpdateCheckRequest(
                                context.getWindow(),
                                KdxConstants.VERSION_CODE,
                                KdxConstants.VERSION,
                                true,
                                url);

                        new UpdateDialog(request,
                                context,
                                KdxConstants.getVersionInfo())
                                        .setVisible(true);
                    }
                }
                catch (IllegalStateException ignore) {

                }
            }
        });
    }

    public static void mainImpl(String[] args,
            Closure<KDXploreFrame> onCreateCallback,
            final Closure<UpdateCheckContext> updateChecker) {
        Locale defaultLocale = Locale.getDefault();
        System.out.println("Locale=" + defaultLocale); //$NON-NLS-1$

        // System.setProperty("apple.laf.useScreenMenuBar", "true");
        // //$NON-NLS-1$ //$NON-NLS-2$

        // Initialise the appFolder

        KdxplorePreferences prefs = KdxplorePreferences.getInstance();
        applyUIdefaultPreferences(prefs);

        String kdxploreName = KDXPLORE_APP_NAME;
        ApplicationFolder defaultAppFolder = ApplicationFolders.getApplicationFolder(kdxploreName);
        String[] newArgs = CommandArgs.parseRunModeOption(defaultAppFolder, args);

        String baseNameForDistrib = kdxploreName.toLowerCase();
        if (RunMode.DEMO == RunMode.getRunMode()) {
            kdxploreName = kdxploreName + "Demo"; //$NON-NLS-1$
        }
        final ApplicationFolder appFolder = ApplicationFolders.getApplicationFolder(kdxploreName);
        CommandArgs commandArgs = new CommandArgs(appFolder, KdxConstants.VERSION, KdxConstants.VERSION_CODE, newArgs);

        org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory
                .getLog(ClassPathExtender.class);

        if (commandArgs.baseDir == null) {
            File userDir = new File(System.getProperty("user.dir")); //$NON-NLS-1$
            
            File distribDir;
            if ("kdxos_main".equals(userDir.getName())) {
                // In Eclipse project this is where we store it
                distribDir = new File(userDir.getParentFile(), baseNameForDistrib);
            }
            else {
                distribDir = new File(userDir, baseNameForDistrib);
            }

            System.out.println("userDir=" + userDir); //$NON-NLS-1$
            System.out.println("distribDir=" + distribDir); //$NON-NLS-1$

            commandArgs.baseDir = distribDir.isDirectory() ? distribDir : userDir;
        }

        if (!commandArgs.baseDir.isDirectory()) {
            GuiUtil.errorMessage(null, "baseDir is not a directory: " + commandArgs.baseDir); //$NON-NLS-1$
            System.exit(1);
        }

        File libDir = new File(commandArgs.baseDir, "lib"); //$NON-NLS-1$
        File[] libFiles = libDir.listFiles();
        if (libFiles == null) {
            MsgBox.error(null,
                    Msg.MSG_APP_START_DIRECTORY(kdxploreName, commandArgs.baseDir),
                    Msg.ERRTITLE_MISSING_LIBRARY_FILES() + ": " + libDir.getPath());
            System.exit(1);
        }
        else if (libFiles.length < REQD_LIB_COUNT) {
            MsgBox.error(null,
                    Msg.MSG_APP_START_DIRECTORY(kdxploreName, commandArgs.baseDir),
                    Msg.ERRTITLE_MISSING_LIBRARY_FILES() + ": " + libFiles.length);
            System.exit(1);
        }

        // = = = = = = = = = = = = = = = = = = =
        // = = = = = = = = = = = = = = = = = = =
        // = = = = = = = CLASSPATH = = = = = = =
        ClassPathExtender.VERBOSE = !commandArgs.quiet; // RunMode.getRunMode().isDeveloper();
        String libs_sb = "lib,plugins,kdxlibs,../runlibs"; //$NON-NLS-1$

        boolean[] seenPdfbox = new boolean[1];
        Consumer<File> jarChecker = new Consumer<File>() {
            @Override
            public void accept(File f) {
                if (f.getName().startsWith("pdfbox")) {
                    seenPdfbox[0] = true;
                }
            }
        };
        ClassPathExtender.appendToClassPath(commandArgs.baseDir, libs_sb, jarChecker, log);
        if (seenPdfbox[0]) {
            System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        }

        // = = = = = = = = = = = = = = = = = = =
        // = = = = = = = = = = = = = = = = = = =
        // = = = = = = = = = = = = = = = = = = =

        doStaticInitChecks(commandArgs.quiet);
        if (commandArgs.runInitChecks) {
            System.out.println("Init checks OK"); //$NON-NLS-1$
            System.exit(0);
        }

        establishLogger(appFolder);

        @SuppressWarnings("unused")
		String configName = commandArgs.establishKdxConfig();

        Long versionSubinfo = null;
        if (commandArgs.errmsg == null) {
            if (!KdxploreConfig.getInstance().isEternal()) {
                commandArgs.expiryChecks(KdxConstants.VERSION);
                versionSubinfo = KdxConstants.getVersionSubinfo();
                if (versionSubinfo == Long.MAX_VALUE) {
                    versionSubinfo = null;
                }
            }
        }

        String baseTitle = appFolder.getApplicationName() + " v" + KdxConstants.VERSION; //$NON-NLS-1$

        String expiresIn = ""; //$NON-NLS-1$
        if (versionSubinfo != null) {
            if ((0 < versionSubinfo && versionSubinfo < 14) || RunMode.getRunMode().isDeveloper()) {
                expiresIn = " " + Msg.KDX_EXPIRES_IN_N_DAYS(versionSubinfo.intValue()); //$NON-NLS-1$
            }
        }
        if (commandArgs.errmsg != null) {
            JOptionPane.showMessageDialog(null,
                    commandArgs.errmsg,
                    baseTitle + expiresIn,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        final String kdxploreTitle = buildKdxploreTitle(baseTitle,
                expiresIn,
                commandArgs.kdxConfigService.getConfigName());


        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO allow user to change "base font size"
                String uiMultiplier = null;
                try {
                    String propertyName = CommandArgs.UI_MULTIPLIER_PROPERTY_NAME;
                    uiMultiplier = System.getProperty(propertyName);
                    if (uiMultiplier != null) {
                        try {
                            float multiplier = Float.parseFloat(uiMultiplier);
                            setUIfontSize(multiplier);
                        }
                        catch (NumberFormatException e) {
                            System.err.println(String.format("?invalid value for %s: %s", //$NON-NLS-1$
                                    propertyName, uiMultiplier));
                        }
                    }
                }
                catch (SecurityException e) {
                    System.err.println(String.format("Ignoring: %s %s", //$NON-NLS-1$
                            e.getClass().getSimpleName(),
                            e.getMessage()));
                }

                GuiUtil.initLookAndFeel();

                try {
                    KDXploreFrame frame = new KDXploreFrame(appFolder,
                            kdxploreTitle,
                            KdxConstants.VERSION_CODE,
                            KdxConstants.VERSION,
                            updateChecker);
                    frame.setVisible(true);
                    if (onCreateCallback != null) {
                        onCreateCallback.execute(frame);
                    }
                }
                catch (IOException e) {
                    MsgBox.error(null, e, Msg.ERRTITLE_UNABLE_TO_START_KDXPLORE(KDXPLORE_APP_NAME));
                }

            }
        });
    }

    private static String buildKdxploreTitle(String baseTitle, String expiresIn,
            String configName) {
        StringBuilder sb = new StringBuilder();
        if (RunMode.DEMO == RunMode.getRunMode()) {
            sb.append("[DEMO] "); //$NON-NLS-1$
        }
        // Use configName to expand on the title
        if (configName != null && !configName.isEmpty()) {
            sb.append(baseTitle);
            sb.append(" (").append(configName).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(expiresIn);
        }
        final String kdxploreTitle = sb.toString();
        return kdxploreTitle;
    }

    private static void doStaticInitChecks(boolean quiet) {
        try {
            KdxConstants.runStaticInitChecks(true, quiet);
        }
        catch (RuntimeException e) {
            String msg = e.getClass().getName() + "\n" + e.getMessage(); //$NON-NLS-1$
            // deliberately NOT using MsgBox here
            JTextArea ta = new JTextArea(msg);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(null, new JScrollPane(ta),
                    "KDXplore Initialisation Error", //$NON-NLS-1$
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void applyUIdefaultPreferences(KdxplorePreferences prefs) {
        UIDefaults uiDefaults = UIManager.getDefaults();
        for (KdxPreference<?> pref : KdxPreference.getUiDefaultPreferences()) {
            Object value = prefs.getPreferenceValue(pref);
            if (value != null) {
                uiDefaults.put(pref.uiDefaultName, value);
            }
        }
    }

    public static void initLAF() {
        // GuiUtil.initLookAndFeel();
        try {

            for (UIManager.LookAndFeelInfo lafi : UIManager
                    .getInstalledLookAndFeels()) {
                System.out.println(lafi.getName() + "\t" + lafi.getClassName()); //$NON-NLS-1$
            }
            System.out.println("-----"); //$NON-NLS-1$

            // String slaf = "javax.swing.plaf.nimbus.NimbusLookAndFeel"; //
            // UIManager.getSystemLookAndFeelClassName();
            // String slaf = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
            // // UIManager.getSystemLookAndFeelClassName();
            // String slaf = "javax.swing.plaf.metal.MetalLookAndFeel"; //
            // UIManager.getSystemLookAndFeelClassName();
            String slaf = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(slaf);
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        finally {

        }
    }

    // ===========================

    static private java.util.logging.Logger establishLogger(ApplicationFolder appFolder) {
        // Establish logger
        java.util.logging.Logger logger = null;

        try {
            File applicationFolder = appFolder.getApplicationFolder();

            logger = Shared.Log.getLogger();
            if (logger == null) {
                String kdxploreLog = appFolder.getApplicationName().toLowerCase() + ".log"; //$NON-NLS-1$
                File logFile = new File(applicationFolder, kdxploreLog);
                if (logFile.exists()) {
                    File bakFile = new File(applicationFolder, kdxploreLog + ".bak"); //$NON-NLS-1$
                    if (bakFile.exists()) {
                        bakFile.delete();
                    }
                    logFile.renameTo(bakFile);
                }
                java.util.logging.FileHandler fh = new FileHandler(kdxploreLog);
                fh.setFormatter(new SimpleFormatter());
                logger = java.util.logging.Logger.getLogger(appFolder.getApplicationName());
                logger.addHandler(fh);
                Shared.Log.setLogger(logger);

                logger.info("==== Log Started ===="); //$NON-NLS-1$
            }

            ExplorerProperties.getInstance(applicationFolder);
        }
        catch (IOException e1) {
            JOptionPane.showMessageDialog(null, e1.getMessage(),
                    "Unable to initialise environment", //$NON-NLS-1$
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        return logger;
    }
}
