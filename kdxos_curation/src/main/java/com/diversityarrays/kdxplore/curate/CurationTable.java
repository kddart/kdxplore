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

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.diversityarrays.kdxplore.curate.ValueRetriever.ValueType;

@SuppressWarnings("nls")
public class CurationTable extends JTable {
    
    static private TableCellRenderer createDefaultTableCellRenderer(Color fg, Color bg) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        if (fg != null) {
            r.setForeground(fg);
        }
        if (bg != null) {
            r.setBackground(bg);
        }
        return r;
    }
	
    // Any PlotAttribute
    protected TableCellRenderer traitInstanceHeaderRenderer = createDefaultTableCellRenderer(
            null, Color.LIGHT_GRAY);

    // Any PlotAttribute
    protected TableCellRenderer plotAttributeHeaderRenderer = createDefaultTableCellRenderer(
            Color.decode("#993300"), Color.LIGHT_GRAY);

    // (User)PlotId, PlotRow, PlotColumn, PlotType
    protected TableCellRenderer plotInfoHeaderRenderer = createDefaultTableCellRenderer(Color.BLUE, Color.LIGHT_GRAY);

    // DartEntityFeature, Unknown
    protected TableCellRenderer otherHeaderRenderer = createDefaultTableCellRenderer(Color.RED, Color.LIGHT_GRAY);

	public CurationTable(String name, CurationTableModel tm, CurationTableSelectionModel sm) {
		super(tm, null, sm);
		setName(name);
	}
	
	@Override
	public void setSelectionModel(ListSelectionModel lsm) {
		if (! (lsm instanceof CurationTableSelectionModel)) {
			throw new IllegalArgumentException("Incorrect class for ListSelectionModel: " + lsm.getClass().getName());
		}
		super.setSelectionModel(lsm);
	}
		
	@Override
	public void setModel(TableModel model) {
		if (! (model instanceof CurationTableModel)) {
			throw new IllegalArgumentException("Incorrect class for TableModel: " + model.getClass().getName());
		}
		super.setModel(model);
	}

	private void initHeaderRenderers() {
        CurationTableModel curationTableModel = (CurationTableModel) getModel();

        TableColumnModel tcm = getColumnModel();
        int nColumns = tcm.getColumnCount();
        for (int col = 0; col < nColumns; ++col) {
            TableCellRenderer hr = traitInstanceHeaderRenderer;

            ValueType vt = curationTableModel.getColumnValueType(col);
            if (vt == null) {
                hr = otherHeaderRenderer;
            }
            else {
                switch (vt) {
                case USER_PLOT_ID:
                case X_COLUMN:
                case Y_ROW:
                case PLOT_TYPE:
                case SPECIMEN_NUMBER:
                case ATTACHMENT_COUNT:
                case PLOT_NOTE:
                case PLOT_TAGS:
                    hr = plotInfoHeaderRenderer;
                    break;
                case PLOT_ATTRIBUTE:
                    hr = plotAttributeHeaderRenderer;
                    break;
                case TRAIT_INSTANCE:
                    hr = traitInstanceHeaderRenderer;
                    break;
                default:
                    hr = otherHeaderRenderer;
                    break;
                }
            }
            TableColumn tc = tcm.getColumn(col);
            tc.setHeaderRenderer(hr);
        }
	}
	
    @Override
    public void createDefaultColumnsFromModel() {
        super.createDefaultColumnsFromModel();
        initHeaderRenderers();
    }

	// TODO make this do a different way of extended selection

}
