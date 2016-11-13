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

import com.diversityarrays.kdxplore.ui.HelpUtils;

public class GotItDialog extends JDialog {

	static public boolean show(Window owner, String title, String msg) {
		GotItDialog dlg = new GotItDialog(owner, title, msg);
		if (owner != null) {
			dlg.setLocationRelativeTo(owner);
		}
		dlg.setVisible(true);
		return dlg.result;
	}
	
	boolean result = false;
	private final Action cancel = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
		@Override
		public void actionPerformed(ActionEvent e) {
			result = false;
			dispose();
		}
	};	

	private final Action gotitAction = new AbstractAction("Got It!") {
		@Override
		public void actionPerformed(ActionEvent e) {
			result = true;
			dispose();
		}
	};	

	public GotItDialog(Window owner, String title, String msg) {
		super(owner, title, ModalityType.APPLICATION_MODAL);
		
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(new JButton(cancel));
		box.add(new JButton(gotitAction));
		
		JLabel label = new JLabel(msg);
		label.setBackground(HelpUtils.PALE_YELLOW);
		label.setOpaque(true);
		
		Container cp = getContentPane();
		cp.add(label, BorderLayout.CENTER);	
		cp.add(box, BorderLayout.SOUTH);
		
		pack();
	}
}
