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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.BatchHandler;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialChangeListener;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.db.util.SampleOrder;
import com.diversityarrays.kdsmart.scoring.DateDiffChoice;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.OfflineDataChangeListener;
import com.diversityarrays.kdxplore.data.dal.ScoreRatio;
import com.diversityarrays.kdxplore.data.dal.TrialData;
import com.diversityarrays.kdxplore.data.dal.TrialDataCellRenderer;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.editing.EntityPropertiesTable;
import com.diversityarrays.kdxplore.editing.EntityPropertiesTable.PropertyChangeConfirmer;
import com.diversityarrays.kdxplore.editing.EntityPropertiesTableModel;
import com.diversityarrays.kdxplore.editing.GenericTrialPropertyRenderer;
import com.diversityarrays.kdxplore.editing.PropertiesTableLegendPanel;
import com.diversityarrays.kdxplore.editing.TrialPropertiesTableModel;
import com.diversityarrays.kdxplore.field.TrialLayoutEditorDialog;
import com.diversityarrays.kdxplore.trialmgr.TrialManagerPreferences;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.SunSwingDefaultCellHeaderRenderer;

import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.util.MessagePrinter;

/**
 * Show brief details about a trial.
 * @author brianp
 */
public class TrialViewPanel extends JPanel {

	protected TrialChangeListener trialChangeListener = new TrialChangeListener() {

		@Override
		public void entityAdded(KDSmartDatabase db, Trial t) {
			if (trialPropertiesTableModel.isCurrentTrial(t.getTrialId())) {
				setCurrentTrialDetails(t, true);
			}
		}

		@Override
		public void entityChanged(KDSmartDatabase db, Trial t) {
			if (trialPropertiesTableModel.isCurrentTrial(t.getTrialId())) {
				setCurrentTrialDetails(t, true);
			}
		}

		@Override
		public void entitiesRemoved(KDSmartDatabase db, Set<Integer> trialIds) {
			for (Integer trialId : trialIds) {
				if (isMyTrialId(trialId)) {
					setTrial(null);
					break;
				}
			}
		}

		@Override
		public void listChanged(KDSmartDatabase db, int nChanges) {
		}

		private boolean isMyTrialId(int id) {
			return trial != null && (id == trial.getTrialId());
		}
	};

	private final OfflineDataChangeListener offlineDataChangeListener = new OfflineDataChangeListener() {

		@Override
		public void offlineDataChanged(Object source, String reason,
				KdxploreDatabase oldDb, KdxploreDatabase newDb) {
			if (oldDb != null) {
				oldDb.removeEntityChangeListener(trialChangeListener);
			}
			if (newDb != null) {
				newDb.addEntityChangeListener(trialChangeListener);
			}
			setTrial(null);
			trialPropertiesTableModel.setDatabase(newDb);
		}

		@Override
		public void trialUnitsAdded(Object source, int trialId) {
			if (trialPropertiesTableModel.isCurrentTrial(trialId)) {
				try {
					trialPropertiesTableModel.setPlotCount(offlineData.getPlotCount(trial.getTrialId()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private Trial trial;
	private Point elapsedDaysTraitsValueMinMax;

	private final OfflineData offlineData;

	private Action editAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int vrow = trialPropertiesTable.getSelectedRow();
			if (vrow >= 0) {
				trialPropertiesTable.editCellAt(vrow, 1);
			}
		}
	};
	
	private final Action exportSamplesAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			TableModel model = trialDataTable.getModel();
			if (! (model instanceof TrialData)) {
			    return;
			}

			TrialData trialData = (TrialData) model;
			if (trialDataTableHeaderRenderer.columnSelected >= 0) {

				Pair<DeviceIdentifier, SampleGroup> pair = getSelectedDeviceAndSampleGroup(trialData);
				
				if (pair==null) {
					// Must be the TraitInstances
				    MsgBox.info(TrialViewPanel.this, 
				            "Please select a different column heading",
				            "Export Traits");
				}
				else {
					DeviceIdentifier devid = pair.first;
					SampleGroup sampleGroup = pair.second;
					
					String dlgTitle = SampleGroupExportDialog.createTitle(devid.getDeviceType(), trial);
					
					KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
					Set<Integer> excludeTraitIds = SampleGroupExportDialog.getExcludedTraitIds(TrialViewPanel.this, dlgTitle, kdxdb, sampleGroup);

					if (excludeTraitIds != null) {
    					SampleGroupExportDialog dlg = new SampleGroupExportDialog(
    							GuiUtil.getOwnerWindow(TrialViewPanel.this),
    							dlgTitle,
    							trial,
    							kdxdb,
    							devid.getDeviceType(),
    							devid, 
    							sampleGroup,
    							excludeTraitIds);
    					dlg.setLocationRelativeTo(trialDataTable);
    					dlg.setVisible(true);
					}
				}
			}
			else {
			    if (trialData.hasExportableSampleGroups()) {
			        MsgBox.info(TrialViewPanel.this, 
			                "Click on a heading to Select", 
			                "No Dataset Selected");
			    }
			    else {
	                int answer = JOptionPane.showConfirmDialog(TrialViewPanel.this, 
	                        "Do you want to create a 'Scoring Set'?", 
	                        "No Exportable Datasets Exist", 
	                        JOptionPane.YES_NO_OPTION, 
	                        JOptionPane.QUESTION_MESSAGE);
	                if (JOptionPane.YES_OPTION == answer) {
	                    doAddSampleDataset();
	                }
			    }
			}
		}		
	};
	
    private final Action addSampleGroupAction = new AbstractAction("Add") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doAddSampleDataset();
        }
    };

