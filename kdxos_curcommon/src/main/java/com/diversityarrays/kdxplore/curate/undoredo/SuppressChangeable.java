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
package com.diversityarrays.kdxplore.curate.undoredo;

import com.diversityarrays.kdxplore.curate.EditedSampleInfo;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

/**
 * This Changeable is for when the suppressed state of a KdxSample changes.
 * @author alexs
 *
 */
public class SuppressChangeable implements Changeable<PlotAndSampleChanger> {

	private final EditedSampleInfo info;
	private final String oldReason;
	private final String newReason;

	public SuppressChangeable(EditedSampleInfo info,
			String oldReason,
			String newReason)
	{
		this.info = info;
		this.oldReason = oldReason;
		this.newReason = newReason;
	}

	@Override
	public Object getOldValue() {
		return oldReason;
	}

	@Override
	public Object getNewValue() {
		return newReason;
	}

	@Override
	public void redo(PlotAndSampleChanger source) throws Exception {
		// need to have an old sample
		KdxSample newSample = info.newEditedSample;
		newSample.setSuppressedReason(newReason);
		source.setEditedSampleValue(info.ccid, newSample);
	}

	@Override
	public void undo(PlotAndSampleChanger source) throws Exception {
		// need to have an old sample
		KdxSample oldSample = info.oldEditedSample;
		if (oldSample == null) {
			// 
			source.setEditedSampleValue(info.ccid, null);
		}
		else {
			oldSample.setSuppressedReason(oldReason);
			source.setEditedSampleValue(info.ccid, oldSample);
		}
	}

	@Override
	public String getInfo() {
		StringBuilder sb = new StringBuilder();
		if (newReason == null) {
			sb.append("Unsuppress Sample");
		}
		else {
			sb.append("Suppress Sample (").append(newReason).append(")");
		}
		
		if (oldReason != null) {
			sb.append(", previously: ").append(oldReason);
		}
		return sb.toString();
	}

}
