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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.pearcan.excel.ExcelUtil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

@SuppressWarnings("nls")
public class SectionRowData {
	
	static public String[] splitValuePrefix(String input) {

		List<String> result = new ArrayList<>();
		
		int cpos = input.indexOf(':');
		if (cpos >= 0) {
			result.add(input.substring(0, cpos));
			String rest = input.substring(cpos+1);
			if (! rest.isEmpty()) {
				Collections.addAll(result, rest.split("\\|"));
			}
		}
		else {
			result.add(input);
		}
		return result.toArray(new String[result.size()]);
	}
	
	static public List<String> splitForOutput(String input, String[] allHeadings) {
		List<String> output = new ArrayList<>();
		
		String value;
		String tail;
		int cpos = input.indexOf(':');
		if (cpos >= 0) {
			value = input.substring(0, cpos);
			tail = input.substring(cpos+1);
		}
		else {
			value = input;
			tail = "";
		}

		String[] rest = tail.split("\\|");
		
		int index = 0;
		for (String hdg : allHeadings) {
			if (BmsConstant.VALUE_HEADING.equals(hdg)) {
				output.add(value);
			}
			else {
				if (index < rest.length) {
					output.add(rest[index]);
				}
				else {
					output.add("");
				}
				++index;
			}
		}
		
		return output;
	}

	private final Map<String,String> valueByHeading = new LinkedHashMap<>();

	private final String lastHeading;
	
	public SectionRowData(String lastHeading) { 
		this.lastHeading = lastHeading;
	}

	public void collectFrom(Row row) {
		int cellnum = 0;
		for (String hdg : BmsConstant.FIRST_SIX_OTHER_HEADINGS) {
			String value = getCellValue(row, ++cellnum);
			valueByHeading.put(hdg, value);
		}

		String value = getCellValue(row, ++cellnum);
		valueByHeading.put(lastHeading, value);
	}
	
	public String getVALUEthenRest() {
		StringBuilder sb = new StringBuilder();
		sb.append(getVALUE());
		String sep = ":";
		for (String hdg : valueByHeading.keySet()) {
			if (! BmsConstant.VALUE_HEADING.equals(hdg)) {
				sb.append(sep).append(valueByHeading.get(hdg));
				sep = "|";
			}
		}
		return sb.toString();
	}

	
	public String getDESCRIPTION() {
		return valueByHeading.get(BmsConstant.DESCRIPTION_HEADING);
	}
	
	public String getPROPERTY() {
		return valueByHeading.get(BmsConstant.PROPERTY_HEADING);
	}
	
	public String getSCALE() {
		return valueByHeading.get(BmsConstant.SCALE_HEADING);
	}
	
	public String getMETHOD() {
		return valueByHeading.get(BmsConstant.METHOD_HEADING);
	}
	
	public String getDATA_TYPE() {
		return valueByHeading.get(BmsConstant.DATA_TYPE_HEADING);
	}
	
	public String getVALUE() {
		return valueByHeading.get(BmsConstant.VALUE_HEADING);
	}
	
	// Next two are actually dependent on the type
	
	private String getCellValue(Row row, int cellnum) {
		Cell cn = row.getCell(cellnum);
		String cn_value = cn == null ? "" : 
			ExcelUtil.getCellStringValue(cn, "");
		return cn_value;
	}

	public void print(PrintWriter ps) {
		for  (String hdg : valueByHeading.keySet()) {
			ps.print(hdg);
			String val = valueByHeading.get(hdg);
			if (val!=null && ! val.isEmpty()) {
				ps.print("\t="+val);
			}
			ps.println();
		}
	}

}
