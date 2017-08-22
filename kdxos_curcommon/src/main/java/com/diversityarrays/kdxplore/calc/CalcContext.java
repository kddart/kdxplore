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
package com.diversityarrays.kdxplore.calc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRule.Calculated;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Either;

import net.pearcan.util.StringUtil;

public class CalcContext {
	
	// Names in formulae should use the TraitInstance name
	// ... must modify Parsii to handle ':' formatted names
	
	private final Map<String,CalcTraitInstanceData> ctidataByName = new HashMap<>();
	
	private final Map<String,TraitInstance> calcInstanceByName = new HashMap<>();

	private final CalcContextDataProvider calcDataProvider;

    private final Map<TraitInstance,String> tiNameByTraitInstance = new HashMap<>();
    private final Map<String, TraitInstance> traitInstanceByLowcaseName = new HashMap<>();
	
    // TODO consider using Set<TraitInstance> if performance is an issue
	private final Map<TraitInstance, List<TraitInstance>> dependentsByCalcTraitInstance = new HashMap<>();

	private final Map<TraitInstance, Set<TraitInstance>> calcTraitInstancesByDependent = new HashMap<>();
//	private final Map<String, List<TraitInstance>> dependentsByCalcTraitInstanceId = new HashMap<>();

    public Set<TraitInstance> getDependents(List<TraitInstance> calcs) {
        Set<TraitInstance> result = new HashSet<>();
        for (TraitInstance calc : calcs) {
            List<TraitInstance> list = dependentsByCalcTraitInstance.get(calc);
            if (list != null) {
                result.addAll(list);
            }
        }
        return result;
    }

	
	public List<TraitInstance> getCalculatedForDependent(TraitInstance ti) {
	    List<TraitInstance> result = new ArrayList<>();
	    for (TraitInstance calc : dependentsByCalcTraitInstance.keySet()) {
	        if (dependentsByCalcTraitInstance.get(calc).contains(ti)) {
	            result.add(calc);
	        }
	    }
	    return result;
	}
	
	public Set<TraitInstance> getCalculatedForDependents(KdxSample sample) {
        Set<TraitInstance> result = new HashSet<>();
        
        String ident = InstanceIdentifierUtil.getInstanceIdentifier(sample);
        for (TraitInstance dep : calcTraitInstancesByDependent.keySet()) {
            if (InstanceIdentifierUtil.getInstanceIdentifier(dep).equals(ident)) {
                result.addAll(calcTraitInstancesByDependent.get(dep));
            }
        }

        return result;
	}
	
	public CalcContext(CalcContextDataProvider cdp) {
		this.calcDataProvider = cdp;
		
		TraitNameStyle traitNameStyle = calcDataProvider.getTrial().getTraitNameStyle();
      
        boolean anyCalcFound = false;
		for (TraitInstance ti : calcDataProvider.getTraitInstances()) {						
			String traitName = ti.trait==null 
					? "Trait#" + ti.getTraitId()
					: ti.trait.getAliasOrName();
					
			String tiName =  traitNameStyle.makeTraitInstanceName(traitName, ti.getInstanceNumber());
			tiNameByTraitInstance.put(ti, tiName);
			traitInstanceByLowcaseName.put(tiName.toLowerCase(), ti);
			
			if (ti.trait != null && TraitDataType.CALC==ti.trait.getTraitDataType()) {
                anyCalcFound = true;
			}
		}
		
//		for (TraitInstance ti : tiNameByTraitInstance.keySet()) {
//			Trait trait = ti.trait;
//			
//			if (trait != null && TraitDataType.CALC==ti.trait.getTraitDataType()) {
//				anyCalcFound = true;
//				break;
//			}
//		}
		
		if (anyCalcFound) {
			for (TraitInstance ti :  tiNameByTraitInstance.keySet()) {
				
				String tiName = tiNameByTraitInstance.get(ti);
				
				if (ti.trait != null) {
					if (TraitDataType.CALC == ti.trait.getTraitDataType()) {
						addCalcTraitInstance(ti, tiName);
					}
					else {
						CalcTraitInstanceData data = new CalcTraitInstanceData(ti, tiName);
						ctidataByName.put(data.name, data);
					}
				}
			}
		}
	}

