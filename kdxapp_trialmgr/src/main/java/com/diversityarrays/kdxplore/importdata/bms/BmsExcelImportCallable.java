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
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.KDSmartApplication;
import com.diversityarrays.kdsmart.db.BatchHandler;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.csvio.ImportError;
import com.diversityarrays.kdsmart.db.csvio.PlotIdentCollector;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.db.util.CreateItemException;
import com.diversityarrays.kdsmart.db.util.ItemConsumerHelper;
import com.diversityarrays.kdxplore.data.kdx.DeviceAndOperator;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.trialmgr.ImportDataException;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Origin;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.Traversal;

/* TODO use more headings from VARIATE section:
 * Rotated they are:
 * VARIATE		AleuCol_1_5
 * DESCRIPTION	Aleurone color - AleuCol observation (1-5 ALEUCOL scale)
 * PROPERTY		Aleurone color
 * SCALE		1-5 ALEUCOL scale
 * METHOD		AleuCol observation
 * DATA TYPE	C
 * ---
 * VARIATE		SilkLng  _cm
 * DESCRIPTION	Silk length - SilkLng   measurement (cm)
 * PROPERTY		Silk length
 * SCALE		cm
 * METHOD		SilkLng measurement
 * DATA TYPE	N
 * ---
 */
public class BmsExcelImportCallable implements BatchHandler<Either<Throwable,BmsXlsTrialImportResult>> {

		final ItemConsumerHelper itemConsumerHelper;

		final PlotIdentCollector plotIdentCollector = new PlotIdentCollector(KDSmartApplication.getInstance());

		final Map<String, PlotAttribute> plotAttributeByName = new HashMap<>();
		
//		final List<PlotAttributeValue> plotAttributeValues = new ArrayList<>();
		int nPlotAttributeValues = 0;
		
		final List<Plot> plots = new ArrayList<>();
//		final List<Sample> samples = new ArrayList<>();
		int nSamples = 0;
		final Map<String, Trait> traitByName = new HashMap<>();
		
		final Map<String,Set<Integer>> lineNumbersByMessage = new TreeMap<>();
		
		private final KDSmartDatabase kdsmartDatabase;
		private final File excelFile;

		private final int traitInstanceNumber;
		
