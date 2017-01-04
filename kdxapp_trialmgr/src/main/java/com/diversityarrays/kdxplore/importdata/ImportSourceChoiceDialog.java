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
package com.diversityarrays.kdxplore.importdata;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;

import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.ExportFor;
import com.diversityarrays.kdsmart.kdxs.PackageImportException;
import com.diversityarrays.kdsmart.kdxs.WorkPackage;
import com.diversityarrays.kdsmart.kdxs.WorkPackageImportHelper;
import com.diversityarrays.kdsmart.kdxs.WorkPackageImporter;
import com.diversityarrays.kdx2s.kdsimport.KdxploreWorkPackageImportHelper;
import com.diversityarrays.kdx2s.kdsimport.KdxploreWorkPackageImporter;
import com.diversityarrays.kdx2s.util.DeviceAndOperatorPanel;
import com.diversityarrays.kdx2s.util.DevicesAndOperators;
import com.diversityarrays.kdx2s.util.Msg;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.SourceChoiceHandler.SourceChoice;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel.FileImportResult;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel.ImportFileGroup;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel.ImportState;
import com.diversityarrays.kdxplore.curate.kdsimport.FileImportTableModel.ImportType;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.jdbc.KdxploreConfigException;
import com.diversityarrays.kdxplore.data.kdx.DeviceAndOperator;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.importdata.bms.BmsExcelImportCallable;
import com.diversityarrays.kdxplore.importdata.bms.BmsImportOptions;
import com.diversityarrays.kdxplore.importdata.bms.BmsXlsTrialImportResult;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.MessagesPanel;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.CompoundMessagePrinter;
import net.pearcan.util.GBH;
import net.pearcan.util.MessagePrinter;

public class ImportSourceChoiceDialog extends JDialog {
	
	private final FileImportTableModel fileImportTableModel = new FileImportTableModel();
	private final JTable fileImportTable = new JTable(fileImportTableModel);

//	private boolean deviceIdOk;
	
	private final Action browseAction = new AbstractAction("Add Files...") {
		@Override
		public void actionPerformed(ActionEvent e) {
		    File[] files = KdxplorePreferences.getInstance().askInputFiles(
		            ImportSourceChoiceDialog.this,
		            "Select KDXchange Files", 
		            Shared.KDX_FILTER);
		    if (files != null && files.length > 0) {
                fileImportTableModel.addFiles(Arrays.asList(files));
		    }
		}
	};
	
	private final Action importAction = new AbstractAction("Import") {
		@Override
		public void actionPerformed(ActionEvent ae) {
			
			if (lowcaseTrialNames == null) {
				try {
					lowcaseTrialNames = kdxDatabase.getTrials().stream()
					        .map(trial -> trial.getTrialName().toLowerCase())
					        .collect(Collectors.toSet());
				} catch (IOException e) {
					GuiUtil.errorMessage(ImportSourceChoiceDialog.this, 
							"Unable to get existing Trial Names:\n" + e.getMessage(), 
							getTitle());
					return;
				}
			}
			
			List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(fileImportTable);
			if (! selectedModelRows.isEmpty()) {
				List<File> files = fileImportTableModel.getFiles(selectedModelRows);
				if (! files.isEmpty()) {

					String operatorName = devAndOpPanel.getOperatorName();
					
					Map<DeviceType,List<File>> filesByDeviceType = 
							fileImportTableModel.getDeviceTypesRequiredForSelectedFiles(fileImportTable);

					Map<DeviceAndOperator,List<File>> filesByDeviceAndOperator = new HashMap<>();
					int totalFileCount = 0;
					
					for (DeviceType dt : filesByDeviceType.keySet()) {
						List<File> list = filesByDeviceType.get(dt);

						DeviceAndOperator devAndOp = null;
						switch (dt) {
						case DATABASE:
							devAndOp = new DeviceAndOperator(databaseDeviceIdentifier, operatorName);
							break;
							
						case KDSMART:
							DeviceIdentifier did = devAndOpPanel.getDeviceIdentifier();
							if (! DeviceType.KDSMART.equals(did.getDeviceType())) {
								showErrorMessage("KDSMART device required",
										"File " + list.get(0).getName() + "\n"
										+ "Device '" + did.getDeviceName() + "'");
								return;
							}
							devAndOp = new DeviceAndOperator(did, operatorName);
							break;
							
						default:
							showErrorMessage("Internal Error", 
									new IllegalArgumentException("Unsupported Device type: " + dt));
							return;
						}
						
						if (devAndOp != null) {
							filesByDeviceAndOperator.put(devAndOp, list);
							totalFileCount += list.size();
						}
					}
					
					BmsImportOptions options = bmsOptionsPanel.getBmsImportOptions();
					
					ImportTask task = new ImportTask(options, filesByDeviceAndOperator);
					backgroundRunner.setProgressRange(0,  totalFileCount);
					backgroundRunner.runBackgroundTask(task);
				}
			}
		}
	};
	
