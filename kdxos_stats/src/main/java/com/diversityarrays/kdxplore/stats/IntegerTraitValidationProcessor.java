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
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.util.Either;

public class IntegerTraitValidationProcessor extends NumericTraitValidationProcessor {

	public IntegerTraitValidationProcessor(ValidationRule vr) {
		super(vr);
	}

	public Integer transformValue(String traitValue) {
		Integer result = null;
		try { result = new Integer(traitValue); } 
		catch (NumberFormatException ignore) { }
		return result;
	}
	
	@Override
	public Either<TraitValueType,Number> isTraitValueValid(String traitValue) {

		TraitValueType tvt = TraitValue.classify(traitValue);
		
		if (TraitValueType.SET.equals(tvt)) {
			if (validationRule.evaluate(traitValue)) {
				
				try {
					Integer value = new Integer(traitValue);
					return Either.right(value);
				}
				catch (NumberFormatException e) { }
			}
		}

		invalidValues.add(traitValue);

		return Either.left(tvt);
	}

	@Override
	public Class<? extends Number> getNumberClass() {
		return Integer.class;
	}
}
