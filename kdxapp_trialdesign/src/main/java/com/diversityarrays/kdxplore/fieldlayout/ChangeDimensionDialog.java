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
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

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

import net.pearcan.util.GBH;

@SuppressWarnings("nls")
public abstract class ChangeDimensionDialog extends JDialog {

    static final int MAX_ROW_COUNT = 10000;
    static final int MAX_COLUMN_COUNT = 10000;

    private final SpinnerNumberModel modelColumnCount;
    private final SpinnerNumberModel modelRowCount;

    private ChangeListener rowColChangeListener = new ChangeListener() {
        boolean changing = false;
        @Override
        public void stateChanged(ChangeEvent e) {
            if (changing) {
                return;
            }
            int wid = modelColumnCount.getNumber().intValue();
            int hyt = modelRowCount.getNumber().intValue();
            if (modelColumnCount == e.getSource()) {
                // wid changed by user
                while ((wid * hyt) < minimumCellCount) {
                    ++hyt;
                }
                changing = true;
                try {
                    modelRowCount.setValue(hyt);
                }
                finally {
                    changing = false;
                }
            }
            else {
                // hyt changed by user
                while ((wid * hyt) < minimumCellCount) {
                    ++wid;
                }
                changing = true;
                try {
                    modelColumnCount.setValue(wid);
                }
                finally {
                    changing = false;
                }
            }

            applyChanges(wid, hyt);
        }
    };

    private final Action applyAction = new AbstractAction("Apply") {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };

    private final Action cancelAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Undo changes
            undoChanges.accept(initialSize);
            dispose();
        }
    };
    private Integer minimumCellCount;
    private Dimension initialSize;
    private final Consumer<Dimension> undoChanges;

    public ChangeDimensionDialog(
            Window owner,
            String title,
            ModalityType modalityType,
            Dimension initialSize,
            Integer minimumCellCount,
            Consumer<Dimension> undoChanges
            )
    {
        super(owner, title, modalityType);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.undoChanges = undoChanges;
        modelColumnCount = new SpinnerNumberModel(initialSize.width, 1, MAX_COLUMN_COUNT, 1);
        modelRowCount = new SpinnerNumberModel(initialSize.height, 1, MAX_ROW_COUNT, 1);

        this.initialSize = initialSize;
        this.minimumCellCount = minimumCellCount;

        if (minimumCellCount != null) {
            // We need to limit things
            modelColumnCount.addChangeListener(rowColChangeListener);
            modelRowCount.addChangeListener(rowColChangeListener);
        }

        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel, 0,1,0,1);
        int y = 0;
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Width:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSpinner(modelColumnCount));
        ++y;
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Height:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, new JSpinner(modelRowCount));
        ++y;
        y = addExtraRow(gbh, y);

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

    protected int addExtraRow(GBH gbh, int y) {
        return y;
    }

    abstract protected void applyChanges(int wid, int hyt);
}
