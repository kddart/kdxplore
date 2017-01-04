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

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.PlantingBlockFactory;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.SetByOne;

@SuppressWarnings("nls")
public class LocationReplicatesTableModel extends AbstractTableModel {

    static public final String PROPERTY_LOCATION_COUNT = "locationCount";

    private final List<SiteLocation> locations = new ArrayList<>();

    private final SetByOne<SiteLocation, ReplicateDetailsModel> replicateModelsForLocation = new SetByOne<>();
    private boolean editable = true;
    private int replicateCount = 1;

    private final Consumer<SiteLocation> onLocationChanged;

    private final PlantingBlockFactory<ReplicateCellContent> blockFactory;

    private final Function<EntryType, Color> entryTypeColorSupplier;

    static private final int N_NON_REPLICATE_COLUMNS = 2;
    public LocationReplicatesTableModel(
            PlantingBlockFactory<ReplicateCellContent> blockFactory,
            Function<EntryType, Color> entryTypeColorSupplier,
            Consumer<SiteLocation> onLocationChanged)
    {
        this.blockFactory = blockFactory;
        this.entryTypeColorSupplier = entryTypeColorSupplier;
        this.onLocationChanged = onLocationChanged!=null ? onLocationChanged : (t) -> {};
    }

    public Optional<SiteLocation> addLocation(String siteName) {

        Optional<SiteLocation> findFirst = locations.stream()
            .filter(loc -> loc.name.equalsIgnoreCase(siteName))
            .findFirst();

        if (findFirst.isPresent()) {
            return Optional.empty();
        }

        int oldCount = locations.size();
        int row = locations.size();

        int maxWidth = SiteLocation.INITIAL_MIN_WIDTH_IN_CELLS;
        int maxHeight = SiteLocation.INITIAL_MIN_HEIGHT_IN_CELLS;
        int totalPlots = 0;

        List<ReplicateDetailsModel> models = new ArrayList<>();
        for (int r = 1; r <= replicateCount; ++r) {
            ReplicateDetailsModel rdm = createForReplicate(r);
            models.add(rdm);
            Dimension size = rdm.getPlantingBlockSize();
            maxWidth = Math.max(maxWidth, size.width);
            maxHeight = Math.max(maxHeight, size.height);
            totalPlots += (size.width * size.height);
        }

        while ((maxWidth * maxHeight) < totalPlots) {
            ++maxWidth;
            ++maxHeight;
        }

        SiteLocation loc = new SiteLocation(siteName, maxWidth, maxHeight);

        locations.add(loc);
        models.stream()
            .forEach(m -> replicateModelsForLocation.addKeyValue(loc, m));

        fireTableRowsInserted(row, row);
        fireLocationCountChanged(oldCount);

        return Optional.of(loc);
    }

    private ReplicateDetailsModel createForReplicate(int replicate) {
        PlantingBlock<ReplicateCellContent> pb = blockFactory.createPlantingBlockFrom(replicate);
        ReplicateDetailsModel model = new ReplicateDetailsModel(pb, entryTypeColorSupplier);
        return model;
    }

    public List<SiteLocation> removeLocationsAt(List<Integer> rows) {

        List<SiteLocation> result = new ArrayList<>();

        List<SiteLocation> list = rows.stream().map(r -> getLocationAt(r))
            .filter(t -> t != null)
            .collect(Collectors.toList());

        for (SiteLocation loc : list) {
            int index = locations.indexOf(loc);
            if (index >= 0) {
                SiteLocation rmv = locations.remove(index);
                result.add(rmv);
                fireTableRowsDeleted(index, index);
            }
        }

        return result;
    }

    public void setLocations(List<SiteLocation> locs) {
        locations.clear();
        replicateModelsForLocation.clear();
        if (! Check.isEmpty(locs)) {
            locations.addAll(locs);
            for (SiteLocation loc : locations) {
                replicateModelsForLocation.addKey(loc);
            }
            setReplicateCount(replicateCount);
        }
        fireTableDataChanged();
    }

    public Map<SiteLocation, Set<WhatChanged>> setDesignParams(
            DesignParams designParams,
            Map<EntryType, Integer> entryTypeCounts)
    {
        // First do the replicate count because that may add/remove replicateModels
        setReplicateCount(designParams.replicateCount);

        Function<Set<ReplicateDetailsModel>, Set<WhatChanged>> f = new Function<Set<ReplicateDetailsModel>, Set<WhatChanged>>() {
            @Override
            public Set<WhatChanged> apply(Set<ReplicateDetailsModel> set) {
                Set<WhatChanged> result = set.stream().flatMap(m -> m.setDesignParams(designParams).stream())
                    .collect(Collectors.toSet());
                set.stream().forEach(m -> m.setEntryTypeCounts(false, entryTypeCounts));
                return result;
            }
        };

        Map<SiteLocation, Set<WhatChanged>> changes = replicateModelsForLocation.entrySet().stream()
            .map(e -> new Pair<>(e.getKey(), f.apply(e.getValue())))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

//        Map<SiteLocation, Set<WhatChanged>> map = changes.stream()

        return changes;
    }

