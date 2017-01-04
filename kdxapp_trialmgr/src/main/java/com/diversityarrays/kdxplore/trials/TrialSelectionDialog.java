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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.dalclient.DalResponse;
import com.diversityarrays.dalclient.DalResponseException;
import com.diversityarrays.dalclient.DalResponseRecord;
import com.diversityarrays.dalclient.DalResponseRecordVisitor;
import com.diversityarrays.daldb.core.TrialUnit;
import com.diversityarrays.db.DalOperationsManager;
import com.diversityarrays.db.DartEntityBuilder;
import com.diversityarrays.db.ValueConversionProblem;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.beans.DartEntityBeanRegistry;
import com.diversityarrays.kdxplore.beans.DartEntityTableModel;
import com.diversityarrays.kdxplore.data.dal.TrialPlus;
import com.diversityarrays.kdxplore.data.dal.TrialPlusImpl;
import com.diversityarrays.kdxplore.data.tool.ListCommandProcessor;
import com.diversityarrays.kdxplore.data.tool.OkCancelDialog;
import com.diversityarrays.kdxplore.data.util.EntityConverterUtility;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.ui.LoginDialog;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.OptionalCheckboxRenderer;
import com.diversityarrays.util.RunMode;

import net.pearcan.ui.DefaultBackgroundRunner;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.table.TableColumnSelectionButton;
import net.pearcan.ui.widget.CheckSelectionButtonsPanel;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.StringUtil;

public class TrialSelectionDialog extends OkCancelDialog {

	private static final String INITIAL_COLUMNS_TAGNAME = "Initial";

	private static final String GET_TRIALS = "Find Trials";

	private static final String CARD_TRIALS = "TRIALS";

	private static final String CARD_HELP = "HELP";

	private static final String USE_TRIALS = "Download Trials";

	public static final boolean DEBUG = Boolean.getBoolean(TrialSelectionDialog.class.getSimpleName() + ".DEBUG");

	enum CollectPlotsMethod {
		USE_NUM_RECORDS, // does NOT get each TrialUnit
		SIMPLE_LIST, // gets all TrialUnits in one big download
		PAGED_LIST // gets all TrialUnits in multiple pages - so user may be able to cancel 
	}
	

	static public void main(String[] args) {

		Transformer<BackgroundRunner, TrialSearchOptionsPanel> factory = new Transformer<BackgroundRunner, TrialSearchOptionsPanel>() {
			@Override
			public TrialSearchOptionsPanel transform(BackgroundRunner backgroundRunner) {
				return TrialSelectionSearchOptionsPanel.create(backgroundRunner);
			}
		};
		
		boolean test = false;
		if (test) {
			@SuppressWarnings("unchecked")
			TrialSelectionDialog tsd = new TrialSelectionDialog(null, "Test Tree", null, Collections.EMPTY_LIST, //$NON-NLS-1$
					factory);
			//			tsd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			tsd.setVisible(true);
		}
		else {
			
			File propertiesFile = new File(System.getProperty("user.home"), //$NON-NLS-1$
			        "LoginDialog.properties"); //$NON-NLS-1$
			//.getBundle("LoginDialog"); //, Locale.getDefault());

			ResourceBundle bundle = null;

			if (propertiesFile.exists()) {
				try {
					bundle = new PropertyResourceBundle(new FileReader(propertiesFile));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				} 
			}
			
			Preferences loginPreferences = KdxplorePreferences.getInstance().getPreferences();
//			Preferences loginPreferences = Preferences.userNodeForPackage(KdxConstants.class);
			
			LoginDialog ld = new LoginDialog(null, "Login Please", loginPreferences, bundle); //$NON-NLS-1$
			ld.setVisible(true);
			DALClient client = ld.getDALClient();
			if (client!=null) {
				@SuppressWarnings("unchecked")
				TrialSelectionDialog tsd = new TrialSelectionDialog(null, "Test Tree", client, Collections.EMPTY_LIST, //$NON-NLS-1$
						factory);
				//				tsd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				tsd.setVisible(true);
				if (tsd.trialRecords == null) {
					System.out.println("Cancelled !"); //$NON-NLS-1$
				}
				else {
					System.out.println(tsd.trialRecords.length+" chosen: "); //$NON-NLS-1$
					for (TrialPlus tr : tsd.trialRecords) {
						System.out.println("\t"+tr.getTrialId()+": "+tr.getTrialName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
					System.out.println("- - -"); //$NON-NLS-1$
				}
			}
		}
		System.exit(0);
	}

	   
	private class TrialNameCellRenderer extends DefaultTableCellRenderer {
	    
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) 
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String ttt = null;
            Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
            int mrow = table.convertRowIndexToModel(row);
            if (mrow >= 0) {
                TrialPlusImpl tp = trialRecordTableModel.getRecordData(mrow);
                Trial kdxTrial = kdxTrialByIdDownloaded.get(tp.getTrialId());
                if (kdxTrial != null) {
                    if (! tp.getTrialName().equals(kdxTrial.getTrialName())) {
                        fg = Color.RED;
                        ttt = "<HTML>KDDartID=" + tp.getTrialId() + " but KDX Trial Name=[" + 
                                StringUtil.htmlEscape(kdxTrial.getTrialName()) + "]";
                    }
                }
            }
            setForeground(fg);
            setToolTipText(ttt);
            return this;
        }
        
    }