    private final Action removeTraitInstancesAction = new AbstractAction("Rmv") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doRemoveTraitInstancesWithoutSamples();
        }
    };
			
	private final Action deleteSamplesAction = new AbstractAction("Del") {
		@Override
		public void actionPerformed(ActionEvent e) {
			TableModel model = trialDataTable.getModel();
			if (model instanceof TrialData) {
				TrialData trialData = (TrialData) model;
				
				Pair<DeviceIdentifier, SampleGroup> pair = getSelectedDeviceAndSampleGroup(trialData);
				SampleGroup sampleGroup = null;
				boolean scoringSampleGroup = false;
				if (pair != null) {
				    switch (pair.first.getDeviceType()) {
                    case DATABASE:
                    case EDITED:
                        break;

                    case FOR_SCORING:
                        scoringSampleGroup = true;
                    case KDSMART:
                        sampleGroup = pair.second;
                        break;

                    default:
                        break;
				    
				    }
				}
				
				if (sampleGroup != null) {
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd@HH:mm");
					
                    StringBuilder sb = new StringBuilder();
					if (scoringSampleGroup) {
					    sb.append("Scoring Set:");
					}
					else {
	                    sb.append("Samples from:");
	                    
					}
                    ScoreRatio sr = trialData.getSampleCounts(sampleGroup);
                    sb.append('\n').append(dateFormat.format(sampleGroup.getDateLoaded()))
                        .append(": ")
                        .append(sr.getScored()).append(" / ").append(sr.getTotal());
                    
                    String groupName = sb.toString();

					int answer = JOptionPane.showOptionDialog(TrialViewPanel.this, 
					        sb, 
					        "Confirm Sample Removal", 
					        JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE, 
							null, 
							new String[] { "Delete", "Cancel" }, "Cancel");
					
					if (answer == 0) {
						try {
							offlineData.getKdxploreDatabase().removeSampleGroups(sampleGroup);
							// This refreshes the data on display
							setTrial(trial);
						}
						catch (IOException err) {
							GuiUtil.errorMessage(TrialViewPanel.this, err, "Database Error");
						}
					}
				}

			}
		}
	};
	
	private Pair<DeviceIdentifier, SampleGroup> getSelectedDeviceAndSampleGroup(TrialData trialData) {		
		 Pair<DeviceIdentifier, SampleGroup> result = null;
		if (trialDataTableHeaderRenderer.columnSelected >= 0) {
			int mcol = trialDataTable.convertColumnIndexToModel(trialDataTableHeaderRenderer.columnSelected);
			if (mcol >= 0) {
				Pair<DeviceIdentifier,SampleGroup> pair = trialData.getSampleGroupAtColumn(mcol);
				if (pair != null) {
					result = pair;
				}
			}
		}
		return result;
	}
	
	
	private void doRemoveTraitInstancesWithoutSamples() {
	    TableModel model = trialDataTable.getModel();
	    if (model instanceof TrialData) {
	        TrialData trialData = (TrialData) model;   
	        TraitNameStyle tns = trialData.trial.getTraitNameStyle();

	           
	        List<Integer> modelRows = GuiUtil.getSelectedModelRows(trialDataTable);
	        List<TraitInstance> withData = new ArrayList<>();
	        List<TraitInstance> withoutData = new ArrayList<>();

	        for (Integer row : modelRows) {
	            TraitInstance ti = trialData.getTraitInstanceAt(row);
	            int traitId = ti.getTraitId();
	            int instanceNumber = ti.getInstanceNumber();
	            
	            boolean[] anyScored = new boolean[1];
	            BiPredicate<SampleGroup, KdxSample> sampleVisitor = new BiPredicate<SampleGroup, KdxSample>() {
                    @Override
                    public boolean test(SampleGroup sg, KdxSample s) {
                        if (traitId != s.getTraitId()) {
                            return true; // not the correct trait
                        }
                        if (instanceNumber != s.getTraitInstanceNumber()) {
                            return true; // not the correct trait
                        }
                        // Ok - it is for this traitInstance
                        if (s.hasBeenScored()) {
                            anyScored[0] = true;
                            return false;
                        }
                        return true; // keep looking for scored samples
                    }	                
	            };
	            trialData.visitSampleGroupSamples(sampleVisitor);
	            if (anyScored[0]) {
	                withData.add(ti);
	            }
	            else {
	                withoutData.add(ti);
	            }
	        }
	        
	        if (withoutData.isEmpty()) {
	            if (withData.isEmpty()) {
	                MsgBox.warn(TrialViewPanel.this, "Nothing to do!", "Remove Trait/Instances");
	            }
	            else {
	                String s = withData.stream().map(ti -> tns.makeTraitInstanceName(ti))
	                        .collect(Collectors.joining("</li><li>", "<html>With Data:<ul><li>", "</li></ul>"));
	                MsgBox.warn(TrialViewPanel.this, s, "Cannot remove Traits with Data");
	            }
	        }
	        else {
                String s = withoutData.stream().map(ti -> tns.makeTraitInstanceName(ti))
                        .collect(Collectors.joining("</li><li>", "<html><ul><li>", "</li></ul>"));
	            int answer = JOptionPane.showConfirmDialog(TrialViewPanel.this, 
	                    s,
	                    "Confirm Trait Instance Removeal",
	                    JOptionPane.YES_NO_OPTION);
	            if (JOptionPane.YES_OPTION == answer) {
                    try {
                        offlineData.getKdxploreDatabase().removeTraitInstancesFromTrial(trial, withoutData);
                    }
                    catch (IOException e) {
                        MsgBox.warn(TrialViewPanel.this, e, "Remove Trait/Instances from Trial");
                    }
                    finally {
                        setCurrentTrialDetails(trial, false);
                        onTraitInstancesRemoved.accept(trial);
                    }
	            }
	        }
	    }
	}
	
	private void doAddSampleDataset() {
	    
	    TableModel model = trialDataTable.getModel();
        if (! (model instanceof TrialData)) {
            return;
        }
	    
        TrialData trialData = (TrialData) model;
	    // Need to choose Traits.
	    try {
//            Set<Trait> trialTraits = offlineData.getKdxploreDatabase().getTrialTraits(trial.getTrialId());
            
            AddScoringSetDialog dlg = new AddScoringSetDialog(
                    GuiUtil.getOwnerWindow(TrialViewPanel.this), 
                    offlineData.getKdxploreDatabase(), 
                    trial, 
//                    trialTraits,
                    trialData.traitInstancesByTrait,
                    trialData.getCuratedSampleGroup());
            GuiUtil.centreOnOwner(dlg);
            dlg.setVisible(true);
            if (dlg.addedSampleGroup) {
                setTrial(trial);
            }
        }
//        catch (IOException e) {
//            MsgBox.error(TrialViewPanel.this, e.getMessage(), "Add Sample Dataset - Database Error");
//        }
	    finally {
	        
	    }
    }


