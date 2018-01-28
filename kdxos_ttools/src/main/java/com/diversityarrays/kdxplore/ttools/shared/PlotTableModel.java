/**
 * 
 */
/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.ttools.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.collections15.Transformer;
import org.pietschy.wizard.Wizard;
import org.pietschy.wizard.WizardModel;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.dal.DalSpecimen;
import com.diversityarrays.kdxplore.data.loading.TrialLoadWorker;
import com.diversityarrays.kdxplore.ttools.shared.Msg;

public class PlotTableModel extends AbstractTableModel implements PlotSpecimensModel {

	private final String [] PLANTED_SPECIMENS_COLUMNS = { 
	        TrialLoadWorker.PLANTED_SPECIMEN_NAME,
	        TrialLoadWorker.PLANTED_SPECIMEN_ID 
	};

	private boolean needNewSpecimensColumn;
	public  PlotTableModel(boolean needNewSpecimenColumn) {
		this.needNewSpecimensColumn = needNewSpecimenColumn;
	}

	public static final Comparator<Plot> PLOT_COMPARATOR = new Comparator<Plot>() {
		@Override
		public int compare(Plot o1, Plot o2) {
			int diff = 0;

			Integer userPlotId1 = o1.getUserPlotId();
			if (userPlotId1 != null) {
				Integer userPlotId2 = o2.getUserPlotId();
				if (userPlotId2 != null) {
					diff = userPlotId1.compareTo(userPlotId2);
				}
			}

			if (diff == 0) {
				diff = Integer.compare(o1.getPlotColumn(), o2.getPlotColumn());
				if (diff == 0) {
					diff = Integer.compare(o1.getPlotRow(), o2.getPlotRow());
				}
			}
			return diff;
		}
	};

	private boolean trialDesignEditorMode = false;

	private final Map<Plot,List<DalSpecimen>> plotToSpecimens = new HashMap<>();
	private Map<Plot,List<DalSpecimen>> editedSpecimenByplot = new HashMap<>();

	private final List<Plot> plots = new ArrayList<>();

	private Trial trial;

	private List<PlotPositionIdentifier> plotIdentifiers = 
			Arrays.asList(new PlotSpecimensSupport.PlotIdPPI("Plot ID"));

	@Override
	public void setTrial(Trial trial) {
		this.trial = trial;
		plotIdentifiers = PlotSpecimensSupport.getPPI(trial);
		fireTableStructureChanged();
	}


	@Override
	public void setSpecimensByPlot(Map<Plot,List<DalSpecimen>> map) {
		this.plotToSpecimens.clear();
		this.plots.clear();

		this.plotToSpecimens.putAll(map);
		this.plots.addAll(map.keySet());

		Collections.sort(plots, PLOT_COMPARATOR);
		this.fireTableDataChanged();
	}

	@Override
	public void setPlots(List<Plot> list) {

		this.plotToSpecimens.clear();
		this.plots.clear();

		this.plots.addAll(list);

		Collections.sort(plots, PLOT_COMPARATOR);
		this.fireTableDataChanged();
	}

	private boolean showSelectedColumn = false;
	private String selectedColumnName = null;

	public void showSelectedColumn(String selectedColumnName) {
		if (selectedColumnName != null) {
			this.selectedColumnName = selectedColumnName;
			showSelectedColumn = true;
		}
	}

	public void disableSelectedColumn() {
		this.selectedColumnName = null;
		this.showSelectedColumn = false;
	}

	@Override
	public Class<?> getColumnClass(int col) {
		if (showSelectedColumn) {
			if (col == 0) {
				return Boolean.class;
			}
			int index = col - 1;
			if (index < plotIdentifiers.size()) {
				return Integer.class;
			}
		} else {
			if (col < plotIdentifiers.size()) {
				return Integer.class;
			}
		}
		return String.class;
	}

	public Plot getPlotAt(int selIRow) {
		return plots.get(selIRow);
	}

	@Override
	public String getColumnName(int col) {

		int index = col;
		if (showSelectedColumn) {
			if (col == 0) {
				return selectedColumnName;
			}
			index--;
		}

		if (index < plotIdentifiers.size()) {
			return plotIdentifiers.get(index).getDisplayName();
		}
		else {
			if (col < plotIdentifiers.size()) {
				return plotIdentifiers.get(index).getDisplayName();
			}
		}

		index = index - plotIdentifiers.size();

		if (index <  PLANTED_SPECIMENS_COLUMNS.length) {
			return PLANTED_SPECIMENS_COLUMNS[index];	
		}

		if(this.needNewSpecimensColumn) {
			return Msg.COLHDG_SPECIMENS_PRESENT();
		}
		return "";

	}


