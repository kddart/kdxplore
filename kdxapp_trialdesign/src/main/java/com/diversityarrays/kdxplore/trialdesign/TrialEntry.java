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

import java.util.Map;

import com.diversityarrays.kdxplore.design.DesignEntry;
import com.diversityarrays.kdxplore.design.EntryFactor;
import com.diversityarrays.kdxplore.design.EntryType;

public class TrialEntry extends DesignEntry {

    private String nesting;
    private EntryType entryType;
    // FIXME replication not yet supported in TrialEntryAssigner
    private int replication;

    public TrialEntry(String loc, String exp, int entryId, String name, EntryType type, String nest, Map<EntryFactor,String> map) {
        super(loc, exp, entryId, name, map);
        this.entryType = type;
        this.nesting = nest;
    }

    public String getExperimentName() {
        return nurseryOrExperiment;
    }

    public String getNesting() {
        return nesting;
    }
    public void setNesting(String nesting) {
        this.nesting = nesting;
    }
    public EntryType getEntryType() {
        return entryType;
    }
    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public int getReplication() {
        return replication;
    }
    public void setReplication(int v) {
        this.replication = v;
    }
}
