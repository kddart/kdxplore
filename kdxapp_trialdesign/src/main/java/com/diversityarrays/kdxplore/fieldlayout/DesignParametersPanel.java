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
package com.diversityarrays.kdxplore.fieldlayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.design.DesignEntry;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.GBH;

@SuppressWarnings("nls")
public class DesignParametersPanel extends JPanel {

    private static final String N_ENTRIES = "# Entries:";
    private static final String N_ENTRIES_FROM_ENTRY_LIST = "# Entries (from Entry List)";

    public static final String DESIGN_PARAMS_HEADING = "Parameters"; // "Entries & Reps";
    static public enum DesignParam {
        ROWS_PER_PLOT,
        ENTRY_COUNT,
        REP_COUNT,
        SPATIAL_CHECKS,
        ;

        public boolean isInteger() {
            return this != SPATIAL_CHECKS;
        }
    }

    private static final int MAX_ROWS_PER_PLOT = 5000;
    private static final int MAX_ENTRIES = 20000;
    private static final int MAX_REPLICATIONS = 100;

    private List<? extends DesignEntry> designEntries;
    private final JLabel zeroSpatialWarning = new JLabel();
    private final SpinnerNumberModel modelRowsPerPlot = new SpinnerNumberModel(1, 1, MAX_ROWS_PER_PLOT, 10);
    private final SpinnerNumberModel modelEntryCount = new SpinnerNumberModel(1, 1, MAX_ENTRIES, 10);
    private final SpinnerNumberModel modelRepCount = new SpinnerNumberModel(1, 1, MAX_REPLICATIONS, 1);

    private final SpinnerNumberModel modelPercentSpatial = new SpinnerNumberModel(10.0, 0.0, 50.0, 1.0);
    private final JCheckBox spatialChecksOption = new JCheckBox("% spatial checks:", true);

    private final Map<SpinnerNumberModel,DesignParam> designParamBySource = new HashMap<>();

    private final Action calculate = new AbstractAction("Calc") {
        @Override
        public void actionPerformed(ActionEvent e) {
            FieldDimensionsDialog dlg = new FieldDimensionsDialog(
                    GuiUtil.getOwnerWindow(DesignParametersPanel.this),
                    "Row Parameters");
            dlg.setLocationRelativeTo(calcButton);
            dlg.setVisible(true);
        }
    };
    private JButton calcButton = new JButton(calculate);

    private final Action calcRowCols = new AbstractAction("CxR") {
        @Override
        public void actionPerformed(ActionEvent e) {
            recalculate();
        }
    };

    private final ChangeListener spinnerModelChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            SpinnerNumberModel m = (SpinnerNumberModel) e.getSource();

            Number value = m.getNumber();
            DesignParam designParam = designParamBySource.get(m);

            fireDesignParamChanged(designParam, value);

