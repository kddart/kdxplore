package com.diversityarrays.kdxplore.fielddesign.alg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.EntryType.Variant;
import com.diversityarrays.kdxplore.fielddesign.CellShape;
import com.diversityarrays.kdxplore.fielddesign.ContentRenderer;
import com.diversityarrays.kdxplore.fielddesign.DefaultFieldModel;
import com.diversityarrays.kdxplore.fielddesign.DefaultPlantingBlockRenderer;
import com.diversityarrays.kdxplore.fielddesign.FieldCell;
import com.diversityarrays.kdxplore.fielddesign.FieldCellType;
import com.diversityarrays.kdxplore.fielddesign.FieldModel;
import com.diversityarrays.kdxplore.fielddesign.FieldView;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockSelectionEvent;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockTableModel;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.ColorSupplier;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.NameMaker;

import net.pearcan.color.ColorPair;
import net.pearcan.ui.widget.MessagesPanel;
import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.GBH;

@SuppressWarnings("nls")
public class SpatialMain extends JFrame {

    private static final String TAG = "SpatialMain";

    static enum Which {
        GUI, TEST, LLOYDS, FARTHEST_FIRST, COMBINED,
        ;

        public static Which[] valuesForCombo() {
            List<Which> list = Arrays.asList(values()).stream()
                    .filter(w -> w != GUI && w != TEST)
                    .collect(Collectors.toList());
            return list.toArray(new Which[list.size()]);
        }

        public IterableAlg create(int wid, int hyt, long seed) {
            switch (this) {
            case COMBINED:
                return new CombinedAlg(wid, hyt, seed);
            case FARTHEST_FIRST:
                return new FarthestFirst(wid, hyt, seed, 1);
            case LLOYDS:
                return new LloydsAlg(wid, hyt, seed);
            case GUI:
            default:
                throw new IllegalArgumentException(this.name() + ".create not supported");
            }
        }

    }

    public static void main(String[] args) {

        Which which = Which.GUI;

        switch (which) {
        case TEST:
            Consumer<List<String>> printIt = new Consumer<List<String>>() {
                @Override
                public void accept(List<String> t) {
                    if (!t.isEmpty()) {
                        System.out.println(t.stream().map(s -> String.format("%2s", s))
                                .collect(Collectors.joining(", ")));
                    }
                }
            };

            NameMaker nameMaker = new NameMaker('a');
            // List<String> names = IntStream.rangeClosed(1, 64).mapToObj(i ->
            // nameMaker.apply(i*2-2))
            // .collect(Collectors.toList());
            List<String> names = IntStream.rangeClosed(1, 64).mapToObj(i -> nameMaker.get())
                    .collect(Collectors.toList());
            while (names.size() > 30) {
                printIt.accept(names.subList(0, 30));
                names = names.subList(30, names.size());
            }
            printIt.accept(names);
            return;
        case GUI:
            SwingUtilities.invokeLater(() -> new SpatialMain().setVisible(true));
            break;
        default:
            IterableAlg alg = which.create(10, 8, System.currentTimeMillis());
            alg.start(10);
            while (alg.step().isPresent())
                ;

            Set<NamedPoint> clusterPoints = alg.getClusterPoints();
            System.out.println("Found " + clusterPoints.size() + " cluster Points");
            for (NamedPoint pt : clusterPoints) {
                System.out.println(String.format("\t%s", pt.toString()));
            }
            return;
        }
    }

    private Function<Point, FieldCell<?>> borderFactory = new Function<Point, FieldCell<?>>() {
        @Override
        public FieldCell<?> apply(Point pt) {
            return new FieldCell<>(pt, String.format("%d,%d", pt.x, pt.y),
                    FieldCellType.BORDER_PLOT);
        }
    };

    private FieldModel model = new DefaultFieldModel(borderFactory);

    Consumer<PlantingBlock<EntryContent>[]> onEntryTypesChanged = new Consumer<PlantingBlock<EntryContent>[]>() {
        @Override
        public void accept(PlantingBlock<EntryContent>[] t) {
            System.out.println(TAG + " onEntryTypesChanged");
            for (PlantingBlock<?> tb : t) {
                System.out.println("\t" + tb.getName());
            }
            System.out.println("- - - - - - - - - - -");
        }
    };

