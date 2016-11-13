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
package com.diversityarrays.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diversityarrays.kdxplore.Shared;

@SuppressWarnings("nls")
public abstract class AbstractMsg {
    
    private final ResourceBundle bundle;

    protected AbstractMsg() {
        String bundleName = this.getClass().getName();
        
        ResourceBundle b;
        try {
            b = ResourceBundle.getBundle(bundleName);
        }
        catch (MissingResourceException e) {
            e.printStackTrace();
            Shared.Log.w(getClass().getName(), "MISSING " + bundleName + ".properties", e);
            b = null;
        }
        bundle = b;
    }

    static private final String[][] REPLACEMENTS = {
            new String[] { "\\n", "\n"  },
            new String[] { "\\t", "\t"  },
    };
    
    static public final String ALPHA_SUFFIX = " (\u03b1)";
    static public final String BETA_SUFFIX = " (\u03b2)";
  

    static private String replaceEscape(String fmt, String patternString, String replaceWith) {
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile(Pattern.quote(patternString)).matcher(fmt);
        while (m.find()) {
            m.appendReplacement(sb, replaceWith);
            
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    static public String unescape(final String input) {
        String tmp = input;
        if (tmp.indexOf('\\') >= 0) {
            for (String[] pair : REPLACEMENTS) {
                String patternString = pair[0];
                if (tmp.indexOf(patternString) >= 0) {
                    String replaceWith = pair[1];
                    tmp = replaceEscape(tmp, patternString, replaceWith);
                }
            }
            
            Pattern pattern = Pattern.compile("\\\\u([0-9a-f]{4})", Pattern.CASE_INSENSITIVE);
            StringBuffer sb = new StringBuffer();
            Matcher m = pattern.matcher(tmp);
            while (m.find()) {
                String hex_s = m.group(1);
                int hex = Integer.parseInt(hex_s, 16);
                m.appendReplacement(sb, Character.toString((char) hex));
                
            }
            m.appendTail(sb);
            tmp = sb.toString();
        }                

        return tmp;
    }
    
    public final String getString(String key, Object ... args) {
        String result;
        try {
            if (bundle == null) {
                result = handleError("", key, args);
            }
            else {
                String fmt = bundle.getString(key);
                if (args==null || args.length <= 0) {
                    result = fmt;
                }
                else {
                    result = MessageFormat.format(fmt, args);
                }
            }
        }
        catch (IllegalArgumentException e) {
            result = handleError("!", key, args);
        }
        catch (MissingResourceException e) {
            result = handleError("?", key, args);
        }
        return result;
    }

    static public String handleError(String prefix, String key, Object ... args) {
        String result;
        if (args==null || args.length <= 0) {
            result = prefix + key;
        }
        else {
            StringBuilder sb = new StringBuilder(prefix);
            sb.append(key);
            for (Object arg : args) {
                sb.append(',').append(arg==null ? "<null>" : arg.toString());
            }
            result = sb.toString();
        }
        return result;
    }

}
