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

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdxplore.fielddesign.PositionChangedListener;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.Attribute;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.util.TableColumnInfo;

import net.pearcan.color.ColorGroups;
import net.pearcan.color.ColorPair;
import net.pearcan.color.ColorPairFactory;

@SuppressWarnings("nls")
public class PlantingBlockTableModel<E> extends AbstractTableModel implements Iterable<PlantingBlock<E>> {

    static abstract class EditableColumnInfo extends TableColumnInfo<PlantingBlock<?>> {
        EditableColumnInfo(String hdg, Class<?> cls) {
            super(hdg, cls);
        }
        abstract void setValue(PlantingBlock<?> tb, Integer value);
    }

    static class ColourInfo extends TableColumnInfo<PlantingBlock<?>> {
        private Map<PlantingBlock<?>, ColorPair> colorPairByTrialBlock;

        public ColourInfo(Map<PlantingBlock<?>, ColorPair> map) {
            super("Colour", Color.class);
            this.colorPairByTrialBlock = map;
        }

        @Override
        public Object getColumnValue(int rowIndex, PlantingBlock<?> t) {
            ColorPair cp = colorPairByTrialBlock.get(t);
            return cp==null ? null : cp.getBackground();
        }
    }

    static class SideInfo extends EditableColumnInfo {
        private Side side;

        SideInfo(Side side) {
            super(side.displayName, Integer.class);
            this.side = side;
        }
        @Override
        public Object getColumnValue(int rowIndex, PlantingBlock<?> t) {
            return t.getBorderCount(side);
        }
        @Override
        public void setValue(PlantingBlock<?> tb, Integer value) {
            tb.setBorder(side, value);
        }
    }

    static public <E> PlantingBlockTableModel<E> create(Consumer<PlantingBlock<E>[]> onEntryTypesChanged)
    {
        return create(onEntryTypesChanged, Heading.NAME.csvHeading);
    }


