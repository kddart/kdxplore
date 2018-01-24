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
package com.diversityarrays.kdxplore.curate.fieldview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.TraitValueType;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdsmart.field.FieldLayoutUtil;
import com.diversityarrays.kdsmart.field.GradientChoice;
import com.diversityarrays.kdsmart.scoring.PlotVisitList;
import com.diversityarrays.kdsmart.scoring.PlotVisitListBuilder;
import com.diversityarrays.kdsmart.scoring.PlotsPerGroup;
import com.diversityarrays.kdsmart.scoring.setup.PlotVisitListBuildParams;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.curate.AttributeValue;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.CurationMenuProvider;
import com.diversityarrays.kdxplore.curate.CurationTableModel;
import com.diversityarrays.kdxplore.curate.PlotAttributeValueRetriever;
import com.diversityarrays.kdxplore.curate.PlotCellChoicesListener;
import com.diversityarrays.kdxplore.curate.PlotCellChoicesPanel;
import com.diversityarrays.kdxplore.curate.SampleValue;
import com.diversityarrays.kdxplore.curate.SelectedInfo;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.TraitInstanceCellRenderer;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.curate.fieldview.InterceptFieldLayoutView.RefreshListener;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.CurationDataChangeListener;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.field.CollectionPathSetupDialog;
import com.diversityarrays.kdxplore.field.FieldLayoutTableModel;
import com.diversityarrays.kdxplore.field.KdxploreFieldLayout;
import com.diversityarrays.kdxplore.trials.PlotCellRenderer;
import com.diversityarrays.kdxplore.ui.CellSelectableTable;
import com.diversityarrays.kdxplore.ui.CellSelectionListener;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.KDClientUtils.ResizeOption;
import com.diversityarrays.util.OrOrTr;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.SunSwingDefaultLookup;
import com.diversityarrays.util.TableColumnResizer;
import com.diversityarrays.util.TableRowResizer;
import com.diversityarrays.util.TableRowResizer.RowHeightChangeListener;
import com.diversityarrays.util.VisitOrder2D;
import com.diversityarrays.util.XYPos;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.table.RowHeaderTable;
import net.pearcan.ui.table.RowHeaderTableModel;
import net.pearcan.ui.table.RowRemovable;
import net.pearcan.util.MessagePrinter;

public class FieldLayoutViewPanel extends JPanel implements FieldLayoutView {

//	private static final String TRAIT_TO_EDIT = "Trait to Edit";

	protected static final boolean DEBUG = Boolean.getBoolean("FieldView.DEBUG"); //$NON-NLS-1$

	private static final String TAG = FieldLayoutViewPanel.class.getSimpleName();
	
	private final FieldLayoutTableModel fieldLayoutTableModel;
	private final CellSelectableTable fieldLayoutTable;
    private final FieldViewSelectionModel fieldViewSelectionModel;
    
	static public class DebugSettings {
		
		private final Map<JCheckBoxMenuItem,Setting> settingByCbox = new HashMap<>();
		
		private final MessagePrinter messagePrinter;

