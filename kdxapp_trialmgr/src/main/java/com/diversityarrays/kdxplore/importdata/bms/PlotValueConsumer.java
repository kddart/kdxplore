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
package com.diversityarrays.kdxplore.importdata.bms;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.util.CreateItemException;
import com.diversityarrays.kdxplore.trialmgr.ImportDataException;

public interface PlotValueConsumer {
	public void storePlotAttributeValue(Plot plot, String attributeName,
			String attributeValue) throws CreateItemException;

	public void storeTraitValue(Plot plot, String traitName,
			String traitValue) throws CreateItemException, ImportDataException;

	public void plotComplete(int lineNumber, Plot plot)
			throws CreateItemException;

	public void warn(int lineNumber, String msg);

	public void createTraitInstance(String traitName) throws CreateItemException;
}
