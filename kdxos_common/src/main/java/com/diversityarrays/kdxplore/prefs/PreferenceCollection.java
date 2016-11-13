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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdxplore.services.KdxApp;

public class PreferenceCollection {

    public final String appKey;
    private final KdxApp kdxApp;
    private final Map<String, KdxPreference<?>> preferenceMap = new HashMap<>();

    private final Map<String, String> keyToDisplayName = new HashMap<>();
    // For use by KdxplorePreference internal
    /* package */ PreferenceCollection(String appKey, Map<String, String> k2d) {
        kdxApp = null;
        this.appKey = appKey;
        
        this.keyToDisplayName.putAll(k2d);
    }
    
    public PreferenceCollection(KdxApp kdxApp, Map<String, String> k2d) {
        this.kdxApp = kdxApp;
        this.appKey = kdxApp.getClass().getName();
        
        this.keyToDisplayName.putAll(k2d);
    }
    
    @Override
    public String toString() {
        return "[PreferenceCollection: " + appKey + "]";  //$NON-NLS-1$//$NON-NLS-2$
    }
    
    public Collection<KdxPreference<?>> getKdxPreferences() {
        return preferenceMap.values();
    }
    
    @Override
    public int hashCode() {
        return appKey.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof PreferenceCollection)) return false;
        return this.appKey.equals(((PreferenceCollection) o).appKey);
    }
    
    public void addAll(List<KdxPreference<?>> list) {
        for (KdxPreference<?> p : list) {
            add(p);
        }
    }
    
    public void add(KdxPreference<?> pref) {
        if (preferenceMap.containsKey(pref.key)) {
            String msg = MessageFormat.format("Key ''{0}.{1}'' is already stored", 
                    kdxApp.getAppName(), pref.key);
            throw new IllegalArgumentException(msg);
        }
        preferenceMap.put(pref.key, pref);
    }

    /**
     * Sub-classes should override this.
     * @param pathComponent
     * @return
     */
    public String getPathComponentName(String pathComponent) {
        String result = keyToDisplayName.get(pathComponent);
        if (result == null) { 
            result = pathComponent.toUpperCase();
        }
        return result;
    }

}
