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
package com.diversityarrays.kdxplore.fieldlayout;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.trialdesign.TrialEntry;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ListByOne;
import com.diversityarrays.util.VisitOrder2D;

class TrialEntryAssigner {

    static public enum ErrorType {
        SPATIALLY_INCOMPLETE("There are Replicates without sufficient %s checks"),
        OTHERS_COMPLETED("There are Replicates that thate have non-%s checks already assigned"),
        NOT_YET_SUPPORTED("Not yet supported")
        ;

        public final String messageTemplate;
        ErrorType(String mt) {
            messageTemplate = mt;
        }
        public String formatMessage(String spatialName) {
            if (this == ErrorType.NOT_YET_SUPPORTED) {
                return messageTemplate;
            }
            return String.format(messageTemplate, spatialName);
        }
    }

    static public class Error {
        public final ErrorType errorType;
        public final List<ReplicateDetailsModel> models;

        Error(ErrorType t, List<ReplicateDetailsModel> list) {
            errorType = t;
            models = list;
        }
    }

    private static final boolean DONT_USE_NESTING = true;

    private final Collection<ReplicateDetailsModel> allModels;
    private final ReplicateDetailsModel target;
    private final EntryType spatialEntryType;
    private final List<TrialEntry> trialEntries;
    private final VisitOrder2D visitOrder2d;
    private final Consumer<ReplicateDetailsModel> onModelAssignmentDone;

    TrialEntryAssigner(Collection<ReplicateDetailsModel> allModels,
            ReplicateDetailsModel target,
            EntryType spatial,
            List<TrialEntry> trialEntries,
            VisitOrder2D visitOrder2D,
            Consumer<ReplicateDetailsModel> onModelAssignmentDone)
    {
        this.allModels = allModels;
        this.target = target;
        this.spatialEntryType = spatial;
        this.trialEntries = trialEntries;
        this.visitOrder2d = visitOrder2D;
        this.onModelAssignmentDone = onModelAssignmentDone;
    }


    /*
     * 1. Make sure all spatials have been provided for target models
     * 2. If any models have other non-empty entry types
     */
    /**
     *
     * @param erc
     * @param confirmClearOtherEntryTypes confirm with user about clearing the completed non-Spatials
     * @return
     */
    public Either<Error, Collection<ReplicateDetailsModel>> doAssignment(
            EntryRandomChoice erc,
            Predicate<Collection<ReplicateDetailsModel>> confirmClearOtherEntryTypes)
    {
        Collection<ReplicateDetailsModel> modelsToCheck = getModelsToCheck(erc);

        // 1. All that need it must have all Spatials assigned
        List<ReplicateDetailsModel> spatialIncomplete = modelsToCheck.stream()
            .filter(m -> ! m.isSpatiallyComplete())
            .collect(Collectors.toList());
        if (! spatialIncomplete.isEmpty()) {
            return Either.left(new Error(ErrorType.SPATIALLY_INCOMPLETE, spatialIncomplete));
        }

        // 2. If any other entry types assigned, we want to over-write so make sure that is ok.
        List<ReplicateDetailsModel> completed = modelsToCheck.stream()
            .filter(m -> ! m.getNonSpatialsWithDefinedValues().isEmpty())
            .collect(Collectors.toList());

        if (! completed.isEmpty()) {
            if (! confirmClearOtherEntryTypes.test(completed)) {
                return Either.left(new Error(ErrorType.OTHERS_COMPLETED, completed));
            }

            // User said its ok to clear them
            completed.stream()
                .forEach(m -> m.clearNonSpatials());
        }

        Optional<TrialEntry> opt_nesting = trialEntries.stream()
            .filter(te -> ! Check.isEmpty(te.getNesting()))
            .findFirst();

        if (opt_nesting.isPresent()) {
            return processWithNesting(erc);
        }
        else {
            return processWithoutNesting(erc);
        }
    }


    private Either<Error, Collection<ReplicateDetailsModel>> processWithNesting(
            EntryRandomChoice erc)
    {
        if (DONT_USE_NESTING) {
            return processWithoutNesting(erc);
        }
        switch (erc) {
        case ALL:
            break;
        case ALL_WIHOUT_RANDOM:
            break;
        case EXCLUDE_REP_1:
            break;
        case ONLY_THIS:
            break;
        default:
            break;
        }
        return Either.left(new Error(ErrorType.NOT_YET_SUPPORTED, Collections.emptyList()));
    }

