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
package com.diversityarrays.kdxplore.prefs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.KdxConstants;
import com.diversityarrays.kdxplore.services.KdxApp;

public abstract class KdxAppPreferences {

    /*
     * Excel 2003 constraints: Make sure the name you entered does not exceed 31
     * characters. Make sure the name does not contain any of the following
     * characters: : \ / ? * [ or ] Make sure you did not leave the name blank.
     */
    static public final Function<String, String> SHEET_NAME_VALIDATOR = new Function<String, String>() {
        @Override
        public String apply(String t) {
            String s = t.trim();
            if (s.isEmpty()) {
                return Msg.EXCEL_NAME_MUST_NOT_BE_BLANK();
            }
            if (s.length() > 31) {
                return Msg.EXCEL_NAME_AT_MOST_31_CHARS();
            }
            if (s.matches("^.*[\\[\\\\/\\?\\*\\]:].*")) { //$NON-NLS-1$
                return Msg.EXCEL_NAME_CANNOT_CONTAIN();
            }
            return null;
        }
    };
    
    protected final Preferences preferences;
    
    private final Map<KdxPreference<?>, Set<ChangeListener>> changeListenersByPreference = new HashMap<>();

    public KdxAppPreferences() {
        this.preferences = Preferences.userNodeForPackage(KdxConstants.class);
    }
    
    // for KdxplorePreferences
    /*package*/ KdxAppPreferences(Preferences prefs) {
        this.preferences = prefs;
    }
    
    protected Map<String,String> addGroupNameTo(Map<String,String> input, KdxApp app, String appGroupName) {
    	Map<String,String> result = new HashMap<>();
    	result.putAll(input);
    	result.put(app.getClass().getName(), appGroupName);
    	return result;
    }
    
    abstract public PreferenceCollection getPreferenceCollection(KdxApp app, String appGroupName);

    public final void addChangeListener(KdxPreference<?> pref, ChangeListener l) {
        synchronized (changeListenersByPreference) {
            Set<ChangeListener> set = changeListenersByPreference.get(pref);
            if (set == null) {
                set = new HashSet<>();
                changeListenersByPreference.put(pref, set);
            }
            set.add(l);
        }
    }

    public final void removeChangeListener(KdxPreference<?> pref, ChangeListener l) {
        synchronized (changeListenersByPreference) {
            Set<ChangeListener> set = changeListenersByPreference.get(pref);
            if (set != null) {
                set.remove(l);
                if (set.isEmpty()) {
                    changeListenersByPreference.remove(pref);
                }
            }
        }
    }
    
    public final <T> T getPreferenceValue(KdxPreference<T> pref) {
        return KdxPreference.getValue(preferences, pref,
                pref.defaultValue);
    }
    
    public final <T> T getPreferenceValue(KdxPreference<T> pref, T overrideDefault) {
        return KdxPreference.getValue(preferences, pref,
                overrideDefault == null ? pref.defaultValue : overrideDefault);
    }

    public final <T> void savePreferenceValue(KdxPreference<T> pref, T value) {
        KdxPreference.setValue(preferences, pref, value);
        ChangeListener[] listeners = null;
        synchronized (changeListenersByPreference) {
            Set<ChangeListener> set = changeListenersByPreference.get(pref);
            if (set != null) {
                listeners = set.toArray(new ChangeListener[set.size()]);
            }
        }
        try {
            if (listeners != null) {
                ChangeEvent event = new ChangeEvent(pref);
                for (ChangeListener l : listeners) {
                    l.stateChanged(event);
                }
            }
        }
        finally {
            pref.handleChange(value);
        }
    }


}
