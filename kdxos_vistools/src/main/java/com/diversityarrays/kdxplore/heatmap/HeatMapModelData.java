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

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.bag.TreeBag;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.curate.CurationContext;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever.DateValueRetriever;
import com.diversityarrays.kdxplore.curate.ValueRetriever;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.Check;

import net.pearcan.heatmap.DefaultHeatMapModel;
import net.pearcan.heatmap.DoubleValueModel;
import net.pearcan.heatmap.HeatMapModel;
import net.pearcan.heatmap.IntegerValueModel;
import net.pearcan.heatmap.ValueModel;

public class HeatMapModelData<T extends Number> {

	public final HeatMapModel<PlotOrSpecimen> model;
	public final ValueModel<PlotOrSpecimen, ? extends Number> valueModel;
    public final Bag<String> missingOrBad;
    public final Bag<String> suppressed;
//	public final Map<Point,PlotOrSpecimen> plotSpecimensByPoint = new HashMap<>();
	public final Point columnRowLabelOffset;
	
	public final Map<String,List<Point>> plotPointsByMark = new TreeMap<>();
	
	public final CurationContext context;
	public final ValueRetriever<?> xValueRetriever;
	public final ValueRetriever<?> yValueRetriever;
	public final TraitInstanceValueRetriever<?> traitInstanceValueRetriever;
	
	public final TraitInstance zTraitInstance;
    
	public HeatMapModelData(
			CurationContext ctx,
			ValueRetriever<?> xvr,
			ValueRetriever<?> yvr,
			TraitInstanceValueRetriever<?> tivr
			)
	{
		context = ctx;
		xValueRetriever = xvr;
		yValueRetriever = yvr;
		traitInstanceValueRetriever = tivr;

		zTraitInstance = traitInstanceValueRetriever.getTraitInstance();
		
		String traitInstanceName = context.makeTraitInstanceName(zTraitInstance);

		columnRowLabelOffset = new Point(
				xValueRetriever.getAxisZeroValue(),
				yValueRetriever.getAxisZeroValue());
		
		Class<?> valueClass = traitInstanceValueRetriever.getComparableValueClass();

        Dimension size = new Dimension(
                xValueRetriever.getAxisValueCount(),
                yValueRetriever.getAxisValueCount()
                );

        model = new DefaultHeatMapModel<PlotOrSpecimen>(
                size, 
                traitInstanceName, 
                traitInstanceValueRetriever.getDisplayName(),
                yValueRetriever.getDisplayName(), 
                xValueRetriever.getDisplayName());

		if (Double.class == valueClass) {

            DoubleGenerator generator = new DoubleGenerator();

            ValueInfo<Double> valueInfo = generator.generate(
                    context,
                    xValueRetriever,
                    yValueRetriever,
                    traitInstanceValueRetriever,
                    plotPointsByMark,
                    columnRowLabelOffset);

            Map<Point,List<PlotOrSpecimen>> psListByPoint = valueInfo.plotSpecimensByPoint;
            
            Function<Point,PlotOrSpecimen> psArrayForPoint = new Function<Point,PlotOrSpecimen>(){
                @Override
                public PlotOrSpecimen apply(Point pt) {
                    List<PlotOrSpecimen> list = psListByPoint.get(pt);
                    return Check.isEmpty(list) ? null : list.get(0);
//                    return list.toArray(new PlotOrSpecimen[list.size()]);
                }
            };

            Map<Point, PlotOrSpecimen> contentByPoint = psListByPoint.keySet()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), psArrayForPoint));

            Map<PlotOrSpecimen, Double> valueByContent = new HashMap<>();
            for (Point pt : contentByPoint.keySet()) {
                PlotOrSpecimen pos = contentByPoint.get(pt);
                Double value = valueInfo.valuesByPoint.get(pt);
                if (value != null) {
                    valueByContent.put(pos, value);
                }
            }

            Function<Double, String> formatter = new Function<Double, String>() {                
                Map<Double, String> displayByValue = valueInfo.displayByValue;
                @Override
                public String apply(Double t) {
                    return displayByValue.get(t);
                }
            };
            valueModel = new DoubleValueModel<>(valueByContent, formatter);

            model.setCellContent(contentByPoint);

            missingOrBad = valueInfo.missingOrBad;
            suppressed = valueInfo.suppressed;
        }
		else if (Integer.class == valueClass || traitInstanceValueRetriever.supportsGetAxisValue()) {

		    IntegerGenerator generator = new IntegerGenerator();

            ValueInfo<Integer> valueInfo = generator.generate(
                    context,
                    xValueRetriever,
                    yValueRetriever,
                    traitInstanceValueRetriever,
                    plotPointsByMark,
                    columnRowLabelOffset);

            Map<Point,List<PlotOrSpecimen>> psListByPoint = valueInfo.plotSpecimensByPoint;

            Function<Point, PlotOrSpecimen> psArrayForPoint = new Function<Point,PlotOrSpecimen>(){
                @Override
                public PlotOrSpecimen apply(Point pt) {
                    List<PlotOrSpecimen> list = psListByPoint.get(pt);
                    return Check.isEmpty(list) ? null : list.get(0);
//                    return list.toArray(new PlotOrSpecimen[list.size()]);
                }
            };
// FIXME for Specimen, we DO want the multiple results
            Map<Point, PlotOrSpecimen> contentByPoint = psListByPoint.keySet()
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), psArrayForPoint));

            Map<PlotOrSpecimen, Integer> valueByContent = new HashMap<>();
            for (Point pt : contentByPoint.keySet()) {
                PlotOrSpecimen pos = contentByPoint.get(pt);
                Integer value = valueInfo.valuesByPoint.get(pt);
                if (value != null) {
                    valueByContent.put(pos, value);
                }
            }
            
            model.setCellContent(contentByPoint);

            Function<Integer, String> formatter = new Function<Integer, String>() {                
                Map<Integer, String> displayByValue = valueInfo.displayByValue;
                @Override
                public String apply(Integer t) {
                    return displayByValue.get(t);
                }
            };
            valueModel = new IntegerValueModel<>(valueByContent, formatter);
            
            missingOrBad = valueInfo.missingOrBad;
            suppressed = valueInfo.suppressed;
		}
		else {
			throw new RuntimeException("Unsupported Value Retriever: " + traitInstanceValueRetriever.getDisplayName()); //$NON-NLS-1$
		}

	}

