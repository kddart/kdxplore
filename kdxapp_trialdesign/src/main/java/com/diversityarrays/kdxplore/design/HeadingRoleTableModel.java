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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.pearcan.ui.table.BspAbstractTableModel;

@SuppressWarnings("nls")
public class HeadingRoleTableModel<T> extends BspAbstractTableModel {

    static private final String[] EMPTY = new String[0];

    public static final int ROLE_COLUMN_INDEX = 2;

    private List<String> headings;
    private String[] firstDataRow = EMPTY;

    private Map<String,T> roleByHeading = new HashMap<>();

    private Class<T> roleClass;
    private T defaultValue;
    private List<T> valueChoices;

    private final ChangeEvent changeEvent = new ChangeEvent(this);

    private Function<String, Optional<T>> headingClassifier;

    public HeadingRoleTableModel(Class<T> roleClass, T[] roles, T defaultValue, Function<String,Optional<T>> headingClassifier) {
        super("Heading", "First data value", "Role");

        this.roleClass = roleClass;
        this.defaultValue = defaultValue;
        this.headingClassifier = headingClassifier;

        valueChoices = Arrays.asList(roles);
    }

    public Class<T> getRoleClass() {
        return roleClass;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T[] getRoleValues() {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(roleClass, valueChoices.size());
        return valueChoices.toArray(result);
    }

    public Map<String,T> getRoleByHeading() {
        return Collections.unmodifiableMap(roleByHeading);
    }

    /**
     * Add a listener for changes in the heading/role assignments.
     * @param l
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    protected void fireHeadingRoleAssignmentChanged() {
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
            l.stateChanged(changeEvent);
        }
    }

    public Optional<Integer> getHeadingIndex(Predicate<T> filter) {
    	return roleByHeading.entrySet().stream()
    			.filter(e -> filter.test(e.getValue()))
    			.map(e -> headings.indexOf(e.getKey()))
    			.filter(i -> i >= 0)
    			.findFirst();
    }

    public void setHeadingsAndData(String[] h, String[] d) {
    	headings = h==null ? Collections.emptyList() : Arrays.asList(h);
        firstDataRow = d==null ? EMPTY : d;
        roleByHeading.clear();
        if (headings != null) {
            for (String hdg: headings) {
                T t;
                if (headingClassifier == null) {
                   t = defaultValue;
                }
                else {
                    Optional<T> opt = headingClassifier.apply(hdg);
                    t = opt.orElse(defaultValue);
                }
                roleByHeading.put(hdg, t);
            }
        }
        fireTableDataChanged();
        fireHeadingRoleAssignmentChanged();
    }

    @Override
    public int getRowCount() {
        return headings==null ? 0 : headings.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0: return String.class;
        case 1: return String.class;
        case 2: return roleClass;
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return 2==columnIndex;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (2 != columnIndex) {
            return;
        }

        String hdg = headings.get(rowIndex);
        if (aValue == null) {
            roleByHeading.put(hdg, defaultValue);
            fireTableRowsUpdated(rowIndex, rowIndex);
            fireHeadingRoleAssignmentChanged();
        }
        else if (roleClass.isAssignableFrom(aValue.getClass()) ) {
            roleByHeading.put(hdg, (T) aValue);
            fireTableRowsUpdated(rowIndex, rowIndex);
            fireHeadingRoleAssignmentChanged();
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        String hdg = headings.get(rowIndex);
        switch (columnIndex) {
        case 0: return hdg;
        case 1: return rowIndex < firstDataRow.length ? firstDataRow[rowIndex] : null;
        case 2:
            T role = roleByHeading.get(hdg);
            return role==null ? defaultValue : role;
        }
        return null;
    }

    public void setRoleValueAt(List<Integer> rowIndices, T role) {
        for (Integer rowIndex : rowIndices) {
            String hdg = headings.get(rowIndex);
            roleByHeading.put(hdg, role);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
        fireHeadingRoleAssignmentChanged();
    }

    public T getRoleAt(int rowIndex) {
        String hdg= headings.get(rowIndex);
        T role = roleByHeading.get(hdg);
        return role==null ? defaultValue : (T) role;
    }

    public boolean getAllRowsHaveSameRole(List<Integer> rows) {
        if (rows.isEmpty()) {
            return false;
        }

        T first = null;
        for (Integer row : rows) {
            T role = getRoleAt(row);
            if (first==null) {
                first = role;
            }
            else {
                if (first != role) {
                    return false;
                }
            }
        }
        return true;
    }

}
