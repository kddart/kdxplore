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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.text.DateFormat;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class TsmCellRenderer extends DefaultTableCellRenderer {
	
	static private final int EDGE_LEN = 7;
	
	private final Polygon moreRecentTriangle;
	
	private boolean moreRecent;
	
	private DateFormat dateFormat;

	public TsmCellRenderer() {
		int npoints = 3;
		int[] xpoints = new int[npoints];
		int[] ypoints = new int[npoints];
		xpoints[0] = -EDGE_LEN;  ypoints[0] = 0;
		xpoints[1] = 0;          ypoints[1] = 0;
		xpoints[2] = 0;          ypoints[2] = EDGE_LEN;
		moreRecentTriangle = new Polygon(xpoints, ypoints, npoints);
	}
	
	public void setDateFormat(DateFormat df) {
		this.dateFormat = df;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column) 
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		moreRecent = false;
		if (value instanceof TypedSampleMeasurement) {
			TypedSampleMeasurement tsm = (TypedSampleMeasurement) value;
			moreRecent = tsm.moreRecent;
		}
		
		setToolTipText(moreRecent ? "More recent than Curated value" : null);
		return this;
	}

	@Override
	protected void setValue(Object value) {
		if (value instanceof TypedSampleMeasurement) {
			TypedSampleMeasurement tsm = (TypedSampleMeasurement) value;
			setText(tsm.asDeviceNameString(dateFormat));
		}
		else {
			super.setValue(value);
		}
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (moreRecent) {
			Rectangle rect = getBounds();
			g.translate(rect.width, 0);
			g.setColor(Color.RED);
			g.fillPolygon(moreRecentTriangle);
			g.translate(-rect.width, 0);
		}
	}
	
}
