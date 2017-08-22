/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.curate;

import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import javax.swing.JFrame;

import org.apache.commons.collections15.Predicate;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.vistool.VisualisationTool;

import net.pearcan.ui.desktop.DesktopObject;

public interface CurationContext {
    
    static public final String PROP_SHOW_ALIAS = "showAlias";
    static public final String PROP_SHOW_TRAIT_LEVEL = "showTraitLevel";
	
	public Window getDialogOwnerWindow();

	// from CurationTableModel
//	public List<Comparable<?>> getSampleValues(TraitInstance ti);

	public Map<Plot,Map<Integer,KdxSample>> getPlotSampleMeasurements(TraitInstance traitInstance, Predicate<Plot> plotFilter);

	public Map<PlotOrSpecimen,KdxSample> getSampleMeasurements(TraitInstance traitInstance, Predicate<Plot> plotFilter);
	public List<CurationCellValue> getCurationCellValuesForPlot(PlotOrSpecimen pos);
	
	// from TraitInstanceChoiceTableModel
	public Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance();

	// VisualisationTool calls this to add a new window
	public JFrame addVisualisationToolUI(DesktopObject dobj);
	public IntFunction<Trait> getTraitProvider();
	
	public void errorMessage(VisualisationTool tool, String message);
	
//	public String getTraitInstanceName(TraitInstance traitInstance);
	public TraitColorProvider getTraitColorProvider();

	public Trial getTrial();
	
	public Map<PlotAttribute, Set<String>> getPlotAttributesAndValues();

	PlotInfoProvider getPlotInfoProvider();

	TraitValue getTraitValue(TraitInstance traitInstance, PlotOrSpecimen pos);

	
	// These affect the rendering of Trait names
	boolean getShowAliasForTraits();
    void setShowAliasForTraits(boolean b);
    
    boolean getShowTraitLevelPrefix();
    void setShowTraitLevelPrefix(boolean b);
	
    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);

    String makeTraitInstanceName(TraitInstance ti);
}
