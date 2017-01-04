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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.diversityarrays.kdxplore.prefs.KdxAppPreferences;
import com.diversityarrays.kdxplore.prefs.KdxPreference;
import com.diversityarrays.kdxplore.prefs.MessageId;
import com.diversityarrays.kdxplore.prefs.PreferenceCollection;
import com.diversityarrays.kdxplore.services.KdxApp;
import com.diversityarrays.util.Check;

public class TrialDesignPreferences extends KdxAppPreferences{

    static private final TrialDesignPreferences singleton = new TrialDesignPreferences();

    static public final Map<String, String> BRANCH_NAME_BY_PATH_COMPONENT;

    private static final Function<String, String> SPATIAL_CHECKS_VALIDATOR =
            new Function<String, String>()
    {
        @Override
        public String apply(String candidate) {
            if (Check.isEmpty(candidate)) {
                return "Name for Spatial Checks must be non-blank"; //$NON-NLS-1$
            }
            return null;
        }
    };

    static {
        Map<String, String> map = new HashMap<>();

//        map.put("algorithm", "Algorithm"); //$NON-NLS-1$ //$NON-NLS-2$
        map.put("manual", "Manual Design"); //$NON-NLS-1$ //$NON-NLS-2$

        BRANCH_NAME_BY_PATH_COMPONENT = Collections.unmodifiableMap(map);
    }

    static public TrialDesignPreferences getInstance() {
        return singleton;
    }

//    private final KdxPreference<String> RSCRIPT_PATH = new KdxPreference<>(
//            TrialDesignPreferences.class, String.class,
//            new MessageId("Location of Rscript exectable"), //$NON-NLS-1$
//            "algorithm/rscriptPath",  //$NON-NLS-1$
//            ""); //$NON-NLS-1$

    private final KdxPreference<String> SPATIAL_ENTRY_TYPE_NAME = new KdxPreference<>(
            TrialDesignPreferences.class, String.class,
            new MessageId("Name for Spatial Checks Entry Type"), //$NON-NLS-1$
            "manual/spatialCheckEntryType",  //$NON-NLS-1$
            "Spatial", //$NON-NLS-1$
            SPATIAL_CHECKS_VALIDATOR);

    private final KdxPreference<String> NORMAL_ENTRY_TYPE_NAME = new KdxPreference<>(
            TrialDesignPreferences.class, String.class,
            new MessageId("Name for Normal Entries"), //$NON-NLS-1$
            "manual/normalEntryTypeName",  //$NON-NLS-1$
            "Entry", //$NON-NLS-1$
            SPATIAL_CHECKS_VALIDATOR);

//    public String getRscriptPath() {
//        return getPreferenceValue(RSCRIPT_PATH);
//    }
//
//    public void setRscriptPath(String value) {
//        savePreferenceValue(RSCRIPT_PATH, value);
//    }

    public String getSpatialEntryName() {
        return getPreferenceValue(SPATIAL_ENTRY_TYPE_NAME);
    }

    public String getNormalEntryTypeName() {
        return getPreferenceValue(NORMAL_ENTRY_TYPE_NAME);
    }


    @Override
    public PreferenceCollection getPreferenceCollection(KdxApp app, String appGroupName) {
        PreferenceCollection pc = new PreferenceCollection(app,
                addGroupNameTo(BRANCH_NAME_BY_PATH_COMPONENT, app, appGroupName));
        List<KdxPreference<?>> list = KdxPreference.getKdxPreferences(TrialDesignPreferences.class);
        pc.addAll(list);
        return pc;
    }

}
