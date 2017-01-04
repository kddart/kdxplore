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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections15.Factory;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

import com.diversityarrays.kdsmart.db.csvio.ImportAsHeading;
import com.diversityarrays.kdsmart.db.entities.KDSmartDbEntity;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.CreateItemException;
import com.diversityarrays.kdsmart.db.util.InvalidValueException;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.exportdata.ExportOptions;
import com.diversityarrays.kdxplore.trialmgr.ImportDataException;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

import net.pearcan.excel.ExcelUtil;
import net.pearcan.util.StringUtil;

public class BmsExcelImportHelper {
	
	
	static private Field findField(Class<?> startClass, String fieldName) {
		Class<?> use = startClass;
		Field field = null;
		while (field == null) {
			try {
				field = use.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				if (KDSmartDbEntity.class.equals(use)) {
					break;
				}
				use = use.getSuperclass();
			}
		}					
		if (field == null) {
			throw new IllegalArgumentException("No such field: " + startClass.getSimpleName() + "." + fieldName);
		}
		
		return field;
	}
	
	static public final Map<String,Field> PLOT_FIELD_BY_FACTOR_NAME;
	static {
		Map<String,Field> map = new HashMap<>();
		for (String factorName : BmsConstant.PLOT_FIELD_NAME_BY_FACTOR.keySet()) {
			String plotFieldName = BmsConstant.PLOT_FIELD_NAME_BY_FACTOR.get(factorName);
			Field field = findField(Plot.class, plotFieldName);
			field.setAccessible(true);
			map.put(factorName, field);
		}
		
		PLOT_FIELD_BY_FACTOR_NAME = Collections.unmodifiableMap(map);
	}
	
	static public final Map<String,Field> TRIAL_FIELD_BY_CONDITION_NAME;
	static {
		Map<String,Field> map = new HashMap<>();
		for (String condName : BmsConstant.TRIAL_FIELD_NAME_BY_CONDITION.keySet()) {
			String fieldName = BmsConstant.TRIAL_FIELD_NAME_BY_CONDITION.get(condName);
			
			Field field = findField(Trial.class, fieldName);
			field.setAccessible(true);
			
			map.put(condName, field);
		}
		
		TRIAL_FIELD_BY_CONDITION_NAME = Collections.unmodifiableMap(map);
	}

	static public Map<String, Field> getFieldByFactorName(ExportOptions options) {
		
		Collection<Field> plotFields = PLOT_FIELD_BY_FACTOR_NAME.values();

		Map<String,Field> fieldNameByName = new HashMap<>();
		for (Field field : plotFields) {
			fieldNameByName.put(field.getName(), field);
		}
		
		Map<String,Field> fieldByFactorName = new TreeMap<>();
		
		for (String fieldName : fieldNameByName.keySet()) {
			Field field = fieldNameByName.get(fieldName);
			if (Plot.FIELDNAME_PLOT_TYPE.equals(fieldName)) {
				fieldByFactorName.put(BmsConstant.XLSHDG_ENTRY_TYPE, field);
			}
			else if (Plot.FIELDNAME_USER_PLOT_ID.equals(fieldName)) {
				fieldByFactorName.put(BmsConstant.XLSHDG_PLOT_NO, field);
			}
			else if (Plot.FIELDNAME_PLOT_ROW.equals(fieldName)) {
				String name = options.nameForRow;
				if (Check.isEmpty(name)) {
					name = BmsConstant.XLSHDG_FIELDMAP_RANGE;
				}
				fieldByFactorName.put(name, field);
			}
			else if (Plot.FIELDNAME_PLOT_COLUMN.equals(fieldName)) {
				String name = options.nameForColumn;
				if (Check.isEmpty(name)) {
					name = BmsConstant.XLSHDG_FIELDMAP_COLUMN;
				}
				fieldByFactorName.put(name, field);
			}
			else {
				// TODO error
			}
		}
		
		return fieldByFactorName;
	}
	
	public static final String SHEET_NAME_OBSERVATION = "Observation";

	public static final String SHEET_NAME_DESCRIPTION = "Description";

	

	static class FactorEntityField {

		public final String factorName;
		public final Class<? extends KDSmartDbEntity> entityClass;
		public final Field field;

