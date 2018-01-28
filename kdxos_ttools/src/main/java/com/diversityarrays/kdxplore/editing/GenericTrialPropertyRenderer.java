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
package com.diversityarrays.kdxplore.editing;

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import net.pearcan.util.StringUtil;
import android.content.Context;

import com.diversityarrays.kdsmart.KDSmartApplication;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;

public class GenericTrialPropertyRenderer extends DefaultTableCellRenderer {
	
	static public final Color TRIAL_ATTR_BORDER_COLOR = Color.decode("#0099ff"); //$NON-NLS-1$
	static public final Color PLOT_ATTR_BORDER_COLOR = Color.decode("#996633"); //$NON-NLS-1$

	private final Border trialBorder = new LineBorder(TRIAL_ATTR_BORDER_COLOR);
	private final Border plotBorder = new LineBorder(PLOT_ATTR_BORDER_COLOR);

	private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd"); //$NON-NLS-1$

	private final String X_0DF  = Pattern.quote("0x00ddff"); //$NON-NLS-1$

	private final Context context;
	public GenericTrialPropertyRenderer() {
		context = KDSmartApplication.getInstance();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column) 
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);

		TableModel tm = table.getModel();
		if (tm instanceof TrialPropertiesTableModel) {
			doTrialPropertiesTableModel(table, value, isSelected, row,
					(TrialPropertiesTableModel) tm);
		}
		else {
			setToolTipText(null);
		}

		return this;
	}

	public void doTrialPropertiesTableModel(JTable table, Object value,
			boolean isSelected, int row, TrialPropertiesTableModel tptm) 
	{
		Trial trial = tptm.getTrial();
		String ttt = null;
		if (trial != null) {
			if (value instanceof TrialLayout) {
				PlotIdentSummary plotIdentSummary = trial.getPlotIdentSummary();
				if (plotIdentSummary!= null && (plotIdentSummary.hasXandY() || ! plotIdentSummary.plotIdentRange.isEmpty())) {
					TrialLayout trialLayout = (TrialLayout) value;
					String s = trialLayout.getUserVisibleString(
							KDSmartApplication.getInstance(), plotIdentSummary);
					setText(s);
				}
				else {
					setText("- No Plot Data -");
				}
			}
			else if (value instanceof PlotIdentOption) {
				String plotIdentDescription = ((PlotIdentOption) value)
						.createPlotIdentDescription(trial, "/"); //$NON-NLS-1$
				setText(plotIdentDescription);
			}
			else if (value instanceof Date) {
				setText(dateFormat.format(value));
			}
			else if (value instanceof PlotIdentSummary) {
				PlotIdentOption plotIdentOption = trial.getPlotIdentOption();
				String s = ((PlotIdentSummary) value).getUserVisibleString(trial,
						plotIdentOption,
						"; "); //$NON-NLS-1$
				setText(s);
			}
			else if (value instanceof TraitNameStyle) {
				TraitNameStyle traitNameStyle = (TraitNameStyle) value;
				String s = context.getString(traitNameStyle.htmlStringResourceId);
				if (s != null && ! s.isEmpty()) {
					setText("<HTML>" + s.replaceAll(X_0DF, "red"));  //$NON-NLS-1$//$NON-NLS-2$
				}
			}
			else {
				if (value instanceof Set) {
					Set<?> set = (Set<?>) value;
					int count = set.size();
					setText(count==1 ? "1 value" : (count + " values"));
					if (count <= 10) {
						if (count > 0) {
							ttt = StringUtil.join(",", set); //$NON-NLS-1$
						}
					}
					else if (count <= 20) {
					    ttt = set.stream()
					        .map(o -> o==null ? "" : StringUtil.htmlEscape(o.toString())) //$NON-NLS-1$
					        .collect(Collectors.joining("</LI><LI>", //$NON-NLS-1$
					                "<HTML>" + "Values:" + "<UL><LI>",  //$NON-NLS-1$ //$NON-NLS-3$
					                "</LI></UL>")); //$NON-NLS-1$
					}
					else {
						ttt = "too many values to show";
					}
				}
				int modelRow = table.convertRowIndexToModel(row);
				if (modelRow >= 0 && ! isSelected) {					
					switch (tptm.getRowType(modelRow)) {
					case PLOT_ATTRIBUTE:
						setBorder(plotBorder);
						break;
					case TRIAL_ATTRIBUTE:
						setBorder(trialBorder);
						break;
					case TRIAL_PROPERTY:
					case PLOT_COUNT:
					default:
						break;
					
					}
				}
			}
		}
		
		setToolTipText(ttt);
	}
}
