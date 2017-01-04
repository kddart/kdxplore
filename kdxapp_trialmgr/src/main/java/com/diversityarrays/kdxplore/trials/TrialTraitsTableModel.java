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
package com.diversityarrays.kdxplore.trials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.model.TraitAttribute;
import com.diversityarrays.kdxplore.model.TraitAttribute.SortOrderAttribute;
import com.diversityarrays.kdxplore.model.TraitAttribute.ValidationRuleAttribute;
import com.diversityarrays.util.RunMode;

public class TrialTraitsTableModel extends AbstractTableModel {
    
    private final List<TraitAttribute> traitAttributes = new ArrayList<>();

    public final List<Trait> traitList = new ArrayList<>();
	private final Map<Trial, Map<Trait, Integer>> traitAndSsoByTrial = new HashMap<>();

	private boolean hasMultipleTraitLevels = false;
	private boolean hasMultipleSso = false;
	private boolean showSortOrder = false;

	private Trial selectedTrial;

	private final ValidationRuleAttribute validationRuleAttribute = new ValidationRuleAttribute();
    private final SortOrderAttribute sortOrderAttribute = new SortOrderAttribute();
	
	public TrialTraitsTableModel() {
		super();
		
		traitAttributes.clear();

        traitAttributes.add(TraitAttribute.TRAIT_NAME);
        traitAttributes.add(TraitAttribute.TRAIT_DATA_TYPE);
        traitAttributes.add(TraitAttribute.DESCRIPTION);
        traitAttributes.add(TraitAttribute.TRAIT_UNIT);
        traitAttributes.add(validationRuleAttribute);
        
        if (showSortOrder) {
            traitAttributes.add(0, sortOrderAttribute);
        }

		if (RunMode.getRunMode().isDeveloper()) {
	        traitAttributes.add(TraitAttribute.WHEN_DOWNLOADED);
		}
	}
	
	public boolean getHasMultipleScoringSortOrders() {
		return hasMultipleSso;
	}
	
	public boolean getShowSortOrder() {
		return showSortOrder;
	}

	public List<Trait> getTraitList() {
		return Collections.unmodifiableList(this.traitList);
	}
	
	public  Map<Trait, Integer> getTraitsSsoForTrial(Trial trial) {
		Map<Trait, Integer> map = this.traitAndSsoByTrial.get(trial);
		return map!=null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
	}
	
	public void setShowSortOrder(boolean b) {
		boolean old = showSortOrder;
		showSortOrder = b;
		if (showSortOrder != old) {
		    if (showSortOrder) {
		        traitAttributes.add(0, sortOrderAttribute);
		    }
		    else {
		        traitAttributes.remove(sortOrderAttribute);
		    }
			fireTableStructureChanged();
		}
	}
	
	@Override
	public int getRowCount() {
		return traitList.size();
	}

	@Override
	public String getColumnName(int column) {
	    if (column < 0 || column >= traitAttributes.size()) {
	        return super.getColumnName(column);
	    }
	    return traitAttributes.get(column).getAttributeName();
	}
	

	
	@Override
	public int getColumnCount() {
	    return traitAttributes.size();
	}

	@Override
	public Class<?> getColumnClass(int column) {
        if (column < 0 || column >= traitAttributes.size()) {
            return Object.class;
        }
        return traitAttributes.get(column).getAttributeClass();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex < 0 || columnIndex >= traitAttributes.size()) {
            return Object.class;
        }
        Trait trait = getTraitAt(rowIndex);
        return traitAttributes.get(columnIndex).getAttributeValue(trait);
	}

	public Trait getTraitAt(int rowIndex) {
	    if  (rowIndex < 0 || rowIndex >= traitList.size()) {
	        return null;
	    }
		return traitList.get(rowIndex);
	}

	// =====
	
	public void traitChanged(Trait trait) {
		int rowIndex = traitList.indexOf(trait);
		if (rowIndex >= 0) {
			fireTableRowsUpdated(rowIndex, rowIndex);
		}
	}

	public void setTraitsByTrial(Map<Trial, Map<Trait, Integer>> map) {
	    this.validationRuleAttribute.clear();
		this.traitAndSsoByTrial.clear();			
		this.traitAndSsoByTrial.putAll(map);

		// Use the standard logic
        setSelectedTrial(selectedTrial);
	}

	public Trial getSelectedTrial() {
		return selectedTrial;
	}

	public void setSelectedTrial(Trial t) {

		boolean oldHasMultipleSso = hasMultipleSso;

		selectedTrial = t;

		this.traitList.clear();
		
		sortOrderAttribute.setSortOrderByTrait(null);

		boolean multipleLevels = false;
		if (selectedTrial != null) {
	        Map<Trait,Integer> traitAndSso = traitAndSsoByTrial.get(selectedTrial);
	        sortOrderAttribute.setSortOrderByTrait(traitAndSso);

	        if (traitAndSso != null) {
				Set<Integer> ssos = new HashSet<>(traitAndSso.values());
				hasMultipleSso = ssos.size() > 1;

				this.traitList.addAll(traitAndSso.keySet());
				Collections.sort(traitList, new Comparator<Trait>() {
					@Override
					public int compare(Trait t1, Trait t2) {
						Integer i1 = traitAndSso.get(t1);
						Integer i2 = traitAndSso.get(t2);
						return i1.compareTo(i2);
					}
				});

				Set<TraitLevel> set = traitAndSso.keySet().stream().map(Trait::getTraitLevel).collect(Collectors.toSet());
				multipleLevels = set.size() > 1;
			}
		}
		
		boolean structureChanged = false;
		if (oldHasMultipleSso != hasMultipleSso) {
		    structureChanged = true;
		}

		// If multiplicity of TraitDataTypes changed ...
		if (multipleLevels != hasMultipleTraitLevels) {
            structureChanged = true;

            hasMultipleTraitLevels = multipleLevels;
		    if (hasMultipleTraitLevels) {
		        int nameIndex = traitAttributes.indexOf(TraitAttribute.TRAIT_NAME);
		        traitAttributes.add(nameIndex + 1, TraitAttribute.TRAIT_LEVEL);
		    }
		    else {
		        traitAttributes.remove(TraitAttribute.TRAIT_LEVEL);
		    }
		}

		if (structureChanged) {
			fireTableStructureChanged();
		}
		else {
			fireTableDataChanged();
		}
	}
}
