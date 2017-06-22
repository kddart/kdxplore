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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.diversityarrays.kdsmart.scoring.PlotsPerGroup;
import com.diversityarrays.kdxplore.curate.CollectionPathSetupDialog;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.EntryTypeCounter;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel.DetailsChangeListener;
import com.diversityarrays.kdxplore.fielddesign.EditModeWidget;
import com.diversityarrays.kdxplore.fielddesign.FieldLayoutEditPanel;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.kdxplore.fieldlayout.TrialEntryAssigner.Error;
import com.diversityarrays.kdxplore.trialdesign.TrialEntry;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.OptionalCheckboxRenderer;
import com.diversityarrays.util.VisitOrder2D;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.renderer.ColorCellRenderer;
import net.pearcan.util.GBH;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
class ReplicateDetailsPanel extends JPanel {

    static class EntryTypeCounterRenderer extends DefaultTableCellRenderer {

        private Color histogramColor = Color.PINK;

        EntryTypeCounter counter;
        EntryTypeCounterRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        protected void setValue(Object value) {
            if (value instanceof EntryTypeCounter) {
                counter = (EntryTypeCounter) value;
            }
            else {
                counter = null;
            }
            super.setValue(value);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (counter != null) {
                Color saveColor = g.getColor();

                double ratio = counter.getRatio();
                if (ratio > 1.0) {
                    g.setColor(Color.RED);
                    ratio = 1.0;
                }
                Rectangle bounds = getBounds();

                int x = 0;
                int y = 0;
                int drawHyt = bounds.height;
                int drawWid = (int) (ratio * bounds.width);

                g.setXORMode(histogramColor);

                g.drawRect(x, y, drawWid, drawHyt);
                g.fillRect(x, y, drawWid, drawHyt);
                g.setPaintMode();
                g.setColor(saveColor);

            }
        }
    }

    static class SimpleContentFactory implements BiFunction<PlantingBlock<ReplicateCellContent>, Point, ReplicateCellContent> {

        private final EntryType entryType;
        public SimpleContentFactory(EntryType t) {
            this.entryType = t;
        }
        @Override
        public ReplicateCellContent apply(PlantingBlock<ReplicateCellContent> t, Point u) {
            return new ReplicateCellContent(entryType);
        }
    }

    enum EditWhat {
        SPATIAL("Spatial"), // TODO use the TrialDesignPreferences name
        NON_SPATIAL("Other Entry Types");

        public final String visible;
        EditWhat(String s) {
            visible = s;
        }

        @Override
        public String toString() {
            return visible;
        }
    }

    private final ReplicateDetailsModel replicateDetailsModel;
    private final JTable entryTypesTable;

    private final Consumer<String> messagePrinter;

