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

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.pearcan.util.MixedString;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Either;

public class TextSimpleStatistics extends AbstractSimpleStatistics<String> {

	private final String median;
	private final String maxValue;
	private final String minValue;

	public TextSimpleStatistics(String statsName,
			List<KdxSample> sampleMeasurements,
			TraitValidationProcessor<String> tvp) 
	{
		super(statsName, String.class);

		nSampleMeasurements = sampleMeasurements.size();
		
		Bag<String> svalues = new HashBag<>();
		List<String> values = new ArrayList<>(nSampleMeasurements);
		
		for (KdxSample sm : sampleMeasurements) {
			String traitValue = sm.getTraitValue();
			switch (TraitValue.classify(traitValue)) {
			case NA:
				++nNA;
				break;
			case SET:
				Either<TraitValueType,String> either = tvp.isTraitValueValid(traitValue);
				if (either.isRight()) {
					String tv = either.right();
					if (tv == null) {
						++nMissing;
					}
					else {
						values.add(tv);
						svalues.add(traitValue);
					}
				}
				else {
					++nInvalid;
				}
				break;
			case MISSING:
			case UNSET:
			default:
				++nMissing;
				break;			
			}
		}
		
		nValidValues = values.size();
		switch (nValidValues) {
		case 0:
			minValue = null;
			maxValue = null;
			mode = null;
			median = null;
			break;
		case 1:
			mode = values.get(0);
			median = mode;
			minValue = mode;
			maxValue = mode;
			break;
		default:
			
			Map<String,MixedString> mixedByValue = new HashMap<>();
			for (String v : svalues) {
				MixedString ms = MixedString.createFloatingMixed(v, 
						MixedString.DIFFERENTIATE_ON_STRINGS_WHEN_NUMERICALLY_EQUAL);
				mixedByValue.put(v, ms);
			}
			
			Comparator<String> comparator = new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					MixedString lft = mixedByValue.get(o1);
					MixedString ryt = mixedByValue.get(o2);
					
					int diff;
					if (lft==null) {
						if (ryt==null) {
							diff = 0;
						}
						else {
							diff = -1;
						}
					}
					else if (ryt==null) {
						diff = 1;
					}
					else {
						diff = lft.compareTo(ryt);
					}
					return diff;
				}
			};
			Collections.sort(values, comparator);
			
			minValue = values.get(0);
			maxValue = values.get(values.size()-1);
			median = StatsUtil.computeStringMedian(values);
			List<String> modes = StatsUtil.computeMode(svalues, null);
			
			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (String s : modes) {
				sb.append(sep);
				if (tvp == null) {
					sb.append(s);
				}
				else {
					Either<TraitValueType, String> either = tvp.isTraitValueValid(s);
					if (either.isRight()) {
						sb.append(either.right());
					}
					else {
						sb.append(s);
					}
				}
				sep = " , ";
			}
			mode = sb.toString();
			break;
		}
	}

	@Override
	public Format getFormat() {
		return null;
	}

	@Override
	public String getMinValue() {
		return minValue;
	}

	@Override
	public String getMaxValue() {
		return maxValue;
	}

	@Override
	public String getMean() {
		return null;
	}

	@Override
	public String getMedian() {
		return median;
	}

	@Override
	public String getQuartile1() {
		return null;
	}
	@Override
	public String getQuartile3() {
		return null;
	}

}
