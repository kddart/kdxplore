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

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.curate.ExclusionDialog.ExcludeReasonDialog;
import com.diversityarrays.kdxplore.curate.ExclusionDialog.ExcludeUncuratedSampleReason;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleSource;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;

import net.pearcan.ui.GuiUtil;

public class SuppressionHandler {

	public final CurationContext curationContext;
	
	public final CurationCellEditor curationCellEditor;
	
	public final CurationData curationData;
	
	public List<Consumer<List<KdxSample>>> repaintConsumers = new ArrayList<>();
	
	private static final String NOT_SUPPRESSED = null;
	
	private final SuppressionInfoProvider suppressionInfoProvider;
	
	public SuppressionHandler(
			CurationContext curationContext,
			CurationData curationData,
			CurationCellEditor curationCellEditor,
			SuppressionInfoProvider suppressionInfoProvider) {
		
		this.curationContext = curationContext;
		this.curationCellEditor = curationCellEditor;
		this.curationData = curationData;
		this.suppressionInfoProvider = suppressionInfoProvider;
	}
	
	public void setSamplesSuppressed(
			SuppressOption option,
			SuppressionArgs sargs)
	{
		if (null != sargs) {
			SuppressionData sd = new SuppressionData(option, 
			        sargs.askAboutValueForUnscored,
					sargs.editedSamples, 
					sargs.nonEditedSamples, 
					sargs.ccvs);

			doComplexSuppression(sd);
		}
	}
	
	public void setSamplesSuppressed(SuppressOption option, SuppressionArgs sargs, JComponent curationControls) {
    	if (null != sargs) {
    		SuppressionData sd = new SuppressionData(option, 
    		        sargs.askAboutValueForUnscored,
    				sargs.editedSamples, 
    				sargs.nonEditedSamples, 
    				sargs.ccvs);
    
    		doComplexSuppression(sd, curationControls);
    	}
	}
	
//	public void setSuppressionSimple(SuppressOption option, List<KdxSample> editedSamples, List<CurationCellValue> ccvList) {
//		setSuppressionSimple(option, editedSamples, ccvList, null);
//	}
	
	/**
	 * We only have to change the editedSamples into "suppressed" samples.
	 * @param option
	 * @param editedSamples
	 */
	public void setSuppressionSimple(
	        SuppressOption option, 
	        List<KdxSample> editedSamples, 
	        List<CurationCellValue> ccvList, 
	        JComponent parent) 
	{		
		if (editedSamples.size() > 0) {
			switch (option) {
			case ACCEPT:
				List<KdxSample> notYetIncluded = new ArrayList<>();
				for (KdxSample sample : editedSamples) {
					if (sample.isSuppressed()) {
						notYetIncluded.add(sample);
					}
				}
				
				if (notYetIncluded.isEmpty()) {
					MsgBox.info(parent == null ? curationContext.getDialogOwnerWindow() : parent, 
					        Vocab.MSG_ALL_VALUES_ALREADY_ACCEPTED(),
					        Vocab.ACTION_ACCEPT_TRAIT_VALUES());
				}
				else {
					// Some of them were NOT already included
					Closure<List<EditedSampleInfo>> registerSamplesClosure = new Closure<List<EditedSampleInfo>>() {
						@Override
						public void execute(List<EditedSampleInfo> infoList) {
							registerAcceptedSamples(infoList);
						}
					};
					curationCellEditor.acceptOrSuppressEditedSamples(
							ccvList, NOT_SUPPRESSED, registerSamplesClosure);
				}
				break;

			case REJECT:
				List<KdxSample> notYetRejected = new ArrayList<>();
				for (KdxSample sample : editedSamples) {
					if (! sample.isSuppressed()) {
						notYetRejected.add(sample);
					}
				}
				
				if (notYetRejected.isEmpty()) {
				    MsgBox.info(parent == null ? curationContext.getDialogOwnerWindow() : parent, 
				            Vocab.MSG_ALL_VALUES_ALREADY_SUPPRESSED(),
				            Vocab.ACTION_SUPPRESS_TRAIT_VALUES());
				}
				else {
				    Window owner = parent==null
				            ? curationContext.getDialogOwnerWindow()
				            : GuiUtil.getOwnerWindow(parent);
				    ExcludeReasonDialog dlg = new ExclusionDialog.ExcludeReasonDialog(owner);
				    dlg.setVisible(true);
				    
					String reason = dlg.reason;
					if (reason != null) {
						Closure<List<EditedSampleInfo>> registerSamplesClosure = new Closure<List<EditedSampleInfo>>() {
							@Override
							public void execute(List<EditedSampleInfo> infoList) {
//								for (EditedSampleInfo info : infoList) {
//									curationTableModel.setEditedSampleValue(
//											info.modelRow, info.modelColumn, info.ccid,
//											info.newEditedSample);
//								}
								registerRejectedSamples(infoList, reason);
							}
						};
						curationCellEditor.acceptOrSuppressEditedSamples(ccvList, reason, registerSamplesClosure);
					}
				}
				break;
			}
		}	
	}

	public void doComplexSuppression(SuppressionData suppressionData) {
		 doComplexSuppression(suppressionData, null);
	}
	
