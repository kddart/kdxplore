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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingMessageLogger implements MessageLogger {
	
	static private final Map<MessageLevel, Level> ML_TO_L = new HashMap<MessageLevel, Level>();
	static {
		ML_TO_L.put(MessageLevel.Verbose, Level.FINEST);
		ML_TO_L.put(MessageLevel.Debug, Level.FINER);
		ML_TO_L.put(MessageLevel.Info, Level.INFO);
		ML_TO_L.put(MessageLevel.Warning, Level.WARNING);
		ML_TO_L.put(MessageLevel.Error, Level.SEVERE);
		ML_TO_L.put(MessageLevel.Fatal, Level.SEVERE);
		ML_TO_L.put(MessageLevel.Silent, Level.OFF);
	}
	
	private final Logger logger;
	private MessageLevel level = MessageLevel.Debug;

	public LoggingMessageLogger(Logger logger) {
		this.logger = logger;
	}
	
	private String prepareMessage(String tag, String message, Throwable t) {
		StringBuilder sb = new StringBuilder(tag);
		sb.append(": ").append(message);
		if (t!=null) {
			sb.append('\n').append(collectStacktrace(t));
		}
		return sb.toString();
	}

	private String collectStacktrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.close();
		
		return sw.toString();
	}


	@Override
	public MessageLevel getMessageLevel() {
		return level;
	}

	@Override
	public void setMessageLevel(MessageLevel level) {
		this.level = level;
		logger.setLevel(ML_TO_L.get(level));
	}

	@Override
	public void i(String tag, String message) {
		logger.info(prepareMessage(tag, message, null));
	}

	@Override
	public void i(String tag, String message, Throwable t) {
		logger.info(prepareMessage(tag, message, t));
	}


	@Override
	public void d(String tag, String message) {
		logger.fine(prepareMessage(tag, message, null));
	}

	@Override
	public void d(String tag, String message, Throwable t) {
		logger.fine(prepareMessage(tag, message, t));
	}

	@Override
	public void e(String tag, String message) {
		logger.severe(prepareMessage(tag, message, null));
	}

	@Override
	public void e(String tag, String message, Throwable t) {
		logger.severe(prepareMessage(tag, message, t));
	}

	@Override
	public void v(String tag, String message) {
		logger.finer(prepareMessage(tag, message, null));
	}

	@Override
	public void v(String tag, String message, Throwable t) {
		logger.finer(prepareMessage(tag, message, t));
	}

	@Override
	public void w(String tag, String message) {
		logger.warning(prepareMessage(tag, message, null));
	}

	@Override
	public void w(String tag, Throwable t) {
		logger.warning(prepareMessage(tag, "", t));
	}

	@Override
	public void w(String tag, String message, Throwable t) {
		logger.warning(prepareMessage(tag, message, t));
	}

	@Override
	public void wtf(String tag, String message) {
		logger.severe(prepareMessage("WTF-"+tag, message, null));
	}

	@Override
	public void wtf(String tag, Throwable t) {
		logger.severe(prepareMessage("WTF-"+tag, "", t));
	}

	@Override
	public void wtf(String tag, String message, Throwable t) {
		logger.severe(prepareMessage("WTF-"+tag, message, t));
	}

}
