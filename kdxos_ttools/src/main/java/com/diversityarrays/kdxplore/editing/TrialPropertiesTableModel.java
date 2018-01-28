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
package com.diversityarrays.kdxplore.editing;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdxplore.beans.DartEntityBeanRegistry;
import com.diversityarrays.util.RunMode;

public class TrialPropertiesTableModel extends EntityPropertiesTableModel<Trial> {
	
	private int plotCount;
	private final List<TrialAttribute> trialAttributes = new ArrayList<>();
	private final List<PlotAttribute> plotAttributes = new ArrayList<>();
	private final Map<PlotAttribute,Set<String>> plotAttributeValuesByPa = new TreeMap<>();

	public TrialPropertiesTableModel() {
		super(Trial.class,
				DartEntityBeanRegistry.TRIAL_BEAN_INFO.getPropertyDescriptors());
		
		if (RunMode.getRunMode().isDeveloper()) {
			System.out.println("- - - - - - - - - RunMode.DEVELOPER - - - - - - - - - - -");
			System.out.println("TrialPropertiesTableModel: " + propertyDescriptors.length + " descriptors");
			int rowIndex = 0;
			for (PropertyDescriptor pd : propertyDescriptors) {
				StringBuilder sb = new StringBuilder();
				sb.append(rowIndex)
					.append(": ").append(pd.getName())
				.append(" [").append(pd.getDisplayName()).append("] ");
				
				if (pd.getReadMethod() != null) {
					sb.append("R");
					if (null != pd.getWriteMethod()) {
						sb.append("/");
					}
				}
				if (null != pd.getWriteMethod()) {
					sb.append("W");
				}
				System.out.println(sb);
			}
			System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		}
	}

	public void setNewTrialLayout(TrialLayout tl) {
		entity.setTrialLayout(tl);
		int rowIndex = 0;
		for (PropertyDescriptor pd : propertyDescriptors) {
			if ("trialLayout".equals(pd.getName())) {
				fireTableRowsUpdated(rowIndex, rowIndex);
				break;
			}
			++rowIndex;
		}
	}
	
	public void clearData() {
		plotCount = -1;
		trialAttributes.clear();
		plotAttributes.clear();
		super.clearData();
	}
	
	public Trial getTrial() {
		return entity;
	}

	public void setPlotCount(int plotCount) {
		boolean changed = this.plotCount != plotCount;
		this.plotCount = plotCount;
		if (changed) {
			int row = propertyDescriptors.length;
			fireTableRowsUpdated(row, row);
		}
	}

	public boolean isCurrentTrial(int trialId) {
		return (entity != null && entity.getTrialId()==trialId);
	}
	
	public void setData(Trial t, 
			List<TrialAttribute> tAttributes, 
			Map<PlotAttribute,Set<String>> paValuesByPa) 
	{
		trialAttributes.clear();
		plotAttributes.clear();
		plotAttributeValuesByPa.clear();
		if (tAttributes != null) {
			trialAttributes.addAll(tAttributes);
		}
		if (paValuesByPa != null) {
			plotAttributes.addAll(paValuesByPa.keySet());
			plotAttributeValuesByPa.putAll(paValuesByPa);
		}
		
		super.setData(t);
	}

	@Override
	public int getRowCount() {
		return entity == null 
				? 0 
				: propertyDescriptors.length 
				+ 1 // plot count 
				+ trialAttributes.size() 
				+ plotAttributes.size();
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {

		// Rows 0..#propertyDesc-1 : values from propertyDescriptor
		// Row propertyDesc.length : Plot Count
		// Rows > propertyDesc.length: trialAttributes
		if (rowIndex < propertyDescriptors.length) {
			return super.getValueAt(rowIndex, columnIndex);
		}
		
		// Immediately after the Trial's properties show the number of plots.
		int aindex = rowIndex - propertyDescriptors.length;
		if (aindex == 0) {
			switch (columnIndex) {
			case 0:
				return entity.getNameForPlot() + " Count";
			case 1:
				return plotCount < 0 ? "" : String.valueOf(plotCount);
			}
			return null;
		}

		// Then come the TrialAttributes
		--aindex;
		if (aindex < trialAttributes.size()) {
			TrialAttribute ta = trialAttributes.get(aindex);
			
			switch (columnIndex) {
			case 0:
				return ta.getTrialAttributeName();
			case 1:
				return ta.getTrialAttributeValue();
			}
            return null;
		}
		
		// And then the PlotAttributes
		aindex -= trialAttributes.size();
		
		PlotAttribute pa = plotAttributes.get(aindex);
		switch (columnIndex) {
		case 0:
			return pa.getPlotAttributeName();
		case 1:
			return plotAttributeValuesByPa.get(pa);
		}
		return null;
	}
	
	public boolean hasAnyTrialAttributes() {
		return ! trialAttributes.isEmpty();
	}
	
	public List<Integer> getTrialAttributeRowNumbers() {
		return getRowNumbers(RowType.TRIAL_ATTRIBUTE);
	}

	public boolean hasAnyPlotAttributes() {
		return ! plotAttributes.isEmpty();
	}

	public List<Integer> getPlotAttributeRowNumbers() {
		return getRowNumbers(RowType.PLOT_ATTRIBUTE);
	}

	private List<Integer> getRowNumbers(RowType rowType) {
		List<Integer> list = new ArrayList<>();
		boolean found = false;
		for (int modelRow = getRowCount(); --modelRow >= 0; ) {
			if (getRowType(modelRow) == rowType) {
				list.add(modelRow);
				found = true;
			}
			else if (found) {
				break;
			}
		}
		return list;
	}

	public RowType getRowType(int modelRow) {
		int aindex = modelRow - propertyDescriptors.length;
		
		if (aindex < 0) {
			return RowType.TRIAL_PROPERTY;
		}
		if (aindex == 0) {
			return RowType.PLOT_COUNT;
		}
//		--aindex;
//		if (aindex == 0) {
//			return RowType.SAMPLE_GROUP_COUNT;
//		}
		--aindex;
		if (aindex < trialAttributes.size()) {
			return RowType.TRIAL_ATTRIBUTE;
		}
		return RowType.PLOT_ATTRIBUTE;
	}
	
	public enum RowType {
		TRIAL_PROPERTY,
		PLOT_COUNT,
//		SAMPLE_GROUP_COUNT,
		TRIAL_ATTRIBUTE,
		PLOT_ATTRIBUTE
	}

}
