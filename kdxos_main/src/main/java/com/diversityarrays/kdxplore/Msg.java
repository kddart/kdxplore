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
package com.diversityarrays.kdxplore;

import java.io.File;

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
public class Msg extends AbstractMsg {

    static public final Msg i = new Msg();

    static public String MSG_APP_START_DIRECTORY(String appName, File file) {
        return i.getString("MSG_APP_START_DIRECTORY",
                appName, file.getPath());
    }
    
    private Msg() { }

    public static String ERRTITLE_MISSING_LIBRARY_FILES() {
        return i.getString("ERRTITLE_MISSING_LIBRARY_FILES");
    }
    
    public static String ERRTITLE_UNABLE_TO_START_KDXPLORE(String appName) {
        return i.getString("ERRTITLE_UNABLE_TO_START_KDXPLORE", appName);
    }
    
    public static String KDX_EXPIRES_IN_N_DAYS(int ndays) {
        return i.getString("KDX_EXPIRES_IN_N_DAYS", ndays);
    }
    
    public static String TOOLTIP_CHANGE_PREFS() {
        return i.getString("TOOLTIP_CHANGE_PREFS");
    }
    
    public static String TOOLTIP_CONNECT_TO_DATABASE() {
        return i.getString("TOOLTIP_CONNECT_TO_DATABASE");
    }
    
    public static String TOOLTIP_DISCONNECT_FROM_DATABASE() {
        return i.getString("TOOLTIP_DISCONNECT_FROM_DATABASE");
    }
    
    public static String HDG_MESSAGES() {
        return i.getString("HDG_MESSAGES");
    }
    
    public static String ACTION_EXIT() {
        return i.getString("ACTION_EXIT");
    }
    
    public static String ACTION_ABOUT() {
        return i.getString("ACTION_ABOUT");
    }
    
    public static String ACTION_ONLINE_HELP() {
        return i.getString("ACTION_ONLINE_HELP");
    }
    
    public static String ACTION_PREFERENCES() {
        return i.getString("ACTION_PREFERENCES");
    }
    
    public static String ACTION_CHECK_FOR_UPDATES() {
        return i.getString("ACTION_CHECK_FOR_UPDATES");
    }
    
    public static String TITLE_PREFIX_ABOUT(String title) {
        return i.getString("TITLE_PREFIX_ABOUT", title);
    }

    public static String MSG_KDXAPP_INIT_PROBLEM(String appName) {
        return i.getString("MSG_KDXAPP_INIT_PROBLEM");
    }

    public static String MSG_NO_KDXPLORE_APPS_AVAILABLE() {
        return i.getString("MSG_NO_KDXPLORE_APPS_AVAILABLE");
    }

    public static String MSG_SHOWING_KDXAPP(String appName) {
        return i.getString("MSG_SHOWING_KDXAPP", appName);
    }

    public static String MSG_PROBLEM_GETTING_KDXAPP(String className) {
        return i.getString("MSG_PROBLEM_GETTING_KDXAPP", className);
    }
    
    public static String MSG_CLICK_TO_VISIT_WEBSITE() {
        return i.getString("MSG_CLICK_TO_VISIT_WEBSITE");
    }

    public static String MSG_LOGGED_IN(String newTitle) {
        return i.getString("MSG_LOGGED_IN", newTitle);
    }

    public static String MSG_LOGGED_OUT() {
        return i.getString("MSG_LOGGED_OUT");
    }

    public static String MSG_NO_UPDATE_AVAILABLE() {
        return i.getString("MSG_NO_UPDATE_AVAILABLE");
    }

    public static String TITLE_UPDATE_CHECK(String title) {
        return i.getString("TITLE_UPDATE_CHECK", title);
    }

    public static String MSG_NO_UPDATE_URL() {
        return i.getString("MSG_NO_UPDATE_URL");
    }
    
    public static String TITLE_KDXPLORE_PREFERENCES(String kdxploreName) {
        return i.getString("TITLE_KDXPLORE_PREFERENCES", kdxploreName);
    }
    
    public static String MENU_SERVICES() {
        return i.getString("MENU_SERVICES");
    }

    public static String MENU_FILE() {
        return i.getString("MENU_FILE");
    }

    public static String MENU_HELP() {
        return i.getString("MENU_HELP");
    }
    
    public static String OPTION_CLOSE_WITHOUT_ASKING_IN_FUTURE() {
        return i.getString("OPTION_CLOSE_WITHOUT_ASKING_IN_FUTURE");
    }
    
    public static String MSG_DO_YOU_REALLY_WANT_TO_CLOSE() {
        return i.getString("MSG_DO_YOU_REALLY_WANT_TO_CLOSE");
    }
    
    public static String MSG_YOU_STILL_HAVE_ACTIVITIES_ARE_YOU_SURE(int count) {
        return i.getString("MSG_YOU_STILL_HAVE_ACTIVITIES_ARE_YOU_SURE", count);
    }

    public static String MSG_NO_DB_BACKUP_APPS_AVAILABLE() {
        return i.getString("MSG_NO_DB_BACKUP_APPS_AVAILABLE");
    }
    
    public static String MSG_SELECT_APP_FOR_BACKUP() {
        return i.getString("MSG_SELECT_APP_FOR_BACKUP");
    }
    
    public static String TITLE_BACKUP_DATABASE() {
        return i.getString("TITLE_BACKUP_DATABASE");
    }

    public static String MENU_TOOLS() {
        return i.getString("MENU_TOOLS");
    }
    
    public static String MENUITEM_BACKUP_DB() {
        return i.getString("MENUITEM_BACKUP_DB");
    }

    public static String VRULE_NO_LIMIT_TEXT() {
        return i.getString("VRULE_NO_LIMIT_TEXT");
    }

    public static String ERRMSG_CHOICE_WITH_NO_CHOICES() {
        return i.getString("ERRMSG_CHOICE_WITH_NO_CHOICES");
    }

    public static String ERRMSG_MUST_HAVE_AT_LEAST_2() {
        return i.getString("ERRMSG_MUST_HAVE_AT_LEAST_2");
    }
    
    public static String ERRMSG_UNSUPPORTED_VALRULE() {
        return i.getString("ERRMSG_UNSUPPORTED_VALRULE");
    }
    
    public static String ERRMSG_UNSUPPORTED_VALRULE_TYPE() {
        return i.getString("ERRMSG_UNSUPPORTED_VALRULE_TYPE");
    }
}
