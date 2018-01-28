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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.TreeBag;

public abstract class AbstractSimpleStatistics<T extends Comparable<? super T>> implements SimpleStatistics<T> {

	private final String statsName;
	
	protected int nSampleMeasurements;
	protected  int nValidValues;
	protected int nInvalid;
	protected int nMissing;
	protected int nNA;

	protected Integer nOutliers;
	
	protected Double variance;
	protected Double stddev;
	
	protected Double stderr;
	
	protected String mode;
	protected final Set<String> modeValues = new HashSet<>();
	
	protected final Class<? extends T> valueClass;
	
	protected final Bag<T> lowOutliers = new TreeBag<>();
	protected final Bag<T> highOutliers = new TreeBag<>();

	public AbstractSimpleStatistics(String statsName, Class<? extends T> vc) {
		this.statsName = statsName;
		this.valueClass = vc;
	}
	
	public String getStatsName() {
		return statsName;
	}
	
	@Override
	final public Class<? extends T> getValueClass() {
		return valueClass;
	}
	
	@Override
	public Bag<T> getLowOutliers() {
		return lowOutliers;
	}

	@Override
	public Bag<T> getHighOutliers() {
		return highOutliers;
	}
	
	@Override
	final public Integer getSampleMeasurementCount() {
		return nSampleMeasurements;
	}

	@Override
	final public Integer getValidCount() {
		return nValidValues;
	}

	@Override
	final public Integer getInvalidCount() {
		return nInvalid;
	}

	@Override
	final public Integer getMissingCount() {
		return nMissing;
	}
	
	@Override
	final public Integer getNA_Count() {
		return nNA;
	}

	@Override
	final public Integer getOutlierCount() {
		return nOutliers;
	}

	@Override
	final public String getMode() {
		return mode;
	}

	@Override
	final public Double getVariance() {
		return variance;
	}

	@Override
	final public Double getStddev() {
		return stddev;
	}

	@Override
	final public Double getStderr() {
		return stderr;
	}
}
