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
package com.diversityarrays.kdxplore.trials;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.collections15.Predicate;
import org.jdesktop.swingx.JXTreeTable;

import com.diversityarrays.kdsmart.db.BatchHandler;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithPlotAttributesOption;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithTraitOption;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.CreateItemException;
import com.diversityarrays.kdsmart.db.util.ItemConsumerHelper;
import com.diversityarrays.kdsmart.db.util.SampleOrder;
import com.diversityarrays.kdsmart.db.util.Util;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.data.util.DatabaseUtil;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ChoiceNode;
import com.diversityarrays.util.ChoiceTreeTableModel;
import com.diversityarrays.util.ChoiceTreeTableModel.ChoiceChangedListener;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.widget.PromptTextField;

@SuppressWarnings("nls")
public class AddScoringSetDialog extends JDialog {
    
    private static final String PLEASE_CHOOSE_ONE_OR_MORE_TRAITS = "Please choose one or more Traits";

    private static final String PLEASE_PROVIDE_A_DESCRIPTION = "Please provide a description";

    static private Comparator<Trait> TRAIT_COMPARATOR = new Comparator<Trait>() {
        @Override
        public int compare(Trait o1, Trait o2) {
            int diff = o1.getTraitLevel().compareTo(o2.getTraitLevel());
            if (diff == 0) {
                diff = o1.getTraitName().compareToIgnoreCase(o2.getTraitName());
            }
            return diff;
        }
    };

    private final Action useAllAction = new AbstractAction(Msg.ACTION_USE_ALL()) {
        @Override
        public void actionPerformed(ActionEvent e) {            
            /*List<ChoiceNode> changed = */traitInstanceChoiceTreeModel.setAllChosen(true);
            treeTable.repaint();
        }
    };
    
