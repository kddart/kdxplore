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
package com.diversityarrays.kdxplore.curate.undoredo;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public class ChangeManager<T> {

	private Stack<Changeable<T>> undoStack = new Stack<>();
	
	private Stack<Changeable<T>> redoStack = new Stack<>();
	
	private EventListenerList listenerList = new EventListenerList();

	private final ChangeEvent changeEvent = new ChangeEvent(this);
	
	public ChangeManager() { }
	
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}
	
	protected void fireStateChanged() {
		for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
			l.stateChanged(changeEvent);
		}
	}
	
	public List<Changeable<T>> getUndoChangeables() {
		List<Changeable<T>> result = new ArrayList<>(undoStack);
		return result;
	}
	
	public void clear() {
		undoStack.clear();
		redoStack.clear();
		fireStateChanged();
	}

	public void addChangeable(Changeable<T> changeable){
		undoStack.push(changeable);
		redoStack.clear();
		fireStateChanged();
	}
	
	public int getRedoCount() {
		return redoStack.size();
	}
	
	public int getUndoCount() {
		return undoStack.size();
	}

	public String undo(T object) {
		String result = "Nothing to undo";
		if (! undoStack.isEmpty()) {
			Changeable<T> change = undoStack.pop();
			try {
				change.undo(object);
				String info = change.getInfo() 
						+" was Undone from "+ change.getNewValue()
						+" to " + change.getOldValue();
				redoStack.push(change);
				result = info;

			} catch (Exception e) {
				e.printStackTrace();
				result = "Undo Error: " + e.getMessage();
			}
			fireStateChanged();
		}
		return result;
	}

	public String redo(T object) {
		String result = "Nothing to redo";
		if (! redoStack.isEmpty()) {
			Changeable<T> change = redoStack.pop();
			try {
				change.redo(object);
				undoStack.push(change);
				result = change.getInfo() 
						+ " was Redone from " + change.getOldValue()
						+ " to " + change.getNewValue();
			} catch (Exception e) {
				e.printStackTrace();
				result = "Redo Error: " + e.getMessage();
			}
			fireStateChanged();
		}
		return result;
	}

}
