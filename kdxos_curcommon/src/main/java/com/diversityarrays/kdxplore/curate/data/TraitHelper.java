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
package com.diversityarrays.kdxplore.curate.data;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRule.Calculated;
import com.diversityarrays.daldb.ValidationRule.Range;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.stats.DoubleTraitValidationProcessor;
import com.diversityarrays.kdxplore.stats.ElapsedDaysTraitValidationProcessor;
import com.diversityarrays.kdxplore.stats.TraitValidationProcessor;
import com.diversityarrays.kdxplore.stats.TraitValidationProcessorAlwaysValid;
import com.diversityarrays.util.Either;


/**
 * Provides some helper methods to make it easier to work with Traits and DeviceSamples. One of the
 * problems is that the Trait.TraitDataType field is just text and so people using the database can put
 * anything in there. I have seen, variously, INT, INTEGER, NUMBER, DECIMAL, VARCHAR, STRING, TEXT.
 * But there could be anything - including language variants.
 * <p>
 * The <code>getTraitValueClass(aTrait)</code> looks for the ones I've seen and returns Integer.class,
 * Double.class or String.class. All of them "must" be Comparable as that is what makes other code work.
 * So even "categorical" data will be sortable - after a fashion.
 * @author brian
 *
 */
public class TraitHelper {
	
	static public final Comparator<TraitInstance> COMPARATOR = new Comparator<TraitInstance>() {

		@Override
		public int compare(TraitInstance o1, TraitInstance o2) {
			int diff = 0;
			if (o1.trait != null && o2.trait != null) {
				diff = o1.trait.getTraitName().compareToIgnoreCase(o2.trait.getTraitName());
			}
			else {
				diff = Integer.compare(o1.getScoringSortOrder(), o2.getScoringSortOrder());
			}
			
			if (diff == 0) {
				diff = Integer.compare(o1.getInstanceNumber(), o2.getInstanceNumber());
			}
			return diff;
		}
	};
	
	private static final String TAG = TraitHelper.class.getSimpleName();

	/**
	 * Base class for getting the values from a DeviceSample.
	 * @author brian
	 *
	 * @param <T>
	 */
	static abstract public class ValueFactory<T extends Comparable<?>> {

		public final Class<T> valueClass;
		public final Trait trait;

		ValueFactory(Class<T> valueClass, Trait trait) {
			this.valueClass = valueClass;
			this.trait = trait;
		}
		
		public abstract T getTraitValue(KdxSample sm);
	}
	
	/**
	 * ValueFactory which create String values.
	 * @author brian
	 *
	 */
	static public class StringValueFactory extends ValueFactory<String> {

		public StringValueFactory(Trait trait) {
			super(String.class, trait);
		}

		@Override
		public String getTraitValue(KdxSample sm) {
			return sm==null ? null : sm.getTraitValue();
		}
		
	}
	
	static public class DateValueFactory extends ValueFactory<java.util.Date> {

		DateValueFactory(Trait trait) {
			super(java.util.Date.class, trait);
		}

		@Override
		public Date getTraitValue(KdxSample sm) {
			Date result = null;
			try {
				result = TraitValue.getTraitValueDateFormat().parse(sm.getTraitValue());
			} catch (ParseException e) {
			}
			return result;
		}
		
	}
	
	static public class ElapsedDaysValueFactory extends ValueFactory<Integer> {

//		private final Date plantingDate;
//		private final DateFormat dateFormat;
		
		ElapsedDaysValueFactory(Trait trait /*, Date plantingDate */) {
			super(Integer.class, trait);
//			this.plantingDate = plantingDate;
//			this.dateFormat = TraitValue.getTraitValueDateFormat();
		}

		@Override
		public Integer getTraitValue(KdxSample sm) {
			Integer result = null;
			
			if (sm != null && sm.hasBeenScored()) {
				String traitValue = sm.getTraitValue();
				if (traitValue != null && ! traitValue.isEmpty()) {
					try {
						// TAG_ELAPSED_DAYS_AS_INTEGER
						result = Integer.valueOf(traitValue);
//						Date date = dateFormat.parse(traitValue);
//						result = DateDiffChoice.differenceInDays(plantingDate, date);
					} catch (NumberFormatException e) {
						// invalid integer
					}
				}
			}
			
			return result;
		}
		
	}
	
