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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.event.EventListenerList;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

public class ChoiceTreeTableModel<P,C> extends DefaultTreeTableModel{

    public interface ChoiceChangedListener extends EventListener {
        void choiceChanged(Object source, ChoiceNode[] changedNodes);
    }

    class ChildNode extends ChoiceNode {

        private final Function<C, String> nameProvider;

        ChildNode(C c, Function<C, String> nameProvider) {
            setUserObject(c);
            this.nameProvider = nameProvider;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int column) {
            switch (column) {
            case 0: return getName();
            case 1: return isChosen();
            }
            return null;
        }

        @Override
        public String getName() {
            return nameProvider.apply(getChildItem());
        }
        
        @SuppressWarnings("unchecked")
        public C getChildItem() {
            return (C) getUserObject();
        }
        
        @Override
        protected boolean isChild() {
            return true;
        }

        @Override
        protected boolean isParent() {
            return false;
        }

        @Override
        protected void childChanged(ChoiceNode choiceNode) {
            // NO-OP
        }
    }

    class ParentNode extends ChoiceNode {

        private final BiFunction<P,C,String> nameProvider;
        private C singleChild;

        ParentNode(P parent, List<C> childList, 
                BiFunction<P,C,String> nameProvider, 
                Function<C, String> childNameProvider)
        {
            setUserObject(parent);
            this.nameProvider = nameProvider;
            if (childList.size() == 1) {
                this.singleChild = childList.get(0);
            }
            else {
                for (C c : childList) {
                    ChildNode cnode = new ChildNode(c, childNameProvider);
                    insert(cnode, getChildCount());
                }
            }
        }

        @Override
        public int getColumnCount() {
            return 2; // anyParentHasChildren ? 3 : 2;
        }

        @Override
        public Object getValueAt(int column) {
            switch (column) {
            case 0: return getName();
            case 1: return isChosen();
            }
            return null;
        }