    private void addCalcTraitInstance(TraitInstance calc, String calcName) {
        calcInstanceByName.put(calcName, calc);

        String rule_s = calc.trait.getTraitValRule();
        try {
            ValidationRule vrule = ValidationRule.create(rule_s);
            if  (vrule instanceof Calculated) {
                Calculated calculated = (Calculated) vrule;
                Set<String> variableNames = calculated.getVariableNames();
                
                List<TraitInstance> dependents = variableNames.stream()
                    .map(vname -> traitInstanceByLowcaseName.get(vname.toLowerCase()))
                    .filter(dep -> dep != null)
                    .collect(Collectors.toList());

                dependentsByCalcTraitInstance.put(calc, dependents);
                
                for (TraitInstance dep : dependents) {
                    Set<TraitInstance> set = calcTraitInstancesByDependent.get(dep);
                    if (set == null) {
                        set = new HashSet<>();
                        calcTraitInstancesByDependent.put(dep, set);
                    }
                    set.add(calc);
                }
            }
            else {
                // TODO fatal error
            }
        }
        catch (InvalidRuleException e) {
         // TODO fatal error
        }
    }
	
	private void collectTraitInstanceData(EvalContext context) {
		context.ctiDataFound.clear();
		for (String vname : context.calc.getVariableNames()) {
			CalcTraitInstanceData ctiData = ctidataByName.get(vname);
			if (ctiData == null) {
				context.missing.add(vname);
			}
			else {
			    ctiData.refreshData(calcDataProvider);
			    context.ctiDataFound.add(ctiData);
			}
		}
	}
	
	private boolean anyPlotAttributeValues(String attrName, Set<Integer> plotIds) {
		for (Integer plotId : plotIds) {
			Map<String, String> valueByName = calcDataProvider.getPlotAttributeValues(plotId);
			if (valueByName != null && valueByName.containsKey(attrName)) {
				return true;
			}
		}
		return false;
	}
	
	private void collectPlotAttributeNamesWanted(EvalContext context) {

		context.plotAttributeNamesWanted.clear();

		if (! context.missing.isEmpty()) {
			for (String name : context.missing) {
				if (anyPlotAttributeValues(name, context.plotIds)) {
					context.plotAttributeNamesWanted.add(name);
				}
			}
		}
	}
	
	private void collectTrialAttributeValuesByName(EvalContext context) {

		context.trialAttributeValuesByName.clear();

		if (! context.missing.isEmpty()) {
			for (TrialAttribute ta : calcDataProvider.getTrialAttributes()) {
				if (context.missing.contains(ta.getTrialAttributeName())) {
					try {
						Double vd = new Double(ta.getTrialAttributeValue());
						context.trialAttributeValuesByName.put(ta.getTrialAttributeName(), vd);
					} catch (NumberFormatException ignore) {
						// If it was required we'll catch the problem later ...
					}
				}
			}
		}
	}
	
