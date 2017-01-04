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
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel.DetailsChangeListener;
import com.diversityarrays.kdxplore.fielddesign.FieldLayoutEditPanel;
import com.diversityarrays.kdxplore.fieldlayout.ReplicateDetailsPanel.SimpleContentFactory;
import com.diversityarrays.util.Either;

public class LocationEditPanel extends JPanel {

    static private final String CARD_NOT_SINGLE = "notSingle"; //$NON-NLS-1$
    static private final String CARD_SINGLE = "single"; //$NON-NLS-1$

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private final CardLayout repPanelsCardLayout = new CardLayout();
    private final JPanel repPanelsCardPanel = new JPanel(repPanelsCardLayout);

    private final Consumer<PlantingBlock<ReplicateCellContent>[]> onEntryTypesChanged = new Consumer<PlantingBlock<ReplicateCellContent>[]>() {
        @Override
        public void accept(PlantingBlock<ReplicateCellContent>[] blocks) {
            if (blocks != null) {
                for (PlantingBlock<?> b : blocks) {
                    ReplicateDetailsPanel panel = replicatePanelByBlock.get(b);
                    if (panel != null) {
                        panel.updateEntryTypeCounts();
                    }
                }
            }
        }
    };
    private final PlantingBlockTableModel<ReplicateCellContent> plantingBlockTableModel =
            PlantingBlockTableModel.create(onEntryTypesChanged);

    private Consumer<PlantingBlockSelectionEvent<ReplicateCellContent>> doSelectBlock =
            new Consumer<PlantingBlockSelectionEvent<ReplicateCellContent>>()
    {
        @Override
        public void accept(PlantingBlockSelectionEvent<ReplicateCellContent> t) {
            plantingBlockTablePanel.doSelectBlock(t);
        }
    };

    private final FieldLayoutEditPanel<ReplicateCellContent> fieldLayoutEditPanel;

    private final Map<PlantingBlock<ReplicateCellContent>, ReplicateDetailsPanel> replicatePanelByBlock = new HashMap<>();

    private final FieldSizer<ReplicateCellContent> fieldSizer = new FieldSizer<ReplicateCellContent>() {
//        @Override
//        public void setWidth(int w) {
//            fieldLayoutEditPanel.setWidth(w);
//        }
        @Override
        public void setSelectedBlocks(Collection<PlantingBlock<ReplicateCellContent>> blocks) {
            fieldLayoutEditPanel.setSelectedBlocks(blocks);
            updateEntryTypeCounts(blocks);
        }
//        @Override
//        public void setHeight(int h) {
//            fieldLayoutEditPanel.setHeight(h);
//        }
        @Override
        public void setDimension(int w, int h) {
            fieldLayoutEditPanel.setDimension(w, h);
        }
        @Override
        public Point getMaxFieldCoordinate() {
            return fieldLayoutEditPanel.getMaxFieldCoordinate();
        }
    };

    private final PlantingBlockTablePanel<ReplicateCellContent> plantingBlockTablePanel =
            new PlantingBlockTablePanel<>(plantingBlockTableModel, fieldSizer);

    private SiteLocation siteLocation;

    private Function<ReplicateCellContent, Color> rccColorSupplier =
            new Function<ReplicateCellContent, Color>()
    {
        @Override
        public Color apply(ReplicateCellContent t) {
            return entryTypeColorSupplier.apply(t==null ? null : t.entryType);
        }
    };
    private Function<EntryType, Color> entryTypeColorSupplier;
    private final Consumer<SiteLocation> onLocationNameChanged;
    private final Consumer<String> messagePrinter;

    public LocationEditPanel(
            Consumer<SiteLocation> onLocationNameChanged,
            SiteLocation location,
            Set<ReplicateDetailsModel> replicateModels,
            Map<EntryType, Integer> countByEntryType,
            Function<EntryType, Color> entryTypeColorSupplier,
            TrialEntryAssignmentDataProvider dataProvider,
            Consumer<String> messagePrinter)
    {
        super(new BorderLayout());

        this.onLocationNameChanged = onLocationNameChanged!=null ? onLocationNameChanged : (s) -> {};
        this.siteLocation = location;

        this.entryTypeColorSupplier = entryTypeColorSupplier;
        this.messagePrinter = messagePrinter;

        fieldLayoutEditPanel = new FieldLayoutEditPanel<>(
                new Dimension(siteLocation.widthInCells, siteLocation.heightInCells),
                plantingBlockTableModel,
                true, // showSizeControls
                false, // allowAutoSize
                doSelectBlock,
                rccColorSupplier);

        List<ReplicateDetailsModel> list = new ArrayList<>(replicateModels);
        Collections.sort(list, new Comparator<ReplicateDetailsModel>() {
            @Override
            public int compare(ReplicateDetailsModel o1, ReplicateDetailsModel o2) {
                return Integer.compare(o1.getReplicateNumber(), o2.getReplicateNumber());
            }
        });

        list.stream().forEach(m -> addReplicatePanel(m, dataProvider));

        cardPanel.add(new JLabel("Select a single Replicate to view its design progress"), CARD_NOT_SINGLE); //$NON-NLS-1$
        cardPanel.add(repPanelsCardPanel, CARD_SINGLE);

        JPanel rightPanel = cardPanel;

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(plantingBlockTablePanel, BorderLayout.CENTER);
        leftPanel.add(fieldLayoutEditPanel.getEditModeWidgetComponent(), BorderLayout.SOUTH);

        JSplitPane leftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel);
        leftRight.setResizeWeight(0.5);

