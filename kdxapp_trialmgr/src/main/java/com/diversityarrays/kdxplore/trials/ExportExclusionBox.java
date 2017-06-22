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
package com.diversityarrays.kdxplore.trials;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.diversityarrays.kdsmart.db.util.OmittedEntities;
import com.diversityarrays.kdxplore.ui.Toast;

public class ExportExclusionBox extends JPanel {

	private JCheckBox excludeSamples = new JCheckBox("Suppressed Samples");
	
	private JCheckBox excludePlots = new JCheckBox("Deactivated Plots");

	private JCheckBox excludeTraits = new JCheckBox("All Traits");
	
	private JLabel label = new JLabel("Export:");
	
	private boolean deactivated = true;
	
	public void selectAndDeactivateButtons(boolean sadb) {
//		excludePlots.setSelected(sadb);
//		excludeSamples.setSelected(sadb);
//		excludeTraits.setSelected(sadb);
		
		excludePlots.setEnabled(! sadb);
		excludeSamples.setEnabled(! sadb);
		excludeTraits.setEnabled(! sadb);
		
		deactivated = sadb;
	}
	
	public ExportExclusionBox() {
		super(new BorderLayout());
		
		Box hBox = Box.createHorizontalBox();
		hBox.add(label);
		hBox.add(excludeSamples);
		hBox.add(excludePlots);
//		hBox.add(excludeTraits);
		
		excludePlots.setSelected(true);
		excludeSamples.setSelected(true);
		excludeTraits.setSelected(true);
		
		MouseAdapter adapt = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (deactivated) {
					new Toast(hBox, "This export option is only for CSV!", Toast.SHORT).show();
				}
			}
		};
		
		excludePlots.addMouseListener(adapt);
		excludeSamples.addMouseListener(adapt);
		excludeTraits.addMouseListener(adapt);
		
		this.add(hBox, BorderLayout.CENTER);
	}
	
	public OmittedEntities createOmittedObject(Set<Integer> excludedPlots,
			Set<Integer> excludedTraits,
			Set<Integer> excludedSamples) {
		
		OmittedEntities result = new OmittedEntities(
				! excludePlots.isSelected() ? excludedPlots : Collections.emptySet(),
						! excludeTraits.isSelected() ? excludedTraits : Collections.emptySet(),
						! excludeSamples.isSelected() ? excludedSamples : Collections.emptySet());
		
		return result;	
	}
	
}
