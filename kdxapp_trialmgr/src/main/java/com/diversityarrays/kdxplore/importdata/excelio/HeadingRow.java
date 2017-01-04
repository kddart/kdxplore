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
package com.diversityarrays.kdxplore.importdata.excelio;

import java.util.LinkedHashMap;
import java.util.Map;

public class HeadingRow {
    /**
     * Collects the ImportFields when they are found by matching the heading
     * (case insensitively) to the ImportField's wsHeading.
     */
    public final Map<Integer, ImportField> importFieldByColumnIndex = new LinkedHashMap<>();
    /**
     * If a heading does NOT match an ImportField then it is collected here for those
     * worksheets that may need to to further processing; Plots and Specimens both do that.
     */
    public final Map<Integer, String> headingByColumnIndex = new LinkedHashMap<>();
}
