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

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenu;

public interface KdxApp {
    
    static public enum AfterUpdateResult {
        OK, // KdxApp successfully completed post-UpdateCheck initialisation.
        ABORT, // Immediate failure of the entire application
        FAIL_IF_ALL // Only fail if all other apps also return FAIL_IF_ALL
    }
    
    static public enum DevelopmentState {
        ALPHA,
        BETA,
        PRODUCTION;
        
        public boolean getShouldShowHeading() {
            return PRODUCTION != this;
        }

        public Color getHeadingFontColor() {
            switch (this) {
            case ALPHA:
                return Color.RED;
            case BETA:
                return Color.BLUE;
            case PRODUCTION:
                return Color.BLACK; // should never be used
            default:
                break;
            }
            return Color.ORANGE; // should not get here
        }

        public String getHeadingText() {
            switch (this) {
            case ALPHA:
                return "Pre-Production";
            case BETA:
                return "Pre-Production";
            case PRODUCTION:
                return ""; // should never be used
            default:
                break;
            }
            return name(); // should never be used
        }
    }
    
    DevelopmentState getDevelopmentState();
    
    /**
     * Determines where it displays in the KDXplore Framework
     * @return
     */
    default int getDisplayOrder() {
        return Integer.MAX_VALUE;
    }
    
    /**
     * Order in which KdxApp is called for initialisation.
     * @return
     */
    default int getInitialisationOrder() {
        return Integer.MAX_VALUE;
    }
    
    default void shutdown() {
        // NO-OP
    }

    default BackupProvider getBackupProvider() { return null; }

    /**
     * Perform pre-display initialisation. This method is 
     * called in initialisationPriority order.
     */
    default void initialiseAppBeforeUpdateCheck(AppInitContext initContext) { /* NO-OP */ }

    /**
     * Perform post-update check initialisation. This method is called in displayPriority order.
     */
    default AfterUpdateResult initialiseAppAfterUpdateCheck(AppInitContext initContext) { 
        return AfterUpdateResult.OK; 
    }

    String getAppName();
    Component getUIComponent();

    default List<JMenu> getAppMenus() {
        return null;
    }
    /**
     * If the KdxApp has a default button, it will be set
     * as the rootPane's default button.
     * @return
     */
    default JButton getDefaultButton() {
        return null;
    }
    
    /**
     * Lets the KdxApp respond to whether is is the Active App
     * @param b
     */
    default void setActive(boolean b) { 
        // No-op
    }
}
