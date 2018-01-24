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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.util.Check;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.renderer.DateCellRenderer;
import net.pearcan.util.GBH;

/**
 * <pre>
 * +-------------------------------------------+
 * |  .--------------------------------------. |
 * |  |                                      | |
 * |  |  Table of Sample Sources showing     | |
 * |  |    a summary of what is in them.     | |
 * |  |                                      | |
 * |  .--------------------------------------. |
 * |                                           |
 * |  .--------------------------------------. |
 * |  | Curated:   [ ] ([ ] Appr [ ] Supp )  | |  -- the ApplyToPanel
 * |  | Uncurated: [ ]                       | |
 * |  .--------------------------------------. |
 * |                                           |
 * | Values For: [ -- Select -- ]  (Set Value) |
 * |  "Select Data Source above ..."           |
 * +-------------------------------------------+
 * </pre>
 * @author brianp
 *
 */
public class SampleSourcesTablePanel extends JPanel {
    
    static private class SelectedTraitInstance {
        private final String displayValue;

        public final TraitInstance traitInstance;

        SelectedTraitInstance(String s, TraitInstance ti) {
            this.displayValue = s;
            this.traitInstance = ti;
        }
        
        @Override
        public String toString() {
            return displayValue;
        }
    }
    
    static private final SelectedTraitInstance SELECT_OPTION = new SelectedTraitInstance("- Select Trait -", null);
    
//    static private final SelectedTraitInstance ALL_SELECTED = new SelectedTraitInstance("Selected Trait Instances", null);
    
    private String getInstanceName(TraitInstance ti) {
        if (ti.trait==null) {
            return ti.toString();
        }
        return traitNameStyle.makeTraitInstanceName(ti, TraitNameStyle.Prefix.EXCLUDE);
    }
    
    private final ApplyToPanel applyToPanel = new ApplyToPanel();
    
    private final JComboBox<SelectedTraitInstance> traitInstanceCombo = new JComboBox<SelectedTraitInstance>();
    
    /**
     * Visible in the "Sample Data" tab in the top left corner.
     */
    private final SampleGroupTableModel sampleGroupTableModel;
    private final JTable sampleGroupTable;


    private final Consumer<TraitInstance> handleSetMultiple;

    private Action setTraitValuesAction = new AbstractAction("Set Values") {
        @Override
        public void actionPerformed(ActionEvent e) {
            SelectedTraitInstance value = (SelectedTraitInstance) traitInstanceCombo.getSelectedItem();
            if (SELECT_OPTION != value) {
                handleSetMultiple.accept(value.traitInstance);
            }
        }
    };

    private final TraitNameStyle traitNameStyle;

    private final CurationData curationData;

    private final CurationTableModel curationTableModel;
    
    public SampleSourcesTablePanel(CurationData cd, CurationTableModel ctm, Consumer<TraitInstance> handleSetMultiple) {
        super(new BorderLayout());
        this.curationData = cd;
        this.traitNameStyle = curationData.getTrial().getTraitNameStyle();
        this.curationTableModel = ctm;
        this.handleSetMultiple = handleSetMultiple;

        // "Device", "Operator", "# Samples", "# Traits"
        sampleGroupTableModel = new SampleGroupTableModel(curationData);
        sampleGroupTable = new JTable(sampleGroupTableModel);

        sampleGroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sampleGroupTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean enable;
                if (GuiUtil.getSelectedModelRows(sampleGroupTable).isEmpty()) {
                    enable = false;
                }
                else {
                    SampleGroup group = sampleGroupTableModel.getSampleGroups()[GuiUtil
                            .getSelectedModelRows(sampleGroupTable).get(0)];
                    if (group.equals(curationData.getDatabaseSampleGroup())) {
                        enable = false;
                    }
                    else {
                        if (curationTableModel.hasAnyTraitInstanceColumns()
                                && 
                                hasTraitInstanceSelected()) 
                        {
                            enable = true;
                        }
                        else {
                            enable = false;
                        }
                    }
                }
                
                setTraitValuesActionEnabled(enable);
            }
        });

        sampleGroupTable.setDefaultRenderer(java.util.Date.class,
                new DateCellRenderer(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")));

        
        traitInstanceCombo.addItem(SELECT_OPTION);
        traitInstanceCombo.setEnabled(false);
        traitInstanceCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectedTraitInstance sti = (SelectedTraitInstance) traitInstanceCombo.getSelectedItem();
                setTraitValuesAction.setEnabled(SELECT_OPTION != sti);
            }
        });

        setTraitValuesAction.setEnabled(false);

        setTraitValuesAction.putValue(Action.SHORT_DESCRIPTION,
                Vocab.TOOLTIP_SET_ALL_VALUES_FOR_SELECTED());
        
        setBorder(new TitledBorder(new LineBorder(Color.BLUE), "Change All Samples"));

        Box box = Box.createHorizontalBox();
        box.add(new JLabel("Values for Trait:"));
        box.add(traitInstanceCombo);
        box.add(new JButton(setTraitValuesAction));

        JLabel label = new JLabel("<html><i>Select Data Source Above to Set Values.</i>", JLabel.CENTER);
        label.setForeground(Color.DARK_GRAY);

        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel);
        int y = 0;

        gbh.add(0, y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, applyToPanel);
        ++y;

        gbh.add(0, y, 1,1, GBH.HORZ, 0, 1, GBH.CENTER, box);
        ++y;

        gbh.add(0, y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, label);
        ++y;
        
        
        add(new JScrollPane(sampleGroupTable), BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);
    }
    
    public void updateTraitInstancesCombo(List<TraitInstance> checkedTraitInstances) {
        int persistedIndex = traitInstanceCombo.getSelectedIndex();
        int itemCount = traitInstanceCombo.getItemCount();

        traitInstanceCombo.removeAllItems();
        traitInstanceCombo.addItem(SELECT_OPTION);
        if (Check.isEmpty(checkedTraitInstances)) {
            traitInstanceCombo.setSelectedIndex(0);
            traitInstanceCombo.setEnabled(false);
        }
        else {
            traitInstanceCombo.setEnabled(true);
//            traitInstanceCombo.addItem(ALL_SELECTED);
            for (TraitInstance ti : checkedTraitInstances) {
                String display = getInstanceName(ti); 
                SelectedTraitInstance sti = new SelectedTraitInstance(display, ti);
                traitInstanceCombo.addItem(sti);
            }

            // Reset selection if selection is smaller than before. Keep if same
            // size
            if (traitInstanceCombo.getItemCount() > persistedIndex
                    && itemCount <= traitInstanceCombo.getItemCount()) 
            {
                traitInstanceCombo.setSelectedIndex(persistedIndex);
            }
        }
    }

    private boolean hasTraitInstanceSelected() {
        return traitInstanceCombo.getSelectedIndex() > 0;
    }

    private void setTraitValuesActionEnabled(boolean enable) {
        setTraitValuesAction.setEnabled(enable);
    }

    public SampleGroup getSelectedSampleGroup() {
        SampleGroup group = null;
        List<Integer> rows = GuiUtil.getSelectedModelRows(sampleGroupTable);
        if (rows.size() == 1) {
            group = sampleGroupTableModel.getSampleGroups()[rows.get(0)];
        }
        return group;
    }
}
