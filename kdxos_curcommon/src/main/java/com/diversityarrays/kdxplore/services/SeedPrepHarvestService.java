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

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.diversityarrays.db.DartSchemaHelper;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.prefs.PreferenceCollection;
import com.diversityarrays.kdxservice.KDXDeviceService;
import com.diversityarrays.util.DALClientProvider;
import com.diversityarrays.util.MessageLogger;

import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.util.BackgroundRunner;

public interface SeedPrepHarvestService {

    PreferenceCollection getPreferenceCollection(KdxApp kdxApp);

    static public class HarvestParams {
        public String title;
        public Trial trial;
        public OfflineData offlineData; 
        public DALClientProvider clientProvider; 
        public DartSchemaHelper dartSchemaHelper;
        public MessageLogger messageLogger;
        public KDXDeviceService deviceService;
        public JComponent component;
        public WindowOpener<JFrame> windowOpener;
    }
    
    /**
     * @param id must be the DAL id
     * @return
     */
    static public Object createSeedPrepWindowIdentifier(Integer dalTrialId) {
        return "SEEDPREP-" + dalTrialId; //$NON-NLS-1$
    }
    
    static public class SeedPrepParams {
        public String title;
        public Trial trial;
        public OfflineData offlineData; 
        public DALClientProvider clientProvider; 
        public DartSchemaHelper dartSchemaHelper;
        public KDXDeviceService deviceService;
        public MessageLogger messageLogger;
        public JComponent component;
        public WindowOpener<JFrame> windowOpener;
    }

    void createSeedPrepUserInterface(BackgroundRunner backgroundRunner, SeedPrepParams params);


    /**
     * @param id must be the DAL id
     * @return
     */
    static public Object createHarvestWindowIdentifier(Integer dalTrialId) {
        return "HARVEST-" + dalTrialId; //$NON-NLS-1$
    }

    void createHarvestUserInterface(BackgroundRunner backgroundRunner, HarvestParams params);
}