            recalculate();
        }
    };

    private final FileDrop fileDrop = new FileDrop() {
        @Override
        public void dropFiles(Component component, List<File> files, DropLocationInfo dli) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    File file = files.get(0);
                    if (fileAcceptable==null || fileAcceptable.test(file)) {
                        importHandler.accept(file);
                        return;
                    }
                    MsgBox.warn(DesignParametersPanel.this, "Only Excel, CSV and TXT files supported", "Can't Import");
                }
            });
        }
    };

    private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

    private final JLabel totalEntriesField  = new JLabel();
    private final JLabel spatialChecksPerRepField = new JLabel();
    private final JLabel spatialChecksTotalField = new JLabel();

    private final JLabel plotsPerRepField    = new JLabel();
    private final JLabel totalPlotsField    = new JLabel();

    private final JLabel dropLabel = new JLabel("<HTML>Drop Entry List<BR>files here<BR>(Excel, CSV or TXT)");
    private final Consumer<File> importHandler;
    private final Predicate<File> fileAcceptable;
    private final JSpinner entryCountSpinner;

    private final JLabel entryCountLabel = new JLabel(N_ENTRIES);
    private JSpinner spatialChecksSpinner;
    private JLabel spatialPerRepLabel;
    private JLabel totalSpatialsLabel;

    private final List<JComponent> spatialComponents = new ArrayList<>();

    public DesignParametersPanel(boolean forTrial) {
        this(forTrial, null, null);
    }

    public DesignParametersPanel(boolean forTrial, Predicate<File> fileAcceptable, Consumer<File> importHandler) {

        this.fileAcceptable = fileAcceptable;
        this.importHandler = importHandler;
        zeroSpatialWarning.setForeground(Color.RED);

        List<JLabel> labelsToBold = new ArrayList<>();
        Collections.addAll(labelsToBold, totalEntriesField, spatialChecksTotalField, totalPlotsField);

        addSpatialComponent(spatialChecksPerRepField, spatialChecksTotalField);


        int nWide = 3;
        GBH gbh = new GBH(this, 1,2,0,2);
        int y = 0;

        gbh.add(0,y, nWide,1, GBH.HORZ, 1,1, GBH.CENTER, GuiUtil.createLabelSeparator(DESIGN_PARAMS_HEADING));
        ++y;

        if (RunMode.getRunMode().isDeveloper()) {
            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "# rows per plot:");
            gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, makeSpinner(modelRowsPerPlot, DesignParam.ROWS_PER_PLOT));
            gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.WEST, calcButton);
            ++y;

            // = = = = = = = = = = = = = = =
            // Don't use JSeparator because they vanish from sight if panel is made too short
            gbh.add(0,y, nWide,1, GBH.HORZ, 1,1, GBH.CENTER, GuiUtil.createLabelSeparator(""));
            ++y;
        }

        // = = = = = = = = = = = = = = =

        entryCountSpinner = makeSpinner(modelEntryCount, DesignParam.ENTRY_COUNT);
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, entryCountLabel);
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, entryCountSpinner);
        if (importHandler != null) {
            dropLabel.setOpaque(true);
//          dropLabel.setBorder(new LineBorder(Color.BLUE));
            dropLabel.setHorizontalAlignment(JLabel.CENTER);
            dropLabel.setFont(dropLabel.getFont().deriveFont(Font.ITALIC));
            dropLabel.setBackground(new Color(200,200,200)); // Color.LIGHT_GRAY); // new Color(96, 96, 96));
            dropLabel.setTransferHandler(flth);
            gbh.add(2,y, 1,9, GBH.BOTH, 1,1, GBH.CENTER, dropLabel);
        }
        ++y;

        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "# Replicates/Site:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, makeSpinner(modelRepCount, DesignParam.REP_COUNT));
        ++y;
        if (forTrial) {
            spatialChecksSpinner = makeSpinner(modelPercentSpatial, DesignParam.SPATIAL_CHECKS);
            addSpatialComponent(spatialChecksSpinner);
            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, spatialChecksOption);
            gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, spatialChecksSpinner);
            ++y;
        }
        else {
            modelPercentSpatial.setValue(0);
        }

        // = = = = = = = = = = = = = = =
        gbh.add(0,y, nWide - (importHandler != null ? 1 : 0),1, GBH.HORZ, 1,1, GBH.CENTER, GuiUtil.createLabelSeparator(""));
        ++y;
        // = = = = = = = = = = = = = = =

        labelsToBold.add(gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Total Entries:"));
        gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.WEST, totalEntriesField);
        ++y;

        gbh.add(1,y, nWide-1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSeparator(JSeparator.HORIZONTAL));
        ++y;

        if (forTrial) {
            spatialPerRepLabel = gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "# Spatial Checks per Rep:");
            addSpatialComponent(spatialPerRepLabel);
            gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.WEST, spatialChecksPerRepField);
            gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.WEST, zeroSpatialWarning);
            ++y;
            totalSpatialsLabel = gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "# Total Spatial Checks:");
            labelsToBold.add(totalSpatialsLabel);
            addSpatialComponent(totalSpatialsLabel);
            gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.WEST, spatialChecksTotalField);
            ++y;
            gbh.add(1,y, nWide-1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSeparator(JSeparator.HORIZONTAL));
            ++y;
        }
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Plots Per Replicate:");
        gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.WEST, plotsPerRepField);
        ++y;
        labelsToBold.add(gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Total Plots:"));
        gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.WEST, totalPlotsField);
        ++y;
        gbh.add(1,y, nWide-1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSeparator(JSeparator.HORIZONTAL));
        ++y;


        Font bold = null;
        for (JLabel lbl : labelsToBold) {
            if (bold == null) {
                bold = lbl.getFont().deriveFont(Font.BOLD);
            }
            lbl.setFont(bold);
        }

        initialiseSpatialCheckFonts();

        spatialChecksOption.addActionListener(
                (e) -> handleSpatialChecksOptionChanged());

        recalculate();
    }

    private final Map<JComponent, Font[]> fontsByComponent = new HashMap<>();

    private void initialiseSpatialCheckFonts() {
        Map<AttributedCharacterIterator.Attribute, Object> strikeThroughAttributes
            = new HashMap<>();
        strikeThroughAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);

        for (JComponent c : spatialComponents) {
            Font normal = c.getFont();
            Font strike = normal.deriveFont(strikeThroughAttributes);

            fontsByComponent.put(c, new Font[] { normal, strike });
        }
    }

    private void addSpatialComponent(JComponent ... components) {
        if (components != null) {
            Collections.addAll(spatialComponents, components);
        }
    }

    private void handleSpatialChecksOptionChanged() {
        boolean enb = spatialChecksOption.isSelected();
        for (JComponent c : spatialComponents) {
            c.setEnabled(enb);
            Font[] fonts = fontsByComponent.get(c);
            if (fonts != null) {
                c.setFont(enb ? fonts[0] : fonts[1]);
            }
        }
        recalculate();
        fireDesignParamChanged(DesignParam.SPATIAL_CHECKS, modelPercentSpatial.getNumber());
    }

    public int getRowsPerPlot() {
        return modelRowsPerPlot.getNumber().intValue();
    }
    public int getEntryCount() {
        return modelEntryCount.getNumber().intValue();
    }
    public int getReplicateCount() {
        return modelRepCount.getNumber().intValue();
    }
    public double getPercentSpatialChecks() {
        if (spatialChecksOption.isSelected()) {
            return modelPercentSpatial.getNumber().doubleValue();
        }
        return 0;
    }

    public void addDesignParamChangeListener(DesignParamChangeListener l) {
        listenerList.add(DesignParamChangeListener.class, l);
    }

    public void removeDesignParamChangeListener(DesignParamChangeListener l) {
        listenerList.remove(DesignParamChangeListener.class, l);
    }

    protected void fireDesignParamChanged(DesignParam designParam, Number value) {

        for (DesignParamChangeListener l : listenerList.getListeners(DesignParamChangeListener.class)) {
            switch (designParam) {
            case ENTRY_COUNT:
                l.entryCountChanged(this, value.intValue());
                break;
            case REP_COUNT:
                l.replicateCountChanged(this, value.intValue());
                break;
            case ROWS_PER_PLOT:
                l.rowsPerPlotChanged(this, value.intValue());
                break;
            case SPATIAL_CHECKS:
                l.spatialChecksChanged(this, value.doubleValue());
                break;
            default:
                break;
            }
        }
    }

    private JSpinner makeSpinner(SpinnerNumberModel m, DesignParam designParam) {

//        nameByModel.put(m, designParam.name());

        m.addChangeListener(spinnerModelChangeListener);
        JSpinner s;
        if (designParam.isInteger()) {
            s = new NumberSpinner(m, "#");
        }
        else {
            s = new NumberSpinner(m, "#.0");
        }
        designParamBySource.put(m, designParam);
        return s;
    }


    private void recalculate() {

        DesignParams designParams = getDesignParams();

        int replicateCount = getReplicateCount();

        int totalEntries = designParams.entryCount * replicateCount;
        int totalSpatialChecks = designParams.nSpatials * replicateCount;
        int totalPlots = designParams.plotsPerReplicate * replicateCount;

        totalEntriesField.setText(Integer.toString(totalEntries));
        spatialChecksPerRepField.setText(Integer.toString(designParams.nSpatials));
        spatialChecksTotalField.setText(Integer.toString(totalSpatialChecks));
        if (designParams.percentSpatials  > 0 && designParams.nSpatials <= 0) {
            zeroSpatialWarning.setText("*** NO SPATIAL CHECKS ***");
        }
        else {
            zeroSpatialWarning.setText("");
        }
        plotsPerRepField.setText(Integer.toString(designParams.plotsPerReplicate));
        totalPlotsField.setText(Integer.toString(totalPlots));
//        fireStateChanged();
    }

    public int getSpatialChecksCountPerReplicate() {
        double psc = getPercentSpatialChecks();
        int nSpatialChecksPerReplicate = (int) Math.floor(getEntryCount() * psc / 100.0);
        return nSpatialChecksPerReplicate;
    }

    public void setDesignEntries(List<? extends DesignEntry> list) {
        designEntries = list;
        if (designEntries==null) {
            modelEntryCount.setValue(1);
            entryCountSpinner.setEnabled(true);
            entryCountLabel.setText(N_ENTRIES);
            entryCountLabel.setForeground(Color.BLACK);
        }
        else {
            modelEntryCount.setValue(designEntries.size());
            if (designEntries.size() <= 0) {
                entryCountSpinner.setEnabled(true);
                entryCountLabel.setText(N_ENTRIES);
                entryCountLabel.setForeground(Color.BLACK);
            }
            else {
                entryCountSpinner.setEnabled(false);
                entryCountLabel.setForeground(Color.RED);
                entryCountLabel.setText(N_ENTRIES_FROM_ENTRY_LIST);
            }
        }
    }

    public DesignParams getDesignParams() {
        return new DesignParams(getEntryCount(), getPercentSpatialChecks(),
                getReplicateCount());
    }

}
