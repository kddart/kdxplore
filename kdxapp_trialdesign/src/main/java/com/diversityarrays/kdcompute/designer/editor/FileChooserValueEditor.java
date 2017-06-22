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
package com.diversityarrays.kdcompute.designer.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.diversityarrays.util.UnicodeChars;

/**
 * Implementation of ValueEditor that lets the user choose a file from the file system.
 * <p>
 * When implemented on a web page this corresponds to a <code>FILE</code> INPUT element.
 * @author brianp
 *
 */
public class FileChooserValueEditor extends AbstractValueEditor {

    private final JFileChooser fileChooser;
    private final JTextField textField = new JTextField();
    private final Action browse = new AbstractAction(UnicodeChars.ELLIPSIS) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(chooserParent)) {
                File f = fileChooser.getSelectedFile();
                textField.setText(f.getPath());
            }
        }

    };
    private final Box both = Box.createHorizontalBox();
    private final Component chooserParent;

    public FileChooserValueEditor(String initialValue, Component chooserParent, JFileChooser fileChooser) {
        this.fileChooser = fileChooser;
        this.chooserParent = chooserParent;
        both.add(textField);
        both.add(new JButton(browse));

        textField.setText(initialValue);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireStateChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireStateChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fireStateChanged();
            }

        });
    }

    @Override
    public JComponent getVisualComponent() {
        return both;
    }

    @Override
    public String getValue() {
        return textField.getText();
    }

    @Override
    public void setValue(String value) {
        textField.setText(value);
    }

}