		public FactorEntityField(String factorName,
				Class<? extends KDSmartDbEntity> clz, Field field) {
			this.factorName = factorName;
			this.entityClass = clz;
			this.field = field;
		}
	}

	static public class SheetData {


		class Section {

			private final BmsExcelSection excelSection;
			/**
			 * The "foregroundColorColor" for RnC1.
			 */
			private final BmsCellColor sectionColor;
			/**
			 * The heading text to be found in column 1.
			 */
			private final String sectionHeadingRnC1;

			Section(BmsExcelSection es, BmsCellColor cc, String rnc1_heading) {
				this.excelSection = es;
				this.sectionColor = cc;
				this.sectionHeadingRnC1 = rnc1_heading;
			}

			@Override
			public String toString() {
				return this.excelSection.name();
			}
		}

		private static final String TAG = SheetData.class.getSimpleName();

		private final Map<String, Field> fieldByHeaderRnC1 = new HashMap<>();

		private final Map<String, String> trialAttributeNameByHeading = new HashMap<>();

		private final List<Section> sections = new ArrayList<>();

		SheetData() {
		}

		public Map<String, String> getTrialAttributeNameByHeading() {
			return trialAttributeNameByHeading;
		}
		
//		public Map<String, Field> getPlotFieldByFactorName(BmsExcelSection es) {
//			for (Section section : sections) {
//				if (es.equals(section.excelSection)) {
//					return section.plotFieldByFactorName;
//				}
//			}
//			return null;
//		}
		
		// Values are in column 2
		public void addTrialAttribute(String rnc1_heading,
				String rnc2_trialAttributeName) {
			trialAttributeNameByHeading.put(rnc1_heading,
					rnc2_trialAttributeName);
		}

		public void addTrialHeaderField(String rnc1_heading, String fieldName) {
			try {
				Field field = Trial.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				fieldByHeaderRnC1.put(rnc1_heading, field);
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("No such field: Trial." + fieldName);
			}
		}