		private PlotValueConsumer plotValueConsumer = new PlotValueConsumer() {
			
//			final DateFormat elapsedDaysDateFormat = TraitValue.getTraitValueDateFormat();

			int scoringSortOrder = 0;
			
			private Trait getTrait(String traitName) throws CreateItemException {
				Trait trait = traitByName.get(traitName.toLowerCase());
				if (trait == null) {
					trait = new Trait();
					trait.setTraitName(traitName);
					trait.setTraitDataType(TraitDataType.TEXT);

					itemConsumerHelper.createNewItemInDatabase(Trait.class, trait);

					traitByName.put(traitName.toLowerCase(), trait);

					importResult.trialTraits.put(traitName, trait);
				}
				else {
					importResult.trialTraits.put(traitName, trait);
				}
				return trait;
			}
			
			Map<Trait, ValidationRule> valRuleByTrait = new HashMap<>();
			
			private ValidationRule getValidationRule(Trait t) {
			    ValidationRule result = valRuleByTrait.get(t);
			    if (result == null) {
			        String vrs = t.getTraitValRule();
			        if (Check.isEmpty(vrs)) {
			            result = ValidationRule.NO_VALIDATION_RULE;
			        }
			        else {
			            try {
                            result = ValidationRule.create(vrs);
                        }
                        catch (InvalidRuleException e) {
                            result = ValidationRule.NO_VALIDATION_RULE;
                        }
			        }
			        valRuleByTrait.put(t, result);
			    }
			    return result;
			}
			
			@Override
			public void storeTraitValue(Plot plot, String traitName, String traitValue) 
			throws CreateItemException, ImportDataException 
			{
				Trait trait = getTrait(traitName);

				KdxSample sample = new KdxSample();
				
				sample.setSampleGroupId(importResult.sampleGroup.getSampleGroupId());
				
				sample.setTrialId(plot.getTrialId());
				sample.setPlotId(plot.getPlotId());
				sample.setTraitId(trait.getTraitId());
				sample.setTraitInstanceNumber(traitInstanceNumber);

				if (traitValue != null && !traitValue.isEmpty()) {
					sample.setMeasureDateTime(TraitValue.MEASURED_DATE_TIME_FROM_IMPORT);
					sample.setTraitValue(traitValue);
					
					ValidationRule vrule = getValidationRule(trait);

					switch (trait.getTraitDataType()) {
                    case CALC:
                        // TODO something here
                        break;
                    case CATEGORICAL:
                        if (! vrule.evaluate(traitValue)) {
                            throw new ImportDataException(traitValue, 
                                    "Failed: " + vrule.getDescription());
                        }
                        break;
                    case DATE:
                        break;

                    case DECIMAL:
                        try {
                            double d = Double.parseDouble(traitValue);
                            if (! vrule.evaluate(traitValue)) {
                                throw new ImportDataException(traitValue, 
                                        "Failed: " + vrule.getDescription());
                            }                            
                        }
                        catch (NumberFormatException e) {
                            throw new ImportDataException(traitValue, 
                                    "Non-numeric value '" + traitValue + "' for " + traitName);
                        }
                        break;
                    case ELAPSED_DAYS:
                    case INTEGER:
                        try {
                            double d = Double.parseDouble(traitValue);
                            int nDays = (int) d;
                            sample.setTraitValue(Integer.toString(nDays));
                            if (! vrule.evaluate(traitValue)) {
                                throw new ImportDataException(traitValue, 
                                        "Failed: " + vrule.getDescription());
                            }
                        } catch (NumberFormatException e) {
                            throw new ImportDataException(traitValue, 
                                    "Non-numeric value '" + traitValue + "' for " + traitName);
                        }
                        break;
                    case TEXT:
                        break;
                    default:
                        break;					
					}
					
					// TAG_ELAPSED_DAYS_AS_INTEGER
//					if (TraitDataType.ELAPSED_DAYS == trait.getTraitDataType()) {
//						try {
//							int nDays = Integer.parseInt(traitValue);
//							if (trialPlantingDate == null) {
//								String msg = "'" + traitValue + "' : No TrialPlantingDate available for '" + trait.getTraitName() + "'";
//								throw new InvalidValueException(null, msg, traitValue);
//							}
//							Date traitValueAsDate = TraitValue.nDaysAsElapsedDaysDate(trialPlantingDate, nDays);
//							sample.setTraitValue(elapsedDaysDateFormat.format(traitValueAsDate));
//						} catch (NumberFormatException e) {
//							// It is either in a date format or invalid
//							sample.setTraitValue(traitValue);
//						}
//						
//					}
//					else {
//					}
				}

				itemConsumerHelper.createNewItemInDatabase(KdxSample.class, sample);

				++nSamples;
				importResult.sampleGroup.addSample(sample);
			}

			@Override
			public void storePlotAttributeValue(Plot plot, String attributeName, String attributeValue)
			throws CreateItemException 
			{
				PlotAttribute pa = plotAttributeByName.get(attributeName); // FIXME case-insensitive
				if (pa == null) {
					pa = new PlotAttribute();
					pa.setPlotAttributeName(attributeName);
					pa.setTrialId(plot.getTrialId());

					itemConsumerHelper.createNewItemInDatabase(PlotAttribute.class, pa);

					plotAttributeByName.put(pa.getPlotAttributeName(), pa);
				}

				PlotAttributeValue pav = new PlotAttributeValue();
				pav.setTrialId(plot.getTrialId());
				pav.setPlotId(plot.getPlotId());
				pav.setAttributeId(pa.getPlotAttributeId());
				pav.setAttributeValue(attributeValue==null ? "" : attributeValue);

				itemConsumerHelper.createNewItemInDatabase(PlotAttributeValue.class, pav);

				++nPlotAttributeValues;
//				plotAttributeValues.add(pav);
			}

			@Override
			public void plotComplete(int lineNumber, Plot plot) throws CreateItemException {
				itemConsumerHelper.createNewItemInDatabase(Plot.class, plot);
				plots.add(plot);
				ImportError importError = plotIdentCollector.collectPlotIdentifiers(lineNumber, plot);
				if (importError != null) { 
					throw new CreateItemException(importError.message); 
				}
			}

			@Override
			public void warn(int lineNumber, String msg) {
				Set<Integer> range = lineNumbersByMessage.get(msg);
				if (range == null) {
					range = new TreeSet<>();
					lineNumbersByMessage.put(msg, range);
				}
				range.add(lineNumber);
			}

			@Override
			public void createTraitInstance(String traitName) throws CreateItemException {
				Trait trait = getTrait(traitName);
				TraitInstance ti = new TraitInstance();
				ti.setTrialId(importResult.trial.getTrialId());
				ti.setTraitId(trait.getTraitId());
				ti.setInstanceNumber(1);
				ti.setScoringSortOrder(++scoringSortOrder);
				ti.setUsedForScoring(true);
				itemConsumerHelper.createNewItemInDatabase(TraitInstance.class, ti);

			}
		};

