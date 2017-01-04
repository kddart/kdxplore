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
package com.diversityarrays.kdxplore.importdata.excelio;

import java.lang.reflect.Field;

public class ImportField {
    public final Class<?> fieldClass;
    public final String fieldName;
    public final String wsHeading;
    public final Field field;
    ImportField(Class<?> clz, String fn, String ws) {
        fieldClass = clz;
        fieldName = fn;
        wsHeading = ws;
        Field f = null;
        Class<?> c = clz;
        while (f == null) {
            try {
                f = c.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException e) {
                c = c.getSuperclass();
                if (Object.class == c) {
                    throw new RuntimeException("No such field as " + clz.getName() + "." + fieldName);
                }
            }
        }
        field = f;
        field.setAccessible(true);
    }
    
    @Override
    public int hashCode() {
        return fieldClass.hashCode() * 17 +
                fieldName.hashCode() * 13 +
                wsHeading.hashCode() * 19 +
                field.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}
