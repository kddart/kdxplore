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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

@SuppressWarnings("nls")
class WorksheetInfo {
    
    public final WorksheetId worksheetId;
    private final List<ImportField> importFields;
    private final Map<String, ImportField> importFieldByHeading = new HashMap<>();
    
    public WorksheetInfo(WorksheetId wsid, List<ImportField> list) {
        worksheetId = wsid;
        importFields = list;
        for (ImportField f : importFields) {
            importFieldByHeading.put(f.wsHeading.toLowerCase(), f);
        }
    }
    
    @Override
    public String toString() {
        return "WorksheetInfo[" + worksheetId.name() + "]";
    }
    
    public ImportField getFieldForHeading(String h) {
        return importFieldByHeading.get(h.toLowerCase());
    }

    /**
     * Scan the Row in the sheet and return a list of problem column numbers
     * (1 based) or the details discovered that will be used for the rest of
     * the import.
     * @param row
     * @return
     */
    public Either<List<Pair<Integer,String>>, HeadingRow> scanHeadingRow(List<String> cellValues) {
        
        List<Pair<Integer,String>> duplicates = new ArrayList<>();
        
        Set<String> headingsFound = new HashSet<>();
        
        HeadingRow result = new HeadingRow();
        
        int nCells = cellValues.size();
        
        for (int cellIndex = 0; cellIndex < nCells; ++cellIndex) {
            String hdg = cellValues.get(cellIndex);
            if (! Check.isEmpty(hdg)) {
                if (headingsFound.add(hdg.toLowerCase())) {
                    ImportField importField = getFieldForHeading(hdg);
                    if (importField == null) {
                        result.headingByColumnIndex.put(cellIndex, hdg);
                    }
                    else {
                        result.importFieldByColumnIndex.put(cellIndex, importField);
                    }
                }
                else {
                    duplicates.add(new Pair<>(cellIndex+1,hdg));
                }
            }
        }
        if (! duplicates.isEmpty()) {
            return Either.left(duplicates);
        }
        return Either.right(result);
    }

    /**
     * The result may be modified.
     * @return
     */
    public Set<ImportField> getRequiredFields(Predicate<ImportField> filter) {
        Set<ImportField> result = new LinkedHashSet<>();
        for (ImportField f : importFields) {
            boolean include = false;
            if (filter == null) {
                include = ! KdxploreDatabase.Util.canBeNull(f.field);
            }
            else {
                include = filter.test(f);
            }

            if (include) {
                result.add(f);
            }
        }
        return result;
    }

    public RowData collectImportFields(
            Map<Integer, ImportField> fieldByColumnIndex, 
            List<String> cellValues) 
    {
        RowData rowData = new RowData();
        
        int nCells = cellValues.size();
        
        for (int cellIndex = 0; cellIndex < nCells; ++cellIndex) {

            String value = cellValues.get(cellIndex);
            
            ImportField importField = fieldByColumnIndex.get(cellIndex);
            if (importField == null) {
                rowData.cellValuesByCellIndex.put(cellIndex, value);
            }
            else {
                rowData.importFieldsAndCellValues.add(new Pair<>(importField, value));
            }
        }

        return rowData;
    }
}
