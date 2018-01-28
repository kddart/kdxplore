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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.dal.DalSpecimen;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;

public class PlotSpecimensPanelImpl extends JPanel implements PlotSpecimensPanel {
    
    private  PlotTableModel plotTableModel  = null; 
    private  JTable plotTable = null;
    
    private final JScrollPane scrollPane;
    private Trial trial;
    protected boolean needNewSpecimenField;
    
    public PlotSpecimensPanelImpl(boolean sorted, boolean needNewSpecimenField) {
        this(null, null, sorted,needNewSpecimenField);
    }

    public PlotSpecimensPanelImpl(String title, String messageIfEmpty, boolean sorted, boolean needNewSpecimenField) {
        super(new BorderLayout());
        
        plotTableModel = new PlotTableModel(needNewSpecimenField);
        plotTable = new JTable(plotTableModel);
        // The "Plot Identifier" columns are all Integer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        plotTable.setDefaultRenderer(Integer.class, centerRenderer);
        this.needNewSpecimenField  = needNewSpecimenField;
        
        plotTable.setAutoCreateRowSorter(sorted);
        plotTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        if (messageIfEmpty == null) {
            this.scrollPane = new JScrollPane(plotTable);
        }
        else {
            this.scrollPane = new PromptScrollPane(plotTable, messageIfEmpty);
        }
        if (title != null) {
            add(GuiUtil.createLabelSeparator(title), BorderLayout.NORTH);
        }
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public Component getUiComponent() {
        return this;
    }

    @Override
    public void setSelectedColumnHeading(String hdg) {
        plotTableModel.showSelectedColumn(hdg);
    }

    @Override
    public void setEnableTable(boolean enable) {
        plotTable.setEnabled(enable);
    }

    @Override
    public void clearPlotSelection() {
        plotTable.clearSelection();
    }

    @Override
    public int getPlotCount() {
        return plotTableModel.getRowCount();
    }

    @Override
    public void setTrial(Trial t) {
        this.trial = t;
        plotTableModel.setTrial(trial);
    }

    @Override
    public void setPlots(List<Plot> plots) {
        plotTableModel.setPlots(plots);
    }

    @Override
    public void setSpecimensByPlot(Map<Plot, List<DalSpecimen>> map) {
        plotTableModel.setSpecimensByPlot(map);
    }

    @Override
    public void addPlotSelectionListener(ListSelectionListener l) {
        plotTable.getSelectionModel().addListSelectionListener(l);
    }
    
    @Override
    public void addPlotsChangedListener(PlotsChangedListener l) {
        listenerList.add(PlotsChangedListener.class, l);
    }
    
    @Override
    public void removePlotsChangedListener(PlotsChangedListener l) {
        listenerList.remove(PlotsChangedListener.class, l);
    }

    @Override
    public List<Plot> getSelectedPlots(Consumer<List<Integer>> rowsConsumer) {
        
        List<Plot> selectedPlots = new ArrayList<>();
        
        List<Integer> rows = GuiUtil.getSelectedModelRows(plotTable);

        for (Integer row : rows) {
            Plot plot = plotTableModel.getPlotAt(row);
            if (plot != null) {
                selectedPlots.add(plot);
            }
        }

        if (rowsConsumer != null) {
            rowsConsumer.accept(rows);
        }
        return selectedPlots;
    }
    
    @Override
    public List<Plot> updateAllCompletedHarvestItems(boolean areHarvested) {
        List<Plot> plotsHarvested = plotTableModel.getPlots();

        List<Integer> rows = new ArrayList<>();
        for (Plot p : plotsHarvested) {
            setPlotHarvested(p, true); // ??? TODO should this be "areHarvested"?
            rows.add(getRowForPlot(p));
        }
        
        updatePlotTableView(rows);
        return plotsHarvested;
    }

    @Override
    public int getRowForPlot(Plot plot) {
        return plotTableModel.getRowAt(plot);
    }
    
    @Override
    public void setPlotHarvested(Plot plot, boolean isHarvested) {
        plotTableModel.setPlotHarvested(plot, isHarvested);
    }
    
    @Override
    public void updatePlotTableView(List<Integer> rows) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Integer row : rows) {
                    plotTableModel.fireTableCellUpdated(row , 0);
                }
            }
        });
    }

    @Override
    public void setSelectedPlot(Plot plot) {
        if (plot != null) {
            int row = plotTableModel.getRowAt(plot);
            plotTable.setRowSelectionInterval(row, row);
        }
    }

    @Override
    public Plot getSelectedPlot() {
        Plot result = null;
        int viewRow = plotTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = plotTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0) {
                result = plotTableModel.getPlotAt(modelRow);
            }
        }
        return result;
    }

    @Override
    public List<Plot> getPlots() {
        return plotTableModel.getPlots();
    }

	@Override
	public void setPlotAttributeProvider(PlotAttributesProvider pAp) {
		plotTableModel.setPlotAttributeProvider(pAp);
		
	}


}
