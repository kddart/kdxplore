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

import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class SampleName {

	private final Integer traitInstance;
	
	private final Integer traitId;
	
	private final Integer plotId;
		
	public SampleName(Integer plotId, Integer traitInstanceId, Integer traitId) {
		this.plotId = plotId;
		this.traitInstance = traitInstanceId;
		this.traitId = traitId;
	}

    public Integer getPlotId() {
        return plotId;
    }

	public KdxSample getFirstSampleForCurationSampleType(CurationCellValue ccv, CurationSampleType curationSampleType) {
	    KdxSample result = null;
	    if (ccv != null) {
	        // Note: some of the added entries may be null
	        List<KdxSample> list = new ArrayList<>();
	        switch (curationSampleType) {
	        case ANY:
	            list.add(ccv.getEditedSample());
	            list.add(ccv.getDatabaseSample());
	            list.add(ccv.getLatestRawSample());
	            break;
	        case EDITED:
                list.add(ccv.getEditedSample());
	            break;
	        case NOT_EDITED:
                list.add(ccv.getDatabaseSample());
                list.add(ccv.getLatestRawSample());
	            break;
	        }
	        for (KdxSample s : list) {
                if (isForThisSampleName(s)) {
                    result = s;
                    break;
                }
	        }
	    }
	    return result;
	}

    private boolean isForThisSampleName(KdxSample sample) {
        if (sample != null) {
            if (sample.getPlotId() == plotId && sample.getTraitId() == traitId && sample.getTraitInstanceNumber() == traitInstance) {
                return true;
            }
        }
        return false;
    }
	
//	public boolean checkSample(CurationCellValue ccv, CurationSampleType curationSampleType) {
//		if (ccv != null) {
//		    KdxSample rawSample;
//			switch (curationSampleType) {
//			case ANY:
//				if (ccv.getEditedSample() != null) {
//				    if (isForThisSampleName(ccv.getEditedSample())) {
//				        return true;
//				    }
//				} 
//				if (ccv.getDatabaseSample() != null) {
//					if (isForThisSampleName(ccv.getDatabaseSample())) {
//					    return true;
//					}
//				}
//				rawSample = ccv.getLatestRawSample();
//				if (rawSample != null) {
//				    return isForThisSampleName(rawSample);
//				}
//				break;
//			case EDITED:
//				if (ccv.getEditedSample() != null) {
//					return isForThisSampleName(ccv.getEditedSample());
//				}
//				break;
//			case NOT_EDITED:
//				if (ccv.getDatabaseSample() != null) {
//				    if (isForThisSampleName(ccv.getDatabaseSample())) {
//				        return true;
//				    }
//				}
//				rawSample = ccv.getLatestRawSample();
//                if (rawSample != null) {
//                    return isForThisSampleName(rawSample);
//                }
//				break;
//			}		
//		}
//		return false;
//	}

//	public KdxSample getValue(CurationCellValue ccv, CurationSampleType any) {
//		if (ccv != null) {
//			switch (any) {
//			case ANY:
//				if (ccv.getEditedSample() != null) {
//					return ccv.getEditedSample();
//				}
//				if (ccv.getDatabaseSample() != null) {
//					return ccv.getDatabaseSample();
//				}
//				return ccv.getLatestRawSample();
//			case EDITED:
//				if (ccv.getEditedSample() != null) {
//					return ccv.getEditedSample();
//				}
//				break;
//			case NOT_EDITED:
//				if (ccv.getDatabaseSample() != null) {
//					return ccv.getDatabaseSample();
//				}
//                return ccv.getLatestRawSample();
//			}		
//		}
//		return null;
//	}

}
