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
package com.diversityarrays.kdxplore.vistool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdxplore.curate.data.TraitHelper;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;

import com.diversityarrays.util.UnicodeChars;

class AskForTraitInstances extends JDialog {
	
//	static private final List<Feature> TRAIT_INSTANCE_FEATURES ; //Feature.getAllExhibitFeatures(TraitInstance.class);
//	static {
//		List<Feature> list = new ArrayList<>();
//		list.add(Feature.create(TraitInstance.class, "traitName"));
//		list.add(Feature.create(TraitInstance.class, "traitDataType"));
//		list.add(Feature.create(TraitInstance.class, "instanceNumber"));
//		
//		TRAIT_INSTANCE_FEATURES = Collections.unmodifiableList(list);
//	}
	
	private Set<Integer> chosen = new HashSet<Integer>();
	private int xAxisRowIndex = -1;
	private List<Integer> yAxisIndices = new ArrayList<>();
	
	class TraitInstanceChoiceTableModel extends AbstractTableModel {
		
		private final String[] columnNames = {
			"Use?",
			"Trait",
			"Data Type"
		};
		
		public TraitInstanceChoiceTableModel() {
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0: return Boolean.class;
			case 1: return String.class;
			case 2: return TraitDataType.class;
			}
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}

		private void handleSelection(int rowIndex, boolean select) {
			if (select) {
				if (xAxisRowIndex < 0) {
					xAxisRowIndex = rowIndex;
				}
				else if (! yAxisIndices.contains(rowIndex)){
					yAxisIndices.add(rowIndex);
				}
				chosen.add(rowIndex);
			} 
			else {
				// De-selecting
				if (xAxisRowIndex == rowIndex) {
					// ... the X-axis
					if (yAxisIndices.isEmpty()) {
						// No Y-axis so there are NO X- or Y-axes
						xAxisRowIndex = -1;
					}
					else {
						// Make the first yAxis into the xAxis, it remains in chosen
						xAxisRowIndex = yAxisIndices.remove(0);
						// New X-axis so ensure the table updates
						fireTableRowsUpdated(xAxisRowIndex, xAxisRowIndex);
					}
				}
				else {
					// Must be a Y-axis
					yAxisIndices.remove(new Integer(rowIndex));
				}
				chosen.remove(rowIndex);
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex==0 && aValue instanceof Boolean) {
				handleSelection(rowIndex, (Boolean) aValue);
				fireTableRowsUpdated(rowIndex, rowIndex);
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			
			TraitInstance ti = traitInstances.get(rowIndex);
			switch (columnIndex) {
			case 0: 
				return chosen.contains(rowIndex);
			case 1:
				return traitNameStyle.makeTraitInstanceName(ti);
			case 2:
				return ti.getTraitDataType();
			}
			return null;
		}
		
		@Override
		public int getRowCount() {
			return traitInstances.size();
		}
	}
	
	class TraitInstanceAxisChoiceTableModel extends AbstractTableModel {

		private final String[] columnNames = {
			"Use?",
			"Axis",
			"Trait",
			"Data Type"
		};
		
		public TraitInstanceAxisChoiceTableModel() {
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0: return Boolean.class;
			case 1: return String.class;
			case 2: return String.class;
			case 3: return TraitDataType.class;
			}
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex==0;
		}

		private void handleSelection(int rowIndex, boolean select) {
			if (select) {
				if (xAxisRowIndex < 0) {
					xAxisRowIndex = rowIndex;
				}
				else if (! yAxisIndices.contains(rowIndex)){
					yAxisIndices.add(rowIndex);
				}
				chosen.add(rowIndex);
			} 
			else {
				// De-selecting
				if (xAxisRowIndex == rowIndex) {
					// ... the X-axis
					if (yAxisIndices.isEmpty()) {
						// No Y-axis so there are NO X- or Y-axes
						xAxisRowIndex = -1;
					}
					else {
						// Make the first yAxis into the xAxis, it remains in chosen
						xAxisRowIndex = yAxisIndices.remove(0);
						// New X-axis so ensure the table updates
						fireTableRowsUpdated(xAxisRowIndex, xAxisRowIndex);
					}
				}
				else {
					// Must be a Y-axis
					yAxisIndices.remove(new Integer(rowIndex));
				}
				chosen.remove(rowIndex);
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex==0 && aValue instanceof Boolean) {
				handleSelection(rowIndex, (Boolean) aValue);
				fireTableRowsUpdated(rowIndex, rowIndex);
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			
			TraitInstance ti = traitInstances.get(rowIndex);
			switch (columnIndex) {
			case 0: 
				return chosen.contains(rowIndex);
			case 1:				
				if (xAxisRowIndex == rowIndex) {
					return "X Axis";
				} 
				if (yAxisIndices.contains(rowIndex)) {
					return "Value";
				}	
				return "";	
			case 2:
				return traitNameStyle.makeTraitInstanceName(ti);
			case 3:
				return ti.getTraitDataType();
			}
			return null;
		}
		
		@Override
		public int getRowCount() {
			return traitInstances.size();
		}
	}
	
	
	private List<TraitInstance> traitInstances;
	
