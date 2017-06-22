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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;

import net.pearcan.ui.DefaultBackgroundRunner;

public abstract class OkCancelDialog extends JDialog {

	private Action okAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (handleOkAction()) {
				dispose();
			}
		}
	};
	private final JButton okButton = new JButton(okAction);

	private Action cancelAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (handleCancelAction()) {
				dispose();
			}
		}
	};

	private final JButton cancelButton = new JButton(cancelAction);

	protected final DefaultBackgroundRunner backgroundRunner;

	public OkCancelDialog(Window owner, String title) {
		this(owner, title, UnicodeChars.CONFIRM_TICK, UnicodeChars.CANCEL_CROSS);
	}

	public OkCancelDialog(Window owner, String title, String okLabel) {
		this(owner, title, okLabel, UnicodeChars.CANCEL_CROSS);
	}

	public OkCancelDialog(Window owner, String title, String okLabel, String cancelLabel) {
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		backgroundRunner = new DefaultBackgroundRunner(title, this);
		setGlassPane(backgroundRunner.getBlockingPane());

		okAction.putValue(Action.NAME, okLabel);
		cancelAction.putValue(Action.NAME, cancelLabel);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
			    removeWindowListener(this);
				if (! initCalled) {
					initialiseGui();
				}
				doPostOpenInitialisation();
			}
		});
	}

	public boolean isOkButtonVisible() {
        return true;
    }

    private boolean initCalled;

	/**
	 * Sub-class *must* call this from constructor
	 */
	protected void initialiseGui() {

	    constructContentPane();

	    getRootPane().setDefaultButton(isOkButtonVisible() ? okButton : cancelButton);

		pack();

		initCalled = true;
	}

	/**
	 * Override this if you want to change the way the ContentPane is constructed
	 */
	protected void constructContentPane() {
        getContentPane().add(createMainPanel(), BorderLayout.CENTER);
        getContentPane().add(createBottomRowComponent(), BorderLayout.SOUTH);
	}

	protected Action getOkAction() {
		return okAction;
	}

	protected Action getCancelAction() {
		return cancelAction;
	}

	/**
	 * Construct and return the main panel that is displayed in the CENTER.
	 * @return
	 */
	protected abstract Component createMainPanel();

	/**
	 * Return true if the Cancel should proceed.
	 * It will result in dispose() being called.
	 * @return
	 */
	protected abstract boolean handleCancelAction();

	/**
	 * Return true if the "Ok" should proceed. It will result in dispose() being called.
	 * @return
	 */
	protected abstract boolean handleOkAction();

	/**
	 * Override this to do any processing that needs to happen after the
	 * Dialog opens for the first time.
	 */
	protected void doPostOpenInitialisation() {
	}

	protected void addExtraBottomComponents(Box box) {
	}

	protected Component createBottomRowComponent() {
		Box buttons = Box.createHorizontalBox();
		addExtraBottomComponents(buttons);
//		buttons.add(Box.createHorizontalStrut(10));
//		buttons.add(new JButton(clearChosenAction));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(cancelButton);
		if (isOkButtonVisible()) {
		    buttons.add(okButton);
		}
		buttons.add(Box.createHorizontalStrut(10));

		return buttons;
	}
}