	public void doComplexSuppression(SuppressionData suppressionData, JComponent parent) {
		if (! suppressionData.editedSamples.isEmpty() && suppressionData.nonEditedSamples.isEmpty()) {
			// All the samples to be suppressed or approved are editedSamples.
			setSuppressionSimple(suppressionData.suppressOption,
			        suppressionData.editedSamples,
			        suppressionData.curationCellValues,
			        parent);
		}
		else {
			List<CurationCellValue> values = suppressionData.curationCellValues;
			
			if (values.isEmpty()) {
			    String msg;
			    String title;
			    if (suppressionData.suppressOption == SuppressOption.REJECT) {
			        msg = Vocab.MSG_NO_VALID_SAMPLES_SELECTED_TO_SUPPRESS(); 
			        title = Vocab.ACTION_SUPPRESS_TRAIT_VALUES();
			    }
			    else {
			        msg = Vocab.MSG_NO_VALID_SAMPLES_SELECTED_TO_SET(); 
			        title = Vocab.ACTION_ACCEPT_TRAIT_VALUES();
			    }
				MsgBox.warn(parent == null ? curationContext.getDialogOwnerWindow() : parent, msg, title);
			} else {

				// TODO the "edited" samples for both of these should be the "visible" i.e. most recent
				// from both. We can change the "which data" selection to be:
				//    Visible (most recent)
				//    DeviceName     noting that if only one devices data is available then we just use it.
				//    DeviceName     and possibly don't even bother showing the list. So we just want the reason.
				//    ...
				
				// TODO: instead of sampleGetter.getEditedSamples(), make the exclusionDialog call return them
			    
                Window owner = parent == null 
                ? curationContext.getDialogOwnerWindow() 
                : GuiUtil.getOwnerWindow(parent);

			    Pair<SampleSource,ValueForUnscored> pair;
				switch (suppressionData.suppressOption) {
				case ACCEPT:
					
					pair = ExclusionDialog.confirmIncludeUnCuratedSamples(owner,
							curationCellEditor, suppressionData.askAboutValueForUnscored, values);
					
					if (pair != null) {
					    SampleSource selectedSampleSource = pair.first;
					    ValueForUnscored valueForUnscored = pair.second;

						Closure<List<EditedSampleInfo>> registerSamplesClosure = new Closure<List<EditedSampleInfo>>() {
							@Override
							public void execute(List<EditedSampleInfo> infoList) {
//								for (EditedSampleInfo info : infoList) {
//									info.newEditedSample.setTraitValue(valueToUse);
//								}
								
								registerAcceptedSamples(infoList);
							}
						};
						curationCellEditor.acceptOrSuppressValueIgnoringParameters(
								valueForUnscored,
								selectedSampleSource, 
								values, 
								NOT_SUPPRESSED,
								registerSamplesClosure);
						
//						addSuppressChangeable(newEditedSamples, null);
					} 
					break;

				case REJECT:
				    ExcludeUncuratedSampleReason dlg = new ExclusionDialog.ExcludeUncuratedSampleReason(
				            owner, curationCellEditor, 
				            suppressionData.askAboutValueForUnscored,
				            values);
				    dlg.setVisible(true);
				    
				    if (dlg.sampleSource != null) {
				        String reason = dlg.excludeReason;
                        Closure<List<EditedSampleInfo>> closure = new Closure<List<EditedSampleInfo>>() {
                            @Override
                            public void execute(List<EditedSampleInfo> infoList) {
//                              for (EditedSampleInfo info : infoList) {
////                                    info.newEditedSample.setSuppressedReason(reason);
//                                  curationTableModel.setEditedSampleValue(
//                                          info.modelRow, info.modelColumn, info.ccid,
//                                          info.newEditedSample);
//                              }
                                registerRejectedSamples(infoList, reason);
                            }
                        };

                        curationCellEditor.acceptOrSuppressValueIgnoringParameters(
                                dlg.valueForUnscored,
                                dlg.sampleSource, 
                                values, 
                                reason,
                                closure);
				    }
				    
					break;
				}
			}
		}
	}

	public void registerRejectedSamples(List<EditedSampleInfo> infoList, String reason) {
		curationData.registerRejectedSampleInfo(infoList, reason);
		for (Consumer<List<KdxSample>> repaintConsumer : repaintConsumers) {
			repaintConsumer.accept(new ArrayList<>());
		}
	}

	public void registerAcceptedSamples(List<EditedSampleInfo> infoList) {
		curationData.registerAcceptedSampleInfo(infoList); // ???? been through here
		for (Consumer<List<KdxSample>> repaintConsumer : repaintConsumers) {
			repaintConsumer.accept(new ArrayList<>());
		}	
	}	

	public void addRefreshConsumer(Consumer<List<KdxSample>> repaintConsumer) {
		this.repaintConsumers.add(repaintConsumer);
	}
	
	public void clearRefreshConsumers() {
		this.repaintConsumers.clear();
	}

	public SuppressionArgs createSuppressionArgs(boolean askAboutValueForUnscored, 
	        List<SampleName> selectedSamples, 
	        List<TraitInstance> instances) 
	{
		return suppressionInfoProvider.createSuppressionArgs(
		        askAboutValueForUnscored, selectedSamples, instances);
	}

	public boolean isTraitInstanceEnabled(TraitInstance ti) {
		return suppressionInfoProvider.isTraitInstanceEnabled(ti);

	}

	public void addTraitChangeListener(Consumer<Void> consumer) {
		suppressionInfoProvider.addTraitChangeListener(consumer);
	}
}
