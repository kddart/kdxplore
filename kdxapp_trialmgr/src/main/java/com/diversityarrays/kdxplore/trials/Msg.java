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
package com.diversityarrays.kdxplore.trials;

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
public class Msg extends AbstractMsg {
    static private final Msg i = new Msg();
    
    public static final String ACTION_USE_ALL() {
        return i.getString("ACTION_USE_ALL");
    }
    
    public static final String ACTION_USE_NONE() {
        return i.getString("ACTION_USE_NONE");
    }
    
    public static final String ACTION_CANCEL() {
        return i.getString("ACTION_CANCEL");
    }

    public static final String ACTION_CREATE() {
        return i.getString("ACTION_CREATE");
    }
    
    public static final String COLHDG_ADD() {
        return i.getString("COLHDG_ADD");
    }
    public static final String COLHDG_USE() {
        return i.getString("COLHDG_USE");
    }
    public static final String COLHDG_TRAIT() {
        return i.getString("COLHDG_TRAIT");
    }
    public static String COLHDG_TRAIT_UNIT() {
        return i.getString("COLHDG_TRAIT_UNIT");
    }
    public static final String COLHDG_LEVEL() {
        return i.getString("COLHDG_LEVEL");
    }
    public static final String COLHDG_DESCRIPTION() {
        return i.getString("COLHDG_DESCRIPTION");
    }
    public static final String COLHDG_N_INSTANCES_TO_CREATE() {
        return i.getString("COLHDG_N_INSTANCES_TO_CREATE");
    }
    public static final String COLHDG_INSTANCE_NUMBERS() {
        return i.getString("COLHDG_INSTANCE_NUMBERS");
    }
    
    public static String HTML_DOUBLE_CLICK_TO_CHANGE_INSTANCE_COUNT() {
        return i.getString("HTML_DOUBLE_CLICK_TO_CHANGE_INSTANCE_COUNT");
    }

    public static final String ERRMSG_MISSING_DEVICE_ID(String deviceId) {
        return i.getString("ERRMSG_MISSING_DEVICE_ID", deviceId);
    }
    
    public static final String ERRMSG_INTERNAL_ERROR_MISSING_TRAIT(int traitId) {
        return i.getString("ERRMSG_INTERNAL_ERROR_MISSING_TRAIT", traitId);
    }
    
    public static final String ERRTITLE_ERROR(String title) {
        return i.getString("ERRTITLE_ERROR", title);
    }
    public static final String ERRTITLE_INTERNAL_ERROR(String title) {
        return i.getString("ERRTITLE_INTERNAL_ERROR", title);
    }
    
    public static final String PROMPT_DESC_FOR_SCORING_SET() {
        return i.getString("PROMPT_DESC_FOR_SCORING_SET");
    }
    public static final String PROMPT_ENTER_FILTER_STRING() {
        return i.getString("PROMPT_ENTER_FILTER_STRING");
    }
    
    public static final String MSG_THERE_ARE_NO_CURATED_SAMPLES() {
        return i.getString("MSG_THERE_ARE_NO_CURATED_SAMPLES");
    }
    public static final String LABEL_FILTER() {
        return i.getString("LABEL_FILTER");
    }

    public static final String OPTION_NO_SAMPLE_VALUES() {
        return i.getString("OPTION_NO_SAMPLE_VALUES");
    }
    public static final String OPTION_CURATED_SAMPLE_VALUES() {
        return i.getString("OPTION_CURATED_SAMPLE_VALUES");
    }
    
    public static final String TITLE_ADD_SCORING_SET() {
        return i.getString("TITLE_ADD_SCORING_SET");
    }
    public static final String TITLE_ADD_TRAITS_TO_TRIAL(String trialName) {
        return i.getString("TITLE_ADD_TRAITS_TO_TRIAL", trialName);
    }
    
    public static String ENUM_NO_TRIAL_UNIT_LOAD_FROM_DB() {
        return i.getString("ENUM_NO_TRIAL_UNIT_LOAD_FROM_DB");
    }
    public static String ENUM_NO_TRIAL_UNIT_EDIT_WITHOUT() {
        return i.getString("ENUM_NO_TRIAL_UNIT_EDIT_WITHOUT");
    }

    public static String MSG_RETRIEVED_N_PLOTS(int plotCount) {
        return i.getString("MSG_RETRIEVED_N_PLOTS", plotCount);
    }
    public static String MSG_NO_PLOTS_IN_DATABASE() {
        return i.getString("MSG_NO_PLOTS_IN_DATABASE");
    }
    public static String MSG_NO_PLOTS_AVAILABLE_HOW_PROCEED() {
        return i.getString("MSG_NO_PLOTS_AVAILABLE_HOW_PROCEED");
    }

    public static String HTML_NO_TRIALS_LOADED() {
        return i.getString("HTML_NO_TRIALS_LOADED");
    }

    public static String HTML_NO_TRIAL_SELECTED() {
        return i.getString("HTML_NO_TRIAL_SELECTED");
    }

    public static String TITLE_ADD_TRIALS() {
        return i.getString("TITLE_ADD_TRIALS");
    }

    public static String TITLE_REFRESH_TRIAL_DATA() {
        return i.getString("TITLE_REFRESH_TRIAL_DATA");
    }

    public static String TITLE_HARVEST_PROCESSING() {
        return i.getString("TITLE_HARVEST_PROCESSING");
    }

    public static String TITLE_PREPARE_FOR_PLANTING() {
        return i.getString("TITLE_PREPARE_FOR_PLANTING");
    }

    public static String TITLE_EDIT_VIEW_TRIAL() {
        return i.getString("TITLE_EDIT_VIEW_TRIAL");
    }

    public static String TITLE_STORE_TRIAL() {
        return i.getString("TITLE_STORE_TRIAL");
    }

    public static String TITLE_REMOVE_TRIAL() {
        return i.getString("TITLE_REMOVE_TRIAL");
    }

    public static String TRIAL_SEARCH_FILTER(TrialSearchFilter f) {
        return i.getString("TRIAL_SEARCH_FILTER_" + f.name());
    }

    public static String TOOLTIP_ADD_SAMPLES_FOR_SCORING() {
        return i.getString("TOOLTIP_ADD_SAMPLES_FOR_SCORING");
    }

    public static String TOOLTIP_DELETE_COLLECTED_SAMPLES() {
        return i.getString("TOOLTIP_DELETE_COLLECTED_SAMPLES");
    }

    public static String TOOLTIP_EXPORT_SAMPLES_OR_TRAITS() {
        return i.getString("TOOLTIP_EXPORT_SAMPLES_OR_TRAITS");
    }

    public static String TOOLTIP_REMOVE_TRAIT_INSTANCES_WITH_NO_DATA() {
        return i.getString("TOOLTIP_REMOVE_TRAIT_INSTANCES_WITH_NO_DATA");
    }

    public static String MSG_NO_TRAITS_ALLOCATED() {
        return i.getString("MSG_NO_TRAITS_ALLOCATED");
    }

    public static String TITLE_PRINT_TRAIT_BARCODES(String suffix) {
        return i.getString("TITLE_PRINT_TRAIT_BARCODES", suffix);
    }

    public static String TITLE_PRINT_PLOT_BARCODES(String suffix) {
        return i.getString("TITLE_PRINT_PLOT_BARCODES");
    }

    public static String TITLE_PRINT_SUBPLOT_BARCODES(String suffix) {
        return i.getString("TITLE_PRINT_SUBPLOT_BARCODES");
    }

    public static String LABEL_NO_TRIAL_SELECTED() {
        return i.getString("LABEL_NO_TRIAL_SELECTED");
    }
}
