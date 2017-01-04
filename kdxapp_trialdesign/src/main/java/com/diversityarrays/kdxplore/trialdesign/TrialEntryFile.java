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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.design.EntryFactor;
import com.diversityarrays.kdxplore.design.EntryFileException;
import com.diversityarrays.kdxplore.design.EntryFileUtil;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.EntryType.Variant;
import com.diversityarrays.kdxplore.design.RowDataProvider;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.SetByOne;

@SuppressWarnings("nls")
public class TrialEntryFile {


    /**
     * Either.left indicates the error and
     * Either.right provides the set of factor headings in lowerCase.
     * @param roleByHeading
     * @return
     */
    static public Either<String,Set<String>> checkValidity(Map<String, TrialHeading> roleByHeading) {
        return EntryFileUtil.checkValidity(TrialHeading.values(),
                roleByHeading,
                (h) -> h.isRequired(),
                (h) -> h.isOnlyValidOnce(),
                TrialHeading.FACTOR);
    }

    private final List<TrialEntry> trialEntries = new ArrayList<>();

    private List<EntryFactor> entryFactors = new ArrayList<>();

    private List<EntryType> entryTypes = new ArrayList<>();

    private final Map<TrialHeading, String> userHeadings = new HashMap<>();
//    private final TrialEntryFileParams entryFileParams;

    static public final List<Pair<TrialHeading,Predicate<TrialEntry>>> FIELD_CHECKS_BY_HEADING;
    static {
        List<Pair<TrialHeading,Predicate<TrialEntry>>> list = new ArrayList<>();

        list.add(new Pair<>(TrialHeading.ENTRY_ID, (te) -> true ));
        list.add(new Pair<>(TrialHeading.LOCATION, (te) -> ! Check.isEmpty(te.getLocation()) ));
        list.add(new Pair<>(TrialHeading.EXPERIMENT, (te) -> ! Check.isEmpty(te.getExperimentName()) ));
        list.add(new Pair<>(TrialHeading.ENTRY_TYPE, (te) -> null != te.getEntryType() ));
        list.add(new Pair<>(TrialHeading.NESTING, (te) -> ! Check.isEmpty(te.getNesting()) ));
        list.add(new Pair<>(TrialHeading.ENTRY_NAME, (te) -> ! Check.isEmpty(te.getEntryName()) ));

        FIELD_CHECKS_BY_HEADING = Collections.unmodifiableList(list);
    }

    public TrialEntryFile(List<TrialEntry> entries) {
        trialEntries.clear();
        trialEntries.addAll(entries);

        Set<EntryType> typeSet = trialEntries.stream()
                .map(TrialEntry::getEntryType)
                .filter(i -> i != null)
                .collect(Collectors.toSet());
        entryTypes.clear();
        entryTypes.addAll(typeSet);
        Collections.sort(entryTypes);


        Set<EntryFactor> factorSet = new HashSet<>();
        trialEntries.stream().map(TrialEntry::getEntryFactors)
                .forEach(s -> factorSet.addAll(s));
        entryFactors.clear();
        entryFactors.addAll(factorSet);
        Collections.sort(entryFactors);

        userHeadings.clear();
        BiConsumer<TrialHeading, Predicate<TrialEntry>> action = new BiConsumer<TrialHeading, Predicate<TrialEntry>>() {
            @Override
            public void accept(TrialHeading th, Predicate<TrialEntry> pred) {
                Optional<TrialEntry> opt = entries.stream()
                        .filter(pred)
                        .findFirst();
                if (opt.isPresent()) {
                    userHeadings.put(th, th.display);
                }
            }
        };
        FIELD_CHECKS_BY_HEADING.stream()
            .forEach(pair -> action.accept(pair.first, pair.second));
    }

