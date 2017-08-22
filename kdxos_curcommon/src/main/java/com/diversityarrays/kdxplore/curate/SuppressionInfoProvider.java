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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JTable;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class SuppressionInfoProvider {

		private final CurationData curationData;
		private final CurationTableModel curationTableModel;
		private final JTable curationTable;
		
		public SuppressionInfoProvider(
				CurationData curationData,
				CurationTableModel curationTableModel,
				JTable curationTable) 
		{
			this.curationData = curationData;
			this.curationTableModel = curationTableModel;
			this.curationTable = curationTable;
		}
		
		private List<CurationCellValue> collectCurationCellValues() {
			List<CurationCellValue> selectedCells = new ArrayList<CurationCellValue>();

			int[] rows = curationTable.getSelectedRows();
			int[] cols = curationTable.getSelectedColumns();

			for (int c = 0; c < cols.length; c++) {
				for (int i = 0; i < rows.length; i++) {	

					int modelColumn  = curationTable.convertColumnIndexToModel(cols[c]);
					int modelRow = curationTable.convertRowIndexToModel(rows[i]);

					CurationCellValue ccv = curationTableModel.getCurationCellValue(modelRow, modelColumn);
					if (ccv == null) {
						//TODO - NOT SURE WHY THIS IS RETURNING NULL
						//						Sample sample = curationTableModel.getSample(modelRow, modelColumn);
						//						if (sample != null) {
						//							selectedSamples.add(sample);
						//						}
					}
					else {
						selectedCells.add(ccv);
					}
				}
			}
			return selectedCells;
		}

		private List<CurationCellValue> getCurationCellValuesForSamples(List<SampleName> sampleNames, List<TraitInstance> instances) {
			List<CurationCellValue> selectedCells = new ArrayList<CurationCellValue>();

			for (SampleName sampleName : sampleNames) {
				List<CurationCellValue> ccvs = curationTableModel.getCurationCellValuesForPlotSpecial(sampleName.getPlotId(), instances);		
				if (ccvs != null) {
					for (CurationCellValue ccv : ccvs) {
					    KdxSample match = sampleName.getFirstSampleForCurationSampleType(ccv, CurationSampleType.ANY);
					    if (match != null) {
							selectedCells.add(ccv);
						}
					}
				}
			}
			return selectedCells;
		}

		private List<KdxSample> getKdxSamplesForSampleNames(List<SampleName> sampleNames, CurationSampleType onlyEdited, List<TraitInstance> instances) {
			List<KdxSample> selectedCells = new ArrayList<KdxSample>();

			for (SampleName sampleName : sampleNames) {
				List<CurationCellValue> ccvs = curationTableModel.getCurationCellValuesForPlotSpecial(sampleName.getPlotId(), instances);				
				if (ccvs != null) {
					for (CurationCellValue ccv : ccvs) {
					    KdxSample match = sampleName.getFirstSampleForCurationSampleType(ccv, onlyEdited);
					    if (match != null) {
					        selectedCells.add(match);
					    }
					}
				}
			}
			return selectedCells;
		}

		private List<KdxSample> collectSelectedSamples(boolean onlyEdited) {
			// need to return the CCVs paired with the samples
			// so we can build EditedSampleInfo instances
			List<KdxSample> selectedSamples = new ArrayList<>();

			int[] rows = curationTable.getSelectedRows();
			int[] cols = curationTable.getSelectedColumns();

			for (int c = 0; c < cols.length; c++) {
				for (int i = 0; i < rows.length; i++) {	

					if (onlyEdited) {
						int modelColumn  = curationTable.convertColumnIndexToModel(cols[c]);
						int modelRow = curationTable.convertRowIndexToModel(rows[i]);

						CurationCellValue ccv = curationTableModel.getCurationCellValue(modelRow, modelColumn);
						if (ccv == null) {
							//TODO - NOT SURE WHY THIS IS RETURNING NULL
//							Sample sample = curationTableModel.getSample(modelRow, modelColumn);
//							if (sample != null) {
//								selectedSamples.add(sample);
//							}
						}
						else {
							if (ccv.getEditedSample() != null) {
								selectedSamples.add(ccv.getEditedSample());
							}
						}
					} else {

						TraitInstance ti = curationTableModel.getTraitInstanceAt(cols[c]);

						if (ti != null) {
							String ti_ident = InstanceIdentifierUtil.getInstanceIdentifier(ti);
							
							PlotOrSpecimen pos = curationTableModel.getPlotOrSpecimenAtRowIndex(rows[i]);
							Consumer<KdxSample> visitor = new Consumer<KdxSample>() {
                                @Override
                                public void accept(KdxSample sample) {
                                    if (InstanceIdentifierUtil.getInstanceIdentifier(sample).equals(ti_ident)) {
                                        if (!selectedSamples.contains(sample)) {
                                            selectedSamples.add(sample);
                                        }
                                    }
                                }
                            };
                            curationData.visitSamplesForPlotOrSpecimen(pos, visitor);
						}
					}
				}
			}
			return selectedSamples;
		}

		public SuppressionArgs createSuppressionArgs(boolean askAboutValueForUnscored, List<SampleName> samples, List<TraitInstance> instances) {
			SuppressionArgs result = null;
			List<CurationCellValue> ccvs = getCurationCellValuesForSamples(samples, instances);
			List<KdxSample> editedSamples = getKdxSamplesForSampleNames(samples, CurationSampleType.EDITED, instances);
			List<KdxSample> nonEditedSamples = getKdxSamplesForSampleNames(samples, CurationSampleType.NOT_EDITED, instances);
			if (! ccvs.isEmpty()) {
				result = new SuppressionArgs(
				        askAboutValueForUnscored,
						ccvs,
						editedSamples,
						nonEditedSamples);
			}
			return result;
		}

		public SuppressionArgs createSuppressionArgs(boolean askAboutValueForUnscored) {
			SuppressionArgs result = null;
			List<CurationCellValue> ccvs = collectCurationCellValues();
			List<KdxSample> editedSamples = collectSelectedSamples(true);
			List<KdxSample> nonEditedSamples = collectSelectedSamples(false);
			if (! ccvs.isEmpty()) {
				result = new SuppressionArgs(
				        askAboutValueForUnscored,
						ccvs,
						editedSamples,
						nonEditedSamples);
			}
			return result;
		}

		public boolean isTraitInstanceEnabled(TraitInstance ti) {
			return null != this.curationTableModel.getColumnIndexForTraitInstance(ti);
		}

		public void addTraitChangeListener(Consumer<Void> consumer) {
			curationTableModel.addTraitChangeListener(consumer);
		}

}
