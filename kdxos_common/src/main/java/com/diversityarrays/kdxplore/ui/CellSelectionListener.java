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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

public abstract class CellSelectionListener implements TableColumnModelListener, ListSelectionListener {

	public enum EventType {
		COLUMN_SELECTION_CHANGED,
		VALUE_CHANGED;
	}
	
	public CellSelectionListener() { }
	
	abstract public void handleChangeEvent(CellSelectionListener.EventType eventType, ListSelectionEvent event);
	
	@Override
	public void columnAdded(TableColumnModelEvent e) {
	}

	@Override
	public void columnRemoved(TableColumnModelEvent e) {
	}

	@Override
	public void columnMoved(TableColumnModelEvent e) {
	}

	@Override
	public void columnMarginChanged(ChangeEvent e) {
	}

	@Override
	public void columnSelectionChanged(ListSelectionEvent e) {
		handleChangeEvent(EventType.COLUMN_SELECTION_CHANGED, e);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		handleChangeEvent(EventType.VALUE_CHANGED, e);
	}
}