		public BmsXlsTrialImportResult scan(HSSFSheet worksheet,
				String trialNameInstanceSeparator,
				Set<String> lowcaseTrialNames, List<SectionData> sectionDataList)
				throws IOException {

			BmsXlsTrialImportResult scanResult = new BmsXlsTrialImportResult();

			int rowCount = ExcelUtil.getRowCount(worksheet);

			SectionData currentSectionData = null;
			for (int rownum = 0; rownum < rowCount; ++rownum) {
				Row row = worksheet.getRow(rownum);
				if (row == null)
					continue;

				int cellCount = ExcelUtil.getCellCount(row);
				if (cellCount <= 0) {
					// Note that this will also skip the empty row at the end of
					// "mySection"
					continue;
				}

				Cell rnc1 = row.getCell(0);
				if (rnc1 == null)
					continue;

				CellStyle cellStyle = rnc1.getCellStyle();
				if (!(cellStyle instanceof org.apache.poi.hssf.usermodel.HSSFCellStyle)) {
					continue;
				}

				String rnc1_value = ExcelUtil.getCellStringValue(rnc1, "");

				org.apache.poi.hssf.usermodel.HSSFCellStyle hssfCellStyle = (org.apache.poi.hssf.usermodel.HSSFCellStyle) cellStyle;
				String fg = hssfCellStyle.getFillForegroundColorColor()
						.getHexString();

				if (BmsCellColor.NO_COLOR.hexString.equals(fg)) {
					if (currentSectionData != null) {
						currentSectionData.processRow(row, cellCount,
								rnc1_value, scanResult);
					}
				} else if (BmsCellColor.TRIAL_NAME_ETC_BROWN.hexString.equals(fg)) {
					if (cellCount > 1) {
						Cell rnc2 = row.getCell(1);
						if (rnc2 != null) {
							String rnc2_value = ExcelUtil.getCellStringValue(
									rnc2, "");
							rnc2_value = rnc2_value.trim();
							// In the heading section?
							Field field = fieldByHeaderRnC1.get(rnc1_value);
							if (field != null) {
								storeValueInEntity(Trial.class,
										scanResult.trial, field, rnc2_value);
							} 
							
							scanResult.trialAttributesByName.put(rnc1_value, rnc2_value);
//							// Not a Trial.field
//							String trialAttributeName = trialAttributeNameByHeading
//									.get(rnc1_value);
//							if (trialAttributeName != null) {
//							}
						}
					}
				} else {
					Shared.Log.d(TAG, "scan: looking for fg=" + fg);
					for (SectionData sectionData : sectionDataList) {
						Section section = sectionData.section;
						if (section.sectionColor.hexString.equals(fg)
								&& section.sectionHeadingRnC1.equals(rnc1_value)) 
						{

							if (sectionData.getValueCellnum() > 0) {
								throw new IllegalStateException(
										"Section '"
												+ section.sectionHeadingRnC1
												+ "' already scanned with 'VALUE' in column "
												+ sectionData.getValueCellnum());
							}
							currentSectionData = sectionData;

							currentSectionData.init(row, cellCount);
							Shared.Log.d(TAG, "\tFOUND "
									+ currentSectionData.section
									+ " with 'VALUE' in column "
									+ currentSectionData.getValueCellnum());
							break;
						}
					}
				}
			}
			// Some of the TrialAttributes change what we do in the Trial...
			if (DESCRIPTION == this && trialNameInstanceSeparator != null
					&& !trialNameInstanceSeparator.isEmpty()) 
			{
				
				String rawTrialName = scanResult.trial.getTrialName();
				
				System.out.println(scanResult.trialAttributesByName);
				
//				Map<String, SectionRowData> map = scanResult.attributesByDescriptionSection
//						.get(BmsExcelSection.TRIAL_ATTRIBUTES);

				String trialInstanceRaw = scanResult.trialAttributesByName.get(BmsConstant.CIMMYT_TRIAL_INSTANCE);
				if (trialInstanceRaw != null && !trialInstanceRaw.isEmpty()) {
					
					String[] valueAndRest = SectionRowData.splitValuePrefix(trialInstanceRaw);
					
					String trialInstanceValue = valueAndRest[0];
					String trialName = rawTrialName
							+ trialNameInstanceSeparator
							+ trialInstanceValue;

					if (lowcaseTrialNames.contains(trialName.toLowerCase())) {
						throw new IOException("Trial Name '"
								+ trialName + "' already exists");
					}

					scanResult.trial.setTrialName(trialName);
				}
				
				String studyAbbr = scanResult.trialAttributesByName.get(BmsConstant.CIMMYT_STUDY_ABBR);
				if (studyAbbr != null && ! studyAbbr.isEmpty()) {
					scanResult.trial.setTrialAcronym(studyAbbr);
				}
			}

			return scanResult;
		}

		public Section startSection(BmsExcelSection es, BmsCellColor cc,
				String rnc1_heading) {
			for (Section s : sections) {
				if (cc.equals(s.sectionColor)
						&& rnc1_heading.equals(s.sectionHeadingRnC1)) {
					throw new IllegalArgumentException(s.toString()
							+ " already defined with " + cc + "/"
							+ rnc1_heading);
				}
			}

			Section section = new Section(es, cc, rnc1_heading);
			sections.add(section);

			return section;
		}
	}

	static private void storeValueInEntity(
			Class<? extends KDSmartDbEntity> cls, KDSmartDbEntity entity,
			Field field, String cell_value) throws IOException {
		try {
			Object valueToStore = ImportAsHeading.checkAndConvertValue(null,
					cls, field, cell_value);
			field.set(entity, valueToStore);
		} catch (InvalidValueException e) {
			throw new IOException(e);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IOException(e);
		}
	}

	static public final SheetData DESCRIPTION = new SheetData();

