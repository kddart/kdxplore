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
package com.diversityarrays.kdxplore.vistool;

import javax.swing.JComboBox;

/**
 * Provide standard behaviour for "Synced" option.
 * @author brian
 *
 */
public class SyncWhatOption extends JComboBox<SyncWhat> {

	public SyncWhatOption() {
		this(SyncWhat.SYNC_ALL);
	}
	
	public SyncWhatOption(SyncWhat defaultValue) {
		super(SyncWhat.values());
		
		setSelectedItem(defaultValue==null ? SyncWhat.SYNC_ALL : defaultValue);
		
		super.setEditable(false);
	}
	
	@Override
	public void setEditable(boolean b) {
		super.setEditable(false);
	}

	public SyncWhat getSyncWhat() {
		return (SyncWhat) getSelectedItem();
	}

	public void setSyncWhat(SyncWhat syncWhat) {
		setSelectedItem(syncWhat);
	}
	
}