//	private final BackgroundRunner backgroundRunner;

	private final MessagePrinter messagePrinter;

	private JLabel errorMessage = new JLabel();

	private final TrialPropertiesTableModel trialPropertiesTableModel = new TrialPropertiesTableModel();

	private PropertyChangeConfirmer propertyChangeConfirmer = new PropertyChangeConfirmer() {
		
		private boolean isForTrialPlantingDate(PropertyDescriptor pd) {
			Method m = pd.getWriteMethod();
			return (m != null 
				&& 
				Trial.class.isAssignableFrom(m.getDeclaringClass())
				&&
				"setTrialPlantingDate".equals(m.getName()));
		}

		@Override
		public boolean isChangeAllowed(PropertyDescriptor pd) {
			boolean result = true;
		
			if (elapsedDaysTraitsValueMinMax != null) {
				if (isForTrialPlantingDate(pd) 
					&&
					trial.getTrialPlantingDate() != null)
				{
					int answer = JOptionPane.showConfirmDialog(
							TrialViewPanel.this, 
							"This Trial already contains values for ELAPSED_DAYS Traits\n"
							+ "\nin the range " + elapsedDaysTraitsValueMinMax.x + " to " + elapsedDaysTraitsValueMinMax.y
							+ "\n\nDo you really want to change the Planting Date?"
							, 
							"Confirm Planting Date Change", 
							JOptionPane.YES_NO_OPTION);
					result = JOptionPane.YES_OPTION==answer;
				}
			}
					
			return result;
		}

		private Object oldValue;
		@Override
		public void setValueBeforeChange(Object oldValue) {
			this.oldValue = oldValue;
		}

		@Override
		public boolean valueChangeCanCommit(PropertyDescriptor pd, Object newValue) {
			
			boolean result = true;
			
			if (newValue != null && newValue instanceof java.util.Date && isForTrialPlantingDate(pd)) {
				
				if (oldValue != null && oldValue instanceof java.util.Date) {
					java.util.Date newTrialPlantingDate = (java.util.Date) newValue;
					java.util.Date oldTrialPlantingDate = (java.util.Date) oldValue;
					
					result = changeElapsedDaysValues(oldTrialPlantingDate, newTrialPlantingDate);
				}
			}
			
			return result;
		}

	};


