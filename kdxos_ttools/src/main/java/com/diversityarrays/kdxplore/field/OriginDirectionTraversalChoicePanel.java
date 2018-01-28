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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.R;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.OrOrTr;
import com.diversityarrays.util.Orientation;
import com.diversityarrays.util.Origin;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.Traversal;
import com.diversityarrays.util.UpDownLeftRight;

import net.pearcan.util.GBH;

/*
 *  .--->           <---.
 *  |    (o)      (o)   |
 *  v                   v
 *   (o)             (o)
 *
 *
 *   (o)             (o)
 *  ^                   ^
 *  |    (o)      (o)   |
 *  `--->           <---'
 *
 *  Traversal: (o) 1-w  (o) 2-w
 */

public class OriginDirectionTraversalChoicePanel extends JPanel implements OrOrTr {


	static enum CardName {
		UL_RIGHT(Origin.UPPER_LEFT, UpDownLeftRight.RIGHT),
		UL_DOWN(Origin.UPPER_LEFT, UpDownLeftRight.DOWN),
		LL_RIGHT(Origin.LOWER_LEFT, UpDownLeftRight.RIGHT),
		LL_UP(Origin.LOWER_LEFT, UpDownLeftRight.UP),
		UR_LEFT(Origin.UPPER_RIGHT, UpDownLeftRight.LEFT),
		UR_DOWN(Origin.UPPER_RIGHT, UpDownLeftRight.DOWN),
		LR_LEFT(Origin.LOWER_RIGHT, UpDownLeftRight.LEFT),
		LR_UP(Origin.LOWER_RIGHT, UpDownLeftRight.UP);

		public final Origin origin;
		public final UpDownLeftRight direction;

		CardName(Origin org, UpDownLeftRight dirn) {
			this.origin = org;
			direction = dirn;
		}
	}

	private Origin origin;
	private UpDownLeftRight direction;
	private Traversal traversal;
	private Orientation orientation;

	private void setCurrentOD(Origin o, UpDownLeftRight d) {
		origin = o;
		direction = d;
	}

	private void setCurrentTO(Pair<Traversal,Orientation> cto) {
		traversal = cto.first;
		orientation = cto.second;
		maybeFirePropertyChanged();
	}

	private boolean initialised;

