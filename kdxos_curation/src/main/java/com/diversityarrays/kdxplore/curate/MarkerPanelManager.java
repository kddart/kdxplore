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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.pearcan.ui.marker.AbstractMarkerGroup;
import net.pearcan.ui.marker.MarkerGroup;
import net.pearcan.ui.marker.MarkerMouseClickHandler;
import net.pearcan.ui.marker.MarkerPanel;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.TitledTablePanelWithResizeControls;

public class MarkerPanelManager {

	private static final String TAG = MarkerPanelManager.class.getSimpleName();
	
	private boolean debugGetIndexName = Boolean.getBoolean("DEBUG_MARKER_GET_INDEX_NAME");	
	private boolean debugProposals = Boolean.getBoolean("DEBUG_MARKER_PROPOSALS");
	private boolean debugRightClick = Boolean.getBoolean("DEBUG_MARKER_RIGHT_CLICK");
	
	private class TableSelectionMarkerGroup extends AbstractMarkerGroup {

		TableSelectionMarkerGroup(JTable table, Color fill) {
			super(0 /* always before TraitInstances */, "Selection", fill, fill.darker(), USES_VIEW_INDICES);
			
			ListSelectionModel lsm = table.getSelectionModel();
	        
	        lsm.addListSelectionListener(new ListSelectionListener() {
	            @Override
	            public void valueChanged(ListSelectionEvent e) {
	                if (! e.getValueIsAdjusting()) {
	                    int[] rows = table.getSelectedRows();
	                    List<Integer> indices = new ArrayList<Integer>(rows.length);
	                    if (rows!=null) {
	                        for (int r : rows) {
	                            indices.add(r);
	                        }
	                    }
	                    setMarkers(indices);
	                }
	            }
	        });
		}
		
		@Override
	    public String getIndexName(Integer row) {
			String result = curationTableModel.getPlotName(row);
			return result;
		}
	}
	
	private class TraitInstanceMarkerGroup extends AbstractMarkerGroup {

		private final TraitInstance traitInstance;
		
		private Transformer<Integer,String> rowIndexToNameTransformer = new Transformer<Integer, String>() {
			@Override
			public String transform(Integer row) {
				String result = curationTableModel.getPlotName(row) + "[" + getGroupName() + "]";
				System.out.println("indexToName: " + result);
				return result;
			}
		};

		TraitInstanceMarkerGroup(TraitInstance ti, int displayOrder, String name, Color fillColor, Color borderColor) {
			super(displayOrder, name, fillColor, borderColor, USES_MODEL_INDICES);
			this.traitInstance = ti;
			
			setIndexNameTransformer(rowIndexToNameTransformer);
		}
		
		@Override
		public String toString() {
			return "TraitInstanceMarkerGroup: " + traitInstance.trait.getAliasOrName() + "_" + traitInstance.getInstanceNumber();
		}
		
		@Override
	    public String getIndexName(Integer row) {
			String result = "";
			if (getMarkers().contains(row)) {
				result = rowIndexToNameTransformer.transform(row);
			}
			if (debugGetIndexName) {
				System.out.println(getGroupName() + ".getIndexName=" + result);
			}
			return result;
		}
		
	}
	
    private final MarkerPanel markerPanel;
    
    private final List<MarkerGroup> markerGroups = new ArrayList<>();

    private final MarkerMouseClickHandler mmch = new MarkerMouseClickHandler() {
        
		@Override
        public void mouseClickedOn(MouseEvent me, MarkerGroup mg,
                List<Integer> indices, JScrollBar scrollBar,
                int proposedScrollBarValue)
        {
            String groupName = mg == null ? "<nogroup>" : mg.getGroupName();
            
            if (debugProposals) {
                StringBuilder sb = new StringBuilder(groupName);
                sb.append(" ").append(indices.size()).append(" indexes:");
                for (Integer i : indices) {
                    sb.append(' ').append(i);
                }
                sb.append(", propose=").append(proposedScrollBarValue);
                System.out.println(sb);
            }
            
            if (me.getClickCount()==1) {
            	if (SwingUtilities.isLeftMouseButton(me)) {
                	if (scrollBar!=null && proposedScrollBarValue>=0) {
                		scrollBar.setValue(proposedScrollBarValue);
                	}
            	}
            	else if (SwingUtilities.isRightMouseButton(me)) {
            		// perhaps support a right click menu
            		
            		if (debugRightClick) {
                		StringBuilder sb = new StringBuilder("mouseClicked: ");
                		if (indices.isEmpty()) {
                			sb.append("No markers");
                		}
                		else {
                			String sep = "";
                			if (indices.size() > 1) {
                				sb.append(indices.size()).append(" markers:");
                				sep = "\n";
                			}
                			
                			if (mg==null) {
                    			for (Integer row : indices) {
                    				sb.append(sep).append("row#").append(row);
                    				sep = "\n";
                    			}
                			}
                			else {
                    			for (Integer row : indices) {
                    				sb.append(sep).append(mg.getIndexName(row));
                    				sep = "\n";
                    			}
                			}
                		}
                		android.util.Log.d(TAG, sb.toString());
            		}
            	}
            }
        }
    };
    
