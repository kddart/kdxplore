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
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.GBH;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdsmart.field.PlotIdFieldLayoutProcessor;
import com.diversityarrays.kdsmart.scoring.IntRange;

/**
 * When X and Y are not available so we only have the PlotId,
 * let the user choose how to layout the cells and
 * and then create a FieldLayout that contains the Plot.plotId for
 * each cell.
 * @author brianp
 */

public class PlotIdTrialLayoutPane extends JPanel implements TrialLayoutPane {

	private static final boolean DEBUG = false;

	private final SpinnerNumberModel runLengthModel = new SpinnerNumberModel(1, 1, 100, 1);
	private final NumberSpinner runLengthSpinner = new NumberSpinner(runLengthModel, "0");

	private final SpinnerNumberModel plotIdModel = new SpinnerNumberModel(1, 1, 100, 1);
	private final NumberSpinner plotSpinner = new NumberSpinner(plotIdModel, "0");

	private final JLabel plotIdentInfo = new JLabel();

	private final JLabel fieldLayoutRunLengthLE_0 = new JLabel("LE 0");
	private final JPanel fieldLayoutRunLengthGT_0 = new JPanel();
	
	private PlotIdentSummary plotIdentSummary;
	
	private final Map<Integer,PlotName> plotNameByPlotId = new HashMap<>();
	private final Closure<Void> onChange = new Closure<Void>() {
		@Override
		public void execute(Void arg) {
			firePropertyChange(PROP_LAYOUT_CHANGED, false, true);
		}
	};
	
	private OriginDirectionTraversalChoicePanel odtPanel = new OriginDirectionTraversalChoicePanel(onChange);

	private boolean initialised;
	
