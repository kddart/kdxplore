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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import com.diversityarrays.kdxplore.config.KdxConfigService;
import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.prefs.ExplorerProperties;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;

import net.pearcan.application.ApplicationFolder;

@SuppressWarnings("nls")
class CommandArgs {

    private static final String NL_PLEASE_CONTACT_DART = "\nPlease contact Diversity Arrays Technology for a replacement";
    

    static private final String[] HELP_LINES = { "Options are:",
            "  -version to show the version number", 
            "  -user to turn off the DAL explorer",
            "  -dalx to turn on the DAL explorer",
            "  -demo for demo mode",
            "  Logging args:      -log SEVERE | WARNING | INFO | DEBUG",
            "  -initchecks  run initialisation checks then exit",
            "  -quiet  don't print ClassLoader initialisation messages"
            };


    public static final String UI_MULTIPLIER_PROPERTY_NAME = "KDXplore.uiMultiplier";
    
    private static final float DEFAULT_UI_MULTIPLIER = 1.4f;

    public final ApplicationFolder appFolder;

    public String errmsg = null;
    public boolean debug = false;

    public final boolean runInitChecks;

    public File baseDir = null;
    
    public String expiryMessage;
    public String expiresInMessage;

    // Set in establishConfig()
    public KdxConfigService kdxConfigService = null;


	private final String appName;


    public boolean quiet;
    
    CommandArgs(ApplicationFolder af, String version, int versionCode, String[] args) {

        appFolder = af;
        
        appName = appFolder.getApplicationName();
        
        expiryMessage = null;
        if (KdxConstants.getVersionSubinfo() <= 0) {
            expiryMessage = "This copy of "
                    + appName
                    + " (version "
                    + version
                    + ") has expired.";
        }
        expiresInMessage = null;
        if (KdxConstants.getVersionSubinfo() < 10) {
            expiresInMessage = "This copy expires in " + KdxConstants.getVersionSubinfo() + " days.";
        }
        
        boolean runChecks = false;
        
        for (int argidx = 0; argidx < args.length; ++argidx) {
            String argi = args[argidx];
            if (argi.startsWith("-")) { //$NON-NLS-1$
                if ("--".equals(argi)) { //$NON-NLS-1$
                    break;
                }

                if ("-quiet".equals(argi)) {
                    quiet = true;
                }
                else if ("-initchecks".equals(argi)) {
                    runChecks = true;
                }
                else if (argi.startsWith("-log")) {
                    if (++argidx >= args.length || args[argidx].startsWith("-")) {
                        errmsg = "Missing value for -log";
                        System.err.println(errmsg);
                        break;
                    }
                    String s = args[argidx];
                    try {
                        if ("DEBUG".equalsIgnoreCase(s)) {
                            Shared.Log.setLoggingLevel(Level.FINEST);
                        }
                        else {
                            try {
                                Shared.Log.setLoggingLevel(Level.parse(s));
                            }
                            catch (IllegalArgumentException e) {
                                System.err.println("?Invalid logging level: " + s+ ", using FINEST");
                                Shared.Log.setLoggingLevel(Level.FINEST);
                            }
                        }
                    }
                    catch (IllegalArgumentException e) {
                        errmsg = "?Invalid -loa: " + s;
                        break;
                    }
                }
                else if ("-help".equals(argi)) { //$NON-NLS-1$
                    StringBuilder sb = new StringBuilder(appName + " v" + version + "(" + versionCode + ")");
                    for (String h : HELP_LINES) {
                        sb.append('\n').append(h);
                    }
                    errmsg = sb.toString();
                    System.out.println(errmsg);
                    break;
                } else if ("-debug".equals(argi)) { //$NON-NLS-1$
                    debug = true;
                } else if ("-basedir".equals(argi)) { //$NON-NLS-1$
                    if (++argidx >= args.length || args[argidx].startsWith("-")) {
                        errmsg = "Missing parameter for " + argi;
                        break;
                    }
                    baseDir = new File(args[argidx]);
                } else if ("-version".equals(argi)) { //$NON-NLS-1$
                    System.out.println(appName + " version: " + version); //$NON-NLS-1$
                    System.out.println(appName + " versionCode: " + versionCode); //$NON-NLS-1$
                    String buildDate = KdxConstants.getKdxploreBuildDate();
                    System.out.println(KdxConstants.BUILD_DATE + ": " + buildDate); //$NON-NLS-1$
                    
                    showExpiry(System.err);
                    System.exit(0);
                } else if ("-expires".equals(argi)) { //$NON-NLS-1$
                    showExpiry(System.out);
                    System.exit(0);
                } else {
                    errmsg = "Invalid option: " + argi;
                    break;
                }
            } else {
                errmsg = "Unexpected arg: " + argi;
                break;
            }
        }
        runInitChecks = runChecks;
        // Some final RunMode variations

        if (RunMode.DEMO == RunMode.getRunMode()) {
            String uiMultiplier = System.getProperty(UI_MULTIPLIER_PROPERTY_NAME);
            if (Check.isEmpty(uiMultiplier)) {
                System.setProperty(UI_MULTIPLIER_PROPERTY_NAME, Float.toString(DEFAULT_UI_MULTIPLIER));
            }
        }        
    }


