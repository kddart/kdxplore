/**
 *
 */
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
package com.diversityarrays.kdxplore.trialdesign;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.diversityarrays.kdcompute.db.TrialDesignOutput;
import com.diversityarrays.kdxplore.fieldlayout.DesignParams;
import com.diversityarrays.kdxplore.trialdesign.algorithms.CsvTreatmentWriter;
import com.diversityarrays.util.ListByOne;
import com.diversityarrays.util.XYPos;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author alexs, brian
 *
 */
public class AlgorithmRunResult {

    private ListByOne<Integer, PositionedDesignEntry<TrialEntry>> positionedEntriesByReplicate = new ListByOne<>();

	private TrialDesignOutput trialDesignOutput;

	private final String algorithmName;
	private final File algorithmFolder;

	public AlgorithmRunResult(String algname, File algorithmFolder) {
	    this.algorithmName = algname;
	    this.algorithmFolder = algorithmFolder;
    }

	public String getAlgorithmName() {
	    return algorithmName;
	}

	public File getAlgorithmFolder() {
	    return algorithmFolder;
	}

    public void addTrialEntries(
			File kdxploreOutputFile,
			List<TrialEntry> userTrialEntries) throws IOException
	{

	    Map<Integer, TrialEntry> entryBySequence = userTrialEntries.stream()
	        .collect(Collectors.toMap(e -> e.getSequence(), Function.identity()));

		CSVReader reader = new CSVReader(new FileReader(kdxploreOutputFile));
		TrialDesignOutput trialDesignOutput = null;

		try {
			String[] fields;
			int lineNumber = 0;
			while (null != (fields = reader.readNext())) {
			    ++lineNumber;
				if (trialDesignOutput == null) {
					// Heading line - check that it is what we expect from
					trialDesignOutput = new TrialDesignOutput(fields);

					Optional<String> opt = trialDesignOutput.getWhyOutputNotUsable();

					if (opt.isPresent()) {
						//Not usable output.
						throw new IOException(opt.get());
					}

                    //Usable output.
				}
				else {
                    Optional<Integer> optReplicate = trialDesignOutput.getReplicate(fields,
                            DesignParams.DEFAULT_SINGLE_REPLICATE_NUMBER);
                    if (! optReplicate.isPresent()) {
                        throw new IOException("Missing field for replicate at line#" + lineNumber);
                    }

                    Integer replicateNumber = optReplicate.get();

					String treatment = fields[trialDesignOutput.getTreatmentIndex()];

					Optional<TrialEntry> optTrialEntry = CsvTreatmentWriter.fromTreatmentName(treatment, entryBySequence);
					if (! optTrialEntry.isPresent()) {
					    throw new IOException(
					            String.format("Entry for: '%s' missing from input Entry list. Algorithm output problem!",
					                    treatment));
					}

					TrialEntry trialEntry = optTrialEntry.get();
                    XYPos xypos = trialDesignOutput.getXyPos(fields);
                    Optional<Integer> blockIndex = trialDesignOutput.getBlockIndex();

                    PositionedDesignEntry<TrialEntry> p = new PositionedDesignEntry<>(
                            replicateNumber,
                            xypos,
                            blockIndex.orElse(null),
                            trialEntry);

                    positionedEntriesByReplicate.addKeyValue(replicateNumber, p);
				}
			}

		} catch (IOException | NumberFormatException | IndexOutOfBoundsException e) {
			e.printStackTrace();
			throw e;

		} finally {
			this.trialDesignOutput = trialDesignOutput;
			reader.close();
		}
	}

	public TrialDesignOutput getTrialDesignOutput() {
		return trialDesignOutput;
	}

	public Optional<List<PositionedDesignEntry<TrialEntry>>> getOutputEntries(int replicate) {
		return positionedEntriesByReplicate.get(replicate);
	}

	public ListByOne<Integer, PositionedDesignEntry<TrialEntry>> getOutputEntries() {
	    // Beware - modifiable !
		return positionedEntriesByReplicate;
	}

}
