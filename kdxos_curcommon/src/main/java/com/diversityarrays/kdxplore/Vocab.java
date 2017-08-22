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

import com.diversityarrays.util.AbstractMsg;

@SuppressWarnings("nls")
public class Vocab extends AbstractMsg {

	static private Vocab i = new Vocab();
	
	private Vocab() {
	}
	
    static public String ACTION_DEACTIVATE_PLOTS() {
        return i.getString("ACTION_DEACTIVATE_PLOTS");
    }
    static public String ACTION_ACTIVATE_PLOTS() {
        return i.getString("ACTION_ACTIVATE_PLOTS");
    }
    static public String MSG_DEACTIVATED_PLOT_COUNT(int count) {
        return i.getString("MSG_DEACTIVATED_PLOT_COUNT", count);
    }
    static public String MSG_ACTIVATED_PLOT_COUNT(int count) {
        return i.getString("MSG_ACTIVATED_PLOT_COUNT", count);
    }
    
    static public String MENU_TITLE_FIELD_VIEW() {
        return i.getString("MENU_TITLE_FIELD_VIEW");
    }
    static public String MSG_COUNT_PLOTS(int count) {
        return i.getString("MSG_COUNT_PLOTS", count);
    }
    static public String TITLE_PLOT_LIST() {
        return i.getString("TITLE_PLOT_LIST");
    }
	
	static public String CBOX_CURATED() {
	    return i.getString("CBOX_CURATED");
	}
	static public String CBOX_UNCURATED() {
        return i.getString("CBOX_UNCURATED");
    }
    static public String CBOX_SUPPRESSED_SAMPLES() {
        return i.getString("CBOX_SUPPRESSED_SAMPLES");
    }
    static public String CBOX_ACCEPTED_SAMPLES() {
        return i.getString("CBOX_ACCEPTED_SAMPLES");
    }
	
	static public String COLHDG_CURATED() {
        return i.getString("COLHDG_CURATED");
	}

	static public String COLHDG_UNCURATED() {
	    return i.getString("COLHDG_UNCURATED");
	}

	static public String OPTION_SHOW_POSITION() {
	    return i.getString("OPTION_SHOW_POSITION");
	}
	
	static public String TITLE_HELP_MULTIPLE_VALUES() {
	    return i.getString("TITLE_HELP_MULTIPLE_VALUES");
	}
	static public String HTML_HELP_SET_MULTIPLE_VALUES() {
	    return i.getString("HTML_HELP_SET_MULTIPLE_VALUES");
	}
    static public String HTML_APPLY_TO_CURATED_SAMPLES() {
        return i.getString("HTML_APPLY_TO_CURATED_SAMPLES");
    }
    static public String HTML_DO_NOT_APPLY_TO_CURATED_SAMPLES() {
        return i.getString("HTML_DO_NOT_APPLY_TO_CURATED_SAMPLES");
    }

    static public String HTML_APPLY_TO_UNCURATED_SAMPLES() {
        return i.getString("HTML_APPLY_TO_UNCURATED_SAMPLES");
    }
    static public String HTML_DO_NOT_APPLY_TO_UNCURATED_SAMPLES() {
        return i.getString("HTML_DO_NOT_APPLY_TO_UNCURATED_SAMPLES");
    }
	
    static public String WORD_ACCEPTED_SAMPLES() {
        return i.getString("WORD_ACCEPTED_SAMPLES");
    }
    
    
    static public String ACTION_ACCEPT_TRAIT_VALUES() {
        return i.getString("ACTION_ACCEPT_TRAIT_VALUES");
    }
    static public String ACTION_SUPPRESS_TRAIT_VALUES() {
        return i.getString("ACTION_SUPPRESS_TRAIT_VALUES");
    }
    
    static public String ACTION_SET_MULTIPLE_VALUES() {
        return i.getString("ACTION_SET_MULTIPLE_VALUES");
    }
    static public String ACTION_SET_VALUE() {
        return i.getString("ACTION_SET_VALUE");
    }
    static public String ACTION_SET_MISSING() {
        return i.getString("ACTION_SET_MISSING");
    }
    static public String ACTION_SET_NA() {
        return i.getString("ACTION_SET_NA");
    }
    static public String ACTION_SET_UNSET() {
        return i.getString("ACTION_SET_UNSET");
    }
    static public String ACTION_HELP() {
        return i.getString("ACTION_HELP");
    }
    static public String ACTION_STATS() {
        return i.getString("ACTION_STATS");
    }
    
    static public String OPTION_NO_SAMPLE_TYPE() {
        return i.getString("OPTION_NO_SAMPLE_TYPE");
    }

