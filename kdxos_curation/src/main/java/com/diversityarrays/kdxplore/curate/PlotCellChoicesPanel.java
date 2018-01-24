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
package com.diversityarrays.kdxplore.curate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdxplore.curate.PlotAttrAndTraitInstChoiceTableModel.IncludeWhat;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.exportdata.WhichTraitInstances;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Html;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.OptionalCheckboxRenderer;
import com.diversityarrays.util.Pair;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.SeparatorPanel;
import net.pearcan.util.StringUtil;

public class PlotCellChoicesPanel extends JPanel {

	private int lineLengthLimit = KdxplorePreferences.getInstance().getTooltipLineLengthLimit();

	class PatiTable extends JTable {

		PatiTable(PlotAttrAndTraitInstChoiceTableModel model) {
			super(model);
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			Point point = event.getPoint();
			int vcol = columnAtPoint(event.getPoint());
			if (vcol >= 0) {
				int mcol = convertColumnIndexToModel(vcol);
				if (mcol >= 0 && mcol == PlotAttrAndTraitInstChoiceTableModel.ATTRIBUTE_TRAIT_COLUMN_INDEX) {
					int vrow = rowAtPoint(point);
					if (vrow >= 0) {
						int mrow = convertRowIndexToModel(vrow);
						if (mrow >= 0) {
							event.consume();
							Trait trait = ((PlotAttrAndTraitInstChoiceTableModel) getModel()).getTrait(mrow);
							if (trait == null) {
								return null;
							}
							StringBuilder html = new StringBuilder("<HTML>");
							html.append("<b>")
								.append(StringUtil.htmlEscape(trait.getTraitName()))
								.append("</b>");
							String desc = trait.getTraitDescription();
							if (Check.isEmpty(desc)) {
								html.append("<BR>-- No Description Available --");
							}
							else {
								Html.appendHtmlLines(html, desc, lineLengthLimit);
							}
							return html.toString();
						}
					}

				}
			}
			return super.getToolTipText(event);
		}
	}

	private final JLabel multipleMessage = new JLabel(" ");

	private final PlotAttrAndTraitInstChoiceTableModel patiTableModel;
	private final JTable patiTable;

	private final Action showPatiErrorsAction = new AbstractAction("?") {
        @Override
        public void actionPerformed(ActionEvent ae) {
            String html = patiTableModel.tivrErrorMessages.entrySet().stream()
                .map(e -> StringUtil.htmlEscape(traitNameStyle.makeTraitInstanceName(e.getKey()) + ": " + e.getValue()))
                .collect(Collectors.joining("<BR>", "<HTML>", ""));
            MsgBox.error(PlotCellChoicesPanel.this, html, "Errors Encountered");
        }
	};
	private final JButton showPatiErrorsButton = new JButton(showPatiErrorsAction);

