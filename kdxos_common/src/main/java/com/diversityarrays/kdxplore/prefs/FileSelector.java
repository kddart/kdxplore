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
package com.diversityarrays.kdxplore.prefs;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class FileSelector extends JPanel {
    
    static public enum ForWhat {
        INPUT_DIR,
        OUTPUT_DIR;
    }

	private ChangeListener changeListener;

	private File selectedFile;
	private JTextField filePath = new JTextField();

	protected ChangeEvent changeEvent = new ChangeEvent(this);

	private FileSelector.ForWhat forWhat;
    private String dialogTitle;

	private Action chooseAction = new AbstractAction(Msg.ACTION_CHOOSE()) {

		@Override
		public void actionPerformed(ActionEvent e) {

		    JFileChooser chooser;
		    KdxplorePreferences prefs = KdxplorePreferences.getInstance();
		    switch (forWhat) {
            case INPUT_DIR:
                chooser = prefs.getOutputFileChooser(dialogTitle, JFileChooser.DIRECTORIES_ONLY, false);
                break;
            case OUTPUT_DIR:
                chooser = prefs.getInputFileChooser(dialogTitle, JFileChooser.DIRECTORIES_ONLY, false);

                break;
            default:
                throw new RuntimeException("unhandled ForWhat=" + forWhat); //$NON-NLS-1$
		    }

		    if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(FileSelector.this)) {
		        File file = chooser.getSelectedFile();
                boolean changed = selectedFile==null ? true : ! selectedFile.equals(file);
                selectedFile = file;
                filePath.setText(selectedFile.getPath());
                if (changed && changeListener != null) {
                    changeListener.stateChanged(changeEvent);
                }
		    }
		}
	};

	FileSelector() {
		filePath.setEditable(false);
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(filePath);
		add(new JButton(chooseAction));
	}

	public void initialise(String dialogTitle, File value, ForWhat forWhat) {
		this.selectedFile = value;
        this.forWhat = forWhat;
        this.dialogTitle = dialogTitle;
        
		if (selectedFile==null) {
			filePath.setText(""); //$NON-NLS-1$
		}
		else {
			filePath.setText(selectedFile.getPath());
		}
		
	}

	public File getSelectedFile() {
		return selectedFile;
	}

	public void addChangeListener(ChangeListener changeListener) {
		this.changeListener = changeListener;
	}		
}
