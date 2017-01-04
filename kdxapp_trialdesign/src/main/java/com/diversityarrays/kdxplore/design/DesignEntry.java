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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class DesignEntry {
    protected final String location;
    protected final String nurseryOrExperiment;
    protected final int entryId;
    protected final String entryName;
    protected final Map<EntryFactor,String> factorValues = new TreeMap<>();

    protected DesignEntry(String loc, String exp, int entryId, String name, Map<EntryFactor, String> map) {
    	this.entryId = entryId;
        this.location = loc==null ? "" : loc;
        this.nurseryOrExperiment = exp==null ? "" : exp;
        this.entryName = name;
        if (map != null) {
            factorValues.putAll(map);
        }
    }

    @Override
    public String toString() {
        return entryName;
    }

    public int getEntryId() {
        return entryId;
    }
    public String getEntryName() {
        return entryName;
    }
    public String getLocation() {
        return location;
    }
    public String getNurseryOrExperiment() {
        return nurseryOrExperiment;
    }

    public Set<EntryFactor> getEntryFactors() {
        return factorValues.keySet();
    }

    public void setFactorValue(EntryFactor f, String v) {
        factorValues.put(f, v);
    }
    public Optional<String> getFactorValue(EntryFactor f) {
        return Optional.ofNullable(factorValues.get(f));
    }
}
