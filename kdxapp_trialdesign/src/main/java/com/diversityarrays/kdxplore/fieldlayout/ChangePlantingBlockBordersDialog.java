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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;

import net.pearcan.util.GBH;

@SuppressWarnings("nls")
class ChangePlantingBlockBordersDialog extends JDialog {

    @SuppressWarnings("rawtypes")
    private final PlantingBlockTableModel trialBlockModel;
    @SuppressWarnings("rawtypes")
    private final PlantingBlock plantingBlock;

    private final Action applyAction = new AbstractAction("Apply") {
        @Override
        public void actionPerformed(ActionEvent e) {
            applyChanges();
            dispose();
        }
    };

    private final Action cancelAction = new AbstractAction("Cancel") {
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {
            // Undo changes
            for (Side side : Side.values()) {
                plantingBlock.setBorder(side,
                        initialBorderCounts[side.ordinal()]);
            }
            trialBlockModel.blockChanged(WhatChanged.BORDER, plantingBlock);
            dispose();
        }
    };

    private final int[] initialBorderCounts;
    private final Map<SpinnerNumberModel, Side> sideByModel = new HashMap<>();
    private final ChangeListener changeListener = new ChangeListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void stateChanged(ChangeEvent e) {
            Object src = e.getSource();
            if (src instanceof SpinnerNumberModel) {
                SpinnerNumberModel snm = (SpinnerNumberModel) src;
                Side side = sideByModel.get(snm);
                int count = snm.getNumber().intValue();
                plantingBlock.setBorder(side, count);

                trialBlockModel.blockChanged(WhatChanged.BORDER, plantingBlock);

            }
        }
    };

    ChangePlantingBlockBordersDialog(Window owner,
            PlantingBlockTableModel<?> pbtm,
            PlantingBlock<?> pb)
    {
        super(owner, "Set Borders for " + pb.getName(), ModalityType.MODELESS);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.trialBlockModel = pbtm;
        this.plantingBlock = pb;

        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel, 0,1,0,1);
        int y = 0;

        initialBorderCounts= plantingBlock.getBorderCountBySide();
        for (Side side : Side.values()) {
            SpinnerNumberModel model = new SpinnerNumberModel(
                    initialBorderCounts[side.ordinal()],
                    0, Side.MAX_BORDER_COUNT, 1);
            sideByModel.put(model, side);
            model.addChangeListener(changeListener);
            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, side.displayName);
            gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, new JSpinner(model));
            ++y;
        }

        JButton applyButton = new JButton(applyAction);

        Box btns = Box.createHorizontalBox();
        btns.add(Box.createHorizontalGlue());
        btns.add(new JButton(cancelAction));
        btns.add(applyButton);

        Container cp = getContentPane();
        cp.add(panel, BorderLayout.CENTER);
        cp.add(btns, BorderLayout.SOUTH);

        pack();

        getRootPane().setDefaultButton(applyButton);

    }

    @SuppressWarnings("unchecked")
    private void applyChanges() {
        BiConsumer<Side, SpinnerNumberModel> action = new BiConsumer<Side, SpinnerNumberModel>() {
            @Override
            public void accept(Side side, SpinnerNumberModel model) {
                int count = model.getNumber().intValue();
                plantingBlock.setBorder(side, count);
            }
        };
        sideByModel.entrySet().stream()
            .forEach(e -> action.accept(e.getValue(), e.getKey()));
        trialBlockModel.blockChanged(WhatChanged.BORDER, plantingBlock);
    }

}
