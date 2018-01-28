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

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;

import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class DateSimpleStatistics extends AbstractSimpleStatistics<Date> {

	private final DateFormat dateFormat = TraitValue.getTraitValueDateFormat();

	private final Date median;
	private final Date maxValue;
	private final Date minValue;
	private final Date mean;

	private Date quartile1; // TODO
	private Date quartile3; // TODO
	
	public DateSimpleStatistics(String statsName,
			List<KdxSample> samples,
			Integer nStdDevForOutlier) 
	{
		super(statsName, Date.class);

		nSampleMeasurements = samples.size();

		Bag<String> bag = new HashBag<>();
		List<Long> values = new ArrayList<>(nSampleMeasurements);
		
		for (KdxSample sm : samples) {
			switch (TraitValue.classify(sm.getTraitValue())) {
			case NA:
				++nNA;
				break;
			case SET:
				try {
					Date date = dateFormat.parse(sm.getTraitValue());
					
					long millis = date.getTime();
					values.add(millis);
					bag.add(String.valueOf(millis));
				} catch (ParseException e) {
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
			mean = null;
			
			variance = null;
			stddev = null;
			nOutliers = null;
			
			stderr = null;
			break;

		case 1:
			mean = new Date(values.get(0));
			median = mean;
			minValue = mean;
			maxValue = mean;
			
			mode = dateFormat.format(mean);
			
			variance = null;
			stddev = null;
			nOutliers = null;
			
			stderr = null;

			break;

		default:
			Collections.sort(values);
			minValue = new Date(values.get(0));
			maxValue = new Date(values.get(values.size()-1));
			
			mean = new Date((minValue.getTime() + maxValue.getTime()) / 2);
			
			long median_l = StatsUtil.computeLongMedian(values);
			median = new Date(median_l);
			List<String> modes = StatsUtil.computeMode(bag, null);

			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (String s : modes) {
				sb.append(sep);
				long millis = Long.parseLong(s);
				Date d = new Date(millis);
				sb.append(dateFormat.format(d));
				sep = " , ";
			}
			mode = sb.toString();
			
			// - - - -
			// Now for variance, stddev, stderr, nOutliers
			Instant start = minValue.toInstant();
			long meanDays = ChronoUnit.DAYS.between(start, mean.toInstant());

			double s2 = 0;
			for (Long v : values) {
				s2 += (v - meanDays) * (v - meanDays);
			}			
			variance = s2 / (nValidValues - 1);
			stddev = Math.sqrt(variance);
			
			stderr = stddev / Math.sqrt(nValidValues);
			
	        double q1 = BoxAndWhiskerCalculator.calculateQ1(values);
	        double q3 = BoxAndWhiskerCalculator.calculateQ3(values);
	        
			int nout = 0;
			if (nStdDevForOutlier == null) {
		        double interQuartileRange = q3 - q1;

		        double lowerOutlierThreshold = q1 - (interQuartileRange * 1.5);
		        double upperOutlierThreshold = q3 + (interQuartileRange * 1.5);
		        
		        for (Long value : values) {
		        	if (value < lowerOutlierThreshold) {
		        		++nout;
		        		lowOutliers.add(new Date(value));
		        	}
		        	else if (value > upperOutlierThreshold) {
		        		++nout;
		        		highOutliers.add(new Date(value));
		        	}
		        	
		        	if (lowerOutlierThreshold < value
		        		||
		        		value < upperOutlierThreshold) 
		        	{
		        		++nout;
		        	}
		        }
			}
			else {
				double lowerOutlierThreshold = meanDays - (nStdDevForOutlier * stddev);
				double upperOutlierThreshold = meanDays + (nStdDevForOutlier * stddev);
				
				for (Long v : values) {
					Date d = new Date(v);
					long nDays = ChronoUnit.DAYS.between(start, d.toInstant());
					if (nDays < lowerOutlierThreshold) {
						++nout;
						lowOutliers.add(d);
					}
					else if (nDays > upperOutlierThreshold) {
						++nout;
						highOutliers.add(d);
					}
				}
			}
			nOutliers = nout;
			
			break;
		}	
	}

	@Override
	public Format getFormat() {
		return dateFormat;
	}

	@Override
	public Date getMinValue() {
		return minValue;
	}

	@Override
	public Date getMaxValue() {
		return maxValue;
	}

	@Override
	public Date getMean() {
		return mean;
	}

	@Override
	public Date getMedian() {
		return median;
	}

	@Override
	public Date getQuartile1() {
		return quartile1;
	}

	@Override
	public Date getQuartile3() {
		return quartile3;
	}

}
