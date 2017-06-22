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
package com.diversityarrays.kdcompute.designer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdcompute.db.DataSet;
import com.diversityarrays.kdcompute.db.DataSetBinding;
import com.diversityarrays.kdcompute.db.Knob;
import com.diversityarrays.kdcompute.db.KnobBinding;
import com.diversityarrays.kdcompute.db.KnobDataType;
import com.diversityarrays.kdcompute.db.Plugin;
import com.diversityarrays.kdcompute.db.RunBinding;
import com.diversityarrays.kdcompute.db.helper.InvalidRuleException;
import com.diversityarrays.kdcompute.db.helper.KnobValidationRule;
import com.diversityarrays.kdcompute.db.helper.KnobValidationRule.Choice;
import com.diversityarrays.kdcompute.db.helper.KnobValidationRule.Range;
import com.diversityarrays.kdcompute.designer.editor.ChoicesValueEditor;
import com.diversityarrays.kdcompute.designer.editor.FileChooserValueEditor;
import com.diversityarrays.kdcompute.designer.editor.RangeValueEditor;
import com.diversityarrays.kdcompute.designer.editor.TextValueEditor;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.GBH;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class RunBindingPanel extends JPanel {

    private static final boolean ADD_LISTENERS = true;
    private static final boolean REMOVE_LISTENERS = false;

    private final JLabel message = new JLabel("", SwingConstants.CENTER);

    private final Map<Knob, ValueEditor> editorByKnob = new LinkedHashMap<>();

    private final Map<DataSet, ValueEditor> editorByDataSet = new LinkedHashMap<>();

    private final JFileChooser fileChooser = new JFileChooser();

    private Plugin plugin;
    private RunBinding editing;

    private String title = "Edit Run Binding";

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private boolean wantDataSets;

    public RunBindingPanel() {
        this(true);
    }

    public RunBindingPanel(boolean wantDataSets) {
        super(new BorderLayout());
        //      message.setEditable(false);

        this.wantDataSets = wantDataSets;

        message.setForeground(Color.RED);

        mainPanel.setBorder(new EmptyBorder(0, 4, 0, 4));

        add(mainPanel, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
    }

    public void applySuggestions(Map<Knob, String> valueByKnob) {
        valueByKnob.entrySet()
            .forEach(entry -> {
                ValueEditor editor = editorByKnob.get(entry.getKey());
                if (editor != null) {
                    editor.setValue(entry.getValue());
                }
            });
    }

    public Map<Knob, ValueEditor> getEditorByKnob() {
        return Collections.unmodifiableMap(editorByKnob);
    }

    public Map<DataSet, ValueEditor> getEditorByDataSet() {
        return Collections.unmodifiableMap(editorByDataSet);
    }

    public void setData(Plugin a, RunBinding rb) {
        this.editing = rb;
        this.plugin = a;

        mainPanel.removeAll();

        editorByKnob.clear();
        editorByDataSet.clear();

        addOrRemoveChangeListeners(REMOVE_LISTENERS);

        if (plugin != null) {
            JPanel panel = initUI();
            panel.setBorder(new LineBorder(Color.GRAY));
            mainPanel.add(panel, BorderLayout.CENTER);
            addOrRemoveChangeListeners(ADD_LISTENERS);
        }

        invalidate();
    }

    public Plugin getPlugin() {
    	return this.plugin;
    }

    public void setTitle(String s) {
        this.title = s;
    }

    public String getIncompleteMessage(String missingAlgorithmMessage) {
        String msg = missingAlgorithmMessage;
        if (plugin != null) {
            msg = getIncompleteMessage();
        }
        return msg;
    }

    private boolean wantKnob(Knob k) {
        if (! k.isRequired()) {
            return false;
        }
        if (! wantDataSets) {
            if (KnobDataType.FILE_UPLOAD == k.getKnobDataType()) {
                return false;
            }
        }
        return true;
    }

    public String getIncompleteMessage() {
        String msg = ""; //$NON-NLS-1$

        List<Knob> unbound = new ArrayList<>();
        for (Knob k : editorByKnob.keySet()) {
            if (wantKnob(k)) {
                String value = editorByKnob.get(k).getValue();
                if (Check.isEmpty(value)) {
                    unbound.add(k);
                }
            }
        }

        if (! unbound.isEmpty()) {
            msg = unbound.stream().map(Knob::getVisibleName)
                    .collect(Collectors.joining(", ", "Missing parameters: ", ""));
        }
        else if (wantDataSets) {
            List<DataSet> datasets = new ArrayList<>();

            for (DataSet ds : editorByDataSet.keySet()) {
                if (ds.isRequired()) {
                    String value = editorByDataSet.get(ds).getValue();
                    if (Check.isEmpty(value)) {
                        datasets.add(ds);
                    }
                }
            }

            if (! datasets.isEmpty()) {
                msg = datasets.stream().map(DataSet::getVisibleName)
                        .collect(Collectors.joining(", ", "Missing datasets: ", ""));
            }
        }

        message.setText(msg);
        return msg;
    }

    private JPanel initUI() {

        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel);
        int y = 0;

        gbh.add(0,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, GuiUtil.createLabelSeparator("Enter Parameter Values:"));
        ++y;

        Map<Knob, String> initialValueByKnob = new HashMap<>();
        Map<DataSet, String> initialValueByDataSet = new HashMap<>();

        if (editing == null) {
            for (Knob k : plugin.getKnobs()) {
                initialValueByKnob.put(k, k.getDefaultValue());
            }
        }
        else {
            for (KnobBinding kb : editing.getKnobBindings()) {
                initialValueByKnob.put(kb.getKnob(), kb.getKnobValue());
            }
            for (DataSetBinding dsb : editing.getInputDataSetBindings()) {
                initialValueByDataSet.put(dsb.getDataSet(), dsb.getDataSetUrl());
            }
        }

        y = addKnobsToUserInterface(initialValueByKnob, gbh, y);

        if (wantDataSets) {
            addDataSetsToUserInterface(initialValueByDataSet, gbh, y);
        }

        return panel;
    }

    public void addDataSetsToUserInterface(Map<DataSet, String> initialValueByDataSet, GBH gbh,
            int y) {
        Collection<DataSet> inputDataSets = plugin.getInputDataSets();
        if (! inputDataSets.isEmpty()) {
            gbh.add(0,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, GuiUtil.createLabelSeparator("Parameters:"));
            ++y;

            for (DataSet ds : inputDataSets) {

                String question = "Enter value for " + ds.getVisibleName();
                String initialValue = initialValueByDataSet.get(ds);
                if (initialValue == null) {
                    initialValue = "";
                }

                ValueEditor editor;
                switch (ds.getDataSetType()) {
                case ANY:
                    editor = new TextValueEditor(initialValue);
                    break;
                case URL:
                    editor = new TextValueEditor(initialValue);
                    break;
                case ANALYSIS_GROUP:
                case FILE_REF:
                case MARKER_DATASET:
                default:
                    throw new RuntimeException("Unsupported dataSetType: " + ds.getDataSetType() + " for " + ds.getVisibleName());
                }


                editorByDataSet.put(ds, editor);


                boolean usedDatSetName = false;
                String labelText = ds.getDataSetDescription();
                StringBuilder tt = new StringBuilder(ds.getTooltip());

                if (Check.isEmpty(labelText)) {
                    usedDatSetName = true;
                    labelText = ds.getDataSetName();
                }
                labelText = "<HTML>" + StringUtil.htmlEscape(labelText);
                JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
//                label.setHorizontalAlignment(SwingConstants.RIGHT);
                if (! usedDatSetName) {
                    tt.append(" (").append(ds.getDataSetName()).append(')');
                }
                label.setToolTipText(tt.toString());

                JComponent vc = editor.getVisualComponent();
                if  (vc instanceof JScrollPane)  {
                    gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.NE, label);
                }
                else {
                    gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.EAST, label);
                }
                gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, vc);
//                gbh.add(2,y, 1,1, GBH.NONE, 0,1, GBH.WEST, ds.getDataSetDescription());
                ++y;
            }
        }
    }

    public int addKnobsToUserInterface(Map<Knob, String> initialValueByKnob, GBH gbh, int y) {
        for (Knob k : plugin.getKnobs()) {

            if (! wantDataSets && KnobDataType.FILE_UPLOAD == k.getKnobDataType()) {
                continue;
            }

            String initialValue = initialValueByKnob.get(k);
            if (initialValue == null) {
                initialValue = k.getDefaultValue();
            }

            String vruleStr = k.getValidationRule();

            KnobValidationRule kvr = null;

            if (! Check.isEmpty(vruleStr)) {
                try {
                    Pair<KnobDataType, KnobValidationRule> pair = KnobValidationRule.create(vruleStr);
                    if (k.getKnobDataType().equals(pair.first)) {
                        kvr = pair.second;
                    }
                }
                catch (InvalidRuleException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            ValueEditor editor;
            if (kvr == null) {
                editor = getEditorWithoutValidationRule(k, initialValue);
            }
            else {
                editor = getEditorUsingValidationRule(k, initialValue, kvr);
            }


            editorByKnob.put(k, editor);

            boolean usedKnobName = false;
            String labelText = k.getDescription();
            StringBuilder tt = new StringBuilder(k.getTooltip());

            if (Check.isEmpty(labelText)) {
                usedKnobName = true;
                labelText = k.getKnobName();
            }
            labelText = "<HTML>" + StringUtil.htmlEscape(labelText);
            JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
            if (! usedKnobName) {
                tt.append(" (").append(k.getKnobName()).append(')');
            }
            label.setToolTipText(tt.toString());

            JComponent vc = editor.getVisualComponent();
            if  (vc instanceof JScrollPane)  {
                gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.NE, label);
            }
            else {
                gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.EAST, label);
            }
            gbh.add(1,y, 2,1, GBH.HORZ, 2,1, GBH.CENTER, vc);
            ++y;

        }
        return y;
    }

    public void addOrRemoveChangeListeners(boolean add) {
        if (add) {
            for (ValueEditor editor : editorByKnob.values()) {
                editor.addChangeListener(changeListener);
            }
            for (ValueEditor editor : editorByDataSet.values()) {
                editor.addChangeListener(changeListener);
            }
        }
        else {
            for (ValueEditor editor : editorByKnob.values()) {
                editor.removeChangeListener(changeListener);
            }
            for (ValueEditor editor : editorByDataSet.values()) {
                editor.removeChangeListener(changeListener);
            }
        }
    }

    private final ChangeEvent changeEvent = new ChangeEvent(this);

    private final ChangeListener changeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    };
    protected void fireStateChanged() {
        for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
            l.stateChanged(changeEvent);
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }


    private ValueEditor getEditorWithoutValidationRule(Knob k, String initialValue) {
        ValueEditor editor;
        switch (k.getKnobDataType()) {
        case CHOICE:
            editor = new TextValueEditor(initialValue);
            break;
        case DECIMAL:
            editor = new RangeValueEditor(initialValue, new Range(Range.MAX_DECIMAL_NDECS));
            break;
        case FILE_UPLOAD:
            editor = new FileChooserValueEditor(initialValue, RunBindingPanel.this, fileChooser);
            break;
        case INTEGER:
            editor = new RangeValueEditor(initialValue, new Range(0));
            break;
        case FORMULA:
        case TEXT:
            editor = new TextValueEditor(initialValue);
            break;
        default:
            throw new IllegalArgumentException("Unsupported type: " + k.getKnobDataType() + " for " + k.getVisibleName());
        }
        return editor;
    }

    private ValueEditor getEditorUsingValidationRule(Knob k, String initialValue, KnobValidationRule rule) {
        ValueEditor editor;
        switch (k.getKnobDataType()) {
        case CHOICE:
            editor = new ChoicesValueEditor(initialValue, ((Choice) rule).getChoices());
            break;
//        case DATE:
//            break;
        case INTEGER:
        case DECIMAL:
            editor = new RangeValueEditor(initialValue, (Range) rule);
            break;
        case FILE_UPLOAD:
            editor = new FileChooserValueEditor(initialValue, RunBindingPanel.this, fileChooser);
            break;
        case FORMULA:
        case TEXT:
            editor = new TextValueEditor(initialValue);
            break;
        default:
            throw new IllegalArgumentException("Unsupported type: " + k.getKnobDataType() + " for " + k.getVisibleName());
        }
        return editor;
    }


    public RunBinding getRunBinding() {
        RunBinding runBinding = new RunBinding(plugin);

        List<KnobBinding> knobBindings = new ArrayList<>();
        for (Knob k : editorByKnob.keySet()) {
            KnobBinding kb = new KnobBinding(k, editorByKnob.get(k).getValue());
            knobBindings.add(kb);
        }

        runBinding.setKnobBindings(knobBindings);

        List<DataSetBinding> dsBindings = new ArrayList<>();
        for (DataSet ds : editorByDataSet.keySet()) {
            DataSetBinding dsb = new DataSetBinding(ds, editorByDataSet.get(ds).getValue());
            dsBindings.add(dsb);
        }

        runBinding.setInputDataSetBindings(dsBindings);

        return runBinding;
    }

    static class DataSetBindingTableModel extends ColumnsTableModel<DataSetBinding> {

        public DataSetBindingTableModel() {
            super(new String[] { "Name", "Location" });
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex==1 && aValue instanceof String) {
                get(rowIndex).setDataSetUrl((String) aValue);
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DataSetBinding dsd = get(rowIndex);
            switch (columnIndex) {
            case 0: return dsd.getDataSet().getDataSetName();
            case 1: return dsd.getDataSetUrl();
            }
            return null;
        }

    }


    static class KnobBindingTableModel extends ColumnsTableModel<KnobBinding> {

        static public final int VALUE_COLUMN_INDEX = 3;

        public KnobBindingTableModel() {
            super(new String[] {  "Name", "Type", "Validation", "Value" });
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 3;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
            case 0: return String.class;
            case 1: return KnobDataType.class;
            case 2: return String.class;
            case 3: return String.class;
            }
            return Object.class;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 3 && aValue instanceof String) {
                KnobBinding kb = get(rowIndex);
                kb.setKnobValue((String) aValue);
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            KnobBinding kb = get(rowIndex);
            switch (columnIndex) {
            case 0: return kb.getKnob().getKnobName();
            case 1: return kb.getKnob().getKnobDataType();
            case 2: return kb.getKnob().getValidationRule();
            case 3: return getKnobValue(kb);
            }
            return null;
        }

        private String getKnobValue(KnobBinding kb) {
            String result = kb.getKnobValue();
            if (Check.isEmpty(result)) {
                switch (kb.getKnob().getKnobDataType()) {
                case CHOICE:
                    break;
                    //                case DATE:
                    //
                    //                    break;
                case DECIMAL:
                    result = "0.0";
                    break;
                case FILE_UPLOAD:
                    break;
                case FORMULA:
                    break;
                case INTEGER:
                    result = "0";
                    break;
                case TEXT:
                    break;
                default:
                    break;

                }
            }
            return result;
        }
    }


    public Component getMessageComponent() {
        return message;
    }
}
