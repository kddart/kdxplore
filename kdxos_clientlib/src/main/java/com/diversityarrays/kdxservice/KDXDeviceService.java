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
import java.util.Map;
import javax.swing.JFrame;

import com.diversityarrays.kdclientlib.SerialPortLineListener;

/**
 * Created by Manil Chaudhary on 3/03/2016.
 */
public interface KDXDeviceService  {
    static public final String SERVICE_NAME = "deviceserver"; //$NON-NLS-1$

    public JFrame createUserInterface();

    public boolean isServerActive();
    
    public String receiveMessages(String serverUri);
    
    /**
     * Name of device by URL to access.
     * @return
     */
    public Map<String,String> getDeviceInfo();
    
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl);

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener pcl);
	
	public void addReadListener(String deviceUri, SerialPortLineListener listener);
	
	public void removeReadListener(String deviceUri);

}
