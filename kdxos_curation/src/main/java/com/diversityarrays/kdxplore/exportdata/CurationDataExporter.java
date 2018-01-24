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

import java.awt.Component;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import javax.swing.JOptionPane;

import org.apache.commons.collections15.Transformer;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.csvio.CsvColumn;
import com.diversityarrays.kdsmart.db.csvio.CsvHeadings;
import com.diversityarrays.kdsmart.db.csvio.CsvImportDefinition;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdsmart.db.util.CsvWriter;
import com.diversityarrays.kdsmart.db.util.CsvWriterImpl;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.importdata.bms.BmsConstant.BmsCellColor;
import com.diversityarrays.kdxplore.importdata.bms.BmsExcelExportHelper;
import com.diversityarrays.kdxplore.importdata.bms.BmsExcelImportHelper;
import com.diversityarrays.kdxplore.importdata.bms.SectionRowData;
import com.diversityarrays.kdxplore.trialmgr.TrialManagerPreferences;

import net.pearcan.excel.ExcelUtil;
import net.pearcan.excel.WorkbookOutput;
import net.pearcan.excel.WorksheetOutput;
import net.pearcan.util.MessagePrinter;
import net.pearcan.util.Util;

public class CurationDataExporter {

	private static final String TEMPDIR_KDX = "kdx"; //$NON-NLS-1$
    private static final String SUFFIX_CSV = ".csv"; //$NON-NLS-1$
    private static final String SUFFIX_XLSX = ".xlsx"; //$NON-NLS-1$
    private static final String SUFFIX_XLS = ".xls"; //$NON-NLS-1$
    private static final String SUFFIX_TMP = ".tmp"; //$NON-NLS-1$

    static private final Transformer<String, String> INVALID_DATE_TRANSFORMER = new Transformer<String, String>() {
		@Override
		public String transform(String notValidDate) {
			return "?" + notValidDate; //$NON-NLS-1$
		}
	};

	static abstract class Accessor<T> {
		
		public final Class<T> sourceClass;
		public final String title;
		
		public Accessor(Class<T> tclass, String title) {
			this(tclass, null, title);
		}
		
		public Accessor(Class<T> tclass, List<Accessor<T>> list, String title) {
			sourceClass = tclass;
			this.title = title;
			if (list != null) {
				list.add(this);
			}
		}
		abstract Object fieldValue(T t);
	}
	
	private final Trial trial;
	private final CurationExportHelper helper;
	private final Component messageComponent;
	private final MessagePrinter messagePrinter;

	private final List<TrialAttribute> trialAttributes;

	public CurationDataExporter(Trial trial,
			List<TrialAttribute> trialAttributes, 
			CurationExportHelper helper,
			Component mc, MessagePrinter mp) {
		this.trial = trial;
		this.trialAttributes = trialAttributes;
		this.helper = helper;
		this.messageComponent = mc;
		this.messagePrinter = mp;
	}
	
	static abstract class TraitAccessor extends Accessor<Trait> {
		public TraitAccessor(String title) {
			super(Trait.class, title);
		}
	}

	static abstract class TrialAccessor extends Accessor<Trial> {
		public TrialAccessor(String title) {
			super(Trial.class, title);
		}
	}

	static abstract class PlotAccessor extends Accessor<Plot> {
		public PlotAccessor(String title) {
			super(Plot.class, title);
		}
	}

	static class PlotAttributeValueAccessor extends
			Accessor<PlotAttributeValue> {

		public final Integer plotAttributeId;

		public PlotAttributeValueAccessor(PlotAttribute pa) {
			super(PlotAttributeValue.class, pa.getPlotAttributeName());
			plotAttributeId = pa.getPlotAttributeId();
		}

		@Override
		Object fieldValue(PlotAttributeValue t) {
			return t.getAttributeValue();
		}
	}

	static class SampleAccessor extends Accessor<Sample> {

		private final DateFormat dateFormat = TraitValue
				.getTraitValueDateFormat();

		public final Date trialPlantingDate;
		public final TraitInstance traitInstance;

		public final ExportOptions options;

		public SampleAccessor(String tiName, Date trialPlantingDate,
				TraitInstance ti, ExportOptions eo) {
			super(Sample.class, tiName);
			this.trialPlantingDate = trialPlantingDate;
			this.traitInstance = ti;
			this.options = eo;
		}

		@Override
		Object fieldValue(Sample t) {
			String sv = TraitValue.transformSampleToDisplayValue(null,
					traitInstance.trait, t, trialPlantingDate, dateFormat,
					options.unscoredValueString, options.naValueString,
					options.missingValueString, INVALID_DATE_TRANSFORMER);
			return sv;
		}
	}

