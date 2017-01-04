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
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.util.Check;

import au.com.bytecode.opencsv.CSVReader;

public class CsvRowDataProvider implements RowDataProvider {
    
    private CSVReader csvReader;

    private int lineNumber = 0;
    private String[] headings;
    private String[] firstDataRow;
    private boolean haveSeenFirstDataRow;

    private final File file;
    
    public CsvRowDataProvider(File file) throws IOException {
        this.file = file;
        reset();
    }
    
    @Override
    public void reset() throws IOException {
        csvReader = new CSVReader(new FileReader(file));
        
        String[] line;
        while (null != (line = csvReader.readNext())) {
            if (line.length > 0 && ! Check.isEmpty(line[0])) {
                headings = line;
                break;
            }
        }
        
        if (headings == null) {
            throw new IOException("File only has blank lines");
        }
    }
    
    @Override
    public String getDataSourceName() {
        return file.getPath();
    }

    @Override
    public Optional<String[]> getHeadings() {
        return Optional.ofNullable(headings);
    }
    
    @Override
    public Optional<String[]> getFirstDataRow() {
        if (firstDataRow==null) {
            if (! haveSeenFirstDataRow) {
                try {
                    firstDataRow = csvReader.readNext();
                }
                catch (IOException e) {
                    Shared.Log.w("CsvRowDataProvider", "getFirstDataRow", e);
                }
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
            return csvReader.readNext();
        }
        haveSeenFirstDataRow = true;
        firstDataRow = csvReader.readNext();
        return firstDataRow;
    }

    // Closeable
    @Override
    public void close() throws IOException {
        if (csvReader != null) {
            try {
                csvReader.close();
            }
            finally {
                csvReader = null;
            }
        }
    }
    
}