	private final Action checkAllAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            IncludeWhat includeWhat = IncludeWhat.forEvent(e);
            patiTableModel.checkAll(includeWhat);
        }
	};
	private final Action checkSelected = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Integer> rows = GuiUtil.getSelectedModelRows(patiTable);
            patiTableModel.checkRows(rows);
        }
	};
    private final Action uncheckAllAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            IncludeWhat includeWhat = IncludeWhat.forEvent(e);
            patiTableModel.uncheckAll(includeWhat);
        }
    };
    private final JCheckBox allowTraitsWithoutDataOption = new JCheckBox("Data Entry Mode");

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent me) {
            boolean right = SwingUtilities.isRightMouseButton(me);
            boolean left = SwingUtilities.isLeftMouseButton(me);
            int clickCount = me.getClickCount();
            if (right && 1==clickCount) {
                if (curationMenuProvider != null) {
                    me.consume();

                    Set<Integer> rowSet = new HashSet<>();
                    for (int row : patiTable.getSelectedRows()) {
                        rowSet.add(row);
                    }
                    int row = patiTable.rowAtPoint(me.getPoint());
                    if (row >= 0) {
                        rowSet.add(row);
                    }
                    List<Integer> vrows = new ArrayList<>(rowSet);
                    Collections.sort(vrows);

                    List<TraitInstance> selectedInstances = new ArrayList<>();
                    for (Integer vrow : vrows) {
                        collectTraitInstance(vrow, selectedInstances);
                    }

                    List<TraitInstance> checkedInstances = patiTableModel.getCheckedInstances();

                    curationMenuProvider.showTraitInstanceTableToolMenu(me, checkedInstances, selectedInstances);
                }
            }
            else if (left && 2==clickCount) {
                int vrow = patiTable.rowAtPoint(me.getPoint());
                if (vrow >= 0) {
                    int mrow = patiTable.convertRowIndexToModel(vrow);
                    if (mrow >= 0) {
                        patiTableModel.toggleSelection(mrow);
                    }
                }
            }
        }

        private void collectTraitInstance(int vrow, List<TraitInstance> selectedInstances) {
            int mrow = patiTable.convertRowIndexToModel(vrow);
            if (mrow >= 0) {
                TraitInstance ti = patiTableModel.getTraitInstance(mrow);
                if (ti != null) {
                    selectedInstances.add(ti);
                }
            }
        }
    };

    private final CurationMenuProvider curationMenuProvider;

    private final TraitNameStyle traitNameStyle;

    private final CurationContext curationContext;

    private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            patiTable.repaint();
        }
    };

	public PlotCellChoicesPanel(
	        CurationContext context,
	        CurationData curationData,
	        DeviceType deviceTypeForSamples,
	        String heading,
	        CurationMenuProvider cmp,
	        Supplier<TraitColorProvider> colorProviderFactory)
	{
		super(new BorderLayout());

		this.curationContext = context;
		this.traitNameStyle = curationData.getTrial().getTraitNameStyle();
		this.curationMenuProvider = cmp;
		patiTableModel = new PlotAttrAndTraitInstChoiceTableModel(curationContext, curationData, deviceTypeForSamples, colorProviderFactory);
		patiTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                showPatiErrorsButton.setVisible(! patiTableModel.tivrErrorMessages.isEmpty());
            }
        });

		patiTable = new PatiTable(patiTableModel);
		patiTable.addMouseListener(mouseAdapter);
		patiTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    checkSelected.setEnabled(patiTable.getSelectedRowCount() > 0);
                }
            }
        });

        curationContext.addPropertyChangeListener(propertyChangeListener);

		TableColumn tc = patiTable.getColumnModel().getColumn(PlotAttrAndTraitInstChoiceTableModel.ATTRIBUTE_TRAIT_COLUMN_INDEX);
		tc.setCellRenderer(new PaTiTableCellRenderer());

		tc = patiTable.getColumnModel().getColumn(PlotAttrAndTraitInstChoiceTableModel.ICON_COLUMN_INDEX);
		tc.setMaxWidth(TraitColorProvider.ICON_SIZE + 8);

		tc = patiTable.getColumnModel().getColumn(PlotAttrAndTraitInstChoiceTableModel.VIEW_COLUMN_INDEX);
		tc.setMaxWidth(40);
        tc.setCellRenderer(new OptionalCheckboxRenderer("No Values"));
