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
package com.diversityarrays.kdxplore.design;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.fieldlayout.DesignParams;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.kdxplore.fieldlayout.ReplicateCellContent;
import com.diversityarrays.kdxplore.trialdesign.TrialDesignPreferences;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;

import net.pearcan.ui.table.BspAbstractTableModel;

@SuppressWarnings("nls")
public class ReplicateDetailsModel extends BspAbstractTableModel {

    static public interface DetailsChangeListener extends EventListener {
        void chosenEntryTypeChanged(ReplicateDetailsModel source,
                EntryType entryType, boolean isSpatial);

//        void designParametersChanged(ReplicateDetailsModel source,
//                Set<WhatChanged> whatChanged);
    }

    static public final int DEFAULT_VISIBLE_ROW_COUNT = 4;

    private static final boolean DONT_ALLOW_SPATIAL = Boolean.getBoolean("DONT_ALLOW_SPATIAL");

    private EntryType chosenEntryType;

    private final List<EntryType> entryTypes = new ArrayList<>();
    private final Map<EntryType, EntryTypeCounter> counterByEntryType = new HashMap<>();

    private Function<EntryType, Color> colorSupplier;

    private final String spatialChecksName;

    private final PlantingBlock<ReplicateCellContent> plantingBlock;

    private EntryType spatialEntryType;

    private int spatialEntryTypeIndex;

    public ReplicateDetailsModel(
            PlantingBlock<ReplicateCellContent> plantingBlock,
            Function<EntryType, Color> colorSupplier)
    {
        super("Draw?", "Progress", "Type", "Colour");

        this.plantingBlock = plantingBlock;
        this.colorSupplier = colorSupplier;

        this.spatialChecksName = TrialDesignPreferences.getInstance().getSpatialEntryName();
    }

    public Set<WhatChanged> setDesignParams(DesignParams designParams) {
        Set<WhatChanged> whatChanged = plantingBlock.updateDesignParameters(designParams);
        if (! whatChanged.isEmpty()) {
            updateEntryTypeCounts();
            fireTableDataChanged();
        }
        return whatChanged;
    }

    public int getReplicateNumber() {
        return plantingBlock.getReplicateNumber();
    }

    public String getReplicateName() {
        return plantingBlock.getName();
    }


    public void addDetailsChangeListener(DetailsChangeListener l) {
        listenerList.add(DetailsChangeListener.class, l);
    }

    public void removeDetailsChangeListener(DetailsChangeListener l) {
        listenerList.remove(DetailsChangeListener.class, l);
    }

    protected void fireChosenEntryTypeChanged() {
        EntryType entryType = chosenEntryType;
        boolean isSpatial = isSpatialChecksChosen();
        for (DetailsChangeListener l : listenerList.getListeners(DetailsChangeListener.class)) {
            l.chosenEntryTypeChanged(this, entryType, isSpatial);
        }
    }

    public void setEntryTypeCounts(boolean forceDoChosen, Map<EntryType, Integer> map) {
        boolean doChosen = forceDoChosen;

        entryTypes.clear();
        counterByEntryType.clear();
        chosenEntryType = null;

        if (! Check.isEmpty(map)) {
            counterByEntryType.putAll(map.entrySet().stream()
                .map(e -> new EntryTypeCounter(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(EntryTypeCounter::getEntryType, Function.identity())));

            entryTypes.addAll(map.keySet());
            Collections.sort(entryTypes);
        }

        // Force assign all the entryTypes NOW
        for (EntryType t : entryTypes) {
            colorSupplier.apply(t);
        }

        // Auto-select the first one
        if (! doChosen) {
            if (chosenEntryType == null) {
                doChosen = true;
            }
            else {
                doChosen = ! entryTypes.contains(chosenEntryType);
            }
        }

        if (doChosen) {
            // Assume not found
            spatialEntryType = null;
            spatialEntryTypeIndex = -1;

            if (! Check.isEmpty(spatialChecksName)) {
                Optional<EntryType> opt = entryTypes.stream()
                        .filter(e -> e.getName().equalsIgnoreCase(spatialChecksName))
                        .findFirst();
                if (opt.isPresent()) {
                    spatialEntryType = opt.get();
                    spatialEntryTypeIndex = entryTypes.indexOf(spatialEntryType);
                }
            }
            // Find the non-spatialEntryType

            Optional<EntryType> opt = entryTypes.stream()
                    .filter(e -> ! e.getName().equalsIgnoreCase(spatialChecksName))
                    .findFirst();
            if (opt.isPresent()) {
                chosenEntryType = opt.get();
            }
        }
        fireTableDataChanged();

        fireChosenEntryTypeChanged();
    }

    @Override
    public int getRowCount() {
        return entryTypes.size();
    }

    private boolean isEditAllowed(int rowIndex) {
        if (DONT_ALLOW_SPATIAL) {
            return rowIndex != spatialEntryTypeIndex;
        }
        return true;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (! isEditAllowed(rowIndex)) {
            return false;
        }
        return 0 == columnIndex;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex==0 && aValue instanceof Boolean) {
            if (! isEditAllowed(rowIndex)) {
                return;
            }
            EntryType oldChosen = chosenEntryType;
            if ((Boolean) aValue) {
                chosenEntryType = entryTypes.get(rowIndex);
            }
            else {
                chosenEntryType = null;
            }

            fireTableRowsUpdated(rowIndex, rowIndex);
            if (oldChosen != null) {
                int row = entryTypes.indexOf(oldChosen);
                if (row >= 0) {
                    fireTableRowsUpdated(row, row);
                }
            }

            fireChosenEntryTypeChanged();
        }
    }

