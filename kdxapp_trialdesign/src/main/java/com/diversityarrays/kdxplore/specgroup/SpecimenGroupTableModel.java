/**
 * 
 */
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
package com.diversityarrays.kdxplore.specgroup;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.daldb.core.Specimen;
import com.diversityarrays.daldb.core.SpecimenGroup;

/**
 * @author alexs
 *
 */
class SpecimenGroupTableModel extends AbstractTableModel {

	List<SpecimenGroup> groups = new ArrayList<SpecimenGroup>();
	Map<SpecimenGroup,List<Specimen>> specimensByGroup = new HashMap<>();
	
	private final static String[] COL_NAMES = {"Group Name", "# Specimens", "Status", "Created", "Updated"};
	
	@Override
	public int getRowCount() {
		return groups.size();
	}

	@Override
	public int getColumnCount() {
		return COL_NAMES.length;	
	}
	
	@Override
	public String getColumnName(int col) {
		return COL_NAMES[col];	
	}
	
	public void setData(Map<SpecimenGroup,List<Specimen>> map) {
		this.groups.clear();
		specimensByGroup.clear();
		
		groups.addAll(map.keySet());
		specimensByGroup.putAll(map);
		
		fireTableDataChanged();
	}
	
	public SpecimenGroup getSpecimenGroup(int rowIndex) {
		if (rowIndex < groups.size() && rowIndex > 0) {
			return groups.get(rowIndex);
		} else {
			return null;
		}
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		if (rowIndex < 0) {
			return null;
		}
		
		SpecimenGroup group = groups.get(rowIndex);
		
		switch(columnIndex) {
		case 0: return group.getSpecimenGroupName();
		case 1: 
		    List<Specimen> list = specimensByGroup.get(group);
		    return list==null ? 0 : list.size();
		case 2: return group.getSpecimenGroupStatus();
		case 3: return group.getSpecimenGroupCreated();
		case 4: return group.getSpecimenGroupLastUpdate();
		}
		
		return null;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {

		switch(columnIndex) {
		case 0: return String.class;
		case 1: return Integer.class;
		case 2: return String.class;
		case 3: return Date.class;
		case 4: return Date.class;
		}
		
		return null;
	}

    public SpecimenGroup getItemAt(int rowIndex) {
        return groups.get(rowIndex);
    }
    
    public List<Specimen> getSpecimensAt(int rowIndex) {
        List<Specimen> result = specimensByGroup.get(groups.get(rowIndex));
        if (result == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }

}