	public PlotIdTrialLayoutPane() {
		super(new BorderLayout());

		/*
		 *  *  .--->           <---.
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
		 *  
		 *  (above is in odtPanel)
		 *  ---------------------------
		 *  
		 *  Run Length:     [         ]
		 *  Origin Plot Id: [         ]
		 */

		JPanel top = new JPanel();

		GBH gbh = new GBH(top);
		int y = 0;

		gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Plot Ids:");
		gbh.add(1,y, 3,1, GBH.HORZ, 1,1, GBH.WEST, plotIdentInfo);
		++y;

		// - - - - - - -

		JPanel bottom = new JPanel();
		GBH rgbh = new GBH(bottom);
		y = 0;

		rgbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Run Length:");
		rgbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, runLengthSpinner);
		++y;
		rgbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "Origin Plot Id:");
		rgbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, plotSpinner);
		++y;

		// - - - - - - -
		Box main = Box.createVerticalBox();
		main.add(top);
		main.add(withInsets(new JSeparator(JSeparator.HORIZONTAL), new Insets(0,20,0,20)));
		main.add(odtPanel);
		main.add(withInsets(new JSeparator(JSeparator.HORIZONTAL), new Insets(0,20,0,20)));
		
		main.add(bottom);

		add(main, BorderLayout.CENTER);

		//			crb_ll_right.doClick();

		ChangeListener changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (initialised) {
					firePropertyChange(PROP_LAYOUT_CHANGED, false, true);
				}
			}
		};
		runLengthModel.addChangeListener(changeListener);
		plotIdModel.addChangeListener(changeListener );

		initialised = true;
	}


	@Override
	public void setTrial(Trial trial, List<PlotName> plotNames) {
		initialised = false;
		
		try {
    		plotNameByPlotId.clear();
    		if (plotNames != null) {
    			for (PlotName pn : plotNames) {
    				plotNameByPlotId.put(pn.getPlotId(), pn);
    			}
    		}
    		
    		plotIdentSummary = trial.getPlotIdentSummary();
    		if (plotIdentSummary.hasXandY()) {
    			// Shouldn't be here !
    			return;
    		}
    		TrialLayout trialLayout = trial.getTrialLayout();
    		setTrialLayout(trialLayout);
    
    		if (plotIdentSummary.plotIdentRange.isEmpty()) {
    			plotIdentInfo.setText("");
    
    			// No PlotIds
    			plotSpinner.setEnabled(false);
    			runLengthSpinner.setEnabled(false);
    
    		}
    		else {
    
    			plotIdentInfo.setText(plotIdentSummary.plotIdentRange.getDescription());
    
    			plotSpinner.setEnabled(true);
    
    			int minPlotId = plotIdentSummary.plotIdentRange.getMinimum();
    
    			plotIdModel.setMaximum(plotIdentSummary.plotIdentRange.getMaximum());
    			plotIdModel.setMinimum(minPlotId);
    			int originPlotId = trialLayout.getOriginPlotId();
    			plotIdModel.setValue(Math.max(minPlotId, originPlotId));
    		}
    
    		int runLengthMin = 1;			
    		int runLengthMax = 0;
    
    		IntRange xRange = plotIdentSummary.xColumnRange;
    		if (! xRange.isEmpty()) {
    			runLengthMin = xRange.getMinimum();
    			runLengthMax = xRange.getMaximum();
    		}
    
    		IntRange yRange = plotIdentSummary.yRowRange;
    		if (! yRange.isEmpty()) {
    			runLengthMin = Math.min(runLengthMin, yRange.getMinimum());
    			runLengthMax = Math.max(runLengthMax, yRange.getMaximum());
    		}
    		
    		if (runLengthMax == 0) {
    			runLengthMax = plotIdentSummary.plotIdentRange.getMaximum();
    		}
    		
    		
    		runLengthModel.setMaximum(runLengthMax);
    		runLengthModel.setMinimum(runLengthMin);
    		int runLength = trialLayout.getRunLength();
    		
    		int value = Math.min(Math.max(runLengthMin, runLength), runLengthMax);
    		runLengthModel.setValue(value);
		}
		finally {
		    initialised = true;
		}
		
		firePropertyChange(PROP_LAYOUT_CHANGED, false, true);
	}
	
	@Override
	public JComponent getFieldLayoutPane(FieldLayout<Integer>[] returnLayout) {
		int runLength = runLengthModel.getNumber().intValue();
		if (runLength <= 0) {
			fieldLayoutRunLengthLE_0.setText("RunLength=" + runLength);
			return fieldLayoutRunLengthLE_0;
		}

		int firstPlotId = plotIdModel.getNumber().intValue();
		
		PlotIdFieldLayoutProcessor layoutProcessor = new PlotIdFieldLayoutProcessor();
		FieldLayout<Integer> fieldLayout = layoutProcessor.layoutField(
				plotIdentSummary, 
				odtPanel.getOrigin(), 
				firstPlotId, 
				odtPanel.getOrientation(), 
				runLength, 
				odtPanel.getTraversal());
		
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
		
		fieldLayoutRunLengthGT_0.setLayout(gridLayout);
		
		fieldLayoutRunLengthGT_0.removeAll();
		for (int y = 0; y < fieldLayout.ysize; ++y) {
			for (int x = 0; x < fieldLayout.xsize; ++x) {
				Integer plotId = fieldLayout.cells[y][x];
				
				String label_s;
				if (plotId == null) {
					label_s = ".";
				}
				else {
					PlotName plotName = plotNameByPlotId.get(plotId);
					if (plotName == null) {
						label_s = "-";
					}
					else {
						//s = "P_" + plotId + ":" + x + "," + y;
						StringBuilder sb = new StringBuilder("P_");
						sb.append(plotName.getPlotId());
						Integer xx = plotName.getX();
						Integer yy = plotName.getY();
						
						if (xx!=null || yy!=null) {
							sb.append(": ");
							if (xx!=null) {
								sb.append(xx);
							}
							sb.append(",");
							if (yy != null) {
								sb.append(yy);
							}
						}
						label_s = sb.toString();
					}
				}
				JLabel label = new JLabel("<HTML><BR>" + label_s + "<BR>&nbsp;");
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setBorder(border);
				fieldLayoutRunLengthGT_0.add(label);
			}
		}

		return fieldLayoutRunLengthGT_0;
	}
	
