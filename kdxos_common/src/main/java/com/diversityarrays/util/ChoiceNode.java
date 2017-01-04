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
package com.diversityarrays.util;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

public abstract class ChoiceNode extends AbstractMutableTreeTableNode {

    enum Propagate {
        DONT,
        UP,
        DOWN
    }
    private boolean chosen = false;

    public ChoiceNode() { }
    
    abstract public String getName();
    
    public boolean isChosen() {
        return chosen;
    }

    protected void setChosen(boolean b, List<ChoiceNode> changed, boolean propagate) {
//        System.out.println("setChosen for " + this.getClass().getSimpleName() + ": " + this);

        if (chosen != b) {
            if (changed != null) {
                changed.add(this);
            }
        }
        chosen = b;

        if (propagate) {
            if (isParent()) {
                // propagate down to children
                for (int index = getChildCount(); --index >= 0; ) {
                    TreeTableNode child = getChildAt(index);
                    if (child instanceof ChoiceNode) {
                        ((ChoiceNode) child).setChosen(b, changed, false);
                    }
                }
            }

            if (isChild()) {
                // propagate upwards to the parent
                TreeTableNode parent = getParent();
                if (parent instanceof ChoiceNode) {
                    ((ChoiceNode) parent).childChanged(this);
                }
            }
        }
    }

    abstract protected boolean isChild();
    abstract protected boolean isParent();
    abstract protected void childChanged(ChoiceNode choiceNode);

//    abstract public boolean isParentNode();
//    abstract public boolean isChildNode();
    
    // TODO check if we should return boolean to short circut visiting
    public void visit(Predicate<ChoiceNode> visitor) {
        if (visitor.test(this)) {
            for (int index = getChildCount(); --index >= 0; ) {
                TreeTableNode child = getChildAt(index);
                if (child instanceof ChoiceNode) {
                    ((ChoiceNode) child).visit(visitor);
                }
            }
        }
    }

    public Optional<ChoiceNode> findFirst(Predicate<ChoiceNode> predicate) {
        if (predicate.test(this)) {
            return Optional.of(this);
        }
        for (int index = getChildCount(); --index >= 0; ) {
            TreeTableNode child = getChildAt(index);
            if (child instanceof ChoiceNode) {
                ChoiceNode cnode = (ChoiceNode) child;
                Optional<ChoiceNode> opt = cnode.findFirst(predicate);
                if (opt.isPresent()) {
                    return opt;
                }
            }
        }
        return Optional.empty();
    }

//    public boolean isAnyChildChosen() {
//        for (int index = getChildCount(); --index >= 0; ) {
//            TreeTableNode child = getChildAt(index);
//            if (child instanceof ChoiceNode) {
//                if (((ChoiceNode) child).isChosen()) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}
