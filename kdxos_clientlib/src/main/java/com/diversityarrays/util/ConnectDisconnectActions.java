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

import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ConnectDisconnectActions {
    
	private DALClientProvider clientProvider;
	
    /**
     * Called before the getDALClient() call is performed on the DALClientProvider.
     * If a non-null result is provided it is invoked after the call to getDALClient().
     * @param provider
     * @return null to stop the call to the getDALClient()
     */
	private Function<DALClientProvider, Consumer<DALClientProvider>> connectIntercept;

	public final Action connectAction = new AbstractAction("Connect..") {
		@Override
		public void actionPerformed(ActionEvent e) {
		    
		    Consumer<DALClientProvider> afterConnect = null;
		    if (connectIntercept != null) {
		        afterConnect = connectIntercept.apply(clientProvider);
		        if (afterConnect == null) {
		            return;
		        }
		    }
		    try {
	            clientProvider.getDALClient();
		    }
		    finally {
	            if (afterConnect != null) {
	                afterConnect.accept(clientProvider);
	            }
		    }
		}
	};
	
	public final  Action disconnectAction = new AbstractAction("Disconnect..") {
		@Override
		public void actionPerformed(ActionEvent e) {
			clientProvider.logout();
		}
	};
	
	public ConnectDisconnectActions(DALClientProvider clientProvider, 
	        String connectToolTip, 
	        String disconnectToolTip) 
	{	
		this.clientProvider = clientProvider;
		
		KDClientUtils.initAction(ImageId.CONNECTED_24, connectAction, connectToolTip, true);
		KDClientUtils.initAction(ImageId.DISCONNECTED_24, disconnectAction, disconnectToolTip, true);

		clientProvider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateConnectionActions();
			}
		});
		updateConnectionActions();
	}
	
	public void setConnectIntercept(Function<DALClientProvider, Consumer<DALClientProvider>> intercept) {
	    this.connectIntercept = intercept;
	}

	private void updateConnectionActions() {
		boolean connected = clientProvider.isClientAvailable();
		connectAction.setEnabled(! connected);
		disconnectAction.setEnabled(connected);
	}
	
}