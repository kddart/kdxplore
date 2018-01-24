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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.RowSorterEvent;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.ui.ColumnRows;

public class CurationTableSelectionModelImpl extends DefaultListSelectionModel implements CurationTableSelectionModel {

	private CurationTableModel curationModel;

	private final List<ColumnRows> selectedColumnRows = new ArrayList<>();

	private final ChangeEvent changeEvent = new ChangeEvent(this);

	private final SelectedValueStore selectedValueStore;
	
	public CurationTableSelectionModelImpl(SelectedValueStore svs, CurationTableModel curationModel) {
		selectedValueStore = svs;
		this.curationModel = curationModel;
	}

	/**
	 * Using selected plot Id's and TraitInstance to extrapolate the selected table cells. ToolInstanceId 
	 * included for specific rendering if wished.
	 * @param traitInstanceByPlotIds
	 * @param toolId
	 */
	@Override
	public void updateSampleSelection() {

		setValueIsAdjusting(true);
		try {
			selectedColumnRows.clear();

			PlotsByTraitInstance plotsByTraitInstance = selectedValueStore.getSyncedPlotsByTrait();

			ColumnRows columnRows = new ColumnRows();

			for (TraitInstance ti : plotsByTraitInstance.getTraitInstances()) {
				Integer columnNumber = curationModel.getColumnIndexForTraitInstance(ti);
				if (columnNumber != null) {
					Set<Integer> rows = new HashSet<Integer>();
					for (PlotOrSpecimen pos : plotsByTraitInstance.getPlotSpecimens(ti)) {
						if (pos != null) {
//						    Optional<Integer> opt = curationModel.getRowForPlotOrSpecimen(pos);
//						    if (opt.isPresent()) {
//						        Integer row = opt.get();
//                                if (! modelRowsNotShown.contains(row)) {
//                                    rows.add(row);
//                                }
//						    }
							Integer[] plotRows = curationModel.getRowsForPlotId(pos.getPlotId(), ti.trait.getTraitLevel());
							for (Integer row : plotRows) {
							    if (! modelRowsNotShown.contains(row)) {
							        rows.add(row);
							    }
							}
						}
					}
					
					if (! rows.isEmpty()) {
						columnRows.add(columnNumber,rows);
					}
				}
			}
			selectedColumnRows.add(columnRows);
		}
		finally {
			setValueIsAdjusting(false);
		}

		
		ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
		for (ChangeListener l : listeners) {
			l.stateChanged(changeEvent);
		}
	}

	/**
	 * Return the 
	 */
	public List<ColumnRows> getSelectedColumnRows() {
		return selectedColumnRows;		
	}

	/**
	 * @return
	 */
	@Override
	public int[] getToolSelectedModelColumns() {
	
		Set<Integer> columnSet = new HashSet<>();
		
		for (ColumnRows crows : selectedColumnRows) {
			columnSet.addAll(crows.getColumns());
		}
		
		int[] cols = new int[columnSet.size()];
		int i = 0;
		for (Integer column : columnSet) {
			cols[i] = column.intValue();
			i++;
		}
		
		return cols;
	}
	
	/**
	 * @return
	 */
	@Override
	public int[] getToolSelectedModelRows() {
		
		Set<Integer> rowSet = new HashSet<>();
		
		for (ColumnRows e : selectedColumnRows) {
			for (Set<Integer> set : e.getRowSets()) {
				rowSet.addAll(set);
			}
		}
		
		int[] rows = new int[rowSet.size()];
		int i = 0;
		for (Integer row : rowSet) {
			rows[i] = row.intValue();
			i++;
		}
		
		return rows;
	}

	// Used to protect against races in table sorting and model updates
	private List<Integer> modelRowsNotShown = new ArrayList<Integer>();
	
	/**
	 * @param e
	 */
	@Override
	public void handleSorterChanged(JTable curationTable, RowSorterEvent e) {

		modelRowsNotShown.clear();
		
		for (int i = 0; i < curationModel.getRowCount(); i++) {
			int check = curationTable.convertRowIndexToView(i);		
			if (check < 0) {
				modelRowsNotShown.add(i);
			}			
		}

		switch( e.getType() ) {
		case SORTED:
			break;
		case SORT_ORDER_CHANGED:
			break;
		default:
			break;
		}		
	}

	@Override
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);		
	}

	@Override
	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);		
	}

}
