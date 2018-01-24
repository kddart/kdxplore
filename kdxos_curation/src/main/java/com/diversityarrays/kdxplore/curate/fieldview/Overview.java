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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.CurationTableModel;
import com.diversityarrays.kdxplore.curate.TraitColorProvider;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.field.FieldLayoutTableModel;

@SuppressWarnings("nls")
public class Overview extends JPanel {
    
    public interface OverviewInfoProvider {
        Color getSelectionBackground();

        void showRectangle(int row, int col);

//        void addListSelectionListener(ListSelectionListener listSelectionListener);

        boolean isCellSelected(int row, int col);

        Rectangle getVisibleRect();

        Point getColumnRowPoint(Point start);
        
    }

	enum OverviewIcon {
//		NORMAL( "\u25c9" ), // 25c9==black dot in circle, 25a3==black square in white
		RAW( "\u25cc" ),    // 25cc=dotted-circle
		DATABASE( "\u2338" ), // 2338==Boxed 2 bars (APL quad),  2261==three bars
		NO_VALUES( "\u2b1c" ), // No values at all in CCV : large white square
		MISSING_CCV( "?" ),    // CCV not found when it should have been
		NO_PLOT ( " " ),   // ? perhaps something else: 25a6==crosshatch does not render well on all screens
		INACTIVE_PLOT( "\u22a0" ), // 22a0==cross in small square, 236f==NE in box, 2327==cross on square  232b="kboard delete/erase"
		NO_INSTANCE( "\u00b7" ), // centred dot 

		// These are when values exist
		CURATED( "\u25c9" ), // Edited value in CCV   : paint Tick mark
		NOT_SET( "-" ),
		NA( "\u25c7" ), // 25c7==small diamond, 25cb==small circle  25c8 filled diamond
		MISSING( "\u25ce" ); // circle in circle
		
		public final char[] chars;
		OverviewIcon(String s) {
			chars = s.toCharArray();
			if (chars.length > 1) {
				throw new RuntimeException("More than 1 char defined for " + this);
			}
		}
	}
	
	static public String getOverviewHelpHtml() {

	    List<String> lines = new ArrayList<>();
	    lines.add("");
        lines.add("<TABLE>");
        lines.add("<TR><TD>No Plot</TD><TD><i>blank</i></TD></TR>"); //  + new String(OverviewIcon.NO_PLOT.chars) + "</TD></TR>");
        lines.add("<TR><TD>Deactivated Plot</TD><TD>" + new String(OverviewIcon.INACTIVE_PLOT.chars) + "</TD></TR>");
        lines.add("<TR><TD>No Trait selected</TD><TD>" + new String(OverviewIcon.NO_INSTANCE.chars) + "</TD></TR>");
        lines.add("<TR><TD>No Samples</TD><TD>" + new String(OverviewIcon.NO_VALUES.chars) + "</TD></TR>");
        lines.add("<TR><TD>Value is from Database</TD><TD>" + new String(OverviewIcon.DATABASE.chars) + "</TD></TR>");
        lines.add("<TR><TD>Not yet Curated</TD><TD>" + new String(OverviewIcon.RAW.chars) + "</TD></TR>");
        lines.add("<TR><TD>Curated</TD><TD>");
        lines.add(new String(OverviewIcon.CURATED.chars) + ": Has value");
        lines.add("<BR>" + new String(OverviewIcon.NA.chars) + ": N/A");
        lines.add("<BR>" + new String(OverviewIcon.MISSING.chars) + ": Missing");
        lines.add("</TD></TR>");
        lines.add("</TABLE>");
        
        StringBuilder sb = new StringBuilder("<HTML>");
        for (String s : lines) {
            sb.append(s);
        }
        return sb.toString();
	}

	private final int triangleEdgeLen = 6;
	private final Polygon commentTriangle;

	private final int cellSizeX = 16; // min 12
	private final int cellSizeY = 16;
	
	private final PropertyChangeListener fieldLayoutChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			repaint();
		}
	};
	
	private final FieldLayoutTableModel fltm;
	
	private TraitInstance activeTraitInstance;
	
	private final TraitColorProvider colorProvider;

	private final CurationTableModel curationTableModel;
