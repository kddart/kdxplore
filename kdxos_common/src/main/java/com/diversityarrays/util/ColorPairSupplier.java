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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.pearcan.color.ColorGroups;
import net.pearcan.color.ColorPair;
import net.pearcan.color.ColorPairFactory;

public class ColorPairSupplier<T> implements Function<T, ColorPair>{

    private final Map<T, ColorPair> pairByEntry = new HashMap<>();

    private final ColorPairFactory colorPairFactory;
    
    public ColorPairSupplier() {
        this(ColorGroups.COLOURS_GROUPED_BY_BRIGHTNESS);
    }
    
    public ColorPairSupplier(Color[] colors) {
        colorPairFactory = new ColorPairFactory(colors);
    }

    @Override
    public ColorPair apply(T t) {
        ColorPair result = pairByEntry.get(t);
        if (result == null) {
//            if (colorPairFactory == null) {
//                List<Color> list = new ArrayList<>();
//                Collections.addAll(list, ColorGroups.COLOURS_GROUPED_BY_BRIGHTNESS);
//                colorPairFactory = new ColorPairFactory(list.toArray(new Color[list.size()]));
//            }
            result = colorPairFactory.getNextColorPair();
            pairByEntry.put(t, result);
        }
        return result;
    }
}