//	public Transformer<HeatMapLocationValue, String> getHeatMapToolTipFactory(HeatMapModel<?> model) {
//
//	    Map<Point,int[]> tooltipIds = null;
//	    if (TrialCoord.X == xValueRetriever.getTrialCoord() 
//	            && 
//	            TrialCoord.Y == yValueRetriever.getTrialCoord()) 
//	    {
//	        tooltipIds = plotIdsByPoint;
//	    }
//
//	    Transformer<HeatMapLocationValue, String> result;
//	    
//	    result = new HeatMapToolTipFactory(
//	            context, 
//	            zTraitInstance, 
//	            model, 
//	            columnRowLabelOffset, 
//	            tooltipIds);
//
//	    return result;
//	}
	
	static class ValueInfo<T> {
		Integer xMin;
		Integer yMin;
		
		public final Map<Point,T> valuesByPoint = new HashMap<>();
		public final Map<T, String> displayByValue = new HashMap<>();

        public final Bag<String> missingOrBad = new TreeBag<>();
        public final Bag<String> suppressed = new TreeBag<>();
		
		public final Map<Point,List<PlotOrSpecimen>> plotSpecimensByPoint = new HashMap<>();
		
		public void addValue(Point pt, T value, String display) {
			xMin = (xMin == null) ? pt.x : Math.min(xMin, pt.x);
			yMin = (yMin == null) ? pt.y : Math.min(yMin, pt.y);
			
			valuesByPoint.put(pt, value);
			displayByValue.put(value, display);
		}

        public void addPlotSpecimenForPoint(Point pt, PlotOrSpecimen pos) {
            List<PlotOrSpecimen> plotSpecimens = plotSpecimensByPoint.get(pt);
            if (plotSpecimens == null) {
                plotSpecimens = new ArrayList<>();
                plotSpecimensByPoint.put(pt,  plotSpecimens);
            }
            plotSpecimens.add(pos);
        }
	}
	
	static interface ValueGenerator<T> {
		
		ValueInfo<T> generate(CurationContext context, 
				ValueRetriever<?> xValueRetriever,
				ValueRetriever<?> yValueRetriever,
				TraitInstanceValueRetriever<?> traitInstanceValueRetriever,
				Map<String,List<Point>> plotPointsByMark,
				Point labelOffset);
			
	}
	
