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
package com.diversityarrays.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Provides an implementation of {@link ManyByOne} for a List&lt;V&gt; as the
 * collection class.
 * <br>
 * It replaces the code pattern:
 * <pre>
 * Map&lt;String, List&lt;Integer&gt;&gt; map = new HashMap&lt;&gt;();
 *   :
 * List&lt;Integer&gt; list = map.get(key);
 * if (list == null) {
 *   list = new ArrayList&lt;&gt;();
 *   map.put(key, list);
 * }
 * list.add(value);
 * </pre>
 * with:
 * <pre>
 * ListByOne&lt;String, Integer&gt; listByOne = new ListByOne&lt;&gt;();
 *   :
 * listByOne.add(key, value);
 * </pre>
 * @author brianp
 *
 * @param <K> is the key class
 * @param <V> is the value class
 */
public class ListByOne<K, V> extends ManyByOne<K, V, List<V>> {
    /**
     * Create a new ListByOne.
     */
    public ListByOne() {
        this(new HashMap<>());
    }

    /**
     * Create a new ListByOne with an initial value.
     */
    public ListByOne(Map<K, List<V>> init) {
        super(init,
                () -> new ArrayList<>(),
                (v,vc) -> vc.add(v),
                (vc,v) -> vc.remove(v));
    }

    /**
     * Remove from the ManyByOne map all of the keys
     * that have empty collections.
     * @return
     */
    @Override
    public List<K> clean() {
        List<K> list = map.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        list.stream().
                forEach(k -> map.remove(k));
        return list;
    }

    @Override
    public List<V> removeValues(K key, Predicate<V> predicate) {
        List<V> result = Collections.emptyList();
        Optional<List<V>> opt = get(key);
        if (opt.isPresent()) {
            result = opt.get().stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
            result.stream().forEach(v -> removeKeyValue(key, v));
        }
        return result;
    }

    @Override
    public Optional<V> findFirst(K key, Predicate<V> predicate) {
        Optional<List<V>> optional = get(key);
        if (optional.isPresent()) {
            return optional.get().stream()
                .filter(predicate)
                .findFirst();
        }
        return Optional.empty();
    }

    @Override
	public Optional<V> findAny(K key, Predicate<V> predicate) {
        Optional<List<V>> optional = get(key);
        if (optional.isPresent()) {
            return optional.get().stream()
                .filter(predicate)
                .findAny();
        }
        return Optional.empty();
    }
}
