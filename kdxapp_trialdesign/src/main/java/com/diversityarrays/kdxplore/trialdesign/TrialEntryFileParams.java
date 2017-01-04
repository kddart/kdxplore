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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.diversityarrays.kdxplore.design.RowDataProvider;

public class TrialEntryFileParams {
    public final RowDataProvider rowDataProvider;
    public final Map<String,TrialHeading> roleByHeading;
    
    public TrialEntryFileParams(RowDataProvider rdp, Map<String, TrialHeading> map) {
        rowDataProvider = rdp;
        roleByHeading = Collections.unmodifiableMap(map);
    }

    public String getFirstHeading(TrialHeading ht) {
        Optional<String> opt = roleByHeading.entrySet()
            .stream()
            .filter(e -> e.getValue().equals(ht))
            .map(Map.Entry::getKey)
            .findFirst();
        return opt.orElse(null);
    }
    
    public List<String> getHeadings(TrialHeading ht) {
        return roleByHeading.entrySet()
            .stream()
            .filter(e -> e.getValue().equals(ht))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

}
