/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.vistool;

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
public class Msg extends AbstractMsg {

    static private final Msg i = new Msg();

    static public String ACTION_DEACTIVATE_PLOTS() {
        return i.getString("ACTION_DEACTIVATE_PLOTS");
    }
    public static String ACTION_REFRESH() {
        return i.getString("ACTION_REFRESH");
    }
    public static String ACTION_SNAPSHOT() {
        return i.getString("ACTION_SNAPSHOT");
    }
    public static String ACTION_ALL_PLOTS_FOR_NAME(String name) {
        return i.getString("ACTION_ALL_PLOTS_FOR_NAME", name);
    }
    public static String ACTION_N_PLOTS_FOR_NAME(String name, int nPlots) {
        return i.getString("ACTION_N_PLOTS_FOR_NAME", name, nPlots);
    }
    public static String ACTION_ONE_CHART_WITH_N_TRAITS(int count) {
        return i.getString("ACTION_ONE_CHART_WITH_N_TRAITS", count);
    }
    public static String ACTION_EACH_TRAIT_IN_OWN_CHART() {
        return i.getString("ACTION_EACH_TRAIT_IN_OWN_CHART");
    }
    
    public static String ACTION_ONE_CHART_WITH_N_SELECTED_TRAITS(int count) {
        return i.getString("ACTION_ONE_CHART_WITH_N_SELECTED_TRAITS", count);
    }
    public static String ACTION_EACH_SELECTED_TRAIT_IN_OWN_CHART() {
        return i.getString("ACTION_EACH_SELECTED_TRAIT_IN_OWN_CHART");
    }
    public static String ACTION_NO_DATA_TO_PLOT() {
        return i.getString("ACTION_NO_DATA_TO_PLOT");
    }

    public static String ACTION_AT_LEAST_2_TRAIT_INSTANCES() {
        return i.getString("ACTION_AT_LEAST_2_TRAIT_INSTANCES");
    }
    public static String ACTION_X_AXIS_NAME_Y_AXIS_NAME(String xName, String yName) {
        return i.getString("ACTION_X_AXIS_NAME_Y_AXIS_NAME", xName, yName);
    }
    public static String ACTION_X_AXIS_NAME_Y_N_TRAITS(String xName, int yCount) {
        return i.getString("ACTION_X_AXIS_NAME_Y_N_TRAITS", xName, yCount);
    }
    public static String ACTION_ALL_N_SCATTER_PLOTS(int count) {
        return i.getString("ACTION_ALL_N_SCATTER_PLOTS", count);
    }
    public static String ACTION_ALL_Y_WITH_X_AXIS_NAME(int yCount, String activeName) {
        return i.getString("ACTION_ALL_Y_WITH_X_AXIS_NAME", yCount, activeName);
    }
    public static String ACTION_CREATE_HEATMAP() {
        return i.getString("ACTION_CREATE_HEATMAP");
    }

    
    static public String AXIS_LABEL_TRAIT_INSTANCE() {
        return i.getString("AXIS_LABEL_TRAIT_INSTANCE");
    }
    static public String AXIS_LABEL_SAMPLE_VALUE() {
        return i.getString("AXIS_LABEL_SAMPLE_VALUE");
    }

    static public String CBOX_OUTLIERS() {
        return i.getString("CBOX_OUTLIERS");
    }
    static public String CBOX_MEAN() {
        return i.getString("CBOX_MEAN");
    }
    static public String CBOX_MEDIAN() {
        return i.getString("CBOX_MEDIAN");
    }

    public static String DESC_IMAGE_FILE() {
        return i.getString("DESC_IMAGE_FILE");
    }

    static public String ERRTITLE_UNABLE_TO_SNAPSHOT() {
        return i.getString("ERRTITLE_UNABLE_TO_SNAPSHOT");
    }

    static public String HTML_CURATION_NOT_AVAILABLE_WITH_MULTIPLE_TRAITS() {
        return i.getString("HTML_CURATION_NOT_AVAILABLE_WITH_MULTIPLE_TRAITS");
    }

    static public String LABEL_MARK_INFO_PLOT_TYPE() {
        return i.getString("LABEL_MARK_INFO_PLOT_TYPE");
    }
    static public String LABEL_SHOW_PARAMETERS() {
        return i.getString("LABEL_SHOW_PARAMETERS");
    }


    public static String MSG_NO_INSTANCES_WITH_NUMERIC_DATA() {
        return i.getString("MSG_NO_INSTANCES_WITH_NUMERIC_DATA");
    }

    public static String MSG_NO_INPUT_DATA() {
        return i.getString("MSG_NO_INPUT_DATA");
    }

