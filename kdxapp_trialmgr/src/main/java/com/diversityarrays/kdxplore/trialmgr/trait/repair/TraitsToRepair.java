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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

public class TraitsToRepair {
    public final List<Trait> traits;
    public final Map<Integer,Trait> traitById;
    private final KdxploreDatabase kdxdb;
    
    private final Map<Trait,TraitSamplesToRepair> traitSamplesByTrait = new HashMap<>();

    public TraitsToRepair(KdxploreDatabase kdxdb, List<Trait> toBeFixed) {
        this.kdxdb = kdxdb;
        this.traits = toBeFixed;
        Collections.sort(traits);
        this.traitById = traits.stream()
                .collect(Collectors.toMap(Trait::getTraitId, Function.identity()));
    }
    
    public boolean isEmpty() {
        if (! traitSamplesByTrait.isEmpty()) {
            for (TraitSamplesToRepair ts : traitSamplesByTrait.values()) {
                if (! ts.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public Map<Trait, String> repairTraits(Closure<Pair<Trait, Either<Exception, String>>> publishPartial) {
        Map<Trait,String> result = new LinkedHashMap<>();
        for (Trait trait : traitSamplesByTrait.keySet()) {
            TraitSamplesToRepair traitSamples = traitSamplesByTrait.get(trait);
            Either<Exception,String> either = traitSamples.applyRepair(kdxdb);
            if (either.isRight()) {
                result.put(trait, either.right());
            }
            publishPartial.execute(new Pair<>(trait, either));
        }
        return result;
    }
    
    public String getDescription() {
        StringBuilder sb = new StringBuilder("=== Traits that should be repaired ===");

        List<Pair<Integer,String>> idNamePairs = new ArrayList<>();
        for (TraitSamplesToRepair traitSamples : traitSamplesByTrait.values()) {
            sb.append("\n").append(traitSamples.trait.getTraitName());
            sb.append("\n- - - - - - - - - -");
            sb.append("\n  Trial : sample count");
            for (TrialSamplesToRepair trialSamples: traitSamples.sampleCountsByTrialId.values()) {
                sb.append(trialSamples.trialName).append(" : " );
                if (trialSamples.plotLevelSampleCount > 0) {
                    sb.append(" Plot samples: " ).append(trialSamples.plotLevelSampleCount);
                }
                if (trialSamples.subplotLevelSampleCount > 0) {
                    sb.append(" Subplot samples: ").append(trialSamples.subplotLevelSampleCount);
                }
            }
        }
        
        return sb.toString();
    }

    public void getProblemSampleCountByTrialId() throws IOException {
        
        traitSamplesByTrait.clear();
        
        Map<Integer,String> trialNameById = new HashMap<>();
        
        Predicate<KdxSample> predicate = new Predicate<KdxSample>() {
            @Override
            public boolean test(KdxSample sample) {
                int traitId = sample.getTraitId();
                
                int trialId = sample.getTrialId();

                if (traitById.keySet().contains(traitId)) {
                    int snum = sample.getSpecimenNumber();
                    
                    Trait trait = traitById.get(traitId);

                    TraitSamplesToRepair traitSamplesByTrialId = traitSamplesByTrait.get(trait);
                    if (traitSamplesByTrialId == null) {
                        traitSamplesByTrialId = new TraitSamplesToRepair(trait);
                        traitSamplesByTrait.put(trait, traitSamplesByTrialId);
                    }

                    String trialName = trialNameById.get(trialId);
                    if (trialName == null) {
                        try {
                            Trial trial = kdxdb.getKDXploreKSmartDatabase().getTrial(trialId);
                            trialName = trial.getTrialName();
                        } catch (IOException ignore) {
                            trialName = "Trial#" + trialId;
                        }
                        trialNameById.put(trialId, trialName);
                    }


                    traitSamplesByTrialId.addSampleCount(trialId, trialName, sample);
                }
                return true;
            }
        };

        // No traitId so all Traits are scanned
        kdxdb.visitScoredKdxSamples(null, predicate);
    }

}