	public void exportCurationData(ExportOptions options) {
		RowDataEmitter rowDataEmitter = null;

		boolean success = false;
		File tmpfile = null;

		File outputFile = options.file;

		List<PlotAttribute> plotAttributes = helper
				.getPlotAttributes(options.allPlotAttributes);
		List<TraitInstance> traitInstances = helper
				.getTraitInstances(options.whichTraitInstances);

		try {
			String loname = outputFile.getName().toLowerCase();

			tmpfile = File.createTempFile(TEMPDIR_KDX, SUFFIX_TMP,
					outputFile.getParentFile());

			if (loname.endsWith(SUFFIX_XLS)) {
				rowDataEmitter = new OldExcelRowDataEmitter(options, tmpfile, trial);
			} else if (loname.endsWith(SUFFIX_XLSX)) {
				// NOTE: BMS not supported for XLSX
				rowDataEmitter = new NewExcelRowDataEmitter(options, tmpfile, trial);
			} else {
				rowDataEmitter = new CsvRowDataEmitter(options, tmpfile);
				if (!loname.endsWith(SUFFIX_CSV)) {
					outputFile = new File(outputFile.getParentFile(),
							outputFile.getName() + SUFFIX_CSV);
				}
			}
			
			rowDataEmitter.emitTrialDetails(
					trialAttributes, plotAttributes, traitInstances);

			rowDataEmitter.emitSampleData(plotAttributes, traitInstances);

			success = true;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(messageComponent, e.getMessage(),
					"I/O Error", JOptionPane.ERROR_MESSAGE);
		} finally {
			if (rowDataEmitter != null) {
				try {
					rowDataEmitter.close();
				} catch (IOException ignore) {
				}
			}
			if (success) {
				if (outputFile.exists()) {
					outputFile.delete();
				}
				if (tmpfile.renameTo(outputFile)) {
					messagePrinter.println("Exported data to " + outputFile.getPath());
					if (JOptionPane.YES_OPTION == JOptionPane
							.showConfirmDialog(messageComponent, "Saved "
									+ outputFile.getName(),
									"Open the saved file?",
									JOptionPane.YES_NO_OPTION)) {
						try {
							Util.openFile(outputFile);
						} catch (IOException e) {
							messagePrinter.println("Failed to open "
									+ outputFile.getPath());
							messagePrinter.println(e.getMessage());
						}
					}
				} else {

				}
			} else {
				if (tmpfile != null) {
					tmpfile.delete();
				}
			}
		}
	}

	interface SampleEmitter {
		void emitSamples(Plot plot, List<Object> rowData);

//		static public boolean shouldExportSample(Sample sample) {
//			if (sample instanceof KdxSample) {
//				if (((KdxSample) sample).isSuppressed()) {
//					return false;
//				}
//			}
//			return true;
//		}

        static public boolean isSuppressedSample(Sample sample) {
            if (sample instanceof KdxSample) {
                return ((KdxSample) sample).isSuppressed();
            }
            return false;
        }
	}

	class SampleAccessorEmitter implements SampleEmitter {

		private static final String SAMPLE_VALUE_NULL = "??"; //$NON-NLS-1$

        DateFormat plantingDateFormat = TraitValue.getPlantingDateFormat();

		private final Date trialPlantingDate = trial.getTrialPlantingDate();
		private final Map<String, SampleAccessor> sampleAccessorByInstanceId = new LinkedHashMap<>();
		private final Map<String, TraitInstance> traitInstanceByInstanceId = new LinkedHashMap<>();
		private final Map<Integer,Trait> traitByTraitId = new HashMap<>();

		private final ExportOptions options;
		
		SampleAccessorEmitter(ExportOptions options,
				List<TraitInstance> traitInstances)
		{
			this.options = options;
			
			TraitNameStyle tns = trial.getTraitNameStyle();
			for (TraitInstance ti : traitInstances) {
				traitByTraitId.put(ti.trait.getTraitId(), ti.trait);

				String tiname = tns.makeTraitInstanceName(ti);
				String id = InstanceIdentifierUtil.getInstanceIdentifier(ti);

				traitInstanceByInstanceId.put(id, ti);
				sampleAccessorByInstanceId.put(id, new SampleAccessor(tiname,
						trialPlantingDate, ti, options));
			}
		}

