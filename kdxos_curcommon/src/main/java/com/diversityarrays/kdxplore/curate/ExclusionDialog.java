/**
 * 
 */
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleSource;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.GBH;

/**
 * Provide classes and methods to handle Exclusion reasons etc.
 */
public class ExclusionDialog {

    private ExclusionDialog() { }

    static class ReasonsPanel extends JPanel {

        private String selectedReason;

        private JComboBox<String> combo = new JComboBox<String>(
                ReasonsCache.getInstance().getReasons());

        public ReasonsPanel(Consumer<String> reasonHandler) {
            super(new BorderLayout());

            combo.setEditable(true);
            Component editor = combo.getEditor().getEditorComponent();
            if (editor instanceof JTextField) {
                JTextField textField = ((JTextField) editor);
                textField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        handleUpdate();
                    }
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        handleUpdate();
                    }
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        handleUpdate();
                    }
                    private void handleUpdate() {
                        String s = textField.getText().trim();
                        reasonHandler.accept(s);
                    }
                });
            }
            
            add(new JLabel(Vocab.LABEL_SELECT_REASON_FOR_SUPPRESS()), BorderLayout.NORTH);
            add(combo, BorderLayout.CENTER);

            Object item = combo.getSelectedItem();
            selectedReason = item == null ? null : item.toString().trim();

            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object item = combo.getSelectedItem();
                    selectedReason = item == null ? null : item.toString().trim();
                    reasonHandler.accept(selectedReason);
                }
            });
        }

        public String getSelectedReason() {
            return selectedReason;
        }
    }

    static public class ExcludeReasonDialog extends JDialog {
        
        private final Action excludeAction = new AbstractAction(Vocab.ACTION_SUPPRESS_TRAIT_VALUES()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Check.isEmpty(reason)) {
                    reason = null; // protective coding
                }
                else {
                    ReasonsCache.getInstance().addReason(reason);
                }
                dispose();
            }  
        };

        private final Action cancelAction = new AbstractAction(Vocab.ACTION_SUPPRESS_TRAIT_VALUES()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                reason = null;
                dispose();
            }  
        };

        private final ReasonsPanel reasonsPanel = new ReasonsPanel(new Consumer<String>() {
            @Override
            public void accept(String s) {
                reason = s;
                excludeAction.setEnabled(! Check.isEmpty(reason));
            }
        });
        
        public String reason = null;
        public ExcludeReasonDialog(Window owner) {
            super(owner, Vocab.TITLE_SUPPRESS_REASON(), ModalityType.APPLICATION_MODAL);
            
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            Box buttons = Box.createHorizontalBox();
            buttons.add(Box.createHorizontalGlue());
            buttons.add(new JButton(cancelAction));
            buttons.add(new JButton(excludeAction));
            
            excludeAction.setEnabled(false);

            Container cp = getContentPane();
            cp.add(reasonsPanel, BorderLayout.CENTER);
            cp.add(buttons, BorderLayout.SOUTH);
            
            pack();
            GuiUtil.centreOnOwner(this);
        }
    }

    static public class ExcludeUncuratedSampleReason extends JDialog {
        
        private final Action excludeAction = new AbstractAction(Vocab.ACTION_SUPPRESS_TRAIT_VALUES()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReasonsCache.getInstance().addReason(excludeReason);
                if (askValueForUnscored) {
                    valueForUnscored = missing.isSelected()
                            ? ValueForUnscored.MISSING
                            : ValueForUnscored.NA;
                }
                else {
                    valueForUnscored = ValueForUnscored.DONT_USE;
                }
                dispose();
            }
        };
        
        private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sampleSource = null;
                dispose();
            }
        };

        // Null means user clicked Cancel
        public SampleSource sampleSource = null;
        public ValueForUnscored valueForUnscored = null;
        public String excludeReason = null;
        
        private final JRadioButton missing = new JRadioButton(TraitValue.EXPORT_VALUE_MISSING, true);
        private final JRadioButton na = new JRadioButton(TraitValue.EXPORT_VALUE_NA, ! missing.isSelected());
        private final JLabel chooseDefaultLabel = new JLabel(Msg.LABEL_SET_UNSCORED_TO());

        private final Consumer<String> checker = new Consumer<String>() {
            @Override
            public void accept(String reason) {
                excludeReason = reason;
                updateOkAction();
            }
        };

        private final ReasonsPanel reasonsPanel = new ReasonsPanel(checker);

        private final boolean askValueForUnscored;
        
        public ExcludeUncuratedSampleReason(Window owner, CurationCellEditor cce, 
                boolean askAboutValueForUnscored, 
                List<CurationCellValue> ccvs)
        {
            super(owner, Vocab.TITLE_SUPPRESS_SELECT_SOURCE_FOR_VALUES(), ModalityType.APPLICATION_MODAL);
            
            this.askValueForUnscored = askAboutValueForUnscored;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel main = new JPanel();
            GBH gbh = new GBH(main, 2,2,2,2);
            int y = 0;
            Component c = createSampleSourcePanel(cce.getCurationData(), ccvs);
            if (c != null) {
                gbh.add(0,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, c);
                ++y;
            }
            if (askAboutValueForUnscored) {
                ButtonGroup bg = new ButtonGroup();
                bg.add(missing);
                bg.add(na);

                Box dbox = Box.createHorizontalBox();
                dbox.add(chooseDefaultLabel);
                dbox.add(missing);
                dbox.add(na);

                gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, dbox);
                ++y;
            }
            gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, reasonsPanel);
            ++y;

            
            Box buttons = Box.createHorizontalBox();
            buttons.add(Box.createHorizontalGlue());
            buttons.add(new JButton(cancelAction));
            buttons.add(new JButton(excludeAction));
            
            Container cp = getContentPane();
            cp.add(main, BorderLayout.NORTH);
