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
package com.diversityarrays.kdcompute.db.helper;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import com.diversityarrays.kdcompute.db.Knob;
import com.diversityarrays.kdcompute.db.KnobBinding;
import com.diversityarrays.kdcompute.db.Plugin;

@SuppressWarnings("nls")
public class DbUtil {

    /*
     * No instances allowed.
     */
    private DbUtil() {}
    
    static public Throwable doTransaction(EntityManager em, Consumer<EntityTransaction> worker) {
        Throwable error = null;
        EntityTransaction txn = em.getTransaction();
        txn.begin();
        try {
            worker.accept(txn);
        }
        catch (Throwable t) {
            error = t;
        }
        finally {
            if (error == null) {
                txn.commit();
            }
            else {
                txn.rollback();
            }
        }
        return error;
    }
    
    static public void exceptionUnlessAllValueKnobsAreInPlugin(Plugin parentPlugin, Set<KnobBinding> presets) {

        Set<Knob> knobs = presets.stream().map(KnobBinding::getKnob).collect(Collectors.toSet());
        
        // Note: Relies on Knob equality semantics being same name (case-insensitive);
        knobs.removeAll(parentPlugin.getKnobs());
        if (! knobs.isEmpty()) {
            String msg = knobs.stream().map(Knob::getVisibleName).collect(Collectors.joining(","));
            throw new IllegalArgumentException("Some preset Knobs are not in " + parentPlugin.getAlgorithmName() + ": " + msg);
        }
    }
    
    static public <T> void moveElement(List<T> list, int fromPos, int toPos) {
        if (fromPos == toPos) {
            return;
        }
        if (fromPos < toPos) {
            // this is easy
            // ... , FROM_POS , ... , TO_POS: size == N
            T elt = list.remove(fromPos);
            // ... , <gone> , ... , TO_POS  : size == N-1
            // We reduced the size by 1 so ... 
            list.add(toPos-1, elt);
        }
        else {
            // ..., TO_POS, ..., FROM_POS , ...
            T elt = list.remove(fromPos);
            // ..., TO_POS, ..., <gone> , ...
            list.add(toPos, elt);
        }
    }

    static public <T> void appendListInfo(StringBuilder sb, String listName, Collection<T> list, Function<T,String> mapper) {
        sb.append(listName).append('=')
            .append(list.size()).append(":[")
            .append(list.stream().map(mapper).collect(Collectors.joining(",")))
            .append(']');
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static public <T extends Comparable> int compareLists(List<T> a, List<T> b) {
        int diff = Integer.compare(a.size(), b.size());
        if (diff == 0) {
            for (int index = a.size(); --index >= 0; ) {
                T aa = a.get(index);
                T bb = b.get(index);
                diff = aa.compareTo(bb);
                if (diff != 0) {
                    break;
                }
            }
        }
        return diff;
    }

}
