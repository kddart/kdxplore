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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Bag;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRule.Calculated;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.stats.SimpleStatistics.StatName;

import net.pearcan.exhibit.ExhibitColumn;
import net.pearcan.util.StringUtil;

public class StatsUtil {
	
	static private final Map<SimpleStatistics.StatName,Method> METHOD_BY_STATNAME = new HashMap<SimpleStatistics.StatName, Method>();
	
	static private boolean checkDone = false;
	// Validation check
	static {
		initCheck();
	}
	
	
	static public void initCheck() {
		if (checkDone) {
			return;
		}
		Set<String> okIfExhibitMissing = new HashSet<>();
		Collections.addAll(okIfExhibitMissing, "getFormat", "getValueClass",
				"getStatsName",
				"getLowOutliers", "getHighOutliers");
		List<String> errors = new ArrayList<String>();
		
		Set<String> unMatchedStatNameValues = new HashSet<String>();
		for (SimpleStatistics.StatName sname : StatName.values()) {
			unMatchedStatNameValues.add(sname.displayName);
		}
		
		for (Method m : SimpleStatistics.class.getDeclaredMethods()) {
			
			if (! Modifier.isPublic(m.getModifiers())) {
				continue; // shouldn't happen!
			}
			
			ExhibitColumn ec = m.getAnnotation(ExhibitColumn.class);
			
			if (ec == null) {
				if (okIfExhibitMissing.contains(m.getName())) {
//					if ("getQuartiles".equals(m.getName())) {
//						System.err.println("%TODO: @ExhibitColumn for 'SimpleStatistics.getQuartiles()'");
//					}
				}
				else {
					errors.add("Missing @ExhibitColumn: " + m.getName());
				}
			}
			else {
				String ecValue = ec.value();
				boolean found = false;
				for (SimpleStatistics.StatName sname : StatName.values()) {
					if (sname.displayName.equals(ecValue)) {
						unMatchedStatNameValues.remove(sname.displayName);
						METHOD_BY_STATNAME.put(sname, m);
						found = true;
						break;
					}
				}
				if (! found) {
					errors.add("Doesn't match any StatName: '" + ecValue + "', method=" + m.getName());
				}
			}
		}
		
		if (! unMatchedStatNameValues.isEmpty()) {
			errors.add(StringUtil.join("Unmatched StatName values: ", " ", unMatchedStatNameValues));
		}
		
		if (! errors.isEmpty()) {
			throw new RuntimeException(StringUtil.join("Problems in SimpleStatistics config: ", "\n", errors));
		}
		
		checkDone = true;
	}

	public static Class<?> getValueClass(SimpleStatistics.StatName statName) {
		Method method = METHOD_BY_STATNAME.get(statName);
		return method.getReturnType();
	}

	static public Object getStatNameValue(SimpleStatistics<?> ss, SimpleStatistics.StatName statName) {
		Object result = null;
		Method method = METHOD_BY_STATNAME.get(statName);
		try {
			result = method.invoke(ss);
		} catch (IllegalAccessException | InvocationTargetException e) {
			Shared.Log.e("StatsUtil", "SimpleStatistics." + method.getName(), e);
		}
		return result;
	}
	
	static public Double computeDoubleMedian(List<Double> values) {
		Double median;
		int nValues = values.size();
		switch (nValues) {
		case 0:
			median = null;
			break;
		case 1:
			median = values.get(0);
			break;
		default:
			int mid = nValues / 2;
			if (0 == (nValues & 1)) {
				// Even
				median = ( values.get(mid - 1) + values.get(mid) ) / 2.0;
			}
			else {
				// Odd
				median = values.get(mid + 1);
			}
			break;
		}
		return median;
	}
	
	static public Integer computeIntegerMedian(List<Integer> values) {
		Integer median;
		int nValues = values.size();
		switch (nValues) {
		case 0:
			median = null;
			break;
		case 1:
			median = values.get(0);
			break;
		default:
			int mid = nValues / 2;
			if (0 == (nValues & 1)) {
				// Even
				median = (int) ( ( values.get(mid - 1) + values.get(mid) ) / 2.0 );
			}
			else {
				// Odd
				median = values.get(mid + 1);
			}
			break;
		}
		return median;
	}
	
	static public Long computeLongMedian(List<Long> values) {
		Long median;
		int nValues = values.size();
		switch (nValues) {
		case 0:
			median = null;
			break;
		case 1:
			median = values.get(0);
			break;
		default:
			int mid = nValues / 2;
			if (0 == (nValues & 1)) {
				// Even
				median = ( values.get(mid - 1) + values.get(mid) ) / 2;
			}
			else {
				// Odd
				median = values.get(mid + 1);
			}
			break;
		}
		return median;
	} 