//            cp.add(main, BorderLayout.CENTER);
            cp.add(buttons, BorderLayout.SOUTH);
            
            excludeReason = reasonsPanel.getSelectedReason();
            updateOkAction();

            pack();
            GuiUtil.centreOnOwner(this);
        }

        private Component createSampleSourcePanel(CurationData curationData, List<CurationCellValue> ccvs) {

            final List<SampleSource> deviceNames = getSampleSources(curationData, ccvs);
            switch (deviceNames.size()) {
            case 0:
                sampleSource = SampleSource.MOST_RECENT;
                return null;
            case 1:
                // No need to ask about deviceNames, there is only one possible
                sampleSource = deviceNames.get(0);
                excludeAction.setEnabled(sampleSource != null);
                return new JLabel(Vocab.LABEL_FROM_SOURCE(sampleSource.toString()));
            }
            
            // There are multiple...
            deviceNames.add(0, SampleSource.MOST_RECENT);
            
            excludeAction.setEnabled(false); // until deviceName chosen
            JComboBox<SampleSource> deviceNamesCombo = createSampleSourcesCombo(
                    createSelectTitleSampleSource(),
                    deviceNames);
            deviceNamesCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = deviceNamesCombo.getSelectedIndex();
                    if (index > 0) {
                        sampleSource = deviceNames.get(index - 1);
                    }
                    else {
                        sampleSource = null;
                    }

                    updateOkAction();
                }
            });
            
            Box result = Box.createHorizontalBox();
            result.add(new JLabel(Vocab.LABEL_SUPPRESS_WITH_VALUES_FROM()));
            result.add(deviceNamesCombo);
            return result;
        }

        private void updateOkAction() {
            excludeAction.setEnabled(sampleSource != null
                    &&
                    ! Check.isEmpty(excludeReason));
        }

