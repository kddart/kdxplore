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

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * Let the user provide some Text with no "validation" at all.
 * @author brianp
 *
 */
public class TextValueEditor extends AbstractValueEditor {

    private final JTextField textField = new JTextField();
    public TextValueEditor(String initialValue) {
        textField.setText(initialValue);

        textField.getDocument().addDocumentListener(new DelayedDocumentListener());
    }

    @Override
    public JComponent getVisualComponent() {
        return textField;
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
