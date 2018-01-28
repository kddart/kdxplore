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
package com.diversityarrays.kdxplore.editing;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdxplore.beans.DartEntityBeanRegistry;

public class TraitPropertiesTableModel extends EntityPropertiesTableModel<Trait> {
	
	static public TraitPropertiesTableModel create() {
		List<PropertyDescriptor> list  = new ArrayList<>();
		Collections.addAll(list, DartEntityBeanRegistry.TRAIT_BEAN_INFO.getPropertyDescriptors());
		for (int index = list.size(); --index >= 0; ) {
			String dn = list.get(index).getDisplayName();
			if ("Orig Trait Val Rule".equals(dn) || "Trait Id".equals(dn)) {
				list.remove(index);
			}
		}
		
		return new TraitPropertiesTableModel(list.toArray(new PropertyDescriptor[list.size()]));
	}

	private TraitPropertiesTableModel(PropertyDescriptor[] pds) {
		super(Trait.class, pds);
	}
	
	public Trait getTrait() {
		return getEntity();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		Trait trait = getEntity();
		if (TraitLevel.class == getColumnClass(columnIndex)) {
		    
		    return false;
		}
		if (trait.isProtected() && ! isTraitAlias(rowIndex)) {
		    return false;		    
		}
		return super.isCellEditable(rowIndex, columnIndex);
	}
	
	private boolean isTraitAlias(int row) {
		PropertyDescriptor pd = getPropertyDescriptor(row);
		return DartEntityBeanRegistry.TRAIT_ALIAS.equals(pd.getDisplayName());
	}
}