        public void visitChosenChildNodes(Predicate<C> visitor) {
            if (singleChild != null) {
                // singleton
                if (isChosen()) {
                    visitor.test(singleChild);
                }
            }
            else {
                for (Enumeration<? extends MutableTreeTableNode> children = children(); children.hasMoreElements(); ) {
                    MutableTreeTableNode child = children.nextElement();
                    if (child instanceof ChoiceTreeTableModel.ChildNode) {
                        @SuppressWarnings("unchecked")
                        ChildNode childNode = (ChildNode) child;
                        if (childNode.isChosen()) {
                            if (! visitor.test(childNode.getChildItem())) {
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        @Override
        public String getName() {
            return nameProvider.apply(getParentItem(), singleChild);
        }
        
        @SuppressWarnings("unchecked")
        public P getParentItem() {
            return (P) getUserObject();
        }
        
        @Override
        protected boolean isChild() {
            return false;
        }

        @Override
        protected boolean isParent() {
            return true;
        }

        @Override
        protected void childChanged(ChoiceNode child) {

            if (isChosen() != child.isChosen()) {
                // Only do something if child is different to our setting

                boolean oldChosen = isChosen();

                if (child.isChosen()) {
                    // so we were not
                    boolean allOthersChosen = true;
                    for (Enumeration<? extends MutableTreeTableNode> children = children(); children.hasMoreElements(); ) {
                        MutableTreeTableNode ch = children.nextElement();
                        if (ch instanceof ChoiceNode && ch != child) {
                            if (! ((ChoiceNode) ch).isChosen()) {
                                allOthersChosen = false;
                                break;
                            }
                        }
                    }

                    if (allOthersChosen) {
                        setChosen(true, null, false);
                    }
                }
                else {
                    // Child is not chosen (hence we must have been)
                    setChosen(false, null, false);
                }

                if (oldChosen != isChosen()) {
                    fireChoiceChanged(this);
                }
            }
        }
    }

    static private MutableTreeTableNode createRoot() {
        MutableTreeTableNode root = new AbstractMutableTreeTableNode() {
            @Override
            public Object getValueAt(int column) {
                return null;
            }
            @Override
            public int getColumnCount() {
                return 1;
            }
        };
        return root;
    }

    private final MutableTreeTableNode myRoot;

    private Map<P, ParentNode> parentNodeByItem = new HashMap<>();

    public ChoiceTreeTableModel(
            String nameHeading,
            String useHeading,
            Map<P, List<C>> childrenByParent, 
            BiFunction<P, C, String> parentNameProvider, 
            Function<C, String> childNameProvider) 
    {
        super(createRoot(), Arrays.asList(nameHeading, useHeading));

        this.myRoot = (MutableTreeTableNode) getRoot();

        for (P p : childrenByParent.keySet()) {
            List<C> list = childrenByParent.get(p);
            ParentNode pnode = new ParentNode(p, 
                    list, 
                    parentNameProvider,
                    childNameProvider);
            parentNodeByItem.put(p, pnode);
            myRoot.insert(pnode, myRoot.getChildCount());
        }
    }
    
    @SuppressWarnings("unchecked")
    public OptionalInt getChildChosenCountIfNotAllChosen(P p) {
        int count = 0;
        ParentNode pnode = parentNodeByItem.get(p);
        if (pnode != null && pnode.singleChild == null) {
            boolean allChosen = true;
            for (Enumeration<? extends MutableTreeTableNode> children = pnode.children();
                    children.hasMoreElements(); )
            {
                MutableTreeTableNode node = children.nextElement();
                if (node instanceof ChoiceTreeTableModel.ChildNode) {
                    if (((ChildNode) node).isChosen()) {
                        ++count;
                    }
                    else {
                        allChosen = false;
                    }
                }
            }
            
            if (! allChosen) {
                return OptionalInt.of(count);
            }
        }
        return OptionalInt.empty();
    }
    
    public void visitChosenNodes(Predicate<ChoiceNode> visitor) {
        for (Enumeration<? extends MutableTreeTableNode> children = myRoot.children();
                children.hasMoreElements(); )
        {
            MutableTreeTableNode node = children.nextElement();
            if (node instanceof ChoiceNode) {
                ((ChoiceNode) node).visit(visitor);
            }
        }
    }

    public Optional<ChoiceNode> findFirst(java.util.function.Predicate<ChoiceNode> predicate) {
        for (Enumeration<? extends MutableTreeTableNode> children = myRoot.children();
                children.hasMoreElements(); )
        {
            MutableTreeTableNode node = children.nextElement();
            if (node instanceof ChoiceNode) {
                ChoiceNode choiceNode = (ChoiceNode) node;
                Optional<ChoiceNode> opt = choiceNode.findFirst(predicate);
                if (opt.isPresent()) {
                    return opt;
                }
            }
        }
        return Optional.empty();
    }

    public boolean getAnyChosen() {
        Predicate<ChoiceNode> visitor = new Predicate<ChoiceNode>() {
            @Override
            public boolean test(ChoiceNode node) {
                return ! node.isChosen();
            }
        };
        visitChosenNodes(visitor);

        Optional<ChoiceNode> opt = findFirst(visitor);

        return opt.isPresent();
    }

    @SuppressWarnings("rawtypes")
    public List<ChoiceNode> setAllChosen(boolean b) {
        List<ChoiceNode> result = new ArrayList<>();
        for (Enumeration<? extends MutableTreeTableNode> children = myRoot.children();
                children.hasMoreElements(); )
        {
            MutableTreeTableNode node = children.nextElement();
            if (node instanceof ChoiceTreeTableModel.ParentNode) {
                ((ChoiceTreeTableModel.ParentNode) node).setChosen(true, result, true);
            }
        }
        return result;
    }

    public void visitChosenChildNodes(Predicate<C> visitor) {
        for (Enumeration<? extends MutableTreeTableNode> children = myRoot.children();
                children.hasMoreElements(); )
        {
            MutableTreeTableNode node = children.nextElement();
            if (node instanceof ChoiceTreeTableModel.ParentNode) {
                @SuppressWarnings("unchecked")
                ParentNode pnode = (ParentNode) node;
                pnode.visitChosenChildNodes(visitor);
            }
        }
    }

    // See ChoiceNode.getValueAt()
    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
        case 0: return String.class;
        case 1: return Boolean.class;
        case 2: return String.class;
        }
        return Object.class;
    }
    
    @Override
    public boolean isCellEditable(Object node, int col) {
        boolean result = false;
        if (node instanceof ChoiceNode) {
            result = ( col==1 );
        }
        return result;
    }

    @Override
    public void setValueAt(Object value, Object node, int column) {
        if (column==1 && value instanceof Boolean) {
            boolean b = ((Boolean) value).booleanValue();
            if (node instanceof ChoiceTreeTableModel.ParentNode) {
                @SuppressWarnings("unchecked")
                ParentNode pnode = (ParentNode) node;
                List<ChoiceNode> changed = new ArrayList<>();
                pnode.setChosen(b, changed, true);
                fireChoiceChanged(changed);
            }
            else if (node instanceof ChoiceTreeTableModel.ChildNode) {
                @SuppressWarnings("unchecked")
                ChildNode cnode = (ChildNode) node;
                List<ChoiceNode> changed = new ArrayList<>();
                cnode.setChosen(b, changed, true);
                fireChoiceChanged(changed);
            }
        }
    }
    
    private EventListenerList listenerList = new EventListenerList();

    protected void fireChoiceChanged(ChoiceNode ... nodes) {
        for (ChoiceChangedListener l : listenerList.getListeners(ChoiceChangedListener.class)) {
            l.choiceChanged(this, nodes);
        }
    }

    protected void fireChoiceChanged(List<ChoiceNode> list) {
        ChoiceNode[] nodes = null;
        for (ChoiceChangedListener l : listenerList.getListeners(ChoiceChangedListener.class)) {
            if (nodes == null) {
                nodes = list.toArray(new ChoiceNode[list.size()]);
            }
            l.choiceChanged(this, nodes);
        }
    }

    public void addChoiceChangedListener(ChoiceChangedListener l) {
        listenerList.add(ChoiceChangedListener.class, l);
    }

    public void removeChoiceChangedListener(ChoiceChangedListener l) {
        listenerList.remove(ChoiceChangedListener.class, l);
    }
}
