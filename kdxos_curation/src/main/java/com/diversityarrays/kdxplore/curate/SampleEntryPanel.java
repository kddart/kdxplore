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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.data.dal.SampleType;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleSource;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.stats.StatsUtil;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.GBH;

/**
 * <pre>
 * 
 * +---------------------------------------------------+ 
 * |  .----------------------------------------------. |
 * |  |  stats controls                              | |
 * |  '----------------------------------------------' |
 * |                                                   |
 * |  Sample Type: [ -- choose -- ]                    |
 * |                                                   |
 * |  {value description here}                         |
 * |  (STATS) | [ Enter value or select from above ]   |
 * |                                                   |
 * |  (Delete)  ( NA )   ( Missing )   ( Set Value )   |
 * |  {   validation message                         } |
 * |                                                   |
 * |                                                   |
 * |  .----------------------------------------------. |
 * |  |  Single-  OR  Multi-cell Controls Panel      | |
 * |  |                                              | |
 * |  |                                              | |
 * |  |                                              | |
 * |  .----------------------------------------------. |
 * +---------------------------------------------------+ 
 * </pre>
 * @author brianp
 *
 */
class SampleEntryPanel extends JPanel {
    
    private static final String CARD_MULTI = "cardMulti"; //$NON-NLS-1$
    private static final String CARD_SINGLE = "cardSingle"; //$NON-NLS-1$

    
    public static final String NOT_SUPPRESSED = null;

    private final String FROM_DEVICE = Vocab.LABEL_SOURCE_FROM_DEVICE() + " "; //$NON-NLS-1$
    private final String EDITED_VALUE = Vocab.LABEL_SOURCE_CURATED_VALUE() + " "; //$NON-NLS-1$
    private final String FROM_DATABASE = Vocab.LABEL_SOURCE_FROM_DATABASE() + " "; //$NON-NLS-1$
    
    // This is used to indicate that we have tried to establish the validation rule for the Trait
    // but there is none.
    // The instance is stored in validationRuleByTraitId.
    static private final ValidationRule NO_VALIDATION_RULE = ValidationRule.NO_VALIDATION_RULE;

    static private final SampleType NO_SAMPLE_TYPE = new SampleType(Vocab.OPTION_NO_SAMPLE_TYPE());

    // cache for validation rules
    private final Map<Integer,ValidationRule> validationRuleByTraitId = new HashMap<>();

    private KdxSample selectedSourceSample;

    private Map<Integer, SampleType> sampleTypeById = new HashMap<>();
    private JComboBox<SampleType> sampleTypeCombo;
    
    private PromptTextField sampleValueTextField = new PromptTextField(Vocab.PROMPT_ENTER_VALUE_OR_SELECT());
    
    private JLabel valueDescription = new JLabel();
    
    private JLabel validationMessage = new JLabel();

    private ActionListener enterKeyListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (setValueAction.isEnabled()) {
                String newValue = sampleValueTextField.getText();
                setTraitValue(newValue, selectedSourceSample);
            }
        }       
    };

    private ApplyToPanel applyToPanel = new ApplyToPanel();

//    private final Consumer<SampleSource> acceptMultipleSampleValues = new Consumer<SampleSource>() {
//        @Override
//        public void accept(SampleSource source) {
//            setMultipleValuesToAccepted(source, false);
//        }
//    };  
//    private final MultipleCellControlsPanel multiCellControlsPanel = new MultipleCellControlsPanel(); // acceptMultipleSampleValues);
    
    private final CardLayout singleOrMultiCardLayout = new CardLayout();
    private final JPanel singleOrMultiCardPanel = new JPanel(singleOrMultiCardLayout);
    private boolean showingMultiCell;
    
    private Action setValueAction = new AbstractAction(Vocab.ACTION_SET_VALUE()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (showingMultiCell) {
                SampleSource source = getSelectedSampleSource();
                if (source==null) {
                    // protective coding in case we didn't disable the button
                    MsgBox.warn(SampleEntryPanel.this, "Please select a Source for Samples", Vocab.ACTION_SET_VALUE());
                }
                else {
                    setMultipleValuesToAccepted(source, false);
                }
            }
            else {
//                setMultipleValuesToAccepted(source, true);

                String newValue = sampleValueTextField.getText();
                setTraitValue(newValue, selectedSourceSample);
            }
        }
    };
    
