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

import java.util.LinkedHashMap;
import java.util.Map;

// TODO Convert to using this instead of KdxplorePreferences.
public class PreferenceRegistry {
    
    static private final PreferenceRegistry singleton = new PreferenceRegistry();
    
    static public PreferenceRegistry getInstance() {
        return singleton;
    }

    private Map<String, PreferenceCollection> preferencesByApp = new LinkedHashMap<>();
    
    private PreferenceRegistry() {}

    public void addPreferenceSet(PreferenceCollection pc) {
        preferencesByApp.put(pc.appKey, pc);
    }
    
}
