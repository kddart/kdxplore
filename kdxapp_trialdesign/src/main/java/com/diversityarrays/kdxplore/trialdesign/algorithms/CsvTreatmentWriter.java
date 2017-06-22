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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.diversityarrays.kdcompute.db.Plugin;
import com.diversityarrays.kdcompute.db.RunBinding;
import com.diversityarrays.kdxplore.design.DesignEntry;
import com.diversityarrays.kdxplore.trialdesign.TrialEntry;

@SuppressWarnings("nls")
abstract public class CsvTreatmentWriter {

    static public final String TREATMENT_CSV = "treatment.csv";

    static public final String ONE = Integer.toString(1);


	public enum TreatmentHeading {
		TREATMENT("Treatment"),
		TREATMENT_1("Treatment1"),
		TREATMENT_2("Treatment2"),
		REPLICATION("Replicate");

		String inputHeadingName;

		private TreatmentHeading(String inputHeadingName) {
			this.inputHeadingName = inputHeadingName;
		}

		public String getInputHeadingName() {
			return inputHeadingName;
		}

	}



	static final Map<String, List<TreatmentHeading>> HEADINGS_BY_PLUGIN_NAME;
	static {
		Map<String, List<TreatmentHeading>> map = new HashMap<>();

		map.put("kdcp_standalone_trialDesignAlpha",
				Arrays.asList(TreatmentHeading.TREATMENT) );
		map.put("kdcp_standalone_trialDesignBib",
				Arrays.asList(TreatmentHeading.TREATMENT) );
		map.put("kdcp_standalone_trialDesignCrd",
				Arrays.asList(TreatmentHeading.TREATMENT, TreatmentHeading.REPLICATION) );
		map.put("kdcp_standalone_trialDesignCyclic",
				Arrays.asList(TreatmentHeading.TREATMENT) );
		map.put("kdcp_standalone_trialDesignDau",
				Arrays.asList(TreatmentHeading.TREATMENT_1, TreatmentHeading.TREATMENT_2) ); // TODO: agricolaeTrialDesign handles as seperate files, i.e. NewTreatments.csv  Treatments.csv
		map.put("kdcp_standalone_trialDesignGraeco",
				Arrays.asList(TreatmentHeading.TREATMENT_1, TreatmentHeading.TREATMENT_2) );
		map.put("kdcp_standalone_trialDesignLatinSquare",
				Arrays.asList(TreatmentHeading.TREATMENT) );
		map.put("kdcp_standalone_trialDesignLattice",
				Arrays.asList(TreatmentHeading.TREATMENT) );
		map.put("kdcp_standalone_trialDesignRcbd",
				Arrays.asList(TreatmentHeading.TREATMENT) );
		map.put("kdcp_standalone_trialDesignStrip",
				Arrays.asList(TreatmentHeading.TREATMENT_1,TreatmentHeading.TREATMENT_2) );
		map.put("kdcp_standalone_trialDesignYouden",
				Arrays.asList(TreatmentHeading.TREATMENT) );

		HEADINGS_BY_PLUGIN_NAME = Collections.unmodifiableMap(map);
	}

	public static List<TreatmentHeading> getTreatmentHeadings(String pluginName) {
		return HEADINGS_BY_PLUGIN_NAME.get(pluginName);
	}

	abstract protected void prepareWriter(File outputFile) throws IOException;

	abstract protected void writeLine(String[] line);

	abstract protected void closeWriter() throws IOException;

	// These two methods ensure that we are using the same transform
	static public String getTreatmentName(TrialEntry e) {
	    return String.valueOf(e.getSequence());
	}

	static public Optional<TrialEntry> fromTreatmentName(String treatmentName, Map<Integer, TrialEntry> entryBySequence) {
	    try {
	        return Optional.ofNullable(entryBySequence.get(Integer.valueOf(treatmentName)));
        }
        catch (NumberFormatException e) {
            return Optional.empty();
        }
	}

	public void writeEntries(RunBinding runBinding, List<TrialEntry> entries, File outputFile)
			throws IOException
	{
		Plugin plugin = runBinding.getPlugin();
		String pluginName = plugin.getPluginName();
		List<TreatmentHeading> headings = HEADINGS_BY_PLUGIN_NAME.get(pluginName);
		if (headings != null) {

			if(headings.contains(TreatmentHeading.REPLICATION)) {
				Optional<TrialEntry> opt = entries.stream()
						.filter((e) -> e.getReplication().orElse(0) <= 0)
						.findFirst();

				if (opt.isPresent()) {
					throw new IOException("Must have non-zero replicates for all Entries");
				}
			}

			if (headings.contains(TreatmentHeading.TREATMENT_2)) {
				throw new IOException("Double Treatment not yet supported");
			}

			prepareWriter(outputFile);
			try {
				List<String> header = headings.stream().map(h -> h.getInputHeadingName()).collect(Collectors.toList());
				writeLine(header.toArray(new String[header.size()]));

				Consumer<TrialEntry> entryToLine;
				if (headings.size()==2 && headings.contains(TreatmentHeading.TREATMENT) && headings.contains(TreatmentHeading.REPLICATION)) {
				    String[] line = new String[2];
                    entryToLine = new Consumer<TrialEntry>() {
                        @Override
                        public void accept(TrialEntry e) {
                            Optional<Integer> opt = e.getReplication();

                            line[0] = getTreatmentName(e);
                            line[1] = opt.isPresent() ? opt.get().toString() : ONE;
                            writeLine(line);
                        }
                    };
				} else if(headings.size()==1 && headings.contains(TreatmentHeading.TREATMENT)) {
                    String[] line = new String[1];
                    entryToLine = new Consumer<TrialEntry>() {
                        @Override
                        public void accept(TrialEntry e) {
                            line[0] = getTreatmentName(e);
                            writeLine(line);
                        }
                    };
				} else {
				    throw new RuntimeException("Unsupported header set:"+headings);
				}
				entries.stream().forEach(entryToLine);
			}
			finally {
				closeWriter();
			}
		}
		else {
			String[] entryInfo = new String[1];

			prepareWriter(outputFile);
			try {
				for (DesignEntry entry :  entries) {
					entryInfo[0] = entry.getEntryName();
					//                entryInfo[1] = entry.getExperiment();
					//                entryInfo[2] = entry.getLocation();
					//                entryInfo[3] = entry.getNesting();
					//                entryInfo[4] = entry.getEntryType() == null ? null : entry.getEntryType().toString();

					writeLine(entryInfo);
				}
			}
			finally {
				closeWriter();
			}
		}
	}

}