//	private void store(FieldLayout<Triad> cellArray, int plotId, int x, int y) {
//		cellArray.store(new Triad(plotId, x, y), x, y);
//	}
//
//	private void prepareTwoWay(FieldLayout<Triad> cellArray, int firstPlotId,
//			int lastPlotId, IntRange xRange, IntRange yRange) {
//		switch (origin) {
//		case LOWER_LEFT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer y : yRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer x : evenOdd.forward(xRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer x : xRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer y : evenOdd.reverse(yRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			break;
//			
//		case LOWER_RIGHT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer y : yRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer x : evenOdd.reverse(xRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer x : xRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer y : evenOdd.reverse(yRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			break;
//			
//		case UPPER_LEFT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				
//				for (Integer y : yRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer x : evenOdd.forward(xRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer x : xRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer y : evenOdd.forward(yRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			break;
//			
//		case UPPER_RIGHT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer y : yRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer x : evenOdd.reverse(xRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				EvenOdd evenOdd = EvenOdd.EVEN;
//				for (Integer x : xRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId+"\tevenOdd=" + evenOdd);
//					for (Integer y : evenOdd.forward(yRange)) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//					evenOdd = evenOdd.other();
//				}
//			}
//			break;
//		}
//	}
//
//	private void prepareOneWay(FieldLayout<Triad> cellArray, int firstPlotId,
//			int lastPlotId, IntRange xRange, IntRange yRange) {
//		switch (origin) {
//		case LOWER_LEFT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				for (Integer y : yRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer x : xRange.forward()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				for (Integer x : xRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer y : yRange.reverse()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			break;
//			
//		case LOWER_RIGHT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				for (Integer y : yRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer x : xRange.reverse()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				for (Integer x : xRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer y : yRange.reverse()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			break;
//			
//		case UPPER_LEFT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				for (Integer y : yRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer x : xRange.forward()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				for (Integer x : xRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer y : yRange.forward()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			break;
//			
//		case UPPER_RIGHT:
//			if (Orientation.HORIZONTAL==orientation) {
//				int plotId = firstPlotId;
//				for (Integer y : yRange.forward()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer x : xRange.reverse()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			else {
//				int plotId = firstPlotId;
//				for (Integer x : xRange.reverse()) {
//					if (DEBUG) System.out.println("\tplotId=" + plotId);
//					for (Integer y : yRange.forward()) {
//						store(cellArray, plotId, x, y);
//						++plotId;
//						if (plotId > lastPlotId) {
//							break;
//						}
//					}
//					if (plotId > lastPlotId) {
//						break;
//					}
//				}
//			}
//			break;
//		}
//	}
//	
	static class Triad {
		public final int plotId;
		public final int x;
		public final int y;
		Triad(int p, int x, int y) {
			plotId = p;
			this.x = x;
			this.y = y;
		}
		
		public String toString() {
			if (plotId > 0) {
				return x+","+y+"=" + "P." + plotId;
			}
			return x+","+y;
		}
	}
	
	@Override
	public void setTrialLayout(TrialLayout tl) {
		
		odtPanel.setOrOrTr(tl);
//		CardName found = null;
//		for (CardName cname : CardName.values()) {
//			if (cname.origin == tl.getOrigin()) {
//				switch (tl.getOrientation()) {
//				case HORIZONTAL:
//					if (cname.direction.isHorizontal()) {
//						found = cname;
//						break;
//					}
//					break;
//				case VERTICAL:
//					if (cname.direction.isVertical()) {
//						found = cname;
//						break;
//					}
//					break;
//				}
//			}
//
//			if (found != null) {
//				break;
//			}
//		}
//
//		if (found == null) {
//			found = CardName.LL_RIGHT;
//		}
//
//		CornerDirectionRadioButton crb = crbByCardName.get(found);
//		crb.doClick();
//
//
//		RbPair rbPair = crb.rbPair;
//		switch (tl.getTraversal()) {
//		case ONE_WAY:
//			rbPair.rb1.doClick();
//			break;
//		case TWO_WAY:
//			rbPair.rb2.doClick();
//			break;
//		}
	}

	@Override
	public TrialLayout getTrialLayout() {
		TrialLayout result = new TrialLayout();

		result.setOrientation(odtPanel.getOrientation());
		result.setOrigin(odtPanel.getOrigin());
		result.setTraversal(odtPanel.getTraversal());
		result.setRunLength(runLengthModel.getNumber().intValue());
		result.setOriginPlotId(plotIdModel.getNumber().intValue());

		return result;
	}
	
	private JComponent withInsets(JComponent comp, Insets insets) {
		comp.setBorder(new EmptyBorder(insets));
		return comp;
	}
}
