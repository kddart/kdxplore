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
package com.diversityarrays.kdxplore.vistool;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Bag;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.util.Check;

public class VisToolData {
	
	public final CurationContext context;
	public final List<PlotOrSpecimen> plotSpecimensToGraph;

	public final List<TraitInstance> traitInstances;
	
	public VisToolData(CurationContext context, 
			List<PlotOrSpecimen> posList,
			List<TraitInstance> selectedInstances) 
	{
		this.context = context;
		this.plotSpecimensToGraph = Check.isEmpty(posList) ? null : posList;
		this.traitInstances = new ArrayList<>(selectedInstances);
	}
	
	static public String createReportText(Bag<String> missingOrBad, Bag<String> suppressed) {
	    
	    String fmt = Msg.raw_MSG_VALUE_COLON_VALUE_COUNT();
	    
	    List<String> lines = new ArrayList<>();
	    
	    if (! missingOrBad.isEmpty()) {
	        lines.add(Msg.MSG_SOME_TRAIT_VALUES_NOT_PLOTTED());
	        lines.add("Missing or Invalid:");
	        appendLines(fmt, missingOrBad, lines);
	    }
	        
	    if (! suppressed.isEmpty()) {
	        if (missingOrBad.isEmpty()) {
	            lines.add(Msg.MSG_SOME_TRAIT_VALUES_NOT_PLOTTED());
	        }
	        lines.add("Suppressed:");
	        appendLines(fmt, suppressed, lines);    
	    }

	    return lines.stream().collect(Collectors.joining("\n")); //$NON-NLS-1$
	}
	
	static  private void appendLines(String fmt, Bag<String> bag, List<String> lines) {
	    for (String s : bag.uniqueSet()) {
	        lines.add("  " + MessageFormat.format(fmt, s, bag.getCount(s))); //$NON-NLS-1$
	    }
	}
}
