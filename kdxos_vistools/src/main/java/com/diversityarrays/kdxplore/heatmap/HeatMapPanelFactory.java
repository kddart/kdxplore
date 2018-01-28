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

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.vistool.Msg;
import com.diversityarrays.kdxplore.vistool.VisualisationTool;
import com.diversityarrays.util.Pair;

public class HeatMapPanelFactory {

	private final VisualisationTool visualisationTool;
	
	private final SuppressionHandler suppressionHandler;
	
	public HeatMapPanelFactory(VisualisationTool vtool, SuppressionHandler sh) {
		visualisationTool = vtool;
		this.suppressionHandler = sh;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
    public Pair<HeatMapPanelParameters,HeatMapPanel> createHeatMap(
			CurationContext context,
			SelectedValueStore svs,
			int uniqueId,
			ValueRetriever<?> xValueRetriever,
			final ValueRetriever<?> yValueRetriever,
			TraitInstanceValueRetriever<?> traitInstanceValueRetriever)
	{
		final TraitInstance zTraitInstance = traitInstanceValueRetriever.getTraitInstance();

        PlotInfoProvider plotInfoProvider = context.getPlotInfoProvider(); // new PlotInfoProviderImpl(zTraitInstance, plotByPlotId, sampleByPlotId);

        String title = Msg.TITLE_HEATMAP(context.makeTraitInstanceName(zTraitInstance));

        HeatMapPanel heatMapPanel;
		if (TraitLevel.PLOT == zTraitInstance.trait.getTraitLevel()) {

	        HeatMapModelData hmmd = new HeatMapModelData(
	                context, 
	                xValueRetriever, 
	                yValueRetriever, 
	                traitInstanceValueRetriever);

	        heatMapPanel = new HeatMapPanel(
	                visualisationTool,
	                uniqueId,
	                svs,
	                plotInfoProvider,
	                title,
	                
	                hmmd,
	                suppressionHandler)
	        ;

		}
		else {
            // === === === === === === === === === === === === === === === === === === ===
            // === === === === === === === === === === === === === === === === === === ===
            // === === === === === === === === === === === === === === === === === === ===
		    //
		    // FIXME replace this with sub-plot variants of HeatMapModelData and HeatMapPanel.
		    //
            // === === === === === === === === === === === === === === === === === === ===
            // === === === === === === === === === === === === === === === === === === ===
            // === === === === === === === === === === === === === === === === === === ===
            HeatMapModelData hmmd = new HeatMapModelData(
                    context, 
                    xValueRetriever, 
                    yValueRetriever, 
                    traitInstanceValueRetriever);

            heatMapPanel = new HeatMapPanel(
                    visualisationTool,
                    uniqueId,
                    svs,
                    plotInfoProvider,
                    title,
                    
                    hmmd,
                    suppressionHandler)
            ;
		}

		HeatMapPanelParameters params = new HeatMapPanelParameters(
				xValueRetriever.getDisplayName(), 
				yValueRetriever.getDisplayName(), 
				zTraitInstance,
				plotInfoProvider);
		
		return new Pair<>(params, heatMapPanel);
	}

}