    public static String MSG_BLANK() {
        return i.getString("MSG_BLANK");
    }
    public static String MSG_ONLY_FOR_N_PLOTS(int nPlots) {
        return i.getString("MSG_ONLY_FOR_N_PLOTS", nPlots);
    }
    static public String MSG_SOME_TRAIT_VALUES_NOT_PLOTTED() {
        return i.getString("MSG_SOME_TRAIT_VALUES_NOT_PLOTTED");
    }
    public static String MSG_VALUE_COLON_VALUE_COUNT(String traitValue, int count) {
        return i.getString("MSG_VALUE_COLON_VALUE_COUNT", traitValue, count);
    }
    
    public static String raw_MSG_VALUE_COLON_VALUE_COUNT() {
        return i.getString("MSG_VALUE_COLON_VALUE_COUNT");
    }

    public static String OPTION_KEEP_ON_TOP() {
        return i.getString("OPTION_KEEP_ON_TOP");
    }
    static public final String OPTION_DO_NOT_SHOW_MARK() {
        return i.getString("OPTION_DO_NOT_SHOW_MARK");
    }

    public static String PLOT_TITLE_TOOLNAME_OF(String toolName) {
        return i.getString("PLOT_TITLE_TOOLNAME_OF", toolName);
    }

    static public final String TAB_CURATION() {
        return i.getString("TAB_CURATION");
    }
    static public final String TAB_MESSAGES() {
        return i.getString("TAB_MESSAGES");
    }
    public static String LABEL_MIN_TO_MAX_SEPARATOR() {
        return i.getString("LABEL_MIN_TO_MAX_SEPARATOR");
    }
    public static String LABEL_X_COMMA_Y() {
        return i.getString("LABEL_X_COMMA_Y");
    }
    public static String QUESTION_DO_YOU_WANT_TO_VIEW_THE_SNAPSHOT() {
        return i.getString("QUESTION_DO_YOU_WANT_TO_VIEW_THE_SNAPSHOT");
    }
    public static String ERRTITLE_UNABLE_TO_OPEN_IMAGE_FILE(String path) {
        return i.getString("ERRTITLE_UNABLE_TO_OPEN_IMAGE_FILE", path);
    }

    public static String TOOLNAME_BOX_PLOT() {
        return i.getString("TOOLNAME_BOX_PLOT");
    }
    public static String TOOLNAME_HEATMAP() {
        return i.getString("TOOLNAME_HEATMAP");
    }
    public static String TOOLNAME_SCATTER_PLOT() {
        return i.getString("TOOLNAME_SCATTER_PLOT");
    }

    public static String TITLE_HEATMAP(String tiname) {
        return i.getString("TITLE_HEATMAP", tiname);
    }
    public static String ERRMSG_X_AND_Y_AXES_MUST_BE_PROVIDED() {
        return i.getString("ERRMSG_X_AND_Y_AXES_MUST_BE_PROVIDED");
    }
    public static String ERRMSG_AT_LEAST_1_VALUE_SELECTION_MUST_BE_MADE() {
        return i.getString("ERRMSG_AT_LEAST_1_VALUE_SELECTION_MUST_BE_MADE");
    }
    public static String TRAIT_NAME_UNKNOWN() {
        return i.getString("TRAIT_NAME_UNKNOWN");
    }
    public static String ERRMSG_NO_VALUE_DATA_FOR_HEATMAP() {
        return i.getString("ERRMSG_NO_VALUE_DATA_FOR_HEATMAP");
    }
    public static String ERRMSG_NO_X_AND_Y_FOR_HEATMAP() {
        return i.getString("ERRMSG_NO_X_AND_Y_FOR_HEATMAP");
    }
    public static String ACTION_NO_X_OR_Y_AVAILABLE() {
        return i.getString("ACTION_NO_X_OR_Y_AVAILABLE");
    }
    public static String ACTION_NO_X_AVAILABLE() {
        return i.getString("ACTION_NO_X_AVAILABLE");
    }
    public static String ACTION_NO_Y_AVAILABLE() {
        return i.getString("ACTION_NO_Y_AVAILABLE");
    }
    public static String ACTION_X_Y_WITH_NAME(String name) {
        return i.getString("ACTION_X_Y_WITH_NAME", name);
    }
    public static String ACTION_ALL_TRAITS() {
        return i.getString("ACTION_ALL_TRAITS");
    }
    public static String MSG_USE_BUTTON_ON_TOOLBAR() {
        return i.getString("MSG_USE_BUTTON_ON_TOOLBAR");
    }
    public static String ACTION_ACTIVATE_PLOTS() {
        return i.getString("ACTION_ACTIVATE_PLOTS");
    }
    public static String ACTION_VISTOOL_DISABLED_FOR_LEVEL(String visTool, String levelName) {
        return i.getString("ACTION_VISTOOL_DISABLED_FOR_LEVEL", visTool, levelName);
    }
    
}
