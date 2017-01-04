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
package com.diversityarrays.util;

public enum RunMode {
	END_USER,
	DEMO,
	DEVELOPER;

	public boolean isDeveloper() {
		return this == DEVELOPER;
	}

	public boolean isOperationsMenuAllowed() {
		return this == DEVELOPER;
	}
	
	static private RunMode singleton;
	
	static public RunMode getRunMode() {
		if (singleton==null) {
			singleton = RunMode.END_USER;
		}
		return singleton;
	}
	
	static public void setRunMode(RunMode rm) {
		singleton = rm;
	}

	public boolean isDemo() {
		return this == DEMO;
	}

	public boolean isUser() {
		return this == END_USER;
	}
}