//    private final JButton setButton = new JButton(setValueAction);

    private Action missingAction = new AbstractAction(Vocab.ACTION_SET_MISSING()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            setTraitValue(TraitValue.VALUE_MISSING, null);
        }
    };
    private Action notApplicableAction = new AbstractAction(Vocab.ACTION_SET_NA()) {
        @Override
        public void actionPerformed(ActionEvent e) {        
            setTraitValue(TraitValue.VALUE_NA, null);
        }
    };
    private Action deleteAction = new AbstractAction(Vocab.ACTION_SET_UNSET()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            setTraitValue(TraitValue.VALUE_UNSET, null);
            deleteAction.setEnabled(false);
        }
    };
    
    private Action showStatsAction = new AbstractAction(Vocab.ACTION_STATS()) {
        @Override
        public void actionPerformed(ActionEvent e) {                        
            updateStatsControls(showStatsOption.isSelected());
        }       
    };
    private JToggleButton showStatsOption = new JToggleButton(showStatsAction);
    
    private final JPanel statisticsControls;
    private Map<StatType,JButton> statButtonByStatType = new HashMap<>();
    private Map<StatType,Component> statComponentByStatType = new HashMap<>();

    private Map<JComponent,StatType> statTypeByButton = new LinkedHashMap<>();
    
    private boolean repopulatingStatsControls = false;
    private ActionListener statButtonActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (repopulatingStatsControls) {
                return;
            }
            
            StatType statType = statTypeByButton.get(e.getSource());
            Object value = statType.getStatValue(kdsmartSampleStatistics);
            
            if (statType == StatType.MODE) {
                @SuppressWarnings("unchecked")
                JComboBox<String> modebox = (JComboBox<String>) statComponentByStatType.get(StatType.MODE);
                value = modebox.getSelectedItem();
            }
            
            if (value != null) {
                updateValueLabel(String.valueOf(value), statType.name());
            }       
        }
    };

    private final JComboBox<SampleSource> sampleSourceComboBox = new JComboBox<SampleSource>();

    private final int initialTableRowHeight;

    private boolean everSetData = false;
    
    private final IntFunction<Trait> traitProvider;
    private final Closure<Void> refreshFieldLayoutView;
    private final BiConsumer<Comparable<?>,List<CurationCellValue>> showChangedValue;

    private final TypedSampleMeasurementTableModel typedSampleTableModel;
    private final JTable typedSampleTable;
    private final TsmCellRenderer tsmCellRenderer;
    private final JToggleButton showPpiOption;

    private final CurationData curationData;
    private final Box sampleSourceControls;

    private Trait traitBeingEdited; 

    private SimpleStatistics<?> kdsmartSampleStatistics;

    private List<CurationCellValue> curationCellValues;

    SampleEntryPanel(CurationData cd, 
            IntFunction<Trait> traitProvider,
            TypedSampleMeasurementTableModel tsm, 
            JTable table, 
            TsmCellRenderer tsmCellRenderer,
            JToggleButton showPpiOption, 
            Closure<Void> refreshFieldLayoutView,
            BiConsumer<Comparable<?>,List<CurationCellValue>> showChangedValue,
            SampleType[] sampleTypes) 
    {
        this.curationData = cd;
        this.traitProvider = traitProvider;
        this.typedSampleTableModel = tsm;
        this.typedSampleTable = table;
        
        this.showPpiOption = showPpiOption;
        
        this.initialTableRowHeight = typedSampleTable.getRowHeight();
        this.tsmCellRenderer = tsmCellRenderer;
        this.refreshFieldLayoutView = refreshFieldLayoutView;
        this.showChangedValue = showChangedValue;
        
        List<SampleType> list = new ArrayList<>();
        list.add(NO_SAMPLE_TYPE);
        for (SampleType st : sampleTypes) {
            list.add(st);
            sampleTypeById.put(st.getTypeId(), st);
        }

        sampleTypeCombo = new JComboBox<SampleType>(list.toArray(new SampleType[list.size()]));
        
        typedSampleTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (TableModelEvent.HEADER_ROW == e.getFirstRow()) {
                    typedSampleTable.setAutoCreateColumnsFromModel(true);
                    everSetData = false;
                }
            }
        });
        

        showStatsAction.putValue(Action.SHORT_DESCRIPTION, 
                Vocab.TOOLTIP_STATS_FOR_KDSMART_SAMPLES());
        showStatsOption.setFont(showStatsOption.getFont().deriveFont(Font.BOLD));
        showStatsOption.setPreferredSize(new Dimension(30, 30));

        JLabel helpPanel = new JLabel();
        helpPanel.setHorizontalAlignment(JLabel.CENTER);
        String html = "<HTML>Either enter a value or select<br>a <i>Source</i> for <b>Value From:</b>";
        if (shouldShowSampleType(sampleTypes)) {
            html += "<BR>You may also select a <i>Sample Type</i> if it is relevant.";
        }
        helpPanel.setText(html);
        
        singleOrMultiCardPanel.add(helpPanel, CARD_SINGLE);
        singleOrMultiCardPanel.add(applyToPanel, CARD_MULTI);
