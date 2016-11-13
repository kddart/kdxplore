/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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
package com.diversityarrays.kdxplore.prefs;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import com.diversityarrays.kdxplore.KdxConstants;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.services.KdxApp;

@SuppressWarnings("nls")
public class KdxplorePreferences extends KdxAppPreferences {

    static public final Map<String, String> BRANCH_NAME_BY_PATH_COMPONENT;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("apps", Msg.GROUP_APPLICATIONS()); 
        map.put("ui", Msg.GROUP_USER_INTERFACE());
        map.put("io", Msg.GROUP_INPUT_OUTPUT());

        map.put("KDXplore", "KDXplore");

        BRANCH_NAME_BY_PATH_COMPONENT = Collections.unmodifiableMap(map);
    }

    static private KdxplorePreferences singleton;

    static public KdxplorePreferences getInstance() {
        if (singleton == null) {
            synchronized (KdxplorePreferences.class) {
                if (singleton == null) {
                    singleton = new KdxplorePreferences(
                            Preferences.userNodeForPackage(KdxConstants.class));
                }
            }
        }
        return singleton;
    }

    private final Map<String, List<PreferenceCollection>> prefcollByKey = new LinkedHashMap<>();

    private KdxplorePreferences(Preferences prefs) {
        super(prefs);
        
        PreferenceCollection pc = new PreferenceCollection("KDXplore", BRANCH_NAME_BY_PATH_COMPONENT);
        pc.addAll(KdxPreference.getKdxPreferences(KdxplorePreferences.class));
        addPreferenceCollection(pc);
    }

    public void addPreferenceCollection(PreferenceCollection pc) {
    	List<PreferenceCollection> list = prefcollByKey.get(pc.appKey);
    	if (list == null) {
    		list = new ArrayList<>();
    		prefcollByKey.put(pc.appKey, list);
    	}
    	list.add(pc);
    }

    public List<KdxPreference<?>> getKdxPreferences() {
        List<KdxPreference<?>> result = new ArrayList<>();
        for (String appKey : prefcollByKey.keySet()) {
        	for (PreferenceCollection pc : prefcollByKey.get(appKey)) {
                result.addAll(pc.getKdxPreferences());
        	}
        }
        return result;
    }

    public List<PreferenceCollection> getPreferenceCollections() {
        List<PreferenceCollection> result = new ArrayList<>();
        // Not using stream because I want them in the order they got added.
        for (String appKey : prefcollByKey.keySet()) {
            result.addAll(prefcollByKey.get(appKey));
        }
        return result;
    }

    public File askOutputDirectory(Component parent, String dialogTitle,
            FileFilter... fileFilters) {
        File result = null;

        JFileChooser chooser = getOutputFileChooser(dialogTitle, JFileChooser.DIRECTORIES_ONLY,
                false, fileFilters);
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(parent)) {
            result = chooser.getSelectedFile();
            saveOutputDirectory(result);
        }
        return result;
    }

    public File askInputDirectory(Component parent, String dialogTitle, FileFilter... fileFilters) {
        File result = null;

        JFileChooser chooser = getInputFileChooser(dialogTitle, JFileChooser.DIRECTORIES_ONLY,
                false, fileFilters);
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(parent)) {
            result = chooser.getSelectedFile();
            saveInputDirectory(result);
        }
        return result;
    }

    private JFileChooser fileChooser;

    public JFileChooser getInputFileChooser() {
        return getInputFileChooser(null, JFileChooser.FILES_ONLY, false);
    }

    public JFileChooser getInputFileChooser(boolean multiple) {
        return getInputFileChooser(null, JFileChooser.FILES_ONLY, multiple);
    }

    public JFileChooser getInputFileChooser(String dialogTitle, int selectionMode, boolean multiple,
            FileFilter... fileFilters) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
        }

        initialiseFileChooser(fileChooser, dialogTitle, selectionMode, multiple,
                getInputDirectory(), fileFilters);

        return fileChooser;
    }

    public JFileChooser getOutputFileChooser(String dialogTitle, int selectionMode,
            boolean multiple, FileFilter... fileFilters) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
        }

        initialiseFileChooser(fileChooser, dialogTitle, selectionMode, multiple,
                getOutputDirectory(), fileFilters);

        return fileChooser;
    }

    private void initialiseFileChooser(JFileChooser fc,
            String dialogTitle,
            int selectionMode,
            boolean multiple,
            File initialDir,
            FileFilter[] fileFilters) {
        fc.setFileSelectionMode(selectionMode);

        fc.setMultiSelectionEnabled(multiple);

        if (dialogTitle != null) {
            fc.setDialogTitle(dialogTitle);
        }

        if (initialDir != null) {
            fc.setCurrentDirectory(initialDir);
        }

        for (FileFilter ff : fc.getChoosableFileFilters()) {
            fc.removeChoosableFileFilter(ff);
        }

        if (fileFilters != null && fileFilters.length > 0) {
            for (FileFilter ff : fileFilters) {
                fc.addChoosableFileFilter(ff);
            }
        }
    }

    public File askInputFile(Component parent, String dialogTitle, FileFilter... fileFilters) {
        File result = null;

        JFileChooser chooser = getInputFileChooser(dialogTitle, JFileChooser.FILES_ONLY, false,
                fileFilters);
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(parent)) {
            result = chooser.getSelectedFile();
            saveInputDirectory(result.getParentFile());
        }

        return result;
    }

    public File[] askInputFiles(Component parent, String dialogTitle, FileFilter... fileFilters) {
        File[] result = null;

        JFileChooser chooser = getInputFileChooser(dialogTitle, JFileChooser.FILES_ONLY, true,
                fileFilters);
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(parent)) {
            result = chooser.getSelectedFiles();
            if (result.length > 0) {
                saveInputDirectory(result[0].getParentFile());
            }
        }

        return result;
    }

    public File askInputFileOrDirectory(Component parent, String dialogTitle,
            FileFilter... fileFilters) {
        File result = null;
        JFileChooser chooser = getInputFileChooser(dialogTitle, JFileChooser.FILES_AND_DIRECTORIES,
                false, fileFilters);
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(parent)) {
            result = chooser.getSelectedFile();
            saveInputDirectory(result.getParentFile());
        }
        return result;
    }

    public Color getAlternateRowColor() {
        return getPreferenceValue(ALTERNATE_ROW_COLOR);
    }

    public Integer getSplitPaneDividerSize() {
        return getPreferenceValue(SPLIT_PANE_DIVIDER_SIZE);
    }

    // kdx_curation, kdxapp_trialmgr
    public int getTooltipLineLengthLimit() {
        return getPreferenceValue(TOOLTIP_LINE_LENGTH_LIMIT);
    }


    // kdx_main
    public boolean getShowAllApps() {
        Boolean b = getPreferenceValue(SHOW_ALL_APPS);
        return b != null && b;
    }

    // kdx_main
    public void setShowAllApps(boolean b) {
        savePreferenceValue(SHOW_ALL_APPS, b);
    }

    public CsvDelimiter getCsvDelimiter() {
        return getPreferenceValue(CSV_DELIMITER);
    }

    public void saveCsvDelimiter(CsvDelimiter d) {
        savePreferenceValue(CSV_DELIMITER, d);
    }

    // kdx_common, others
    public void saveOutputDirectory(File dir) {
        savePreferenceValue(OUTPUT_FOLDER_NAME, dir);
    }

    // kdx_common, others
    public File getOutputDirectory() {
        return getPreferenceValue(OUTPUT_FOLDER_NAME);
    }

    // kdx_common, others
    public void saveInputDirectory(File dir) {
        savePreferenceValue(INPUT_FOLDER_NAME, dir);
    }

    // kdx_common, others
    public File getInputDirectory() {
        return getPreferenceValue(INPUT_FOLDER_NAME);
    }

    // = = = = = = = = =

