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
package com.diversityarrays.kdxplore.trialmgr.trait;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.db.DartSchemaHelper;
import com.diversityarrays.kdsmart.KDSmartApplication;
import com.diversityarrays.kdsmart.db.EntityChangeListener;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.csvio.ImportError;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.BarcodeFactory;
import com.diversityarrays.kdsmart.db.util.ProgressReporter;
import com.diversityarrays.kdsmart.db.util.TraitImportTransactions;
import com.diversityarrays.kdxplore.KDDartEntityFactory;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.SourceChoiceHandler;
import com.diversityarrays.kdxplore.SourceChoiceHandler.SourceChoice;
import com.diversityarrays.kdxplore.barcode.BarcodeSheetDialog;
import com.diversityarrays.kdxplore.beans.DartEntityBeanRegistry;
import com.diversityarrays.kdxplore.config.KdxploreConfig;
import com.diversityarrays.kdxplore.curate.undoredo.ChangeManager;
import com.diversityarrays.kdxplore.curate.undoredo.Changeable;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.OfflineDataChangeListener;
import com.diversityarrays.kdxplore.editing.EntityPropertiesTable;
import com.diversityarrays.kdxplore.editing.EntityPropertiesTableModel;
import com.diversityarrays.kdxplore.editing.PropertiesTableLegendPanel;
import com.diversityarrays.kdxplore.editing.TraitPropertiesTableModel;
import com.diversityarrays.kdxplore.model.TraitTableModel;
import com.diversityarrays.kdxplore.model.TrialTableModel;
import com.diversityarrays.kdxplore.prefs.KdxPreference;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferenceEditor;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.trialmgr.TrialManagerPreferences;
import com.diversityarrays.kdxplore.trialmgr.trait.repair.TraitsToRepair;
import com.diversityarrays.kdxplore.ui.HelpUtils;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.kdxplore.upload.TraitUploadHandler;
import com.diversityarrays.kdxplore.upload.TraitUploadTask;
import com.diversityarrays.kdxplore.upload.TraitUploadTask.Answer;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.DALClientProvider;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;

import android.content.Context;
import net.pearcan.dnd.ChainingTransferHandler;
import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.FileChooserFactory;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.MessagePrinter;

public class TraitExplorerPanel extends JPanel implements TraitExplorer {

    static private final String TAG = "TraitExplorerPanel";

	private static final String SELECT_A_TRAIT_TO_VIEW_DETAILS = "Select a Trait to view details";

	// private final DartSchemaHelper dartSchemaHelper = new DartSchemaHelper(
	// KDDartEntityFactory.Util.getInstance().newCoreSchema());

	private final FileDrop fileDrop = new FileDrop() {
		@Override
		public void dropFiles(Component arg0, List<File> files, DropLocationInfo arg2) {
			for (File f : files) {
				if (FileChooserFactory.CSV_FILE_FILTER.accept(f)) {
					doImportTraitsFile(f);
					break;
				}
			}
		}
	};

	private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

