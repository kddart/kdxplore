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
package com.diversityarrays.kdxplore.trialdesign;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.util.BoxBuilder;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.UnicodeChars;

import au.com.bytecode.opencsv.CSVReader;
import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.util.Util;

public class AlgorithmOutputViewer extends JFrame {

    private final Action closeAction = new AbstractAction("Close") {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };

    private final Action openFolderAction = new AbstractAction("Open Output Folder") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Desktop.getDesktop().open(algorithmRunResult.getAlgorithmFolder());
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    };

    private final AlgorithmRunResult algorithmRunResult;

	public AlgorithmOutputViewer(AlgorithmRunResult arr, File file) {
	    super(RunMode.getRunMode().isDeveloper() ? file.getName()
	    		: "Output of " + arr.getAlgorithmName());

	    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

	    this.algorithmRunResult = arr;

		List<String> headings = null;
		List<String[]> dataLines = new ArrayList<>();
		CSVReader csvReader = null;
		try {
		    csvReader = new CSVReader(new FileReader(file));
			int lineCount = 0;
			String[] line;
			while (null != (line = csvReader.readNext())) {
			    if (++lineCount == 1) {
	                headings = Arrays.asList(line);
			    }
			    else {
			        dataLines.add(line);
			    }
			}
		} catch (IOException ignore) {
		} finally {
		    if (csvReader != null) {
		        try { csvReader.close(); } catch (IOException ignore) {}
		    }
		}

		OutputTableModel model = new OutputTableModel(headings, dataLines);
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		TableTransferHandler.initialiseForCopySelectAll(table, true);
		JScrollPane scrollPane = new JScrollPane(table);

		StringBuilder sb = new StringBuilder("<HTML>");
		if (RunMode.getRunMode().isDeveloper()) {
		    sb.append("Folder: ").append(file.getParent());
		}
		else {
		    if (KdxploreConfig.getInstance().isBeta()) {
		        sb.append("In this Beta release, ");
		    }
		    sb.append("Algorithm Output is not yet available in the Field Design editor.")
		        .append("<BR>However, the output is displayed here so you can check the output and/or copy it for use elsewhere");
		}

		scrollPane.setBorder(new EmptyBorder(4, 4, 4, 4));

		Box box;
		if (RunMode.getRunMode().isDeveloper()) {
		    JLabel warning = new JLabel("Output folder will be removed on close");
		    warning.setForeground(Color.RED);

            box = BoxBuilder.horizontal().add(warning, 0, openFolderAction, 0, closeAction, 10).get();
		}
		else {
			String cmd = Util.isMacOS() ? UnicodeChars.MAC_COMMAND_KEY : "^";
			JLabel help = new JLabel(String.format("Use %sA, %sC to copy to clipboard", cmd, cmd));
		    box = BoxBuilder.horizontal().add(4, help, 0, closeAction, 10).get();
		}

		JLabel info = new JLabel(sb.toString());
		info.setBorder(new EmptyBorder(2,2,2,2));

		Container cp = getContentPane();
		cp.add(info, BorderLayout.NORTH);
		cp.add(scrollPane, BorderLayout.CENTER);
		cp.add(box, BorderLayout.SOUTH);
		pack();
	}

	class OutputTableModel extends AbstractTableModel{

	    private final List<String> headings;
        private final List<String[]> dataLines;

        private final Class<?>[] columnClasses;

        public OutputTableModel(List<String> headings, List<String[]> dataLines) {
	        this.headings = headings;
	        this.dataLines = dataLines;

	        columnClasses = new Class<?>[getColumnCount()];

	        for (int column = getColumnCount(); --column >= 0; ) {
	        	Class<?> clz = Integer.class;
	        	for (int row = getRowCount(); --row >= 0; ) {
	        		String s = getStringAt(row, column);
	        		if (Check.isEmpty(s)) {
	        			clz = String.class;
	        			break;
	        		}
	        		try {
	        			// Checking if it is Integer
						Integer.valueOf(s);
					} catch (NumberFormatException ignore) {
	        			clz = String.class;
	        			break;
					}
	        	}

	        	columnClasses[column] = clz;
	        }
        }

        @Override
	    public int getRowCount() {
	        return dataLines.size();
	    }

	    @Override
	    public int getColumnCount() {
	        return headings.size();
	    }


	    @Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
	    public String getColumnName(int column) {
	        return headings.get(column);
	    }

	    private String getStringAt(int rowIndex, int columnIndex) {
	        String[] columnData = dataLines.get(rowIndex);
	        return columnIndex < columnData.length ? columnData[columnIndex] : null;
	    }

	    @Override
	    public Object getValueAt(int rowIndex, int columnIndex) {
	    	String result = getStringAt(rowIndex, columnIndex);
	    	if (Integer.class == getColumnClass(columnIndex)) {
	    		if (Check.isEmpty(result)) {
	    			return null;
	    		}
	    		try {
					return Integer.valueOf(result);
				} catch (NumberFormatException ignore) {
				}
	    	}
	    	return result;
	    }
	}
}
