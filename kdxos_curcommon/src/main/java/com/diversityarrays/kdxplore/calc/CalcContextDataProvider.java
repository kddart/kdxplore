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
package com.diversityarrays.kdxplore.calc;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public interface CalcContextDataProvider {

	Trial getTrial();
	List<TraitInstance> getTraitInstances();
	List<TrialAttribute> getTrialAttributes();
	Map<String, String> getPlotAttributeValues(int plotId);
	
	DeviceIdentifier getDeviceIdentifierForSampleGroup(int sampleGroupId);

    Plot getPlotByPlotId(int plotId);

	List<KdxSample> getSampleMeasurements(TraitInstance ti);
	KdxSample getSampleFor(TraitInstance ti, int plotId, DeviceType deviceType);

	KdxSample createCalcSampleMeasurement(
	        CurationCellId ccid,
			String traitValue, 
			Date dateTimeStamp);
	
    CurationCellId getCurationCellId(PlotOrSpecimen pos, TraitInstance calcInstance);
}
