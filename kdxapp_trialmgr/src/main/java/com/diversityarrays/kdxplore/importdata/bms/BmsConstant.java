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
package com.diversityarrays.kdxplore.importdata.bms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trial;

@SuppressWarnings("nls")
public class BmsConstant {
	/**
	 * If this is found in the CONDITION section of the DESCRIPTION Worksheet
	 * then we want to use its "Value" as the "instance number" of the Trial.
	 * CIMMYT uses "Occurrences" of a Trial - this is the "occurrence number".
	 */
	public static final String CIMMYT_TRIAL_INSTANCE = "TRIAL_INSTANCE";

	// This gets interpreted as the TrialAcronym
	public static final String CIMMYT_STUDY_ABBR = "STUDY_ABBR";

	public static final String CIMMYT_TITLE = "TITLE";
	public static final String CIMMYT_STUDY = "STUDY";
	
	public static final String CIMMYT_START_DATE = "START DATE";
	/**
	 * These are the ones at the beginning of the Description worksheet.
	 */
	public static final String[] BROWN_HEADINGS = {
		"STUDY",
		"TITLE",
		"OBJECTIVE",
		CIMMYT_START_DATE,
		"END DATE",
		"STUDY TYPE"
	};
	
	static public final String SECTION_NAME_CONDITION = "CONDITION";
	
	static public final String SECTION_NAME_FACTOR = "FACTOR";
	// Nothing imported from this one
	static public final String SECTION_NAME_CONSTANT = "CONSTANT";
	// These are all the Traits
	static public final String SECTION_NAME_VARIATE = "VARIATE";

	static public final String DESC_HEADING_PLANTING_DATE = "PlantingDate";

	/**
	 * These are presented to the user as options for separating the 
	 * CIMMYT Trial name and the CIMMYT TrialInstance so as to construct a
	 * unique TrialName for use in KDXplore.
	 */
	public static final String[] TI_SEPARATOR_OPTIONS = { "_", "#", ";" };
	
	static public final Map<String,String> PLOT_FIELD_NAME_BY_FACTOR;

	static public final Map<String,String> TRIAL_FIELD_NAME_BY_CONDITION;
	
	static public final String XLSHDG_ENTRY_TYPE = "ENTRY_TYPE";
	static public final String XLSHDG_PLOT_NO = "PLOT_NO";
	static public final String XLSHDG_COLUMN_NO = "COLUMN_NO";
	static public final String XLSHDG_RANGE_NO = "RANGE_NO";
	static public final String XLSHDG_FIELDMAP_COLUMN = "FIELDMAP COLUMN";
	static public final String XLSHDG_FIELDMAP_RANGE = "FIELDMAP RANGE";
	
	static {
		Map<String,String> map = new HashMap<>();
		map.put(XLSHDG_ENTRY_TYPE, Plot.FIELDNAME_PLOT_TYPE);
		map.put(XLSHDG_PLOT_NO,    Plot.FIELDNAME_USER_PLOT_ID);
		
		map.put(XLSHDG_COLUMN_NO,       Plot.FIELDNAME_PLOT_COLUMN);
		map.put(XLSHDG_RANGE_NO,        Plot.FIELDNAME_PLOT_ROW);
		map.put(XLSHDG_FIELDMAP_COLUMN, Plot.FIELDNAME_PLOT_COLUMN);
		map.put(XLSHDG_FIELDMAP_RANGE,  Plot.FIELDNAME_PLOT_ROW);
		
		PLOT_FIELD_NAME_BY_FACTOR = Collections.unmodifiableMap(map);
		
		// - - - -
		map = new HashMap<>();
		map.put(DESC_HEADING_PLANTING_DATE, Trial.FIELD_NAME_PLANTING_DATE);
		
		TRIAL_FIELD_NAME_BY_CONDITION = Collections.unmodifiableMap(map);
	}

	
	
	static public final String DESCRIPTION_HEADING = "DESCRIPTION";
	static public final String PROPERTY_HEADING = "PROPERTY";
	static public final String SCALE_HEADING = "SCALE";
	static public final String METHOD_HEADING = "METHOD";
	static public final String DATA_TYPE_HEADING = "DATA TYPE";
	static public final String VALUE_HEADING = "VALUE";
	
	static public final String SAMPLE_LEVEL_HEADING = "SAMPLE LEVEL";
	static public final String LABEL_HEADING = "LABEL";
	
	static public final String[] FIRST_SIX_OTHER_HEADINGS = {
		DESCRIPTION_HEADING,
		PROPERTY_HEADING,
		SCALE_HEADING,
		METHOD_HEADING,
		DATA_TYPE_HEADING,
		VALUE_HEADING
	};
	
	static public final String[] VARIATE_AND_CONSTANT_HEADINGS = {
		DESCRIPTION_HEADING,
		PROPERTY_HEADING,
		SCALE_HEADING,
		METHOD_HEADING,
		DATA_TYPE_HEADING,
		VALUE_HEADING,
		SAMPLE_LEVEL_HEADING
	};

	static public final String[] FACTOR_AND_CONDITION_HEADINGS =  {
		DESCRIPTION_HEADING,
		PROPERTY_HEADING,
		SCALE_HEADING,
		METHOD_HEADING,
		DATA_TYPE_HEADING,
		VALUE_HEADING,
		LABEL_HEADING
	};
	
	static private String[] makeArray(String first, String[] rest) {
		List<String> tmp = new ArrayList<>();
		tmp.add(first);
		Collections.addAll(tmp, rest);
		return tmp.toArray(new String[tmp.size()]);
	}

	static public final String[] EXPORT_CONDITION_HEADINGS = makeArray(
			BmsConstant.SECTION_NAME_CONDITION, 
			FACTOR_AND_CONDITION_HEADINGS);
	
	static public final String[] EXPORT_FACTOR_HEADINGS = makeArray(
			BmsConstant.SECTION_NAME_FACTOR, 
			FACTOR_AND_CONDITION_HEADINGS);
	
	static public final String[] EXPORT_CONSTANT_HEADINGS = makeArray(
			BmsConstant.SECTION_NAME_CONSTANT, 
			VARIATE_AND_CONSTANT_HEADINGS);
	
	static public final String[] EXPORT_VARIATE_HEADINGS = makeArray(
			BmsConstant.SECTION_NAME_VARIATE, 
			VARIATE_AND_CONSTANT_HEADINGS);
	
	// No instances
	private BmsConstant() {}
}
