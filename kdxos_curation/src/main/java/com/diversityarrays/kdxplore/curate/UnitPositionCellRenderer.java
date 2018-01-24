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

import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.pearcan.ui.renderer.NumberCellRenderer;

public class UnitPositionCellRenderer extends NumberCellRenderer {
	
	public UnitPositionCellRenderer() {
		super(new DecimalFormat("#"), SwingConstants.CENTER); //$NON-NLS-1$
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) 
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		if (value instanceof UnitPosition) {
			setToolTipText(((UnitPosition) value).unitPositionText);
		}
		else {
			setToolTipText(null);
		}
		
		return this;
	}

	@Override
	protected void setValue(Object value) {
		if (value instanceof UnitPosition) {
			super.setValue(((UnitPosition) value).value);
		}
		else {
			super.setValue(value);
		}
	}

}
