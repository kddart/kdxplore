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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class PropertiesTableLegendPanel extends Box {
	private final Action trialAttributeLabel = new AbstractAction("Trial Attribute") {
		@Override
		public void actionPerformed(ActionEvent e) {
			List<Integer> rows = trialPropertiesTableModel.getTrialAttributeRowNumbers();
			selectTableRows(rows);
		}
	};
	private final Action plotAttributeLabel = new AbstractAction("Plot Attribute") {
		@Override
		public void actionPerformed(ActionEvent e) {
			List<Integer> rows = trialPropertiesTableModel.getPlotAttributeRowNumbers();
			selectTableRows(rows);
		}
	};
	private TrialPropertiesTableModel trialPropertiesTableModel;
	private JTable trialPropertiesTable;
	
	public PropertiesTableLegendPanel(JTable table) {
		super(BoxLayout.X_AXIS);
		
		JLabel label = new JLabel("<HTML><B>Bold &amp; Blue</B>: double-click Value to edit");
		label.setForeground(table.getSelectionBackground());
		add(label);
		
		TableModel tm = table.getModel();
		if (tm instanceof TrialPropertiesTableModel) {
			trialPropertiesTable = table;
			trialPropertiesTableModel = (TrialPropertiesTableModel) tm;
			
			final JButton ta_btn = new JButton(trialAttributeLabel);
			initialiseButton(ta_btn, GenericTrialPropertyRenderer.TRIAL_ATTR_BORDER_COLOR);
			final JButton pa_btn = new JButton(plotAttributeLabel);
			initialiseButton(pa_btn, GenericTrialPropertyRenderer.PLOT_ATTR_BORDER_COLOR);

			add(Box.createHorizontalGlue());
			add(ta_btn);
			add(Box.createHorizontalStrut(4));
			add(pa_btn);
			add(Box.createHorizontalStrut(10));
			
			trialPropertiesTableModel.addTableModelListener(new TableModelListener() {					
				@Override
				public void tableChanged(TableModelEvent e) {
					if (0 == e.getFirstRow() 
						&& Integer.MAX_VALUE == e.getLastRow()
						&& TableModelEvent.ALL_COLUMNS == e.getColumn()
						&& TableModelEvent.UPDATE == e.getType())
					{
						ta_btn.setVisible(trialPropertiesTableModel.hasAnyTrialAttributes());
						pa_btn.setVisible(trialPropertiesTableModel.hasAnyPlotAttributes());
					}
				}
			});
		}
	}

	private  void selectTableRows(List<Integer> modelRows) {
		ListSelectionModel sm = trialPropertiesTable.getSelectionModel();
		sm.clearSelection();
		int maxVrow = -1;
		for (Integer mrow : modelRows) {
			int vrow = trialPropertiesTable.convertRowIndexToView(mrow);
			if (vrow >= 0) {
				sm.addSelectionInterval(vrow,  vrow);
				maxVrow = Math.max(maxVrow, vrow);
			}
		}
		if (maxVrow >= 0) {
			trialPropertiesTable.scrollRectToVisible(
					trialPropertiesTable.getCellRect(maxVrow, 1, true));
		}
	}

	private void initialiseButton(JButton btn, Color colour) {
		btn.setFont(btn.getFont().deriveFont(Font.BOLD));
		btn.setForeground(colour);
//		btn.setBorder(new LineBorder(colour));
	}
}
