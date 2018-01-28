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
package com.diversityarrays.kdxplore.field;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.Origin;

import net.pearcan.util.GBH;

public class OriginChoicePanel extends JPanel {

	class CornerRadioButton extends JRadioButton {

		public final Origin origin;

		CornerRadioButton(Origin origin) {
			this.origin = origin;
			bg.add(this);
			cornerRadioButtons.add(this);
		}
	}

	private Origin selectedOrigin;

	private final ButtonGroup bg = new ButtonGroup();

	private final List<CornerRadioButton> cornerRadioButtons = new ArrayList<>();

	private final ActionListener rbListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			selectedOrigin = ((CornerRadioButton) e.getSource()).origin;
			onChange.accept(selectedOrigin);
		}
	};

	private final CornerRadioButton rb_ll = new CornerRadioButton(Origin.LOWER_LEFT);
	private final CornerRadioButton rb_lr = new CornerRadioButton(Origin.LOWER_RIGHT);
	private final CornerRadioButton rb_ul = new CornerRadioButton(Origin.UPPER_LEFT);
	private final CornerRadioButton rb_ur = new CornerRadioButton(Origin.UPPER_RIGHT);

	private final Consumer<Origin> onChange;

	public OriginChoicePanel(String prompt, Consumer<Origin> onChange) {

		this.onChange = onChange;

		rb_ll.addActionListener(rbListener);
		rb_lr.addActionListener(rbListener);
		rb_ul.addActionListener(rbListener);
		rb_ur.addActionListener(rbListener);

		JLabel label;
		GBH gbh = new GBH(this);
		int y = 0;

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, new JLabel(KDClientUtils.getIcon(ImageId.CORNER_UL)));
		gbh.add(3,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, new JLabel(KDClientUtils.getIcon(ImageId.CORNER_UR)));
		++y;

		gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, rb_ul);
		gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.EAST, rb_ur);
		++y;

		label = gbh.add(0,y, 4,1, GBH.HORZ, 1,1, GBH.CENTER, prompt);
		label.setHorizontalAlignment(JLabel.HORIZONTAL);
		++y;
		label.setBorder(new EmptyBorder(10, 0, 10, 0));

		gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, rb_ll);
		gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.EAST, rb_lr);
		++y;

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, new JLabel(KDClientUtils.getIcon(ImageId.CORNER_LL)));
		gbh.add(3,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, new JLabel(KDClientUtils.getIcon(ImageId.CORNER_LR)));
		++y;
	}

	public void setOrigin(Origin origin) {
		selectedOrigin = origin;
		for (CornerRadioButton crb : cornerRadioButtons) {
			if (origin == crb.origin) {
				crb.setSelected(true);
				break;
			}
		}
	}

	public Origin getSelectedOrigin() {
		Origin result = null;
		for (CornerRadioButton crb : cornerRadioButtons) {
			if (crb.isSelected()) {
				result = crb.origin;
				break;
			}
		}
		return result;
	}
}
