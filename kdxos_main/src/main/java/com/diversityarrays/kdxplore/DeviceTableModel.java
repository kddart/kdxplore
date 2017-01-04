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
package com.diversityarrays.kdxplore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

public class DeviceTableModel extends AbstractTableModel {
    
    private final List<String> urls = new ArrayList<>();
	private Map<String, String> deviceTypeByUrl  = new HashMap<>();

	@Override
	public int getRowCount(){ 
		return urls.size();	
	}

	@Override
	public int getColumnCount(){
		return 3; 

	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 2;
	}
	
	@Override
	public Object getValueAt(int row, int column) {	
	    
		String url = urls.get(row);
		switch(column) {
		case 0:
			return deviceTypeByUrl.get(url);
		case 1:
			return url;
		}

		return null;     
	}
	@Override
	public String getColumnName(int column) {

		switch(column) {
		case 0:
			return "Device Type";
		case 1:
			return "Device Uri";
		case 2: 
			return "Device Name";
		}

		return super.getColumnName(column);
	}
	
	
	
	@Override
	public Class<?> getColumnClass(int col) {

		switch (col) {
		
		case 0: return String.class;
		case 1: return String.class;	

		}
		return Object.class;
	}

	public void setModelData(Map<String, String> map) {
	    urls.clear();
	    urls.addAll(map.keySet());
	    Collections.sort(urls);
		deviceTypeByUrl.clear();
		deviceTypeByUrl.putAll(map);
	}
	
	public Map<String, String> getTableData() {
		return this.deviceTypeByUrl;
	}

    public String getUrlAt(int row) {
        return urls.get(row);
    }
}
