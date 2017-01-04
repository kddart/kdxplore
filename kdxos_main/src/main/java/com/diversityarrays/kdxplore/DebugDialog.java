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
package com.diversityarrays.kdxplore;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.pearcan.ui.table.BspAbstractTableModel;
import net.pearcan.util.StringUtil;

class DebugDialog extends JDialog {
	

    static private final String[] DEBUG_BOOLEAN_PROPERTY_NAMES = {

            "DEBUG_DOWNLOAD",

            //-DCSV_DOWNLOAD_DELAY_MILLIS=3000
            //-DNOT_com.diversityarrays.kdxplore.KDXplore.uiMultiplier=1.5

            "ImportTrialCsvDialog.DEBUG",

            "CurationTableModel.DEBUG",

            "com.diversityarrays.dalclient.DalUtil.DEBUG",
            "com.diversityarrays.dalclient.DefaultDALClient.DEBUG",
            "com.diversityarrays.dbmodel.DbData.DEBUG",
            "com.diversityarrays.dalexplorer.views.DalResponseViewer.DEBUG",
            "com.diversityarrays.schemaexplorer.DbSchemaViewController.DEBUG",
            "com.diversityarrays.dalclient.AbstractDalResponse.TIMING",

            "com.diversityarrays.kdxplore.data.DatabaseDataUtils.DEBUG",
            "com.diversityarrays.kdxplore.data.xml.XmlUtils.DEBUG",

            "Step_10_RetrieveItemsForAllSpecimens.MY_DEBUG",
            "Step_20_TransferSeed.MY_DEBUG",
            "Step_30_ValidateEnvelopeOrder.MY_DEBUG",
            
            "Step_10_RetrieveItemsForAllSpecimens.MY_DEBUG",
            
            "CurationData.DEBUG_CALC",
            
            "ParsedHeadingInfo.DEBUG",
            "PlotIdFieldLayoutProcessor.DEBUG",
            "DartEntityBeanInfo.DEBUG",

            "net.pearcan.ui.marker.MarkerViewComponent.DEBUG",
            "DEBUG_MARKER_GET_INDEX_NAME",
            "DEBUG_MARKER_PROPOSALS",
            "DEBUG_MARKER_RIGHT_CLICK",
            
            "FieldView.DEBUG",
            
            "TableColumnResizer.DEBUG",
            "TableRowSizer.DEBUG",

            "RowColumnResizer.DEBUG",
            "TableColumnResizer.DEBUG",
                    

            "HeatMapToolTip.DEBUG",
            
            "COPY_ASIDE_TO",
            
            "log_dalclient",
            
    };

	class DebugOptionTableModel extends BspAbstractTableModel {
		
		final Set<Integer> checked = new HashSet<>();
		final List<String> optionNames = new ArrayList<>();
		
		public DebugOptionTableModel() {
			super("Set", "Name");
		}
		
		public void setOptionNames(Set<String> set) {
			optionNames.clear();
			checked.clear();

			optionNames.addAll(set);
			Collections.sort(optionNames);
			fireTableDataChanged();
		}

		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0: return Boolean.class;
			case 1: return String.class;
			}
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 0;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0 && aValue instanceof Boolean) {
				if ((Boolean) aValue) {
					checked.add(rowIndex);
				}
				else {
					checked.remove(rowIndex);
				}
				fireTableRowsUpdated(rowIndex, rowIndex);
			}
		}

		@Override
		public int getRowCount() {
			return optionNames.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0: return checked.contains(rowIndex);
			case 1: return optionNames.get(rowIndex);
			}
			return null;
		}

	}
	
	private final DebugOptionTableModel tableModel = new DebugOptionTableModel();
	private final JTable table = new JTable(tableModel);

	private Map<String, String> optionToPropertyName = new HashMap<String, String>();

	public DebugDialog() {
		super(null, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final List<String> missingPropertyNames = new ArrayList<String>();
		for (String propertyName : DEBUG_BOOLEAN_PROPERTY_NAMES) {
			// first strip off the property name tail
			int pos = propertyName.lastIndexOf('.');
			if (pos > 0) {
				String className = propertyName.substring(0, pos);

				try {
					// Class.forName(className);
					pos = className.lastIndexOf('.');
					String optionName = propertyName.substring(pos + 1);

					optionToPropertyName.put(optionName, propertyName);
					// } catch (ClassNotFoundException e1) {
					// missingPropertyNames.add(propertyName);
				} finally {

				}
			}
		}
		
		tableModel.setOptionNames(optionToPropertyName.keySet());

		JButton continueButton = new JButton(new AbstractAction("Continue") {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Integer rowIndex : tableModel.checked) {
					String pname = optionToPropertyName.get(tableModel.optionNames.get(rowIndex));
					System.out.println(pname + "=true");
					System.setProperty(pname, "true");
				}
				dispose();
			}
		});
		Container cp = getContentPane();
		cp.add(BorderLayout.NORTH, new JLabel(
				"Select one or more debug options"));
		cp.add(BorderLayout.CENTER, new JScrollPane(table));
		cp.add(BorderLayout.SOUTH, continueButton);
		pack();

		if (!missingPropertyNames.isEmpty()) {
			addWindowListener(new WindowAdapter() {

				@Override
				public void windowOpened(WindowEvent e) {
					removeWindowListener(this);
					String message = StringUtil.join("\n",
							missingPropertyNames);
					JOptionPane.showMessageDialog(DebugDialog.this,
							message, "Missing classes for propertyNames",
							JOptionPane.WARNING_MESSAGE);
				}

			});
		}
	}
}
