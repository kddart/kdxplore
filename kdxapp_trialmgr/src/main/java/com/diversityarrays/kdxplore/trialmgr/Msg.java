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
package com.diversityarrays.kdxplore.trialmgr;

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
public class Msg extends AbstractMsg {
    
    static public final Msg i = new Msg();
    
    private Msg() { }
    
    public static String APPNAME_TRIAL_MANAGER() {
        return i.getString("APPNAME_TRIAL_MANAGER");
    }

    public static String TAB_TRIALS() {
        return i.getString("TAB_TRIALS");
    }


    public static String TAB_TAGS() {
        return i.getString("TAB_TAGS");
    }
    
    public static String TAB_INIT_ERROR() {
        return i.getString("TAB_INIT_ERROR");
    }
    
    public static String TITLE_MULTIPLE_DATABASES_FOUND() {
        return i.getString("TITLE_MULTIPLE_DATABASES_FOUND");
    }
    
    public static String MSG_CHOOSE_DATABASE_TO_OPEN_DEFAULT(String defaultName) {
        return i.getString("MSG_CHOOSE_DATABASE_TO_OPEN_DEFAULT", defaultName);
    }

    
    public static String TOOLTIP_NO_CURRENT_KDDART_CONNECTION() {
        return i.getString("TOOLTIP_NO_CURRENT_KDDART_CONNECTION");
    }

    public static String MSG_NOT_LOGGED_IN() {
        return i.getString("MSG_NOT_LOGGED_IN");
    }

    public static String MSG_SERVICE_NOT_AVAILABLE() {
        return i.getString("MSG_SERVICE_NOT_AVAILABLE");
    }

    public static String MSG_CONNECTED_AS_USER(String userName) {
        return i.getString("MSG_CONNECTED_AS_USER", userName);
    }

    public static String MSG_UNABLE_TO_START_DEVICE_SERVER_NAME_CAUSE(String name, String cause) {
        return i.getString("MSG_UNABLE_TO_START_DEVICE_SERVER_NAME_CAUSE", name, cause);
    }
    
    public static String MSG_UNABLE_TO_START_KDXCHANGE_SERVER_NAME_CAUSE(String name, String cause) {
        return i.getString("MSG_UNABLE_TO_START_KDXCHANGE_SERVER_NAME_CAUSE", name, cause);
    }
    
    public static String MSG_UNABLE_TO_START_GENOTYPE_EXPLORER_NAME_CAUSE(String name, String cause) {
        return i.getString("MSG_UNABLE_TO_START_GENOTYPE_EXPLORER_NAME_CAUSE", name, cause);
    }
    
    public static String ACTION_KDXCHANGE_SERVER() {
        return i.getString("ACTION_KDXCHANGE_SERVER");
    }
    
    public static String ACTION_DEVICE_SERVER() {
        return i.getString("ACTION_DEVICE_SERVER");
    }
    
    

    public static String ACTION_GENOTYPE_EXPLORER() {
        return i.getString("ACTION_GENOTYPE_EXPLORER");
    }
    public static String ERRMSG_CHECK_MESSSAGES_URL_CAUSE(String url, String cause) {
        return i.getString("ERRMSG_CHECK_MESSSAGES_URL_CAUSE", url, cause);
    }
    
    public static String ERRMSG_THIS_FUNCTION_NOT_AVAILABLE() {
        return i.getString("ERRMSG_THIS_FUNCTION_NOT_AVAILABLE");
    }

    public static String TOOLTIP_DISPLAY_KDXCHANGE_SERVER() {
        return i.getString("TOOLTIP_DISPLAY_KDXCHANGE_SERVER");
    }

    public static String TOOLTIP_DISPLAY_GENOTYPE_EXPLORER() {
        return i.getString("TOOLTIP_DISPLAY_GENOTYPE_EXPLORER");
    }

    public static String TOOLTIP_DISPLAY_DEVICE_SERVER() {
        return i.getString("TOOLTIP_DISPLAY_DEVICE_SERVER");
    }

    
    public static String GROUP_EXPORT() {
        return i.getString("GROUP_EXPORT");
    }

    public static String TAB_TRAITS() {
        return i.getString("TAB_TRAITS");
    }
}
