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
package com.diversityarrays.kdxplore.trialdesign.algorithms;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.diversityarrays.util.Check;

public class UnQuotedCsvTreatmentWriter extends CsvTreatmentWriter {

    PrintWriter writer;
    @Override
    protected void prepareWriter(File outputFile) throws IOException {
        if (writer != null) {
            throw new IllegalStateException("previous writer still extant");
        }
        writer = new PrintWriter(outputFile);
    }

    @Override
    protected void writeLine(String[] line) {
        if (writer == null) {
            throw new IllegalStateException("prepareWriter() must be called before writeLine()");
        }
        boolean first = true;
        for (String s : line) {
            if (first) {
                first = false;
            }
            else {
                writer.print(',');
            }

            if (! Check.isEmpty(s)) {
                writer.print(s);
            }
        }
        writer.println();
    }

    @Override
    protected void closeWriter() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }


}