	static {
		DESCRIPTION.addTrialHeaderField(BmsConstant.CIMMYT_STUDY,
				Trial.FIELD_NAME_TRIAL_NAME);
		
		DESCRIPTION.addTrialHeaderField(BmsConstant.CIMMYT_TITLE,
				Trial.FIELD_NAME_TRIAL_NOTE);

		DESCRIPTION.addTrialAttribute(BmsConstant.CIMMYT_START_DATE, "StartDate"); //$NON-NLS-1$
		DESCRIPTION.addTrialAttribute("END DATE", "EndDate");  //$NON-NLS-1$//$NON-NLS-2$
		DESCRIPTION.addTrialAttribute("STUDY TYPE", "StudyType"); //$NON-NLS-1$ //$NON-NLS-2$

		DESCRIPTION.startSection(
				BmsExcelSection.TRIAL_ATTRIBUTES, 
				BmsCellColor.CONDITION_OR_FACTOR_GREEN, 
				BmsConstant.SECTION_NAME_CONDITION);
		// TODO Redmine#2375: recognise all other rows as TrialAttribute if they
		// have a non-blank value

		DESCRIPTION.startSection(BmsExcelSection.PLOT_ATTRIBUTES, 
						BmsCellColor.CONDITION_OR_FACTOR_GREEN, 
						BmsConstant.SECTION_NAME_FACTOR);

		DESCRIPTION.startSection(BmsExcelSection.TRAITS, 
				BmsCellColor.CONSTANT_OR_VARIATE_PURPLE,
				BmsConstant.SECTION_NAME_VARIATE);
	}

	private final File excelFile;
	private HSSFSheet description;
	private HSSFSheet observation;
	
	public BmsExcelImportHelper(File excelFile) throws IOException {
		this.excelFile = excelFile;
		if (!excelFile.getName().toLowerCase().endsWith(".xls")) { //$NON-NLS-1$
			throw new IllegalArgumentException("Only .xls files supported");
		}

		POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(excelFile));

		HSSFWorkbook workbook = null;
		workbook = new HSSFWorkbook(fs);

		try {
			List<String> missing = new ArrayList<>();
			description = workbook.getSheet(SHEET_NAME_DESCRIPTION);
			if (null == description) {
				missing.add(SHEET_NAME_DESCRIPTION);
			}
			observation = workbook.getSheet(SHEET_NAME_OBSERVATION);
			if (null == observation) {
				missing.add(SHEET_NAME_OBSERVATION);
			}

			if (!missing.isEmpty()) {
				throw new IOException(StringUtil.join(
						"Missing required worksheet(s): ", ",", missing));
			}

		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException ignore) {
				}
			}
		}
	}