    /**
     *
     * @param rowDataProvider
     * @param nameHeading REQUIRED
     * @param typeHeading OPTIONAL
     * @param nestHeading OPTIONAL
     * @param factorHeadings OPTIONAL
     * @throws IOException
     * @throws EntryFileException
     */
    public TrialEntryFile(TrialEntryFileParams entryFileParams)
    throws IOException, EntryFileException
    {
        String spatialChecksName = TrialDesignPreferences.getInstance().getSpatialEntryName();
        String normalEntryName = TrialDesignPreferences.getInstance().getNormalEntryTypeName();

        Either<String, Set<String>> either = checkValidity(entryFileParams.roleByHeading);
        if (either.isLeft()) {
            throw new EntryFileException(either.left());
        }

        Set<String> lowcaseFactorHeadings = either.right();

        RowDataProvider rowDataProvider = entryFileParams.rowDataProvider;

        Map<TrialHeading,String> singleOccurrenceHeadings = new HashMap<>();
        for (String hdg : entryFileParams.roleByHeading.keySet()) {
            TrialHeading ht = entryFileParams.roleByHeading.get(hdg);
            if (ht.isOnlyValidOnce()) {
                singleOccurrenceHeadings.put(ht, hdg);
            }
        }

        Map<Integer,EntryFactor> entryFactorByColumnIndex = new HashMap<>();
        Map<String,EntryType> entryTypeByLowcaseNameValue = new HashMap<>();

        try {
            String[] headings;
            Map<Integer,TrialHeading> headingTypeByColumnIndex = new HashMap<>();

            Optional<String[]> opt = rowDataProvider.getHeadings();
            if (! opt.isPresent()) {
                throw new IOException("No headings in " + rowDataProvider.getDataSourceName());
            }
            headings = opt.get();
            processHeadingsLine(
                    lowcaseFactorHeadings,
                    rowDataProvider.getLineNumber(),
                    headings,
                    headingTypeByColumnIndex,
                    singleOccurrenceHeadings,
                    entryFactorByColumnIndex);

            int row = 0;

            String experimentHeading = EntryFileUtil.getHeadingIfPresent(TrialHeading.EXPERIMENT, headings, headingTypeByColumnIndex);

            String locationHeading = EntryFileUtil.getHeadingIfPresent(TrialHeading.LOCATION, headings, headingTypeByColumnIndex);

            String entryIdHeading = EntryFileUtil.getHeadingIfPresent(TrialHeading.ENTRY_ID, headings, headingTypeByColumnIndex);

            boolean idsRequired = Check.isEmpty(entryIdHeading);

            SetByOne<String, Integer> entryIdsByExperiment = new SetByOne<>();

            String[] fields;
            while (null != (fields = rowDataProvider.getNextRowData())) {
                row++;

            	// data line
                String location = "";
                String experiment = "";
                String entryName = null;

                // Use sequence if the heading was not provided missing
                Integer entryId = idsRequired ? row : null;

                EntryType entryType = null;
                String nest = "";
                Map<EntryFactor, String> valueByFactor = new HashMap<>();

                Set<Integer> entriesFound = new HashSet<>();

                int columnIndex = -1;
                for (String field : fields) {
                    ++columnIndex;
                    TrialHeading ht = headingTypeByColumnIndex.get(columnIndex);

                    if (ht == null) {
                        continue;
                    }

                    if (TrialHeading.FACTOR != ht) {
                        userHeadings.put(ht, headings[columnIndex]);
                    }

                    switch (ht) {
                    case LOCATION:
                        location = field;
                        break;
                    case EXPERIMENT:
                        experiment = field;
                        break;
                    case FACTOR:
                        EntryFactor factor = entryFactorByColumnIndex.get(columnIndex);
                        valueByFactor.put(factor, field);
                        break;
                    case ENTRY_ID:
                      	try {
                        	entryId = Integer.parseInt(field);
                        	if (! entriesFound.add(entryId)) {
                        		throw new EntryFileException("Duplicate Entry Id at Row: " + row);
                        	}
                        	} catch (NumberFormatException e) {
                        		e.printStackTrace();
                        		throw new EntryFileException("Non-numeric/missing Entry Id at Row: " + row);
                        	}
                      	break;
                    case ENTRY_NAME:
                    	entryName = field;
                    	break;
                    case ENTRY_TYPE:
                    	entryType = entryTypeByLowcaseNameValue.get(field.toLowerCase());
                    	if (entryType == null) {
                    	    Variant v = Variant.CHECK;
                    	    if (spatialChecksName.equalsIgnoreCase(field)) {
                    	        v = Variant.SPATIAL;
                    	    }
                    	    else if (normalEntryName.equalsIgnoreCase(field)) {
                    	        v = Variant.ENTRY;
                    	    }
                    		entryType = new EntryType(field, v);
                    		entryTypeByLowcaseNameValue.put(field.toLowerCase(), entryType);
                    	}
                    		break;
                    	case NESTING:
                    		nest = field;
                    		break;
                    	default:
                    		throw new IllegalStateException("Unhandled HeadingType; " + ht);
                    }

                } // end for each field

                if (entryName == null) {
                    throw EntryFileException.create(rowDataProvider.getLineNumber(), columnIndex,
                            "Missing Required Heading '" + TrialHeading.ENTRY_NAME.display + "'");
                }

                if (locationHeading != null && Check.isEmpty(location)) {
                    throw EntryFileException.create(rowDataProvider.getLineNumber(), columnIndex,
                            "Blank value for column '" + locationHeading + "'");
                }
                if (experimentHeading != null && Check.isEmpty(experiment)) {
                    throw EntryFileException.create(rowDataProvider.getLineNumber(), columnIndex,
                            "Blank value for column '" + experimentHeading + "'");
                }

                if (entryId == null) {
                    throw new EntryFileException("Missing Entry Id at Row: " + row);
                }
                if (entryId <= 0) {
                    throw new EntryFileException("Invalid Entry Id (" + entryId + ") at Row: " + row);
                }

                if (! entryIdsByExperiment.addKeyValue(
                        experiment==null ? "*experiment*" : experiment,
                        entryId))
                {
                    if (experimentHeading == null) {
                        throw new EntryFileException(
                                String.format("Duplicate Entry Id '%d' at Row: %d", entryId, row));
                    }
                    throw new EntryFileException(String.format("Duplicate Experiment(%s) Entry Id '%d' at Row: %d",
                            experiment, entryId, row));
                }

                trialEntries.add(new TrialEntry(location, experiment, entryId, entryName, entryType, nest, valueByFactor));
            } // end while more lines


        }
        finally {
            try { rowDataProvider.close(); } catch (IOException ignore) {}
        }

        entryFactors.clear();
        entryFactors.addAll(entryFactorByColumnIndex.values());
        Collections.sort(entryFactors);

        entryTypes.addAll(entryTypeByLowcaseNameValue.values());
        Collections.sort(entryTypes);
    }

