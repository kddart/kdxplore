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
package com.diversityarrays.kdxplore.design;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.diversityarrays.util.Check;

import net.pearcan.excel.ExcelUtil;

@SuppressWarnings("nls")
public class ExcelRowDataProvider implements RowDataProvider {
    
    static private final String[] EMPTY = new String[0];
    
    private boolean isClosed;
    private final Workbook workbook;
    private final Sheet sheet;
    private final int rowCount;
    private int rowIndex;
    private String[] headings;
    private String[] firstDataRow;
    private boolean haveSeenFirstDataRow;
    private int lineNumber;
    private final String dataSourceName;
    
    public ExcelRowDataProvider(File file, String sheetName) throws IOException {
        
        dataSourceName = file.getPath() + "[" + sheetName + "]";
        
        workbook = ExcelUtil.getWorkbook(file.getName(), file);
    
        sheet = workbook.getSheet(sheetName);
        rowCount = ExcelUtil.getRowCount(sheet);

        reset();
    }
    
    @Override
    public void reset() throws IOException {
        rowIndex = -1;
        String[] fields;
        while (null != (fields = nextLine())) {
            if (fields.length > 0) {
                headings = fields;
                break;
            }
        }
    }
    
    @Override
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Return null at EOF.
     * @return
     */
    private String[] nextLine() {
        String[] result = null;

        DecimalFormat dfmt = new DecimalFormat("#.000");
        if ((rowIndex+1) < rowCount) {
            result = EMPTY;
            ++rowIndex;
            lineNumber = rowIndex + 1;
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                int nCells = ExcelUtil.getCellCount(row);
                if (nCells > 0) {
                    List<String> list = new ArrayList<>();
                    boolean anyNonBlank = false;
                    for (int colIndex = 0; colIndex < nCells; ++colIndex) {
                        Cell cell = row.getCell(colIndex);
                        String v = null;
                        if (cell != null) {
                            v = ExcelUtil.getCellStringValue(cell, "", dfmt);
                        }
                        
                        if (Check.isEmpty(v)) {
                            list.add("");
                        }
                        else {
                            anyNonBlank = true;
                            list.add(v);
                        }
                    }
                    if (! list.isEmpty() && anyNonBlank) {
                        result = list.toArray(new String[list.size()]);
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public Optional<String[]> getHeadings() {
        return Optional.ofNullable(headings);
    }

    @Override
    public Optional<String[]> getFirstDataRow() {
        if (firstDataRow==null) {
            if (! haveSeenFirstDataRow) {
                firstDataRow = nextLine();
            }
        }
        return Optional.ofNullable(firstDataRow);
    }
    
    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String[] getNextRowData() throws IOException {
        if (haveSeenFirstDataRow) {
            return nextLine();
        }
        haveSeenFirstDataRow = true;
        firstDataRow = nextLine();
        return firstDataRow;
    }

    @Override
    public void close() throws IOException {
        if (! isClosed) {
            try {
                if (workbook != null) {
                    workbook.close();
                }
            }
            finally {
                isClosed = true;
            }
        }            
    }
}
