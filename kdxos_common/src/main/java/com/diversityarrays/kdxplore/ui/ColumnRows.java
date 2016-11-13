/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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
package com.diversityarrays.kdxplore.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ColumnRows {
	
	private final Map<Integer,Set<Integer>> rowsByColumn = new HashMap<>();
	
	public ColumnRows() {
	}
	
	public Set<Integer> getRowsFor(int column) {
		Set<Integer> result = rowsByColumn.get(column);
		if (result == null) {
			result = Collections.emptySet();
		}
		return result;
	}

	public void add(Integer column, Set<Integer> rows) {
		rowsByColumn.put(column, rows);
	}

	public Collection<? extends Integer> getColumns() {
		return rowsByColumn.keySet();
	}

	public Collection<Set<Integer>> getRowSets() {
		return rowsByColumn.values();
	}
}
