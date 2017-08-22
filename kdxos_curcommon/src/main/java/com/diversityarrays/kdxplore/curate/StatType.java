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

import java.text.Format;

import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;

public enum StatType {
	MIN,
	MAX,
	MEDIAN,
	MEAN,
	MODE;
	
	public final String label;
	StatType() {
		label = Vocab.getStatTypeLabel(this.name());
	}
	
	public Object getStatValue(SimpleStatistics<?> statistics) {
		
		Object result = null;
		
		if (statistics!=null) {
			if (MODE==this) {
				result = statistics.getMode();
			}
			else {
				switch (this) {
				case MAX:
					result = statistics.getMaxValue();
					break;
				case MEAN:
					result = statistics.getMean();
					break;
				case MEDIAN:
					result = statistics.getMedian();
					break;
				case MIN:
					result = statistics.getMinValue();
					break;
				case MODE:
					
					break;
				default: 
					throw new RuntimeException("Unhandled StatType=" + this); //$NON-NLS-1$
				}
				
				if (result != null) {
					Format format = statistics.getFormat();
					if (format != null) {
						try { result = format.format(result); }
						catch (IllegalArgumentException e) {}
					}
				}
			}
		}
		
		return result;
	}
}
