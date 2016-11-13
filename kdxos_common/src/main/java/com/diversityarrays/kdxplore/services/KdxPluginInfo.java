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
package com.diversityarrays.kdxplore.services;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;

import com.diversityarrays.util.DALClientProvider;
import com.diversityarrays.util.PrintStreamMessageLogger;

import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.MessagePrinter;

public interface KdxPluginInfo {
    
    /**
     * Provide the icon for KDXplore.
     * @return an Icon
     */
    ImageIcon getKdxploreIcon();
    /**
     * This is the main application frame.
     * @return JFrame
     */
    JFrame getKdxploreFrame();
    /**
     * A MessagePrinter that will log messages in the global messages panel.
     * @return MessagePrinter
     */
    MessagePrinter getMessagePrinter();
    /**
     * A PrintStream that will log messages in the global messages panel.
     * @return
     */
    PrintStream getMessagePrintStream();
    
    PrintStreamMessageLogger getMessageLogger();
    /**
     * Provide a WindowOpener that can be used by all Applications - this will register
     * the new JFrames into the Windows menu.
     * @return WindowOpener
     */
    WindowOpener<JFrame> getWindowOpener();
    
    /**
     * A BackgroundRunner that will block interaction on the main application JFrame.
     * @return BackgroundRunnere
     */
    BackgroundRunner getBackgroundRunner();
    /**
     * The folder that contains the userData for this application.
     * The location is operating system dependent.
     * @return
     */
    File getUserDataFolder();
    
    /**
     * A shared DALClientProvider.
     * @return
     */
    DALClientProvider getClientProvider();

    void addToolsMenuActions(Action ... actions);
    Action[] getToolsMenuActions();
    
    Set<KdxApp> getAppsWithMenus();
    
    /**
     * Add an Action that should appear on the named Menu in the
     * main application menuBar.
     * @param menuName
     * @param action
     */
    void addMenus(KdxApp app, List<JMenu> menus);
    
    /**
     * Menus that KdxApps want added to the main application menu bar.
     * @return unmodifiable map of all the menuNamse and actions therein.
     */
    List<JMenu> getMainMenuActions(KdxApp app);
    
    /**
     * If the KdxApp has a visible UI component then remove that
     * component from view.
     * @param app
     */
    void removeAppFromView(KdxApp app);
    void appWantsToHide(KdxApp app);
    
    /**
     * Sets the value of the shared resource of the specified Class and
     * returns any previously set value.
     * @param rClass
     * @param resource
     * @return
     */
    <R> R setSingletonSharedResource(Class<? extends R> rClass, R resource);
    <R> R getSingletonSharedResource(Class<R> rClass);
}
