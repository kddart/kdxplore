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
package com.diversityarrays.kdxplore.exportdata;

import java.io.File;

import com.diversityarrays.kdsmart.db.entities.TraitValue;

public class ExportOptions {
	
	public enum PlotAttrAsNumber {
		NONE("None"),
		ENDING_WITH_NO("if Name ends with '_NO'"),
		ALL("All");
		
		private final String displayValue;
		PlotAttrAsNumber(String v) {
			displayValue = v;
		}
		
		@Override
		public String toString() {
			return displayValue;
		}
	}
	
	public File file;
	
	public final boolean bmsFormat = false;
	public boolean excelFormat; // TODO implement this
	public boolean colourOutputCells;
	public boolean includeDateTimeMeasured;

	public ExportOptions.PlotAttrAsNumber plotAttributeAsNumber;

	public Iterable<Integer> modelRows;
	public boolean showTrialName;
	
	public WhichTraitInstances whichTraitInstances = WhichTraitInstances.ALL_WITH_DATA;
	public boolean allPlotAttributes;

	public String nameForColumn = "";
	public String nameForRow = "";

	public String unscoredValueString = TraitValue.EXPORT_VALUE_UNSCORED;
	public String naValueString = TraitValue.EXPORT_VALUE_NA;
	public String missingValueString = TraitValue.EXPORT_VALUE_MISSING;
	public String suppressedValueString = "";
	
	// null means don't export
	public String exportInactiveTraitValue;
}
