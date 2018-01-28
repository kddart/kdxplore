/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import com.diversityarrays.kdxplore.SourceChoiceHandler.SourceChoice;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.ui.GuiUtil;

/**
 * Use SourceChoiceHandler.Util.showSourceChoicePopup() instead of this.
 */
@Deprecated
public class ChooseSourceDialog extends JDialog {
    
	private final Action cancel = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	};

	private final SourceChoiceHandler sourceChoiceHandler;
	
	public ChooseSourceDialog(Window owner, 
			String title, 
			String label,
			SourceChoiceHandler choiceHandler, 
			SourceChoice ... choices) 
	{
		super(owner, title, ModalityType.APPLICATION_MODAL);
		
		this.sourceChoiceHandler = choiceHandler;
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(new JButton(cancel));
		
		for (final SourceChoice choice : choices) {
			Action action = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dispose();
					sourceChoiceHandler.handleSourceChosen(choice);
				}
			};
			KDClientUtils.initAction(choice.imageId, action, choice.text);
			buttons.add(Box.createHorizontalStrut(8));
			buttons.add(new JButton(action));
		}
		
		buttons.add(Box.createHorizontalStrut(10));
		
		JLabel jlabel = new JLabel(label);
		jlabel.setBorder(new EmptyBorder(4, 10, 4, 10));
		Container cp = getContentPane();
		cp.add(jlabel, BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);

		pack();
		
		GuiUtil.centreOnOwner(this);
	}
}
