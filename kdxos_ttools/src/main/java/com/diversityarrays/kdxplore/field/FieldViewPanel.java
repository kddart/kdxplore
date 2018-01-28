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
package com.diversityarrays.kdxplore.field;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.table.TableModel;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.javase.JavaseContext;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.csvio.ImportError;
import com.diversityarrays.kdsmart.db.csvio.PlotIdentCollector;
import com.diversityarrays.kdsmart.db.entities.MediaFileRecord;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialLayout;
import com.diversityarrays.kdsmart.db.util.WhyMissing;
import com.diversityarrays.kdsmart.field.FieldLayout;
import com.diversityarrays.kdsmart.field.FieldLayoutUtil;
import com.diversityarrays.kdsmart.field.GradientChoice;
import com.diversityarrays.kdsmart.scoring.PlotVisitList;
import com.diversityarrays.kdsmart.scoring.PlotVisitListBuilder;
import com.diversityarrays.kdsmart.scoring.PlotsPerGroup;
import com.diversityarrays.kdsmart.scoring.setup.PlotVisitListBuildParams;
import com.diversityarrays.kdxplore.data.util.DatabaseUtil;
import com.diversityarrays.kdxplore.trialtool.SimplePlotCellRenderer;
import com.diversityarrays.kdxplore.ui.CellSelectableTable;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.VisitOrder2D;
import com.diversityarrays.util.XYPos;

import android.content.Context;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.table.RowHeaderTable;
import net.pearcan.ui.table.RowHeaderTableModel;
import net.pearcan.ui.table.RowRemovable;
import net.pearcan.util.StringUtil;

public class FieldViewPanel extends JPanel {

	static private int uniqueId = 0;

	static public PlotVisitList buildPlotVisitList(Trial trial, 
			VisitOrder2D visitOrder, 
			PlotsPerGroup plotsPerGroup,
			Map<Integer,Trait> traitMap,
			Collection<TraitInstance> traitInstances,
			List<Plot> plots) 
	{
		int trialId = trial.getTrialId();

		PlotVisitListBuildParams buildParams = new PlotVisitListBuildParams(trialId);

		buildParams.leftHandedMode = false; // appInstance.isLeftHandedLayout();
		buildParams.lockScoredTraits = true; // params.lockScoredTraits;
		buildParams.useWhiteBackground = true; // params.useWhiteBackground;
		buildParams.visitOrder = visitOrder;
		buildParams.plotsPerGroup = plotsPerGroup;

		PlotVisitListBuilder builder = new PlotVisitListBuilder(
				plots,
				traitMap,
				traitInstances);


		return builder.create(trial, buildParams, GradientChoice.DEFAULT_GRADIENT_CHOICE);
	}


	private PlotVisitList plotVisitList;

	private final FieldLayoutTableModel fieldLayoutTableModel = new FieldLayoutTableModel();

	private final CellSelectableTable fieldLayoutTable = new CellSelectableTable(
			"FieldViewPanel-" + (++uniqueId), 
			fieldLayoutTableModel, 
			true);

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

	private RowHeaderTableModel rhtm = new RowHeaderTableModel(true, fieldLayoutTable, rowRemovable) {		
		public String getRowLabel(int rowIndex) {
			int yCoord = FieldLayoutUtil.convertRowIndexToYCoord(rowIndex, plotVisitList.getTrial(), fieldLayoutTableModel.getFieldLayout());
			return String.valueOf(yCoord);
		}
	};

	private final RowHeaderTable rowHeaderTable = new RowHeaderTable(
			SwingConstants.CENTER, 
			false, 
			fieldLayoutTable, 
			rowRemovable,
			rhtm, RowHeaderTable.createDefaultColumnModel("X/Y")) 
	{
		public String getMarkerIndexName(int viewRow) {
			return "MIN-"+viewRow;
		}
	};

	private final JLabel warningMessage = new JLabel();