	private final Action uploadTraitsAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			List<Trait> traits = collectTraitsForUploadOrExport();
			handleTraitUpload(traits);
		}
	};

	private final Action exportTraitsAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Trait> traits = collectTraitsForUploadOrExport();
            TraitExportDialog dlg = new TraitExportDialog(GuiUtil.getOwnerWindow(TraitExplorerPanel.this), traits);
            dlg.setVisible(true);
        }
	};

	private final SourceChoiceHandler sourceChoiceHandler = new SourceChoiceHandler() {
		@Override
		public void handleSourceChosen(SourceChoice choice) {
			switch (choice) {
			case DATABASE:
				loadFromKddartDatabase();
				break;
			case CSV:
				loadUsingCsvFiles();
				break;
			case XLS:
			default:
				break;
			}
		}
	};

	private final Action importTraitsAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {

		    SourceChoice[] choices;
		    if (KdxploreConfig.getInstance().getModeList().contains("CIMMYT")) { //$NON-NLS-1$
		        choices = new SourceChoice[] { SourceChoice.CSV };
		    }
		    else {
		        choices = new SourceChoice[] { SourceChoice.DATABASE, SourceChoice.CSV };
		    }
			SourceChoiceHandler.Util.showSourceChoicePopup(importTraitsButton,
			        0,
			        0,
			        "Select source for Trait data",
					sourceChoiceHandler, choices);
		}
	};
	private final JButton importTraitsButton = new JButton(importTraitsAction);

	private final Action refreshAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			refreshTraitsTable();
			new Toast(refreshButton, "Trait Data Refreshed", Toast.SHORT).show();
		}
	};
	private final JButton refreshButton = new JButton(refreshAction);

	private final Action addNewTraitAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				TraitEditDialog dlg = new TraitEditDialog(GuiUtil.getOwnerWindow(TraitExplorerPanel.this), null, null,
						offlineData.getKdxploreDatabase(), traitTableModel.collectTraitsOkForCALC(), changeConsumer);
				GuiUtil.centreOnOwner(dlg);
				dlg.setVisible(true);
			} catch (IOException e1) {
				MsgBox.error(TraitExplorerPanel.this, e, "Unable to Add Trait");
			}
		}
	};

	private File outputDir;
	private final Closure<File> outputDirectoryChanged = new Closure<File>() {
		@Override
		public void execute(File dir) {
			outputDir = dir;
		}
	};

	private final JCheckBox italicsForProtectedCheckbox = new JCheckBox("<html><i>Show Protected</i>");
	private final JLabel badForCalc = new JLabel("Invalid for CALC");

	private static final String EDITABLE_LOCKED = " Editing Locked";
	private static final String EDITABLE_UNLOCKED = " Double-click a Trait to Edit";

	private boolean traitsEditable = false;

	private final JCheckBox editingLocked = new JCheckBox(EDITABLE_LOCKED, ! traitsEditable);

	private Action deleteTraitsAction = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			List<Integer> modelRows = GuiUtil.getSelectedModelRows(traitsTable);
			List<Trait> traitsToCheck = new ArrayList<>();
			for (Integer row : modelRows) {
				traitsToCheck.add(traitTableModel.getEntityAt(row));
			}
			if (!traitsToCheck.isEmpty()) {
			    removeTraits(traitsToCheck);
			}
		}
	};

	private Action barcodesMenuAction = new AbstractAction() {

		JPopupMenu menu;

		@Override
		public void actionPerformed(ActionEvent e) {
			Point pt = barcodesMenuButton.getLocation();
			if (menu == null) {
				menu = new JPopupMenu("Barcodes");
				menu.add(generateBarcodesAction);
				menu.add(printBarcodesAction);
			}

			menu.show(barcodesMenuButton, pt.x, pt.y);
		}
	};

	private JButton barcodesMenuButton = new JButton(barcodesMenuAction);

	private Action generateBarcodesAction = new AbstractAction("Generate Barcodes") {
		@Override
		public void actionPerformed(ActionEvent e) {

			List<Trait> traitsWithoutBarcode = new ArrayList<>();

			List<Integer> selectedModelRows = GuiUtil.getSelectedModelRows(traitsTable);
			if (selectedModelRows.isEmpty()) {
				for (int index = traitTableModel.getRowCount(); --index >= 0;) {
					Trait t = traitTableModel.getEntityAt(index);
					String bc = t.getBarcode();
					if (Check.isEmpty(bc)) {
						traitsWithoutBarcode.add(t);
					}
				}
			} else {
				for (Integer row : selectedModelRows) {
					Trait t = traitTableModel.getEntityAt(row);
					String bc = t.getBarcode();
					if (Check.isEmpty(bc)) {
						traitsWithoutBarcode.add(t);
					}
				}
			}

			if (traitsWithoutBarcode.isEmpty()) {
				String errmsg = selectedModelRows.isEmpty() ? "All Traits have barcodes"
						: "All selected Traits have barcodes";
				JOptionPane.showMessageDialog(TraitExplorerPanel.this, errmsg);
			} else {
				StringBuilder sb = new StringBuilder("Confirm Barcode Generation for:");
				for (Trait t : traitsWithoutBarcode) {
					sb.append("\n").append(t.getTraitName());
				}
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(TraitExplorerPanel.this, sb.toString(),
						"Generate Barcodes", JOptionPane.YES_NO_OPTION)) {
					for (Trait t : traitsWithoutBarcode) {
						BarcodeFactory.setTraitBarcode(t);
						try {
							offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase().saveTrait(t, false);
						} catch (IOException e1) {
							messagePrinter
									.println("Unable to save change for " + t.getTraitName() + ": " + e1.getMessage());
						}
					}
					traitsTable.repaint();
				}
			}
		}
	};

	private Action fixTraitLevelsAction = new AbstractAction("Repair") {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub

			// Step 1. Find the Trials containing Samples that have Traits with
			// TraitLevel.UNDECIDABLE
			// Display:
			// Trait, Trial, # Plot Samples, # Sub-Plot Samples

			// Step 2. User chooses Trial/Trials to be repaired.
			// Repair is: (1) Convert Plot Samples to Sub-Plot Samples (and
			// Trait becomes TraitLevel.SPECIMEN)
			// (2) Convert Sub-Plot Samples to Plot Samples (and Trait becomes
			// TraitLevel.PLOT)

		    if (traitsToRepair == null || traitsToRepair.traits.isEmpty()) {
	            MsgBox.warn(TraitExplorerPanel.this, "Nothing to do", "Repair Trait Levels");
	            fixTraitLevelsButton.setVisible(false);
	            return;
		    }

		    BackgroundTask<Map<Trait,String>, Pair<Trait, Either<Exception, String>>> task =
		            new BackgroundTask<Map<Trait,String>, Pair<Trait,Either<Exception,String>>>("", false)
		    {
                @Override
                public Map<Trait,String> generateResult(
                        Closure<Pair<Trait, Either<Exception, String>>> publishPartial)
                throws Exception {
                    return traitsToRepair.repairTraits(publishPartial);
                }

                @Override
                public void onCancel(CancellationException e) {}

                @Override
                public void onException(Throwable cause) {
                    MsgBox.error(TraitExplorerPanel.this, cause, "Error Repairing Traits");
                }

                @Override
                public void processPartial(List<Pair<Trait, Either<Exception, String>>> chunks) {
                    for (Pair<Trait, Either<Exception, String>> pair : chunks) {
                        Trait t = pair.first;
                        StringBuilder sb = new StringBuilder("Trait ");
                        sb.append(pair.first.getTraitName()).append(": ");
                        Either<Exception, String> either = pair.second;
                        if (either.isLeft()) {
                            sb.append("ERROR=");
                            sb.append(either.left().getMessage());
                        }
                        else {
                            sb.append(either.right());
                        }
                        messagePrinter.println(sb);
                    }
                }

                @Override
                public void onTaskComplete(Map<Trait,String> result) {
                    traitsTable.repaint();
                }
            };

            backgroundRunner.runBackgroundTask(task);
		}
	};
	private final JButton fixTraitLevelsButton = new JButton(fixTraitLevelsAction);

	private Action printBarcodesAction = new AbstractAction("Print Barcodes") {
		@Override
		public void actionPerformed(ActionEvent e) {

			List<Trait> traits = new ArrayList<>();
			for (int row = traitTableModel.getRowCount(); --row >= 0;) {
				traits.add(traitTableModel.getEntityAt(row));
			}

			if (traits.isEmpty()) {
				MsgBox.info(TraitExplorerPanel.this, "No Traits", "Print Barcodes");
				return;
			}
			Collections.sort(traits);

			KdxplorePreferences prefs = KdxplorePreferences.getInstance();
			if (outputDir == null) {
				outputDir = prefs.getOutputDirectory();
			}

			final File initialOutputDir = outputDir;

			BarcodeSheetDialog dlg = new BarcodeSheetDialog(GuiUtil.getOwnerWindow(TraitExplorerPanel.this),
					"Print Trait Barcodes", null, traits, outputDirectoryChanged, initialOutputDir,
					offlineData.getKdxploreDatabase());
			dlg.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					dlg.removeWindowListener(this);
					if (outputDir != null) {
						if (initialOutputDir == null || !initialOutputDir.equals(outputDir)) {
							prefs.saveOutputDirectory(outputDir);
						}
					}
				}
			});
			dlg.setVisible(true);
		}
	};

	private final TraitTrialsTableModel traitTrialsTableModel = new TraitTrialsTableModel();
	private final JTable traitTrialsTable = new JTable(traitTrialsTableModel);

	private final TraitTableModel traitTableModel = TraitTableModel.create();
	private final JTable traitsTable = new JTable(traitTableModel);

	private final TraitPropertiesTableModel traitPropertiesTableModel = TraitPropertiesTableModel.create();
	private final TraitPropertiesTable traitPropertiesTable = new TraitPropertiesTable(traitPropertiesTableModel);

	private final PropertiesTableLegendPanel legendPanel = new PropertiesTableLegendPanel(traitPropertiesTable);

	private OfflineData offlineData;

	private final EntityChangeListener<Trait> traitChangeListener = new EntityChangeListener<Trait>() {

        @Override
        public Class<Trait> getEntityClass() {
            return Trait.class;
        }

        @Override
        public void entityAdded(KDSmartDatabase db, Trait t) {
            int modelRow = traitTableModel.addTrait(t);
            if (modelRow >= 0) {
                int viewRow = traitsTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    traitsTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    traitPropertiesTable.repaint();
                }
            }
        }

        @Override
        public void entityChanged(KDSmartDatabase db, Trait t) {
            traitTableModel.traitChanged(t);
        }

        @Override
        public void entitiesRemoved(KDSmartDatabase db, Set<Integer> ids) {
            traitTableModel.removeTraits(ids);
        }

        @Override
        public void listChanged(KDSmartDatabase db, int nChanges) {
            try {
                traitTableModel.setData(db.getTraits());
            } catch (IOException e) {
                Shared.Log.w(TAG, "listChanged(... , " + nChanges + ")", e);
            }
        }
    };

	private OfflineDataChangeListener offlineDataListener = new OfflineDataChangeListener() {
		@Override
		public void trialUnitsAdded(Object source, int trialId) {
		}

		// TODO NEED TO Add OfflineDataChangeListener.traitsAdded(Source,
		// Trait[])
		// traitChanged(Source, Trait)
		// traitsRemoved(Integer[] traitIds);

		@Override
		public void offlineDataChanged(Object source, String reason, KdxploreDatabase oldDb, KdxploreDatabase newDb) {
			if (oldDb != null) {
				oldDb.removeEntityChangeListener(traitChangeListener);
			}
			if (newDb != null) {
				newDb.addEntityChangeListener(traitChangeListener);
			}

			traitPropertiesTableModel.setDatabase(newDb);
			refreshTraitsTable();

			if (newDb != null) {
				checkForTraitLevels(newDb);
			}
		}
	};

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);

	private final JLabel noTraitsComponent = new JLabel("You need to import some Traits");
	private final JLabel selectTraitComponent = new JLabel(SELECT_A_TRAIT_TO_VIEW_DETAILS);

	static private final String CARD_NO_TRAITS = "noTraits";
	static private final String CARD_TRAIT_EDITOR = "editTrait";
	static private final String CARD_SELECT_TO_EDIT = "selectTrait";

    private static final boolean OVERRIDE = true;
    private static final boolean DONT_OVERRIDE = false;

