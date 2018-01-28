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
package com.diversityarrays.kdxplore.field;

import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdsmart.field.FieldLayoutUtil;
import com.diversityarrays.util.XYPos;

public class FieldLayoutTableModel extends AbstractTableModel {
	
	public static final String PROP_FIELD_LAYOUT = "fieldLayout";
	
	private final boolean showYcoord;
	private KdxploreFieldLayout<Plot> fieldLayout;
	private Trial trial;
	
	public FieldLayoutTableModel() {
		this(false);
	}
	
	public FieldLayoutTableModel(boolean showYcoord) {
		this.showYcoord = showYcoord;
	}
	
	public void setTrial(Trial trial) {
		this.trial = trial;
		fireTableDataChanged();
	}
	
	public void setFieldLayout(KdxploreFieldLayout<Plot> newLayout) {
		FieldLayout<Plot> oldLayout = this.fieldLayout;
		this.fieldLayout = newLayout;
		fireTableStructureChanged();
		
		propertyChangeSupport.firePropertyChange(PROP_FIELD_LAYOUT, oldLayout, this.fieldLayout);
	}
	
	public KdxploreFieldLayout<Plot> getFieldLayout() {
		return fieldLayout;
	}
	
	@Override
	public String getColumnName(int columnIndex) {
		String result;
		if (trial == null) {
			if  (showYcoord && columnIndex==0) {
				return "X/Y";
			}
			result = String.valueOf(columnIndex);
		}
		else {
			// We have a trial
			if (showYcoord && columnIndex==0) {
				// This is the heading for the "column numbers"
				result = trial.getNameForColumn() + "/" + trial.getNameForRow();
			}
			else {
				PlotIdentSummary pis = trial.getPlotIdentSummary();

				switch (trial.getTrialLayout().getOrigin()) {

				case LOWER_LEFT:
				case UPPER_LEFT:
					if (pis.xColumnRange.isEmpty()) {
						result = String.valueOf(columnIndex + 1);
					}
					else {
						result = String.valueOf(pis.xColumnRange.getMinimum() + columnIndex - (showYcoord ? 1 : 0));
					}
					break;
					
				case LOWER_RIGHT:
				case UPPER_RIGHT:
					int offset = fieldLayout.xsize - columnIndex - 1;
					
					if (pis.xColumnRange.isEmpty()) {
						result = String.valueOf(offset + 1);
					}
					else {
						result = String.valueOf(pis.xColumnRange.getMinimum() + offset - (showYcoord ? 1 : 0));
					}
					break;

				default:
					result = "?" + columnIndex;
					break;
				
				}
			}
		}
		
		return result;
	}
	
	@Override
	public Class<?> getColumnClass(int column) {
		if (showYcoord && column == 0) {
			return Integer.class;
		}
		return Plot.class;
	}

	@Override
	public int getRowCount() {
		return fieldLayout==null ? 0 : fieldLayout.ysize;
	}

	@Override
	public int getColumnCount() {
		return (showYcoord ? 1 : 0) + (fieldLayout==null ? 0 : fieldLayout.xsize);
	}
	
//	public Plot getPlotAt(int rowIndex, int columnIndex) {
//		return fieldLayout.cells[rowIndex][columnIndex - (showYcoord ? 1 : 0)];
//	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object result = null;
				
		if (showYcoord && columnIndex == 0) {
			result = FieldLayoutUtil.convertRowIndexToYCoord(rowIndex, trial, fieldLayout);
		}
		else {
			result = fieldLayout.cells[rowIndex][columnIndex - (showYcoord ? 1 : 0)];
		}		
		return result;
	}

	public Plot getPlot(int row, int column) {
		Plot result = null;
	
		if (0 <= row && row < fieldLayout.ysize
				&&
			0 <= column && column < fieldLayout.xsize) 
		{
			result = fieldLayout.cells[row][column];
		}
		return result;
	}

	public XYPos getXYForPlot(int plotId) {
		for (int row = 0; row < fieldLayout.ysize; row++) {
			for (int col = 0; col < fieldLayout.xsize; col++) {
				Plot checkPlot = fieldLayout.cells[row][col];
				if (checkPlot != null && checkPlot.getPlotId() == plotId) {
					return new XYPos(col,row);
				}
			}
		}
		
		return new XYPos(-1,-1);
	}
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	public void plotsChanged(List<Plot> plots) {
		for (Plot plot : plots) {
			Point pt = fieldLayout.getPointForItem(plot);
			if (pt != null) {
				fireTableCellUpdated(pt.y, pt.x);
			}
		}
	}

}
