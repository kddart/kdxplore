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
package com.diversityarrays.kdxplore.vistool;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.pearcan.ui.GuiUtil;

import org.apache.commons.collections15.Closure;

public class VisToolbarFactory {

	private static final String PROPERTY_ALWAYS_ON_TOP = "alwaysOnTop"; //$NON-NLS-1$

    static public enum ImageFormat {
		PNG(".png", "PNG"),  //$NON-NLS-1$//$NON-NLS-2$
		JPG(".jpg", "JPG"),  //$NON-NLS-1$//$NON-NLS-2$
		JPEG(".jpeg", "JPG");  //$NON-NLS-1$//$NON-NLS-2$
		
		public final String suffix;
		public final String formatName;
		ImageFormat(String sfx, String fn) {
			suffix = sfx;
			formatName = fn;
		}
	}
	static public final String[] IMAGE_SUFFIXES;
	
	static {
		List<String> list = new ArrayList<>();
		for (ImageFormat f : ImageFormat.values()) {
			list.add(f.suffix);
		}
		IMAGE_SUFFIXES = list.toArray(new String[list.size()]);
	}

	
	static private JFileChooser chooser;

	static public VisToolToolBar create(
			final String title,
			final JComponent comp,
			final Closure<File> snapshotter,
			VisToolDataProvider visToolDataProvider,
			final String[] imageSuffixes) 
	{
		return create(title,
				comp,
				snapshotter,
				visToolDataProvider,
				false,
				imageSuffixes);
	}

	static public VisToolToolBar create(
			final String title,
			final JComponent comp,
			final Closure<File> snapshotter,
			final VisToolDataProvider visToolDataProvider,
			boolean floatable,
			final String[] imageSuffixes) 
	{
		Window window = GuiUtil.getOwnerWindow(comp);

		boolean anyButtons = false;
		
		final JCheckBox keepOnTop;
		
		if (window == null) {
			keepOnTop = null;
		}
		else {
			anyButtons  = true;
			keepOnTop = new JCheckBox(Msg.OPTION_KEEP_ON_TOP(), true);
			
			keepOnTop.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					window.setAlwaysOnTop(keepOnTop.isSelected());
				}
			});
			window.setAlwaysOnTop(keepOnTop.isSelected());
			
//			buttons.add(keepOnTop);
			
			final PropertyChangeListener alwaysOnTopListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					keepOnTop.setSelected(window.isAlwaysOnTop());
				}
			};
			window.addPropertyChangeListener(PROPERTY_ALWAYS_ON_TOP, alwaysOnTopListener);
			
			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					window.removeWindowListener(this);
					window.removePropertyChangeListener(PROPERTY_ALWAYS_ON_TOP, alwaysOnTopListener);
				}
			});
		}
		
		final JButton cameraButton;
		if (snapshotter==null) {
			cameraButton = null;
		}
		else {
			Action cameraAction = new AbstractAction(Msg.ACTION_SNAPSHOT()) {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (chooser == null) {
						chooser = new JFileChooser();
						chooser.setFileFilter(new FileFilter() {
							@Override
							public boolean accept(File f) {
								if (! f.isFile()) {
									return true;
								}
								String loname = f.getName().toLowerCase();
								for (String sfx : imageSuffixes) {
									if (loname.endsWith(sfx)) {
										return true;
									}
								}
								return false;
							}

							@Override
							public String getDescription() {
								return Msg.DESC_IMAGE_FILE();
							}
						});
						chooser.setMultiSelectionEnabled(false);
						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					}

					if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(comp)) {
						File file = chooser.getSelectedFile();
						snapshotter.execute(file);
					}
				}			
			};
			

			ImageIcon icon = loadIcon("camera-24.png"); //$NON-NLS-1$
			if (icon != null) {
				cameraAction.putValue(Action.SMALL_ICON, icon);
				cameraAction.putValue(Action.NAME, null);
			}
			
			anyButtons = true;
			cameraButton = new JButton(cameraAction);
		}
		
		
		final JButton refreshButton;
		if (visToolDataProvider == null) {
			refreshButton = null;
		}
		else {
			anyButtons = true;
			
			refreshButton = new JButton(Msg.ACTION_REFRESH());
			
			ImageIcon icon = loadIcon("refresh-24.png"); //$NON-NLS-1$
			if (icon != null) {
				refreshButton.setIcon(icon);
				// don't remove the name
			}
			
			refreshButton.setForeground(Color.RED);
			refreshButton.setEnabled(false);

			refreshButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (visToolDataProvider.refreshData()) {
						refreshButton.setEnabled(false);
					}
				}
			});
			
			visToolDataProvider.addVisToolDataChangedListener(new VisToolDataChangedListener() {
				@Override
				public void visToolDataChanged(Object source) {
					refreshButton.setEnabled(true);
				}
			});
		}
		
		VisToolToolBar toolBar = null;
		
		if (anyButtons) {
			toolBar = new VisToolToolBar(
					keepOnTop,
					cameraButton,
					refreshButton
					);
			toolBar.setFloatable(floatable);
		}
		return toolBar;

	}

	private static ImageIcon loadIcon(String resourceName) {
		ImageIcon icon = null;
		InputStream is = VisToolbarFactory.class.getResourceAsStream(resourceName);
		if (is != null) {
			try {
				icon = new ImageIcon(ImageIO.read(is));
			} catch (IOException ignore) {}
		}	
		return icon;
	}

	public static ImageFormat getImageFormatName(File file) {
		
		String loname = file.getName().toLowerCase();

		for (ImageFormat fmt : ImageFormat.values()) {
			if (loname.endsWith(fmt.suffix)) {
				return fmt;
			}
		}
		return null;
	}
}