		public DebugSettings(JComponent comp, MessagePrinter mp) {
			messagePrinter = mp;
			
			comp.addMouseListener(new MouseAdapter() {				
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount()==1 && SwingUtilities.isRightMouseButton(e)) {
						changeSettings(e);
					}
				}
			});
		}
		
		private final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Setting s = settingByCbox.get(e.getSource());
				if (s != null) {
					JCheckBoxMenuItem cbox = (JCheckBoxMenuItem) e.getSource();
					boolean b = cbox.isSelected();
					s.setSetting(b);
					System.out.println("[" + s + ": " + b + "]");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		};
		
		public void changeSettings(MouseEvent me) {
			settingByCbox.clear();
			JPopupMenu popupMenu = new JPopupMenu("Resize Settings"); //$NON-NLS-1$
			for (Setting s : Setting.values()) {
				boolean selected = s.getSetting();
				messagePrinter.println(s + ": " + selected); //$NON-NLS-1$
				JCheckBoxMenuItem cbox = new JCheckBoxMenuItem(s.displayName, selected);
				cbox.addActionListener(actionListener);
				settingByCbox.put(cbox, s);
				popupMenu.add(cbox);
			}
			popupMenu.show(me.getComponent(), me.getX(), me.getY());
		}
		
	}

	private RowRemovable rowRemovable = new RowRemovable() {		
		@Override
		public boolean isWarningAppropriate(TableModel dependentTableModel, int modelRow) {
			return false;
		}
		
		@Override
		public boolean isRowRemovable(TableModel tableModel, int row) {
			return false;
		}
	};

	private final RowHeaderTableModel rhtm;
	
	private final RowHeaderTable rowHeaderTable;
	
	private final TableRowResizer rhtTableRowResizer ;
	
	private Trial trial;
	
	private final JLabel warningMessage = new JLabel();

	private final Action changeVisitOrderAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			changeVisitOrder();
		}		
	};
	private final CurationData curationData;
	private final CellSelectionListener cellSelectionListener = new CellSelectionListener() {

		@Override
		public void handleChangeEvent(EventType eventType,ListSelectionEvent event) {
			
			if (event.getValueIsAdjusting()) {
				return;
			}

			switch (eventType) {
			case COLUMN_SELECTION_CHANGED:
				updatePlotAttributeTableModel("columnSelectionChanged"); //$NON-NLS-1$
				break;
			case VALUE_CHANGED:
				updatePlotAttributeTableModel("valueChanged"); //$NON-NLS-1$
				break;
			default:
				break;
			
			}
			
		}		
	};

	private final PlotCellChoicesPanel plotCellChoicesPanel;
	private final Transformer<PlotAttributeValue,PlotAttribute> plotAttributeProvider = new Transformer<PlotAttributeValue, PlotAttribute>() {
		@Override
		public PlotAttribute transform(PlotAttributeValue pav) {
			return plotCellChoicesPanel.getPlotAttributeForValue(pav);
		}
	};
	
	private final PlotCellRenderer plotCellRenderer;
	
	private PlotCellChoicesListener plotCellChoicesListener = new PlotCellChoicesListener() {

		// Prevent recursion
		boolean plotBusy;
		@Override
		public void plotAttributeChoicesChanged(Object source, List<ValueRetriever<?>> vrList) {
			if (plotBusy) {
				Shared.Log.d(TAG,
						"plotAttributeChoicesChanged: ***** LOOPED with \n\tattributes=" //$NON-NLS-1$
								+ vrList);
			}
			else {
				Shared.Log.d(TAG, "plotAttributeChoicesChanged: BEGIN"); //$NON-NLS-1$
				plotBusy = true;
				try {
				    Set<String> attributeNames = vrList.stream().filter(vr -> vr instanceof PlotAttributeValueRetriever)
				        .map(vr -> ((PlotAttributeValueRetriever) vr).getAttributeName())
				        .collect(Collectors.toSet());
					plotCellRenderer.setAttributeNames(attributeNames);
					plotCellRenderer.updateTableRowHeight(fieldLayoutTable);
					fieldLayoutTable.repaint();				
				}
				finally {
					plotBusy = false;	
					Shared.Log.d(TAG, "plotAttributeChoicesChanged: END"); //$NON-NLS-1$
				}
			}
		}

		// Prevent recursion
		boolean tiBusy;
		@Override
		public void traitInstanceChoicesChanged(Object source, 
				boolean choiceAdded, 
				TraitInstance[] choice, 
				Map<Integer, Set<TraitInstance>> traitInstancesByTraitId) 
		{
			if (tiBusy) {
				Shared.Log.d(TAG, 
						"traitInstanceChoicesChanged: ***** LOOPED : nTraitInstances=" + traitInstancesByTraitId.size()); //$NON-NLS-1$
			}
			else {
				Shared.Log.d(TAG, "traitInstanceChoicesChanged: BEGIN"); //$NON-NLS-1$
				tiBusy = true;
				try {
					plotCellRenderer.setSelectedTraitInstanceNumbersByTraitId(
							traitInstancesByTraitId);
					plotCellRenderer.updateTableRowHeight(fieldLayoutTable);
					fieldLayoutTable.repaint();
				}
				finally {
					tiBusy = false;
					Shared.Log.d(TAG, "traitInstanceChoicesChanged: END"); //$NON-NLS-1$
				}
			}
		}
	};
	
	private Closure<String> selectionClosure;
	
	private final Map<String,TraitInstance> traitById = new HashMap<>();
	
	private final DateFormat dateFormat = TraitValue.getTraitValueDateFormat();

	private JComboBox<ResizeOption> resizeCombo;

	// Warning: item#0 is a String, the others are TraitInstances.
	@SuppressWarnings("rawtypes")
    private final JComboBox traitInstanceCombo = new JComboBox();

	private final MessagePrinter messagePrinter;
	
	@Override
	public void setSelectedPlots(List<Plot> plots) {
		//fieldViewSelectionModel.clearSelection();
//		fieldViewSelectionModel.clearSelectedAndRemovePlotSelection();
		
//		PlotsByTraitInstance plotsByTi = new PlotsByTraitInstance();
//		if (activeTraitInstance != null) {
//			plotsByTi.addPlots(activeTraitInstance, getSelectedPlots());
//		}
		fieldViewSelectionModel.setSelectedPlots(plots);
		
		String toolId = ""; //$NON-NLS-1$
		selectionClosure.execute(toolId);
		updateSelectedMeasurements("FieldLayoutViewPanel.setSelectedPlots");	 //$NON-NLS-1$	
	}
	
	@Override
	public void updateSamplesSelectedInTable() {
		
		//fieldViewSelectionModel.clearSelection();
//		fieldViewSelectionModel.clearSelectedAndRemovePlotSelection();
		
//		PlotsByTraitInstance plotsByTi = new PlotsByTraitInstance();
//		if (activeTraitInstance != null) {
//			plotsByTi.addPlots(activeTraitInstance, getSelectedPlots());
//		}
//		fieldViewSelectionModel.setSelectedPlots(plotsByTi);

		fieldViewSelectionModel.setSelectedPlots(getSelectedPlots());

		String toolId = ""; //$NON-NLS-1$
		selectionClosure.execute(toolId);
		updateSelectedMeasurements("FieldLayoutViewPanel.updateSamplesSelectedInTable");	 //$NON-NLS-1$	
	}

	@Override
	public List<Plot> getFieldViewSelectedPlots() {
	    return fieldViewSelectionModel.getSelectedPlots();
	}

	public List<Plot> getSelectedPlots() {
		List<Plot> plots = new ArrayList<>();

		int vrows[] = fieldLayoutTable.getSelectedRows();
		if (vrows==null || vrows.length > 0) {
			int vcols[] = fieldLayoutTable.getSelectedColumns();
			for (int vrow : vrows) {
				for (int vcol : vcols) {
					if (fieldLayoutTable.isCellSelected(vrow, vcol)) {
						Plot plot = fieldLayoutTableModel.getPlot(vrow, vcol);
						if (plot != null) {
							plots.add(plot);
						}
					}
				}
			}
		}
		return plots;
	}
	
	private boolean busyUpdating = false;
	
	@Override
	public void updateSelectedMeasurements(String fromWhere) {

		if (busyUpdating) {
			Shared.Log.d(TAG, "updateSelectedMeasurements: **** LOOPED, fromWhere=" + fromWhere); //$NON-NLS-1$
			return;
		}

		Shared.Log.d(TAG, "updateSelectedMeasurements: BEGIN"); //$NON-NLS-1$
		busyUpdating = true;
		try {
			fieldViewSelectionModel.refreshSelectedMeasurements(fromWhere);
			plotCellRenderer.setActiveInstance(fieldViewSelectionModel.getActiveTraitInstance(true));
		}
		finally {
			busyUpdating = false;
			
			Shared.Log.d(TAG, "updateSelectedMeasurements: END"); //$NON-NLS-1$
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fieldLayoutTable.repaint();
				}			
			});
		}
	}
	
	private final CurationTableModel curationTableModel;
	
	private CurationDataChangeListener plotActivationListener = new CurationDataChangeListener() {
		@Override
		public void plotActivationChanged(Object source, boolean activated, List<Plot> plots) {
			fieldLayoutTableModel.plotsChanged(plots);
			fireRefreshRequired();
		}

		@Override
		public void editedSamplesChanged(Object source, List<CurationCellId> curationCellIds) {
			fieldLayoutTable.repaint();
		}
	};

	private final Transformer<TraitInstance, String> instanceNameProvider = new Transformer<TraitInstance, String>() {
		@Override
		public String transform(TraitInstance ti) {
			return makeTraitInstanceName(ti);
		}
	};


	@SuppressWarnings("unchecked")
	public FieldLayoutViewPanel(
			@SuppressWarnings("rawtypes") MutableComboBoxModel comboBoxModel,
			JCheckBox alwaysOnTopOption,
			CurationData cd, 
			CurationTableModel ctm, 
			SelectedValueStore svs,
			PlotCellChoicesPanel pccp,
			JPopupMenu popuMenu,
			Font fontForResizeControls,
			Action curationHelpAction,
			MessagePrinter mp,
			Closure<String> selectionClosure,
			CurationContext curationContext,
			CurationMenuProvider curationMenuProvider,
			
			FieldLayoutTableModel fieldLayoutTableModel,
			CellSelectableTable fieldLayoutTable,
			FieldViewSelectionModel fvsm,
			
			JButton undockButton)
	{
		super(new BorderLayout());
		
		this.traitInstanceCombo.setModel(comboBoxModel);
		this.curationData = cd;
		this.messagePrinter = mp;
		this.selectionClosure = selectionClosure;
		this.curationTableModel = ctm;
		
		this.fieldLayoutTableModel = fieldLayoutTableModel;
		this.fieldLayoutTable = fieldLayoutTable;
		this.fieldViewSelectionModel = fvsm;
		
		traitInstanceCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object item = comboBoxModel.getSelectedItem();
                if (item instanceof TraitInstance) {
                    TraitInstance ti = (TraitInstance) item;
                    plotCellRenderer.setActiveInstance(ti);
                }
            }
        });
		
		rhtm = new RowHeaderTableModel(true, fieldLayoutTable, rowRemovable) {        
	        public String getRowLabel(int rowIndex) {
	            int yCoord = FieldLayoutUtil.convertRowIndexToYCoord(rowIndex, trial, fieldLayoutTableModel.getFieldLayout());
	            return String.valueOf(yCoord);
	        }
	    };
		rowHeaderTable = new RowHeaderTable(
	            SwingConstants.CENTER, 
	            false, 
	            fieldLayoutTable, 
	            rowRemovable,
	            rhtm, RowHeaderTable.createDefaultColumnModel("X/Y"))
	    {
	        public String getMarkerIndexName(int viewRow) {
	            return "MIN-"+viewRow; //$NON-NLS-1$
	        }
	    };
	    rhtTableRowResizer = new TableRowResizer(rowHeaderTable, true);
		
		curationData.addCurationDataChangeListener(plotActivationListener);
		
		curationTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				fieldLayoutTable.repaint();
			}
		});
		plotCellRenderer = new PlotCellRenderer(plotAttributeProvider, 
				curationTableModel);

		TraitInstanceCellRenderer tiCellRenderer = new TraitInstanceCellRenderer(
				curationData.getTraitColorProvider(),
				instanceNameProvider
				);
		traitInstanceCombo.setRenderer(tiCellRenderer);
		traitInstanceCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateActiveTraitInstance();
			}
		});
		traitInstanceCombo.getModel().addListDataListener(new ListDataListener() {
			@Override
			public void intervalRemoved(ListDataEvent e) {
				updateActiveTraitInstance();
			}
			
			@Override
			public void intervalAdded(ListDataEvent e) {
				updateActiveTraitInstance();
			}
			
			@Override
			public void contentsChanged(ListDataEvent e) {
				updateActiveTraitInstance();
			}
		});

		this.trial = curationData.getTrial();
		this.plotCellChoicesPanel = pccp;
		
		for (TraitInstance t : curationData.getTraitInstances()) {
			String id = InstanceIdentifierUtil.getInstanceIdentifier(t);
			traitById.put(id, t);
		}
		
