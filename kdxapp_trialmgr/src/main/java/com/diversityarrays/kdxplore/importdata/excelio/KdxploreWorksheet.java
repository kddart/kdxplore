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
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.diversityarrays.kdsmart.db.KDSmartDbUtil;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

import net.pearcan.excel.ExcelUtil;

@SuppressWarnings("nls")
abstract class KdxploreWorksheet {

    static interface EntityProcessor<T> {

        Either<DataError, T> createEntity(Integer rowIndex);

        default DataError handleRemainingColumns(int rowIndex, T entity, HeadingRow headingRow, RowData rowData, WorkbookReadResult wrr) {
            return null;
        }
    }

    protected final WorksheetInfo worksheetInfo;

    public KdxploreWorksheet(WorksheetInfo wsi) {
        this.worksheetInfo = wsi;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    abstract public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr);

    //        abstract public DataError handleRemainingColumns(int rowIndex, HeadingRow headingRow, RowData rowData, WorkbookReadResult wrr);

    /**
     * Returns null if all the Cell values in the Row are blank.
     * @param row
     * @return List or null
     */
    static public List<String> getCellValuesIfAnyNonBlank(Row row) {
        List<String> result = new ArrayList<>();

        boolean allBlank = true;
        int nCells = ExcelUtil.getCellCount(row);
        for (int cellIndex = 0; cellIndex < nCells; ++cellIndex) {
            Cell cell = row.getCell(cellIndex);
            String cellValue = "";
            if (cell != null) {
                cellValue = ExcelUtil.getCellStringValue(cell, "");
            }
            result.add(cellValue);
            if (! Check.isEmpty(cellValue)) {
                allBlank = false;
            }
        }

        return allBlank ? null : result;
    }

    protected <T> DataError processWorksheet(
            Sheet sheet, 
            Class<T> tClass,
            EntityProcessor<T> entityProcessor,
            WorkbookReadResult wrr) 
    {
        HeadingRow headingRow = null;

        int nRows = ExcelUtil.getRowCount(sheet);
        for (int rowIndex = 0; rowIndex < nRows; ++rowIndex) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            List<String> cellValues = getCellValuesIfAnyNonBlank(row);
            if (cellValues == null) {
                continue;
            }

            if (headingRow==null) {
                Either<List<Pair<Integer,String>>, HeadingRow> either = worksheetInfo.scanHeadingRow(cellValues);
                if (either.isLeft()) {
                    String errmsg = either.left().stream()
                            .map(pair -> pair.first + ":" + pair.second)
                            .collect(Collectors.joining(","));

                    return new DataError(rowIndex, errmsg);
                }
                headingRow = either.right();
                if (headingRow.importFieldByColumnIndex.isEmpty()) {
                    return new DataError(rowIndex,
                            "No Column Headings found in worksheet '" + sheet.getSheetName() + "'");
                }
            }
            else {
                RowData rowData = worksheetInfo.collectImportFields(headingRow.importFieldByColumnIndex, cellValues);

                Either<DataError, T> pEither = entityProcessor.createEntity(rowIndex);

                if (pEither.isLeft()) {
                    return pEither.left();
                }
                T entity = pEither.right();

                for (Pair<ImportField,String> pair : rowData.importFieldsAndCellValues) {
                    ImportField importField = pair.first;
                    String cellValue = pair.second;

                    Either<String, Object> either = KDSmartDbUtil.convertValueOrError(
                            tClass, importField.field, cellValue);

                    if (either.isLeft()) {
                        return new DataError(rowIndex, either.left());
                    }

                    try {
                        importField.field.set(entity, either.right());
                    }
                    catch (IllegalArgumentException | IllegalAccessException e) {
                        String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                        return new DataError(rowIndex, msg);
                    }
                }

                DataError error = entityProcessor.handleRemainingColumns(rowIndex, entity, headingRow, rowData, wrr);
                if (error != null) {
                    return error;
                }
            }
        }

        return null;
    }
}
