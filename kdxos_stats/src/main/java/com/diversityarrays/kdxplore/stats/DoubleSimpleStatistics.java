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

public class DoubleSimpleStatistics extends AbstractSimpleStatistics<Double> {
	
	private static final boolean USE_TWO_PASS = true;

	// mean, variance, standard deviation, 
	// max, min, 
	// mode, median, 
	// standard error, 
	// quartiles, outliers (> 2 SD for numeric, histograms for categorical)
	
	private final Double minValue;
	private final Double maxValue;

	private final Double mean;
	private final Double median;
	
	private Double quartile1;
	private Double quartile3;

	private final DecimalFormat decimalFormat;
	
	public DoubleSimpleStatistics(String statsName,
			List<KdxSample> sampleMeasurements, 
			Integer nStdDevForOutlier, 
			NumericTraitValidationProcessor tvp) 
	{
		super(statsName, Double.class);

		String stringFormat = tvp.getStringNumberFormat();

		if (tvp.validationRule.isIntegralRange()) {
			throw new IllegalStateException("ValidationRule is for an integralRange: " + tvp.validationRule);
		}
		
		decimalFormat = tvp.validationRule.getDecimalFormat();
	
		nSampleMeasurements = sampleMeasurements.size();
		
		double sum = 0;
		@SuppressWarnings("unused")
		double ssq = 0;
		
		Bag<String> svalues = new HashBag<String>();
		List<Double> values = new ArrayList<>(nSampleMeasurements);
		
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
							double d = number.doubleValue();
							sum += d;
							ssq += d * d;

							values.add(d);
							if (stringFormat==null) {
								svalues.add(traitValue);
							}
							else {
								svalues.add(String.format(stringFormat, d));
							}							
							
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
			mean = values.get(0);
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
			mean = sum / nValidValues;
			if (USE_TWO_PASS) {
				double s2 = 0;
				for (Double d : values) {
					s2 += (d - mean) * (d - mean);
				}
				variance = s2 / (nValidValues - 1);
			}
			else {
				variance = (ssq - (sum*sum)/nValidValues) / (nValidValues - 1);
			}
			stddev = Math.sqrt(variance);
			
			stderr = stddev / Math.sqrt(nValidValues);

			Collections.sort(values);
			minValue = values.get(0);
			maxValue = values.get(values.size()-1);
			
			median = StatsUtil.computeDoubleMedian(values);
			
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
							sb.append(String.format(numberFormat, number.doubleValue()));
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

	        quartile1 = q1;
	        quartile3 = q3;

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
	
	private void collectOutliers(List<Double> values, double low, double hi) {
		int nout = 0;
		for (Double value : values) {
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
	
//	public static double calculateQ1(List<Double> values) {
//
//        double result = Double.NaN;
//        int count = values.size();
//        if (count > 0) {
//            if (count % 2 == 1) {
//                if (count > 1) {
//                    result = Statistics.calculateMedian(values, 0, count / 2);
//                }
//                else {
//                    result = Statistics.calculateMedian(values, 0, 0);
//                }
//            }
//            else {
//                result = Statistics.calculateMedian(values, 0, count / 2 - 1);
//            }
//
//        }
//        return result;
//    }
//	
//    public static double calculateQ3(List<Double> values) {
//        double result = Double.NaN;
//        int count = values.size();
//        if (count > 0) {
//            if (count % 2 == 1) {
//                if (count > 1) {
//                    result = Statistics.calculateMedian(values, count / 2,
//                            count - 1);
//                }
//                else {
//                    result = Statistics.calculateMedian(values, 0, 0);
//                }
//            }
//            else {
//                result = Statistics.calculateMedian(values, count / 2,
//                        count - 1);
//            }
//        }
//        return result;
//    }
	
	@Override
	public Format getFormat() {
		return decimalFormat;
	}
	
	@Override
	public Double getMinValue() {
		return minValue;
	}

	@Override
	public Double getMaxValue() {
		return maxValue;
	}

	@Override
	public Double getMean() {
		return mean;
	}

	@Override
	public Double getMedian() {
		return median;
	}

	@Override
	public Double getQuartile1() {
		return quartile1;
	}

	@Override
	public Double getQuartile3() {
		return quartile3;
	}
	
}