		private final DeviceAndOperator deviceAndOperator;

		private final Set<String> lowcaseTrialNames;

		private BmsXlsTrialImportResult importResult;

		private final BmsImportOptions bmsImportOptions;
		
		public BmsExcelImportCallable(KDSmartDatabase db, 
				File excelFile, 
				DeviceAndOperator devAndOp, 
				int traitInstanceNumber,
				BmsImportOptions bmsImportOptions,
				Set<String> lowcaseTrialNames)
		{
			this.kdsmartDatabase = db;
			
			this.excelFile = excelFile;
			this.deviceAndOperator = devAndOp;
			this.traitInstanceNumber = traitInstanceNumber;
			this.bmsImportOptions = bmsImportOptions;
			this.lowcaseTrialNames = lowcaseTrialNames;

			itemConsumerHelper = kdsmartDatabase.getItemConsumerHelper();
		}

		public Either<Throwable,BmsXlsTrialImportResult> call() {

			Throwable throwable = null;
			try {
				BmsExcelImportHelper importHelper = new BmsExcelImportHelper(excelFile);
				importResult = importHelper.parseTrialData(
						bmsImportOptions.trialNameInstanceSeparator, lowcaseTrialNames);

				for (Pair<String,Field> pair : importResult.plotFactorFields) {
					if (Plot.FIELDNAME_USER_PLOT_ID.equals(pair.second.getName())) {
						plotIdentCollector.setUsingPlotId();
					}
					else if (Plot.FIELDNAME_PLOT_COLUMN.equals(pair.second.getName())) {
						plotIdentCollector.setUsingPlotX();
					}
					else if (Plot.FIELDNAME_PLOT_ROW.equals(pair.second.getName())) {
						plotIdentCollector.setUsingPlotY();
					}
				}
				// We need the trialId when we parse the Samples as we will be creating
				// Plots (and possibly Samples) - which need a trialId.
				itemConsumerHelper.createNewItemInDatabase(Trial.class, importResult.trial);
				
				// Now establish the SampleGroup instance
				importResult.sampleGroup.setDeviceIdentifierId(deviceAndOperator.deviceIdentifier.getDeviceIdentifierId());
				importResult.sampleGroup.setOperatorName(deviceAndOperator.operatorName);
				importResult.sampleGroup.setTrialId(importResult.trial.getTrialId());
				// Note that we don't bother collecting the Samples at this stage.

				itemConsumerHelper.createNewItemInDatabase(SampleGroup.class, importResult.sampleGroup);
				
				for (Trait trait : kdsmartDatabase.getTraits()) {
					traitByName.put(trait.getTraitName().toLowerCase(), trait);
				}

				importResult.trial.setTraitNameStyle(TraitNameStyle.NO_INSTANCES);
				importResult.trial.setDateDownloaded(new Date());
				importResult.trial.setNameForPlot("Plot");
				importResult.trial.setNameForColumn("Row");
				importResult.trial.setNameForRow("Range");

				// 1) traitByName MUST be populated before calling this
				// 2) trial.trialPlantingDate MUST be set before calling this
				//   if any ELAPSED_DAYS Traits have non-empty values
				//   And it should be in the "PlantingDate" CONDITION
				importHelper.parsePlotsAndSamples(importResult, plotValueConsumer);

				PlotIdentSummary plotIdentSummary = plotIdentCollector.getPlotIdentSummary();

				importResult.trial.setPlotIdentSummary(plotIdentSummary);

				TrialLayout trialLayout = TrialLayout.create(Origin.LOWER_LEFT, Traversal.TWO_WAY, 
						plotIdentSummary);

				PlotIdentOption plotIdentOption = PlotIdentOption.create(plotIdentSummary);
				
				importResult.trial.setPlotIdentOption(plotIdentOption);

				importResult.trial.setTrialLayout(trialLayout);

				// Now we have used the Plot to set the remaining Trial fields we must update
				itemConsumerHelper.updateItemInDatabase(Trial.class, importResult.trial);
				
				List<String> attributeNames = new ArrayList<>(importResult.trialAttributesByName.keySet());
				if (bmsImportOptions.sortTrialAttributesByName) {
					Collections.sort(attributeNames);
				}
				
				for (String attributeName : attributeNames) {
					String attributeValue = importResult.trialAttributesByName.get(attributeName);
					TrialAttribute ta = new TrialAttribute();
					ta.setTrialId(importResult.trial.getTrialId());
					ta.setTrialAttributeName(attributeName);
					ta.setTrialAttributeValue(attributeValue);
					
					itemConsumerHelper.createNewItemInDatabase(TrialAttribute.class, ta);
				}
				
				if (! lineNumbersByMessage.isEmpty()) {
					importResult.lineNumbersByMessage = lineNumbersByMessage;
				}
				
				itemConsumerHelper.notifyCreatedTTT();
				
				return Either.right(importResult);
				
			} catch (IOException e) {
				throwable = e;
			} catch (CreateItemException e) {
				throwable = e;
			} catch (IllegalArgumentException e) {
				throwable = e;
			} catch (ImportDataException e) {
				throwable = e;
			}
			
			return Either.left(throwable);
		}
		