    private final PropertyChangeListener traitInstancesPropertyChangeListener = new PropertyChangeListener() {		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (CurationTableModel.PROPERTY_TRAIT_INSTANCES.equals(evt.getPropertyName())) {
				
//				markerPanel.removeMarkerGroup(tableSelectionMarkerGroup);
				markerPanel.removeMarkerGroups(markerGroups);

				markerGroups.clear();
				
				List<TraitInstance> traitInstances = curationTableModel.getTraitInstances();
				if  (traitInstances.isEmpty()) {
					markerGroups.add(tableSelectionMarkerGroup);
				}
				else {
					int displayOrder = 1;
					for (TraitInstance ti : traitInstances) {
						++displayOrder;
						String groupName = traitNameStyle.makeTraitInstanceName(ti);
						Color fillColor = traitInstanceColorProvider.transform(ti);
						Color borderColor = fillColor.darker();
						TraitInstanceMarkerGroup timg = new TraitInstanceMarkerGroup(
								ti, 
								displayOrder, 
								groupName, 
								fillColor, 
								borderColor);
						
						markerGroups.add(timg);
					}
				}
				
				markerPanel.addMarkerGroups(markerGroups);
			}
		}
	};
	
	private final TraitNameStyle traitNameStyle;

	@SuppressWarnings("unused")
	private final JTable table;
	private final CurationTableModel curationTableModel;
	
	private final Transformer<TraitInstance,Color> traitInstanceColorProvider;
	
	private final CurationTableSelectionModel curationTableSelectionModel;
	private final ChangeListener selectionChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (curationTableSelectionModel == e.getSource()) {
				handleSelectionChanges();
			}			
		}
	};

	private final TableSelectionMarkerGroup tableSelectionMarkerGroup;

    public MarkerPanelManager(
    		Trial trial, 
    		CurationTableModel curationTableModel,  
    		JTable curationTable, 
    		TitledTablePanelWithResizeControls curationTablePanel,
    		Transformer<TraitInstance,Color> ticp,
    		CurationTableSelectionModel ctsm) 
    {
    	this.table = curationTable;
    	this.curationTableModel = curationTableModel;
    	this.traitInstanceColorProvider = ticp;
    	this.curationTableSelectionModel = ctsm;
    	
    	traitNameStyle = trial.getTraitNameStyle();
    	
		curationTablePanel.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
		markerPanel = new MarkerPanel(curationTablePanel.scrollPane, SwingConstants.VERTICAL);
        Component corner = curationTablePanel.scrollPane.getCorner(JScrollPane.UPPER_RIGHT_CORNER);
        if (corner != null) {
        	markerPanel.setHeaderComponent(corner);
        }
		
        markerPanel.addMarkerMouseClickHandler(mmch);
        
        // Initialise the table selection marker
        Color selfill = curationTable.getSelectionBackground();
        
        tableSelectionMarkerGroup = new TableSelectionMarkerGroup(curationTable, selfill);
        markerGroups.add(tableSelectionMarkerGroup);
        markerPanel.addMarkerGroup(tableSelectionMarkerGroup);
        
        // Then amend the UI
        curationTablePanel.add(markerPanel, BorderLayout.EAST);
        
        curationTableModel.addPropertyChangeListener(CurationTableModel.PROPERTY_TRAIT_INSTANCES, 
        		traitInstancesPropertyChangeListener);
        
        curationTableSelectionModel.addChangeListener(selectionChangeListener);
    }

	protected void handleSelectionChanges() {
		int[] modelRows = curationTableSelectionModel.getToolSelectedModelRows();
		int[] modelCols = curationTableSelectionModel.getToolSelectedModelColumns();
		
		Map<TraitInstance,List<Integer>> viewRowsByTraitInstance = new HashMap<>();
		for (int modelRow : modelRows) {
			if (modelRow < 0) {
				continue; // must have been filtered out?
			}
			for (int modelColumn : modelCols) {
				// Note that these are "selected" in the tools - NOT in the table
				if (modelColumn >= 0) {
					// ... and it is visible
					TraitInstance ti = curationTableModel.getTraitInstanceAt(modelColumn);
					if (ti != null) {
						// ... and corresponds to a TraitInstance
						List<Integer> list = viewRowsByTraitInstance.get(ti);
						if (list == null) {
							list = new ArrayList<>();
							viewRowsByTraitInstance.put(ti, list);
						}
						list.add(modelRow);
					}
				}
			}
		}
		
		for (MarkerGroup markerGroup : markerGroups) {
			if (markerGroup instanceof TraitInstanceMarkerGroup) {
				TraitInstanceMarkerGroup timg = (TraitInstanceMarkerGroup) markerGroup;
				List<Integer> tiViewRows = viewRowsByTraitInstance.get(timg.traitInstance);
				if (tiViewRows == null) {
					markerGroup.clearMarkers();
				}
				else {
					markerGroup.setMarkers(tiViewRows);
				}
			}
		}	
	}
}
