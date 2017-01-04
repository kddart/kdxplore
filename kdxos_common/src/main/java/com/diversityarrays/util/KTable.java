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
package com.diversityarrays.util;

import javax.swing.JTable;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.table.TableModel;

public class KTable extends JTable {
    
    static public final String KTABLE_ROWHEIGHT = "KTable.rowHeight"; //$NON-NLS-1$

    public KTable() {
        super();
    }
    
    public KTable(TableModel model) {
        super(model);
    }
    
    protected void applyUIdefaults() {
        UIDefaults defaults = UIManager.getDefaults();
        int tableRowHeight = defaults.getInt(KTABLE_ROWHEIGHT);
        if (tableRowHeight > 0) {
            setRowHeight(tableRowHeight);
        }
    }
}
