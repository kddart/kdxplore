/**
 * 
 */
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

import java.awt.Graphics;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.table.DefaultTableCellRenderer;

import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.ttools.shared.CommentMarker;
import com.diversityarrays.kdxplore.ttools.shared.SampleIconType;
import com.diversityarrays.util.Pair;

public class AbstractCurationTableCellRenderer extends DefaultTableCellRenderer {

    private static final boolean DEBUG = Boolean.getBoolean("AbstractCurationTableCellRenderer.DEBUG"); //$NON-NLS-1$
	
	protected SampleIconType sampleIconType;

	protected CommentMarker commentMarker;
	
	protected final DateFormat whenDateFormat = TraitValue.getSampleMeasureDateTimeFormat();


	public AbstractCurationTableCellRenderer() {
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (DEBUG) {
	        System.out.println("sampleIconType: " + sampleIconType); //$NON-NLS-1$
		}
		if (sampleIconType != null) {
		    sampleIconType.draw(this, g);
		}

		if (commentMarker != null) {
		    commentMarker.draw(this, g);
		}
	}

	// Invoked when CCV edit state is MORE_RECENT
    protected Pair<CommentMarker,String> moreRecentUnlessSameValue(CurationCellValue ccv) {
        String ttt = null;
        CommentMarker cm = CommentMarker.MORE_RECENT;
        KdxSample latestRawSample = ccv.getLatestRawSample();
        Date latestWhen = latestRawSample.getMeasureDateTime();
        if (latestWhen == null) {
            ttt = "?no measure date?"; //$NON-NLS-1$
        }
        else {
            ttt = "<HTML>" + CommentMarker.MORE_RECENT.toolTipText +  //$NON-NLS-1$
                    "<BR>" + whenDateFormat.format(latestWhen); //$NON-NLS-1$
            KdxSample editedSample = ccv.getEditedSample();
            if (editedSample != null && editedSample.hasBeenScored()) {
                String editedValue = editedSample.getTraitValue();
                String rawValue = latestRawSample.getTraitValue();
                if (editedValue.equals(rawValue)) {
                    // They are the same - don't care about being more recent.
                    int rawSampleCount = ccv.getRawSampleCount();
                    if (rawSampleCount <= 0) {
                        cm = null;
                        ttt = null;
                    }
                    else {
                        cm = CommentMarker.MULTIPLE_VALUES;
                    }
                }
            }
        }
        
        return new Pair<>(cm, ttt);
    }
}
