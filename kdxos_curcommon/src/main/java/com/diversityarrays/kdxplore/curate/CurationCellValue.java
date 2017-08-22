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

import java.util.Collections;
import java.util.List;

import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class CurationCellValue {
    
    static public enum DeviceSampleStatus {
        NO_DEVICE_SAMPLES,
        ONE_DEVICE_SAMPLE,
        MULTIPLE_SAMPLES_ONE_VALUE,
        MULTIPLE_SAMPLES_MANY_VALUES
    }
	
	static public final Pair<EditState,KdxSample> getEditStateAndSample(
			KdxSample edited, 
			List<KdxSample> rawSamples,
			KdxSample database,
			KdxSample calc)
	{
	    if (rawSamples == null) {
	        rawSamples = Collections.emptyList();
	    }
		EditState editState;
		KdxSample sample;

		if (calc != null) {
		    editState = EditState.CALCULATED;
            sample = calc;
		}
		else {
	        KdxSample latestRaw = rawSamples.isEmpty() ? null : rawSamples.get(0);
	        if (latestRaw != null && ! latestRaw.hasBeenScored()) {
	            latestRaw = null;
	        }

	        if (edited != null) {
                sample = edited;
	            if (latestRaw == null) {
	                // No raw or it hasn't been scored
	                editState = EditState.CURATED;
	            }
	            else {
	                // We have a "most recent" scored raw sample
	                if (edited.hasBeenScored() 
	                        && 
	                    edited.getMeasureDateTime().before(latestRaw.getMeasureDateTime())) 
	                {
	                    // edited sample is "out-of-date"
	                    editState = EditState.MORE_RECENT;
	                }
	                else {
	                    // edited sample was done after the raw sample
	                    editState = EditState.CURATED;
	                }               
	            }
	        }
	        else if (! rawSamples.isEmpty()) {
	            // Definitely no 'edited' sample - but there are raw samples
	            sample = rawSamples.get(0);
	            editState = EditState.RAW_SAMPLE;
	        }
	        else if (database != null) {
	            editState = EditState.FROM_DATABASE;
	            sample = database;
	        }
	        else {
	            editState = EditState.NO_VALUES;
	            sample = null;
	        }  
		}

		
		return new Pair<>(editState, sample);
	}
	
	
	private static final String NO_VALUE = "-noval-"; //$NON-NLS-1$
	private EditState editState;
	private KdxSample valueForEditState;
	
	private final CurationCellId ccid;
	
	private final KdxSample calcSample;
	private KdxSample editedSample; // is the one selected
	private KdxSample databaseSample;
	private final List<KdxSample> rawSamples;
    private DeviceSampleStatus deviceSampleStatus = null;

	public CurationCellValue(CurationCellId ccid, KdxSample ed, KdxSample db, List<KdxSample> raw, KdxSample calc) {
		this.ccid = ccid;
		
		calcSample = calc;

		editedSample = ed;
		databaseSample = nullIfNotScored(db);
		rawSamples = raw != null ? raw : Collections.emptyList();

		updateEditState();
	}
	
	static private KdxSample nullIfNotScored(KdxSample input) {
	    KdxSample result = input;
	    if (result != null && ! result.hasBeenScored()) {
	        result = null;
	    }
	    return result;
	}

	@Override
	public String toString() {
		// THis is mainly for debugging
	    if (calcSample != null) {
	        return calcSample.getTraitValue();
	    }
		if (editedSample != null) {
			return editedSample.getTraitValue();
		}
		if (rawSamples!=null && ! rawSamples.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			String sep = "raw=[";
			for (KdxSample sm : rawSamples) {
				sb.append(sep).append(sm.getTraitValue());
				sep = "|";
			}
			sb.append("]");
			return sb.toString(); 
		}
		if (databaseSample != null) {
			return "@ " + databaseSample.getTraitValue(); //$NON-NLS-1$
		}
		return NO_VALUE;
	}
	
	public String getDisplayHtml() {
		return getDisplayHtml(true);
	}
	
	public String getDisplayHtml(boolean withHtmlPrefix) {
		
		String valu = (valueForEditState==null) ? NO_VALUE: valueForEditState.getTraitValue();
		
		StringBuilder sb = new StringBuilder();
		if (withHtmlPrefix) {
			sb.append("<HTML>"); //$NON-NLS-1$
		}
		
		editState.font.wrap(sb, StringUtil.htmlEscape(valu));
		
		return sb.toString();
	}
	
	public int getPlotId() {
		return ccid.plotId;
	}

	public int getTraitId() {
		return ccid.traitId;
	}

	public KdxSample getEditStateSampleMeasurement() {
		return valueForEditState;
	}
	
	private boolean updateEditState() {
		EditState old = editState;
		
		Pair<EditState, KdxSample> pair = getEditStateAndSample(editedSample, rawSamples, databaseSample, calcSample);

		editState = pair.first;
		valueForEditState = pair.second;

		return ! editState.equals(old);
	}
	
	public EditState getEditState() {
		return editState;
	}

	public KdxSample getEditedSample() {
		return editedSample;
	}

	public KdxSample getLatestRawSample() {
		return Check.isEmpty(rawSamples) ? null : rawSamples.get(0);
	}

	/**
	 * Change the editedSample
	 * @param editedSample
	 * @return true if EditState changed
	 */
	public boolean setEditedSample(KdxSample editedSample) {
		this.editedSample = editedSample;
		return updateEditState();
	}

	public KdxSample getDatabaseSample() {
		return databaseSample;
	}

	/**
	 * Change the database sample.
	 * @param databaseSample
	 * @return true if EditState changed
	 */
	public boolean setDatabaseSample(KdxSample databaseSample) {
		this.databaseSample = databaseSample;
		return updateEditState();
	}

	public List<KdxSample> getRawSamples() {
		return Collections.unmodifiableList(rawSamples);
	}

	public CurationCellId getCurationCellId() {
		return ccid;
	}

	public DeviceSampleStatus getDeviceSampleStatus() {
	    if (deviceSampleStatus == null) {
	        switch (rawSamples.size()) {
	        case 0:
	            deviceSampleStatus = DeviceSampleStatus.NO_DEVICE_SAMPLES;
	            break;
	        case 1:
	            deviceSampleStatus = DeviceSampleStatus.ONE_DEVICE_SAMPLE;
	            break;
	        default:
                deviceSampleStatus = DeviceSampleStatus.MULTIPLE_SAMPLES_ONE_VALUE;
                String lastValue = null;
                for (KdxSample s : rawSamples) {
                    if (s.hasBeenScored()) {
                        String sv = s.getTraitValue();
                        if (lastValue == null) {
                            lastValue = sv;
                        }
                        else if (! lastValue.equals(sv)) {
                            deviceSampleStatus = DeviceSampleStatus.MULTIPLE_SAMPLES_MANY_VALUES;
                            break;
                        }
                    }
                }
	            break;
	        }
	    }
	    return deviceSampleStatus;
	}

    public int getRawSampleCount() {
        return rawSamples==null ? 0 : rawSamples.size();
    }

}