    private final Action useNoneAction = new AbstractAction(Msg.ACTION_USE_NONE()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            /*List<ChoiceNode> changed = */traitInstanceChoiceTreeModel.setAllChosen(false);
            treeTable.repaint();
        }
    };

    private final Action cancelAction = new AbstractAction(Msg.ACTION_CANCEL()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };
    
    public boolean addedSampleGroup = false;
    
    private final Action createAction = new AbstractAction(Msg.ACTION_CREATE()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            
            DeviceIdentifier scoringDevId = getScoringDeviceIdentifier();
            if (scoringDevId == null) {
                return;
            }

            Map<Integer, Integer> ssoByTraitId = new HashMap<>();
            Map<String, TraitInstance> tiByKey = new HashMap<>();
            if (! collectScoringSortOrderAndTraitInstanceByKey(ssoByTraitId, tiByKey)) {
                return;
            }
            
            List<TraitInstance> traitInstances = new ArrayList<>();

            SampleGroup sampleGroup = createSampleGroupAndCollectTraitInstances(
                    scoringDevId, ssoByTraitId, tiByKey, traitInstances);
            
            List<Plot> plots = collectPlots();

            BatchHandler<SampleGroup> batchHandler = new MyBatchHandler(sampleGroup, traitInstances, plots);

            Either<Exception, SampleGroup> either = kdxploreDatabase.doBatch(batchHandler);            
            if (either.isRight()) {
                addedSampleGroup = true;
                dispose();
            }
            else {
                MsgBox.error(AddScoringSetDialog.this, either.left(), Msg.ERRTITLE_ERROR(getTitle()));
            }
        }
        
        private boolean collectScoringSortOrderAndTraitInstanceByKey(
                Map<Integer, Integer> ssoByTraitId, 
                Map<String, TraitInstance> tiByKey) 
        { 
            try {
                Predicate<TraitInstance> visitor = new Predicate<TraitInstance>() {
                    @Override
                    public boolean evaluate(TraitInstance ti) {
                        tiByKey.put(makeTiKey(ti), ti);
                        if (! ssoByTraitId.containsKey(ti.getTraitId())) {
                            int scoringSortOrder = ti.getScoringSortOrder();
                            ssoByTraitId.put(ti.getTraitId(), scoringSortOrder);
                        }
                        return true;
                    }
                };
                kdxploreDatabase.getKDXploreKSmartDatabase().visitTraitInstancesForTrial(trial.getTrialId(), 
                        WithTraitOption.ONLY_NON_CALC_TRAITS,
                        visitor);
            }
            catch (IOException e1) {
                MsgBox.error(AddScoringSetDialog.this, e1.getMessage(), getTitle());
                return false;
            }
            return true;
        }

        private DeviceIdentifier getScoringDeviceIdentifier() {
            DeviceIdentifier scoringDevId = null;
            try {
                for (DeviceIdentifier devid : kdxploreDatabase.getDeviceIdentifiers()) {
                    if (DeviceType.FOR_SCORING.equals(devid.getDeviceType())) {
                        scoringDevId = devid;
                        break;
                    }
                }
                if (scoringDevId == null) {
                    MsgBox.error(AddScoringSetDialog.this, 
                            Msg.ERRMSG_MISSING_DEVICE_ID(DeviceType.FOR_SCORING.name()),
                            Msg.ERRTITLE_INTERNAL_ERROR(getTitle()));
                    return null;
                }
            }
            catch (IOException e1) {
                MsgBox.error(AddScoringSetDialog.this, e1.getMessage(), getTitle());
                return null;
            }
            
            return scoringDevId;
        }

        private SampleGroup createSampleGroupAndCollectTraitInstances(DeviceIdentifier scoringDevId,
                Map<Integer, Integer> ssoByTraitId, Map<String, TraitInstance> tiByKey,
                List<TraitInstance> traitInstances) 
        {
//            int maxSso = 0;
//            if (! ssoByTraitId.isEmpty()) {
//                List<Integer> ssos = new ArrayList<>(ssoByTraitId.values());
//                ssos.sort(Comparator.reverseOrder());
//                maxSso = ssos.get(0);
//            }
            
            SampleGroup sampleGroup = new SampleGroup();
            sampleGroup.setDateLoaded(new Date());
            sampleGroup.setDeviceIdentifierId(scoringDevId.getDeviceIdentifierId());
            sampleGroup.setOperatorName(descriptionField.getText().trim());
            sampleGroup.setTrialId(trial.getTrialId());

            // Now need to create the samples - but first - need all the TraitInstances
            java.util.function.Predicate<TraitInstance> visitor = new java.util.function.Predicate<TraitInstance>() {
                @Override
                public boolean test(TraitInstance ti) {
                    traitInstances.add(ti);
                    return true;
                }
            };
            traitInstanceChoiceTreeModel.visitChosenChildNodes(visitor);

            return sampleGroup;
        }

        private List<Plot> collectPlots() {
            List<Plot> plots = new ArrayList<>();
            
            TrialItemVisitor<Plot> plotVisitor = new TrialItemVisitor<Plot>() {
                @Override
                public void setExpectedItemCount(int count) {
                    // ignore
                }
                
                @Override
                public boolean consumeItem(Plot plot) throws IOException {
                    plots.add(plot);
                    return true;
                }
            };
            kdxploreDatabase.getKDXploreKSmartDatabase().visitPlotsForTrial(trial.getTrialId(), 
                    SampleGroupChoice.NO_TAGS_SAMPLE_GROUP, 
                    WithPlotAttributesOption.WITHOUT_PLOT_ATTRIBUTES, 
                    plotVisitor);
            return plots;
        }
    };
    
    
    
//    static private String makeTiKey(Trait trait, int instanceNumber) {
//        return trait.getTraitId() + "/" + instanceNumber; //$NON-NLS-1$
//    }

    static private String makeTiKey(TraitInstance ti) {
        return ti.getTraitId() + "/" + ti.getInstanceNumber(); //$NON-NLS-1$
    }

    private final KdxploreDatabase kdxploreDatabase;

    private final Trial trial;

    protected boolean wantSampleValues = false;
    
    private JRadioButton noSampleValuesButton = null;

    private final JLabel warningMsg = new JLabel();
    private PromptTextField descriptionField = new PromptTextField(Msg.PROMPT_DESC_FOR_SCORING_SET());
    
    private final int curatedSampleGroupId;

    private final JXTreeTable treeTable;
    
    private final Function<TraitInstance, String> childNameProvider = new Function<TraitInstance, String>() {
        @Override
        public String apply(TraitInstance ti) {
            return trial.getTraitNameStyle().makeTraitInstanceName(ti);
        }
    };

