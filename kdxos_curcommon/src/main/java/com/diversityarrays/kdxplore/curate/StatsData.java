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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.stats.StatsUtil;

public class StatsData implements Iterable<TraitInstance> {
	
	
	static private final Comparator<TraitInstance> TRAIT_INSTANCE_COMPARATOR = new Comparator<TraitInstance>() {
		@Override
		public int compare(TraitInstance o1, TraitInstance o2) {
			int diff = o1.getTraitName().compareTo(o2.getTraitName());
			if (diff == 0) {
				diff = o1.getInstanceNumber() - o2.getInstanceNumber();
			}
			return diff;
		}
	};
	
	private final Map<TraitInstance,InvalidRuleException> invalidRuleByTraitInstance = new HashMap<>();
	private final Map<TraitInstance,ValidationRule> validationRuleByTraitInstance = new HashMap<>();
	private final Map<TraitInstance,String> expressionByTraitInstance = new HashMap<>();
	private final Set<Integer> instanceNumbers = new HashSet<>();

	private final Map<TraitInstance,String> statsNameByTi = new HashMap<>();

	private final List<TraitInstance> traitInstances = new ArrayList<>();
	private final Map<TraitInstance,SimpleStatistics<?>> statsByTraitInstance = new HashMap<>();
	
	private final Map<TraitInstance, String> errorByTraitInstance = new HashMap<>();
	
	private final Integer nStdDevForOutlier;

	private final TraitNameStyle traitNameStyle;
	private final Date trialPlantingDate;

	public StatsData(Integer nStdDevForOutlier, Trial trial) {
		this.nStdDevForOutlier = nStdDevForOutlier;
		this.traitNameStyle = trial.getTraitNameStyle();
		this.trialPlantingDate = trial.getTrialPlantingDate();
	}
	
	public void setTraitInstances(
			List<TraitInstance> tiList,
			Transformer<TraitInstance,List<KdxSample>> sampleProvider)
	{
		traitInstances.clear();
		if (tiList != null) {
			traitInstances.addAll(tiList);
		}
		// FIXME optimise this by only updating the Stats whose traitInstances have changed
		statsNameByTi.clear();
		for (TraitInstance ti : traitInstances) {
			String traitName = ti.trait==null 
					? "Trait#" + ti.getTraitId() //$NON-NLS-1$
					: ti.trait.getAliasOrName();
			String statsName =  traitNameStyle.makeTraitInstanceName(traitName, ti.getInstanceNumber());
			statsNameByTi.put(ti, statsName);
		}
		
		instanceNumbers.clear();
		
		for (TraitInstance ti : traitInstances) {
			String statsName = statsNameByTi.get(ti);
			
			// Don't bother with "instance number" for CALC
			if (ti.trait == null || TraitDataType.CALC != ti.trait.getTraitDataType()) {
			    instanceNumbers.add(ti.getInstanceNumber());
			}
			
			if (TraitDataType.TEXT == ti.getTraitDataType()) {
				expressionByTraitInstance.put(ti, ""); //$NON-NLS-1$
			}
			else {
				try {
					ValidationRule rule = ValidationRule.create(ti.trait.getTraitValRule());
					validationRuleByTraitInstance.put(ti, rule);
					expressionByTraitInstance.put(ti, rule.getExpression());
				} catch (InvalidRuleException e) {
					invalidRuleByTraitInstance.put(ti, e);
				}
			}

			Trait trait = ti.trait;
			if (trait != null) {
				// TODO: make caller give all of the devices and we create a tab for each
				//       deviceType as well as for <null> (if there is more than one device)
				List<KdxSample> list = sampleProvider.transform(ti);
				
				SimpleStatistics<?> statistics =
						StatsUtil.createStatistics(statsName, 
								nStdDevForOutlier, 
								trialPlantingDate, 
								trait, 
								list);
				if (statistics != null) {
					statsByTraitInstance.put(ti, statistics);
				}
			}	
		}
		
		sortTraitInstances();
	}
	
	public InvalidRuleException getInvalidRuleByTraitInstance(TraitInstance ti) {
		return invalidRuleByTraitInstance.get(ti);
	}
	
	public List<TraitInstance> getTraitInstances() {
		return Collections.unmodifiableList(traitInstances);
	}
	
	public void sortTraitInstances() {
		Comparator<TraitInstance> comparator = new Comparator<TraitInstance>() {
			@Override
			public int compare(TraitInstance t1, TraitInstance t2) {
				SimpleStatistics<?> s1 = statsByTraitInstance.get(t1);
				SimpleStatistics<?> s2 = statsByTraitInstance.get(t2);

				Boolean has1 = s1!=null && (s1.getValidCount() > 0);
				Boolean has2 = s2!=null && (s2.getValidCount() > 0);
				
				// Those WITH come before those WITHOUT
				int diff = has2.compareTo(has1);
				
				if (diff == 0) {
					String n1 = statsNameByTi.get(t1);
					String n2 = statsNameByTi.get(t2);
					diff = n1.compareTo(n2);
				}
				return diff;
			}
		};
		Collections.sort(traitInstances, comparator);
	}

	public int getTraitInstanceCount() {
		return traitInstances.size();
	}

	public TraitInstance getTraitInstance(int rowIndex) {
		return traitInstances.get(rowIndex);
	}

	public SimpleStatistics<?> getStatistics(TraitInstance ti) {
		return statsByTraitInstance.get(ti);
	}
	
	public SimpleStatistics<?> getStatistics(int rowIndex) {
	    if (rowIndex < 0 || rowIndex >= traitInstances.size()) {
	        return null;
	    }
		TraitInstance ti = traitInstances.get(rowIndex);
		return statsByTraitInstance.get(ti);
	}

	public int indexOf(TraitInstance ti) {
		return traitInstances.indexOf(ti);
	}

	@Override
	public Iterator<TraitInstance> iterator() {
		return traitInstances.iterator();
	}

	/**
	 * Returns the TraitInstances that have data sorted by TraitName and InstanceNumber
	 * @return List
	 */
	public List<TraitInstance> getTraitInstancesWithData() {
		List<TraitInstance> result = new ArrayList<>();
		
		for (TraitInstance ti : traitInstances) {
			SimpleStatistics<?> stats = getStatistics(ti);
			if (stats != null && stats.getValidCount() > 0) {
				result.add(ti);
			}
		}
		
		Collections.sort(result, TRAIT_INSTANCE_COMPARATOR);
		
		return result;
	}

	public String getStatsName(TraitInstance ti) {
		return statsNameByTi.get(ti);
	}

	public Set<Integer> getInstanceNumbers() {
		return Collections.unmodifiableSet(instanceNumbers);
	}

	public int getInvalidRuleCount() {
		return invalidRuleByTraitInstance.size();
	}

	public ValidationRule getValidationRuleAt(int rowIndex) {
		TraitInstance ti = traitInstances.get(rowIndex);
		return validationRuleByTraitInstance.get(ti);
	}

	public String getValidationExpressionFor(TraitInstance ti) {
        return expressionByTraitInstance.get(ti);     
	}

	public String getValidationExpressionAt(int rowIndex) {
		return expressionByTraitInstance.get(traitInstances.get(rowIndex));		
	}


}