//	private Transformer<PropertyDescriptor,Boolean> propertyChangeConfirmation = new Transformer<PropertyDescriptor, Boolean>() {
//		@Override
//		public Boolean transform(PropertyDescriptor pd) {
//			Boolean result = Boolean.TRUE;
//			if (trialHasAnyScoredElapsedDaysTraits) {
//				Method m = pd.getWriteMethod();
//				if (m != null) {
//					if (Trial.class.isAssignableFrom(m.getDeclaringClass())
//							&&
//						"setTrialPlantingDate".equals(m.getName()))
//					{
//						if (trial.getTrialPlantingDate() )
//					}
//				}
//			}
//			return result;
//		}
//	};
	
	
	private final TrialPropertiesTable trialPropertiesTable = new TrialPropertiesTable(trialPropertiesTableModel, propertyChangeConfirmer);

	private final PropertiesTableLegendPanel legendPanel = new PropertiesTableLegendPanel(trialPropertiesTable);

	private final Transformer<Trial,Boolean> checkIfEditorActive;
	
	private final Consumer<Trial> onTraitInstancesRemoved;
	public TrialViewPanel(
	        WindowOpener<JFrame> windowOpener,
			OfflineData od,
			Transformer<Trial,Boolean> checkIfEditorActive,
			Consumer<Trial> onTraitInstancesRemoved,
			MessagePrinter mp) 
	{
		super(new BorderLayout());

		this.windowOpener = windowOpener;
		this.checkIfEditorActive = checkIfEditorActive;
		this.onTraitInstancesRemoved = onTraitInstancesRemoved;
		this.messagePrinter = mp;

		this.offlineData = od;
		this.offlineData
				.addOfflineDataChangeListener(offlineDataChangeListener);
		KdxploreDatabase db = offlineData.getKdxploreDatabase();
		if (db != null) {
			db.addEntityChangeListener(trialChangeListener);
		}
		
        trialDataTable.setTransferHandler(
                TableTransferHandler.initialiseForCopySelectAll(trialDataTable, true));
        trialPropertiesTable.setTransferHandler(
                TableTransferHandler.initialiseForCopySelectAll(trialPropertiesTable, true));
		
		// Note: Can't use renderers because the TM always returns String.class
		// for getColumnClass()
		// trialPropertiesTable.setDefaultRenderer(TrialLayout.class, new
		// TrialLayoutRenderer(trialPropertiesTableModel));
		// trialPropertiesTable.setDefaultRenderer(PlotIdentOption.class, new
		// PlotIdentOptionRenderer(trialPropertiesTableModel));

		trialPropertiesTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (trialPropertiesTableModel.getRowCount() > 0) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							GuiUtil.initialiseTableColumnWidths(trialPropertiesTable);
						}
					});
					trialPropertiesTableModel.removeTableModelListener(this);
				}
			}
		});
		
//		int tnsColumnIndex = -1;
//		for (int col = trialPropertiesTableModel.getColumnCount(); --col >= 0; ) {
//			if (TraitNameStyle.class == trialPropertiesTableModel.getColumnClass(col)) {
//				tnsColumnIndex = col;
//				break;
//			}
//		}

		editAction.setEnabled(false);
		trialPropertiesTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent e) {
						if (!e.getValueIsAdjusting()) {
							int vrow = trialPropertiesTable.getSelectedRow();
							editAction.setEnabled(vrow >= 0
									&& trialPropertiesTableModel
											.isCellEditable(vrow, 1));
						}
					}
				});

		errorMessage.setForeground(Color.RED);
		Box top = Box.createHorizontalBox();
		top.add(errorMessage);
		top.add(Box.createHorizontalGlue());
		top.add(new JButton(editAction));

		JPanel main = new JPanel(new BorderLayout());
		main.add(new JScrollPane(trialPropertiesTable), BorderLayout.CENTER);
		main.add(legendPanel, BorderLayout.SOUTH);
		
		JScrollPane trialDataTableScrollPane = new JScrollPane(trialDataTable);
