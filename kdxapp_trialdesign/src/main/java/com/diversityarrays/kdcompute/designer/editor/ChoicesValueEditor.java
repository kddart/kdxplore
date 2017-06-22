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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * ValueEditor implementation that lets the user choose from a number of prededined values.
 * @author brianp
 *
 */
public class ChoicesValueEditor extends AbstractValueEditor {

    private final JComponent component;

    private String value;
    private final List<String> choices;

    private JCheckBox checkBox;
    private Map<JRadioButton, String> valueByRb;
    private JComboBox<String> combo;

    public ChoicesValueEditor(String initialValue, List<String> inputChoices) {

        value = initialValue;
        choices = inputChoices;

        boolean valueInChoices = choices.contains(value);

        switch (choices.size()) {
        case 0:
            throw new IllegalArgumentException("must have at least one choice");
        case 1:
            checkBox = new JCheckBox(choices.get(0), valueInChoices);
            component = checkBox;
            checkBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    value = checkBox.isSelected() ? choices.get(0) : "";
                    fireStateChanged();
                }
            });
            break;
        case 2:
            valueByRb = new HashMap<>();
            ActionListener rbListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    value = valueByRb.get(e.getSource());
                    fireStateChanged();
                }
            };
            ButtonGroup bg = new ButtonGroup();
            JPanel buttons = new JPanel();
            component = buttons;
            for (String choice : choices) {
                JRadioButton rb = new JRadioButton(choice);
                rb.addActionListener(rbListener);
                valueByRb.put(rb, choice);
                rb.setSelected(choice.equals(initialValue));
                bg.add(rb);
                buttons.add(rb);
            }
            break;
        default:
            combo = new JComboBox<>(choices.toArray(new String[choices.size()]));
            component = combo;
            if  (choices.contains(initialValue)) {
                combo.setSelectedItem(initialValue);
            }
            else {
                combo.setSelectedIndex(0);
            }

            combo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    value = (String) combo.getSelectedItem();
                    fireStateChanged();
                }
            });
            break;
        }
    }

    @Override
    public void setValue(String value) {
        if (choices.contains(value)) {
            this.value = value;

            if (checkBox!=null) {
                checkBox.setSelected(true);
            }
            else if (combo != null) {
                combo.setSelectedItem(value);
            }
            else if (valueByRb != null) {
                Optional<JRadioButton> opt = valueByRb.entrySet().stream()
                    .filter(e -> e.getValue().equals(value))
                    .map(Map.Entry::getKey)
                    .findFirst();
                if (opt.isPresent()) {
                    opt.get().setSelected(true);
                }
            }
        }
    }


    @Override
    public JComponent getVisualComponent() {
        return component;
    }

    @Override
    public String getValue() {
        return value;
    }
}
