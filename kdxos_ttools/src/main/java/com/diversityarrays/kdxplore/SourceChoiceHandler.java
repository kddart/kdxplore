/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

public interface SourceChoiceHandler {
	
	public enum SourceChoice {
		DATABASE(ImageId.ADD_KDDART_24, "Load from Database"),
		KDX(ImageId.ADD_KDSMART_24, "Load from Tablet Data"),
		XLS(ImageId.ADD_XLS_24, "Load from XLS"),
		CSV(ImageId.ADD_CSV_24, "Load from CSV");
		
		public final ImageId imageId;
		public final String text;
		SourceChoice(ImageId iid, String text) {
			this.imageId = iid;
			this.text = text;
		}
	}
	
	void handleSourceChosen(SourceChoice choice);
	
	static public class Util {
	    
	    static public void showSourceChoicePopup(Component invoker, int x, int y,
	            String prompt,
	            SourceChoiceHandler sourceChoiceHandler, 
	            SourceChoice ... choices) {
	        
	        if (choices.length == 1) {
	            sourceChoiceHandler.handleSourceChosen(choices[0]);
	            return;
	        }

	        JPopupMenu popupMenu = new JPopupMenu();
	        popupMenu.add(prompt);
	        
	        for (final SourceChoice choice : choices) {
	            popupMenu.addSeparator();
	            Action action = new AbstractAction(choice.text) {
	                @Override
	                public void actionPerformed(ActionEvent e) {
	                    sourceChoiceHandler.handleSourceChosen(choice);
	                }
	            };
	            KDClientUtils.initAction(choice.imageId, action, choice.text, true);
	            popupMenu.add(new JMenuItem(action));
	        }
	        popupMenu.show(invoker, x, y);
	    }	    
	}
}
