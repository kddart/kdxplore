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
package com.diversityarrays.kdxplore.trials;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableCellEditor;

class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
	
	  final JSpinner spinner = new JSpinner();

	  public SpinnerEditor() {
	    spinner.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
	  }

	  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
	      int row, int column) {
	    spinner.setValue(value);
	    return spinner;
	  }

	  public boolean isCellEditable(EventObject evt) {
	    if (evt instanceof MouseEvent) {
	      return ((MouseEvent) evt).getClickCount() >= 2;
	    }
	    return true;
	  }

	  public Object getCellEditorValue() {
	    return spinner.getValue();
	  }
	}
