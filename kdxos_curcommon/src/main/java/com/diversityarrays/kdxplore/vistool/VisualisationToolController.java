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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SelectionChangeListener;

/**
 * Provides a "go-between" for clients and all the VisualisationTools.
 * One of its responsibilities is to monitor the tools for selectionChanges
 * (they fire a ChangeEvent when that happens) and 
 * @author brianp
 *
 */
public class VisualisationToolController {
	
	final private VisualisationTool[] tools;
	
	private final Map<String,VisToolPanel> visToolPanelById = new HashMap<>();
	
	private final ChangeListener vtpSelectionChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			VisToolPanel vtp = (VisToolPanel) e.getSource();
			String toolPanelId = vtp.getToolPanelId();
			fireSelectionChanged(toolPanelId);
		}
	};
	
	private final Map<VisualisationToolId<?>,List<JFrame>> framesByToolId = new HashMap<>();

	private final SelectedValueStore selectedValueStore;
	
	private final VisToolListener visToolListener = new VisToolListener() {
		@Override
		public void visToolPanelCreated(Object visTool, JFrame frame, VisToolPanel visToolPanel) {
			try {
				String toolPanelId = visToolPanel.getToolPanelId();
				if (! visToolPanelById.containsKey(toolPanelId)) {
					visToolPanelById.put(toolPanelId, visToolPanel);
					
					visToolPanel.addSelectionChangeListener(vtpSelectionChangeListener);
				}
			}
			finally {
				if (visTool instanceof VisualisationTool) {
					VisualisationTool visualisationTool = (VisualisationTool) visTool;
					VisualisationToolId<?> toolId = visualisationTool.getVisualisationToolId();
					List<JFrame> list = framesByToolId.get(toolId);
					if (list == null) {
						list = new ArrayList<>();
						framesByToolId.put(toolId, list);
					}
					list.add(frame);
				}
			}
		}

		@Override
		public void visToolPanelClosed(Object visTool, JFrame frame, VisToolPanel visToolPanel) {
			String toolPanelId = visToolPanel.getToolPanelId();
			
			try {
				if (visToolPanel == visToolPanelById.remove(toolPanelId)) {
					visToolPanel.removeSelectionChangeListener(vtpSelectionChangeListener);
					// do NOT change the selection when a tool closes
					//				fireSelectionChanged(toolPanelId);
				}
			}
			finally {
			    selectedValueStore.setSelectedPlots(toolPanelId, null);
				
				if (visTool instanceof VisualisationTool) {
					VisualisationTool visualisationTool = (VisualisationTool) visTool;
					VisualisationToolId<?> toolId = visualisationTool.getVisualisationToolId();
					List<JFrame> list = framesByToolId.get(toolId);
					if (list != null) {
						list.remove(frame);
						if (list.isEmpty()) {
							framesByToolId.remove(toolId);
						}
					}
				}
			}
		}
	};
	
	private final EventListenerList listenerList = new EventListenerList();
	
	public VisualisationToolController(SelectedValueStore svs, VisualisationTool[] tools) {
		this.selectedValueStore = svs;
		this.tools = tools;
		
		for (VisualisationTool tool : tools) {
			tool.addVisToolListener(visToolListener);
		}
	}

	private void fireSelectionChanged(String toolPanelId) {
		for (SelectionChangeListener l : listenerList.getListeners(SelectionChangeListener.class)) {
			l.selectionChanged(toolPanelId);
		}
	}

	public List<VisualisationTool> getTools() {
		return Arrays.asList(tools);
	}
	
	public void addSelectionChangeListener(SelectionChangeListener l) {
		listenerList.add(SelectionChangeListener.class, l);
	}

	public void removeSelectionChangeListener(SelectionChangeListener l) {
		listenerList.remove(SelectionChangeListener.class, l);
	}

	/**
	 * Refreshing the samples selected in tools 
	 * except for the one specified by the toolId
	 * @param toolId
	 */
	public void updateSelectedSamplesExceptFor(String toolId) {
		for (VisToolPanel vtp : visToolPanelById.values()) {
			if (! vtp.getToolPanelId().equals(toolId)) {
				if (! vtp.getSelectionChanging() && vtp.getSyncWhat().isSync()) {
					vtp.updateSelectedSamples();
				}
			}
		}
	}

	public void plotActivationsChanged(boolean activated, List<Plot> plots) {
		for (VisToolPanel vtp : visToolPanelById.values()) {
			vtp.plotActivationsChanged(activated, plots);
		}
	}

	public void editedSamplesChanged() {
		for (VisToolPanel vtp : visToolPanelById.values()) {
			vtp.editedSamplesChanged();
		}
	}

}
