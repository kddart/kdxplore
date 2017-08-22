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

import java.util.List;

/**
 * @author alexs
 *
 */
public class EditedValueChangeableGroup implements Changeable<PlotAndSampleChanger>{

	private List<Changeable<PlotAndSampleChanger>> changeables;
	
	public EditedValueChangeableGroup(List<Changeable<PlotAndSampleChanger>> changeables) {
		this.changeables =  changeables;
	}
	
	@Override
	public Object getOldValue() {
		StringBuilder sb = new StringBuilder();
		for (Changeable<PlotAndSampleChanger> changeable : changeables) {
			sb.append(',').append(changeable.getOldValue());
		}	
		return sb.substring(1);
	}


	@Override
	public Object getNewValue() {
		StringBuilder sb = new StringBuilder();
		for (Changeable<?> changeable : changeables) {
			sb.append(',').append(changeable.getNewValue());
		}	
		return sb.substring(1);
	}


	@Override
	public void redo(PlotAndSampleChanger source) throws Exception {
		for (Changeable<PlotAndSampleChanger> changeable : changeables) {
			changeable.redo(source);
		}		
	}


	@Override
	public void undo(PlotAndSampleChanger source) throws Exception {
		for (Changeable<PlotAndSampleChanger> changeable : changeables) {
			changeable.undo(source);
		}	
	}

	@Override
	public String getInfo() {
		StringBuilder sb = new StringBuilder();
		for (Changeable<?> changeable : changeables) {
			sb.append(',').append(changeable.getInfo());
		}	
		return sb.substring(1);
	}
	
}