//    public <T> T getPreferenceValue(KdxPreference<T> pref) {
//        return KdxPreference.getValue(preferences, pref,
//                pref.defaultValue);
//    }
//
//    public <T> T getPreferenceValue(KdxPreference<T> pref, T overrideDefault) {
//        return KdxPreference.getValue(preferences, pref,
//                overrideDefault == null ? pref.defaultValue : overrideDefault);
//    }
//
//    public <T> void savePreferenceValue(KdxPreference<T> pref, T value) {
//        KdxPreference.setValue(preferences, pref, value);
//        ChangeListener[] listeners = null;
//        synchronized (changeListenersByPreference) {
//            Set<ChangeListener> set = changeListenersByPreference.get(pref);
//            if (set != null) {
//                listeners = set.toArray(new ChangeListener[set.size()]);
//            }
//        }
//        try {
//            if (listeners != null) {
//                ChangeEvent event = new ChangeEvent(pref);
//                for (ChangeListener l : listeners) {
//                    l.stateChanged(event);
//                }
//            }
//        }
//        finally {
//            pref.handleChange(value);
//        }
//    }

    // Generic
    private final KdxPreference<Boolean> CLOSE_KDXPLORE_WITHOUT_ASKING = new KdxPreference<Boolean>(
            KdxplorePreferences.class, Boolean.class,
            new MessageId("Close KDXplore without confirming"),
            "ui/closeKdxploreWithoutAsking", false); //$NON-NLS-1$

    private final KdxPreference<Integer> SPLIT_PANE_DIVIDER_SIZE = new KdxPreference<Integer>(
            KdxplorePreferences.class, Integer.class,
            new MessageId("Size of Split Pane Divider"),
            "ui/splitPaneDividerSize", //$NON-NLS-1$
            15, "SplitPane.dividerSize"); //$NON-NLS-1$

    private final KdxPreference<Integer> TOOLTIP_LINE_LENGTH_LIMIT = new KdxPreference<Integer>(
            KdxplorePreferences.class, Integer.class,
            new MessageId("Max character per line for tooltip"),
            "ui/tooltipLineLengthLimit", 60); //$NON-NLS-1$

    // Make this generic
    private final KdxPreference<Color> ALTERNATE_ROW_COLOR = new KdxPreference<Color>(
            KdxplorePreferences.class, Color.class,
            new MessageId("Colour for Odd numbered Table Rows"),
            "ui/alternateRowColour", //$NON-NLS-1$
            new Color(240, 240, 240), "Table.alternateRowColor")
    {
        @Override
        public void handleChange(Color value) {
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            defaults.put("Table.alternateRowColor", value); //$NON-NLS-1$
        }
    };

    private final KdxPreference<File> OUTPUT_FOLDER_NAME = new KdxPreference<File>(
            KdxplorePreferences.class, File.class,
            new MessageId("Last used folder for exported data"),
            "io/outputFolderName", //$NON-NLS-1$
            new File(System.getProperty("user.home"))); //$NON-NLS-1$

    private final KdxPreference<File> INPUT_FOLDER_NAME = new KdxPreference<File>(
            KdxplorePreferences.class, File.class,
            new MessageId("Last used folder for data import"),
            "io/inputFolderName", //$NON-NLS-1$
            new File(System.getProperty("user.home"))) { //$NON-NLS-1$
        @Override
        public boolean isForInputDir() {
            return true;
        }
    };

    private final KdxPreference<Boolean> SHOW_ALL_APPS = new KdxPreference<Boolean>(
            KdxplorePreferences.class, Boolean.class,
            new MessageId("Show all Hidden Apps"),
            "apps/showAllApps", true) //$NON-NLS-1$
    {
        @Override
        public void handleChange(Boolean value) {
            if (value != null && value) {
                ExplorerProperties.getInstance().clearAppNamesToHide();
            }
        }
    };
    
    // make this generic
    private final KdxPreference<CsvDelimiter> CSV_DELIMITER = new KdxPreference<CsvDelimiter>(
            KdxplorePreferences.class, CsvDelimiter.class,
            new MessageId("Delimiter when exporting CSV files"),
            "io/csvDelimiter", //$NON-NLS-1$
            CsvDelimiter.TAB);

    public boolean getCloseKdxploreWithoutAsking() {
        return getPreferenceValue(CLOSE_KDXPLORE_WITHOUT_ASKING);
    }

    public void saveCloseKdxploreWithoutAsking(boolean b) {
        savePreferenceValue(CLOSE_KDXPLORE_WITHOUT_ASKING, b);
    }

    @Override
    public PreferenceCollection getPreferenceCollection(KdxApp app, String appGroupName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Expose the actual Preferences object for those who need it.
     * @return
     */
	public Preferences getPreferences() {
		return preferences;
	}

	public void saveOnShutdown() {
		if (preferences != null) {
			try {
				preferences.flush();
			} catch (BackingStoreException e) {
				Shared.Log.w("KdxplorePreferences", "saveOnShutdown", e);
			}
		}
	}
}
