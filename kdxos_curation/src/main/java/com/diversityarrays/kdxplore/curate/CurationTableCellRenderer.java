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
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.ttools.shared.CommentMarker;
import com.diversityarrays.kdxplore.ttools.shared.SampleIconType;
import com.diversityarrays.kdxplore.ui.ColumnRows;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.SunSwingDefaultLookup;

import net.pearcan.color.ColorPair;
import net.pearcan.util.StringUtil;

/**
 * <dl>
 *  <dt>Underline:</dt>
 *  <dd>From database</dd>
 *  <dt>Italic:</dt>
 *  <dd>Needs curation</dd>
 *  <dt>Bold</dt>
 *  <dd>Edited sample: not suppressed</dd>
 *  <dt>Bold  &amp; Strike-through:</dt>
 *  <dd>Edited Sample: suppressed</dd>
 *  <dt>DottedUnderline:</dt>
 *  <dd>uncertain provenance</dd>
 * </dl>
 * @author brianp
 *
 */
public class CurationTableCellRenderer extends AbstractCurationTableCellRenderer {
	
	private static final boolean SHOW_BORDER = false;
	
	private static String FONT_LEGEND;

	static {
	    FONT_LEGEND = Msg.HTML_FONT_LEGEND();
	}
	
	static public String getFontDescriptorHtml(boolean raw) {
		return raw ? FONT_LEGEND : "<HTML>" + FONT_LEGEND;
	}

	private final NumberFormat integerFormat = new DecimalFormat("#");
	private final DateFormat dateFormat = TraitValue.getTraitValueDateFormat();

	private Color unselectedBackground = Color.WHITE;
	private Color unselectedForeground = Color.BLACK;

	private CurationTableModel curationTableModel;

	private final Font defaultFont;
	private final Font suppressedFont;
	private final Font boldSuppressedFont;
	private final Font boldFont;
	private final Font italicFont;
//	private final Font boldItalicFont;
	private final Font underlineFont;
	private final Font dottedUnderlineFont;
	
	private final Supplier<TraitColorProvider> colorProviderFactory;

	private final CurationTableSelectionModel curationTableSelectionModel;

	private final Color alternateColor;


