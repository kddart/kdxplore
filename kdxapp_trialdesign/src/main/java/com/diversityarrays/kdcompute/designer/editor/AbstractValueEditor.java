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
package com.diversityarrays.kdcompute.designer.editor;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;

import com.diversityarrays.kdcompute.designer.ValueEditor;

/**
 * Abstract implementation of ValueEditor to provide common functionality for
 * concrete implementations.
 * @author brianp
 *
 */
public abstract class AbstractValueEditor implements ValueEditor {
    
    protected final EventListenerList listenerList = new EventListenerList();

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
    
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }
    
    private final ChangeEvent changeEvent = new ChangeEvent(this);

    protected void fireStateChanged() {
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
            l.stateChanged(changeEvent);
        }
    }
    
    protected class DelayedDocumentListener implements DocumentListener {
        
        class StateChangedTask extends TimerTask {
            @Override
            public void run() {
                fireStateChanged();
            }
        }

        private final Timer delayedUpdateTimer = new Timer(true);
        private StateChangedTask currentTask;
        
        @Override
        public void removeUpdate(DocumentEvent e) {
            scheduleDelayedUpdate();
        }
        
        @Override
        public void insertUpdate(DocumentEvent e) {
            scheduleDelayedUpdate();
        }
        
        @Override
        public void changedUpdate(DocumentEvent e) {
            scheduleDelayedUpdate();
        }
        
        private void scheduleDelayedUpdate() {
            if (currentTask != null) {
                currentTask.cancel();
                currentTask = null;
            }
            currentTask = new StateChangedTask();
            delayedUpdateTimer.schedule(currentTask, 1000);
        }
    }

}
