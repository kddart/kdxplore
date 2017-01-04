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
package com.diversityarrays.util;

import java.util.Collection;
import java.util.Map;

public class Check {

	static public boolean isEmpty(Collection<?> coll) {
		return coll==null || coll.isEmpty();
	}
	
	static public boolean isEmpty(Map<?,?> coll) {
		return coll==null || coll.isEmpty();
	}
	
	static public boolean isEmpty(String s) {
		return s==null || s.isEmpty();
	}
	
	static public boolean isEmpty(CharSequence cs) {
		return cs==null || cs.length()<=0;
	}


	
	private Check() {}
}