//	private final Map<OverviewIcon, char[]> iconToChars;
	private final Map<char[],Integer> textxForChars = new HashMap<>();

	private final MouseListener tttMouseListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (! SwingUtilities.isLeftMouseButton(e)) {
				return;
			}
			int clickCount = e.getClickCount();
			if (1 == clickCount) {
				e.consume();
				
				String ttt = null;
				Point where = new Point(-1, -1);
				Plot plot = getPlotAt(e, where);
				if (plot != null) {
					ttt = getTraitDisplayValue(plot);
				}

				if (where.x >= 0) {
					Point old = infoPlotPoint;
					infoPlotPoint = where;
					if (! infoPlotPoint.equals(old)) {
						repaint();
					}
				}
				
				infoLabel.setText(ttt==null ? " " : ttt);
			}
			else if (2 == clickCount) {
				Point where = new Point(-1, -1);
				Plot plot = getPlotAt(e, where);
				if (plot != null && (where.x >= 0)) {
				    overviewInfoProvider.showRectangle(where.y, where.x);
//					Rectangle rect = fieldTable.getCellRect(where.y, where.x, false);
//					fieldTable.scrollRectToVisible(rect);
				}				
			}
		}
	};
	
	private final DateFormat traitValueDateFormat = TraitValue.getTraitValueDateFormat();
	private final CurationData curationData;
	private final JLabel infoLabel;
	private Color tableSelectionBackground;


    private final OverviewInfoProvider overviewInfoProvider;

	public Overview(OverviewInfoProvider ip, 
			FieldLayoutTableModel fltm, 
			CurationData cd, 
			CurationTableModel ctm,
			JLabel infoLabel)
	{
		super(new BorderLayout());
		
		this.overviewInfoProvider = ip;
		int npoints = 3;
		int[] xpoints = new int[npoints];
		int[] ypoints = new int[npoints];
		xpoints[0] = - triangleEdgeLen;  ypoints[0] = 0;
		xpoints[1] = 0;          ypoints[1] = 0;
		xpoints[2] = 0;          ypoints[2] = triangleEdgeLen;
		commentTriangle = new Polygon(xpoints, ypoints, npoints);

		this.tableSelectionBackground = overviewInfoProvider.getSelectionBackground();
		this.fltm = fltm;
		this.curationData = cd;
		this.curationTableModel = ctm;
		this.infoLabel = infoLabel;
		
		colorProvider = curationData.getTraitColorProvider();
		
		curationTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				repaint();
			}
		});

		fltm.addPropertyChangeListener(FieldLayoutTableModel.PROP_FIELD_LAYOUT,
				fieldLayoutChangeListener);
		
		
		addMouseListener(tttMouseListener);
	}
	
	private Plot getPlotAt(MouseEvent e, Point where) {
		FieldLayout<Plot> fieldLayout = fltm.getFieldLayout();
		if (fieldLayout == null) {
			return null;
		}
		
		Point pt = e.getPoint();
		Insets insets = getInsets();
		if (insets != null) {
			pt.x -= insets.left;
			pt.y -= insets.top;
		}
		
		int cell_x = pt.x / cellSizeX;
		int cell_y = pt.y / cellSizeY;
		System.out.println("cell_x,y:  " + cell_x + "," + cell_y);
		if (cell_x < fieldLayout.xsize && cell_y < fieldLayout.ysize) {
			where.x = cell_x;
			where.y = cell_y;
			return fieldLayout.cells[cell_y][cell_x];
		}
		return null;
	}
	
	private String getTraitDisplayValue(Plot plot) {
		String result = null;
		
		if (plot != null) {
			CurationCellValue ccv = 
					curationTableModel.getCurationCellValueForTraitInstanceInPlot(
					activeTraitInstance, plot);
			if (ccv!=null) {
				KdxSample sm = ccv.getEditStateSampleMeasurement();
				if (sm != null) {
					TraitInstance ti = curationData.getTraitInstanceForSample(sm);
					result = TraitValue.transformTraitValueToDisplayValue(null, 
							ti.trait, 
							curationData.getTrial().getTrialPlantingDate(), 
							traitValueDateFormat, 
							TraitValue.INVALID_DATE_TRANSFORMER, 
							sm.getTraitValue());
					
					switch (ccv.getEditState()) {
					case CALCULATED:
					    result = "Calculated: " + result;
					    break;
					case CURATED:
						break;

					case MORE_RECENT:
					case RAW_SAMPLE:
						if (! TraitValue.EXPORT_VALUE_UNSCORED.equals(result)) {
							result = "Raw: " + result;
						}
						break;

					case FROM_DATABASE:
						result = "Database: " + result;
						break;

					case NO_VALUES:
						break;
					default:
						break;
					
					}
				}
//				KdxSample sample = ccv.getEditedSample();
//				if (sample == null) {
//					result = "Not Curated";
//				}
//				else {
//					TraitInstance ti = curationData.getTraitInstanceForSample(sample);
//
//					result = TraitValue.transformTraitValueToDisplayValue(null, 
//							ti.trait, 
//							curationData.getTrial().getTrialPlantingDate(), 
//							traitValueDateFormat, 
//							TraitValue.INVALID_DATE_TRANSFORMER, 
//							sample.getTraitValue());
//				}
			}
		}
		return result;
	}
	
