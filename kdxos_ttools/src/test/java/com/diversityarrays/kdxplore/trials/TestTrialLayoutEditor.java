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
package com.diversityarrays.kdxplore.trials;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import net.pearcan.ui.widget.MessagesPanel;
import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.GBH;

import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.scoring.IntRange;
import com.diversityarrays.kdxplore.field.PlotName;
import com.diversityarrays.kdxplore.field.TrialLayoutEditorDialog;
import com.diversityarrays.util.Orientation;
import com.diversityarrays.util.Origin;
import com.diversityarrays.util.Traversal;

public class TestTrialLayoutEditor extends JFrame {

	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new TestTrialLayoutEditor().setVisible(true);				
			}
		});

	}
	
	class RangePanel extends JPanel {
		SpinnerNumberModel minModel = new SpinnerNumberModel(1, 1, 100, 1);
		SpinnerNumberModel maxModel = new SpinnerNumberModel(1, 1, 100, 1);
		
		RangePanel(String label, int min, int max) {
			minModel.setValue(min);
			maxModel.setValue(max);
			
			GBH gbh = new GBH(this);
			int y= 0;
			
			gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Min:");
			gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new NumberSpinner(minModel, "0"));
			++y;
			gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Max:");
			gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new NumberSpinner(maxModel, "0"));
		}
		
		public IntRange getIntRange(boolean use) {
			IntRange result = new IntRange();
			if (use) {
				result.add(minModel.getNumber().intValue());
				result.add(maxModel.getNumber().intValue());
			}
			return result;
		}
	}
	
	JCheckBox usePlotId = new JCheckBox("Use PlotId", true);
	
	RangePanel plotRangePanel = new RangePanel("PlotId", 1, 41);
	
	JCheckBox useXY = new JCheckBox("Use X and Y", true);

	RangePanel xRangePanel = new RangePanel("X Col", 1, 8);
	RangePanel yRangePanel = new RangePanel("Y Row", 1, 12);
	
	JComboBox<Orientation> orientationCombo = new JComboBox<>(Orientation.values());
	JComboBox<Origin> originCombo = new JComboBox<>(Origin.values());
	JComboBox<Traversal> traversalCombo = new JComboBox<>(Traversal.values());
	
	SpinnerNumberModel originPlotIdModel = new SpinnerNumberModel(1, 1, 100, 1);
	SpinnerNumberModel runLengthModel = new SpinnerNumberModel(1, 1, 100, 1);
	
	Action useAction = new AbstractAction("Use") {
		@Override
		public void actionPerformed(ActionEvent e) {
			doTest();
		}
	};
	
	private MessagesPanel messages = new MessagesPanel();
	private JLabel originPlotIdLabel;
	private JLabel runLengthLabel;
	private NumberSpinner originPlotIdSpinner = new NumberSpinner(originPlotIdModel, "0");
	private NumberSpinner runLengthSpinner = new NumberSpinner(runLengthModel, "0");
	
	TestTrialLayoutEditor() {
		super("Test TrialLayoutEditor");
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		orientationCombo.setSelectedItem(Orientation.HORIZONTAL);
		originCombo.setSelectedItem(Origin.UPPER_LEFT);
		traversalCombo.setSelectedItem(Traversal.TWO_WAY);
		
		usePlotId.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateOriginAndRunLength();				
			}
		});
		
		JPanel main = new JPanel();
		GBH gbh = new GBH(main);
		int y = 0;
		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.NW, usePlotId);
		gbh.add(1,y, 1,1, GBH.BOTH, 1,1, GBH.CENTER, plotRangePanel);
		++y;
		
		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.NW, useXY);
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.NE, "-- X --");
		gbh.add(2,y, 1,1, GBH.HORZ, 1,1, GBH.NE, "-- Y --");
		++y;
		
		gbh.add(1,y, 1,1, GBH.BOTH, 2,1, GBH.CENTER, xRangePanel);
		gbh.add(2,y, 1,1, GBH.BOTH, 2,1, GBH.CENTER, yRangePanel);
		++y;
		
		originPlotIdLabel = 
		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Origin Plot Id:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, originPlotIdSpinner);
		++y;

		runLengthLabel = 
		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Run Length:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.WEST, runLengthSpinner);
		++y;
		
		
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(20));
		box.add(new JButton(useAction));
		box.add(Box.createHorizontalGlue());
		
		Container cp = getContentPane();
		cp.add(main, BorderLayout.CENTER);
		cp.add(box, BorderLayout.SOUTH);
		
		pack();
		
		updateOriginAndRunLength();
	}
	
	private void updateOriginAndRunLength() {
		boolean enb = usePlotId.isSelected();
		originPlotIdLabel.setEnabled(enb);
		originPlotIdSpinner.setEnabled(enb);
		runLengthLabel.setEnabled(enb);
		runLengthSpinner.setEnabled(enb);
	}

	private Orientation getSelectedOrientation() {
		return (Orientation) orientationCombo.getSelectedItem();
	}

	private Origin getSelectedOrigin() {
		return (Origin) originCombo.getSelectedItem();
	}
	
	private Traversal getSelectedTraversal() {
		return (Traversal) traversalCombo.getSelectedItem();
	}
	
