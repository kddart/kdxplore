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
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;

import com.diversityarrays.kdsmart.db.entities.ParsedTraitName;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitNameAndInstanceParser;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

@SuppressWarnings("nls")
class PlotsWorksheet extends KdxploreWorksheet {
    
    static private final List<ImportField> PLOT_FIELDS = new ArrayList<>();

    static {
        PLOT_FIELDS.add(new ImportField(ImportPlot.class, "userPlotId", "Plot Id"));
        PLOT_FIELDS.add(new ImportField(ImportPlot.class, "plotColumn", "Plot Column"));
        PLOT_FIELDS.add(new ImportField(ImportPlot.class, "plotRow", "Plot Row"));
        PLOT_FIELDS.add(new ImportField(ImportPlot.class, "plotType", "Plot Type"));
        PLOT_FIELDS.add(new ImportField(ImportPlot.class, "barcode", "Barcode"));
    }

    public PlotsWorksheet() {
        super(new WorksheetInfo(WorksheetId.PLOTS, PLOT_FIELDS));
    }

    @Override
    public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr) {

        TraitNameStyle tns = wrr.trial.getTraitNameStyle();
        
        TraitNameAndInstanceParser traitNameAndInstanceParser = new TraitNameAndInstanceParser(tns);

        EntityProcessor<ImportPlot> entityProcessor = new EntityProcessor<ImportPlot>() {
            @Override
            public DataError handleRemainingColumns(int rowIndex, ImportPlot plot,
                    HeadingRow headingRow, RowData rowData, WorkbookReadResult wrr) 
            {
                for (Integer cellIndex : rowData.cellValuesByCellIndex.keySet()) {
                    String cellValue = rowData.cellValuesByCellIndex.get(cellIndex);
                    
                    String hdg = headingRow.headingByColumnIndex.get(cellIndex);
                    if (Check.isEmpty(hdg)) {
                        continue;
                    }

                    PlotAttribute pa = wrr.getPlotAttributeByName(hdg);
                    if (pa != null) {
                        // Ok. We're at a PlotAttribute column
                        plot.plotAttributeValueByName.put(pa.getPlotAttributeName(), cellValue);
                    }
                    else {
                        // Heading is NOT a PlotAttribute name, assume it is a Trait
                        
                       ParsedTraitName parsedTraitName = traitNameAndInstanceParser.parseNameAndInstanceNumber(hdg);
                       if (parsedTraitName.specimenNumber > 0) {
                           return new DataError(rowIndex, "Specimen number suffix not valid in Plots worksheet");
                       }

                        String traitName = parsedTraitName.traitName;
                        Integer instanceNumber = parsedTraitName.instanceNumber;
                        
                        Either<DataError,Trait> either = wrr.getPlotLevelTrait(rowIndex, traitName);
                        if (either.isLeft()) {
                            return either.left();
                        }
                        
                        Trait trait = either.right();
                        Pair<Integer,String> tiValue = new Pair<>(instanceNumber, cellValue);
                        plot.traitInstanceValueByName.put(trait.getTraitName(), tiValue);
                    }
                }
                
                return null;
            }

            @Override
            public Either<DataError, ImportPlot> createEntity(Integer rowIndex) {
                ImportPlot p = new ImportPlot();
                DataError error = wrr.addPlot(rowIndex, p);
                if (error != null) {
                    return Either.left(error);
                }
                return Either.right(p);
            }
        };
        return processWorksheet(sheet, ImportPlot.class, entityProcessor, wrr);
    }
}
