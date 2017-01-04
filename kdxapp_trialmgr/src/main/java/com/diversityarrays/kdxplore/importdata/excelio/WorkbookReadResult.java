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
package com.diversityarrays.kdxplore.importdata.excelio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.XYPos;

@SuppressWarnings("nls")
public class WorkbookReadResult implements Iterable<ImportPlot> {
    
    public final Trial trial = new Trial();
    public final List<TrialAttribute> trialAttributes = new ArrayList<>();
    
    private final Map<String, Trait> allTraitsByName = new LinkedHashMap<>();
    public final Map<String, Trait>  plotTraitsByLowname = new HashMap<>();
    public final Map<String, Trait>  subplotTraitsByLowname = new HashMap<>();
    public final Map<String, Trait> nonLevelTraitsByLowname = new HashMap<>();
    
    public final Map<String, PlotAttribute> plotAttributeByLowcaseName = new LinkedHashMap<>();

    public WorkbookReadResult.ImportPlotStore plotStore;
    public final List<ImportPlot> plots = new ArrayList<>();
    
    public final Map<String,Tag> tagByLowcaseLabel = new LinkedHashMap<>();
    
    
    /**
     * Return non-null error message if TraitLevel is not supported.
     * @param t
     * @return
     */
    public String addTrait(Trait t) {
        allTraitsByName.put(t.getTraitName(), t);

        String loname = t.getTraitName().toLowerCase();
        TraitLevel level = t.getTraitLevel();
        if (level == null) {
            nonLevelTraitsByLowname.put(loname, t);
        }
        else {
            switch (level) {
            case PLOT:
                plotTraitsByLowname.put(loname, t);
                break;
            case SPECIMEN:
                subplotTraitsByLowname.put(loname, t);
                break;
            case UNDECIDABLE:
            default:
                return "Unsuported TraitLevel=" + level;
            }
        }
        return null;
    }
    
    public Either<DataError,Trait> getPlotLevelTrait(int rowIndex, String hdg) {
        String loname = hdg.toLowerCase();
        Trait result = plotTraitsByLowname.get(loname);
        if (result == null) {
            // look in nonLevelTraits
            result = nonLevelTraitsByLowname.remove(loname);
            if (result == null) {
                // Still not there! This is an error
                return Either.left(new DataError(rowIndex, "Undefined Trait '" + hdg + "'"));
            }
            plotTraitsByLowname.put(loname, result);
        }
        return Either.right(result);
    }
    
    public Either<DataError, Trait> getSpecimenLevelTrait(int rowIndex, String hdg) {
        String loname = hdg.toLowerCase();
        Trait result = subplotTraitsByLowname.get(loname);
        if (result == null) {
            // look in nonLevelTraits
            result = nonLevelTraitsByLowname.remove(loname);
            if (result == null) {
                // Still not there! This is an error
                return Either.left(new DataError(rowIndex, "Undefined Trait '" + hdg + "'"));
            }
            subplotTraitsByLowname.put(loname, result);
        }
        return Either.right(result);
    }

    public Either<DataError, ImportPlot> addSpecimenToPlot(int rowIndex, ImportSpecimen specimen) {
        return plotStore.getPlotForSpecimen(rowIndex, specimen);
    }

    public DataError addPlotAttribute(int rowIndex, PlotAttribute pa) {
        String loname = pa.getPlotAttributeName().toLowerCase();
        if (plotAttributeByLowcaseName.containsKey(loname)) {
            return new DataError(rowIndex, "Duplicate PlotAtribute: " + pa.getPlotAttributeName());
        }
        
        int count = plotAttributeByLowcaseName.size() + 1;
        // HACK: use negative ids so PlotAttributeValues can link to the correct PlotAttribute.
        pa.setPlotAttributeId(- count);
        plotAttributeByLowcaseName.put(loname, pa);
        return null;
    }
    

    public PlotAttribute getPlotAttributeByName(String hdg) {
        return plotAttributeByLowcaseName.get(hdg.toLowerCase());
    }

    public DataError addTag(int rowIndex, Tag tag) {
        String lolabel = tag.getLabel().toLowerCase();
        if (tagByLowcaseLabel.containsKey(lolabel)) {
            return new DataError(rowIndex, "Duplicate Tag label: " + tag.getLabel());
        }
        tagByLowcaseLabel.put(lolabel, tag);
        return null;
    }
    