//	public void setTrialPlantingDate(Date d) {
//		trialPlantingDate = d;
//	}
	
	public File getExcelFile() {
		return excelFile;
	}

	interface CellValueConsumer {
		public int getSortPriority();

		public void consume(
				Plot plot, 
				String cellValue,
				PlotValueConsumer plotValueConsumer)
				throws IllegalArgumentException,
					IOException, 
					CreateItemException,
					ImportDataException;

		public boolean isPlotField();
	}

	static class PlotFieldConsumer implements CellValueConsumer {
		public final Field field;

		public PlotFieldConsumer(Field field) {
			this.field = field;
		}

		@Override
		public boolean isPlotField() {
			return true;
		}

		@Override
		public int getSortPriority() {
			return 1;
		}

		@Override
		public String toString() {
			return "PlotField[" + field.getName() + "]";  //$NON-NLS-1$//$NON-NLS-2$
		}

		@Override
		public void consume(Plot plot, String cellValue,
				PlotValueConsumer plotValueConsumer)
				throws IllegalArgumentException, // InvalidValueException,
				IOException, CreateItemException {
			storeValueInEntity(Plot.class, plot, field, cellValue);
		}

	}

	static class PlotAttributeConsumer implements CellValueConsumer {
		public final String attributeName;

		public PlotAttributeConsumer(String attributeName) {
			this.attributeName = attributeName;
		}

		@Override
		public boolean isPlotField() {
			return false;
		}

		@Override
		public int getSortPriority() {
			return 2;
		}

		@Override
		public String toString() {
			return "PlotAttribute[" + attributeName + "]";  //$NON-NLS-1$//$NON-NLS-2$
		}

		@Override
		public void consume(Plot plot, String cellValue,
				PlotValueConsumer plotValueConsumer)
				throws IllegalArgumentException, // InvalidValueException,
				CreateItemException 
		{
			plotValueConsumer.storePlotAttributeValue(plot, attributeName,
					cellValue);
		}
	}

	static class SectionData {

		final SheetData.Section section;
		
		private int valueCellnum;
		
		private Factory<SectionRowData> dataFactory;

		public SectionData(SheetData.Section section) {
			this.section = section;
		}

		public int getValueCellnum() {
			return valueCellnum;
		}

		public void init(Row row, int cellCount) throws IOException {
			if (valueCellnum > 0) {
				throw new IllegalStateException(
						"Already got a value for valueCellnum: " + valueCellnum);
			}
			
			switch (section.excelSection) {
			case PLOT_ATTRIBUTES:
				break;
			case TRAITS:
				break;
			case TRIAL_ATTRIBUTES:
				break;
			default:
				break;
			}
			
			Cell c0 = row.getCell(0);
			if (c0 == null) {
				throw new IOException("Missing cell in R" + (row.getRowNum()+1) + "C1");
			}
			String rnc1_value = ExcelUtil.getCellStringValue(c0, ""); //$NON-NLS-1$
			
			if (BmsConstant.SECTION_NAME_CONDITION.equals(rnc1_value)) {
				dataFactory = new Factory<SectionRowData>() {
					@Override
					public SectionRowData create() {
						return new SectionRowData(BmsConstant.LABEL_HEADING);
					}
				};
			}
			else if (BmsConstant.SECTION_NAME_FACTOR.equals(rnc1_value)) {
				dataFactory = new Factory<SectionRowData>() {
					@Override
					public SectionRowData create() {
						return new SectionRowData(BmsConstant.LABEL_HEADING);
					}
				};
			}
			else if (BmsConstant.SECTION_NAME_CONSTANT.equals(rnc1_value)) {
				dataFactory = new Factory<SectionRowData>() {
					@Override
					public SectionRowData create() {
						return new SectionRowData(BmsConstant.SAMPLE_LEVEL_HEADING);
					}
				};
			}
			else if (BmsConstant.SECTION_NAME_VARIATE.equals(rnc1_value)) {
				dataFactory = new Factory<SectionRowData>() {
					@Override
					public SectionRowData create() {
						return new SectionRowData(BmsConstant.SAMPLE_LEVEL_HEADING);
					}
				};
			}
			
			// VALUE must ALWAYS be present
			for (int cellnum = 1; cellnum < cellCount; ++cellnum) {
				Cell cell = row.getCell(cellnum);
				String hdg = cell == null ? "" : ExcelUtil.getCellStringValue(cell, ""); //$NON-NLS-2$
				
				if (BmsConstant.VALUE_HEADING.equals(hdg)) {
					valueCellnum = cellnum;
					break;
				}
			}

			if (valueCellnum <= 0) {
				throw new IOException("No cell with '" + BmsConstant.VALUE_HEADING + "' in row"
						+ (row.getRowNum() + 1));
			}
		}

		public void processRow(Row row, int cellCount, 
				String rnc1_value,
				BmsXlsTrialImportResult scanResult) 
		throws IOException 
		{
			// OK. we're in "mySection"
			SectionRowData data = null;
			if (dataFactory != null) {
				data = dataFactory.create();
				data.collectFrom(row);
				scanResult.addDescriptionSectionAttribute(section.excelSection,
						rnc1_value, data);
			}
			
			Cell cell = row.getCell(valueCellnum);
			String cell_value = cell == null ? null : 
				ExcelUtil.getCellStringValue(cell, null);
			
			Field field = null;
			switch (section.excelSection) {
			case TRIAL_ATTRIBUTES: // aka CONDITION
				field = TRIAL_FIELD_BY_CONDITION_NAME.get(rnc1_value);
				if (field != null) {
					storeValueInEntity(Trial.class,
							scanResult.trial, field, cell_value);
				}
				
				if (data == null) {
					scanResult.trialAttributesByName.put(rnc1_value, cell_value);
				}
				else {
					String valueThenRest = data.getVALUEthenRest();
					scanResult.trialAttributesByName.put(rnc1_value, valueThenRest);
				}
				break;

			case PLOT_ATTRIBUTES: // aka FACTOR
				// some of these we know of as plot member variable
				// 
				// and all of the others are PlotAttributes
				field = PLOT_FIELD_BY_FACTOR_NAME.get(rnc1_value);
				if (field != null) {
					scanResult.addPlotField(rnc1_value, field);
				}
				break;

			case TRAITS: // aka VARIATES
				// Nothing to do here
				break;

			default:
				break;
			}
		}

	}
	
	static class TraitConsumer implements CellValueConsumer {
		public final String traitName;

		public TraitConsumer(String traitName) {
			this.traitName = traitName;
		}

		@Override
		public boolean isPlotField() {
			return false;
		}

		@Override
		public int getSortPriority() {
			return 3;
		}

		@Override
		public String toString() {
			return "Trait[" + traitName + "]";  //$NON-NLS-1$//$NON-NLS-2$
		}

		@Override
		public void consume(Plot plot, String cellValue,
				PlotValueConsumer plotValueConsumer)
				throws IllegalArgumentException, ImportDataException,
				IOException, CreateItemException {
			plotValueConsumer.storeTraitValue(plot, traitName, cellValue);
		}
	}

	public void parsePlotsAndSamples(BmsXlsTrialImportResult trialImportResult,
			PlotValueConsumer plotValueConsumer) throws IOException,
			IllegalArgumentException, ImportDataException,
			CreateItemException {

		int rowCount = ExcelUtil.getRowCount(observation);
		int expectedCellCount = 0;

//		final Date trialPlantingDate = trialImportResult.trial.getTrialPlantingDate();
//      do NOT do this check NOW as there may not actually be any data for these Traits
//      If there is then we will throw an InvalidValueException in the PlotValueConsumer.
//		if (trialPlantingDate == null) {
//			List<String> traitNames = new ArrayList<>();
//			for (Trait trait : trialImportResult.trialTraits.values()) {
//				if (TraitDataType.ELAPSED_DAYS == trait.getTraitDataType()) {
//					traitNames.add(trait.getTraitName());
//				}
//			}
//			if (! traitNames.isEmpty()) {
//				throw new IOException(StringUtil.join("Planting Date required:", ",", traitNames));
//			}
//		}

		Map<Integer, String> headingByCellNumber = new TreeMap<>();
		List<ConsumerAndCellnum> consumerAndCellNum = null; // Gets set when rownum==0

		for (int rownum = 0; rownum < rowCount; ++rownum) {
			Row row = observation.getRow(rownum);
			if (row == null)
				continue;

			int cellCount = ExcelUtil.getCellCount(row);
			if (rownum == 0) {
				expectedCellCount = cellCount;
				consumerAndCellNum = buildConsumerByCellnum(trialImportResult,
						row, cellCount, headingByCellNumber);

				for (ConsumerAndCellnum cc : consumerAndCellNum) {
					if (cc.cellValueConsumer instanceof TraitConsumer) {
						TraitConsumer tc = (TraitConsumer) cc.cellValueConsumer;						
						plotValueConsumer.createTraitInstance(tc.traitName);
					}
					System.out.println(cc.cellnum + "\t"
							+ cc.cellValueConsumer.toString());
				}
			} else if (cellCount > 0) {

				if (cellCount != expectedCellCount) {
					String msg = "expected " + expectedCellCount
							+ " columns but got " + cellCount;
					plotValueConsumer.warn(rownum + 1, msg);
				}

				Map<Integer, String> cellValueByCellnum = new HashMap<>();

				for (int cellnum = 0; cellnum < cellCount; ++cellnum) {
					Cell cell = row.getCell(cellnum);
					String cellValue = ""; //$NON-NLS-1$
					if (cell != null) {
						cellValue = ExcelUtil.getCellStringValue(cell, "");
					}
					cellValueByCellnum.put(cellnum, cellValue);
				}

				Plot plot = new Plot();
				plot.setTrialId(trialImportResult.trial.getTrialId());

				// New collection because we are going to modify
				// cellValueByCellnum
//				List<Integer> cellNumbers = new ArrayList<>(
//						cellValueByCellnum.keySet());
//				Collections.sort(cellNumbers);

				boolean completed = false;
				for (ConsumerAndCellnum cnc : consumerAndCellNum) {
					int cellnum = cnc.cellnum;
					CellValueConsumer consumer = cnc.cellValueConsumer;
					if (consumer != null) {
						if (!completed && !consumer.isPlotField()) {
							plotValueConsumer.plotComplete(rownum + 1, plot);
							completed = true;
						}
						String cellValue = cellValueByCellnum.remove(cellnum);
						if (cellValue == null) {
							plotValueConsumer.warn(rownum + 1, "Null value for cell#" + cellnum);
						} else if (cellValue.isEmpty()) {
							cellValue = null;
						}
						consumer.consume(plot, cellValue, plotValueConsumer);
					} else {
						System.err
								.println("% no CellValueConsumer found for cellnum="
										+ cellnum
										+ ", value="
										+ cellValueByCellnum.get(cellnum));
						;
					}
				}
				if (!completed) {
					plotValueConsumer.plotComplete(rownum + 1, plot);
				}

				if (!cellValueByCellnum.isEmpty()) {
					List<String> unused = new ArrayList<>();
					for (Integer cellnum : cellValueByCellnum.keySet()) {
						unused.add(headingByCellNumber.get(cellnum) + " (#" //$NON-NLS-1$
								+ cellnum + ")"); //$NON-NLS-1$
					}
					String msg = StringUtil.join("Unused Data: ", ",", unused); //$NON-NLS-2$
					plotValueConsumer.warn(rownum + 1, msg);
				}
			}
		}
	}

	class ConsumerAndCellnum implements Comparable<ConsumerAndCellnum> {
		final CellValueConsumer cellValueConsumer;
		final int cellnum;

		ConsumerAndCellnum(int cnum, CellValueConsumer cvc) {
			cellValueConsumer = cvc;
			cellnum = cnum;
		}

		@Override
		public int compareTo(ConsumerAndCellnum o) {
			return Integer.compare(this.cellValueConsumer.getSortPriority(),
					o.cellValueConsumer.getSortPriority());
		}
	}

	private List<ConsumerAndCellnum> buildConsumerByCellnum(
			BmsXlsTrialImportResult descriptionScanResult, Row row,
			int cellCount, 
			Map<Integer, String> headingByCellNumber) 
	{
		List<ConsumerAndCellnum> result = new ArrayList<>();

		Map<Integer, String> headingByCellnum = new HashMap<>();
		Map<String, Integer> cellnumByHeading = new HashMap<>();

		for (int cellnum = 0; cellnum < cellCount; ++cellnum) {
			Cell cell = row.getCell(cellnum);
			if (cell == null)
				continue;
			String heading = ExcelUtil.getCellStringValue(cell, ""); //$NON-NLS-1$
			headingByCellnum.put(cellnum, heading);
			cellnumByHeading.put(heading, cellnum);
		}

		headingByCellNumber.putAll(headingByCellnum);

		for (Pair<String, Field> pair : descriptionScanResult.plotFactorFields) {
			Integer cellnum = cellnumByHeading.get(pair.first);
			if (cellnum != null) {
				result.add(new ConsumerAndCellnum(cellnum,
						new PlotFieldConsumer(pair.second)));
				cellnumByHeading.remove(pair.first);
				headingByCellnum.remove(cellnum);
			}
		}

		for (BmsExcelSection excelSection : BmsExcelSection.values()) {
			
			Map<String, SectionRowData> map = 
					descriptionScanResult.attributesByDescriptionSection.get(excelSection);
			
			if (map != null) {
				for (String attributeName : map.keySet()) {
					Integer cellnum = cellnumByHeading.get(attributeName);
					if (cellnum != null) {
						cellnumByHeading.remove(attributeName);
						headingByCellnum.remove(cellnum);
						if (!excelSection.isValueFromDescriptionWorksheet()) {
							result.add(new ConsumerAndCellnum(
									cellnum,
									excelSection
											.createCellValueConsumer(attributeName)));
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * The Trial related stuff is all in the DESCRIPTION sheet.
	 * 
	 * @param trialNameInstanceSeparator
	 * @param lowcaseTrialNames
	 * @return
	 * @throws IOException
	 */
	public BmsXlsTrialImportResult parseTrialData(
			String trialNameInstanceSeparator, Set<String> lowcaseTrialNames)
	throws IOException 
	{
		List<SectionData> sectionDataList = new ArrayList<>();
		for (SheetData.Section section : DESCRIPTION.sections) {
			sectionDataList.add(new SectionData(section));
		}
		return DESCRIPTION.scan(description, trialNameInstanceSeparator,
				lowcaseTrialNames, sectionDataList);
	}
}
