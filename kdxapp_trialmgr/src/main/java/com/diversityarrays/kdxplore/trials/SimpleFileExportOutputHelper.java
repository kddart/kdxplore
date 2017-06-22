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
package com.diversityarrays.kdxplore.trials;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import com.diversityarrays.kdsmart.db.FileUtility;
import com.diversityarrays.kdsmart.db.util.CsvWriter;
import com.diversityarrays.kdsmart.db.util.CsvWriterImpl;
import com.diversityarrays.kdsmart.db.util.ExportFor;
import com.diversityarrays.kdsmart.db.util.ExportOutputHelper;
import com.diversityarrays.util.Pair;

class SimpleFileExportOutputHelper implements ExportOutputHelper<File, File> {

	private final File outputFolder;
	private final String fileName;
	private File lastOutfile;

	SimpleFileExportOutputHelper(File outfile) {
		this.outputFolder = outfile.getParentFile();
		this.fileName = outfile.getName();
	}

	@Override
	public Pair<File, CsvWriter> createOutputCsvFileAndWriterForPlot() throws IOException {
    	String name = fileName.toLowerCase().endsWith(".csv")
    			? fileName
    			: fileName + ".csv";
        File outfile = new File(outputFolder, name);
        CsvWriter csvWriter = new CsvWriterImpl(new FileWriter(outfile));
        return new Pair<>(outfile,csvWriter);
	}

    @Override
    public Pair<File[], CsvWriter[]> createOutputCsvFileAndWriterForPlotAndSpecimen() throws IOException {
        String plotFileName;
        String specimenFileName;

        if (fileName.toLowerCase().endsWith(".csv")) {
            plotFileName = fileName;
            specimenFileName = fileName.substring(0, fileName.length() - 4)  + SUB_PLOT_SUFFIX + ".csv";
        }
        else {
            plotFileName = fileName + ".csv";
            specimenFileName = fileName  + SUB_PLOT_SUFFIX + ".csv";
        }

        File plotOutputFile = FileUtility.constructOutputFileWithoutOverwriting(new File(outputFolder,plotFileName));
        File specimenOutputFile = FileUtility.constructOutputFileWithoutOverwriting(new File(outputFolder, specimenFileName));

        File files [] = new File[2];
        files[0] = plotOutputFile;
        files[1] = specimenOutputFile;

        lastOutfile = plotOutputFile;
        CsvWriter plotCsvWriter  = new CsvWriterImpl(new FileWriter(plotOutputFile));
        CsvWriter specimenCsvWriter = new CsvWriterImpl(new FileWriter(specimenOutputFile));

        CsvWriter [] csvWriters = new CsvWriter[2];
        csvWriters[0]= plotCsvWriter;
        csvWriters[1]= specimenCsvWriter;

        Pair<File[], CsvWriter[]>  pair = new Pair<>(files,csvWriters);

        return pair;
    }

	@Override
	public File createOutputZipFile() throws IOException {
        String zipFilename = fileName.toLowerCase().endsWith(".zip")
        		? fileName
        		: fileName + ".zip";
        return new File(outputFolder, zipFilename);
	}

	@Override
	public File createOutputKdxchangeFile() {
        String kdxFilename = fileName.toLowerCase().endsWith(ExportFor.KDX_SUFFIX)
        		? fileName
        		: fileName + ExportFor.KDX_SUFFIX;
        return new File(outputFolder, kdxFilename);
	}

	@Override
	public OutputStream getOutputStream(File file) throws IOException {
        return new FileOutputStream(file);
	}

	@Override
	public String getName(File docFile) {
        return docFile.getName();
	}

	@Override
	public void close() throws IOException {
        // No-op
	}
}