    static class AttributeColumnInfo extends TableColumnInfo<PlantingBlock<?>> {
        private final Attribute attribute;
        AttributeColumnInfo(Attribute attr) {
            super(attr.displayValue, attr.valueClass);
            this.attribute = attr;
        }
        @Override
        public Object getColumnValue(int rowIndex, PlantingBlock<?> t) {
            return attribute.getColumnValue(t);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static public <E> PlantingBlockTableModel<E> create(Consumer<PlantingBlock<E>[]> onEntryTypesChanged,
            String nameHeading)
    {
        List<TableColumnInfo<PlantingBlock<?>>> list = new ArrayList<>();

        Map<PlantingBlock<?>, ColorPair> map = new HashMap<>();
        Map<Attribute, Integer> columnIndexByAttribute = new HashMap<>();

        list.add(new ColourInfo(map));
        list.add(new TableColumnInfo<PlantingBlock<?>>(nameHeading, String.class) {
            @Override
            public Object getColumnValue(int rowIndex, PlantingBlock<?> t) {
                return t.getName();
            }
        });
        list.add(new TableColumnInfo<PlantingBlock<?>>(Heading.MINIMUM_PLOTS.csvHeading, Integer.class) {
            @Override
            public Object getColumnValue(int rowIndex, PlantingBlock<?> t) {
                return t.getMinimumCellCount();
            }
        });

        list.add(new TableColumnInfo<PlantingBlock<?>>(Heading.EXCESS.csvHeading, Integer.class) {
            @Override
            public Object getColumnValue(int rowIndex, PlantingBlock<?> t) {
                return t.getFillerCount();
            }
        });


        columnIndexByAttribute.put(Attribute.DIMENSION, list.size());
        list.add(new AttributeColumnInfo(Attribute.DIMENSION));

        columnIndexByAttribute.put(Attribute.POSITION, list.size());
        list.add(new AttributeColumnInfo(Attribute.POSITION));

        columnIndexByAttribute.put(Attribute.BORDERS, list.size());
        list.add(new AttributeColumnInfo(Attribute.BORDERS));

        return new PlantingBlockTableModel(onEntryTypesChanged, list, map, columnIndexByAttribute);
    }

    private final ColorPairFactory colorPairFactory;

    private final List<PlantingBlock<E>> data = new ArrayList<>();

    private List<PlantingBlock<E>> decreasingSize;

    private final Map<PlantingBlock<E>, ColorPair> colorPairByTrialBlock;

    private final Consumer<PlantingBlock<?>[]> onEntryTypesChanged;

    private final List<TableColumnInfo<PlantingBlock<?>>> columnInfoList;

    private final Map<Attribute, Integer> columnIndexByAttribute;

    private PlantingBlockTableModel(Consumer<PlantingBlock<?>[]> onEntryTypesChanged,
            List<TableColumnInfo<PlantingBlock<?>>> tciList,
            Map<PlantingBlock<E>, ColorPair> map,
            Map<Attribute, Integer> columnIndexByAttribute)
    {
        this.onEntryTypesChanged = onEntryTypesChanged;
        this.colorPairByTrialBlock = map;
        this.columnInfoList = tciList;
        this.columnIndexByAttribute = columnIndexByAttribute;

        boolean reverse = false;

        List<Color> list = new ArrayList<>();
        Collections.addAll(list, ColorGroups.COLOURS_GROUPED_BY_BRIGHTNESS);
        if (reverse) {
            Collections.reverse(list);
        }
        colorPairFactory = new ColorPairFactory(list.toArray(new Color[list.size()]));
    }

    public Set<String> getLowercaseTrialNames() {
        return data.stream()
            .map(tb -> tb.getName().toLowerCase())
            .collect(Collectors.toSet());
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnInfoList.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnInfoList.get(col).getColumnName();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        TableColumnInfo<?> tci = columnInfoList.get(columnIndex);
        //return tb.getCanEditSize();
        return tci instanceof EditableColumnInfo;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        TableColumnInfo<?> tci = columnInfoList.get(columnIndex);
        if (! (tci instanceof EditableColumnInfo)) {
            return;
        }
        if (! (aValue instanceof Integer)) {
            return;
        }

        Integer value = (Integer) aValue;
        EditableColumnInfo eci = (EditableColumnInfo) tci;

        PlantingBlock<E> tb = data.get(rowIndex);

        eci.setValue(tb, value);

        String hdg = eci.getColumnName();

        if (Heading.COLUMN_COUNT.csvHeading.equals(hdg)
                ||
            Heading.ROW_COUNT.csvHeading.equals(hdg))
        {
            decreasingSize = null;
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return columnInfoList.get(col).getColumnClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PlantingBlock<E> r = data.get(rowIndex);
        return columnInfoList.get(columnIndex).getColumnValue(rowIndex, r);
    }

    public boolean containsName(String name) {
        Optional<PlantingBlock<E>> find  = data.stream()
                .filter(tr -> tr.getName().equalsIgnoreCase(name))
                .findFirst();
        return find.isPresent();
    }
    /**
     * Return true if added
     * @param more
     * @return
     */
    public boolean addOne(PlantingBlock<E> more) {
        decreasingSize = null;

        if (containsName(more.getName())) {
            return false;
        }
        int row = data.size();
        data.add(more);

        ColorPair cp = colorPairByTrialBlock.get(more);
        if (cp == null) {
            cp = colorPairFactory.getNextColorPair();
            colorPairByTrialBlock.put(more, cp);
        }

        fireTableRowsInserted(row, row);
        return true;
    }

    private void setData(List<PlantingBlock<E>> input) {
        if (! data.isEmpty()) {
            data.forEach(releaseColorPair);
        }

        data.clear();

        for (PlantingBlock<E> tb : input) {
            addOne(tb);
        }
        fireTableDataChanged();
    }

    // Replace the current data with input but if any
    // in data have the same name then retain the X,Y position.
    public void replaceData(List<PlantingBlock<E>> input) {
        if (data.isEmpty()) {
            setData(input);
            return;
        }

        Map<String, PlantingBlock<E>> dataByName = data.stream()
            .collect(Collectors.toMap(PlantingBlock::getName, Function.identity()));

        for (PlantingBlock<E> new_tb : input) {
            ColorPair cp = null;

            PlantingBlock<E> old = dataByName.get(new_tb.getName());
            if (old != null) {
                cp = colorPairByTrialBlock.remove(old);
                new_tb.setX(old.getX());
                new_tb.setY(old.getY());
                Integer new_mincc = new_tb.getMinimumCellCount();
                if (new_mincc != null && new_mincc.equals(old.getMinimumCellCount())) {
                    new_tb.setFinalColumnCount(old.getColumnCount());
                    new_tb.setFinalRowCount(old.getRowCount());
                }
            }
            if (cp == null) {
                cp = colorPairFactory.getNextColorPair();
            }
            colorPairByTrialBlock.put(new_tb, cp);
        }

        decreasingSize = null;
        data.clear();
        data.addAll(input);
        fireTableDataChanged();
    }

    private final Consumer<PlantingBlock<E>> releaseColorPair = new Consumer<PlantingBlock<E>>() {
        @Override
        public void accept(PlantingBlock<E> tb) {
            ColorPair cp = colorPairByTrialBlock.remove(tb);
            if (cp != null) {
                colorPairFactory.release(cp);
            }
        }
    };

    public void removeRows(List<Integer> rowsIn) {

        decreasingSize = null;

        List<Integer> rows = new ArrayList<>(rowsIn);
        Collections.sort(rows, Collections.reverseOrder());
        for (Integer row : rows) {
            int rowIndex = row;
            PlantingBlock<E> removed = data.remove(rowIndex);
            if (removed != null) {
                relelaseColorPairFor(removed);
            }
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    private void relelaseColorPairFor(PlantingBlock<E> tb) {
        ColorPair cp = colorPairByTrialBlock.get(tb);
        if (cp != null) {
            colorPairFactory.release(cp);
        }
    }

    public List<PlantingBlock<E>> getPlantingBlocks() {
        return Collections.unmodifiableList(data);
    }

    public List<PlantingBlock<E>> getBlocksInDecreasingSize() {
        if (decreasingSize == null) {
            List<PlantingBlock<E>> list = new ArrayList<>();
            list.addAll(data);
            Collections.sort(list, DECREASING_SIZE_COMPARATOR);
            decreasingSize = Collections.unmodifiableList(list);
        }
        return decreasingSize;
    }

    static private final Comparator<PlantingBlock<?>> DECREASING_SIZE_COMPARATOR = new Comparator<PlantingBlock<?>>() {
        @Override
        public int compare(PlantingBlock<?> o1, PlantingBlock<?> o2) {
            int size_1 = o1.getColumnCount() * o1.getRowCount();
            int size_2 = o2.getColumnCount() * o2.getRowCount();
            int diff = Integer.compare(size_2, size_1);
            if (diff == 0) {
                diff = o1.getName().compareTo(o2.getName());
            }
            return diff;
        }

    };

    public ColorPair getColorPair(PlantingBlock<E> tb) {
        return colorPairByTrialBlock.get(tb);
    }

    @SafeVarargs
    final public void blockChanged(WhatChanged whatChanged, PlantingBlock<E> ... blocks) {
        switch (whatChanged) {
        case ENTRY_TYPES:
            if (onEntryTypesChanged != null) {
                onEntryTypesChanged.accept(blocks);
            }
            break;
        case DIMENSION:
            for (PlantingBlock<E> block : blocks) {
                int index = data.indexOf(block);
                if (index >= 0) {
                    fireTableRowsUpdated(index, index);
                }
            }
            fireBlockDimensionChanged(blocks);
            break;
        case POSITION:
            for (PlantingBlock<E> block : blocks) {
                int index = data.indexOf(block);
                if (index >= 0) {
                    fireTableRowsUpdated(index, index);
                }
            }
            fireBlockPositionsChanged(blocks);
            break;
        case BORDER:
            for (PlantingBlock<E> block : blocks) {
                int index = data.indexOf(block);
                if (index >= 0) {
                    fireTableRowsUpdated(index, index);
                }
            }
            fireBlockBordersChanged(blocks);
            break;
        default:
            break;

        }
    }

    @SuppressWarnings("unchecked")
    protected void fireBlockPositionsChanged(PlantingBlock<E> ... blocks) {
        for (PositionChangedListener l : listenerList.getListeners(PositionChangedListener.class)) {
            l.blockPositionsChanged(this, blocks);
        }
    }

    @SuppressWarnings("unchecked")
    protected void fireBlockDimensionChanged(PlantingBlock<E> ... blocks) {
        for (PositionChangedListener l : listenerList.getListeners(PositionChangedListener.class)) {
            l.blockDimensionChanged(this, blocks);
        }
    }

    @SuppressWarnings("unchecked")
    protected void fireBlockBordersChanged(PlantingBlock<E> ... blocks) {
        for (PositionChangedListener l : listenerList.getListeners(PositionChangedListener.class)) {
            l.blockBordersChanged(this, blocks);
        }
    }

    public void addPositionChangedListener(PositionChangedListener l) {
        listenerList.add(PositionChangedListener.class, l);
    }
    public void removePositionChangedListener(PositionChangedListener l) {
        listenerList.remove(PositionChangedListener.class, l);
    }

    @Override
    public Iterator<PlantingBlock<E>> iterator() {
        return data.iterator();
    }

    public PlantingBlock<E> getItemAt(int rowIndex) {
        return data.get(rowIndex);
    }

    public boolean isAnyOtherBlockIntersecting(PlantingBlock<E> block) {
        if (block != null) {
            Rectangle r = block.getRectangle();
            for (PlantingBlock<E> b : data) {
                if (b != block) {
                    if (b.getRectangle().intersects(r)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PlantingBlock<E> removeByReplicate(int replicate) {
        for (int index = getRowCount(); --index >= 0; ) {
            PlantingBlock<E> b = data.get(index);
            if (b.getReplicateNumber()==replicate) {
                data.remove(index);
                decreasingSize = null;
                fireTableRowsDeleted(index, index);
                return b;
            }
        }
        return null;
    }

    public int indexOf(PlantingBlock<E> block) {
        return data.indexOf(block);
    }

//    public int getColumnIndexFor(Attribute attr) {
//        Integer result = columnIndexByAttribute.get(attr);
//        return result==null ? -1 : result.intValue();
//    }
//
//    public boolean isColumnIndexFor(int columnIndex, Attribute attribute) {
//        return columnIndex == getColumnIndexFor(Attribute.DIMENSION);
//    }

    public Optional<Attribute> getAttributeFor(int columnIndex) {
        Optional<Map.Entry<Attribute, Integer>> found = columnIndexByAttribute.entrySet().stream()
            .filter(e -> columnIndex == e.getValue())
            .findFirst();
        if (found.isPresent()) {
            return Optional.of(found.get().getKey());
        }
        return Optional.empty();
    }


}
