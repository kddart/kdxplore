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
package com.diversityarrays.update;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.diversityarrays.util.Check;
import com.diversityarrays.util.Msg;
import com.diversityarrays.util.RunMode;

import net.pearcan.ui.GuiUtil;

@SuppressWarnings("nls")
public class UpdateDialog extends JDialog {


	private Action installUpdateAction = new AbstractAction(Msg.ACTION_INSTALL_UPDATE()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(UpdateDialog.this,
					Msg.AUTO_UPDATE_IS_NOT_YET_SUPPORTED(),
					getTitle(),
					JOptionPane.WARNING_MESSAGE);
		}
	};

	private Action moreAction = new AbstractAction(Msg.ACTION_SHOW_DETAILS()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				openWebpage(new URL(updateCheckRequest.kdxploreUpdate.helpUrl));
			} catch (MalformedURLException e1) {
				JOptionPane
						.showMessageDialog(
								UpdateDialog.this,
								Msg.ERRMSG_MALFORMED_URL_IN_UPDATE_FILE(),
								getTitle(), JOptionPane.ERROR_MESSAGE);
				e1.printStackTrace();
			}
		}
	};

	private SwingWorker<String, Void> worker;
	private Action closeAction = new AbstractAction(Msg.ACTION_CANCEL()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (worker != null) {
				worker.cancel(true);
			}
			dispose();
		}
	};

    private Action backupDatabaseAction = new AbstractAction("Backup Database ...") {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateCheckContext.backupDatabase();
        }
    };
    private JButton backupDatabaseButton = new JButton(backupDatabaseAction);

	private final UpdateCheckRequest updateCheckRequest;

	private final JProgressBar progressBar = new JProgressBar();
	private final JLabel messageLabel = new JLabel();

	static private final String CARD_PROGRESS = "progress"; //$NON-NLS-1$
	static private final String CARD_MESSAGE = "message"; //$NON-NLS-1$

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);
	private final long daysToGo;

    private final UpdateCheckContext updateCheckContext;

	public UpdateDialog(UpdateCheckRequest updateCheckRequest, UpdateCheckContext ctx, long exp_ms) {
		super(updateCheckRequest.owner, Msg.TITLE_UPDATE_CHECK(),
				ModalityType.APPLICATION_MODAL);

		this.updateCheckRequest = updateCheckRequest;
        this.updateCheckContext = ctx;

        moreAction.setEnabled(false);
		installUpdateAction.setEnabled(false);

		cardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

		progressBar.setIndeterminate(true);
		cardPanel.add(progressBar, CARD_PROGRESS);
		if (exp_ms > 0) {
			daysToGo = 1 + ChronoUnit.DAYS.between(new Date().toInstant(), new Date(exp_ms).toInstant());
			JPanel tmp = new JPanel(new BorderLayout());
			tmp.add(new JLabel("<HTML>" + Msg.HTML_THIS_VERSION_EXPIRES_IN_N_DAYS((int) daysToGo)), BorderLayout.NORTH);
			tmp.add(messageLabel, BorderLayout.CENTER);
			cardPanel.add(tmp, CARD_MESSAGE);
		}
		else {
			daysToGo = 0;
			cardPanel.add(messageLabel, CARD_MESSAGE);
		}

		cardLayout.show(cardPanel, CARD_PROGRESS);

		Box buttons = Box.createHorizontalBox();
		buttons.add(new JButton(closeAction));
		buttons.add(new JButton(moreAction));
        if (RunMode.getRunMode().isDeveloper()) {
            buttons.add(new JButton(installUpdateAction));
        }
        buttons.add(Box.createHorizontalGlue());
        buttons.add(backupDatabaseButton);
        backupDatabaseButton.setVisible(false);
        backupDatabaseAction.setEnabled(updateCheckContext.canBackupDatabase());

		Container cp = getContentPane();

		cp.add(buttons, BorderLayout.SOUTH);
		cp.add(cardPanel, BorderLayout.CENTER);

		pack();

		GuiUtil.centreOnOwner(this);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				removeWindowListener(this);
				checkForUpdates(updateCheckContext.getPrintStream());
			}

		});
	}

	private void updateMoreAction() {
		moreAction.setEnabled(updateCheckRequest.isNotError());
	}

	private void checkForUpdates(PrintStream ps) {

	    Consumer<String> onReceiveComplete = new Consumer<String>() {
            @Override
            public void accept(String msg) {
                handleCheckCompleted(msg);
            }
        };
        worker = updateCheckRequest.createWorker(ps, onReceiveComplete);
        worker.execute();
	}

	private void setResultMessage(String msg) {
		messageLabel.setText(msg);
		cardLayout.show(cardPanel, CARD_MESSAGE);
		closeAction.putValue(Action.NAME, Msg.ACTION_CLOSE());
		pack();
	}