//        singleOrMultiCardPanel.add(multiCellControlsPanel, CARD_MULTI);
        
        validationMessage.setBorder(new LineBorder(Color.LIGHT_GRAY));
        validationMessage.setForeground(Color.RED);
        validationMessage.setBackground(new JLabel().getBackground());
        validationMessage.setHorizontalAlignment(SwingConstants.CENTER);
//      validationMessage.setEditable(false);
        Box setButtons = Box.createHorizontalBox();
        setButtons.add(new JButton(deleteAction));
        setButtons.add(new JButton(notApplicableAction));
        setButtons.add(new JButton(missingAction));
        setButtons.add(new JButton(setValueAction));
        
        deleteAction.putValue(Action.SHORT_DESCRIPTION, Vocab.TOOLTIP_SET_UNSET());
        notApplicableAction.putValue(Action.SHORT_DESCRIPTION, Vocab.TOOLTIP_SET_NA());
        missingAction.putValue(Action.SHORT_DESCRIPTION, Vocab.TOOLTIP_SET_MISSING());
        setValueAction.putValue(Action.SHORT_DESCRIPTION, Vocab.TOOLTIP_SET_VALUE());

        Box sampleType = Box.createHorizontalBox();
        sampleType.add(new JLabel(Vocab.LABEL_SAMPLE_TYPE()));
        sampleType.add(sampleTypeCombo);
            
        statisticsControls = generateStatControls();
                
        
        setBorder(new TitledBorder(new LineBorder(Color.GREEN.darker().darker()), "Sample Entry Panel"));
        GBH gbh = new GBH(this);
        int y = 0;

        gbh.add(0,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, statisticsControls);
        ++y;

        if (shouldShowSampleType(sampleTypes)) {
            sampleType.setBorder(new LineBorder(Color.RED));
            sampleType.setToolTipText("DEVELOPER MODE: sampleType is possible hack for accept/suppress");
            gbh.add(0,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, sampleType);
            ++y;
        }
        
        sampleSourceControls = Box.createHorizontalBox();
        sampleSourceControls.add(new JLabel(Vocab.PROMPT_VALUES_FROM()));
