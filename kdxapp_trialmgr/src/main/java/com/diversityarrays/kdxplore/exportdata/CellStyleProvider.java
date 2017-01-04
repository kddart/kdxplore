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

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

import com.diversityarrays.kdxplore.importdata.bms.BmsCellColor;

public class CellStyleProvider {

	private final Map<BmsCellColor,CellStyle> styleByBmsCellColor = new HashMap<>();
	
	private final Workbook workbook;

	private CellStyle boldHeading;

	private CellStyle dateFormat;

	public CellStyleProvider(Workbook workbook) {
		this.workbook = workbook;
	}
	

	public CellStyle getBoldHeading() {
		if (boldHeading == null) {
			boldHeading = workbook.createCellStyle();
			
			Font font = workbook.createFont();
			font.setBold(true);
			boldHeading.setFont(font);
		}
		return boldHeading;
	}
	
	public CellStyle getCellStyle(BmsCellColor bmsCellColor) {
		
		CellStyle result = styleByBmsCellColor.get(bmsCellColor);

		if (result == null) {
			
			result = workbook.createCellStyle();
			
			result.setFillPattern(CellStyle.SOLID_FOREGROUND);
			result.setFillForegroundColor(bmsCellColor.indexedColor.getIndex());
			
			Font font = workbook.createFont();
			font.setColor(IndexedColors.WHITE.getIndex());
			result.setFont(font);

			styleByBmsCellColor.put(bmsCellColor, result);
		}
		
		return result;
	}


	public CellStyle getDateFormat() {
		if (dateFormat == null) {
			dateFormat = workbook.createCellStyle();
			dateFormat.setDataFormat(workbook.getCreationHelper()
					.createDataFormat()
					.getFormat("yyyy/mm/dd hh:mm:ss")); //$NON-NLS-1$
		}
		return dateFormat;
	}

}
