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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.util.ObjectUtil;

import net.pearcan.ui.table.BspAbstractTableModel;

/**
 * Provides a summary view of the SampleMeasurements (and their Traits)
 * in a given TrialData instance. This should have the "edited" SampleMeasurements
 * as well as those that are stored in the database and all of the Raw (imported from scoring device)
 * values too.
 * <p>
 * There seems to be a case where none of the database sample measurements are in the trial definition!
 * @author brian
 *
 */
public class SampleGroupTableModel extends BspAbstractTableModel {

	static private void addIfNonEmpty(List<SampleGroup> smdata, SampleGroup s) {
		if (s != null && s.getSampleCount() > 0) {
			smdata.add(s);
		}
	}
	
	List<SampleGroup> sampleGroups = new ArrayList<>();
	Map<SampleGroup, Integer> traitCountBySampleGroup = new HashMap<>();
	
	private CurationData curationData;
	
	public void setSampleMeasurementData(SampleGroup sampleMeasurements){
		
		List<Integer> rowList = new ArrayList<Integer>();
		
		for (SampleGroup sg: sampleGroups) {
			for (KdxSample sm: sg.getSamples()) {
				if (sampleMeasurements.contains(sm)) {
					rowList.add(sampleGroups.indexOf(sg));
				}
			}
		}
		
		for(Integer i: rowList){
			this.setValueAt(0, i, 0);			
		}
	}
	
	private final Map<Integer,DeviceIdentifier> deviceIdentifierById = new HashMap<>();
	
	public SampleGroupTableModel(CurationData cd) {
		super("Device", "Date Loaded", "Operator", "# Samples", "# Traits");
		
		this.curationData = cd;

		List<DeviceIdentifier> list = curationData.getDeviceIdentifiers();
		for (DeviceIdentifier di : list) {
			deviceIdentifierById.put(di.getDeviceIdentifierId(), di);
		}
		
		for (SampleGroup s : curationData.getDeviceSampleGroups()) {
			sampleGroups.add(s);
		}
		// First: KDSmart devices in Descending order by date
		Collections.sort(sampleGroups, new Comparator<SampleGroup>() {
			@Override
			public int compare(SampleGroup o1, SampleGroup o2) {
				return ObjectUtil.safeCompare(o2.getDateLoaded(), o1.getDateLoaded());
			}
		});
		// Then Curated - and lastly Database
		addIfNonEmpty(sampleGroups, curationData.getEditedSampleGroup());
		addIfNonEmpty(sampleGroups, curationData.getDatabaseSampleGroup());
		
		
		for (SampleGroup s : sampleGroups) {
			Set<Integer> traitIds = new HashSet<Integer>();
			for (KdxSample sm : s.getSamples()) {
				traitIds.add(sm.getTraitId());
			}
			traitCountBySampleGroup.put(s, traitIds.size());
		}
	}
	
	public SampleGroup[] getSampleGroups() {
		return sampleGroups.toArray(new SampleGroup[sampleGroups.size()]);
	}

	@Override
	public int getRowCount() {
		return sampleGroups.size();
	}
	
	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case 0: return DeviceIdentifier.class;
		case 1: return java.util.Date.class;
		case 2: return String.class;
		case 3: return Integer.class;
		case 4: return Integer.class;
		}
		return Object.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		SampleGroup s = sampleGroups.get(rowIndex);
		switch (columnIndex) {
		case 0: 
			return deviceIdentifierById.get(s.getDeviceIdentifierId());
		case 1: 
			DeviceIdentifier devid = deviceIdentifierById.get(s.getDeviceIdentifierId());
			if (devid==null || DeviceType.KDSMART != devid.getDeviceType()) {
				return null;
			}
			return s.getDateLoaded();
		case 2: return s.getOperatorName();
		case 3: return s.getSampleCount();
		case 4: return traitCountBySampleGroup.get(s);
		}
		return null;
	}

	public boolean isKdsmartDeviceRow(int rowIndex) {
		SampleGroup s = sampleGroups.get(rowIndex);
		DeviceIdentifier devid = deviceIdentifierById.get(s.getDeviceIdentifierId());
		return devid != null && DeviceType.KDSMART==devid.getDeviceType();
	}
}
