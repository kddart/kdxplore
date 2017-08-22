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
package com.diversityarrays.kdxplore.curate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.diversityarrays.kdxplore.prefs.ExplorerProperties;

class ReasonsCache {
	
	static private ReasonsCache singleton;
	static public ReasonsCache getInstance() {
		if (singleton == null) {
			synchronized (ReasonsCache.class) {
				if (singleton == null) {
					singleton = new ReasonsCache();
				}
			}
		}
		return singleton;
	}
	
	static public File getReasonsFile() {
		return new File(ExplorerProperties.getInstance().getUserDataDirectory(),
				"excludeReasons.txt"); //$NON-NLS-1$
	}
	
	List<String> reasons = new ArrayList<>();
	
	private ReasonsCache() {
		File reasonsFile = getReasonsFile();
		if (reasonsFile.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(reasonsFile));
				String line;
				while (null != (line = br.readLine())) {
					line = line.trim();
					if (! line.isEmpty()) {
						reasons.add(line);
					}
				}
			} catch (IOException ignore) {
			} finally {
                if (br != null) {
                    try { br.close(); } catch (IOException ignore) {}
                }
			}
		}
	}
	
	public void addReason(String reason) {
		int pos = reasons.indexOf(reason);
		if (pos > 0) {
			reasons.remove(pos);
		}
		if (pos != 0) {
			reasons.add(0, reason);
			try {
				PrintWriter pw = new PrintWriter(getReasonsFile());
				for (String line : reasons) {
					pw.println(line);
				}
				pw.close();
			} catch (FileNotFoundException ignore) {
			}
		}
	}
	
	public String[] getReasons() {
		return reasons.toArray(new String[reasons.size()]);
	}
}