//        tc.setCellRenderer(new BooleanRenderer());

        if (heading != null) {
            checkSelected.setEnabled(false);

            KDClientUtils.initAction(ImageId.CHECK_ALL_SMALL, checkAllAction,
                    "<HTML>Check All Traits<BR>SHIFT to <b>also</b> check <i>Plot Info</i><BR>CTRL to <b>only</b> check <i>Plot Info</i>");
            KDClientUtils.initAction(ImageId.UNCHECK_ALL_SQUARE, uncheckAllAction,
                    "<HTML>Uncheck All Traits<BR>SHIFT to <b>also</b> uncheck <i>Plot Info</i><BR>CTRL to <b>only</b> uncheck <i>Plot Info</i>");
            KDClientUtils.initAction(ImageId.CHECK_SELECTED, checkSelected, "Check Selected");

            showPatiErrorsButton.setVisible(false);

            Box buttons = Box.createHorizontalBox();
            buttons.add(showPatiErrorsButton);
            buttons.add(new JButton(checkAllAction));
            buttons.add(new JButton(uncheckAllAction));
            buttons.add(new JButton(checkSelected));
            buttons.add(Box.createHorizontalGlue());
            buttons.add(allowTraitsWithoutDataOption);
            allowTraitsWithoutDataOption.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    patiTableModel.setAllowTraitsWithoutData(allowTraitsWithoutDataOption.isSelected());
                }
            });

            SeparatorPanel headingComponent = GuiUtil.createLabelSeparator(heading, buttons);
            add(headingComponent, BorderLayout.NORTH);

            new CurationContextOptionsPopup(curationContext, headingComponent);
        }
		add(new JScrollPane(patiTable), BorderLayout.CENTER);
		add(multipleMessage, BorderLayout.SOUTH);

		Font font = multipleMessage.getFont();
		multipleMessage.setFont(font.deriveFont(font.getSize2D() * 0.8f));

		updateMultipleMessage(false);
	}

    public void doPostOpenActions() {
        patiTableModel.selectPlotIdentAttributes();
	}

    public List<TraitInstance> getTraitInstances(WhichTraitInstances which) {
        return patiTableModel.getTraitInstances(which);
    }

	public List<PlotAttribute> getPlotAttributes(boolean allElseSelected) {
		return patiTableModel.getPlotAttributes(allElseSelected);
	}

	public PlotAttribute getPlotAttributeForValue(PlotAttributeValue pav) {
		return patiTableModel.getPlotAttribute(pav);
	}

	// = = =

	public void updateData(
			Map<Pair<Integer, Integer>, SampleValue> sampleMap,
			Map<Integer, AttributeValue> valueByAttributeId,
			boolean anyMultiple)
	{
		patiTableModel.setSampleAndAttributeValues(sampleMap, valueByAttributeId);
		updateMultipleMessage(anyMultiple);
	}

	private void updateMultipleMessage(boolean anyMultiple) {
	    if (anyMultiple) {
	        multipleMessage.setText("");
	    }
	    else {
	        multipleMessage.setText("'*' means multiple different values");
	    }
	}

	public void addPlotCellChoicesListener(PlotCellChoicesListener l) {
		patiTableModel.addPlotCellChoicesListener(l);
	}

	public void addSelectedTraitInstance(TraitInstance ti) {
		patiTableModel.addSelectedTraitInstance(ti);
	}

	public void removeSelectedTraitInstance(TraitInstance ti) {
		patiTableModel.removeSelectedTraitInstance(ti);
	}

	class PaTiTableCellRenderer extends DefaultTableCellRenderer {

		private final Font normalFont;
		private final Font italicFont;

		PaTiTableCellRenderer() {
			super();

			normalFont = getFont();
			italicFont = normalFont.deriveFont(Font.ITALIC);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			boolean shouldUseItalics = false;
			TableModel model = table.getModel();
			if (model instanceof PlotAttrAndTraitInstChoiceTableModel) {
				PlotAttrAndTraitInstChoiceTableModel patiModel = (PlotAttrAndTraitInstChoiceTableModel) model;
				int mrow = table.convertRowIndexToModel(row);
				if (mrow >= 0) {
					shouldUseItalics = patiModel.isPlotAttributeRow(mrow);
				}
			}
			setFont(shouldUseItalics ? italicFont : normalFont);
			return this;
		}

	}

	public List<TraitInstance> getCheckedTraitInstances() {
		return patiTableModel.getCheckedInstances();
	}

    public Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance() {
        return patiTableModel.getStatsByTraitInstance();
    }

    public List<ValueRetriever<?>> getChosenPlotValueRetrievers() {
        return patiTableModel.getChosenPlotValueRetrievers();
    }
}
