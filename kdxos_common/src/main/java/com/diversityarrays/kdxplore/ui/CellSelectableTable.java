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
package com.diversityarrays.kdxplore.ui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;

import com.diversityarrays.util.ResizableTable;

import net.pearcan.ui.table.RowHeaderTable;

public class CellSelectableTable extends ResizableTable {

	private final Map<Integer, Set<Integer>> columnsByRowIndex = new HashMap<>();

	private Point firstExtendCell;
	private RowHeaderTable rowHeaderTable;

	public CellSelectableTable(String name, TableModel tm, boolean resizeAll) {
		this(tm, resizeAll);
		setName(name);
	}

	public CellSelectableTable(TableModel tm, boolean resizeAll) {
		super(tm);
		setResizeAll(resizeAll);
	}

	@Override
    public void setAutoCreateRowSorter(boolean b) {
        if (b) {
            throw new IllegalArgumentException("Does not support setAutoCreateRowSorter(true)");
        }
        super.setAutoCreateRowSorter(false);
    }

    @Override
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        if (sorter != null) {
            throw new IllegalArgumentException("Does not support setRowSorter(non-null)");
        }
        super.setRowSorter(null);
    }

    public void setRowHeaderTable(RowHeaderTable rht) {
		this.rowHeaderTable = rht;
		if (rowHeaderTable != null) {
			rowHeaderTable.setRowHeight(getRowHeight());
		}
	}

    @Override
    public void setRowHeight(int rowHeight) {
    	super.setRowHeight(rowHeight);
    	if (rowHeaderTable != null) {
    		rowHeaderTable.setRowHeight(rowHeight);
    	}
    }

	@Override
	public void setRowHeight(int row, int rowHeight) {
		super.setRowHeight(row, rowHeight);
		if (rowHeaderTable != null) {
			rowHeaderTable.setRowHeight(row, rowHeight);
		}
	}


	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		if (toggle && isCellSelected(rowIndex, columnIndex) && !extend) {
			columnsByRowIndex.get(rowIndex).remove(columnIndex);
		} else {
			if (!toggle && !extend) {
				columnsByRowIndex.clear();
			}

			Set<Integer> selectedColumns = columnsByRowIndex.get(rowIndex);
			if (selectedColumns == null) {
				selectedColumns = new TreeSet<>();
				columnsByRowIndex.put(rowIndex, selectedColumns);
			}
			selectedColumns.add(columnIndex);

			if (!extend) {
				// Set this for next time in with EXTEND==true
						firstExtendCell = new Point(rowIndex, columnIndex);
			} else {
				for (int row = Math.min(firstExtendCell.x, rowIndex);
						row <= Math.max(firstExtendCell.x, rowIndex); row++)
				{
					for (int col = Math.min(firstExtendCell.y, columnIndex);
							col <= Math.max(firstExtendCell.y, columnIndex);
							col++)
					{
						if (! columnsByRowIndex.isEmpty()) {
							Set<Integer> tmp = columnsByRowIndex.get(row);
							if (tmp == null) {
								tmp = new HashSet<>();
								columnsByRowIndex.put(row, tmp);
							}
							tmp.add(col);
						}
					}
				}
			}
		}
		super.changeSelection(rowIndex, columnIndex, toggle, extend);
	}

	@Override
	public void addRowSelectionInterval(int rowIndex0, int rowIndex1) {
		for (int i = rowIndex0; i < rowIndex1; i++) {
			columnsByRowIndex.remove(i); // FIXME: check code in isCellSelected() may have required this
		}
		super.addRowSelectionInterval(rowIndex0, rowIndex1);
	}

	@Override
	public void removeRowSelectionInterval(int rowIndex0, int rowIndex1) {
		for (int i = rowIndex0; i < rowIndex1; i++) {
			columnsByRowIndex.remove(i);
		}
		super.removeRowSelectionInterval(rowIndex0, rowIndex1);
	}

	@Override
	public void selectAll() {
		columnsByRowIndex.clear();
		super.selectAll();
	}

	@Override
	public void clearSelection() {
		if (columnsByRowIndex != null) {
			// DO NOT REMOVE THIS null check.
			// Even though columnsByRowIndex is declared final
			// this method gets called in the JTable constructor
			// before the instance variable is initialised.
			columnsByRowIndex.clear();
		}
		super.clearSelection();
	}

	@Override
	public boolean isCellSelected(int row, int column) {
		// The row must be selected

//		boolean rowIsSelected = getSelectionModel().isSelectedIndex(row);
//		if (! rowIsSelected) {
//			return false;
//		}

		Set<Integer> columnIndices = columnsByRowIndex.get(row);
		return columnIndices != null && columnIndices.contains(column);
//		if (columnIndices == null) {
//			// FIXME check - previously returned TRUE which means all of the columns in the row!
//			return false;
//		}
//
//		return columnIndices.contains(column);
	}

	public List<Point> getSelectedPoints() {
		List<Point> result = new ArrayList<>();
		for (Integer row : columnsByRowIndex.keySet()) {
			for (Integer col : columnsByRowIndex.get(row)) {
				result.add(new Point(col, row));
			}
		}
		return result;
	}

	public void setSelectedPoints(List<Point> points) {
		ListSelectionModel rowSelectionModel = getSelectionModel();
		if (points == null || points.isEmpty()) {
			rowSelectionModel.clearSelection();
			columnsByRowIndex.clear();
		}
		else {
			rowSelectionModel.setValueIsAdjusting(true);

			rowSelectionModel.clearSelection();
			columnsByRowIndex.clear();
			try {
				columnsByRowIndex.clear();
				for (Point pt : points) {
					int row = pt.y;
					int col = pt.x;

					Set<Integer> columns = columnsByRowIndex.get(row);
					if (columns==null) {
						columns = new HashSet<>();
						columnsByRowIndex.put(row, columns);
					}
					columns.add(col);
				}

				List<Integer> rows = new ArrayList<>(columnsByRowIndex.keySet());
				Collections.sort(rows);
				for (Integer row : rows) {
					rowSelectionModel.addSelectionInterval(row, row);
				}
			}
			finally {
				rowSelectionModel.setValueIsAdjusting(false);
			}
		}

	}

}