    public Map<TrialHeading, String> getUserHeadings() {
        return Collections.unmodifiableMap(userHeadings);
    }
//    public TrialEntryFileParams getEntryFileParams() {
//        return entryFileParams;
//    }

    public List<EntryType> getEntryTypes() {
        return entryTypes;
    }

    public int getEntryFactorCount() {
        return entryFactors.size();
    }

    public List<EntryFactor> getEntryFactors() {
        return Collections.unmodifiableList(entryFactors);
    }

    public EntryFactor getEntryFactor(int rowIndex) {
        return (rowIndex < 0 || rowIndex >= entryFactors.size()) ? null : entryFactors.get(rowIndex);
    }

    private void processHeadingsLine(
            final Set<String> lowcaseFactorHeadings,
            int lineNumber,
            String[] fields,
            Map<Integer, TrialHeading> headingTypeByColumnIndex,
            Map<TrialHeading,String> singleOccurrenceHeadings,
            Map<Integer, EntryFactor> entryFactorByColumnIndex)
    throws EntryFileException
    {
        Set<TrialHeading> seenHeadingTypes = new HashSet<>();
        Set<String> unfoundFactorHeadings = new HashSet<>(lowcaseFactorHeadings);

        int nFields = fields.length;
        for (int column = 0; column < nFields; ++column) {
            String hdg = fields[column];

            boolean matched = false;
            for (TrialHeading ht : singleOccurrenceHeadings.keySet()) {
                String so_hdg = singleOccurrenceHeadings.get(ht);
                if (so_hdg.equalsIgnoreCase(hdg)) {
                    if (seenHeadingTypes.contains(ht)) {
                        String msg = String.format("Duplicate '%s' heading: [%s]",
                                ht.display, hdg);
                        throw EntryFileException.create(lineNumber, column, msg);
                    }

                    matched = true;
                    seenHeadingTypes.add(ht);
                    headingTypeByColumnIndex.put(column, ht);
                }
            }

            if (! matched) {
                String lo_hdg = hdg.toLowerCase();
                if (lowcaseFactorHeadings.contains(lo_hdg)) {
                    if (! unfoundFactorHeadings.remove(lo_hdg)) {
                        throw EntryFileException.create(lineNumber, column,
                                "Duplicate Factor [" + hdg + "]");
                    }
                    headingTypeByColumnIndex.put(column, TrialHeading.FACTOR);

                    EntryFactor entryFactor = new EntryFactor(hdg);
                    entryFactorByColumnIndex.put(column, entryFactor);
                }
            }
        }
    }

    public List<TrialEntry> getEntries() {
        return Collections.unmodifiableList(trialEntries);
    }
}
