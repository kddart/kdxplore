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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Trial;

@SuppressWarnings("nls")
public class PlotOrSpecimenComparator implements Comparator<PlotOrSpecimen> {

    private final ToIntFunction<PlotOrSpecimen> xFunc = new ToIntFunction<PlotOrSpecimen>() {
        @Override
        public String toString() {
            return "xFunc"; // For debugging
        }
        @Override
        public int applyAsInt(PlotOrSpecimen pos) {
            return pos.getPlotColumn();
        }
    };
    private final ToIntFunction<PlotOrSpecimen> yFunc = new ToIntFunction<PlotOrSpecimen>() {
        @Override
        public String toString() {
            return "yFunc"; // For debugging
        }
        @Override
        public int applyAsInt(PlotOrSpecimen pos) {
            return pos.getPlotRow();
        }  
    };
    private final ToIntFunction<PlotOrSpecimen> userPlotIdFunc = new ToIntFunction<PlotOrSpecimen>() {
        @Override
        public String toString() {
            return "userPlotIdFunc"; // For debugging
        }
        @Override
        public int applyAsInt(PlotOrSpecimen pos) {
            Plot plot = plotByPlotId.get(pos.getPlotId());
            Integer userPlotId = plot==null ? null : plot.getUserPlotId();
            return userPlotId==null ? 0 : userPlotId.intValue();
        }
    };
    private final ToIntFunction<PlotOrSpecimen> specimenFunc = new ToIntFunction<PlotOrSpecimen>() {
        @Override
        public String toString() {
            return "specimenFunc"; // For debugging
        }
        @Override
        public int applyAsInt(PlotOrSpecimen pos) {
            return pos.getSpecimenNumber();
        }  
    };
    
    private final Map<Integer,Plot> plotByPlotId;
    
    private final List<ToIntFunction<PlotOrSpecimen>> functions = new ArrayList<>();

    public PlotOrSpecimenComparator(Trial trial, List<Plot> plots) {        
        this(trial,
                plots.stream()
                .collect(Collectors.toMap(Plot::getPlotId, Function.identity())),
                false);
    }

    public PlotOrSpecimenComparator(Trial trial, Map<Integer,Plot> plotByPlotId, boolean anySubplots) {        
        this.plotByPlotId = plotByPlotId;
        
        switch (trial.getPlotIdentOption()) {
        case NO_X_Y_OR_PLOT_ID:
            break;
        case PLOT_ID:
            functions.add(userPlotIdFunc);
            break;
        case PLOT_ID_THEN_X:
            functions.add(userPlotIdFunc);
            functions.add(xFunc);
            break;
        case PLOT_ID_THEN_XY:
            functions.add(userPlotIdFunc);
            functions.add(xFunc);
            functions.add(yFunc);
            break;
        case PLOT_ID_THEN_Y:
            functions.add(userPlotIdFunc);
            functions.add(yFunc);
            break;
        case PLOT_ID_THEN_YX:
            functions.add(userPlotIdFunc);
            functions.add(yFunc);
            functions.add(xFunc);
            break;
        case X_THEN_Y:
            functions.add(xFunc);
            functions.add(yFunc);
            break;
        case Y_THEN_X:
            functions.add(yFunc);
            functions.add(xFunc);
            break;
        default:
            break;
        }

        if (anySubplots && ! functions.isEmpty()) {
            functions.add(specimenFunc);
        }
    }
    
    public boolean isNoOperation() {
        return functions.isEmpty();
    }

    @Override
    public int compare(PlotOrSpecimen o1, PlotOrSpecimen o2) {
        // TODO Work out why this doesn't work to sort PlotIdent/SpecimenNumber (never seem to get to compare specimen numbers for a Plot and Specimen)
        for (ToIntFunction<PlotOrSpecimen> func : functions) {
            int diff = Integer.compare(func.applyAsInt(o1), func.applyAsInt(o2));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }
}