	private final ActionListener otrbListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			OT_RadioButton otrb = (OT_RadioButton) e.getSource();
			setCurrentTO(otrb.getTraversalAndOrientation());
		}
	};

	private CornerDirectionRadioButton current_crb;

	private final ButtonGroup crb_bg = new ButtonGroup();

	private final ActionListener crbl = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			Boolean rb1_selected = null;
			if (current_crb != null) {
				rb1_selected = current_crb.rbPair.rb1.isSelected();
			}

			current_crb = (CornerDirectionRadioButton) e.getSource();
			if (rb1_selected != null) {
				if (rb1_selected) {
					current_crb.rbPair.rb1.setSelected(true);
				}
				else {
					current_crb.rbPair.rb2.setSelected(true);
				}
			}

			CardName cname = current_crb.rbPair.cardName;
			rbCardLayout.show(rbPanel, cname.name());

			setCurrentOD(cname.origin, cname.direction);
			setCurrentTO(current_crb.rbPair.getTraversalAndOrientation());
		}
	};

    private final List<RbPair> rbPairs = new ArrayList<>();
    private final List<CornerDirectionRadioButton> cornerDirectionButtons = new ArrayList<>();
    private final List<CornerLabel> cornerLabels = new ArrayList<>();
    private final List<OT_RadioButton> orientationTraversalButtons = new ArrayList<>();

	class CornerLabel extends JLabel {

		public final Origin origin;

		CornerLabel(Origin origin, ImageId imageId) {
			super(KDClientUtils.getIcon(imageId));
			this.origin = origin;

			cornerLabels.add(this);
		}
	}

	class OT_RadioButton extends JRadioButton {

		public final int drawableId;
		public final Orientation orientation;
		public final Traversal traversal;
		public final ImageId imageId;

		public OT_RadioButton(Orientation o, Traversal t, int drawableId, ImageId imageId) {
			super("");
			this.orientation = o;
			this.traversal = t;
			this.drawableId = drawableId;
			this.imageId = imageId;

			addActionListener(otrbListener);

			orientationTraversalButtons.add(this);
		}

		@Override
		public String toString() {
		    return "OT_Radio[" + orientation + " / " + traversal + "]";
		}

		public Pair<Traversal, Orientation> getTraversalAndOrientation() {
			return new Pair<>(traversal, orientation);
		}
	}

	class RbPair extends JPanel {

		public final CardName cardName;
		public final OT_RadioButton rb1;
		public final OT_RadioButton rb2;

        @Override
        public String toString() {
            return "RbPair[" + cardName + ": " + rb1 + " || " + rb2 + "]";
        }

		RbPair(CardName cname, OT_RadioButton b1, OT_RadioButton b2) {

		    rbPairs.add(this);

			new BoxLayout(this, BoxLayout.X_AXIS);

			cardName = cname;
			rbPanel.add(this, cardName.name());

			this.rb1 = b1;
			this.rb2 = b2;

			rb1.setSelected(true);

			ButtonGroup bg = new ButtonGroup();
			bg.add(rb1);
			bg.add(rb2);

			JLabel rb1Label = new JLabel(KDClientUtils.getIcon(rb1.imageId));
			rb1Label.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					rb1.doClick();
				}
			});
			JLabel rb2Label = new JLabel(KDClientUtils.getIcon(rb2.imageId));
			rb2Label.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					rb2.doClick();
				}
			});

			add(rb1);
			add(rb1Label);
			add(Box.createHorizontalStrut(20));
			add(rb2);
			add(rb2Label);
		}



		public Pair<Traversal, Orientation> getTraversalAndOrientation() {
			OT_RadioButton rb = rb1.isSelected() ? rb1 : rb2;
			TrialLayout tl = new TrialLayout();
			tl.setOrientation(rb.orientation);
			//				tl.setOrigin);
			return rb.getTraversalAndOrientation();
		}

        public boolean isEitherButtonEnabled() {
            return rb1.isEnabled() || rb2.isEnabled();
        }
	}

	class CornerDirectionRadioButton extends JRadioButton {

		public final RbPair rbPair;

		CornerDirectionRadioButton(RbPair rbPair) {
		    cornerDirectionButtons.add(this);

			this.rbPair = rbPair;

			crb_bg.add(this);
			addActionListener(crbl);

			crbByCardName.put(rbPair.cardName, this);
		}

		@Override
        public String toString() {
			return rbPair.rb1.orientation  + "/" + rbPair.rb1.imageId + ":" + rbPair.rb2.imageId;
		}
	}

	Map<CardName, CornerDirectionRadioButton> crbByCardName = new HashMap<>();

	CardLayout rbCardLayout = new CardLayout();
	JPanel rbPanel = new JPanel(rbCardLayout);

	CornerLabel corner_ul = new CornerLabel(Origin.UPPER_LEFT,  ImageId.CORNER_UL);
	CornerLabel corner_ur = new CornerLabel(Origin.UPPER_RIGHT, ImageId.CORNER_UR);
	CornerLabel corner_ll = new CornerLabel(Origin.LOWER_LEFT,  ImageId.CORNER_LL);
	CornerLabel corner_lr = new CornerLabel(Origin.LOWER_RIGHT, ImageId.CORNER_LR);

	RbPair rbPair_ul_right = new RbPair(CardName.UL_RIGHT,
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.ONE_WAY, R.drawable.oneway_ul_right, ImageId.ONEWAY_UL_RIGHT),
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.TWO_WAY, R.drawable.twoway_ul_right, ImageId.TWOWAY_UL_RIGHT));

	RbPair rbPair_ul_down  = new RbPair(CardName.UL_DOWN,
			new OT_RadioButton(Orientation.VERTICAL, Traversal.ONE_WAY, R.drawable.oneway_ul_down, ImageId.ONEWAY_UL_DOWN),
			new OT_RadioButton(Orientation.VERTICAL, Traversal.TWO_WAY, R.drawable.twoway_ul_down, ImageId.TWOWAY_UL_DOWN));

	RbPair rbPair_ll_right = new RbPair(CardName.LL_RIGHT,
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.ONE_WAY, R.drawable.oneway_ll_right, ImageId.ONEWAY_LL_RIGHT),
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.TWO_WAY, R.drawable.twoway_ll_right, ImageId.TWOWAY_LL_RIGHT));

	RbPair rbPair_ll_up    = new RbPair(CardName.LL_UP,
			new OT_RadioButton(Orientation.VERTICAL, Traversal.ONE_WAY, R.drawable.oneway_ll_up, ImageId.ONEWAY_LL_UP),
			new OT_RadioButton(Orientation.VERTICAL, Traversal.TWO_WAY, R.drawable.twoway_ll_up, ImageId.TWOWAY_LL_UP));

	RbPair rbPair_ur_left = new RbPair(CardName.UR_LEFT,
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.ONE_WAY, R.drawable.oneway_ur_left, ImageId.ONEWAY_UR_LEFT),
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.TWO_WAY, R.drawable.twoway_ur_left, ImageId.TWOWAY_UR_LEFT));

	RbPair rbPair_ur_down = new RbPair(CardName.UR_DOWN,
			new OT_RadioButton(Orientation.VERTICAL, Traversal.ONE_WAY, R.drawable.oneway_ur_down, ImageId.ONEWAY_UR_DOWN),
			new OT_RadioButton(Orientation.VERTICAL, Traversal.TWO_WAY, R.drawable.twoway_ur_down, ImageId.TWOWAY_UR_DOWN));

	RbPair rbPair_lr_left = new RbPair(CardName.LR_LEFT,
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.ONE_WAY, R.drawable.oneway_lr_left, ImageId.ONEWAY_LR_LEFT),
			new OT_RadioButton(Orientation.HORIZONTAL, Traversal.TWO_WAY, R.drawable.twoway_lr_left, ImageId.TWOWAY_LR_LEFT));

	RbPair rbPair_lr_up = new RbPair(CardName.LR_UP,
			new OT_RadioButton(Orientation.VERTICAL, Traversal.ONE_WAY, R.drawable.oneway_lr_up, ImageId.ONEWAY_LR_UP),
			new OT_RadioButton(Orientation.VERTICAL, Traversal.TWO_WAY, R.drawable.twoway_lr_up, ImageId.TWOWAY_LR_UP));

	CornerDirectionRadioButton crb_ul_right = new CornerDirectionRadioButton(rbPair_ul_right);
	CornerDirectionRadioButton crb_ul_down = new CornerDirectionRadioButton(rbPair_ul_down);

	CornerDirectionRadioButton crb_ll_right = new CornerDirectionRadioButton(rbPair_ll_right);
	CornerDirectionRadioButton crb_ll_up  = new CornerDirectionRadioButton(rbPair_ll_up);

	CornerDirectionRadioButton crb_ur_left = new CornerDirectionRadioButton(rbPair_ur_left);
	CornerDirectionRadioButton crb_ur_down = new CornerDirectionRadioButton(rbPair_ur_down);

	CornerDirectionRadioButton crb_lr_left = new CornerDirectionRadioButton(rbPair_lr_left);
	CornerDirectionRadioButton crb_lr_up = new CornerDirectionRadioButton(rbPair_lr_up);

	private final Closure<Void> onChange;

	public OriginDirectionTraversalChoicePanel(Closure<Void> onChange) {

		this.onChange = onChange;

		Border lineBorder = new LineBorder(Color.BLACK);
		JComponent topsep, leftsep, rightsep, botsep;

		topsep = new JSeparator(JSeparator.VERTICAL);
		botsep = new JSeparator(JSeparator.VERTICAL);

		topsep.setBorder(lineBorder);
		botsep.setBorder(lineBorder);

		leftsep = new JSeparator(JSeparator.HORIZONTAL);
		rightsep = new JSeparator(JSeparator.HORIZONTAL);

		GBH gbh = new GBH(this);
		int y = 0;


		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, corner_ul);
		gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, crb_ul_right);
