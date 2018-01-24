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

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Point;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.field.UnicodeChars;
import com.diversityarrays.kdsmart.field.UnicodeChars.Number;
import com.diversityarrays.kdsmart.scoring.PlotVisitGroup;
import com.diversityarrays.kdsmart.scoring.PlotVisitList;
import com.diversityarrays.kdsmart.scoring.WalkSegment;
import com.diversityarrays.kdxplore.curate.AbstractCurationTableCellRenderer;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.CurationTableModel;
import com.diversityarrays.kdxplore.curate.EditState;
import com.diversityarrays.kdxplore.curate.TraitColorProvider;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.fieldview.FieldViewSelectionModel;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.ttools.shared.CommentMarker;
import com.diversityarrays.kdxplore.ttools.shared.SampleIconType;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.SunSwingDefaultLookup;

import android.util.Log;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class PlotCellRenderer extends AbstractCurationTableCellRenderer {

	private final Border lineBorder = new LineBorder(Color.GRAY);
	private final Border emptyBorder = new EmptyBorder(2, 2, 2, 2);
	private final Border border = new CompoundBorder(lineBorder, emptyBorder);
	
	private final Set<String> attributeNames = new HashSet<>();
	private final Transformer<PlotAttributeValue, PlotAttribute> plotAttributeProvider;
	private boolean showUserPlotId;
	private PlotVisitList plotVisitList;
	
	private Map<Integer, Set<TraitInstance>> traitInstancesByTraitId = Collections.emptyMap();

	private CurationData curationData;
	private final Map<Integer,Trait> traitById = new HashMap<>();
	
	private FieldViewSelectionModel fieldViewSelectionModel;
	
	private CurationTableModel curationTableModel;
	
	private TraitColorProvider colorProvider;

	private Border focusSelectedCellHighlightBorder;
	
	private final DateFormat whenDateFormat = TraitValue.getSampleMeasureDateTimeFormat();
	
	public PlotCellRenderer(Transformer<PlotAttributeValue, 
			PlotAttribute> attributeProvider, 
			CurationTableModel ctm) {
		super();
		this.plotAttributeProvider = attributeProvider;
		this.curationTableModel = ctm;
		
		focusSelectedCellHighlightBorder = SunSwingDefaultLookup.getBorder(this, ui, "Table.focusSelectedCellHighlightBorder");
	}

	public void setSelectionModel(FieldViewSelectionModel fieldViewSelectionModel) {
		this.fieldViewSelectionModel = fieldViewSelectionModel;
	}
	
	private TraitInstance activeInstance = null;
	
	public void setActiveInstance(TraitInstance activeInstance) {
		this.activeInstance = activeInstance;
	}
	
	public void updateTableRowHeight(JTable table) {
		FontMetrics fm = getFontMetrics(getFont());
		int lineHeight = fm.getMaxAscent() + fm.getMaxDescent();
		int rowHeight = lineHeight * (3 + attributeNames.size() + 1 + traitInstancesByTraitId.size());
		table.setRowHeight(rowHeight);
	}

	public void setSelectedTraitInstanceNumbersByTraitId(Map<Integer, Set<TraitInstance>> input)
	{
		this.traitInstancesByTraitId = input==null ? Collections.emptyMap() : input;
		for (Integer traitId : traitInstancesByTraitId.keySet()) {
			Set<TraitInstance> tiset = traitInstancesByTraitId.get(traitId);
			Set<Integer> instanceNumbers = new HashSet<>();
			for (TraitInstance ti : tiset) {
				instanceNumbers.add(ti.getInstanceNumber());
			}
		}
	}

	public void setAttributeNames(Set<String> names) {
		this.attributeNames.clear();
		this.attributeNames.addAll(names);
	}

	public PlotVisitList getPlotVisitList() {
		return plotVisitList;
	}

	public void setPlotVisitList(PlotVisitList pvl) {
		this.plotVisitList = pvl;
	}

	public void setCurationData(CurationData cd) {
		this.curationData = cd;
		
		this.colorProvider = curationData.getTraitColorProvider();
		
		Trial trial = curationData.getTrial();
		showUserPlotId = ! trial.getPlotIdentSummary().plotIdentRange.isEmpty();

		traitById.clear();
		for (Trait t : curationData.getTraits()) {
			traitById.put(t.getTraitId(), t);
		}
	}

	@Override
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row,
			int column) 
	{
		boolean cellSelected = table.isCellSelected(row, column);

		super.getTableCellRendererComponent(table, value, cellSelected, hasFocus, row, column);

        setToolTipText(null);

        commentMarker = null;
		sampleIconType = SampleIconType.NORMAL;
		if (value instanceof Plot) {
			Plot plot = (Plot) value;
			handlePlot(table, isSelected, hasFocus, row, column, cellSelected, plot);
            if (! plot.isActivated()) {
                sampleIconType = isSelected
                        ? SampleIconType.INACTIVE_PLOT_SELECTED
                        : SampleIconType.INACTIVE_PLOT;
            }
		}
		else {
			if (cellSelected) {
				this.setBackground(table.getSelectionBackground());
				this.setForeground(table.getSelectionForeground());
			} else {
				this.setBackground(table.getBackground());
				this.setForeground(table.getForeground());
			}
		}

		return this;
	}

    private void handlePlot(JTable table, boolean isSelected, boolean hasFocus, int row, int column,
            boolean cellSelected, Plot plot) {
        StringBuilder sb = new StringBuilder("<HTML>");

        if (plotVisitList != null) {
        	PlotVisitGroup pvg = plotVisitList.getPlotVisitGroup(plot);
        	if (pvg != null) {
        		WalkSegment ws = pvg.walkSegment;
        		sb.append(ws.segmentIndex+1).append(":").append(ws.orientationUDLR.unicodeArrow)
        		.append("<BR>");
        	}
        }

        if (showUserPlotId) {
        	sb.append("<b>")
        	.append(plot.getUserPlotId())
        	.append("</b>");
        }
        
//      Collection<String> tagLabels = plot.getTagLabels();
        // Instead of the above, collect for all SampleGroups
        Set<String> tagLabels = plot.getTagsBySampleGroup().entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .map(tag -> tag.getLabel())
            .collect(Collectors.toSet());        
        
        addAnnotations(plot, tagLabels, sb);

        String plotType = plot.getPlotType();
        if (plotType != null && ! plotType.isEmpty()) {
        	sb.append(": <i>").append(StringUtil.htmlEscape(plotType)).append("</i>");
        }

        
        Pair<Integer,Integer> cell = new Pair<Integer,Integer>(row, column);
        
        Map<TraitInstance, Comparable<?>> tempMap = new HashMap<TraitInstance, Comparable<?>> ();
        if (temporaryValues.containsKey(cell)) {
        	tempMap = temporaryValues.get(cell);
        }
        
        boolean anyAttributesAdded = false;

        if (attributeNames != null && ! attributeNames.isEmpty()) {
        	if (plot.plotAttributeValues != null) {
        		for (PlotAttributeValue pav : plot.plotAttributeValues) {
        			if (attributeNames.contains(pav.plotAttributeName)) {
        				anyAttributesAdded = true;
        				String attrValue = pav.getAttributeValue(); 
        				sb.append("<br/>");
        				PlotAttribute pa = plotAttributeProvider.transform(pav);
        				sb.append("<i>")
        					.append(StringUtil.htmlEscape(pa.getPlotAttributeName()))
        					.append("</i>");
        				sb.append(": ");
        				if (attrValue==null) {
        					sb.append("-no-value-");
        				}
        				else {
        					sb.append(StringUtil.htmlEscape(attrValue));
        				}
        			}
        		}
        	}
        }

        String tttResult = null;
        if (! traitInstancesByTraitId.isEmpty()) {
        	// Appends html to sb. Also sets commentMarker
            tttResult = collectCellHtml(isSelected, plot, sb, tempMap);
        }
        if (! Check.isEmpty(tagLabels)) {
            StringBuilder tmp = new StringBuilder();
            if (tttResult != null) {
                if (! tttResult.startsWith("<HTML>")) {
                    tmp.append("<HTML>");
                }
                tmp.append(tttResult).append("<br>");
            }
            else {
                tmp.append("<HTML>");
            }
            String tagsep = "";
            for (String tag : tagLabels) {
                tmp.append(tagsep).append(tag);
                tagsep = ", ";
            }
            tttResult = tmp.toString();
        }
        setToolTipText(tttResult);

        if (activeInstance != null) {
        	Color c = colorProvider==null
        			? table.getSelectionBackground()
        			: colorProvider.getTraitInstanceColor(activeInstance).getBackground();

        	if (! cellSelected) {
        		cellSelected = fieldViewSelectionModel.isSelectedPlot(plot);
        	}

        	if (cellSelected) {
        		this.setBackground(c);
        		this.setForeground(table.getForeground());
        	} else {
        		this.setBackground(table.getBackground());
        		this.setForeground(table.getForeground());
        	}
        } else {
        	if (cellSelected) {
        		this.setBackground(table.getSelectionBackground());
        		this.setForeground(table.getSelectionForeground());
        	} else {
        		this.setBackground(table.getBackground());
        		this.setForeground(table.getForeground());
        	}
        }


        setText(sb.toString());
        if (isSelected && hasFocus) {				
        	setBorder(focusSelectedCellHighlightBorder);			
        } else {
        	setBorder(border);
        }
    }

    static public void addAnnotations(Plot plot, Set<String> tagLabels, StringBuilder sb) {

        int nAttachments = plot.getMediaFileCount();
        int nSpecimens = plot.getSpecimenCount(false);
        int nLabels = tagLabels==null ? 0 : tagLabels.size();
        
        if (nAttachments > 0 || nSpecimens > 0 || nLabels > 0) {
            
            Number[] values = UnicodeChars.Number.values();
            int maxValues = values.length - 1;

            if (nAttachments > 0) {
                if (nAttachments < maxValues) {
                    sb.append(' ').append(values[nAttachments].parenthesis);
                }
                else {
                    sb.append(' ').append(Number.N_nn.parenthesis);
                }
            }
            
            if (nSpecimens > 0) {
                if (nSpecimens < maxValues) {
                    sb.append(' ').append(values[nSpecimens].positive);
                }
                else {
                    sb.append(' ').append(Number.N_nn.positive);
                }
            }
            
            if (nLabels > 0) {
                if (nLabels < maxValues) {
                    sb.append(' ').append(values[nLabels].negative);
                }
                else {
                    sb.append(' ').append(Number.N_nn.negative);
                }
            }
        }
    }

    // Returns toolTipText or null, appends to sb, sets commentMarker and sampleIconType
	private String collectCellHtml(
			boolean isSelected,
			Plot plot, 
			StringBuilder sb, 
			Map<TraitInstance, Comparable<?>> tempMap)
	{
	    String tttResult = null;

		String sep = "<hr>";

		Trial trial = curationData.getTrial();
		Date trialPlantingDate = trial.getTrialPlantingDate();
		TraitNameStyle traitNameStyle = trial.getTraitNameStyle();
		
		for (Integer traitId : traitInstancesByTraitId.keySet()) {	
			Trait trait = traitById.get(traitId);
		
			for (TraitInstance instance : traitInstancesByTraitId.get(traitId)) {
				
				if (instance == activeInstance) {
					CurationCellValue ccv = curationTableModel.getCurationCellValueForTraitInstanceInPlot(
							activeInstance, plot);
					if (ccv != null) {
					    switch (ccv.getDeviceSampleStatus()) {
                        case MULTIPLE_SAMPLES_MANY_VALUES:
                            commentMarker = CommentMarker.MULTIPLE_VALUES;
                            tttResult = commentMarker.toolTipText;
                            break;
                        case MULTIPLE_SAMPLES_ONE_VALUE:
                            commentMarker = CommentMarker.MULTIPLE_WITH_ONE_VALUE;
                            tttResult = commentMarker.toolTipText;
                            break;
                        case NO_DEVICE_SAMPLES:
                            break;
                        case ONE_DEVICE_SAMPLE:
                            break;
                        default:
                            break;					    
					    }
	                    
						switch (ccv.getEditState()) {
						case CURATED:
							break;
						case MORE_RECENT:
						    // Note: may override MULTIPLE_SAMPLES
						    Pair<CommentMarker,String> pair = moreRecentUnlessSameValue(ccv);
		                        
						    commentMarker = pair.first;
						    tttResult = pair.second;

						    sampleIconType = SampleIconType.RAW;
                            break;
						case RAW_SAMPLE:
							sampleIconType = SampleIconType.RAW;
							break;
						case FROM_DATABASE:
							sampleIconType = SampleIconType.DATABASE;
							break;
						case NO_VALUES:
							sampleIconType = SampleIconType.NORMAL;
							break;
						default:
							sampleIconType = SampleIconType.NORMAL;
							break;					
						}
					}
				}
				
				String traitInstanceName = traitNameStyle.makeTraitInstanceName(instance, trait);
				
				CurationCellValue ccv = curationTableModel.getCurationCellValueForTraitInstanceInPlot(instance, plot);
				
				if (ccv != null) {
					ValidationRule vrule = null;
					try {
						String vrs = trait.getTraitValRule();
						if (vrs != null && ! vrs.isEmpty()) {
							vrule = ValidationRule.create(vrs);
						}
					}
					catch (InvalidRuleException e) {
						Log.w("PlotCellRenderer", 
								"getTableCellRendererComponent." + traitInstanceName, 
								e);
					}
					
					String wrapStart = "";
					String wrapEnd = "";

					KdxSample sample = ccv.getEditStateSampleMeasurement();
					String valueFromSample = "";
					if (sample != null) {
						EditState editState = ccv.getEditState();
						if (editState != null) {
							wrapStart = editState.font.getWrapPrefix();
							wrapEnd = editState.font.getWrapSuffix();
							
							if (sample.isSuppressed()) {
								wrapStart = "<s>" + wrapStart;
								wrapEnd = wrapEnd + "</s>";
							}
						}

						TraitValue traitValue = TraitInstanceValueRetriever.makeTraitValue(sample, instance, vrule, trialPlantingDate);	
						valueFromSample = traitValue.displayValue;
					}

                    sb.append(sep);
					if (wrapStart.isEmpty()) {
						sb.append(StringUtil.htmlEscape(traitInstanceName));
					}
					else {
						sb
    						.append(wrapStart)
    						.append(StringUtil.htmlEscape(traitInstanceName))
    						.append("=");
					}

					Comparable<?> value = tempMap.get(instance);
					if (value != null) {	
						sb.append(StringUtil.htmlEscape(value.toString()));
					} else {
						sb.append(StringUtil.htmlEscape(valueFromSample));
					}
					sb.append(wrapEnd);
				
				}
				
				sep = "<br>";

			} // each traitInstance
		} // each traitId
		
		return tttResult;
	}

	private Map<Point,Map<TraitInstance,Comparable<?>>> temporaryValues = new HashMap<Point,Map<TraitInstance,Comparable<?>>>();
	
	public void setTemporaryValue(int row, int column, Map<TraitInstance,Comparable<?>> value) {		
		Point pt = new Point(column, row);
		if (value == null) {
			temporaryValues.remove(pt);
		}
		else {
			temporaryValues.put(pt, value);		
		}
	}
	
	/**
	 * @param column 
	 * @param row 
	 * 
	 */
	public void clearTemporaryValues() {
		if (temporaryValues != null) {
			temporaryValues.clear();
		}
	}	

}
