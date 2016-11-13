/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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

public interface MessageLogger {
	
	static public enum MessageLevel {
		Verbose,
		Debug,
		Info,
		Warning,
		Error,
		Fatal,
		Silent
	}

	MessageLevel getMessageLevel();
	void setMessageLevel(MessageLevel level);
	
	// Info
	void i(String tag, String message);
	void i(String tag, String message, Throwable t);
	
	// Debug
	void d(String tag, String message);
	void d(String tag, String message, Throwable t);
	
	// Error
	void e(String tag, String message);
	void e(String tag, String message, Throwable t);
	
	void v(String tag, String message);
	void v(String tag, String message, Throwable t);

	void w(String tag, String message);
	void w(String tag, Throwable t);
	void w(String tag, String message, Throwable t);
	
	void wtf(String tag, String message);
	void wtf(String tag, Throwable t);
	void wtf(String tag, String message, Throwable t);

}