//	new Transformer<HeatMapLocationValue, String>() {
//        @Override
//        public String transform(HeatMapLocationValue wp) {
//            switch (wp.location) {
//            case LEGEND:
//                if (wp.scaleValue==null) {
//                    return model.getScaleName();
//                }
//                return model.getScaleName() + ": " + model.formatValue(wp.scaleValue);
//                
//            case HEATMAP:
//                
//                String rcName = String.format("%s %d, %s %d",
//                        model.getRowName(), (int) (rowLabelOffset + wp.row), 
//                        model.getColumnName(), (int) (columnLabelOffset + wp.column));
//    
//                String fmtValue = model.getFormattedValueAt(wp.row, wp.column);
//
//                if (fmtValue==null || fmtValue.isEmpty())
//                    return rcName;
//                
//                return rcName + " : " + fmtValue;
//                
//            default:
//                break;
//            }
//            return null;
//        }
//    }
	
	class DoubleGenerator implements ValueGenerator<Double> {

		private void changePointOrigin(Map<Point,List<PlotOrSpecimen>> plotSpecimensByPoint,
				Map<Point,Double> valuesByPoint,
				Transformer<Point,Point> pointChanger)
		{
			Map<Point,Double> tmp = new HashMap<>();
			for (Point pt : valuesByPoint.keySet()) {
				Point p2 = pointChanger.transform(pt);
				tmp.put(p2, valuesByPoint.get(pt));
			}
			valuesByPoint.clear();
			valuesByPoint.putAll(tmp);
			
			Map<Point,List<PlotOrSpecimen>> tmp2 = new HashMap<>();
			for (Point pt : plotSpecimensByPoint.keySet()) {
				Point p2 = pointChanger.transform(pt);
				tmp2.put(p2, plotSpecimensByPoint.get(pt));
			}
			plotSpecimensByPoint.clear();
			plotSpecimensByPoint.putAll(tmp2);
		}
		
		@Override
		public ValueInfo<Double> generate(CurationContext context, 
				ValueRetriever<?> xValueRetriever,
				ValueRetriever<?> yValueRetriever,
				TraitInstanceValueRetriever<?> traitInstanceValueRetriever,
				Map<String,List<Point>> plotPointsByMark,
				Point labelOffset) 
		{
			TraitInstance zTraitInstance = traitInstanceValueRetriever.getTraitInstance();
			PlotInfoProvider plotInfoProvider = context.getPlotInfoProvider();
			
			ValueInfo<Double> valueInfo = new ValueInfo<>();

			// Note that getSampleMeasurements() is retrieving the "EditState"
			// KdxSample so we don't need to use TIVR.getAttribute

			Predicate<Plot> plotFilter = null;
			if (ValueRetriever.isEitherOneXorY(xValueRetriever, yValueRetriever)) {
			    plotFilter = ValueRetriever.ONLY_ACTIVATED_PLOTS;
			}
			
			Map<PlotOrSpecimen,KdxSample> sampleByPlot  = context.getSampleMeasurements(
					zTraitInstance, plotFilter);

			for (PlotOrSpecimen pos : sampleByPlot.keySet()) {

			    Sample sample = sampleByPlot.get(pos);

				TraitValue traitValue = traitInstanceValueRetriever.createTraitValue(sample, null);
				if (traitValue.suppressed) {
				    valueInfo.suppressed.add(traitInstanceValueRetriever.getDisplayName());
				    continue;
				}

				Number number = null;
				if (traitValue.comparable instanceof Number) {
					number = (Number) traitValue.comparable;
				}

				if (number != null) {
					double d = number.doubleValue();

					Point pt = null;
					Integer x = xValueRetriever.getAxisValue(plotInfoProvider, pos);
					Integer y = yValueRetriever.getAxisValue(plotInfoProvider, pos);
					if (x != null && y != null) {
						pt = new Point(x, y);
						
						valueInfo.addValue(pt, d, traitValue.displayValue);
			            valueInfo.addPlotSpecimenForPoint(pt, pos);

						Plot plot  = plotInfoProvider.getPlotByPlotId(pos.getPlotId());
						String plotType = plot==null ? null : plot.getPlotType();
						if (! Check.isEmpty(plotType)) {
							List<Point> plotPoints = plotPointsByMark.get(plotType);
							if (plotPoints == null) {
								plotPoints = new ArrayList<>();
								plotPointsByMark.put(plotType, plotPoints);
							}
							plotPoints.add(pt);
						}
					}
				}
				else {
					switch (traitValue.traitValueType) {
					case MISSING:
						valueInfo.missingOrBad.add(TraitValue.EXPORT_VALUE_MISSING);
						break;
					case NA:
						valueInfo.missingOrBad.add(TraitValue.EXPORT_VALUE_NA);
						break;
					case SET:
						valueInfo.missingOrBad.add(traitValue.displayValue);
						break;
					case UNSET:
						valueInfo.missingOrBad.add(TraitValue.EXPORT_VALUE_UNSCORED);
						break;
					default:
						break;				
					}
				}
			}
			
			
			if (valueInfo.xMin != null && xValueRetriever instanceof DateValueRetriever) {
				final int zero = valueInfo.xMin - 1;
				
				labelOffset.x -= zero;

				Transformer<Point,Point> pointChanger = new Transformer<Point,Point>() {
					@Override
					public Point transform(Point pt) {
						return new Point(pt.x - zero, pt.y);
					}
				};
				
				changePointOrigin(valueInfo.plotSpecimensByPoint, valueInfo.valuesByPoint, pointChanger);	
			}
			
			if (valueInfo.yMin != null && yValueRetriever instanceof DateValueRetriever) {
				final int zero = valueInfo.yMin - 1;

				labelOffset.y -= zero;
				
				Transformer<Point,Point> pointChanger = new Transformer<Point,Point>() {
					@Override
					public Point transform(Point pt) {
						return new Point(pt.x, pt.y - zero);
					}
				};
				
				changePointOrigin(valueInfo.plotSpecimensByPoint, valueInfo.valuesByPoint, pointChanger);
			}
			
			return valueInfo;
		}
	}
	
	class IntegerGenerator implements ValueGenerator<Integer> {

        private void changePointOrigin(Map<Point,List<PlotOrSpecimen>> plotSpecimensByPoint,
                Map<Point,Integer> valuesByPoint,
                Transformer<Point,Point> pointChanger)
        {
            Map<Point,Integer> tmp = new HashMap<>();
            for (Point pt : valuesByPoint.keySet()) {
                Point p2 = pointChanger.transform(pt);
                tmp.put(p2, valuesByPoint.get(pt));
            }
            valuesByPoint.clear();
            valuesByPoint.putAll(tmp);
            
            Map<Point,List<PlotOrSpecimen>> tmp2 = new HashMap<>();
            for (Point pt : plotSpecimensByPoint.keySet()) {
                Point p2 = pointChanger.transform(pt);
                tmp2.put(p2, plotSpecimensByPoint.get(pt));
            }
            plotSpecimensByPoint.clear();
            plotSpecimensByPoint.putAll(tmp2);
        }
        
        @Override
        public ValueInfo<Integer> generate(CurationContext context, 
                ValueRetriever<?> xValueRetriever,
                ValueRetriever<?> yValueRetriever,
                TraitInstanceValueRetriever<?> traitInstanceValueRetriever,
                Map<String,List<Point>> plotPointsByMark,
                Point labelOffset) 
        {
            TraitInstance zTraitInstance = traitInstanceValueRetriever.getTraitInstance();
            PlotInfoProvider plotInfoProvider = context.getPlotInfoProvider();
            
            ValueInfo<Integer> valueInfo = new ValueInfo<>();

            // Note that getSampleMeasurements() is retrieving the "EditState"
            // KdxSample so we don't need to use TIVR.getAttribute

            Predicate<Plot> plotFilter = null;
            if (ValueRetriever.isEitherOneXorY(xValueRetriever, yValueRetriever)) {
                plotFilter = ValueRetriever.ONLY_ACTIVATED_PLOTS;
            }

            Map<PlotOrSpecimen,KdxSample> sampleByPlot  = context.getSampleMeasurements(zTraitInstance, plotFilter);

            for (PlotOrSpecimen pos : sampleByPlot.keySet()) {

                Sample sample = sampleByPlot.get(pos);

                TraitValue traitValue = traitInstanceValueRetriever.createTraitValue(sample, null);
                if (traitValue.suppressed) {
                    valueInfo.suppressed.add(traitInstanceValueRetriever.getDisplayName());
                    continue;
                }

                switch (traitValue.traitValueType) {
                case MISSING:
                    valueInfo.missingOrBad.add(TraitValue.EXPORT_VALUE_MISSING);
                    break;
                case NA:
                    valueInfo.missingOrBad.add(TraitValue.EXPORT_VALUE_NA);
                    break;
                case SET:
                    useTraitValue(pos, 
                            traitValue, 
                            plotInfoProvider, 
                            traitInstanceValueRetriever, 
                            valueInfo);
                    break;

                case UNSET:
                    valueInfo.missingOrBad.add(TraitValue.EXPORT_VALUE_UNSCORED);
                    break;
                default:
                    break;              
                }
            }
            
            // Date values need to be specially handled because there *is* no minimum value.
            // TODO change DateValueRetriever to use it's detected minimum because we now do that in the code
            if (valueInfo.xMin != null && xValueRetriever instanceof DateValueRetriever) {
                final int zero = valueInfo.xMin - 1;
                
                labelOffset.x -= zero;

                Transformer<Point,Point> pointChanger = new Transformer<Point,Point>() {
                    @Override
                    public Point transform(Point pt) {
                        return new Point(pt.x - zero, pt.y);
                    }
                };
                
                changePointOrigin(valueInfo.plotSpecimensByPoint, valueInfo.valuesByPoint, pointChanger);    
            }
            
            // Date values need to be specially handled because there *is* no minimum value.
            // TODO change DateValueRetriever to use it's detected minimum because we now do that in the code
            if (valueInfo.yMin != null && yValueRetriever instanceof DateValueRetriever) {
                final int zero = valueInfo.yMin - 1;

                labelOffset.y -= zero;
                
                Transformer<Point,Point> pointChanger = new Transformer<Point,Point>() {
                    @Override
                    public Point transform(Point pt) {
                        return new Point(pt.x, pt.y - zero);
                    }
                };
                
                changePointOrigin(valueInfo.plotSpecimensByPoint, valueInfo.valuesByPoint, pointChanger);
            }
            return valueInfo;
        }

        private void useTraitValue(
                PlotOrSpecimen pos, 
                TraitValue traitValue, 
                PlotInfoProvider plotInfoProvider,
                TraitInstanceValueRetriever<?> traitInstanceValueRetriever,
                ValueInfo<Integer> valueInfo) 
        {
            Number number = traitInstanceValueRetriever.getAxisValue(plotInfoProvider, pos);

            traitInstanceValueRetriever.getAttributeValue(plotInfoProvider, pos, null);
            if (number != null) {

                int d = number.intValue();
                
                Point pt = null;
                Integer x = xValueRetriever.getAxisValue(plotInfoProvider, pos);
                if (x != null) {
                    Integer y = yValueRetriever.getAxisValue(plotInfoProvider, pos);
                    if (y != null) {
                        pt = new Point(x, y);
                        
                        valueInfo.addValue(pt, d, traitValue.displayValue);
                        // FIXME must be able to handle multiple values per pt for Specimen                   

                        valueInfo.addPlotSpecimenForPoint(pt, pos);

                        Plot plot = plotInfoProvider.getPlotByPlotId(pos.getPlotId());
                        String plotType = plot==null ? null : plot.getPlotType();
                        if (! Check.isEmpty(plotType)) {
                            List<Point> plotPoints = plotPointsByMark.get(plotType);
                            if (plotPoints == null) {
                                plotPoints = new ArrayList<>();
                                plotPointsByMark.put(plotType, plotPoints);
                            }
                            plotPoints.add(pt);
                        }
                    }
                }
            }
        }
    }
}
