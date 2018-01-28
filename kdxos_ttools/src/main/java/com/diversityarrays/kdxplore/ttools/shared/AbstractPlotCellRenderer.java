/**
 * 
 */
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

import java.awt.Graphics;

import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("nls")
public class AbstractPlotCellRenderer extends DefaultTableCellRenderer {
	
	protected SampleIconType sampleIconType;
	protected CommentMarker commentMarker;

	public AbstractPlotCellRenderer() {
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (sampleIconType != null) {
		    sampleIconType.draw(this, g, "=");
		}
		
		if (commentMarker != null ) {
		    commentMarker.draw(this, g);
		}
	}
}
