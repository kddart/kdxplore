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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.CurationMenuProvider;
import com.diversityarrays.kdxplore.curate.CurationTableModel;
import com.diversityarrays.kdxplore.curate.Msg;
import com.diversityarrays.kdxplore.curate.PlotCellChoicesPanel;
import com.diversityarrays.kdxplore.curate.SelectedInfo;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.fieldview.Overview.OverviewInfoProvider;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.field.FieldLayoutTableModel;
import com.diversityarrays.kdxplore.ui.CellSelectableTable;
import com.diversityarrays.kdxplore.ui.CellSelectionListener;

import net.pearcan.util.MessagePrinter;

public class InterceptFieldLayoutView implements FieldLayoutView {

    static public interface RefreshListener extends EventListener {
        void refreshRequired(Object source);

        void traitInstanceActivated(Object source, TraitInstance ti);
    }
    
    private FieldLayoutViewPanel viewPanel;

    private ItemListener itemListener = new ItemListener()  {
        @Override
        public void itemStateChanged(ItemEvent e) {
            for (ItemListener l : listenerList.getListeners(ItemListener.class)) {
                l.itemStateChanged(e);
            }
        }
    };

    private CellSelectionListener cellSelectionListener = new CellSelectionListener() {

        @Override
        public void handleChangeEvent(EventType eventType, ListSelectionEvent event) {
            for (CellSelectionListener l : listenerList.getListeners(CellSelectionListener.class)) {
                l.handleChangeEvent(eventType, event);
            }
        }
    };

    private JFrame fieldLayoutFrame = null;

    private RefreshListener refreshListener = new RefreshListener() {
        @Override
        public void refreshRequired(Object source) {
            fireRefreshRequired(source);
        }

        @Override
        public void traitInstanceActivated(Object source, TraitInstance ti) {
            fireTraitInstanceActivated(source, ti);
        }
    };

    public void openFieldLayoutView(
            Component trigger,
            String title,
            @SuppressWarnings("rawtypes") MutableComboBoxModel comboBoxModel, 
            CurationData curationData, 
            CurationTableModel curationTableModel, 
            SelectedValueStore selectedValueStore, 
            PlotCellChoicesPanel plotCellChoicesPanel, 
            JPopupMenu popuMenu,
            Font smallFont, 
            Action curationHelpAction, 
            MessagePrinter messages,
            Closure<String> selectionClosure, 
            CurationContext curationContext, 
            CurationMenuProvider curationMenuProvider,
            
            FieldLayoutTableModel fieldLayoutTableModel,
            CellSelectableTable fieldLayoutTable,
            FieldViewSelectionModel fieldViewSelectionModel,
            
            JButton undockButton)
    {
        if (fieldLayoutFrame != null) {
            fieldLayoutFrame.toFront();
            return;
        }

        JCheckBox alwaysOnTopOption = new JCheckBox(Msg.OPTION_KEEP_ON_TOP(), true);

        viewPanel = new FieldLayoutViewPanel(
                comboBoxModel,
                alwaysOnTopOption,
                curationData, 
                curationTableModel,
                selectedValueStore, 
                plotCellChoicesPanel,
                popuMenu,
                smallFont,
                curationHelpAction,
                messages,
                selectionClosure,
                curationContext,
                curationMenuProvider,
                
                fieldLayoutTableModel,
                fieldLayoutTable,
                fieldViewSelectionModel,
                
                undockButton);
        

        viewPanel.addRefreshListener(refreshListener);

        viewPanel.addTraitInstanceSelectionListener(itemListener);
        viewPanel.addCellSelectionListener(cellSelectionListener);

        fieldLayoutFrame = new JFrame(title);
        fieldLayoutFrame.setAlwaysOnTop(alwaysOnTopOption.isSelected());
        fieldLayoutFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fieldLayoutFrame.addWindowListener(new WindowAdapter() {            
            @Override
            public void windowOpened(WindowEvent e) {
                viewPanel.doPostOpenActions();
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    viewPanel.removeCellSelectionListener(cellSelectionListener);
                }
                finally {
                    fieldLayoutFrame.removeWindowListener(this);
                    fieldLayoutFrame = null;
                }
            }
        });
        fieldLayoutFrame.setContentPane(viewPanel);
        fieldLayoutFrame.pack();
        fieldLayoutFrame.setLocationRelativeTo(trigger);
        fieldLayoutFrame.setVisible(true);
        fieldLayoutFrame.toFront();
        
        alwaysOnTopOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldLayoutFrame.setAlwaysOnTop(alwaysOnTopOption.isSelected());
            }
        });
    }
    
    public void initialiseAfterOpening() {
        if (viewPanel != null) {
            viewPanel.initialiseAfterOpening();
        }
    }
    

    
    private OverviewDialog overviewDialog;
    
    private OverviewInfoProvider overviewInfoProvider = new OverviewInfoProvider() {
        
        @Override
        public void showRectangle(int row, int col) {
            if (fieldViewTable != null) {
                Rectangle rect = fieldViewTable.getCellRect(row, col, false);
                fieldViewTable.scrollRectToVisible(rect);
            }
        }
        
        @Override
        public boolean isCellSelected(int row, int column) {
            if (fieldViewTable == null) {
                return false;
            }
            return fieldViewTable.isCellSelected(row, column);
        }
        
        @Override
        public Rectangle getVisibleRect() {
            return fieldViewTable==null ? null : fieldViewTable.getVisibleRect();
        }
        
        @Override
        public Color getSelectionBackground() {
            if (fieldViewTable == null) {
                return Color.gray;
            }
            return fieldViewTable.getSelectionBackground();
        }
        
        @Override
        public Point getColumnRowPoint(Point pt) {
            if (fieldViewTable == null) {
                return new Point(0,0);
            }
            return new Point(
                    fieldViewTable.columnAtPoint(pt),
                    fieldViewTable.rowAtPoint(pt));
        }
    };

    private CellSelectableTable fieldViewTable;

    public void openOverview(Window owner,
            String title,
            Component relativeTo,
            @SuppressWarnings("rawtypes") ComboBoxModel comboBoxModel, 
            CurationData curationData, 
            Transformer<TraitInstance, String> tiNameProvider, 
            CellSelectableTable fvt,
            FieldViewSelectionModel fieldViewSelectionModel, 
            FieldLayoutTableModel fieldLayoutTableModel, 
            CurationTableModel curationTableModel) 
    {
        if (overviewDialog != null) {
            overviewDialog.toFront();
            return;
        }
        
        this.fieldViewTable = fvt;
        
        overviewDialog = new OverviewDialog(owner, 
                title,
                comboBoxModel,
                curationData,
                tiNameProvider,
                overviewInfoProvider,
                fieldViewSelectionModel,
                fieldLayoutTableModel,
                curationTableModel);

        overviewDialog.setLocationRelativeTo(relativeTo);
        overviewDialog.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowOpened(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
           
            @Override
            public void windowClosed(WindowEvent e) {
                overviewDialog.removeWindowListener(this);
                overviewDialog = null;    

                fieldViewTable = null;
            }
        });
        overviewDialog.setVisible(true);
    }

    @Override
    public void updateSelectedMeasurements(String fromWhere) {
        if (viewPanel != null) {
            viewPanel.updateSelectedMeasurements(fromWhere);
        }
        
        if (overviewDialog != null) {
            overviewDialog.repaint();
        }
    }

    @Override
    public void repaint() {
        if (viewPanel != null) {
            viewPanel.repaint();
        }
    }

    @Override
    public void clearSelection() {
        if (viewPanel != null) {
            viewPanel.clearSelection();
        }
    }

    @Override
    public void setTemporaryValue(Collection<CurationCellValue> ccvs, Comparable<?> value) {
        if (viewPanel != null) {
            viewPanel.setTemporaryValue(ccvs, value);
        }
    }

    @Override
    public void clearTemporaryValues(Collection<CurationCellValue> ccvs) {
        if (viewPanel != null) {
            viewPanel.clearTemporaryValues(ccvs);
        }
    }

    @Override
    public void refreshSelectedMeasurements(String fromWhere) {
        if (viewPanel != null) {
            viewPanel.refreshSelectedMeasurements(fromWhere);
        }
    }

    @Override
    public TraitInstance getActiveTraitInstance(boolean nullIf_NO_TRAIT_INSTANCE) {
        if (viewPanel != null) {
            return viewPanel.getActiveTraitInstance(nullIf_NO_TRAIT_INSTANCE);
        }
        return null;
    }

    @Override
    public void addTraitInstance(TraitInstance traitInstance) {
        if (viewPanel != null) {
            viewPanel.addTraitInstance(traitInstance);
        }
    }

    @Override
    public void removeTraitInstance(TraitInstance traitInstance) {
        if (viewPanel != null) {
            viewPanel.removeTraitInstance(traitInstance);
        }
    }

    @Override
    public List<Plot> getFieldViewSelectedPlots() {
        if (viewPanel != null) {
            return viewPanel.getFieldViewSelectedPlots();
        }
        return null;
    }

    @Override
    public void setSelectedPlots(List<Plot> plots) {
        if (viewPanel != null) {
            viewPanel.setSelectedPlots(plots);
        }
    }

    @Override
    public String getStoreId() {
        return viewPanel==null 
                ? "" //$NON-NLS-1$
                : viewPanel.getStoreId();
    }

    @Override
    public Component getPanel() {
        return viewPanel;
    }

    @Override
    public SelectedInfo createFromFieldView() 
    {
        if (viewPanel == null) {
            return null;
        }
        return viewPanel.createFromFieldView();
    }

    @Override
    public void doPostOpenActions() {
        if (viewPanel != null) {
            viewPanel.doPostOpenActions();
        }
    }

    @Override
    public void addCellSelectionListener(CellSelectionListener l) {
        listenerList.add(CellSelectionListener.class, l);
    }

    @Override
    public void removeCellSelectionListener(CellSelectionListener l) {
        listenerList.remove(CellSelectionListener.class, l);
    }

    @Override
    public void updateSamplesSelectedInTable() {
        if (viewPanel != null) {
            viewPanel.updateSamplesSelectedInTable();
        }
    }

    private final EventListenerList listenerList = new EventListenerList();

    @Override
    public void addTraitInstanceSelectionListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }
    
    public void addRefreshListener(RefreshListener l) {
        listenerList.add(RefreshListener.class, l);
    }
    
    public void removeRefreshListener(RefreshListener l) {
        listenerList.remove(RefreshListener.class, l);
    }

    public void fireRefreshRequired(Object source) {
        for (RefreshListener l : listenerList.getListeners(RefreshListener.class)) {
            l.refreshRequired(source);
        }
    }
    
    public void fireTraitInstanceActivated(Object source, TraitInstance ti) {
        for (RefreshListener l : listenerList.getListeners(RefreshListener.class)) {
            l.traitInstanceActivated(source, ti);
        }
    }
}
