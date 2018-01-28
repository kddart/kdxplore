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

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;

import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Either;

public class IntegerSimpleStatistics extends AbstractSimpleStatistics<Integer> {
	
	private static final boolean USE_TWO_PASS = true;

	// mean, variance, standard deviation, 
	// max, min, 
	// mode, median, 
	// standard error, 
	// quartiles, outliers (> 2 SD for numeric, histograms for categorical)
	
	private final Integer minValue;
	private final Integer maxValue;

	private final Integer mean;
	private final Integer median;
	
	private Integer quartile1;
	private Integer quartile3;

	private final DecimalFormat decimalFormat = new DecimalFormat("#");
	
	public IntegerSimpleStatistics(String statsName,
			List<KdxSample> sampleMeasurements, 
			Integer nStdDevForOutlier, 
			NumericTraitValidationProcessor tvp) 
	{
		super(statsName, Integer.class);
		
		nSampleMeasurements = sampleMeasurements.size();
		
		long sum = 0;
		@SuppressWarnings("unused")
		double ssq = 0;
		
		Bag<String> svalues = new HashBag<String>();
		List<Integer> values = new ArrayList<>(nSampleMeasurements);
		
		for (KdxSample sm : sampleMeasurements) {
			String traitValue = sm.getTraitValue();
			
			switch (TraitValue.classify(traitValue)) {
			case NA:
				++nNA;
				break;
			case SET:
				Either<TraitValueType,Number> either = tvp.isTraitValueValid(traitValue);
				if (either.isRight()) {
					try {
						Number number = either.right();
						if (number==null) {
							++nMissing;
						}
						else {
							int i = number.intValue();
							sum += i;
							ssq += i * 1.0 * i;

							values.add(i);
							svalues.add(String.valueOf(i));
						}
					} catch (NumberFormatException e) {
						++nInvalid;
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
			mean = null;
			mode = null;
			median = null;
			minValue = null;
			maxValue = null;
			
			variance = null;
			stddev = null;
			nOutliers = null;
			
			stderr = null;
			break;
		case 1:
			mean = values.get(0).intValue();
			mode = mean.toString();
			median = mean;
			minValue = mean;
			maxValue = mean;

			variance = null;
			stddev = null;
			nOutliers = null;
			
			stderr = null;
			break;
		
		default:
			mean = (int) sum / nValidValues;
			if (USE_TWO_PASS) {
				double s2 = 0;
				for (Number n : values) {
					double i = n.doubleValue();
					s2 += (i - mean) * (i - mean);
				}
				variance = s2 / (nValidValues - 1);
			}
			else {
				variance = (ssq - (sum*sum)/nValidValues) / (nValidValues - 1);
			}
			stddev = Math.sqrt(variance);
			
			stderr = stddev / Math.sqrt(nValidValues);

			Collections.sort(values);
			minValue = values.get(0).intValue();
			maxValue = values.get(values.size()-1).intValue();
			
			median = StatsUtil.computeIntegerMedian(values);
			
			List<String> modes = StatsUtil.computeMode(svalues, tvp);
			
			String numberFormat = tvp==null ? null : tvp.getStringNumberFormat();

			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (String s : modes) {
				sb.append(sep);
				if (tvp == null) {
					sb.append(s);
				}
				else {
					Either<TraitValueType, Number> either = tvp.isTraitValueValid(s);
					if (either.isRight()) {
						Number number = either.right();
						if (Integer.class.isAssignableFrom(tvp.getNumberClass())) {
							sb.append(number.intValue());
						}
						else if (numberFormat==null) {
							sb.append(s);
						}
						else {
							sb.append(number.intValue());
						}
					}
					else {
						sb.append(s);
					}
				}
				sep = " , ";
			}
			mode = sb.toString();
			
	        double q1 = BoxAndWhiskerCalculator.calculateQ1(values);
	        double q3 = BoxAndWhiskerCalculator.calculateQ3(values);

	        quartile1 = (int) Math.round(q1);
	        quartile3 = (int) Math.round(q3);
	        
			if (nStdDevForOutlier == null) {
		        double interQuartileRange = q3 - q1;

		        double lowerOutlierThreshold = q1 - (interQuartileRange * 1.5);
		        double upperOutlierThreshold = q3 + (interQuartileRange * 1.5);

				collectOutliers(values, lowerOutlierThreshold, upperOutlierThreshold);
			}
			else {
				double lowerOutlierThreshold = mean - (nStdDevForOutlier * stddev);
				double upperOutlierThreshold = mean + (nStdDevForOutlier * stddev);

				collectOutliers(values, lowerOutlierThreshold, upperOutlierThreshold);
			}

			break;
		}
	}
	
	private void collectOutliers(List<Integer> values, double low, double hi) {
		int nout = 0;
		for (Integer value : values) {
			if (value < low) {
				++nout;
				lowOutliers.add(value);
			}
			else if (value > hi) {
				++nout;
				highOutliers.add(value);
			}
		}
		nOutliers = nout;
	}
	
	@Override
	public Format getFormat() {
		return decimalFormat;
	}

	@Override
	public Integer getMedian() {
		return median;
	}

	@Override
	public Integer getMinValue() {
		return minValue;
	}

	@Override
	public Integer getMaxValue() {
		return maxValue;
	}

	@Override
	public Integer getMean() {
		return mean;
	}

	@Override
	public Integer getQuartile1() {
		return quartile1;
	}
	
	@Override
	public Integer getQuartile3() {
		return quartile3;
	}
	
}
