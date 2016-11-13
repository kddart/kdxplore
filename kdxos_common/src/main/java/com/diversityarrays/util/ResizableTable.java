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
package com.diversityarrays.util;

import java.awt.Cursor;

import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * Provide a table that can optionally resize rows, columns, cells by dragging.
 * @author brian
 *
 */
public class ResizableTable extends JTable {
	
	public static boolean RESIZE_ALL_COLUMNS = false;
	
	protected TableRowResizer rowResizer;
	protected TableColumnResizer columnResizer; 

	private boolean resizeAll;
	
	public ResizableTable(TableModel dm){ 
		super(dm); 
	} 
	
	public TableColumnResizer getTableColumnResizer() {
		return columnResizer;
	}
	
	public boolean getResizeAll() {
		return resizeAll;
	}
	
	public void setResizeAll(boolean b) {
		boolean changed = resizeAll != b;
		resizeAll = b;
		if (changed) {
			if (rowResizer != null) {
				rowResizer.setResizeAllRows(resizeAll);
			}
			if (columnResizer != null) {
				columnResizer.setResizeAllColumns(RESIZE_ALL_COLUMNS && resizeAll);
			}
		}
	}

	// turn resizing on/of 
	public void setResizable(boolean row, boolean column){ 
		if (row) { 
			if (rowResizer==null) { 
				rowResizer = new TableRowResizer(this);
			}
			rowResizer.setResizeAllRows(resizeAll);
		}
		else if (rowResizer!=null) { 
			removeMouseListener(rowResizer); 
			removeMouseMotionListener(rowResizer); 
			rowResizer = null; 
		} 
		
		if ( column) { 
			if (columnResizer==null) { 
				columnResizer = new TableColumnResizer(this); 
			}
			columnResizer.setResizeAllColumns(RESIZE_ALL_COLUMNS && resizeAll);
		}
		else if (columnResizer!=null) { 
			removeMouseListener(columnResizer); 
			removeMouseMotionListener(columnResizer); 
			columnResizer = null; 
		} 
	} 

	// If we are doing a mouse press for resize then don't change row/col/cell selection 
	@Override
	public void changeSelection(int row, int column, boolean toggle, boolean extend) { 
		Cursor c = getCursor();
		if (c==TableColumnResizer.resizeCursor || c==TableRowResizer.resizeCursor) {
			return; 
		}
		super.changeSelection(row, column, toggle, extend); 
	} 
}