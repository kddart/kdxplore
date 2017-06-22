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
package com.diversityarrays.kdcompute.db;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.diversityarrays.util.XYPos;

public class TrialDesignOutput {

    private static final String TREATMENT = "treatment";
    private static final String TREATMENT_1 = "treatment1";
    private static final String TREATMENT_2 = "treatment2";
    private static final String ROW = "row";
    private static final String COLUMN = "column";
    private static final String BLOCK =  "block";
    private static final String REPLICATE = "replicate";

    private static final String[] COLUMNS = {
    		TREATMENT,
    		TREATMENT_1,
    		TREATMENT_2,
    		ROW,
    		COLUMN,
    		BLOCK,
    		REPLICATE
    };

    private static final String[] MANDATORY_COLUMNS = {
    		TREATMENT,
    		ROW,
    		COLUMN
    };

    private final Map<String,Optional<Integer>> indexByHeading = new HashMap<>();

    private Set<String> missingHeadings = new HashSet<>();

    public TrialDesignOutput(String[] headings) {

    	if (headings.length > 0) {
    		List<String> headingList = Arrays.asList(headings);

    		List<String> foundHeadings = headingList.stream()
    				.map(String::toLowerCase)
    				.collect(Collectors.toList());

    		List<String> mandatoryColumns = Arrays.asList(MANDATORY_COLUMNS).stream()
    				.map(String::toLowerCase)
    				.collect(Collectors.toList());

    		List<String> columns = Arrays.asList(COLUMNS).stream()
    				.map(String::toLowerCase)
    				.collect(Collectors.toList());

    		int i = 0;
    		for (String col : columns) {
    			boolean columnPresent = foundHeadings.stream().anyMatch(s -> s.equals(col));
   				indexByHeading.put(col, columnPresent ? Optional.of(i) : Optional.empty());
    			if (mandatoryColumns.contains(col) && ! columnPresent) {
    				missingHeadings.add(col);
    			}
    			i+= columnPresent ? 1 : 0;
    		}
    	} else {
    		missingHeadings.addAll(Arrays.asList(MANDATORY_COLUMNS));
    	}
    }

    public Optional<String> getWhyOutputNotUsable() {
    	if (! missingHeadings.isEmpty()) {
    		String msg = missingHeadings.stream()
    		    .collect(Collectors.joining(", ",
    		            "The following mandatory headings are missing from the output file: \n",
    		            ".\n Is the plugin algorithm you are currently using applicable for use KDXplore?"));
    		return Optional.of(msg);
    	}
    	return Optional.empty();
    }

    public Map<String,Optional<Integer>> getHeadingsByIndex() {
    	return indexByHeading;
    }

    public int getTreatmentIndex() {
        return getMandatoryInt(TREATMENT);
    }

    public int getRowIndex() {
        return getMandatoryInt(ROW);
    }

    public int getColumnIndex() {
        return getMandatoryInt(COLUMN);
    }

    private int getMandatoryInt(String hdg) {
        Optional<Integer> opt = indexByHeading.get(hdg);
        if (! opt.isPresent()) {
            throw new IllegalStateException(hdg + " is supposed to be mandatory");
        }
        return opt.get().intValue();
    }

    public Optional<Integer> getBlockIndex() {
    	return indexByHeading.get(BLOCK);
    }

    public Optional<Integer> getReplicate(String[] fields, int defaultValue) {
        Integer replicate = defaultValue;
    	Optional<Integer> opt = indexByHeading.get(REPLICATE);
    	if (opt != null && opt.isPresent()) {
    	    int index = opt.get();
    	    if (index < fields.length) {
    	        try {
    	            replicate = Integer.valueOf(fields[index]);
    	        }
    	        catch (NumberFormatException e) {
    	            replicate = null;
    	        }
    	    }
    	}
    	return Optional.ofNullable(replicate);
    }

    public Optional<Integer> getTreatmentOneIndex() {
    	//TODO - Multiple treatment file handling.
    	throw new RuntimeException("Multiple Treatment file input not yet implemented.");
    }

    public Optional<Integer> getTreatmentTwoIndex() {
    	//TODO - Multiple treatment file handling.
    	throw new RuntimeException("Multiple Treatment file input not yet implemented.");
    }

    /**
     *
     * @param fields
     * @return
     * @throws NumberFormatException
     */
    public XYPos getXyPos(String[] fields) throws NumberFormatException {
        int x = Integer.parseInt(fields[getColumnIndex()]);
        int y = Integer.parseInt(fields[getRowIndex()]);
        return new XYPos(x, y);
    }


}
