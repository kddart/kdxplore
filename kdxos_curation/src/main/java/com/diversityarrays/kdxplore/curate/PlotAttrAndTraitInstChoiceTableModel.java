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

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.Icon;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.exportdata.WhichTraitInstances;
import com.diversityarrays.kdxplore.stats.SimpleStatistics;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Pair;

import net.pearcan.color.ColorPair;
import net.pearcan.ui.table.BspAbstractTableModel;


public class PlotAttrAndTraitInstChoiceTableModel extends BspAbstractTableModel {

    static public enum IncludeWhat {
        ALL,
        ONLY_TRAITS,
        ONLY_PLOT_INFO,
        ;

        public boolean includesPlotInfo() {
            return this==ONLY_PLOT_INFO || this==IncludeWhat.ALL;
        }

        public boolean includesTrait() {
            return this==ONLY_TRAITS || this==IncludeWhat.ALL;
        }

        public static IncludeWhat forEvent(AWTEvent event) {
            IncludeWhat result = ONLY_TRAITS;

            boolean shifted = false;
            boolean control = false;
            if (event instanceof ActionEvent) {
                ActionEvent ae = (ActionEvent) event;
                shifted = 0 != (ActionEvent.SHIFT_MASK & ae.getModifiers());
                control = 0 != (ActionEvent.CTRL_MASK & ae.getModifiers());
            }
            else if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                shifted = 0 != (InputEvent.SHIFT_DOWN_MASK & ie.getModifiersEx());
                control = 0 != (InputEvent.CTRL_DOWN_MASK & ie.getModifiersEx());
            }

