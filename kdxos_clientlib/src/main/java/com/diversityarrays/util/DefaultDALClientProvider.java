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

import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.commons.collections15.Factory;
import org.apache.commons.logging.Log;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.dalclient.DalLoginException;
import com.diversityarrays.dalclient.DalResponse;
import com.diversityarrays.dalclient.DalResponseException;
import com.diversityarrays.dalclient.HttpResponseInfo;
import com.diversityarrays.dalclient.PostBuilder;
import com.diversityarrays.dalclient.QueryBuilder;
import com.diversityarrays.dalclient.ResponseType;
import com.diversityarrays.dalclient.SessionExpiryOption;
import com.diversityarrays.dalclient.UpdateBuilder;
import com.diversityarrays.ui.LoginDialog;
import com.diversityarrays.ui.LoginUrlsProvider;

public class DefaultDALClientProvider implements DALClientProvider {

	private Window defaultOwner;
	private LoginUrlsProvider loginUrlsProvider;
	private JComponent brandingComponent;

	private ProxyClient client;
	private String clientVersion;
	private String initialClientUrl;
	private String initialClientUsername;
    private boolean canChangeUrl = true;

	public DefaultDALClientProvider(Window owner, LoginUrlsProvider lup, JComponent brandingComponent) {
		this.defaultOwner = owner;
		this.loginUrlsProvider = lup;
		this.brandingComponent = brandingComponent;
	}
	
	@Override
	public void setDefaultOwner(Window owner) {
		this.defaultOwner = owner;
	}
	
	@Override
	public void setCanChangeUrl(boolean enable) {
	    this.canChangeUrl = enable;
	}
	
	@Override
    public boolean getCanChangeUrl() {
	    return canChangeUrl;
	}

	@Override
	public void setInitialClientUrl(String clientUrl) {
		this.initialClientUrl = clientUrl;
	}
	
	@Override
	public void setInitialClientUsername(String username) {
		this.initialClientUsername = username;
	}

	@Override
	public DALClient getDALClient() {
		return getDALClient(defaultOwner, null);
	}
	
	@Override
	public DALClient getDALClient(String dialogTitle) {
		return getDALClient(defaultOwner, dialogTitle);
	}
	
	@Override
	public DALClient getDALClient(Window owner, String dialogTitle) {
		
		
		if (client==null) {
			Window useOwner = owner!=null ? owner : defaultOwner;
			
			String title = dialogTitle;
			if (title==null) {
				
				if (useOwner != null && useOwner instanceof Frame) {
					title = ((Frame) useOwner).getTitle();
				}
				if (title==null) {
					title = "KDDart Login"; //$NON-NLS-1$
				}
			}
			
			LoginDialog loginDialog = new LoginDialog(useOwner, title, loginUrlsProvider);
			loginDialog.setUseUrlField(! canChangeUrl);
			if (brandingComponent!=null) {
				loginDialog.addBrandingComponent(brandingComponent);
			}
			
			loginDialog.setInitialUrl(initialClientUrl);
			loginDialog.setInitialUsername(initialClientUsername);
			
			loginDialog.setVisible(true);
			
			DALClient tmp = loginDialog.getDALClient();
			if (tmp != null) {
				client = new ProxyClient(tmp);
				fireStateChanged();
			}
		}
		return client;
	}

	@Override
	public void logout() {
		if (client != null) {
			handleProxyLogout(client);
		}
	}
	
	@Override
	public LoginUrlsProvider getLoginUrlsProvider() {
	    return loginUrlsProvider;
	}
	
	protected void handleProxyLogout(ProxyClient proxy) {

		if (proxy != null) {
			if (client != proxy) {
				proxy.wrapped.logout();
			}
			else {
				// proxy *IS* the client
				ProxyClient tmp = client;
				client = null;
				tmp.wrapped.logout();
				fireStateChanged();
			}
		}
	}

	@Override
	public boolean isClientAvailable() {
		return client!=null;
	}

	private ChangeEvent changeEvent = new ChangeEvent(this);
	protected EventListenerList listenerList = new EventListenerList();
	