	private DALClient client;

	private JLabel messageLabel = new JLabel();
	private JSplitPane splitPane;
	
	private DartEntityTableModel<TrialPlusImpl,Void> trialRecordTableModel = new DartEntityTableModel<TrialPlusImpl,Void>(
			TrialPlusImpl.class,
			DartEntityBeanRegistry.TRIAL_PLUS_BEAN_INFO,
			DartEntityTableModel.DEFAULT_SELECTED_HEADING) 
	{
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object result = super.getValueAt(rowIndex, columnIndex);
			if (showChosen && columnIndex==0) {
				TrialPlus tp = records.get(rowIndex);
				Trial kdxTrial = kdxTrialByIdDownloaded.get(tp.getTrialId());
				if (kdxTrial != null) {
                    result = null; // hack so that OptionalCheckboxRenderer can do its thing
				}
			}
			return result;
		}
	};
	
	private JTable trialRecordTable = new JTable(trialRecordTableModel);

	private JCheckBox wantTrialUnits = new JCheckBox("Get Plot Counts");

	private String filteringClauseUsed;
	private Action findTrialRecords = new AbstractAction(GET_TRIALS) {
		@Override
		public void actionPerformed(ActionEvent e) {

			if (client==null || ! client.isLoggedIn()) {
				JOptionPane.showMessageDialog(TrialSelectionDialog.this, 
						"DAL Client is not logged in", getTitle(), JOptionPane.WARNING_MESSAGE);
				return;
			}

			String clauseToUse = searchOptionsPanel.getFilteringClause();
			
			TrialCollectionTask task = new TrialCollectionTask(wantTrialUnits.isSelected(), clauseToUse);

			setMessage(""); //$NON-NLS-1$
			trialRecordTableModel.clearRecords();
			backgroundRunner.runBackgroundTask(task);
		}
	};

	public TrialPlus[] trialRecords;
	
	private DefaultBackgroundRunner backgroundRunner = new DefaultBackgroundRunner();

	private final TrialSearchOptionsPanel searchOptionsPanel;

	private final Map<Integer,Trial> kdxTrialByIdDownloaded;

	private SearchOptionsChangeListener searchOptionsListener = new SearchOptionsChangeListener() {
		@Override
		public void choiceChanged() {
		    handleSearchOptionsChoiceChanged();
		}
		
		@Override
		public void lookupsLoaded() {
			findTrialRecords.setEnabled(true);
//			filteringClauseUsed = searchOptionsPanel.getFilteringClause();
		}
	};

	public TrialSelectionDialog(Window owner, String title, DALClient c, 
	        List<Trial> kdxTrials, 
			Transformer<BackgroundRunner, TrialSearchOptionsPanel> searchOptionsPanelFactory)
	{
		super(owner, title, USE_TRIALS);
		
		setGlassPane(backgroundRunner.getBlockingPane());
		
		searchOptionsPanel = searchOptionsPanelFactory.transform(backgroundRunner);
		helpInstructions = new JLabel(searchOptionsPanel.getHtmlHelp(GET_TRIALS));

		kdxTrialByIdDownloaded = kdxTrials.stream()
		    .filter(t -> t.getIdDownloaded()!=null)
		    .collect(Collectors.toMap(Trial::getIdDownloaded, java.util.function.Function.identity()));
		
		trialRecordTable.setName(this.getClass().getName()+".trialRecordTable"); //$NON-NLS-1$
		
		TableColumnModel tcm = trialRecordTable.getColumnModel();
		TableCellRenderer cellRenderer = new OptionalCheckboxRenderer("Already downloaded");
		tcm.getColumn(trialRecordTableModel.getChosenColumnIndex())
		    .setCellRenderer(cellRenderer);
		
		for (int col = tcm.getColumnCount(); --col >= 0; ) {
		    if ("TrialName".equals(trialRecordTable.getColumnName(col))) {
	            TableColumn tc = tcm.getColumn(col);
		        tc.setCellRenderer(new TrialNameCellRenderer());
		        break;
		    }
		}

		findTrialRecords.setEnabled(false);

		searchOptionsPanel.addSearchOptionsChangeListener(searchOptionsListener);
		wantTrialUnits.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filteringClauseUsed = null;
                handleSearchOptionsChoiceChanged();
            }
        });
		
		setDALClient(c);
		
		initialiseGui();
		
		if (owner != null) {
		    Dimension ownerSize = owner.getSize();
		    Dimension mySize = getSize();
		    int w = (ownerSize.width * 3) / 4;
		    int h = (ownerSize.height * 3) / 4;
		    if (w > mySize.width || h > mySize.height) {
		        if (w > mySize.width) {
		            mySize.width = w;
		        }
		        if (h > mySize.height) {
		            mySize.height = h;
		        }
		        setSize(mySize);
		    }
		}
		
		// TODO consider using setSize to increase to parent's width
		getOkAction().setEnabled(false);
		trialRecordTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				getOkAction().setEnabled(trialRecordTableModel.getAnyChosen());
			}
		});
	}

	private void handleSearchOptionsChoiceChanged() {
        String fc = searchOptionsPanel.getFilteringClause();
        if (filteringClauseUsed==null) {
            findTrialRecords.setEnabled(true);
//              findTrialRecords.setEnabled(fc!=null); // this might mean first time always disables
        }
        else {
            findTrialRecords.setEnabled(! filteringClauseUsed.equals(fc));
        }
    }

    @Override
	public void setVisible(boolean b) {
		if (b) {
			setLocationRelativeTo(null);
		}
		super.setVisible(b);
	}
	
	private void setMessage(final String msg) {
		if (! SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					setMessage(msg);
				}
			});
		}
		else {
			messageLabel.setText(msg);
		}
	}
	
	public void setDALClient(DALClient c) {
		this.client = c;

		if (client==null) {
			// Without a client we will show nothing
			trialRecordTableModel.clearRecords();
		}
		searchOptionsPanel.setDALClient(client);
		helpInstructions.setText(searchOptionsPanel.getHtmlHelp(GET_TRIALS));
	}

	@Override
	protected void doPostOpenInitialisation() {
		if (splitPane!=null) {
			splitPane.setDividerLocation(0.3);
		}
	}

	@Override
	protected Component createBottomRowComponent() {
		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalStrut(10));
        buttons.add(wantTrialUnits);
		if (RunMode.getRunMode().isDeveloper()) {
		    wantTrialUnits.setSelected(true);
		}
		buttons.add(new JButton(findTrialRecords));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(getOkAction()));
		buttons.add(new JButton(getCancelAction()));
		buttons.add(Box.createHorizontalStrut(10));

		return buttons;
	}
	
	private final JLabel helpInstructions;
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	@SuppressWarnings("rawtypes")
	@Override
	protected Component createMainPanel() {
		
		CheckSelectionButtonsPanel csbp = new CheckSelectionButtonsPanel(CheckSelectionButtonsPanel.ALL, trialRecordTable);
		csbp.addUncheckAllActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trialRecordTableModel.clearChosen();
			}
		});
		csbp.addCheckAllActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trialRecordTableModel.chooseAll();
			}
		});
		csbp.addCheckSelectedActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(trialRecordTable);
				trialRecordTableModel.addChosenRows(selectedModelRows);
			}
		});
		csbp.addCheckUnselectedActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Set<Integer> selectedModelRows = new HashSet<Integer>(GuiUtil.getSelectedModelRows(trialRecordTable));
				if (selectedModelRows.isEmpty()) {
					trialRecordTableModel.chooseAll();
				}
				else {
					List<Integer> rowsToChoose = new ArrayList<Integer>();
					for (int mrow = trialRecordTable.getRowCount(); --mrow >= 0; ) {
						if (! selectedModelRows.contains(mrow)) {
							rowsToChoose.add(mrow);
						}
					}
					trialRecordTableModel.addChosenRows(rowsToChoose);
				}
			}
		});



		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JLabel("Select one or more and click '"+USE_TRIALS+"'"));
		buttons.add(csbp);
		buttons.add(Box.createHorizontalGlue());

		List<TableColumn> columns = DartEntityTableModel.collectNamedColumns(trialRecordTable, trialRecordTableModel, true,
				"TrialName","Site","# Plots","# Measurements","Design","Manager","TrialType","Project","Start Date");
