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
package com.diversityarrays.kdxplore.heatmap;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Specimen;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.vistool.Msg;

import net.pearcan.heatmap.HeatMapLocationValue;
import net.pearcan.heatmap.HeatMapModel;
import net.pearcan.heatmap.ValueModel;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class HeatMapToolTipFactory implements Function<HeatMapLocationValue, String> {
		
		private static final boolean DEBUG = Boolean.getBoolean("HeatMapToolTip.DEBUG");
		
		private final HeatMapModel<PlotOrSpecimen> model;
        private final ValueModel<PlotOrSpecimen, Number> valueModel;

        private final int columnOffset;
		private final int rowOffset;
		private final CurationContext context;
		private final TraitInstance traitInstance;
		private final Map<Point,PlotOrSpecimen> plotSpecimensByPoint;
		
		public HeatMapToolTipFactory(
				CurationContext context, 
				TraitInstance traitInstance, 
				HeatMapModel<PlotOrSpecimen> model, 
				ValueModel<PlotOrSpecimen, Number> vm,
				Point columnRowOffset,
				Map<Point,PlotOrSpecimen> plotSpecimensByPoint) 
		{
			this.context = context;
			this.traitInstance = traitInstance;
			this.model = model;
			this.valueModel = vm;
			this.columnOffset = columnRowOffset.x;
			this.rowOffset = columnRowOffset.y;
			
			this.plotSpecimensByPoint = plotSpecimensByPoint;
		}
		
		@Override
		public String apply(HeatMapLocationValue wp) {
			switch (wp.location) {
			case LEGEND:
				if (wp.scaleValue==null) {
					return model.getScaleName();
				}
				// TODO make this return the non-numeric values if zTraitInstance is not numeric
                return model.getScaleName() + ": " + valueModel.getFormattedValueFor(wp.scaleValue);
				
			case HEATMAP:
				
				StringBuilder sb = new StringBuilder("<HTML>");
				sb.append(StringUtil.htmlEscape(model.getRowName()))
					.append(' ').append(rowOffset + wp.row)
					.append(", ").append(StringUtil.htmlEscape(model.getColumnName()))
					.append(' ').append(columnOffset + wp.column);
				
				String s = ""; // model.getNonNumberValueAt(wp.row, wp.column);
                if (s .isEmpty()) {
                    sb.append(" : ").append(StringUtil.htmlEscape(s));
                }
                else {
                    Number cellValue = valueModel.getValueForContent(model.getCellContentAt(new Point(wp.column, wp.row)));
                    if (DEBUG) {
                        System.err.println("HeatMapToolTip: wp" + wp + " value=" + cellValue); 
                    }                    
                    if (cellValue != null) {
                        List<PlotOrSpecimen> contents = valueModel.getContentsForValue(cellValue);
                        if (! contents.isEmpty()) {
                            PlotIdentOption pio = context.getTrial().getPlotIdentOption();
                            Function<PlotOrSpecimen, String> pos2string = new Function<PlotOrSpecimen, String>() {
                                @Override
                                public String apply(PlotOrSpecimen pos) {
                                    if (pos instanceof Plot) {
                                        return pio.createPlotIdentString((Plot) pos, "/");
                                    }
                                    if (pos instanceof Specimen) {
                                        Specimen s = (Specimen) pos;
                                        return "#" + s.getSpecimenNumber();
                                    }
                                    return null;
                                }
                                
                            };
                            sb.append(contents.stream().map(pos2string)
                                .filter(spec -> spec != null)
                                .collect(Collectors.joining("<BR>", ": ", "")));
                        }
//                        .append(valueModel.getFormattedValueFor(cellValue));
                    }
                }

				if (plotSpecimensByPoint != null) {
					appendInfoAtLocation(wp, sb);
				}

				return sb.toString();
				
			default:
				break;
			}
			return null;
		}

        private void appendInfoAtLocation(HeatMapLocationValue wp, StringBuilder sb) {
            Point pt = new Point(wp.column, wp.row);
            PlotOrSpecimen plotSpecimens = plotSpecimensByPoint.get(pt);
            if (plotSpecimens != null) { // && plotSpecimens.length==1) {
            	// Only do this if there is a single PlotId at the location
                PlotOrSpecimen pos = plotSpecimens;// [0];
            	List<CurationCellValue> cellValues = context.getCurationCellValuesForPlot(pos);
            	
            	TraitNameStyle traitNameStyle = context.getTrial().getTraitNameStyle();
            	
            	for (CurationCellValue ccv : cellValues) {
            		sb.append("<br>"); //$NON-NLS-1$
            		CurationCellId curationCellId = ccv.getCurationCellId();
            		
            		Trait trait = curationCellId.traitInstance==null ? null : curationCellId.traitInstance.trait;
            		if (trait == null) {
            			trait = context.getTraitProvider().apply(curationCellId.traitId);
            		}
            		
            		String unknown = Msg.TRAIT_NAME_UNKNOWN();
            		String traitName = trait!=null ? trait.getTraitName() : unknown;
            		
            		if (curationCellId.traitInstance==null) {
            			traitName = (trait!=null ? trait.getAliasOrName() : unknown);
            			traitName = traitNameStyle.makeTraitInstanceName(traitName, curationCellId.instanceNumber);
            		}
            		else {
            			traitName = traitNameStyle.makeTraitInstanceName(curationCellId.traitInstance);
            		}
            		
            		// Make the selected instance name BOLD
            		if (traitInstance.trait.getTraitId() == curationCellId.traitId
            				&& 
            			traitInstance.getInstanceNumber() == curationCellId.instanceNumber)
            		{
            			sb.append("<b>").append(StringUtil.htmlEscape(traitName)).append("</b>: ");
            		}
            		else {
            			sb.append("<i>").append(StringUtil.htmlEscape(traitName)).append("</i>: ");
            		}

            		TraitValue traitValue = context.getTraitValue(traitInstance, pos);
            		
            		String valu = traitValue==null ? "" : traitValue.displayValue;
            		
            		String[] wrappers = ccv.getEditState().getHtmlWrappers(traitValue.suppressed);
            		
            		sb.append(wrappers[0])
            			.append(StringUtil.htmlEscape(valu))
            			.append(wrappers[1]);
            	}
            }
        }
	}
