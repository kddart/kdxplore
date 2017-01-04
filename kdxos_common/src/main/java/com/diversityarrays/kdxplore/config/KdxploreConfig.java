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
package com.diversityarrays.kdxplore.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.google.gson.GsonBuilder;

public class KdxploreConfig implements Comparable<KdxploreConfig> {
	
	static private KdxploreConfig instance;

	static public KdxploreConfig initInstance(InputStream is) throws IOException {
		if (instance != null) {
			throw new IllegalStateException("KdxploreConfig.initInstance() was already called"); //$NON-NLS-1$
		}
		instance = create(is);
		return instance;
	}

	public static KdxploreConfig create(InputStream is) throws IOException {
	    KdxploreConfig config;
	    if (is == null) {
	        config = new KdxploreConfig();
	    }
	    else {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();

	        byte[] buf = new byte[1024];
	        int n;
	        while (-1 != (n = is.read(buf))) {
	            baos.write(buf, 0, n);
	        }
	        is.close();
	        baos.close();

	        String json = new String(baos.toByteArray());
	        config = new GsonBuilder().create().fromJson(json, KdxploreConfig.class);
	    }
		return config;
	}
	
	static public KdxploreConfig getInstance() {
		if (instance == null) {
			throw new IllegalStateException("KdxploreConfig.initInstance() has not been called"); //$NON-NLS-1$
		}
		return instance;
	}
	
	private boolean beta;
	private int priority;
	private boolean eternal;
	private String updateBaseUrl;
	private String onlineHelpUrl;
	private String supportEmail;
	private List<String> modeList;
	private int flags;
	private String[] mainPluginClassNames;
	
	private KdxploreConfig() { }
	
	public boolean isBeta() {
		return beta;
	}
	
	public int getPriority() {
		return priority;
	}

	public boolean isEternal() {
		return eternal;
	}
	
	public String getUpdatebaseUrl() {
	    return updateBaseUrl;
	}
	
	public String getOnlineHelpUrl() {
	    return onlineHelpUrl;
	}
	
	public String getSupportEmail() {
	    return supportEmail;
	}
	
	public String[] getMainPluginClassNames() {
	    return mainPluginClassNames;
	}

	public List<String> getModeList() {
		return modeList==null ? Collections.emptyList() : Collections.unmodifiableList(modeList);
	}

	public int getFlags() {
	    return flags;
	}

	@Override
	public int compareTo(KdxploreConfig o) {
		return Integer.compare(this.priority, o.priority);
	}

}