//		List<TableColumn> columns = DartEntityTableModel.collectNonExpertColumns(trialRecordTable, trialRecordTableModel, true);
		
		Map<String,TableColumn[]> choices = new HashMap<String,TableColumn[]>();
		choices.put(INITIAL_COLUMNS_TAGNAME, columns.toArray(new TableColumn[columns.size()]));
		
		TableColumnSelectionButton tcsb = new TableColumnSelectionButton(trialRecordTable, choices);
		tcsb.setSelectedColumns(INITIAL_COLUMNS_TAGNAME);
		
		trialRecordTable.setRowSorter(new TableRowSorter<DartEntityTableModel>(trialRecordTableModel));

		JScrollPane scrollPane = new JScrollPane(trialRecordTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, tcsb);

		JPanel trialsPanel = new JPanel(new BorderLayout());
		trialsPanel.add(messageLabel, BorderLayout.NORTH);
		trialsPanel.add(scrollPane, BorderLayout.CENTER);
		trialsPanel.add(buttons, BorderLayout.SOUTH);

		cardPanel.add(helpInstructions, CARD_HELP);
		cardPanel.add(trialsPanel, CARD_TRIALS);
		cardLayout.show(cardPanel, CARD_HELP);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				searchOptionsPanel.getViewComponent(),
				cardPanel);
		
		return splitPane;
	}

	@Override
	protected boolean handleCancelAction() {
		trialRecords = null;
		return true;
	}

	@Override
	protected boolean handleOkAction() {
		trialRecords = trialRecordTableModel.getChosenDataRecords();
		return true;
	}

	class TrialCollectionTask extends BackgroundTask<List<TrialPlusImpl>,Void> {
		
		private final String TAG = TrialCollectionTask.class.getSimpleName();
		
		private final CollectPlotsMethod collectPlotsMethod = CollectPlotsMethod.USE_NUM_RECORDS;
		private final boolean collectPlotCounts;
		private int plotsCollectedCount = -1;

		private final String filterClause;
		private boolean cancelled;

		TrialCollectionTask(boolean collectPlotCounts, String filterClause) {
			super("Collecting Trials...", true);

			this.collectPlotCounts = collectPlotCounts;
			this.filterClause = filterClause;
		}

		@Override
		public List<TrialPlusImpl> generateResult(Closure<Void> publishPartial) throws Exception {
			trialRecordTableModel.clearRecords();

			final int pageSize = 200; // TODO make this a parameter
			ListCommandProcessor lcp = new ListCommandProcessor("list/trial/_nperpage/page/_num"); //$NON-NLS-1$

			DalOperationsManager dalOperationsManager = null; // TODO remove
			final DartEntityBuilder<TrialPlusImpl> trialBuilder = new DartEntityBuilder<TrialPlusImpl>(TrialPlusImpl.class, dalOperationsManager);
			
			final List<TrialPlusImpl> records = new ArrayList<TrialPlusImpl>();
			DalResponseRecordVisitor visitor = new DalResponseRecordVisitor() {
				@Override
				public boolean visitResponseRecord(String resultTagName, DalResponseRecord record) {
					List<ValueConversionProblem> problems = new ArrayList<ValueConversionProblem>();
					TrialPlusImpl tr = trialBuilder.build(record.rowdata, null, problems);	
					if (tr != null) {
	                    records.add(tr);                
	                    if (DEBUG) {
	                        for(String s : record.rowdata.keySet()){
	                            Shared.Log.d(TAG, "visitResponseRecord: Columns ------------------- "+s); //$NON-NLS-1$
	                        }
	                    }	                    
					}
					return ! backgroundRunner.isCancelRequested();
				}
			};


			Shared.Log.i(TAG, "Collecting Trials with filter="+filterClause); //$NON-NLS-1$
			cancelled = ! lcp.visitAllWithFilter(client, visitor, filterClause, pageSize, backgroundRunner);
			
			if (! cancelled) {
				Shared.Log.i(TAG, "Collected "+records.size() + " Trials"); //$NON-NLS-1$ //$NON-NLS-2$
				
				if (collectPlotCounts && ! records.isEmpty()) {
					retrieveTrialUnits(records);
				}
			}

			return records;
		}
		
		private void retrieveTrialUnits(final List<TrialPlusImpl> records) {
			
			DartEntityBuilder<TrialUnit> trialUnitBuilder = null;
			if (CollectPlotsMethod.USE_NUM_RECORDS != collectPlotsMethod) {
				DalOperationsManager dalOperationsManager = null;
				trialUnitBuilder = new DartEntityBuilder<TrialUnit>(TrialUnit.class, dalOperationsManager);
			}

			backgroundRunner.setProgressRange(0, records.size());
			
			plotsCollectedCount = 0;
			for (TrialPlusImpl trial : records) {
			    if (trial == null || trial.getTrialId() == null) {
			        continue;
			    }
				int trialId = trial.getTrialId();
				
				backgroundRunner.setProgressString("Get Plot count for (#" + trialId + ") " + trial.getTrialName());
				
				switch (collectPlotsMethod) {
				case PAGED_LIST:
					collectTrialUnitsUsingPagedList(trialUnitBuilder, trial);
					break;
				case SIMPLE_LIST:
					collectTrialUnitsUsingSimpleList(trialUnitBuilder, trial);
					break;
				case USE_NUM_RECORDS:
					collectTrialUnitCounts(trial);
					break;
				default:
					throw new RuntimeException("Unsupported method " + collectPlotsMethod); //$NON-NLS-1$
				}
				backgroundRunner.setProgressValue(++plotsCollectedCount);

				cancelled = backgroundRunner.isCancelRequested();
				if (cancelled) {
					break;
				}
			}
		}

		@Override
		public void onCancel(CancellationException e) {
			MsgBox.error(TrialSelectionDialog.this, 
					"Cancelled by user", 
					getTitle()+": Cancelled");
		}

		@Override
		public void onException(Throwable cause) {
			MsgBox.error(TrialSelectionDialog.this, 
					cause.getMessage(), 
					getTitle()+": Error");
		}

		@Override
		public void onTaskComplete(List<TrialPlusImpl> records) {
			filteringClauseUsed = filterClause;
			findTrialRecords.setEnabled(false);

			StringBuilder sb = new StringBuilder();
			if (cancelled) {
				sb.append("Cancelled after ").append(records.size()).append(" Trials found");
				
				if (collectPlotCounts) {
					if (plotsCollectedCount > 0) {
						sb.append(" and Plot Counts retrieved for ").append(plotsCollectedCount);
					}					
				}
			}
			else {
				sb.append("Found ").append(records.size()).append(" Trials");
			}
			setMessage(sb.toString());

			if (! records.isEmpty()) {
				trialRecordTableModel.addRecordsList(records);
			}
			cardLayout.show(cardPanel, CARD_TRIALS);
			GuiUtil.initialiseTableColumnWidths(trialRecordTable);
		}

		private void collectTrialUnitCounts(TrialPlusImpl trial) {
			// We only want the NUM_OF_RECORDS so use pageSize==1
			String cmd = "trial/" + trial.getTrialId() + "/list/trialunit/1/page/1";  //$NON-NLS-1$//$NON-NLS-2$
			if  (DEBUG) {
				Shared.Log.d(TAG, "collectTrialUnitCounts(" + trial + ") cmd=" + cmd); //$NON-NLS-1$ //$NON-NLS-2$
			}
			try {
				DalResponse response = client.performQuery(cmd);
				String numOfRecords = response.getRecordFieldValue(
						DALClient.TAG_PAGINATION, DALClient.ATTR_NUM_OF_RECORDS);
				try {
					int nPlots = Integer.parseInt(numOfRecords);
					trial.setPlotsCount(nPlots);
				} catch (NumberFormatException e) {
					Shared.Log.e(TAG, 
							"collectTrialunitCounts: " + cmd + "\nBAD numOfRecords=" + numOfRecords); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (DalResponseException | IOException e) {
				Shared.Log.e(TAG, "collectTrialUnitCounts: " + cmd, e); //$NON-NLS-1$
			}
		}
		
		private void collectTrialUnitsUsingSimpleList(
				final DartEntityBuilder<TrialUnit> trialUnitBuilder,
				TrialPlusImpl trial) 
		{
			String cmd = "trial/" + trial.getTrialId() + "/list/trialunit";  //$NON-NLS-1$//$NON-NLS-2$
			if  (DEBUG) {
				Shared.Log.d(TAG, "collectTrialUnitsUsingSimpleList: cmd=" + cmd); //$NON-NLS-1$
			}
			List<Plot> plots = new ArrayList<>();
			
			try {
				DalResponse response = client.performQuery(cmd);
				DalResponseRecordVisitor visitor = new DalResponseRecordVisitor() {
					@Override
					public boolean visitResponseRecord(String tag, DalResponseRecord rr) {
						List<ValueConversionProblem> problems = new ArrayList<ValueConversionProblem>();
						TrialUnit tu = trialUnitBuilder.build(rr.rowdata, null, problems);			
						plots.add(EntityConverterUtility.makePlotFromTrialUnit(tu));				
						return true;
					}
				};
			
				response.visitResults(visitor);
				trial.setPlots(plots);
			} catch (DalResponseException | IOException e) {
				Shared.Log.e(TAG, "collectTrialUnitsUsingSimpleList: cmd=" + cmd, e); //$NON-NLS-1$
			}
		}
		

		private void collectTrialUnitsUsingPagedList(
				final DartEntityBuilder<TrialUnit> trialUnitBuilder,
				TrialPlusImpl trial) 
		{
			String cmd = "trial/" + trial.getTrialId()  + "/list/trialUnit/_nperpage/page/_num";  //$NON-NLS-1$//$NON-NLS-2$
			if  (DEBUG) {
				Shared.Log.d(TAG, "collectTrialUnitsUsingPagedList: cmd=" + cmd); //$NON-NLS-1$
			}

			try {
				List<Plot> plots = new ArrayList<>();
				ListCommandProcessor lcp = new ListCommandProcessor(cmd);
				DalResponseRecordVisitor trialUnitVisitor = new DalResponseRecordVisitor() {
					@Override
					public boolean visitResponseRecord(String resultTagName, DalResponseRecord record) {
						List<ValueConversionProblem> problems = new ArrayList<ValueConversionProblem>();
						TrialUnit tu = trialUnitBuilder.build(record.rowdata, null, problems);			
						plots.add(EntityConverterUtility.makePlotFromTrialUnit(tu));				
						return true;
					}
				};
				lcp.visitAll(client, trialUnitVisitor, 1000);
				trial.setPlots(plots);
				
			} catch (DalResponseException | IOException e) {
				Shared.Log.e(TAG, "collectTrialUnitsUsingPagedList: " + cmd, e); //$NON-NLS-1$
			}
		}
		
	}
}