	protected void fireStateChanged() {

		if  (SwingUtilities.isEventDispatchThread()) {
			// We want to run OUTSIDE the event dispatch thread.
			new Thread(new Runnable() {
				@Override
				public void run() {
					fireStateChanged();
				}
			}).start();
			
			return;
		}
		
		
		for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
			try {
				l.stateChanged(changeEvent);
			}
			catch (Throwable t) {
				t.printStackTrace();
				if (t instanceof OutOfMemoryError) {
					// Give up !
					break;
				}
			}
		}
	}
	
	@Override
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}
	
	@Override
	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}

	@Override
	public String getClientVersion() {
		if (clientVersion==null) {
			clientVersion = "Unknown-version"; //$NON-NLS-1$
			try {
				DalResponse versionResponse = client.performQuery("get/version"); //$NON-NLS-1$
				clientVersion = 
						versionResponse.getRecordFieldValue(DALClient.TAG_INFO, DALClient.ATTR_VERSION);
			} catch (DalResponseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return clientVersion;
	}

	class ProxyClient implements DALClient {
		
		final DALClient wrapped;
		
		ProxyClient(DALClient c) {
			this.wrapped = c;
		}

		public String getBaseUrl() {
			return wrapped.getBaseUrl();
		}

		public DALClient setResponseType(ResponseType responseType) {
			return wrapped.setResponseType(responseType);
		}

		public ResponseType getResponseType() {
			return wrapped.getResponseType();
		}

		public DALClient setAutoSwitchGroupOnLogin(boolean switchGroup) {
			return wrapped.setAutoSwitchGroupOnLogin(switchGroup);
		}

		public boolean getAutoSwitchGroupOnLogin() {
			return wrapped.getAutoSwitchGroupOnLogin();
		}

		public void setSessionExpiryOption(
				SessionExpiryOption sessionExpiryOption) {
			wrapped.setSessionExpiryOption(sessionExpiryOption);
		}

		public SessionExpiryOption getSessionExpiryOption() {
			return wrapped.getSessionExpiryOption();
		}

		public boolean isLoggedIn() {
			return wrapped.isLoggedIn();
		}

		public void login(String username, String password) throws IOException,
				DalLoginException, DalResponseException {
			wrapped.login(username, password);
		}

		public void logout() {
			handleProxyLogout(this);
		}

		public String switchGroup(String groupId) throws IOException,
				DalResponseException {
			return wrapped.switchGroup(groupId);
		}

		public String getUserId() {
			return wrapped.getUserId();
		}

		public String getUserName() {
			return wrapped.getUserName();
		}

		public String getWriteToken() {
			return wrapped.getWriteToken();
		}

		public boolean isInAdminGroup() {
			return wrapped.isInAdminGroup();
		}

		public String getGroupName() {
			return wrapped.getGroupName();
		}

		public String getGroupId() {
			return wrapped.getGroupId();
		}

		public DalResponse performQuery(String command) throws IOException,
				DalResponseException {
			return wrapped.performQuery(command);
		}

		public QueryBuilder prepareQuery(String command) {
			return wrapped.prepareQuery(command);
		}

		@Override
		public QueryBuilder prepareGetQuery(String command) {
			return wrapped.prepareGetQuery(command);
		}

		@Override
		public PostBuilder preparePostQuery(String command) {
			return wrapped.preparePostQuery(command);
		}
		
		public DalResponse performUpdate(String command,
				Map<String, String> postParameters) throws IOException,
				DalResponseException {
			return wrapped.performUpdate(command, postParameters);
		}

		public UpdateBuilder prepareUpdate(String command) {
			return wrapped.prepareUpdate(command);
		}

		public DalResponse performUpload(String command,
				Map<String, String> postParameters, File upload)
				throws FileNotFoundException, IOException, DalResponseException {
			return wrapped.performUpload(command, postParameters, upload);
		}

		public DalResponse performUpload(String command,
				Map<String, String> postParameters,
				Factory<InputStream> streamFactory)
				throws DalResponseException, IOException {
			return wrapped
					.performUpload(command, postParameters, streamFactory);
		}

		public UpdateBuilder prepareUpload(String command, File upload) {
			return wrapped.prepareUpload(command, upload);
		}

		public UpdateBuilder prepareUpload(String command,
				Factory<InputStream> streamFactory) {
			return wrapped.prepareUpload(command, streamFactory);
		}

		public PostBuilder prepareExport(String command) {
			return wrapped.prepareExport(command);
		}

		public DalResponse performExport(String command,
				Map<String, String> postParameters) throws IOException,
				DalResponseException {
			return wrapped.performExport(command, postParameters);
		}

		public void setLog(Log log) {
			wrapped.setLog(log);
		}
/*
		@Override
		public HttpResponseInfo performQueryRaw(String arg0) throws IOException, DalResponseException {
			return wrapped.performQueryRaw(arg0);
		}
*/

	}
}
