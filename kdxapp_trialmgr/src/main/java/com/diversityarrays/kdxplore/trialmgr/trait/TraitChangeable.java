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
package com.diversityarrays.kdxplore.trialmgr.trait;

import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdxplore.curate.undoredo.Changeable;

public class TraitChangeable implements Changeable<Trait> {

	private Trait oldValue;
	private Trait newValue;
		
	private final String info;
	
	private final KDSmartDatabase database;
	
	public TraitChangeable(Trait oldValue, Trait newValue, KDSmartDatabase db) {
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.database = db;
		
		StringBuilder sb = new StringBuilder();
		sb.append(oldValue.getTraitName() + " to " + newValue.getTraitName());
		info = sb.toString();
	}
	
	@Override
	public void redo(Trait changer) throws Exception {
		if (null != newValue) {
			database.saveTrait(newValue, true);
		}
	}

	@Override
	public void undo(Trait changer) throws Exception {
		if (null != oldValue) {
			database.saveTrait(oldValue, true);
		}
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public Object getOldValue() {
		return oldValue == null 
				? null 
				: oldValue;
	}

	@Override
	public Object getNewValue() {
		Object result = null;
		if (newValue != null) {
			result = newValue;
		}
		return result;
	}
	
}
