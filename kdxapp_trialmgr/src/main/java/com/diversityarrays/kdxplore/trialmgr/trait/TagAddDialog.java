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
package com.diversityarrays.kdxplore.trialmgr.trait;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.GBH;

import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.field.UnicodeChars;

public class TagAddDialog extends JDialog {
	
	private final PromptTextField labelField = new PromptTextField("enter new label", 10);
	private final PromptTextField descriptionTextField =  new PromptTextField("description goes here", 20); 
	
	
	private Action saveTagAction = new AbstractAction("Save Tag") {
		@Override
		public void actionPerformed(ActionEvent e) {
			Tag tag = new Tag();
			tag.setLabel(labelField.getText());
			tag.setDescription(descriptionTextField.getText());	
			if (saveNewTag(tag)) {
				dispose();
			}
		}
	};
	

	private Action cancelTagAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	};
	
	
	private final JLabel warningMessage = new JLabel("Enter Label then Description");
	private final List<Tag> allTags;
	private final KDSmartDatabase kdsmartDatabase;
	
	public TagAddDialog(Window owner, List<Tag> allTags, KDSmartDatabase kdsdb) {
		super(owner, "Create a New Tag", ModalityType.APPLICATION_MODAL);
		
		this.allTags = allTags;
		this.kdsmartDatabase = kdsdb;
		
		setResizable(false);
		
		warningMessage.setForeground(Color.RED);
		warningMessage.setBorder(new BevelBorder(BevelBorder.LOWERED));
		
		DocumentListener docListener = new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateAddTagAction();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateAddTagAction();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateAddTagAction();
			}
		};
		updateAddTagAction();
		labelField.getDocument().addDocumentListener(docListener);
		descriptionTextField.getDocument().addDocumentListener(docListener);

		Box buttonsBox  = Box.createHorizontalBox();
		buttonsBox.add(new JButton(cancelTagAction));
		buttonsBox.add(Box.createHorizontalGlue());
		buttonsBox.add(new JButton(saveTagAction));
		
		JPanel mainPanel = new JPanel();
		GBH gbh = new GBH(mainPanel, 4,4,4,4);
		
		int y = 0;
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "New Tag Label:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, labelField);
		y++;

		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Description:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, descriptionTextField);
		y++;
		
		gbh.add(0,y, 2,1, GBH.HORZ, 1,0, GBH.CENTER,buttonsBox);
		y++;
		
		Container cp = getContentPane();
		cp.add(mainPanel, BorderLayout.NORTH);
		cp.add(warningMessage, BorderLayout.SOUTH);
		
		pack();
		
	}

	private boolean saveNewTag(Tag tag) {
		try {
			kdsmartDatabase.addOrSaveTag(tag, true);
			return true;
		} catch (IOException e) {
			GuiUtil.errorMessage(this, e);
			return false;
		}
	}

	private void updateAddTagAction() {
		
		String msg = null;

		Tag exactMatch = null;
		String label = labelField.getText().trim();
		if (! label.isEmpty()) {
			for (Tag tag : allTags) {
				if (label.equalsIgnoreCase(tag.getLabel())) {
					exactMatch = tag;
				}
			}
		}

		if  (exactMatch != null) {
			msg = "Label already in use";
		}
		else if (label.isEmpty()) {
			msg = "Provide a new Tag Label";
		}
		else {
			String desc = descriptionTextField.getText().trim();
			if (desc.isEmpty()) {
				msg = "Please supply a description";
			}
		}
		warningMessage.setText(msg==null ? " " : msg);
		saveTagAction.setEnabled(msg==null);
	}
	
}