    public boolean isSpatialChecksChosen() {
        return chosenEntryType !=null
                &&
                spatialChecksName.equalsIgnoreCase(chosenEntryType.getName());
    }

    @Override
    public Class<?> getColumnClass(int col) {
        switch (col) {
        case 0: return Boolean.class;
        case 1: return EntryTypeCounter.class;
        case 2: return String.class;
        case 3: return Color.class;
        }
        return Object.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        EntryType entryType = entryTypes.get(rowIndex);
        switch (columnIndex) {
        case 0:
            if (! isEditAllowed(rowIndex)) {
                return null;
            }
            return entryType == chosenEntryType;

        case 1:
            return counterByEntryType.get(entryType);

        case 2:
            return entryType.getName();

        case 3:
            return colorSupplier.apply(entryType);
        }
        return null;
    }

    public void updateEntryTypeCounts() {

        // Total Count by EntryType
        Map<EntryType, Integer> entryTypeCounts = plantingBlock.getAllContents().stream()
                .collect(Collectors.toMap(
                        ReplicateCellContent::getEntryType,
                        (t) -> new Integer(1),
                        (a,b) -> a+b));

        counterByEntryType.values().forEach(c -> c.setDefinedCount(0));

        BiConsumer<EntryType, Integer> accumulate = new BiConsumer<EntryType, Integer>() {
            @Override
            public void accept(EntryType t, Integer nDefined) {
                EntryTypeCounter counter = counterByEntryType.get(t);
                if (counter == null) {
                    counterByEntryType.put(t, new EntryTypeCounter(t, 0, nDefined));
                }
                else {
                    counter.setDefinedCount(nDefined);
                }
            }
        };
        entryTypeCounts.entrySet().stream()
            .forEach(e -> accumulate.accept(e.getKey(), e.getValue()));

        fireTableDataChanged();

        // TODO check if really need to do this because it actually hasn't!
//        fireChosenEntryTypeChanged();
    }

    public Either<String, Optional<EntryType>> getSpatialEntryType() {
        if (Check.isEmpty(spatialChecksName)) {
            return Either.left("No name specified for Spatial Checks");
        }
        Optional<EntryType> opt = entryTypes.stream()
                .filter(e -> e.getName().equalsIgnoreCase(spatialChecksName))
                .findFirst();
        if (opt.isPresent()) {
            return Either.right(opt);
        }
        return Either.left("Unable to find EntryType with the name '" + spatialChecksName + "'");
    }

    public String getSpatialChecksName() {
        return spatialChecksName;
    }

    /**
     * Return the number of Spatial Checks needing to be defined.
     * @return
     */
    public Optional<EntryTypeCounter> getSpatialCheckCounter() {
        Optional<EntryType> opt = entryTypes.stream()
                .filter(e -> e.getName().equalsIgnoreCase(spatialChecksName))
                .findFirst();
        if (! opt.isPresent()) {
            return Optional.empty();
        }
        EntryTypeCounter etc = counterByEntryType.get(opt.get());
        return Optional.ofNullable(etc);
    }

    public int getSpatialCheckCountRequired() {
        Optional<EntryTypeCounter> opt = getSpatialCheckCounter();
        return opt.isPresent() ? opt.get().getRequiredCount() : 0;
    }

    public int getSpatialCheckCountDefined() {
        Optional<EntryTypeCounter> opt = getSpatialCheckCounter();
        return opt.isPresent() ? opt.get().getDefinedCount() : 0;
    }

    public boolean isSpatiallyComplete() {
        Optional<EntryTypeCounter> opt = getSpatialCheckCounter();
        if (! opt.isPresent()) {
            return true; // No spatials means its ok
        }
        EntryTypeCounter etc = opt.get();
        return etc.isComplete();
    }


    public PlantingBlock<ReplicateCellContent> getPlantingBlock() {
        return plantingBlock;
    }

    public void clearPlantingBlockContent() {
        plantingBlock.clearContent(null /*clear all*/);
        updateEntryTypeCounts();
        fireTableDataChanged();
    }

    public void setContentAtPoints(ReplicateCellContent newContent, Point[] points) {
        plantingBlock.setContentAtPoints(newContent, Arrays.asList(points));
        updateEntryTypeCounts();
        fireTableDataChanged();
    }

    public Dimension getPlantingBlockSize() {
        return plantingBlock.getSize();
    }

    public List<EntryType> getNonSpatialsWithDefinedValues() {
        return counterByEntryType.values().stream()
            .filter(e -> EntryType.Variant.SPATIAL != e.getEntryType().variant)
            .filter(e -> e.getDefinedCount() > 0)
            .map(e -> e.getEntryType())
            .collect(Collectors.toList());
    }

    public List<EntryType> getOnlyCompletedNonSpatials() {
        return counterByEntryType.values().stream()
            .filter(e -> ! spatialChecksName.equalsIgnoreCase(e.getEntryType().getName()))
            .filter(e -> e.isComplete())
            .map(e -> e.getEntryType())
            .collect(Collectors.toList());
    }

    public void clearNonSpatials() {
        getOnlyCompletedNonSpatials().stream()
            .forEach(type -> counterByEntryType.get(type).setDefinedCount(0));
    }

}
