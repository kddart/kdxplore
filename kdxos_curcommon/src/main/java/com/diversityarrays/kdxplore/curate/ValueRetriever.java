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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.Predicate;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;

/**
 * Provides a mechanism to obtain values for use in Visualisation Tools;
 * either a value from 0..n for use along an graph axis, or the
 * "raw" value of the attribute. There broadly three sources:
 * <ul>
 * <li>The UserPlotId, PlotColumn or PlotRow (of the Plot)</li>
 * <li>The PlotAttributeValues for the Plot</li>
 * <li>The TraitInstance values for the Plot</li>
 * </ul>
 * @author brianp
 *
 * @param <T>
 */
public interface ValueRetriever<T extends Comparable<T>> {
	
	enum ValueType {
	    USER_PLOT_ID,
	    X_COLUMN,
	    Y_ROW,
        PLOT_TYPE,
        SPECIMEN_NUMBER,
        ATTACHMENT_COUNT,
	    PLOT_NOTE,
	    PLOT_TAGS,
	    PLOT_ATTRIBUTE,
	    TRAIT_INSTANCE,
	    ;
	}
	
	enum TrialCoord {
		NONE,
		PLOT_ID,
		X,
		Y
	}
	
	static public final Predicate<Plot> ONLY_ACTIVATED_PLOTS = new Predicate<Plot>() {
	    @Override
	    public boolean evaluate(Plot plot) {
	        return plot.isActivated();
	    }
	};

	static public final Set<ValueRetriever.TrialCoord> ONLY_X_Y = Collections.unmodifiableSet(new HashSet<>(
	        Arrays.asList(ValueRetriever.TrialCoord.X, ValueRetriever.TrialCoord.Y)));

	static public boolean isEitherOneXorY(ValueRetriever<?> xvr, ValueRetriever<?> yvr) {
	    return ONLY_X_Y.contains(xvr.getTrialCoord()) 
	            || 
	            ONLY_X_Y.contains(yvr.getTrialCoord());
	}

	ValueType getValueType();
	
	String getDisplayName();
	
	TrialCoord getTrialCoord();

	Class<T> getValueClass();
	
	T getAttributeValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos, T valueIfNull);

	/**
	 * Return a value from 0..n
	 * @param valueProvider
	 * @param plotId
	 * @param valueIfNull
	 * @return null if not available
	 */
	Integer getAxisValue(PlotInfoProvider infoProvider, PlotOrSpecimen pos);
	
	/**
	 * Return the value that corresponds to getAxisValue()==0.
	 * @return int
	 */
	int getAxisZeroValue();
	
	/**
	 * Return the number of values for this axis.
	 * If zero then can't be used for plotting. 
	 * @return
	 */
	int getAxisValueCount();
	
    default public boolean supportsGetAxisValue() {
        return true;
    }

    /**
     * Return true if this ValueRetriever is for the Plot column.
     * @return boolean
     */
    boolean isPlotColumn();
    /**
     * Return true if this ValueRetriever is for the Plot row.
     * @return boolean
     */
    boolean isPlotRow();
    
}
