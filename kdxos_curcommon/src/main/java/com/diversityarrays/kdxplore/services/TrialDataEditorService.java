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
package com.diversityarrays.kdxplore.services;

import java.awt.Component;
import java.util.function.Consumer;

import javax.swing.JFrame;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MessageLogger;

import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.util.BackgroundRunner;

public interface TrialDataEditorService {
    
    static public class InitError {
        
        static public final InitError NO_PLOTIDENT_SUMMARY = new InitError("No PlotIdentifier summary available");
        
        static public final InitError NO_ID_X_OR_Y = new InitError("No PlotId, X or Y ranges available");
        
        static public final InitError NO_IDENT_SPECIFIED = new InitError("No PlotIdentification specified");
        
        static public InitError error(Throwable t) {
            return new InitError(t);
        }
        
        public final String message;
        public final Throwable throwable;
        
        private InitError(String msg) {
            this.message = msg;
            this.throwable = null;
        }
        
        public InitError(Throwable t) {
            this.message = t.getMessage();
            this.throwable = t;
        }
    }
    

    
    static public class CurationParams {

        public String title;
        public Trial trial;
        public OfflineData offlineData; 
        public MessageLogger messageLogger;

        public Component component;
        public WindowOpener<JFrame> windowOpener;
 
    }
    
    static public class TrialDataEditorResult {
        public JFrame frame;
        public CurationData curationData;
        public Closure<Void> onFrameClosed;
    }

    
    void createUserInterface(BackgroundRunner backgroundRunner, CurationParams params, Consumer<Either<InitError,TrialDataEditorResult>> onComplete);
}
