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
package com.diversityarrays.kdxplore.trialdesign;

import com.diversityarrays.kdcompute.db.Plugin;
import com.diversityarrays.kdcompute.designer.ColumnsTableModel;

class AlgorithmTableModel extends ColumnsTableModel<Plugin> {

	public AlgorithmTableModel() {
		super("Name", "Documentation URL");
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case 0: return String.class;
		case 1: return String.class;
		}
		return Object.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Plugin a = get(rowIndex);
		switch (columnIndex) {
		case 0: return a.getAlgorithmName();
		case 1: return a.getDocUrl();
		}
		return null;
	}
}
