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
package com.diversityarrays.kdxplore.heatmap;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.PlotAttributeValueRetriever;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetrieverFactory;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.vistool.AbstractVisToolPanel;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.VisToolPanel;
import com.diversityarrays.kdxplore.vistool.VisualisationTool;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

public class HeatMapParamsDialog extends JDialog {
	
	public interface VisToolOpenClose {
		void visToolCreated(JFrame frame, VisToolPanel panel);
		void visToolClosed(JFrame frame, VisToolPanel panel);
	}

	private final AskForPositionNamesAndTraitInstancePanel pnatPanel;
	
	private final Closure<Boolean> enableActionNotifier = new Closure<Boolean>() {
		@Override
		public void execute(Boolean anyChosen) {
			createHeatMapAction.setEnabled(anyChosen);
		}
	};

	private Action createHeatMapAction = new AbstractAction(Msg.ACTION_CREATE_HEATMAP()) {

		@Override
		public void actionPerformed(ActionEvent e) {
			Either<String,Pair<HeatMapPanelParameters,HeatMapPanel>> either = createHeatMap();
			
			if (! either.isRight()) {
				context.errorMessage(visualisationTool, either.left());
			}
			else {
				HeatMapPanelParameters hmpp = either.right().first;
				final AbstractVisToolPanel heatMapPanel = either.right().second;
				
				final JFrame frame = context.addVisualisationToolUI(heatMapPanel);
				
				visToolOpenClose.visToolCreated(frame, heatMapPanel);
				
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						frame.removeWindowListener(this);

						heatMapPanelParamsByFrame.remove(frame);

						visToolOpenClose.visToolClosed(frame, heatMapPanel);
					}
				});
				heatMapPanelParamsByFrame.put(frame, hmpp);
				
				pnatPanel.addHeatMapFrame(hmpp, frame);
			}
		}
	};
	
	private final SuppressionHandler suppressionHandler;
	private final CurationContext context;
	private final VisualisationTool visualisationTool;
	private final Map<JFrame, HeatMapPanelParameters> heatMapPanelParamsByFrame;

	private VisToolOpenClose visToolOpenClose;

	private final SelectedValueStore selectedValueStore;

	public HeatMapParamsDialog(VisualisationTool vtool, 
			CurationContext ctx, 
//			PlotInfoProvider pip,
			SelectedValueStore svs,
			Map<TraitInstance, SimpleStatistics<?>> numericStatsByTraitInstance,
			Map<JFrame,HeatMapPanelParameters> heatMapPanelParamsByFrame,
			VisToolOpenClose vtoc,
			SuppressionHandler suppressionHandler)
	{
		super(ctx.getDialogOwnerWindow(), Msg.TOOLNAME_HEATMAP(), ModalityType.MODELESS);
		
		this.suppressionHandler = suppressionHandler;
		this.visualisationTool = vtool;
		this.context = ctx;
		this.selectedValueStore = svs;
//		this.plotInfoProvider = pip;
		this.heatMapPanelParamsByFrame = heatMapPanelParamsByFrame;
		this.visToolOpenClose = vtoc;
		
//		this.heatMapFrameByPane = new HashMap<JFrame, HeatMapPane>();
		
		List<ValueRetriever<?>> positionAndPlotRetrievers = new ArrayList<>();
		Trial trial = context.getTrial();
		positionAndPlotRetrievers.addAll(ValueRetrieverFactory.getPlotIdentValueRetrievers(trial));
		
		Map<PlotAttribute, Set<String>> attributesAndValues = context.getPlotAttributesAndValues();
		
		for (PlotAttribute pa : attributesAndValues.keySet()) {
			positionAndPlotRetrievers.add(
					new PlotAttributeValueRetriever(pa, attributesAndValues.get(pa)));
		}
		
		pnatPanel = new AskForPositionNamesAndTraitInstancePanel(
				2, // positionNames 
				1, // traitInstances
				positionAndPlotRetrievers,
				numericStatsByTraitInstance,
				enableActionNotifier, context);
		
		createHeatMapAction.setEnabled(false);
		
		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(createHeatMapAction));
		
		getContentPane().add(pnatPanel, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);
		
		pack();
	}
	
	static private int nextId = 1;
	
	static public int getNextId() {
		return nextId++;
	}
	
	private TraitInstanceValueRetriever<?> findFirstTraitInstanceValueRetriever(List<ValueRetriever<?>> list) {
		TraitInstanceValueRetriever<?> tivr = null;
		for (ValueRetriever<?> vr : list) {
			if (vr instanceof TraitInstanceValueRetriever) {
				tivr = (TraitInstanceValueRetriever<?>) vr;
				break;
			}
		}
		return tivr;
	}
	
    @SuppressWarnings("rawtypes")
	private Either<String, Pair<HeatMapPanelParameters,HeatMapPanel>> createHeatMap() {
		
		AxesAndValues axesAndValues = pnatPanel.getAxesAndValues();
		
		final ValueRetriever<?> xvr = axesAndValues.xAxisValueRetriever;
		final ValueRetriever<?> yvr = axesAndValues.yAxisValueRetriever;

		if (xvr == null || yvr == null) {
			return Either.left(Msg.ERRMSG_X_AND_Y_AXES_MUST_BE_PROVIDED());
		}

		final TraitInstanceValueRetriever<?> plotValueRetriever =
				findFirstTraitInstanceValueRetriever(axesAndValues.zValueRetrievers);		
		if (plotValueRetriever == null) {
			return Either.left(Msg.ERRMSG_AT_LEAST_1_VALUE_SELECTION_MUST_BE_MADE());
		}
		
		HeatMapPanelFactory factory = new HeatMapPanelFactory(visualisationTool, suppressionHandler);
		
        Pair<HeatMapPanelParameters, HeatMapPanel> pair = factory.createHeatMap(
				context, selectedValueStore, getNextId(), xvr, yvr, plotValueRetriever);
		
		HeatMapPanelParameters params = pair.first;
		HeatMapPanel heatMapPanel = pair.second;
		
		return Either.right(new Pair<HeatMapPanelParameters,HeatMapPanel>(params, heatMapPanel));
	}
}
