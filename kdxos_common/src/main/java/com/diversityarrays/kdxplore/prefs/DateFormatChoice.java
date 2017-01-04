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
package com.diversityarrays.kdxplore.prefs;

@SuppressWarnings("nls")
public enum DateFormatChoice {

    SLASH_YYYY_MMM_DD("yyyy/MMM/dd"),
    SLASH_YYYY_MM_DD("yyyy/MM/dd"),
    SLASH_YY_MM_DD("yy/MM/dd"),
    SLASH_DD_MM_YYYY("dd/MM/yyyy"),
    SLASH_MM_DD_YYYY("MM/dd/yyyy"),
    
    DASH_YYYY_MMM_DD("yyyy-MMM-dd"),
    DASH_YYYY_MM_DD("yyyy-MMM-dd"),
    DASH_YY_MM_DD("yy-MM-dd"),
    DASH_DD_MM_YYYY("dd-MM-yyyy"),
    DASH_MM_DD_YYYY("MM-dd-yyyy"),
    ;
    
    public final String fmt;
    DateFormatChoice(String fmt) {
        this.fmt = fmt;
    }
    
    @Override
    public String toString() {
        return fmt;
    }
}
