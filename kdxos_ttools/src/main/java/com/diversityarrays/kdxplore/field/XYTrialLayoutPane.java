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
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdsmart.field.XYFieldLayoutProcessor;
import com.diversityarrays.kdsmart.scoring.IntRange;
import com.diversityarrays.util.Origin;
import com.diversityarrays.util.XYPos;

/**
 * When only X and Y are being used, let the user choose which
 * and then create a FieldLayout that contains the Plot.plotId for
 * each cell.
 * @author brianp
 */
public class XYTrialLayoutPane extends JPanel implements TrialLayoutPane {

	private static final boolean DEBUG = true;


	JLabel xRangeInfo = new JLabel();
	JLabel yRangeInfo = new JLabel();

	private JComponent fieldLayoutPane;

	private PlotIdentSummary plotIdentSummary;

	private final Map<XYPos, PlotName> plotNameByXy = new LinkedHashMap<>();


	private final OriginChoicePanel originChoicePanel = new OriginChoicePanel(
	        "Select Field Origin",
	        (t) -> firePropertyChange(PROP_LAYOUT_CHANGED, false, true));

	public XYTrialLayoutPane() {
		super(new BorderLayout());

		Box rangeInfo = Box.createHorizontalBox();
		rangeInfo.add(Box.createHorizontalStrut(20));
		rangeInfo.add(new JLabel("X:"));
		rangeInfo.add(Box.createHorizontalStrut(10));
		rangeInfo.add(xRangeInfo);

		rangeInfo.add(Box.createHorizontalStrut(20));
		rangeInfo.add(new JLabel("Y:"));
		rangeInfo.add(Box.createHorizontalStrut(10));
		rangeInfo.add(yRangeInfo);


		Box box = Box.createVerticalBox();
		box.add(rangeInfo);
		box.add(new JSeparator(JSeparator.HORIZONTAL));
		box.add(originChoicePanel);
		box.add(Box.createHorizontalGlue());

		add( box, BorderLayout.NORTH);

	}

	private Map<XYPos,Integer> createPlotIdMap(Origin origin, IntRange xRange, IntRange yRange) {

		Map<XYPos,Integer> plotIdByXypos = new HashMap<>();

//		int nXdigits = (int) Math.ceil(Math.log10(xRange.getMaximum()));
		int nYdigits = (int) Math.ceil(Math.log10(yRange.getMaximum()));

		int yy = (int) Math.pow(10, nYdigits);

		for (Integer y : yRange.forward()) {
			int yPlotId = yy * y;
			for (Integer x : xRange.forward()) {
				int plotId = yPlotId + x;

				plotIdByXypos.put(new XYPos(x,y), plotId);
			}
		}

		return plotIdByXypos;
	}

	@Override
	public JComponent getFieldLayoutPane(FieldLayout<Integer>[] returnLayout) {

		Origin origin = originChoicePanel.getSelectedOrigin();

		IntRange xRange = plotIdentSummary.xColumnRange;
		IntRange yRange = plotIdentSummary.yRowRange;

		Map<XYPos,Integer> plotIdByXypos = new LinkedHashMap<>();
		Map<Integer,XYPos> xyposByPlotId = new HashMap<>();

		if (! plotNameByXy.isEmpty()) {
			if (plotIdentSummary.plotIdentRange.isEmpty()) {
				plotIdByXypos = createPlotIdMap(origin, xRange, yRange);
				for (XYPos xy : plotIdByXypos.keySet()) {
					Integer plotId = plotIdByXypos.get(xy);
					xyposByPlotId.put(plotId, xy);
				}
			}
			else {
				for (XYPos xypos : plotNameByXy.keySet()) {
					PlotName pn = plotNameByXy.get(xypos);
					plotIdByXypos.put(xypos, pn.getPlotId());
					xyposByPlotId.put(pn.getPlotId(), xypos);
				}
			}
		}

		XYFieldLayoutProcessor layoutProcessor = new XYFieldLayoutProcessor();

		FieldLayout<Integer> fieldLayout = layoutProcessor.layoutField(plotIdentSummary, origin, plotIdByXypos);
		if  (returnLayout != null && returnLayout.length > 0) {
			returnLayout[0] = fieldLayout;
		}

		Border insideBorder = new LineBorder(Color.BLACK);
		Border outsideBorder = new EmptyBorder(1,1,1,1);
		Border border = new CompoundBorder(outsideBorder, insideBorder);

		GridLayout gridLayout = new GridLayout(fieldLayout.ysize, fieldLayout.xsize);
		if (DEBUG) {
			System.out.println("GridLayout( rows=" + gridLayout.getRows() + " , cols=" + gridLayout.getColumns() + ")");
		}

		JPanel panel = new JPanel(gridLayout);
		for (int y = 0; y < fieldLayout.ysize; ++y) {
			for (int x = 0; x < fieldLayout.xsize; ++x) {
				Integer plotId = fieldLayout.cells[y][x];
				String label_s;
				if (plotId == null) {
					label_s = ".";
				}
				else {
					XYPos xy = xyposByPlotId.get(plotId);
					PlotName pn = plotNameByXy.get(xy);

					if (pn == null) {
						label_s = "-";
					}
					else {
						if (plotId <= 0) {
							label_s = x + "," + y;
						}
						else {
							label_s = "P_" + plotId + ": " + x + "," + y;
						}
					}
				}
				JLabel label = new JLabel("<HTML><BR>" + label_s + "<BR>&nbsp;");
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setBorder(border);
				panel.add(label);
			}
		}

		fieldLayoutPane = panel;

		return fieldLayoutPane;
	}

	@Override
	public TrialLayout getTrialLayout() {
		TrialLayout result = new TrialLayout();
		Origin origin = originChoicePanel.getSelectedOrigin();
		if (origin != null) {
			result.setOrigin(origin);
		}
		return result;
	}



	@Override
	public void setTrial(Trial trial, List<PlotName> plotNames) {
		System.out.println("--- XY.setTrial ---");

		plotNameByXy.clear();
		if (plotNames != null) {
			for (PlotName pn : plotNames) {
				XYPos xypos = new XYPos(pn.getX(), pn.getY());
				plotNameByXy.put(xypos, pn);
			}
		}

		plotIdentSummary = trial.getPlotIdentSummary();
		if (! plotIdentSummary.hasXandY()) {
			throw new IllegalArgumentException(plotIdentSummary + " must have X and Y");
		}

		setTrialLayout(trial.getTrialLayout());

		xRangeInfo.setText(plotIdentSummary.xColumnRange.getDescription());
		yRangeInfo.setText(plotIdentSummary.yRowRange.getDescription());

	}

	@Override
	public void setTrialLayout(TrialLayout tl) {
		originChoicePanel.setOrigin(tl.getOrigin());
	}


}
