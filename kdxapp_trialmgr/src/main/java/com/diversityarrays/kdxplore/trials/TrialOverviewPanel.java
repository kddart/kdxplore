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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.kdsmart.db.BatchHandler;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithTraitOption;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TraitChangeListener;
import com.diversityarrays.kdsmart.db.TrialChangeListener;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.KdxConstants;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.OfflineDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.data.kdx.TrialPayload;
import com.diversityarrays.kdxplore.data.kdx.TrialPayloadImpl;
import com.diversityarrays.kdxplore.data.util.DatabaseUtil;
import com.diversityarrays.kdxplore.model.TrialTableModel;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.trialmgr.TrialExplorerManager;
import com.diversityarrays.kdxplore.trialmgr.trait.TraitExplorer;
import com.diversityarrays.kdxplore.ui.HelpUtils;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Html;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.dnd.ChainingTransferHandler;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.MessagePrinter;

/**
 * Presents all of the Trials and lets the user select one
 * for which more details will be displayed.
 * @author brianp
 *
 */
public class TrialOverviewPanel extends JPanel {

    // Used to signal to TrialExplorerPanel to handle the request to edit the Trial
	public static final String EDIT_TRIAL_COMMAND = "editTrial"; //$NON-NLS-1$

	private static final int MAX_INITIAL_VISIBLE_TRIAL_ROWS = 15;

	private static int EVENT_COUNT = 0;

	private boolean developer = RunMode.getRunMode().isDeveloper();

	private final OfflineData offlineData;

	private final int lineLengthLimit = KdxplorePreferences.getInstance().getTooltipLineLengthLimit();

	private final TrialTraitsTableModel trialTraitsTableModel = new TrialTraitsTableModel();
	private final JTable trialTraitsTable = new JTable(trialTraitsTableModel) {
		@Override
		public String getToolTipText(MouseEvent event) {
			Point pt = event.getPoint();
			if (pt != null) {
				int vrow = trialsTable.rowAtPoint(pt);
				if (vrow >= 0) {
					int mrow = trialsTable.convertRowIndexToModel(vrow);
					if (mrow >= 0) {
						TableModel model = getModel();
						if (model instanceof TrialTraitsTableModel) {
							TrialTraitsTableModel ttm = (TrialTraitsTableModel) model;
							Trait trait = ttm.getTraitAt(mrow);
							if (trait == null) {
							    return null;
							}
							StringBuilder html = new StringBuilder("<HTML>");
							html.append("Description");
							if (developer) {
								html.append(" (#").append(trait.getTraitId()).append(")");
							}
							Html.appendHtmlLines(html,
									trait.getTraitDescription(),
									lineLengthLimit);
							return html.toString();
						}
					}
				}
			}
			return super.getToolTipText(event);
		}
	};

	private final TrialTableModel trialTableModel = new TrialTableModel();
	private final JTable trialsTable = new JTable(trialTableModel) {

		@Override
		public String getToolTipText(MouseEvent event) {

			Point pt = event.getPoint();
			if (pt != null) {
				int vrow = trialsTable.rowAtPoint(pt);
				if (vrow >= 0) {
					int mrow = trialsTable.convertRowIndexToModel(vrow);
					if (mrow >= 0) {
						TableModel model = getModel();
						if (model instanceof TrialTableModel) {
							TrialTableModel ttm = (TrialTableModel) model;
							Trial trial = ttm.getTrialAt(mrow);

							StringBuilder html = new StringBuilder("<HTML>");
	                         String trialNote = trial.getTrialNote();
                            if (Check.isEmpty(trialNote)) {
                                html.append("&lt;Trial note is empty&gt;");
                            }
                            else {
                                Html.appendHtmlLines(html, trialNote, lineLengthLimit);
                            }
                            if (developer || ! KdxConstants.PRODUCTION_MODE) {
                                html.append("<HR>id=").append(trial.getTrialId())
                                .append(", kddid=").append(trial.getIdDownloaded());
                            }

							return html.toString();
						}
					}
				}
			}
			return super.getToolTipText(event);
		}

	};

