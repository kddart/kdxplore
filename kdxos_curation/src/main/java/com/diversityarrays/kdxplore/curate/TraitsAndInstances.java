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
package com.diversityarrays.kdxplore.curate;

import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;

public interface TraitsAndInstances  {

    JComponent getComponent();
    
    Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance();
//    List<TraitInstance> getTraitInstancesWithData();
    List<TraitInstance> getCheckedTraitInstances();
    // all==flase means only the "chosen" ones
    List<TraitInstance> getTraitInstances(boolean allElseOnlyChecked);
    
    void changeTraitInstanceChoice(boolean choiceAdded, TraitInstance[] choice);
    void addTraitInstanceStatsItemListener(ItemListener l);
}
