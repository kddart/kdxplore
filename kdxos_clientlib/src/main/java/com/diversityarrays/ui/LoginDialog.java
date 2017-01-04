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
package com.diversityarrays.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.dalclient.DalResponse;
import com.diversityarrays.dalclient.DalResponseRecord;
import com.diversityarrays.dalclient.DalResponseRecordVisitor;
import com.diversityarrays.dalclient.DefaultDALClient;

import net.pearcan.ui.widget.BlockingPane;
import net.pearcan.util.GBH;

/**
 * Provides a standardised Login dialog which maintains URL history, lets you remove
 * URLs you no longer want and allows for selection of SystemGroup if there is more than
 * one defined for the user.
 * <p>
 * If you need to introduce some branding, you can call <code>addBrandingComponent(JComponent)</code>
 * which will be displayed to the left of the main panel. See also <code>BrandingImageComponent</code>
 * which displays the image provided to it and fills in the rest of any space with its background colour.
 * @author brian
 *
 */
public class LoginDialog extends JDialog {
    
    static private final String CARD_COMBO = "combo";
    static private final String CARD_FIELD = "field";
    
    private static final Color FG_WHEN_NOT_EDITABLE = Color.GRAY;
	
	private JButton cancelButton = makeJButton("cancelButton", "cross-61x24.png", new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (client!=null) {
				client.logout();
				client = null;
			}
			dispose();
		}
	});
	
	private JButton loginButton = makeJButton("loginButton", "loginblue-64x24.png", new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			message.setText("Connecting...");
			
			blockingPane.block("Connecting...", true);
			String url = getUrl();
            if (! url.matches("^[a-zA-Z]+://.*")) {
                url = "http://" + url;
            }
			
			loginWorker = new LoginWorker(url,
					usernameField.getText().trim(),
					new String(passwordField.getPassword()));
			
			loginWorker.execute();
		}
	});

	private final JLabel urlLabel = loadImageLabel("urlLabel", "folder-web-green-41x32.png");
	@SuppressWarnings("rawtypes")
	private final JComboBox urlComboBox = new JComboBox();
	private final JTextField urlField = new JTextField();
	
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel urlCardPanel = new JPanel(cardLayout);

	private final JLabel usernameLabel = loadImageLabel("usernameLabel", "person-41x32.png");
	private final JTextField usernameField = new JTextField();
	
	private final JLabel passwordLabel = loadImageLabel("passwordLabel", "password-41x32.png");
	private final JPasswordField passwordField = new JPasswordField();

	private final JTextArea message = new JTextArea(3, 0);
	@SuppressWarnings("rawtypes")
	private final JComboBox groupsComboBox = new JComboBox();
	private final JButton setGroupButton = makeJButton("setGroupButton", "group-61x24.png", new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			blockingPane.block("Setting Group...");
			new SwitchGroupWorker((GroupChoice) groupsComboBox.getSelectedItem()).execute();
		}
	});
	
	private final JButton deleteUrlButton = makeJButton("deleteUrlButton", "trash-16b.png", new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent ev) {
			Object item = urlComboBox.getSelectedItem();
			if (item!=null) {
				if (TrashQuestionDialog.confirm(LoginDialog.this, getTitle(), item.toString(), bundle)) {
					urlComboBox.removeItem(item);
					loginUrls.removeUrl(item.toString());
					try {
						loginUrls.save();
					} catch (IOException e) {
						Throwable cause = e.getCause();
						if (cause==null) {
							cause = e;
						}
						Logger.getLogger(LoginDialog.class.getName()).warning(cause.getMessage());
					}
				}
				
			}
		}
		
	});
	
	private final BlockingPane blockingPane = new BlockingPane();
	private DALClient client;
	
	private final LoginUrlsProvider loginUrls;

	private JLabel groupsLabel = null;
	
	private JPanel controls;

	private JPanel main;
	
	private final ResourceBundle bundle;
	
	private final String enterUrl;
	private final String enterUsername;
	private final String enterPassword;

	private final String setGroupMessage;

	private LoginWorker loginWorker;
	
	private boolean useUrlField = false;

	private PropertyChangeListener cancelListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			loginWorker.cancel(true);
		}
	};

    private final Color urlFieldInitialForeground;

	public LoginDialog(Window owner, String title, Preferences loginPrefs) {
		this(owner, title, null,
				new PreferencesLoginUrlsProvider(
				loginPrefs!=null  ? loginPrefs  : Preferences.userNodeForPackage(LoginDialog.class) ));
	}
	
	public LoginDialog(Window owner, String title, Preferences loginPrefs, ResourceBundle resources) {
		this(owner, title, resources,
				new PreferencesLoginUrlsProvider(
				loginPrefs!=null  ? loginPrefs  : Preferences.userNodeForPackage(LoginDialog.class) )
				);
	}
	
	public LoginDialog(Window owner, String title, LoginUrlsProvider urlsProvider) {
		this(owner, title, null, urlsProvider);
	}
	
	@SuppressWarnings("unchecked")
	public LoginDialog(Window owner, String title, ResourceBundle resources, LoginUrlsProvider urlsProvider) {
		super(owner, title, ModalityType.APPLICATION_MODAL);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setAlwaysOnTop(true);

		loginUrls = urlsProvider;
		this.bundle = resources;
		
		urlFieldInitialForeground = urlField.getForeground();
		blockingPane.addPropertyChangeListener(BlockingPane.PROPERTY_CANCELLED, cancelListener);
		
		enterUrl = getBundleString("enterUrlMessage", bundle, "Please select or enter a URL");
		enterUsername = getBundleString("enterUsernameMessage", bundle, "Please enter a Username");
		enterPassword = getBundleString("enterPasswordMessage", bundle, "Please enter a Password");
		setGroupMessage = getBundleString("setGroupMessage", bundle, "Set your group to complete the connection");

		deleteUrlButton.setFocusable(false);
		
		setGlassPane(blockingPane);
		
		initControls(bundle,
				urlLabel, urlComboBox, urlField,
				usernameLabel, passwordLabel,
				loginButton, deleteUrlButton,
				cancelButton, setGroupButton);
		
		String[] urls = loginUrls.getLoginUrls();
		int urlCount = urls.length;
		while (--urlCount>=0) {
			urlComboBox.addItem(urls[urlCount]);
		}
		
		urlField.setEditable(false);
		
		DocumentListener docListener = new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateLoginButton();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateLoginButton();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateLoginButton();
			}
		};
		
		urlComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateLoginButton();
				updateDelUrlButton();
			}
		});
		usernameField.getDocument().addDocumentListener(docListener);
		passwordField.getDocument().addDocumentListener(docListener);
		
