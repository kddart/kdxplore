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

public class EntryTypeCounter {
        private final EntryType entryType;
        private int nRequired;
        private int nDefined;

        public EntryTypeCounter(EntryType e, int nr) {
            this(e, nr, 0);
        }

        public EntryTypeCounter(EntryType e, int nr, int nd) {
            entryType = e;
            nRequired = nr;
            nDefined = nd;
        }

        public EntryType getEntryType() {
            return entryType;
        }

        public boolean isComplete() {
            return nDefined >= nRequired;
        }

        public int getDefinedCount() {
            return nDefined;
        }

        public void setDefinedCount(int v) {
            nDefined = v;
        }

        public int getRequiredCount() {
            return nRequired;
        }

        @Override
        public String toString() {
            return String.format("%d / %d", nDefined, nRequired);
//            return String.format("Counters[%s.%s req=%d def=%d",
//                    entryType.getName(), entryType.variant.name(),
//                    nRequired, nDefined);
        }
        public double getRatio() {
            return nDefined * 1.0 / nRequired;
        }

    }
