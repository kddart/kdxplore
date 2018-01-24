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
package com.diversityarrays.kdxplore.curate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultRowSorter;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.TitledTablePanelWithResizeControls;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.OptionalCheckboxRenderer;

import net.pearcan.exhibit.ExhibitColumn;
import net.pearcan.reflect.Feature;
import net.pearcan.ui.renderer.StringCellRenderer;
import net.pearcan.ui.table.TableColumnSelectionButton;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.StringUtil;

/**
 * Provides a view of the SimpleStatistics for each of the TraitInstances
 * selected for display in the CurationTable (and the FieldView).
 * @author brianp
 */
public class TraitsAndInstancesPanel2 extends JPanel implements TraitsAndInstances {

    private final TIStatsTableModel tiStatsModel;
    private final CurationContext curationContext;
    private Consumer<List<OutlierSelection>> outlierConsumer;
    private CurationMenuProvider curationMenuProvider;
    private TraitInstanceStatsTable traitInstanceStatsTable;
    
    private List<OutlierSelection> outlierSelections = new ArrayList<>();
    
    @SuppressWarnings("rawtypes")
    private Comparator comparableComparator = new Comparator() {
        @SuppressWarnings("unchecked")
        public int compare(Object o1, Object o2) {
            int result;
            
            if (o1==null) {
                if (o2==null) {
                    return 0;
                }
                return -1;
            }
            else if (o2==null) {
                return 1;
            }
            
            Comparable lhs = (Comparable) o1;
            Comparable rhs = (Comparable) o2;
            
            if (lhs.getClass().equals(rhs.getClass())) {
                result = lhs.compareTo(rhs);
            }
            else if (lhs instanceof Double) {
                result = -1;
            }
            else if (rhs instanceof Double) {
                result = +1;
            }
            else {
                String sl = lhs.toString();
                String sr = rhs.toString();
                result = sl.compareTo(sr);
//              result = lhs.getClass().getName().compareTo(rhs.getClass().getName());
            }
            return result;
        }
    };
    private final PropertyChangeListener rowSorterChangeListener = new PropertyChangeListener() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            RowSorter<? extends TableModel> rowSorter = traitInstanceStatsTable.getRowSorter();
            if (rowSorter instanceof javax.swing.DefaultRowSorter) {
                DefaultRowSorter drs = (DefaultRowSorter) rowSorter;
                for (int column = tiStatsModel.getColumnCount(); --column >= 0; ) {
                    Class<?> clz = tiStatsModel.getColumnClass(column);
                    if (Comparable.class.isAssignableFrom(clz)) {
                        drs.setComparator(column, comparableComparator);
                    }
                }
            }
        }
    };
    
    private final DefaultTableCellRenderer statDetailRenderer = new DefaultTableCellRenderer() {

        private final Set<String> meanMedianQ1Q3 = new HashSet<>(Arrays.asList("Mean","Median", "Q1", "Q3"));

        // TODO Consider removing this as the "formatting" may also get done in the getTableCellRendererComponent() method
        private final DateFormat df = TraitValue.getTraitValueDateFormat();
        
        @Override
        protected void setValue(Object value) {
            if (value instanceof Date) {
                super.setValue(df.format((Date) value));
            }
            else {
                super.setValue(value);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) 
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);

            if (value instanceof Comparable) {
                // This is one of Min,Max,Mean,Median,Q1,Q3
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0) {
                    int modelColumn = table.convertColumnIndexToModel(column);
                    if (modelColumn >= 0) { 
                        
                        // FIXME should distinguish Mean,Median,Q1,Q3 and, for "DoubleStats" probably
                        //       use one more decimal place to render the value
                        String tv = "";
                        SimpleStatistics<?> stats = tiStatsModel.getStatsAt(modelRow);
                        if (stats != null) {
                            Format format = stats.getFormat();
                            if (format != null) {
                                tv = format.format(value);
                            }
                        }
                        setText(tv);
                    }
                }
            }
            
            return this;
        }
    };    
    
    private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            traitInstanceStatsTable.repaint();
        }
    };

    public TraitsAndInstancesPanel2(CurationContext cc,
            Font smallFont, 
            TIStatsTableModel tistm,    
            boolean anyInstanceNumbers,
            int nInvalidRules,
            String tAndIpanelLabel, 
            CurationMenuProvider curationMenuProvider, 
            Consumer<List<OutlierSelection>> outlierConsumer) 
    {
        super(new BorderLayout());
        
        this.curationContext = cc;
        this.outlierConsumer = outlierConsumer;
        this.curationMenuProvider = curationMenuProvider;

        this.tiStatsModel = tistm;
        
        curationContext.addPropertyChangeListener(propertyChangeListener);
        
        traitInstanceStatsTable = new TraitInstanceStatsTable(tiStatsModel);

        traitInstanceStatsTable.addPropertyChangeListener("rowSorter", rowSorterChangeListener);
        traitInstanceStatsTable.setAutoCreateRowSorter(true);

        traitInstanceStatsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                int clickCount = me.getClickCount();
                
                if (SwingUtilities.isRightMouseButton(me) && 1 == clickCount) {
                    me.consume();
                    showContextMenu( me);
                }
                else if (SwingUtilities.isLeftMouseButton(me) && 2 == clickCount) {
                    showTraitInstanceInfo(me);
                }
            }
        });

        Map<String, TableColumn[]> tableColumnsByChoice = createInstanceTableColumnsByChoice();
        TableColumnSelectionButton tcsb = new TableColumnSelectionButton(traitInstanceStatsTable, tableColumnsByChoice);
        
        StringCellRenderer scRenderer = new StringCellRenderer();
        scRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        statDetailRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        
        traitInstanceStatsTable.setDefaultRenderer(String.class, scRenderer);
        Transformer<TraitInstance,String> instanceNameTransformer = new Transformer<TraitInstance,String>() {
            @Override
            public String transform(TraitInstance ti) {
                return curationContext.makeTraitInstanceName(ti);
            }           
        };
        TraitInstanceCellRenderer tiRenderer = new TraitInstanceCellRenderer(
                curationContext.getTraitColorProvider(), 
                instanceNameTransformer);
        tiRenderer.setName("FOR-STATS-TABLE"); //$NON-NLS-1$
        traitInstanceStatsTable.setDefaultRenderer(TraitInstance.class, tiRenderer);
        traitInstanceStatsTable.setDefaultRenderer(Comparable.class, statDetailRenderer);
        
        TableColumnModel tcm = traitInstanceStatsTable.getColumnModel();
        Integer viewColumnIndex = tistm.getViewColumnIndex();
        if (viewColumnIndex != null) {
            TableColumn viewColumn = tcm.getColumn(viewColumnIndex);
            viewColumn.setMaxWidth(40);
            viewColumn.setCellRenderer(new OptionalCheckboxRenderer("No Values"));
        }
        
        
        if (nInvalidRules <= 0) {
            // Hide that column!
            int columnIndex = tistm.getValRuleErrorColumnIndex();
            columnIndex = traitInstanceStatsTable.convertColumnIndexToView(columnIndex);
            if (columnIndex >= 0) {
                TableColumn c = tcm.getColumn(columnIndex);
                tcm.removeColumn(c);
            }
        }
        
        TableColumn dataTypeTableColumn = traitInstanceStatsTable.getColumnModel().getColumn(
                tistm.getTraitInstanceDatatypeColumnIndex());
        dataTypeTableColumn.setCellRenderer(new TraitDataTypeRenderer());
        
        Function<JTable,JScrollPane> scrollMaker = new Function<JTable, JScrollPane>() {
            @Override
            public JScrollPane apply(JTable t) {
                return new PromptScrollPane(t, "Select Trait in the Panel: " + tAndIpanelLabel);
            }
        };
        TitledTablePanelWithResizeControls ttp = new TitledTablePanelWithResizeControls("Trait Instances", traitInstanceStatsTable, smallFont, scrollMaker);
        ttp.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        ttp.scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, tcsb);
        
        new CurationContextOptionsPopup(curationContext, ttp.separator);
        
        if (anyInstanceNumbers) {
            tcsb.setSelectedColumns(SimpleStatistics.GROUP_BASIC_STATS);
        }
        else {
            Set<Object> headerValues = new HashSet<>();
            for (TableColumn tc : tableColumnsByChoice.get(SimpleStatistics.GROUP_BASIC_STATS)) {
                headerValues.add(tc.getHeaderValue());
            }
            headerValues.remove(tistm.getInstanceHeading());
            tcsb.initialiseSelectedColumns(new Predicate<Object>() {
                @Override
                public boolean evaluate(Object hvalue) {
                    return headerValues.contains(hvalue);
                }
            });
        }

        add(ttp, BorderLayout.CENTER);
    }
    
    public Map<String, TableColumn[]> createInstanceTableColumnsByChoice() {

        TableColumnModel tcm = traitInstanceStatsTable.getColumnModel();
        
        Map<String, List<TableColumn>> tmp = new HashMap<String, List<TableColumn>>();
        Map<Feature, Integer> fmap = tiStatsModel.getFeatureToColumnIndex();
        
        List<TableColumn> basic = new ArrayList<>();
        tmp.put(SimpleStatistics.GROUP_BASIC_STATS, basic);
        Integer viewColumnIndex = tiStatsModel.getViewColumnIndex();
        if (viewColumnIndex != null) {
            basic.add(tcm.getColumn(viewColumnIndex));
        }
        for (int index : tiStatsModel.getTraitInstanceColumnIndices()) {
            basic.add(tcm.getColumn(index));
        }
        
        for (Feature f : fmap.keySet()) {
            String grp = null;
            ExhibitColumn exhibitColumn = f.getExhibitColumn();
            if (exhibitColumn != null) {
                grp = exhibitColumn.group();
            }
            if (Check.isEmpty(grp)) {
                grp = SimpleStatistics.GROUP_BASIC_STATS;
            }
            List<TableColumn> list = tmp.get(grp);
            if (list == null) {
                list = new ArrayList<TableColumn>();
                tmp.put(grp, list);
            }
            list.add(tcm.getColumn(fmap.get(f)));
        }

        Map<String, TableColumn[]> result = new HashMap<String, TableColumn[]>();
        for (String grp : tmp.keySet()) {
            List<TableColumn> list = tmp.get(grp);
            result.put(grp, list.toArray(new TableColumn[list.size()]));
        }
        
        return result;
    }
    private void showContextMenu(MouseEvent me) {
        
        Map<SimpleStatistics<?>,TraitInstance> tiByStat = new HashMap<>();
        
        List<SimpleStatistics<?>> statsList = new ArrayList<>();
        List<TraitInstance> selectedInstances = new ArrayList<>();      

        Set<Integer> rowSet = new HashSet<>();

        for (int row : traitInstanceStatsTable.getSelectedRows()) {
            rowSet.add(row);
        }

        // Include the right-click one too
        int row = traitInstanceStatsTable.rowAtPoint(me.getPoint());
        if (row >= 0) {
            rowSet.add(row);
        }

        List<Integer> vrows = new ArrayList<>(rowSet);
        Collections.sort(vrows);
        for (Integer vrow : vrows) {
            collectInfoFromRow(vrow, selectedInstances, tiByStat, statsList);
        }

        List<TraitInstance> checkedInstances = tiStatsModel.getCheckedTraitInstances();
        
        Action action = null;
        if (! statsList.isEmpty()) {
            String label = statsList.size()==1
                    ? "Show Outliers" 
                    : "Show Outliers for " + statsList.size() + " Traits";
            action = new AbstractAction(label) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    StringBuilder html = new StringBuilder();
                    html.append("<HTML><TABLE><TR>");
                    
                    outlierSelections.clear();
                    
                    for (SimpleStatistics<?> stat : statsList) {
                        html.append("<TH>");
                        html.append(StringUtil.htmlEscape(stat.getStatsName()));
                        html.append("</TH>");
                    }
                    
                    html.append("</TR><TR>");
                    
                    for (SimpleStatistics<?> stat : statsList) {
                        TraitInstance ti = tiByStat.get(stat);
                        html.append("<TD valign='top'>");
                        describeStats(stat, html, ti);
                        html.append("</TD>");

                        outlierSelections.add(createOutlierSelection(stat, ti));
                    }
                    html.append("</TR></TABLE>");
                    
                    outlierConsumer.accept(outlierSelections);
                    
                    JOptionPane.showMessageDialog(TraitsAndInstancesPanel2.this,
                            html.toString(), 
                            "Outliers",
                            JOptionPane.INFORMATION_MESSAGE);
                    
                }
            };
        }
        
        curationMenuProvider.showTraitInstanceTableToolMenu(
                me,
                checkedInstances,
                selectedInstances,
                action);
    }

    private void collectInfoFromRow(int vrow, List<TraitInstance> selectedInstances,
            Map<SimpleStatistics<?>, TraitInstance> mapping, List<SimpleStatistics<?>> statsList) {
        int modelRow = traitInstanceStatsTable.convertRowIndexToModel(vrow);
        if (modelRow >= 0) {
            TraitInstance ti = tiStatsModel.getTraitInstanceAt(modelRow);
            selectedInstances.add(ti);
            
            SimpleStatistics<?> stat = tiStatsModel.getStatsAt(modelRow);
            if (stat != null) {
                statsList.add(stat);
                mapping.put(stat,ti);
            }
        }
    }
    
    private void showTraitInstanceInfo(MouseEvent me) {
        Point point = me.getPoint();
        int vrow = traitInstanceStatsTable.rowAtPoint(point);
        if (vrow >= 0) {
            int mrow = traitInstanceStatsTable.convertRowIndexToModel(vrow);
            if (mrow >= 0) {
                SimpleStatistics<?> stats = tiStatsModel.getStatsAt(mrow);
                
                TraitInstance ti = tiStatsModel.getTraitInstanceAt(mrow);
                String tiName = curationContext.getTrial().getTraitNameStyle().makeTraitInstanceName(ti);
                
                StringBuilder html = new StringBuilder("<HTML>");
                
                html.append("<dl>");
                html.append("<dt><b>Description:</b></dt>")
                    .append("<dd>")
                    .append(StringUtil.htmlEscape(ti.trait.getTraitDescription()))
                    .append("</dd>");
                html.append("<dt><b>Unit:</b></dt>")
                    .append("<dd>")
                    .append(StringUtil.htmlEscape(ti.trait.getTraitUnit()))
                    .append("</dd>");
                
                
                try {
                    ValidationRule rule = ValidationRule.create(ti.trait.getTraitValRule());
                    html.append("<dt><b>Validation Rule:</b></dt>")
                    .append("<dd>")
                    .append(StringUtil.htmlEscape(rule.getDescription()))
                    .append("</dd>");
                } catch (InvalidRuleException e) {
                }
                html.append("</dl>");

                html.append("<hr>");

                if (stats == null) {
                    html.append("No Outliers");
                }
                else {
                    html.append("<b>Outliers</b><br>");
                    describeStats(stats, html, ti);
                }
                
                JOptionPane.showMessageDialog(
                        TraitsAndInstancesPanel2.this, 
                        html.toString(), 
                        "Information for  " + tiName,
                        JOptionPane.INFORMATION_MESSAGE);

            }
        }
    }

    private void describeStats(SimpleStatistics<?> stats, StringBuilder html, TraitInstance ti) {
        TraitDataType tdt = ti.trait.getTraitDataType();
        if (tdt.isNumeric()) {
            Bag<?> lowOutliers = stats.getLowOutliers();
            Bag<?> highOutliers = stats.getHighOutliers();
            describe(html, "<i>Low Outliers</i>", lowOutliers);
            html.append("<HR>");
            describe(html, "<i>High Outliers</i>", highOutliers);
        }
        else {
            html.append("None<BR>(").append(tdt.name()).append(")");
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void describe(StringBuilder html, String which, Bag outliers) {
        html.append(which);
        if (Check.isEmpty(outliers)) {
            html.append("<BR>").append("None");
        }
        else {
            for (Object t : outliers.uniqueSet()) {
                html.append("<BR>").append(t);
                
                int count = outliers.getCount(t);
                if (count > 1) {
                    html.append(" (").append(count).append(')');
                }
            }
        }
    }

    private OutlierSelection createOutlierSelection(SimpleStatistics<?> stats, TraitInstance ti) {
        Bag<?> lowOutliers = stats.getLowOutliers();
        Bag<?> highOutliers = stats.getHighOutliers();

        Double maxMin = Double.MAX_VALUE;
        Double maxMax = 0.0;
        Comparable<?> max = stats.getMaxValue();
        int count = 0;
        for (Object o : highOutliers) {

            String val = String.valueOf(o);
            try {
                Double inter = Double.parseDouble(val);
                if (inter < maxMin) {
                    maxMin = inter;
                }

                if (count == 0) {
                    maxMax = Double.parseDouble(String.valueOf(max));
                }
                count++;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        Double minMin = 0.0;
        Double minMax = Double.MIN_VALUE;
        Comparable<?> min = stats.getMinValue();
        count = 0;
        for (Object o : lowOutliers) {

            String val = String.valueOf(o);
            try {
                Double inter = Double.parseDouble(val);
                if (inter > minMax) {
                    minMax = inter;
                }

                if (count == 0) {
                    minMin = Double.parseDouble(String.valueOf(min));
                }
                count++;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        OutlierSelection selection = new OutlierSelection(minMin, minMax, maxMin, maxMax, ti);
        return selection;
    }
    // --- TraitsAndInstances interface ---
    @Override
    public JComponent getComponent() {
        return this;
    }

    // TODO remove - is no longer used
    @Override
    public Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance() {
        return tiStatsModel.getStatsByTraitInstance();
    }

    @Override
    public List<TraitInstance> getCheckedTraitInstances() {
        return tiStatsModel.getCheckedTraitInstances();
    }

    @Override
    public List<TraitInstance> getTraitInstances(boolean allElseOnlyChecked) {
        return tiStatsModel.getAllTraitInstances(allElseOnlyChecked);
    }

    @Override
    public void changeTraitInstanceChoice(boolean choiceAdded, TraitInstance[] choices) {
        tiStatsModel.changeTraitInstanceChoice(choiceAdded, choices);
    }

    @Override
    public void addTraitInstanceStatsItemListener(ItemListener l) {
        tiStatsModel.addItemListener(l);
    }

    class TraitDataTypeRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column)
        {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            String ttt = null;
            int modelRow = table.convertRowIndexToModel(row);
            if (modelRow >= 0) {
                ttt = tiStatsModel.getValidationExpressionAt(modelRow);
            }
            setToolTipText(ttt);
            return comp;
        }
        
    }
}