//		JScrollPane promptScrollPane = new PromptScrollPane(trialDataTable, "No Trial Data");
		JViewport viewPort = new JViewport() {
		      @Override public Dimension getPreferredSize() {
			        Dimension d = super.getPreferredSize();
			        d.height = 32;
			        TableModel model = trialDataTable.getModel();
			        if (model instanceof TrialData) {
			        	if (((TrialData) model).isUsingHMSformat()) {
			        		d.height = 48;
			        	}
			        }
			        return d;
			      }
			    };
		trialDataTableScrollPane.setColumnHeader(viewPort);
		
		JTableHeader th = trialDataTable.getTableHeader();
		th.setDefaultRenderer(trialDataTableHeaderRenderer);
		th.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
                int column = th.columnAtPoint(e.getPoint());
                trialDataTableHeaderRenderer.columnSelected = column;
                boolean shifted = 0 != (MouseEvent.SHIFT_MASK & e.getModifiers());
                boolean right = SwingUtilities.isRightMouseButton(e);
                updateDeleteSamplesAction(shifted, right);
                e.consume();
			}
		});
		
		trialDataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    removeTraitInstancesAction.setEnabled(trialDataTable.getSelectedRowCount() > 0);
                }
            }
        });
		removeTraitInstancesAction.setEnabled(false);

		KDClientUtils.initAction(ImageId.PLUS_BLUE_24, addSampleGroupAction, Msg.TOOLTIP_ADD_SAMPLES_FOR_SCORING());
		KDClientUtils.initAction(ImageId.TRASH_24, deleteSamplesAction, Msg.TOOLTIP_DELETE_COLLECTED_SAMPLES());
		KDClientUtils.initAction(ImageId.EXPORT_24, exportSamplesAction, Msg.TOOLTIP_EXPORT_SAMPLES_OR_TRAITS());
		KDClientUtils.initAction(ImageId.MINUS_GOLD_24, removeTraitInstancesAction, Msg.TOOLTIP_REMOVE_TRAIT_INSTANCES_WITH_NO_DATA());

		JPanel trialDataPanel = new JPanel(new BorderLayout());
		Box buttons = Box.createHorizontalBox();

		buttons.add(new JButton(removeTraitInstancesAction));
        buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(exportSamplesAction));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(addSampleGroupAction));
		buttons.add(Box.createHorizontalStrut(8));
		buttons.add(new JButton(deleteSamplesAction));
		trialDataPanel.add(GuiUtil.createLabelSeparator("Measurements by Source", buttons), BorderLayout.NORTH);
		trialDataPanel.add(trialDataTableScrollPane, BorderLayout.CENTER);
		
		JSplitPane splitPane = new JSplitPane(
				JSplitPane.VERTICAL_SPLIT, 
				main, 
				trialDataPanel);
		splitPane.setResizeWeight(0.5);
		
		add(top, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);
		
		trialDataTable.setDefaultRenderer(Object.class, new TrialDataCellRenderer());
		
		trialDataTable.addPropertyChangeListener("model", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				trialDataTableHeaderRenderer.columnSelected = -1;
				updateDeleteSamplesAction(false, false);
			}
		});
	}
	
	protected boolean changeElapsedDaysValues(Date oldTrialPlantingDate, Date newTrialPlantingDate) {
		
		boolean result = true;
		
		KDSmartDatabase kdsdb = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase();
		
		int nDaysDiff = DateDiffChoice.differenceInDays(newTrialPlantingDate, oldTrialPlantingDate);
		
		BatchHandler<Integer> callable = new BatchHandler<Integer>() {
			
			int updateCount = 0;

			@Override
			public Integer call() throws Exception {

				Map<Integer, Trait> traitMap = kdsdb.getTraitMap();
				
				Map<Integer,ValidationRule> ruleByTraitId = new HashMap<>();

				TrialItemVisitor<Sample> sampleVisitor = new TrialItemVisitor<Sample>() {
					@Override
					public void setExpectedItemCount(int count) { }
					
					@Override
					public boolean consumeItem(Sample sample) throws IOException {
						if (sample.hasBeenScored()) {
							Trait trait = traitMap.get(sample.getTraitId());
							
							if (trait != null && TraitDataType.ELAPSED_DAYS == trait.getTraitDataType()) {
								ValidationRule rule = ruleByTraitId.get(sample.getTraitId());
								if (rule == null) {
									rule = ValidationRule.NO_VALIDATION_RULE;
									
									String tvr = trait.getTraitValRule();
									if (Check.isEmpty(tvr)) {
										tvr = ValidationRule.CHOICE_ELAPSED_DAYS_NO_LIMIT;
									}
									try {
										rule = ValidationRule.create(tvr);
									} catch (InvalidRuleException ignore) {
									}
									
									ruleByTraitId.put(sample.getTraitId(), rule);
								}
								
								try {
									int nDays = Integer.parseInt(sample.getTraitValue());
									int newValue = nDays + nDaysDiff;
									
									String newTraitValue = Integer.toString(newValue);
									if (rule != ValidationRule.NO_VALIDATION_RULE) {
										if (! rule.evaluate(newTraitValue)) {
											throw new IOException(
													"new value '" + newTraitValue 
													+ "' does not validate: " + rule.getDescription());
										}
									}
									
									sample.setTraitValue(newTraitValue);
									kdsdb.saveSample(sample, false);
									++updateCount;
								} catch (NumberFormatException e) {
								}
							}
						}
						return true;
					}
				};
				
				kdsdb.visitSamplesForTrial(
				        SampleGroupChoice.ANY_SAMPLE_GROUP,
				        trial.getTrialId(),
						SampleOrder.ALL_UNORDERED, 
						sampleVisitor);
				
				return updateCount;
			}

            @Override
            public boolean checkSuccess(Integer count) {
                return count != null;
            }
		};

		Either<Exception,Integer> either = kdsdb.doBatch(callable);
		
		if (! either.isRight()) {
			Exception err = either.left();
			JOptionPane.showMessageDialog(TrialViewPanel.this, err.getMessage(),
					"Unable to change Planting Date", JOptionPane.ERROR_MESSAGE);
			result = false;
		}
		
		return result;
	}

	private final MyTableCellHeaderRenderer trialDataTableHeaderRenderer = new MyTableCellHeaderRenderer();

    private final WindowOpener<JFrame> windowOpener;
	
	static class MyTableCellHeaderRenderer extends SunSwingDefaultCellHeaderRenderer {

	    static private final int EDGE_LEN = 11;

		protected int columnSelected = -1;
		
		private boolean hasSubPlotSamples;
		private static final Polygon MARKER_TRIANGLE;
		static private final Color SUBPLOT_SAMPLES_COLOR = Color.decode("#00ddFF");

	    static {
	        int npoints = 3;
	        int[] xpoints = new int[npoints];
	        int[] ypoints = new int[npoints];
	        
	        xpoints[0] = -EDGE_LEN;  ypoints[0] = 0;
	        xpoints[1] = 0;          ypoints[1] = 0;
	        xpoints[2] = 0;          ypoints[2] = EDGE_LEN;
	        MARKER_TRIANGLE = new Polygon(xpoints, ypoints, npoints);
	    }
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) 
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (column == columnSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}
			else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			
			String ttt = null;
			hasSubPlotSamples = false;
			
			if (TrialManagerPreferences.getInstance().getShowIfSubplotScoredSamplesExist()) {
	            TableModel model = table.getModel();
	            if (model instanceof TrialData) {
	                TrialData trialData = (TrialData) model;
	                int mcol = table.convertColumnIndexToModel(column);
	                if (mcol >= 0) {
	                    Pair<DeviceIdentifier, SampleGroup> pair = trialData.getSampleGroupAtColumn(mcol);
	                    if (pair != null) {
	                        Integer subPlotSampleCount = trialData.getSubPlotSampleCount(pair.second);
	                        if (subPlotSampleCount != null && subPlotSampleCount > 0) {
	                            hasSubPlotSamples = true;
	                            ttt = "Scored sub-Plot samples: " + subPlotSampleCount;
	                        }
	                    }
	                }
	            }			    
			}

			setToolTipText(ttt);
			return this;
		}

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (hasSubPlotSamples) {
                Rectangle rect = getBounds();

                g.translate(rect.width, 0);
                g.setColor(SUBPLOT_SAMPLES_COLOR);
                g.fillPolygon(MARKER_TRIANGLE);
                g.translate(-rect.width, 0);
            }
        }
		
		
		
	}
	
	private void updateDeleteSamplesAction(boolean shifted, boolean isRightClick) {
		TableModel model = trialDataTable.getModel();
		
        SampleGroup sampleGroup = null;
		boolean enableDel = false;
//		boolean enableExp = false;
		if (model instanceof TrialData) {
			TrialData trialData = (TrialData) model;
			if (trialDataTableHeaderRenderer.columnSelected >= 0) {
				Pair<DeviceIdentifier, SampleGroup> pair = getSelectedDeviceAndSampleGroup(trialData);
				if (pair == null) {
					// Must be the Trait Instance names
//					enableExp = true;
				}
				else {
				    sampleGroup = pair.second;
					// is a SampleGroup
				    switch (pair.first.getDeviceType()) {
                    case DATABASE:
                    case EDITED:
                        break;

                    case FOR_SCORING:
                    case KDSMART:
                        enableDel = true;
                        break;
                    default:
                        break;
				    
				    }
//					enableExp = true;
				}
			}
		}
		
		deleteSamplesAction.setEnabled(enableDel);
//		exportSamplesAction.setEnabled(enableExp);
		
		if (shifted && (sampleGroup != null)) {
		    String title = createTitleForSampleGroupViewer(sampleGroup);
		    JFrame frame = windowOpener.getWindowByIdentifier(title);
		    if (frame != null) {
		        frame.toFront();
		    }
		    else {
	            SampleGroupViewer sgv = SampleGroupViewer.create(
	                    title,
	                    offlineData.getKdxploreDatabase(), trial, sampleGroup);
	            frame = windowOpener.addDesktopObject(sgv);
	            frame.setVisible(true);
		    }
		}
	}
	
	private String createTitleForSampleGroupViewer(SampleGroup sampleGroup) {
        StringBuilder sb = new StringBuilder(trial.getTrialName());
        sb.append(": ");
        Integer id = sampleGroup.getDeviceIdentifierId();
        
        DeviceIdentifier devid = null;
        try {
             devid = offlineData.getKdxploreDatabase().getDeviceIdentifierMap().get(id);            
        }
        catch (IOException e) {
            Shared.Log.w("TrialViewPanel", "createTitleForSampleGroupViewer", e);
        }
        
        if (devid == null) {
            sb.append(sampleGroup.getStoreIdentifier());
        }
        else {
            sb.append(devid.getDeviceName());
        }
        Date dateLoaded = sampleGroup.getDateLoaded();
        if (dateLoaded != null) {
            sb.append(" @ " )
                .append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(dateLoaded));
        }

        return sb.toString();
	}
