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
package com.diversityarrays.kdxplore.welcomeapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.services.KdxApp;
import com.diversityarrays.kdxplore.services.KdxAppService;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.util.MsgBox;

public class WelcomeAppService implements KdxAppService {
    
    static private final String TAG = "WelcomeApp"; //$NON-NLS-1$

	@Override
	public KdxApp createKdxApp(KdxPluginInfo pluginInfo) throws Exception {
		return new WelcomeApp(pluginInfo);
	}

	static class WelcomeApp implements KdxApp {

        private static final String WWW_DIVERSITYARRAYS_COM = "http://www.diversityarrays.com/"; //$NON-NLS-1$
        
//        private static final String SOFTWARE_KDDART_COM_KDXPLORE_DOC = "http://software.kddart.com/KDXplore/doc/"
        private static final String KDXPLORE_SPLASH_PNG = "kdxplore_splash.png"; //$NON-NLS-1$
        
        private final JPanel ui = new JPanel(new BorderLayout());
        
        private final String onlineHelpUrl;

		public WelcomeApp(KdxPluginInfo pluginInfo) {
		    
		    onlineHelpUrl = KdxploreConfig.getInstance().getOnlineHelpUrl();
		    JComponent labelComponent = null;
		    
		    Locale locale = Locale.getDefault();
		    
		    URL url = null;
		    String lcl = locale.toString();
		    if (! lcl.isEmpty()) {
	            url = getClass().getResource("welcome.html_" + lcl); //$NON-NLS-1$
		    }
		    if (url == null) {
	            url = getClass().getResource("welcome.html"); //$NON-NLS-1$
		    }
		    
		    if (url != null) {
		        try {
                    JEditorPane ep = new JEditorPane(url);
                    ep.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                    ep.setBackground(Color.decode("#ffffcc")); //$NON-NLS-1$
                    ep.setEditable(false);
                    
                    HyperlinkListener hyperlinkListener = new HyperlinkListener() {
                        @Override
                        public void hyperlinkUpdate(HyperlinkEvent event) {
                            HyperlinkEvent.EventType type = event.getEventType();
                            final URL url = event.getURL();
                            if (type == HyperlinkEvent.EventType.ACTIVATED) {
                                try {
                                    Desktop.getDesktop().browse(url.toURI());
                                }
                                catch (IOException | URISyntaxException e) {
                                    MsgBox.warn(ui, 
                                            url.toString() + "\n\n" + e.getMessage(), //$NON-NLS-1$
                                            Messages.getString(MessageId.UNABLE_TO_OPEN_URL));
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    ep.addHyperlinkListener(hyperlinkListener);
                    
                    labelComponent = new JScrollPane(ep);
                }
                catch (IOException e1) {
                    Shared.Log.w(TAG, "unable to create editorPane", e1); //$NON-NLS-1$
                }
		    }
		    
		    if (labelComponent == null) {
		        labelComponent = new JLabel(Messages.getString(MessageId.WELCOME_TO_KDXPLORE), JLabel.CENTER);
		    }
		    
		    Action removeAppAction = new AbstractAction(Messages.getString(MessageId.HIDE_FROM_VIEW)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pluginInfo.removeAppFromView(WelcomeApp.this);
                }		        
		    };
		    
	        JCheckBox hideThisApp = new JCheckBox(Messages.getString(MessageId.ALSO_HIDE_ON_NEXT_START));
            hideThisApp.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pluginInfo.appWantsToHide(WelcomeApp.this);
                    
                }
            });
            
		    
	        Box controls = Box.createHorizontalBox();
	        controls.add(new JButton(removeAppAction));
	        controls.add(hideThisApp);
	        controls.add(Box.createHorizontalGlue());
	        
            addSplash();
            
            ui.add(labelComponent, BorderLayout.CENTER);                
            ui.add(controls, BorderLayout.SOUTH);
		}

        private void addSplash() {
            InputStream is = getClass().getResourceAsStream(KDXPLORE_SPLASH_PNG);
            if (is != null) {
                try {
                    BufferedImage img = ImageIO.read(is);
                    JLabel splashLabel = new JLabel(new ImageIcon(img), JLabel.CENTER);
                    Box top = Box.createHorizontalBox();
                    top.add(Box.createHorizontalGlue());
                    top.add(splashLabel);
                    top.add(Box.createHorizontalGlue());
                    
                    ui.add(top, BorderLayout.NORTH);
                    splashLabel.addMouseListener(new MouseListener() {
                        @Override
                        public void mouseReleased(MouseEvent e) {}
                        @Override
                        public void mousePressed(MouseEvent e) {}
                        @Override
                        public void mouseExited(MouseEvent e) {
                            splashLabel.setCursor(Cursor.getDefaultCursor());
                        }
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            splashLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            String where = WWW_DIVERSITYARRAYS_COM;
                            Point pt = e.getPoint();
                            int halfWidth = e.getComponent().getWidth() / 2;
                            if (onlineHelpUrl != null && ! onlineHelpUrl.isEmpty() && pt.x < halfWidth) {
                                where = onlineHelpUrl;
                            }
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(new URI(where));
                                }
                            }
                            catch (IOException e1) {
                                Shared.Log.w(TAG, e1.getMessage());
                            }
                            catch (URISyntaxException e1) {
                                Shared.Log.w(TAG, e1.getMessage());
                            }
                        }
                    });
                }
                catch (IOException e1) {
                    Shared.Log.w(TAG, "missing resoure: " + KDXPLORE_SPLASH_PNG, e1); //$NON-NLS-1$
                }
                finally {
                    if (is != null) {
                        try { is.close(); } catch (IOException ignore) {} 
                    }
                }
            }
        }
        
        @Override
        public DevelopmentState getDevelopmentState() {
            return DevelopmentState.PRODUCTION;
        }
		
		@Override
		public int getDisplayOrder() {
			return 0;
		}

		@Override
		public String getAppName() {
			return Messages.getString(MessageId.APPNAME_WELCOME);
		}

		@Override
		public Component getUIComponent() {
			return ui;
		}
		
	}
}