	private final SimplePlotCellRenderer plotCellRenderer;

	public enum SeparatorVisibilityOption {
		VISIBLE,
		NOTVISIBLE,
	}

	private final Action changeCollectionOrder = new AbstractAction("Change Path") {
		@Override
		public void actionPerformed(ActionEvent e) {
			showCollectionOrderDialogIsChanged();
		}
	};

	private final Map<Integer,Trait> traitMap;

	private final Trial trial;

	private final boolean hasUserPlotId;

    private final JScrollPane fieldTableScrollPane;

	public FieldViewPanel(PlotVisitList plotVisitList, 
			Map<Integer,Trait> traitMap,
			SeparatorVisibilityOption visible,
			SimplePlotCellRenderer plotRenderer, 
			Component ... extras) 
	{
		super(new BorderLayout());

		this.plotVisitList = plotVisitList;
		this.traitMap = traitMap;

		trial = plotVisitList.getTrial();

		fieldLayoutTableModel.setTrial(trial);

		int rowHeight = fieldLayoutTable.getRowHeight();
		fieldLayoutTable.setRowHeight(4 * rowHeight);

		fieldLayoutTable.setCellSelectionEnabled(true);

		fieldLayoutTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		// IMPORTANT: DO NOT SORT THE FIELD LAYOUT TABLE
		fieldLayoutTable.setAutoCreateRowSorter(false);

		Map<Integer, Plot> plotById = new HashMap<>();
		FieldLayout<Integer> plotIdLayout = FieldLayoutUtil.createPlotIdLayout(
				trial.getTrialLayout(), 
				trial.getPlotIdentSummary(), 
				plotVisitList.getPlots(), 
				plotById);

		KdxploreFieldLayout<Plot> kdxFieldLayout = new KdxploreFieldLayout<Plot>(Plot.class, 
				plotIdLayout.imageId, 
				plotIdLayout.xsize, plotIdLayout.ysize);
		kdxFieldLayout.warning = plotIdLayout.warning;

		String displayName = null;
		for (VisitOrder2D vo : VisitOrder2D.values()) {
			if (vo.imageId == plotIdLayout.imageId) {
				displayName = vo.displayName;
				break;
			}
		}
		//		VisitOrder2D vo = plotVisitList.getVisitOrder();
		KDClientUtils.initAction(plotIdLayout.imageId, changeCollectionOrder, displayName);

		hasUserPlotId = lookForUserPlotIdPresent(plotById, plotIdLayout, kdxFieldLayout);

		this.plotCellRenderer = plotRenderer;
		plotCellRenderer.setShowUserPlotId(hasUserPlotId);

		plotCellRenderer.setPlotXYprovider(getXYprovider());

		plotCellRenderer.setPlotVisitList(plotVisitList);

		fieldLayoutTable.setDefaultRenderer(Plot.class, plotCellRenderer);
		fieldLayoutTable.setCellSelectionEnabled(true);


		fieldLayoutTableModel.setFieldLayout(kdxFieldLayout);

		if (kdxFieldLayout.warning != null && ! kdxFieldLayout.warning.isEmpty()) {
			warningMessage.setText(kdxFieldLayout.warning);
		}
		else {
			warningMessage.setText("");
		}

		fieldLayoutTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		fieldLayoutTable.getTableHeader().setReorderingAllowed(false);
		fieldLayoutTable.setCellSelectionEnabled(true);

		StringBuilder naming = new StringBuilder();
		String nameForRow = plotVisitList.getTrial().getNameForRow();
		if (! Check.isEmpty(nameForRow)) {
			naming.append(nameForRow);
		}
		String nameForCol = plotVisitList.getTrial().getNameForColumn();
		if (! Check.isEmpty(nameForCol)) {
			if (naming.length() > 0) {
				naming.append('/');
			}
			naming.append(nameForCol);
		}
		fieldTableScrollPane = new JScrollPane(fieldLayoutTable);
		if (naming.length() > 0) {
			JLabel cornerLabel = new JLabel(naming.toString());
			fieldTableScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, cornerLabel);
		}
		fieldTableScrollPane.setRowHeaderView(rowHeaderTable);

