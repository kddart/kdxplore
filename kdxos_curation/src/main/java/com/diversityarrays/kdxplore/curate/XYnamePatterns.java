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

import java.awt.Point;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diversityarrays.util.Either;

public class XYnamePatterns {
	
	public final Pattern xxpat;
	public final Pattern yypat;
	private String xname;
	private String yname;
	
	public XYnamePatterns(String xname, String yname) {
		this.xname = xname;
		this.yname = yname;
		// "^"+Pattern.quote(upfn)+"[0-9]+$"
		xxpat = Pattern.compile("^"+Pattern.quote(xname)+"([0-9]+)$");
		yypat = Pattern.compile("^"+Pattern.quote(yname)+"([0-9]+)$");
	}
	
	public Either<String,Point> parse(String uptext) {
		String[] parts = uptext.split("\\|", 0);
		String xpart = null;
		String ypart = null;
		
		for (int pi = parts.length; --pi>=0 && (xpart==null || ypart==null); ) {
			String p = parts[pi];
			Matcher m = null;
			if (xpart==null) {
				m = xxpat.matcher(p);
				if (m.matches()) {
					xpart = m.group(1);
				}
				else {
					m = null; // let yname checker know we didn't match
				}
			}
			if (ypart==null && m==null) {
				m = yypat.matcher(p);
				if (m.matches()) {
					ypart = m.group(1);
				}
				else {
					m = null;
				}
			}
		}
		
		if (xpart == null || ypart == null) {
			StringBuilder sb = new StringBuilder("Couldn't find: ");
			if (xpart==null) {
				sb.append("'").append(xname).append("'");
			}
			if (ypart==null) {
				if (xpart==null) sb.append(" OR ");
				sb.append("'").append(yname).append("'");
			}
			sb.append(" in '").append(uptext).append("'");
			return Either.left(sb.toString());
		}

		int x = Integer.parseInt(xpart, 10);
		int y = Integer.parseInt(ypart, 10);
		return Either.right(new Point(x,y));
	}
}