	public List<TraitInstance> selectedTraitInstances = null;
	
	private JLabel message = new JLabel();
	
	private PropertyChangeSupport selection = new PropertyChangeSupport(this);
	
    public void addPropertyChangeListener(
            PropertyChangeListener l) {
            selection.addPropertyChangeListener(l);
        }
        public void removePropertyChangeListener(
            PropertyChangeListener l) {
            selection.removePropertyChangeListener(l);
        }
	
	private Action okAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			selectedTraitInstances = new ArrayList<TraitInstance>();
			
			// First one is the X-Axis
			selectedTraitInstances.add(traitInstances.get(xAxisRowIndex));
			
			for (Integer row : yAxisIndices) {
				selectedTraitInstances.add(traitInstances.get(row));
			}
			
			if (onInstancesChosen != null) {
				onInstancesChosen.execute(selectedTraitInstances);
			}
		}
	};
	
	private Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
		@Override
		public void actionPerformed(ActionEvent e) {
			selectedTraitInstances = null;
			dispose();
		}
	};

	private final int minInstances;

	private final int maxInstances;

	private final Closure<List<TraitInstance>> onInstancesChosen;

	private final TraitNameStyle traitNameStyle;
	
	private final TableModel tableModel;

	private final JTable table;

	AskForTraitInstances(Window owner, 
			String title, boolean xAndYaxes,
			String okLabel, 
			TraitNameStyle tns,
			Map<TraitInstance, SimpleStatistics<?>> map, 
			int[] minMax,
			Closure<List<TraitInstance>> onInstancesChosen) 
	{
		super(owner, title, ModalityType.MODELESS);
		
		this.traitNameStyle = tns;
		this.minInstances = minMax[0];
		this.maxInstances = minMax[1];
		this.onInstancesChosen = onInstancesChosen;
		
		if (xAndYaxes) {
			tableModel = new TraitInstanceAxisChoiceTableModel();
		}
		else {
			tableModel = new TraitInstanceChoiceTableModel();
		}
		table = new JTable(tableModel);
		
		okAction.putValue(Action.NAME, okLabel);
		
		traitInstances = new ArrayList<TraitInstance>(map.keySet());
		Collections.sort(traitInstances, TraitHelper.COMPARATOR);

		table.setAutoCreateRowSorter(true);
		
		Box box = Box.createHorizontalBox();
		box.add(message);
		box.add(Box.createHorizontalGlue());
		box.add(new JButton(okAction));
		box.add(new JButton(cancelAction));
		
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		getContentPane().add(box, BorderLayout.SOUTH);
		
		pack();
		
		tableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				updateOkButton();
			}
		});
		updateOkButton();
	}
	
	private void updateOkButton() {
		int nsel = chosen.size();
		
		boolean ok = false;
		if (minInstances <= nsel && nsel <= maxInstances) {
			ok = true;
		}
		else if (minInstances <= nsel && maxInstances == 0) {
			ok = true;
		}
		
		if (ok) {
			okAction.setEnabled(true);
			message.setForeground(Color.BLACK);
			if (maxInstances==0 && traitInstances.size() > 1) {
				message.setText("You may choose more if you wish");
			}
			else {
				message.setText("You're ready to proceed");
			}
		}
		else {
			okAction.setEnabled(false);
			message.setForeground(Color.RED);
			if (minInstances == maxInstances) {
				message.setText("Please select exactly "+ maxInstances + " Trait Instance" + (minInstances==1 ? "" : "s"));
			}
			else if (maxInstances > 0) {
				message.setText("Please select between "+minInstances+"and"+maxInstances+" Trait Instances");				
			}
			else {
				message.setText("Please select at least " + minInstances + " Trait Instance" + (minInstances==1 ? "" : "s"));				
			}
		}
	}
}
