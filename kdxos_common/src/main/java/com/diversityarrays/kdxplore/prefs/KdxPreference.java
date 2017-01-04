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

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.util.Check;

public class KdxPreference<T> {
	
	private static final String TAG = "KdxPreference"; //$NON-NLS-1$
	
//	static public String getPreferenceName(KdxPreference<?> pref) {
//        return Messages.getString(pref.messageId);
//    }

    static private final Map<Class<?>, List<KdxPreference<?>>> ALL_BY_CONTEXT = new HashMap<>();
	
//	static public KdxPreference<?>[] values() {
//		return ALL.toArray(new KdxPreference<?>[ALL.size()]);
//	}

	public final Class<?> contextClass;
	public final Class<T> valueClass;
	public final MessageId messageId;
	public final String key;
	public final T defaultValue;
	
	public final String uiDefaultName;
	
	/**
	 * Return null or empty if value is OK.
	 * Else an error message to explain why the value is not valid.
	 */
	public final Function<T,String> validator;

	public KdxPreference(Class<?> cc, Class<T> vclass, MessageId id, String key, T defaultValue) {
	    this(cc, vclass, id, key, defaultValue, null, null);
	}

	public KdxPreference(Class<?> cc, Class<T> vclass, MessageId id, String key, T defaultValue,
	        String uiDefaultName) 
	{
        this(cc, vclass, id, key, defaultValue, uiDefaultName, null);
	}

	public KdxPreference(Class<?> cc, Class<T> vclass, MessageId id, String key, T defaultValue,
	        Function<T,String> validator)
	{
	    this(cc, vclass, id, key, defaultValue, null, validator);
	}

	public KdxPreference(Class<?> cc, Class<T> vclass, MessageId id, String key, T defaultValue,
	        String uiDefaultName,
	        Function<T,String> validator)
	{
	    contextClass = cc;
		valueClass = vclass;
		this.messageId = id;
		
//		String tmpkey = contextClass.getName() + "/" + key; //$NON-NLS-1$
//		if (tmpkey.startsWith(COM_DIVERSITYARRAYS_KDXPLORE_DOT)) {
//			tmpkey = "cdk." + key.substring(COM_DIVERSITYARRAYS_KDXPLORE_DOT.length());
//		}
//		this.key = tmpkey;

		this.key = key;

		this.defaultValue = defaultValue;
		// Optional ones - may be null
		this.uiDefaultName = uiDefaultName;
		this.validator = validator;
		
		List<KdxPreference<?>> list = ALL_BY_CONTEXT.get(contextClass);
		if (list == null) {
		    list = new ArrayList<>();
		    ALL_BY_CONTEXT.put(contextClass, list);
		}
		list.add(this);
	}
	
//	@Override
//	public boolean equals(Object o) {
//	    if (this==o) return true;
//	    if (! (o instanceof KdxPreference)) return false;
//	    KdxPreference other = (KdxPreference) o;
//	    return this.key.equals(other.key);
//	}
	
	@Override
	public String toString() {
		return messageId.name;
	}
	
	public String getName() {
	    return Msg.getMessageIdText(messageId); 
	}
	
	public boolean isForInputDir() {
	    return false;
	}
	
    public void handleChange(T value) { }
	
	static public <T> void setValue(Preferences  preferences, KdxPreference<T> pref, T value) {
		if (value == null) {
			preferences.remove(pref.key);
		}
		else {
			String svalue;
			if (value instanceof Enum) {
				svalue = ((Enum<?>) value).name();
			}
			else if (value instanceof File) {
				svalue = ((File) value).getPath();
			}
			else if (value instanceof Color) {
				Color c = (Color) value;
				svalue = String.format("0x%02x%02x%02x",  //$NON-NLS-1$
	                    c.getRed(), c.getGreen(), c.getBlue());
			}
			else {
				svalue = value.toString();
			}
			preferences.put(pref.key, svalue);
		}
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
		    Shared.Log.w(TAG, "setValue: " + pref.messageId, e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static public <T> T getValue(Preferences preferences, KdxPreference<T> pref, T overrideDefault) {
		T result = pref.defaultValue;
		
		String svalue = preferences.get(pref.key, null);
		if (svalue != null) {
			Class<T> vclass = pref.valueClass;
			if (Boolean.class == vclass) {
				result = (T) Boolean.valueOf(svalue);
			}
			else if (Number.class.isAssignableFrom(vclass)) {
	            if (Double.class == vclass) {
	                try {
	                    result = (T) Double.valueOf(svalue);
	                }
	                catch (NumberFormatException e) { }
	            }
	            else if (Float.class == vclass) {
	                try {
	                    result = (T) Float.valueOf(svalue);
	                }
	                catch (NumberFormatException e) { }
	                
	            }
	            else if (Integer.class == vclass) {
	                try {
	                    result = (T) Integer.valueOf(svalue);
	                }
	                catch (NumberFormatException e) { }
	                
	            }
	            else if (Long.class == vclass) {
	                try {
	                    result = (T) Long.valueOf(svalue);
	                }
	                catch (NumberFormatException e) { }
	            }
	            else {
	                Class<? extends Number> nclass = (Class<? extends Number>) vclass;
	                try {
                        Constructor<? extends Number> ctor = nclass.getConstructor(String.class);
                        try {
                            result = (T) ctor.newInstance(svalue);
                        }
                        catch (InstantiationException | IllegalAccessException
                                | IllegalArgumentException | InvocationTargetException e) 
                        {
                            throw new RuntimeException("Can't create " + nclass.getName() //$NON-NLS-1$
                                + "(" + svalue + ")",  //$NON-NLS-1$ //$NON-NLS-2$
                                    e);
                        }
                    }
                    catch (NoSuchMethodException | SecurityException e) {
                        throw new RuntimeException("No constructor for " + nclass.getName() + "(String)");  //$NON-NLS-1$//$NON-NLS-2$
                    }
	            }
			}
			else if (String.class == vclass) {
				result = (T) svalue;
			}
			else if (Enum.class.isAssignableFrom(vclass)) {
	            try {
	                Class<? extends Enum> vc = (Class<? extends Enum>) vclass;
	                Enum eValue = Enum.valueOf(vc, svalue);
	                result = (T) eValue;
	            }
	            catch (IllegalArgumentException e) { }
			}
			else if (Color.class == vclass) {
				try {
					result = (T) Color.decode(svalue);
				} catch (NumberFormatException e) {
						result = (T) Color.RED;
						Shared.Log.w(TAG,
						        String.format("%s.getValue(%s) : %s", //$NON-NLS-1$
						                pref.messageId.name,
						                svalue,
						                e.getMessage()));
						e.printStackTrace();
				}
			}
			else if (File.class == vclass) {
				result = (T) new File(svalue);
			}
			else {
				Shared.Log.e(TAG, 
				        String.format(
				                "%s.getValue(%s): Unsupported class %s\nreturning %s", //$NON-NLS-1$
				                pref.messageId.name,
				                svalue,
				                vclass.getName(),
				                (result==null ? "<null>" : result.toString()))); //$NON-NLS-1$
			}
		}

		
		return result;
	}

    public static List<KdxPreference<?>> getUiDefaultPreferences() {
        return ALL_BY_CONTEXT.values().stream()
                .flatMap(tmp -> tmp.stream())
                .filter(x -> ! Check.isEmpty(x.uiDefaultName))
                .collect(Collectors.toList());
    }

    public static List<KdxPreference<?>> getKdxPreferences(Class<?> clz) {
        List<KdxPreference<?>> list = ALL_BY_CONTEXT.get(clz);
        return list==null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

}
