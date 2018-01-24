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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.curate.fieldview.FieldLayoutView;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.dal.SampleType;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.data.kdx.SampleSource;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.GBH;

class CurationCellEditorImpl extends JPanel implements CurationCellEditor {
	
	private final TypedSampleMeasurementTableModel typedSampleTableModel = new TypedSampleMeasurementTableModel();
	private final JTable typedSampleTable = new JTable(typedSampleTableModel);
	
    private final JCheckBox showPpiOption = new JCheckBox(Vocab.OPTION_SHOW_POSITION(), true);

	public static final boolean IGNORE_PARAMETERS = true;
	public static final boolean DONT_IGNORE_PARAMETERS = false;

	private final CurationTableModel curationTableModel;
	private final Closure<Void> refreshFieldLayoutView;
	private final CurationData curationData;

//	private final EditedSampleManager editedSampleFactory;
	
	private final Map<Integer,DeviceIdentifier> devIdentifierById = new HashMap<>();
	
	// When changing multiple samples for a TraitInstance, the user must
	// tell us which ones they wish the change to apply to.
	// This makes it easier for the user to select a number of rows (Samples in a Plot) 
	// and only affect some of those samples.

	private final SampleEntryPanel sampleEntryPanel;

	private ListSelectionListener typedSampleListSelectionListener = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
		    if (! e.getValueIsAdjusting()) {
		        sampleEntryPanel.updateValueLabel(null, null);
		    }
		}
	};

	private FieldLayoutView fieldViewLayoutPanel;

	private final CustomDateCellRenderer dateCellRenderer = new CustomDateCellRenderer();
	
	private final TsmCellRenderer tsmCellRenderer = new TsmCellRenderer();
	
	private final BiConsumer<Comparable<?>,List<CurationCellValue>> changedValueConsumer = new BiConsumer<Comparable<?>,List<CurationCellValue>>() {
        @Override
        public void accept(Comparable<?> value, List<CurationCellValue> curationCellValues) {
            for (CurationCellValue ccv : curationCellValues) {
                curationTableModel.setTemporaryValue(ccv.getCurationCellId(), value);
            }
            fieldViewLayoutPanel.setTemporaryValue(curationCellValues, value);
        }
	};
	
	public CurationCellEditorImpl(
			CurationTableModel curationTableModel,
			FieldLayoutView fieldViewLayoutPanel,
			CurationData curationData, 
			Closure<Void> refresh, 
			KdxploreDatabase kdxdb,
			IntFunction<Trait> traitProvider,
			SampleType[] sampleTypes) 
	throws IOException 
	{
		super();
		
		this.fieldViewLayoutPanel = fieldViewLayoutPanel;
		this.refreshFieldLayoutView = refresh;
		this.curationData = curationData;
		this.curationTableModel = curationTableModel;
		
		sampleEntryPanel = new SampleEntryPanel(curationData, 
		        traitProvider,
		        typedSampleTableModel, typedSampleTable, tsmCellRenderer,
		        showPpiOption,
		        refreshFieldLayoutView,
		        changedValueConsumer,
		        sampleTypes);
		 
		typedSampleTableModel.setCurationData(curationData);
		 
		for (DeviceIdentifier did : kdxdb.getDeviceIdentifiers()) {
			devIdentifierById.put(did.getDeviceIdentifierId(), did);
		}
		
		typedSampleTableModel.setShowSampleType(sampleTypes.length > 0);

		typedSampleTable.setRowHeight(typedSampleTable.getRowHeight() * 2);
		
		typedSampleTable.setAutoCreateRowSorter(true);

		typedSampleTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		typedSampleTable.getSelectionModel().addListSelectionListener(typedSampleListSelectionListener);
		
		typedSampleTable.setDefaultRenderer(String.class, new CurationCellRenderer(typedSampleTableModel));
        typedSampleTable.setDefaultRenderer(java.util.Date.class, dateCellRenderer);
        typedSampleTable.setDefaultRenderer(TypedSampleMeasurement.class, tsmCellRenderer);
        GuiUtil.setVisibleRowCount(typedSampleTable, 5);
//      typedSampleTable.setDefaultRenderer(Integer.class, new NumberCellRenderer(null, SwingConstants.CENTER));
//		typedSampleTable.setDefaultRenderer(String.class, new CurationCellRenderer());
//		typedSampleTable.setDefaultRenderer(Double.class, new CurationCellRenderer());
//		typedSampleTable.setDefaultRenderer(Object.class, new CurationCellRenderer());
//		typedSampleTable.setDefaultRenderer(String.class, new CurationCellRenderer());
//		typedSampleTable.setDefaultRenderer(Integer.class, new CurationCellRenderer());
		
        showPpiOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                typedSampleTableModel.setShowPositions(showPpiOption.isSelected());
            }
        });

		
		GBH gbh = new GBH(this);
		
		int y = 0;

		gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, GuiUtil.createLabelSeparator(Vocab.TITLE_SAMPLE_VALUES(), showPpiOption));
		++y;

		gbh.add(0,y, 1,1, GBH.BOTH, 2,3, GBH.CENTER, new PromptScrollPane(typedSampleTable, "No Samples"));
		++y;

		gbh.add(0,y, 1,1, GBH.BOTH, 1,1, GBH.CENTER, sampleEntryPanel);
		++y;
	}
	
	@Override
	public Component getComponent() {
	    return this;
	}

	@Override
	public void acceptOrSuppressEditedSamples(
			List<CurationCellValue> ccvList,
			String reasonForSuppression,
			Closure<List<EditedSampleInfo>> register)
	{
		boolean suppressing = reasonForSuppression != null;
		
		List<EditedSampleInfo> infoList = new ArrayList<>();
		
		final SampleType sampleType = sampleEntryPanel.getSelectedSampleType();

		for (CurationCellValue ccv : ccvList) {

			String traitValue = null;
			java.util.Date when = null;

			// If editedSample alreadyExists then don't change it
			KdxSample editedSample = ccv.getEditedSample();
			if (editedSample != null) {
				if (suppressing == editedSample.isSuppressed()) {
					// doing the same thing as the sample's suppression state so NO need to change
					continue;
				}
				// If we get here we are changing it
				traitValue = editedSample.getTraitValue();
				when = editedSample.getMeasureDateTime();
				
				// It shouldn't be null but let's just protect
				KdxSample newEditedSample = curationData.createEditedSampleMeasurement(
						ccv, 
						traitValue, when,
						sampleType,
						reasonForSuppression);
				
				infoList.add(new EditedSampleInfo(
						ccv.getCurationCellId(),
						editedSample, newEditedSample));
			}
		} // each ccv

		if (! infoList.isEmpty()) {
			register.execute(infoList);
		}

		refreshFieldLayoutView.execute(null);
		this.repaint();
	}
	
	/**
	 * 
	 * @param selectedDeviceName
	 * @param ccvList
	 * @param reasonForSuppression
	 * @param register
	 */
	@Override
	public void acceptOrSuppressValueIgnoringParameters(
	        ValueForUnscored valueForUnscored,
			SampleSource source,
			List<CurationCellValue> ccvList,
			String reasonForSuppression,
			Closure<List<EditedSampleInfo>> register)
	{		
		List<EditedSampleInfo> infoList = new ArrayList<>();
		
		final SampleType sampleType = sampleEntryPanel.getSelectedSampleType();

		final java.util.Date now = new java.util.Date();
		
		for (CurationCellValue ccv : ccvList) {
			// If editedSample alreadyExists then don't change it
			KdxSample editedSample = ccv.getEditedSample();
			if (editedSample != null) {
				continue;
			}

			String traitValue =	null;
			java.util.Date when = null;

			KdxSample sampleToUse = null;
			if (source.equals(SampleSource.MOST_RECENT)) {
				sampleToUse = ccv.getLatestRawSample();
			} else if (source == SampleSource.DATABASE) {
			    sampleToUse = ccv.getDatabaseSample();
			} else if (source == SampleSource.CURATED) {
			    sampleToUse = ccv.getEditedSample();
//			} else if (source == SampleSource.SCORING) {
//				//TODO - Check on whether SampleSource.SCORING is still relevant
//			    sampleToUse = null;
			} else {
				//TODO - TODO - TODO This does currently not support multiple different samples for the same plot
				// from the same device. It only grabs the first one it finds.
			    sampleToUse = sampleEntryPanel.findSampleForDevice(ccv, source.sampleGroupId);
			}
			
			boolean applyValueForUnscored;
            if (sampleToUse != null && sampleToUse.hasBeenScored()) {
                traitValue = sampleToUse.getTraitValue();
                
                switch (TraitValue.classify(traitValue)) {
                case MISSING:
                case NA:
                case SET:
                    when = sampleToUse.getMeasureDateTime();
                    applyValueForUnscored = false;
                    break;

                case UNSET:
                default:
                    applyValueForUnscored = true;
                    traitValue = null;
                    when = null;
                    break;              
                }
            }
            else {
                applyValueForUnscored = true;
            }

            boolean doSample = true;
            if (applyValueForUnscored) {
                switch (valueForUnscored) {
                case DONT_USE:
                    doSample = false;
                    break;
                case MISSING:
                    traitValue = TraitValue.VALUE_MISSING;
                    when = now;
                    break;
                case NA:
                    traitValue = TraitValue.VALUE_NA;
                    when = now;
                    break;
                default:
                    throw new RuntimeException("Unhandled ValueForUnscored: " + valueForUnscored); //$NON-NLS-1$
                }
            }

            if (doSample) {    
                KdxSample newEditedSample = curationData.createEditedSampleMeasurement(
                        ccv, 
                        traitValue, when,
                        sampleType,
                        reasonForSuppression);
                
                infoList.add(new EditedSampleInfo(
                        ccv.getCurationCellId(),
                        editedSample, newEditedSample));
                ccv.setEditedSample(newEditedSample); // ignore boolean result
            }
		} // each ccv

		if (! infoList.isEmpty()) {
			register.execute(infoList);
		}

		refreshFieldLayoutView.execute(null);
		this.repaint();
	}

	@Override
	public void setAllValuesToAccepted(List<CurationCellValue> ccvs, SampleGroup sourceGroup) {

		List<EditedSampleInfo> infoList = new ArrayList<>();
		
		DeviceIdentifier did = curationData.getDeviceIdentifierForSampleGroup(sourceGroup.getSampleGroupId());
		Date sampleGroupDate = curationData.getSampleGroupDateLoaded(sourceGroup.getSampleGroupId());
		SampleSource source = SampleSource.createDeviceSampleSource(did.getDeviceName(),
				sourceGroup.getSampleGroupId(), sampleGroupDate);
		
		sampleEntryPanel.refreshFromCheckboxes();

		SampleType sampleType = sampleEntryPanel.getSelectedSampleType();

		final java.util.Date now = new java.util.Date();
		
		
		for (CurationCellValue ccv : ccvs) {
			
			KdxSample curatedSample = ccv.getEditedSample();
			
			KdxSample deviceIdSample = null;
			if (SampleSource.MOST_RECENT.equals(source)) {
				deviceIdSample = ccv.getLatestRawSample();
			}
			else {
				if (source != null && DeviceType.KDSMART==source.deviceType) {
					deviceIdSample = sampleEntryPanel.findSampleForDevice(ccv, source.sampleGroupId);
				}
			}
			
			java.util.Date when;
			String traitValue;
			if (deviceIdSample != null) {
				when = deviceIdSample.getMeasureDateTime();
				traitValue = deviceIdSample.getTraitValue();

				switch (TraitValue.classify(traitValue)) {
				case MISSING:
				case NA:
				case SET:
					break;

				case UNSET:
				default:
					// NOTE: converting UNSET to missing
					traitValue = TraitValue.VALUE_MISSING;
					when = now;
					break;				
				}
			}
			else {
				// There is no deviceSample
				traitValue = TraitValue.VALUE_MISSING;
				when = now;
			}
			
			KdxSample newEditedSample = curationData.createEditedSampleMeasurement(
					ccv, 
					traitValue, when,
					sampleType,
					SampleEntryPanel.NOT_SUPPRESSED);

			ccv.setEditedSample(newEditedSample);

			infoList.add(new EditedSampleInfo(
					ccv.getCurationCellId(),
					curatedSample, newEditedSample));
		} // each ccv

		if (infoList.isEmpty()) {
			String html = sampleEntryPanel.getApplyToHtmlDescription();
			
			MsgBox.info(CurationCellEditorImpl.this, html, Vocab.ACTION_SET_VALUE());
			return;
		}
		else {
			curationData.registerChangeablesForNewEditedSamples(infoList);
		}

		refreshFieldLayoutView.execute(null);
		this.repaint();
	}
	


	
	class CustomDateCellRenderer extends DefaultTableCellRenderer {
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); //$NON-NLS-1$
		private final Color alternateRowColor;
		
		public CustomDateCellRenderer() {
			alternateRowColor = KdxplorePreferences.getInstance().getAlternateRowColor();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			if (value == null) {
				setText(Vocab.MSG_DATE_NOT_SCORED());
			}
			else if (value instanceof Date) {
				Date date = (Date) value;
				if (date.getTime() == 0) {
					setText(Vocab.MSG_DATE_UNKNOWN());
				}
				else {
					setText(dateFormat.format(date));
				}
			}
			else {
				setText(value.toString());
			}
			
			if (isSelected) {
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else {
				setForeground(table.getForeground());
				setBackground(row % 2 == 0 ? table.getBackground() : alternateRowColor);
			}
			
			return this;
		}
		
	}
	
	static class CurationCellRenderer extends DefaultTableCellRenderer {

		private final TypedSampleMeasurementTableModel typedSampleTableModel;
		private final Font normalFont;
		private final Font strikeThroughFont;
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		CurationCellRenderer(TypedSampleMeasurementTableModel typedSampleTableModel) {
			this.typedSampleTableModel = typedSampleTableModel;
			
			normalFont = getFont();
			Map  attributes = normalFont.getAttributes();
			attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			strikeThroughFont = new Font(attributes);	
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			KdxSample sample = typedSampleTableModel.getSampleAt(row);		

			Font font = normalFont;
			if (sample != null) {
				if (sample.isSuppressed()) {
					font = strikeThroughFont;
				}
			}
			this.setFont(font);
			return this;	
		}
	}
	
	@Override
	public CurationData getCurationData() {
		return curationData;
	}

	@Override
    public void setCurationCellValue(List<CurationCellValue> ccvs) {
        sampleEntryPanel.setCurationCellValue(ccvs);
    }

}
