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
package com.diversityarrays.kdxplore.trialdesign;

import java.util.Optional;
import java.util.Set;

import com.diversityarrays.kdxplore.design.DesignEntry;
import com.diversityarrays.kdxplore.design.EntryFactor;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.util.XYPos;

public class PositionedDesignEntry<E extends DesignEntry> {

    private final int replicateNumber;
    private final XYPos where;
    private final Integer block; // TODO - check if this should only be available for TrialEntry
    private final E entry;

    public PositionedDesignEntry(int replicateNumber, XYPos where, Integer block, E entry) {
        this.replicateNumber = replicateNumber;
        this.where = where;
        this.block = block;
        this.entry = entry;
    }

    @Override
    public String toString() { // mainly for debugging
        StringBuilder sb = new StringBuilder("PDE[");
        sb.append(entry.getSequence()).append("=").append(entry.getEntryName());
        EntryType entryType = entry.getEntryType();
        if (entryType != null) {
            sb.append('/').append(entryType).append(".").append(entryType.variant);
        }
        if (where != null) {
            sb.append(" @").append(where);
        }
        if (block != null) {
            sb.append(" BLOCK=").append(block);
        }
        sb.append(']');
        return sb.toString();
    }

    public PositionedDesignEntry<E> makeCopyReplacing(E trialEntry, int x, int y) {
        return new PositionedDesignEntry<>(
                replicateNumber, new XYPos(x,y), block, entry);
    }

    public int getReplicateNumber() {
        return replicateNumber;
    }

    public Optional<XYPos> getWhere() {
        return Optional.ofNullable(where);
    }

    public Integer getBlock() {
        return block;
    }

    public E getEntry() {
        return entry;
    }

    public String getCsvValue() {
        return entry.getCsvValue();
    }

    public EntryType getEntryType() {
        return entry.getEntryType();
    }

    public int getSequence() {
        return entry.getSequence();
    }

    public String getEntryName() {
        return entry.getEntryName();
    }

    public int getEntryId() {
    	return entry.getEntryId();
    }
//    public Integer getEntryNumber() {
//        return entry.getEntryNumber();
//    }

    public String getLocation() {
        return entry.getLocation();
    }

    public String getNurseryOrExperiment() {
        return entry.getNurseryOrExperiment();
    }

    public Set<EntryFactor> getEntryFactors() {
        return entry.getEntryFactors();
    }

    public Optional<String> getFactorValue(EntryFactor f) {
        return entry.getFactorValue(f);
    }

}
