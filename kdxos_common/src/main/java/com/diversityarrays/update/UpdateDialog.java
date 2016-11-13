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
package com.diversityarrays.update;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

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
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.util.KDXploreUpdate;
import com.diversityarrays.util.Msg;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.SwingWorkerCompletionWaiter;
import com.google.gson.Gson;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.StringUtil;

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

	private KDXploreUpdate kdxploreUpdate;
	private Action moreAction = new AbstractAction(Msg.ACTION_SHOW_DETAILS()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				openWebpage(new URL(kdxploreUpdate.helpUrl));
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

		setKDXploreUpdate(null);
		installUpdateAction.setEnabled(false);

		cardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

		progressBar.setIndeterminate(true);
		cardPanel.add(progressBar, CARD_PROGRESS);
		if (exp_ms > 0) {
			daysToGo = 1 + ChronoUnit.DAYS.between(new Date().toInstant(), new Date(exp_ms).toInstant());
			JPanel tmp = new JPanel(new BorderLayout());
			tmp.add(new JLabel(Msg.HTML_THIS_VERSION_EXPIRES_IN_N_DAYS((int) daysToGo)), BorderLayout.NORTH);
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

	private void setKDXploreUpdate(KDXploreUpdate v) {
		this.kdxploreUpdate = v;
		moreAction.setEnabled(kdxploreUpdate != null
				&& !kdxploreUpdate.isError());
	}

	private void checkForUpdates(PrintStream ps) {

		StringBuilder sb = new StringBuilder(updateCheckRequest.updateBaseUrl);
		sb.append(updateCheckRequest.versionCode);
		if (RunMode.getRunMode().isDeveloper()) {
			sb.append("-dev"); //$NON-NLS-1$
		}
		sb.append(".json"); //$NON-NLS-1$

		final String updateUrl;
		updateUrl = sb.toString();
		// updateUrl = "NONEXISTENT"; // Uncomment to check error

		final ProgressMonitor progressMonitor = new ProgressMonitor(
				updateCheckRequest.owner, Msg.PROGRESS_CHECKING(), null, 0, 0);

		worker = new SwingWorker<String, Void>() {

			@Override
			protected String doInBackground() throws Exception {

				// Thread.sleep(3000); // Uncomment to check delay

				BufferedReader reader = null;
				StringBuffer buffer = new StringBuffer();

				try {
					URL url = new URL(updateUrl);
					reader = new BufferedReader(new InputStreamReader(
							url.openStream()));
					int read;
					char[] chars = new char[1024];
					while ((read = reader.read(chars)) != -1) {
						if (progressMonitor.isCanceled()) {
							cancel(true);
							return null;
						}
						buffer.append(chars, 0, read);
					}
				} catch (IOException e) {
					System.err.println("checkForUpdates: " + e.getMessage()); //$NON-NLS-1$
				} finally {
					if (reader != null) {
						reader.close();
					}
				}

				return buffer.toString();
			}

			@Override
			protected void done() {
				try {
					String json = get();
					Gson gson = new Gson();
					setKDXploreUpdate(gson.fromJson(json, KDXploreUpdate.class));
				} catch (CancellationException ignore) {
				} catch (InterruptedException ignore) {
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();

					if (cause instanceof UnknownHostException) {
					    String site = extractSite(updateUrl);
						ps.println(Msg.ERRMSG_UNABLE_TO_CONTACT_UPDATE_SITE(site));
					} else {
						cause.printStackTrace(ps);
					}

					if (cause instanceof FileNotFoundException) {
						FileNotFoundException fnf = (FileNotFoundException) cause;
						if (updateUrl.equals(fnf.getMessage())) {
							// Well maybe someone forgot to create the file on
							// the server!
							System.err
									.println("Maybe someone forgot to create the file!"); //$NON-NLS-1$
							System.err.println(fnf.getMessage());

							setKDXploreUpdate(new KDXploreUpdate(
									Msg.ERRMSG_PROBLEMS_CONTACTING_UPDATE_SERVER_1()));
							return;
						}
					}

					String msg = Msg.HTML_PROBLEMS_CONTACTING_UPDATE_2(
					        StringUtil.htmlEscape(updateUrl),
					        cause.getClass().getSimpleName(),
					        StringUtil.htmlEscape(cause.getMessage()));
					kdxploreUpdate = new KDXploreUpdate(msg);

					kdxploreUpdate.unknownHost = (cause instanceof UnknownHostException);
					return;
				}
			}

			private String extractSite(final String updateUrl) {
				String site = null;
				int spos = updateUrl.indexOf("://");
				if (spos > 0) {
				    int epos = updateUrl.indexOf('/', spos+1);
				    if (epos > 0) {
				        site = updateUrl.substring(0, epos);
				    }
				}
				if (site == null) {
				    site = updateUrl;
				}
				return site;
			}
		};

		Closure<JDialog> onComplete = new Closure<JDialog>() {
			@Override
			public void execute(JDialog d) {
				if (! processReadUrlResult(updateUrl)) {
					d.dispose();
				} else {
					d.setVisible(true);
				}
			}
		};

		SwingWorkerCompletionWaiter waiter = new SwingWorkerCompletionWaiter(
				this, onComplete);
		worker.addPropertyChangeListener(waiter);

		worker.execute();
	}

	private void setResultMessage(String msg) {
		messageLabel.setText(msg);
		cardLayout.show(cardPanel, CARD_MESSAGE);
		closeAction.putValue(Action.NAME, Msg.ACTION_CLOSE());
		pack();
	}

	/**
	 * Return true to display the dialog
	 *
	 * @return
	 */
	private boolean processReadUrlResult(String updateUrl) {

		if (kdxploreUpdate == null) {
			if (RunMode.getRunMode().isDeveloper()) {
				setResultMessage("<html>Unable to read update information:<br>" + updateUrl); //$NON-NLS-1$
			}
			else {
				setResultMessage(Msg.ERRMSG_UNABLE_TO_READ_UPDATE_INFO());
			}
			return updateCheckRequest.userCheck;
		}

		if (kdxploreUpdate.isError()) {
			if (!updateCheckRequest.userCheck && kdxploreUpdate.unknownHost) {
				return false;
			}
			setResultMessage(kdxploreUpdate.errorMessage);
			return true;
		}

		// User is checking or we have an update.

		if (!updateCheckRequest.userCheck) {
			// Auto check ...
			if (kdxploreUpdate.versionCode <= updateCheckRequest.versionCode) {
				// We have the latest
				setResultMessage(Msg.YOUR_VERSION_IS_THE_LATEST());
				return false;
			}
		}

		installUpdateAction
				.setEnabled(kdxploreUpdate.versionCode > updateCheckRequest.versionCode);
		closeAction.putValue(Action.NAME, Msg.ACTION_CLOSE());

		UpdatePanel updatePanel = new UpdatePanel(updateCheckRequest, kdxploreUpdate, daysToGo);

		Container cp = getContentPane();
		cp.remove(cardPanel);

		cp.add(updatePanel, BorderLayout.CENTER);
		pack();

		return true;
	}

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
}