//    private static final boolean USE_COPY_TRAIT_ACTION = false;

	private final DALClientProvider clientProvider;
	// private final KdxUploadHandler uploadHandler;
	private final BackgroundRunner backgroundRunner;

	private final JSplitPane splitPane;
	private final MessagePrinter messagePrinter;

	private TraitNameCellRenderer traitNameCellRenderer = new TraitNameCellRenderer();

	private ChangeListener badForCalcColorChangeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			Color c = TrialManagerPreferences.getInstance().getBadForCalcColor();
			traitNameCellRenderer.setBadForCalcColor(c);
			badForCalc.setForeground(c);
			traitsTable.repaint();
		}
	};

	private final Transformer<Trial, Boolean> checkIfEditorActive;

	private TraitsToRepair traitsToRepair;

	private final DartSchemaHelper schemaHepler = new DartSchemaHelper(
			KDDartEntityFactory.Util.getInstance().newCoreSchema());;

	public TraitExplorerPanel(
			MessagePrinter mp,
			OfflineData od,
			DALClientProvider clientProvider,
			// KdxUploadHandler uploadHandler,
			BackgroundRunner backgroundRunner,
			ImageIcon addBarcodeIcon,
			Transformer<Trial, Boolean> checkIfEditorActive) {
		super(new BorderLayout());

		this.backgroundRunner = backgroundRunner;
		this.clientProvider = clientProvider;
		// this.uploadHandler = uploadHandler;
		this.messagePrinter = mp;
		this.offlineData = od;
		this.checkIfEditorActive = checkIfEditorActive;

		offlineData.addOfflineDataChangeListener(offlineDataListener);

		editingLocked.setIcon(KDClientUtils.getIcon(ImageId.LOCKED));
		editingLocked.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				changeEditable(editingLocked.isSelected(), DONT_OVERRIDE);
			}
		});

		changeManager.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				updateUndoRedoActions();
			}
		});

		KDClientUtils.initAction(ImageId.TRASH_24, deleteTraitsAction, "Remove Trait");
		deleteTraitsAction.setEnabled(false);

		KDClientUtils.initAction(ImageId.REFRESH_24, refreshAction, "Refresh Data");

		KDClientUtils.initAction(ImageId.PLUS_BLUE_24, addNewTraitAction, "Add Trait");

		KDClientUtils.initAction(ImageId.UPLOAD_24, uploadTraitsAction, "Upload Traits");

		KDClientUtils.initAction(ImageId.ADD_TRIALS_24, importTraitsAction, "Import Traits");

		KDClientUtils.initAction(ImageId.EXPORT_24, exportTraitsAction, "Export Traits");

		try {
			Class.forName("com.diversityarrays.kdxplore.upload.TraitUploadTask");
		} catch (ClassNotFoundException e1) {
			uploadTraitsAction.setEnabled(false);
			if (RunMode.getRunMode().isDeveloper()) {
				new Toast((JComponent) null, "<HTML>Developer Warning<BR>" + "Trait Upload currently unavailable<BR>",
						4000).showAsError();
			}
		}

		traitPropertiesTable.setTransferHandler(
		        TableTransferHandler.initialiseForCopySelectAll(traitPropertiesTable, true));
		traitPropertiesTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (traitPropertiesTableModel.getRowCount() > 0) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							GuiUtil.initialiseTableColumnWidths(traitPropertiesTable);
						}
					});
					traitPropertiesTableModel.removeTableModelListener(this);
				}
			}
		});

		traitTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				showCorrectCard();
			}
		});

		TrialManagerPreferences preferences = TrialManagerPreferences.getInstance();
		preferences.addChangeListener(TrialManagerPreferences.BAD_FOR_CALC, badForCalcColorChangeListener);
		badForCalc.setForeground(preferences.getBadForCalcColor());
		badForCalc.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					KdxPreference<Color> pref = TrialManagerPreferences.BAD_FOR_CALC;
					String title = pref.getName();
					KdxplorePreferenceEditor.startEditorDialog(TraitExplorerPanel.this, title, pref);
				}
			}
		});

		traitsTable.setAutoCreateRowSorter(true);
		int index = traitTableModel.getTraitNameColumnIndex();
		if (index >= 0) {
	        traitsTable.getColumnModel().getColumn(index).setCellRenderer(traitNameCellRenderer);
		}

		traitsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && 2 == e.getClickCount()) {
					e.consume();

					int vrow = traitsTable.rowAtPoint(e.getPoint());
					if (vrow >= 0) {
						int mrow = traitsTable.convertRowIndexToModel(vrow);
						if (mrow >= 0) {
							Trait trait = traitTableModel.getTraitAtRow(mrow);
							Integer selectViewRow = null;
							if (!traitTrialsTableModel.isSelectedTrait(trait)) {
								selectViewRow = vrow;
							}
							if (traitsEditable) {
								startEditingTraitInternal(trait, selectViewRow, null);
							}
							else {
							    warnEditingLocked();
							}
						}
					}
				}
			}
		});

		traitsTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		traitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {

					List<Trait> selectedTraits = getSelectedTraits();
					traitTrialsTableModel.setSelectedTraits(selectedTraits);

					if (selectedTraits.size() == 1) {
						Trait trait = null;
						int vrow = traitsTable.getSelectedRow();
						if (vrow >= 0) {
							int mrow = traitsTable.convertRowIndexToModel(vrow);
							if (mrow >= 0) {
								trait = traitTableModel.getEntityAt(mrow);
							}
						}
						showTraitDetails(trait);
					}

					deleteTraitsAction.setEnabled(selectedTraits.size() > 0);

					showCorrectCard();
				}
			}
		});

		TraitTableModel.initValidationExpressionRenderer(traitsTable);
        if (RunMode.getRunMode().isDeveloper()) {
            TraitTableModel.initTableForRawExpression(traitsTable);
        }
		cardPanel.add(noTraitsComponent, CARD_NO_TRAITS);
		cardPanel.add(selectTraitComponent, CARD_SELECT_TO_EDIT);
		cardPanel.add(new JScrollPane(traitPropertiesTable), CARD_TRAIT_EDITOR);

        JButton undoButton = initAction(undoAction, ImageId.UNDO_24, "Undo",
                KeyStroke.getKeyStroke('Z', Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
        JButton redoButton = initAction(redoAction, ImageId.REDO_24, "Redo",
                KeyStroke.getKeyStroke('Y', Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()));
		Box undoRedoButtons = Box.createHorizontalBox();
        undoRedoButtons.add(undoButton);
        undoRedoButtons.add(redoButton);


		JPanel detailsPanel = new JPanel(new BorderLayout());
		detailsPanel.add(GuiUtil.createLabelSeparator("Details", undoRedoButtons), BorderLayout.NORTH);
		detailsPanel.add(cardPanel, BorderLayout.CENTER);
		detailsPanel.add(legendPanel, BorderLayout.SOUTH);

		PromptScrollPane scrollPane = new PromptScrollPane(traitsTable,
				"Drag/Drop Traits CSV file or use 'Import Traits'");

		TableTransferHandler tth = TableTransferHandler.initialiseForCopySelectAll(traitsTable, true);
		traitsTable.setTransferHandler(new ChainingTransferHandler(flth, tth));

		scrollPane.setTransferHandler(flth);

		if (addBarcodeIcon == null) {
			barcodesMenuAction.putValue(Action.NAME, "Barcodes...");
		} else {
			barcodesMenuAction.putValue(Action.SMALL_ICON, addBarcodeIcon);
		}

		italicsForProtectedCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				traitNameCellRenderer.setUseItalicsForProtected(italicsForProtectedCheckbox.isSelected());
				traitsTable.repaint();
			}
		});

		Box leftTopControls = Box.createHorizontalBox();
		leftTopControls.add(importTraitsButton);
		leftTopControls.add(barcodesMenuButton);
		leftTopControls.add(new JButton(addNewTraitAction));
		leftTopControls.add(new JButton(uploadTraitsAction));
		leftTopControls.add(new JButton(exportTraitsAction));

		leftTopControls.add(Box.createHorizontalGlue());

		leftTopControls.add(editingLocked);
		leftTopControls.add(fixTraitLevelsButton);
		leftTopControls.add(refreshButton);
		leftTopControls.add(Box.createHorizontalStrut(8));
		leftTopControls.add(new JButton(deleteTraitsAction));
		// leftTopControls.add(Box.createHorizontalStrut(4));

		Box explanations = Box.createHorizontalBox();
		explanations.add(italicsForProtectedCheckbox);
		explanations.add(badForCalc);
		explanations.add(Box.createHorizontalGlue());

		fixTraitLevelsButton.setToolTipText("Fix Traits with " + TraitLevel.UNDECIDABLE.visible + " 'Level'");
        fixTraitLevelsButton.setVisible(false);

		JPanel leftTop = new JPanel(new BorderLayout());
		leftTop.add(leftTopControls, BorderLayout.NORTH);
		leftTop.add(scrollPane, BorderLayout.CENTER);
		leftTop.add(explanations, BorderLayout.SOUTH);

		JPanel leftBot = new JPanel(new BorderLayout());
		leftBot.add(GuiUtil.createLabelSeparator("Used by Trials"), BorderLayout.NORTH);
		leftBot.add(new PromptScrollPane(traitTrialsTable, "Any Trials using selected Traits appear here"));

		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftTop, leftBot);
		leftSplit.setResizeWeight(0.5);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, detailsPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.5);

		add(splitPane, BorderLayout.CENTER);
	}

	private JButton initAction(Action action, ImageId imageId, String toolTip, KeyStroke keyStroke) {
	    KDClientUtils.initAction(imageId, action, toolTip, false);
	    action.setEnabled(false);

	    String command = action.toString();

	    JButton result = new JButton(action);
	    result.getActionMap().put(command, action);
	    result.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, command);

	    return result;
	}

	private Action undoAction = new AbstractAction("Undo") {
		@Override
		public void actionPerformed(ActionEvent e) {
			changeManager.undo(null);
            // refresh the views that may have been affected
			traitsTable.repaint();
			traitTrialsTable.repaint();
			traitPropertiesTable.repaint();
		}
	};

	private List<Trait> collectTraitsForUploadOrExport() {
	    List<Integer> modelRows = GuiUtil.getSelectedModelRows(traitsTable);
        List<Trait> result = new ArrayList<>();

        if (modelRows == null || modelRows.size() <= 0) {
            Collection<Trait> traits = traitTableModel.getTraitMap().values();
            if (traits != null) {
                result.addAll(traits);
            }
        } else {
            for (Integer row : modelRows) {
                Trait trait = traitTableModel.getTraitAtRow(row);
                if (trait != null) {
                    result.add(trait);
                }
            }
        }
        return result;
	}

	private Action redoAction = new AbstractAction("Redo") {
		@Override
		public void actionPerformed(ActionEvent e) {
			changeManager.redo(null);
			// refresh the views that may have been affected
            traitsTable.repaint();
            traitTrialsTable.repaint();
            traitPropertiesTable.repaint();
		}
	};

	private void updateUndoRedoActions() {
		undoAction.setEnabled(getUndoCount() > 0);
		redoAction.setEnabled(getRedoCount() > 0);
	}

	private int getUndoCount() {
		return changeManager.getUndoCount();
	}

	private int getRedoCount() {
		return changeManager.getRedoCount();
	}

	private void checkForTraitLevels(KdxploreDatabase kdxdb) {

	    traitsToRepair = null;
	       // Assume nothing needs fixing
        fixTraitLevelsButton.setVisible(false);

		if (kdxdb == null) {
			return;
		}

		// Step 1. Find all Traits with TraitLevel.UNDECIDABLE
		// If none, disable the "Fix" action and return.
		// Step 2. See if any Samples are "scored" for the Traits detected in
		// Step 1.
		// If none, disable the "Fix" action and return.
		// Step 3. Enable the "Fix" action.

		// Step 1:
		List<Trait> toBeFixed = new ArrayList<>();
		for (int index = traitTableModel.getRowCount(); --index >= 0;) {
			Trait trait = traitTableModel.getEntityAt(index);
			if (TraitLevel.UNDECIDABLE == trait.getTraitLevel()) {
				toBeFixed.add(trait);
			}
		}

		if (toBeFixed.isEmpty()) {
		    // Assumption was correct - nothing to fix
			return;
		}

		traitsToRepair = new TraitsToRepair(kdxdb, toBeFixed);

		// Step 2:
		try {
		    traitsToRepair.getProblemSampleCountByTrialId();
		}
		catch (IOException e) {
            MsgBox.error(TraitExplorerPanel.this, e,
                    "Error while checking for Problem Traits");
            return;
		}

		if (traitsToRepair.isEmpty()) {
		    traitsToRepair = null;
		    fixTraitLevelsButton.setVisible(false);
			return;
		}

		// Ok. There are some !
		// Step 3:
		fixTraitLevelsButton.setVisible(true);

		messagePrinter.println(traitsToRepair.getDescription());
	}

	private List<Trait> getSelectedTraits() {
		List<Trait> traits = new ArrayList<>();

		List<Integer> modelRows = GuiUtil.getSelectedModelRows(traitsTable);
		if (!modelRows.isEmpty()) {
			for (Integer row : modelRows) {
				Trait t = traitTableModel.getEntityAt(row);
				traits.add(t);
			}
		}
		return traits;
	}

	private void loadFromKddartDatabase() {
		JOptionPane.showMessageDialog(this, "Waiting no SIU DAL-Interop", "Add Traits",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void loadUsingCsvFiles() {
		File file = Shared.chooseFileToOpen(TraitExplorerPanel.this, FileChooserFactory.CSV_FILE_FILTER);
		if (file != null) {
			doImportTraitsFile(file);
		}
	}

	private void changeEditable(boolean isLocked, boolean override) {

		if (isLocked) {
            editingLocked.setIcon(KDClientUtils.getIcon(ImageId.LOCKED));
            editingLocked.setForeground(Color.BLACK);
            editingLocked.setText(EDITABLE_LOCKED);
            traitsEditable = false;
		}
		else {
			TrialManagerPreferences prefs = TrialManagerPreferences.getInstance();

			if (! override && prefs.getShowEditTraitWarning()) {

		         JPanel dialogPanel = new JPanel(new BorderLayout());
		         JLabel dialogLabel = new JLabel(
		                 "<html>Editing <b><i>Traits</i></b> may adversely affect the <b><i>Trials</i></b> using them.");
		         JCheckBox checkBox = new JCheckBox("Don't show me this again.", false);
		         dialogPanel.add(dialogLabel, BorderLayout.CENTER);
		         dialogPanel.add(checkBox, BorderLayout.SOUTH);

			    if (JOptionPane.YES_OPTION
			            ==
			            JOptionPane.showConfirmDialog(this, dialogPanel, "Allow Trait Editing?", JOptionPane.YES_NO_OPTION))
			    {
					editingLocked.setIcon(KDClientUtils.getIcon(ImageId.UNLOCKED));
					editingLocked.setForeground(Color.RED);
					editingLocked.setText(EDITABLE_UNLOCKED);
					traitsEditable = true;
				}

				if (checkBox.isSelected()) {
                    prefs.setShowEditTraitWarning(false);
				}
			} else {
				editingLocked.setIcon(KDClientUtils.getIcon(ImageId.UNLOCKED));
				editingLocked.setForeground(Color.RED);
				editingLocked.setText(EDITABLE_UNLOCKED);
				traitsEditable = true;
			}
		}
	}

	protected void showTraitDetails(Trait trait) {
		if (trait == null) {
			Trait tmp = new Trait();
			tmp.setBarcode("");
			tmp.setTraitAlias("");
			tmp.setTraitDataType(null);
			tmp.setTraitDescription("");
			tmp.setTraitName("");
			tmp.setTraitUnit("");
			tmp.setTraitValRule("");
			traitPropertiesTableModel.setData(tmp);
		} else {
			traitPropertiesTableModel.setData(trait);
		}
	}

	public void doPostOpenActions() {
		splitPane.setDividerLocation(0.5);
	}

	public void refreshTraitsTable() {
		KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
		if (kdxdb == null) {
			return;
		}
		try {
			traitTableModel.setData(kdxdb.getAllTraits());

			KDSmartDatabase kdsdb = kdxdb.getKDXploreKSmartDatabase();
			Map<Trait, List<Trial>> trialsByTrait = new HashMap<>();

			for (Trial trial : kdsdb.getTrials()) {
				Set<Trait> trialTraits = kdsdb.getTrialTraits(trial.getTrialId());
				if (trialTraits != null && !trialTraits.isEmpty()) {
					for (Trait trait : trialTraits) {
						List<Trial> list = trialsByTrait.get(trait);
						if (list == null) {
							list = new ArrayList<>();
							trialsByTrait.put(trait, list);
						}
						list.add(trial);
					}
				}
			}
			traitTrialsTableModel.setTrialsByTrait(trialsByTrait);

		} catch (IOException e) {
			Shared.Log.e("TraitExplorerPanel", "refreshTraitsTable", e);
			MsgBox.error(TraitExplorerPanel.this, e, "refreshTraitsTable - Database Error");
		}
	}

	private void doImportTraitsFile(final File file) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doImportTraitsFileImpl(file);
			}
		});
	}

	private void doImportTraitsFileImpl(File file) {
		Context context = KDSmartApplication.getInstance();
		final ProgressMonitor monitor = new ProgressMonitor(TraitExplorerPanel.this, "Loading", "", 0, 100);
		ProgressReporter progressReporter = new ProgressReporter() {

			@Override
			public void setProgressNote(String note) {
				monitor.setNote(note);
			}

			@Override
			public void setProgressMaximum(int max) {
				monitor.setMaximum(max);
			}

			@Override
			public void setProgressCount(int count) {
				monitor.setProgress(count);
			}

			@Override
			public void dismissProgress() {
				monitor.close();
			}
		};

		try {
			Either<ImportError, TraitImportTransactions> either = offlineData.getKdxploreDatabase()
					.getKDXploreKSmartDatabase().importTraitsFile(context, file, progressReporter);

			if (either.isLeft()) {
				ImportError ie = either.left();
				MsgBox.error(TraitExplorerPanel.this, ie.getMessage("Import Traits"), "Import Failed");
			} else {
				TraitImportTransactions tit = either.right();

				if (!tit.traitsToBeUpdated.isEmpty()) {

				}

				refreshTraitsTable();

				StringBuilder sb = new StringBuilder("Import Result");
				if (tit.nSkipped > 0) {
					sb.append("\nSkipped ").append(tit.nSkipped);
				}
				if (!tit.traitsToBeAdded.isEmpty()) {
					sb.append("\nAdded: ").append(tit.traitsToBeAdded.size());
				}
				if (!tit.traitsToBeUpdated.isEmpty()) {
					sb.append("\nUpdated: ").append(tit.traitsToBeUpdated.size());
				}
				MsgBox.info(TraitExplorerPanel.this, sb.toString(), "Import Complete");
			}
		} finally {
			progressReporter.dismissProgress();
		}
	}

	private void showCorrectCard() {
		String card;
		if (traitTableModel.getRowCount() <= 0) {
			card = CARD_NO_TRAITS;
		} else if (traitsTable.getSelectedRowCount() == 1) {
			card = CARD_TRAIT_EDITOR;
		} else {
			card = CARD_SELECT_TO_EDIT;
		}
		cardLayout.show(cardPanel, card);
	}

	class TraitPropertiesTable extends EntityPropertiesTable<Trait> {

		public TraitPropertiesTable(EntityPropertiesTableModel<Trait> tm) {
			super(tm);
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			TableModel model = getModel();
			if (model instanceof TraitPropertiesTableModel) {
				TraitPropertiesTableModel tptm = (TraitPropertiesTableModel) model;
				if (!tptm.isCellEditable(rowIndex, columnIndex)) {
					return false;
				}
			}
			return super.isCellEditable(rowIndex, columnIndex);
		}

		// Return true if we "handled" the editing
		@Override
		protected boolean handleEditCellAt(EntityPropertiesTableModel<Trait> eptm, int row, int column) {
			TraitPropertiesTableModel tptm = (TraitPropertiesTableModel) eptm;

			PropertyDescriptor pd = tptm.getPropertyDescriptor(row);
			Trait trait = tptm.getTrait();
			startEditingTraitInternal(trait, null, pd);
			return true;
		}

		private boolean isTraitDataTypeOrValidationRule(TraitPropertiesTableModel tptm, int row) {
			PropertyDescriptor pd = tptm.getPropertyDescriptor(row);

			Class<?> propertyClass = pd.getPropertyType();

			if (TraitDataType.class == propertyClass) {
				return true;
			}
			if (DartEntityBeanRegistry.TRAIT_VAL_RULE_HEADING.equals(pd.getDisplayName())) {
				return true;
			}
			return false;
		}

	}

	public int getTraitCount() {
		return traitTableModel.getRowCount();
	}

	static class TraitNameCellRenderer extends DefaultTableCellRenderer {

		private final boolean developer = RunMode.getRunMode().isDeveloper();

		private final Font normalFont;
		private final Font italicFont;
		private Color badForCalcColor;
		private boolean useItalics = false;

		TraitNameCellRenderer() {
			normalFont = getFont();
			italicFont = normalFont.deriveFont(Font.ITALIC);
			badForCalcColor = TrialManagerPreferences.getInstance().getBadForCalcColor();
		}

		public void setBadForCalcColor(Color c) {
			badForCalcColor = c;
		}

		public void setUseItalicsForProtected(boolean b) {
			useItalics = b;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			setToolTipText(null);

			Color fg = null;
			Font font = normalFont;
			TableModel model = table.getModel();
			if (model instanceof TraitTableModel) {
				int modelRow = table.convertRowIndexToModel(row);
				if (modelRow >= 0) {
					Trait trait = ((TraitTableModel) model).getEntityAt(modelRow);

					if (developer) {
						setToolTipText("Trait ID=" + trait.getTraitId());
					}

					if (useItalics && trait.isProtected()) {
						font = italicFont;
					}

					if (!isSelected) {
						if (TraitDataType.isTraitValidInCALC(trait)) {
							fg = table.getForeground();
						} else {
							fg = badForCalcColor;
						}
					}

				}
			}

			if (fg != null) {
				setForeground(fg);
			}
			setFont(font);

			return this;
		}

	}

	private boolean isSameDatabase(DALClient client) {
		String baseUrl = client.getBaseUrl();
		if (KdxploreDatabase.LOCAL_DATABASE_URL.equalsIgnoreCase(baseUrl)) {
			return KdxploreDatabase.LOCAL_DATABASE_URL.equalsIgnoreCase(offlineData.getDatabaseUrl());
		}
		return baseUrl.equalsIgnoreCase(offlineData.getDatabaseUrl());
	}

	private void handleTraitUpload(List<Trait> traitsToUpload) {

		// FIXME having problems with the gradle build so disabling this so I
		// get get a version out.

		DALClient dalClient = this.clientProvider.getDALClient();
		if (dalClient != null) {

			// TODO - Why can't we upload to a different DB?
//			if (! isSameDatabase(dalClient)) {
//				MsgBox.warn(TraitExplorerPanel.this, "Client is on different URL to Offline database.",
//						"Trait Upload - Cannot proceed");
//				return;
//			}

			Map<Trait,Boolean> conflictingTraits =
					TraitUploadTask.checkTraitsForConflict(traitsToUpload,
							clientProvider.getDALClient());
			List<Trait> confirmedTraits = new ArrayList<>();

			confirmedTraits = traitsToUpload;

			if (! conflictingTraits.isEmpty()) {
				List<Trait> changeableTraits = new ArrayList<>();
				List<Trait> unChangeableTraits = new ArrayList<>();

				for (Trait trait : conflictingTraits.keySet()) {
					System.out.println("Trait Name: " + trait.getTraitName());
					confirmedTraits.remove(trait);

					if (conflictingTraits.get(trait)) {
						changeableTraits.add(trait);
					} else {
						unChangeableTraits.add(trait);
					}
				}

				Answer answer =
						TraitUploadTask.createTraitOverwriteDialog(TraitExplorerPanel.this,
								changeableTraits, unChangeableTraits);
				switch (answer) {
				case NO_ONLY_NONEX:
					confirmedTraits.clear();
					break;
				case YES_OVERWRITE:
					confirmedTraits.addAll(changeableTraits);
					break;
				case CANCEL:
				default:
					return;
				}
			}

			TraitUploadHandler uploadHandler = new TraitUploadHandler(offlineData.getKdxploreDatabase(), this.clientProvider, schemaHepler);

			TraitUploadTask task = new TraitUploadTask(TraitExplorerPanel.this,uploadHandler, traitsToUpload, dalClient);

			backgroundRunner.runBackgroundTask(task);
		}
	}

	class TraitTrialsTableModel extends TrialTableModel {

		private final Map<Trait, List<Trial>> trialsByTrait = new HashMap<>();

		private final List<Trait> selectedTraits = new ArrayList<>();

		public boolean isSelectedTrait(Trait t) {
			return selectedTraits.contains(t);
		}

		public void setTrialsByTrait(Map<Trait, List<Trial>> map) {
			trialsByTrait.clear();
			trialsByTrait.putAll(map);

			List<Trait> newSelectedTraits = new ArrayList<>();
			Set<Trait> newTraits = trialsByTrait.keySet();
			for (Trait t : selectedTraits) {
				if (newTraits.contains(t)) {
					newSelectedTraits.add(t);
				}
			}

			setSelectedTraits(newSelectedTraits);
		}

		public List<Trial> getTrials(Trait trait) {
			return trialsByTrait.get(trait);
		}

		public List<Trait> getSelectedTraits() {
			return Collections.unmodifiableList(selectedTraits);
		}

		public void setSelectedTraits(Collection<Trait> traits) {
			selectedTraits.clear();
			selectedTraits.addAll(traits);

			Set<Trial> all = new TreeSet<>();
			for (Trait t : selectedTraits) {
				List<Trial> list = trialsByTrait.get(t);
				if (list != null) {
					all.addAll(list);
				}
			}
			List<Trial> list = new ArrayList<>(all);
			Collections.sort(list);

			setTrials(list);
		}
	}

	private Trial getFirstTrialBeingEdited(Trait trait) {
		List<Trial> trials = traitTrialsTableModel.getTrials(trait);
		if (!Check.isEmpty(trials)) {
			for (Trial trial : trials) {
				if (checkIfEditorActive.transform(trial)) {
					return trial;
				}
			}
		}
		return null;
	}

	private ChangeManager<Trait> changeManager = new ChangeManager<>();

	private List<Changeable<Trait>> changeList = new ArrayList<>();

	private Consumer<TraitChangeable> changeConsumer = new Consumer<TraitChangeable>() {
		@Override
		public void accept(TraitChangeable t) {
			changeList.add(t);
			changeManager.addChangeable(changeList.get(changeList.size() - 1));
		}
	};

	private void startEditingTraitInternal(Trait trait, Integer selectViewRow, PropertyDescriptor descriptor) {
		if (traitsEditable) {
			Trial edited = getFirstTrialBeingEdited(trait);
			if (edited != null) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						MsgBox.warn(TraitExplorerPanel.this, "Trial '" + edited.getTrialName() + "' is being curated",
								"Can't edit '" + trait.getTraitName() + "'");
					}

				});
				return;
			}
			// Ok - not editing a Trial that uses this trait.

			if (selectViewRow != null) {
				int vrow = selectViewRow;
				traitsTable.getSelectionModel().setSelectionInterval(vrow, vrow);
			}

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						TraitEditDialog dlg = new TraitEditDialog(GuiUtil.getOwnerWindow(TraitExplorerPanel.this), trait,
								descriptor, offlineData.getKdxploreDatabase(), traitTableModel.collectTraitsOkForCALC(), changeConsumer);
						dlg.setLocationRelativeTo(traitPropertiesTable);
						dlg.setVisible(true);
					} catch (IOException e) {
						MsgBox.error(TraitExplorerPanel.this, e, "Unable to Edit Trait");
					}
				}
			});
		}
		else {
		    warnEditingLocked();
		}
	}

	private void warnEditingLocked() {
        MsgBox.warn(TraitExplorerPanel.this, "Click the '" + EDITABLE_LOCKED + "' icon to allow editing", "Editing is Disabled");
	}

	// TraitExplorer
	@Override
	public void startEditing(Trait trait) {
		int mrow = traitTableModel.indexOf(trait);
		if (mrow >= 0) {
			int vrow = traitsTable.convertRowIndexToView(mrow);
			if (vrow >= 0) {
			    if (! traitsEditable) {
			        if (JOptionPane.YES_OPTION !=
			                JOptionPane.showConfirmDialog(TraitExplorerPanel.this,
			                        "Editing is locked. Do you want to proceed?",
			                        "Edit Trait " + trait.getTraitName(),
			                JOptionPane.YES_NO_OPTION))
			        {
			            return;
			        }
			        editingLocked.setSelected(false);
			        changeEditable(false, OVERRIDE);
			    }
				startEditingTraitInternal(trait, vrow, null);
			}
		}
	}

	@Override
	public void setSelectedTraits(List<Trait> traits) {
		List<Integer> traitIndices = traits.stream()
		        .map(traitTableModel::indexOf)
		        .filter(index -> index != null) // null if not found in model
				.map(modelIndex -> traitsTable.convertRowIndexToView(modelIndex))
				.filter(index -> index >= 0) // -1 if not visible
				.collect(Collectors.toList());

		if (!traitIndices.isEmpty()) {
			Collections.sort(traitIndices);
			ListSelectionModel lsm = traitsTable.getSelectionModel();
			lsm.setValueIsAdjusting(true);
			lsm.clearSelection();
			try {
				for (Integer index : traitIndices) {
					lsm.addSelectionInterval(index, index);
				}
			} finally {
				lsm.setValueIsAdjusting(false);
			}
		}
	}

    public void removeTraits(Collection<Trait> traitsToCheck) {

        KDSmartDatabase kdsDb = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase();
        List<Trait> okToDelete = new ArrayList<>();
        Map<Trait, List<Trial>> dontDelete = new TreeMap<>();

        try {

            checkOnTraitUsage(kdsDb, traitsToCheck, okToDelete, dontDelete);

            Function<Trait,Integer> dontDeleteNamer = new Function<Trait,Integer>() {
                @Override
                public Integer apply(Trait t) {
                    List<Trial> list = dontDelete.get(t);
                    return list==null ? 0 : list.size();
                }
            };

            if (okToDelete.isEmpty()) {
                Object msg;
                if (dontDelete.size()==1) {
                    String s = "";
                    for (Trait t : dontDelete.keySet()) {
                        s = t.getTraitName() + ": used by " + dontDelete.get(t).size() + " Trials";
                        break;
                    }
                    msg = s;
                }
                else {
                    msg = HelpUtils.makeTableInScrollPane("Trait", "# Trials",
                            dontDelete.keySet(), dontDeleteNamer);
//                    msg = HelpUtils.makeListInScrollPane(dontDelete.entrySet(), namer);
                }
                JOptionPane.showMessageDialog(TraitExplorerPanel.this,
                        msg,
                        "None of the selected Traits may be removed",
                        JOptionPane.WARNING_MESSAGE);
            } else {

                Collections.sort(okToDelete);

                Box box = Box.createVerticalBox();
                box.add(GuiUtil.createLabelSeparator("Traits to Remove"));
                box.add(HelpUtils.makeListInScrollPane(okToDelete, Trait::getTraitName));

                if (! dontDelete.isEmpty()) {
                    if (dontDelete.size() == 1) {
//                        box.add(GuiUtil.createLabelSeparator("These will not be removed"));
                        for (Trait t : dontDelete.keySet()) {
                            box.add(new JLabel("Will not be removed: " + t.getTraitName()));
                        }
                    }
                    else {
                        JScrollPane sp = HelpUtils.makeTableInScrollPane("Trait", "# Trials",
                                dontDelete.keySet(), dontDeleteNamer);
                        box.add(GuiUtil.createLabelSeparator("These will not be removed"));
                        box.add(sp);
                    }
                }

                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(TraitExplorerPanel.this,
                        box,
                        "Confirm Trait Removal", JOptionPane.YES_NO_OPTION)) {
                    int[] traitIds = new int[okToDelete.size()];
                    for (int index = okToDelete.size(); --index >= 0;) {
                        traitIds[index] = okToDelete.get(index).getTraitId();
                    }
                    kdsDb.removeTraits(traitIds);
                }
            }

        } catch (IOException e1) {
            MsgBox.error(TraitExplorerPanel.this, e1, "Error Removing Traits");
        }
    }

    private void checkOnTraitUsage(KDSmartDatabase db,
            Collection<Trait> traitsToCheck,
            List<Trait> okToDelete,
            Map<Trait, List<Trial>> dontDelete)
    throws IOException
    {
        Set<Integer> traitIds = new HashSet<>();
        for (Trait t : traitsToCheck) {
            traitIds.add(t.getTraitId());
        }

        Map<Integer, Set<Trial>> trialsByTraitId = db.getTrialsUsedByTraitId(traitIds);

        for (Trait trait : traitsToCheck) {
            Set<Trial> trialSet = trialsByTraitId.get(trait.getTraitId());
            if (trialSet.isEmpty()) {
                okToDelete.add(trait);
            } else {
                List<Trial> list = new ArrayList<>(trialSet);
                Collections.sort(list, Trial.TRIAL_TITLE_COMPARATOR);
                dontDelete.put(trait, list);
            }
        }
    }

}
