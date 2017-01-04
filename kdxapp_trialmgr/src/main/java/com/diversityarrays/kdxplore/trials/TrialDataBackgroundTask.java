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
package com.diversityarrays.kdxplore.trials;

import java.awt.Component;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.BackgroundTask;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.dalclient.DalUtil;
import com.diversityarrays.kdxplore.data.dal.TrialPlus;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MessageLogger;

abstract class TrialDataBackgroundTask<T> extends BackgroundTask<T, Void> {

	final TrialPlus trial;
	private final Component messageComponent;
	private final Closure<String> errorConsumer;
	private MessageLogger messageLogger;
	
	protected final String tag;

	public TrialDataBackgroundTask(String message, 
			TrialPlus trial,
			Component messageComponent,
			Closure<String> errorConsumer,
			String tag,
			MessageLogger messageLogger)
	{
		super(message, true);
		this.tag = tag;
		
		this.messageComponent = messageComponent;
		this.trial = trial;
		this.errorConsumer = errorConsumer;
		this.messageLogger = messageLogger;
	}

	protected void maybeShowCancelMessage(String msgTitle, String whats,
			int count) {
		if (count > 0) {
			String errmsg = "Load of " + whats + " was cancelled";
			messageLogger.i(tag, errmsg);
			errorConsumer.execute(errmsg);

			String msg = "You cancelled after retrieving "
					+ count
					+ " "
					+ whats
					+ "\nThese will be discarded otherwise other problems will arise.";
			GuiUtil.infoMessage(messageComponent, msg, msgTitle);
		}
	}

	@Override
	final public void onException(Throwable cause) {
		String errmsg = DalUtil.extractPossibleDalErrorMessage(cause);
		if (errmsg == null) {
			errmsg = cause.getMessage();
			if (Check.isEmpty(errmsg)) {
				errmsg = cause.getClass().getName() + " during "
						+ this.getMessage();
			}

			GuiUtil.errorMessage(messageComponent, cause, 
					"Error doing: " + this.getMessage());
		}

		messageLogger.e(tag, errmsg);
		errorConsumer.execute(errmsg);
	}

}