//	private final MultiLineHeaderRenderer headerRenderer = new MultiLineHeaderRenderer();
	
	private final JTable trialDataTable = new JTable();

	public void setTrial(Trial t) {
		this.trial = t;
		setCurrentTrialDetails(trial, false);
	}

	private void setCurrentTrialDetails(Trial t, boolean onlyUpdating) {
		errorMessage.setText("");

		this.trial = t;
		this.elapsedDaysTraitsValueMinMax = null;

		if (trial == null) {
			trialPropertiesTableModel.clearData();
			trialDataTable.setModel(new TrialData());
		} else {
			try {
				KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
				List<TrialAttribute> trialAttributes = kdxdb.getTrialAttributes(
								trial.getTrialId());
				
				final Map<Integer,PlotAttribute> paById = new HashMap<>();
				final Map<PlotAttribute,Set<String>> paValuesByPa = new TreeMap<>();
				TrialItemVisitor<PlotAttribute> visitor = new TrialItemVisitor<PlotAttribute>() {
					@Override
					public void setExpectedItemCount(int count) {
					}					
					@Override
					public boolean consumeItem(PlotAttribute pa) throws IOException {
						paById.put(pa.getPlotAttributeId(), pa);
						paValuesByPa.put(pa, new TreeSet<>());
						return true;
					}
				};
				KDSmartDatabase kdsmartDatabase = kdxdb.getKDXploreKSmartDatabase();
				
				int trialId = this.trial.getTrialId();

				kdsmartDatabase.visitPlotAttributesForTrial(trialId, visitor);
				
				TrialItemVisitor<PlotAttributeValue> pavVisitor = new TrialItemVisitor<PlotAttributeValue>() {
					@Override
					public void setExpectedItemCount(int count) {
					}
					@Override
					public boolean consumeItem(PlotAttributeValue pav) throws IOException {
						PlotAttribute pa = paById.get(pav.getAttributeId());
						if (pa != null) {
							Set<String> set = paValuesByPa.get(pa);
							if (set == null) {
								set = new TreeSet<>();
								paValuesByPa.put(pa, set);
							}
							String value = pav.getAttributeValue();
							set.add(value==null ? "" : value);
						}
						return true;
					}
				};
				kdsmartDatabase.visitPlotAttributeValuesForTrial(trialId, pavVisitor);
	
				elapsedDaysTraitsValueMinMax = findElapsedDaysTraitsValueMinMax(kdxdb, kdsmartDatabase, trialId);
				
				trialPropertiesTableModel.setData(trial, trialAttributes, paValuesByPa);
				trialPropertiesTableModel.setPlotCount(kdxdb.getPlotCount(trialId));
				
				TrialData trialData = kdxdb.getTrialData(trial);
				trialDataTable.setModel(trialData);
			} catch (IOException e) {
				GuiUtil.errorMessage(TrialViewPanel.this,
						"Error getting Trial details", e.getMessage());
			}
			if (! onlyUpdating) {
			    trialDataTable.getTableHeader().resizeAndRepaint();
			}
		}
	}
	
	public void updateIfSameTrial(int trialId) {		
		if (this.trial!=null && this.trial.getTrialId() == trialId) {
		    setCurrentTrialDetails(trial, true);
//			KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
//			
//			try {
//			    elapsedDaysTraitsValueMinMax = findElapsedDaysTraitsValueMinMax(kdxdb, 
//						kdxdb.getKDXploreKSmartDatabase(),
//						trialId);
//				TrialData trialData = kdxdb.getTrialData(trial);
//				trialDataTable.setModel(trialData);
//			} catch (IOException e) {
//				GuiUtil.errorMessage(TrialViewPanel.this,
//						"Error Refreshing Trial Sample details", 
//						e.getMessage());
//			}
		}
	}

	private Point findElapsedDaysTraitsValueMinMax(
			KdxploreDatabase kdxdb, 
			KDSmartDatabase kdsmartDatabase, 
			int trialId) 
	throws IOException
	{
		Set<Integer> databaseDeviceIdentifierIds = new HashSet<>();
		int editedDevId = -1;
		
		Map<Integer,DeviceIdentifier> deviceIdentifierById = new HashMap<>();
		
		for (DeviceIdentifier devid : kdxdb.getDeviceIdentifiers()) {
			deviceIdentifierById.put(devid.getDeviceIdentifierId(), devid);

			switch (devid.getDeviceType()) {
			case DATABASE:
				databaseDeviceIdentifierIds.add(devid.getDeviceIdentifierId());
				break;
			case EDITED:
				if (editedDevId > 0) {
					messagePrinter.println("WARNING: multiple EDITED Device Ids found: "
							+ editedDevId + ", " + devid.getDeviceIdentifierId());
				}
				editedDevId = devid.getDeviceIdentifierId();
				break;
			case KDSMART:
			case FOR_SCORING:
			default:
				break;
			}
		}
		
//		boolean foundDb = false;
//		boolean foundEd = false;
//		Bag<String> deviceNames = new HashBag<>();
	
		Map<Integer, Trait> traitMap = kdsmartDatabase.getTraitMap();
		
		Map<SampleGroup,Long> sampleGroupCounts = kdxdb.getSampleGroupCounts(trialId);
		
		Integer[] minMax = new Integer[2];
		for (SampleGroup sg : sampleGroupCounts.keySet()) {
//			long count = sampleGroupCounts.get(sg);
//			if (databaseDeviceIdentifierIds.contains(sg.getDeviceIdentifierId())) {
//				foundDb = count > 0;
//			}
//			else if (editedDevId == sg.getDeviceIdentifierId()) {
//				foundEd = count > 0;
//			}
//			else if (count > 0) {
//				DeviceIdentifier devid = deviceIdentifierById.get(sg.getDeviceIdentifierId());
//				if (devid != null) {
//					deviceNames.add(devid.getDeviceName());
//				}
//			}

			
			TrialItemVisitor<Sample> sampleVisitor = new TrialItemVisitor<Sample>() {						
				@Override
				public void setExpectedItemCount(int count) { }
				
				@Override
				public boolean consumeItem(Sample sample) throws IOException {
					if (sample.hasBeenScored()) {
						// Scored sample
						Trait trait = traitMap.get(sample.getTraitId());
						if (trait != null && TraitDataType.ELAPSED_DAYS == trait.getTraitDataType()) {
							try {
								int value = Integer.parseInt(sample.getTraitValue());
								if (minMax[0] == null) {
									minMax[0] = value;
									minMax[1] = value;
								}
								else {
									minMax[0] = Math.min(minMax[0], value);
									minMax[1] = Math.max(minMax[1], value);
								}
							} catch (NumberFormatException e) {
							}
						}
					}
					return true;
				}
			};
			kdsmartDatabase.visitSamplesForTrial(
			        SampleGroupChoice.create(sg.getSampleGroupId()),
                    trialId, 
			        SampleOrder.ALL_UNORDERED, 
					sampleVisitor);
		}
		
		Point result = null;
		if (minMax[0] != null) {
			result = new Point(minMax[0], minMax[1]);
		}
		return result;
		
//		StringBuilder samplesFor = new StringBuilder();
//		String sep = "";
//		if (foundDb) {
//			samplesFor.append("Database");
//			sep = ",";
//		}
//		if (foundEd) {
//			samplesFor.append(sep).append("Curated");
//			sep = ",";
//		}
//		if (! deviceNames.isEmpty()) {
//			samplesFor.append(sep);
//			if (deviceNames.size() <= 1) {
//				samplesFor.append("Device:");
//			}
//			else {
//				samplesFor.append("Devices:");
//			}
//			sep = "";
//			for (String dname : deviceNames) {
//				int count = deviceNames.getCount(dname);
//				samplesFor.append(sep);
//				samplesFor.append(dname);
//				if (count > 1) {
//					samplesFor.append('(').append(count).append(')');
//				}
//				sep = ",";
//			}
//		}
//
//		if (samplesFor.length() <= 0) {
//			samplesFor.append("-No Samples-");
//		}
//	
//		return samplesFor.toString();
	}

	class TrialPropertiesTable extends EntityPropertiesTable<Trial> {

		public TrialPropertiesTable(EntityPropertiesTableModel<Trial> tm, EntityPropertiesTable.PropertyChangeConfirmer pcc) {
			super(tm, pcc);
			
			setDefaultRenderer(Object.class, new GenericTrialPropertyRenderer());
		}

		@Override
		protected boolean handleEditCellAt(EntityPropertiesTableModel<Trial> eptm, int row, int column) {
			
			TrialPropertiesTableModel tptm = (TrialPropertiesTableModel) eptm;

			PropertyDescriptor pd = tptm.getPropertyDescriptor(row);
			
			Class<?> propertyClass = pd.getPropertyType();

			Trial trial = tptm.getTrial();
			
			if (checkIfEditorActive.transform(trial)) {
				MsgBox.warn(TrialViewPanel.this, 
						"Trial '" + trial.getTrialName() + "' is being curated", 
						"Can't Edit " + pd.getDisplayName());
				return true;
			}
			
			if (TrialLayout.class == propertyClass) {
				TrialLayoutEditorDialog tle = new TrialLayoutEditorDialog(
						GuiUtil.getOwnerWindow(this),
						"Trial Layout");
				tle.setTrial(trial, null);
				tle.setVisible(true);
				TrialLayout tl = tle.getTrialLayout();
				if (tl != null) {
				    TrialLayout old = trial.getTrialLayout();
				    trial.setTrialLayout(tl);
				    try {
                        offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase().saveTrial(trial);
                        tptm.setNewTrialLayout(tl);
                    }
                    catch (IOException e) {
                        trial.setTrialLayout(old);
                        MsgBox.warn(TrialViewPanel.this, e.getMessage(), 
                                "Unable to save change to Trial Layout");
                    }
				}
				return true;
			}
			
			if (PlotIdentOption.class == propertyClass) {
				PlotIdentSummary pis = trial.getPlotIdentSummary();
				if (! pis.hasXandY() && pis.plotIdentRange.isEmpty()) {
					JOptionPane.showMessageDialog(this, 
							"No Plot Data", 
							pd.getDisplayName(), 
							JOptionPane.WARNING_MESSAGE);
					return true;
				}
			}

			// caller (superclass) needs to handle this
			return false;
		}
	}

//	class MultiLineHeaderRenderer extends JList implements TableCellRenderer {
//		  public MultiLineHeaderRenderer() {
//		    setOpaque(true);
//		    setForeground(UIManager.getColor("TableHeader.foreground"));
//		    setBackground(UIManager.getColor("TableHeader.background"));
//		    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
//		    ListCellRenderer renderer = getCellRenderer();
//		    ((JLabel) renderer).setHorizontalAlignment(JLabel.CENTER);
//		    setCellRenderer(renderer);
//		  }
//
//		  public Component getTableCellRendererComponent(JTable table, Object value,
//		      boolean isSelected, boolean hasFocus, int row, int column) {
//		    setFont(table.getFont());
//		    String str = (value == null) ? "" : value.toString();
//		    String[] lines = str.split("\n");
//		    
//		    Vector v = new Vector();
//		    Collections.addAll(v, lines);
//		    setListData(v);
//		    return this;
//		  }
//		}
	

}
