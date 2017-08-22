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

import java.util.List;

import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class SuppressionArgs {
    public final boolean askAboutValueForUnscored;
	public final List<CurationCellValue> ccvs;
	public final List<KdxSample> editedSamples;
	public final List<KdxSample> nonEditedSamples;
	
	public SuppressionArgs(
	        boolean askAboutValueForUnscored,
	        List<CurationCellValue> ccvs,
			List<KdxSample> editedSamples,
			List<KdxSample> nonEditedSamples)
	{
	    this.askAboutValueForUnscored = askAboutValueForUnscored;
		this.ccvs = ccvs;
		this.editedSamples = editedSamples;
		this.nonEditedSamples = nonEditedSamples;
	}
}
