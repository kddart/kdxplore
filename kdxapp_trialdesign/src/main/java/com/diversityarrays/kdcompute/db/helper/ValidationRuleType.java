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
package com.diversityarrays.kdcompute.db.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diversityarrays.util.Pair;

@SuppressWarnings("nls")
public enum ValidationRuleType {
//	BOOLEX,
//	REGEX,
//	CHOICE,
	RANGE,
	LERANGE,
	RERANGE,
	BERANGE,
//	ELAPSED_DAYS,
//	CALC(true)
	;

	static public final String CALC_PATTERN_STRING = "^CALC *\\((.*)\\)$";

	public final int expressionGroupIndex;
	public final Pattern pattern;
	
	ValidationRuleType() {
		this(false);
	}
	
	ValidationRuleType(boolean computed) {
		if (computed) {
			pattern = Pattern.compile(CALC_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
			expressionGroupIndex = 1;
		}
		else {
			expressionGroupIndex = 1;
			pattern = Pattern.compile(
					"^"+Pattern.quote(this.name())+"\\((.*)\\)$",
					Pattern.CASE_INSENSITIVE);		
		}
	}
	
	
	static public Pair<ValidationRuleType,String> parseTypeAndExpression(String ruleText) {

        if (ruleText == null) {
            return null;        //Not every trait has validation
        }

		for (ValidationRuleType vrt : ValidationRuleType.values()) {
			Matcher m = vrt.pattern.matcher(ruleText);
			if (m.matches()) {
				return new Pair<>(vrt, m.group(vrt.expressionGroupIndex).trim());
			}
		}
		return null;
	}

	public boolean isRange() {
		switch (this) {
		case RANGE:
		case LERANGE:
		case RERANGE:
		case BERANGE:
			return true;
		default:
			return false;
		}
	}

    public boolean includesLowerLimit() {
        switch (this) {
        case BERANGE:
            return false;
        case LERANGE:
            return false;
        case RANGE:
            return true;
        case RERANGE:
            return true;
        default:
            return false;
        }
    }

    public boolean includesUpperLimit() {
        switch (this) {
        case BERANGE:
            return false;
        case LERANGE:
            return true;
        case RANGE:
            return true;
        case RERANGE:
            return false;
        default:
            return false;
        }
    }
}
