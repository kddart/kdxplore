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
package com.diversityarrays.kdxplore.fielddesign.alg;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings("nls")
public class CombinedAlg extends AbstractIterableAlg {

    private long seed;

    private LloydsAlg lloydsAlg;

    private int farthestFirstInset = 1;

    public CombinedAlg(int wid, int hyt, long seed) {
        super("FF.then.Lloyds", wid, hyt, seed);
        this.seed = seed;
    }

    public void setFarthestFirstInset(int v) {
        this.farthestFirstInset = v;
    }

    @Override
    public Set<NamedPoint> getClusterPoints() {
        if (lloydsAlg == null) {
            throw new IllegalStateException("start not called");
        }
        return lloydsAlg.getClusterPoints();
    }

    @Override
    public StepState start(int nPoints) {
        FarthestFirst ff = new FarthestFirst(width, height, seed, farthestFirstInset);

        ff.start(nPoints);
        while (ff.step().isPresent()) ;

        this.lloydsAlg = new LloydsAlg(width, height, seed);
        this.lloydsAlg.setExcluding(ff.getExcluding());

        return lloydsAlg.startWith(nPoints, ff.getClusterPoints());
    }

    @Override
    public Optional<StepState> step() {
        if (lloydsAlg == null) {
            throw new IllegalStateException("start not called");
        }
        return lloydsAlg.step();
    }

    @Override
    public boolean canUndo() {
        return lloydsAlg.canUndo();
    }

    @Override
    public StepState undo() {
        return lloydsAlg.undo();
    }

    @Override
    public boolean canRedo() {
        return lloydsAlg.canRedo();
    }

    @Override
    public StepState redo() {
        return lloydsAlg.redo();
    }

}
