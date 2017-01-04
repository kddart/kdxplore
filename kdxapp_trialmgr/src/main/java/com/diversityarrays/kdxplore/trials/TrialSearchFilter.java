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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.tool.LookupTable;
import com.diversityarrays.kdxplore.data.tool.LookupTables;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

@SuppressWarnings("nls")
public enum TrialSearchFilter {
    
    PROJECT(LookupTables.PROJECT_LOOKUP, true),
    TRIAL_TYPE(LookupTables.TRIAL_TYPE_LOOKUP, true),
    SITE(LookupTables.SITE_LOOKUP, true),
    DESIGN_TYPE(LookupTables.DESIGN_TYPE_LOOKUP, false);
    
    public final LookupTable<?> lookupTable;
    public final boolean isDefault;
    TrialSearchFilter(LookupTable<?> lt, boolean defalt) {
        lookupTable = lt;
        isDefault = defalt;
    }

    // CIMMYT needs to provide a configuration file:
    // TrialSearchFilter
    // TrialSearchFilter.name()=title
    
    // # Example TrialSearchFilter.properties
    // # Disable by making a comment
    // #PROJECT=CIMMYT Project
    // TRIAL_TYPE=CIMMYT Trial Type
    // SITE=CIMMYT Site
    // DESIGN_TYPE=CIMMYT Season
    
    static public final List<Pair<String, LookupTable<?>>> TRIAL_SEARCH_FILTERS;
    static {
        List<Pair<String, LookupTable<?>>> list = new ArrayList<>();
//        try {
//            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("TrialSearchFilter.properties");
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                }
//                catch (IOException ignore) { }
//            }
//            ResourceBundle b = ResourceBundle.getBundle("TrialSearchFilter");
//        }
//        catch (MissingResourceException e) {
//            Shared.Log.w("TrialSearchFilter", "Missing bundle: " + e.getMessage());
//        }
        File pluginsDir = new File(System.getProperty("user.dir"), "plugins");
        File file = new File(pluginsDir, "TrialSearchFilter.properties");
        if (file.exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
                String line;
                int lineNumber = 0;
                while (null != (line = br.readLine())) {
                    ++lineNumber;
                    line = line.trim();
                    if (! line.isEmpty() && ! line.startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            try {
                                TrialSearchFilter f = TrialSearchFilter.valueOf(parts[0]);
                                if (Check.isEmpty(parts[1])) {
                                    Shared.Log.w("TrialSearchFilter", 
                                            file.getName()+"#"+lineNumber+": Empty Filter name: " + line);
                                }
                                else {
                                    Shared.Log.i("TrialSearchFilter", 
                                            "Using name '" + parts[1] + "' for " + f.name());
                                    list.add(new Pair<>(parts[1], f.lookupTable));
                                }
                            }
                            catch (IllegalArgumentException e) {
                                Shared.Log.w("TrialSearchFilter", 
                                        file.getName()+"#"+lineNumber+": Invalid Filter: '" + line + "'");
                            }
                        }
                    }
                }
            }
            catch (IOException e) {
                list.clear();
            }
            finally {
                if (br != null) {
                    try { br.close(); } catch (IOException ignore) {}
                }
            }
        }
        
        if (list.isEmpty()) {
            Shared.Log.i("TrialSearchFilter", "Using default filters");
            for (TrialSearchFilter f : values()) {
                String name = Msg.TRIAL_SEARCH_FILTER(f);
                list.add(new Pair<>(name, f.lookupTable));
            }
        }
        
        TRIAL_SEARCH_FILTERS = list;
//        Arrays.asList(DartEntityBeanRegistry.TRIAL_SELECTION_LOOKUPS)
    }
}
