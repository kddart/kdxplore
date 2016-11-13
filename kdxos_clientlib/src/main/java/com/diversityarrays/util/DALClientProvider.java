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
package com.diversityarrays.util;

import java.awt.Window;

import javax.swing.event.ChangeListener;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.ui.LoginUrlsProvider;

public interface DALClientProvider {
	public DALClient getDALClient();
	
	public DALClient getDALClient(String dialogTitle);
	
	public DALClient getDALClient(Window owner, String dialogTitle);

	public void logout();
	public boolean isClientAvailable();
	public void addChangeListener(ChangeListener changeListener);
	public void removeChangeListener(ChangeListener changeListener);
	public String getClientVersion();
	
	public void setDefaultOwner(Window owner);
	public void setInitialClientUrl(String clientUrl);
	/**
	 * If false then the user may only enter a username
	 * if no initialClientUrl has been set.
	 * @param enable
	 */
    public void setCanChangeUrl(boolean enable);
    /**
     * Retrieve the current value of the flag.
     * The default value if it has never been set is true.
     * @return
     */
	public boolean getCanChangeUrl();

	public void setInitialClientUsername(String username);

	public LoginUrlsProvider getLoginUrlsProvider();
}
