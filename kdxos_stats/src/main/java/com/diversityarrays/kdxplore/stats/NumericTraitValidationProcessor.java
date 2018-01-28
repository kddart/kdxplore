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
package com.diversityarrays.kdxplore.stats;

import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRule.Range;
abstract public class NumericTraitValidationProcessor extends
		AbstractTraitValidationProcessor<Number> {

	protected ValidationRule validationRule;
	
	abstract public Class<? extends Number> getNumberClass();
	
	public NumericTraitValidationProcessor(ValidationRule vr) {
		validationRule = vr;
	}
	
	public String getStringNumberFormat() {
		String result = null;
		if (validationRule!=null && (validationRule.isElapsedDays() || validationRule.isRange())) {
			result = ((Range) validationRule).getNumberFormat();
		}
		return result;
	}
}
