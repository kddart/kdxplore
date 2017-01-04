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
package com.diversityarrays.kdxplore.trialmgr.trait;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRule.Calculated;
import com.diversityarrays.daldb.ValidationRule.Choice;
import com.diversityarrays.daldb.ValidationRule.ElapsedDays;
import com.diversityarrays.daldb.ValidationRule.Range;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.field.UnicodeChars;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.FilterTextField;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.table.BspAbstractTableModel;
import net.pearcan.ui.widget.PromptTextArea;
import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.GBH;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class TraitEditDialog extends JDialog {

    private JPanel currentCardContainer = new JPanel(new BorderLayout());

    private List<Trait> allTraits = new ArrayList<Trait>();
    private Map<TraitDataType, TraitDataTypePanel> datatypeCardPanelByDataType = new HashMap<>();

    private ItemListener dataTypeItemChangeListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showTraitDataTypePanel();
            }
        }
    };

    private KDSmartDatabase database;

    private TraitDataTypePanel currentCardPanel;
    private final Trait editedTrait;

    private final Action helpAction = new AbstractAction("Help") {
        @Override
        public void actionPerformed(ActionEvent e) {
            String html = currentCardPanel.getHtmlHelpText();
            if (html != null) {
                JLabel label = new JLabel(html);
                MsgBox.info(TraitEditDialog.this, label,
                        "Help for " + currentCardPanel.getTitle());
            }
        }
    };

    private JTextField nameField = new JTextField();

    private JLabel traitLevelLabel = new JLabel();
    private JComboBox<TraitLevel> traitLevelCombo = new JComboBox<>(new TraitLevel[] {
            TraitLevel.PLOT, TraitLevel.SPECIMEN
    });

    private JTextField aliasField = new JTextField();
    private JTextArea descField = new JTextArea(5, 20);

    private JTextField unitField = new JTextField();

    private JTextField errorMessage = new JTextField("", SwingConstants.CENTER); //$NON-NLS-1$
    private JTextField warningMessage = new JTextField("", SwingConstants.CENTER); //$NON-NLS-1$

    private final JComboBox<TraitDataType> datatypeCombo = new JComboBox<TraitDataType>(
            TraitDataType.values());

    private final AbstractAction saveAction = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {

            if (currentCardPanel != null) {
                try {
                    String newValidationRule = currentCardPanel.getSetValidationRule();
                    TraitDataType traitDataType = TraitDataType.valueOf(currentCardPanel.getName());
                    if (canEditTraitLevel && ! editedTrait.isProtected()) {
                        editedTrait.setTraitLevel((TraitLevel) traitLevelCombo.getSelectedItem());
                    }
                    editedTrait.setTraitDataType(traitDataType);
                    editedTrait.setTraitValRule(newValidationRule);
                    editedTrait.setTraitDescription(descField.getText().trim());
                    editedTrait.setTraitAlias(aliasField.getText());
                    editedTrait.setTraitUnit(unitField.getText());

                    Integer id = editedTrait.getTraitId();
                    if (id == null || id == 0) {
                        editedTrait.setTraitName(nameField.getText());
                    }

                    try {
                        if (originalTrait != null) {
                            TraitChangeable changeable = new TraitChangeable(originalTrait,
                                    copyTraitFields(editedTrait), database);
                            consumer.accept(changeable);
                        }
                        database.saveTrait(editedTrait, false);
                    }
                    catch (IOException e1) {
                        e1.printStackTrace();
                    }

                }
                catch (InvalidRuleException e1) {
                    e1.printStackTrace();
                }
                dispose();
            }
        }
    };

    private final AbstractAction cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };

    private final DocumentListener nameDescWatcher = new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
            updateErrorMessageForNameAndDesc();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateErrorMessageForNameAndDesc();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateErrorMessageForNameAndDesc();
        }
    };

    private final List<Trait> traitsForCalc;