    private void showExpiry(PrintStream ps) {
        if (expiryMessage != null) {
            ps.println("!!!!!! WARNING");
            ps.println(expiryMessage);
        }
        else if (expiresInMessage != null) {
            ps.println("!!!!!! WARNING");
            ps.println(expiresInMessage);
        }
        else if (KdxConstants.getVersionSubinfo() > 0) {
            ps.println(" [Expires in " + KdxConstants.getVersionSubinfo() + " days]");
        }
        else {
            ps.println("No expiry");
        }
    }


    public String establishKdxConfig() {
        
        String result = "";

        ServiceLoader<KdxConfigService> serviceLoader = ServiceLoader.load(KdxConfigService.class);
        Iterator<KdxConfigService> iter = serviceLoader.iterator();
        List<KdxConfigService> services = new ArrayList<>();
        while (iter.hasNext()) {
            services.add(iter.next());
        }

        switch (services.size()) {
        case 0:
//            errmsg = "No " + appName + " Configurations available";
            kdxConfigService = EMPTY_CONFIG;
            break;
        case 1:
            kdxConfigService = services.get(0);
            break;
        default:
            if (RunMode.getRunMode().isDeveloper()) {
                
                String devConfigName = ExplorerProperties.getInstance().getPreviousDeveloperConfigName();
                
                Object initialSelection = null;
                Object[] selectionValues = new Object[services.size()];
                Map<String,KdxConfigService> serviceByName = new HashMap<>();
                int index = 0;
                for (KdxConfigService s : services) {
                    String name = s.getConfigName();
                    selectionValues[index++] = name;
                    serviceByName.put(name, s);
                    
                    if (name.equals(devConfigName)) {
                        initialSelection = name;
                    }
                }
                Object choice = JOptionPane.showInputDialog(null,
                        "Choose Configuration to run with:", 
                        "Developer Mode: Multiple Configurations", 
                        JOptionPane.QUESTION_MESSAGE,
                        null, selectionValues, initialSelection);
                if (choice==null) {
                    errmsg = "Exiting - no Config chosen";
                }
                else {
                    String chosenName = choice.toString();
                    kdxConfigService = serviceByName.get(chosenName);
                    
                    if (! chosenName.equals(devConfigName)) {
                        ExplorerProperties.getInstance().setPreviousDeveloperConfigName(chosenName);
                    }
                }
            }
            else {
                List<Pair<KdxploreConfig,KdxConfigService>> configs = new ArrayList<>();
                for (KdxConfigService s : services) {
                    try {
                        KdxploreConfig cfg = KdxploreConfig.create(s.getInputStream());
                        configs.add(new Pair<>(cfg,s));
                    } catch (IOException ignore) {
                    }
                }
                if (! configs.isEmpty()) {
                    Comparator<Pair<KdxploreConfig, KdxConfigService>> comparator = new Comparator<Pair<KdxploreConfig,KdxConfigService>>() {
                        @Override
                        public int compare(
                                Pair<KdxploreConfig, KdxConfigService> o1,
                                Pair<KdxploreConfig, KdxConfigService> o2) 
                        {
                            return o1.first.compareTo(o2.first);
                        }   
                    };
                    if (RunMode.DEMO == RunMode.getRunMode()) {
                        configs.sort(comparator.reversed());
                    }
                    else {
                        configs.sort(comparator);
                    }
                    kdxConfigService = configs.get(0).second;
                }
            }
            break;
        }
        
        if (errmsg == null) {
            if (kdxConfigService == null) {
                errmsg = "No usable " + appName + " Configuration available";
            }
            else {
                InputStream is2 = kdxConfigService.getInputStream();
                if (is2 == null) {
                    if (EMPTY_CONFIG == kdxConfigService) {
                        try {
                            KdxploreConfig.initInstance(null);
                        }
                        catch (IOException e) {
                            errmsg = "Unable to initialise " + appName + " Configuration\n"
                                    + e.getMessage();
                        }

                    }
                    else {
                        errmsg = "Empty " + appName + " Configuration";
                    }
                }
                else {
                    try {
                        KdxploreConfig.initInstance(is2);
                        result = kdxConfigService.getConfigName();
                    } catch (IOException e) {
                        errmsg = "Unable to initialise " + appName + " Configuration\n"
                                + e.getMessage();
                    }
                }
            }
        }
        
        return result;
    }
    
    static private final KdxConfigService EMPTY_CONFIG = new KdxConfigService() {
        @Override
        public InputStream getInputStream() {
            return null;
        }
        
        @Override
        public String getConfigName() {
            return "Empty Config";
        }
    };

