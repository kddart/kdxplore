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

import java.awt.ItemSelectable;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableModel;

import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;

import net.pearcan.reflect.Feature;

public interface TIStatsTableModel extends ItemSelectable {

    TableModel getTableModel();

    SimpleStatistics<?> getStatsAt(int modelRow);

    int getColumnCount();

    Class<?> getColumnClass(int column);
    
    int getTraitInstanceNameColumnIndex();

    // Return null if it doesn't support the view column.
    Integer getViewColumnIndex();
    int getValRuleErrorColumnIndex();
    Trait getTrait(int mrow);

//    int getInvalidRuleCount();
//    Set<Integer> getInstanceNumbers();
    List<TraitInstance> getTraitInstancesWithData();
    
    /**
     * For the TraitInstances currently in this model,
     * return the map of TI to stats.
     * @return
     */
    Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance();

    Map<Feature, Integer> getFeatureToColumnIndex();

    void changeTraitInstanceChoice(boolean choiceAdded, TraitInstance[] choices);

    String getValidationExpressionAt(int rowIndex);

    List<TraitInstance> getCheckedTraitInstances();

    List<TraitInstance> getAllTraitInstances(boolean all);

    TraitInstance getTraitInstanceAt(int rowIndex);

    int getTraitInstanceDatatypeColumnIndex();

    String getInstanceHeading();

    int[] getTraitInstanceColumnIndices();

}
