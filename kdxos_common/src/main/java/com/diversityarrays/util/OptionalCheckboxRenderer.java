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

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class OptionalCheckboxRenderer extends JLabel implements TableCellRenderer {
	
	protected TableCellRenderer booleanRenderer = new BooleanRenderer();
	
	private String textForNullValue;
	
	public OptionalCheckboxRenderer(String toolTipTextForNullValue) {
		this(toolTipTextForNullValue, "*"); //$NON-NLS-1$
	}
	
	public OptionalCheckboxRenderer(String toolTipTextForNullValue, String textForNullValue) {
		this.textForNullValue = textForNullValue;
		setHorizontalAlignment(CENTER);
		setToolTipText(toolTipTextForNullValue);
	}
	
	public String getTextForNullValue() {
		return textForNullValue;
	}

	public void setTextForNullValue(String s) {
		this.textForNullValue = s;
	}

	/**
	 * Override this if you want to support "truthy" in other ways for
	 * non-Boolean 
	 * @param value
	 * @return Boolean
	 */
	protected boolean isValueTruthy(Object value) {
		boolean result = false;
		if (value != null) {
			if (value instanceof Boolean) {
				result = ((Boolean) value).booleanValue();
			}
			else {
				result = true;
			}
		}
		return result;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column) 
	{
		Component result = null;
		if (value==null) {
			this.setText(textForNullValue);
			result = this;
		}
		else {
			Boolean booleanValue = isValueTruthy(value);
			result = booleanRenderer.getTableCellRendererComponent(table, booleanValue, isSelected, hasFocus, row, column);
		}
		return result;
	}
	
}
