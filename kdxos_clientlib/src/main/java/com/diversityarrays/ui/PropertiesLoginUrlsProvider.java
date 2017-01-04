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
package com.diversityarrays.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesLoginUrlsProvider implements LoginUrlsProvider {
	
	private final File propertiesFile;
	
	private List<String> urls = new ArrayList<String>();
	
	public PropertiesLoginUrlsProvider(File propertiesFile) throws IOException {
		
		this.propertiesFile = propertiesFile;
		
		Properties properties = loadProperties();
		int urlCount = Integer.parseInt(properties.getProperty("urlCount", "0"));
		for (int i = 0; i < urlCount; ++i) {
			String url_s = properties.getProperty("url-"+i, null);
			if (url_s!=null && ! url_s.isEmpty()) {
				try {
					@SuppressWarnings("unused")
					URL url = new URL(url_s);
					urls.add(url_s);
				} catch (MalformedURLException ignore) {
				}
			}
		}
	}
	
	private Properties loadProperties() throws IOException {
		Properties result = new Properties();
	
		Reader r = null;
		try {
			r = new FileReader(propertiesFile);
			result.load(r);
		} catch (FileNotFoundException ignore) {
			
		} finally {
			if (r != null) {
				try { r.close(); } catch (IOException ignore) {}
			}
		}
		return result;
	}

	@Override
	public void removeUrl(String url) {
		urls.remove(url);
	}

	@Override
	public boolean containsUrl(String url) {
		return urls.contains(url);
	}

	@Override
	public String[] getLoginUrls() {
		return urls.toArray(new String[urls.size()]);
	}

	@Override
	public void addUrl(String url) {
		urls.remove(url);
		urls.add(url);
	}

	@Override
	public void save() throws IOException {
		Writer w = null;
		
		try {
			Properties properties = loadProperties();
			
			int urlCount = Integer.parseInt(properties.getProperty("urlCount", "0"));
			for (int i = 0; i < urlCount; ++i) {
				properties.remove("url-"+i);
			}
			
			urlCount = 0;
			for (String url : urls) {
				properties.setProperty("url-"+urlCount, url);
				++urlCount;
			}
			properties.setProperty("urlCount", Integer.toString(urlCount));
			
			w = new FileWriter(propertiesFile);
			
			properties.store(w, "saved urls");
		}
		finally {
			if (w != null) {
				try { w.close(); } catch (IOException ignore) {}
			}
		}
		
	}
	
}
