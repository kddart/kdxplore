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

import java.text.DateFormat;
import java.util.Date;

import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;

public class SampleValue {
	
	public boolean multiple;
	public final String rawTraitValue;
	public final TraitValueType traitValueType;
	public final String displayValue;
	
	public SampleValue(String s, TraitValueType tvt, String dv) {
		this.rawTraitValue = s;
		this.traitValueType = tvt;
		this.displayValue = dv;
	}
	
	public void addDisplayValue(String dv) {
		if (displayValue == null) {
			if (dv != null) {
				multiple = true;
			}
		}
		else if (! displayValue.equals(dv)) {
			multiple = true;
		}
	}
	
	@Override
	public String toString() {
		return displayValue;
	}
	
	static public String toDisplayValue(String raw, TraitValueType tvt, TraitDataType tdt, 
			DateFormat dateFormat, Date trialPlantingDate) 
	{
		String result;
		switch (tvt) {
		case MISSING:
			result = "-missing-";
			break;
		case NA:
			result = "N/A";
			break;
		case SET:
			switch (tdt) {
			case CATEGORICAL:
			case CALC:
			case TEXT:
			case DATE:
			case INTEGER:
			case DECIMAL: // should already look correct
				result = raw;
				break;
			case ELAPSED_DAYS:
				result = raw;
//				try {
//					// TAG_ELAPSED_DAYS_AS_INTEGER: In case we didn't convert
//					Date date = dateFormat.parse(raw);
//					int nDays = DateDiffChoice.differenceInDays(trialPlantingDate, date);
//					result = String.valueOf(nDays);
//				} catch (ParseException e) {
//					result = raw;
//				}
				break;
			default:
				result = "?" + raw; //$NON-NLS-1$
				break;
			}
			break;
		case UNSET:
			result = null;
			break;
		default:
			result = "?" + raw + "?"; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		
		}
		return result;
	}
}
