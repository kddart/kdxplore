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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JTable;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.PlantingBlockFactory;

import net.pearcan.ui.GuiUtil;

public class LocationReplicatesTable extends JTable {

    public LocationReplicatesTable(
            PlantingBlockFactory<ReplicateCellContent> blockFactory,
            Function<EntryType, Color> entryTypeColorSupplier,
            Consumer<SiteLocation> onLocationChanged)
    {
        super(new LocationReplicatesTableModel(
                blockFactory,
                entryTypeColorSupplier,
                onLocationChanged));

        super.setAutoCreateRowSorter(false);
    }

    @Override
    public void setAutoCreateRowSorter(boolean b) {
        // NO-OP
    }

    public LocationReplicatesTableModel getLocationReplicatesTableModel() {
        return (LocationReplicatesTableModel) getModel();
    }

//    public Map<Location, Set<ReplicateDetailsModel>> getReplicatesByLocation() {
//        return getLocationReplicatesTableModel().getReplicatesByLocation();
//    }

    public Optional<SiteLocation> getLocationWithName(String name) {
        return getLocationReplicatesTableModel().getLocationsWithName(name);
    }

    public List<SiteLocation> getSelectedLocations() {
        LocationReplicatesTableModel m = getLocationReplicatesTableModel();
        return GuiUtil.getSelectedModelRows(this).stream()
            .map(i -> m.getLocationAt(i))
            .collect(Collectors.toList());
    }
}
