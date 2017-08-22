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
package com.diversityarrays.kdxplore.curate;

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
public class Msg extends AbstractMsg {

    static private final Msg i = new Msg();
    
    static public String OPTION_KEEP_ON_TOP() {
        return i.getString("OPTION_KEEP_ON_TOP");
    }

    public static String HTML_FONT_LEGEND() {
        return i.getString("HTML_FONT_LEGEND");
    }

    public static Object HTML_EDITING_HELP(String panelTitle) {
        return i.getString("HTML_EDITING_HELP", panelTitle);
    }

    public static String HTML_SELECT_VALUES_INSTRUCTION(String samplesTableName) {
        return i.getString("HTML_SELECT_VALUES_INSTRUCTION", samplesTableName);
    }

    public static String ACTION_OVERVIEW() {
        return i.getString("ACTION_OVERVIEW");
    }

    public static String MENU_VIEW() {
        return i.getString("MENU_VIEW");
    }
    
    public static String MENU_FILE() {
        return i.getString("MENU_FILE");
    }
    
    public static String MENU_EDIT() {
        return i.getString("MENU_EDIT");
    }

    public static String ACTION_FIELD_VIEW() {
        return i.getString("ACTION_FIELD_VIEW");
    }

    public static String LABEL_SET_UNSCORED_TO() {
        return i.getString("LABEL_SET_UNSCORED_TO");
    }
    
    public static String PHRASE_SHOWING() {
        return i.getString("PHRASE_SHOWING");
    }
    public static String PHRASE_SHOWING_ALL() {
        return i.getString("PHRASE_SHOWING_ALL");
    }
    
    public static String TITLE_FIELD_VIEW() {
        return i.getString("TITLE_FIELD_VIEW");
    }
    
    public static String TOOLTIP_EXPORT() {
        return i.getString("TOOLTIP_EXPORT");
    }
    public static String TOOLTIP_FIELD_VIEW() {
        return i.getString("TOOLTIP_FIELD_VIEW");
    }
    public static String TOOLTIP_HELP_DATA_CURATION() {
        return i.getString("TOOLTIP_HELP_DATA_CURATION");
    }
    public static String TOOLTIP_IMPORT_DATA() {
        return i.getString("TOOLTIP_IMPORT_DATA");
    }
    public static String TOOLTIP_REDO() {
        return i.getString("");
    }
    public static String TOOLTIP_SAMPLES_TABLE() {
        return i.getString("TOOLTIP_SAMPLES_TABLE");
    }
    public static String TOOLTIP_SAVE_CHANGES() {
        return i.getString("TOOLTIP_REDO");
    }
    public static String TOOLTIP_UNDO() {
        return i.getString("TOOLTIP_UNDO");
    }
}
