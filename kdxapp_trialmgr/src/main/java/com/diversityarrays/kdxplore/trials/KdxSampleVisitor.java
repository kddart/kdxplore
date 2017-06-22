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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.SampleImpl;
import com.diversityarrays.util.Pair;

public class KdxSampleVisitor implements TrialItemVisitor<Sample> {

    private List<Pair<Field,Field>> fromTo;

    private final TrialItemVisitor<Sample> wrapped;

    private final Set<Integer> traitIds;

    KdxSampleVisitor(Set<Integer> traitIds, TrialItemVisitor<Sample> wrapped) {
        this.traitIds = traitIds;
        this.wrapped = wrapped;
    }

    @Override
    public void setExpectedItemCount(int count) {
        wrapped.setExpectedItemCount(count);
    }

    @Override
    public boolean consumeItem(Sample sample) throws IOException {
        boolean result = true;

        if (traitIds.contains(sample.getTraitId())) {

            SampleImpl tmp;

            if (sample instanceof SampleImpl) {
                tmp = (SampleImpl) sample;
            }
            else {
                if (fromTo==null) {
                    fromTo = buildFromTo(sample);
                }

                // The wrapped visitor expects a SampleImpl
                tmp = new SampleImpl();
                for (Pair<Field,Field> pair : fromTo) {
                    try {
                        Object value = pair.first.get(sample);
                        pair.second.set(tmp, value);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new IOException(e);
                    }
                }
            }

            result = wrapped.consumeItem(tmp);
        }
        return result;
    }

 // Can't build the map until we get the first concrete Sample so we then know the class.
    private List<Pair<Field, Field>> buildFromTo(Sample sample) {
        List<Pair<Field,Field>> fromTo = new ArrayList<>();

        Map<String,Field> toFieldByName = new HashMap<>();
        Class<?> to_cls = SampleImpl.class;
        while (to_cls != Object.class) {
            for (Field  to : to_cls.getDeclaredFields()) {
                if (Modifier.isStatic(to.getModifiers())) {
                    continue;
                }
                toFieldByName.put(to.getName(), to);
            }
            to_cls = to_cls.getSuperclass();
        }

        Class<?> from_cls = sample.getClass();
        while (from_cls != Object.class) {
            for (Field from : from_cls.getDeclaredFields()) {
                if (Modifier.isStatic(from.getModifiers())) {
                    continue;
                }

                Field to = toFieldByName.get(from.getName());
                if (to != null) {
                    from.setAccessible(true);
                    to.setAccessible(true);

                    fromTo.add(new Pair<>(from, to));
                }
            }
            from_cls = from_cls.getSuperclass();
        }

        return fromTo;
    }
}