	static public String HTML_NO_SAMPLES_MET_THE_APPLY_TO_CRITERIA() {
	    return i.getString("HTML_NO_SAMPLES_MET_THE_APPLY_TO_CRITERIA");
	}
	
	static public String HTML_CHANGE_SUPPRESSED_SAMPLES() {
	    return i.getString("HTML_CHANGE_SUPPRESSED_SAMPLES");
	}
	
    static public String HTML_DO_NOT_CHANGE_SUPPRESSED_SAMPLES() {
        return i.getString("HTML_DO_NOT_CHANGE_SUPPRESSED_SAMPLES");
    }
    
    static public String HTML_CHANGE_SAMPLES() {
        return i.getString("HTML_CHANGE_SAMPLES");
    }
    static public String HTML_DO_NOT_CHANGE_SAMPLES() {
        return i.getString("HTML_DO_NOT_CHANGE_SAMPLES");
    }
	
	static public String HTML_IF_CURATED_APPLY_TO_ACCEPTED_SAMPLES() {
	    return i.getString("HTML_IF_CURATED_APPLY_TO_ACCEPTED_SAMPLES");
	}
	static public String HTML_IF_CURATED_DO_NOT_APPLY_TO_ACCEPTED_SAMPLES() {
	    return i.getString("HTML_IF_CURATED_DO_NOT_APPLY_TO_ACCEPTED_SAMPLES");
	}

	static public String HTML_IF_CURATED_APPLY_TO_SUPPRESSED_SAMPLES() {
        return i.getString("HTML_IF_CURATED_APPLY_TO_SUPPRESSED_SAMPLES");
    }
    static public String HTML_IF_CURATED_DO_NOT_APPLY_TO_SUPPRESSED_SAMPLES() {
        return i.getString("HTML_IF_CURATED_DO_NOT_APPLY_TO_SUPPRESSED_SAMPLES");
    }

	public String getValuesAlreadySet() {
		return "Values already Set";
	}

	//getNoValidSamplesSelectedToExclude
	static public String MSG_NO_VALID_SAMPLES_SELECTED_TO_SUPPRESS() {
	    return i.getString("MSG_NO_VALID_SAMPLES_SELECTED_TO_SUPPRESS");
	}
	
	//getNoValidSamplesSelectedToSet
	static public String MSG_NO_VALID_SAMPLES_SELECTED_TO_SET() {
	    return i.getString("MSG_NO_VALID_SAMPLES_SELECTED_TO_SET");
	}

	// getPleaseSelectReasonForExclude()
	static public String LABEL_SELECT_REASON_FOR_SUPPRESS() {
	    return i.getString("LABEL_SELECT_REASON_FOR_SUPPRESS");
	}

    static public String TITLE_ACCEPT_SELECT_SOURCE_FOR_VALUES() {
        return i.getString("TITLE_ACCEPT_SELECT_SOURCE_FOR_VALUES");
    }
    
    static public String TITLE_SUPPRESS_SELECT_SOURCE_FOR_VALUES() {
        return i.getString("TITLE_SUPPRESS_SELECT_SOURCE_FOR_VALUES");
    }
    
	static public String TITLE_SET_UNCURATED_SAMPLES_FROM_SOURCE(String source) {
	    return i.getString("TITLE_SET_UNCURATED_SAMPLES_FROM_SOURCE", source);
	}
	
	static public String TITLE_SUPPRESS_REASON() {
	    return i.getString("TITLE_SUPPRESS_REASON");
	}
	
	static public String LABEL_SET_SAMPLES_FROM() {
	    return i.getString("LABEL_SET_SAMPLES_FROM");
	}

	// getAllSelectedSamplesAlreadyIncluded
	static public String MSG_ALL_VALUES_ALREADY_ACCEPTED() {
	    return i.getString("MSG_ALL_VALUES_ALREADY_ACCEPTED");
	}
	// getAllSelectedSamplesAlreadyExcluded
	static public String MSG_ALL_VALUES_ALREADY_SUPPRESSED() {
	    return i.getString("MSG_ALL_VALUES_ALREADY_SUPPRESSED");
	}

	static public String ACTION_SET_TO_NA() {
	    return i.getString("ACTION_SET_TO_NA");
	}
	static public String ACTION_SET_TO_MISSING() {
        return i.getString("ACTION_SET_TO_MISSING");
    }
//    static public String ACTION_SET_TO_UNSCORED() {
//        return i.getString("ACTION_SET_TO_UNSCORED");
//    }
	
	static public String ACTION_REMOVE_CURATED() {
	    return i.getString("ACTION_REMOVE_CURATED");
	}

