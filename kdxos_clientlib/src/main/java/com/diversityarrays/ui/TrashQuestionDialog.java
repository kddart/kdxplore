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
package com.diversityarrays.ui;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class TrashQuestionDialog {
	
	static public boolean confirm(Window owner, String title, String message) {
		return confirm(owner, title, message, null);
	}

	static public boolean confirm(Window owner, String title, String message, ResourceBundle bundle) {
		TrashQuestionDialog d = new TrashQuestionDialog(message, bundle);
		return d.ask(owner, title);
	}

	private final JButton deleteButton = new JButton(new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			optionPane.setValue(deleteButton);
		}
	});
	
	private final JButton cancelButton = new JButton(new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			optionPane.setValue(cancelButton);
		}
	});
	
	private JOptionPane optionPane;
	
	private TrashQuestionDialog(Object message, ResourceBundle bundle) {
		cancelButton.setName("trashCancelButton");
		deleteButton.setName("trashConfirmButton");
		
		cancelButton.setIcon(ImageStore.getImageStore().getImageIcon(ImageStore.ImageId.CrossButton));
		deleteButton.setIcon(ImageStore.getImageStore().getImageIcon(ImageStore.ImageId.Trash24));
		Icon dialogIcon = ImageStore.getImageStore().getImageIcon(ImageStore.ImageId.TrashQuestion);
		
		if (bundle!=null) {
			Icon icon = getIcon("trashDialogIcon", bundle);
			if (icon!=null) {
				dialogIcon = icon;
			}
			for (JButton btn : new JButton[] { cancelButton, deleteButton }) {
				String iconUrl = getBundleString(btn.getName()+".icon", bundle);
				if (iconUrl!=null) {
					if (iconUrl.trim().isEmpty()) {
						// Leave empty
						btn.setIcon(null);
					}
					else {
						icon = getIcon(btn.getName()+".icon", bundle);
						if (icon!=null) {
							btn.setIcon(icon);
						}
					}
					
				}
				String text = getBundleString(btn.getName()+".text", bundle);
				if (text!=null) {
					btn.setText(text);
				}
			}
			
		}
		Component[] options = new Component[] { cancelButton, deleteButton };
		optionPane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, dialogIcon, options, options[0]);
	}
	
	private boolean ask(Component parentComponent, String title) {
		optionPane.setValue(cancelButton); // default is no
		JDialog dlg = optionPane.createDialog(parentComponent, title);
		dlg.setVisible(true);
		return deleteButton == optionPane.getValue();
	}
	
	static private Icon getIcon(String key, ResourceBundle bundle) {
		Icon result = null;
		String iconUrl = getBundleString(key, bundle);
		if (iconUrl!=null) {
			result = makeIcon(iconUrl);
		}
		return result;
	}
	
	static private Icon makeIcon(String iconUrl) {
		Icon result = null;
		try {
			BufferedImage image = ImageIO.read(new URL(iconUrl));
			result = new ImageIcon(image);
			
		} catch (MalformedURLException e) {
			System.err.println("Invalid icon URL: '"+iconUrl+"'\n"+e.getMessage());
		} catch (IOException e) {
			System.err.println("Faile to load icon from '"+iconUrl+"'\n"+e.getMessage());
		}
		return result;
	}
	
	static private String getBundleString(String key, ResourceBundle bundle) {
		String result = null;
		try {
			result = bundle.getString(key);
		}
		catch (MissingResourceException ignore) {
		}
		return result;
	}


}
