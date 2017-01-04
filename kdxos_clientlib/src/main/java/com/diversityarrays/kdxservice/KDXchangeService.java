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
package com.diversityarrays.kdxservice;

import java.beans.PropertyChangeListener;

import javax.swing.JFrame;

public interface KDXchangeService {
    public interface KDXchangeGui {
        void setOfflineData(IOfflineData offlineData);
        JFrame getJFrame();
    }
	
	static public final String SERVICE_NAME = "kdxchange"; //$NON-NLS-1$

	/**
	 * Filename that holds the deviceId (as supplied in the upload form) in device-specific
	 * sub-directory of uploadDestinationDirectory.
	 */
	static public final String DEVICE_ID_FILENAME = "deviceId.txt"; //$NON-NLS-1$
	
	public KDXchangeGui createUserInterface();

	public boolean isServerActive();

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl);

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener pcl);

}
