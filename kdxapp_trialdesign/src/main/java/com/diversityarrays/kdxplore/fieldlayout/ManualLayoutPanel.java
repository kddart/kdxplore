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
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.diversityarrays.kdxplore.design.DesignEntry;
import com.diversityarrays.kdxplore.design.PlantingBlockFactory;

public class ManualLayoutPanel extends JPanel implements PlantingBlockFactory<ReplicateCellContent> {

    public static final boolean FOR_NURSERY = false;
    public static final boolean FOR_TRIAL = true;

    private final DesignParamChangeListener designChangeListener = new DesignParamChangeListener() {

        @Override
        public void entryCountChanged(Object source, int intValue) {
            updateTrialBlocks();
        }

        @Override
        public void rowsPerPlotChanged(Object source, int intValue) {
        }

        @Override
        public void replicateCountChanged(Object source, int intValue) {
            updateTrialBlocks();
        }

        @Override
        public void spatialChecksChanged(Object source, double intValue) {
            updateTrialBlocks();
        }
    };

    private final DesignParametersPanel paramsPanel;

    private final Consumer<DesignParams> totalPlotsChanged;
    private String instructionsHtml = "<HTML>Add <i>Locations</i> using the blue <b>Plus</b> button"
            + "<br>Use the <b>Pencil</b> button to open an editor for the <i>Location</i>"
            + "<HR>"
            + "Support for reusing <i>Location</i> size and details is in the next release";


    public ManualLayoutPanel(boolean forTrial, Consumer<DesignParams> totalPlotsChanged) {
        super(new BorderLayout());

        this.paramsPanel = new DesignParametersPanel(forTrial);
        this.totalPlotsChanged = totalPlotsChanged;

        paramsPanel.addDesignParamChangeListener(designChangeListener);

        add(paramsPanel, BorderLayout.NORTH);
        add(new JLabel(instructionsHtml, JLabel.CENTER), BorderLayout.CENTER);
    }

    private void updateTrialBlocks() {
        DesignParams designParams = paramsPanel.getDesignParams();
        totalPlotsChanged.accept(designParams);
    }

    public void setDesignEntries(List<? extends DesignEntry> list) {
        paramsPanel.setDesignEntries(list);
    }

//    @Deprecated
//    public int getReplicateCount() {
//        return paramsPanel.getReplicateCount();
//    }

    @Override
    public PlantingBlock<ReplicateCellContent> createPlantingBlockFrom(int replicateNumber) {

        DesignParams designParams = paramsPanel.getDesignParams();

        return new PlantingBlock<>(replicateNumber, "Rep#" + replicateNumber, designParams);
    }

    public int getSpatialChecksCountPerReplicate() {
        return paramsPanel.getSpatialChecksCountPerReplicate();
    }

    public double getPercentSpatialChecks() {
        return paramsPanel.getPercentSpatialChecks();
    }

}