//		fieldViewSelectionModel = new FieldViewSelectionModel(
//				fieldLayoutTable, 
//				fieldLayoutTableModel, 
//				svs);
		fieldLayoutTable.setSelectionModel(fieldViewSelectionModel);
		
		plotCellRenderer.setCurationData(curationData);
		plotCellRenderer.setSelectionModel(fieldViewSelectionModel);

		plotCellChoicesPanel.addPlotCellChoicesListener(plotCellChoicesListener);
		
		fieldLayoutTableModel.setTrial(trial);

		// IMPORTANT: DO NOT SORT THE FIELD LAYOUT TABLE
		fieldLayoutTable.setAutoCreateRowSorter(false);
		JScrollPane fieldTableScrollPane = new JScrollPane(fieldLayoutTable);
		
		if (undockButton != null) {
			fieldTableScrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, undockButton);
		}
		fieldTableScrollPane.setRowHeaderView(rowHeaderTable);
		ChangeListener scrollBarChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
			    fireRefreshRequired();
			}
		};
		fieldTableScrollPane.getVerticalScrollBar().getModel().addChangeListener(scrollBarChangeListener);
		fieldTableScrollPane.getHorizontalScrollBar().getModel().addChangeListener(scrollBarChangeListener);

		fieldLayoutTable.setRowHeaderTable(rowHeaderTable);
		