	@SuppressWarnings("unchecked")
	public CurationTableCellRenderer(
			CurationTableModel curateModel, 
			Supplier<TraitColorProvider> colorProviderFactory, 
			CurationTableSelectionModel curationTableSelectionModel) 
	{
		
		setHorizontalAlignment(SwingConstants.CENTER);
		
		alternateColor = SunSwingDefaultLookup.getColor(this, ui, "Table.alternateRowColor");

		this.curationTableModel = curateModel;
		this.colorProviderFactory = colorProviderFactory;
		this.curationTableSelectionModel = curationTableSelectionModel;
		
		defaultFont = this.getFont();
		
		boldFont = defaultFont.deriveFont(Font.BOLD);
		italicFont = defaultFont.deriveFont(Font.ITALIC);
		
		@SuppressWarnings("rawtypes")
		Map attributes;
		
		attributes = defaultFont.getAttributes();
		attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		suppressedFont = new Font(attributes);
		
		attributes = boldFont.getAttributes();
		attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		boldSuppressedFont = new Font(attributes);
		
		attributes = defaultFont.getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		underlineFont = new Font(attributes);
		
		attributes = defaultFont.getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
		dottedUnderlineFont = new Font(attributes);
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int viewRow, int viewColumn)
	{
		if (table == null) {
			return this;
		}
		
		Color fg = null;
		Color bg = null;
		Font font = defaultFont;
		sampleIconType = SampleIconType.NORMAL;
		commentMarker = null;
		String ttt = "";

		JTable.DropLocation dropLocation = table.getDropLocation();
		if (dropLocation != null
				&& !dropLocation.isInsertRow()
				&& !dropLocation.isInsertColumn()
				&& dropLocation.getRow() == viewRow
				&& dropLocation.getColumn() == viewColumn) {

			fg = SunSwingDefaultLookup.getColor(this, ui, "Table.dropCellForeground");
			bg = SunSwingDefaultLookup.getColor(this, ui, "Table.dropCellBackground");

			isSelected = true;
		}

		TraitColorProvider traitColorProvider =  colorProviderFactory.get();
		
        int modelColumn = table.convertColumnIndexToModel(viewColumn);

        if (isSelected) {
			TraitInstance ti = curationTableModel.getTraitInstanceAt(modelColumn);
			
			ColorPair colorPair = traitColorProvider.getTraitInstanceColor(ti);
			if (colorPair != null) {
				setBackground(colorPair.getBackground());
				setForeground(Color.BLACK);
			} else {
				setForeground(fg == null ? table.getSelectionForeground() : fg);
				setBackground(bg == null ? table.getSelectionBackground() : bg);
			}
		} else {
			Color background = unselectedBackground != null
					? unselectedBackground
							: table.getBackground();
			if (alternateColor != null && (viewRow & 1) != 0) {
				background = alternateColor;
			}
			setForeground(unselectedForeground != null ? unselectedForeground : table.getForeground());
			setBackground(background);
		}

		if (hasFocus) {
			Border border = null;
			if (isSelected) {
				border = SunSwingDefaultLookup.getBorder(this, ui, "Table.focusSelectedCellHighlightBorder");
			}
			if (border == null) {
				border = SunSwingDefaultLookup.getBorder(this, ui, "Table.focusCellHighlightBorder");
			}
			setBorder(border);

			if (!isSelected && table.isCellEditable(viewRow, viewColumn)) {
				Color col;
				col = SunSwingDefaultLookup.getColor(this, ui, "Table.focusCellForeground");
				if (col != null) {
					setForeground(col);
				}
				col = SunSwingDefaultLookup.getColor(this, ui, "Table.focusCellBackground");
				if (col != null) {
					setBackground(col);
				}
			}
		} else {

			if (!SHOW_BORDER) {
				setBorder(BorderFactory.createEmptyBorder());
			}
			else {
				Color color = Color.gray;
				MatteBorder border = new MatteBorder(1, 1, 0, 0, color);
				setBorder(border);
			}
		}

		// do NOT just use the code below as this will prevent cells that are
		// selected via the VisualisationTools from highlighting.
		//
		// int modelRow = table.convertRowIndexToModel(viewRow);
		//
		// do NOT use just the above line - read the comment
		
		int modelRow = getModelRowAndSetFG_BG(modelColumn, table, isSelected, viewRow, traitColorProvider);
		
		KdxSample sample = null;

		if (modelRow >= 0) {
			Plot plot = curationTableModel.getPlotAtRowIndex(modelRow);
			if (plot!=null && ! plot.isActivated()) {
				sampleIconType = isSelected
						? SampleIconType.INACTIVE_PLOT_SELECTED
						: SampleIconType.INACTIVE_PLOT;
			}
			else {
				CurationCellValue ccv = curationTableModel.getCurationCellValue(modelRow, modelColumn);
				if (ccv == null) {
					sample = curationTableModel.getSample(modelRow, modelColumn);
				}
				else {
				    switch (ccv.getDeviceSampleStatus()) {
                    case MULTIPLE_SAMPLES_MANY_VALUES:
                        commentMarker = CommentMarker.MULTIPLE_VALUES;
                        ttt = commentMarker.toolTipText;
                        break;
                    case MULTIPLE_SAMPLES_ONE_VALUE:
                        commentMarker = CommentMarker.MULTIPLE_WITH_ONE_VALUE;
                        ttt = "Multiple Samples, Same value";
                        break;
                    case NO_DEVICE_SAMPLES:
                        break;
                    case ONE_DEVICE_SAMPLE:
                        break;
                    default:
                        break;
				    }

                    switch (ccv.getEditState()) {
					case CALCULATED:
					    font = boldFont;
					    sampleIconType = SampleIconType.CALCULATED;
					    break;

                    case MORE_RECENT:
                        // Note: may override MULTIPLE_SAMPLES if there is a more recent sample
                        Pair<CommentMarker,String> pair = moreRecentUnlessSameValue(ccv);
                        commentMarker = pair.first;
                        ttt = pair.second;

                        sample = ccv.getEditedSample();
                        font = boldFont;
                        break;

                    case CURATED:
                        sample = ccv.getEditedSample();
						font = boldFont;
						break;

					case RAW_SAMPLE:
						font = italicFont;
						sampleIconType = SampleIconType.RAW;
						break;

					case FROM_DATABASE:
						sampleIconType = SampleIconType.DATABASE;
						font = underlineFont;
						break;
					case NO_VALUES:
						break;
					default:
						break;
					}		
				}
			}
		}
		
		if (curationTableModel.isTemporaryValue(modelRow, modelColumn)) {
			font = dottedUnderlineFont;
		}
		else if (sample != null) {
			if (sample.isSuppressed()) {
				font = font==boldFont ? boldSuppressedFont : suppressedFont;
			}
		}	
		
		this.setFont(font);

		if (value instanceof TraitValue) {
		    TraitValue tv = (TraitValue) value;
			setValue(tv.displayValue);
		}
		else if (value instanceof Integer) {
			setValue(integerFormat.format(value));
		}
		else if (value instanceof Date) {
			setValue(dateFormat.format(value));
		}
		else if (value instanceof Double) {
			if (modelColumn >= 0) {
				String format = curationTableModel.getStringNumberFormatAt(modelColumn);
				if (format != null) {
					setValue(String.format(format, value));
				}
			}
		}
		else {
			setValue(value);
		}

		if (sample != null && sample.isSuppressed()) {
		    StringBuilder sb = new StringBuilder("<HTML>");
		    sb.append(ttt);
		    if (! Check.isEmpty(ttt)) {
		        sb.append("<BR>");
		    }
		    sb.append(StringUtil.htmlEscape(sample.getSuppressedReason()));
		    ttt = sb.toString();
		}
        setToolTipText(ttt);
		return this;
	}

	/**
	 * We can't just use <i>isSelected</i> because the ListSelectionModel on the curationTable is
	 * actually a CurationTableSelectionModel and the behaviour for selection is more complex than
	 * a standard table; we want to track the individual rows by the columns (for each TraitInstance)
	 * and so the standard way that JTable uses to detect "selection" isn't quite what we need.
	 */
    private int getModelRowAndSetFG_BG(int modelColumn, JTable table, boolean isSelected, int viewRow, TraitColorProvider traitColorProvider) {
        int modelRow = -1;
        if (modelColumn >= 0) {
            modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow >= 0) {
                List<ColumnRows> selectedColumnRows = curationTableSelectionModel.getSelectedColumnRows();
                
                for (ColumnRows crows : selectedColumnRows) {
                    Set<Integer> modelRows = crows.getRowsFor(modelColumn);
                    if (modelRows != null) {
                        if (modelRows.contains(modelRow)) {
                            TraitInstance ti = curationTableModel.getTraitInstanceAt(modelColumn);
                            
                            ColorPair colorPair = traitColorProvider.getTraitInstanceColor(ti);
                            if (colorPair != null) {
                                setBackground(colorPair.getBackground());
                                if (isSelected) {
                                    setForeground(Color.BLACK);
                                }
                            }
                        }
                    }
                }
            }
        }
        return modelRow;
    }
}
