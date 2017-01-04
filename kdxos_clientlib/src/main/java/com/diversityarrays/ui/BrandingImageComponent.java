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
package com.diversityarrays.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * A simple widget to display an image and background color
 * which can be used with LoginDialog.
 * @author brian
 *
 */
public class BrandingImageComponent extends JComponent {
	
	private final Image image;
	
	public BrandingImageComponent(Image image) {
		this.image = image;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension sz = super.getPreferredSize();

		int wid = image.getWidth(null);
		int hyt = image.getHeight(null);
		sz.width += wid;
		sz.height = Math.max(sz.height, sz.height + hyt);

		return sz;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(getBackground());
		Rectangle r = getBounds();
		g.fillRect(0, 0, r.width, r.height);
		g.drawImage(image, 0, 0, null);
	}
}
