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
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetrieverFactory;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.TableColumnInfo;
import com.diversityarrays.util.TableColumnInfoTableModel;

import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.DesktopObject;

public class SampleGroupViewer extends JPanel implements DesktopObject { // extends JFrame {

    static String makeTiKey(int traitId, int instance) {
        return traitId + "/" + instance;
    }


    static private String makeSampleKey(Sample s) {
        return makeSampleKey(s.getPlotId(), s.getTraitId(), s.getTraitInstanceNumber(), s.getSpecimenNumber());
    }

    static private String makeSampleKey(int plotid, int traitid, int instance, int specimen) {
        return plotid + ":" + traitid + "/" + instance + "#" + specimen;
    }

    private class PlotInfo {
        public final Plot plot;
        private Map<TraitInstance, KdxSample> sampleByTi = new HashMap<>();
        private List<? extends KdxSample> samples = new ArrayList<>();
//        private
        PlotInfo(Plot plot) {
            this.plot = plot;
        }
        public void addSample(KdxSample s) {
            String key = makeTiKey(s.getTraitId(),  s.getTraitInstanceNumber());
            TraitInstance ti = tiByKey.get(key);
            sampleByTi.put(ti, s);
        }

    }

    private final Map<Integer, PlotInfo> plotInfoByPlotId = new HashMap<>();
    private Map<String,TraitInstance> tiByKey = new HashMap<>();

    private final KdxploreDatabase kdxdb;
    private Trial trial;
    private SampleGroup sampleGroup;

    Map<Integer, Trait> traitById;
    private DataTableModel tableModel;
    private final String title;

    private TraitInstanceChoiceTableModel tiChoiceTableModel;
    private JSplitPane splitPane;

    static public SampleGroupViewer create(String title, KdxploreDatabase kdxdb, Trial trial, SampleGroup sampleGroup) {
        return new SampleGroupViewer(title, kdxdb, trial, sampleGroup);
    }

