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
package com.diversityarrays.kdxplore.chartcommon;

import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.EventListener;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;

/**
 * @author alexs
 *
 */
public class KDXploreChartPanel extends ChartPanel {

	/**
	 * @param chart
	 */
	public KDXploreChartPanel(JFreeChart chart) {
		super(chart);
	}

	
	private boolean shiftDownatStart = false;
	
	@Override
	public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);
	}
	
	@Override
	public void mousePressed(MouseEvent e) { 	
		if (e.isShiftDown() || shiftDownatStart) {
			super.mousePressed(e);
			shiftDownatStart = true;	
		}
		else {
			super.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) { 	
		if (e.isShiftDown() || shiftDownatStart) {
			super.mouseReleased(e);
			shiftDownatStart = false;

			Insets insets = getInsets();
			int x = (int) ((e.getX() - insets.left) / this.getScaleX());
			int y = (int) ((e.getY() - insets.top) / this.getScaleY());

			ChartEntity entity = null;
			if (this.getChartRenderingInfo() != null) {
				EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
				if (entities != null) {
					entity = entities.getEntity(x, y);
				}
			}

			Object[] listeners = this.getListeners(
					ChartMouseListener.class);

			if (this.getChart() != null) {
				ChartMouseEvent event = new ChartMouseEvent(getChart(), e, entity);
				for (int i = listeners.length - 1; i >= 0; i -= 1) {
					if (listeners[i] instanceof KDXChartMouseListener) {
						((KDXChartMouseListener) listeners[i]).chartMouseZoomingReleased(event);
					}
				}
			}
			
		}
		else {
			super.mouseReleased(e);

			Insets insets = getInsets();
			int x = (int) ((e.getX() - insets.left) / this.getScaleX());
			int y = (int) ((e.getY() - insets.top) / this.getScaleY());

			ChartEntity entity = null;
			if (this.getChartRenderingInfo() != null) {
				EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
				if (entities != null) {
					entity = entities.getEntity(x, y);
				}
			}

			Object[] listeners = this.getListeners(
					ChartMouseListener.class);

			if (this.getChart() != null) {
				ChartMouseEvent event = new ChartMouseEvent(getChart(), e, entity);
				for (int i = listeners.length - 1; i >= 0; i -= 1) {
					if (listeners[i] instanceof KDXChartMouseListener) {
						((KDXChartMouseListener) listeners[i]).chartMouseSelectedReleased(event);
					}
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) { 	
		if (e.isShiftDown() || shiftDownatStart) {
			super.mouseDragged(e);
				
		}
		else {

			Insets insets = getInsets();
			int x = (int) ((e.getX() - insets.left) / this.getScaleX());
			int y = (int) ((e.getY() - insets.top) / this.getScaleY());

			ChartEntity entity = null;
			if (this.getChartRenderingInfo() != null) {
				EntityCollection entities = this.getChartRenderingInfo().getEntityCollection();
				if (entities != null) {
					entity = entities.getEntity(x, y);
				}
			}

			if (this.getChart() != null) {
				ChartMouseEvent event = new ChartMouseEvent(getChart(), e, entity);

				EventListener[] listeners = this.getListeners(ChartMouseListener.class);

				for (int i = listeners.length - 1; i >= 0; i -= 1) {
					if (listeners[i] instanceof KDXChartMouseListener) {
						((KDXChartMouseListener) listeners[i]).chartMouseSelected(event);
					}
				}
			}
		}
	}
}
