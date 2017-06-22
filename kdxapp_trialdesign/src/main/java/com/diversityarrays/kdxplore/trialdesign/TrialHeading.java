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
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("nls")

public enum TrialHeading {
    DONT_USE("Don't Import"),
    LOCATION("Location", "Loc"),
    // Keep each import set separate
    EXPERIMENT("Experiment", "Exp", "Expname"),
    // These are unique within experiment
    ENTRY_ID("Entry ID", "Entry#"),
    // These are unique within experiment
    ENTRY_NAME("Entry Name", "Name", "GID"),
    NESTING("Nest"), // starts with Nest
    ENTRY_TYPE("Entry Type", "Group", "Role"),
    FACTOR("Plot Attribute");

    /*
    add another heading: Replicate Name

    Off-by-one in XLS import column

    Experiment/Site/Replicates

    # Replicates/Location

    Each Replicate different
    */
    public final String display;
    public final Set<String> matchWords;

    TrialHeading(String d, String ... matchWords) {
        this.display = d;
        Set<String> set = new HashSet<>();
        set.add(display.toLowerCase().replaceAll(" *", ""));
        if (matchWords != null) {
            for (String mw : matchWords) {
                if (mw != null && ! mw.isEmpty()) {
                    set.add(mw.toLowerCase().replaceAll(" *", ""));
                }
            }
        }
        this.matchWords = Collections.unmodifiableSet(set);
    }

    public boolean isRequired() {
        // If ENTRY_ID isn't supplied we will generate them
        return this==ENTRY_NAME; // || this==ENTRY_ID;
    }

    public boolean isOnlyValidOnce() {
        return this!=DONT_USE && this!=FACTOR;
    }

    @Override
    public String toString() {
        return display;
    }
}
