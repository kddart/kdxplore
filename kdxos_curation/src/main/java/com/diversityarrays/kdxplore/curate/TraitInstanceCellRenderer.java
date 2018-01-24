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

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.TraitColorProvider;

public class TraitInstanceCellRenderer extends DefaultTableCellRenderer implements ListCellRenderer<TraitInstance> {
	
    /**
     * Provide a unique TraitInstance that can be used, for example, at the top of a 
     * JComboBox and we render it as "Trait to Edit" 
     */
	static public final TraitInstance TRAIT_TO_EDIT = new TraitInstance();
	
	DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();
	
	private final TraitColorProvider traitColorProvider;
	private Transformer<TraitInstance, String> instanceNameTransformer;

	public TraitInstanceCellRenderer(TraitColorProvider tcp,
			Transformer<TraitInstance,String> instanceNameTransformer) 
	{
		this.traitColorProvider = tcp;
		this.instanceNameTransformer = instanceNameTransformer;
	}
	
	private void initIcon(JLabel label, Object value) {
		Icon icon = null;
		if (value instanceof TraitInstance) {
			TraitInstance ti = (TraitInstance) value;
			if (TRAIT_TO_EDIT == ti) {
				label.setText("Trait to Edit");
			}
			else {
				icon = traitColorProvider.getTraitLegendTile(ti);
				label.setText(instanceNameTransformer.transform(ti));
			}
		}
		label.setIcon(icon);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends TraitInstance> list, TraitInstance value,
			int index, boolean isSelected, boolean cellHasFocus) 
	{
		listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		initIcon(listCellRenderer, value);
		
		return listCellRenderer;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		initIcon(this, value);

		return this;
	}
	
	
}
