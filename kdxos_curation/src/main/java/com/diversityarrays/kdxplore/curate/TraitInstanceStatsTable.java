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

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JTable;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Html;

import net.pearcan.util.StringUtil;

class TraitInstanceStatsTable extends JTable {

    private final TIStatsTableModel tiStatsTableModel;
    private int lineLengthLimit = KdxplorePreferences.getInstance().getTooltipLineLengthLimit();

	public TraitInstanceStatsTable(/*TraitInstanceStatsTableModel*/ TIStatsTableModel model) {
		super(model.getTableModel());
		this.tiStatsTableModel = model;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		Point point = event.getPoint();
		int vcol = columnAtPoint(event.getPoint());
		if (vcol >= 0) {
			int mcol = convertColumnIndexToModel(vcol);
			if (mcol >= 0 && mcol == tiStatsTableModel.getTraitInstanceNameColumnIndex()) { //TraitInstanceStatsTableModel.TRAIT_INSTANCE_NAME_COLUMN_INDEX) {
				int vrow = rowAtPoint(point);
				if (vrow >= 0) {
					int mrow = convertRowIndexToModel(vrow);
					if (mrow >= 0) {
						event.consume();
						
						Trait trait = tiStatsTableModel.getTrait(mrow);
						if (trait == null) {
							return null;
						}

						StringBuilder html = new StringBuilder("<HTML>");
						html.append("<b>")
							.append(StringUtil.htmlEscape(trait.getTraitName()))
							.append("</b>");
						
						String desc = trait.getTraitDescription();
						if (Check.isEmpty(desc)) {
							html.append("<BR>-- No Description Available --");
						}
						else {
							Html.appendHtmlLines(html, desc, lineLengthLimit);
						}
						return html.toString();
					}
				}

			}
		}
		return super.getToolTipText(event);
	}
	
}
