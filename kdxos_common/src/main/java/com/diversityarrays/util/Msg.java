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
package com.diversityarrays.util;

@SuppressWarnings("nls")
public class Msg extends AbstractMsg {
    
    static public final Msg instance = new Msg();

    private Msg() {}

    public static String ERROR_DETAILS() {
        return instance.getString("ERROR_DETAILS");
    }

    public static String PROMPT_ENTER_OTHER_HOSTNAME() {
        return instance.getString("PROMPT_ENTER_OTHER_HOSTNAME");

    }

    public static String LABEL_PORT() {
        return instance.getString("LABEL_PORT");
    }

    public static String ACTION_OK() {
        return instance.getString("ACTION_OK");

    }

    public static String ACTION_CANCEL() {
        return instance.getString("ACTION_CANCEL");

    }

    public static String MSG_ADDRESS_SET_TO(String address) {
        return instance.getString("MSG_ADDRESS_SET_TO", address);
    }

    public static String OPTION_OTHER() {
        return instance.getString("OPTION_OTHER");

    }

    public static String HDG_IP_ADDRESSES_AVAILABLE() {
        return instance.getString("HDG_IP_ADDRESSES_AVAILABLE");
    }

    public static String MSG_PLEASE_SUPPLY_A_HOST_NAME() {
        return instance.getString("MSG_PLEASE_SUPPLY_A_HOST_NAME");

    }

    public static String MSG_NO_NETWORK_INTERFACES() {
        return instance.getString("MSG_NO_NETWORK_INTERFACES");

    }

    public static String ACTION_SAVE() {
        return instance.getString("ACTION_SAVE");
    }

    public static String PREFIX_MISSING_FIELDS() {
        return instance.getString("PREFIX_MISSING_FIELDS");

    }

    public static String ERRMSG_LINE_1_OF(String inputPath) {
        return instance.getString("ERRMSG_LINE_1_OF", inputPath);

    }

    public static String ACTION_INSTALL_UPDATE() {
        return instance.getString("ACTION_INSTALL_UPDATE");

    }

    public static String AUTO_UPDATE_IS_NOT_YET_SUPPORTED() {
        return instance.getString("AUTO_UPDATE_IS_NOT_YET_SUPPORTED");

    }

    public static String ACTION_SHOW_DETAILS() {
        return instance.getString("ACTION_SHOW_DETAILS");

    }

    public static String ERRMSG_MALFORMED_URL_IN_UPDATE_FILE() {
        return instance.getString("ERRMSG_MALFORMED_URL_IN_UPDATE_FILE");

    }

    public static String TITLE_UPDATE_CHECK() {
        return instance.getString("TITLE_UPDATE_CHECK");

    }


    public static String PROGRESS_CHECKING() {
        return instance.getString("PROGRESS_CHECKING");

    }

    public static String ERRMSG_UNABLE_TO_CONTACT_UPDATE_SITE(String url) {
        return instance.getString("ERRMSG_UNABLE_TO_CONTACT_UPDATE_SITE", url);

    }

    public static String ERRMSG_PROBLEMS_CONTACTING_UPDATE_SERVER_1() {
        return instance.getString("ERRMSG_PROBLEMS_CONTACTING_UPDATE_SERVER_1");

    }

    public static String HTML_PROBLEMS_CONTACTING_UPDATE_2(
            String updateUrl,
            String errorClassName,
            String errorMessage)
    {
        return instance.getString("HTML_PROBLEMS_CONTACTING_UPDATE_2",
                updateUrl, errorClassName, errorMessage);
    }

    public static String ACTION_CLOSE() {
        return instance.getString("ACTION_CLOSE");
    }

    public static String ERRMSG_UNABLE_TO_READ_UPDATE_INFO() {
        return instance.getString("ERRMSG_UNABLE_TO_READ_UPDATE_INFO");
    }

    public static String YOUR_VERSION_IS_THE_LATEST() {
        return instance.getString("YOUR_VERSION_IS_THE_LATEST");
    }

    public static String ERRTITLE_BROWSER_OPEN_ERROR(String title) {
        return instance.getString("ERRTITLE_BROWSER_OPEN_ERROR", title);
    }

    public static String MSG_RESIZE_MODE_N(int resizeMode) {
        return instance.getString("MSG_RESIZE_MODE_N", resizeMode);
    }

    public static String AUTO_RESIZE_ALL_COLUMNS() {
        return instance.getString("AUTO_RESIZE_ALL_COLUMNS");
    }

    public static String AUTO_RESIZE_LAST_COLUMN() {
        return instance.getString("AUTO_RESIZE_LAST_COLUMN");
    }

    public static String AUTO_RESIZE_NEXT_COLUMN() {
        return instance.getString("AUTO_RESIZE_NEXT_COLUMN");
    }

    public static String AUTO_RESIZE_OFF() {
        return instance.getString("AUTO_RESIZE_OFF");
    }

    public static String AUTO_RESIZE_SUBSEQUENT_COLUMNS() {
        return instance.getString("AUTO_RESIZE_SUBSEQUENT_COLUMNS");
    }

    public static String HTML_NO_NEW_UPDATES_CURRENTLY_AVAILABLE() {
        return instance.getString("HTML_NO_NEW_UPDATES_CURRENTLY_AVAILABLE");
    }

    public static String HTML_A_NEW_UPDATE_FOR_XX_IS_AVAILABLE(String name) {
        return instance.getString("HTML_A_NEW_UPDATE_FOR_XX_IS_AVAILABLE", name);
    }

    public static String HTML_THIS_VERSION_EXPIRES_IN_N_DAYS(int daysToGo) {
        return instance.getString("HTML_THIS_VERSION_EXPIRES_IN_N_DAYS", daysToGo);
    }
    

//    public static String MSG_VERSION_EXPIRES_IN_N_DAYS(int daysToGo) {
//        return instance.getString("MSG_VERSION_EXPIRES_IN_N_DAYS", daysToGo);
//    }

    public static String TEXT_CURRENT_VERSION(String versionName) {
        return instance.getString("TEXT_CURRENT_VERSION", versionName);
    }

    public static String TEXT_UPDATE_VERSION(String versionName) {
        return instance.getString("TEXT_UPDATE_VERSION", versionName);
    }

    public static String TEXT_UPDATE_SIZE(String updateSize) {
        return instance.getString("TEXT_UPDATE_SIZE", updateSize);
    }

    public static String HTML_PREFIX_UPDATE_DETAILS() {
        return instance.getString("HTML_PREFIX_UPDATE_DETAILS");
    }
}