		//		fieldLayoutTable.setRowHeaderTable(rowHeaderTable);

//		Box extra = Box.createHorizontalBox();
//		extra.add(new JButton(changeCollectionOrder));
//		if (extras != null && extras.length > 0) {
//			extra.add(Box.createHorizontalStrut(8));
//			for (Component c : extras) {
//				extra.add(c);
//			}
//		}
//		extra.add(Box.createHorizontalGlue());

		switch(visible) {
		case NOTVISIBLE :
			break;

		case VISIBLE:
		default:
		    Box top = Box.createHorizontalBox();
		    top.setOpaque(true);
		    top.setBackground(Color.LIGHT_GRAY);
		    top.setBorder(new MatteBorder(0,0,1,0,Color.GRAY));
		    JLabel label = new JLabel("Field");
		    label.setForeground(Color.DARK_GRAY);
		    label.setFont(label.getFont().deriveFont(Font.BOLD));
		    top.add(label);
		    top.add(new JButton(changeCollectionOrder));
		    
		    if (extras != null && extras.length > 0) {
		        top.add(Box.createHorizontalStrut(8));
		        for (Component c : extras) {
		            top.add(c);
		        }
		    }
			add(top, BorderLayout.NORTH);
			break;
		}


		add(fieldTableScrollPane, BorderLayout.CENTER);
		add(warningMessage, BorderLayout.SOUTH);
	}

	JLabel errorLabel  = new JLabel();
	
	/**
	 * use when cant make one
	 * @param errorMessage
	 */
	public FieldViewPanel(String errorMessage) {
		super(new BorderLayout());
		this.traitMap = null;
		this.trial = null;
		this.plotCellRenderer = null;
		this.fieldTableScrollPane = null;
		this.hasUserPlotId = false;
		errorLabel.setText(errorMessage);
		add(errorLabel, BorderLayout.CENTER);
	}
	
	public JScrollPane getFieldTableScrollPane() {
	    return fieldTableScrollPane;
	}
	
	public SimplePlotCellRenderer getPlotCellRenderer() {
	    return plotCellRenderer;
	}

	private boolean lookForUserPlotIdPresent(Map<Integer, Plot> plotById,
			FieldLayout<Integer> plotIdLayout, KdxploreFieldLayout<Plot> kdxFieldLayout) {
		boolean foundUserPlotId = false;
		for (int y = 0; y < plotIdLayout.ysize; ++y) {
			for (int x = 0; x < plotIdLayout.xsize; ++x) {
				Integer id = plotIdLayout.cells[y][x];
				if (id != null) {
					Plot plot = plotById.get(id);
					if (plot.getUserPlotId() != null) {
						foundUserPlotId = true;
					}
					kdxFieldLayout.store_xy(plot, x, y);
				}
			}
		}
		return foundUserPlotId;
	}

	public CellSelectableTable getFieldLayoutTable() {
		return fieldLayoutTable;
	}

	public FieldLayoutTableModel getFieldLayoutTableModel() {
		return this.fieldLayoutTableModel;
	}


	private final Transformer<Plot, Point> xyProvider = new Transformer<Plot, Point>() {
		@Override
		public Point transform(Plot plot) {
			XYPos cell = fieldLayoutTableModel.getXYForPlot(plot.getPlotId());

			int xcoord = FieldLayoutUtil.convertColumnIndexToXCoord(cell.x, trial, getFieldLayout());
			int ycoord = FieldLayoutUtil.convertRowIndexToYCoord(cell.y, trial, getFieldLayout());

			return new Point(xcoord, ycoord);
		}
	};

	// Override this if you need to
	public Transformer<Plot, Point> getXYprovider() {
		return xyProvider;
	}

	// Used by the InventoryWizardStep
	public static FieldViewPanel createUsingPlots (Trial trial, List<Plot> plots,
			KDSmartDatabase database, VisitOrder2D visitOrder, PlotsPerGroup plotsPerGroup, 
			SimplePlotCellRenderer cellRenderer,boolean isItForItemValidation, SeparatorVisibilityOption visibilityOption)
					throws IOException
	{
		FieldViewPanel result = null;
		Context context = JavaseContext.getInstance();
		PlotIdentCollector pic = new PlotIdentCollector(context);


		int lineNumber = 0;
		pic.setUsingAllPlotIdentifiers();
		for (Plot plot : plots) {
			ImportError err = pic.collectPlotIdentifiers(++lineNumber, plot);
			if (err != null) {
				throw new IOException(err.message);
			}
		}

		PlotIdentSummary plotIdentSummary = pic.getPlotIdentSummary();

		trial.setPlotIdentSummary(plotIdentSummary);

		if (plotIdentSummary.hasXandY() || ! plotIdentSummary.plotIdentRange.isEmpty()) {
			return FieldViewPanel.create(database, 
					trial, 
					visitOrder, 
					plotsPerGroup, 
					cellRenderer,
					visibilityOption);
		}

		return result;
	}

	/**
	 * Create a new FieldViewPanel using the TrialLayout of the Trial to determine
	 * the initial VisitOrder and a PlotsPerGroup of ONE.
	 * @param database
	 * @param trial
	 * @return
	 * @throws IOException
	 */
	public static FieldViewPanel create(KDSmartDatabase database,
			Trial trial, 
			SeparatorVisibilityOption option, 
			SimplePlotCellRenderer plotRenderer,
			Component ... extras) 
					throws IOException 
	{
		TrialLayout trialLayout = trial.getTrialLayout();
		VisitOrder2D visitOrder = VisitOrder2D.LL_RIGHT_SERPENTINE;
		if (trialLayout != null) {
			visitOrder = VisitOrder2D.findMatch(trialLayout, visitOrder);
		}		
		return create(database, trial, visitOrder, PlotsPerGroup.ONE, plotRenderer, option, extras);
	}

	/**
	 * Create a new FieldViewPanel using the caller-provided VisitOrder2D and PlotsPerGroup.
	 * @param database
	 * @param trial
	 * @param visitOrder
	 * @param plotsPerGroup
	 * @return
	 * @throws IOException
	 */
	public static FieldViewPanel create(
			KDSmartDatabase database, 
			Trial trial, 
			VisitOrder2D visitOrder, 
			PlotsPerGroup plotsPerGroup,
			SimplePlotCellRenderer plotRenderer,
			SeparatorVisibilityOption visibilityOption, 
			Component ... extras) 
					throws IOException 
	{
		Map<WhyMissing,List<String>> missing = new TreeMap<>();

		Closure<Pair<WhyMissing,MediaFileRecord>> reportMissing = new Closure<Pair<WhyMissing,MediaFileRecord>>() {
            @Override
            public void execute(Pair<WhyMissing,MediaFileRecord> pair) {
                WhyMissing why = pair.first;
                MediaFileRecord mfr = pair.second;
                List<String> list = missing.get(why);
                if (list == null) {
                    list = new ArrayList<>();
                    missing.put(why, list);
                }
                list.add(mfr.getFilePath());
            }
        };
        
        int trialId = trial.getTrialId();
        Map<Integer, Plot> plotById = DatabaseUtil.collectPlotsIncludingMediaFiles(
                database, trialId, "FieldViewPanel", SampleGroupChoice.ANY_SAMPLE_GROUP, 
                reportMissing);

        List<Plot> plots = new ArrayList<>(plotById.values());
        
		if (! missing.isEmpty()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					StringBuilder html = new StringBuilder("<HTML><DL>");
					for (WhyMissing why : missing.keySet()) {
						html.append("<DT>").append(why.name()).append("</DT><DD>");
						List<String> list = missing.get(why);
						if (list.size() == 1) {
							html.append(StringUtil.htmlEscape(list.get(0)));
						}
						else {
							html.append("<UL>");
							for (String s : list) {
								html.append("<LI>").append(StringUtil.htmlEscape(s)).append("</LI>");
							}
							html.append("</UL>");
						}
						html.append("</DD>");
					}
					html.append("</DL>");
					MsgBox.warn(null, html.toString(), "Missing Files");
				}
			});
		}

		Map<Integer,Trait> traitMap = new HashMap<>();
		database.getTrialTraits(trialId).stream().forEach(t -> traitMap.put(t.getTraitId(), t));

		List<TraitInstance> traitInstances = new ArrayList<>();
		Predicate<TraitInstance> traitInstanceVisitor = new Predicate<TraitInstance>() {
			@Override
			public boolean evaluate(TraitInstance ti) {
				traitInstances.add(ti);
				return true;
			}
		};
		database.visitTraitInstancesForTrial(trialId, 
				KDSmartDatabase.WithTraitOption.ALL_WITH_TRAITS, 
				traitInstanceVisitor);

		PlotVisitList plotVisitList = buildPlotVisitList(trial, 
				visitOrder,
				plotsPerGroup,
				traitMap,
				traitInstances,
				plots);

		if (plotRenderer == null) {
			plotRenderer = new SimplePlotCellRenderer();
		}
		return new FieldViewPanel(plotVisitList, traitMap, visibilityOption, plotRenderer, extras);
	}


	public Plot getPlotAt(int modelCol, int modelRow) {
		return fieldLayoutTableModel.getPlot(modelRow, modelCol);
	}

	public Map<Integer, Trait> getTraitMap() {
		return Collections.unmodifiableMap(traitMap);
	}

	public KdxploreFieldLayout<Plot> getFieldLayout() {
		return fieldLayoutTableModel.getFieldLayout();
	}

	public PlotVisitList getPlotVisitList() {
		return plotVisitList;
	}
	
	public enum IsSortOrderChanged {
		CHANGED,
		UNCHANGED
	}
	
	/**
	 * @return one value of IsSortOrderChanged enum.
	 * CHANGED on use pressed.
	 * UNCHANGED on cancel pressed.
	 */

	public IsSortOrderChanged showCollectionOrderDialogIsChanged() {
		
		CollectionPathSetupDialog ssd = new CollectionPathSetupDialog(
				GuiUtil.getOwnerWindow(FieldViewPanel.this), 
				"Select Collection Path");
		ssd.setOrOrTr(plotVisitList.getVisitOrder(), plotVisitList.getPlotsPerGroup());
		ssd.setVisible(true);
		
		if (ssd.visitOrder != null) {

			Map<Integer, Trait> traitMap = new HashMap<>();
			for (TraitInstance ti : plotVisitList.getTraitInstances()) {
				Integer traitId = ti.trait.getTraitId();
				traitMap.put(traitId, ti.trait);
			}

			PlotVisitList newPvl = buildPlotVisitList(plotVisitList.getTrial(), 
					ssd.visitOrder, ssd.plotsPerGroup,
					traitMap,
					plotVisitList.getTraitInstances(),
					plotVisitList.getPlots());

			plotVisitList = newPvl;
			plotCellRenderer.setPlotVisitList(plotVisitList);

			VisitOrder2D vo = plotVisitList.getVisitOrder();
			KDClientUtils.initAction(vo.imageId, changeCollectionOrder, vo.displayName);
			
			repaint();
			
			return IsSortOrderChanged.CHANGED;
		}
		
		return IsSortOrderChanged.UNCHANGED;
	}

	public static FieldViewPanel createErrorPanel(String message) {
		FieldViewPanel panel = new FieldViewPanel(message);
		return panel;
	}
}
