/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.heatmap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.curate.data.TraitHelper;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.kdxplore.stats.SimpleStatistics.StatName;
import com.diversityarrays.kdxplore.stats.StatsUtil;
import com.diversityarrays.util.Check;

@SuppressWarnings("nls")
// TODO i18n
public class AskForPositionNamesAndTraitInstancePanel extends JPanel {
	
	
	static private final SimpleStatistics.StatName[] STATS_WANTED = {
		SimpleStatistics.StatName.N_SAMPLES,
		SimpleStatistics.StatName.MIN_VALUE,
		SimpleStatistics.StatName.MAX_VALUE,
		SimpleStatistics.StatName.N_OUTLIERS,
	};
	
	static private final int N_STATS_FEATURES = STATS_WANTED.length;
	
	static private final int N_LEAD_COLUMNS = 5;

	// TODO remove this when we generalise the use of ValueRetriever-s for plotting any-X by any-Y for any-Z.
	private final int nPositionNamesToChoose;
	private final int nTraitInstancesToChoose;
	
	final private List<TraitInstance> traitInstancesX;
	private final Map<TraitInstance, SimpleStatistics<?>> statsByTraitInstance;
	private final Map<TraitInstance, ValidationRule> validationRuleByTraitInstance = new HashMap<TraitInstance, ValidationRule>();
	private final Map<TraitInstance, InvalidRuleException> validationRuleExceptionByTraitInstance = new HashMap<TraitInstance, InvalidRuleException>();
	

	class AxisChoiceTableModel extends AbstractTableModel {

		final List<ValueRetriever<?>> valueRetrievers = new ArrayList<>();
		final List<ValueRetriever<?>> excluded = new ArrayList<>();
		
		final Map<AxisChoice,Set<Integer>> rowIndicesByAxisChoice = new HashMap<>();
		final Map<Integer,AxisChoice> axisChoiceByRowIndex = new HashMap<>();
		
		public Map<AxisChoice,Set<Integer>> getCurrentAxisChoices(){
			return rowIndicesByAxisChoice;
		}
		
		public AxesAndValues getAxesAndValues() {
			ValueRetriever<?> x = null;
			ValueRetriever<?> y = null;
			List<ValueRetriever<?>> zlist = new ArrayList<>();

			Set<Integer> rows = rowIndicesByAxisChoice.get(AxisChoice.X);
			if (! rows.isEmpty()) {
				// Only the first (there should only be one!
				for (Integer row : rows) {
					x = valueRetrievers.get(row);
					break;
				}
			}

			rows = rowIndicesByAxisChoice.get(AxisChoice.Y);
			if (! rows.isEmpty()) {
				// Only the first (there should only be one!
				for (Integer row : rows) {
					y = valueRetrievers.get(row);
					break;
				}
			}

			rows = rowIndicesByAxisChoice.get(AxisChoice.Z);
			if (! rows.isEmpty()) {
				for (Integer row : rows) {
					zlist.add(valueRetrievers.get(row));
				}
				Collections.sort(zlist, new Comparator<ValueRetriever<?>>() {
					@Override
					public int compare(ValueRetriever<?> o1, ValueRetriever<?> o2) {
						return o1.getDisplayName().compareTo(o2.getDisplayName());
					}
					
				});
			}
			
			return new AxesAndValues(x, y, zlist);
		}
		
		public void initialise(List<ValueRetriever<?>> list) {
			// Only add to the list if the value can be used
			this.valueRetrievers.clear();
			this.excluded.clear();
			
			for (ValueRetriever<?> vr : list) {
				if (vr instanceof TraitInstanceValueRetriever) {
					TraitInstanceValueRetriever<?> tivr = (TraitInstanceValueRetriever<?>) vr;
					SimpleStatistics<?> ss = statsByTraitInstance.get(tivr.getTraitInstance());
					if (ss != null && ss.getValidCount() > 0) {
						valueRetrievers.add(vr);
					}
					else {
						excluded.add(vr);
					}
				}
				else {
					AxisChoice axisChoice = null;
					switch (vr.getTrialCoord()) {
					case X:
						axisChoice = AxisChoice.X;
						break;
					case Y:
						axisChoice = AxisChoice.Y;
						break;
					case NONE:
					case PLOT_ID:
					default:
						break;
					}

					if (axisChoice != null) {
						int index = valueRetrievers.size();
						valueRetrievers.add(vr);
						
						rowIndicesByAxisChoice.put(axisChoice, new HashSet<>(Arrays.asList(index)));
						axisChoiceByRowIndex.put(index, axisChoice);
					}
					else {
						valueRetrievers.add(vr);
					}
				}
			}

			fireTableDataChanged();
		}
		
