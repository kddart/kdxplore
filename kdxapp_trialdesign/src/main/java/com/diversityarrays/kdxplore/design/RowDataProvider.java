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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

public interface RowDataProvider extends Closeable {

    void reset() throws IOException;

    /**
     * Return the name of the input source
     * @return
     */
    String getDataSourceName();

    /**
     * Return the row of headings data.
     * @return
     */
    Optional<String[]> getHeadings();

    /**
     * Return first data row
     * @return
     */
    Optional<String[]> getFirstDataRow();

    /**
     * Return the lineNumber of the last call to getNextRowData();
     * @return
     */
    int getLineNumber();
    /**
     * Return the next array of row data or null.
     * @return null if no more rows of data
     * @throws IOException
     */
    String[] getNextRowData() throws IOException;


}