	class ImportTask extends BackgroundTask<List<FileImportResult>,FileImportResult> {

		boolean cancelled = false;
		
		private final BmsImportOptions options;
		private final Map<DeviceAndOperator,List<File>> filesByDeviceAndOperator;
		
		private final Set<File> processed = new HashSet<>();
		private final List<Pair<File,Trial>> completedImports = new ArrayList<>();

		public ImportTask(BmsImportOptions options,
				Map<DeviceAndOperator,List<File>> filesByDeviceAndOperator) 
		{
			super("Importing...", true);
			
			this.options = options;
			this.filesByDeviceAndOperator = filesByDeviceAndOperator;
		}

		@Override
		public List<FileImportResult> generateResult(Closure<FileImportResult> progress) throws Exception {

			List<FileImportResult> result = new ArrayList<>();

			busy = true;
			try {

				int count = 0;
				
				for (DeviceAndOperator devAndOp : filesByDeviceAndOperator.keySet()) {
					
					final DeviceType deviceType = devAndOp.deviceIdentifier.getDeviceType();
					
					List<File> files = filesByDeviceAndOperator.get(devAndOp);
					
					for (File file : files) {
						
						backgroundRunner.setProgressString(file.getName());
						FileImportResult ir = new FileImportResult(file);
						ir.importState = ImportState.WORKING;
						progress.execute(ir);

						messagePrinter.println("Importing '" + file.getName() + "'");
						messagePrinter.println("  (in " + file.getParent() + ")");
						
						fileImportTableModel.setImportState(file, ImportState.WORKING);
						String lowFileName = file.getName().toLowerCase();
						
						Either<Throwable,Trial> either = null;
						if (lowFileName.endsWith(".xls")) {
							// This pairing matches DeviceType.getDeviceTypeFor
							if (DeviceType.DATABASE==deviceType) {
							    if (RunMode.getRunMode().isDeveloper()) {
	                              either = doImportBmsExcel(file, devAndOp, options);
							    }
							    else {
	                                either = Either.left(new Exception("Excel import not supported in this version"));
							    }
							}
							else {
								either = Either.left(new IllegalArgumentException(
										"Expected DeviceType DATABASE but got " + deviceType));
							}
						}
						else if (lowFileName.endsWith(ExportFor.KDX_SUFFIX)) {
							// This pairing matches DeviceType.getDeviceTypeFor
							if (DeviceType.KDSMART==deviceType) {
								either = doImportKdsmartWorkPackage(file, devAndOp);
							}
							else {
								either = Either.left(new IllegalArgumentException(
										"Expected DeviceType KDSMART but got " + deviceType));
							}
						}
						else {
							either = Either.left(new IllegalArgumentException("Unsupported file: " + file.getName()));
						}
						
						if (either.isRight()) {
							ir.importState = ImportState.IMPORTED;
							Trial trial = either.right();
							if (trial != null && trial.getTrialId() > 0) {
								completedImports.add(new Pair<>(file, trial));
							}
						}
						else {
							ir.importState = ImportState.FAILED;
							ir.error = either.left();
							messagePrinter.println(ir.error);
						}
						
						progress.execute(ir);
						
						++count;
						backgroundRunner.setProgressValue(count);

						processed.add(ir.file);
						
						if (backgroundRunner.isCancelRequested()) {
							cancelled = true;
							break;
						}
					}
				}

			}
			finally {
				busy = false;
			}
			return result;
		}
		

