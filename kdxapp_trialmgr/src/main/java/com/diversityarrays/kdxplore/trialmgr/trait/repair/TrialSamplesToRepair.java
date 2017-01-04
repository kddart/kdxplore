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
package com.diversityarrays.kdxplore.trialmgr.trait.repair;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

class TrialSamplesToRepair {
    public final int trialId;
    public final String trialName;

    public int plotLevelSampleCount;
    public int subplotLevelSampleCount;
    
    public TrialSamplesToRepair(int id, String n) {
        trialId = id;
        trialName = n;
    }
    
    void addSampleCount(KdxSample sample) {
        if (PlotOrSpecimen.isSpecimenNumberForPlot(sample.getSpecimenNumber())) {
            ++plotLevelSampleCount;
        }
        else {
            ++subplotLevelSampleCount;
        }
    }

    public boolean isEmpty() {
        return plotLevelSampleCount<=0 && subplotLevelSampleCount <= 0;
    }
}
