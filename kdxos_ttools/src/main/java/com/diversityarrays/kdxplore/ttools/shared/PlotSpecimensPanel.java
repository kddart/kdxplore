/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.ttools.shared;

import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.event.ListSelectionListener;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.dal.DalSpecimen;

public interface PlotSpecimensPanel {

    boolean SORTED = true;
    boolean NOT_SORTED = false;

    void setSelectedColumnHeading(String hdg);
    
    void setPlotAttributeProvider(PlotAttributesProvider  pAp);

    void setEnableTable(boolean enable);

    void clearPlotSelection();

    int getPlotCount();

    void setTrial(Trial t);

    void setPlots(List<Plot> plots);

    void setSpecimensByPlot(Map<Plot, List<DalSpecimen>> map);
    

    void addPlotSelectionListener(ListSelectionListener l);

    void addPlotsChangedListener(PlotsChangedListener l);

    void removePlotsChangedListener(PlotsChangedListener l);

    List<Plot> getSelectedPlots(Consumer<List<Integer>> rowsConsumer);

    List<Plot> updateAllCompletedHarvestItems(boolean areHarvested);

    int getRowForPlot(Plot plot);

    void setPlotHarvested(Plot plot, boolean isHarvested);

    void updatePlotTableView(List<Integer> rows);

    void setSelectedPlot(Plot plot);

    Plot getSelectedPlot();

    List<Plot> getPlots();

    Component getUiComponent();

}
