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
package com.diversityarrays.kdxplore.curate.fieldview;

import java.awt.Point;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.PlotsByTraitInstance;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.field.FieldLayoutTableModel;
import com.diversityarrays.kdxplore.field.KdxploreFieldLayout;
import com.diversityarrays.kdxplore.ui.CellSelectableTable;
import com.diversityarrays.util.Check;

@SuppressWarnings("nls")
public class FieldViewSelectionModel extends DefaultListSelectionModel implements ListSelectionModel {

//	private PlotsByTraitInstance selectedPlotsByTraitInstance = new PlotsByTraitInstance();

	private static final String TAG = FieldViewSelectionModel.class.getSimpleName();
	
	static public final TraitInstance NO_TRAIT_INSTANCE = new TraitInstance();

	private final SelectedValueStore selectedValueStore;

	private String fieldLayoutTableName;

	private CellSelectableTable fieldLayoutTable;

	private TraitInstance activeTraitInstance = NO_TRAIT_INSTANCE;
	
	private final FieldLayoutTableModel fieldLayoutTableModel;
	
	private final PlotInfoProvider plotInfoProvider;
	
	public FieldViewSelectionModel(
	        PlotInfoProvider plotInfoProvider,
			CellSelectableTable fieldLayoutTable,
			FieldLayoutTableModel fltm,
			SelectedValueStore svs) 
	{
	    this.plotInfoProvider = plotInfoProvider;
		this.fieldLayoutTable = fieldLayoutTable;
		this.fieldLayoutTableName = fieldLayoutTable.getName();
		this.fieldLayoutTableModel = fltm;
		this.selectedValueStore = svs;
	}

	public String getStoreId() {
		return fieldLayoutTableName;
	}
	
	public KdxploreFieldLayout<Plot> getFieldLayout() {
		return fieldLayoutTableModel.getFieldLayout();
	}
	
	@Override
	public void clearSelection() {
		Shared.Log.d(TAG, "clearSelection");
		super.clearSelection();
	}
	/**
	 * Using selected plot Id's and TraitInstance to extrapolate the selected table cells. ToolInstanceId 
	 * included for specific rendering if wished.
	 * @param plotIdsByTraitInstance
	 * @param toolId
	 */
	public void refreshSelectedMeasurements(String fromWhere) {
		Shared.Log.d(TAG, "refreshSelectedMeasurements: BEGIN from" + fromWhere);

		PlotsByTraitInstance plotsByTraitInstance = selectedValueStore.getSyncedPlotsByTrait();

		Set<Plot> plots = plotsByTraitInstance.getPlots(activeTraitInstance, plotInfoProvider);

		List<Point> points = getFieldLayout().getPointsForItems(plots);
		fieldLayoutTable.setSelectedPoints(points);

//		if (activeTraitInstance == null) {
//			fieldLayoutTable.setSelectedPoints(null);
//		}
//		else {
//		}
		
		Shared.Log.d(TAG, "refreshSelectedMeasurements: END");
	}
	
	public boolean isSelectedPlot(Plot plot) {
		boolean result = false;
		Point pt = getFieldLayout().getPointForItem(plot);
		if  (pt != null) {
			result = fieldLayoutTable.isCellSelected(pt.y, pt.x);
		}
		return result;
	}

	/**
	 * @return
	 */
	public List<Plot> getSelectedPlots() {
		List<Point> points = fieldLayoutTable.getSelectedPoints();
		return getFieldLayout().getItemsAt(points);
	}

	// prevent recursion
	private boolean busy = false;
	public void setSelectedPlots(List<Plot> plots) {

		if (busy) {
			Shared.Log.d(TAG, "setSelectedPlots: ***** LOOPED, nPlots=" + plots.size());
			return;
		}
		Shared.Log.d(TAG, "setSelectedPlots: BEGIN nPlots=" + plots.size());
		
		busy = true;
		try {
			if (Check.isEmpty(plots)) {
				clearSelection();
				selectedValueStore.setSelectedPlots(fieldLayoutTableName, null);
			}
			else {
				PlotsByTraitInstance plotsByTraitInstance = new PlotsByTraitInstance();
				for (Plot plot : plots) {
				    plotsByTraitInstance.addPlot(activeTraitInstance, plot);
				}

				selectedValueStore.setSelectedPlots(fieldLayoutTableName, plotsByTraitInstance);

				// FIXME check if the removal of this fixes the non-selection of Plots in fieldview
				// when the last TI gets unchecked while looking at the SAmplesTable
				List<Point> points = getFieldLayout().getPointsForItems(plots);
				fieldLayoutTable.setSelectedPoints(points);
			}
		}
		finally {
			busy = false;
			
			Shared.Log.d(TAG, "setSelectedPlots: END");
		}
	}


	public void setActiveTraitInstance(TraitInstance ti) {
		this.activeTraitInstance = ti==null ? NO_TRAIT_INSTANCE : ti;
	}

	public TraitInstance getActiveTraitInstance(boolean nullIf_NO_TRAIT_INSTANCE) {
		if (nullIf_NO_TRAIT_INSTANCE) {
			return NO_TRAIT_INSTANCE==activeTraitInstance ? null : activeTraitInstance;
		}
		return activeTraitInstance;
	}
}
