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

import java.awt.Point;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import com.diversityarrays.util.NameMaker;

@SuppressWarnings("nls")
abstract public class AbstractIterableAlg implements IterableAlg {

    static public enum Corner {
        NW(-1, -1),
        NE(-1, +1),
        SW(+1, +1),
        SE(+1, -1);

        public final int xinc;
        public final int yinc;
        Corner(int x, int y) {
            xinc = x;
            yinc = y;
        }
    }

    protected final int width;
    protected final int height;
    protected StepState currentState;
    protected final Stack<StepState> redoStack = new Stack<>();
    protected final Random random;
    protected PrintStream ps = System.out;

    protected final Map<Point, NamedPoint> fixedPoints = new HashMap<>();

    protected final NameMaker nameMaker = new NameMaker('a');

    private final String name;

    public AbstractIterableAlg(String name, int wid, int hyt) {
        this(name, wid, hyt, 0);
    }

    public AbstractIterableAlg(String name, int wid, int hyt, long seed) {
        if (wid <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        if (hyt <= 0) {
            throw new IllegalArgumentException("height must be > 0");
        }
        this.name = name;
        width = wid;
        height = hyt;
        random = new Random(seed);
    }


    public int manhattanDistance(Point p1, Point p2) {
        int dx = Math.abs(p1.x - p2.x);
        int dy = Math.abs(p1.y - p2.y);
        return Math.min(dx, dy);
    }


    @Override
    public void setExcluding(Collection<Point> pointSet) {
        this.fixedPoints.clear();
        int i = 0;
        for (Point pt : new HashSet<>(pointSet)) {
            ++i;
            fixedPoints.put(pt, new NamedPoint("F" + i, pt, true));
        }
    }

    @Override
    public Set<Point> getExcluding() {
        return Collections.unmodifiableSet(fixedPoints.keySet());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean canUndo() {
        return currentState != null && currentState.previousStepState != null;
    }

    @Override
    public StepState undo() {
        if (! canUndo()) {
            if (currentState == null) {
                throw new IllegalStateException("No currentState");
            }
            throw new IllegalStateException("currentState has no prior");
        }
        StepState result = currentState.previousStepState;
        redoStack.push(result);
        return result;
    }

    @Override
    public boolean canRedo() {
        return ! redoStack.isEmpty();
    }

    @Override
    public void setPrintStream(PrintStream ps) {
        this.ps = ps==null ? System.out : ps;
    }

    @Override
    public StepState redo() {
        StepState result = redoStack.pop();
        currentState = result;
        return result;
    }

    protected String pointToString(Point pt) {
        return String.format("(%d,%d)", pt.x, pt.y);
    }

}