    private Either<Error, Collection<ReplicateDetailsModel>> processWithoutNesting(
            EntryRandomChoice erc)
    {

        boolean randomising = erc != EntryRandomChoice.ALL_WIHOUT_RANDOM;

        Collection<ReplicateDetailsModel> modelsToProcess;

        switch (erc) {
        case ALL:
        case ALL_WIHOUT_RANDOM:
            modelsToProcess = allModels;
            break;
        case EXCLUDE_REP_1:
            modelsToProcess = allModels.stream()
                .filter((m) -> 1 != m.getReplicateNumber())
                .collect(Collectors.toList());
            break;
        case ONLY_THIS:
            modelsToProcess = Arrays.asList(target);
            break;
        default:
            throw new RuntimeException("Unsupported choice: " + erc);
        }

        if (randomising) {
            // Randomising but no nesting
            for (ReplicateDetailsModel rdm : modelsToProcess) {
                List<PlantingPlot> plots = processOneModelRandomisingNoNesting(rdm);
                storePlotsInModel(rdm, plots);
                onModelAssignmentDone.accept(rdm);
            }
        }
        else {
            // Not randomising and no nesting
            for (ReplicateDetailsModel rdm : modelsToProcess) {
                List<PlantingPlot> plots = processOneModelNoRandomisingNoNesting(rdm);
                storePlotsInModel(rdm, plots);
                onModelAssignmentDone.accept(rdm);
            }
        }

        return Either.right(modelsToProcess);
    }

    private void storePlotsInModel(ReplicateDetailsModel rdm, List<PlantingPlot> plots) {
        Map<Point, ReplicateCellContent> map = new HashMap<>();
        for (PlantingPlot plot : plots) {
            if (plot.trialEntry != null) {
                ReplicateCellContent rcc = new ReplicateCellContent(plot.trialEntry);
                map.put(plot.screenCoordPoint, rcc);
            }
        }
        PlantingBlock<ReplicateCellContent> pb = rdm.getPlantingBlock();
        pb.setContentUsing(map);
    }

    private List<PlantingPlot> processOneModelRandomisingNoNesting(ReplicateDetailsModel rdm) {
        ListByOne<EntryType, TrialEntry> entriesByType = new ListByOne<>();
        trialEntries.stream()
            .forEach(te -> entriesByType.addKeyValue(te.getEntryType(), te));

        List<PlantingPlot> plots = getPlantingPlotsInVisitOrder(rdm);

        List<TrialEntry> randomisedEntryList = createRandomisedTrialEntryList();

        Iterator<PlantingPlot> plotIterator = plots.iterator();
        Iterator<TrialEntry> entryIterator = randomisedEntryList.iterator();

        while (plotIterator.hasNext() && entryIterator.hasNext()) {
            TrialEntry entry = entryIterator.next();
            PlantingPlot plot = plotIterator.next();

            plot.trialEntry = entry;
        }
        return plots;
    }

    private List<TrialEntry> createRandomisedTrialEntryList() {
        List<TrialEntry> sourceEntryList = new ArrayList<>(trialEntries);
        List<TrialEntry> randomisedEntryList = new ArrayList<>(trialEntries.size());

        Random random = new Random(System.currentTimeMillis());
        while (! sourceEntryList.isEmpty()) {
            int index = random.nextInt(sourceEntryList.size());
            randomisedEntryList.add(sourceEntryList.remove(index));
        }
        return randomisedEntryList;
    }

    private List<PlantingPlot> processOneModelNoRandomisingNoNesting(ReplicateDetailsModel rdm) {

        List<PlantingPlot> plots = getPlantingPlotsInVisitOrder(rdm);

        Iterator<PlantingPlot> plotIterator = plots.iterator();
        Iterator<TrialEntry> entryIterator = trialEntries.iterator();


        while (plotIterator.hasNext() && entryIterator.hasNext()) {
            TrialEntry entry = entryIterator.next();
            PlantingPlot plot = plotIterator.next();

            plot.trialEntry = entry;
        }

        return plots;
    }

    private Collection<ReplicateDetailsModel> getModelsToCheck(EntryRandomChoice erc) {
        switch (erc) {
        case ALL:
        case ALL_WIHOUT_RANDOM:
        case EXCLUDE_REP_1:
            return allModels;
        case ONLY_THIS:
            return Arrays.asList(target);
        default:
            throw new RuntimeException("Unsupported choice: " + erc);
        }
    }



    private Predicate<EntryType> getEntryTypePredicate() {
        Predicate<EntryType> predicate;
        if (spatialEntryType == null) {
            predicate = (e) -> true;
        }
        else {
            predicate = (e) -> ! spatialEntryType.equals(e);
        }
        return predicate;
    }

    private List<PlantingPlot> getPlantingPlotsInVisitOrder(ReplicateDetailsModel rdm) {
        PlantingBlock<ReplicateCellContent> pb = rdm.getPlantingBlock();
        Predicate<EntryType> trueIfCanOverwrite = getEntryTypePredicate();
        return PlantingPlot.getPlantingPlots(pb, trueIfCanOverwrite, visitOrder2d);
    }

}
