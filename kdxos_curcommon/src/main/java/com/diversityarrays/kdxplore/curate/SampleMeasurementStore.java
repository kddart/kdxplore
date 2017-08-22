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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;

public class SampleMeasurementStore {
	
	private static final boolean DEBUG = false;
	
	private final Map<CurationCellId,KdxSample> sampleByCellId = new HashMap<>();
	private final String name;
	private final DeviceType deviceType;

	private final PlotInfoProvider plotInfoProvider;
	
	public SampleMeasurementStore(PlotInfoProvider pip, DeviceType deviceType) {
		this(pip, deviceType.name(), deviceType);
	}

	public SampleMeasurementStore(PlotInfoProvider pip, String name, DeviceType deviceType) {
		this.plotInfoProvider = pip;
		this.name = name;
		this.deviceType = deviceType;
	}
	
	public Iterable<KdxSample> getSamples() {
		return sampleByCellId.values();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"["+name+" / "+ deviceType +"]";
	}
	
	public String getName() {
		return name;
	}
	
	public void setCalculatedSamples(TraitInstance ti, List<KdxSample> samples) {
		sampleByCellId.clear();
		for (KdxSample sm : samples) {
			CurationCellId ccid = new CurationCellId(ti, sm);
			sampleByCellId.put(ccid, sm);
		}
	}
	
	public void setSampleMeasurementData(SampleGroup sampleGroup) {
		if (sampleGroup != null) {
			List<KdxSample> measures = sampleGroup.getSamples();
			if (measures != null && ! measures.isEmpty()) {
				for (KdxSample sm : measures) {
					TraitInstance ti = plotInfoProvider.getTraitInstanceForSample(sm);
					CurationCellId ccid = new CurationCellId(ti, sm);
					sampleByCellId.put(ccid, sm);
				}
			}
		}
		
		if (! DEBUG) {
			return;
		}
		List<CurationCellId> ccidlist = new ArrayList<CurationCellId>(sampleByCellId.keySet());
		Collections.sort(ccidlist);
		
		PrintStream ps = null;
		try {
			File tmp = new File(System.getProperty("user.dir"), "sms_"+this.name+".txt");
			ps = new PrintStream(tmp);
			System.out.println("===== "+tmp.getPath());
			for (CurationCellId ccid : ccidlist) {
				ps.println(ccid);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	public KdxSample getSampleMeasurement(CurationCellId ccid) {
		return sampleByCellId.get(ccid);
	}

	public void putSampleMeasurement(CurationCellId ccid, KdxSample sm) {
		if (DeviceType.EDITED != deviceType) {
			throw new RuntimeException(
					"putSampleMeasurement(" + ccid + " , " + sm.getTraitValue() 
					+ " ) is INVALID for Store " + name);
		}
		if (sm==null) {
			sampleByCellId.remove(ccid);
		}
		else {
			sampleByCellId.put(ccid, sm);
		}
	}
}
