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

import java.awt.Component;

import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.prefs.PreferenceCollection;
import com.diversityarrays.kdxplore.services.KdxApp;
import com.diversityarrays.kdxplore.services.KdxPluginInfo;
import com.diversityarrays.kdxplore.trialdesign.TrialDesignPanel;
import com.diversityarrays.kdxplore.trialdesign.TrialDesignPreferences;

public class TrialDesignApp implements KdxApp {

    private final TrialDesignPanel trialDesignPanel;

    public TrialDesignApp(KdxPluginInfo pluginInfo) {

        PreferenceCollection pc = TrialDesignPreferences.getInstance()
                .getPreferenceCollection(this, getAppName());

        KdxplorePreferences.getInstance().addPreferenceCollection(pc);

        trialDesignPanel = new TrialDesignPanel(pluginInfo);
    }

    public TrialDesignPanel getTrialDesignPanel() {
        return trialDesignPanel;
    }

    @Override
    public DevelopmentState getDevelopmentState() {
        return DevelopmentState.PRODUCTION;
    }

    @Override
    public int getDisplayOrder() {
        return OfflineData.AppOrder.TRIAL_DESIGN.displayOrder;
    }

    @Override
    public int getInitialisationOrder() {
        return OfflineData.AppOrder.TRIAL_DESIGN.initOrder;
    }

    @Override
    public String getAppName() {
        return "Trial Design";
    }

    @Override
    public Component getUIComponent() {
        return trialDesignPanel;
    }
}