    public DataError addPlot(int rowIndex, ImportPlot plot) {
        if (plotStore == null) {
            if (plot.userPlotId == null) {
                if (plot.plotColumn==null) {
                    return new DataError(rowIndex, "Plot Column and Row are required if no PlotId is available");
                }
                if (plot.plotRow==null) {
                    return new DataError(rowIndex, "Plot Column and Row are required if no PlotId is available");
                }
                plotStore = new ColumnRowPlotStore();
            }
            else {
                plotStore = new UserPlotIdPlotStore();
            }
        }            
        return plotStore.addPlot(rowIndex, plot);
    }

    @Override
    public Iterator<ImportPlot> iterator() {
        if (plotStore == null) {
            return Collections.emptyIterator();
        }
        return plotStore.iterator();
    }


    static interface ImportPlotStore {
        DataError addPlot(int rowIndex, ImportPlot plot);

        Iterator<ImportPlot> iterator();

        boolean hasUserPlotId();

        Either<DataError, ImportPlot> getPlotForSpecimen(int rowIndex, ImportSpecimen specimen);
    }

    static class UserPlotIdPlotStore implements WorkbookReadResult.ImportPlotStore {

        private final Map<Integer, ImportPlot> plotByUserPlotId = new LinkedHashMap<>();

        @Override
        public boolean hasUserPlotId() {
            return true;
        }
        
        @Override
        public Either<DataError, ImportPlot> getPlotForSpecimen(int rowIndex, ImportSpecimen specimen) {
            if (specimen.userPlotId == null) {
                return Either.left(new DataError(rowIndex, "Missing Plot Id"));
            }
            ImportPlot importPlot = plotByUserPlotId.get(specimen.userPlotId);
            if (importPlot == null) {
                return Either.left(new DataError(rowIndex, "No Plot with PlotId='" + specimen.userPlotId + "'"));
            }
            return Either.right(importPlot);
        }
        
        @Override
        public DataError addPlot(int rowIndex, ImportPlot plot) {
            if (plot.userPlotId == null) {
                return new DataError(rowIndex, "No Plot Id provided");
            }
            
            if (plotByUserPlotId.containsKey(plot.userPlotId)) {
                return new DataError(rowIndex, "Duplicate PlotId: " + plot.userPlotId);
            }
            
            plotByUserPlotId.put(plot.userPlotId, plot);
            return null;
        }

        @Override
        public Iterator<ImportPlot> iterator() {
            return plotByUserPlotId.values().iterator();
        }
        
    }
    
    static class ColumnRowPlotStore implements WorkbookReadResult.ImportPlotStore {

        private final Map<XYPos,ImportPlot> plotByXY = new LinkedHashMap<>();
        
        @Override
        public boolean hasUserPlotId() {
            return false;
        }
        
        @Override
        public Either<DataError, ImportPlot> getPlotForSpecimen(int rowIndex, ImportSpecimen specimen) {
            if (specimen.plotColumn == null) {
                return Either.left(new DataError(rowIndex, "Missing Plot Column"));
            }
            if (specimen.plotRow == null) {
                return Either.left(new DataError(rowIndex, "Missing Plot Row"));
            }
            
            XYPos xyPos = new XYPos(specimen.plotColumn, specimen.plotRow);
            ImportPlot importPlot = plotByXY.get(xyPos);
            if (importPlot == null) {
                return Either.left(new DataError(rowIndex, 
                        "No Plot with PlotColumn,PlotRow=" + specimen.plotColumn + "," + specimen.plotRow));
            }
            return Either.right(importPlot);
        }
        


        @Override
        public DataError addPlot(int rowIndex, ImportPlot plot) {
            if (plot.plotColumn == null) {
                return new DataError(rowIndex, "No Plot Column provided");
            }
            if (plot.plotRow == null) {
                return new DataError(rowIndex, "No Plot Row provided");
            }
            XYPos xyPos = new XYPos(plot.plotColumn, plot.plotRow);
            
            if (plotByXY.containsKey(xyPos)) {
                return new DataError(rowIndex, "Duplicate Column,Row: " + xyPos);
            }

            plotByXY.put(xyPos, plot);
            return null;
        }
        @Override
        public Iterator<ImportPlot> iterator() {
            return plotByXY.values().iterator();
        }        
    }
    
}