    private void setReplicateCount(int nReps) {

        replicateCount = Math.max(1, nReps);

        SetByOne<SiteLocation, ReplicateDetailsModel> toRemove = new SetByOne<>();

        for (SiteLocation loc : replicateModelsForLocation.keySet()) {
            Optional<Set<ReplicateDetailsModel>> optional = replicateModelsForLocation.get(loc);
            if (optional.isPresent()) {
                optional.get().stream()
                    .filter(m -> m.getReplicateNumber() > replicateCount)
                    .forEach(m -> toRemove.addKeyValue(loc, m));
            }
        }

        for (SiteLocation loc : toRemove.keySet()) {
            Optional<Set<ReplicateDetailsModel>> optional = toRemove.get(loc);
            if (optional.isPresent()) {
                for (ReplicateDetailsModel m : optional.get()) {
                    replicateModelsForLocation.removeKeyValue(loc, m);
                }
            }
        }

        replicateModelsForLocation.clean();

        Set<Integer> allReplicateNumbers = IntStream.range(1, replicateCount+1)
                    .mapToObj(Integer::valueOf)
                    .collect(Collectors.toSet());

        BiConsumer<SiteLocation, Set<ReplicateDetailsModel>> addReplicatesIfNotYetPresent = new BiConsumer<SiteLocation, Set<ReplicateDetailsModel>>() {
            @Override
            public void accept(SiteLocation loc, Set<ReplicateDetailsModel> set) {
                Set<Integer> existing = set.stream()
                        .map(m -> m.getReplicateNumber())
                        .collect(Collectors.toSet());
                Set<Integer> toBeAdded = new HashSet<>(allReplicateNumbers);
                toBeAdded.removeAll(existing);
                for (Integer r : toBeAdded) {
                    replicateModelsForLocation.addKeyValue(loc, createForReplicate(r));
                }
            }
        };

        replicateModelsForLocation.entrySet().stream()
            .forEach(e -> addReplicatesIfNotYetPresent.accept(e.getKey(), e.getValue()));

        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return locations.size();
    }

    @Override
    public int getColumnCount() {
        // "Name", "Width x Height", "Rep-1", "Rep-2", ...
        return N_NON_REPLICATE_COLUMNS + replicateCount;
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
        case 0: return "Location";
        case 1: return "Dimension";
        }
        return "Rep#" + columnIndexToReplicate(col);
    }

    private int columnIndexToReplicate(int columnIndex) {
        return 1 + (columnIndex - N_NON_REPLICATE_COLUMNS);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return col < N_NON_REPLICATE_COLUMNS
                ? String.class
                : Boolean.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex >= N_NON_REPLICATE_COLUMNS;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        SiteLocation location;
        switch (columnIndex) {
        case 0: return;
        case 1:
            // update Width
            if (aValue instanceof Integer) {
                int width = (Integer) aValue;
                if (width <= 0) {
                    return;
                }
                location = locations.get(rowIndex);
                if (width != location.widthInCells) {
                    Dimension d = new Dimension(width, location.heightInCells);
                    SiteLocation newLoc = location.copy(location, location.name, d,
                            location.isSizeEditable(),
                            location.widthInMetres, location.heightInMetres);
                    locations.set(rowIndex, newLoc);
                    onLocationChanged.accept(newLoc);
                }
            }
            return;
        case 2:
            // update Height
            if (aValue instanceof Integer) {
                int height = (Integer) aValue;
                if (height <= 0) {
                    return;
                }
                location = locations.get(rowIndex);
                if (height != location.heightInCells) {
                    Dimension d = new Dimension(location.widthInCells, height);
                    SiteLocation newLoc = location.copy(location, location.name, d,
                            location.isSizeEditable(),
                            location.widthInMetres, location.heightInMetres);
                    locations.set(rowIndex, newLoc);
                    onLocationChanged.accept(newLoc);
                }
            }
            return;
        }

        if (! (aValue instanceof Boolean)) {
            return;
        }

        int replicate = columnIndexToReplicate(columnIndex);
        // make sure we have an Object
        location = locations.get(rowIndex);

        boolean changed = false;
        if ((Boolean) aValue) {
            // Adding
            Optional<ReplicateDetailsModel> findFirst = replicateModelsForLocation
                    .findFirst(location, (m) -> m.getReplicateNumber() == replicate);

            if (! findFirst.isPresent()) {
                replicateModelsForLocation.addKeyValue(location, createForReplicate(replicate));
                changed = true;
            }
        }
        else {
            // Remove
            List<ReplicateDetailsModel> removeValues =
                    replicateModelsForLocation
                        .removeValues(location, (m) -> m.getReplicateNumber() == replicate);
            changed = ! removeValues.isEmpty();
        }

        if (changed) {
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SiteLocation loc = locations.get(rowIndex);
        switch (columnIndex) {
        case 0: return loc.name;
        case 1: return loc.widthInCells + " x " + loc.heightInCells;
        }

        // Remaining columns are the Checkboxes
        int replicateNumber = columnIndexToReplicate(columnIndex);

        Optional<ReplicateDetailsModel> findFirst = replicateModelsForLocation
                .findFirst(loc, m ->  m.getReplicateNumber() == replicateNumber);
        return findFirst.isPresent();
    }

    public Optional<Set<ReplicateDetailsModel>> getReplicateModelsForLocation(SiteLocation loc) {
        return replicateModelsForLocation.get(loc);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        editable = b;
    }

    public SiteLocation getLocationAt(int row) {
        if (row < 0 || row >= locations.size()) {
            return null;
        }
        return locations.get(row);
    }


    protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    protected void fireLocationCountChanged(int oldCount) {
        propertyChangeSupport.firePropertyChange(PROPERTY_LOCATION_COUNT, oldCount, locations.size());
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, l);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, l);
    }

    public Optional<SiteLocation> getLocationsWithName(String name) {
        return locations.stream().filter(l -> l.name.equalsIgnoreCase(name))
                    .findFirst();
    }
}