//        private JComponent createDeviceNamesPanel(JComboBox<SampleSource> sampleSourceCombo) {
//            JPanel panel = new JPanel();
//            GBH gbh = new GBH(panel);
//            int y = 0;
//
//            gbh.add(0, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, Vocab.LABEL_SUPPRESS_WITH_VALUES_FROM());
//            ++y;
//
//            gbh.add(0, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, sampleSourceCombo);
//            ++y;
//
//            return panel;
//        }
    }


    static public SampleSource createSelectTitleSampleSource() {
        return SampleSource.createDeviceSampleSource(Vocab.LIST_TITLE_SELECT_SAMPLE_SOURCE(), -1,
                SampleSource.IN_THE_FUTURE);
    }
    static private JComboBox<SampleSource> createSampleSourcesCombo(
            SampleSource titleSampleSource,
            List<SampleSource> sampleSources) 
    {
        List<SampleSource> list = new ArrayList<>();
        if (titleSampleSource != null) {
            list.add(titleSampleSource);
        }
        list.addAll(sampleSources);
        final JComboBox<SampleSource> result = new JComboBox<>(
                list.toArray(new SampleSource[list.size()]));
        return result;
    }

    public static List<SampleSource> getSampleSources(CurationData curationData,
            List<CurationCellValue> ccvList) {

        Set<Integer> sampleGroupIds = new HashSet<>();

        for (CurationCellValue ccv : ccvList) {
            for (KdxSample sm : ccv.getRawSamples()) {
                sampleGroupIds.add(sm.getSampleGroupId());
            }
        }

        List<SampleSource> sampleSources = new ArrayList<>();

        for (Integer sampleGroupId : sampleGroupIds) {
            DeviceIdentifier devid = curationData.getDeviceIdentifierForSampleGroup(sampleGroupId);

            Date sampleGroupDate = curationData.getSampleGroupDateLoaded(sampleGroupId);

            SampleSource ss = SampleSource.createDeviceSampleSource(devid.getDeviceName(),
                    sampleGroupId, sampleGroupDate);
            sampleSources.add(ss);
        }

        return sampleSources;
    }
    
    static class ConfirmIncludeUncuratedSamples extends JDialog {
     
        private final JComboBox<SampleSource> deviceNamesCombo;
        
        public ValueForUnscored valueForUnscored = null;
        public SampleSource sampleSource = null;
        
        private final JRadioButton missing = new JRadioButton(TraitValue.EXPORT_VALUE_MISSING, true);
        private final JRadioButton na = new JRadioButton(TraitValue.EXPORT_VALUE_NA);

        private final JLabel chooseDefaultLabel = new JLabel(Msg.LABEL_SET_UNSCORED_TO());

        private final Action okAction = new AbstractAction(Vocab.ACTION_ACCEPT_TRAIT_VALUES()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (askValueForMissing) {
                    valueForUnscored = missing.isSelected()
                            ? ValueForUnscored.MISSING
                            : ValueForUnscored.NA;
                }
                else {
                    valueForUnscored = ValueForUnscored.DONT_USE;
                }
                sampleSource = (SampleSource) deviceNamesCombo.getSelectedItem();
                dispose();
            }
        };

        private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sampleSource = null;
                valueForUnscored = null;
                dispose();
            }
        };

        private SampleSource titleSampleSource = null;
        
        private boolean askValueForMissing;
        
        private ConfirmIncludeUncuratedSamples(Window owner, 
                String title, 
                boolean askAboutValueForMissing,
                List<SampleSource> sampleSources) 
        {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            
            this.askValueForMissing = askAboutValueForMissing;
            
            if (sampleSources.size() > 1) {
                titleSampleSource = createSelectTitleSampleSource();
            }
            
            deviceNamesCombo = createSampleSourcesCombo(titleSampleSource, sampleSources);
            deviceNamesCombo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object item = deviceNamesCombo.getSelectedItem();
                    if (item instanceof SampleSource) {
                        sampleSource = (SampleSource) item;
                    }
                    else {
                        sampleSource = titleSampleSource;
                    }
                    okAction.setEnabled(sampleSource != titleSampleSource);
                }
            });
            okAction.setEnabled(titleSampleSource == null);
            
            JPanel tmp = new JPanel();
            GBH gbh = new GBH(tmp, 2,2,2,2);
            int y = 0;
            gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.WEST, Vocab.LABEL_SET_SAMPLES_FROM());
            gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, deviceNamesCombo);
            ++y;
            if (askValueForMissing) {
                ButtonGroup bg = new ButtonGroup();
                bg.add(missing);
                bg.add(na);

                Box box = Box.createHorizontalBox();
                box.add(chooseDefaultLabel);
                box.add(missing);
                box.add(na);
                
                gbh.add(0, y, 2, 1, GBH.HORZ, 1, 1, GBH.CENTER, box);
                ++y;
            }

            Box buttons = Box.createHorizontalBox();
            buttons.add(Box.createHorizontalGlue());
            buttons.add(new JButton(cancelAction));
            buttons.add(new JButton(okAction));

            Container cp = getContentPane();
            
            cp.add(tmp, BorderLayout.NORTH);
            cp.add(buttons, BorderLayout.SOUTH);
            
            pack();
            GuiUtil.centreOnOwner(this);
        }
    }

    static public Pair<SampleSource,ValueForUnscored> confirmIncludeUnCuratedSamples(
            Window parent, 
            CurationCellEditor cce,
            boolean askAboutValueForUnscored,
            List<CurationCellValue> ccvs) {

        String title = Vocab.TITLE_ACCEPT_SELECT_SOURCE_FOR_VALUES();

        List<SampleSource> sampleSources = getSampleSources(cce.getCurationData(), ccvs);
        switch (sampleSources.size()) {
        case 0:
            break;
        case 1:
            SampleSource source = sampleSources.get(0);
            title = Vocab.TITLE_SET_UNCURATED_SAMPLES_FROM_SOURCE(source.deviceName);
            break;
        default:
            sampleSources.add(0, SampleSource.MOST_RECENT);
            break;
        }
        Collections.sort(sampleSources);
        
        ConfirmIncludeUncuratedSamples dlg = new ConfirmIncludeUncuratedSamples(
                parent, title, askAboutValueForUnscored, sampleSources);
        dlg.setVisible(true);
        
        if (dlg.sampleSource == null) {
            return null;
        }
        return new Pair<>(dlg.sampleSource, dlg.valueForUnscored);
    }

    protected JOptionPane getOptionPane(JComponent parent) {
        JOptionPane pane = null;
        if (!(parent instanceof JOptionPane)) {
            pane = getOptionPane((JComponent) parent.getParent());
        }
        else {
            pane = (JOptionPane) parent;
        }
        return pane;
    }

}
