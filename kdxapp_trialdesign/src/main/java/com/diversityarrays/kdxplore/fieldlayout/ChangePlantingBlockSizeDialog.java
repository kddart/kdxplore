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
import java.awt.Dimension;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JLabel;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;

import net.pearcan.util.GBH;

@SuppressWarnings({"nls", "rawtypes"})
class ChangePlantingBlockSizeDialog extends ChangeDimensionDialog {


    private PlantingBlock plantingBlock;
//    private Integer minimumCellCount;
//    private ChangeListener rowColChangeListener = new ChangeListener() {
//        boolean changing = false;
//        @SuppressWarnings("unchecked")
//        @Override
//        public void stateChanged(ChangeEvent e) {
//            if (changing) {
//                return;
//            }
//            int wid = modelColumnCount.getNumber().intValue();
//            int hyt = modelRowCount.getNumber().intValue();
//            if (modelColumnCount == e.getSource()) {
//                // wid changed by user
//                while ((wid * hyt) < minimumCellCount) {
//                    ++hyt;
//                }
//                changing = true;
//                try {
//                    modelRowCount.setValue(hyt);
//                }
//                finally {
//                    changing = false;
//                }
//            }
//            else {
//                // hyt changed by user
//                while ((wid * hyt) < minimumCellCount) {
//                    ++wid;
//                }
//                changing = true;
//                try {
//                    modelColumnCount.setValue(wid);
//                }
//                finally {
//                    changing = false;
//                }
//            }
//
//        }
//    };

//    private Dimension initialBlockSize;

    private PlantingBlockTableModel trialBlockModel;

    private JLabel fillerCount;

    private final Consumer<Dimension> applyChanges = new Consumer<Dimension>() {

        @Override
        public void accept(Dimension t) {
            // TODO Auto-generated method stub

        }
    };

    @SuppressWarnings("unchecked")
    ChangePlantingBlockSizeDialog(Window owner, PlantingBlockTableModel tbm, PlantingBlock tb) {
        super(owner, "Resize " + tb.getName() + " (will soon be available via corner drag)",
                ModalityType.MODELESS,
                new Dimension(tb.getColumnCount(), tb.getRowCount()),
                tb.getMinimumCellCount(),
                (sz) -> {
                    tb.setFinalColumnCount(sz.width);
                    tb.setFinalRowCount(sz.height);
                    tbm.blockChanged(WhatChanged.DIMENSION, tb);
                }
                );


        this.trialBlockModel = tbm;
        this.plantingBlock = tb;
//        initialBlockSize = new Dimension(plantingBlock.getColumnCount(), plantingBlock.getRowCount());
//
//
//        minimumCellCount = plantingBlock.getMinimumCellCount();

    }

    @Override
    protected int addExtraRow(GBH gbh, int y) {
        if (fillerCount == null) {
            fillerCount = new JLabel();
            fillerCount.setForeground(Color.RED);
            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, Heading.EXCESS.csvHeading + ":");
            gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, fillerCount);
            ++y;
        }
        return y;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void applyChanges(int wid, int hyt) {
        Integer minCellCount = plantingBlock.getMinimumCellCount();
        if (minCellCount != null) {
            while ((wid * hyt) < minCellCount) {
                ++hyt;
                ++wid;
            }
        }

        plantingBlock.setTemporaryColumnCount(wid);
        plantingBlock.setTemporaryRowCount(hyt);
        Optional<Integer> fc_opt = plantingBlock.getFillerCount();
        fillerCount.setText(fc_opt.isPresent() ? fc_opt.get().toString() : "");
        trialBlockModel.blockChanged(WhatChanged.DIMENSION, plantingBlock);
    }
}