	/**
	 * ValueFactory which create Integer values.
	 * @author brian
	 *
	 */
	static public class IntegerValueFactory extends ValueFactory<Integer> {

		public IntegerValueFactory(Trait trait) {
			super(Integer.class, trait);
		}

		@Override
		public Integer getTraitValue(KdxSample sm) {
			Integer result = null;
			if (sm != null) {
				try {
					result = new Integer(sm.getTraitValue());
				} catch (NumberFormatException ignore) {
					// Non-integer ignore
				}
			}
			return result;
		}
		
	}
	
	/**
	 * ValueFactory which create Double values.
	 * @author brian
	 *
	 */
	static public class DoubleValueFactory extends ValueFactory<Double> {

		public DoubleValueFactory(Trait trait) {
			super(Double.class, trait);
		}

		@Override
		public Double getTraitValue(KdxSample sm) {
			Double result = null;
			if (sm != null) {
				try {
					result = new Double(sm.getTraitValue());
				} catch (NumberFormatException | NullPointerException e) {
					// Non-integer ignore
					// Alex - I added a null value ignore for empty trait values..
				}
			}
			return result;
		}
	}
	
	/**
	 * Facilitates the calculation of averages from observations.
	 * It caters for DeviceSample and Numbers.
	 * @author brian
	 *
	 */
	static public class AverageValueFactory {
		
		protected int count;
		protected double sum;
		private final String name;
		private NumberFormat numberFormat;
		
		public  AverageValueFactory(String name, NumberFormat nf) { 
			this.name = name;
			this.numberFormat = nf;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		public void initialise() {
			count = 0;
			sum = 0;
		}

		protected void addObservation(double d) {
			++count;
			sum += d;
		}
		
		public void addObject(Object o) {
			if (o instanceof Number) {
				addObservation(((Number) o).doubleValue());
			} else if (o instanceof KdxSample) {
				KdxSample sm = (KdxSample) o;
				try {
					addObservation(Double.parseDouble(sm.getTraitValue()));
				} catch (NumberFormatException e) {
					// Ignore invalid values - don't even increment count
				}
			} else {
				// Ignore everything else
			}
		}
		
		public String getAverageValue() {
			if (count <= 0) {
				return null;
			}
			return numberFormat.format( sum / count );
		}
	}
	
	/**
	 * Return a ValueFactory appropriate for the TraitDataType of the given Trait.
	 * @param trait
	 * @return
	 */
	static public ValueFactory<?> getValueFactory(Trait trait /*, Date trialPlantingDate */) {
		
		ValueFactory<?> result = null;

		TraitDataType tdt = trait.getTraitDataType();
		switch (tdt) {
		case DATE:
			result = new DateValueFactory(trait);
			break;
		case DECIMAL:
			result = new DoubleValueFactory(trait);
			break;
		case ELAPSED_DAYS:
			result = new ElapsedDaysValueFactory(trait /*, trialPlantingDate */);
			break;
		case INTEGER:
			result = new IntegerValueFactory(trait);
			break;
			
		case CALC:
			Either<Throwable, Calculated> either = createCalculated(trait.getTraitValRule());
            if (either.isRight()) {
                Calculated calc = either.right();
                Range range = calc.getRangeCheckRule();
                if (range.isIntegralRange()) {
                    result = new IntegerValueFactory(trait);
                }
                else {
                    result = new DoubleValueFactory(trait);
                }
            }
            else {
                 Shared.Log.w(TAG,
                            String.format("getValueFactory( %s )", trait.getTraitName()), //$NON-NLS-1$
                            either.left());
                    result = new StringValueFactory(trait);

            }
			break;

		case CATEGORICAL:
		case TEXT:
		default:
			result = new StringValueFactory(trait);
			break;
		
		}
		
		return result;
	}
	

	static public Either<Throwable, Calculated> createCalculated(String traitValRule) {
        try {
            ValidationRule vrule = ValidationRule.create(traitValRule);
            if (vrule instanceof Calculated) {
                Calculated calc = (Calculated) vrule;
                return Either.right(calc);
            }
            String msg = MessageFormat.format("Not Calculated[ {0}] : {1} ]", traitValRule, vrule.getClass().getName());
            return Either.left(new IllegalArgumentException(msg));
        }
        catch (InvalidRuleException e) {
            return Either.left(e);
        }

	}
	
