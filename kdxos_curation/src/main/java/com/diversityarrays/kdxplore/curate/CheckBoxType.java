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
package com.diversityarrays.kdxplore.curate;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;

public class CheckBoxType {
	
	private final List<JCheckBox> checkBoxes;

	private final ApplyTo applyTo;
	
	CheckBoxType(ApplyTo applyTo) {
		this.checkBoxes = new ArrayList<>();
		this.applyTo = applyTo;
	}

	public List<JCheckBox> getCheckBoxes() {
		return checkBoxes;
	}

	public void addCheckBox(JCheckBox checkBox) {
		checkBoxes.add(checkBox);
		checkBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSelected(checkBox.isSelected());		
				applyTo.refreshFromCheckboxes();
			}			
		});
	}

	public boolean isSelected() {
		boolean val = checkBoxes.get(0).isSelected();
		for (JCheckBox checkBox : checkBoxes) {
			if (checkBox.isSelected() != val) {
				// Should never get here;
				throw new RuntimeException("CheckBox value invalid!!");
			}
		}

		return val;
	}

	public void setSelected(boolean b) {
		for (JCheckBox checkBox : checkBoxes) {
			checkBox.setSelected(b);
		}
	}

	public void setEnabled(boolean enable) {
		for (JCheckBox checkBox : checkBoxes) {
			checkBox.setEnabled(enable);
		}
	}
}
