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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;

import net.pearcan.util.GBH;

public class ApplyToPanel extends JPanel {
    
    private final ApplyTo applyTo = new ApplyTo();
    
    private final JCheckBox applyToCuratedCheckBox = new JCheckBox(Vocab.CBOX_CURATED(), applyTo.curated);
    
    private final JCheckBox applyToExcludedOption = new JCheckBox(Vocab.CBOX_SUPPRESSED_SAMPLES(), applyTo.excluded);

    private final JCheckBox applyToIncludedOption = new JCheckBox(Vocab.CBOX_ACCEPTED_SAMPLES(), applyTo.included);

    private final JCheckBox applyToUncuratedCheckBox = new JCheckBox(Vocab.CBOX_UNCURATED(), applyTo.uncurated);

    private final Action applyToHelpAction = new AbstractAction(Vocab.ACTION_HELP()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            MsgBox.info(ApplyToPanel.this,
                    "<HTML>" + Vocab.HTML_HELP_SET_MULTIPLE_VALUES(), //$NON-NLS-1$
                    Vocab.TITLE_HELP_MULTIPLE_VALUES());
        }
    };

    
    public ApplyToPanel() {

        KDClientUtils.initAction(ImageId.HELP_24, applyToHelpAction, null);


        applyTo.curatedType.addCheckBox(applyToCuratedCheckBox);
        applyTo.excludedType.addCheckBox(applyToExcludedOption);
        applyTo.includedType.addCheckBox(applyToIncludedOption);
        applyTo.uncuratedType.addCheckBox(applyToUncuratedCheckBox);


        applyToCuratedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enable = applyToCuratedCheckBox.isSelected();
                applyTo.excludedType.setEnabled(enable);
                applyTo.includedType.setEnabled(enable);
            }
        });
        if (! applyToCuratedCheckBox.isSelected()) {
            applyToCuratedCheckBox.doClick();
        }

        applyToCuratedCheckBox.setToolTipText(Vocab.TOOLTIP_APPLY_TO_CURATED_SAMPLES());
        applyToExcludedOption.setToolTipText(Vocab.TOOLTIP_CHANGE_SUPPRESSED_SAMPLES());
        applyToIncludedOption.setToolTipText(Vocab.TOOLTIP_CHANGE_ACCEPTED_SAMPLES());

        applyToUncuratedCheckBox.setToolTipText(Vocab.TOOLTIP_APPLY_TO_UNCURATED_SAMPLES());
        
        Box two = Box.createHorizontalBox();
        two.add(applyToExcludedOption);
        two.add(applyToIncludedOption);
        two.setBorder(BorderFactory.createDashedBorder(Color.GRAY));

        GBH gbh = new GBH(this, 1,1,0,0);
        int y = 0;
        
        Box applyToBox = Box.createHorizontalBox();
        applyToBox.add(applyToCuratedCheckBox);
        applyToBox.add(new JLabel(": ")); //$NON-NLS-1$
        applyToBox.add(two);
        applyToBox.add(Box.createHorizontalGlue());
        applyToBox.add(new JButton(applyToHelpAction));

        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, applyToBox);
        ++y;
        
        gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, applyToUncuratedCheckBox);
        ++y;
    }


    public ApplyTo getApplyTo() {
        return applyTo;
    }
}