	public void generateSamples(
			CalcSamplesConsumer samplesConsumer,
			Map<TraitInstance, String> errorByTraitInstance)
	{
		// TODO need a dependency graph and process from leaves to root if supporting CALC refer to CALC

		for (String statsName : calcInstanceByName.keySet()) {

			TraitInstance calcInstance = calcInstanceByName.get(statsName);

			try {
				ValidationRule rule = ValidationRule.create(calcInstance.trait.getTraitValRule());
				
				if (rule instanceof Calculated) {
					
					EvalContext evalContext = new EvalContext(calcInstance, (Calculated) rule);
					
					collectTraitInstanceData(evalContext);

					for (CalcTraitInstanceData ctiData : evalContext.ctiDataFound) {
						ctiData.addPlotIdsTo(evalContext.plotIds);
					}

					collectPlotAttributeNamesWanted(evalContext);
					evalContext.missing.removeAll(evalContext.plotAttributeNamesWanted);

					collectTrialAttributeValuesByName(evalContext);
					evalContext.missing.removeAll(evalContext.trialAttributeValuesByName.keySet());

					if (evalContext.missing.isEmpty()) {
						doOneCalcInstance(evalContext, samplesConsumer);
					}
					else {
						errorByTraitInstance.put(calcInstance, 
								StringUtil.join("Missing Traits: ", ",", evalContext.missing));
					}
				}
				else {
					errorByTraitInstance.put(calcInstance, "Not a CALC rule: " + rule.getValidationRuleType());
				}
			} catch (InvalidRuleException e) {
				errorByTraitInstance.put(calcInstance, "Invalid rule: " + e.getMessage());
			}
		}
	}
	
	class EvalContext {
		public final Set<String> missing = new HashSet<>();
		
		public final TraitInstance calcInstance;
		public final Calculated calc;
		
		public final List<CalcTraitInstanceData> ctiDataFound = new ArrayList<>();
		public final Set<Integer> plotIds = new HashSet<>();
		public final Set<String> plotAttributeNamesWanted = new HashSet<>();
		public final Map<String, Double> trialAttributeValuesByName = new HashMap<>();
		
		EvalContext(TraitInstance ti, Calculated c) {
			this.calcInstance = ti;
			this.calc = c;
		}
	}

	private void doOneCalcInstance(
			EvalContext context,
			CalcSamplesConsumer samplesConsumer) {
		// All the reference variables are there ...

		List<KdxSample> calcSamples = new ArrayList<>();

		Map<String,Double> valuesByName = new HashMap<>();

		Date now = new Date();
		for (Integer plotId : context.plotIds) {
			
			valuesByName.clear();
			
			Map<String, String> plotAttributeValueByName = 
					calcDataProvider.getPlotAttributeValues(plotId);
			
			boolean allFound = true;

			for (CalcTraitInstanceData ctiData : context.ctiDataFound) {
				KdxSample s = ctiData.getSample(plotId);
				if (s == null || ! s.hasBeenScored()) {
					allFound = false;
					break;
				}
				
				try {
					Double vd = new Double(s.getTraitValue());
					valuesByName.put(ctiData.name, vd);
				} catch (NumberFormatException e) {
					allFound = false;
					break;
				}
			}
			
			if (allFound && plotAttributeValueByName!=null) {
				for (String paName : context.plotAttributeNamesWanted) {
					String v = plotAttributeValueByName.get(paName);
					if (v == null) {
						allFound = false;
						break;
					}
					
					try {
						Double vd = new Double(v);
						valuesByName.put(paName, vd);
					} catch (NumberFormatException e) {
						allFound = false;
						break;
					}
				}
			}

			if (allFound) {
				valuesByName.putAll(context.trialAttributeValuesByName);

				Either<Set<String>, Double> either = context.calc.calculate(valuesByName);
				String traitValue;
				if (either.isRight()) {
					traitValue = context.calc.getDecimalFormat().format(either.right());
				}
				else {
					traitValue = TraitValue.VALUE_MISSING;
				}
				
				
				Plot plot = calcDataProvider.getPlotByPlotId(plotId);
                CurationCellId ccid = calcDataProvider.getCurationCellId(plot, context.calcInstance);
				
				KdxSample calcSample = calcDataProvider.createCalcSampleMeasurement(ccid, 
						traitValue, 
						now);

//				calcSample.setTraitValue(traitValue);
				
				calcSamples.add(calcSample);
			}
		}
		
		samplesConsumer.handleCalcSamplesChanged(context.calcInstance, calcSamples);
	}

}