            if (shifted) {
                result = ALL;
            }
            else if (control) {
                result = ONLY_PLOT_INFO;
            }
            return result;
        }
    }

    private final List<ValueRetriever<?>> plotInfoValueRetrievers = new ArrayList<>();

    private Map<Integer,PlotAttribute> plotAttributeById = new HashMap<>();
	private Map<Integer, AttributeValue> valueByAttributeId = new HashMap<>();

    static private final boolean ADD = true;
    static private final boolean REMOVE = false;
	// = = = = = = = =
	// Trait Instances

    private final List<TraitInstanceValueRetriever<?>> traitInstanceValueRetrievers = new ArrayList<>();
	private Map<Pair<Integer,Integer>,SampleValue> sampleValueByTraitIdAndNumber = new HashMap<>();
	private final TraitNameStyle traitNameStyle;

	// = = = = = =

	private final Set<Integer> choices = new HashSet<>();
	private final Supplier<TraitColorProvider> colorProviderFactory;

	private final CurationData curationData;
    private final DeviceType deviceTypeForSamples;

    private StatsData statsData;
    private boolean allowTraitsWithoutData;

    public final Map<TraitInstance,String> tivrErrorMessages = new LinkedHashMap<>();

    private final int nPlotInfos;

    private CurationContext curationContext;

	static public final int ATTRIBUTE_TRAIT_COLUMN_INDEX = 2;
	public static final int ICON_COLUMN_INDEX = 1;
	public static final int VIEW_COLUMN_INDEX = 0;

	public PlotAttrAndTraitInstChoiceTableModel(
	        CurationContext context,
	        CurationData curationData,
	        DeviceType deviceTypeForSamples,
	        Supplier<TraitColorProvider> colorProviderFactory)
	{
		super("View?", "", "Attribute/Trait", "Field Value(s)");

		this.curationContext = context;
		this.curationData = curationData;
		this.deviceTypeForSamples = deviceTypeForSamples;

		Trial trial = curationData.getTrial();
		traitNameStyle = trial.getTraitNameStyle();

        int maxAttachmentCount = 0;
        int maxSpecimenNumber = 0;
        for (Plot plot : curationData.getPlots()) {
            for (Integer psnum : plot.getSpecimenNumbers(PlotOrSpecimen.INCLUDE_PLOT)) {
                maxSpecimenNumber = Math.max(maxSpecimenNumber, psnum);

                PlotOrSpecimen pos = plot.getPlotOrSpecimen(psnum);
                maxAttachmentCount = Math.max(maxAttachmentCount, pos.getMediaFileCount());
            }
        }

		plotInfoValueRetrievers.clear();

		// ValueType.PLOT_POSITION:
		plotInfoValueRetrievers.addAll(
		        ValueRetrieverFactory.getPlotIdentValueRetrievers(trial));

		// ValueType.PLOT_TYPE: Add if PlotTypes
		Set<String> plotTypes = curationData.getPlots().stream()
		        .map(Plot::getPlotType)
		        .filter(plotType -> ! Check.isEmpty(plotType))
		        .collect(Collectors.toSet());
        if (! plotTypes.isEmpty()) {
            plotInfoValueRetrievers.add(new PlotTypeValueRetriever(plotTypes));
        }

        // ValueType.SPECIMEN_NUMBER:
        // Any specimens?
        if  (maxSpecimenNumber > 0) {
            plotInfoValueRetrievers.add(
                    ValueRetrieverFactory.getSpecimenNumberValueRetriever("Individual#", maxSpecimenNumber));
        }

        // ValueType.ATTACHMENT_COUNT:
        if (maxAttachmentCount > 0) {
            plotInfoValueRetrievers.add(
                    ValueRetrieverFactory.getAttachmentCountValueRetriever("# Attachments", maxAttachmentCount));
        }

        // ValueType.PLOT_NOTE:
        Optional<String> opt = curationData.getPlots().stream()
            .map(p -> p.getNote())
            .filter(n -> ! Check.isEmpty(n))
            .findFirst();
        if (opt.isPresent()) {
            plotInfoValueRetrievers.add(ValueRetrieverFactory.getPlotNoteValueRetriever());
        }

        // ValueType.PLOT_TAGS:
        List<String> sortedTagLabels = curationData.getSortedTagLabels();
        if (! sortedTagLabels.isEmpty()) {
            String joiner = " ";
            plotInfoValueRetrievers.add(ValueRetrieverFactory.getPlotTagsValueRetriever(sortedTagLabels, joiner));
        }

        // ValueType.PLOT_ATTRIBUTE:
		Map<PlotAttribute, Set<String>> plotAttributesAndValues = curationData.getPlotAttributesAndValues();
        for (PlotAttribute pa : plotAttributesAndValues.keySet()) {
            Set<String> values = plotAttributesAndValues.get(pa);

	        PlotAttributeValueRetriever pavr = new PlotAttributeValueRetriever(pa, values);
	        plotInfoValueRetrievers.add(pavr);

	        plotAttributeById.put(pa.getPlotAttributeId(), pa);;
	    }

        Collections.sort(plotInfoValueRetrievers, new Comparator<ValueRetriever<?>> () {
            @Override
            public int compare(ValueRetriever<?> o1, ValueRetriever<?> o2) {
                return o1.getValueType().compareTo(o2.getValueType());
            }
        });
        nPlotInfos = plotInfoValueRetrievers.size();

        // ValueType.TRAIT_INSTANCE

		rePopulateModel();

        curationData.addPropertyChangeListener(CurationData.PROPERTY_N_STD_DEV_FOR_OUTLIER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                rePopulateModel();
            }
        });

		this.colorProviderFactory = colorProviderFactory;
	}

	public void selectPlotIdentAttributes() {
	    boolean anyChanges = false;
	    for (int i = plotInfoValueRetrievers.size(); --i >= 0; ) {
	        switch (plotInfoValueRetrievers.get(i).getTrialCoord()) {
            case PLOT_ID:
            case X:
            case Y:
                if (choices.add(i)) {
                    anyChanges = true;
                }
                break;
            case NONE:
            default:
                break;
	        }
	    }
	    if (anyChanges) {
	        firePlotAttributeChoicesChanged();
	    }
	}

    public boolean getAllowTraitsWithoutData() {
        return allowTraitsWithoutData;
    }

    public void setAllowTraitsWithoutData(boolean b) {
        this.allowTraitsWithoutData = b;
        if (! allowTraitsWithoutData) {
            // Ok - so any that weren't allowed we now remove

            Map<Integer, TraitInstance> tiByRowIndex = new HashMap<>();
            for (Integer rowIndex : choices) {
                TraitInstance ti = getTraitInstance(rowIndex);
                if (! allowTraitInstance(ti)) {
                    tiByRowIndex.put(rowIndex, ti);
                }
            }
            if (! tiByRowIndex.isEmpty()) {
                choices.removeAll(tiByRowIndex.keySet());
                Collection<TraitInstance> coll = tiByRowIndex.values();
                fireTraitInstanceChoicesChanged(REMOVE,
                        coll.toArray(new TraitInstance[coll.size()]));
            }

        }
        fireTableDataChanged();
    }

    private boolean allowTraitInstance(TraitInstance ti) {
        boolean result = false;
        if (ti != null) {
            if (allowTraitsWithoutData) {
                // so don't bother checking
                result = true;
            }
            else {
                SimpleStatistics<?> stats = statsData.getStatistics(ti);
                if (stats!=null && stats.getValidCount() > 0) {
                    result = true;
                }
                else if (TraitDataType.CALC == ti.getTraitDataType()) {
                    result = true;
                }
            }
        }
        return result;
    }

    private void rePopulateModel() {
        statsData = curationData.getStatsData(deviceTypeForSamples);
        traitInstanceValueRetrievers.clear();

        Function<TraitInstance, List<KdxSample>> sampleProvider = curationData.getSampleProvider();
        Trial trial = curationData.getTrial();

        for (TraitInstance ti : statsData.getTraitInstances()) {
            try {
                traitInstanceValueRetrievers.add(TraitInstanceValueRetriever.getValueRetriever(trial, ti, sampleProvider));
            }
            catch (InvalidRuleException e) {
                String tiName = traitNameStyle.makeTraitInstanceName(ti);
                Shared.Log.w("PlotAttrAndTraitInstChoiceTableModel", "tivr " + tiName, e);
                tivrErrorMessages.put(ti, tiName + ": " + e.getMessage());
            }
        }

        fireTableDataChanged();
    }

    public List<TraitInstance> getTraitInstances(WhichTraitInstances which) {
        if (WhichTraitInstances.ALL == which) {
            return statsData.getTraitInstances();
        }

        List<TraitInstance> tmp = new ArrayList<>();
        switch (which) {
        case ALL_WITH_DATA:
            for (TraitInstance ti : statsData.getTraitInstances()) {
                SimpleStatistics<?> ss = statsData.getStatistics(ti);
                if (ss != null) {
                    tmp.add(ti);
                }
            }
            break;
        case SELECTED:
            List<Integer> list = new ArrayList<>(choices);
            Collections.sort(list);
            for (Integer choice : list) {
                if (choice >= nPlotInfos) {
                    TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(choice - nPlotInfos);
                    tmp.add(tivr.traitInstance);
                }
            }
            break;
        case ALL:
            // already handled
        default:
            // nothing
            break;
        }
        return tmp;
    }

	public List<PlotAttribute> getPlotAttributes(boolean all) {
		if (all) {
		    List<PlotAttribute> list = plotInfoValueRetrievers.stream()
		        .filter(vr -> vr instanceof PlotAttributeValueRetriever)
		        .map(vr -> ((PlotAttributeValueRetriever) vr).getPlotAttribute())
		        .collect(Collectors.toList());
		    return list;
		}

		List<PlotAttribute> tmp = new ArrayList<>();

		for (Integer choice : choices) {
			if (choice < nPlotInfos) {
			    ValueRetriever<?> vr = plotInfoValueRetrievers.get(choice);
			    if (vr instanceof PlotAttributeValueRetriever) {
			        tmp.add(((PlotAttributeValueRetriever) vr).getPlotAttribute());
			    }
			}
		}
		return tmp;
	}

    public TraitInstance getTraitInstance(int rowIndex) {
        TraitInstance result = null;
        if (rowIndex >= nPlotInfos) {
            TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
            result = tivr.traitInstance;
        }
        return result;
    }

	// = = = = = = = =
	// Plot Attribute

    public List<ValueRetriever<?>> getChosenPlotValueRetrievers() {

        List<ValueRetriever<?>> result = new ArrayList<>();
        for (Integer choice : choices) {
            if (choice < nPlotInfos) {
                ValueRetriever<?> vr = plotInfoValueRetrievers.get(choice);
                result.add(vr);
            }
        }

        return result;
    }

	private void firePlotAttributeChoicesChanged() {
        List<ValueRetriever<?>> vrList = null;

		for (PlotCellChoicesListener listener : listenerList.getListeners(PlotCellChoicesListener.class)) {
			if (vrList == null) {
			    vrList = getChosenPlotValueRetrievers();
			}
            listener.plotAttributeChoicesChanged(this, vrList);
		}
	}

	public List<TraitInstance> getCheckedInstances() {

		List<TraitInstance> result = new ArrayList<>();

		if (!choices.isEmpty()) {
			List<Integer> rowIndices = new ArrayList<>(choices);
			Collections.sort(rowIndices);

			for (Integer rowIndex : rowIndices) {
				if (rowIndex > nPlotInfos) {
				    TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
					if (tivr!=null && tivr.traitInstance != null) {
						result.add(tivr.traitInstance);
					}
				}
			}
		}

		return result;
	}

	public PlotAttribute getPlotAttribute(PlotAttributeValue pav) {
		return plotAttributeById.get(pav.getAttributeId());
	}

	// = = = = = = = =
	// Trait Instance

	public void setSampleAndAttributeValues(
			Map<Pair<Integer,Integer>,SampleValue> sampleMap,
			Map<Integer, AttributeValue> attrMap)
	{
		sampleValueByTraitIdAndNumber = sampleMap==null ? Collections.emptyMap() : sampleMap;
		valueByAttributeId = attrMap==null ? Collections.emptyMap() : attrMap;
		fireTableDataChanged();
	}

	private void fireTraitInstanceChoicesChanged(boolean added, TraitInstance ... changed) {

		Map<Integer, Set<TraitInstance>> instancesByTraitId = null; // created on demand

		for (PlotCellChoicesListener listener : listenerList.getListeners(PlotCellChoicesListener.class)) {
			if (instancesByTraitId == null) {
				instancesByTraitId = new HashMap<>();

				if (! choices.isEmpty()) {
					List<Integer> rowIndices = new ArrayList<>(choices);
					Collections.sort(rowIndices);

					for (Integer rowIndex : rowIndices) {
						if (rowIndex < nPlotInfos) {
							continue;
						}
						TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
                        TraitInstance ti = tivr.traitInstance;
						Set<TraitInstance> set = instancesByTraitId.get(ti.getTraitId());
						if (set == null) {
							set = new HashSet<>();
							instancesByTraitId.put(ti.getTraitId(), set);
						}
						set.add(ti);
					}
				}
			}
			listener.traitInstanceChoicesChanged(this, added, changed, instancesByTraitId);
		}
	}

	private int getTraitInstanceIndex(TraitInstance ti) {
        int rowIndex = -1;
        for (int i = traitInstanceValueRetrievers.size(); --i >= 0; ) {
            if (ti.equals(traitInstanceValueRetrievers.get(i).traitInstance)) {
                rowIndex = i;
                break;
            }
        }
        return rowIndex;
	}

	public void addSelectedTraitInstance(TraitInstance ti) {
	    int rowIndex = getTraitInstanceIndex(ti);
		if (rowIndex >= 0) {
			int index = nPlotInfos + rowIndex;
			if (choices.add(index)) {
				fireTableCellUpdated(index, 0);
				fireTraitInstanceChoicesChanged(ADD, ti);
			}
		}
	}

	public void removeSelectedTraitInstance(TraitInstance ti) {
        int rowIndex = getTraitInstanceIndex(ti);
		if (rowIndex >= 0) {
			int index = nPlotInfos + rowIndex;
			if (choices.remove(index)) {
				fireTableCellUpdated(index, 0);
				fireTraitInstanceChoicesChanged(REMOVE, ti);
			}
		}
	}

	// = = = = = = = =


	public boolean isPlotAttributeRow(int rowIndex) {
		return rowIndex < plotInfoValueRetrievers.size();
	}

	public void addPlotCellChoicesListener(PlotCellChoicesListener l) {
		listenerList.add(PlotCellChoicesListener.class, l);
	}

	public void removePlotCellChoicesListener(PlotCellChoicesListener l) {
		listenerList.remove(PlotCellChoicesListener.class, l);
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if (0 == column) {
			return Boolean.class;
		}
		else if (column == 1) {
			return Icon.class;
		}
		else {
			return String.class;
		}
	}

	@Override
	public int getRowCount() {
		return nPlotInfos + traitInstanceValueRetrievers.size();
	}

	public Trait getTrait(int rowIndex) {
		Trait result = null;
		if (rowIndex >= nPlotInfos) {
			TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
			if (tivr != null) {
				result = tivr.traitInstance.trait;
			}
		}
		return result;
	}

	@Override
	public Object getValueAt(final int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
		    if (rowIndex < nPlotInfos) {
	            return choices.contains(rowIndex);
		    }
		    TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
		    TraitInstance ti = tivr.traitInstance;
            Boolean result = null;
            if (allowTraitInstance(ti)) {
                result = choices.contains(rowIndex);
            }
            return result;
		}

		if (rowIndex < nPlotInfos) {
		    ValueRetriever<?> vr;
			switch (columnIndex) {
			case 1:
			    return null; // no colour for PlotAttributes
			case 2:
			    vr = plotInfoValueRetrievers.get(rowIndex);
				return vr.getDisplayName();

			case 3:
			    vr = plotInfoValueRetrievers.get(rowIndex);
			    if (vr instanceof PlotAttributeValueRetriever) {
			        PlotAttributeValueRetriever pavr = (PlotAttributeValueRetriever) vr;
			        PlotAttribute pa = pavr.getPlotAttribute();
	                AttributeValue av = valueByAttributeId.get(pa.getPlotAttributeId());
	                if (av == null) {
	                    return null;
	                }
	                return av.multiple ? "*" : av.value;
			    }
			    return null;
			}
			// end is in PlotInfo range
		}
		else {
		    TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
		    TraitInstance ti = tivr.traitInstance;

			switch (columnIndex) {
			case 1:
				TraitColorProvider traitColorProvider = colorProviderFactory.get();
				ColorPair colorPair = traitColorProvider.getTraitInstanceColor(ti);
				if (colorPair != null) {
					Icon icon = traitColorProvider.getTraitLegendTile(ti);
					return icon;
				}
				break;

			case 2:
				if (traitNameStyle==null) {
					return ti.trait.getTraitName() + ";" + ti.getInstanceNumber();
				}
				StringBuilder sb = new StringBuilder();
				if (curationContext.getShowTraitLevelPrefix()) {
				    sb.append(ti.trait.getTraitLevel().prefix);
				}
				if (curationContext.getShowAliasForTraits()) {
	                sb.append(ti.trait.getAliasOrName());
				}
				else {
	                sb.append(ti.trait.getTraitName());
				}
				return traitNameStyle.makeTraitInstanceName(sb.toString(), ti.getInstanceNumber());

			case 3:
				SampleValue sampleValue = sampleValueByTraitIdAndNumber.get(new Pair<>(ti.trait.getTraitId(),ti.getInstanceNumber()));
				String result = null;
				if (sampleValue != null) {
					if (sampleValue.multiple) {
						result = "*";
					}
					else {
						result = sampleValue.displayValue;
					}
				}
				return result;
			}
		}

		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return 0==columnIndex;
	}

	@Override
	public void setValueAt(Object aValue, final int rowIndex, int columnIndex) {
		if (0 != columnIndex || ! (aValue instanceof Boolean)) {
			return;
		}

		boolean b = ((Boolean) aValue).booleanValue();

		if (rowIndex < nPlotInfos) {
			if (b) {
				choices.add(rowIndex);
			}
			else {
				choices.remove(rowIndex);
			}

			fireTableRowsUpdated(rowIndex, rowIndex);
			firePlotAttributeChoicesChanged();
		}
		else {
		    int tiIndex = rowIndex - nPlotInfos;

		    TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(tiIndex);
		    TraitInstance ti = tivr.traitInstance;

		    if (allowTraitInstance(ti)) {
                boolean changed;
                if (b) {
                    changed = choices.add(rowIndex);
                }
                else {
                    changed = choices.remove(rowIndex);
                }

                if (changed) {
                    fireTableRowsUpdated(rowIndex, rowIndex);
                    fireTraitInstanceChoicesChanged(b, ti);
                }
		    }
		}
	}

    public void checkRows(List<Integer> rows) {
        if (Check.isEmpty(rows)) {
            return;
        }

        for (Integer rowIndex : rows) {
            if (choices.add(rowIndex)) {
                fireTableRowsUpdated(rowIndex, rowIndex);
                if (rowIndex < nPlotInfos) {
                    firePlotAttributeChoicesChanged();
                }
                else {
                    TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
                    TraitInstance ti = tivr.traitInstance;
                    fireTraitInstanceChoicesChanged(ADD, ti);
                }
            }
        }
    }

    public void checkAll(IncludeWhat includeWhat) {
        List<Integer> choicesToAdd = new ArrayList<>();

        int n = getRowCount();
        if (n > 0) {
            for (int rowIndex = n; --rowIndex >= 0; ) {
                if (rowIndex < nPlotInfos) {
                    if (includeWhat.includesPlotInfo()) {
                        choicesToAdd.add(rowIndex);
                    }
                }
                else if (includeWhat.includesTrait()){
                    choicesToAdd.add(rowIndex);
                }
            }

            boolean anyPlotInfos = false;
            List<Integer> nonPlotInfoChoices = new ArrayList<>();
            choices.addAll(choicesToAdd);
            for (Integer choice : choicesToAdd) {
                int rowIndex = choice.intValue();
                if (rowIndex < nPlotInfos) {
                    anyPlotInfos = true;
                }
                else {
                    nonPlotInfoChoices.add(choice);
                }
                fireTableRowsUpdated(rowIndex, rowIndex);
            }

            if (nPlotInfos > 0 && anyPlotInfos) {
                firePlotAttributeChoicesChanged();
            }

            if (! traitInstanceValueRetrievers.isEmpty() && ! nonPlotInfoChoices.isEmpty()) {
                List<TraitInstance> allowed = nonPlotInfoChoices.stream()
                    .map(choice -> traitInstanceValueRetrievers.get(choice - nPlotInfos))
                    .map(tivr -> tivr.traitInstance)
                    .filter(ti -> allowTraitInstance(ti))
                    .collect(Collectors.toList());
                TraitInstance[] changed = allowed.toArray(new TraitInstance[allowed.size()]);
                fireTraitInstanceChoicesChanged(ADD, changed);
            }
        }
    }

    public void uncheckAll(IncludeWhat includeWhat) {
        List<Integer> choicesToClear = new ArrayList<>();
        for (Integer choice : choices) {
            if (choice < nPlotInfos) {
                if (includeWhat.includesPlotInfo()) {
                    choicesToClear.add(choice);
                }
            }
            else if (includeWhat.includesTrait()) {
                choicesToClear.add(choice);
            }
        }
        choices.removeAll(choicesToClear);

        if (! choicesToClear.isEmpty()) {
            List<Integer> nonPlotInfoChoices = new ArrayList<>();
            boolean anyPlotInfos = false;
            for (Integer choice : choicesToClear) {
                if (choice < nPlotInfos) {
                    anyPlotInfos = true;
                }
                else {
                    nonPlotInfoChoices.add(choice);
                }
                int rowIndex = choice.intValue();
                fireTableRowsUpdated(rowIndex, rowIndex);
            }

            if (nPlotInfos > 0 && anyPlotInfos) {
                firePlotAttributeChoicesChanged();
            }

            if (! traitInstanceValueRetrievers.isEmpty() && ! nonPlotInfoChoices.isEmpty()) {
                List<TraitInstance> traitInstances = nonPlotInfoChoices.stream()
                        .map(choice -> traitInstanceValueRetrievers.get(choice - nPlotInfos))
                        .map(tivr -> tivr.traitInstance)
                        .collect(Collectors.toList());
//                List<TraitInstance> traitInstances = traitInstanceValueRetrievers.stream()
//                    .map(tivr -> tivr.traitInstance)
//                    .collect(Collectors.toList());
                TraitInstance[] changed = traitInstances.toArray(new TraitInstance[traitInstances.size()]);
                fireTraitInstanceChoicesChanged(REMOVE, changed);
            }
        }
    }


    public void toggleSelection(int rowIndex) {

        if (choices.remove(Integer.valueOf(rowIndex))) {
            fireTableRowsUpdated(rowIndex, rowIndex);
            if (rowIndex < nPlotInfos) {
                firePlotAttributeChoicesChanged();
            }
            else {
                TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
                TraitInstance ti = tivr.traitInstance;
                SimpleStatistics<?> stats = statsData.getStatistics(ti);
                if (stats!=null && stats.getValidCount() > 0) {
                    fireTraitInstanceChoicesChanged(REMOVE, ti);
                }
            }
        }
        else {
            // Possibly add it
            if (rowIndex < nPlotInfos) {
                choices.add(rowIndex);
                fireTableRowsUpdated(rowIndex, rowIndex);
                firePlotAttributeChoicesChanged();
            }
            else {
                // only toggle selection if TraitInstance has data
                TraitInstanceValueRetriever<?> tivr = traitInstanceValueRetrievers.get(rowIndex - nPlotInfos);
                TraitInstance ti = tivr.traitInstance;
                SimpleStatistics<?> stats = statsData.getStatistics(ti);
                if (stats!=null && stats.getValidCount() > 0) {
                    choices.add(rowIndex);
                    fireTableRowsUpdated(rowIndex, rowIndex);
                    fireTraitInstanceChoicesChanged(ADD, ti);
                }
            }

        }
    }

    public Map<TraitInstance, SimpleStatistics<?>> getStatsByTraitInstance() {
        Map<TraitInstance,SimpleStatistics<?>> result = new HashMap<>();
        for (TraitInstanceValueRetriever<?> tivr : traitInstanceValueRetrievers) {
            SimpleStatistics<?> stats = statsData.getStatistics(tivr.traitInstance);
            if (stats != null) {
                result.put(tivr.traitInstance, stats);
            }
        }
        return result;
    }

}