//    private final Set<TraitDataTypePanel> firstTimeDone = new HashSet<>();

    private void showTraitDataTypePanel() {
        TraitDataType item = (TraitDataType) datatypeCombo.getSelectedItem();

        if (item != null) {
            boolean firstOrNumber = currentCardPanel == null
                    || currentCardPanel instanceof NumberDataTypePanel;
            if (currentCardPanel != null) {
                currentCardContainer.remove(currentCardPanel);
            }

            currentCardPanel = datatypeCardPanelByDataType.get(item);
//            if (! firstTimeDone.contains(currentCardPanel)) {
//                firstTimeDone.add(currentCardPanel);
//                currentCardPanel.initialiseFirstTime();
//            }
            if (editedTrait.isProtected()) {
                errorMessageCallback.updateErrorMessage("Protected Trait", true);
            }

            if (firstOrNumber || currentCardPanel instanceof NumberDataTypePanel) {
                // We can use the editedTrait
                currentCardPanel.setTraitBeingEdited(editedTrait);
            }

            String html = currentCardPanel.getHtmlHelpText();
            helpAction.setEnabled(html != null && !html.isEmpty());

            updateErrorMessageForNameAndDesc();

            currentCardContainer.add(currentCardPanel, BorderLayout.CENTER);
            pack();
        }
    }

    private void updateErrorMessageForNameAndDesc() {

        String warning = "";
        String msg = "";
        String nameText = nameField.getText();
        boolean enable;

        if (nameField.isEnabled()) {
            if (Check.isEmpty(nameText)) {
                enable = false;
                msg = "A unique Name is required";
            }
            else {
                for (Trait t : allTraits) {
                    if (t.getTraitName().equalsIgnoreCase(nameText)) {
                        msg = "Trait with this name exists";
                        break;
                    }
                }
            }

            if (msg.isEmpty()) {
                if (! Check.isEmpty(nameText) && ! TraitDataType.isTraitNameValidInCALC(nameText)) {
                    warning = "Invalid for use in CALC: " + nameText;
                }
                currentCardPanel.initializeCard();
                enable = true;
            }
            else {
                enable = false;
            }
        }
        else {
            enable = true;
        }

        if (enable) {
            String descText = descField.getText();
            if (Check.isEmpty(descText)) {
                if (originalTrait != null && ! Check.isEmpty(originalTrait.getTraitDescription())) {
                    enable = false;
                    msg = "Description is required";
                }
            }
        }

        errorMessageCallback.updateErrorMessage(msg, enable, warning);
    }

    private String message = null;
    private boolean canEditTraitLevel;

    private Consumer<TraitChangeable> consumer;

    private Trait originalTrait;

    public TraitEditDialog(Window owner,
            final Trait trait,
            PropertyDescriptor descriptor,
            KdxploreDatabase kdxdb,
            List<Trait> traitsForCalc,
            Consumer<TraitChangeable> consumer)
                    throws IOException {
        super(owner, "Edit Trait Properties", ModalityType.APPLICATION_MODAL);

        this.consumer = consumer;
        this.database = kdxdb.getKDXploreKSmartDatabase();
        this.traitsForCalc = traitsForCalc;
        Collections.sort(traitsForCalc);
        if (trait != null) {
            this.originalTrait = copyTraitFields(trait);
        }

        if (trait == null) {
            canEditTraitLevel = true;
            message = "New Trait";
        }
        else if (trait.isProtected()) {
            canEditTraitLevel = false;
            message = "Protected Trait (only Alias is editable)";
        }
        else {
            Predicate<KdxSample> predicate = new Predicate<KdxSample>() {
                @Override
                public boolean test(KdxSample sample) {
                    // We've seen a scored sample
                    message = "Scored Samples exist";
                    canEditTraitLevel = false;
                    return false;
                }
            };

            message = "Unprotected trait with no Samples";
            try {
                // No idDownloaded so all scored Samples are scanned
                canEditTraitLevel = true; // assume no score samples
                // the predicate will change this assumption
                kdxdb.visitScoredKdxSamples(trait.getTraitId(), predicate);
            }
            catch (IOException e) {
                // err on the side of caution
                canEditTraitLevel = false;
                message = "Error: " + e.getMessage();
            }
        }

        KDClientUtils.initAction(ImageId.HELP_24, helpAction, "Help for Trait Definition");

        errorMessage.setEditable(false);
        errorMessage.setBackground(getContentPane().getBackground());
        
        warningMessage.setEditable(false);
        warningMessage.setBackground(errorMessage.getBackground());

        if (trait != null) {
            this.editedTrait = trait;

            nameField.setEditable(false);
            nameField.setEnabled(false);

            if (trait.isProtected()) {
                descField.setEditable(false);
                descField.setEnabled(false);

                unitField.setEditable(false);
                unitField.setEnabled(false);
            }

            nameField.setText(trait.getTraitName());
            aliasField.setText(trait.getTraitAlias());
            descField.setText(trait.getTraitDescription());
            unitField.setText(trait.getTraitUnit());
        }
        else {
            Trait newTrait = new Trait();
            this.editedTrait = newTrait;
            this.editedTrait.setTraitDataType(TraitDataType.TEXT);
            this.editedTrait.setTraitValRule(""); //$NON-NLS-1$
        }

        allTraits = database.getTraits();

        nameField.getDocument().addDocumentListener(nameDescWatcher);
        descField.getDocument().addDocumentListener(nameDescWatcher);

        descField.setLineWrap(true);
        descField.setWrapStyleWord(true);

        JPanel mainPanel = new JPanel();

        for (TraitDataType tdt : TraitDataType.values()) {
            TraitDataTypePanel traitPanel = createDataTypeCardPanel(tdt);
            datatypeCardPanelByDataType.put(tdt, traitPanel);
        }
        datatypeCombo.setEditable(false);
        datatypeCombo.addItemListener(dataTypeItemChangeListener);
        if (editedTrait.isProtected()) {
            datatypeCombo.setEnabled(false);
        }

        JScrollPane descScrollPane = new JScrollPane(descField);
        descField.scrollRectToVisible(new Rectangle()); // TODO make this work

        GBH gbh = new GBH(mainPanel, 1, 2, 1, 2);
        int y = 0;

        if (!Check.isEmpty(message)) {
            gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.WEST,
                    "<HTML><I>" + StringUtil.htmlEscape(message) + "</i>");
            y++;
        }

        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "Name:");
        gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, nameField);
        y++;

        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "Alias:");
        gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, aliasField);
        y++;

        JComponent traitLevelComponent;
        if (trait == null) {
            traitLevelCombo.setSelectedItem(editedTrait.getTraitLevel());
            traitLevelComponent = traitLevelCombo;
        }
        else {
            TraitLevel level = editedTrait.getTraitLevel();
            if (TraitLevel.UNDECIDABLE == level) {
                // TODO change the predicate so we look at all Scored samples
                // and work out if they are all at one or the other level.
                traitLevelLabel.setText("*used for both Plot and Sub-Plot*");
                traitLevelComponent = traitLevelLabel;
            }
            else {
                if (canEditTraitLevel) {
                    traitLevelCombo.setSelectedItem(editedTrait.getTraitLevel());
                    traitLevelComponent = traitLevelCombo;
                }
                else {
                    traitLevelLabel.setText(level.toString());
                    traitLevelComponent = traitLevelLabel;
                }
            }
        }
        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "Level:");
        gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.WEST, traitLevelComponent);
        y++;

        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.NE, "Description:");
        gbh.add(1, y, 2, 1, GBH.HORZ, 1, 2, GBH.CENTER, descScrollPane);
        y++;

        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "Unit:");
        gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, unitField);
        y++;

        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "Datatype:");
        gbh.add(1, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, datatypeCombo);
        y++;

        gbh.add(0, y, 3, 1, GBH.HORZ, 1, 1, GBH.CENTER, warningMessage);
        ++y;
        gbh.add(0, y, 3, 1, GBH.HORZ, 1, 1, GBH.CENTER, errorMessage);
        ++y;

        errorMessage.setForeground(Color.red);
        warningMessage.setForeground(Color.BLUE);

        Box buttonsBox = Box.createHorizontalBox();

        buttonsBox.add(new JButton(helpAction));
        buttonsBox.add(Box.createHorizontalGlue());
        buttonsBox.add(new JButton(cancelAction));
        buttonsBox.add(Box.createHorizontalStrut(10));
        buttonsBox.add(new JButton(saveAction));

        Container mainContainer = getContentPane();
        mainContainer.add(mainPanel, BorderLayout.NORTH);
        mainContainer.add(currentCardContainer, BorderLayout.CENTER);
        mainContainer.add(buttonsBox, BorderLayout.SOUTH);
        pack();

        datatypeCombo.setSelectedIndex(-1);
        datatypeCombo.setSelectedItem(this.editedTrait.getTraitDataType());

        if (descriptor != null) {
            String dn = descriptor.getDisplayName();
            if ("Alias".equals(dn)) {
                aliasField.requestFocus();
            }
            else if ("Description".equals(dn)) {
                descField.requestFocus();
            }
            else if ("Unit".equals(dn)) {
                unitField.requestFocus();
            }
            else if ("Data Type".equals(dn)) {
                datatypeCombo.requestFocus();
            }
            else if ("Validation".equals(dn)) {
                if (currentCardPanel != null) {
                    currentCardPanel.getInitialFocusComp().requestFocus();
                }
            }

        }
    }

    private Trait copyTraitFields(Trait oldTrait) {
        Trait trait = new Trait();
        trait.setTraitDataType(oldTrait.getTraitDataType());
        trait.setTraitValRule(oldTrait.getTraitValRule());
        trait.setTraitDescription(oldTrait.getTraitDescription());
        trait.setTraitAlias(oldTrait.getTraitAlias());
        trait.setTraitUnit(oldTrait.getTraitUnit());
        trait.setTraitId(oldTrait.getTraitId());
        trait.setOrigTraitValRule(oldTrait.getOrigTraitValRule());
        trait.setTraitLevel(oldTrait.getTraitLevel());
        trait.setTraitName(oldTrait.getTraitName());
        trait.setDateChangedInKDS(oldTrait.getDateChangedInKDS());
        trait.setBarcode(oldTrait.getBarcode());
        trait.setDateDownloaded(oldTrait.getDateDownloaded());
        trait.setIdDownloaded(oldTrait.getIdDownloaded());

        return trait;
    }

    private ErrorMessageCallback errorMessageCallback = new ErrorMessageCallback() {
        
        @Override
        public void updateErrorMessage(String message, boolean enableSetButton) {
            updateErrorMessage(message, enableSetButton, null);
        }
        
        @Override
        public void updateErrorMessage(String message, boolean enableSetButton, String warning) {
            errorMessage.setText(message);

            boolean enb = enableSetButton;
            if (enb) {
                if (Check.isEmpty(unitField.getText().trim())) {
                    TraitDataType tdt = editedTrait.getTraitDataType();
                    if (traitDataTypeRequiresUnit(tdt)) {
                        errorMessage.setText("Unit is required for " + tdt.name());
                        enb = false;
                    }
                }
            }
            saveAction.setEnabled(enb);
        }

        private boolean traitDataTypeRequiresUnit(TraitDataType traitDataType) {
            switch (traitDataType) {
            case DECIMAL:
            case INTEGER:
                return true;

            case CALC:
            case CATEGORICAL:
            case DATE:
            case ELAPSED_DAYS:
            case TEXT:
                return false;

            default:
                return false;
            }
        }
    };

    private TraitDataTypePanel createDataTypeCardPanel(TraitDataType traitDataType) {

        TraitDataTypePanel traitPanel = null;

        // create a new
        switch (traitDataType) {

        case TEXT:
            traitPanel = new TextDataTypePanel(errorMessageCallback);
            break;
        case DECIMAL:
            traitPanel = new DecimalDataTypePanel(errorMessageCallback);
            break;
        case INTEGER:
            traitPanel = new IntegerDataTypePanel(errorMessageCallback);
            break;
        case ELAPSED_DAYS:
            traitPanel = new ElapsedDaysDataTypePanel(errorMessageCallback);
            break;
        case CATEGORICAL:
            traitPanel = new CategoricalDataTypePanel(errorMessageCallback);
            break;
        case DATE:
            traitPanel = new DateCardDataTypePanel(errorMessageCallback);
            break;
        case CALC:
            traitPanel = new CalcDataTypePanel(traitsForCalc, errorMessageCallback);
            break;
        default:
            throw new RuntimeException("Unsupported TraitDataType: " + traitDataType);
        }

        traitPanel.setName(traitDataType.name());

        return traitPanel;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // --- These are the Panels for defining a specific TraitDataType

    interface ErrorMessageCallback {
        void updateErrorMessage(String error, boolean saveButtonEnable);
        void updateErrorMessage(String error, boolean saveButtonEnable, String warning);
    }

    static abstract class TraitDataTypePanel extends JPanel {

        private final String title;
        protected ErrorMessageCallback callBack;

        TraitDataTypePanel(String title, ErrorMessageCallback callback) {
            this.title = title;
            this.callBack = callback;
        }

        abstract public Component getInitialFocusComp();

        public final String getTitle() {
            return title;
        }

        public abstract void setTraitBeingEdited(Trait trait);

        public abstract String getSetValidationRule() throws InvalidRuleException;

        public abstract void initializeCard();

        public String getHtmlHelpText() {
            return null;
        }
    }

    static class TextDataTypePanel extends TraitDataTypePanel {

        TextDataTypePanel(ErrorMessageCallback callback) {
            super("TEXT Trait", callback);
            initializeCard();
        }

        @Override
        public Component getInitialFocusComp() {
            return this;
        }

        @Override
        public String getSetValidationRule() {
            return "";
        }

        @Override
        public void setTraitBeingEdited(Trait trait) {
            // NO Operation
        }

        @Override
        public void initializeCard() {
            callBack.updateErrorMessage("No more information needed", true);
        }
    }

    static abstract class NumberDataTypePanel extends TraitDataTypePanel {

        private final ButtonGroup minRadioGroup = new ButtonGroup();
        protected JRadioButton minLessThanEqual = new JRadioButton("<=");
        protected JRadioButton minLessThan = new JRadioButton("<");

        private final ButtonGroup maxRadioGroup = new ButtonGroup();
        protected JRadioButton maxLessThanEqual = new JRadioButton("<=");
        protected JRadioButton maxLessThan = new JRadioButton("<");

        protected NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());

        protected JFormattedTextField minTextField = new JFormattedTextField(numberFormat);
        protected JFormattedTextField maxTextField = new JFormattedTextField(numberFormat);

        protected ValidationRule valRule = null;

        protected boolean isTraitProtected;

        protected final SpinnerNumberModel decimalModel;

        NumberDataTypePanel(String title, ErrorMessageCallback erm,
                SpinnerNumberModel decimalModel) {
            super(title, erm);

            this.decimalModel = decimalModel;

            minTextField.setToolTipText("Minimum value to accept");
            maxTextField.setToolTipText("Maximum value to accept");

            minRadioGroup.add(minLessThanEqual);
            minRadioGroup.add(minLessThan);

            maxRadioGroup.add(maxLessThanEqual);
            maxRadioGroup.add(maxLessThan);

            DocumentListener listener = new DocumentListener() {

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateWarningMessage();

                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateWarningMessage();

                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateWarningMessage();

                }
            };

            minTextField.setColumns(10);
            maxTextField.setColumns(10);
            minTextField.getDocument().addDocumentListener(listener);
            maxTextField.getDocument().addDocumentListener(listener);
        }

        @Override
        public Component getInitialFocusComp() {
            return minTextField;
        }

        @Override
        public final void initializeCard() {
            minTextField.setText("0");
            maxTextField.setText("1");
            if (decimalModel != null) {
                decimalModel.setValue(1);
            }
            minLessThanEqual.setSelected(true);
            maxLessThanEqual.setSelected(true);
        }

        protected void updateDecimalPlaces(int nDecimalPlaces) {

            String fmt = "%." + nDecimalPlaces + "f";

            if (decimalModel != null) {
                decimalModel.setValue(nDecimalPlaces);
            }

            if (this.valRule == null) {
                minTextField.setText("0");
                maxTextField.setText("1");
            }
            else {
                double[] rangeLimits = valRule.getRangeLimits();
                minTextField.setText(String.format(fmt, rangeLimits[0]));
                maxTextField.setText(String.format(fmt, rangeLimits[1]));
            }
        }

        @Override
        final public void setTraitBeingEdited(Trait trait) {

            isTraitProtected = trait.isProtected();

            TraitDataType tdt = trait.getTraitDataType();

            if (TraitDataType.DECIMAL == tdt || TraitDataType.INTEGER == tdt) {
                try {
                    this.valRule = ValidationRule.create(trait.getTraitValRule());

                    if (valRule.isRange()) {

                        minLessThanEqual.setSelected(valRule.isRangeStartIncluded());
                        maxLessThanEqual.setSelected(valRule.isRangeEndIncluded());

                        if (decimalModel == null) {
                            updateDecimalPlaces(0);
                        }
                        else {
                            updateDecimalPlaces(valRule.getNumberOfDecimalPlaces());
                        }
                    }
                    else {
                        setDefaultValues();
                    }
                }
                catch (InvalidRuleException e) {
                    initializeCard();
                    e.printStackTrace();
                }
            }
            else {
                setDefaultValues();
            }

            updateWarningMessage();
        }

        public void setDefaultValues() {
            minTextField.setText("0");
            maxTextField.setText("1");
            if (decimalModel != null) {
                decimalModel.setValue(0);
            }
            minLessThanEqual.setSelected(true);
            maxLessThanEqual.setSelected(true);
        }

        protected void updateWarningMessage() {

            String msg = "";

            double minD, maxD;

            String minNumber = minTextField.getText();
            String maxNumber = maxTextField.getText();

            if (minNumber.isEmpty() || maxNumber.isEmpty()) {
                msg = "Both min and max field are required";
                callBack.updateErrorMessage(msg, false);
                // return;
            }

            try {
                minD = Double.parseDouble(minNumber);
                maxD = Double.parseDouble(maxNumber);
            }
            catch (NumberFormatException e) {
                msg = "Min and max both should be numbers";
                callBack.updateErrorMessage(msg, false);
                return;
            }

            if (maxD <= minD) {
                msg = "Max should be more than min";
                callBack.updateErrorMessage(msg, false);
                return;
            }

            if (this.isTraitProtected && this.valRule != null) {
                double[] limits = valRule.getRangeLimits();

                if (minD < limits[0] || maxD > limits[1]) {
                    msg = "Protected trait keep rule between " + String.valueOf(limits[0]) + " and "
                            + String.valueOf(limits[1]);
                    callBack.updateErrorMessage(msg, false);
                    return;
                }
            }
            callBack.updateErrorMessage("", true); // All good, ready to save.
        }

        protected void initialiseLayout(JComponent decimals) {

            GBH gbh = new GBH(this, 4, 4, 4, 4);

            Box xBox = Box.createVerticalBox();
            xBox.add(Box.createVerticalGlue());
            xBox.add(new JLabel("x"));
            xBox.add(Box.createVerticalGlue());

            int y = 0;

            gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.CENTER, "Min:");
            gbh.add(1, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, minLessThanEqual);
            gbh.add(2, y, 1, 2, GBH.VERT, 1, 1, GBH.CENTER, xBox);
            gbh.add(3, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, maxLessThanEqual);
            gbh.add(4, y, 1, 1, GBH.NONE, 0, 1, GBH.CENTER, "Max:");
            if (decimals != null) {
                gbh.add(5, y, 1, 1, GBH.NONE, 0, 1, GBH.CENTER, ".nnn");
            }
            ++y;

            gbh.add(0, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, minTextField);
            gbh.add(1, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, minLessThan);
            // x
            gbh.add(3, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, maxLessThan);
            gbh.add(4, y, 1, 1, GBH.HORZ, 1, 1, GBH.CENTER, maxTextField);
            if (decimals != null) {
                gbh.add(5, y, 1, 1, GBH.NONE, 1, 1, GBH.CENTER, decimals);
            }
            ++y;
        }

        protected String buildRangeValidationRule(String min, boolean includeMin,
                String max, boolean includeMax) {
            String rangeType = "RANGE";
            if (!includeMin) {
                if (!includeMax) {
                    rangeType = "BERANGE";
                }
                else {
                    rangeType = "LERANGE";
                }
            }
            else if (!includeMax) {
                rangeType = "RERANGE";
            }

            StringBuilder sb = new StringBuilder(rangeType);
            sb.append("(");
            sb.append(min).append("..").append(max);
            sb.append(")");

            return sb.toString();
        }
    }

    static class DecimalDataTypePanel extends NumberDataTypePanel {

        DecimalDataTypePanel(ErrorMessageCallback erm) {
            super("DECIMAL Trait", erm,
                    new SpinnerNumberModel(1, 1, ValidationRule.MAX_DECIMAL_PLACES, 1));

            JSpinner decimalSpinner = new JSpinner(decimalModel);

            decimalModel.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    updateDecimalPlaces(decimalModel.getNumber().intValue());
                }
            });
            decimalSpinner.setToolTipText("Number of decimal places allowed");
            initialiseLayout(decimalSpinner);
            initializeCard();
        }

        @Override
        public String getSetValidationRule() throws InvalidRuleException {

            double dmin, dmax;
            try {
                dmin = Double.parseDouble(minTextField.getText());
            }
            catch (NumberFormatException nfe) {
                throw new InvalidRuleException("Invalid min: " + minTextField.getText());
            }
            try {
                dmax = Double.parseDouble(maxTextField.getText());
            }
            catch (NumberFormatException nfe) {
                throw new InvalidRuleException("Invalid max: " + maxTextField.getText());
            }

            if (dmin >= dmax) {
                throw new InvalidRuleException("Min must be less than Max");
            }

            int nDecimals = decimalModel.getNumber().intValue();

            String fmt = "%." + nDecimals + "f";

            return buildRangeValidationRule(
                    String.format(fmt, dmin), minLessThanEqual.isSelected(),
                    String.format(fmt, dmax), maxLessThanEqual.isSelected());
        }

    }

    static class IntegerDataTypePanel extends NumberDataTypePanel {

        IntegerDataTypePanel(ErrorMessageCallback erm) {
            super("INTEGER Trait", erm, null);
            initialiseLayout(null);
            initializeCard();
        }

        @Override
        public String getSetValidationRule() {

            String min, max;

            min = minTextField.getText();
            max = maxTextField.getText();

            boolean includeMin = minLessThanEqual.isSelected();
            boolean includeMax = maxLessThanEqual.isSelected();

            return buildRangeValidationRule(min, includeMin, max, includeMax);

        }

    }

    // =======
    static class DateCardDataTypePanel extends TraitDataTypePanel {

        DateCardDataTypePanel(ErrorMessageCallback emrs) {
            super("DATE Trait", emrs);
            initializeCard();
        }

        @Override
        public void setTraitBeingEdited(Trait trait) {
        }

        @Override
        public Component getInitialFocusComp() {
            return this;
        }

        @Override
        public String getSetValidationRule() {
            return "CHOICE(date)";
        }

        @Override
        public void initializeCard() {
            callBack.updateErrorMessage("No more information needed", true);
        }

    }

    // =====
    static class CalcDataTypePanel extends TraitDataTypePanel {

        private final PromptTextArea expressionArea = new PromptTextArea("enter expression here", 5, 0);

        // Note : use ZERO as min here because that's allowed for CALC
        private final SpinnerNumberModel nDecimalsModel = new SpinnerNumberModel(1, 0,
                ValidationRule.MAX_DECIMAL_PLACES, 1);

        private final JSpinner nDecimalsSpinner = new JSpinner(nDecimalsModel);

        private final FilterTextField filterField = new FilterTextField("search by name or description");

        private final DocumentListener docListener = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSaveButton();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSaveButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSaveButton();
            }
        };

        class VariableNameTableModel extends BspAbstractTableModel {

            private final List<Trait> allTraits = new ArrayList<>();

            private final List<Trait> matchingTraits = new ArrayList<>();

            private String filterText = "";

            VariableNameTableModel() {
                super("Name", "DataType", "Description");
            }

            public void setFilter(String s) {
                filterText = s.toLowerCase();
                applyFilter();
            }

            private boolean getFeatureContainsFilter(String feature) {
                if (feature == null) {
                    return false;
                }
                return feature.toLowerCase().contains(filterText);
            }

            private void applyFilter() {
                matchingTraits.clear();
                if (filterText.isEmpty()) {
                    matchingTraits.addAll(allTraits);
                }
                else {
                    for (Trait t : allTraits) {
                        if (getFeatureContainsFilter(t.getTraitName())
                                ||
                                getFeatureContainsFilter(t.getTraitAlias())
                                ||
                                getFeatureContainsFilter(t.getTraitDescription())) {
                            matchingTraits.add(t);
                        }
                    }
                }
                fireTableDataChanged();
            }

            public void setTraits(List<Trait> list) {
                allTraits.clear();
                allTraits.addAll(list);
                applyFilter();
                fireTableDataChanged();
            }

            @Override
            public int getRowCount() {
                return matchingTraits.size();
            }

            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                case 0:
                    return String.class;
                case 1:
                    return TraitDataType.class;
                case 2:
                    return String.class;
                }
                return Object.class;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Trait trait = getTrait(rowIndex);
                if (trait != null) {
                    switch (columnIndex) {
                    case 0:
                        return trait.getTraitName();
                    case 1:
                        return trait.getTraitDataType();
                    case 2:
                        return trait.getTraitDescription();
                    }
                }
                return null;
            }

            public Trait getTrait(int row) {
                if (0 <= row && row < matchingTraits.size()) {
                    return matchingTraits.get(row);
                }
                return null;
            }
        }

        private final VariableNameTableModel variableNameTableModel = new VariableNameTableModel();
        private final JTable variableNameTable = new JTable(variableNameTableModel);

        private final Set<String> namesOkForCalcSet = new HashSet<>();

        private JSplitPane splitPane;

        CalcDataTypePanel(List<Trait> traits, ErrorMessageCallback erms) {
            super("CALC Trait", erms);

            for (Trait t : traits) {
                namesOkForCalcSet.add(t.getTraitName());
            }

            variableNameTable.setAutoCreateRowSorter(true);
            variableNameTableModel.setTraits(traits);
            variableNameTable.getSelectionModel()
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            variableNameTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && 2 == e.getClickCount()) {
                        e.consume();

                        int vrow = variableNameTable.rowAtPoint(e.getPoint());
                        if (vrow >= 0) {
                            int mrow = variableNameTable.convertRowIndexToModel(vrow);
                            if (mrow >= 0) {
                                Trait trait = variableNameTableModel.getTrait(mrow);
                                if (trait != null) {
                                    int pos = expressionArea.getCaretPosition();
                                    expressionArea.insert(trait.getTraitName(), pos);
                                }
                            }
                        }
                    }
                }
            });

            expressionArea.getDocument().addDocumentListener(docListener);

            nDecimalsSpinner.setToolTipText(
                    "0 for INT, 1.." + ValidationRule.MAX_DECIMAL_PLACES + " for DEC");

            filterField.addFilterChangeHandler((text) -> variableNameTableModel.setFilter(text));

            Box vartop = Box.createVerticalBox();
            vartop.add(GuiUtil.createLabelSeparator("Traits"));
            vartop.add(filterField);
            
            JPanel variablePanel = new JPanel(new BorderLayout());
            variablePanel.add(vartop, BorderLayout.NORTH);
            variablePanel.add(new JScrollPane(variableNameTable), BorderLayout.CENTER);

            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    new JScrollPane(expressionArea),
                    variablePanel);
            splitPane.setResizeWeight(0.5);

            Box top = Box.createHorizontalBox();
            top.add(new JLabel("nDecimals:"));
            top.add(nDecimalsSpinner);
            top.add(Box.createHorizontalGlue());

            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(0, 2, 0, 2));
            add(top, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);

            initializeCard();
        }

        @Override
        public Component getInitialFocusComp() {
            return expressionArea;
        }

        @Override
        public String getHtmlHelpText() {
            return "Explain the syntax and functions available";
            // FIXME provide help for CALC
        }

        private ValidationRule createRule() throws InvalidRuleException {
            String inRule = collectRuleString();
            return ValidationRule.create(inRule);
        }

        public String collectRuleString() {
            StringBuilder sb = new StringBuilder("CALC(");
            sb.append(expressionArea.getText());
            int nDecimals = nDecimalsModel.getNumber().intValue();
            if (nDecimals > 0) {
                sb.append(',').append(nDecimals);
            }
            sb.append(")");

            String inRule = sb.toString();
            return inRule;
        }

        private void updateSaveButton() {
            try {
                ValidationRule vrule = createRule();
                System.err.println("EXPR=" + vrule.getExpression());
                if (vrule instanceof Calculated) {
                    Set<String> variableNames = ((Calculated) vrule).getVariableNames();
                    List<String> invalid = new ArrayList<>();
                    for (String vname : variableNames) {
                        if (!namesOkForCalcSet.contains(vname)) {
                            invalid.add(vname);
                        }
                    }

                    if (invalid.isEmpty()) {
                        callBack.updateErrorMessage("", true);
                    }
                    else {
                        String message = StringUtil.join("Not Trait Name: '", "', '", invalid)
                                + "'";
                        callBack.updateErrorMessage(message, false);
                    }
                }
                else {
                    callBack.updateErrorMessage(
                            "Incorrect rule type: " + vrule.getValidationRuleType(), false);
                }
            }
            catch (InvalidRuleException e) {
                String msg = e.getMessage();
                if (Check.isEmpty(msg)) {
                    msg = "Invalid rule";
                }
                callBack.updateErrorMessage(msg, false);
            }
        }

        @Override
        public void setTraitBeingEdited(Trait trait) {

            boolean enable = !trait.isProtected();

            expressionArea.setEnabled(enable);
            nDecimalsSpinner.setEnabled(enable);

            ValidationRule vrule = null;
            try {
                vrule = ValidationRule.create(trait.getTraitValRule());
            }
            catch (InvalidRuleException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (vrule instanceof Calculated) {
                expressionArea.setText(((Calculated) vrule).getExpression());
            }
            else {
                expressionArea.setText("");
            }
        }

        @Override
        public String getSetValidationRule() {
            return collectRuleString();
        }

        @Override
        public void initializeCard() {
            // TODO
        }
    }

    // =====
    static class CategoricalDataTypePanel extends TraitDataTypePanel {

        static class ChoicePair {
            String value;
            String desc;

            public ChoicePair(String v, String d) {
                value = v;
                desc = d;
            }
        }

        static class ChoicesTableModel extends BspAbstractTableModel {

            private final List<ChoicePair> choicePairs = new ArrayList<>();

            ChoicesTableModel() {
                super("Choice", "Description");
            }

            public boolean containsChoice(String choiceValue) {
                for (ChoicePair cp : choicePairs) {
                    if (cp.value.equals(choiceValue)) {
                        return true;
                    }
                }
                return false;
            }

            public void setData(Map<String, ChoicePair> more) {
                List<ChoicePair> list = new ArrayList<>();
                for (String key : more.keySet()) {
                    list.add(more.get(key));
                }

                choicePairs.clear();
                choicePairs.addAll(list);
                fireTableDataChanged();
            }

            @Override
            public int getRowCount() {
                return choicePairs.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                ChoicePair choice = choicePairs.get(rowIndex);
                switch (columnIndex) {
                case 0:
                    return choice.value;
                case 1:
                    return choice.desc;
                }
                return null;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (1 != columnIndex) {
                    return;
                }
                ChoicePair choice = choicePairs.get(rowIndex);
                choice.desc = aValue == null ? "" : aValue.toString();
                fireTableCellUpdated(rowIndex, columnIndex);
            }

            public void removeChoicesAtIndices(List<Integer> rows) {
                Collections.sort(rows, Collections.reverseOrder());
                for (Integer rowIndex : rows) {
                    choicePairs.remove(rowIndex.intValue());
                    fireTableRowsDeleted(rowIndex, rowIndex);
                }
            }

            public List<ChoicePair> getChoicePairs(List<Integer> rows) {
                List<ChoicePair> result = new ArrayList<>();
                Collections.sort(rows);
                for (Integer row : rows) {
                    result.add(choicePairs.get(row));
                }
                return result;
            }

        }

        private final ChoicesTableModel choicesTableModel = new ChoicesTableModel();
        private final JTable choicesTable = new JTable(choicesTableModel);

        private PromptTextField choiceText = new PromptTextField(
                "value or value:description. multiple using , or |");

        private final Action addButtonAction = new AbstractAction("Add/Set") {

            @Override
            public void actionPerformed(ActionEvent e) {

                Map<String, ChoicePair> choiceMap = new LinkedHashMap<>();
                for (ChoicePair ch : choicesTableModel.choicePairs) {
                    choiceMap.put(ch.value, ch);
                }

                for (String part : choiceText.getText().split("[,|]")) {
                    part = part.trim();
                    int cpos = part.indexOf(':');

                    String value;
                    String desc;
                    if (cpos >= 0) {
                        value = part.substring(0, cpos);
                        desc = part.substring(cpos + 1);
                    }
                    else {
                        value = part;
                        desc = "";
                    }

                    ChoicePair choice = choiceMap.get(value);
                    if (choice == null) {
                        choice = new ChoicePair(value, desc);
                        choiceMap.put(value, choice);
                    }
                    else {
                        choice.desc = desc;
                    }
                }

                choicesTableModel.setData(choiceMap);
                choiceText.setText("");
            }
        };

        private Action deleteButtonAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(choicesTable);
                if (!selectedModelRows.isEmpty()) {
                    choicesTableModel.removeChoicesAtIndices(selectedModelRows);
                }
            }
        };

        // private final JLabel warning = new JLabel();

        CategoricalDataTypePanel(ErrorMessageCallback erms) {
            super("CATEGORICAL Trait", erms);

            // warning.setForeground(Color.RED);
            // warning.setBackground(getBackground());
            initializeCard();

            KDClientUtils.initAction(ImageId.TRASH_24, deleteButtonAction, "Delete");

            Box controls = Box.createHorizontalBox();
            controls.add(choiceText);
            controls.add(new JButton(addButtonAction));
            controls.add(new JButton(deleteButtonAction));

            choiceText.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateAddAction();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateAddAction();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateAddAction();
                }
            });

            choicesTableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    updateSaveButton();
                }
            });

            choicesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    updateAddDeleteActions();
                }
            });
            updateAddDeleteActions();
            
            Box top = Box.createVerticalBox();
            top.add(GuiUtil.createLabelSeparator("Choices"));
            top.add(controls);

            setLayout(new BorderLayout());
            add(top, BorderLayout.NORTH);
            add(new JScrollPane(choicesTable), BorderLayout.CENTER);
        }

        @Override
        public Component getInitialFocusComp() {
            return choiceText;
        }

        private void updateSaveButton() {
            int nChoices = choicesTableModel.getRowCount();

            if (nChoices < 2) {
                String msg = "Need at least two Choices";
                callBack.updateErrorMessage(msg, false);
            }
            else {
                callBack.updateErrorMessage("", true);
            }
        }

        private void updateAddAction() {

            boolean enable = false;
            String text = choiceText.getText().trim();

            String msg = null;
            if (!text.isEmpty()) {
                enable = true;
                int pos = text.indexOf(':');
                String choiceValue = text;
                if (pos >= 0) {
                    choiceValue = text.substring(0, pos);
                }

                if (choicesTableModel.containsChoice(choiceValue)) {
                    msg = "'" + choiceValue + "' will be replaced";
                }
            }

            // warning.setText(msg==null ? " " : msg);

            addButtonAction.setEnabled(!text.isEmpty());
        }

        private void updateAddDeleteActions() {
            List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(choicesTable);

            boolean enable = selectedModelRows.size() > 0 && !protectedTrait;
            addButtonAction.setEnabled(enable);
            deleteButtonAction.setEnabled(enable);

            if (enable) {
                StringBuilder sb = new StringBuilder();
                for (ChoicePair cp : choicesTableModel.getChoicePairs(selectedModelRows)) {
                    sb.append('|').append(cp.value);
                    if (cp.desc != null && !cp.desc.isEmpty()) {
                        sb.append(':').append(cp.desc);
                    }
                }
                choiceText.setText(sb.substring(1));
            }

            // warning.setText(" ");
        }

        private boolean protectedTrait;

        @Override
        public void setTraitBeingEdited(Trait trait) {

            protectedTrait = trait.isProtected();

            try {
                ValidationRule valRule = ValidationRule.create(trait.getTraitValRule());
                List<String> values = valRule.getChoices();
                String[] descriptions = new String[0];
                if (valRule instanceof Choice) {
                    descriptions = ((Choice) valRule).getDescriptions();
                }
                Map<String, ChoicePair> map = new LinkedHashMap<>();
                int index = -1;
                for (String value : values) {
                    ++index;
                    String desc = index < descriptions.length ? descriptions[index] : "";
                    map.put(value, new ChoicePair(value, desc));
                }
                choicesTableModel.setData(map);
            }
            catch (InvalidRuleException e) {
                e.printStackTrace();
            }

            updateAddDeleteActions();
        }

        @Override
        public String getSetValidationRule() {
            StringBuilder sb = new StringBuilder();
            sb.append("CHOICE(");

            String sep = "";
            for (ChoicePair cp : choicesTableModel.choicePairs) {
                sb.append(sep);
                sb.append(cp.value);
                if (cp.desc != null && !cp.desc.isEmpty()) {
                    sb.append(':').append(cp.desc);
                }
                sep = "|";
            }
            sb.append(")");
            return sb.toString();
        }

        @Override
        public void initializeCard() {
            callBack.updateErrorMessage("Need at least two choices", false);
        }

    }

    static class ElapsedDaysDataTypePanel extends TraitDataTypePanel {

        JCheckBox limitBox;

        SpinnerNumberModel model = new SpinnerNumberModel(500, 1, 5000, 1);
        JSpinner limitValue = new JSpinner(model);

        ElapsedDaysDataTypePanel(ErrorMessageCallback erms) {
            super("ELAPSED_DAYS", erms);

            Box elapsedDaysBox = Box.createHorizontalBox();
            JLabel elapsedDaysText = new JLabel("Elapsed Days: ");

            limitBox = new JCheckBox("Apply Limit");

            initializeCard();

            elapsedDaysBox.add(elapsedDaysText);
            elapsedDaysBox.add(limitBox);
            elapsedDaysBox.add(limitValue);

            add(elapsedDaysBox);
        }

        @Override
        public Component getInitialFocusComp() {
            return limitBox;
        }

        @Override
        public void setTraitBeingEdited(Trait trait) {
            String validationRule = trait.getTraitValRule();
            if (validationRule != null && !validationRule.isEmpty()) {
                limitBox.setSelected(true);
                try {
                    ValidationRule val = ValidationRule.create(validationRule);
                    if (val instanceof ElapsedDays) {
                        ElapsedDays edays = (ElapsedDays) val;
                        model.setValue(edays.getMaxValue());
                    }
                    else if (val instanceof Range) {
                        model.setValue(((Range) val).getRangeLimits()[1]);
                    }
                    else {
                        model.setValue(Integer.MAX_VALUE);
                    }
                }
                catch (InvalidRuleException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public String getSetValidationRule() throws InvalidRuleException {
            if (!limitBox.isSelected()) {
                return "";
            }

            int imax = model.getNumber().intValue();
            return ValidationRule.createValidationRuleForMaxElapsedDays(imax);
        }

        @Override
        public void initializeCard() {
            limitBox.setSelected(false);
            model.setValue(90);
            callBack.updateErrorMessage("", true);
        }
    }

}