//        sampleSourceControls.add(new JSeparator(JSeparator.VERTICAL));
        sampleSourceControls.add(sampleSourceComboBox);
        sampleSourceControls.add(Box.createHorizontalGlue());
        sampleSourceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSetValueAction();
            }
        });
        
        gbh.add(0,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, sampleSourceControls);
        ++y;

        gbh.add(0,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, valueDescription);
        ++y;

        gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.WEST, showStatsOption);
        gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, sampleValueTextField);
        ++y;

        gbh.add(0,y, 2,1, GBH.NONE, 1,1, GBH.CENTER, setButtons);
        ++y;
        
        gbh.add(0,y, 2,1, GBH.HORZ, 2,1, GBH.CENTER, validationMessage);
        ++y;

        gbh.add(0,y, 2,1, GBH.HORZ, 2,0, GBH.CENTER, singleOrMultiCardPanel);
        ++y;
        
        deleteAction.setEnabled(false);
        sampleSourceControls.setVisible(false);

        sampleValueTextField.setGrayWhenDisabled(true);
        sampleValueTextField.addActionListener(enterKeyListener);
        
        sampleValueTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSetValueAction();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSetValueAction();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSetValueAction();
            }
        });

        setValueAction.setEnabled(false);
    }
    
    // Currently, hide this unless in developer mode.
    private boolean shouldShowSampleType(SampleType[] sampleTypes) {
        return RunMode.getRunMode().isDeveloper() && sampleTypes.length > 0;
    }


    private SampleSource getSelectedSampleSource() {
        SampleSource source = (SampleSource) sampleSourceComboBox.getSelectedItem();
        if (SampleSource.ENTER_VALUE_OR_SELECT_SOURCE == source) {
            source = null;
        }
        return source;
    }
    
    private void updateSetValueAction() {

        boolean enableSet;
        
        SampleSource source = null;
        if (sampleSourceControls.isVisible()) {
            source = getSelectedSampleSource();
        }

        if (source != null) {
            enableSet = true;
            valueDescription.setText(null);

//            sampleValueTextField.setText("");
            sampleValueTextField.setEnabled(false);
        }
        else {
            // Either "-- Please Select --" or nothing selected

            sampleValueTextField.setEnabled(true);

            String sampleValue = sampleValueTextField.getText().trim();

            boolean statInField = false;
            StatType typeInField = null;
            if (kdsmartSampleStatistics != null) {
                for (StatType statType : statTypeByButton.values()) {
                    String value = String.valueOf(statType.getStatValue(kdsmartSampleStatistics));
                    if (sampleValue.equals(value)) {
                        statInField =  true;
                        typeInField = statType;
                        break;
                    }
                }
            }

            String validMsg = null;
            String valueDesc = sampleValue.isEmpty() ? null : EDITED_VALUE;
            if (sampleValue.isEmpty()) {
                enableSet = false;
            }
            else if (statInField) {
                valueDesc = typeInField.name();
                enableSet = true;
            }
            else if (isMissingOrNA(sampleValue)) {
                enableSet = true;
            }
            else {
                ValidationRule valRule = getValidationRule();
                if (NO_VALIDATION_RULE == valRule) {
                    enableSet = true;
                }
                else if (valRule.evaluate(sampleValue)) {
                    enableSet = true;
                }
                else {
                    validMsg = valRule.getDescription();
                    enableSet = false;
                }
            }

            valueDescription.setText(valueDesc);

            if (Check.isEmpty(validMsg)) {
                validationMessage.setText("--");
                validationMessage.setForeground(Color.GRAY);
//                validationMessage.setForeground(validationMessage.getBackground());
            }
            else {
                validationMessage.setText(validMsg);
                validationMessage.setForeground(Color.RED);
            }

            showChangedValue.accept(Check.isEmpty(sampleValue) ? null : sampleValue, curationCellValues);
        }

        setValueAction.setEnabled(enableSet);
    }


    
    public void updateValueLabel(String value, String from) {

        selectedSourceSample = null;
        if (value == null && from == null) {
            int[] vrows = typedSampleTable.getSelectedRows();
            
            int mrow = -1;
            if (vrows.length == 1) {
                mrow = typedSampleTable.convertRowIndexToModel(vrows[0]);
            }
            
            if (mrow >= 0) {
                selectedSourceSample = typedSampleTableModel.getSampleAt(mrow);
                DeviceIdentifier did = typedSampleTableModel.getDeviceIdentifierAt(mrow);

                String displayValue = typedSampleTableModel.getSampleDisplayValueAt(mrow);

                sampleValueTextField.setText(displayValue);

                switch (did.getDeviceType()) {
                case DATABASE:
                    valueDescription.setText(FROM_DATABASE);
                    deleteAction.setEnabled(false);
                    break;
                case EDITED:
                    valueDescription.setText(EDITED_VALUE);
                    deleteAction.setEnabled(true);
                    break;

                case KDSMART:
                    valueDescription.setText(FROM_DEVICE + did.getDeviceName() +" : "); //$NON-NLS-1$
                    deleteAction.setEnabled(false);
                    break;
                    
                case FOR_SCORING:
                    throw new RuntimeException("Internal Error: Invalid for editing " + did.getDeviceType()); //$NON-NLS-1$

                default:
                    throw new RuntimeException("Internal Error: unsupported DeviceType " + did.getDeviceType()); //$NON-NLS-1$
                }
            }
            else {
                deleteAction.setEnabled(false);
                sampleValueTextField.setText(""); //$NON-NLS-1$
                if (vrows.length > 1) {
                    valueDescription.setText("Select a single Sample Source");
                }
                else {
                    valueDescription.setText(""); //$NON-NLS-1$
                }
            }
        }
        else {
            valueDescription.setText(from);
            sampleValueTextField.setText(value);
        }
    }

    public SampleType getSelectedSampleType() {
        SampleType result = (SampleType) sampleTypeCombo.getSelectedItem();
        if (NO_SAMPLE_TYPE == result) {
            result = null;
        }
        return result;
    }
    
    private boolean initialisingSampleSourceComboBox;
    private void initSampleSourceComboBox(Map<Integer, SampleSource> sampleSourceBySampleGroupId) {
        initialisingSampleSourceComboBox = true;
        try {
            sampleSourceComboBox.removeAllItems();
    
            if (Check.isEmpty(sampleSourceBySampleGroupId)) {
                sampleSourceControls.setVisible(false);
            }
            else {
                // If more than one KDSmart sampleGroup then add "Most recent" as an option
                
                sampleSourceComboBox.addItem(SampleSource.ENTER_VALUE_OR_SELECT_SOURCE);
                if (sampleSourceBySampleGroupId.size() != 1) {
                    sampleSourceComboBox.addItem(SampleSource.MOST_RECENT);
                }
    
                List<SampleSource> list = new ArrayList<>(sampleSourceBySampleGroupId.values());
                Collections.sort(list);
                for (SampleSource ss : list) {
                    sampleSourceComboBox.addItem(ss);
                }
                sampleSourceControls.setVisible(true);
            }
        }
        finally {
            initialisingSampleSourceComboBox = false;
        }
    }

    public void setCurationCellValue(List<CurationCellValue> ccvList) {

        initSampleSourceComboBox(null);

        this.curationCellValues = ccvList;
        
        showingMultiCell = false;
        
        Map<Integer, SampleSource> kdsmartSampleSourceBySampleGroupId = new HashMap<>();
        
        sampleValueTextField.setText(""); //$NON-NLS-1$
        valueDescription.setText(""); //$NON-NLS-1$
        
        Map<TableColumn, Integer> widthByColumn = null; // getTableColumnWidths();
        
        if (curationCellValues==null) {
            typedSampleTableModel.clear();
        }
        else {
            
            showingMultiCell = curationCellValues.size() > 1;
            
            List<TypedSampleMeasurement> typedSampleMeasurements = new ArrayList<>();
            List<KdxSample> kdsmartSamples = new ArrayList<>();
            
//          int nKdsmartMeasurements = 0;
            for (CurationCellValue ccvd : curationCellValues) {

                KdxSample ed = ccvd.getEditedSample();
                Date edWhen = ed==null ? null : ed.getMeasureDateTime();

                // NOTE: we DO want all of the KDSmart device samples.
                //       so DON'T just use the first sample
                for (KdxSample sm : ccvd.getRawSamples()) {
                    DeviceIdentifier did = curationData.getDeviceIdentifierForSampleGroup(sm.getSampleGroupId());
                    Date sampleGroupDate = curationData.getSampleGroupDateLoaded(sm.getSampleGroupId());
                    
                    boolean moreRecent = false;
                    if (edWhen != null
                        && sm.hasBeenScored() 
                        && sm.getMeasureDateTime().after(edWhen))
                    {
                        moreRecent = true;
                    }
                    TypedSampleMeasurement tsm = makeTypedSampleMeasurement(did, sm, sampleGroupDate, moreRecent);
                    typedSampleMeasurements.add(tsm);
                    
                    Integer sampleGroupId = sm.getSampleGroupId();
                    if (! kdsmartSampleSourceBySampleGroupId.containsKey(sampleGroupId)) {
                        kdsmartSampleSourceBySampleGroupId.put(sampleGroupId,
                                SampleSource.createDeviceSampleSource(did.getDeviceName(),
                                        sampleGroupId, sampleGroupDate));
                    }
                }
//              nKdsmartMeasurements = typedSampleMeasurements.size();

                kdsmartSamples.addAll(ccvd.getRawSamples());
    
                KdxSample db = ccvd.getDatabaseSample();
                if (db != null && db.hasBeenScored()) {
                    DeviceIdentifier did = curationData.getDeviceIdentifierForSampleGroup(db.getSampleGroupId());
                    Date sampleGroupDate = curationData.getSampleGroupDateLoaded(db.getSampleGroupId());
                    typedSampleMeasurements.add(makeTypedSampleMeasurement(did, db, sampleGroupDate, false));
                }
                
                
                if (ed != null) {
                    DeviceIdentifier did = curationData.getDeviceIdentifierForSampleGroup(ed.getSampleGroupId());
                    Date sampleGroupDate = curationData.getSampleGroupDateLoaded(ed.getSampleGroupId());
                    typedSampleMeasurements.add(makeTypedSampleMeasurement(did, ed, sampleGroupDate, false));
                }               
            }

            kdsmartSampleStatistics = null;
            traitBeingEdited = null;
            if (kdsmartSamples != null && ! kdsmartSamples.isEmpty()) {
                
                KdxSample sample = kdsmartSamples.get(0);
                traitBeingEdited = traitProvider.apply(sample.getTraitId());

                if (traitBeingEdited.getTraitDataType().isNumeric() && kdsmartSamples.size() >= 2) {
                    String statsName = curationData.getTrial().getTraitNameStyle()
                            .makeTraitInstanceName(traitBeingEdited.getAliasOrName(), sample.getTraitInstanceNumber());
                    kdsmartSampleStatistics = 
                        StatsUtil.createStatistics(statsName,
                                curationData.getNumberOfStdDevForOutlier(), 
                                curationData.getTrial().getTrialPlantingDate(), 
                                traitBeingEdited, 
                                kdsmartSamples);
                }
            }

            boolean wantPPI = showPpiOption.isSelected();
            
            // Work out how we can make the name unique
            DateFormat rendererDateFormat;
        
            
            TsmDateFormatSelector formatSelector = new TsmDateFormatSelector();
            rendererDateFormat = formatSelector.getFormatForUniqueness(typedSampleMeasurements);
            
//          rendererDateFormat = TypedSampleMeasurement.getDateFormatForUniqueIdent(typedSampleMeasurements, shortDateFormat, longDateFormat);
            
            tsmCellRenderer.setDateFormat(rendererDateFormat);

            int rowHeight = initialTableRowHeight;
            if (rendererDateFormat == TsmDateFormatSelector.SHORT_DATE_FORMAT) {
                rowHeight = rowHeight * 2;
            }
            else if (rendererDateFormat == TsmDateFormatSelector.LONG_DATE_FORMAT) {
                rowHeight = rowHeight * 3;
            }
            typedSampleTable.setRowHeight(rowHeight);

            typedSampleTableModel.setData(typedSampleMeasurements, showingMultiCell, wantPPI);
            
            if (! everSetData) {
                typedSampleTable.setAutoCreateColumnsFromModel(false);
                everSetData = true;
            }
        }
        restoreColumnWidths(widthByColumn); // TODO remove this - no longer required

        applyToPanel.getApplyTo().applyToCheckboxes();
//        multiCellControlsPanel.applyToCheckboxes();

        initSampleSourceComboBox(kdsmartSampleSourceBySampleGroupId);
        
        singleOrMultiCardLayout.show(singleOrMultiCardPanel, showingMultiCell ? CARD_MULTI : CARD_SINGLE);

        if (showingMultiCell) {
            setValueAction.putValue(Action.SHORT_DESCRIPTION, Vocab.TOOLTIP_SET_ALL_VALUES_FOR_SELECTED());
        }
        else {
            setValueAction.putValue(Action.SHORT_DESCRIPTION, Vocab.ACTION_SET_VALUE());
        }
        
        if (kdsmartSampleStatistics == null) {
            showStatsOption.setVisible(false);
            showStatsOption.setEnabled(false);
        }
        else {
            showStatsOption.setVisible(true);
            showStatsOption.setEnabled(true);
        }
        
        boolean showStats = showingMultiCell && showStatsOption.isSelected();

        updateStatsControls(showStats);

        sampleValueTextField.requestFocusInWindow();
    }

    private void setTraitValue(String newValueIn, KdxSample sourceSample) {
        
        java.util.Date when = new java.util.Date();
        
        String newValue = newValueIn;

        SampleType sampleType = getSelectedSampleType();

        List<EditedSampleInfo> infoList = new ArrayList<>();

        for (CurationCellValue ccv : curationCellValues) {

            EditedSampleInfo info = null;
            
            KdxSample editedSample = ccv.getEditedSample();
            
            TraitValueType traitValueType = TraitValue.classify(newValue);
            switch (traitValueType) {
            case UNSET:
                info = new EditedSampleInfo(ccv.getCurationCellId(), editedSample, null);
                break;

            case SET:
                ValidationRule valRule = getValidationRule();

                if (NO_VALIDATION_RULE != valRule) {
                    if (! valRule.evaluate(newValue)) {
                        MsgBox.warn(SampleEntryPanel.this, 
                                Vocab.ERRMSG_INVALID_VALUE_VALIDATION(newValue, valRule.getExpression()),
                                Vocab.ACTION_SET_VALUE());
                        return;
                    }
                }

                // TAG_ELAPSED_DAYS_AS_INTEGER
//              if (TraitDataType.ELAPSED_DAYS == traitBeingEdited.getTraitDataType()
//                      &&
//                  newValue.matches("^[0-9]+$")) 
//              {
//                  int nDays = Integer.valueOf(newValue);
//                  Date date = TraitValue.nDaysAsElapsedDaysDate(trialPlantingDate, nDays);
//                  newValue = TraitValue.getTraitValueDateFormat().format(date);
//              }
                
                if (sourceSample != null && sourceSample.getMeasureDateTime() != null) {
                    when = sourceSample.getMeasureDateTime();
                }
                // Drop through to common code...
                
            case MISSING:
            case NA:
                KdxSample newSample = curationData.createEditedSampleMeasurement(ccv, 
                        newValue,
                        when,
                        sampleType, NOT_SUPPRESSED);
                
                ccv.setEditedSample(newSample);
                info = new EditedSampleInfo(ccv.getCurationCellId(), editedSample, newSample);
                
                break;
            default:
                break;
            }
            
            if (info != null) {
                infoList.add(info);
            }
        }
        
        curationData.registerChangedSamples(infoList);

        refreshFieldLayoutView.execute(null);
        this.repaint(); 
    }

    private void setMultipleValuesToAccepted(SampleSource source, boolean singleValue) {

        List<EditedSampleInfo> infoList = new ArrayList<>();
        
        refreshFromCheckboxes();
//        multiCellControlsPanel.refreshFromCheckboxes();

        SampleType sampleType = getSelectedSampleType();

        final java.util.Date now = new java.util.Date();
        
        ApplyTo applyTo = applyToPanel.getApplyTo();
//        ApplyTo applyTo = multiCellControlsPanel.getApplyTo();

        for (CurationCellValue ccv : curationCellValues) {
            
            KdxSample curatedSample = ccv.getEditedSample();
            
            if (!singleValue) {
                boolean apply = false;
                if (curatedSample==null) {
                    // No edited sample... we want to create one ... maybe
                    apply = applyTo.uncurated;
                }
                else if (applyTo.curated) {
                    // Else we already have one - are we supposed to include it?
                    boolean excluded = curatedSample.isSuppressed();

                    if (applyTo.excluded && excluded) {
                        apply = true;
                    }
                    else if (applyTo.included && ! excluded) {
                        apply = true;
                    }
                }

                if (! apply) {
                    continue;
                }
            }
            
            KdxSample deviceIdSample = null;
            if (SampleSource.MOST_RECENT.equals(source)) {
                deviceIdSample = ccv.getLatestRawSample();
            }
            else {
                if (source != null && DeviceType.KDSMART==source.deviceType) {
                    deviceIdSample = findSampleForDevice(ccv, source.sampleGroupId);
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
                    NOT_SUPPRESSED);

            ccv.setEditedSample(newEditedSample);

            infoList.add(new EditedSampleInfo(
                    ccv.getCurationCellId(),
                    curatedSample, newEditedSample));
        } // each ccv

        if (infoList.isEmpty()) {
            String html = applyTo.getHtmlDescription_2();

            MsgBox.info(SampleEntryPanel.this, html, Vocab.ACTION_SET_VALUE());
            return;
        }
        else {
            curationData.registerChangeablesForNewEditedSamples(infoList);
        }

        refreshFieldLayoutView.execute(null);
        this.repaint();
    }

    private TypedSampleMeasurement makeTypedSampleMeasurement(
            DeviceIdentifier did,
            KdxSample sample, 
            Date sampleGroupDate,
            boolean moreRecent)
    {
        TraitValue traitValue = null;
        TraitInstanceValueRetriever<?> tivr = curationData.getTraitInstanceValueRetriever(sample);
        if (tivr != null) {
            traitValue = tivr.createTraitValue(sample, null);
        }
        return new TypedSampleMeasurement(did, sample, sampleTypeById, traitValue, sampleGroupDate, moreRecent);
    }

    /**
     * 
     * @param ccv
     * @param sampleGroupId
     * @return a KdxSample (may be null)
     */
    public KdxSample findSampleForDevice(CurationCellValue ccv, int sampleGroupId) {
        // Return the first sample that matches the sampleGroupId
        for (KdxSample sm : ccv.getRawSamples()) {
            if (sampleGroupId == sm.getSampleGroupId()) {
                return sm;
            }
        }
        return null;
    }

    private void restoreColumnWidths(Map<TableColumn, Integer> widthByColumn) {
        if (widthByColumn==null) {
            return;
        }
        Enumeration<TableColumn> e = typedSampleTable.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            TableColumn tc = e.nextElement();
            Integer width = widthByColumn.get(tc);
            if (width != null) {
                tc.setPreferredWidth(width);
            }
        }
    }
    
    /**
     * @param show
     */
    private void updateStatsControls(boolean showStats) {

        if (! SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateStatsControls(showStats);
                }
            });
            return;
        }

        if (showStats) {
            repopulatingStatsControls = true;
            try {
                doRepopulateStatsControls();
            }
            finally {
                repopulatingStatsControls = false;
            }
            statisticsControls.setVisible(true);
        }
        else {
            statisticsControls.setVisible(false);

            for (JButton btn : statButtonByStatType.values()) {
                btn.setEnabled(false);
            }
            for (Component component : statComponentByStatType.values()) {
                component.setEnabled(false);
            }
        }
        this.repaint();
        this.updateUI();
    }
    
    // NOTE: repopulatingStatsControls is true during this method
    private void doRepopulateStatsControls() {
        for (StatType statType : StatType.values()) {
            Object statValue = statType.getStatValue(kdsmartSampleStatistics);

            statButtonByStatType.get(statType).setEnabled(statValue != null);

            Component component = statComponentByStatType.get(statType);
            if (statType != StatType.MODE) {
                component.setEnabled(statValue != null);
                ((JButton) component).setText(statValue == null ? "" : statValue.toString()); //$NON-NLS-1$
            }
            else {
                @SuppressWarnings("unchecked")
                JComboBox<String> comboBox = (JComboBox<String>) component;

                comboBox.removeAllItems();
                String[] modesArray= (String.valueOf(statValue)).split(",", -1); //$NON-NLS-1$
                List<String> modes = Arrays.asList(modesArray);
                if (modes.size() > 1 ) {
                    comboBox.setEnabled(true);
                    for (String s : modes) {
                        if (s.trim() != null) {
                            comboBox.addItem(s.trim());
                        }
                    }
                }
                else {
                    comboBox.setEnabled(true);
                    comboBox.addItem(String.valueOf(statValue));
                }
            }
        }
    }

    private JPanel generateStatControls() {

        JPanel statsPanel = new JPanel();
        GBH gbh = new GBH(statsPanel);
        
        int x = 0;
        
        for (StatType statType : StatType.values()) {
            JButton button = new JButton(statType.label);
            button.addActionListener(statButtonActionListener);

            statButtonByStatType.put(statType, button);

            JComponent valueLabel = null;
            if (statType == StatType.MODE) {
                JComboBox<String> combo = new JComboBox<String>();
                combo.addActionListener(statButtonActionListener);

                valueLabel = combo;
                valueLabel.setMaximumSize(new Dimension(100,25));
            }
            else {
                JButton btn = new JButton();
                btn.addActionListener(statButtonActionListener);

                valueLabel = btn;
            }
            statComponentByStatType.put(statType, valueLabel);
            statTypeByButton.put(valueLabel, statType);

            Font myFont = new Font(Font.SANS_SERIF, Font.BOLD, 10);
            TitledBorder roundedTitledBorder = new TitledBorder(BorderFactory.createRaisedBevelBorder(), statType.name(), 1, 2, myFont);
            
            valueLabel.setBorder(roundedTitledBorder);
            
            gbh.add(x,0, 1,1, GBH.BOTH, 2,3, GBH.CENTER, valueLabel);
            x++;
        }
        return statsPanel;
    }
    /**
     * Note: ELAPSED_DAYS should return NO_VALIDATION_RULE or 
     * @return
     */
    private ValidationRule getValidationRule() {
        int traitId = curationCellValues.get(0).getTraitId();
        ValidationRule valRule = validationRuleByTraitId.get(traitId);
        if (valRule == null) {
            valRule = NO_VALIDATION_RULE;
            Trait trait = traitProvider.apply(traitId);
            String tvr = trait.getTraitValRule();
            if (Check.isEmpty(tvr) && TraitDataType.ELAPSED_DAYS==trait.getTraitDataType()) {
                tvr = ValidationRule.CHOICE_ELAPSED_DAYS_NO_LIMIT;
            }
            if (! Check.isEmpty(tvr)) {
                try {
                    valRule = ValidationRule.create(tvr);
                } catch (InvalidRuleException e) {
                    e.printStackTrace();
                }
            }
            validationRuleByTraitId.put(traitId, valRule);
        }   
        
        return valRule;
    }


    private boolean isMissingOrNA(String s) {
        if (TraitValue.EXPORT_VALUE_MISSING.equals(s)) {
            return true;
        }
        if (TraitValue.EXPORT_VALUE_NA.equals(s)) {
            return true;
        }
        return false;
    }

    public String getApplyToHtmlDescription() {
        return applyToPanel.getApplyTo().getHtmlDescription_2();
//        return multiCellControlsPanel.getApplyToHtmlDescription();
    }

    public void refreshFromCheckboxes() {
        applyToPanel.getApplyTo().refreshFromCheckboxes();
//        multiCellControlsPanel.refreshFromCheckboxes();
    }
}