	/**
	 * Return an AverageValueFactory appropriate for the trait's getTraitDatatype().
	 * @param trait
	 * @param averageValue 
	 * @return AverageValueFactory
	 */
	static public AverageValueFactory getAverageValueFactory(Trait trait, String avfName) {
		AverageValueFactory result = null;
		
		TraitDataType tdt = trait.getTraitDataType();
		switch (tdt) {
		case DECIMAL:
			result = new AverageValueFactory(avfName, new DecimalFormat("#.###"));
			break;
		case ELAPSED_DAYS:
		case INTEGER:
			result = new AverageValueFactory(avfName, new DecimalFormat("#"));
			break;
			
		case CALC:
			Either<Throwable, Calculated> either = createCalculated(trait.getTraitValRule());
            if (either.isRight()) {
                Calculated calc = either.right();
                Range range = calc.getRangeCheckRule();
                if (range.isIntegralRange()) {
                    result = new AverageValueFactory(avfName, new DecimalFormat("#"));
                }
                else {
                    result = new AverageValueFactory(avfName, new DecimalFormat("#.###"));
                }
                
            }
            else {
                Shared.Log.w(TAG, 
                        MessageFormat.format("getAverageValueFactory( {0} , {1} )", trait.getTraitName(), avfName),
                        either.left());
            }
			break;

		case DATE:
			break;
		case CATEGORICAL:
			break;
		case TEXT:
			break;
		default:
			break;
		}
		return result;
	}

	
	public static TraitValidationProcessor<?> createTraitValidationProcessor(Trait trait) {
		TraitValidationProcessor<?> result;
		try {
			ValidationRule rule = null;
			switch (trait.getTraitDataType()) {
			
			case CALC:
			case INTEGER:
			case DECIMAL:
				rule = ValidationRule.create(trait.getTraitValRule());
				result = new DoubleTraitValidationProcessor(rule);
				break;
				
			case ELAPSED_DAYS:
				String tvr = trait.getTraitValRule();
				if (tvr != null && ! tvr.isEmpty()) {
					rule = ValidationRule.create(trait.getTraitValRule());
//					rule = ValidationRule.createForChecking(trait.getTraitValRule(), true);
				}
				result = new ElapsedDaysTraitValidationProcessor(rule /*, trialPlantingDate */);
				break;
				
			case DATE:
			case CATEGORICAL:
			case TEXT:
			default:
				result = new TraitValidationProcessorAlwaysValid();
				break;
			}
		} catch (InvalidRuleException e) {
			result = new TraitValidationProcessorAlwaysValid();
		}

		return result;
	}



	public static Class<? extends Comparable<?>> getTraitDataTypeValueClass(Trait trait) {
		Class<? extends Comparable<?>> xClass;
		switch (trait.getTraitDataType()) {
		case DATE:
			xClass = java.util.Date.class;
			break;
		case DECIMAL:
			xClass = Double.class;
			break;
		case ELAPSED_DAYS:
		case INTEGER:
			xClass = Integer.class;
			break;
		case CALC:
			Either<Throwable, Calculated> either = createCalculated(trait.getTraitValRule());
            if (either.isRight()) {
                Calculated calc = either.right();
                Range range = calc.getRangeCheckRule();
                if (range.isIntegralRange()) {
                    xClass = Integer.class;
                }
                else {
                    xClass = Double.class;
                }
            }
            else {
                xClass = String.class;
                Shared.Log.w(TAG, 
                        MessageFormat.format("getTraitDataTypeValueClass( {0} )", trait.getTraitName()), 
                        either.left());
                
            }
			break;
		case CATEGORICAL:
		case TEXT:
		default:
			xClass = String.class;
			break;
		
		}
		return xClass;
	}
	
//	/**
//	 * Return the TraitValueType depending value of getTraitDatatype()
//	 * or null if no Trait supplied or the TraitDataType is not one we know about.
//	 * @param trait
//	 * @return null if no Trait or the TraitDataType is not recognised
//	 */
//	static public TraitDataType getTraitDataType(Trait trait) {
//		if (trait==null) {
//			return null;
//		}
//		return TraitDataType.lookup(trait.getTraitDatatype());
//	}
}
