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

import java.awt.Component;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.List;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.SelectedInfo;
import com.diversityarrays.kdxplore.ui.CellSelectionListener;
import com.diversityarrays.util.ResizableTable;
import com.diversityarrays.util.TableColumnResizer;
import com.diversityarrays.util.TableRowResizer;

@SuppressWarnings("nls")
public interface FieldLayoutView {

    static class Util {
        public static boolean DEFAULT_RESIZE_ALL = true;
        public static boolean PROPAGATE_RESIZE_ALL_COLUMNS = true;
    }
    

    enum Setting {
        RESIZABLE_TABLE_ALL_COLUMNS("Resizabletable.RESIZE_ALL_COLUMN"),
        FIELD_VIEW_RESIZE_ALL("FieldView.DEFAULT_RESIZE_ALL (restart)"),
        PROPAGATE_RESIZE_ALL("FieldView.PROPAGATE_RESIZE_ALL_COLUMNS"),
        
        DEBUG_TABLE_COLUMN_RESIZER("TableColumnResizer.DEBUG"),
        DEBUG_TABLE_ROW_RESIZER("TableRowResizer.DEBUG")
        ;
        public final String displayName;
        Setting(String s) {
            displayName = s;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
        
        public void setSetting(boolean b) {
            switch (this) {
            case DEBUG_TABLE_COLUMN_RESIZER:
                TableColumnResizer.DEBUG = b;
                break;
            case DEBUG_TABLE_ROW_RESIZER:
                TableRowResizer.DEBUG = b;
                break;
            case FIELD_VIEW_RESIZE_ALL:
                Util.DEFAULT_RESIZE_ALL = b;
                break;
            case PROPAGATE_RESIZE_ALL:
                Util.PROPAGATE_RESIZE_ALL_COLUMNS = b;
                break;
            case RESIZABLE_TABLE_ALL_COLUMNS:
                ResizableTable.RESIZE_ALL_COLUMNS = b;
                break;
            default:
                break;          
            }
        }
        
        public boolean getSetting() {
            switch (this) {
            case DEBUG_TABLE_COLUMN_RESIZER:
                return TableColumnResizer.DEBUG;
            case DEBUG_TABLE_ROW_RESIZER:
                return TableRowResizer.DEBUG;
            case FIELD_VIEW_RESIZE_ALL:
                return Util.DEFAULT_RESIZE_ALL;
            case PROPAGATE_RESIZE_ALL:
                return Util.PROPAGATE_RESIZE_ALL_COLUMNS;
            case RESIZABLE_TABLE_ALL_COLUMNS:
                return ResizableTable.RESIZE_ALL_COLUMNS;
            default:
                break;
            }
            throw new RuntimeException("Unhandled: " + this);
        }
    }

    
    void updateSelectedMeasurements(String fromWhere);

    void repaint();

    void clearSelection();

    void setTemporaryValue(Collection<CurationCellValue> ccvs, Comparable<?> value);
    void clearTemporaryValues(Collection<CurationCellValue> ccvs);
    
    void refreshSelectedMeasurements(String fromWhere);

    TraitInstance getActiveTraitInstance(boolean nullIf_NO_TRAIT_INSTANCE);
    void addTraitInstance(TraitInstance traitInstance);
    void removeTraitInstance(TraitInstance traitInstance);


    List<Plot> getFieldViewSelectedPlots();
    void setSelectedPlots(List<Plot> plotSpecimens);
    
    String getStoreId();

    Component getPanel();
    
    SelectedInfo createFromFieldView();

    void doPostOpenActions();

    void addCellSelectionListener(CellSelectionListener l);
    void removeCellSelectionListener(CellSelectionListener l);

    void updateSamplesSelectedInTable();

    void addTraitInstanceSelectionListener(ItemListener itemListener);

}
