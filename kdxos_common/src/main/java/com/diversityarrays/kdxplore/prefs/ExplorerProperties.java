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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.Shared;

public class ExplorerProperties {

    private static final String COMMA = ","; //$NON-NLS-1$

    private static final String TAG = "ExplorerProperties"; //$NON-NLS-1$

    private static final String APP_NAMES_TO_HIDE         = "appNamesToHide"; //$NON-NLS-1$
	private static final String CURRENT_DATABASE_URL      = "currentDatabaseUrl"; //$NON-NLS-1$
	private static final String CURRENT_DATABASE_USERNAME = "currentDatabaseUsername"; //$NON-NLS-1$
    private static final String DEV_CONFIG_NAME           = "previousDevConfigName"; //$NON-NLS-1$
	private static final String DEVICE_IDENTIFIER_ID      = "deviceIdentifierId"; //$NON-NLS-1$
    private static final String ELAPSED_DAYS_AS_COUNT     = "elapsedDaysAsCount"; //$NON-NLS-1$
	private static final String OPERATOR_NAME             = "operatorName"; //$NON-NLS-1$
	private static final String OUTPUT_DIRECTORY          = "outputDirectory"; //$NON-NLS-1$

	private Properties properties = new Properties();

	private final File dir;
	private final File propertiesFile;

	public ExplorerProperties(File dir) throws IOException {
		this.dir = dir;
		this.propertiesFile = new File(dir, "explorer.properties"); //$NON-NLS-1$
		
		File odir = getOutputDirectory();
		if (odir != null) {
			KdxplorePreferences.getInstance().saveOutputDirectory(odir);			
			properties.remove(OUTPUT_DIRECTORY);
			try {
				save("Remove OutputDirectory"); //$NON-NLS-1$
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}

		}
	}
	
	public File getUserDataDirectory() {
		return dir;
	}
	
	public void load() throws IOException {
		Shared.Log.d(TAG, "Loading " + propertiesFile.getPath()); //$NON-NLS-1$
		Reader r = null;
		try {
			r = new FileReader(propertiesFile);
			properties.load(r);
		} catch (FileNotFoundException fnf) {
			// That's ok
			Shared.Log.w(TAG, "Not found: " + propertiesFile.getPath()); //$NON-NLS-1$
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
	
	public String getPreviousDeveloperConfigName() {
		return properties.getProperty(DEV_CONFIG_NAME);
	}
	
	public void setPreviousDeveloperConfigName(String name) {
		if (name == null) {
			properties.remove(DEV_CONFIG_NAME);
		}
		else {
			properties.setProperty(DEV_CONFIG_NAME, name);
		}
		
		try {
			save("Changed " + DEV_CONFIG_NAME); //$NON-NLS-1$
		} catch (IOException e) {
			Shared.Log.w(TAG, "setPreviousDeveloperConfigName: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	public String getCurrentDatabaseUrl() {
		return properties.getProperty(CURRENT_DATABASE_URL);
	}

	public void setCurrentDatabaseUrl(String baseUrl) {			
		if (baseUrl == null) {
			properties.remove(CURRENT_DATABASE_URL);
		} else {
			properties.setProperty(CURRENT_DATABASE_URL, baseUrl);
		}
	}

	public String getCurrentDatabaseUsername() {
		return properties.getProperty(CURRENT_DATABASE_USERNAME);
	}

	public void setCurrentDatabaseUsername(String username) {
		if (username == null) {
			properties.remove(CURRENT_DATABASE_USERNAME);
		} else {
			properties.setProperty(CURRENT_DATABASE_USERNAME, username);
		}
	}

	public boolean getDisplayElapsedDaysAsCount() {
		return Boolean.parseBoolean(properties.getProperty(ELAPSED_DAYS_AS_COUNT, "true")); //$NON-NLS-1$
	}
	
	public void setDisplayElapsedDaysAsCount(boolean b) {
		properties.setProperty(ELAPSED_DAYS_AS_COUNT, b ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			save("Changed " + ELAPSED_DAYS_AS_COUNT); //$NON-NLS-1$
		} catch (IOException e) {
			Shared.Log.w(TAG, "setDisplayElapsedDaysAsCount: " + e.getMessage()); //$NON-NLS-1$
		}
	}
	
    public Set<String> getAppNamesToHide() {
        Set<String> result = new HashSet<>();
        String namesToHide = properties.getProperty(APP_NAMES_TO_HIDE, ""); //$NON-NLS-1$
        for (String name : namesToHide.split(COMMA)) {
            name = name.trim();
            if (! name.isEmpty()) {
                result.add(name);
            }
        }
        return result;
    }
    
    public void clearAppNamesToHide() {
        properties.remove(APP_NAMES_TO_HIDE);
        try {
            save("Changed " + APP_NAMES_TO_HIDE); //$NON-NLS-1$
        }
        catch (IOException e) {
            Shared.Log.w(TAG, "clearAppNamesToHide: " + e.getMessage()); //$NON-NLS-1$
        }
    }

    public void addAppNameToHide(String appName) {
        Set<String> set = getAppNamesToHide();
        set.add(appName);
        String namesToHide = set.stream().collect(Collectors.joining(COMMA));
        properties.setProperty(APP_NAMES_TO_HIDE, namesToHide);
        
        try {
            save("Changed " + APP_NAMES_TO_HIDE); //$NON-NLS-1$
        }
        catch (IOException e) {
            Shared.Log.w(TAG, "addAppNameToHide: " + e.getMessage()); //$NON-NLS-1$
        }
    }


	public void save(String comment) throws IOException {
		Writer w = null;
		try {
			w = new FileWriter(propertiesFile);
			properties.store(w, comment);
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
	
	public void saveDeviceIdentifierIdAndOperatorName(int id, String opname) throws IOException {
		properties.setProperty(DEVICE_IDENTIFIER_ID, String.valueOf(id));
		properties.setProperty(OPERATOR_NAME, opname);
		save("DeviceId and OperatorName"); //$NON-NLS-1$
	}

	public String getOperatorName() {
		return properties.getProperty(OPERATOR_NAME, ""); //$NON-NLS-1$
	}
	
	public int getDeviceIdentifierId() {
		String s = properties.getProperty(DEVICE_IDENTIFIER_ID, "-1"); //$NON-NLS-1$
		return Integer.valueOf(s);
	}
	
	private File getOutputDirectory() {
		File result = null;
		String path = properties.getProperty(OUTPUT_DIRECTORY, null);
		if (path != null && ! path.isEmpty()) {
			result = new File(path);
		}
		return result;
	}

	private static ExplorerProperties singleton;
	
	public static ExplorerProperties getInstance() {
		if (singleton==null) {
			throw new IllegalStateException("ExplorerProperties.getInstance(File) has not been called"); //$NON-NLS-1$
		}
		return singleton;
	}
	
	public static ExplorerProperties getInstance(File userDataFolder) throws IOException {
		if (singleton == null) {
			singleton = new ExplorerProperties(userDataFolder);
			singleton.load();
		}
		else {
			if (! userDataFolder.equals(singleton.dir)) {
				throw new IOException("ExplorerProperties.getInstance(" //$NON-NLS-1$
				        + userDataFolder.getPath() 
						+ ") previously called with: " + singleton.dir.getPath() ); //$NON-NLS-1$
			}
		}
		return singleton;
	}



}