	// getChangeValuesAlreadySet
	static public final String HTML_CHANGE_ACCEPTED_SAMPLES() {
		return i.getString("HTML_CHANGE_ACCEPTED_SAMPLES");
	}

    static public final String HTML_DO_NOT_CHANGE_ACCEPTED_SAMPLES() {
        return i.getString("HTML_DO_NOT_CHANGE_ACCEPTED_SAMPLES");
    }

    static public String LABEL_FROM_SOURCE(String source) {
        return i.getString("LABEL_FROM_SOURCE", source);
    }

	// getExcludeSamplesUsingValuesFrom
	static public String LABEL_SUPPRESS_WITH_VALUES_FROM() {
	    return i.getString("LABEL_SUPPRESS_WITH_VALUES_FROM");
	}
	
	static public String LIST_TITLE_SELECT_SAMPLE_SOURCE() {
	    return i.getString("LIST_TITLE_SELECT_SAMPLE_SOURCE");
	}
	
	static public String PROMPT_ENTER_VALUE_OR_SELECT() {
	    return i.getString("PROMPT_ENTER_VALUE_OR_SELECT");
	}
	
	static public String LABEL_SOURCE_FROM_DEVICE() {
        return i.getString("LABEL_SOURCE_FROM_DEVICE");
	}
	static public String LABEL_SOURCE_CURATED_VALUE() {
        return i.getString("LABEL_SOURCE_CURATED_VALUE");
	}
	static public String LABEL_SOURCE_FROM_DATABASE() {
        return i.getString("LABEL_SOURCE_FROM_DATABASE");
	}
	static public String LABEL_SAMPLE_TYPE() {
	    return i.getString("LABEL_SAMPLE_TYPE");
	}
    static public String LABEL_APPLY_TO() {
        return i.getString("LABEL_APPLY_TO");
    }
    static public String LABEL_APPLY_TO_ALL() {
        return i.getString("LABEL_APPLY_TO_ALL");
    }
    static public String LABEL_APPLY_TO_BOTH() {
        return i.getString("LABEL_APPLY_TO_BOTH");
    }
	
	
	static public String PROMPT_VALUES_FROM() {
	    return i.getString("PROMPT_VALUES_FROM");
	}
	
	static public String TITLE_SAMPLE_VALUES() {
	    return i.getString("TITLE_SAMPLE_VALUES");
	}
	static public String TITLE_CHANGE_MULTIPLE_SAMPLES() {
	    return i.getString("TITLE_CHANGE_MULTIPLE_SAMPLES");
	}
	
	static public String TOOLTIP_SET_ALL_VALUES_FOR_SELECTED() {
	    return i.getString("TOOLTIP_SET_ALL_VALUES_FOR_SELECTED");
	}
	
	static public String TOOLTIP_APPLY_TO_CURATED_SAMPLES() {
	    return i.getString("TOOLTIP_APPLY_TO_CURATED_SAMPLES");
	}
	static public String TOOLTIP_APPLY_TO_UNCURATED_SAMPLES() {
        return i.getString("TOOLTIP_APPLY_TO_UNCURATED_SAMPLES");
    }
    static public String TOOLTIP_CHANGE_SUPPRESSED_SAMPLES() {
        return i.getString("TOOLTIP_CHANGE_SUPPRESSED_SAMPLES");
    }
    static public String TOOLTIP_CHANGE_ACCEPTED_SAMPLES() {
        return i.getString("TOOLTIP_CHANGE_ACCEPTED_SAMPLES");
    }
    static public String TOOLTIP_STATS_FOR_KDSMART_SAMPLES() {
        return i.getString("TOOLTIP_STATS_FOR_KDSMART_SAMPLES");
    }
    
    static public String TOOLTIP_SET_UNSET() {
        return i.getString("TOOLTIP_SET_UNSET");
    }
    static public String TOOLTIP_SET_NA() {
        return i.getString("TOOLTIP_SET_NA");
    }
    static public String TOOLTIP_SET_MISSING() {
        return i.getString("TOOLTIP_SET_MISSING");
    }
    static public String TOOLTIP_SET_VALUE() {
        return i.getString("TOOLTIP_SET_VALUE");
    }
    
    static public String MSG_DATE_NOT_SCORED() {
        return i.getString("MSG_DATE_NOT_SCORED");
    }
    static public String MSG_DATE_UNKNOWN() {
        return i.getString("MSG_DATE_UNKNOWN");
    }

    public static String getStatTypeLabel(String name) {
        return i.getString("STAT_TYPE_" + name);
    }

    public static String ERRMSG_INVALID_VALUE_VALIDATION(String newValue, String expression) {
        return i.getString("ERRMSG_INVALID_VALUE_VALIDATION", newValue, expression);
    }
}
