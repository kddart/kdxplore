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
package com.diversityarrays.kdxplore.stats;

import java.text.Format;

import net.pearcan.exhibit.ExhibitColumn;

import org.apache.commons.collections15.Bag;

public interface SimpleStatistics<T extends Comparable<?>> {

	static public enum StatName {
		N_SAMPLES("# samples"),
		N_VALID("# valid"),
		N_INVALID("# invalid"),
		N_MISSING("# missing"),
		N_NA("# NA"),
		MIN_VALUE("Min"),
		MAX_VALUE("Max"),
		MEAN_VALUE("Mean"),
		MODE_VALUES("Mode"),
		MEDIAN_VALUE("Median"),
		QUARTILE_1("Q1"),
		QUARTILE_3("Q3"),
		VARIANCE("Variance"),
		STD_DEV("Std Dev"),
		N_OUTLIERS("# Outliers"),
		STD_ERR("Std Err"),
		;
		
		public final String displayName;
		StatName(String s) {
			displayName = s;
		}
		
		@Override
		public String toString() {
			return displayName;
		}
	}
	

	
	static public final String GROUP_COUNTS = "Counts";
	static public final String GROUP_RANGE = "Range";
	static public final String GROUP_BASIC_STATS = "Basic Stats";
	static public final String GROUP_MORE_STATS = "More Stats";
	
	public String getStatsName();

	public Class<? extends T> getValueClass();

	public Format getFormat();
	
	public Bag<T> getLowOutliers();

	public Bag<T> getHighOutliers();

	// If adding more methods without @Exhibit annotation, 
	// add to the "okIfExhibitMissing" in StatsUtil.initCheck()

	
	@ExhibitColumn(value="# samples", group=GROUP_BASIC_STATS, order=1)
	public Integer getSampleMeasurementCount();

	@ExhibitColumn(value="# valid", group=GROUP_COUNTS, order=2)
	public Integer getValidCount();

	@ExhibitColumn(value="# invalid", group=GROUP_COUNTS, order=3)
	public Integer getInvalidCount();

	@ExhibitColumn(value="# missing", group=GROUP_COUNTS, order=4)
	public Integer getMissingCount();
	
	@ExhibitColumn(value="# NA", group=GROUP_COUNTS, order=5)
	public Integer getNA_Count();

	@ExhibitColumn(value="Min", group=GROUP_BASIC_STATS, order=6)
	public T getMinValue();

	@ExhibitColumn(value="Max", group=GROUP_BASIC_STATS, order=7)
	public T getMaxValue();

	@ExhibitColumn(value="Mean", group=GROUP_BASIC_STATS, order=8)
	public T getMean();

	@ExhibitColumn(value="Median", group=GROUP_BASIC_STATS, order=9)
	public T getMedian();

	@ExhibitColumn(value="Mode", group=GROUP_BASIC_STATS, order=10)
	public String getMode();

	@ExhibitColumn(value="Q1", group=GROUP_BASIC_STATS, order=11)
	public T getQuartile1();

	@ExhibitColumn(value="Q3", group=GROUP_BASIC_STATS, order=12)
	public T getQuartile3();
	
	@ExhibitColumn(value="# Outliers", group=GROUP_BASIC_STATS, order=13)
	public Integer getOutlierCount();

	@ExhibitColumn(value="Variance", group=GROUP_MORE_STATS, order=14)
	public Double getVariance();

	@ExhibitColumn(value="Std Dev", group=GROUP_MORE_STATS, order=15)
	public Double getStddev();

	@ExhibitColumn(value="Std Err", group=GROUP_MORE_STATS, order=16)
	public Double getStderr();
	
}