	static public String computeStringMedian(List<String> values) {
		String median;
		int nValues = values.size();
		int mid = nValues / 2;
		if (0 == (nValues & 1)) {
			// Even
			if ((mid+1) >= values.size()) {
				Shared.Log.d("StatsUtil.computeStringMedian: ", 
						"(mid+1) >= values.size()   ::  " + (mid+1) + " >= " + values.size());
			}
			String a = values.get(mid-1);
			String b = values.get(mid);
			if (a.equals(b)) {
				median = a;
			}
			else {
				median = values.get(mid-1) + ":" + values.get(mid);
			}
		}
		else {
			// Odd
			median = values.get(mid + 1);
		}
		return median;
	}
	
	static public List<String> computeMode(Bag<String> svalues, NumericTraitValidationProcessor tvp) {
		
		List<String> result;
		
		Map<Integer,List<String>> valuesByCount = new HashMap<Integer,List<String>>();
		
		for (String v : svalues.uniqueSet()) {
			Integer count = svalues.getCount(v);
			List<String> list = valuesByCount.get(count);
			if (list==null) {
				list = new ArrayList<String>();
				valuesByCount.put(count, list);
			}
			list.add(v);
		}
		List<Integer> counts = new ArrayList<Integer>(valuesByCount.keySet());
		int nCounts = counts.size();
		
		if (nCounts < 1) {
			result = new ArrayList<>();;
		}
		else if (nCounts == 1) {
			result = valuesByCount.get(counts.get(0));
		}
		else {
			Collections.sort(counts);
			
			int lastIndex = nCounts - 1;
			int countMax = counts.get(lastIndex);
			
			result = valuesByCount.get(countMax);
		}
		
		return result;
	}
	
//	static private Pair<String,ValidationRule> getNumericInfo(Trait trait) throws InvalidRuleException {
//		ValidationRule rule = ValidationRule.create(trait.getTraitValRule());
//		
//		String fmt = null;
//		if (rule.isRange()) {
//			fmt = ((Range) rule).getNumberFormat();
//		}
//		
//		return new Pair<>(fmt, rule);
//	}
	
	/**
	 * 
	 * @param statsName 
	 * @param nStdDevForOutlier
	 * @param trialPlantingDate
	 * @param trait
	 * @param samples
	 * @return
	 */
	static public SimpleStatistics<?> createStatistics(
			String statsName, 
			Integer nStdDevForOutlier,
			Date trialPlantingDate, 
			Trait trait, 
			List<KdxSample> samples) 
	{
		SimpleStatistics<?> statistics = null;
		
		if (! samples.isEmpty()) {
			try {
				ValidationRule vrule;
				String traitValRule = trait.getTraitValRule();
				switch (trait.getTraitDataType()) {
					
				case CALC:
					vrule = ValidationRule.create(traitValRule);
					if (vrule instanceof Calculated) {
						Calculated calculated = (Calculated) vrule;
						if (calculated.isIntegralRange()) {
							statistics = new IntegerSimpleStatistics(statsName,
									samples, 
									nStdDevForOutlier,
									new IntegerTraitValidationProcessor(vrule));
						}
						else {
							vrule = ValidationRule.create(traitValRule);
							statistics = new DoubleSimpleStatistics(statsName,
									samples, 
									nStdDevForOutlier,
									new DoubleTraitValidationProcessor(vrule));
						}
					}
					else {
						statistics = new TextSimpleStatistics(statsName,
								samples, 
								new TraitValidationProcessorAlwaysValid());
					}
					break;
				case INTEGER:
					vrule = ValidationRule.create(traitValRule);
					statistics = new IntegerSimpleStatistics(statsName,
							samples, 
							nStdDevForOutlier,
							new IntegerTraitValidationProcessor(vrule));
					break;
				case DECIMAL:
					vrule = ValidationRule.create(traitValRule);
					statistics = new DoubleSimpleStatistics(statsName,
							samples, 
							nStdDevForOutlier,
							new DoubleTraitValidationProcessor(vrule));
					break;
					
				case ELAPSED_DAYS:
					if (traitValRule != null && ! traitValRule.isEmpty()) {
						vrule = ValidationRule.create(traitValRule);
//						vrule = ValidationRule.createForChecking(traitValRule, true);
					}
					else {
						vrule = null;
					}
					statistics = new IntegerSimpleStatistics(statsName,
							samples, 
							nStdDevForOutlier,
							new ElapsedDaysTraitValidationProcessor(vrule /*, trialPlantingDate*/)
					);
					break;
					
				case DATE:
					statistics = new DateSimpleStatistics(statsName,
							samples, 
							nStdDevForOutlier);
					break;
					
				case CATEGORICAL:
					vrule = ValidationRule.create(traitValRule);
					statistics = new CategoricalSimpleStatistics(statsName,
							samples, 
							new CategoricalTraitValidationProcessor(vrule));
					break;

				case TEXT:
				default:
					statistics = new TextSimpleStatistics(statsName,
							samples, 
							new TraitValidationProcessorAlwaysValid());
					break;
				}
			} catch (InvalidRuleException e) {
				statistics = new TextSimpleStatistics(statsName,
						samples, 
						new TraitValidationProcessorAlwaysValid());
			}			
		}	
		
		return statistics;		
	}
}
