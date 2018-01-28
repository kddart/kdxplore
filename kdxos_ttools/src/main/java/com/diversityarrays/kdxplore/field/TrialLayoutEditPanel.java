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
import java.awt.CardLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.field.FieldLayout;

public class TrialLayoutEditPanel extends JPanel {

	static private final String CARD_PLOT_ID = "plotId";
	static private final String CARD_X_AND_Y = "xAndY";
	static private final String CARD_NO_DATA = "noData";
	
	private PlotIdTrialLayoutPane plotIdTrialLayoutPane = new PlotIdTrialLayoutPane();
	private XYTrialLayoutPane xyTrialLayoutPane = new XYTrialLayoutPane();
	
	private NoDateTrialLayoutPane noDateTrialLayoutPane = new NoDateTrialLayoutPane();
	
	private CardLayout cardLayout = new CardLayout();
	private JPanel cardPanel = new JPanel(cardLayout);
	
	private TrialLayoutPane trialLayoutPane;
	
	private TrialLayout trialLayout;
	

	private PropertyChangeListener selectionChangedListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			updateUsingTrialLayoutPane();
		}
	};
	
	private JSplitPane splitPane;
	private FieldLayout<Integer> fieldLayout;
//	private Trial trial;
	
	private final Closure<TrialLayout> onLayoutComplete;
	
	private boolean hasPlotNames;
	
	public TrialLayoutEditPanel(Closure<TrialLayout> onLayoutComplete) {
		super(new BorderLayout());
		
		this.onLayoutComplete = onLayoutComplete;
		
		plotIdTrialLayoutPane.addPropertyChangeListener(TrialLayoutPane.PROP_LAYOUT_CHANGED, selectionChangedListener);
		xyTrialLayoutPane.addPropertyChangeListener(TrialLayoutPane.PROP_LAYOUT_CHANGED, selectionChangedListener);
		
		cardPanel.add(plotIdTrialLayoutPane, CARD_PLOT_ID);
		cardPanel.add(xyTrialLayoutPane, CARD_X_AND_Y);
		cardPanel.add(noDateTrialLayoutPane, CARD_NO_DATA);
		
//		saveAction.setEnabled(false);
//		Box buttons = Box.createHorizontalBox();
//		buttons.add(new JButton(cancelAction));
//		buttons.add(new JButton(saveAction));
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cardPanel, new JLabel());
		splitPane.setResizeWeight(0.0);
		
		add(splitPane, BorderLayout.CENTER);
//		add(buttons, BorderLayout.SOUTH);
	}
	
//	public Trial getTrial() {
//		return trial;
//	}
	
	public FieldLayout<Integer> getFieldLayout() {
		return fieldLayout;
	}
	
	public void setTrialLayout(TrialLayout tl) {
		trialLayoutPane.setTrialLayout(tl);
	}
	
	public TrialLayout getTrialLayout() {
		return trialLayout;
	}
	
	public void setTrial(Trial trial, List<PlotName> plotNames) {
		
//		this.trial = trial;
		String card;
		
		PlotIdentSummary pis = trial.getPlotIdentSummary();
		if (pis.hasXandY()) {
			trialLayoutPane = xyTrialLayoutPane;
			card = CARD_X_AND_Y;
		}
		else if (! pis.plotIdentRange.isEmpty()) {
			trialLayoutPane = plotIdTrialLayoutPane;
			card = CARD_PLOT_ID;
		}
		else {
			trialLayoutPane = noDateTrialLayoutPane;
			card = CARD_NO_DATA;
		}
		
		hasPlotNames = plotNames != null && ! plotNames.isEmpty();
		
		initialising = true;
		try {
			trialLayoutPane.setTrial(trial, plotNames);
		}
		finally {
			initialising = false;
		}
		
		cardLayout.show(cardPanel, card);
		
		updateUsingTrialLayoutPane();
//		pack();
//		saveAction.setEnabled(false);
	}
	
	boolean initialising = false;
	
	private void updateUsingTrialLayoutPane() {
		if (initialising) {
			return;
		}
		
		trialLayout = trialLayoutPane.getTrialLayout();
		onLayoutComplete.execute(trialLayout);
//		saveAction.setEnabled(trialLayout != null);
		
		if  (hasPlotNames) {
			JPanel holder = new JPanel(new BorderLayout());
			@SuppressWarnings("unchecked")
			FieldLayout<Integer>[] returnLayout = new FieldLayout[1];
			holder.add(trialLayoutPane.getFieldLayoutPane(returnLayout), BorderLayout.NORTH);
			fieldLayout = returnLayout[0];
			splitPane.setRightComponent(new JScrollPane(holder));
		}
	}

	class NoDateTrialLayoutPane extends JPanel implements TrialLayoutPane {
		
		private TrialLayout trialLayout = new TrialLayout();
		
		private JLabel label = new JLabel("No Trial Layout");
		
		public NoDateTrialLayoutPane() {
			super(new BorderLayout());
			
			add(new JLabel("Can't Edit Trial Layout yet"));
		}

		@Override
		public void setTrial(Trial trial, List<PlotName> plotNames) {
			setTrialLayout(trial.getTrialLayout());
		}

		@Override
		public void setTrialLayout(TrialLayout tl) {
			trialLayout = tl;
		}

		@Override
		public TrialLayout getTrialLayout() {
			return trialLayout;
		}

		@Override
		public JComponent getFieldLayoutPane(FieldLayout<Integer>[] returnLayout) {
			return label;
		}
		
	}
}
