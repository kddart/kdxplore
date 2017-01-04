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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.diversityarrays.util.Either;

@SuppressWarnings("nls")
public class EntryFileUtil {

    /**
     * Finds the heading in the lineFields or null if not found
     * @param heading the TrialHeading or
     * @param lineFields array of strings from the line
     * @param roleByColumnIndex
     * @return null or the String heading in the
     */
    static public <H> String getHeadingIfPresent(H heading,
            String[] lineFields,
            Map<Integer, H> roleByColumnIndex)
    {
        Optional<String> opt_s = roleByColumnIndex.entrySet().stream()
                .filter(e -> heading == e.getValue())
                .map(e -> lineFields[e.getKey()])
                .findFirst();
        return opt_s.orElse(null);
    }


    static public <H> Either<String, Set<String>> checkValidity(
            H[] values,
            Map<String, H> roleByHeading,
            Predicate<H> isRequired,
            Predicate<H> isOnlyValidOnce,
            H factor)
    {

        Map<H,List<String>> headingsByType = new HashMap<>();
        for (String h : roleByHeading.keySet()) {
            H ht = roleByHeading.get(h);
            List<String> list = headingsByType.get(ht);
            if (list == null) {
                list = new ArrayList<>();
                headingsByType.put(ht, list);
            }
            list.add(h);
        }

        List<H> requiredNotFound =
                Arrays.asList(values).stream()
                    .filter(h -> isRequired.test(h))
                    .collect(Collectors.toCollection(() -> new ArrayList<>()));

        // Track headings that we find so we can detect duplicate
        // (and hence ambiguous) usage.
        Map<String,H> headingTypeByLowcaseHeading = new HashMap<>();

        // Collect headings detecting required ones and any duplicates
        for (H ht : values) {
            if (isOnlyValidOnce.test(ht)) {
                List<String> headings = headingsByType.get(ht);
                if (headings !=null) {
                    String hdg = headings.get(0); // there is *at least* 1
                    if (headings.size() > 1) {
                        String msg = String.format("Heading '%s' only allowed once",
                                ht.toString());
                        return Either.left(msg);
                    }
                    if (isRequired.test(ht)) {
                        requiredNotFound.remove(ht);
                    }
                    headingTypeByLowcaseHeading.put(hdg.toLowerCase(), ht);
                }
            }
        }

        if (! requiredNotFound.isEmpty()) {
            String msg = requiredNotFound.stream()
                        .map(ht -> ht.toString())
                        .collect(Collectors.joining(", ", "Missing required heading(s): ", ""));
            return Either.left(msg);
        }

        // Now look for those FACTOR headings
        Set<String> lowcaseFactorHeadings = roleByHeading.entrySet()
                .stream()
                .filter(e -> factor == e.getValue())
                .map(Map.Entry::getKey)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String fh : lowcaseFactorHeadings) {
            if (headingTypeByLowcaseHeading.containsKey(fh)) {
                return Either.left(
                        String.format("Factor heading [%s] already in use as [%s]",
                                fh,
                                headingTypeByLowcaseHeading.get(fh.toLowerCase())));

            }
            headingTypeByLowcaseHeading.put(fh.toLowerCase(), factor);
        }

        return Either.right(lowcaseFactorHeadings);
    }
}