//    private final IntFunction<String> chosenCountNameProvider = new IntFunction<String>() {
//        @Override
//        public String apply(int value) {
//            if (value > 0) {
//                return " (" + value + " chosen)";
//            }
//            return null;
//        }
//    };

    public AddScoringSetDialog(Window owner, 
            KdxploreDatabase kdxdb, 
            Trial trial,
            Map<Trait, List<TraitInstance>> instancesByTrait,
            SampleGroup curatedSampleGroup) 
    {
        super(owner, Msg.TITLE_ADD_SCORING_SET(), ModalityType.APPLICATION_MODAL);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        this.kdxploreDatabase = kdxdb;
        this.trial = trial;
        this.curatedSampleGroupId = curatedSampleGroup==null ? 0 : curatedSampleGroup.getSampleGroupId();

        Map<Trait, List<TraitInstance>> noCalcs = instancesByTrait.entrySet().stream()
                .filter(e -> TraitDataType.CALC != e.getKey().getTraitDataType())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<Trait, List<TraitInstance>> noCalcsSorted = new TreeMap<>(TRAIT_COMPARATOR);
        noCalcsSorted.putAll(noCalcs);

        BiFunction<Trait, TraitInstance, String> parentNameProvider = new BiFunction<Trait, TraitInstance, String>() {
            @Override
            public String apply(Trait t, TraitInstance ti) {
                if (ti == null) {
                    List<TraitInstance> list = noCalcsSorted.get(t);
                    if (list == null || list.size() != 1) {
                        OptionalInt opt = traitInstanceChoiceTreeModel.getChildChosenCountIfNotAllChosen(t);
                        StringBuilder sb = new StringBuilder(t.getTraitName());
                        
                        if (opt.isPresent()) {
                            // only some of the children are chosen
                            int childChosenCount = opt.getAsInt();
                            if (childChosenCount > 0) {
                                sb.append(" (")
                                .append(childChosenCount)
                                .append(" of ")
                                .append(list.size())
                                .append(")");
                            }
                        }
                        else {
                            // all of the children are chosen
                            if (list != null) {
                                sb.append(" (").append(list.size()).append(")");
                            }
                        }
                        return sb.toString();
                    }
                }
                return t.getTraitName();
            }
        };

        Optional<List<TraitInstance>> opt = noCalcsSorted.values().stream()
                .filter(list -> list.size() > 1).findFirst();
        String heading1 = opt.isPresent() ? "Trait/Instance" : "Trait";

        traitInstanceChoiceTreeModel = new ChoiceTreeTableModel<Trait, TraitInstance>(
                heading1, "Use?", //$NON-NLS-1$
                noCalcsSorted, 
                parentNameProvider, 
                childNameProvider
                );
        
//        traitInstanceChoiceTreeModel = new TTChoiceTreeTableModel(instancesByTrait);

        traitInstanceChoiceTreeModel.addChoiceChangedListener(new ChoiceChangedListener() {
            @Override
            public void choiceChanged(Object source, ChoiceNode[] changedNodes) {
                updateCreateAction("choiceChanged");
                treeTable.repaint();
            }
        });
        
        traitInstanceChoiceTreeModel.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeStructureChanged(TreeModelEvent e) {
            }
            
            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
            }
            
            @Override
            public void treeNodesInserted(TreeModelEvent e) {
            }
            
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                updateCreateAction("treeNodesChanged");
            }
        });

        warningMsg.setText(PLEASE_PROVIDE_A_DESCRIPTION);
        warningMsg.setForeground(Color.RED);

        Container cp = getContentPane();

        Box sampleButtons = null;
        if (curatedSampleGroup != null && curatedSampleGroup.getAnyScoredSamples()) {
            sampleButtons = createWantSampleButtons(curatedSampleGroup);
        }

        Box top = Box.createVerticalBox();
        if (sampleButtons == null) {
            top.add(new JLabel(Msg.MSG_THERE_ARE_NO_CURATED_SAMPLES()));
        }
        else {
            top.add(sampleButtons);
        }
        top.add(descriptionField);

        cp.add(top, BorderLayout.NORTH);

        descriptionField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCreateAction("documentListener");
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCreateAction("documentListener");
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCreateAction("documentListener");
            }
        });

        updateCreateAction("init");
