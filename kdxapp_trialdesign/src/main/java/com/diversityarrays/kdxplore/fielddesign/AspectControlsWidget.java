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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

@SuppressWarnings("nls")
public class AspectControlsWidget {

    private final Map<JRadioButton, CellShape> shapeByButton = new HashMap<>();
    private final ActionListener rbListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JRadioButton rb = (JRadioButton) e.getSource();
            if (rb.isSelected()) {
                CellShape shape = shapeByButton.get(rb);
                fieldView.setCellShape(shape);
            }
            else {
                System.out.println("Not selected: " + rb.getName());
            }
        }
    };

    private final FieldView<?> fieldView;
    private final Box aspectControls;

    public AspectControlsWidget(FieldView<?> fieldView, Orientation orientation) {
        this.fieldView = fieldView;

        aspectControls = orientation.createBox();

        aspectControls.setBorder(new CompoundBorder(new EmptyBorder(1, 1, 1, 1),
                new LineBorder(Color.LIGHT_GRAY)));
        aspectControls.setToolTipText("Choose shape for plots");

        if (Orientation.HORZ == orientation) {
            aspectControls.add(new JLabel("Aspect:"));
        }
        CellShape current = fieldView.getCellShape();
        ButtonGroup bg = new ButtonGroup();
        for (CellShape s : CellShape.values()) {
            BufferedImage[] images = s.createImages(16, Color.GRAY, Color.BLUE);
            ImageIcon unselectedIcon = new ImageIcon(images[0]);
            JRadioButton rb = new JRadioButton(unselectedIcon);
            rb.setName("radioButton." + s.name());
            rb.setSelectedIcon(new ImageIcon(images[1]));
            shapeByButton.put(rb, s);
            rb.setSelected(current == s);
            rb.addActionListener(rbListener);
            bg.add(rb);
            rb.setToolTipText("Select Plot shape");

            aspectControls.add(rb);
        }
    }

    public Component getWidgetComponent() {
        return aspectControls;
    }
}