	private final OfflineDataChangeListener offlineDataChangeListener = new OfflineDataChangeListener() {

		@Override
		public void offlineDataChanged(Object source, String reason, KdxploreDatabase oldDb, KdxploreDatabase newDb) {
			if (oldDb != null) {
				oldDb.removeEntityChangeListener(trialChangeListener);
				oldDb.removeEntityChangeListener(traitChangeListener);
			}
			if  (newDb != null) {
				newDb.addEntityChangeListener(trialChangeListener);
				newDb.addEntityChangeListener(traitChangeListener);
			}
			refreshTrialTableModel();
		}

		@Override
		public void trialUnitsAdded(Object source, int trialId) {
			trialTableModel.trialChanged(trialId);
		}

	};

	private Map<Trial, TrialPayload> payloadByRecord = new HashMap<>();

	private TrialChangeListener trialChangeListener = new TrialChangeListener() {

		@Override
		public void entityAdded(KDSmartDatabase db, Trial trial) {
			trialAdded(trial);
		}

		@Override
		public void entityChanged(KDSmartDatabase db, Trial trial) {
			trialTableModel.trialChanged(trial.getTrialId());
		}

		@Override
		public void entitiesRemoved(KDSmartDatabase db, Set<Integer> trialIds) {
			trialTableModel.removeTrials(trialIds.toArray(new Integer[trialIds.size()]));
		}

		@Override
		public void listChanged(KDSmartDatabase db, int nChanges) {
			refreshTrialTableModel();
		}
	};

	private TraitChangeListener traitChangeListener = new TraitChangeListener() {

		@Override
		public void entityAdded(KDSmartDatabase db, Trait trait) {
			refreshTrialTableModel();
		}

		@Override
		public void entityChanged(KDSmartDatabase db, Trait trait) {
			trialTraitsTableModel.traitChanged(trait);
		}

		@Override
		public void entitiesRemoved(KDSmartDatabase db, Set<Integer> traitIds) {
			refreshTrialTableModel();
		}

		@Override
		public void listChanged(KDSmartDatabase db, int nChanges) {
			refreshTrialTableModel();
		}
	};

	private final Action addTraitAction = new AbstractAction("Add...") {
		@Override
		public void actionPerformed(ActionEvent e) {
		    doAddTraitsAndOrInstances();
		}
	};

	private final Action setScoringOrderAction = new AbstractAction("Scoring...") {
		@Override
		public void actionPerformed(ActionEvent e) {

		    final Trial selectedTrial = trialTraitsTableModel.getSelectedTrial();
		    if (selectedTrial == null) {
		        return;
		    }
		    ChangeTraitScoringOrderDialog dlg = new ChangeTraitScoringOrderDialog(
		            GuiUtil.getOwnerWindow(TrialOverviewPanel.this),
		            trialTraitsTableModel.getTraitList());
		    dlg.setLocationRelativeTo(trialTraitsTable);
		    dlg.setVisible(true);

		    if (dlg.newTraitOrder != null) {

		        Map<Integer,Integer> orderByTraitId = new HashMap<>();
		        int scoringSortOrder = 0;
		        for (Trait t : dlg.newTraitOrder) {
		            orderByTraitId.put(t.getTraitId(), scoringSortOrder);
		            ++scoringSortOrder;
		        }

                KDSmartDatabase kdsdb = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase();

                final Map<Integer, List<TraitInstance>> traitInstancesByTraitId = new HashMap<>();
                Predicate<TraitInstance> visitor = new Predicate<TraitInstance>() {
                    @Override
                    public boolean evaluate(TraitInstance ti) {
                        List<TraitInstance> list = traitInstancesByTraitId.get(ti.getTraitId());
                        if (list == null) {
                            list = new ArrayList<>();
                            traitInstancesByTraitId.put(ti.getTraitId(), list);
                        }
                        list.add(ti);
                        return Boolean.TRUE;
                    }
                };
                try {
                    kdsdb.visitTraitInstancesForTrial(selectedTrial.getTrialId(),
                            WithTraitOption.ONLY_NON_CALC_TRAITS, visitor);

                    Set<TraitInstance> traitInstances = new HashSet<>();

                    for (Integer traitId : traitInstancesByTraitId.keySet()) {
                        List<TraitInstance> list = traitInstancesByTraitId.get(traitId);

                        Integer scoringOrder = orderByTraitId.get(traitId);
                        if (scoringOrder != null) {
                            // Don't do the CALC traits
                            for (TraitInstance ti : list) {
                                ti.setScoringSortOrder(scoringOrder);
                            }
                            traitInstances.addAll(list);
                        }
                    }
                    kdsdb.saveTraitInstances(traitInstances);

                    refreshTrialTableModel();
//                    trialTraitsTableModel.setNewTraitOrder(selectedTrial, dlg.newTraitOrder);
                }
                catch (IOException err) {
                    MsgBox.error(TrialOverviewPanel.this, err.getMessage(), "Unable to Save Changes - Database Error");
                }
		    }
		}
	};