		@Override
		public void emitSamples(Plot plot, List<Object> rowData) {

			boolean plotIsActive = plot.isActivated();

			Map<String, Sample> sampleByInstanceId = new HashMap<>();

			for (Sample sample : plot) {
				if (PlotOrSpecimen.isSpecimenNumberForPlot(sample
						.getSpecimenNumber())) {
					String instanceId = InstanceIdentifierUtil
							.getInstanceIdentifier(sample);
					sampleByInstanceId.put(instanceId, sample);
				}
			}

			for (String iid : sampleAccessorByInstanceId.keySet()) {
				SampleAccessor a = sampleAccessorByInstanceId.get(iid);

				if (plotIsActive) {
					Sample sample = sampleByInstanceId.get(iid);
					
					Object sampleValue = null;
					Date when = null;
					if (sample == null) {
						sampleValue = options.unscoredValueString;
					}
					else if (SampleEmitter.isSuppressedSample(sample)) {
					    sampleValue = options.suppressedValueString;
					}
					else {
						if (sample.hasBeenScored()) {
							when = sample.getMeasureDateTime();
						}
						
						sampleValue = a.fieldValue(sample);
						Trait trait = traitByTraitId.get(sample.getTraitId());
						switch (trait.getTraitDataType()) {
						case CALC:
						case DECIMAL:
						case ELAPSED_DAYS:
						case INTEGER:
							try {
								if (sampleValue != null) {
									sampleValue = Double.parseDouble(sampleValue.toString());
								}
							}
							catch (NumberFormatException ignore) {
								// it may not be numeric (e.g. MISSING)
							}
							break;
							
						case CATEGORICAL:
						case DATE:
						case TEXT:
						default:
							break;
							
						}
					}
					
					addToRowData(rowData, sampleValue);				
					if (options.includeDateTimeMeasured) {
						addToRowData(rowData, when);
					}
				}
				else {
					// Plot is NOT ACTIVE
					if (options.exportInactiveTraitValue!=null) {
						addToRowData(rowData, options.exportInactiveTraitValue);
						if (options.includeDateTimeMeasured) {
							addToRowData(rowData, null);
						}
					}
				}
			}
		}
	}

	// This one only works for if the samples are in the CurationTableModel
	class CcvSampleEmitter implements SampleEmitter {

		private final ExportOptions options;
		private final List<TraitInstance> traitInstances;
		
		private final Set<TraitInstance> noTivr = new HashSet<>();
		private final Map<TraitInstance,TraitInstanceValueRetriever<?>> tivrCache = new HashMap<>();

		Date trialPlantingDate = trial.getTrialPlantingDate();
		DateFormat plantingDateFormat = TraitValue.getPlantingDateFormat();
        private Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
            @Override
            public List<KdxSample> apply(TraitInstance ti) {
                return helper.getSampleMeasurements(ti);
            }
        };

		CcvSampleEmitter(ExportOptions options,
				List<TraitInstance> traitInstances) {

			this.options = options;
			this.traitInstances = traitInstances;
		}
		
		private TraitInstanceValueRetriever<?> lookup(TraitInstance ti) {
			TraitInstanceValueRetriever<?> result = null;
			if (! noTivr.contains(ti)) {
				result = tivrCache.get(ti);
				if (result == null) {
					try {
						result = TraitInstanceValueRetriever.getValueRetriever(trial, ti, sampleProvider);
						tivrCache.put(ti, result);
					} catch (InvalidRuleException e) {
						noTivr.add(ti);
					}
				}
			}
			return result;
		}

