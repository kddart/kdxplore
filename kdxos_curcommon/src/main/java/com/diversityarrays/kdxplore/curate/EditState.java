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

import java.util.HashMap;
import java.util.Map;


public enum EditState {
	NO_VALUES      (EditStateFont.NORMAL,      "No Measurements available"),
	RAW_SAMPLE     (EditStateFont.NORMAL,      "Uncurated Measurement"),  // At least 1 KDSmart SampleMeasurement for the trialUnidId/traitId/instanceNumber
	MORE_RECENT    (EditStateFont.BOLD_ITALIC, "There is a more recent uncurated Measurement"),   // RAW_SAMPLE exists that was scored after the CURATED on (which also exists)
	CURATED        (EditStateFont.BOLD,        "Curated value has been set"), // User has selected a Curated value
	FROM_DATABASE  (EditStateFont.ITALIC,      "Database value only"),   // The chosen value is from the databaseSamples
	CALCULATED     (EditStateFont.BOLD,      "Calculated value"),
	;
	
	public final EditStateFont font;
	public final String tooltip;
	EditState(EditStateFont f, String t) {
		this.font = f;
		this.tooltip = t;
	}
	
	static private final String[] NO_WRAPPERS = { "", "" };
	static private final String[] ONLY_SUPPRESSED = { "<u>", "</u>" };
	
	static private final Map<EditStateFont,String[]> SUPPRESSED_CACHE = new HashMap<>();
	static private final Map<EditStateFont,String[]> UNSUPPRESSED_CACHE = new HashMap<>();
	
	public String[] getHtmlWrappers(boolean suppressed) {
		String[] result = NO_WRAPPERS;
		if (suppressed) {
			result = SUPPRESSED_CACHE.get(font);
			if (result == null) {
				result = new String[] {
						"<u>" + font.getWrapPrefix(),
						font.getWrapSuffix() + "</u>"
				};
				SUPPRESSED_CACHE.put(font, result);
			}
		}
		else {
			result = UNSUPPRESSED_CACHE.get(font);
			if (result == null) {
				result = new String[] {
						font.getWrapPrefix(),
						font.getWrapSuffix()
				};
				UNSUPPRESSED_CACHE.put(font, result);
			}
		}
		return result;
	}
}