	private final Action refreshTrialTraitsAction = new AbstractAction("Refresh") {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshTrialTableModel();
            new Toast(refreshTrialTraitsButton, "Trial/Trait Data Refreshed", Toast.SHORT).show();
        }
	};
	private final JButton refreshTrialTraitsButton = new JButton(refreshTrialTraitsAction);

	private final Action removeTraitAction = new AbstractAction("Remove") {
		@Override
		public void actionPerformed(ActionEvent e) {
		    List<Integer> modelRows = GuiUtil.getSelectedModelRows(trialTraitsTable);

		    try {
		        Trial trial = trialTraitsTableModel.getSelectedTrial();

                Map<Trait, Integer> sampleCountsByTrait = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase()
                    .getSampleCountsByTrait(trial.getTrialId(), KdxSample.class);

                Function<Trait, Boolean> canDeleteClassifier = new Function<Trait, Boolean>() {
                    @Override
                    public Boolean apply(Trait trait) {
                        Integer count = sampleCountsByTrait.get(trait);
                        return count==null ? true : count <= 0;
                    }
                };
                Map<Boolean, List<Trait>> traitsByCanDelete = modelRows.stream()
                    .map(trialTraitsTableModel::getTraitAt)
                    .filter(t -> t != null)
                    .collect(Collectors.groupingBy(canDeleteClassifier));

                List<Trait> canDelete = traitsByCanDelete.get(Boolean.TRUE);
                List<Trait> cannotDelete = traitsByCanDelete.get(Boolean.FALSE);

                boolean proceed = false;
                if (Check.isEmpty(cannotDelete)) {
                    // They are all valid for removal
                    switch (canDelete.size()) {
                    case 0:
                        break;
                    case 1:
                        proceed = true;
                        break;
                    default:
                        if (JOptionPane.YES_OPTION ==
                            JOptionPane.showConfirmDialog(TrialOverviewPanel.this,
                                    HelpUtils.makeListInScrollPane(canDelete, Trait::getTraitName),
                                    "Confirm Trait Removal",
                                    JOptionPane.YES_NO_OPTION))
                        {
                            proceed = true;
                        }
                    }
                }
                else {
                    // Some are not valid for removal

                    Box box = Box.createVerticalBox();
                    box.add(GuiUtil.createLabelSeparator("Traits with data"));
                    box.add(HelpUtils.makeListInScrollPane(cannotDelete, Trait::getTraitName));

                    if (Check.isEmpty(canDelete)) {
                        // ... and none are valid!
                        JOptionPane.showMessageDialog(TrialOverviewPanel.this,
                                box,
                                "Unable to Remove",
                                JOptionPane.WARNING_MESSAGE);
                    }
                    else {
                        // ... and some are valid

                        box.add(GuiUtil.createLabelSeparator("Remove:"));
                        box.add(HelpUtils.makeListInScrollPane(canDelete, Trait::getTraitName));

                        if (JOptionPane.YES_OPTION ==
                                JOptionPane.showConfirmDialog(TrialOverviewPanel.this,
                                        box,
                                        "Confirm Trait Removal",
                                        JOptionPane.YES_NO_OPTION))
                            {
                                proceed = true;
                            }
                    }
                }

                if (proceed) {
                    KDSmartDatabase kdsdb = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase();

                    BatchHandler<Boolean> callable = new BatchHandler<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            kdsdb.removeTraitsFromTrial(trial, canDelete);
                            return Boolean.TRUE;
                        }

                        @Override
                        public boolean checkSuccess(Boolean b) {
                            return b!=null && b;
                        }
                    };
                    Either<Exception, Boolean> either = kdsdb.doBatch(callable);
                    if (either.isRight()) {

                        refreshTrialTableModel();

                        MsgBox.info(TrialOverviewPanel.this,
                                HelpUtils.makeListInScrollPane(canDelete, Trait::getTraitName),
                                "Traits Removed");
                    }
                    else {
                        MsgBox.error(TrialOverviewPanel.this, either.left(), "Database Error");
                    }
                }

		    } catch (IOException e1) {
		        MsgBox.error(TrialOverviewPanel.this, e1, "Remove Traits - Database Error");
            }
		}
	};

	private final MessagePrinter messagePrinter;

	private TraitExplorer traitExplorer;

	public TrialOverviewPanel(String title,
			OfflineData offdata,
			TrialExplorerManager manager,
			FileListTransferHandler flth,
			MessagePrinter mp,
			final Closure<List<Trial>> onTrialSelected)
	{
		super(new BorderLayout());

		offlineData = offdata;
		KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
		if (kdxdb != null) {
			kdxdb.addEntityChangeListener(trialChangeListener);
			kdxdb.addEntityChangeListener(traitChangeListener);
		}

		this.messagePrinter = mp;

		TableTransferHandler tth = TableTransferHandler.initialiseForCopySelectAll(trialsTable, true);
		trialsTable.setTransferHandler(new ChainingTransferHandler(flth, tth));
		trialsTable.setAutoCreateRowSorter(true);

		trialsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					List<Trial> selectedTrials = getSelectedTrials();
					if (selectedTrials.size() == 1) {
						trialTraitsTableModel.setSelectedTrial(selectedTrials.get(0));
					}
					else {
						trialTraitsTableModel.setSelectedTrial(null);
					}
					onTrialSelected.execute(selectedTrials);
				}
			}
		});
		trialsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2
						&& SwingUtilities.isLeftMouseButton(e)) {
					fireEditCommand(e);
				}
			}
		});

		GuiUtil.setVisibleRowCount(trialsTable, MAX_INITIAL_VISIBLE_TRIAL_ROWS);

		offlineData.addOfflineDataChangeListener(offlineDataChangeListener);

		trialTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateRefreshAction();
            }
        });
		trialTraitsTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				updateAddTraitAction();
				updateRemoveTraitAction();
				updateScoringOrderAction();
			}
		});
		trialTraitsTable.addMouseListener(new MouseAdapter() {

			List<Trait> selectedTraits;
			JPopupMenu popupMenu;
			Action showTraitsAction = new AbstractAction("Select in Trait Explorer") {
				@Override
				public void actionPerformed(ActionEvent e) {
					manager.showTraitsInTraitExplorer(selectedTraits);
				}
			};

			@Override
			public void mouseClicked(MouseEvent e) {

				if (SwingUtilities.isLeftMouseButton(e) && 2 == e.getClickCount()) {
					// Start editing the Trait
					e.consume();
					int vrow = trialTraitsTable.rowAtPoint(e.getPoint());
					if (vrow >= 0) {
						int mrow = trialTraitsTable.convertRowIndexToModel(vrow);
						if (mrow >= 0) {
							Trait trait = trialTraitsTableModel.getTraitAt(mrow);
							if (trait != null) {
							    traitExplorer.startEditing(trait);;
							}
						}
					}
				}
				else if (SwingUtilities.isRightMouseButton(e) && 1 == e.getClickCount()) {
					// Select the traits in the traitExplorer
					e.consume();
					List<Integer> modelRows = GuiUtil.getSelectedModelRows(trialTraitsTable);
					if (! modelRows.isEmpty()) {
						selectedTraits = modelRows.stream()
								.map(trialTraitsTableModel::getTraitAt)
								.collect(Collectors.toList());

						if (popupMenu == null) {
							popupMenu = new JPopupMenu();
							popupMenu.add(showTraitsAction);
						}
						Point pt = e.getPoint();
						popupMenu.show(trialTraitsTable, pt.x, pt.y);
					}
				}
			}
		});
		trialTraitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					updateRemoveTraitAction();
				}
			}
		});
		updateAddTraitAction();
		updateRemoveTraitAction();
		updateScoringOrderAction();
		updateRefreshAction();

		KDClientUtils.initAction(ImageId.REFRESH_24, refreshTrialTraitsAction, "Refresh");
		KDClientUtils.initAction(ImageId.MINUS_GOLD_24, removeTraitAction, "Remove selected Traits");
		KDClientUtils.initAction(ImageId.PLUS_BLUE_24, addTraitAction, "Add Traits to Trial");
		KDClientUtils.initAction(ImageId.TRAIT_ORDER_24, setScoringOrderAction, "Define Trait Scoring Order");

		Box buttons = Box.createHorizontalBox();

        buttons.add(new JButton(setScoringOrderAction));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(addTraitAction));
		buttons.add(new JButton(removeTraitAction));
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(refreshTrialTraitsButton);

		JPanel traitsPanel = new JPanel(new BorderLayout());
		traitsPanel.add(GuiUtil.createLabelSeparator("Uses Traits", buttons),
				BorderLayout.NORTH);
		traitsPanel.add(new PromptScrollPane(trialTraitsTable,
				"If the (single) selected Trial has Traits they will appear here"),
				BorderLayout.CENTER);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				new JScrollPane(trialsTable),
				traitsPanel);
		splitPane.setResizeWeight(0.5);

		add(splitPane, BorderLayout.CENTER);
	}

    public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
	}

	protected void fireEditCommand(MouseEvent e) {
		ActionEvent event = new ActionEvent(this, ++EVENT_COUNT, EDIT_TRIAL_COMMAND);
		for (ActionListener l : listenerList.getListeners(ActionListener.class)) {
			l.actionPerformed(event);
		}
	}

	public List<Trial> getAllTrials() {
	    List<Trial> result = new ArrayList<>();
	    for (int rowIndex = trialTableModel.getRowCount(); --rowIndex >= 0; ) {
	        result.add(trialTableModel.getTrialAt(rowIndex));
	    }
	    return result;
	}

	public Trial getSelectedTrial() {
		Trial result = null;
		if (trialsTable.getSelectedRowCount() == 1) {
			int vrow = trialsTable.getSelectedRow();
			if (vrow >= 0) {
				int mrow = trialsTable.convertRowIndexToModel(vrow);
				if (mrow >= 0) {
					result = trialTableModel.getTrialAt(mrow);
				}
			}
		}
		return result;
	}

	public void addListSelectionListener(ListSelectionListener l) {
		trialsTable.getSelectionModel().addListSelectionListener(l);
	}

	public boolean hasNoTrials() {
		return trialTableModel.getRowCount() <= 0;
	}

	protected TrialPayload getPayload(Trial tr, boolean autocreate) {
		TrialPayload result = payloadByRecord.get(tr);
		if (result == null && autocreate) {
			result = new TrialPayloadImpl();
			payloadByRecord.put(tr, result);
		}
		return result;
	}

	// TODO remember why I put this in - I think it is to help show errors for a specific Trial
	// when TrialExplorerPanel does collectTrialInfo()
	public void setPayloadErrorMessage(Trial trial, String errmsg) {
		TrialPayload payload = getPayload(trial, true);
		payload.setErrorMessage(errmsg);

		handleTrialChanged(trial);
	}

	protected void handleTrialChanged(Trial trial) {
		trialTableModel.trialChanged(trial.getTrialId());
	}

	public void refreshTrialTraits() {
	    refreshTrialTableModel();
	}

	private void refreshTrialTableModel() {

        KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
        if (kdxdb == null) {
            return;
        }
	    Trial selectedTrial = trialTraitsTableModel.getSelectedTrial();

        ListSelectionModel lsm = trialsTable.getSelectionModel();
		try {
	        lsm.setValueIsAdjusting(true);
//			trialsTable.clearSelection();

			Map<Trial, List<SampleGroup>> trialsAndSampleGroups = kdxdb.getTrialsAndSampleGroups(KdxploreDatabase.WithSamplesOption.WITHOUT_SAMPLES);
			trialTableModel.setTrialsAndSampleGroups(trialsAndSampleGroups);

			KDSmartDatabase kdsdb = kdxdb.getKDXploreKSmartDatabase();

			Map<Trial,Map<Trait,Integer>> traitSsoByTrial = new HashMap<>();

			for (Trial trial : trialsAndSampleGroups.keySet()) {

			    Predicate<TraitInstance> traitInstanceVisitor = new Predicate<TraitInstance>() {
					@Override
					public boolean evaluate(TraitInstance ti) {
						Map<Trait,Integer> map = traitSsoByTrial.get(trial);
						if (map == null) {
							map = new HashMap<>();
							traitSsoByTrial.put(trial, map);
						}
						map.put(ti.trait, ti.getScoringSortOrder());
						return Boolean.TRUE;
					}
				};

				kdsdb.visitTraitInstancesForTrial(trial.getTrialId(),
				        WithTraitOption.ALL_WITH_TRAITS,
						traitInstanceVisitor);
			}

			trialTraitsTableModel.setTraitsByTrial(traitSsoByTrial);

		} catch (IOException e) {
			MsgBox.error(TrialOverviewPanel.this, e.getMessage(), "Problem Getting Trials");
		} finally {
            lsm.clearSelection();

		    int modelRow = trialTableModel.indexOfTrial(selectedTrial);
		    if (modelRow >= 0) {
		        int viewRow = trialsTable.convertRowIndexToView(modelRow);
		        if (viewRow >= 0) {
		            lsm.setSelectionInterval(viewRow, viewRow);
		        }
		    }

	        lsm.setValueIsAdjusting(false);
		}
	}

	private void trialAdded(Trial trial) {
		refreshTrialTableModel();
        trialTraitsTableModel.setSelectedTrial(trial);
		selectTrial(trial);
	}

	public void handleTrialsLoaded(List<Trial> newTrials) {
		refreshTrialTableModel();
		if (! newTrials.isEmpty()) {
			selectTrial(newTrials.get(newTrials.size() - 1));
		}
	}

	private void selectTrial(Trial trial) {
		ListSelectionModel lsm = trialsTable.getSelectionModel();
		lsm.setValueIsAdjusting(true);
		try {
			lsm.clearSelection();
			int mrow = trialTableModel.indexOfTrial(trial);
			if (mrow >= 0) {
				int vrow = trialsTable.convertRowIndexToView(mrow);
				if (vrow >= 0) {
					lsm.addSelectionInterval(vrow, vrow);
				}
			}
		}
		finally {
			lsm.setValueIsAdjusting(false);
		}
	}

	public List<Trial> getSelectedTrials() {
		List<Trial> trials = new ArrayList<>();

		List<Integer> modelRows = GuiUtil.getSelectedModelRows(trialsTable);
		if (! modelRows.isEmpty()) {
			for (int row : modelRows) {
				Trial t = trialTableModel.getTrialAt(row);
				trials.add(t);
			}
		}
		return trials;
	}

	public int getTrialCount() {
		return trialTableModel.getRowCount();
	}

    private void updateScoringOrderAction() {
        setScoringOrderAction.setEnabled(trialTraitsTableModel.getRowCount() > 0);
    }

    private void updateRefreshAction() {
        refreshTrialTraitsAction.setEnabled(trialTableModel.getRowCount() > 0);
    }

	private void updateRemoveTraitAction() {
        removeTraitAction.setEnabled(trialTraitsTable.getSelectedRowCount() > 0);
	}

	private void updateAddTraitAction() {
		addTraitAction.setEnabled(trialTraitsTableModel.getSelectedTrial() != null);
	}

