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
package com.diversityarrays.kdxplore.ttools.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.dal.DalSpecimen;

public class PlotSpecimensSupport {

    static public Map<Plot,List<DalSpecimen>> createSpecimensByPlot(
            List<Plot> plots,
            Function<Plot, List<DalSpecimen>> specimensProvider) 
    {
        Map<Plot, List<DalSpecimen>> result = new HashMap<>();
        for (Plot plot : plots) {
            List<DalSpecimen> list = specimensProvider.apply(plot);
            if (list == null) {
                list = new ArrayList<>();
            }
            result.put(plot, list);
        }
        return result;
    }
    
    


    static abstract public class AbstractPPI implements PlotPositionIdentifier {
        
        private final String displayName;
        public AbstractPPI(String dn) {
            displayName = dn;
        }
        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
    
    static public class PlotIdPPI extends AbstractPPI {
        public PlotIdPPI(String s) {
            super(s);
        }

        @Override
        public Integer getDisplayValue(Plot plot) {
            return plot.getPlotId();
        }
    }
    
    static public class UserPlotIdPPI extends AbstractPPI {

        public UserPlotIdPPI(String s) {
            super(s);
        }

        @Override
        public Integer getDisplayValue(Plot plot) {
            return plot.getUserPlotId();
        }
    }

    static public class PlotColumnPPI extends AbstractPPI {

        public PlotColumnPPI(String s) {
            super(s);
        }

        @Override
        public Integer getDisplayValue(Plot plot) {
            return plot.getPlotColumn();
        }
    }

    static public class PlotRowPPI extends AbstractPPI {

        public PlotRowPPI(String s) {
            super(s);
        }

        @Override
        public Integer getDisplayValue(Plot plot) {
            return plot.getPlotRow();
        }
    }

    
    static public List<PlotPositionIdentifier> getPPI(Trial trial) {
        
        List<PlotPositionIdentifier> result = new ArrayList<>();
        if (trial != null) {
            PlotIdentOption pio = trial.getPlotIdentOption();
            switch (pio) {
            case NO_X_Y_OR_PLOT_ID:
                result.add(new PlotIdPPI(trial.getNameForPlot() + " ID"));
                break;
            case PLOT_ID:
                result.add(new UserPlotIdPPI(trial.getNameForPlot()));
                break;
            case PLOT_ID_THEN_X:
                result.add(new UserPlotIdPPI(trial.getNameForPlot()));
                result.add(new PlotColumnPPI(trial.getNameForColumn()));
                break;
            case PLOT_ID_THEN_XY:
                result.add(new UserPlotIdPPI(trial.getNameForPlot()));
                result.add(new PlotColumnPPI(trial.getNameForColumn()));
                result.add(new PlotRowPPI(trial.getNameForRow()));
                break;
            case PLOT_ID_THEN_Y:
                result.add(new UserPlotIdPPI(trial.getNameForPlot()));
                result.add(new PlotRowPPI(trial.getNameForRow()));
                break;
            case PLOT_ID_THEN_YX:
                result.add(new UserPlotIdPPI(trial.getNameForPlot()));
                result.add(new PlotRowPPI(trial.getNameForRow()));
                result.add(new PlotColumnPPI(trial.getNameForColumn()));
                break;
            case X_THEN_Y:
                result.add(new PlotColumnPPI(trial.getNameForColumn()));
                result.add(new PlotRowPPI(trial.getNameForRow()));
                break;
            case Y_THEN_X:
                result.add(new PlotRowPPI(trial.getNameForRow()));
                result.add(new PlotColumnPPI(trial.getNameForColumn()));
                break;
            default:
                break;
            }
        }
        
        if (result.isEmpty()) {
            if (trial==null) {
                result.add(new PlotIdPPI("Plot ID"));
            }
            else {
                result.add(new PlotIdPPI(trial.getNameForPlot() + " ID"));
            }
        }
        return result;
    }
    
    
    
    static public Function<Plot,String> PLOTIDENT_USER_PLOTID = new Function<Plot,String>() {
        @Override
        public String apply(Plot plot) {
            Integer userPlotId = plot.getUserPlotId();
            if (userPlotId != null) {
                return userPlotId.toString();
            }
            // If no userPlotId, default to x,y.
            StringBuilder sb = new StringBuilder();
            sb.append(plot.getPlotColumn())
                .append(',')
                .append(plot.getPlotRow());
            return sb.toString();
        }
    };

    static public Function<Plot,String> PLOTIDENT_PLOTID_X_Y = new Function<Plot,String>() {
        @Override
        public String apply(Plot plot) {
            StringBuilder sb = new StringBuilder();
            sb.append(plot.getUserPlotId());
            sb.append("/");
            sb.append(plot.getPlotColumn()).append(",").append(plot.getPlotRow());
            return sb.toString();
        }
    };

    static public Function<Plot,String> PLOTIDENT_PLOTID_X = new Function<Plot,String>() {
        @Override
        public String apply(Plot plot) {
            StringBuilder sb = new StringBuilder();
            sb.append(plot.getUserPlotId());
            sb.append("/");
            sb.append(plot.getPlotColumn());
            return sb.toString();
        }
    };

    static public Function<Plot,String> PLOTIDENT_PLOTID_Y = new Function<Plot,String>() {
        @Override
        public String apply(Plot plot) {
            StringBuilder sb = new StringBuilder();
            sb.append(plot.getUserPlotId());
            sb.append("/");
            sb.append(plot.getPlotRow());
            return sb.toString();
        }
    };

    static public Function<Plot,String> PLOTIDENT_PLOTID_Y_X = new Function<Plot,String>() {
        @Override
        public String apply(Plot plot) {
            StringBuilder sb = new StringBuilder();
            sb.append(plot.getUserPlotId());
            sb.append("/");
            sb.append(plot.getPlotRow()).append(",").append(plot.getPlotColumn());
            return sb.toString();
        }
    };
    
    static public Function<Plot,String> getPlotIdentFunction(Trial trial) {        

        Function<Plot,String> result = PLOTIDENT_USER_PLOTID;
        
        PlotIdentOption pio = trial==null ? null : trial.getPlotIdentOption();
        
        if (pio != null) {
            switch (pio) {
            case PLOT_ID_THEN_X:
                result = PLOTIDENT_PLOTID_X;
                break;
            case PLOT_ID_THEN_XY:
                result = PLOTIDENT_PLOTID_X_Y;
                break;
            case PLOT_ID_THEN_Y:
                result = PLOTIDENT_PLOTID_Y;
                break;
            case PLOT_ID_THEN_YX:
                result = PLOTIDENT_PLOTID_Y_X;
                break;

            case X_THEN_Y:
                result = PLOTIDENT_PLOTID_X_Y;
                break;
            case Y_THEN_X:
                result = PLOTIDENT_PLOTID_Y_X;
                break;

            case NO_X_Y_OR_PLOT_ID:
            case PLOT_ID:
            default:
                result = PLOTIDENT_USER_PLOTID;
                break;
            }
        }
        return result;
    }
}
