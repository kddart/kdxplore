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
package com.diversityarrays.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.RootPaneContainer;

import org.apache.commons.collections15.Closure;
import org.pietschy.wizard.Wizard;
import org.pietschy.wizard.WizardEvent;
import org.pietschy.wizard.WizardListener;
import org.pietschy.wizard.WizardModel;

public class WizardRunner extends Wizard {

	static public enum WizardEndedReason {
		Closed,
		Cancelled;
	}

	private JLabel status = new JLabel();
	private Window window;
	private Closure<WizardEndedReason> handler;

	public WizardRunner(WizardModel wm) {
		super(wm);
	}

	public void setWindow(Window w) {
		this.window = w;
	}

	public void setWizardEndedHandler(Closure<WizardEndedReason> handler) {
		this.handler = handler;
	}

	WizardListener wizardListener = new WizardListener() {

		@Override
		public void wizardClosed(WizardEvent e) {
			System.err.println("wizardClosed");
			handleWizardEnded(WizardEndedReason.Closed);
		}

		@Override
		public void wizardCancelled(WizardEvent e) {
			System.err.println("wizardCancelled");
			handleWizardEnded(WizardEndedReason.Cancelled);
		}
	};

	private Action closeMe = new AbstractAction("Close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			window.dispose();
		}
	};

	protected void handleWizardEnded(WizardEndedReason reason) {

		if (handler != null) {
			handler.execute(reason);

		}

		status.setText(reason.toString());
		WizardRunner.this.setEnabled(false);

		RootPaneContainer rpc = ((RootPaneContainer) window);

		rpc.getContentPane().remove(this);

		Container cp = rpc.getContentPane();
		cp.removeAll();


		JLabel label = new JLabel(reason.toString());

		Box n = Box.createHorizontalBox();
		n.add(Box.createHorizontalGlue());
		n.add(label);
		n.add(Box.createHorizontalGlue());
		cp.add(n, BorderLayout.CENTER);

		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(new JButton(closeMe));
		box.add(Box.createHorizontalStrut(20));
		cp.add(box, BorderLayout.SOUTH);

		cp.setBackground(Color.LIGHT_GRAY);

		Dimension d = window.getSize();
		++d.width;
		window.setSize(d);

	}


	public Window showRelativeTo(Component relativeTo) {

		RootPaneContainer rpc = ((RootPaneContainer) window);
		Container cp = rpc.getContentPane();
		cp.setLayout(new BorderLayout());

		cp.add(status, BorderLayout.NORTH);
		cp.add(this, BorderLayout.CENTER);
		window.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				cancel();
			}
		});

		status.setText("Active");

		addWizardListener(wizardListener);

		window.pack();
		Dimension d = window.getSize();
		++d.width;
		window.setSize(d);
		
		window.setLocationRelativeTo(relativeTo);
		window.setVisible(true);
		window.toFront();
		
		return window;
	}
}