//	private PlotIdentSummary getPlotIdentSummary() {
//		boolean xy = useXY.isSelected();
//		return new PlotIdentSummary(
//				plotRangePanel.getIntRange(usePlotId.isSelected()), 
//				xRangePanel.getIntRange(xy),
//				yRangePanel.getIntRange(xy));
//	}
	
	static class PlotNameImpl implements PlotName {
		public final int x;
		public final int y;
		public final Integer plotId;

		PlotNameImpl(int x, int y) {
			this.x = x;
			this.y = y;
			plotId = null;
		}
		
		PlotNameImpl(int x, int y, int p) {
			this.x = x;
			this.y = y;
			plotId = p;
		}
		
		@Override
		public String toString() {
			return getX() +"," + getY() + ":" + getPlotId();
		}

		@Override
		public Integer getX() {
			return x;
		}

		@Override
		public Integer getY() {
			return y;
		}

		@Override
		public Integer getPlotId() {
			return plotId;
		}
	}

	private void doTest() {
		
		boolean xy = useXY.isSelected();
		IntRange xRange = xRangePanel.getIntRange(xy);
		IntRange yRange = yRangePanel.getIntRange(xy);

		IntRange plotRange = plotRangePanel.getIntRange(usePlotId.isSelected());
		 
		
		List<PlotName> plotNames = new ArrayList<>();
		if (plotRange.isEmpty()) {
			if (! xRange.isEmpty() && ! yRange.isEmpty()) {
				for (Integer y : yRange.forward()) {
					for (Integer x : xRange.forward()) {
						plotNames.add(new PlotNameImpl(x, y));
					}
				}
			}
		}
		else {
			// We have plotId-s.
			if (! xRange.isEmpty() && ! yRange.isEmpty()) {
				int plotId = plotRange.getMinimum();
				for (Integer y : yRange.forward()) {
					for (Integer x : xRange.forward()) {
						plotNames.add(new PlotNameImpl(x, y, plotId));
						++plotId;
						if (! plotRange.contains(plotId)) {
							plotId = 0;
							break;
						}
					}
					
					if (plotId <= 0) {
						break;
					}
				}
			}
			else {
				// Only PlotId!
				int x  = 0;
				for (Integer plotId : plotRange.forward()) {
					plotNames.add(new PlotNameImpl(++x, 0, plotId));
				}
			}
		}
		
		
		PlotIdentSummary pis = new PlotIdentSummary(plotRange, xRange, yRange);
		
		PlotIdentOption pio = null;
		if (pis.plotIdentRange.isEmpty()) {
			if (! pis.xColumnRange.isEmpty() && ! pis.yRowRange.isEmpty()) {
				pio = PlotIdentOption.X_THEN_Y;
			}
		}
		else {
			if (pis.xColumnRange.isEmpty()) {
				if (pis.yRowRange.isEmpty()) {
					pio = PlotIdentOption.PLOT_ID;
				}
				else {
					pio = PlotIdentOption.PLOT_ID_THEN_Y;
				}
			}
			else if (pis.yRowRange.isEmpty()) {
				pio = PlotIdentOption.PLOT_ID_THEN_X;
			}
			else {
				pio = PlotIdentOption.PLOT_ID_THEN_XY;
			}
		}
		
		TrialLayout tl = new TrialLayout();
		tl.setOrientation(getSelectedOrientation());
		tl.setOrigin(getSelectedOrigin());
		tl.setTraversal(getSelectedTraversal());
		
		if (! pis.hasXandY()) {
			tl.setRunLength(runLengthModel.getNumber().intValue());
			tl.setOriginPlotId(originPlotIdModel.getNumber().intValue());
		}
		
		Trial trial = new Trial();
		trial.setTrialName("TEST TRIAL");
		trial.setPlotIdentSummary(pis);
		trial.setPlotIdentOption(pio);
		trial.setTrialLayout(tl);

		TrialLayoutEditorDialog tle = new TrialLayoutEditorDialog(TestTrialLayoutEditor.this, "Test TLE");
		tle.setTrial(trial, plotNames);
		tle.setVisible(true);
		
		tl = tle.getTrialLayout();
		if (tl != null) {
			messages.println("TrialLayout: " + tl);
		}
	}

}