    public void expiryChecks(String version) {
        
        if (errmsg == null) {
            if (KdxConstants.getVersionSubinfo() <= 0) {
                errmsg = expiryMessage + NL_PLEASE_CONTACT_DART;
            }
            else {
                if (expiresInMessage != null) {
                    JOptionPane.showMessageDialog(null, 
                            expiresInMessage + NL_PLEASE_CONTACT_DART,
                            appName + " (v " + version + ") ", //$NON-NLS-1$ //$NON-NLS-2$
                            JOptionPane.WARNING_MESSAGE);
                }
            }       
        }

        if (errmsg != null) {
            JOptionPane.showMessageDialog(null, 
                    errmsg, 
                    appName,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        if (debug) {
            DebugDialog dd = new DebugDialog();
            dd.setVisible(true);
        }
    }

    static public String[] parseRunModeOption(ApplicationFolder defaultAppFolder, String[] args) {
        
        RunMode savedRunMode = readSavedRunMode(defaultAppFolder);
        
        System.out.println("Startup RunMode=" + savedRunMode); //$NON-NLS-1$

        RunMode cmdRunMode = null;
        
        // Step 1: Get RunMode from the environment
        if (Boolean.getBoolean("ENDUSER_MODE")) { //$NON-NLS-1$
            cmdRunMode = RunMode.END_USER;
        } 
        else if (Boolean.getBoolean("DEVELOPMENT_MODE")) { //$NON-NLS-1$
            cmdRunMode = RunMode.DEVELOPER;
        }

        // Step 2: override if command line says to
        List<String> result = new ArrayList<>();
        for (String arg : args) {
            if ("-demo".equals(arg)) {
                cmdRunMode = RunMode.DEMO;
            }
            else if ("-user".equals(arg)) {
                cmdRunMode = RunMode.END_USER;
            }
            else if ("-dalx".equals(arg)) {
                cmdRunMode = RunMode.DEVELOPER;
            }
            else {
                result.add(arg);
            }
        }

        // If environment or command line option set it, persist for future
        if (cmdRunMode != null) {
            savedRunMode = cmdRunMode;
            saveRunMode(defaultAppFolder, cmdRunMode);
        }
        RunMode.setRunMode(savedRunMode);
        
        return result.toArray(new String[result.size()]);
    }

    private static RunMode readSavedRunMode(ApplicationFolder appFolder) {
        RunMode result = RunMode.END_USER;
        File propertiesFile = getPropertiesFile(appFolder);

        if (propertiesFile != null && propertiesFile.exists()) {
            Properties properties = new Properties();
            try {
                properties.load(new FileReader(propertiesFile));
                
                
                String developerValue = properties.getProperty("developer"); //$NON-NLS-1$
                if (developerValue == null) {
                    String savedRunMode = properties.getProperty("savedRunMode");
                    if (! Check.isEmpty(savedRunMode)) {
                        try {
                            result = RunMode.valueOf(savedRunMode);
                        }
                        catch (IllegalArgumentException ignore) { }
                    }
                }
                else {
                    try {
                        result = RunMode.valueOf(developerValue);
                    }
                    catch (IllegalArgumentException e) {
                        if (Boolean.parseBoolean(developerValue)) {
                            result = RunMode.DEVELOPER;
                        }
                        else {
                            result = RunMode.END_USER;
                        }
                    }
                }

                System.out.println("Loaded developer=" + result + " from " //$NON-NLS-1$ //$NON-NLS-2$
                        + propertiesFile.getPath());

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        return result;
    }

    private static void saveRunMode(ApplicationFolder appFolder, RunMode runMode) {

        File propertiesFile = getPropertiesFile(appFolder);

        if (propertiesFile != null) {
            Properties properties = new Properties();
            try {
                if (propertiesFile.exists()) {
                    properties.load(new FileReader(propertiesFile));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            properties.remove("developer"); //$NON-NLS-1$
            properties.setProperty("savedRunMode", runMode.name()); //$NON-NLS-1$
            Writer w = null;
            try {
                w = new FileWriter(propertiesFile);
                properties.store(w, "Saved developer mode"); //$NON-NLS-1$
                System.out.println("Saved " + propertiesFile.getPath()); //$NON-NLS-1$
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (w != null) {
                    try {
                        w.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }

    public static File getPropertiesFile(ApplicationFolder appFolder) {
        File result = null;

        File kdxploreHome = null;
        try {
            kdxploreHome = appFolder.getApplicationFolder();
        } catch (IOException e) {
        }
        
        boolean ok = false;
        if (kdxploreHome == null) {
            File home = new File(System.getProperty("user.home")); //$NON-NLS-1$
            kdxploreHome = new File(home, "." + appFolder.getApplicationName().toLowerCase()); //$NON-NLS-1$
            if (kdxploreHome.exists()) {
                ok = kdxploreHome.isDirectory();
            } else if (kdxploreHome.mkdir()) {
                ok = true;
            }
        }
        else {
            ok = true;
        }

        if (ok) {
            result = new File(kdxploreHome, appFolder.getApplicationName().toLowerCase() + ".properties"); //$NON-NLS-1$
        }
        return result;
    }

}