        JPanel repsPanel = new JPanel(new BorderLayout());
//        repsPanel.add(GuiUtil.createLabelSeparator("Replicates for " + location.name),  //$NON-NLS-1$
//                BorderLayout.NORTH);
        repsPanel.add(leftRight, BorderLayout.CENTER);

//        fieldLayoutEditPanel.setBorder(new LineBorder(Color.RED));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                repsPanel,
                fieldLayoutEditPanel);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);

        replicatePanelByBlock.values().stream()
            .forEach(p -> p.setEntryTypeCounts(countByEntryType));
    }

    protected void updateEntryTypeCounts(Collection<PlantingBlock<ReplicateCellContent>> blocks) {

        blocks.stream().map(b -> replicatePanelByBlock.get(b))
            .filter(p -> p != null)
            .forEach(p -> p.updateEntryTypeCounts());

        Either<Integer, PlantingBlock<ReplicateCellContent>> either =
                fieldLayoutEditPanel.getSingleSelectedBlock();
        if (either.isLeft()) {
            cardLayout.show(cardPanel, CARD_NOT_SINGLE);
        }
        else {
            PlantingBlock<ReplicateCellContent> block = either.right();
            repPanelsCardLayout.show(repPanelsCardPanel, block.getName());
            replicatePanelByBlock.get(block);

            cardLayout.show(cardPanel, CARD_SINGLE);
        }
    }

    public SiteLocation getSiteLocation() {
        return siteLocation;
    }

    public void handleLocationChanged(SiteLocation locn) {
        SiteLocation oldLocation = siteLocation;
        siteLocation = locn;

        if (! siteLocation.name.equals(oldLocation.name)) {
            onLocationNameChanged.accept(siteLocation);
        }

        if (siteLocation.widthInCells != oldLocation.widthInCells || siteLocation.heightInCells != oldLocation.widthInCells) {
            fieldLayoutEditPanel.setFieldDimension(siteLocation.widthInCells, siteLocation.heightInCells);
        }
    }

    public void /*Set<WhatChanged>*/ setDesignParams(DesignParams dp) {
        fieldLayoutEditPanel.setFieldDimension(dp.width, dp.height);

        for (PlantingBlock<?> pb : plantingBlockTableModel.getPlantingBlocks()) {
            pb.setSpatialChecksCount(dp.nSpatials);
        }

        replicatePanelByBlock.values().stream()
            .forEach(p -> p.updateEntryTypeCounts());

        // FIXME check if we need to do this
//        Set<WhatChanged> changed = replicatePanelByBlock.values().stream()
//            .map(p -> p.setDesignParams(dp))
//            .flatMap(s -> s.stream())
//            .collect(Collectors.toSet());

//        return changed;
    }

    public Set<PlantingBlock<ReplicateCellContent>> getPlantingBlocks() {
        return replicatePanelByBlock.keySet();
    }

    public PlantingBlock<ReplicateCellContent> removeReplicate(int replicate) {
        PlantingBlock<ReplicateCellContent> pb = plantingBlockTableModel.removeByReplicate(replicate);
        if (pb != null) {
            ReplicateDetailsPanel panel = replicatePanelByBlock.remove(pb);
            if (panel != null) {
                ReplicateDetailsModel model = panel.getModel();
                model.removeDetailsChangeListener(detailsChangeListener);

                repPanelsCardPanel.remove(panel);
            }
        }
        return pb;
    }

    private final DetailsChangeListener detailsChangeListener = new DetailsChangeListener() {
        @Override
        public void chosenEntryTypeChanged(
                ReplicateDetailsModel source,
                EntryType entryType,
                boolean isSpatial)
        {
            SimpleContentFactory contentFactory = new SimpleContentFactory(entryType);
            fieldLayoutEditPanel.setContentFactory(contentFactory);

//            ReplicateDetailsPanel panel = replicatePanelByBlock.get(source.getPlantingBlock());
//            if (panel != null) {
//                panel.updateEntryTypesControls(entryType, isSpatial);
//            }
        }
    };

    private void addReplicatePanel(ReplicateDetailsModel model, TrialEntryAssignmentDataProvider dataProvider) {
        PlantingBlock<ReplicateCellContent> pb = model.getPlantingBlock();
        plantingBlockTableModel.addOne(pb);

        model.addDetailsChangeListener(detailsChangeListener);

        ReplicateDetailsPanel panel = new ReplicateDetailsPanel(model,
                fieldLayoutEditPanel,
                entryTypeColorSupplier,
                dataProvider,
                (rdm) -> fieldLayoutEditPanel.repaint(),
                messagePrinter);
        replicatePanelByBlock.put(pb, panel);
        repPanelsCardPanel.add(panel, pb.getName());
    }

}
