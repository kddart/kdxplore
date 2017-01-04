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

import java.io.PrintStream;

public class PrintStreamMessageLogger implements MessageLogger {
	
	private final PrintStream ps;

	private MessageLevel level = MessageLevel.Debug; // TODO change this for production

	public PrintStreamMessageLogger(PrintStream ps) {
		this.ps = ps;
	}
	
	@Override
	public MessageLevel getMessageLevel() {
		return level;
	}
	
	@Override
	public void setMessageLevel(MessageLevel level) {
		this.level = level;
	}
	
	protected void emit(MessageLevel lev, String tag, String msg, Throwable t) {
		if (lev.compareTo(this.level) >= 0) {
			ps.println("Info " + tag + ": "
					+ (msg==null ? "" : msg) 
					+ (t==null ? "" : t.getMessage()));
			
			if (t != null) {
				t.printStackTrace(ps);
			}
		}
	}

	@Override
	public void i(String tag, String message) {
		emit(MessageLevel.Info, tag, message, null);
	}

	@Override
	public void i(String tag, String message, Throwable t) {
		emit(MessageLevel.Info, tag, message, t);
	}

	@Override
	public void d(String tag, String message) {
		emit(MessageLevel.Debug, tag, message, null);
	}

	@Override
	public void d(String tag, String message, Throwable t) {
		emit(MessageLevel.Debug, tag, message, t);
	}

	@Override
	public void e(String tag, String message) {
		emit(MessageLevel.Error, tag, message, null);
	}

	@Override
	public void e(String tag, String message, Throwable t) {
		emit(MessageLevel.Error, tag, message, t);
	}

	@Override
	public void v(String tag, String message) {
		emit(MessageLevel.Verbose, tag, message, null);
	}

	@Override
	public void v(String tag, String message, Throwable t) {
		emit(MessageLevel.Verbose, tag, message, t);
	}

	@Override
	public void w(String tag, String message) {
		emit(MessageLevel.Warning, tag, message, null);
	}

	@Override
	public void w(String tag, Throwable t) {
		emit(MessageLevel.Warning, tag, null, t);
	}

	@Override
	public void w(String tag, String message, Throwable t) {
		emit(MessageLevel.Warning, tag, message, t);
	}

	@Override
	public void wtf(String tag, String message) {
		emit(MessageLevel.Fatal, tag, message, null);
	}

	@Override
	public void wtf(String tag, Throwable t) {
		emit(MessageLevel.Fatal, tag, null, t);
	}

	@Override
	public void wtf(String tag, String message, Throwable t) {
		emit(MessageLevel.Fatal, tag, message, t);
	}

}
