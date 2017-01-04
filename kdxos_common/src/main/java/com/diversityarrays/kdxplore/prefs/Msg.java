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

import java.util.MissingResourceException;

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
/* package */ class Msg extends AbstractMsg {

    public static final Msg instance = new Msg();

    private Msg() {
    }

    public static String getMessageIdText(MessageId msgId) {
        try {
            return instance.getString(msgId.name.replace(' ', '_'));
        }
        catch (MissingResourceException e) {
            return '!' + msgId.name + '!';
        }
    }

    public static String GROUP_APPLICATIONS() {
        return instance.getString("GROUP_APPLICATIONS");
    }

    public static String GROUP_USER_INTERFACE() {
        return instance.getString("GROUP_USER_INTERFACE");
    }

    public static String GROUP_INPUT_OUTPUT() {
        return instance.getString("GROUP_INPUT_OUTPUT");
    }

    public static String COLHDG_PREFERENCE() {
        return instance.getString("COLHDG_PREFERENCE");
    }

    public static String COLHDG_VALUE() {
        return instance.getString("COLHDG_VALUE");
    }

    public static String NODENAME_OPTIONS_COUNT(int count) {
        return instance.getString("NODENAME_OPTIONS_COUNT", count);
    }

    public static String EXCEL_NAME_MUST_NOT_BE_BLANK() {
        return instance.getString("EXCEL_NAME_MUST_NOT_BE_BLANK");
    }

    public static String EXCEL_NAME_AT_MOST_31_CHARS() {
        return instance.getString("EXCEL_NAME_AT_MOST_31_CHARS");
    }

    public static String EXCEL_NAME_CANNOT_CONTAIN() {
        return instance.getString("EXCEL_NAME_CANNOT_CONTAIN");
    }

    public static String ACTION_APPLY() {
        return instance.getString("ACTION_APPLY");
    }
    public static String ACTION_CHOOSE() {
        return instance.getString("ACTION_CHOOSE");
    }

    public static String LABEL_SELECT_PREF() {
        return instance.getString("LABEL_SELECT_PREF");
    }

    public static String ACTION_RESET() {
        return instance.getString("ACTION_RESET");
    }

    public static String CSV_DELIMITER_NAME(CsvDelimiter delim) {
        return instance.getString("CSV_DELIMITER_NAME_" + delim.name());
    }
}