//	/**
//	 * Return true to display the dialog
//	 *
//	 * @return
//	 */
//	private boolean processReadUrlResult(String updateUrl) {
//
//		if (kdxploreUpdate == null) {
//			if (RunMode.getRunMode().isDeveloper()) {
//				setResultMessage("<html>Unable to read update information:<br>" + updateUrl); //$NON-NLS-1$
//			}
//			else {
//				setResultMessage(Msg.ERRMSG_UNABLE_TO_READ_UPDATE_INFO());
//			}
//			return updateCheckRequest.userCheck;
//		}
//
//		if (kdxploreUpdate.isError()) {
//			if (!updateCheckRequest.userCheck && kdxploreUpdate.unknownHost) {
//				return false;
//			}
//			setResultMessage(kdxploreUpdate.errorMessage);
//			return true;
//		}
//
//		// User is checking or we have an update.
//
//		if (!updateCheckRequest.userCheck) {
//			// Auto check ...
//			if (kdxploreUpdate.versionCode <= updateCheckRequest.versionCode) {
//				// We have the latest
//				setResultMessage(Msg.YOUR_VERSION_IS_THE_LATEST());
//				return false;
//			}
//		}
//
//		installUpdateAction
//				.setEnabled(kdxploreUpdate.versionCode > updateCheckRequest.versionCode);
//		closeAction.putValue(Action.NAME, Msg.ACTION_CLOSE());
//
//		UpdatePanel updatePanel = new UpdatePanel(updateCheckRequest, kdxploreUpdate, daysToGo);
//
//		Container cp = getContentPane();
//		cp.remove(cardPanel);
//
//		cp.add(updatePanel, BorderLayout.CENTER);
//		pack();
//
//		return true;
//	}

	private void openWebpage(URI uri) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop()
				: null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(uri);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(new JFrame(), e.getMessage(),
						Msg.ERRTITLE_BROWSER_OPEN_ERROR(getTitle()),
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}

		}
	}

	private void openWebpage(URL url) {
		try {
			openWebpage(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

    public void showUpdateDetails() {
        installUpdateAction.setEnabled(updateCheckRequest.isUpdateAvailable());
        closeAction.putValue(Action.NAME, Msg.ACTION_CLOSE());

        UpdatePanel updatePanel = new UpdatePanel(updateCheckRequest, daysToGo);

        Container cp = getContentPane();
        cp.remove(cardPanel);

        cp.add(updatePanel, BorderLayout.CENTER);
        pack();

        UpdateDialog.this.setVisible(true);
    }

    public void handleCheckCompleted(String msg) {
        updateMoreAction();

        if (! Check.isEmpty(msg)) {
            setResultMessage(msg);
        }

        switch (updateCheckRequest.checkStatus) {
        case ERROR:
            UpdateDialog.this.setVisible(true);
            break;
        case USER_CHECK:
            if (updateCheckRequest.userCheck) {
                UpdateDialog.this.setVisible(true);
            }
            else {
                UpdateDialog.this.dispose();
            }
            break;
        case UNKNOWN_HOST:
            UpdateDialog.this.dispose();
            break;
        case UP_TO_DATE:
            UpdateDialog.this.dispose();
            break;
        case UPDATE_AVAILABLE:
            showUpdateDetails();
            break;

        case NOT_CHECKED:
        default:
            if (Check.isEmpty(msg)) {
                setResultMessage("Unexpected PostCheckStatus: " + updateCheckRequest.checkStatus);
            }
            UpdateDialog.this.setVisible(true);
            break;
        }
    }
}
