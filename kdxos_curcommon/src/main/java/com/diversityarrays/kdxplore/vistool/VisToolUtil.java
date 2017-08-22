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
package com.diversityarrays.kdxplore.vistool;

import java.awt.Point;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JFrame;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class VisToolUtil {

    static public boolean allowSubplotTraits = false;

	private VisToolUtil() {
	}

	static public Map<TraitInstance,TraitInstanceValueRetriever<?>> buildTraitInstanceValueRetrieverMap(
			Trial trial,
			Collection<TraitInstance> traitInstances,
			Function<TraitInstance, List<KdxSample>> sampleProvider) 
	{
		Map<TraitInstance,TraitInstanceValueRetriever<?>> result = new LinkedHashMap<>();

		for (TraitInstance ti : traitInstances) {
			try {
				TraitInstanceValueRetriever<?> tivr = 
						TraitInstanceValueRetriever.getValueRetriever(trial, ti, sampleProvider);
				TraitDataType tdt = tivr.getTraitInstance().getTraitDataType();
				boolean canUse = false;
				switch (tdt) {
				case CALC:
				case DECIMAL:
				case ELAPSED_DAYS:
				case INTEGER:
					canUse = true;
					break;

				case CATEGORICAL:
				case DATE:
                    canUse = true;
					break;
				case TEXT:
					// TODO support this
					break;

				default:
					break;
				}
				if (canUse) {
					result.put(ti, tivr);
				}
			} catch (InvalidRuleException ignore) {
			}
		}
		return result;
	}

	private static final int XY_FRAME_OFFSET = 20;

	public static void staggerOpenedFrames(List<JFrame> frames) {
		JFrame previous = null;
		if (frames != null && frames.size() > 1) {
			for (JFrame frame : frames) {
				if (frame == null) {
					continue;
				}
				if (previous != null) {
					Point pt = previous.getLocation();
					frame.setLocation(pt.x + XY_FRAME_OFFSET, pt.y + XY_FRAME_OFFSET);
				}
				previous = frame;
			}
		}
	}

    public static void collectPlotSpecimens(List<Plot> plots, Consumer<PlotOrSpecimen> visitor) {
        for (Plot plot : plots) {
            for (Integer psNum : plot.getSpecimenNumbers(PlotOrSpecimen.INCLUDE_PLOT)) {
                visitor.accept(plot.getPlotOrSpecimen(psNum));
            }
        }
    }
}
