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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventListener;

import javax.swing.JTable;
import javax.swing.event.EventListenerList;
import javax.swing.event.MouseInputAdapter;

@SuppressWarnings("nls")
public class TableRowResizer extends MouseInputAdapter {  
	
	static public interface RowHeightChangeListener extends EventListener {
		void rowHeightChanged(Object source, int rowIndex, int rowHeight);
	}
	
    public static boolean DEBUG = Boolean.getBoolean("TableRowSizer.DEBUG");

	public static Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR); 
 
    private int mouseYOffset, resizingRow; 
    private Cursor otherCursor = resizeCursor; 
    private JTable table; 

    public TableRowResizer(JTable table) {
    	this(table, false);
    }

    public TableRowResizer(JTable table, boolean resizeAll) { 
        this.table = table; 
        this.resizeAllRows = resizeAll;
        table.addMouseListener(this); 
        table.addMouseMotionListener(this); 
    } 
 
    private int getResizingRow(Point p) { 
        return getResizingRow(p, table.rowAtPoint(p)); 
    } 
 
    private int getResizingRow(Point p, int row){ 
        if(row == -1){ 
            return -1; 
        } 
        int col = table.columnAtPoint(p); 
        if(col==-1) 
            return -1; 
        Rectangle r = table.getCellRect(row, col, true); 
        r.grow(0, -3); 
        if(r.contains(p)) 
            return -1; 
 
        int midPoint = r.y + r.height / 2; 
        int rowIndex = (p.y < midPoint) ? row - 1 : row; 
 
        return rowIndex; 
    } 
    
    protected final EventListenerList listenerList = new  EventListenerList();
    
    public void addRowHeightChangeListener(RowHeightChangeListener l) {
    	listenerList.add(RowHeightChangeListener.class, l);
    }
    public void removeRowHeightChangeListener(RowHeightChangeListener l) {
    	listenerList.remove(RowHeightChangeListener.class, l);
    }
    
	protected void fireRowHeightChanged(int rowIndex, int height) {
		for (RowHeightChangeListener l : listenerList.getListeners(RowHeightChangeListener.class)) {
			l.rowHeightChanged(this, rowIndex, height);
		}
	}
    
    private int lastHeight = 0;
    @Override
    public void mousePressed(MouseEvent e) { 
        Point p = e.getPoint(); 
 
        resizingRow = getResizingRow(p); 
        mouseYOffset = p.y - table.getRowHeight(resizingRow); 
        lastHeight = 0;
    } 
 
    private void swapCursor() { 
        Cursor tmp = table.getCursor(); 
        table.setCursor(otherCursor); 
        otherCursor = tmp; 
    } 
 
    @Override
    public void mouseMoved(MouseEvent e) { 
        if ((getResizingRow(e.getPoint())>=0) != (table.getCursor() == resizeCursor)) { 
            swapCursor(); 
        } 
    } 
 
    @Override
    public void mouseDragged(MouseEvent e) { 
        int mouseY = e.getY(); 
 
        if (resizingRow >= 0) { 
            int newHeight = mouseY - mouseYOffset; 
            if (newHeight > 0) {
            	int diff = newHeight - lastHeight;
            	if (DEBUG) {
                	System.err.println("height diff=" + diff + " = (" + newHeight + " - " + lastHeight + ")");
                    System.out.println("TableRowResizer: setting " 
                    		+ (resizeAllRows ? "ALL" : "Row#" + resizingRow)
                    		+ " rows to rowHeight=" + newHeight);
            	}
            	
            	if (resizeAllRows) {
            		for (int row = table.getRowCount(); --row >= 0; ) {
                        table.setRowHeight(row, newHeight); 
            		}
            	}
            	else {
                    table.setRowHeight(resizingRow, newHeight); 
            	}            	
                
                fireRowHeightChanged(resizingRow, newHeight);

            	lastHeight = newHeight;
            }
        } 
    }
    
    private boolean resizeAllRows = false;
    public void setResizeAllRows(boolean b) {
    	resizeAllRows = b;
    }
    public boolean getResizeAllRows() {
    	return resizeAllRows;
    }
} 