//		groupsLabel = loadImageLabel("Groups", "group-61x24.png");

		
		urlComboBox.setEditable(true);
		loginButton.setEnabled(false);
		deleteUrlButton.setEnabled(false);
		
		message.setBorder(null);
		message.setEditable(false);
		message.setForeground(Color.RED);
		message.setBackground(getContentPane().getBackground());
		
		getRootPane().setDefaultButton(loginButton);
		
		Box loginCancel = Box.createHorizontalBox();
		loginCancel.add(Box.createHorizontalStrut(10));
		loginCancel.add(loginButton);
		loginCancel.add(Box.createHorizontalGlue());
		loginCancel.add(setGroupButton);
		loginCancel.add(Box.createHorizontalGlue());
		loginCancel.add(cancelButton);
		loginCancel.add(Box.createHorizontalStrut(10));
		
		urlCardPanel.add(urlField, CARD_FIELD);
		urlCardPanel.add(urlComboBox, CARD_COMBO);
		initialiseCardShowing();
		
		this.controls = new JPanel();
		GBH gbh = new GBH(controls, 2,2,2,2);
		int y = 0;
		gbh.add(0,y, 1,1, GBH.NONE, 0,0, GBH.EAST, urlLabel);
//		gbh.add(0,y, 1,1, GBH.NONE, 0,0, GBH.EAST, loadImageLabel("Url", "http-41x32.png"));
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, urlCardPanel);
		gbh.add(2,y, 1,1, GBH.NONE, 0,0, GBH.WEST, deleteUrlButton);
		++y;
		gbh.add(0,y, 1,1, GBH.NONE, 0,0, GBH.EAST, usernameLabel);
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, usernameField);
		++y;
		gbh.add(0,y, 1,1, GBH.NONE, 0,0, GBH.EAST, passwordLabel);
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, passwordField);
		++y;
		if (groupsLabel!=null) {
			gbh.add(0,y, 1,1, GBH.NONE, 0,0, GBH.EAST, groupsLabel);
		}
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, groupsComboBox);
		++y;
		
		gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, loginCancel);
		++y;

		JScrollPane messageScroll = new JScrollPane(message);


		loginButton.setOpaque(true);
		setGroupButton.setOpaque(true);
