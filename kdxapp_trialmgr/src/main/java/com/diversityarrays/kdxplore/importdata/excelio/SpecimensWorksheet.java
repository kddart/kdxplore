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
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitNameAndInstanceParser;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

@SuppressWarnings("nls")
class SpecimensWorksheet extends KdxploreWorksheet {
    
    static private final List<ImportField> SPECIMEN_FIELDS = new ArrayList<>();
    static {
        SPECIMEN_FIELDS.add(new ImportField(ImportSpecimen.class, "userPlotId", "Plot Id"));
        SPECIMEN_FIELDS.add(new ImportField(ImportSpecimen.class, "plotColumn", "Plot Column"));
        SPECIMEN_FIELDS.add(new ImportField(ImportSpecimen.class, "plotRow", "Plot Row"));
        SPECIMEN_FIELDS.add(new ImportField(ImportSpecimen.class, "specimenNumber", "Specimen Number"));
        SPECIMEN_FIELDS.add(new ImportField(ImportSpecimen.class, "barcode", "Barcode"));
    }

    public SpecimensWorksheet() {
        super(new WorksheetInfo(WorksheetId.SPECIMENS, SPECIMEN_FIELDS));
    }

    @Override
    public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr) {
        
        TraitNameStyle tns = wrr.trial.getTraitNameStyle();
        TraitNameAndInstanceParser traitNameAndInstanceParser = new TraitNameAndInstanceParser(tns);

        EntityProcessor<ImportSpecimen> entityProcessor = new EntityProcessor<ImportSpecimen>() {
            @Override
            public DataError handleRemainingColumns(int rowIndex, ImportSpecimen specimen,
                    HeadingRow headingRow, RowData rowData, WorkbookReadResult wrr) 
            {
                if (specimen.specimenNumber == null) {
                    return new DataError(rowIndex, "Missing Specimen Number");
                }

                Either<DataError,ImportPlot> either = wrr.addSpecimenToPlot(rowIndex, specimen);
                if (either.isLeft()) {
                    return either.left();
                }
                ImportPlot plot = either.right();
                // TODO something with the plot?
                
                for (Integer cellIndex : rowData.cellValuesByCellIndex.keySet()) {
                    String cellValue = rowData.cellValuesByCellIndex.get(cellIndex);
                    
                    String hdg = headingRow.headingByColumnIndex.get(cellIndex);
                    if (Check.isEmpty(hdg)) {
                        continue;
                    }
                    
                    ParsedTraitName parsedTraitName = traitNameAndInstanceParser.parseNameAndInstanceNumber(hdg);
                    if (parsedTraitName.specimenNumber > 0) {
                        return new DataError(rowIndex, "Specimen number suffix should not be used in Specimen worksheet");
                    }
                    String traitName = parsedTraitName.traitName;
                    Integer instanceNumber = parsedTraitName.instanceNumber;
                    
                    // All other Headings must be Specimen Traits
                    Either<DataError,Trait> either2 = wrr.getSpecimenLevelTrait(rowIndex, traitName);
                    if (either2.isLeft()) {
                        return either2.left();
                    }

                    Trait trait = either2.right();
                    Pair<Integer, String> tiValue = new Pair<>(instanceNumber, cellValue);
                    specimen.traitInstanceValueByName.put(trait.getTraitName(), tiValue);
                }
                
                return null;
            }

            @Override
            public Either<DataError, ImportSpecimen> createEntity(Integer rowIndex) {
                ImportSpecimen spec = new ImportSpecimen();
                return Either.right(spec);
            }
        };
        
        return processWorksheet(sheet, ImportSpecimen.class, entityProcessor, wrr);
    }
    
}