		@Override
		public void processPartial(List<FileImportResult> completed) {
			
			FileImportResult last = null;
			for (FileImportResult ir : completed) {
				fileImportTableModel.setImportResult(ir.file, ir);
				last = ir;
			}

			if (last != null) {
				backgroundRunner.setProgressString(last.file.getName());
			}
		}

		@Override
		public void onCancel(CancellationException ce) {
			messagePrinter.println("**Import Cancelled**");
			finish();
		}

		@Override
		public void onException(Throwable t) {
			messagePrinter.println(t);
			finish();
		}

		@Override
		public void onTaskComplete(List<FileImportResult> list) {
			for (FileImportResult ir : list) {
				if (! processed.contains(ir.file)) {
					fileImportTableModel.setImportResult(ir.file, ir);
				}
			}
			if (cancelled) {
				messagePrinter.println("**Import Cancelled**");
			}
			finish();
		}
		
		private void finish() {
			List<Trial> trials = new ArrayList<>();

			for (Pair<File,Trial> pair : completedImports) {
				File file = pair.first;
				Trial trial = pair.second;
				trials.add(trial);

				StringBuilder sb = new StringBuilder("Completed import of ");
				sb.append(file.getName()).append("\nas '").append(trial.getTrialName()).append("'");
				String a = trial.getTrialAcronym();
				if (a != null && ! a.isEmpty()) {
					sb.append("\n  aliased as '").append(a).append("'");
				}
				messagePrinter.println(sb.toString());
			}
			
			onTrialsLoaded.execute(trials);
			
			fileImportTable.clearSelection();
			updateImportAction();
		}
	}
	
	
	private final FileDrop fileDrop = new FileDrop() {
		@Override
		public void dropFiles(Component comp, List<File> files, DropLocationInfo arg2) {
			
			Closure<String> reportError = new Closure<String>() {
                @Override
                public void execute(String s) {
                    messagePanel.println(s);
                }
			    
			};
            Map<ImportType, ImportFileGroup> groupByType = FileImportTableModel.classifyFiles(files, reportError);
            
            List<File> unsupportedFiles = new ArrayList<>();
            
            for (ImportType it : importTypes) {
                ImportFileGroup group = groupByType.remove(it);
                if (group != null) {
                    fileImportTableModel.addFiles(group.files);
                }
            }
            
            for (ImportType it : groupByType.keySet()) {
                unsupportedFiles.addAll(groupByType.get(it).files);
            }
            
			if (! Check.isEmpty(unsupportedFiles)) {
                String msg = unsupportedFiles.stream()
                        .map(File::getName)
                        .collect(Collectors.joining("\n"));

                messagePanel.println(msg);

                MsgBox.info(ImportSourceChoiceDialog.this, msg, "Unsupported Files");
			}
		}
	};
	
	private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);
	
	private final MessagesPanel messagePanel = new MessagesPanel("Messages");
	
	private final DeviceIdentifier databaseDeviceIdentifier;
	
	
	private final DeviceAndOperatorPanel devAndOpPanel;
	private final ChangeListener devAndOpChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			updateImportAction();
		}		
	};
	
	private final KdxploreDatabase kdxDatabase;
	private final KDSmartDatabase database;
	
	private boolean busy = false;
	
	private Set<String> lowcaseTrialNames;
	
	private JLabel errorMessage = new JLabel();