//		fieldLayoutTable.setComponentPopupMenu(popuMenu);
		
		initFieldLayoutTable();
		
		Map<Integer, Plot> plotById = new HashMap<>();
		FieldLayout<Integer> plotIdLayout = FieldLayoutUtil.createPlotIdLayout(
				trial.getTrialLayout(), trial.getPlotIdentSummary(), 
				curationData.getPlots(), 
				plotById);
		
		KdxploreFieldLayout<Plot> kdxFieldLayout = new KdxploreFieldLayout<Plot>(Plot.class, plotIdLayout.imageId, plotIdLayout.xsize, plotIdLayout.ysize);
		kdxFieldLayout.warning = plotIdLayout.warning;

        for (int y = 0; y < plotIdLayout.ysize; ++y) {
            for (int x = 0; x < plotIdLayout.xsize; ++x) {
                Integer id = plotIdLayout.cells[y][x];
                if (id != null) {
                    Plot plot = plotById.get(id);
                    kdxFieldLayout.store_xy(plot, x, y);
                }
            }
        }
		fieldLayoutTableModel.setFieldLayout(kdxFieldLayout);
		
		if (kdxFieldLayout.warning != null && ! kdxFieldLayout.warning.isEmpty()) {
			warningMessage.setText(kdxFieldLayout.warning);
		}
		else {
			warningMessage.setText(""); //$NON-NLS-1$
		}
		
		changeVisitOrderAction.putValue(Action.SMALL_ICON, KDClientUtils.getIcon(kdxFieldLayout.imageId));
		
		List<Component> components = new ArrayList<>();
		components.add(alwaysOnTopOption);
		
		Collections.addAll(components, 
				new JButton(changeVisitOrderAction),
				new JButton(curationHelpAction),
				traitInstanceCombo
		);
		Box resizeControls = KDClientUtils.createResizeControls(
					fieldLayoutTable, 
					fontForResizeControls,
					components.toArray(new Component[components.size()])
					);	
		resizeCombo = KDClientUtils.findResizeCombo(resizeControls);
		
		if (RunMode.getRunMode().isDeveloper()) {
			new FieldLayoutViewPanel.DebugSettings(resizeControls, messagePrinter);
		}
		
		JPanel fieldPanel = new JPanel(new BorderLayout());
		