//		controls.setOpaque(false);

		this.main = new JPanel(new BorderLayout());
//		main.setOpaque(false);

		main.add(controls, BorderLayout.NORTH);
		main.add(messageScroll, BorderLayout.CENTER);

		getContentPane().add(main, BorderLayout.CENTER);
		
		pack();
		setLocationRelativeTo(null);
		
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				removeWindowListener(this);
				toFront();
			}
		});
	}
	
	public void setUseUrlField(boolean b) {
	    this.useUrlField = b;
	    deleteUrlButton.setVisible(! useUrlField);
	    updateUrlFieldEditable();
	    initialiseCardShowing();
	    updateLoginButton();
	}
	
	private void initialiseCardShowing() {
	    cardLayout.show(urlCardPanel, 
	            useUrlField ? CARD_FIELD : CARD_COMBO );
	}
	
	private void updateUrlFieldEditable() {
	    boolean editable = urlField.getText().trim().isEmpty();
	    urlField.setEditable(editable);
	    
	    urlField.setForeground(editable ? urlFieldInitialForeground : FG_WHEN_NOT_EDITABLE);
	}

	public void setInitialUrl(String url) {
		if (url == null) {
		    urlField.setText("");
		    urlField.requestFocusInWindow();
		}
		else {
			urlComboBox.setSelectedItem(url);
			urlField.setText(url);
			usernameField.requestFocusInWindow();
		}
		updateUrlFieldEditable();
        updateLoginButton();
	}
	
	public void setInitialUsername(String username) {
		if (username != null) {
			usernameField.setText(username);
			passwordField.requestFocusInWindow();
	        updateLoginButton();
		}
	}

	static private String getBundleString(String key, ResourceBundle bundle) {
		return getBundleString(key, bundle, null);
	}
	
	static private String getBundleString(String key, ResourceBundle bundle, String def) {
		String result = def;
		if (bundle!=null) {
			try {
				result = bundle.getString(key);
			}
			catch (MissingResourceException ignore) {
			}
		}
		return result;
	}

	private void initControls(ResourceBundle bundle, JComponent... comps) {
		if (bundle==null) {
			return;
		}
		
		for (JComponent comp : comps) {
			if (comp!=null) {
				String compName = comp.getName();
				if (compName!=null) {

					String text = getBundleString(compName+".text", bundle);
					if (text!=null) {
						if (comp instanceof JButton) {
							((JButton) comp).setText(text);
						}
						else if (comp instanceof JLabel) {
							((JLabel) comp).setText(text);
						}
					}
					
					String iconUrl = getBundleString(compName+".icon", bundle);
					if (iconUrl != null) {
						if (iconUrl.trim().isEmpty()) {
							// I guess this means you don't want an icon!
							if (comp instanceof JButton) {
								((JButton) comp).setIcon(null);
							}
							else if (comp instanceof JLabel) {
								((JLabel) comp).setIcon(null);
							}
						}
						else {
							try {
								BufferedImage image = ImageIO.read(new URL(iconUrl));
								Icon icon = new ImageIcon(image);
								if (comp instanceof JButton) {
									((JButton) comp).setIcon(icon);
								}
								else if (comp instanceof JLabel) {
									((JLabel) comp).setIcon(icon);
								}
							} catch (MalformedURLException e) {
								System.err.println("Invalid icon URL: '"+iconUrl+"'\n"+e.getMessage());
							} catch (IOException e) {
								System.err.println("Faile to load icon from '"+iconUrl+"'\n"+e.getMessage());
							}
						}
					}
				}
			}
		}
	}
	

	public void addBrandingComponent(JComponent c) {
		getContentPane().add(c, BorderLayout.WEST);
		pack();
		setLocationRelativeTo(null);
	}

	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		
		// May be called from super.ctor()
		// If so then controls has not been intialised.
		if (controls!=null) {
			controls.setBackground(bg);
			
			main.setBackground(bg);
			message.setBackground(bg);
			
//			Border b = loginButton.getBorder();
//			if (b!=null) {
//				System.out.println(b.getClass().getName());
//			}
		}
	}
	
	public void setMessageForeground(Color c) {
		message.setForeground(c);
	}
	
	@Override
	public void setVisible(boolean b) {
		if (b) {
			setControlsEnabled(true, urlComboBox, usernameField, passwordField);
			updateLoginButton();
			updateDelUrlButton();
			
			groupsComboBox.removeAllItems();
			setControlsEnabled(false, groupsComboBox, setGroupButton, groupsLabel);

			getRootPane().setDefaultButton(loginButton);
		}
		super.setVisible(b);
	}
	
	private void setControlsEnabled(boolean b, JComponent... controls) {
		for (JComponent c : controls) {
			if (c==null)
				continue;
			c.setEnabled(b);
			if (c instanceof JTextField) {
				((JTextField) c).setEditable(b);
				((JTextField) c).setForeground(b ? Color.BLACK : Color.LIGHT_GRAY);
			}
			else if (c==urlComboBox) {
				urlComboBox.setEditable(b);
			}
			else if (c instanceof JLabel) {
				((JLabel) c).setForeground(b ? Color.BLACK : Color.LIGHT_GRAY);
			}
		}
	}
	
	public DALClient getDALClient() {
		return client;
	}
	
	private void updateDelUrlButton() {
		Object urlItem = urlComboBox.getSelectedItem();
		if (urlItem==null || ! loginUrls.containsUrl(urlItem.toString())) {
			deleteUrlButton.setEnabled(false);
		}
		else {
			deleteUrlButton.setEnabled(true);
		}
	}
	
	private String getUrl() {
	    String url;
        if (useUrlField) {
            url = urlField.getText().trim();
        }
        else {
            Object item = urlComboBox.getSelectedItem();
            url = item==null ? "" : item.toString().trim();
        }
	    return url;
	}
	
	private void updateLoginButton() {
		String msg = null;
		
		String url = getUrl();
		if (url.isEmpty()) {
			msg = enterUrl;
		}
		else if (usernameField.getText().trim().isEmpty()) {
			msg = enterUsername;
		}
		else if (new String(passwordField.getPassword()).trim().isEmpty()) {
			msg = enterPassword;
		}
		message.setText(msg==null?"":msg);
		loginButton.setEnabled(msg==null);
	}
	
	class GroupChoice {
		String id;
		String name;
		String desc;
		
		GroupChoice(String id, String name, String desc) {
			this.id = id;
			this.name = name.trim();
			this.desc = desc==null ? null : desc.trim();
		}
		
		@Override
		public String toString() {
			if (desc!=null && ! desc.isEmpty()) {
				return name + " (" + desc + ")";
			}
			return name;
		}
	}
	
	
	class LoginWorker extends SwingWorker<List<GroupChoice>,Void> {
		
		private String url;
		private String username;
		private String password;
		
		LoginWorker(String url, String username, String password) {
			this.url = url;
			this.username = username;
			this.password = password;
		}

		@Override
		protected List<GroupChoice> doInBackground() throws Exception {
			DALClient c = new DefaultDALClient(url);
			c.login(username, password);
			
			client = c;
			final List<GroupChoice> groups = new ArrayList<GroupChoice>();
			DalResponse response = c.performQuery("list/group");
			
			DalResponseRecordVisitor visitor = new DalResponseRecordVisitor() {
				@Override
				public boolean visitResponseRecord(String resultTagName, DalResponseRecord record) {
					GroupChoice g = new GroupChoice(
						record.rowdata.get("SystemGroupId"),
						record.rowdata.get("SystemGroupName"),
						record.rowdata.get("SystemGroupDescription"));
					
					groups.add(g);
					return true;
				}
			};
			response.visitResults(visitor, "SystemGroup");
			
			return groups;
		}


		@SuppressWarnings("unchecked")
		@Override
		protected void done() {
			
			loginWorker = null;
			
			String errmsg = null;
			try {
				List<GroupChoice> choices = get();
				// success - so let's remember the URL is ok
				loginUrls.addUrl(url);
				try {
					loginUrls.save();
				} catch (IOException e) {
					Throwable cause = e.getCause();
					if (cause==null) {
						cause = e;
					}
					Logger.getLogger(LoginDialog.class.getName()).warning(cause.getMessage());
				}
				
				groupsComboBox.removeAllItems();

				switch (choices.size()) {
				case 0:
					// Let user try a different one
					setControlsEnabled(true, loginButton, urlComboBox, usernameField, passwordField);
					setControlsEnabled(false, groupsComboBox, setGroupButton, groupsLabel);
					
					errmsg = "You are not a member of any groups !"
							+"\n\nTry a different username";
					break;
					
				case 1:
					// ui is still blocked...
					// login worked so don't allow url/username/password again
					setControlsEnabled(false, loginButton, urlComboBox, usernameField, passwordField);
					new SwitchGroupWorker(choices.get(0)).execute();
					break;
					
				default:
					// User has a choice to make...
					for (GroupChoice gc : choices) {
						groupsComboBox.addItem(gc);
					}
					
					setControlsEnabled(false, loginButton, urlComboBox, usernameField, passwordField);
					setControlsEnabled(true, groupsComboBox, setGroupButton, groupsLabel);
					
					message.setText(setGroupMessage);

					// Let user make choice
					blockingPane.unblock();	
					
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							getRootPane().setDefaultButton(setGroupButton);
							setGroupButton.requestFocusInWindow();
						}
					});
				}
			} catch (InterruptedException e) {
				errmsg = e.getMessage();
			} catch (CancellationException e) {
				errmsg = "Cancelled";
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause==null) {
					errmsg = e.getMessage();
				}
				else {
					errmsg = cause.getClass().getSimpleName()+": "+cause.getMessage();
					cause.printStackTrace();
				}
			} finally {
				if (errmsg!=null) {
					try {
						if (client!=null) {
							client.logout();
						}
					}
					finally {
						client = null;
						message.setText(errmsg);
						blockingPane.unblock();
					}
				}
			}
		}
	}
	
	class SwitchGroupWorker extends SwingWorker<Void,Integer> {
		
		private GroupChoice choice;

		SwitchGroupWorker(GroupChoice c) {
			this.choice = c;
		}

		@Override
		protected Void doInBackground() throws Exception {
			client.switchGroup(choice.id);
			return null;
		}

		@Override
		protected void done() {
			String errmsg = null;
			try {
				get();
				blockingPane.unblock();
			} catch (InterruptedException e) {
				errmsg = e.getMessage();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause==null) {
					errmsg = e.getMessage();
				}
				else {
					errmsg = cause.getClass().getSimpleName()+": "+cause.getMessage();
					cause.printStackTrace();
				}
			} finally {
				blockingPane.unblock();
				if (errmsg!=null) {
					message.setText(errmsg);
				}
				else {
					dispose();
				}
			}
		}
		
	}
	
	
	static private JButton makeJButton(String name, String imageName, ActionListener actionListener) {
		JButton result = null;
		try {
			InputStream resource = LoginDialog.class.getResourceAsStream(imageName);
			if (resource!=null) {
				BufferedImage image = ImageIO.read(resource);
				result = new JButton(new ImageIcon(image));
			}
		} catch (IOException ignore) {
		}
		if (result==null) {
			result = new JButton();
		}
		result.setName(name);;
		
		result.addActionListener(actionListener);
		return result;
	}
	
	static private JLabel loadImageLabel(String name, String imageName) {
		JLabel result = null;
		try {
			InputStream resource = LoginDialog.class.getResourceAsStream(imageName);
			if (resource!=null) {
				BufferedImage image = ImageIO.read(resource);
				result = new JLabel(new ImageIcon(image), JLabel.CENTER);
			}
		} catch (IOException ignore) {
		}
		if (result==null) {
			result = new JLabel();
		}
		result.setName(name);
		return result;
	}


}