//	private StatusInfoLine statusInfoLine = new StatusInfoLine();
	
	private final BackgroundRunner backgroundRunner;
	
	private final BmsOptionsPanel bmsOptionsPanel = new BmsOptionsPanel();

	private final CompoundMessagePrinter messagePrinter;

	private final Closure<List<Trial>> onTrialsLoaded;
    private final SourceChoice sourceChoice;
    private final Predicate<DeviceIdentifier> predicate;

    private final ImportType[] importTypes;
	public ImportSourceChoiceDialog(SourceChoice sc,
	        Window owner,
			KdxploreDatabase kdxdb, 
			MessagePrinter mp,
			Closure<List<Trial>> onTrialsLoaded,
			BackgroundRunner backgroundRunner) 
	throws IOException, KdxploreConfigException 
	{
		super(owner, "Load Trial Data", ModalityType.APPLICATION_MODAL);
		
        this.sourceChoice = sc;
        this.kdxDatabase = kdxdb;
        this.databaseDeviceIdentifier = kdxDatabase.getDatabaseDeviceIdentifier();
        this.database = kdxDatabase.getKDXploreKSmartDatabase();
        this.backgroundRunner = backgroundRunner;
        this.messagePrinter = new CompoundMessagePrinter(mp, messagePanel);
        this.onTrialsLoaded = onTrialsLoaded;
        
        DevicesAndOperators devsAndOps = new DevicesAndOperators(System.getProperty("user.name")); //$NON-NLS-1$
        devAndOpPanel = new DeviceAndOperatorPanel(kdxdb, devsAndOps, true);
        devAndOpPanel.addChangeListener(devAndOpChangeListener);
        // Note: devAndOpPanel.initialise() is done in WindowListener.windowOpened() below 

        StringBuilder sb = new StringBuilder("Drag/Drop ");
        ImportType[] tmp = null;

        switch (sourceChoice) {
        case CSV:
            predicate = new Predicate<DeviceIdentifier>() {
                @Override
                public boolean evaluate(DeviceIdentifier devid) {
                    return devid!=null && DeviceIdentifier.PLEASE_SELECT_DEVICE_TYPE != devid.getDeviceType();
                }
            };
            sb.append("CSV");
            tmp = new ImportType[] { ImportType.CSV };
            break;
        case KDX:
            predicate = new Predicate<DeviceIdentifier>() {
                @Override
                public boolean evaluate(DeviceIdentifier devid) {
                    if (devid==null || DeviceIdentifier.PLEASE_SELECT_DEVICE_TYPE==devid.getDeviceType()) {
                        return false;
                    }
                    return DeviceType.KDSMART.equals(devid.getDeviceType());
                }
            };
            sb.append(".KDX");
            tmp = new ImportType[] { ImportType.KDX };
            break;
        case XLS:
            devAndOpPanel.disableAddDevice();
            predicate = new Predicate<DeviceIdentifier>() {
                @Override
                public boolean evaluate(DeviceIdentifier devid) {
                    if (devid==null || DeviceIdentifier.PLEASE_SELECT_DEVICE_TYPE==devid.getDeviceType()) {
                        return false;
                    }
                    return DeviceType.DATABASE.equals(devid.getDeviceType());
                }
            };
            sb.append("Excel");
            tmp = new ImportType[] { ImportType.KDXPLORE_EXCEL, ImportType.BMS_EXCEL };
            break;
        case DATABASE:
        default:
            throw new IllegalStateException("sourceChoice=" + sourceChoice.name());
        }
        importTypes = tmp;
        if (importTypes == null) {
            throw new IllegalArgumentException(sourceChoice.name());            
        }
        
        sb.append(" files here");
        String prompt = sb.toString();
		
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		Container cp = getContentPane();
		
		PromptScrollPane pscrollPane = new PromptScrollPane(fileImportTable, prompt);
		pscrollPane.setTransferHandler(flth);
		fileImportTable.setTransferHandler(flth);
		
		fileImportTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//		fileImportTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		fileImportTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					updateImportAction();
				}
			}
		});

		final JSplitPane vSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				pscrollPane, messagePanel);
		
		updateImportAction();

		
		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalStrut(4));
		buttons.add(new JButton(importAction));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(errorMessage);
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(browseAction));
		buttons.add(Box.createHorizontalStrut(4));

		errorMessage.setForeground(Color.RED);
		
		
		JPanel top = new JPanel();
		GBH gbh = new GBH(top, 2,2,2,2);
		int y = 0;
		
		gbh.add(0,y, 1,1, GBH.BOTH, 1,1, GBH.CENTER, devAndOpPanel);
		++y;
		
		if (RunMode.getRunMode().isDeveloper()) {
		    // Only Developer gets to see the Excel options panel (for now).
			gbh.add(0,y, 3,1, GBH.BOTH, 2,2, GBH.CENTER, bmsOptionsPanel);
			++y;
		}

		gbh.add(0,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, buttons);
		++y;
		
		cp.add(top, BorderLayout.NORTH);
		cp.add(vSplit, BorderLayout.CENTER);

		pack();
		
		GuiUtil.centreOnOwner(ImportSourceChoiceDialog.this);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				vSplit.setDividerLocation(0.5);
				
				// NO_BMS
                bmsOptionsPanel.setVisible(false /* SourceChoice.XLS == sourceChoice */);

				List<Pair<String, Exception>> errors = devAndOpPanel.initialise(predicate);
				if (errors.isEmpty()) {
					List<String> kdxFileNamesWithoutSuffix = new ArrayList<>();
					for (int rowIndex = fileImportTableModel.getRowCount(); --rowIndex >= 0; ) {
						File file = fileImportTableModel.getFileAt(rowIndex);
						String fname = file.getName();
						int dotpos = fname.lastIndexOf('.');
						if (dotpos > 0) {
							String sfx = fname.substring(dotpos);
							if (ExportFor.KDX_SUFFIX.equalsIgnoreCase(sfx)) {
								kdxFileNamesWithoutSuffix.add(fname.substring(0, dotpos));
							}
						}
					}

					if (! kdxFileNamesWithoutSuffix.isEmpty()) {
						devAndOpPanel.selectInitialDeviceIdentifier(kdxFileNamesWithoutSuffix);
					}
				}
				else {
					for (Pair<String,Exception> pair : errors) {
						messagePrinter.println(pair.first + ":");
						messagePrinter.println(pair.second.getMessage());
					}
				}
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				if (busy) {
					GuiUtil.beep();
				}
				else {
					dispose();
				}
			}
		});
	}

	protected void showErrorMessage(String title, Object msg) {
		String message;
		if (msg instanceof Throwable) {
			Throwable e = (Throwable) msg;
			message = e.getMessage();
			if (Check.isEmpty(message)) {
				message = "Error: " + e.getClass().getName();
			}
		}
		else {
			message = msg.toString();
		}
		JOptionPane.showMessageDialog(ImportSourceChoiceDialog.this,
				message, title, 
				JOptionPane.ERROR_MESSAGE);
	}

	private Either<Throwable, Trial> doImportKdsmartWorkPackage(
			File file, DeviceAndOperator deviceAndOperator)
	{
		
		WorkPackageImportHelper helper = new KdxploreWorkPackageImportHelper(
				kdxDatabase.getKDXploreKSmartDatabase(),
				messagePrinter);
				
		WorkPackageImporter importer = new KdxploreWorkPackageImporter(
				kdxDatabase, deviceAndOperator, helper);
		
		try {
			WorkPackage workPackage = importer.importFile(file);
			if (workPackage.databaseTrial==null) {
				StringBuilder sb = new StringBuilder("Errors: ");
				sb.append(workPackage.errors.size());
				for (String e : workPackage.errors) {
					sb.append('\n').append(e);
				}
				messagePrinter.println("Problem importing " + file.getName());
				messagePrinter.println(sb);
				return Either.left(new PackageImportException(sb.toString()));				
			}
			return Either.right(workPackage.databaseTrial);
		} catch (PackageImportException e) {
			messagePrinter.println("Problem importing " + file.getName());
			messagePrinter.println(e.getMessage());
			return Either.left(e);
		}
	}

	private Either<Throwable,Trial> doImportBmsExcel(File file, DeviceAndOperator devAndOp, BmsImportOptions options) {
		
		Either<Throwable,Trial> result;
		
		int traitInstanceNumber = 1;
		BmsExcelImportCallable callable = new BmsExcelImportCallable(
				kdxDatabase.getKDXploreKSmartDatabase(), 
				file, 
				devAndOp, 
				traitInstanceNumber, 
				options,
				lowcaseTrialNames);

		Either<Exception,Either<Throwable,BmsXlsTrialImportResult>> either = database.doBatch(callable);
		
		if (either.isRight()) {
			Either<Throwable, BmsXlsTrialImportResult> e2 = either.right();
			if (e2.isRight()) {
				BmsXlsTrialImportResult sr = e2.right();
				
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				sr.printOn(pw);
				pw.close();
				messagePrinter.println(sw.toString());
				
				// Remember this one now !
				lowcaseTrialNames.add(sr.trial.getTrialName().toLowerCase());
				
				result = Either.right(callable.getImportedTrial());
			}
			else {
				result = Either.left(e2.left());
			}
		}
		else {
			result = Either.left(either.left());
		}
		
		return result;
	}

	
	private void updateImportAction() {
		String msg = null;
		
		boolean seenDatabase = false;
		boolean seenKdsmart = false;
		
		if (fileImportTable.getRowCount() <= 0) {
			msg = "Drag/Drop or use 'Browse' to add files";			
		}
		else {
			Set<DeviceType> deviceTypes = 
					fileImportTableModel.getDeviceTypesRequiredForSelectedFiles(fileImportTable).keySet();
			if (deviceTypes.isEmpty()) {
				if (fileImportTable.getSelectedRowCount() > 0) {
					msg = "Select a file that hasn't been imported";
				}
				else {
					if (ListSelectionModel.SINGLE_INTERVAL_SELECTION == fileImportTable.getSelectionModel().getSelectionMode()) {
						msg = "Select file to Import";
					}
					else {
						msg = "Select one or more files to Import";
					}
				}
			}
			else {
				seenDatabase = deviceTypes.contains(DeviceType.DATABASE);
				seenKdsmart = deviceTypes.contains(DeviceType.KDSMART);
			}
		}
		
		String warn = "";
		// If file is ok, check 
		if (msg == null) {
			String operatorName = devAndOpPanel.getOperatorName();
            DeviceIdentifier devid = devAndOpPanel.getDeviceIdentifier();
			if (seenKdsmart) {
				if (devid != null) {
				    if (DeviceType.KDSMART.equals(devid.getDeviceType())) {
						int vrow = fileImportTable.getSelectedRow();
						if (vrow >= 0) {
							int mrow = fileImportTable.convertRowIndexToModel(vrow);
							if (mrow >= 0) {
								File file = fileImportTableModel.getFileAt(mrow);
								if (file.getName().contains(",")) {
									String lookFor = "," + devid.getDeviceName() + ",";
									if (! file.getName().contains(lookFor)) {
										warn = String.format("Check Source Device (%s)", devid.getDeviceName());
									}
								}
							}
						}
					}
					else {
						msg = Msg.MSG_SELECT_OR_ENTER_KDSMART_DEVICE_ID();
					}
				}
				
				if (devid==null || ! DeviceType.KDSMART.equals(devid.getDeviceType())) {
					// So - no operator required
				}
				else if (operatorName==null || operatorName.trim().isEmpty()) {
					msg = Msg.MSG_SELECT_OR_ENTER_OPNAME();
				}
			}
			
			if (Check.isEmpty(msg) && seenDatabase) {
			    if (devid!=null && DeviceIdentifier.PLEASE_SELECT_DEVICE_TYPE==devid.getDeviceType()) {
                    msg = Msg.MSG_SELECT_OR_ENTER_DEVICE_ID();
			    }
			    else if (Check.isEmpty(operatorName)) {
			        msg = Msg.MSG_SELECT_OR_ENTER_OPNAME();
			    }
			}			
		}
	
		importAction.setEnabled(msg == null);
		if (msg != null) {
			errorMessage.setText(msg);
		}
		else {
			errorMessage.setText(warn);
		}
	}


	public void addFiles(List<File> files) {
		fileImportTableModel.addFiles(files);
	}

}
