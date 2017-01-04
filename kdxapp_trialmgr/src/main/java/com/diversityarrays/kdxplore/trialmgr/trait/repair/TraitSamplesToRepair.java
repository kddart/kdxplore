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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.util.ItemConsumerHelper;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Either;

class TraitSamplesToRepair {
    
    static public enum RepairToApply {
        NO_REPAIR_REQUIRED,
        HAS_BOTH_LEVELS,
        CONVERT_TO_PLOT,
        CONVERT_TO_SUBPLOT
    }
    
    public final Trait trait;
    
    public final Map<Integer,TrialSamplesToRepair> sampleCountsByTrialId = new HashMap<>();
    
    TraitSamplesToRepair(Trait t) {
        trait = t;
    }

    public boolean isEmpty() {
        if (! sampleCountsByTrialId.isEmpty()) {
            for (TrialSamplesToRepair ts : sampleCountsByTrialId.values()) {
                if (! ts.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    void addSampleCount(int trialId, String trialName, KdxSample sample) {
        TrialSamplesToRepair trialSamples = sampleCountsByTrialId.get(trialId);
        if (trialSamples == null) {
            trialSamples = new TrialSamplesToRepair(trialId, trialName);
            sampleCountsByTrialId.put(trialId, trialSamples);
        }
        trialSamples.addSampleCount(sample);
    }

    public Either<Exception,String> applyRepair(KdxploreDatabase kdxdb) {
        int totalPlotSamples = sampleCountsByTrialId.values().stream()
                .collect(Collectors.summingInt(ts -> ts.plotLevelSampleCount));
        int totalSubplotSamples = sampleCountsByTrialId.values().stream()
                .collect(Collectors.summingInt(ts -> ts.subplotLevelSampleCount));
        if (totalPlotSamples > 0) {
            if (totalSubplotSamples > 0) {
                StringBuilder sb = new StringBuilder("Unable to repair: Samples measurements exist at both Levels");
                for (TrialSamplesToRepair ts : sampleCountsByTrialId.values()) {
                    sb.append('\n').append(ts.trialName).append(": P=" ).append(ts.plotLevelSampleCount)
                        .append(" SB=").append(ts.subplotLevelSampleCount);
                }
                return Either.left(new Exception(sb.toString()));
            }
            return convertToLevel(kdxdb, TraitLevel.PLOT);
        }
        // no plot samples
        if (totalSubplotSamples > 0) {
            return convertToLevel(kdxdb, TraitLevel.SPECIMEN);
        }
        // Also No sub-plot samples !
        return Either.right("No Repair Required");
    }
    
    private Either<Exception, String> convertToLevel(KdxploreDatabase kdxdb, TraitLevel level) {
        trait.setTraitLevel(level);
        ItemConsumerHelper ich = kdxdb.getKDXploreKSmartDatabase().getItemConsumerHelper();
        try {
            ich.updateItemInDatabase(Trait.class, trait);
            return Either.right("Trait " + trait.getTraitName() + " converted to " + level);
        }
        catch (IOException e) {
            return Either.left(e);
        }
    }


}