	@Override
	public int getColumnCount() {

		int extraCol = this.showSelectedColumn
				? 2  // checkbox, specimen names 
						: 1; // specimen names
		//2 columns for Planted Specimen Name and Planted Specimen Id.
		if (this.needNewSpecimensColumn) {
			return plotIdentifiers.size() + extraCol + PLANTED_SPECIMENS_COLUMNS.length;		
		}
		else {
			return plotIdentifiers.size() + extraCol + PLANTED_SPECIMENS_COLUMNS.length - 1;
		}

	}

	@Override
	public int getRowCount() {
		return plots.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		Plot plot = plots.get(rowIndex);

		if (! plot.isActivated()) {
			return null;
		}

		int index = columnIndex;
		if (showSelectedColumn) {
			if (index == 0) {
				return harvestedPlots.contains(plot);
			}
			index = index - 1;
		}

		if (index < plotIdentifiers.size()) {
			return plotIdentifiers.get(index).getDisplayValue(plot);
		}

		index = index - plotIdentifiers.size();

		if (plotAttributeProvider != null && index <  PLANTED_SPECIMENS_COLUMNS.length){
			return plotAttributeProvider.getPlotAttributeValue(plot,PLANTED_SPECIMENS_COLUMNS[index]);
		}
		if (this.needNewSpecimensColumn) {
			
			List<DalSpecimen> specimens = new ArrayList<>();
			if (trialDesignEditorMode) {
				specimens = editedSpecimenByplot.get(plot);
			}
			else if (specimens == null || specimens.isEmpty()) {
				if (plotToSpecimens != null) {
					specimens =  plotToSpecimens.get(plot);
				}
			}

			String specimenNames = specimens.stream()
					.map(DalSpecimen::getSpecimenName)
					.collect(Collectors.joining(", "));
			return specimenNames;
		}
		return null;

	}

	private Set<Plot> harvestedPlots = new HashSet<Plot>();

	/**
	 * @param sourcePlot
	 * @param b : set it harvest or not harvested.
	 */
	public void setPlotHarvested(Plot sourcePlot, boolean b) {
		if (!b) {
			harvestedPlots.remove(sourcePlot);
		} else {
			harvestedPlots.add(sourcePlot);
		}
	}

	private String getPlotPosition(Plot plot) {

		StringBuilder sb = new StringBuilder();
		if (plotIdentifiers.isEmpty()) {
			sb.append(plot.getPlotRow()).append(" / ").append(plot.getPlotColumn());
		}
		else {
			String sep = "";
			for (PlotPositionIdentifier ppi : plotIdentifiers) {
				sb.append(sep).append(ppi.getDisplayValue(plot));
				if (sep.isEmpty()) {
					sep = " / ";
				}
				else {
					sep = ",";
				}
			}
		}
		return sb.toString();
	}

	public List<Plot> getPlots() {
		return Collections.unmodifiableList(plots);
	}



	private PlotAttributesProvider plotAttributeProvider = null;

	public void setEditedSpecimensByPlot(Plot plot, List<DalSpecimen> specimens) {
		editedSpecimenByplot.put(plot, specimens)	;
		this.fireTableDataChanged();
	}

	public void removeChangesToPlot(Plot plot) {
		editedSpecimenByplot.remove(plot);
	}

	public void setTrialDesignMode(boolean mode) {
		trialDesignEditorMode = mode;
	}

	/**
	 * @param plot
	 * @return
	 */
	public List<DalSpecimen> getEditedSpecimenForPlot(Plot plot) {
		if (trialDesignEditorMode && editedSpecimenByplot.get(plot) != null) {
			return editedSpecimenByplot.get(plot);
		} else {
			return plotToSpecimens.get(plot);
		}
	}

	/**
	 * @return
	 */
	public Map<Plot, List<DalSpecimen>> getEditedSpecimensByPlot() {
		return editedSpecimenByplot;
	}

	public List<ChangeHolder<Plot,List<DalSpecimen>,List<DalSpecimen>>> getSpecimenChanges() {

		List<ChangeHolder<Plot,List<DalSpecimen>,List<DalSpecimen>>> result = new ArrayList<>();

		for (Plot plot : editedSpecimenByplot.keySet()) {
			ChangeHolder<Plot,List<DalSpecimen>,List<DalSpecimen>> changeHolder = new ChangeHolder<>(
					plot, 
					plotToSpecimens.get(plot), 
					editedSpecimenByplot.get(plot));

			result.add(changeHolder);
		}

		return result;
	}

	public void setPlotDeactivated(Plot plot) {
		if (plot.isActivated()) {
			plot.setWhenDeactivated(new Date());
		}
	}

	public void setPlotActivated(Plot plot) {
		if (!plot.isActivated()) {
			plot.setWhenDeactivated(null);
		}
	}

	private final static String READABLE_NAME = "Trial Unit Specimen information";

	public String getReadableName() {
		return READABLE_NAME;
	}

	public int getRowAt(Plot sourcePlot) {
		return this.plots.indexOf(sourcePlot);
	}

	@Override
	public void setPlotAttributeProvider(PlotAttributesProvider pAP) {
		this.plotAttributeProvider  = pAP;
	}

}