//	class SortOrderAndTrait {
//		public final int scoringSortOrder;
//		public final Trait trait;
//
//		SortOrderAndTrait(int sso, Trait t) {
//			scoringSortOrder = sso;
//			trait = t;
//		}
//
//		public Trait getTrait() {
//			return trait;
//		}
//	}

	public void setTraitExplorer(TraitExplorer traitExplorer) {
		this.traitExplorer = traitExplorer;
	}

    private void doAddTraitsAndOrInstances() {
        try {
            Trial selectedTrial = trialTraitsTableModel.getSelectedTrial();
            if (selectedTrial == null) {
            	return;
            }

            Set<Integer> currentTraitIds = trialTraitsTableModel.getTraitList().stream()
                    .map(Trait::getTraitId)
                    .collect(Collectors.toSet());

            List<Trait> unusedTraits = offlineData.getKdxploreDatabase().getAllTraits()
                .stream()
                .filter(trt -> ! currentTraitIds.contains(trt.getTraitId()))
                .collect(Collectors.toList());

            if (unusedTraits.isEmpty()) {
            	MsgBox.info(TrialOverviewPanel.this,
            			"This Trial already has all the Traits assigned",
            			Msg.TITLE_ADD_TRAITS_TO_TRIAL(selectedTrial.getTrialName()));
            	return;
            }

            AddTraitsDialog dlg = new AddTraitsDialog(
                    GuiUtil.getOwnerWindow(TrialOverviewPanel.this),
                    selectedTrial,
                    unusedTraits);

            Dimension sz = dlg.getSize();
            dlg.setSize((int) (sz.width * 1.4), sz.height);

            dlg.setVisible(true);

            Map<Trait,List<Integer>> map = dlg.getTraitAndInstanceNumber();
            if (map != null) {
                doAddTraitInstances(selectedTrial, map);
            }
        } catch (IOException e1) {
            MsgBox.error(TrialOverviewPanel.this,
                    e1,
                    "Add Traits - Database Error");
        }
    }

    private void doAddTraitInstances(Trial selectedTrial, Map<Trait, List<Integer>> instanceNumbersByTrait) throws IOException {

        if (instanceNumbersByTrait.isEmpty()) {
            return;
        }

        int trialId = selectedTrial.getTrialId();

        int maxSso = 0;
        Bag<Integer> ssoUseCount = new HashBag<>();

        Map<Trait, Integer> traitSsoMap = trialTraitsTableModel.getTraitsSsoForTrial(selectedTrial);
        if (! Check.isEmpty(traitSsoMap)) {
            for (Integer sso : traitSsoMap.values()) {
                if (sso != null) {
                    maxSso = Math.max(maxSso, sso);
                    ssoUseCount.add(sso);
                }
            }
        }

        TraitNameStyle traitNameStyle = selectedTrial.getTraitNameStyle();

        final int firstInstanceNumber = traitNameStyle.getFirstInstanceNumber();

        int maxSsoIncrement = 1;
        if (ssoUseCount.uniqueSet().size() == 1
            &&
            maxSso == 0
            &&
            ssoUseCount.getCount(maxSso) > 1)
        {
            // Special hack for Trials that haven't yet been "sorted".
            // (i.e. all the SSOs are the same value
            maxSsoIncrement = 0;
        }

        List<Trait> ignored = new ArrayList<>();
        Map<TraitLevel, List<TraitInstance>> instancesByLevel = new HashMap<>();
        boolean multipleTraitInstances = false;

        Set<TraitInstance> traitInstances = new LinkedHashSet<>();
        for (Trait trait : instanceNumbersByTrait.keySet()) {

            switch (trait.getTraitLevel()) {
            case PLOT:
            case SPECIMEN:
                break;
            default:
                ignored.add(trait);
                continue;
            }
            maxSso += maxSsoIncrement;

            List<Integer> instancesNumbersWanted = instanceNumbersByTrait.get(trait);
            if (instancesNumbersWanted.size() > 1) {
                multipleTraitInstances = true;
            }

            for (Integer instanceNumber : instancesNumbersWanted) {
                TraitInstance ti = new TraitInstance();
                if (firstInstanceNumber <= 0) {
                    ti.setInstanceNumber(instanceNumber - 1);
                }
                else {
                    ti.setInstanceNumber(instanceNumber);
                }
                ti.setScoringSortOrder(maxSso);
                ti.setTraitId(trait.getTraitId());
                ti.setTrialId(trialId);
                ti.setUsedForScoring(true);

                traitInstances.add(ti);

                List<TraitInstance> list = instancesByLevel.get(trait.getTraitLevel());
                if (list == null) {
                    list = new ArrayList<>();
                    instancesByLevel.put(trait.getTraitLevel(), list);
                }
                list.add(ti);
            }
        }

        KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
        KDSmartDatabase kdsdb = kdxdb.getKDXploreKSmartDatabase();

        Map<Integer, DeviceIdentifier> devidMap = kdxdb.getDeviceIdentifierMap();
        Optional<DeviceIdentifier> opt_devid = devidMap.values().stream().filter(devid -> DeviceType.EDITED.equals(devid.getDeviceType())).findFirst();

        List<KdxSample> samples;
        if (opt_devid.isPresent()) {
            DeviceIdentifier curated = opt_devid.get();
            List<SampleGroup> sampleGroups = kdxdb.getSampleGroups(trialId, KdxploreDatabase.WithSamplesOption.WITH_SAMPLES);
            int curatedSampleGroupId = curated.getDeviceIdentifierId();

            Optional<SampleGroup> opt_sg = sampleGroups.stream().filter(sg -> curatedSampleGroupId == sg.getDeviceIdentifierId()).findFirst();
            if (opt_sg.isPresent()) {
                samples = new ArrayList<>();

                List<TraitInstance> plotInstances = instancesByLevel.get(TraitLevel.PLOT);
                List<TraitInstance> subPlotInstances = instancesByLevel.get(TraitLevel.SPECIMEN);

                Consumer<KdxSample> sampleConsumer = new Consumer<KdxSample>() {
                    @Override
                    public void accept(KdxSample sample) {
                        samples.add(sample);
                    }
                };
                TrialItemVisitor<Plot> plotVisitor = new TrialItemVisitor<Plot>() {
                    @Override
                    public void setExpectedItemCount(int count) {}
                    @Override
                    public boolean consumeItem(Plot plot) {
                        DatabaseUtil.createSamples(
                                trialId,
                                plot,
                                curatedSampleGroupId,
                                null, // previousSamplesByKey
                                plotInstances,
                                subPlotInstances,
                                sampleConsumer);
                        return true;
                    }
                };

                kdsdb.visitPlotsForTrial(trialId, SampleGroupChoice.NO_TAGS_SAMPLE_GROUP,
                        KDSmartDatabase.WithPlotAttributesOption.WITHOUT_PLOT_ATTRIBUTES,
                        plotVisitor);
            }
            else {
                samples = null;
            }
        }
        else {
            samples = null;
        }

        BatchHandler<Void> batchHandler = new BatchHandler<Void>() {
            @Override
            public Void call() throws Exception {
                kdsdb.saveTraitInstances(traitInstances);
                if (! Check.isEmpty(samples)) {
                    kdsdb.saveMultipleSamples(samples, false);
                }
                return null;
            }

            @Override
            public boolean checkSuccess(Void t) {
                return true;
            }
        };

        boolean saved = true;
        Either<Exception, Void> either = kdsdb.doBatch(batchHandler);
        if (either.isLeft()) {
            saved = false;
            MsgBox.error(TrialOverviewPanel.this, either.left(), "Database Error");
        }
        refreshTrialTableModel();

        trialTraitsTableModel.setSelectedTrial(selectedTrial);

        if (saved) {
            String prefix = multipleTraitInstances ? "Trait Instances Added:\n" : "Traits Added:\n";

            Function<Trait, String> nameFactory = new Function<Trait, String>() {
                @Override
				public String apply(Trait trait) {
                    List<Integer> instanceNumbers = instanceNumbersByTrait.get(trait);
                    if (instanceNumbers == null) {
                        return null;
                    }
                    else {
                        return instanceNumbers.stream().map(inum -> inum.toString())
                                    .collect(Collectors.joining(",", trait.getTraitName() + ": ", ""));
                    }
                }
            };

            String selection = instanceNumbersByTrait.keySet().stream()
                .map(nameFactory)
                .filter(n -> n != null)
                .collect(Collectors.joining("\n", prefix, "\n------"));  //$NON-NLS-1$//$NON-NLS-2$

            messagePrinter.println(selection);

            if (samples != null) {
                messagePrinter.println("Samples Added: " + samples.size());
            }
        }
    }
}
