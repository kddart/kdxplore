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

import com.diversityarrays.util.UnicodeChars;

public class EntryType extends UniqueString {

    public enum Variant {
        /**
         * These are Spatial checks.
         */
        SPATIAL(UnicodeChars.BLACK_FLAG),
        /**
         * These mark locations that are deemed to be outside
         * the usable area of the planting area.
         */
        DO_NOT_PLANT(UnicodeChars.CANCEL_CROSS),
        /**
         * These are "normal" values from the user's Entry list.
         */
        ENTRY(UnicodeChars.FLOWER),
        /**
         * These the "checks" from the user's Entry list.
         */
        CHECK(UnicodeChars.CONFIRM_TICK),
        /**
         * Planted 'as' genders for nursery and crossing purposes only
         */
        MALE(UnicodeChars.SIGN_MALE),
        UNKNOWN(UnicodeChars.SIGN_FEMALE_MALE),
        FEMALE(UnicodeChars.SIGN_FEMALE)
        ;

        public final String visual;
        Variant(String s) {
            visual = s;
        }
        public boolean isPlantable() {
            return this==ENTRY || this==CHECK;
        }

        public boolean isValidForNursery() {
            return this==ENTRY || this==DO_NOT_PLANT;
        }

        public boolean isValidForTrial() {
            return true;
        }
    }


    public final Variant variant;

    public EntryType(String name, Variant v) {
        super(name);
        this.variant = v;
    }

    public String getCsvValue(String entryName) {
    	if (variant == Variant.SPATIAL) {
    	    return "-spatial-";
    	}
    	if (variant == Variant.DO_NOT_PLANT) {
    	    return "-outside";
    	}
    	return entryName;
    }

    static public final EntryType DO_NOT_PLANT = new EntryType("-don't plant-", Variant.DO_NOT_PLANT);
}
