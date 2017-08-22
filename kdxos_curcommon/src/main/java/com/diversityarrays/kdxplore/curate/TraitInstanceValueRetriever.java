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
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRule.Calculated;
import com.diversityarrays.daldb.ValidationRule.Range;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.scoring.DateDiffChoice;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public abstract class TraitInstanceValueRetriever<T> implements ValueRetriever<TraitValue> {
	
	static public TraitInstanceValueRetriever<?> getValueRetriever(
	        Trial trial, 
	        TraitInstance ti,
	        Function<TraitInstance, List<KdxSample>> sampleProvider) 
	throws InvalidRuleException 
	{
		TraitInstanceValueRetriever<?> result = null;
		
		TraitDataType tdt = ti.trait.getTraitDataType();
		if (TraitDataType.TEXT == tdt) {
			result = new TextValueRetriever(trial, ti, sampleProvider.apply(ti));
		}
		else {
			ValidationRule vrule;
			String vrule_s = ti.trait.getTraitValRule();

			switch (tdt) {
			case CATEGORICAL:
				vrule = ValidationRule.create(vrule_s);
				result = new CategoricalValueRetriever(trial, ti, vrule);
				break;
			case DATE:
				result = new DateValueRetriever(trial, ti, sampleProvider.apply(ti));
				break;
			case CALC:
				vrule = ValidationRule.create(vrule_s);
				ValidationRule rangeCheckRule = ((Calculated) vrule).getRangeCheckRule();
				if (rangeCheckRule.isIntegralRange()) {
					result = new IntegerValueRetriever(trial, ti, rangeCheckRule);
				}
				else {
					result = new DecimalValueRetriever(trial, ti, rangeCheckRule);
				}
				break;
			case DECIMAL:
				vrule = ValidationRule.create(vrule_s);
				result = new DecimalValueRetriever(trial, ti, vrule);
				break;
			case ELAPSED_DAYS:
				result = new ElapsedDaysValueRetriever(trial, ti, sampleProvider.apply(ti));
				break;
			case INTEGER:
				vrule = ValidationRule.create(vrule_s);
				result = new IntegerValueRetriever(trial, ti, vrule);
				break;
			case TEXT:
				throw new RuntimeException("Should not be here: " + tdt);
			default:
				throw new RuntimeException("Unhandled TraitDataType: " + tdt);
			}
		}

		return result;
	}
	
	static private String defaultMakeDisplayValue(TraitValueType tvt, String setValue) {
		switch (tvt) {
		case MISSING:
			return TraitValue.EXPORT_VALUE_MISSING;
		case NA:
			return TraitValue.EXPORT_VALUE_NA;
		case SET:
			return setValue;
		case UNSET:
			return ""; // TraitValue.EXPORT_VALUE_UNSCORED;
		default:
			break;
		}
		throw new RuntimeException("Unhandled TraitValueType: " + tvt);
	}
	
	protected final TraitInstance traitInstance;
	private final String displayName;
	private final Class<T> comparableValueClass;
	
	private TraitInstanceValueRetriever(Trial trial, TraitInstance ti, Class<T> cvClass) {
		
		this.traitInstance = ti;
		this.comparableValueClass = cvClass;
		
		Trait trait = traitInstance.trait;
		if (trait == null) {
			throw new IllegalArgumentException("TraitInstance does not have the trait field set");
		}
		String traitName = trait.getTraitAlias();
		if (traitName==null || traitName.trim().isEmpty()) {
			traitName = trait.getTraitName();
		}
		displayName = trial.getTraitNameStyle().makeTraitInstanceName(traitInstance);
	}
	
	@Override
	public String toString() {
	    return "TIVR[" + displayName + "]";
	}
	
	@Override
	final public int hashCode() {
	    return traitInstance.hashCode();
	}
	
	@Override
	final public boolean equals(Object o) {
	    if (this==o) return true;
	    if (! (o instanceof TraitInstanceValueRetriever)) return false;
	    TraitInstanceValueRetriever<?> other = (TraitInstanceValueRetriever<?>) o;
	    return this.traitInstance.equals(other.traitInstance);
	}

	@Override
    public ValueType getValueType() {
	    return ValueType.TRAIT_INSTANCE;
	}
	
	@Override
    public boolean isPlotColumn() {
        return false;
    }

    @Override
    public boolean isPlotRow() {
        return false;
    }


	/**
	 * This is the class that the TraitValue.comparable should be.
	 * @return
	 */
	public Class<T> getComparableValueClass() {
		return comparableValueClass;
	}
	
	public String getStringNumberFormat() {
		return null;
	}
	
	public TraitInstance getTraitInstance() {
		return traitInstance;
	}
	
	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public com.diversityarrays.kdxplore.curate.ValueRetriever.TrialCoord getTrialCoord() {
		return TrialCoord.NONE;
	}

	@Override
	public Class<TraitValue> getValueClass() {
		return TraitValue.class;
	}
	
	abstract protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue);
	abstract protected String makeDisplayValue(TraitValueType tvt, String setValue);
	
	@Override
	public TraitValue getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, TraitValue valueIfNull) {
		
		Sample sample = infoProvider.getSampleForTraitInstance(pos, traitInstance);

		TraitValue result = createTraitValue(sample, valueIfNull);
		return result;
	}

	public TraitValue createTraitValue(Sample sample, TraitValue valueIfNull) {
		TraitValue result = valueIfNull;
		if (sample != null) {
			String sampleValue = sample.getTraitValue();

			TraitValueType tvt = TraitValue.classify(sampleValue);

			String dv = makeDisplayValue(tvt, sampleValue);
			
			Comparable<?> comparable = makeComparable(tvt, sampleValue);
			boolean suppressed = (sample instanceof KdxSample) && (((KdxSample) sample).isSuppressed());
			result = new TraitValue(sampleValue, dv, 1, tvt, comparable, suppressed);
		}
		return result;
	}
	
	static public TraitValue makeTraitValue(
			Sample sample, 
			TraitInstance ti, 
			ValidationRule vrule,
			Date trialPlantingDate) 
	{
		TraitValue result;
		
		String sampleValue = sample.getTraitValue();
		boolean suppressed = (sample instanceof KdxSample) && (((KdxSample) sample).isSuppressed());
		
		TraitValueType tvt = TraitValue.classify(sampleValue);
		String dv = defaultMakeDisplayValue(tvt, sampleValue);
		Comparable<?> comparable = null;

		// TODO check about what we should do for MISSING, N/A
		switch (ti.getTraitDataType()) {
		case CALC:
			if (TraitValueType.SET == tvt) {
				Calculated calculated = (Calculated) vrule;
				try {
                    String numberFormat = calculated.getNumberFormat();
					if (calculated.isIntegralRange()) {
						Integer i = new Integer(sampleValue);
	                    dv = String.format(numberFormat, i);
						comparable = i;
					}
					else {
						Double d = new Double(sampleValue);
	                    dv = String.format(numberFormat, d);
						comparable = d;
					}
				}
				catch (NumberFormatException e) {
					
				}
			}
			break;
		case CATEGORICAL:
			comparable = CategoricalValueRetriever.createComparable(tvt, sampleValue);
			break;
		case DATE:
			comparable = DateValueRetriever.createComparable(tvt, sampleValue, 
					TraitValue.getTraitValueDateFormat());
			break;
		case DECIMAL:
			Double d = DecimalValueRetriever.createComparable(tvt, sampleValue);
			if (d != null) {
				String numberFormat = ((Range) vrule).getNumberFormat();
				dv = String.format(numberFormat, d);
			}
			comparable = d;
			break;
		case ELAPSED_DAYS:
			Integer elapsedDaysValue = ElapsedDaysValueRetriever.createComparable(
					tvt, sampleValue, TraitValue.getTraitValueDateFormat(), trialPlantingDate);
			if (elapsedDaysValue != null) {
				dv = elapsedDaysValue.toString();
			}
			comparable = elapsedDaysValue;
			break;
		case INTEGER:
			comparable = IntegerValueRetriever.createComparable(tvt, sampleValue);
			break;
		case TEXT:
			comparable = TraitValueType.SET == tvt ? sampleValue : null;
			break;
		default:
			throw new RuntimeException("Unhandled TraitDataType: " + ti.getTraitDataType());
		}
		
		result = new TraitValue(sampleValue, dv, 1, tvt, comparable, suppressed);
		
		return result;
	}

	static class CategoricalValueRetriever extends TraitInstanceValueRetriever<String> {

		private final List<String> choices;
		
		CategoricalValueRetriever(Trial trial, TraitInstance ti, ValidationRule vrule) {
			super(trial, ti, String.class);
			this.choices = vrule.getChoices();
		}

		@Override
		public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
			Integer result = null;
			TraitValue value = getAttributeValue(infoProvider, pos, null);
			if (value != null) {
				int idx = choices.indexOf(value.rawValue);
				if (idx >= 0) {
					result = idx;
				}
			}
			return result;
		}
		
		@Override
		public int getAxisZeroValue() {
			return 0; // zero is zero!
		}
		
		@Override
		public int getAxisValueCount() {
		    return choices.size();
		}
		
		@Override
		protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue) {
			return createComparable(tvt, sampleValue);
		}
		
		@Override
		protected String makeDisplayValue(TraitValueType tvt, String setValue) {
			return defaultMakeDisplayValue(tvt, setValue);
		}
		
		static public String createComparable(TraitValueType tvt, String sampleValue) {
			return TraitValueType.SET==tvt ? sampleValue : "";
		}
	}
	
	static public class DateValueRetriever extends TraitInstanceValueRetriever<java.util.Date> {

		private final DateFormat dateFormat = TraitValue.getTraitValueDateFormat();
//		private final Instant epochZeroInstant;
		private final LocalDate epochZero;
		private final int valueCount;
		
		DateValueRetriever(Trial trial, TraitInstance ti, List<KdxSample> samples) {
			super(trial, ti, java.util.Date.class);
			epochZero = LocalDate.ofEpochDay(0);

			Date minDate = null;
			Date maxDate = null;
            DateFormat df = TraitValue.getTraitValueDateFormat();

			for (KdxSample sample : samples) {
			    if (sample.hasBeenScored()) {
			        String tv = sample.getTraitValue();
			        try {
                        Date d = df.parse(tv);
                        if (minDate == null) {
                            minDate = d;
                            maxDate = d;
                        }
                        else {
                            if (d.before(minDate)) {
                                minDate = d;
                            }
                            if (d.after(maxDate)) {
                                maxDate = d;
                            }
                        }
                    }
                    catch (ParseException ignore) {
                    }
			    }
			}
			
			if (minDate == null) {
			    valueCount = -1;
			}
			else if (minDate.equals(maxDate)) {
			    valueCount = 1;
			}
			else {
			    int vc = -1;
			    GregorianCalendar gc = new GregorianCalendar();
			    try {
                    long min = getDaysFromEpochZero(minDate, gc);
                    long max = getDaysFromEpochZero(maxDate, gc);
                    vc = (int) (max - min + 1);
			    }
			    catch (DateTimeException e) {
			        
			    }
			    valueCount = vc;
			}
		}

		@Override
		public int getAxisZeroValue() {
			return 0;
		}
		
		@Override
		public int getAxisValueCount() {
		    return valueCount;
		}

		@Override
		public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
			Integer result = null;
			TraitValue tv = getAttributeValue(infoProvider, pos, null);
			if (tv != null && tv.comparable instanceof Date) {
				Date date = (Date) tv.comparable;
				try {
				    GregorianCalendar gc = new GregorianCalendar();
				    long between = getDaysFromEpochZero(date, gc);
    				result = (int) between;
				}
				catch (DateTimeException ignore) { }
			}
			return result;
		}
		
		private long getDaysFromEpochZero(Date date, GregorianCalendar gc) {
            gc.setTime(date);
            int year = gc.get(GregorianCalendar.YEAR);
            int dayOfYear = gc.get(Calendar.DAY_OF_YEAR);
            
            
            LocalDate ldt = LocalDate.ofYearDay(year, dayOfYear);
            long between = ChronoUnit.DAYS.between(epochZero, ldt);
            
            return between;
		}
		
		@Override
		protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue) {
			return createComparable(tvt, sampleValue, dateFormat);
		}
		
		@Override
		protected String makeDisplayValue(TraitValueType tvt, String setValue) {
			return defaultMakeDisplayValue(tvt, setValue);
		}
		
		static public Date createComparable(TraitValueType tvt, String sampleValue, DateFormat dateFormat) {
			Date dateValue = null;
			if (TraitValueType.SET==tvt) {
				try {
					dateValue = dateFormat.parse(sampleValue);
				} catch (ParseException ignore) {
					
				}
			}
			return dateValue;
		}
	}
	
	static class DecimalValueRetriever extends TraitInstanceValueRetriever<Double> {
		
        private final double minimum;
        private final int valueCount;
		private final double increment;
		private String numberFormat;
		
		DecimalValueRetriever(Trial trial, TraitInstance ti, ValidationRule vrule) {
			super(trial, ti, Double.class);
			
            double[] rangeLimits = vrule.getRangeLimits();
            this.minimum = rangeLimits[0];
            double maximum = rangeLimits[1];
			
			int ndecs = vrule.getNumberOfDecimalPlaces();
			this.increment = Math.pow(10.0, -ndecs);
			
			double nValues = (maximum - minimum) / increment;
			if (Double.isInfinite(nValues) || Double.isNaN(nValues)) {
			    valueCount = -1;
			}
			else {
			    valueCount = (int) nValues;
			}
			
			if (vrule instanceof Range) {
				numberFormat = ((Range) vrule).getNumberFormat();
			}
		}
		
		@Override
		public String getStringNumberFormat() {
			return numberFormat;
		}
		
		@Override
		protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue) {
			return createComparable(tvt, sampleValue);
		}
		
		@Override
		protected String makeDisplayValue(TraitValueType tvt, String setValue) {
			if (TraitValueType.SET == tvt) {
				try {
					Double d = new Double(setValue);
					return String.format(numberFormat, d);
				} catch (NumberFormatException ignore) {
				}
			}
			return defaultMakeDisplayValue(tvt, setValue);
		}

		@Override
		public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
			Integer result = null;
			TraitValue value = getAttributeValue(infoProvider, pos, null);
			if (value != null && value.comparable instanceof Double) {
				Double dvalue = (Double) value.comparable;
				if (dvalue >= minimum) {
					double offset = dvalue - minimum;
					result = (int) (offset / increment);
				}
			}
			return result;
		}
		
		@Override
		public int getAxisZeroValue() {
			return (int) minimum; // actually an approximation
		}
		
		@Override
		public int getAxisValueCount() {
		    return valueCount;
		}
		
		static public Double createComparable(TraitValueType tvt, String sampleValue) {
			Double result = null;
			if (TraitValueType.SET == tvt) {
				try {
					result = new Double(sampleValue);
				} catch (NumberFormatException ignore) {
				}
			}
			return result;
		}
	}
	
	static class IntegerValueRetriever extends TraitInstanceValueRetriever<Integer> {

		private final int minimum;
        private final int valueCount;
		
		IntegerValueRetriever(Trial trial, TraitInstance ti, ValidationRule vrule) {
			super(trial, ti, Integer.class);
			
			
			double[] rangeLimits = vrule.getRangeLimits();
            int min = (int) rangeLimits[0];
            int max = (int) rangeLimits[1];
			
			if (! vrule.isRangeStartIncluded()) {
				++min;
			}
			if (! vrule.isRangeEndIncluded()) {
			    --max;
			}
			this.minimum = min;
			this.valueCount = (max - min);
		}

		@Override
		public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
			Integer result = null;
			TraitValue value = getAttributeValue(infoProvider, pos, null);
			if (value != null && value.comparable instanceof Integer) {
				Integer ivalue = (Integer) value.comparable;
				if (ivalue >= minimum) {
					result = ivalue - minimum;
				}
			}
			return result;
		}
		
		@Override
		public int getAxisZeroValue() {
			return minimum;
		}
		
		@Override
		public int getAxisValueCount() {
		    return valueCount;
		}
		
		@Override
		protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue) {
			return createComparable(tvt, sampleValue);
		}

		@Override
		protected String makeDisplayValue(TraitValueType tvt, String setValue) {
			return defaultMakeDisplayValue(tvt, setValue);
		}
		
		static public Integer createComparable(TraitValueType tvt, String sampleValue) {
			Integer result = null;
			if (TraitValueType.SET == tvt) {
				try {
					result = new Integer(sampleValue);
				}
				catch (NumberFormatException ignore) { }
			}
			return result;
		}
	}
	
	static class ElapsedDaysValueRetriever extends TraitInstanceValueRetriever<Integer> {

		private static final String TAG = ElapsedDaysValueRetriever.class.getSimpleName();

		private final DateFormat dateFormat = TraitValue.getTraitValueDateFormat();
		private final Date trialPlantingDate;

        private final int valueCount;
		
		ElapsedDaysValueRetriever(Trial trial, TraitInstance ti, List<KdxSample> samples) {
			super(trial, ti, Integer.class);
			trialPlantingDate = trial.getTrialPlantingDate();
			
			Integer min = null;
			Integer max = null;
			for (KdxSample sample : samples) {
			    if (sample.hasBeenScored()) {
			        try {
			            Integer tv = Integer.valueOf(sample.getTraitValue());
                        if (min==null) {
                            min = tv;
                            max = tv;
                        }
                        else {
                            min = Math.min(min, tv);
                            max = Math.max(max, tv);
                        }
                    }
                    catch (NumberFormatException ignore) {}
			    }
			}
			
			if (min == null) {
			    valueCount = -1;
			}
			else {
			    valueCount = (max - min + 1);
			}
			
		}
		
		@Override
		protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue) {			
			return createComparable(tvt, sampleValue, dateFormat, trialPlantingDate);
		}

		@Override
		public TraitValue getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, TraitValue valueIfNull) {
			
			TraitValue result = valueIfNull;
			
			Sample sample = infoProvider.getSampleForTraitInstance(pos, traitInstance);
			if (sample != null) {
				String sampleValue = sample.getTraitValue();

				TraitValueType tvt = TraitValue.classify(sampleValue);
				String dv = makeDisplayValue(tvt, sampleValue);
				// TAG_ELAPSED_DAYS_AS_INTEGER
				Integer value = createComparable(tvt, sampleValue, dateFormat, trialPlantingDate);
//				String dv = value==null ? "?" : value.toString();

				boolean suppressed = (sample instanceof KdxSample) && (((KdxSample) sample).isSuppressed());
				result = new TraitValue(sampleValue, dv, 1, tvt, value, suppressed);
			}
			return result;
		}
		
		@Override
		protected String makeDisplayValue(TraitValueType tvt, String setValue) {
			String result;
			if (TraitValueType.SET == tvt) {
				Integer nDays = createComparable(tvt, setValue, dateFormat, trialPlantingDate);
				result = nDays==null ? "?" : nDays.toString();
			}
			else {
				result = defaultMakeDisplayValue(tvt, setValue);
			}
			return result;
		}

		@Override
		public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
			Integer result = null;
			TraitValue tv = getAttributeValue(infoProvider, pos, null);
			if (tv != null && tv.comparable instanceof Integer) {
				result = (Integer) tv.comparable;
			}
			return result;
		}
		
		@Override
		public int getAxisZeroValue() {
			return 0; // zero is the planting date
		}
		
		@Override
		public int getAxisValueCount() {
		    return valueCount;
		}
		
		static public Integer createComparable(TraitValueType tvt, String sampleValue, 
				DateFormat dateFormat, Date trialPlantingDate) 
		{
			Integer value = null;
			if (TraitValueType.SET == tvt) {
				// TAG_ELAPSED_DAYS_AS_INTEGER
				if (sampleValue != null) {
					try {
						value = Integer.valueOf(sampleValue);
					} catch (NumberFormatException e) {
						// Hmmm - this one didn't convert !
						Shared.Log.w(TAG, "createComparable: found non-Integer ElapsedDaysValue of '" + sampleValue + "'");
						try {
							Date d = dateFormat.parse(sampleValue);
							value = DateDiffChoice.differenceInDays(trialPlantingDate, d);
						} catch (ParseException ignore) {

						}
					}
				}
			}
			return value;
		}
	}
	
	static class TextValueRetriever extends TraitInstanceValueRetriever<String> {

	    private final List<String> uniqueValues;
		TextValueRetriever(Trial trial, TraitInstance ti, List<KdxSample> samples) {
			super(trial, ti, String.class);

			Set<String> set = new HashSet<>();
			for (KdxSample sample : samples) {
			    if (sample.hasBeenScored()) {
			        String tv = sample.getTraitValue();
			        if (tv != null) {
			            set.add(tv);
			        }
			    }
			}
			uniqueValues = new ArrayList<>(set);
			Collections.sort(uniqueValues);
		}

		@Override
		protected Comparable<?> makeComparable(TraitValueType tvt, String sampleValue) {
			String result = null;
			if (TraitValueType.SET == tvt) {
				result = sampleValue;
			}
			return result;
		}

		@Override
		protected String makeDisplayValue(TraitValueType tvt, String setValue) {
			return defaultMakeDisplayValue(tvt, setValue);
		}
		
		@Override
		public Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos) {
		    Integer result = null;
		    Sample sample = infoProvider.getSampleForTraitInstance(pos, traitInstance);
		    if (sample != null) {
		        if (sample.hasBeenScored()) {
		            String tv = sample.getTraitValue();
		            if (tv != null) {
		                int idx = uniqueValues.indexOf(tv);
		                if (idx >= 0) {
		                    result = idx;
		                }
		            }
		        }
		    }
			return result;
		}
		
      @Override
        public boolean supportsGetAxisValue() {
            return false;
        }
		
		@Override
		public int getAxisZeroValue() {
			return 0;
		}
		
        @Override
        public int getAxisValueCount() {
            return uniqueValues.size();
        }
	}

}