//		gbh.add(2,y, 1,2, GBH.VERT, 1,1, GBH.CENTER, topsep);
		gbh.add(3,y, 1,1, GBH.NONE, 1,1, GBH.EAST, crb_ur_left);
		gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, corner_ur);
		++y;

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, withInsets(crb_ul_down, new Insets(10,0,0,0)));
		//			gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, xxx);
		//			gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, xxx);
		gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, withInsets(crb_ur_down, new Insets(10,0,0,0)));
		++y;

		gbh.add(0,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, leftsep);
		JLabel label = gbh.add(1,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, "Origin & Direction");
		gbh.add(4,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, rightsep);
		++y;
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setBorder(lineBorder);

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, withInsets(crb_ll_up, new Insets(0,0,10,0)));
		//			gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, xxx);
//		gbh.add(2,y, 1,2, GBH.VERT, 1,1, GBH.CENTER, botsep);
		//			gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, xxx);
		gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.CENTER,withInsets( crb_lr_up, new Insets(0,0,10,0)));
		++y;

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, corner_ll);
		gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, crb_ll_right);
		gbh.add(3,y, 1,1, GBH.NONE, 1,1, GBH.EAST, crb_lr_left);
		gbh.add(4,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, corner_lr);
		++y;

		gbh.add(0,y, 5,1, GBH.HORZ, 1,1, GBH.CENTER, new JSeparator(JSeparator.HORIZONTAL));
		++y;

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Traversal:");
		gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, rbPanel);
		++y;
	}

	private JComponent withInsets(JComponent comp, Insets insets) {
		comp.setBorder(new EmptyBorder(insets));
		return comp;
	}

	private void maybeFirePropertyChanged() {
		if (! initialised) {
			return;
		}
		if (origin != null && direction != null && traversal != null && orientation != null) {
			if (initialised) {
				onChange.execute(null);
			}
		}
	}

	@Override
	public Orientation getOrientation() {
		return orientation;
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}

	@Override
	public Traversal getTraversal() {
		return traversal;
	}

	public void setOnlyAllow(OrOrTr ... oots) {
	    List<OrOrTr> list = Arrays.asList(oots);

	    Set<Orientation> orientationSet = list.stream()
	        .map(OrOrTr::getOrientation)
	        .collect(Collectors.toSet());

	    Set<Origin> originSet = list.stream()
	        .map(OrOrTr::getOrigin)
	        .collect(Collectors.toSet());

	    Set<Traversal> traversalSet = list.stream()
	        .map(OrOrTr::getTraversal)
	        .collect(Collectors.toSet());

	    BiConsumer<RbPair, OT_RadioButton> consumer = new BiConsumer<RbPair, OT_RadioButton>() {
            @Override
            public void accept(RbPair rbPair, OT_RadioButton btn) {
                boolean enable = orientationSet.contains(btn.orientation)
                                &&
                            traversalSet.contains(btn.traversal)
                            &&
                            originSet.contains(rbPair.cardName.origin);
                btn.setEnabled(enable);
            }
        };
	    for (RbPair rbp : rbPairs) {
            consumer.accept(rbp, rbp.rb1);
            consumer.accept(rbp, rbp.rb2);
	    }

	    for (CornerLabel label : cornerLabels) {
	        label.setEnabled(originSet.contains(label.origin));
	    }

	    for (CornerDirectionRadioButton cdrb : cornerDirectionButtons) {
	        cdrb.setEnabled(cdrb.rbPair.isEitherButtonEnabled());
	    }

	    for (OT_RadioButton rb : orientationTraversalButtons) {
	        rb.setEnabled(orientationSet.contains(rb.orientation)
	                &&
	                traversalSet.contains(rb.traversal));
	    }
	}

	public void setOrOrTr(OrOrTr tl) {
		CardName found = null;
		for (CardName cname : CardName.values()) {
			if (cname.origin == tl.getOrigin()) {
				switch (tl.getOrientation()) {
				case HORIZONTAL:
					if (cname.direction.isHorizontal()) {
						found = cname;
						break;
					}
					break;
				case VERTICAL:
					if (cname.direction.isVertical()) {
						found = cname;
						break;
					}
					break;
				}
			}

			if (found != null) {
				break;
			}
		}

		if (found == null) {
			found = CardName.LL_RIGHT;
		}

		CornerDirectionRadioButton crb = crbByCardName.get(found);
		crb.doClick();


		RbPair rbPair = crb.rbPair;
		switch (tl.getTraversal()) {
		case ONE_WAY:
			rbPair.rb1.doClick();
			break;
		case TWO_WAY:
			rbPair.rb2.doClick();
			break;
		}

		initialised = true;
	}

}