    private PlantingBlockTableModel<EntryContent> tbtm = PlantingBlockTableModel
            .create(onEntryTypesChanged);

    private Consumer<PlantingBlockSelectionEvent<EntryContent>> onBlockClicked = new Consumer<PlantingBlockSelectionEvent<EntryContent>>() {
        @Override
        public void accept(PlantingBlockSelectionEvent<EntryContent> t) {
            System.out.println("SpatialMain.onBlockClicked: " + t.plantingBlock);
        }
    };

    private final FieldView<EntryContent> fieldView = new FieldView<>(model, tbtm, onBlockClicked);

    private final SpinnerNumberModel percentSpatial = new SpinnerNumberModel(3.0, 0.0, 90.0, 1.0);
    private final JTextField nSpatialsText = new JTextField(4);

    private final JComboBox<Which> whichComboBox = new JComboBox<>(Which.valuesForCombo());

    private IterableAlg iterableAlg;

    private Action stepAction = new AbstractAction("Step") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Optional<StepState> optional = iterableAlg.step();
            if (optional.isPresent()) {
                StepState state = optional.get();
                Map<Point, EntryContent> newContentByPoint = makeEntryContentMap(editingBlock,
                        state);
                editingBlock.setContentUsing(newContentByPoint);
                fieldView.repaint();
            }
            else {
                stepAction.setEnabled(false);
                new Toast(stepButton, "No more changes", Toast.SHORT).show();
            }
            updateUndoRedoActions();
        }
    };
    private JButton stepButton = new JButton(stepAction);

    private Action clearSpatials = new AbstractAction("Clear") {
        @Override
        public void actionPerformed(ActionEvent e) {
            editingBlock.clearContent();
            fieldView.repaint();
        }
    };

    private Action startAction = new AbstractAction("Start") {
        @Override
        public void actionPerformed(ActionEvent e) {

            int nSpatials = computeSpatialCount();

            if (nSpatials <= 0) {
                MsgBox.warn(SpatialMain.this, "nSpatials == " + nSpatials,
                        "% Spatials too Low");
            }
            else {
                editingBlock.setSpatialChecksCount(nSpatials);
                PrintStream ps = messagesPanel.getPrintStream();

                int nCols = editingBlock.getColumnCount();
                int nRows = editingBlock.getRowCount();
                long seed = System.currentTimeMillis();

                Which which = (Which) whichComboBox.getSelectedItem();

                iterableAlg = which.create(nCols, nRows, seed);
                iterableAlg.setPrintStream(ps);

                Set<Point> excluding = editingBlock.getContentByPoint().entrySet().stream()
                    .filter(me -> me.getValue().entryType==FIXED)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
                iterableAlg.setExcluding(excluding);

                StepState state = iterableAlg.start(nSpatials - excluding.size());

                Map<Point, EntryContent> contentByPoint = makeEntryContentMap(editingBlock, state);
                editingBlock.setContentUsing(contentByPoint);

                stepAction.setEnabled(true);

                updateUndoRedoActions();
                fieldView.repaint();
            }
        }
    };

    private Action undoAction = new AbstractAction("Undo") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StepState state = iterableAlg.undo();

            Map<Point, EntryContent> contentByPoint = makeEntryContentMap(editingBlock, state);
            editingBlock.setContentUsing(contentByPoint);
            fieldView.repaint();

            updateUndoRedoActions();
        }
    };

    private Map<Point, EntryContent> makeEntryContentMap(PlantingBlock<EntryContent> tb,
            StepState state) {

        Map<Point, EntryContent> result = new HashMap<>();

        EntryType currentEntryType = spatialEntryTypes.getEntryTypeFor(SpatialEntryTypes.CURRENT);

        // Map<Point, EntryContent> oldMap = tb.getContentByPoint();
        Map<Point, EntryContent> fixedPoints = tb.getContentByPoint().entrySet().stream()
                .filter(e -> e.getValue().entryType == FIXED)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        result.putAll(fixedPoints);

        for (NamedPoint pt : state.spatials) {
            result.put(pt.point, new EntryContent(currentEntryType, pt, state.generation));
        }

        // Do we need to show previous?
        if (state.previousStepState != null) {
            int previousGeneration = state.previousStepState.generation;
            EntryType previousEntryType = (state.previousStepState == null)
                    ? null
                    : spatialEntryTypes.getEntryTypeFor(SpatialEntryTypes.PREVIOUS);

            Set<NamedPoint> spatialPoints = new HashSet<>();
            Collections.addAll(spatialPoints, state.spatials);
            for (NamedPoint pt : state.previousStepState.spatials) {
                if (spatialPoints.contains(pt)) {
                    // present in both !
                    // TODO somehow show the difference
                }
                else {
                    // Not current, so add it in
                    result.put(pt.point,
                            new EntryContent(previousEntryType, pt, previousGeneration));
                }
            }
        }

        // Make sure we REMOVE the old ones by zapping inserting a null entry
        Map<Point, EntryContent> oldContent = tb.getContentByPoint();
        for (Point pt : oldContent.keySet()) {
            if (!result.containsKey(pt)) {
                result.put(pt, null);
            }
        }

        return result;
    }

    private Action redoAction = new AbstractAction("Redo") {
        @Override
        public void actionPerformed(ActionEvent e) {
            StepState state = iterableAlg.redo();
            Map<Point, EntryContent> contentByPoint = makeEntryContentMap(editingBlock, state);
            editingBlock.setContentUsing(contentByPoint);
            fieldView.repaint();

            updateUndoRedoActions();
        }
    };

    private Function<PlantingBlock<EntryContent>, ColorPair> block_cpp = null;

    private Function<EntryType, Color> entryType_cp = new ColorSupplier<>();

    private Function<EntryContent, Color> blockEntry_cp = new Function<EntryContent, Color>() {
        @Override
        public Color apply(EntryContent t) {
            if (FIXED == t.entryType) {
                return Color.RED;
            }
            else {
                if (t.namedPoint.fixed) {
                    return Color.RED;
                }
            }
            return entryType_cp.apply(t.entryType);
        }

    };

    private final List<EntryType> types = new ArrayList<>();

    private DefaultComboBoxModel<EntryType> entryTypeModel = new DefaultComboBoxModel<>();
    private JComboBox<EntryType> entryTypeCombo = new JComboBox<>(entryTypeModel);

    private PlantingBlock<EntryContent> editingBlock;

    class SpatialEntryTypes {

        public static final boolean CURRENT = true;
        public static final boolean PREVIOUS = false;

        private final EntryType currentGeneration = new EntryType("Spatial0", Variant.SPATIAL);
        private final EntryType previousGeneration = new EntryType("Spatial1", Variant.SPATIAL);

        SpatialEntryTypes() {
        }

        public EntryType getEntryTypeFor(boolean current) {
            return current ? currentGeneration : previousGeneration;
        }
    }

    private final SpatialEntryTypes spatialEntryTypes;

    private MessagesPanel messagesPanel = new MessagesPanel();

    private ContentRenderer<EntryContent> contentRenderer = new ContentRenderer<SpatialMain.EntryContent>() {

        private Function<EntryContent, Color> provider;

        @Override
        public void setContentColorProvider(Function<EntryContent, Color> provider) {
            this.provider = provider;
        }

        @Override
        public Function<EntryContent, Color> getContentColorProvider() {
            return provider;
        }

        @Override
        public void draw(Graphics2D g2d,
                Composite transparent,
                Point p,
                EntryContent content,
                int cellWidth, int cellHeight,
                boolean preserveColor) {
            Color c;
            if (provider != null) {
                c = provider.apply(content);
            }
            else {
                int hc = content == null ? 0 : content.hashCode();
                c = new Color(hc & 0x00ffffff);
            }

            Color save = g2d.getColor(); // preserveColor ? g2d.getColor() :
                                         // null;
            g2d.setColor(c);
            g2d.fillRect(p.x, p.y, cellWidth, cellHeight);

            FontMetrics fm = g2d.getFontMetrics();
            g2d.setColor(Color.BLACK);
            g2d.drawString(content.namedPoint.name, p.x + 1, p.y + fm.getMaxAscent());

            if (save != null) {
                g2d.setColor(save);
            }
        }
    };

    static private class EntryContent {

        private final EntryType entryType;
        private final int[] generations;
        public final NamedPoint namedPoint;

        public EntryContent(EntryType t, NamedPoint npt, int g) {
            entryType = t;
            namedPoint = npt;
            generations = new int[] { g };
        }

        private EntryContent(EntryType t, NamedPoint npt, int[] gs) {
            entryType = t;
            namedPoint = npt;
            generations = gs;
        }

        @Override
        public int hashCode() {
            int v = entryType.hashCode();
            for (int g : generations) {
                v ^= Integer.hashCode(g);
            }
            v ^= namedPoint.hashCode();
            return v;
        }

        @Override
        public boolean equals(Object o) {
            if (this==o) return false;
            if (! (o instanceof EntryContent)) return false;
            EntryContent other = (EntryContent) o;
            if (! this.entryType.equals(other.entryType)) return false;
            if (this.generations.length != other.generations.length) return false;
            for (int i = generations.length; --i >= 0; ) {
                if (this.generations[i] != other.generations[i]) return false;
            }
            return this.namedPoint.equals(other.namedPoint);
        }

        // public EntryContent prevGeneration(EntryType newEntryType) {
        // int[] new_gs = new int[generations.length - 1];
        // System.arraycopy(generations, 1, new_gs, 0, new_gs.length);
        // return new EntryContent(newEntryType, new_gs);
        // }

        public EntryContent nextGeneration(EntryType newEntryType, NamedPoint npt, int g) {
            int[] new_gs = new int[generations.length + 1];
            System.arraycopy(generations, 0, new_gs, 1, generations.length);
            new_gs[0] = g;
            return new EntryContent(newEntryType, npt, new_gs);
        }

        @Override
        public String toString() {
            if (generations.length <= 0) {
                return "?";
            }
            return IntStream.of(generations)
                    .mapToObj(i -> String.valueOf(i)).collect(Collectors.joining(","));
        }
    }

    static class SimpleFactory
            implements BiFunction<PlantingBlock<EntryContent>, Point, EntryContent> {

        private final EntryType entryType;
        private final int generation;
        private Map<Point, NamedPoint> map;

        public SimpleFactory(EntryType t, int g, Map<Point, NamedPoint> map) {
            entryType = t;
            generation = g;
            this.map = map != null ? map : Collections.emptyMap();
        }

        @Override
        public EntryContent apply(PlantingBlock<EntryContent> t, Point u) {
            EntryContent old = t.getContent();
            EntryContent result;
            NamedPoint npt = map.get(u);
            if (npt == null) {
                if (FIXED == entryType) {
                    npt = new NamedPoint("**", u, true);
                }
                else {
                    npt = new NamedPoint("--", u, false);
                }
            }
            if (old == null) {
                result = new EntryContent(entryType, npt, generation);
            }
            else {
                result = old.nextGeneration(entryType, npt, generation);
            }
            return result;
        }
    }

    static private final EntryType FIXED = new EntryType("Fixed", Variant.SPATIAL);

    SpatialMain() {
        super("Spatial Main");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        KDClientUtils.initAction(ImageId.UNDO_24, undoAction, "");
        KDClientUtils.initAction(ImageId.REDO_24, redoAction, "");

        fieldView.setPlantingBlockOpacity(0.9f);

        updateUndoRedoActions();

        spatialEntryTypes = new SpatialEntryTypes();

        types.add(spatialEntryTypes.getEntryTypeFor(SpatialEntryTypes.CURRENT));
        types.add(spatialEntryTypes.getEntryTypeFor(SpatialEntryTypes.PREVIOUS));

        entryTypeModel.removeAllElements();
        entryTypeModel.addElement(FIXED);
        for (EntryType t : types) {
            entryTypeModel.addElement(t);
        }
        entryTypeCombo.setSelectedItem(FIXED);

        SimpleFactory factory = new SimpleFactory(FIXED, 1, null);
        fieldView.setContentFactory(factory);

        entryTypeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object item = entryTypeCombo.getSelectedItem();
                if (item instanceof EntryType) {
                    fieldView.setContentFactory(new SimpleFactory((EntryType) item, 1, null));
                }
                else {
                    fieldView.setContentFactory(null);
                }
            }
        });

        int wid = 30;
        int hyt = 20;

        DefaultPlantingBlockRenderer<EntryContent> tbRenderer = new DefaultPlantingBlockRenderer<>(
                block_cpp, blockEntry_cp);
        tbRenderer.setContentRenderer(contentRenderer);
        fieldView.setPlantingBlockRenderer(tbRenderer);
        editingBlock = new PlantingBlock<>(1, "Trial-1", wid, hyt, 0);

        model.setColumnRowCount(wid, hyt);

        tbtm.addOne(editingBlock);

        // fieldView.setBackground(Color.LIGHT_GRAY);
        // fieldView.setForeground(Color.YELLOW);

        fieldView.setCellShape(CellShape.SQUARE);

        fieldView.setEditingPlantingBlock(editingBlock);

        Box buttons = Box.createHorizontalBox();
        buttons.add(whichComboBox);
        buttons.add(new JButton(startAction));
        buttons.add(stepButton);
        buttons.add(new JSeparator(JSeparator.VERTICAL));
        buttons.add(new JButton(undoAction));
        buttons.add(new JButton(redoAction));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(new JButton(clearSpatials));

        nSpatialsText.setEditable(false);
        nSpatialsText.setBorder(new LineBorder(Color.DARK_GRAY));
        JPanel controls = new JPanel();
        GBH gbh = new GBH(controls, 1, 2, 1, 0);
        int y = 0;
        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "% spatials");
        gbh.add(1, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, new NumberSpinner(percentSpatial, "0.0"));
        gbh.add(2, y, 1, 1, GBH.NONE, 0, 1, GBH.CENTER, "=");
        gbh.add(3, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, nSpatialsText);
        ++y;

        gbh.add(0, y, 1, 1, GBH.NONE, 0, 1, GBH.EAST, "Editing Entry Type:");
        gbh.add(1, y, 1, 1, GBH.NONE, 1, 1, GBH.WEST, entryTypeCombo);
        ++y;

        gbh.add(0, y, 4, 1, GBH.HORZ, 2, 2, GBH.CENTER, buttons);
        ++y;

        percentSpatial.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateNSpatialsText();
            }
        });
        updateNSpatialsText();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(fieldView),
                messagesPanel);
        splitPane.setResizeWeight(0.5);

        Container cp = getContentPane();
        cp.add(controls, BorderLayout.NORTH);
        cp.add(splitPane, BorderLayout.CENTER);

        pack();

        setSize(800, 600);
    }

    protected void updateUndoRedoActions() {
        if (iterableAlg == null) {
            undoAction.setEnabled(false);
            redoAction.setEnabled(false);
        }
        else {
            undoAction.setEnabled(iterableAlg.canUndo());
            redoAction.setEnabled(iterableAlg.canRedo());
        }
    }

    protected int computeSpatialCount() {
        double percent = percentSpatial.getNumber().doubleValue();

        int nCols = editingBlock.getColumnCount();
        int nRows = editingBlock.getRowCount();

        int nSpatials = (int) (percent * nCols * nRows / 100.0);
        return nSpatials;
    }

    protected void updateNSpatialsText() {
        int n = computeSpatialCount();
        editingBlock.setSpatialChecksCount(n);
        nSpatialsText.setText(Integer.toString(n));
        startAction.setEnabled(n > 0);
        stepAction.setEnabled(false);
    }

    // @SuppressWarnings("unused")
    // private void addRandomTypes(PlantingBlock<EntryContent> tb) {
    // Random random = new Random(System.currentTimeMillis());
    // for (int y = tb.getRowCount(); --y >= 0; ) {
    // for (int x = tb.getColumnCount(); --x >= 0; ) {
    // if (random.nextDouble() < 0.7) {
    // int idx = random.nextInt(types.size());
    // tb.addEntryTypeAt(types.get(idx), null, new Point(x,y));
    // }
    // }
    // }
    // }
}
