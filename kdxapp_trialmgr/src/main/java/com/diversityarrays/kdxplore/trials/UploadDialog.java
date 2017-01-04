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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import com.diversityarrays.dalclient.DALClient;

import net.pearcan.ui.widget.PromptTextField;

public class UploadDialog extends JDialog {

	private PromptTextField operatorNameField = new PromptTextField("Enter Operator Name");
	
	private PromptTextField sampleTypeField = new PromptTextField("Enter Sample Type Id");
	
	private Consumer<String> consumer;
	
	private Action proceed = new AbstractAction("Upload Trial") {
		@Override
		public void actionPerformed(ActionEvent e) {
			consumer.accept(operatorNameField.getText());
		}		
	};
	
	public UploadDialog(Consumer<String> consumer) {
		this.setLayout(new BorderLayout());
		this.consumer = consumer;	
		this.sampleTypeField.setText("0");
		
		this.setPreferredSize(new Dimension(500, 100));
		Box nameBox = Box.createHorizontalBox();
		nameBox.add(new JLabel("Operator Name: "));
		nameBox.add(operatorNameField);
		this.add(nameBox, BorderLayout.NORTH);
		this.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER);
		Box holderBox = Box.createHorizontalBox();
		holderBox.add(new JLabel("Sample Type: "));
		holderBox.add(sampleTypeField);
		holderBox.add(new JSeparator(JSeparator.VERTICAL));
		holderBox.add(new JButton(proceed));
		this.add(holderBox, BorderLayout.SOUTH);	
		this.pack();
	}
	
}
