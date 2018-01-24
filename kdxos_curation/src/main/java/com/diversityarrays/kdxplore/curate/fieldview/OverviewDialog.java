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
package com.diversityarrays.kdxplore.curate.fieldview;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.CurationTableModel;
import com.diversityarrays.kdxplore.curate.TraitInstanceCellRenderer;
import com.diversityarrays.kdxplore.curate.fieldview.Overview.OverviewInfoProvider;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.field.FieldLayoutTableModel;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

import net.pearcan.ui.GuiUtil;

@SuppressWarnings("nls")
public class OverviewDialog extends JDialog {

    private Overview overview;

    
    private final ListSelectionListener listSelectionListener = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (! e.getValueIsAdjusting()) {
                overview.repaint();
            }
        }
    };


    private FieldViewSelectionModel fieldViewSelectionModel;
    
    @SuppressWarnings("unchecked")
    public OverviewDialog(Window window, 
            String title,
            
            @SuppressWarnings("rawtypes") ComboBoxModel comboBoxModel,
            CurationData curationData,
            Transformer<TraitInstance, String> tiNameProvider,
            OverviewInfoProvider overviewInfoProvider, 
            FieldViewSelectionModel fvsm,
            FieldLayoutTableModel fltm, 
            CurationTableModel ctm
            )
    {
        super(window, title, ModalityType.MODELESS);
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);

        this.fieldViewSelectionModel = fvsm;
        
        @SuppressWarnings({ "rawtypes" })
        JComboBox activeTiCombo = new JComboBox(comboBoxModel);
        TraitInstanceCellRenderer tiCellRenderer = new TraitInstanceCellRenderer(
                curationData.getTraitColorProvider(), tiNameProvider);
        activeTiCombo.setRenderer(tiCellRenderer);

        JLabel infoLabel = new JLabel(" ");
        infoLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        
        final JFrame[] helpDialog = new JFrame[1];
        Action helpAction = new AbstractAction("?") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (helpDialog[0] != null) {
                    GuiUtil.restoreFrame(helpDialog[0]);
                }
                else {
                    JFrame f = new JFrame("Overview Help");
                    helpDialog[0] = f;
                    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    f.setAlwaysOnTop(true);
                    f.setLocationRelativeTo(overview);
                    String html = Overview.getOverviewHelpHtml();
                    JLabel label = new JLabel(html);
                    label.setBorder(new EmptyBorder(0, 10, 0, 10));
                    f.setContentPane(label);
                    f.pack();
                    f.setVisible(true);
                }
            }
        };
        
//        Window window = GuiUtil.getOwnerWindow(FieldLayoutViewPanel.this);
        if (window != null) {
            if (window instanceof JFrame) {
                System.out.println("Found window: " + ((JFrame) window).getTitle());
            }
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    window.removeWindowListener(this);
                    if (helpDialog[0] != null) {
                        helpDialog[0].dispose();
                    }
                }
            });
        }
        
        KDClientUtils.initAction(ImageId.HELP_24, helpAction, "Help for Overview");
        Box top = Box.createHorizontalBox();
        top.add(activeTiCombo);
        top.add(new JButton(helpAction));
        
        overview = new Overview(overviewInfoProvider, 
                fltm, 
                curationData, 
                ctm,
                infoLabel);
        overview.setActiveTraitInstance(fvsm.getActiveTraitInstance(true));
        Container cp = getContentPane();
        
        cp.add(top, BorderLayout.NORTH);
//      cp.add(traitLabel, BorderLayout.NORTH);
        cp.add(infoLabel, BorderLayout.SOUTH);
        cp.add(overview, BorderLayout.CENTER);
        
        pack();
//      setResizable(false);
        
//      setLocationRelativeTo(showOverviewButton);
        // DEFAULT POSITION is "out of the way"
        setVisible(true);
        
        this.fieldViewSelectionModel.addListSelectionListener(listSelectionListener);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                toFront();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                fieldViewSelectionModel.removeListSelectionListener(listSelectionListener);
                removeWindowListener(this);
            }
        });
    }

}