		@Override
		public void emitSamples(Plot plot, List<Object> rowData) {
			
			boolean plotIsActive = plot.isActivated();
			
			for (TraitInstance ti : traitInstances) {

				if (plotIsActive) {
					Object value = null;
					Date when = null;

					Sample sample = helper.getSampleForPlotAndTraitInstance(plot, ti);
					if (sample == null) {
						value = options.unscoredValueString; // Redmine#1575
					}
					else if (SampleEmitter.isSuppressedSample(sample)) {
					    value = options.suppressedValueString;
					}
					else {
						if (sample.hasBeenScored()) {
							when = sample.getMeasureDateTime();
						}
						
						TraitInstanceValueRetriever<?> tivr = lookup(ti);
						// Redmine#1575 -- Start
						if (tivr==null) {
							Trait trait = helper.getTrait(sample.getTraitId());
							// NOTE: This is only when we do NOT have a TraitInstanceValueRetriever
							//       If we DO - then it is responsible for gettinq what we need.
							value = TraitValue.transformSampleToDisplayValue(null,
									trait, sample, trialPlantingDate,
									plantingDateFormat, options.unscoredValueString,
									options.naValueString, options.missingValueString,
									INVALID_DATE_TRANSFORMER);
						}
						else {
					        TraitValueType tvt = TraitValue.classify(sample.getTraitValue());
					        switch (tvt) {
                            case MISSING:
                                value = options.missingValueString;
                                break;
                            case NA:
                                value = options.naValueString;
                                break;
                            case SET:
                                TraitValue traitValue = tivr.createTraitValue(sample, null);
                                value = traitValue==null ? null : traitValue.comparable;
                                break;
                            case UNSET:
                                value = options.unscoredValueString;
                                break;
                            default:
                                throw new RuntimeException("Unexpected TraitValueType: " + tvt);
					        }
						}
						 // Redmine#1575 -- End
					}
					
					rowData.add(value);
					if (options.includeDateTimeMeasured) {
						rowData.add(when);
					}
				}
				else {
					// Plot is NOT ACTIVE
					if (options.exportInactiveTraitValue!=null) {
						rowData.add(options.exportInactiveTraitValue);
						if (options.includeDateTimeMeasured) {
							rowData.add(null);
						}
					}
				}
			}
		}
	}

	private void addToRowData(List<Object> rowData, Object value) {
		rowData.add(value);
	}

	interface RowDataEmitter extends Closeable {
		void emitTrialDetails(
				List<TrialAttribute> trialAttributes,
				List<PlotAttribute> plotAttributes,
				List<TraitInstance> traitInstances);
		
		/**
		 * @param indexOfFirstTraitHeading if non-null then doing headings
		 * @param rowData
		 */
		void writeSampleDataRow(boolean forHeadings, int indexOfFirstTraitHeading, List<Object> rowData);
		
		void emitSampleData(
				List<PlotAttribute> plotAttributes,
				List<TraitInstance> traitInstances);
	}
	
	abstract class AbstractRowDataEmitter implements RowDataEmitter {

		private static final boolean FOR_HEADING = true;
		private static final boolean FOR_DATA = false;
		
		protected final ExportOptions options;

		public AbstractRowDataEmitter(ExportOptions options) {
			this.options = options;
		}
		
		
		public void emitSampleData(List<PlotAttribute> plotAttributes,
				List<TraitInstance> traitInstances)
		{
			// Prepare the Accessors for Sample data output
			boolean hasPlotType = helper.hasPlotType(options.bmsFormat);

			List<TrialAccessor> trialAccessors = new ArrayList<>();
			if (! options.bmsFormat) {
				if (options.showTrialName) {
					String title = CsvImportDefinition.HEADING_TRIAL_NAME;
					trialAccessors.add(new TrialAccessor(title) {
						@Override
						Object fieldValue(Trial t) {
							return t.getTrialName();
						}
					});
				}
			}
			// - - - - - -
			List<Accessor<?>> accessors = new ArrayList<>();
			
			if (options.bmsFormat) {
				collectBMSaccessors(
						options, hasPlotType, plotAttributes, accessors);
				
				// - - - - -
			}
			else {
				collectNonBMSaccessors(
						options, hasPlotType, plotAttributes, accessors);
			}
			
			Set<Integer> plotAttributeIdsAsNumber = new HashSet<>();
			switch (options.plotAttributeAsNumber) {
			case ALL:
				for (PlotAttribute pa : plotAttributes) {
					plotAttributeIdsAsNumber.add(pa.getPlotAttributeId());
				}
				break;
			case ENDING_WITH_NO:
				for (PlotAttribute pa : plotAttributes) {
					if (pa.getPlotAttributeName().endsWith("_NO")) { //$NON-NLS-1$
						plotAttributeIdsAsNumber.add(pa.getPlotAttributeId());
					}
				}
				break;
			case NONE:
				break;
			default:
				break;			
			}

			// - - -
			// Do all of the headings

			List<Object> rowData = new ArrayList<>();

			for (TrialAccessor a : trialAccessors) {
				rowData.add(a.title);
			}

			for (Accessor<?> a : accessors) {
				rowData.add(a.title);
			}
			
			int indexOfFirstTrait = rowData.size();

			TraitNameStyle traitNameStyle = trial.getTraitNameStyle();
			for (TraitInstance ti : traitInstances) {
				String tiname = traitNameStyle.makeTraitInstanceName(ti);
				rowData.add(tiname);
				
				if (options.includeDateTimeMeasured) {
				    // Redmine#1575
				    rowData.add(CsvImportDefinition.TRAIT_NAME_DATE_PREFIX_COLON + tiname);
				}
			}

			if (options.colourOutputCells) {
				writeSampleDataRow(FOR_HEADING, indexOfFirstTrait, rowData);
			}
			else {
				writeSampleDataRow(FOR_HEADING, Integer.MAX_VALUE /* don't want styles */, rowData);
			}
			
			// = = = = = now for the sample rows

			SampleEmitter sampleEmitter;
			sampleEmitter = new CcvSampleEmitter(options, traitInstances);

			for (Integer modelRow : options.modelRows) {
				rowData.clear();

				Plot plot = helper.getPlotByRowIndex(modelRow);

				Map<Integer, PlotAttributeValue> pavByAttributeId = new HashMap<>();
				for (PlotAttributeValue pav : plot.plotAttributeValues) {
					pavByAttributeId.put(pav.getAttributeId(), pav);
				}

				// === Now do the rowdata ===

				for (TrialAccessor a : trialAccessors) {
					Object fv = a.fieldValue(trial);
					addToRowData(rowData, fv);
				}
				
				for (Accessor<?> a : accessors) {
					if (a instanceof PlotAccessor) {
						addToRowData(rowData, ((PlotAccessor) a).fieldValue(plot));
					}
					else if (a instanceof PlotAttributeValueAccessor) {
						PlotAttributeValueAccessor pava = (PlotAttributeValueAccessor) a;
						
						PlotAttributeValue pav = pavByAttributeId.get(pava.plotAttributeId);
						
						if (pav == null) {
							addToRowData(rowData, ""); //$NON-NLS-1$
						} else {
							Object outputValue = pava.fieldValue(pav);
							if (plotAttributeIdsAsNumber.contains(pav.getAttributeId())
								&& outputValue != null)
							{
								try {
									outputValue = Double.parseDouble(outputValue.toString());
								} catch (NumberFormatException ignore) {
									// If not numeric, just use what we had
								}
							}
							addToRowData(rowData, outputValue);
						}
					}
					else {
						// TODO perhaps throw an exception
					}
				}

				sampleEmitter.emitSamples(plot, rowData);

				writeSampleDataRow(FOR_DATA, indexOfFirstTrait, rowData);
			}
		}


		private void collectNonBMSaccessors(ExportOptions options,
				boolean hasPlotType, List<PlotAttribute> plotAttributes,
				List<Accessor<?>> accessors) {
			PlotIdentSummary plotIdentSummary = trial.getPlotIdentSummary();
			boolean hasUserPlotId = !plotIdentSummary.plotIdentRange.isEmpty();
			boolean hasPlotX = !plotIdentSummary.xColumnRange.isEmpty();
			boolean hasPlotY = !plotIdentSummary.yRowRange.isEmpty();

			if (hasUserPlotId) {
				String title = CsvImportDefinition.HEADING_PLOTID;
				accessors.add(new PlotAccessor(title) {
					@Override
					Object fieldValue(Plot t) {
						return t.getUserPlotId();
					}
				});
			}
			
			if (hasPlotX) {
				String title;
				if (options.nameForColumn.isEmpty()) {
					title = CsvImportDefinition.HEADING_COLUMNX;
				}
				else {
					title = options.nameForColumn;
				}
				accessors.add(new PlotAccessor(title) {
					@Override
					Object fieldValue(Plot t) {
						return t.getPlotColumn();
					}
				});
			}
			if (hasPlotY) {
				String title;
				if (options.nameForRow.isEmpty()) {
					title = CsvImportDefinition.HEADING_ROWY;
				}
				else {
					title = options.nameForRow;
				}
				accessors.add(new PlotAccessor(title) {
					@Override
					Object fieldValue(Plot t) {
						return t.getPlotRow();
					}
				});
			}
			if (hasPlotType) {
				String title = CsvImportDefinition.HEADING_PLOTTYPE;
				accessors.add(new PlotAccessor(title) {
							@Override
							Object fieldValue(Plot t) {
								return t.getPlotType();
							}
						});
			}

			for (PlotAttribute pa : plotAttributes) {
				PlotAttributeValueAccessor a = new PlotAttributeValueAccessor(pa) {
					@Override
					Object fieldValue(PlotAttributeValue t) {
						return t.getAttributeValue();
					}
				};
				accessors.add(a);
			}
		}


		private void collectBMSaccessors(ExportOptions options,
				boolean hasPlotType, List<PlotAttribute> plotAttributes,
				List<Accessor<?>> accessors) {
			List<String> paNames = CurationExportHelper.collectPaNamesForBMS(
					trial, 
					options,
					hasPlotType, 
					plotAttributes);
			
			Map<String,PlotAttribute> paByName = new HashMap<>();
			for (PlotAttribute pa : plotAttributes) {
				paByName.put(pa.getPlotAttributeName(), pa);
			}
			
			Map<String,Field> fieldByFactorName = BmsExcelImportHelper.getFieldByFactorName(options);
			
			for (String paName : paNames) {
				PlotAttribute pa = paByName.get(paName);
				if (pa != null) {
					PlotAttributeValueAccessor a = new PlotAttributeValueAccessor(pa) {
						@Override
						Object fieldValue(PlotAttributeValue t) {
							return t.getAttributeValue();
						}
					};
					accessors.add(a);
				}
				else {
					Field field = fieldByFactorName.get(paName);
					if (field != null) {
						String fieldName = field.getName();
						if (Plot.FIELDNAME_PLOT_TYPE.equals(fieldName)) {
							accessors.add(new PlotAccessor(paName) {
								@Override
								Object fieldValue(Plot t) {
									return t.getPlotType();
								}
							});
						}
						else if (Plot.FIELDNAME_USER_PLOT_ID.equals(fieldName)) {
							accessors.add(new PlotAccessor(paName) {
								@Override
								Object fieldValue(Plot t) {
									return t.getUserPlotId();
								}
							});
						}
						else if (Plot.FIELDNAME_PLOT_COLUMN.equals(fieldName)) {
							accessors.add(new PlotAccessor(paName) {
								@Override
								Object fieldValue(Plot t) {
									return t.getPlotColumn();
								}
							});
						}
						else if (Plot.FIELDNAME_PLOT_ROW.equals(fieldName)) {
							accessors.add(new PlotAccessor(paName) {
								@Override
								Object fieldValue(Plot t) {
									return t.getPlotRow();
								}
							});
						}
						else {
							// TODO 
						}
					}
				}
			}
		}
	}

	class CsvRowDataEmitter extends AbstractRowDataEmitter {
		final CsvWriter csvWriter;

		public CsvRowDataEmitter(ExportOptions options, File file) throws IOException {
			super(options);
			csvWriter = new CsvWriterImpl(new FileWriter(file));
		}
		
		@Override
		public void emitTrialDetails(
				List<TrialAttribute> trialAttributes,
				List<PlotAttribute> plotAttributes,
				List<TraitInstance> traitInstances)
		{
			// NOTHING to do for CSVs
		}

		@Override
		public void close() throws IOException {
			csvWriter.close();
		}

		@Override
		public void writeSampleDataRow(boolean forHeadings, int indexOfFirstTraitHeading, List<Object> rowData) {
			String[] line = new String[rowData.size()];
			for (int index = rowData.size(); --index >= 0; ) {
				Object v = rowData.get(index);
				if (v == null) {
					line[index] = ""; //$NON-NLS-1$
				}
				else {
					line[index] = v.toString();
				}
			}
			csvWriter.writeNext(line);
		}
	}

	abstract class ExcelRowDataEmitter extends AbstractRowDataEmitter {
		private File file;
		private Workbook workbook;
		private WorkbookOutput workbookOutput;

		protected final Trial trial;
		
		protected final CellStyleProvider cellStyleProvider;

		private WorksheetOutput wso;

		public ExcelRowDataEmitter(ExportOptions options, 
				File file, Trial trial, 
				ExcelUtil.WorkbookType type) throws IOException 
		{
			super(options);
			this.file = file;
			this.trial = trial;

			workbook = ExcelUtil.createWorkbook(type);
			workbookOutput = new WorkbookOutput(workbook);
			
			cellStyleProvider = new CellStyleProvider(workbook);
		}

		@Override
		public void emitTrialDetails(
				List<TrialAttribute> trialAttributes,
				List<PlotAttribute> plotAttributes,
				List<TraitInstance> traitInstances) 
		{
			if (options.bmsFormat) {
				// similar to below but use BMS names etc.
				// e.g. if trialName ends with trial attribute "Instance" remove the instance number
				BmsExcelExportHelper beeh = new BmsExcelExportHelper(
						workbookOutput, trial, cellStyleProvider, trialAttributes, traitInstances);
				beeh.emitDescriptionSheet(
						options, helper.hasPlotType(true), plotAttributes);
			} else {
				KdxploreExcelExportHelper keeh = new KdxploreExcelExportHelper(
						workbookOutput, trial, cellStyleProvider);
				keeh.emitTrialSheet();
				keeh.emitTrialAttributesSheet(trialAttributes);
				keeh.emitPlotAttributesSheet(plotAttributes);
				keeh.emitTraitsSheet(traitInstances);
			}
			
			prepareForSamples();
		}

		private void prepareForSamples() {
		    String sn = TrialManagerPreferences.getInstance().getSampleMeasurementsSheetName();
			String dataSheetName = options.bmsFormat 
			        ? BmsExcelImportHelper.SHEET_NAME_OBSERVATION
					: "Sample Measurements";
			Sheet sheet = workbookOutput.getWorkbook().createSheet(
					dataSheetName);
			wso = new WorksheetOutput(workbookOutput, sheet);
		}

		@Override
		public void close() throws IOException {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(file);
				workbookOutput.getWorkbook().write(fos);
			} finally {
				try {
					fos.close();
				} catch (IOException ignore) {
				}
			}
		}

		@Override
		public void writeSampleDataRow(boolean headings, int indexOfFirstTraitHeading, List<Object> rowData) {
			
			CellStyle traitStyle = null;
			CellStyle nonTraitStyle = null;
			CellStyle boldHeading = null;
			CellStyle dateFormat = null;
			
			if (headings) {
				boldHeading = cellStyleProvider.getBoldHeading();
				traitStyle = cellStyleProvider.getCellStyle(BmsCellColor.CONSTANT_OR_VARIATE_PURPLE);
				nonTraitStyle = cellStyleProvider.getCellStyle(BmsCellColor.CONDITION_OR_FACTOR_GREEN);
			}
			else if (indexOfFirstTraitHeading < Integer.MAX_VALUE) {
				dateFormat = cellStyleProvider.getDateFormat();
			}
			
			wso.startNewRow();

			int index = -1;
			for (Object s : rowData) {
				++index;
				CellStyle cs = null;
				if (headings) {
					// Doing the headings...
					cs = nonTraitStyle;
					if (index >= indexOfFirstTraitHeading) {
						cs = traitStyle;
						if (options.includeDateTimeMeasured) {
							int offset = indexOfFirstTraitHeading - index;
							if (0 != (offset & 1)) {
								cs = boldHeading;
							}
						}
					}
				}
				else {
					if (options.includeDateTimeMeasured && index >= indexOfFirstTraitHeading) {
						int offset = indexOfFirstTraitHeading - index;
						if (0 != (offset & 1)) {
							cs = dateFormat;
						}
					}
				}
				// Redmine#1575 -- Start
				if (s instanceof String && (s.toString().length() <= 15)) {
				    try {
				        Double d = new Double(s.toString());
	                    wso.addCell(cs, d);
				    }
				    catch (NumberFormatException e) {
	                    wso.addCell(cs, s);
				    }
				}
				else {
	                wso.addCell(cs, s);
				}
				// Redmine#1575 -- End
			}

		}
	}

	class NewExcelRowDataEmitter extends ExcelRowDataEmitter {
		public NewExcelRowDataEmitter(ExportOptions options, 
				File file, Trial trial)
				throws IOException {
			super(options, file, trial, ExcelUtil.WorkbookType.XLS_X_STREAM);
		}
	}

	class OldExcelRowDataEmitter extends ExcelRowDataEmitter {
		public OldExcelRowDataEmitter(ExportOptions options,
				File file, Trial trial)
				throws IOException {
			super(options, file, trial,  ExcelUtil.WorkbookType.XLS);
		}
	}

	static class KdxploreExcelExportHelper {

		private static final String TRAIT_NAME = "Name";
		static public final String SHEET_NAME_TRIAL = "Trial";
		static public final String SHEET_NAME_TRAITS = "Traits";
		private static final String SHEET_NAME_TRIAL_ATTRIBUTES = "Trial Attributes";
		private static final String SHEET_NAME_PLOT_ATTRIBUTES = "Plot Attributes";

		private final WorkbookOutput workbookOutput;
		private final Trial trial;
		private final CellStyleProvider cellStyleProvider;

		KdxploreExcelExportHelper(WorkbookOutput wbo, Trial trial, CellStyleProvider cellStyleProvider) {
			this.workbookOutput = wbo;
			this.trial = trial;
			this.cellStyleProvider = cellStyleProvider;
		}

		public void emitTraitsSheet(List<TraitInstance> traitInstances) {
			Sheet traitsSheet = workbookOutput.getWorkbook().createSheet(SHEET_NAME_TRAITS);
			WorksheetOutput wsoTrait = new WorksheetOutput(workbookOutput, traitsSheet);
			
			CellStyle boldHeading = cellStyleProvider.getBoldHeading();
			CellStyle purple = cellStyleProvider.getCellStyle(BmsCellColor.CONSTANT_OR_VARIATE_PURPLE);

			boolean anyHasIdDownloaded = false;
			boolean anyHasBarcode = false;
			boolean anyHasOrigTraitValRule = false;
			
			Set<Trait> traits = new TreeSet<>();
			for (TraitInstance ti : traitInstances) {
				Trait trait = ti.trait;
				if (trait != null) {
					if (traits.add(trait)) {
						if (trait.getIdDownloaded() != null) {
							anyHasIdDownloaded = true;
						}
						String tmp = trait.getBarcode();
						if (tmp != null && ! tmp.isEmpty()) {
							anyHasBarcode = true;
						}
						tmp = trait.getOrigTraitValRule();
						if (tmp != null && ! tmp.isEmpty()) {
							anyHasOrigTraitValRule = true;
						}
					}
				}
			}
			
			List<TraitAccessor> traitAccessors = collectTraitAccessorsRequired(
					anyHasIdDownloaded, anyHasBarcode, anyHasOrigTraitValRule);

			int nameIndex = -1;
			wsoTrait.startNewRow();
			int index = -1;
			for (TraitAccessor ta : traitAccessors) {
				++index;
				wsoTrait.addCell(boldHeading, ta.title);
				if (TRAIT_NAME.equals(ta.title)) {
					nameIndex = index;
				}
			}
			
			for (Trait trait : traits) {
				wsoTrait.startNewRow();
				index = -1;
				for (TraitAccessor ta : traitAccessors) {
					++index;
					CellStyle cellStyle = null;
					if (index == nameIndex) {
						cellStyle = purple;
					}
					wsoTrait.addCell(cellStyle, ta.fieldValue(trait));
				}
			}
		}

		public List<TraitAccessor> collectTraitAccessorsRequired(
				boolean anyHasIdDownloaded, boolean anyHasBarcode,
				boolean anyHasOrigTraitValRule) {
			List<TraitAccessor> traitAccessors = new ArrayList<>();
			
			traitAccessors.add(new TraitAccessor(TRAIT_NAME) {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitName();
				}				
			});
			traitAccessors.add(new TraitAccessor("Description") {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitDescription();
				}				
			});
			traitAccessors.add(new TraitAccessor("Alias") {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitAlias();
				}				
			});
			traitAccessors.add(new TraitAccessor("Data Type") {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitDataType();
				}				
			});
			traitAccessors.add(new TraitAccessor("Unit") {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitUnit();
				}				
			});
			traitAccessors.add(new TraitAccessor("Validation Rule") {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitValRule();
				}				
			});
			if (anyHasBarcode) {
				traitAccessors.add(new TraitAccessor("Barcode") {
					@Override
					Object fieldValue(Trait t) {
						return t.getBarcode();
					}				
				});
			}

			if (anyHasOrigTraitValRule) {
				traitAccessors.add(new TraitAccessor("Original Validation Rule") {
					@Override
					Object fieldValue(Trait t) {
						return t.getOrigTraitValRule();
					}				
				});
			}
			if (anyHasIdDownloaded) {
				traitAccessors.add(new TraitAccessor("Database Id") {
					@Override
					Object fieldValue(Trait t) {
						return t.getIdDownloaded();
					}				
				});
			}
			traitAccessors.add(new TraitAccessor("Link:TraitId") {
				@Override
				Object fieldValue(Trait t) {
					return t.getTraitId();
				}				
			});
			return traitAccessors;
		}

		public void emitTrialSheet() 
		{
			Sheet trialSheet = workbookOutput.getWorkbook().createSheet(SHEET_NAME_TRIAL);

			WorksheetOutput wsoTrial = new WorksheetOutput(workbookOutput,
					trialSheet);

			CsvHeadings<Trial> csvHeadings = new CsvHeadings<Trial>(Trial.class);

			// TODO use colours
//			wsoTrial.startNewRow();
//			for (CsvColumn cc : csvHeadings) {
//				wsoTrial.addCell(cc.exportAs());
//			}
//			wsoTrial.startNewRow();
//			for (CsvColumn cc : csvHeadings) {
//				String value = csvHeadings.getValueFromItem(cc, trial);
//				wsoTrial.addCell(value);
//			}

			CellStyle brown = cellStyleProvider.getCellStyle(BmsCellColor.DARK_RED);
//			CellStyle brown = cellStyleProvider.getCellStyle(BmsCellColor.TRIAL_NAME_ETC_BROWN);
			
			for (CsvColumn cc : csvHeadings) {
				wsoTrial.startNewRow();
				wsoTrial.addCell(brown, cc.exportAs());
				String value = csvHeadings.getValueFromItem(cc, trial);
				wsoTrial.addCell(value);
			}
		}
		
		public void emitTrialAttributesSheet(List<TrialAttribute> trialAttributes) {
			
			Sheet sheet = workbookOutput.getWorkbook().createSheet(SHEET_NAME_TRIAL_ATTRIBUTES);

			WorksheetOutput wsoTrial = new WorksheetOutput(workbookOutput, sheet);

			CellStyle boldHeading = cellStyleProvider.getBoldHeading();
			CellStyle green = cellStyleProvider.getCellStyle(BmsCellColor.CONDITION_OR_FACTOR_GREEN);

			wsoTrial.startNewRow();
			wsoTrial.addCell(boldHeading, TRAIT_NAME);

			wsoTrial.addCell(boldHeading, "Value");
			// TODO optionally add the BMS headings

			for (TrialAttribute ta : trialAttributes) {
				wsoTrial.startNewRow();
				wsoTrial.addCell(green, ta.getTrialAttributeName());

				String tavalue = ta.getTrialAttributeValue();
				String[] valueAndRest = SectionRowData.splitValuePrefix(tavalue);
				for (String s : valueAndRest) {
					wsoTrial.addCell(s);
				}
			}
		}
		
		public void emitPlotAttributesSheet(List<PlotAttribute> plotAttributes) {
		
			Sheet sheet = workbookOutput.getWorkbook().createSheet(SHEET_NAME_PLOT_ATTRIBUTES);
			WorksheetOutput wsoTrial = new WorksheetOutput(workbookOutput, sheet);

			CellStyle boldHeading = cellStyleProvider.getBoldHeading();
			CellStyle green = cellStyleProvider.getCellStyle(BmsCellColor.CONDITION_OR_FACTOR_GREEN);
			
			wsoTrial.startNewRow();
			wsoTrial.addCell(boldHeading, TRAIT_NAME);

			wsoTrial.addCell(boldHeading, "Alias");

			for (PlotAttribute plotAttribute : plotAttributes) {
				wsoTrial.startNewRow();
				wsoTrial.addCell(green, plotAttribute.getPlotAttributeName());

				wsoTrial.addCell(plotAttribute.getPlotAttributeAlias());
			}
		}

	}
}
