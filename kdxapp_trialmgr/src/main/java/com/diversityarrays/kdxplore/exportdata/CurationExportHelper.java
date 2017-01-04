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
package com.diversityarrays.kdxplore.exportdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.importdata.bms.BmsConstant;
import com.diversityarrays.util.Check;

public interface CurationExportHelper {
	Plot getPlotByRowIndex(int rowIndex);
	Sample getSampleForPlotAndTraitInstance(PlotOrSpecimen plot, TraitInstance traitInstance);
	List<PlotAttribute> getPlotAttributes(boolean all);
	List<TraitInstance> getTraitInstances(WhichTraitInstances whichTraitInstances);
	boolean hasPlotType(boolean bmsMode);
	Trait getTrait(int traitId);
    List<KdxSample> getSampleMeasurements(TraitInstance ti);

	
	static public List<String> collectPaNamesForBMS(
			Trial trial,
			ExportOptions options,
			boolean hasPlotType,
			List<PlotAttribute> plotAttributes) 
	{
		List<String> paNames = new ArrayList<>();
		
		for (PlotAttribute pa : plotAttributes) {
			paNames.add(pa.getPlotAttributeName());
		}
		if (hasPlotType) {
			paNames.add(BmsConstant.XLSHDG_ENTRY_TYPE);
		}
		Collections.sort(paNames);

		PlotIdentSummary plotIdentSummary = trial.getPlotIdentSummary();
		boolean hasUserPlotId = !plotIdentSummary.plotIdentRange.isEmpty();
		boolean hasPlotX = !plotIdentSummary.xColumnRange.isEmpty();
		boolean hasPlotY = !plotIdentSummary.yRowRange.isEmpty();

		if (hasUserPlotId) {
			paNames.add(BmsConstant.XLSHDG_PLOT_NO);
		}
		if (hasPlotX) {
			String name = options.nameForColumn;
			if (Check.isEmpty(name)) {
				name = BmsConstant.XLSHDG_FIELDMAP_COLUMN;
				// TODO how to tell if this or XLSHDG_COLUMN_NO was used
			}
			paNames.add(name);
		}
		if (hasPlotY) {
			String name = options.nameForRow;
			if (Check.isEmpty(name)) {
				name = BmsConstant.XLSHDG_FIELDMAP_RANGE;
				// TODO how to tell if this or XLSHDG_RANGE_NO was used
			}
			paNames.add(name);
		}
		
		return paNames;
	}
	
}
