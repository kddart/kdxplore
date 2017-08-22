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
package com.diversityarrays.kdxplore.curate;

import java.util.Map;
import java.util.TreeMap;

/**
 * Provides a Subscriber/Publisher pattern for tracking the selected
 * TraitInstances/Plots in the various VisToolPanel-s that are created by
 * VisualisationTools.
 * <p>
 * The VisToolPanels register as subscribers.
 */
@SuppressWarnings("nls")
public class SelectedValueStore {

	static private final String SYNC = "SYNC";
	
	private final Map<String,PlotsByTraitInstance> selectedPlotsForTraitsBySource = new TreeMap<>();
	
	private boolean syncAll = true;
	
	private final String name;
	public SelectedValueStore(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SelectedValueStore[");
		sb.append(name).append("] syncAll=").append(syncAll).append(" sources");
		char ch = '=';
		for (String source : selectedPlotsForTraitsBySource.keySet()) {
			sb.append(ch)
				.append(source);
			ch = ',';
		}
		return sb.toString(); // for debugging
	}
	
	public boolean isSyncAll() {
		return syncAll;
	}

	public PlotsByTraitInstance getSelectedPlotsForToolId(String source) {
		PlotsByTraitInstance result = selectedPlotsForTraitsBySource.get(source);
		if (result==null) {
			result = PlotsByTraitInstance.EMPTY;
		}
		return result;
	}

	public PlotsByTraitInstance getSyncedPlotsByTrait() {
		return getSelectedPlotsForToolId(SYNC);
	}

	public void setSelectedPlots(String source, PlotsByTraitInstance plotsByInstance) {
		updateStore(source, plotsByInstance);
		if (syncAll) {
			updateStore(SYNC, plotsByInstance);
		}		
//		if (FIRE) {
//			fireSelectionChangedExceptFor(source);
//		}
	}
	
	private void updateStore(String source, PlotsByTraitInstance plotsByInstance) {
		if (plotsByInstance == null) {
			selectedPlotsForTraitsBySource.remove(source);
		}
		else {
			selectedPlotsForTraitsBySource.put(source, plotsByInstance);
		}
	}
	
//	private boolean FIRE = false;
//	
//	private EventListenerList listenerList = new EventListenerList();
//	
//	public void addSelectedValueChangeListener(SelectedValueChangeListener_NOT l) {
//		listenerList.add(SelectedValueChangeListener_NOT.class, l);
//	}
//	
//	public void removeSelectedValueChangeListener(SelectedValueChangeListener_NOT l) {
//		listenerList.remove(SelectedValueChangeListener_NOT.class, l);
//	}
//	
//	private void fireSelectionChangedExceptFor(String exceptFor) {
//		for (SelectedValueChangeListener_NOT l : listenerList.getListeners(SelectedValueChangeListener_NOT.class)) {
//			if (! l.getListenerName().equals(exceptFor)) {
//				l.selectedValuesChanged(this);
//			}
//		}
//	}

}
