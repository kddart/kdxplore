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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * @author brianp
 *
 */
public class BmsXlsTrialImportResult {

	public final Trial trial = new Trial();
	
	public final Map<String,Trait> trialTraits = new HashMap<>();

	public final SampleGroup sampleGroup = new SampleGroup();

	public final Map<String,String> trialAttributesByName = new HashMap<>();

	public final Map<BmsExcelSection,Map<String,SectionRowData>> attributesByDescriptionSection = new HashMap<>();

	public final List<Pair<String,Field>> plotFactorFields = new ArrayList<>();

	public Map<String,Set<Integer>> lineNumbersByMessage;
	
	public BmsXlsTrialImportResult() {
	}

	public void addDescriptionSectionAttribute(BmsExcelSection excelSection,
			String rnc1_value, 
			SectionRowData rowData) 
	{
		Map<String, SectionRowData> map = attributesByDescriptionSection.get(excelSection);
		if (map == null) {
			map = new LinkedHashMap<String, SectionRowData>();
			attributesByDescriptionSection.put(excelSection, map);
		}
		map.put(rnc1_value, rowData);
	}

	public void printOn(PrintWriter ps) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String json = gson.toJson(trial);
		ps.println("==== Trial ====");
		ps.println(json);

		ps.println("==== Samples ====");
		ps.println("Operator: " + sampleGroup.getOperatorName());
		ps.println("DeviceId: " + sampleGroup.getDeviceIdentifierId());
		ps.println("Samples:  " + sampleGroup.getSampleCount());

		ps.println("==== Plot Fields seen:");
		for (Pair<String,Field> pair : plotFactorFields) {
			ps.println("\t" + pair.first + "\tAS " + pair.second.getName());
		}

		printAttributes(ps, "Trial Fields", trialAttributesByName);

		for (BmsExcelSection excelSection : attributesByDescriptionSection.keySet()) {
			ps.println(" ==== " + excelSection.name() + ":");
			Map<String, SectionRowData> map = attributesByDescriptionSection.get(excelSection);
			
			for (String name : map.keySet()) {
				printRowData(ps, name, map.get(name));
			}
		}
		
		if (lineNumbersByMessage != null) {
			ps.println("=== Warnings ===");
			for (String msg : lineNumbersByMessage.keySet()) {
				Set<Integer> lineNumbers = lineNumbersByMessage.get(msg);
				ps.print(msg + ":");
				for (Integer lnum : lineNumbers) {
					ps.print(' ');
					ps.print(lnum);
				}
				ps.println();
			}
		}
	}
	
	private void printRowData(PrintWriter ps, String rowDataName, SectionRowData sectionRowData) 
	{
		sectionRowData.print(ps);
	}

	private void printAttributes(PrintWriter ps, String hdg, Map<String, String> attrByName) {
		ps.println(" ==== " + hdg + ":");
		for (String name : attrByName.keySet()) {
			String aValue = attrByName.get(name);
			if (Check.isEmpty(aValue)) {
				ps.println(name);
			}
			else {
				ps.println(name + "\t=" + aValue);
			}
		}
	}

	public void addPlotField(String factorName, Field field) {
		plotFactorFields.add(new Pair<>(factorName, field));
	}

}
