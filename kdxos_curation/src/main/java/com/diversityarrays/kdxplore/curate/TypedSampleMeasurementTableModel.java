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
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdxplore.data.dal.SampleType;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.PlotPositionIdentifier;

public class TypedSampleMeasurementTableModel extends AbstractTableModel {
	
	static interface BaseColumnProvider {
		public Class<?> getColumnClass();
		public String getColumnName();
		public Object getColumnValue(int rowIndex);
	}
	
	class PpiProvider implements BaseColumnProvider {
		
		private final PlotPositionIdentifier ppi;

		PpiProvider(PlotPositionIdentifier ppi) {
			this.ppi = ppi;
		}

		@Override
		public Class<?> getColumnClass() {
			return Integer.class;
		}

		@Override
		public String getColumnName() {
			return ppi.getDisplayName();
		}

		@Override
		public Object getColumnValue(int rowIndex) {
			TypedSampleMeasurement tsm = data.get(rowIndex);
			Plot plot = curationData.getPlotByPlotId(tsm.sample.getPlotId());
			return ppi.getPositionValue(plot);
		}
	}
	
	// return true if we changed the structure
	private boolean initialise(boolean showDeviceIdentifier, boolean showInstanceNumber) {
		List<TypedSampleMeasurementTableModel.BaseColumnProvider> list = new ArrayList<>();

		if (showDeviceIdentifier) {
			list.add(new BaseColumnProvider() {
				@Override
				public Object getColumnValue(int rowIndex) {
					TypedSampleMeasurement tsm = data.get(rowIndex);
					return tsm;
				}
				
				@Override
				public String getColumnName() {
					return "Source";
				}
				
				@Override
				public Class<?> getColumnClass() {
					return TypedSampleMeasurement.class;
				}
			});
		}
		
		if (showInstanceNumber) {
			list.add(new BaseColumnProvider() {
				@Override
				public Object getColumnValue(int rowIndex) {
					TypedSampleMeasurement tsm = data.get(rowIndex);
					return tsm.sample.getTraitInstanceNumber();
				}
				
				@Override
				public String getColumnName() {
					return "Instance";
				}
				
				@Override
				public Class<?> getColumnClass() {
					return Integer.class;
				}
			});
		}
		
		list.add(new BaseColumnProvider() {
			@Override
			public Object getColumnValue(int rowIndex) {
				TypedSampleMeasurement tsm = data.get(rowIndex);
				return tsm.sample.getMeasureDateTime();
			}
			
			@Override
			public String getColumnName() {
				return "Measured On";
			}
			
			@Override
			public Class<?> getColumnClass() {
				return java.util.Date.class;
			}
		});

		list.add(new BaseColumnProvider() {
			@Override
			public Object getColumnValue(int rowIndex) {
				TypedSampleMeasurement tsm = data.get(rowIndex);
				return getDisplayValue(tsm);
			}
			
			@Override
			public String getColumnName() {
				return "Value";
			}
			
			@Override
			public Class<?> getColumnClass() {
				return String.class;
			}
		});
		
		boolean changed = ! list.equals(baseColumnProviders);
		baseColumnProviders = list;
		return changed;
	}
	
	private List<TypedSampleMeasurementTableModel.BaseColumnProvider> baseColumnProviders = Collections.emptyList();
	
	private CurationData curationData = null; 
	
	private final List<PpiProvider> ppiProviders = new ArrayList<>();
	
	private final List<TypedSampleMeasurement> data = new ArrayList<>();
	
	private boolean showSampleType = true;
	
	private boolean multiCellSelected = false;
	
	public TypedSampleMeasurementTableModel() {
	}
	
	public void setCurationData(CurationData cd) {
		this.curationData = cd;
		ppiProviders.clear();
		if (curationData != null) {
			for (PlotPositionIdentifier ppi : curationData.getPlotPositionIdentifiers()) {
				ppiProviders.add(new PpiProvider(ppi));
			}
		}
	}

	public void setShowSampleType(boolean b) {
		boolean changed = showSampleType != b;
		showSampleType = b;
		if (changed) {
			fireTableStructureChanged();
		}
	}

