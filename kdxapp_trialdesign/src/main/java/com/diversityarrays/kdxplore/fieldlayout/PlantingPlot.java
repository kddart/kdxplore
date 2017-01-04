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
package com.diversityarrays.kdxplore.fieldlayout;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.trialdesign.TrialEntry;
import com.diversityarrays.util.VisitOrder2D;

public class PlantingPlot {

    public final Point cartesianCoordPoint;
    public final Point screenCoordPoint;

    // These will be filled in
    public TrialEntry trialEntry;

    public PlantingPlot(Point screenPoint, Point cartesianPoint) {
        screenCoordPoint = screenPoint;
        cartesianCoordPoint = cartesianPoint;
    }

    static public List<PlantingPlot> getPlantingPlots(
            PlantingBlock<ReplicateCellContent> pb,
            Predicate<EntryType> trueIfCanOverwrite,
            VisitOrder2D visitOrder
            )
    {
        Map<Point, ReplicateCellContent> contentByPoint = pb.getContentByPoint();

        int nCols = pb.getColumnCount();
        int nRows = pb.getRowCount();

        List<PlantingPlot> result = new ArrayList<>(nCols * nRows);

        for (int row = 0; row < nRows; ++row) {
            for (int col = 0; col < nCols; ++col) {

                Point screenPt = new Point(col, row);
                // NOTE! This is "graphical" coordinates


                ReplicateCellContent rcc = contentByPoint.get(screenPt);

                if (rcc == null || trueIfCanOverwrite.test(rcc.entryType)) {
                    Point cartesianPoint = new Point(screenPt.x, nRows - screenPt.y);
//                  // Now we have (0,0) in the bottom left
                    result.add(new PlantingPlot(screenPt, cartesianPoint));
                }
            }
        }

        return result;
    }

}