//		if (useSeparator) {
//			SeparatorPanel separator = GuiUtil.createLabelSeparator("Field Layout:", resizeControls);
//			fieldPanel.add(separator, BorderLayout.NORTH);
//			fieldPanel.add(fieldTableScrollPane, BorderLayout.CENTER);
//		}
//		else {
			fieldPanel.add(resizeControls, BorderLayout.NORTH);
			fieldPanel.add(fieldTableScrollPane, BorderLayout.CENTER);
//		}
		
//		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
//				plotCellChoicesPanel,
//				fieldPanel);
//		splitPane.setResizeWeight(0.0);
//		splitPane.setOneTouchExpandable(true);
		
		add(warningMessage, BorderLayout.NORTH);
		add(fieldPanel, BorderLayout.CENTER);
//		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
//				plotCellChoicesPanel,
//				fieldPanel);
//		splitPane.setResizeWeight(0.0);
//		splitPane.setOneTouchExpandable(true);
//		
//		add(warningMessage, BorderLayout.NORTH);
//		add(splitPane, BorderLayout.CENTER);

		fieldLayoutTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent me) {
				if (SwingUtilities.isRightMouseButton(me)
					&& 1 == me.getClickCount())
				{
					me.consume();
					
					List<Plot> plots = getSelectedPlots();
					
					List<TraitInstance> checkedInstances = new ArrayList<>();
					for (int index = traitInstanceCombo.getItemCount(); --index >= 1; ) {
						Object item = traitInstanceCombo.getItemAt(index);
						if (item instanceof TraitInstance) {
							checkedInstances.add((TraitInstance) item);
						}
					}
					
					TraitInstance ti = fieldViewSelectionModel.getActiveTraitInstance(true);
					List<PlotOrSpecimen> plotSpecimens = new ArrayList<>();
					plotSpecimens.addAll(plots);
					curationMenuProvider.showFieldViewToolMenu(me, plotSpecimens, ti, checkedInstances);
				}
			}
		});
	}
	
	private String makeTraitInstanceName(TraitInstance ti) {
		Trial trial = curationData.getTrial();
		return trial.getTraitNameStyle().makeTraitInstanceName(ti);
	}
	
	public final Map<String,TraitInstance> tiByName = new HashMap<>();

	private TableColumnResizer tableColumnResizer;
	
	@SuppressWarnings("unchecked")
	@Override
	public void addTraitInstance(TraitInstance ti) {
		String tiName = makeTraitInstanceName(ti);
		if (! tiByName.containsKey(tiName)) {
			tiByName.put(tiName, ti);
			traitInstanceCombo.addItem(ti);
			traitInstanceCombo.setSelectedItem(ti);
			
			updateActiveTraitInstance();
		}
	}
	
	@Override
	public void removeTraitInstance(TraitInstance ti) {
		String tiName = makeTraitInstanceName(ti);
		if (null != tiByName.remove(tiName)) {
			traitInstanceCombo.removeItem(ti);
			// Always leave one selected if we can
			if (traitInstanceCombo.getItemCount() > 1) {
				traitInstanceCombo.setSelectedIndex(1);
			}
			updateActiveTraitInstance();
		}
	}
	
	public void addTraitInstanceSelectionListener(ItemListener itemListener) {
		traitInstanceCombo.addItemListener(itemListener);
	}
	
	@Override
	public void addCellSelectionListener(CellSelectionListener l) {
		fieldLayoutTable.getColumnModel().addColumnModelListener(l);
		fieldLayoutTable.getSelectionModel().addListSelectionListener(l);
	}
	
    @Override
	public void removeCellSelectionListener(CellSelectionListener l) {
		fieldLayoutTable.getColumnModel().removeColumnModelListener(l);
		fieldLayoutTable.getSelectionModel().removeListSelectionListener(l);
	}


	private final PropertyChangeListener tableColumnWidthListener = new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			TableColumn rc = tableColumnResizer.getResizedColumn();
			if (rc != null) {
				int width = rc.getWidth();
				System.out.println("[TableColumn#" + rc.getModelIndex()+" width=" + width + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (Util.PROPAGATE_RESIZE_ALL_COLUMNS) {
					if (DEBUG) {
						System.out.println("\tresizing all other columns"); //$NON-NLS-1$
					}
					TableColumnModel tcm = fieldLayoutTable.getColumnModel();
					for (int index = tcm.getColumnCount(); --index >= 0; ) {
						TableColumn tc = tcm.getColumn(index);
						if (tc != rc) {
							if (DEBUG) {
								System.out.println("Setting width of column#" + tc.getModelIndex() + " to " + width); //$NON-NLS-1$ //$NON-NLS-2$
							}
							tc.setPreferredWidth(width);
						}
					}
				}
			}
			
		}
	};

	private final RowHeightChangeListener rowHeightChangeListener = new RowHeightChangeListener() {
		@Override
		public void rowHeightChanged(Object source, int rowIndex, int rowHeight) {
			if (rowIndex >= 0) {
				int hyt = rowHeaderTable.getRowHeight(rowIndex);
                fieldLayoutTable.setRowHeight(hyt);
				for (int row = fieldLayoutTable.getRowCount(); --row >= 0; ) {
					fieldLayoutTable.setRowHeight(row, hyt);
				}
			}
		}		
	};

	class RowHeaderTableCellRenderer extends DefaultTableCellRenderer {
		
		private final Color alternateColor;
		private final Border focusBorder;
		private final LineBorder unFocusBorder;
		
		public RowHeaderTableCellRenderer() {
			super();
			setHorizontalAlignment(CENTER);
			
			unFocusBorder = new LineBorder(Color.LIGHT_GRAY, 1);
			focusBorder = SunSwingDefaultLookup.getBorder(this, ui, "Table.focusCellHighlightBorder"); //$NON-NLS-1$

			alternateColor = SunSwingDefaultLookup.getColor(this, ui, "Table.alternateRowColor"); //$NON-NLS-1$
		}
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row, int column) 
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			if (hasFocus && focusBorder != null) {
				setBorder(focusBorder);
			}
			else {
				setBorder(unFocusBorder);
			}
			
			setForeground(table.getForeground());
			if (alternateColor!=null && ( (row&1) == 1)) {
				setBackground(alternateColor);
			}
			else {
				setBackground(table.getBackground());
			}
			return this;
		}
	}
	
	private void initFieldLayoutTable() {
		
		rhtTableRowResizer.addRowHeightChangeListener(rowHeightChangeListener);
		rowHeaderTable.setDefaultRenderer(String.class, new RowHeaderTableCellRenderer());
		
		fieldLayoutTable.getTableHeader().setReorderingAllowed(false);
		fieldLayoutTable.setResizable(true, true);
		
		tableColumnResizer = fieldLayoutTable.getTableColumnResizer();
		tableColumnResizer.addPropertyChangeListener(TableColumnResizer.PROPERTY_WIDTH, tableColumnWidthListener);

		plotCellRenderer.updateTableRowHeight(fieldLayoutTable);
		
		fieldLayoutTable.setDefaultRenderer(Plot.class, plotCellRenderer);

		fieldLayoutTable.setCellSelectionEnabled(true);
		
		fieldLayoutTable.getColumnModel().addColumnModelListener(cellSelectionListener);

		fieldLayoutTable.getSelectionModel().addListSelectionListener(cellSelectionListener);
		fieldLayoutTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	@Override
	public void doPostOpenActions() {
//		splitPane.setDividerLocation(0.2);
		if (resizeCombo != null) {
			resizeCombo.setSelectedItem(ResizeOption.OFF);
		}
	}

	private PlotVisitList buildPlotVisitList(VisitOrder2D visitOrder, PlotsPerGroup plotsPerGroup) {
		
		PlotVisitListBuildParams buildParams = new PlotVisitListBuildParams(trial.getTrialId());

		buildParams.leftHandedMode = false; // appInstance.isLeftHandedLayout();
		buildParams.lockScoredTraits = true; // params.lockScoredTraits;
		buildParams.useWhiteBackground = true; // params.useWhiteBackground;
		buildParams.visitOrder = visitOrder;
		buildParams.plotsPerGroup = plotsPerGroup;

		Map<Integer,Trait> traitMap = new HashMap<>();
		for (Trait t : curationData.getTraits()) {
			traitMap.put(t.getTraitId(), t);
		}
		PlotVisitListBuilder builder = new PlotVisitListBuilder(
				curationData.getPlots(),
				traitMap,
				curationData.getTraitInstances());
		
		
		return builder.create(trial, buildParams, GradientChoice.DEFAULT_GRADIENT_CHOICE);
	}

	public FieldLayoutTableModel getFieldModel() {
		return fieldLayoutTableModel;
	}
	
	@Override
	public Component getPanel() {
	    return this;
	}

	@Override
	public TraitInstance getActiveTraitInstance(boolean nullIf_NO_TRAIT_INSTANCE) {
	    return fieldViewSelectionModel.getActiveTraitInstance(nullIf_NO_TRAIT_INSTANCE);
	}
//	public FieldViewSelectionModel getFieldViewSelectionModel() {
//		return fieldViewSelectionModel;
//	}
	
	@Override
	public SelectedInfo createFromFieldView() {
        
        SelectedInfo result = new SelectedInfo();

        TraitInstance ti = fieldViewSelectionModel.getActiveTraitInstance(true);

        if (ti != null) {
            
            List<Plot> plots = fieldViewSelectionModel.getSelectedPlots();          
            for (Plot plot : plots) {
                result.plotsByTraitInstance.addPlot(ti, plot);
            }
            for (Plot plot : plots) {
                CurationCellValue ccv = curationData.getCurationCellValue(plot, ti);
                result.addCcv(ti, ccv);
            }
        }
        
        return result;
    }
	
	
	public CellSelectableTable getFieldLayoutTable() {
		return fieldLayoutTable;
	}
	
	private void updatePlotAttributeTableModel(String fromWhere) {
		
//		System.out.println("updatePlotAttributeTableModel( " + fromWhere + " )");
		
		boolean anyMultiple = false;
		Map<Integer,AttributeValue> valueByAttributeId = new HashMap<>();
		Map<Pair<Integer,Integer>,SampleValue> sampleMap = new HashMap<>();
		
		int vrows[] = fieldLayoutTable.getSelectedRows();
		if (vrows==null || vrows.length > 0) {
			int vcols[] = fieldLayoutTable.getSelectedColumns();
			
//			System.out.println("rows:");
//			for (int r : vrows) {
//				System.out.print(" " + r);
//			}
//			System.out.println();
//			System.out.println("cols:");
//			for (int c : vcols) {
//				System.out.print(" " + c);
//			}
//			System.out.println();
			
			for (int vrow : vrows) {
				for (int vcol : vcols) {
					if (fieldLayoutTable.isCellSelected(vrow, vcol)) {
						// we don't allow sorting or column re-ordering
						Plot plot = fieldLayoutTableModel.getPlot(vrow, vcol);
						if (plot != null) {
							for (PlotAttributeValue pav : plot.plotAttributeValues) {
								AttributeValue aValue = valueByAttributeId.get(pav.getAttributeId());
								if (aValue == null) {
									valueByAttributeId.put(pav.getAttributeId(), 
											new AttributeValue(pav.getAttributeId(), 
													pav.getAttributeValue()));
								}
								else {
									if (! aValue.value.equals(pav.getAttributeValue())) {
										aValue.multiple = true;
										anyMultiple = true;
									}
								}
							}

							
							Consumer<KdxSample> visitor = new Consumer<KdxSample>() {
                                @Override
                                public void accept(KdxSample sample) {
                                    String raw = sample.getTraitValue();
                                    TraitValueType type = TraitValue.classify(raw);
                                    String keyString = InstanceIdentifierUtil.getInstanceIdentifier(sample);
                                    TraitInstance trait = traitById.get(keyString);
                                    if (trait != null) {
                                        String displayValue = SampleValue.toDisplayValue(raw, type, 
                                                trait.getTraitDataType(), 
                                                dateFormat, 
                                                trial.getTrialPlantingDate());

                                        Pair<Integer, Integer> key = new Pair<>(sample.getTraitId(), sample.getTraitInstanceNumber());
                                        SampleValue sampleValue = sampleMap.get(key);
                                        if (sampleValue == null) {
                                            sampleMap.put(key, new SampleValue(raw, type, displayValue));
                                        }
                                        else {
                                            sampleValue.addDisplayValue(displayValue);
                                        }   
                                    }
                                }							    
							};
							
							curationData.visitSamplesForPlotOrSpecimen(plot, visitor);
						}
					}
				}
			}
		}
		
		plotCellChoicesPanel.updateData(sampleMap, valueByAttributeId, anyMultiple);
	}

	private void changeVisitOrder() {
		PlotVisitList plotVisitList = plotCellRenderer.getPlotVisitList();
		OrOrTr ort = plotVisitList==null ? trial.getTrialLayout() : plotVisitList.getVisitOrder();
		PlotsPerGroup ppg = plotVisitList==null ? PlotsPerGroup.ONE : plotVisitList.getPlotsPerGroup();
		
		CollectionPathSetupDialog ssd = new CollectionPathSetupDialog(
				GuiUtil.getOwnerWindow(FieldLayoutViewPanel.this), 
				"Select Collection Path");
		ssd.setOrOrTr(ort, ppg);

		ssd.setVisible(true);
		if (ssd.visitOrder != null) {
//			System.out.println("VisitOrder: " + ssd.visitOrder); //$NON-NLS-1$
//			System.out.println("PlotsPerGroup: " + ssd.plotsPerGroup); //$NON-NLS-1$
			plotVisitList = buildPlotVisitList(ssd.visitOrder, ssd.plotsPerGroup);
			plotCellRenderer.setPlotVisitList(plotVisitList);
			
			changeVisitOrderAction.putValue(Action.SMALL_ICON, 
					KDClientUtils.getIcon(plotVisitList.getVisitOrder().imageId));
		}
	}

	@Override
	public void clearSelection() {
		fieldLayoutTable.clearSelection();
	}

	/**
	 * @param row 
	 * @param value
	 * 
	 */
	@Override
	public void setTemporaryValue(Collection<CurationCellValue> ccvs, Comparable<?> value) {
	    
	    for (CurationCellValue ccv : ccvs) {
    		Map<TraitInstance,Comparable<?>> valueByTraitInstance = new HashMap<TraitInstance,Comparable<?>>();
    		
    		XYPos cell = fieldLayoutTableModel.getXYForPlot(ccv.getPlotId());
    		int col = cell.x;
    		int row = cell.y;
    		
    		TraitInstance ti = fieldViewSelectionModel.getActiveTraitInstance(true);
    		if (ti != null) {
    			valueByTraitInstance.put(ti, value);
    
    			plotCellRenderer.setTemporaryValue(row, col, valueByTraitInstance);
    			if (row > -1 && col > -1) {
    				fieldLayoutTableModel.fireTableCellUpdated(row, col);
    			}
    		}
	    }
	}
	
	@Override
	public String getStoreId() {
	    return fieldViewSelectionModel.getStoreId();
	}
	
	@Override
	public void refreshSelectedMeasurements(String fromWhere) {
	    fieldViewSelectionModel.refreshSelectedMeasurements(fromWhere);
	}
	/**
	 * @param column 
	 * @param row 
	 * 
	 */
	@Override
	public void clearTemporaryValues(Collection<CurationCellValue> ccvs) {		
	    for (CurationCellValue ccv : ccvs) {
    	    XYPos cell = fieldLayoutTableModel.getXYForPlot(ccv.getPlotId());
    		int col = cell.x;
    		int row = cell.y;
    		
    		plotCellRenderer.clearTemporaryValues();
    		if (row > -1 && col > -1) {
    			fieldLayoutTableModel.fireTableCellUpdated(row, col);
    		}
	    }
	}

//	public TraitInstance getActiveTraitInstance() {
//		return fieldViewSelectionModel.getActiveTraitInstance(true);
//	}

	private void updateActiveTraitInstance() {
		TraitInstance ti = null;
		Object item = traitInstanceCombo.getSelectedItem();
		if (item instanceof TraitInstance && item != TraitInstanceCellRenderer.TRAIT_TO_EDIT) {
			ti = (TraitInstance) item;
		}
		
		fireTraitInstanceActivated(ti);
		
		fieldViewSelectionModel.setActiveTraitInstance(ti);
		
		fieldLayoutTable.repaint();
	}

    public void addRefreshListener(RefreshListener l) {
        listenerList.add(RefreshListener.class, l);
    }
    
    public void removeRefreshListener(RefreshListener l) {
        listenerList.remove(RefreshListener.class, l);
    }
    
    public void fireRefreshRequired() {
        for (RefreshListener l : listenerList.getListeners(RefreshListener.class)) {
            l.refreshRequired(this);
        }
    }
    
    public void fireTraitInstanceActivated(TraitInstance ti) {
        for (RefreshListener l : listenerList.getListeners(RefreshListener.class)) {
            l.traitInstanceActivated(this, ti);
        }
    }

    /*
     * 
     */

    public void initialiseAfterOpening() {
        
        Set<String> names = plotCellChoicesPanel.getChosenPlotValueRetrievers().stream()
            .filter(vr -> vr instanceof PlotAttributeValueRetriever)
            .map(vr -> ((PlotAttributeValueRetriever) vr).getAttributeName())
            .collect(Collectors.toSet());
        if (! names.isEmpty()) {
            plotCellRenderer.setAttributeNames(names);
            plotCellRenderer.updateTableRowHeight(fieldLayoutTable);
            fieldLayoutTable.repaint();
        }
    }

}
