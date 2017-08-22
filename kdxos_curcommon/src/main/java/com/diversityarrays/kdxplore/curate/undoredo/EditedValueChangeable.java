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
package com.diversityarrays.kdxplore.curate.undoredo;

import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class EditedValueChangeable implements Changeable<PlotAndSampleChanger> {

	private KdxSample oldValue;
	private KdxSample newValue;
	
	private CurationCellId ccid;
	private final String info;
	
	public EditedValueChangeable(KdxSample oldValue, KdxSample newValue, CurationCellId ccid) {
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.ccid = ccid;
		
		StringBuilder sb = new StringBuilder("Plot#");
		sb.append(ccid.plotId);
		info = sb.toString();
	}
	
	@Override
	public void redo(PlotAndSampleChanger changer) throws Exception {
		changer.setEditedSampleValue(ccid, newValue);
	}

	@Override
	public void undo(PlotAndSampleChanger changer) throws Exception {
		changer.setEditedSampleValue(ccid, oldValue);
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public Object getOldValue() {
		return oldValue == null 
				? null 
				: oldValue.getTraitValue().toString();
	}

	@Override
	public Object getNewValue() {
		Object result = null;
		if (newValue != null) {
			result = newValue.getTraitValue();
		}
		return result;
	}
}
