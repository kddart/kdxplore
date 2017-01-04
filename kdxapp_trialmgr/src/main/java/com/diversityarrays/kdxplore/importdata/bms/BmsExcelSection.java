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

import com.diversityarrays.kdxplore.importdata.bms.BmsExcelImportHelper.CellValueConsumer;
import com.diversityarrays.kdxplore.importdata.bms.BmsExcelImportHelper.PlotAttributeConsumer;
import com.diversityarrays.kdxplore.importdata.bms.BmsExcelImportHelper.TraitConsumer;

public enum BmsExcelSection {
	TRIAL_ATTRIBUTES, // Value is in the Description worksheet
	PLOT_ATTRIBUTES, // Value is in the Observation worksheet
	TRAITS; // Value is in the Observation worksheet

	/**
	 * Return whether the data for this Section gets its value from the
	 * Description worksheet.
	 * 
	 * @return
	 */
	public boolean isValueFromDescriptionWorksheet() {
		return this == TRIAL_ATTRIBUTES;
	}

	public CellValueConsumer createCellValueConsumer(String attributeName) {
		switch (this) {
		case TRIAL_ATTRIBUTES:
			throw new IllegalArgumentException("createCellValueConsumner("
					+ attributeName + ") not supported for " + this);
		case PLOT_ATTRIBUTES:
			return new PlotAttributeConsumer(attributeName);
		case TRAITS:
			return new TraitConsumer(attributeName);
		}
		return null;
	}
}
