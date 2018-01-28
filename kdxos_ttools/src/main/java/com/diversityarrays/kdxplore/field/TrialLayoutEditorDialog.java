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
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;

import net.pearcan.io.IOUtil;
import net.pearcan.ui.GuiUtil;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdsmart.scoring.IntRange;
import com.diversityarrays.util.Orientation;
import com.diversityarrays.util.Origin;
import com.diversityarrays.util.Traversal;

public class TrialLayoutEditorDialog extends JDialog {

	static public void main(String[] args) {

		TrialLayout tl = new TrialLayout();
		tl.setOrientation(Orientation.HORIZONTAL);
		tl.setOrigin(Origin.UPPER_LEFT);
		tl.setTraversal(Traversal.TWO_WAY);
		tl.setRunLength(5);
		tl.setOriginPlotId(1);
		
		IntRange x = new IntRange();
		IntRange y = new IntRange();
		IntRange p = new IntRange();
		
		List<PlotName> plotNames = new ArrayList<>();
		String rname;
		rname = "test-xy.txt";
		rname = "test-xyp.txt";
//		rname = "test-maize.txt";
//		rname = "test-p.txt";
		InputStream is = TrialLayoutEditorDialog.class.getResourceAsStream(rname);
		addPlotNames(plotNames, is);
		
		for (PlotName pn : plotNames) {
			Integer plotId = pn.getPlotId();
			if (plotId != null) {
				p.add(plotId);
			}
			Integer xx = pn.getX();
			if (xx != null) {
				x.add(xx);
			}
			Integer yy = pn.getY();
			if (yy != null) {
				y.add(yy);
			}
		}
		
		PlotIdentSummary pis = new PlotIdentSummary(p, x, y);

		PlotIdentOption pio = null;
		if (p.isEmpty()) {
			if (! x.isEmpty() && ! y.isEmpty()) {
				pio = PlotIdentOption.X_THEN_Y;
			}
		}
		else {
			if (x.isEmpty()) {
				if (y.isEmpty()) {
					pio = PlotIdentOption.PLOT_ID;
				}
				else {
					pio = PlotIdentOption.PLOT_ID_THEN_Y;
				}
			}
			else if (y.isEmpty()) {
				pio = PlotIdentOption.PLOT_ID_THEN_X;
			}
			else {
				pio = PlotIdentOption.PLOT_ID_THEN_XY;
			}
		}
		

		Trial trial = new Trial();
		trial.setTrialName("TEST TRIAL");
		trial.setPlotIdentSummary(pis);
		trial.setPlotIdentOption(pio);
		trial.setTrialLayout(tl);
		
		TrialLayoutEditorDialog editor = new TrialLayoutEditorDialog(null, "Trial Layout Editor");
		editor.setTrial(trial, plotNames);
//		editor.setDefaultCloseOperation(EXIT_ON_CLOSE);
		editor.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				System.out.println("TRialLayout=" + editor.trialLayout);
				System.exit(0);
			}
			
		});
		editor.setVisible(true);
	}


	private static void addPlotNames(List<PlotName> plotNames, InputStream is) {
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			while (null != (line = br.readLine())) {
				line = line.trim();
				if  (! line.isEmpty() && ! line.startsWith("#")) {
					addPlot(plotNames, line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtil.closeQuietly(br);
		}

	}
	
	
	private static void addPlot(List<PlotName> plotNames, String s) {
		plotNames.add(new PlotNameImpl(s));
	}


	TrialLayoutEditPanel trialLayoutEditPanel;

	public FieldLayout<Integer> fieldLayout;
//	private Trial trial;

	private TrialLayout trialLayout;
	
	private Action saveAction = new AbstractAction("Save") {
		@Override
		public void actionPerformed(ActionEvent e) {
//			trial.setTrialLayout(trialLayout);
			dispose();
		}
	};
	
	private Action cancelAction = new AbstractAction("Cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			trialLayout = null;
			fieldLayout = null;
			dispose();
		}
	};

	private Closure<TrialLayout> onLayoutComplete = new Closure<TrialLayout>() {
		
		@Override
		public void execute(TrialLayout tl) {
			trialLayout = tl;
			saveAction.setEnabled(trialLayout != null);
		}
	};

	
	
	public TrialLayoutEditorDialog(Window owner, String title) {
		super(owner, title, ModalityType.APPLICATION_MODAL);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		trialLayoutEditPanel = new TrialLayoutEditPanel(onLayoutComplete);
		
		saveAction.setEnabled(false);
		Box buttons = Box.createHorizontalBox();
		buttons.add(new JButton(cancelAction));
		buttons.add(new JButton(saveAction));
		
		Container cp = getContentPane();
		cp.add(trialLayoutEditPanel, BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);
		
		pack();
	}
	
	@Override
	public void setVisible(boolean b) {
		GuiUtil.centreOnOwner(this);
		super.setVisible(b);
	}
	
	public void setTrial(Trial trial, List<PlotName> plotNames) {

//		this.trial = trial;
		trialLayoutEditPanel.setTrial(trial, plotNames);

		pack();
		saveAction.setEnabled(false);
	}


	public TrialLayout getTrialLayout() {
		return trialLayout;
	}

}
