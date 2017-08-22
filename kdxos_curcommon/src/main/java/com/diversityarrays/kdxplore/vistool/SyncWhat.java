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
package com.diversityarrays.kdxplore.vistool;

public enum SyncWhat {

	SYNC_ALL("Sync with Any"),
	TRAIT_ONLY("Trait Only"),
	DONT_SYNC("Don't Sync");
	
	public final String displayName;
	SyncWhat(String vis) {
		displayName = vis;
	}
	
	@Override
	public String toString() {
		return displayName;
	}

	public boolean isSync() {
		switch (this) {
		case DONT_SYNC:
			return false;
		case SYNC_ALL:
		case TRAIT_ONLY:
		default:
			break;
		}
		return true;
	}
}
