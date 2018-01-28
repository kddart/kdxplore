/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.field;

public class PlotNameImpl implements PlotName {
	public final Integer x;
	public final Integer y;
	public final Integer plotId;

	public PlotNameImpl(String s) {
		int cpos = s.indexOf(':');
		if (cpos < 0) {
			String[] parts = s.split(",");
			plotId = null;
			try {
				switch (parts.length) {
				case 2:
					this.y = Integer.parseInt(parts[1]);
					this.x = Integer.parseInt(parts[0]);
					break;
				default:
					throw new IllegalArgumentException(s);
				}
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else if (cpos == 0) {
			this.x = null;
			this.y = null;
			plotId = new Integer(s.substring(cpos+1));
		}
		else if (cpos > 0) {
			// x,y:p
			String[] parts = s.substring(0, cpos).split(",");
			try {
				switch (parts.length) {
				case 2:
					this.x = parts[0].isEmpty() ? null : new Integer(parts[0]);
					this.y = parts[1].isEmpty() ? null : new Integer(parts[1]);
					break;
				default:
					throw new IllegalArgumentException(s);
				}
				plotId = new Integer(s.substring(cpos+1));
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			throw new IllegalArgumentException(s);
		}
		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (plotId != null) {
			sb.append("P_").append(plotId);
			sb.append(":");
		}
		sb.append(x==null ? "." : x);
		sb.append(",");
		sb.append(y==null ? "." : y);
		return sb.toString();
		
	}

	@Override
	public Integer getX() {
		return x;
	}

	@Override
	public Integer getY() {
		return y;
	}

	@Override
	public Integer getPlotId() {
		return plotId;
	}
}