//        KDClientUtils.initAction(ImageId.`CHECK_ALL, useAllAction, "Click to Use All");
        
        treeTable = new JXTreeTable(traitInstanceChoiceTreeModel);
        treeTable.setAutoResizeMode(JXTreeTable.AUTO_RESIZE_ALL_COLUMNS);

        TableCellRenderer renderer = treeTable.getDefaultRenderer(Integer.class);
        if (renderer instanceof JLabel) {
            ((JLabel) renderer).setHorizontalAlignment(JLabel.CENTER);
        }
        
        Box buttons = Box.createHorizontalBox();
        
        buttons.add(new JButton(useAllAction));
        buttons.add(new JButton(useNoneAction));

        buttons.add(Box.createHorizontalGlue());
        buttons.add(warningMsg);

        buttons.add(new JButton(cancelAction));
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(new JButton(createAction));

        cp.add(new JScrollPane(treeTable), BorderLayout.CENTER);
        cp.add(buttons, BorderLayout.SOUTH);

        pack();
    }
    
    private Box createWantSampleButtons(SampleGroup curatedSampleGroup) {
        Box result = null;
        ActionListener rbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wantSampleValues = noSampleValuesButton != e.getSource();
            }
        };
        
        result = Box.createHorizontalBox();
        String noSampleValues = Msg.OPTION_NO_SAMPLE_VALUES();
        ButtonGroup bg = new ButtonGroup();
        for (String rbname : new String[] { 
                noSampleValues,
                Msg.OPTION_CURATED_SAMPLE_VALUES()})
        {
            JRadioButton rb = new JRadioButton(rbname);
            result.add(rb);
            bg.add(rb);
            rb.addActionListener(rbListener);
            if (noSampleValues.equals(rbname)) {
                noSampleValuesButton = rb;
            }
            else {
                rb.doClick();
            }
        }
        result.add(Box.createHorizontalGlue());
        
        return result;
    }

    private void updateCreateAction(String from) {
        System.out.println("updateCreateAction(" + from + ")");
        String msg = null;
        String desc = descriptionField.getText().trim();
        if (Check.isEmpty(desc)) {
            msg = PLEASE_PROVIDE_A_DESCRIPTION;
        }
        else {
            if (traitInstanceChoiceTreeModel.getAnyChosen()) {
                msg = PLEASE_CHOOSE_ONE_OR_MORE_TRAITS;
            }
        }
        warningMsg.setText(msg==null ? "" : msg);
        createAction.setEnabled(Check.isEmpty(msg));
    }

    private final ChoiceTreeTableModel<Trait, TraitInstance> traitInstanceChoiceTreeModel;

    class MyBatchHandler implements BatchHandler<SampleGroup> {
        
        private final List<TraitInstance> traitInstances;
        private final SampleGroup sampleGroup;
        private final List<Plot> plots;

        MyBatchHandler(SampleGroup sampleGroup, List<TraitInstance> traitInstances, List<Plot> plots) {
            this.sampleGroup = sampleGroup;
            this.traitInstances = traitInstances;
            this.plots = plots;
        }
        
        @Override
        public SampleGroup call() throws Exception {

            ItemConsumerHelper ich = kdxploreDatabase.getKDXploreKSmartDatabase().getItemConsumerHelper();
            
            Map<TraitLevel, List<TraitInstance>> listByLevel = traitInstances.stream()
                    .collect(Collectors.groupingBy(ti -> ti.trait.getTraitLevel()));

            List<TraitInstance> plotTraitInstances = listByLevel.get(TraitLevel.PLOT);
            List<TraitInstance> subPlotTraitInstances = listByLevel.get(TraitLevel.SPECIMEN);
            
            createNewInstances(ich, plotTraitInstances);
            createNewInstances(ich, subPlotTraitInstances);

            kdxploreDatabase.saveSampleGroup(sampleGroup);
            
            int trialId = trial.getTrialId();
            
            Map<String, Sample> sampleByKey = new HashMap<>();
            if (wantSampleValues) {
                TrialItemVisitor<Sample> curatedSamplesVisitor = new TrialItemVisitor<Sample>() {
                    @Override
                    public void setExpectedItemCount(int count) { }
                    
                    @Override
                    public boolean consumeItem(Sample s) throws IOException {
                        String key = Util.createUniqueSampleKey(s);
                        sampleByKey.put(key, s);
                        return true;
                    }
                };
                kdxploreDatabase.getKDXploreKSmartDatabase()
                    .visitSamplesForTrial(
                            SampleGroupChoice.create(curatedSampleGroupId),
                            trialId, 
                            SampleOrder.ALL_UNORDERED, 
                            curatedSamplesVisitor);
            }

            Map<String, Sample> previousSampleByKey = wantSampleValues ? sampleByKey : null;
            Consumer<KdxSample> sampleConsumer = new Consumer<KdxSample>() {
                @Override
                public void accept(KdxSample sample) {
                    sampleGroup.addSample(sample);
                }                
            };
            for (Plot plot : plots) {
                DatabaseUtil.createSamples(trialId, plot, 
                        sampleGroup.getSampleGroupId(),
                        previousSampleByKey, 
                        plotTraitInstances, subPlotTraitInstances, 
                        sampleConsumer);
//                for (TraitInstance ti : plotTraitInstances) {
//                    KdxSample sample = new KdxSample();
//                    sample.setSampleGroupId(sampleGroup.getSampleGroupId());
//                    
//                    // These 5 are the composite-key
//                    sample.setTrialId(trial.getTrialId());
//                    sample.setPlotId(plot.getPlotId());
//                    sample.setSpecimenNumber(PlotOrSpecimen.ORGANISM_NUMBER_IS_PLOT);
//                    sample.setTraitId(ti.getTraitId());
//                    sample.setTraitInstanceNumber(ti.getInstanceNumber());
//                    
//                    
//                    if (wantSampleValues) {
//                        String sampleKey = Util.createUniqueSampleKey(sample);
//                        Sample previous = sampleByKey.get(sampleKey);
//                        if (previous != null) {
//                            sample.setMeasureDateTime(previous.getMeasureDateTime());
//                            sample.setTraitValue(previous.getTraitValue());
//                        }
//                    }
//                    
//                    sampleGroup.addSample(sample);
//                }
//
//                if (! subPlotTraitInstances.isEmpty()) {
//                    for (Specimen specimen : plot.getSpecimens()) {
//                        for (TraitInstance ti : subPlotTraitInstances) {
//                            KdxSample sample = new KdxSample();
//                            sample.setSampleGroupId(sampleGroup.getSampleGroupId());
//
//                            // These 5 are the composite-key
//                            sample.setTrialId(trial.getTrialId());
//                            sample.setPlotId(plot.getPlotId());
//                            sample.setSpecimenNumber(specimen.getSpecimenNumber());
//                            sample.setTraitId(ti.getTraitId());
//                            sample.setTraitInstanceNumber(ti.getInstanceNumber());
//
//                            if (wantSampleValues) {
//                                String key = Util.createUniqueSampleKey(trialId, plot, ti);
//                                Sample previous = sampleByKey.get(key);
//                                if (previous != null) {
//                                    sample.setMeasureDateTime(previous.getMeasureDateTime());
//                                    sample.setTraitValue(previous.getTraitValue());
//                                }
//                            }
//
//                            sampleGroup.addSample(sample);
//                        }
//                    }
//                }
            }

            KDSmartDatabase kdsdb = kdxploreDatabase.getKDXploreKSmartDatabase();

            BatchHandler<Void> batchHandler = new BatchHandler<Void>() {
                @Override
                public Void call() throws Exception {
                    kdsdb.saveMultipleSamples(sampleGroup.getSamples(), false);
                    return null;
                }

                @Override
                public boolean checkSuccess(Void t) {
                    return true;
                }
            };

            Either<Exception, Void> either = kdsdb.doBatch(batchHandler);

            if (either.isLeft()) {
                throw either.left();
            }
            return sampleGroup;
        }

        private void createNewInstances(
                ItemConsumerHelper ich, 
                List<TraitInstance> traitInstances)
        throws CreateItemException
        {
            for (TraitInstance ti : traitInstances) {
                if (ti.getTraitInstanceId() <= 0) {
                    ich.createNewItemInDatabase(TraitInstance.class, ti);
                }
            }
        }

        @Override
        public boolean checkSuccess(SampleGroup sg) {
            return sg.getSampleGroupId() > 0;
        }  
    }
}