    private final Action assignEntriesAction = new AbstractAction("Assign Entries") {
        @Override
        public void actionPerformed(ActionEvent e) {

            Either<String, Optional<EntryType>> either = replicateDetailsModel.getSpatialEntryType();
            if (either.isLeft()) {
                MsgBox.info(ReplicateDetailsPanel.this, either.left(), "Internal Error");
                return;
            }

            EntryType spatial = either.right().isPresent() ? either.right().get() : null;

            ReplicateDetailsModel target = replicateDetailsModel;
            Collection<ReplicateDetailsModel> allModels = dataProvider.getAllReplicateDetailModels();

            List<TrialEntry> trialEntries = dataProvider.getTrialEntryFile().getEntries();

            Optional<TrialEntry> opt_nesting = trialEntries.stream()
                    .filter(te -> ! Check.isEmpty(te.getNesting()))
                    .findFirst();
            if (opt_nesting.isPresent()) {
                if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(ReplicateDetailsPanel.this,
                        "<HTML><i>Nest</i> support is not yet enabled.<BR>Do you want to continue?",
                        "Assign Entries",
                        JOptionPane.YES_NO_OPTION))
                {
                    return;
                }
            }

            TrialEntryAssigner assigner = new TrialEntryAssigner(allModels,
                    replicateDetailsModel,
                    spatial,
                    trialEntries,
                    visitOrder,
                    onModelAssignmentDone);

            Predicate<Collection<ReplicateDetailsModel>> confirmClearOtherEntryTypes =
                    new Predicate<Collection<ReplicateDetailsModel>>()
            {
                @Override
                public boolean test(Collection<ReplicateDetailsModel> list) {
                    if (list.isEmpty()) {
                        return true;
                    }
                    String msg = list.stream()
                        .map(rdm -> rdm.getPlantingBlock().getName())
                        .collect(Collectors.joining("  \n", "Some Replicates already assigned:\n", ""));

                    int answer = JOptionPane.showConfirmDialog(ReplicateDetailsPanel.this, msg, "Confirm", JOptionPane.YES_NO_OPTION);

                    return JOptionPane.YES_OPTION == answer;
                }
            };

            Either<Error, Collection<ReplicateDetailsModel>> either2 =
                    assigner.doAssignment(
                            entryRandomChoice,
                            confirmClearOtherEntryTypes);
            if (either2.isLeft()) {
                Error error = either2.left();
                String prefix = error.errorType.formatMessage(spatial.getName());
                String msg = error.models.stream()
                    .map(m -> m.getPlantingBlock().getName())
                    .collect(Collectors.joining("  \n", prefix + ":\n", ""));
                MsgBox.warn(ReplicateDetailsPanel.this, msg, "Can't Assign");
            }
            else {
                String msg = either2.right().stream()
                    .map(m -> m.getPlantingBlock().getName())
                    .collect(Collectors.joining("  \n", "Assignment Completed:\n", ""));
                messagePrinter.accept(msg);
            }
        }
    };

    private EntryRandomChoice entryRandomChoice = EntryRandomChoice.ONLY_THIS;

    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    private final SpatialEditControlsWidget spatialEditControlsWidget;
    private final Map<EditWhat, JRadioButton> rbByWhat = new HashMap<>();

    private final TrialEntryAssignmentDataProvider dataProvider;

    private final Consumer<ReplicateDetailsModel> onModelAssignmentDone;

    public ReplicateDetailsPanel(ReplicateDetailsModel model,
            FieldLayoutEditPanel<ReplicateCellContent> editPanel,
            Function<EntryType, Color> entryTypeColorSupplier,
            TrialEntryAssignmentDataProvider dataProvider,
            Consumer<ReplicateDetailsModel> onModelAssignmentDone,
            Consumer<String> messagePrinter)
    {
        super(new BorderLayout());

        this.replicateDetailsModel = model;
        this.dataProvider = dataProvider;
        this.onModelAssignmentDone = onModelAssignmentDone;
        this.messagePrinter = messagePrinter;

        visitOrderLabel.setForeground(Color.BLUE);
        visitOrderLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (2 == e.getClickCount()) {
                    CollectionPathSetupDialog dlg = new CollectionPathSetupDialog(
                            GuiUtil.getOwnerWindow(ReplicateDetailsPanel.this),
                            "Choose Order for Assignment",
                            false);
                    dlg.setOnlyAllow(PlotsPerGroup.ONE, VisitOrder2D.LL_UP_SERPENTINE);
//                    dlg.setOrOrTr(VisitOrder2D.LL_UP_SERPENTINE, PlotsPerGroup.ONE /* not used/displayed */);
                    dlg.setVisible(true);

                    if (dlg.visitOrder == null) {
                        return; // cancelled
                    }

                    visitOrder = dlg.visitOrder;
                    updateVisitOrderLabel();
                }
            }
        });
        updateVisitOrderLabel();

        spatialEditControlsWidget = new SpatialEditControlsWidget(replicateDetailsModel, editPanel, entryTypeColorSupplier);

        Color spatialsColor = spatialEditControlsWidget.getSpatialsColor();

        this.entryTypesTable = new JTable(replicateDetailsModel);
        entryTypesTable.setDefaultRenderer(Boolean.class, new OptionalCheckboxRenderer("<HTML>Edit using <i>Spatials</i> option"));
        entryTypesTable.setDefaultRenderer(Color.class, new ColorCellRenderer());
        entryTypesTable.setDefaultRenderer(EntryTypeCounter.class, new EntryTypeCounterRenderer());
        TableCellRenderer cr = entryTypesTable.getDefaultRenderer(Integer.class);
        if (cr instanceof JLabel) {
            ((JLabel) cr).setHorizontalAlignment(JLabel.CENTER);
        }

        GuiUtil.setVisibleRowCount(entryTypesTable, ReplicateDetailsModel.DEFAULT_VISIBLE_ROW_COUNT);

        ButtonGroup bg = new ButtonGroup();
        Box btns = Box.createVerticalBox();
        for (EditWhat w : EditWhat.values()) {
            JRadioButton rb = new JRadioButton(w.toString());
            rb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cardLayout.show(cardPanel, w.name());
                }
            });

            bg.add(rb);
            btns.add(rb);
            rbByWhat.put(w, rb);

            if (EditWhat.SPATIAL == w) {
                rb.setOpaque(true);
                rb.setBackground(spatialsColor.brighter());
            }
        }
        btns.add(Box.createVerticalGlue());

        if (replicateDetailsModel.getRowCount() <= 0) {
            // Don't know yet whether we have spatials so wait until we get something...
            replicateDetailsModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (replicateDetailsModel.getRowCount() > 0) {
                        replicateDetailsModel.removeTableModelListener(this);
                        doFirstClick();
                    }
                }
            });
        }
        else {
            doFirstClick();
        }

        replicateDetailsModel.addDetailsChangeListener(new DetailsChangeListener() {
            @Override
            public void chosenEntryTypeChanged(ReplicateDetailsModel source,
                    EntryType entryType,
                    boolean isSpatial)
            {
                if (isSpatial) {
                    String spatialName = StringUtil.htmlEscape(
                            replicateDetailsModel.getSpatialChecksName());
                    initHelpText(spatialName);
                }
                else {
                    initHelpText(null);
                }
            }
        });
        initHelpText(null);

        cardPanel.add(spatialEditControlsWidget.getWidgetComponent(), EditWhat.SPATIAL.name());
        cardPanel.add(createEntryTypesEditPanel(), EditWhat.NON_SPATIAL.name());

        String heading = String.format("<HTML>Designing <b>%s</b>",
                StringUtil.htmlEscape(replicateDetailsModel.getPlantingBlock().getName()));
        add(GuiUtil.createLabelSeparator(heading), BorderLayout.NORTH);
        add(btns, BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);
    }

    private void updateVisitOrderLabel() {
        visitOrderLabel.setText(visitOrder.displayName);
    }

    static private final String TO_ASSIGN = "To assign to Plots, choose the "
            + EditModeWidget.EditMode.REPLICATE_CONTENT.asBoldHtml()
            + " option on the left";
    private void initHelpText(String spatialNameHtml) {
        if (Check.isEmpty(spatialNameHtml)) {
            helpLabel.setText("<HTML>" + TO_ASSIGN);
        }
        else {
            helpLabel.setText(String.format(
                    "<HTML>Use <b>%s</b> button to randomly distribute them<BR>" + TO_ASSIGN,
                    spatialNameHtml));
        }
    }

    private final JLabel helpLabel = new JLabel();
    private VisitOrder2D visitOrder = VisitOrder2D.LL_UP_SERPENTINE;
    private final JLabel visitOrderLabel = new JLabel(visitOrder.displayName, JLabel.CENTER);

    private JPanel createEntryTypesEditPanel() {
        ButtonGroup bg = new ButtonGroup();
        Map<JRadioButton,EntryRandomChoice> choiceByRb = new HashMap<>();
        ActionListener rbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                entryRandomChoice = choiceByRb.get(e.getSource());
            }
        };

        JComponent choiceButtons = Box.createVerticalBox();
        choiceButtons.setBorder(new CompoundBorder(
                new EmptyBorder(2,2,2,2),
                new BevelBorder(BevelBorder.LOWERED)));
        for (EntryRandomChoice c : EntryRandomChoice.values()) {
            JRadioButton rb = new JRadioButton(c.displayValue, entryRandomChoice==c);
            rb.addActionListener(rbListener);
            bg.add(rb);
            choiceButtons.add(rb);

            choiceByRb.put(rb, c);
        }

        JPanel buttonsPanel = new JPanel();
        GBH gbh = new GBH(buttonsPanel, 1,1,1,1);
        int y = 0;

        gbh.add(0,y, 1,1, GBH.NONE, 1,1,   GBH.EAST, "Randomise:");
        gbh.add(1,y, 1,1, GBH.BOTH, 1,1.5, GBH.CENTER, choiceButtons);
        ++y;
        gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.SW, new JButton(assignEntriesAction));
        gbh.add(1,y, 1,1, GBH.NONE, 1,1, GBH.SW, visitOrderLabel);
        ++y;

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(entryTypesTable), BorderLayout.CENTER);
        center.add(helpLabel, BorderLayout.SOUTH);

        JPanel entryTypesPanel = new JPanel(new BorderLayout());
        entryTypesPanel.add(buttonsPanel, BorderLayout.EAST);
        entryTypesPanel.add(center, BorderLayout.CENTER);

        return entryTypesPanel;
    }

    private void doFirstClick() {
        Either<String, Optional<EntryType>> either = replicateDetailsModel.getSpatialEntryType();
        EditWhat clickWhat = EditWhat.NON_SPATIAL;
        if (either.isRight()) {
            Optional<EntryType> optional = either.right();
            if (optional.isPresent()) {
                clickWhat = EditWhat.SPATIAL;
            }
            else {
                rbByWhat.get(EditWhat.SPATIAL).setEnabled(false);
            }
        }
        rbByWhat.get(clickWhat).doClick();
    }

    public void updateEntryTypeCounts() {
        System.out.println(this.getClass().getSimpleName() + ".updateEntryTypeCounts");
        replicateDetailsModel.updateEntryTypeCounts();
    }

    public void setEntryTypeCounts(Map<EntryType, Integer> countByEntryType) {
        replicateDetailsModel.setEntryTypeCounts(true, countByEntryType);;
    }

    public ReplicateDetailsModel getModel() {
        return replicateDetailsModel;
    }

    public Set<WhatChanged> setDesignParams(DesignParams dp) {
        System.out.println(this.getClass().getSimpleName() + ".setDesignParams");

        Set<WhatChanged> result = replicateDetailsModel.setDesignParams(dp);
        updateEntryTypeCounts();
        return result;
    }
}