	public String getSampleDisplayValueAt(int row) {
		return getDisplayValue(data.get(row));
	}

	public DeviceIdentifier getDeviceIdentifierAt(int row) {
		return data.get(row).deviceIdentifier;
	}

	public void clear() {
		data.clear();
		fireTableDataChanged();
	}
	
	public void setData(List<TypedSampleMeasurement> list, boolean multi, boolean wantPPI) {
		data.clear();
		data.addAll(list);

		boolean showInstanceNumber = false;
		Integer firstInstanceNumber = null;
		for (TypedSampleMeasurement tsm : data) {
			Integer instanceNumber = tsm.getInstanceNumber();
			if (firstInstanceNumber == null) {
				firstInstanceNumber = instanceNumber;
			}
			else if (firstInstanceNumber != instanceNumber) {
				showInstanceNumber = true;
				break;
			}
		}
		
//		boolean showDeviceIdentifier = false;
//		if (true) {
//			showDeviceIdentifier = true;
//		}
//		else {
//			DeviceIdentifier firstDevId = null;
//			for (TypedSampleMeasurement tsm : data) {
//				DeviceIdentifier devid = tsm.getDeviceIdentifier();
//				if (firstDevId == null) {
//					firstDevId = devid;
//				}
//				else if (! firstDevId.equals(devid)) {
//					showDeviceIdentifier = true;
//					break;
//				}
//			}
//		}

		boolean showDeviceIdentifier = true;

		boolean multiChanged = multiCellSelected != multi;
		multiCellSelected = multi;

		boolean structureChanged = initialise(showDeviceIdentifier, showInstanceNumber);

		boolean changedPPI = setShowPositionsInternal(wantPPI);
		
		if (structureChanged || changedPPI || multiChanged) {
			fireTableStructureChanged();
		}
		else {
			fireTableDataChanged();
		}
	}
	
	public void setShowPositions(boolean show) {
		if (setShowPositionsInternal(show)) {
			fireTableStructureChanged();
		}
	}
	
	private boolean setShowPositionsInternal(boolean show) {
		boolean changedPPI = false;

		int nBase = baseColumnProviders.size();
		if (show) {
			if (nBase > 0) {
				if (baseColumnProviders.get(nBase-1) instanceof PpiProvider) {
				}
				else {
					// NOT already showing
					changedPPI = true;
					baseColumnProviders.addAll(ppiProviders);
				}
			}
		}
		else {
			// Want to remove them
			while (nBase > 0 
					&&
					baseColumnProviders.get(nBase-1) instanceof PpiProvider)
			{
				baseColumnProviders.remove(nBase - 1);
				nBase = baseColumnProviders.size();
				changedPPI = true;
			}
		}
		return changedPPI;
	}


	@Override
	public int getColumnCount() {
		int result = baseColumnProviders.size();
		if (showSampleType) {
			++result;
		}
		return result;
	}
	
	@Override
	public String getColumnName(final int column) {
		if (column < baseColumnProviders.size()) {
			return baseColumnProviders.get(column).getColumnName();
		}
		return "Sample Type";
	}
	
	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if (column < baseColumnProviders.size()) {
			return baseColumnProviders.get(column).getColumnClass();
		}
		return SampleType.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TypedSampleMeasurement tsm = data.get(rowIndex);
		if (columnIndex < baseColumnProviders.size()) {
			return baseColumnProviders.get(columnIndex).getColumnValue(rowIndex);
		}
		return tsm.getSampleType();
	}

	private String getDisplayValue(TypedSampleMeasurement tsm) {
		if (tsm.traitValue==null) {
			return tsm.sample.getTraitValue();
		}
		return tsm.traitValue.displayValue;
	}

	public TypedSampleMeasurement getTypedSampleAt(int row) {
		if (row < data.size()) {
			return data.get(row);
		} 
		return null;
	}
	
	/**
	 * @param row
	 * @return
	 */
	public KdxSample getSampleAt(int row) {
		if (row < data.size()) {
			return data.get(row).sample;
		} 
		return null;
	}

	
}
