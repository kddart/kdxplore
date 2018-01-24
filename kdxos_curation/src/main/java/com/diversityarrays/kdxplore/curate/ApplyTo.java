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

import java.util.ArrayList;
import java.util.List;

import com.diversityarrays.kdxplore.Vocab;

class ApplyTo {	

    CheckBoxType curatedType = new CheckBoxType(this);
    CheckBoxType uncuratedType = new CheckBoxType(this);
    CheckBoxType excludedType = new CheckBoxType(this);
    CheckBoxType includedType = new CheckBoxType(this);
    
	// Don't change curated samples unless used tells us to
	boolean curated = false;
	boolean excluded = true;
	boolean included = true;
	
	// Change un-curated samples
	boolean uncurated = true;

	public String getHtmlDescription_2() {
		
		StringBuilder sb = new StringBuilder("<HTML>"); //$NON-NLS-1$
		sb.append(Vocab.HTML_NO_SAMPLES_MET_THE_APPLY_TO_CRITERIA());
		sb.append("<TABLE>"); //$NON-NLS-1$
		
		sb.append("<TR><TH>") //$NON-NLS-1$
		    .append(Vocab.COLHDG_UNCURATED())
		    .append("</TH><TH>") //$NON-NLS-1$
		    .append(Vocab.COLHDG_CURATED()).
		    append("</TH></TR>"); //$NON-NLS-1$
		
		sb.append("<TR>"); //$NON-NLS-1$

		// Uncurated
		sb.append("<TD><UL><LI>"); //$NON-NLS-1$
		
		if (uncurated) {
			sb.append(Vocab.HTML_CHANGE_SAMPLES());
		}
		else {
			sb.append(Vocab.HTML_DO_NOT_CHANGE_SAMPLES());
		}

		sb.append("</LI></UL></TD>"); //$NON-NLS-1$

		//
		// Curated
		
		sb.append("<TD>"); //$NON-NLS-1$
		
		if (curated) {
		    String incl = included 
		            ? Vocab.HTML_CHANGE_ACCEPTED_SAMPLES()
                    : Vocab.HTML_DO_NOT_CHANGE_ACCEPTED_SAMPLES();
		            
		    String excl = excluded
		            ? Vocab.HTML_CHANGE_SUPPRESSED_SAMPLES()
		            : Vocab.HTML_DO_NOT_CHANGE_SUPPRESSED_SAMPLES();

		    sb.append("<UL><LI>") //$NON-NLS-1$
			    .append(incl)
			    .append("</LI><LI>") //$NON-NLS-1$
			    .append(excl)
			    .append("</LI></UL>"); //$NON-NLS-1$
		}
		else {
			sb.append("<UL><LI>") //$NON-NLS-1$
			    .append(Vocab.HTML_DO_NOT_CHANGE_SAMPLES())
			    .append("</LI></UL>"); //$NON-NLS-1$
		}
		sb.append("</TD>"); //$NON-NLS-1$
		
		sb.append("</TR></TABLE>"); //$NON-NLS-1$
		
		
		return sb.toString();
	}

	public String getHtmlDescription() {
		
		List<String> constraints = new ArrayList<>();
		
		if (uncurated) {
			constraints.add(Vocab.HTML_APPLY_TO_UNCURATED_SAMPLES());
		}
		else {
			constraints.add(Vocab.HTML_DO_NOT_APPLY_TO_UNCURATED_SAMPLES());
		}

		// - - -
		
		if (curated) {
			constraints.add(Vocab.HTML_APPLY_TO_CURATED_SAMPLES());
		}
		else {
			constraints.add(Vocab.HTML_DO_NOT_APPLY_TO_CURATED_SAMPLES());
		}
		
		if (included) {
			constraints.add(Vocab.HTML_IF_CURATED_APPLY_TO_ACCEPTED_SAMPLES());
		}
		else {
			constraints.add(Vocab.HTML_IF_CURATED_DO_NOT_APPLY_TO_ACCEPTED_SAMPLES());
		}

		if (excluded) {
			constraints.add(Vocab.HTML_IF_CURATED_APPLY_TO_SUPPRESSED_SAMPLES());
		}
		else {
			constraints.add(Vocab.HTML_IF_CURATED_DO_NOT_APPLY_TO_SUPPRESSED_SAMPLES());
		}
		
		StringBuilder sb = new StringBuilder("<HTML>"); //$NON-NLS-1$
		sb.append(Vocab.HTML_NO_SAMPLES_MET_THE_APPLY_TO_CRITERIA());
		sb.append("<UL>"); //$NON-NLS-1$
		for (String s : constraints) {
			sb.append("<li>") //$NON-NLS-1$
			    .append(s)
			    .append("</li>"); //$NON-NLS-1$
		}
		sb.append("</UL>"); //$NON-NLS-1$
		
		return sb.toString();
	}

	public void refreshFromCheckboxes() {
		excluded = excludedType.isSelected();
		included = includedType.isSelected();
		curated = curatedType.isSelected();	
		uncurated = uncuratedType.isSelected();
	}

	public void applyToCheckboxes() {
		excludedType.setSelected(excluded);
		includedType.setSelected(included);
		curatedType.setSelected(curated);		
		uncuratedType.setSelected(uncurated);
	}
}
