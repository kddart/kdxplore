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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Enumeration;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Allows table columns to be resized not only using the header but from any
 * rows. Based on the BasicTableHeaderUI.MouseInputHandler code.
 *
 */
@SuppressWarnings("nls")
public class TableColumnResizer extends MouseInputAdapter {

    public static boolean DEBUG = Boolean.getBoolean("TableColumnResizer.DEBUG");

	public static final String PROPERTY_WIDTH = "width";

	public static Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);

    private int mouseXOffset;
    private Cursor otherCursor = resizeCursor;

    private JTable table;

    public TableColumnResizer(JTable table) {
        this.table = table;
        table.addMouseListener(this);
        table.addMouseMotionListener(this);
    }

    private boolean canResize(TableColumn column) {
        return column != null
                && table.getTableHeader().getResizingAllowed()
                && column.getResizable();
    }

    private TableColumn getResizingColumn(Point p) {
        return getResizingColumn(p, table.columnAtPoint(p));
    }

    private TableColumn getResizingColumn(Point p, int column) {
        if(column == -1) {
            return null;
        }
        int row = table.rowAtPoint(p);
        if (row==-1) {
            return null;
        }
        Rectangle r = table.getCellRect(row, column, true);
        r.grow( -3, 0);
        if (r.contains(p)) {
            return null;
        }

        int midPoint = r.x + r.width / 2;
        int columnIndex;
        if (table.getTableHeader().getComponentOrientation().isLeftToRight()) {
            columnIndex = (p.x < midPoint) ? column - 1 : column;
        }
        else {
            columnIndex = (p.x < midPoint) ? column : column - 1;
        }

        if (columnIndex == -1) {
            return null;
        }

        return table.getTableHeader().getColumnModel().getColumn(columnIndex);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        table.getTableHeader().setDraggedColumn(null);
        table.getTableHeader().setResizingColumn(null);
        table.getTableHeader().setDraggedDistance(0);

        Point p = e.getPoint();

        // First find which header cell was hit
        int index = table.columnAtPoint(p);
        if (index==-1) {
            return;
        }

        // The last 3 pixels + 3 pixels of next column are for resizing
        TableColumn resizingColumn = getResizingColumn(p, index);
        if (! canResize(resizingColumn)) {
            return;
        }

        table.getTableHeader().setResizingColumn(resizingColumn);
        resizedColumn = resizingColumn;
        startWidth = resizedColumn.getWidth();

        if (table.getTableHeader().getComponentOrientation().isLeftToRight()) {
            mouseXOffset = p.x - resizingColumn.getWidth();
        }
        else {
            mouseXOffset = p.x + resizingColumn.getWidth();
        }
    }

    private void swapCursor() {
        Cursor tmp = table.getCursor();
        table.setCursor(otherCursor);
        otherCursor = tmp;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (canResize(getResizingColumn(e.getPoint()))  != (table.getCursor() == resizeCursor)) {
            swapCursor();
        }
    }

    private TableColumn resizedColumn = null;
    private int startWidth;
    private Integer previousWidth = null;
    @Override
    public void mouseDragged(MouseEvent e) {
        int mouseX = e.getX();

        TableColumn resizingColumn = table.getTableHeader().getResizingColumn();

        boolean headerLeftToRight =
                table.getTableHeader().getComponentOrientation().isLeftToRight();

        if (resizingColumn != null) {
            int oldWidth = resizingColumn.getWidth();
            int newWidth;
            if (headerLeftToRight) {
                newWidth = mouseX - mouseXOffset;
            }
            else {
                newWidth = mouseXOffset - mouseX;
            }

            if (DEBUG) {
                System.out.println("TableColumnResizer.mouseDragged(resizeMode="
                		+ getResizeMode() + ", resizingColumn#"
                		+ resizingColumn.getModelIndex()
                		+ "): mouseXOffset=" + mouseXOffset + ", newWidth=" + newWidth
                		+ ", previousWidth=" + previousWidth);
            }

            if (previousWidth == null) {
            	previousWidth = newWidth;
            }
            else {
            	if (previousWidth == newWidth) {
            		return;
            	}
            	previousWidth = newWidth;
            }

            if (DEBUG) {
                System.out.println("--BEFORE resizingColumn#" + resizingColumn.getModelIndex() + ": width=" + resizingColumn.getWidth());
            }
            resizingColumn.setWidth(newWidth);
            if (resizeAllColumns && JTable.AUTO_RESIZE_OFF==table.getAutoResizeMode()) {
            	TableColumnModel tcm = table.getTableHeader().getColumnModel();
            	for (Enumeration<TableColumn> enumeration = tcm.getColumns(); enumeration.hasMoreElements(); ) {
            		TableColumn tc = enumeration.nextElement();
            		if (resizingColumn != tc) {
            			tc.setWidth(newWidth);
            		}
            	}
            	if (DEBUG) {
                    System.out.println("--AFTER resizingColumn#" + resizingColumn.getModelIndex() + ": width=" + resizingColumn.getWidth());
            	}
            }

            Container container;
            if ((table.getTableHeader().getParent() == null)
               || ((container = table.getTableHeader().getParent().getParent()) == null)
               || ! (container instanceof JScrollPane))
            {
                return;
            }

            if (DEBUG) {
                boolean l2r = container.getComponentOrientation().isLeftToRight();
                String prefix = (l2r ? "LTR" : "RTL") + (headerLeftToRight ? " headerL2R" : " headerR2L");
                report(prefix);
            }

            if (!container.getComponentOrientation().isLeftToRight()
               && !headerLeftToRight)
            {
                if (DEBUG) {
                	System.err.println("-- ! L2R && headerR2L");
                }
                if (table != null) {
                    JViewport viewport = ((JScrollPane)container).getViewport();
                    int viewportWidth = viewport.getWidth();
                    int diff = newWidth - oldWidth;
                    int newHeaderWidth = table.getWidth() + diff;

                    /* Resize a table */
                    Dimension tableSize = table.getSize();
                    tableSize.width += diff;
                    table.setSize(tableSize);

                    /*
                     * If this table is in AUTO_RESIZE_OFF mode and has a horizontal
                     * scrollbar, we need to update a view's position.
                     */
                    if ((newHeaderWidth >= viewportWidth)
                       && (table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF))
                    {
                        Point p = viewport.getViewPosition();
                        p.x = Math.max(0, Math.min(newHeaderWidth - viewportWidth, p.x + diff));
                        viewport.setViewPosition(p);

                        /* Update the original X offset value. */
                        mouseXOffset += diff;
                    }
                }
            }
        }
    }

    private String getResizeMode() {
        int resizeMode = table.getAutoResizeMode();
        String modeName = Msg.MSG_RESIZE_MODE_N(resizeMode);
        switch (resizeMode) {
        case JTable.AUTO_RESIZE_ALL_COLUMNS:
            modeName = Msg.AUTO_RESIZE_ALL_COLUMNS(); //"Auto-Resize-All-Columns";
        	break;
        case JTable.AUTO_RESIZE_LAST_COLUMN:
            modeName = Msg.AUTO_RESIZE_LAST_COLUMN(); //"Auto-Resize-Last-Columns";
        	break;
        case JTable.AUTO_RESIZE_NEXT_COLUMN:
            modeName = Msg.AUTO_RESIZE_NEXT_COLUMN(); //"Auto-Resize-Next-Columns";
        	break;
        case JTable.AUTO_RESIZE_OFF:
            modeName = Msg.AUTO_RESIZE_OFF(); //"Auto-Resize-Off";
        	break;
        case JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS:
            modeName = Msg.AUTO_RESIZE_SUBSEQUENT_COLUMNS(); //"Auto-Resize-Subsequent-Columns";
        	break;
        }
        return modeName;
    }

    private void report(String prefix) {
        System.out.println("=== " + prefix + ": ResizeMode=" + getResizeMode());
//        System.out.println("\tnewHeaderWidth=" + newHeaderWidth + "\tviewportWidth=" + viewportWidth);;
	}

	@Override
    public void mouseReleased(MouseEvent e) {
		previousWidth = null;
        table.getTableHeader().setResizingColumn(null);
        table.getTableHeader().setDraggedColumn(null);

        if (resizedColumn != null) {
            int endWidth = resizedColumn.getWidth();
            if (DEBUG) {
            	System.out.println("[TableColumnResizer: firing propertyChange 'width' from " + startWidth + " to " + endWidth + "]");
            }
            pcs.firePropertyChange(PROPERTY_WIDTH, startWidth, endWidth);
        }
    }

	public TableColumn getResizedColumn() {
		return resizedColumn;
	}
	protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(propertyName, pcl);
	}
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(propertyName, pcl);
	}

    private boolean resizeAllColumns;
	public void setResizeAllColumns(boolean b) {
		resizeAllColumns = b;
	}
	public boolean getResizeAllColumns() {
		return resizeAllColumns;
	}
}
