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
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;

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
class ChangePlantingBlockPositionDialog extends JDialog {
    @SuppressWarnings("rawtypes")
    private final PlantingBlockTableModel trialBlockModel;
    @SuppressWarnings("rawtypes")
    private final PlantingBlock plantingBlock;

    private final SpinnerNumberModel xCoordModel;
    private final SpinnerNumberModel yCoordModel;

    private Integer minimumCellCount;
    private ChangeListener rowColChangeListener = new ChangeListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void stateChanged(ChangeEvent e) {

            if (xCoordModel == e.getSource()) {
                int x = xCoordModel.getNumber().intValue();
                plantingBlock.setX(x);

            }
            else {
                int y = yCoordModel.getNumber().intValue();
                plantingBlock.setY(y);
            }
            trialBlockModel.blockChanged(WhatChanged.POSITION, plantingBlock);
        }
    };

    private Point initialPosition;
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
            plantingBlock.setX(initialPosition.x);
            plantingBlock.setY(initialPosition.y);
            trialBlockModel.blockChanged(WhatChanged.POSITION, plantingBlock);
            dispose();
        }
    };

    ChangePlantingBlockPositionDialog(Window owner,
            PlantingBlockTableModel<?> ptbm,
            PlantingBlock<?> tb,
            Point maxFieldCoord)
    {
        super(owner, "Move " + tb.getName(), ModalityType.MODELESS);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.trialBlockModel = ptbm;
        this.plantingBlock = tb;
        initialPosition = new Point(plantingBlock.getX(), plantingBlock.getY());

        // TODO check if there is a range problem
        //  (in the case when the PlantingBlock is way out of position
        int maxXcoord = maxFieldCoord.x - plantingBlock.getColumnCount();
        int maxYcoord = maxFieldCoord.y - plantingBlock.getRowCount();
        xCoordModel = new SpinnerNumberModel(initialPosition.x, 0, maxXcoord, 1);
        yCoordModel = new SpinnerNumberModel(initialPosition.y, 0, maxYcoord, 1);

        minimumCellCount = plantingBlock.getMinimumCellCount();
        if (minimumCellCount != null) {
            // We need to limit things
            xCoordModel.addChangeListener(rowColChangeListener);
            yCoordModel.addChangeListener(rowColChangeListener);
        }

        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel, 0,1,0,1);
        int y = 0;
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "X:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSpinner(xCoordModel));
        ++y;
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Y:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSpinner(yCoordModel));
        ++y;

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
        int x = xCoordModel.getNumber().intValue();
        int y = yCoordModel.getNumber().intValue();

        plantingBlock.setX(x);
        plantingBlock.setY(y);

        trialBlockModel.blockChanged(WhatChanged.POSITION, plantingBlock);
    }

}