//	@Override
//	public String getToolTipText(MouseEvent e) {
//		
//		Plot plot = getPlotAt(e);
//		if (plot==null) {
//			return super.getToolTipText(e);
//		}
//		
//		return getTraitDisplayValue(plot);		
//	}


	public void setActiveTraitInstance(TraitInstance ti) {
		activeTraitInstance = ti;
		infoLabel.setText(" ");
		infoPlotPoint = null;
		
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	@Override
	public Dimension getMaximumSize() {
		return getMinimumSize();
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension result = new Dimension(100, 100);
		FieldLayout<Plot> fieldLayout = fltm.getFieldLayout();
		if (fieldLayout != null) {
			result = new Dimension(
					fieldLayout.xsize * cellSizeX + 2,
					fieldLayout.ysize * cellSizeY + 2);
		}
		return result;
	}


	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		FieldLayout<Plot> fieldLayout = fltm.getFieldLayout();
		if (fieldLayout != null) {

			FontMetrics fm = g.getFontMetrics();
			int ascent = fm.getAscent();
			int text_y = ascent + (cellSizeY - ascent) / 2;

			Color selectionColor = null;
			if (activeTraitInstance == null) {
				selectionColor = tableSelectionBackground;
			}
			else {
				selectionColor = colorProvider.getTraitInstanceColor(activeTraitInstance).getBackground();
			}
			
			int ncols = fieldLayout.xsize;
			int nrows = fieldLayout.ysize;
			for (int row = 0; row < nrows; ++row) {
				
				for (int col = 0; col < ncols; ++col) {
					boolean cellSelected = overviewInfoProvider.isCellSelected(row, col);

					Overview.OverviewIcon overviewIcon;
					boolean moreRecent = false;

					Plot plot = fieldLayout.cells[row][col];
					
//					boolean plotSelected = false;
//					if (activeTraitInstance != null) {
//						List<Plot> plots = fieldViewSelectionModel.getSelectedPlots();
//						plotSelected = plots.contains(plot);
//					}
//					
//					if (plotSelected != cellSelected) {
//						android.util.Log.d(TAG, "paintComponent: at " + col+","+row+": cellSelected="+cellSelected + ", plotSelected="+plotSelected);
//					}
					
					if (plot == null) {
						overviewIcon = OverviewIcon.NO_PLOT;
					}
					else if (! plot.isActivated()) {
						overviewIcon = OverviewIcon.INACTIVE_PLOT;
					}
					else if (activeTraitInstance==null) {
						overviewIcon = OverviewIcon.NO_INSTANCE;
					}
					else {
						CurationCellValue ccv = 
								curationTableModel.getCurationCellValueForTraitInstanceInPlot(
								activeTraitInstance, plot);
						if (ccv == null) {
							overviewIcon = OverviewIcon.MISSING_CCV;
						}
						else {
							KdxSample esm = ccv.getEditStateSampleMeasurement();
							
							switch (ccv.getEditState()) {
	                         case CALCULATED:
	                                // TODO consider a different icon for calculated?
							case CURATED:
								switch (TraitValue.classify(esm.getTraitValue())) {
								case MISSING:
									overviewIcon = OverviewIcon.MISSING;
									break;
								case NA:
									overviewIcon = OverviewIcon.NA;
									break;
								case UNSET:
									overviewIcon = OverviewIcon.NOT_SET;
									break;

								case SET:
								default:
									overviewIcon = OverviewIcon.CURATED;
									break;
								}
								break;

							case MORE_RECENT:
								//					sample = ccv.getFirstRawSample();
								moreRecent = true;
							case RAW_SAMPLE:
								overviewIcon = OverviewIcon.RAW;
								break;

								
							case FROM_DATABASE:
								overviewIcon = OverviewIcon.DATABASE;
								break;
							case NO_VALUES:
								overviewIcon = OverviewIcon.NO_VALUES;
								break;
							default:
								overviewIcon = null;
								break;
							}	
						}
					}
					
					int x0 = col * cellSizeX;
					int y0 = row * cellSizeY;
					
					if (cellSelected && selectionColor != null) {
						Color save = g.getColor();
						g.setColor(selectionColor);
						g.fillRect(x0, y0, cellSizeX, cellSizeY);
						g.setColor(save);
					}
					
					char[] chars = overviewIcon.chars; // iconToChars.get(overviewIcon);
					if (chars != null && chars.length == 1) {
						Integer text_x = textxForChars.get(chars);
						if (text_x == null) {
							int fm_wid = fm.charsWidth(chars, 0, chars.length);
							if (fm_wid < cellSizeX) {
								text_x = (cellSizeX - fm_wid) / 2;
							}
							else {
								text_x = 0;
							}
							textxForChars.put(chars, text_x);
						}
						Color c = Color.BLACK;
						if (OverviewIcon.DATABASE == overviewIcon) {
							c = Color.BLUE;
						}
						else if (OverviewIcon.INACTIVE_PLOT == overviewIcon) {
							c = Color.RED;
						}
						g.translate(x0, y0);
						g.setColor(c);
						g.drawChars(chars, 0, chars.length, text_x, text_y);
						g.translate(-x0, -y0);
					}
					
					if (moreRecent) {
						int xt = x0 + cellSizeX;
						int yt = y0 + 1;
						g.translate(xt, yt);
						g.setColor(Color.RED);
						g.fillPolygon(commentTriangle);
						g.translate(-xt, -yt);
					}
				}
			}
			
			drawVisibleRect(g);
			
			// If user clicked on a Plot for info, show it
			if (infoPlotPoint != null) {
				drawInfoPlotPoint(g);
			}
		}
	}

	private void drawInfoPlotPoint(Graphics g) {
		g.setColor(Color.RED);
		int x = infoPlotPoint.x * cellSizeX;
		int y = infoPlotPoint.y * cellSizeY;
		g.drawRect(x+1, y+1, cellSizeX-1, cellSizeY-1);
	}

	private void drawVisibleRect(Graphics g) {
		Rectangle visibleRect = overviewInfoProvider.getVisibleRect();
		if  (visibleRect == null) {
		    return;
		}

		Point start = new Point(visibleRect.x, visibleRect.y);
		Point end = new Point(start.x + visibleRect.width - 1, 
				start.y + visibleRect.height - 1);

		Point nw = overviewInfoProvider.getColumnRowPoint(start);
		Point se = overviewInfoProvider.getColumnRowPoint(end);
		
		Point ov_nw = new Point(nw.x * cellSizeX, nw.y * cellSizeY);
		
		int nPlotsWide = (se.x - nw.x + 1);
		int nPlotsHigh = (se.y - nw.y + 1);

		int wid = nPlotsWide * cellSizeX; 
		int hyt = nPlotsHigh * cellSizeY;
		
		g.setColor(Color.BLUE);
		g.drawRect(ov_nw.x + 1, ov_nw.y + 1, wid, hyt);
	}

	private Point infoPlotPoint;

}
