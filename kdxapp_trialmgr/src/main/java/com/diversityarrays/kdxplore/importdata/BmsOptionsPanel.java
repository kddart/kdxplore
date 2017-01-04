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
package com.diversityarrays.kdxplore.importdata;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.diversityarrays.kdxplore.importdata.bms.BmsConstant;
import com.diversityarrays.kdxplore.importdata.bms.BmsImportOptions;

import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.GBH;

public class BmsOptionsPanel extends JPanel {
	
	private final JCheckBox optionSortBmsTrialAttributes = new JCheckBox("Sort Trial Attributes", true);
	private final PromptTextField tagFieldPrefixes = new PromptTextField("comma-separated prefixes for 'Tag' columns");
	private final JComboBox<String> instanceSeparatorCombo = new JComboBox<>(BmsConstant.TI_SEPARATOR_OPTIONS);
	private final JLabel betweenLabel = new JLabel(
			"(between " + BmsConstant.CIMMYT_TITLE + " and " + BmsConstant.CIMMYT_TRIAL_INSTANCE + ")");
	private final List<JLabel> labels = new ArrayList<>();		

	BmsOptionsPanel() {
		
		setBorder(new TitledBorder("Options for Database XLS"));
		
		optionSortBmsTrialAttributes.setToolTipText("Trial Attributes will be sorted alphabetically");
		
		instanceSeparatorCombo.setSelectedIndex(0);
		instanceSeparatorCombo.setEditable(true);

		labels.add(betweenLabel);
		
		GBH gbh = new GBH(this);
		int y = 0;
		
		Box hb = Box.createHorizontalBox();
		hb.add(instanceSeparatorCombo);
		hb.add(betweenLabel);
		hb.add(Box.createHorizontalGlue());
		labels.add(gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Instance# Separator:"));
		gbh.add(1,y, 1,1, GBH.HORZ, 2 ,1, GBH.CENTER, hb);
		++y;
		
		labels.add(gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Tag VARIATE Prefixes:"));
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, tagFieldPrefixes);
		++y;
		
		gbh.add(0,y, 2,1, GBH.NONE, 0,1, GBH.WEST, optionSortBmsTrialAttributes);
		++y;
		
	}
	
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		
		instanceSeparatorCombo.setEnabled(b);
		tagFieldPrefixes.setEnabled(b);
		optionSortBmsTrialAttributes.setEnabled(b);

		for (JLabel label : labels) {
			label.setEnabled(b);
		}
	}
	
	public BmsImportOptions getBmsImportOptions() {
		BmsImportOptions options = new BmsImportOptions();
		options.trialNameInstanceSeparator = instanceSeparatorCombo.getSelectedItem().toString();
		options.sortTrialAttributesByName = optionSortBmsTrialAttributes.isSelected();
		
		String tmp = tagFieldPrefixes.getText().trim();
		if (! tmp.isEmpty()) {
			for (String s : tmp.split(",")) {
				s = s.trim();
				if (! s.isEmpty()) {
					options.tagFieldPrefixes.add(s);
				}
			}
		}
		return options;
	}
}