		@Override
		public int getRowCount() {
			return valueRetrievers==null ? 0 : valueRetrievers.size();
		}
		
		@Override
		public int getColumnCount() {
			return N_LEAD_COLUMNS + N_STATS_FEATURES;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case 0: return "Axis?";
			case 1: return "Plot Attr/Trait";
			case 2: return "Instance";
			case 3: return "Datatype";
			case 4: return "Range";
			}
			
			int index = columnIndex - N_LEAD_COLUMNS;
			
			StatName statName = STATS_WANTED[index];
			return statName.displayName;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0: return AxisChoice.class;
			case 1: return String.class;
			case 2: return Integer.class;
			case 3: return String.class;
			case 4: return TraitInstance.class;
			}
			
			int index = columnIndex - N_LEAD_COLUMNS;
			
			StatName statName = STATS_WANTED[index];
			return StatsUtil.getValueClass(statName);
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}

		@Override
		public void setValueAt(Object aValue, int row, int columnIndex) {
			if (columnIndex==0 && aValue instanceof AxisChoice) {

				Integer newRowIndex = row;
				
				Set<Integer> changed = new HashSet<Integer>();
				
				AxisChoice axisChoice = (AxisChoice) aValue;
				Set<Integer> rowIndices = rowIndicesByAxisChoice.get(axisChoice);
				

				switch (axisChoice) {
				case NOT_SELECTED:
					if (rowIndices != null) {
						rowIndices.remove(newRowIndex);
						if (rowIndices.isEmpty()) {
							rowIndicesByAxisChoice.remove(axisChoice);
						}
					}
					axisChoiceByRowIndex.remove(newRowIndex);
//					axisChoiceByRowIndex.put(newRowIndex, axisChoice);
					changed.add(newRowIndex);
					break;
					
					
				case X:
				case Y:
					// Only one selection allowed
					if (rowIndices != null) {
						changed.addAll(rowIndices);
						for (Integer oldRowIndex : rowIndices) {
							axisChoiceByRowIndex.remove(oldRowIndex);
						}
					}
					
					rowIndices = new HashSet<>();
					rowIndices.add(newRowIndex);
					rowIndicesByAxisChoice.put(axisChoice, rowIndices);
					axisChoiceByRowIndex.put(newRowIndex, axisChoice);
					changed.add(newRowIndex);
					break;
					
				case Z:	
					if (rowIndices != null) {
						// If only 1 allowed then remove the other
						if ( nTraitInstancesToChoose == 1) {
							changed.addAll(rowIndices);
							for (Integer oldRowIndex : rowIndices) {
								axisChoiceByRowIndex.remove(oldRowIndex);
							}
							rowIndices = new HashSet<>();
						}
						// else multiple selections are allowed
					}
					else {
						rowIndices = new HashSet<>();
					}

					rowIndices.add(newRowIndex);
					
					rowIndicesByAxisChoice.put(axisChoice, rowIndices);
					axisChoiceByRowIndex.put(newRowIndex, axisChoice);
					changed.add(newRowIndex);

					break;
				}
				
				if (! changed.isEmpty()) {
					for (Integer ri : changed) {
						fireTableRowsUpdated(ri, ri);
					}

					boolean enableAction = true;
					Set<Integer> zRowIndices = null;
					for (AxisChoice ac : AxisChoice.values()) {
						if (AxisChoice.NOT_SELECTED != ac) {
							Set<Integer> indices = rowIndicesByAxisChoice.get(ac);
							if (Check.isEmpty(indices)) {
								enableAction = false;
								break;
							}
							
							if (AxisChoice.Z == ac) {
								zRowIndices = indices;
							}
						}
					}

					if (enableAction) {
						if (zRowIndices != null) {
							enableAction = zRowIndices.size() == nTraitInstancesToChoose;
						}
					}
					
					enableActionNotifier.execute(enableAction);
				}
				
			}
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			
			ValueRetriever<?> vr = valueRetrievers.get(rowIndex);
			
			TraitInstance ti = null;
			TraitInstanceValueRetriever<?> tivr = null;
			if (vr instanceof TraitInstanceValueRetriever) {
				tivr = (TraitInstanceValueRetriever<?>) vr;
				ti = tivr.getTraitInstance();
			}
			
			switch (columnIndex) {
			case 0:
				if (tivr != null) {
					SimpleStatistics<?> ss = statsByTraitInstance.get(ti);
					if (ss.getValidCount() <= 0) {
						return null; // so the renderer can show '*'
						// THIS may now be redundant because "no data" VRs are
						// excluded in initialise()
					}
				}
				AxisChoice axisChoice = axisChoiceByRowIndex.get(rowIndex);
				return axisChoice==null ? AxisChoice.NOT_SELECTED : axisChoice;
				
			case 1:
				return vr.getDisplayName();
				
			case 2:
				return ti==null ? null : ti.getInstanceNumber();
				
			case 3:
				// Trait Data Type
				Trait trait = null;
				if (ti != null) {
					trait = ti.trait;
				}
				return trait==null ? null : trait.getTraitDataType();

			case 4:
				return ti; 
			}
			
			if (ti == null) {
				return null;
			}
			
			int index = columnIndex - N_LEAD_COLUMNS;
			
			SimpleStatistics<?> ss = statsByTraitInstance.get(ti);
			StatName statName = STATS_WANTED[index];
			return StatsUtil.getStatNameValue(ss, statName);
		}

		public List<TraitInstance> getSelectedTraitInstances() {
			List<TraitInstance> result = new ArrayList<>();
			for (AxisChoice ac : AxisChoice.values()) {
				if (AxisChoice.NOT_SELECTED != ac) {
					Set<Integer> rowIndices = rowIndicesByAxisChoice.get(ac);
					if (rowIndices != null) {
						for (Integer row : rowIndices) {
							ValueRetriever<?> vr = valueRetrievers.get(row);
							if (vr instanceof TraitInstanceValueRetriever) {
								result.add(((TraitInstanceValueRetriever<?>) vr).getTraitInstance());
							}
						}
					}
				}
			}
			return result;
		}

	};
	
	private Map<JFrame,HeatMapPanelParameters> parametersInUse = new HashMap<JFrame,HeatMapPanelParameters>();
	
	private final AxisChoiceTableModel tableModel = new AxisChoiceTableModel();
	private final JTable table = new JTable(tableModel);
		
	private final Closure<Boolean> enableActionNotifier;

	public AskForPositionNamesAndTraitInstancePanel(
			int nPositionsWanted,
			int nTraitInstancesWanted,
			List<ValueRetriever<?>> positionAndPlotRetrievers,
			Map<TraitInstance, SimpleStatistics<?>> statsByTraitInstance,
			final Closure<Boolean> enableActionNotifier, 
			CurationContext context) 
	{
		super(new BorderLayout());
		
		if (nPositionsWanted > 3) {
			// coz we only do X,Y,Z !!
			throw new IllegalArgumentException("At most 3 position names can be chosen");
		}
		
		nPositionNamesToChoose = nPositionsWanted;
		nTraitInstancesToChoose = nTraitInstancesWanted;
		
		this.enableActionNotifier = enableActionNotifier;
//		this.traitInstanceIsAvailable = traitInstanceIsAvailable;
		
		this.statsByTraitInstance = statsByTraitInstance;
		
		traitInstancesX = new ArrayList<TraitInstance>(statsByTraitInstance.keySet());
		Collections.sort(traitInstancesX, TraitHelper.COMPARATOR);
		
		List<ValueRetriever<?>> list = new ArrayList<ValueRetriever<?>>();
		
		list.addAll(positionAndPlotRetrievers);

        Function<TraitInstance, List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
            @Override
            public List<KdxSample> apply(TraitInstance ti) {
                return context.getPlotInfoProvider().getSampleMeasurements(ti);
            }
        };
		for (TraitInstance ti : traitInstancesX) {
			try {
				ValidationRule vrule = ValidationRule.create(ti.trait.getTraitValRule());
				validationRuleByTraitInstance.put(ti, vrule);
                TraitInstanceValueRetriever<?> tivr = TraitInstanceValueRetriever.getValueRetriever(
				        context.getTrial(), ti, sampleProvider);
				list.add(tivr);
				
			} catch (InvalidRuleException e) {
				validationRuleExceptionByTraitInstance.put(ti, e);
			}
		}
			
		tableModel.initialise(list);

		Box buttons = Box.createVerticalBox();
		
		final List<AxisChoiceAction> axisChoiceActions = new ArrayList<>();
		for (AxisChoice ac : AxisChoice.values()) {
			AxisChoiceAction action = new AxisChoiceAction(ac);
			action.setEnabled(false);
			axisChoiceActions.add(action);
		}

		buttons.add(new JLabel("Select Axis:"));
		for (AxisChoiceAction action : axisChoiceActions) {
			if (AxisChoice.Z == action.axisChoice) {
				buttons.add(Box.createVerticalStrut(10));
			}
			buttons.add(new JButton(action));
			if (AxisChoice.NOT_SELECTED == action.axisChoice) {
				buttons.add(Box.createVerticalStrut(10));
			}
		}
		buttons.add(Box.createVerticalGlue());
		
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					int mrow = -1;
					int vrow = table.getSelectedRow();
					if (vrow >= 0) {
						mrow = table.convertRowIndexToModel(vrow);
					}

					if (mrow >= 0) {
						ValueRetriever<?> vr = tableModel.valueRetrievers.get(mrow);
						boolean isTraitInstance = vr instanceof TraitInstanceValueRetriever;
						
						for (AxisChoiceAction action : axisChoiceActions) {
							switch (action.axisChoice) {
							case NOT_SELECTED:
							case X:
							case Y:
								action.setEnabled(true);
								break;

							case Z:
								action.setEnabled(isTraitInstance);
								break;

							default:
								action.setEnabled(false);
								break;
							}
						}
						
					}
					else {
						for (AxisChoiceAction action : axisChoiceActions) {
							action.setEnabled(false);
						}
					}
				}
			}		
		});
		
		table.setDefaultRenderer(AxisChoice.class, new AxisChoiceRenderer("Not available", "*"));
		table.setDefaultRenderer(TraitInstance.class, new TraitInstanceRenderer());
		
		String text = nTraitInstancesToChoose <= 1 ? "Select Axes and Value:" : "Select Axes and Values:";
		JPanel traitInstancesPanel = new JPanel(new BorderLayout());
		
		traitInstancesPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
	
		traitInstancesPanel.add(new JLabel(text), BorderLayout.NORTH);
		traitInstancesPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		add(buttons, BorderLayout.WEST);
		add(traitInstancesPanel, BorderLayout.CENTER);

		if (! tableModel.excluded.isEmpty()) {
			JLabel lbl = new JLabel("TraitInstances without plottable data have been excluded");
			lbl.setHorizontalAlignment(JLabel.CENTER);
			add(lbl, BorderLayout.SOUTH);			
			
//			StringBuilder sb = new StringBuilder("<HTML>No Data:");
//			for (ValueRetriever<?> vr : tableModel.excluded) {
//				sb.append("<BR>").append(StringUtil.htmlEscape(vr.getDisplayName()));
//			}
//			add(new JScrollPane(new JLabel(sb.toString())), BorderLayout.SOUTH);
		}
		
	}

	private WindowListener windowCloseListener = new WindowAdapter() {
		@Override
		public void windowClosed(WindowEvent e) {
			JFrame frm = (JFrame) e.getSource();
			frm.removeWindowListener(this);
			parametersInUse.remove(frm);
			table.repaint();
		}
	};
	
	public void addHeatMapFrame(HeatMapPanelParameters hmpp, JFrame frame) {
		parametersInUse.put(frame,  hmpp);
		frame.addWindowListener(windowCloseListener);
		table.repaint();
	}
	
	public AxesAndValues getAxesAndValues() {
		return tableModel.getAxesAndValues();
	}
	
	class AxisChoiceAction extends AbstractAction {
		private AxisChoice axisChoice;

		AxisChoiceAction(AxisChoice ac) {
			super(AxisChoice.NOT_SELECTED==ac ? "Deselect" : ac.displayName);
			this.axisChoice = ac;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int vrow = table.getSelectedRow();
			if (vrow >= 0) {
				int mrow = table.convertRowIndexToModel(vrow);
				if (mrow >= 0) {
					tableModel.setValueAt(axisChoice, mrow, 0);
				}
			}
		}
	}
	
	class TraitInstanceRenderer extends DefaultTableCellRenderer {

		private final String TAG = TraitInstanceRenderer.class.getSimpleName();
		
		private static final boolean DEBUG = false;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof TraitInstance) {
				TraitInstance ti = (TraitInstance) value;
				ValidationRule vrule = validationRuleByTraitInstance.get(ti);
				InvalidRuleException exception = validationRuleExceptionByTraitInstance.get(ti);
				if (exception != null) {
					setBackground(Color.RED);
					setToolTipText(exception.getMessage());
				}
				else {
					setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
				}

				if (vrule == null) {
					setText("");
				}
				else {
					setText(vrule.getExpression());

					try {
						double[] rangeLimits = vrule.getRangeLimits();
						
						if (rangeLimits == null) {
							if (DEBUG) {
								android.util.Log.d(TAG, 
										"getTableCellRendererComponent:  NO rangeLimits: " + vrule.getExpression());
							}
						}
						else {
							DecimalFormat fmt;
							if (vrule.isIntegralRange()) {
								fmt = new DecimalFormat("0");
							}
							else {
								fmt = new DecimalFormat("0.00");
							}

							String s = fmt.format(rangeLimits[0]) + " to " + fmt.format(rangeLimits[1]);
							setText(s);

							if (DEBUG) {
								android.util.Log.d(TAG, 
										"getTableCellRendererComponent: HAS rangeLimits: "
										+ vrule.getExpression() + ":: " + s) ;
							}
						}
					}
					catch (UnsupportedOperationException e) {
						Shared.Log.w(TAG, "getTableCellRendererComponent: UNSUPPORTED rangeLimits: " + vrule.getExpression());
					}
				}

			}
			else {
				setToolTipText(null);
			}
			
			return this;
		}

	}

	static class AxisChoiceRenderer extends JLabel implements TableCellRenderer {

		private String textForNullValue;
		
		public AxisChoiceRenderer(String toolTipTextForNullValue) {
			this(toolTipTextForNullValue, "*");
		}
		
		public AxisChoiceRenderer(String toolTipTextForNullValue, String textForNullValue) {
			this.textForNullValue = textForNullValue;
			setHorizontalAlignment(CENTER);
			setToolTipText(toolTipTextForNullValue);
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{
			Component result = null;
			if (value == null) {
				this.setText(textForNullValue);
				result = this;
			}
			else {
				AxisChoice axisChoice = (AxisChoice) value;
				this.setText(axisChoice.getDisplayName());
				result = this;
			}
			return result;
		}
		
	}

	// TODO - replace this with a generalisation to ValueRetreiver<?>
	public List<TraitInstance> getSelectedTraitInstances() {
		return tableModel.getSelectedTraitInstances();
	}


}
