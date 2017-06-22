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
package com.diversityarrays.kdcompute.db;

import java.util.Arrays;
import java.util.List;

import com.diversityarrays.util.Check;

public enum DataSetType {
    ANY,  // validation: any text
    FILE_REF, // a file in the user's file repository
    URL, // just somewhere on the web
    ANALYSIS_GROUP, // list/analysisgroup/100/page/1?ctype=json&callback=?
    MARKER_DATASET,;

    static public final List<String> URL_PREFIXES;
    static {
        URL_PREFIXES = Arrays.asList(
                "http://",
                "https://",
                "ftp://");
    }
    public static DataSetType classify(String srcURL, DataSetType defalt) {
        DataSetType result = defalt;
        if (! Check.isEmpty(srcURL)) {
            String src = srcURL.toLowerCase();
            if (src.contains("/analysisgroup/")) {
                result = ANALYSIS_GROUP;
            }
            else if (src.contains("/markerdataset/")) {
                result = MARKER_DATASET;
            }
            else if (src.startsWith("file://")) {
                result = FILE_REF;
            }
            else {
                for (String prefix : URL_PREFIXES) {
                    if (src.startsWith(prefix)) {
                        result = URL;
                        break;
                    }
                }
            }
        }
        // TODO Auto-generated method stub
        return result;
    } // list/type/markerdataset/active?ctype=json&callback=?
}