    private SampleGroupViewer(String title, KdxploreDatabase kdxdb, Trial trial, SampleGroup sampleGroup) {
        super(new BorderLayout());

        this.title = title;
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.kdxdb = kdxdb;
        this.trial = trial;
        this.sampleGroup = sampleGroup;

        initialise();
        if (plotInfoByPlotId.isEmpty()) {
            add(new JLabel("No Plots available"), BorderLayout.CENTER);
        }
        else {
            tiChoiceTableModel = new TraitInstanceChoiceTableModel();
            JTable tiTable = new JTable(tiChoiceTableModel);
            tiTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        List<Integer> modelRows = GuiUtil.getSelectedModelRows(tiTable);
                        if (modelRows.isEmpty()) {
                            Point pt = e.getPoint();
                            int vrow = tiTable.rowAtPoint(pt);
                            if (vrow >= 0) {
                                int mrow = tiTable.convertRowIndexToModel(vrow);
                                if (mrow >= 0) {
                                    showPopupMenu(
                                            tiTable, pt, Arrays.asList(Integer.valueOf(mrow)));
                                }
                            }
                        }
                        else {
                            showPopupMenu(tiTable, e.getPoint(), modelRows);
                        }
                    }
                }
            });

            tableModel = new DataTableModel(plotInfoByPlotId);
            JTable table = new JTable(tableModel);
            DefaultTableCellRenderer r = new DefaultTableCellRenderer();
            r.setHorizontalAlignment(SwingConstants.CENTER);
            table.setDefaultRenderer(String.class, r);

            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(tiTable),
                    new JScrollPane(table));
            splitPane.setResizeWeight(0.2);
            add(splitPane, BorderLayout.CENTER);
            table.setTransferHandler(TableTransferHandler.initialiseForCopySelectAll(table, true));
        }
    }

    private List<Integer> selectedModelRows;
    private JPopupMenu popupMenu;

    private void showPopupMenu(Component c, Point pt, List<Integer> rowIndices) {
        selectedModelRows = rowIndices;
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            popupMenu.add(new AbstractAction("View") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tiChoiceTableModel.changeChosen(selectedModelRows, true);
                }
            });
            popupMenu.add(new AbstractAction("Hide") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tiChoiceTableModel.changeChosen(selectedModelRows, false);
                }
            });
        }
        popupMenu.show(c, pt.x, pt.y);
    }

    public void initialise() {

        plotInfoByPlotId.clear();
        try {

            KDSmartDatabase.WithPlotAttributesOption wpa = KDSmartDatabase.WithPlotAttributesOption.WITHOUT_PLOT_ATTRIBUTES;

            Map<Integer,Plot> plotById = kdxdb.getPlots(trial, SampleGroupChoice.create(sampleGroup.getSampleGroupId()), wpa)
                    .stream()
                .collect(Collectors.toMap(Plot::getPlotId, java.util.function.Function.identity()));

            traitById = kdxdb.getKDXploreKSmartDatabase().getTraitMap();

            KDSmartDatabase.WithTraitOption wto = KDSmartDatabase.WithTraitOption.ALL_WITH_TRAITS;
            Predicate<TraitInstance> tiVisitor = new Predicate<TraitInstance>() {
                @Override
                public boolean evaluate(TraitInstance ti) {
                    String key = makeTiKey(ti.getTraitId(), ti.getInstanceNumber());
                    tiByKey.put(key, ti);
                    return true;
                }
            };
            kdxdb.getKDXploreKSmartDatabase().visitTraitInstancesForTrial(trial.getTrialId(), wto, tiVisitor);

            Set<Integer> traitIdsSeen = new HashSet<>();
            //            sampleGroup.getTrialId();
            java.util.function.Predicate<KdxSample> visitor = new java.util.function.Predicate<KdxSample>() {
                @Override
                public boolean test(KdxSample s) {
                    Plot plot = plotById.get(s.getPlotId());
                    if (plot == null) {
                        System.err.println("Missing Plot#" + s.getPlotId());
                    }
                    else {
                        PlotInfo pinfo = plotInfoByPlotId.get(plot.getPlotId());
                        if (pinfo == null) {
                            pinfo = new PlotInfo(plot);
                            plotInfoByPlotId.put(plot.getPlotId(), pinfo);
                        }
                        Integer traitId = s.getTraitId();
                        traitIdsSeen.add(traitId);

                        pinfo.addSample(s);
                    }
                    return true;
                }
            };
            boolean scored = false;
            kdxdb.visitKdxSamplesForSampleGroup(
                    sampleGroup, KdxploreDatabase.SampleLevel.BOTH, scored, visitor);
        }
        catch (IOException e) {
            MsgBox.error(SampleGroupViewer.this, e, "Database Error");
            return;
        }
    }

    static private final Comparator<TraitInstance> TI_COMPARATOR = new Comparator<TraitInstance>() {
        @Override
        public int compare(TraitInstance t1, TraitInstance t2) {
            int diff = t1.trait.getTraitName().compareTo(t2.trait.getTraitName());
            if (diff == 0) {
                diff = Integer.compare(t1.getInstanceNumber(), t2.getInstanceNumber());
            }
            return diff;
        }
    };

    private String getBriefSummary(SampleGroup sampleGroup) throws IOException {
        Bag<Integer> plotIdsWithSpecimens = new HashBag<>();
        Set<Integer> plotIdsWithScores = new HashSet<>();
        int[] results = new int[3];
        java.util.function.Predicate<KdxSample> visitor = new java.util.function.Predicate<KdxSample>() {
            @Override
            public boolean test(KdxSample s) {
                plotIdsWithScores.add(s.getPlotId());
                int snum = s.getSpecimenNumber();
                if (snum <= 0) {
                    ++results[0]; // Plot level sample count
                }
                else {
                    ++results[1]; // Individual level sample count
                    plotIdsWithSpecimens.add(s.getPlotId());
                    results[2] = Math.max(results[2], snum); // maximum specimen number
                }
                return true;
            }
        };

        boolean scored = true;
        kdxdb.visitKdxSamplesForSampleGroup(
                sampleGroup, KdxploreDatabase.SampleLevel.BOTH, scored, visitor);

        int nPlotSamples = results[0];
        int nSpecimenSamples = results[1];
        int totalScoredSamples = nPlotSamples + nSpecimenSamples;

        int maxSpecimenNumber = results[2];

        int nPlotsWithSpecimens = plotIdsWithSpecimens.size();
        int nPlotsWithScores = plotIdsWithScores.size();

        int maxCount = 0;
        for (Integer plotId : plotIdsWithSpecimens.uniqueSet()) {
            int count = plotIdsWithSpecimens.getCount(plotId);
            maxCount = Math.max(maxCount, count);
        }

        StringBuilder sb = new StringBuilder("<HTML>");

        sb.append("<br><B>Scored Samples:</b> ").append(totalScoredSamples);
        sb.append("<br><B>Plot Level Samples:</b> ").append(nPlotSamples);
        sb.append("<br><B>Individual Samples:</b> ").append(nSpecimenSamples);
        sb.append("<br>");
        sb.append("<br><B>Max Individual number:</b> ").append(maxSpecimenNumber);
        sb.append("<br><B># Plots with Individuals:</b> ").append(nPlotsWithSpecimens);
        sb.append("<br>");
        sb.append("<br><B># Plots with scored samples:</b> ").append(nPlotsWithScores);
        sb.append("<br><B>Max # individual scores per plot:</b> ").append(maxCount);

        return sb.toString();
    }

    // DesktopObject
    @Override
    public JPanel getJPanel() {
        return this;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public JMenuBar getJMenuBar() {
        return null;
    }

    @Override
    public JToolBar getJToolBar() {
        return null;
    }

    @Override
    public void doPostOpenActions() {
        Window w = GuiUtil.getOwnerWindow(this);
        if (w != null) {
            GuiUtil.centreOnScreen(w);
            if (tableModel == null) {
                w.setSize(600, 400);
            }
            if (splitPane != null) {
                splitPane.setDividerLocation(0.2);
            }
        }
    }

    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    public boolean isClosable() {
        return true;
    }

    @Override
    public Object getWindowIdentifier() {
        return title;
    }

    private class DataTableModel extends AbstractTableModel {

        private Map<String, KdxSample> sampleByKey = new HashMap<>();

        private PlotInfoProvider infoProvider = new PlotInfoProvider() {
            @Override
            public Trial getTrial() {
                return trial;
            }

            @Override
            public List<TrialAttribute> getTrialAttributes() {
                return Collections.emptyList();
            }

            @Override
            public List<Plot> getPlots() {
                return plots;
            }

            @Override
            public Set<Plot> getPlotsForPlotSpecimens(Collection<PlotOrSpecimen> plotSpecimens) {
                Set<Plot> set = plotSpecimens.stream()
                    .map(pos -> plotInfoByPlotId.get(pos.getPlotId()))
                    .filter(pi -> pi != null)
                    .map(pi -> pi.plot)
                    .collect(Collectors.toSet());
                return set;
            }

            @Override
            public Sample getSampleForTraitInstance(PlotOrSpecimen pos, TraitInstance ti) {
                PlotInfo pi = plotInfoByPlotId.get(pos.getPlotId());
                for (KdxSample s : pi.samples) {
                    if (s.getTraitId() == ti.getTraitId()) {
                        if (s.getTraitInstanceNumber() == ti.getInstanceNumber()) {
                            return s;
                        }
                    }
                }
                return null;
            }

            @Override
            public List<KdxSample> getSampleMeasurements(TraitInstance ti) {
                List<KdxSample> list = new ArrayList<>();
                for (PlotInfo pi : plotInfoByPlotId.values()) {
                    KdxSample s = pi.sampleByTi.get(ti);
                    if (s != null) {
                        list.add(s);
                    }
                }
                return list;
            }

            @Override
            public Plot getPlotByPlotId(int plotId) {
                return plotInfoByPlotId.get(plotId).plot;
            }

            @Override
            public Map<String, String> getPlotAttributeValues(int plotId) {
                return Collections.emptyMap();
            }

            @Override
            public String getPlotAttributeValue(int plotId, String attributeName) {
                return null;
            }

            @Override
            public Iterator<String> getPlotAttributeValuesIterator(String attributeName) {
                return Collections.emptyIterator();
            }

            @Override
            public void visitSamplesForPlotOrSpecimen(PlotOrSpecimen pos, Consumer<KdxSample> visitor) {
                for (KdxSample s : plotInfoByPlotId.get(pos.getPlotId()).samples) {
                    visitor.accept(s);
                }
            }

            @Override
            public Iterable<? extends KdxSample> getSamplesForCurationCellId(CurationCellId ccid) {
                return plotInfoByPlotId.get(ccid.plotId).samples;
            }


            @Override
            public TraitInstance getTraitInstanceForSample(Sample s) {
                return tiByKey.get(makeTiKey(s.getTraitId(), s.getTraitInstanceNumber()));
            }

            @Override
            public List<TraitInstance> getTraitInstances() {
                return allTraitInstances;
            }

            @Override
            public void changePlotsActivation(boolean activate, List<Plot> plots) {
                // TODO Auto-generated method stub

            }

            @Override
            public void addCurationDataChangeListener(CurationDataChangeListener l) {
                // TODO Auto-generated method stub

            }

            @Override
            public void removeCurationDataChangeListener(CurationDataChangeListener l) {
                // TODO Auto-generated method stub

            }

        };

        List<Plot> plots = new ArrayList<>();
        List<PlotInfo> plotInfoList = new ArrayList<>();

        List<ValueRetriever<?>> plotPositionRetrievers;

        List<TraitInstance> allTraitInstances;
        List<TraitInstance> visibleTraitInstances;

        DataTableModel(Map<Integer, PlotInfo> plotInfoByPlotId) {

            plotPositionRetrievers = ValueRetrieverFactory.getPlotIdentValueRetrievers(trial);

            plotInfoList = new ArrayList<>(plotInfoByPlotId.values());
            Comparator<PlotInfo> comp = new Comparator<PlotInfo>() {

                @Override
                public int compare(PlotInfo o1, PlotInfo o2) {
                    for (ValueRetriever<?> vr : plotPositionRetrievers) {
                        Comparable a1 = vr.getAttributeValue(infoProvider, o1.plot, null);
                        Comparable a2 = vr.getAttributeValue(infoProvider, o2.plot, null);

                        int diff = 0;
                        if (a1 == null) {
                            if (a2 != null) diff = -1;
                        }
                        else if (a2 == null) {
                            diff = 1;
                        }
                        else {
                            diff = a1.compareTo(a2);
                        }

                        if (diff != 0) {
                            return diff;
                        }
                    }
                    return 0;
                }

            };
            Collections.sort(plotInfoList, comp);
            for (PlotInfo pi : plotInfoList) {
                plots.add(pi.plot);
                for (KdxSample s : pi.samples) {
                    String key = makeSampleKey(s);
                    sampleByKey.put(key, s);
                }
            }

            allTraitInstances = new ArrayList<>(tiByKey.values());
            Collections.sort(allTraitInstances, TI_COMPARATOR);

            visibleTraitInstances = new ArrayList<>(allTraitInstances);

            Map<Integer, Set<Integer>> tiNumsByTraitId = new HashMap<>();
            for (TraitInstance ti : tiByKey.values()) {
                Set<Integer> set = tiNumsByTraitId.get(ti.getTraitId());
                if (set == null) {
                    set = new HashSet<>();
                    tiNumsByTraitId.put(ti.getTraitId(),  set);
                }
                set.add(ti.getInstanceNumber());
            }
        }

        public void updateVisible(Set<TraitInstance> chosen) {
            List<TraitInstance> list = allTraitInstances.stream()
                        .filter(ti -> chosen.contains(ti))
                        .collect(Collectors.toList());
            Collections.sort(list, TI_COMPARATOR);
            if (! list.equals(visibleTraitInstances)) {
                visibleTraitInstances = list;
                fireTableStructureChanged();
            }
        }

        @Override
        public String getColumnName(int column) {
            if (column < plotPositionRetrievers.size()) {
                return plotPositionRetrievers.get(column).getDisplayName();
            }
            int index = column - plotPositionRetrievers.size();
            if (index == 0) {
            	return "Plot Tags";
            }
            --index;
            TraitInstance ti = visibleTraitInstances.get(index);
            return trial.getTraitNameStyle().makeTraitInstanceName(ti, TraitNameStyle.Prefix.INCLUDE);
        }
        @Override
        public int getRowCount() {
            return plotInfoList.size();
        }

        @Override
        public int getColumnCount() {
            return plotPositionRetrievers.size() + 1 + visibleTraitInstances.size();
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col < plotPositionRetrievers.size()) {
                return plotPositionRetrievers.get(col).getValueClass();
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PlotInfo pi = plotInfoList.get(rowIndex);
            if (columnIndex < plotPositionRetrievers.size()) {
                ValueRetriever<?> ppr = plotPositionRetrievers.get(columnIndex);
                return ppr.getAttributeValue(infoProvider, pi.plot, null);
            }

            int index = columnIndex - plotPositionRetrievers.size();

            if (index == 0) {
            	// This is the Plot Tag column
            	return pi.plot.getTagLabels().stream().collect(Collectors.joining("|"));
            }

            --index;

            TraitInstance ti = visibleTraitInstances.get(index);
            KdxSample kdxSample = pi.sampleByTi.get(ti);
            return kdxSample==null ? null : kdxSample.getTraitValue();
        }

    }

    private class TraitInstanceChoiceTableModel extends TableColumnInfoTableModel<String> {
        private final Set<Integer> chosen = new HashSet<>();
        private final Map<String, TraitInstance> tiByName = new HashMap<>();

        TraitInstanceChoiceTableModel() {
            TraitNameStyle tns = trial.getTraitNameStyle();
            for (TraitInstance ti : tiByKey.values()) {
                tiByName.put(tns.makeTraitInstanceName(ti), ti);
            }

            List<TableColumnInfo<String>> list = new ArrayList<>();
            list.add(new TableColumnInfo<String>("Show?", Boolean.class) {
                @Override
                public Object getColumnValue(int rowIndex, String t) {
                    return chosen.contains(rowIndex);
                }
            });
            list.add(new TableColumnInfo<String>("Trait Instance", String.class) {
                @Override
                public Object getColumnValue(int rowIndex, String t) {
                    return t;
                }
            });
            setTableColumnInfo(list);

            List<String> nameList = new ArrayList<>(tiByName.keySet());
            Collections.sort(nameList);
            setItems(nameList);
            for (int rowIndex = getRowCount(); --rowIndex >= 0; ) {
                chosen.add(rowIndex);
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col==0;
        }

        @Override
        public void setValueAt(Object aValue, int row, int col) {
            if (col==0 && aValue instanceof Boolean) {
                if ((Boolean) aValue) {
                    if (chosen.add(row)) {
                        fireTableRowsUpdated(row, row);
                        tableModel.updateVisible(getChosenTraitInstances());
                    }
                }
                else {
                    if (chosen.remove(row)) {
                        fireTableRowsUpdated(row, row);
                        tableModel.updateVisible(getChosenTraitInstances());
                    }
                }
            }
        }

        public void changeChosen(List<Integer> rows, boolean choose) {
            if (rows.isEmpty()) {
                return;
            }
            boolean anyChanged = false;
            if (choose) {
                for (Integer row : rows) {
                    if (chosen.add(row)) {
                        anyChanged = true;
                    }
                }
            }
            else {
                for (Integer row : rows) {
                    if (chosen.remove(row)) {
                        anyChanged = true;
                    }
                }
            }
            if (anyChanged) {
                int minRow = rows.get(0);
                int maxRow = rows.get(1);
                for (Integer row : rows) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                }
                fireTableRowsUpdated(minRow, maxRow);
                tableModel.updateVisible(getChosenTraitInstances());
            }
        }

        public Set<TraitInstance> getChosenTraitInstances() {
            return chosen.stream()
                .map(rowIndex -> tiByName.get(getItemAt(rowIndex)))
                .collect(Collectors.toSet());
        }

        public TraitInstance getTraitInstance(int rowIndex) {
            return tiByName.get(getItemAt(rowIndex));
        }
        public boolean isRowChosen(int rowIndex) {
            return chosen.contains(rowIndex);
        }

    }
}
