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
package com.diversityarrays.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesLoginUrlsProvider implements LoginUrlsProvider {
	
	private Preferences preferences;
	private List<String> urls = new ArrayList<String>();
	
	public PreferencesLoginUrlsProvider(Preferences loginPrefs) {
		preferences = loginPrefs.node("loginUrls");
		
		int urlCount = preferences.getInt("urlCount", 0);
		for (int i = 0; i < urlCount; ++i) {
			String url = preferences.get("url-"+i, null);
			if (url!=null && ! url.isEmpty()) {
				urls.add(url);
			}
		}
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
		try {
			preferences.clear();
			int urlCount = 0;
			for (String url : urls) {
				preferences.put("url-"+urlCount, url);
				++urlCount;
			}
			preferences.putInt("urlCount", urlCount);
			
			preferences.flush();
		} catch (BackingStoreException e) {
			throw new IOException(e);
		}
	}

}