		public void report(PrintStream ps) {
			
			ps.println("Collected " + plots.size() + " plots");
			
			Bag<String> plotTypeCounts = new HashBag<>();
			for (Plot plot : plots) {
				plotTypeCounts.add(plot.getPlotType());
			}
			
			ps.println("Found PlotTypes:");
			for (String plotType : plotTypeCounts.uniqueSet()) {
				ps.println("\t" + plotType + ": " + plotTypeCounts.getCount(plotType));
			}

			ps.println("Created " + traitByName.size() + " Traits");
			for (Trait trait : traitByName.values()) {
				ps.println("\t" + trait + " [id=" + trait.getTraitId() + "]");

			}

			ps.println("Created " + plotAttributeByName.size() + " Plot Attributes");
			for (PlotAttribute pa : plotAttributeByName.values()) {
				ps.println("\t" + pa);
			}
			
			ps.println("Created " + nPlotAttributeValues + " Plot Attribute Values");
			ps.println("Created " + nSamples + " Samples");
			
			if (! lineNumbersByMessage.isEmpty()) {
				ps.println("Warnings:");
				for (String msg : lineNumbersByMessage.keySet()) {
					Set<Integer> range = lineNumbersByMessage.get(msg);
					ps.println(range.size()+" times: " + msg);
				}
			}
		}
		
		public Trial getImportedTrial() {
			return importResult.trial;
		}

        @Override
        public boolean checkSuccess(Either<Throwable, BmsXlsTrialImportResult> either) {
            return either.isRight();
        }
	}
