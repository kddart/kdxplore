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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.scoring.PlotsPerGroup;
import com.diversityarrays.util.OrOrTr;
import com.diversityarrays.util.Orientation;
import com.diversityarrays.util.Origin;
import com.diversityarrays.util.Traversal;
import com.diversityarrays.util.UnicodeChars;
import com.diversityarrays.util.VisitOrder2D;

import net.pearcan.util.GBH;

public class CollectionPathSetupDialog extends JDialog {

//	public boolean lockScoredTraits;
//	public boolean useWhiteBackground;

	public VisitOrder2D visitOrder;
	public PlotsPerGroup plotsPerGroup;

	private final Closure<Void> onChange = new Closure<Void>() {
		@Override
		public void execute(Void arg0) {
			updateVisitOrderChosen();
		}
	};

	private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
		@Override
		public void actionPerformed(ActionEvent e) {
			visitOrder = null;
			plotsPerGroup = null;
			dispose();
		}
	};

	private final Action useAction = new AbstractAction(UnicodeChars.CONFIRM_TICK) {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	};

	private final OriginDirectionTraversalChoicePanel odtChoicePanel = new OriginDirectionTraversalChoicePanel(onChange);

	private final JComboBox<PlotsPerGroup> plotsPerGroupChoice = new JComboBox<>(PlotsPerGroup.values());

	public CollectionPathSetupDialog(Window owner, String title) {
	    this(owner, title, true);
	}

	public CollectionPathSetupDialog(Window owner, String title, boolean wantPlotsPerGroup) {
		super(owner, title, ModalityType.APPLICATION_MODAL);

		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(cancelAction));
		buttons.add(new JButton(useAction));

		Container cp = getContentPane();
		if (wantPlotsPerGroup) {
	        plotsPerGroupChoice.addItemListener(new ItemListener() {
	            @Override
	            public void itemStateChanged(ItemEvent e) {
	                plotsPerGroup = (PlotsPerGroup) plotsPerGroupChoice.getSelectedItem();
	            }
	        });

	        JPanel ppgPanel = new JPanel();
	        GBH gbh = new GBH(ppgPanel);
	        int y = 0;
	        gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Plots Per Group:");
	        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, plotsPerGroupChoice);
	        ++y;
	        cp.add(ppgPanel, BorderLayout.NORTH);
		}

		cp.add(odtChoicePanel, BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);

		pack();
	}

	private void updateVisitOrderChosen() {
		Traversal traversal = odtChoicePanel.getTraversal();
		Origin origin = odtChoicePanel.getOrigin();
		Orientation orientation = odtChoicePanel.getOrientation();
//		System.out.println("SSD: update " + origin + "/" + orientation + "/" + traversal);
		updateVisitOrderChosen(origin, orientation, traversal);
	}

	private void updateVisitOrderChosen(Origin origin, Orientation orientation, Traversal traversal) {

		switch (origin) {
		case LOWER_LEFT:
			switch (orientation) {
			case HORIZONTAL:
				visitOrder = Traversal.ONE_WAY==traversal
					? VisitOrder2D.LL_RIGHT_ZIGZAG
					: VisitOrder2D.LL_RIGHT_SERPENTINE;
				break;
			case VERTICAL:
				visitOrder = Traversal.ONE_WAY==traversal
				? VisitOrder2D.LL_UP_ZIGZAG
				: VisitOrder2D.LL_UP_SERPENTINE;
				break;
			default:
				break;
			}
			break;

		case LOWER_RIGHT:
			switch (orientation) {
			case HORIZONTAL:
				visitOrder = Traversal.ONE_WAY==traversal
					? VisitOrder2D.LR_LEFT_ZIGZAG
					: VisitOrder2D.LR_LEFT_SERPENTINE;
				break;
			case VERTICAL:
				visitOrder = Traversal.ONE_WAY==traversal
				? VisitOrder2D.LR_UP_ZIGZAG
				: VisitOrder2D.LR_UP_SERPENTINE;
				break;
			default:
				break;
			}
			break;

		case UPPER_LEFT:
			switch (orientation) {
			case HORIZONTAL:
				visitOrder = Traversal.ONE_WAY==traversal
					? VisitOrder2D.UL_RIGHT_ZIGZAG
					: VisitOrder2D.UL_RIGHT_SERPENTINE;
				break;
			case VERTICAL:
				visitOrder = Traversal.ONE_WAY==traversal
				? VisitOrder2D.UL_DOWN_ZIGZAG
				: VisitOrder2D.UL_DOWN_SERPENTINE;
				break;
			default:
				break;
			}

			break;

		case UPPER_RIGHT:
			switch (orientation) {
			case HORIZONTAL:
				visitOrder = Traversal.ONE_WAY==traversal
					? VisitOrder2D.UR_LEFT_ZIGZAG
					: VisitOrder2D.UR_LEFT_SERPENTINE;
				break;
			case VERTICAL:
				visitOrder = Traversal.ONE_WAY==traversal
				? VisitOrder2D.UR_DOWN_ZIGZAG
				: VisitOrder2D.UR_DOWN_SERPENTINE;
				break;
			default:
				break;
			}

			break;
		default:
			break;

		}
	}

    public void setOnlyAllow(PlotsPerGroup ppg, OrOrTr ... orts) {
        OrOrTr first = orts[0];
        odtChoicePanel.setOnlyAllow(orts);
        setOrOrTr(first, ppg);

        String msg;
        if (orts.length == 1) {
            msg = "<HTML>For now, only supporting:<BR>" + orts[0].toString();
        }
        else {
            msg = Arrays.asList(orts).stream()
            .map(oot -> oot.toString())
            .collect(Collectors.joining("<BR>", "<HTML>For now, only supporting:<BR>", ""));
        }

        JLabel label = new JLabel(msg, JLabel.CENTER);
        label.setForeground(Color.RED);
        getContentPane().add(label, BorderLayout.NORTH);
//        setGlassPane(label);
//        label.setVisible(true);
    }

    public void setOrOrTr(OrOrTr ort, PlotsPerGroup ppg) {

		updateVisitOrderChosen(ort.getOrigin(),
				ort.getOrientation(),
				ort.getTraversal());

		odtChoicePanel.setOrOrTr(ort);

		plotsPerGroup = ppg;
	}

//	@Override
//	public void setVisible(boolean b) {
//		GuiUtil.centreOnOwner(this);
//		super.setVisible(b);
//	}

}
