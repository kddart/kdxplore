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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.diversityarrays.kdsmart.db.KDSmartDbUtil;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.util.Either;

import net.pearcan.excel.ExcelUtil;

@SuppressWarnings("nls")
class TrialWorksheet extends KdxploreWorksheet {
    
    static private final List<ImportField> TRIAL_FIELDS = new ArrayList<>();

    static {
        TRIAL_FIELDS.add(new ImportField(Trial.class, "trialName", "Trial Name"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "trialAcronym", "Abbreviation"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "trialPlantingDate", "Planting Date"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "traitNameStyle", "Trait Name Style"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "trialNote", "Note"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "nameForColumn", "Column Name"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "nameForRow", "Row Name"));
        TRIAL_FIELDS.add(new ImportField(Trial.class, "nameForPlot", "Plot Name"));
    }
    
    public TrialWorksheet() {
        super(new WorksheetInfo(WorksheetId.TRIAL, TRIAL_FIELDS));
    }
    
    @Override
    public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr) {
        
        int nRows = ExcelUtil.getRowCount(sheet);
        for (int rowIndex = 0; rowIndex < nRows; ++rowIndex) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            
            int nCells = Math.min(2, ExcelUtil.getCellCount(row));
            String name = "";
            String value = "";
            for (int cellIndex = 0; cellIndex < nCells; ++cellIndex) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null) {
                    String s = ExcelUtil.getCellStringValue(cell, "");
                    if (cellIndex == 0) {
                        name = s;
                    }
                    else if (cellIndex == 1) {
                        value = s;
                    }
                }
            }
            
            ImportField importField = worksheetInfo.getFieldForHeading(name);
            if (importField == null) {
                TrialAttribute ta = new TrialAttribute();
                ta.setTrialAttributeName(name);
                ta.setTrialAttributeValue(value);
                wrr.trialAttributes.add(ta);
            }
            else {
                Either<String, Object> either = KDSmartDbUtil.convertValueOrError(Trial.class, importField.field, value);
                if (either.isLeft()) {
                    return new DataError(rowIndex, either.left());
                }

                try {
                    importField.field.set(wrr.trial, either.right());
                }
                catch (IllegalArgumentException | IllegalAccessException e) {
                    String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                    return new DataError(rowIndex, msg);
                }
            }
        }
        return null;
    }
}
