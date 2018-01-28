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
package com.diversityarrays.kdxplore.trialtool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.scoring.PlotVisitGroup;
import com.diversityarrays.kdsmart.scoring.PlotVisitList;
import com.diversityarrays.kdsmart.scoring.WalkSegment;
import com.diversityarrays.kdxplore.ttools.shared.AbstractPlotCellRenderer;
import com.diversityarrays.kdxplore.ttools.shared.SampleIconType;
import com.diversityarrays.util.UnicodeChars;

public class SimplePlotCellRenderer extends AbstractPlotCellRenderer {
    
    static private String getAttachmentCountCharacter(int n) {
        UnicodeChars.Number[] numbers = UnicodeChars.Number.values();
        return numbers[Math.min(n, numbers.length-1)].parenthesis;
    }
    
    static private String getLabelCountCharacter(int n) {
        if (n <= 0) {
            return "";
        }
        UnicodeChars.Number[] numbers = UnicodeChars.Number.values();
        return numbers[Math.min(n, numbers.length-1)].negative;
    }
	
	private Transformer<Plot, String> defaultHtmlProvider = new Transformer<Plot, String>() {
		@Override
		public String transform(Plot plot) {
			StringBuilder html = new StringBuilder("<HTML>");

			if (plotVisitList != null) {
				PlotVisitGroup pvg = plotVisitList.getPlotVisitGroup(plot);
				if (pvg != null) {
					WalkSegment ws = pvg.walkSegment;
					html.append('#').append(ws.segmentIndex+1)
						.append(":<B>").append(ws.orientationUDLR.unicodeArrow);
					
		           int nFiles = plot.getMediaFileCount();
		           if (nFiles > 0) {
		               html.append(getAttachmentCountCharacter(nFiles));
		           }
		           
		           Map<Integer, List<Tag>> map = plot.getTagsBySampleGroup();
		           if (! map.isEmpty()) {
		               Set<String> set = new HashSet<>();
		               for (List<Tag> list : map.values()) {
		                   for (Tag t : list) {
		                       set.add(t.getLabel());
		                   }
		               }
		               if (! set.isEmpty()) {
	                       html.append(getLabelCountCharacter(set.size()));
		               }
		           }
			       html.append("</B>");
				}
			}

			Point xy = null;
			if (xyProvider != null) {
				xy = xyProvider.transform(plot);
			}
			
			if (xy != null) {
				html.append("<BR>")
					.append("X: ").append(xy.x)
					.append(" Y: ").append(xy.y);
			}
			
			if  (showUserPlotId) {
				html.append("<BR><b>")
				.append(plot.getUserPlotId())
				.append("</b>");
			}
		
			
			return html.toString();		
		}
	};
	
	private PlotVisitList plotVisitList;
	private boolean showUserPlotId;
	private Transformer<Plot,Point> xyProvider;

	
	private Transformer<Plot, String> htmlProvider = defaultHtmlProvider;
	

	public SimplePlotCellRenderer() { 
		setHorizontalAlignment(CENTER);
	}

	public PlotVisitList getPlotVisitList() {
		return plotVisitList;
	}

	public void setPlotVisitList(PlotVisitList pvl) {
		this.plotVisitList = pvl;
	}

	public void setShowUserPlotId(boolean b) {
		showUserPlotId = b;
	}
	
	public void setPlotXYprovider(Transformer<Plot,Point> xyp) {
		xyProvider = xyp;
	}
	
	public Transformer<Plot, Point> getXYprovider() {
		return xyProvider;
	}

	public void resetHtmlProvider() {
		setHtmlProvider(defaultHtmlProvider);
	}
	
	public Transformer<Plot, String> getHtmlProvider() {
		return htmlProvider;
	}
	
	public void setHtmlProvider(Transformer<Plot, String> provider) {
		htmlProvider = provider;
	}

	/**
	 * Override this to customise behaviour.
	 * @param plot
	 * @return
	 */
	protected String getHtmlForPlot(Plot plot) {
		String result;
		if (htmlProvider == null) {

			StringBuilder sb = new StringBuilder("<HTML>");

			PlotIdentOption pio = plotVisitList.getTrial().getPlotIdentOption();
			
			Point xy = new Point(plot.getPlotColumn(), plot.getPlotRow());
			if (xyProvider != null) {
				xy = xyProvider.transform(plot);
			}
			
			sb.append(plotVisitList.getTrial().getNameForPlot());
			switch (pio) {
			case NO_X_Y_OR_PLOT_ID:
				break;
			case PLOT_ID:
				sb.append(" id=").append(plot.getUserPlotId());
				break;
			case PLOT_ID_THEN_X:
				sb.append(" id=").append(plot.getUserPlotId());
				sb.append("<BR>X=").append(xy.x);
				break;
			case PLOT_ID_THEN_XY:
				sb.append(" id=").append(plot.getUserPlotId());
				sb.append("<BR>X=").append(xy.x);
				sb.append(" Y=").append(xy.y);
				break;
			case PLOT_ID_THEN_Y:
				sb.append(" id=").append(plot.getUserPlotId());
				sb.append("<BR>Y=").append(xy.y);
				break;
			case PLOT_ID_THEN_YX:
				sb.append(" id=").append(plot.getUserPlotId());
				sb.append("<BR>Y=").append(xy.y);
				sb.append(" X=").append(xy.x);
				break;
			case X_THEN_Y:
				sb.append(" id=").append(plot.getUserPlotId());
				sb.append("<BR>X=").append(xy.x);
				sb.append(" Y=").append(xy.y);
			break;
			case Y_THEN_X:
				sb.append(" id=").append(plot.getUserPlotId());
				sb.append("<BR>Y=").append(xy.y);
				sb.append(" X=").append(xy.x);
				break;
			default:
				break;			
			}
			
			result = sb.toString();
		}
		else {
			result = htmlProvider.transform(plot);
		}
		return result;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		commentMarker = null;
		sampleIconType = SampleIconType.NORMAL;
		
		if (value instanceof Plot) {
			Plot plot = (Plot) value;
			if (! plot.isActivated()) {
				sampleIconType = isSelected ? SampleIconType.INACTIVE_PLOT_SELECTED : SampleIconType.INACTIVE_PLOT;
			}
			String str = getHtmlForPlot(plot);
			setText(str);
		}

		if (! isSelected) {
			if ((0 == (row & 1)) == (0 == (column & 1))) {
				// 
				setBackground(Color.LIGHT_GRAY);
			} else {
				// Odd row
				setBackground(Color.WHITE);
			}
		}

		return this;
	}
}
