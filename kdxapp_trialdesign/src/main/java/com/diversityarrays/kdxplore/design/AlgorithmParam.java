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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.diversityarrays.kdcompute.db.KnobBinding;
import com.diversityarrays.util.Check;

@SuppressWarnings("nls")
public abstract class AlgorithmParam<T> {

    static public final AlgorithmParam<Integer> REPLICATE_COUNT =
            new IntegerParam("nrep", "replicate");

    public final Set<String> strings;

    public AlgorithmParam(Class<T> tclass, String ... strings) {
        this.strings = Collections.unmodifiableSet(Arrays.asList(strings).stream()
                .map(s -> s.toLowerCase())
                .collect(Collectors.toSet()));
    }
    
    abstract public Optional<T> getValue(KnobBinding knobBinding);
    
    static class IntegerParam extends AlgorithmParam<Integer> {
        IntegerParam(String ... strings) {
            super(Integer.class, strings);
        }

        @Override
        public Optional<Integer> getValue(KnobBinding kb) {
            Integer result = null;
            String v = kb.getKnobValue();
            if (Check.isEmpty(v)) {
                v = kb.getKnob().getDefaultValue();
            }
            try {
                int r = Integer.parseInt(v, 10);
                if (r > 0) {
                    result = r;
                }
            }
            catch (NumberFormatException e) {
            }
            return result == null ? Optional.empty() : Optional.of(result);
        }
    }
}
