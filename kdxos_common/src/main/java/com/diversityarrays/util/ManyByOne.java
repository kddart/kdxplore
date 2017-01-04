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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Generic version of {@link ListByOne} - see that class for example of usage.
 * @author brianp
 *
 * @param <K> the key class
 * @param <V> the value class
 * @param <VC> the value "collection" class
 */
public class ManyByOne<K,V,VC> {

    protected final Map<K, VC> map;
    private final Supplier<VC> vcSupplier;
    private final BiPredicate<V, VC> adder;
    private final BiPredicate<VC, V> remover;

    public ManyByOne(Map<K, VC> init,
            Supplier<VC> vcSupplier,
            BiPredicate<V, VC> adder,
            BiPredicate<VC, V> remover)
    {
        this.map = init;
        this.vcSupplier = vcSupplier;
        this.adder = adder;
        this.remover = remover;
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public VC getValue(K k) {
        return map.get(k);
    }

    public Set<Map.Entry<K, VC>> entrySet() {
        return map.entrySet();
    }

    public Collection<VC> values() {
        return map.values();
    }

    public void clear() {
        map.clear();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Optional<VC> get(K key) {
        return Optional.ofNullable(map.get(key));
    }

    /**
     * Retrieve the collection for the key, creating
     * a new one if it doesn't yet exist.
     * @param key
     * @return
     */
    public VC addKey(K key) {
        VC valueColl = map.get(key);
        if (valueColl == null) {
            valueColl = vcSupplier.get();
            map.put(key, valueColl);
        }
        return valueColl;
    }
    /**
     * Adds a new value to the mapping for the key
     * and returns the collection for that key.
     * @param key
     * @param value is added to the collection
     * @return true if the element was added (i.e. was not previously present)
     */
    public boolean addKeyValue(K key, V value) {
        VC valueColl = addKey(key);
        return adder.test(value, valueColl);
    }

    public VC removeKey(K key) {
        return map.remove(key);
    }

    public boolean removeKeyValue(K key, V value) {
        if (remover == null) {
            throw new UnsupportedOperationException();
        }
        boolean result = false;
        Optional<VC> opt = get(key);
        if (opt.isPresent()) {
            result = remover.test(opt.get(), value);
        }
        return result;
    }
}
