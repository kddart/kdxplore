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
package com.diversityarrays.kdxplore.heatmap;

import java.util.List;

import com.diversityarrays.kdxplore.curate.ValueRetriever;

/**
 * Collect the ValueRetrievers used by a given VisualisationTool that needs X,Y and Z.
 * Use this in conjunction with TraitInstanceData and the Plots.
 * see {@link #printValues(TraitInstanceData)}
 * 
 * @author brianp
 *
 */
public class AxesAndValues {

	public final ValueRetriever<?> xAxisValueRetriever;
	public final ValueRetriever<?> yAxisValueRetriever;
	public final List<ValueRetriever<?>> zValueRetrievers;

	public AxesAndValues(ValueRetriever<?> x, ValueRetriever<?> y, List<ValueRetriever<?>> zlist) {
		this.xAxisValueRetriever = x;
		this.yAxisValueRetriever = y;
		this.zValueRetrievers = zlist;
	}
	
}
