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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;

public interface FieldViewFunctions<E> {
    void repaint();
    void setCursor(Cursor c);
    void blockPositionChanged(PlantingBlock<E> block);
    boolean isAnyBlockContaining(Point viewPoint, PlantingBlock<E> exceptFor);
    List<PlantingBlock<E>> findBlocksContaining(Point viewPoint, PlantingBlock<E> exceptFor);
    Optional<PlantingBlock<E>> findFirstPlantingBlock(Predicate<PlantingBlock<E>> predicate);

    Point viewToModel(Point viewPoint);
    Point modelToView(Point modelPoint);

    boolean isAnyOtherBlockIntersecting(PlantingBlock<E> block);
    void blockEntryTypesChanged(PlantingBlock<E> block);
    Graphics getGraphics();
}