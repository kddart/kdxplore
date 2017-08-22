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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public interface PlotInfoProvider {

	Trial getTrial();
	List<TrialAttribute> getTrialAttributes();

	List<Plot> getPlots();
	
	Sample getSampleForTraitInstance(PlotOrSpecimen pos, TraitInstance traitInstance);
	List<KdxSample> getSampleMeasurements(TraitInstance ti);

	Plot getPlotByPlotId(int plotId);
	Set<Plot> getPlotsForPlotSpecimens(Collection<PlotOrSpecimen> plotSpecimens);

	Map<String,String> getPlotAttributeValues(int plotId);
	String getPlotAttributeValue(int plotId, String attributeName);
	
	Iterator<String> getPlotAttributeValuesIterator(String attributeName);

	Iterable<? extends KdxSample> getSamplesForCurationCellId(CurationCellId ccid);
    void visitSamplesForPlotOrSpecimen(PlotOrSpecimen pos, Consumer<KdxSample> visitor);

	TraitInstance getTraitInstanceForSample(Sample s);

	List<TraitInstance> getTraitInstances();
	
	void changePlotsActivation(boolean activate, List<Plot> plots);

	void addCurationDataChangeListener(CurationDataChangeListener l);
	void removeCurationDataChangeListener(CurationDataChangeListener l);

}
