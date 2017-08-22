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
package com.diversityarrays.kdxplore.data.kdx;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.event.EventListenerList;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.calc.CalcContext;
import com.diversityarrays.kdxplore.calc.CalcContextDataProvider;
import com.diversityarrays.kdxplore.calc.CalcSamplesConsumer;
import com.diversityarrays.kdxplore.curate.CurationCellId;
import com.diversityarrays.kdxplore.curate.CurationCellIdFactory;
import com.diversityarrays.kdxplore.curate.CurationCellValue;
import com.diversityarrays.kdxplore.curate.EditState;
import com.diversityarrays.kdxplore.curate.EditedSampleInfo;
import com.diversityarrays.kdxplore.curate.PlotAttributeValuesIterator;
import com.diversityarrays.kdxplore.curate.PlotInfoProvider;
import com.diversityarrays.kdxplore.curate.SampleMeasurementStore;
import com.diversityarrays.kdxplore.curate.StatsData;
import com.diversityarrays.kdxplore.curate.TraitColorProvider;
import com.diversityarrays.kdxplore.curate.TraitInstanceValueRetriever;
import com.diversityarrays.kdxplore.curate.undoredo.ChangeManager;
import com.diversityarrays.kdxplore.curate.undoredo.Changeable;
import com.diversityarrays.kdxplore.curate.undoredo.EditedValueChangeable;
import com.diversityarrays.kdxplore.curate.undoredo.EditedValueChangeableGroup;
import com.diversityarrays.kdxplore.curate.undoredo.PlotActivationChangeable;
import com.diversityarrays.kdxplore.curate.undoredo.PlotAndSampleChanger;
import com.diversityarrays.kdxplore.curate.undoredo.SuppressChangeable;
import com.diversityarrays.kdxplore.curate.undoredo.SuppressChangeableGroup;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.dal.SampleType;
import com.diversityarrays.kdxplore.data.jdbc.KdxploreConfigException;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.ObjectUtil;
import com.diversityarrays.util.Pair;

public class CurationData implements PlotInfoProvider, CalcContextDataProvider {

    public static Set<Integer> getTraitIds(CurationData cd) {
        Set<Integer> traitIds = new HashSet<>();

        addTraitIds(traitIds, cd.getDatabaseSampleGroup());
        addTraitIds(traitIds, cd.getEditedSampleGroup());
        for (SampleGroup samples : cd.getDeviceSampleGroups()) {
            addTraitIds(traitIds, samples);
        }

        return traitIds;
    }

    static private void addTraitIds(Set<Integer> traitIds, SampleGroup samples) {
        if (samples != null) {
            for (KdxSample s : samples.getSamples()) {
                traitIds.add(s.getTraitId());
            }
        }
    }

	private final Trial trial;

	private SampleGroup databaseSampleGroup;
	private SampleGroup editedSampleGroup;
	private final SampleGroup calcSampleGroup;

	private final SampleMeasurementStore edited;
	private final SampleMeasurementStore database;
	private final Map<TraitInstance, List<KdxSample>> calculatedSamplesByTraitInstance = new HashMap<>();

	private Map<SampleGroup, SampleMeasurementStore> deviceSamplesBySampleGroup = null;

	private final Map<Integer, List<SampleGroup>> sampleGroupsByDeviceId = new HashMap<>();
	private final Map<Integer, DeviceIdentifier> deviceIdentifierBySampleGroupId = new HashMap<>();

	private final List<Plot> plots = new ArrayList<>();
	private final Map<Integer,Plot> plotByPlotId = new HashMap<>();
	private final Map<Integer,Integer> rowIndexByPlotId = new HashMap<>();

	private List<PlotAttribute> plotAttributes;

	private final Map<String,PlotAttribute> plotAttributeByName = new HashMap<>();
	private final Map<Integer,PlotAttribute> plotAttributeById = new HashMap<>();

	private final Map<PlotAttribute, Set<String>> attributesAndValues = new LinkedHashMap<>();

	private List<TraitInstance> traitInstances;
	private final Map<String,TraitInstance> traitInstanceByIdentifier = new HashMap<>();

	private final Map<Integer,Trait> traitByTraitId = new HashMap<>();

	private List<DeviceIdentifier> deviceIdentifiers;

	private List<PlotPositionIdentifier> ppIdentifiers;

	private List<TrialAttribute> trialAttributes;

	private Map<TraitInstance, TraitInstanceValueRetriever<?>> tiValueRetrieverByTi = new HashMap<>();

//	private final EditedSampleManager editedSampleManager = new EditedSampleManager();

	private ChangeManager<PlotAndSampleChanger> changeManager;

	private KDSmartDatabase kdsmartDatabase;

	private final Map<Integer,Date> sampleGroupDateById = new HashMap<>();

    private final Function<TraitInstance,List<KdxSample>> sampleProvider = new Function<TraitInstance, List<KdxSample>>() {
        @Override
        public List<KdxSample> apply(TraitInstance ti) {
            return getSampleMeasurements(ti);
        }
    };

    private final CurationCellIdFactory curationCellIdFactory = new CurationCellIdFactory();

	public CurationData(Trial trial) {
	    this.trial = trial;
		edited = new SampleMeasurementStore(this, DeviceType.EDITED);
		database = new SampleMeasurementStore(this, DeviceType.DATABASE);

		calcSampleGroup = new SampleGroup();
		calcSampleGroup.setSampleGroupId(-1);
	}

	public Function<TraitInstance,List<KdxSample>> getSampleProvider() {
	    return sampleProvider;
	}

	class PlotAndSampleChangerImpl implements PlotAndSampleChanger {

		private List<CurationCellId> editedSampleCurationCellIds = new ArrayList<>();
		private Map<Boolean, List<Plot>> plotActivationChanges = new HashMap<>();

		@Override
		public Map<Boolean, List<Plot>> getPlotActivationChanges() {
			return plotActivationChanges;
		}

		@Override
		public List<CurationCellId> getEditedCurationCellIds() {
			return editedSampleCurationCellIds;
		}


		@Override
		public void setEditedSampleValue(CurationCellId ccid, KdxSample sample) {
			editedSampleCurationCellIds.add(ccid);
			edited.putSampleMeasurement(ccid, sample);
		}

		@Override
		public void redoPlotActivation(Set<Plot> plots) {
			addActivationChanges(Boolean.TRUE, plots);
			for (Plot p : plots) {
				p.setWhenDeactivated(null);
			}
		}

		@Override
		public void redoPlotDeactivation(Set<Plot> plots, Date date) {
			addActivationChanges(Boolean.FALSE, plots);
			for (Plot p : plots) {
				p.setWhenDeactivated(date);
			}
		}

		private void addActivationChanges(Boolean activation, Set<Plot> plots) {
			List<Plot> list = plotActivationChanges.get(activation);
			if (list == null) {
				list = new ArrayList<>(plots);
				plotActivationChanges.put(activation, list);
			}
			else {
				list.addAll(plots);
			}
		}

		private void addActivationChange(Boolean activation, Plot plot) {
			List<Plot> list = plotActivationChanges.get(activation);
			if (list == null) {
				list = new ArrayList<>();
				plotActivationChanges.put(activation, list);
			}
			list.add(plot);
		}

		@Override
		public void undoPlotDeactivation(Map<Plot, Date> previousDeactivationDateByPlot) {
			for (Plot p : previousDeactivationDateByPlot.keySet()) {
				Date newWhen = previousDeactivationDateByPlot.get(p);
				Date oldWhen = p.getWhenDeactivated();
				p.setWhenDeactivated(newWhen);

				if (oldWhen==null && newWhen!=null) {
					addActivationChange(Boolean.FALSE, p);
				}
				else if (oldWhen!=null && newWhen==null) {
					addActivationChange(Boolean.TRUE, p);
				}
			}
		}
	}

	@Override
	public CurationCellId getCurationCellId(PlotOrSpecimen pos, TraitInstance ti) {
	    return curationCellIdFactory.getCurationCellId(pos, ti);
	}

	public PlotAndSampleChanger getPlotAndSampleChanger() {
		return new PlotAndSampleChangerImpl();
	}

	public void setChangeManager(ChangeManager<PlotAndSampleChanger> changeManager) {
		this.changeManager = changeManager;
	}

	public void setKDSmartDatabase(KDSmartDatabase kdsdb) {
		this.kdsmartDatabase = kdsdb;
	}

    public boolean hasAnySpecimens() {
        Optional<Integer> anyNumber = plots.stream()
            .flatMap(p -> p.getSpecimenNumbers(PlotOrSpecimen.EXCLUDE_PLOT).stream())
            .findFirst();
        return anyNumber.isPresent();
    }

	// CalcContextDataProvider
	@Override
	public Trial getTrial() {
		return trial;
	}

	public int getTrialId() {
		return trial.getTrialId();
	}

	public Collection<Trait> getTraits() {
		return Collections.unmodifiableCollection(traitByTraitId.values());
	}

	private boolean hasPlotType;

	public boolean getHasPlotType() {
		return hasPlotType;
	}

	private List<String> sortedTagLabels = Collections.emptyList();

    private Map<String, Integer> countByTagLabel = Collections.emptyMap();

    public Map<String,Integer> getCountByTagLabel() {
        return countByTagLabel;
    }

	public List<String> getSortedTagLabels() {
	    return sortedTagLabels;
	}

	public void setPlotData(CurationDataCollector cdc)
	throws KdxploreConfigException, IOException
	{
        this.ppIdentifiers = cdc.ppIdentifiers;

        this.trialAttributes = Collections.unmodifiableList(cdc.trialAttributes);
        this.plotAttributes = Collections.unmodifiableList(cdc.plotAttributes);
        this.traitInstances = Collections.unmodifiableList(cdc.traitInstances);

        this.deviceIdentifiers = Collections.unmodifiableList(cdc.deviceIdentifiers);
        for (Pair<SampleGroup, DeviceIdentifier> pair : cdc.sampleGroupData) {
            addDeviceIdentifierForSampleGroup(pair.first, pair.second);
        }

		this.plots.clear();
		this.plots.addAll(cdc.plots);

		this.plotByPlotId.clear();
		this.rowIndexByPlotId.clear();

		hasPlotType = false;
		int rowIndex = 0;
		for (Plot p : plots) {
			String plotType = p.getPlotType();
			if (plotType != null && ! plotType.isEmpty()) {
			    hasPlotType = true;
			}
			plotByPlotId.put(p.getPlotId(), p);
			rowIndexByPlotId.put(p.getPlotId(), rowIndex);
			++rowIndex;
		}

		this.countByTagLabel = cdc.countByTagLabel;
		List<String> tagLabels = new ArrayList<>(cdc.countByTagLabel.keySet());
		Collections.sort(tagLabels);
		sortedTagLabels = Collections.unmodifiableList(tagLabels);


//		Map<TraitInstance, List<TraitInstance>> dependentsByCalcTraitInstances = new HashMap<>();
//		dependentsByCalcTraitInstances.clear();
//		for (TraitInstance ti : traitInstances) {
//			if (TraitDataType.CALC == ti.trait.getTraitDataType()) {
//				// TODO something - whatever I was planning to do !
//			}
//		}

		if (traitColorProvider != null) {
			// Note that this gets done again by TraitInstanceStatsTableModel when
			// it works out who has data to show.
			// TODO make CurationData be the repository for the Statistics because then
			//      it can recompute them when things change and just tell the TISTM to refresh itself
			//      from CurationData.
			traitColorProvider.generateColorMap(traitInstances);
		}


		for (TraitInstance ti : this.traitInstances) {
			Trait t = ti.trait;
			traitByTraitId.put(t.getTraitId(), t);
			traitInstanceByIdentifier.put(InstanceIdentifierUtil.getInstanceIdentifier(ti), ti);

			try {
				TraitInstanceValueRetriever<?> tivr = TraitInstanceValueRetriever.getValueRetriever(trial, ti, sampleProvider);
				tiValueRetrieverByTi.put(ti, tivr);
			} catch (InvalidRuleException e) {
				throw new KdxploreConfigException("Unexpected Error for Trait Instance '" //$NON-NLS-1$
						+ t.getTraitName() + ";" + ti.getInstanceNumber() + "'" ); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		for (PlotAttribute pa : plotAttributes) {
			plotAttributeById.put(pa.getPlotAttributeId(), pa);
			plotAttributeByName.put(pa.getPlotAttributeName(), pa);
		}


		this.attributesAndValues.clear();
		for (Plot plot : plots) {
			for (PlotAttributeValue pav : plot.plotAttributeValues) {

				PlotAttribute pa = plotAttributeById.get(pav.getAttributeId());

				Set<String> set = attributesAndValues.get(pa);
				if (set == null) {
					set = new HashSet<>();
					attributesAndValues.put(pa, set);
				}
				set.add(pav.getAttributeValue());
			}
		}

        calcContext = new CalcContext(this);

        generateCalcSamples();

	}


	private Map<CurationCellId,List<KdxSample>> samplesByCurationCellId = new HashMap<>();

	// CalcContextDataProvider, PlotInfoProvider
	@Override
	public List<TrialAttribute> getTrialAttributes() {
		return trialAttributes==null ? Collections.emptyList() : trialAttributes;
	}

	public List<PlotAttribute> getPlotAttributes() {
		return plotAttributes==null ? Collections.emptyList() : plotAttributes;
	}

	public List<PlotPositionIdentifier> getPlotPositionIdentifiers() {
		return ppIdentifiers==null ? Collections.emptyList() : ppIdentifiers;
	}

	private void addSamplesFromSampleGroup(SampleGroup sampleGroup) {
		if (sampleGroup == null) {
			return;
		}

		Map<String, TraitInstance> traitInstanceByIdent = traitInstances.stream()
		    .collect(Collectors.toMap((ti) ->InstanceIdentifierUtil.getInstanceIdentifier(ti),
		            Function.identity()));

		for (KdxSample s : sampleGroup.getSamples()) {
		    String ident = InstanceIdentifierUtil.getInstanceIdentifier(s);
		    TraitInstance ti = traitInstanceByIdent.get(ident);
		    if (ti == null) {

		    }
		    CurationCellId ccid = curationCellIdFactory.getCurationCellId(ti, s);
			List<KdxSample> list = samplesByCurationCellId.get(ccid);
			if (list == null) {
				list = new ArrayList<>();
				samplesByCurationCellId.put(ccid, list);
			}
			list.add(s);
		}
	}

	public Map<PlotAttribute,Set<String>> getPlotAttributesAndValues() {
		return attributesAndValues;
	}

	public SampleGroup getDatabaseSampleGroup() {
		return databaseSampleGroup;
	}

	public int getDatabaseSampleGroupId() {
		return databaseSampleGroup.getSampleGroupId();
	}

	public SampleGroup getEditedSampleGroup() {
		return editedSampleGroup;
	}

	public List<SampleGroup> getDeviceSampleGroups() {
		List<SampleGroup> result = new ArrayList<>();
		for (List<SampleGroup> list : sampleGroupsByDeviceId.values()) {
			result.addAll(list);
		}
		return result;
	}

	private void addDeviceIdentifierForSampleGroup(SampleGroup sampleGroup, DeviceIdentifier did) {

		sampleGroupDateById.put(sampleGroup.getSampleGroupId(), sampleGroup.getDateLoaded());

		deviceIdentifierBySampleGroupId.put(sampleGroup.getSampleGroupId(), did);

		switch (did.getDeviceType()) {
		case DATABASE:
			this.databaseSampleGroup = sampleGroup;
			database.setSampleMeasurementData(sampleGroup);
			break;

		case EDITED:
			this.editedSampleGroup = sampleGroup;
			edited.setSampleMeasurementData(sampleGroup);
			break;

		case KDSMART:
			Integer deviceId = sampleGroup.getDeviceIdentifierId();
			List<SampleGroup> list = sampleGroupsByDeviceId.get(deviceId);
			if (list == null) {
				list = new ArrayList<>();
				sampleGroupsByDeviceId.put(deviceId, list);
			}
			list.add(sampleGroup);
			break;

		case FOR_SCORING:
		    // IGNORE THESE for CurationData
		    return;

		default:
			throw new RuntimeException("Unsupported DeviceType: " + did.getDeviceType()); //$NON-NLS-1$
		}
		addSamplesFromSampleGroup(sampleGroup);
	}

	// CalcContextDataProvider
	@Override
	public DeviceIdentifier getDeviceIdentifierForSampleGroup(int sampleGroupId) {
		return deviceIdentifierBySampleGroupId.get(sampleGroupId);
	}

	public List<DeviceIdentifier> getDeviceIdentifiers() {
		return Collections.unmodifiableList(deviceIdentifiers);
	}


	private TraitColorProvider traitColorProvider;

	public TraitColorProvider getTraitColorProvider() {
		return traitColorProvider;
	}

	public void setTraitColorProvider(TraitColorProvider tcp) {
		this.traitColorProvider = tcp;
	}

	public SampleMeasurementStore getEditedSampleMeasurementStore() {
		return edited;
	}

	public SampleMeasurementStore getDatabaseSampleMeasurementStore() {
		return database;
	}

	// - - - - - - - -

	public CurationCellValue getCurationCellValue(PlotOrSpecimen pos, TraitInstance traitInstance) {
		CurationCellValue result = null;
		if (pos != null) {

			KdxSample calc = null;
			if (TraitDataType.CALC == traitInstance.trait.getTraitDataType()) {
			    // TODO improve performance here
                List<KdxSample> list = calculatedSamplesByTraitInstance.get(traitInstance);
                if (list != null) {
                    int plotId = pos.getPlotId();
                    for (KdxSample s : list) {
                        if (plotId == s.getPlotId()) {
                            calc = s;
                            break;
                        }
                    }
                }
			}

            CurationCellId ccid = curationCellIdFactory.getCurationCellId(pos, traitInstance);

            KdxSample ed = edited.getSampleMeasurement(ccid);
            KdxSample db = database.getSampleMeasurement(ccid);

            List<KdxSample> raw = getRawSampleMeasurements(ccid);

            result = new CurationCellValue(ccid, ed, db, raw, calc);
		}
		return result;
	}

	public List<KdxSample> getSampleMeasurements(TraitInstance ti, DeviceType deviceType) {
		if (deviceType == null) {
			return getSampleMeasurements(ti);
		}

		List<KdxSample> result = new ArrayList<>();
		for (Plot plot : plots) {
			if (plot.isActivated()) {
				KdxSample sm = getSampleFor(ti, plot, deviceType);
				if (sm != null) {
					result.add(sm);
				}
			}
		}
		return result;
	}

	public Date getSampleGroupDateLoaded(int sampleGroupId) {
		return sampleGroupDateById.get(sampleGroupId);
	}

	public void getKdxSamples(TraitInstance ti, Consumer<KdxSample> consumer) {
        if (TraitDataType.CALC == ti.trait.getTraitDataType()) {
            List<KdxSample> list = calculatedSamplesByTraitInstance.get(ti);
            if (list != null) {
                for (KdxSample s : list) {
                    consumer.accept(s);
                }
            }
        }
        else {
            for (Plot plot : plots) {
                if (plot.isActivated()) {
                    KdxSample sm = getEditStateSampleFor(ti, plot);
                    if (sm != null) {
                        consumer.accept(sm);
                    }
                }
            }
        }
	}

	@Override
	public List<KdxSample> getSampleMeasurements(TraitInstance ti) {

		List<KdxSample> result = new ArrayList<>();
		if (TraitDataType.CALC == ti.trait.getTraitDataType()) {
		    result = calculatedSamplesByTraitInstance.get(ti);
		    if (result == null) {
		        result = Collections.emptyList();
		    }
		}
		else {
	        for (Plot plot : plots) {
	            if (plot.isActivated()) {
	                KdxSample sm;
	                switch (ti.trait.getTraitLevel()) {
                    case PLOT:
                        sm = getEditStateSampleFor(ti, plot);
                        if (sm != null) {
                            result.add(sm);
                        }
                        break;
                    case SPECIMEN:
                        for (Integer snum : plot.getSpecimenNumbers(Plot.EXCLUDE_PLOT)) {
                            PlotOrSpecimen pos = plot.getSpecimen(snum);
                            sm = getEditStateSampleFor(ti, pos);
                            if (sm != null) {
                                result.add(sm);
                            }
                        }
                        break;
                    case UNDECIDABLE:
                        break;
                    default:
                        break;
	                }
	            }
	        }
		}
		return result;
	}

	// CalcContextDataProvider, PlotInfoProvider
	@Override
	public KdxSample getSampleFor(TraitInstance ti, int plotId, DeviceType deviceType) {

		KdxSample result = null;

		Plot plot = getPlotByPlotId(plotId);
		if (plot != null) {
			result = getSampleFor(ti, plot, deviceType);
		}
		return result;
	}

	public KdxSample getSampleFor(TraitInstance ti, PlotOrSpecimen pos, DeviceType deviceType) {
		KdxSample result = null;
		CurationCellId ccid = curationCellIdFactory.getCurationCellId(pos, ti);
		switch (deviceType) {
		case DATABASE:
			result = database.getSampleMeasurement(ccid);
			break;
		case EDITED:
			result = edited.getSampleMeasurement(ccid);
			break;
		case KDSMART:
			List<KdxSample> raw = getRawSampleMeasurements(ccid);
			if (! raw.isEmpty()) {
				result = raw.get(0); // NOTE: the most recent is first
			}
			break;
		case FOR_SCORING:
		    // Not used in editing!
		    break;
		default:
			break;

		}
		return result;
	}

	/**
	 *  NOTE: this logic must match that in
	 *  {@link com.diversityarrays.kdxplore.curate.CurationCellValue#updateEditState}
	 */
	public KdxSample getEditStateSampleFor(TraitInstance ti, PlotOrSpecimen pos) {

	    int wantedSpecimenNumber = pos.getSpecimenNumber();
	    KdxSample result = null;

	    KdxSample calc = null;
	    if (TraitDataType.CALC == ti.trait.getTraitDataType()) {
	        List<KdxSample> list = calculatedSamplesByTraitInstance.get(ti);
	        if (list != null) {
	            int plotId = pos.getPlotId();
	            for (KdxSample sample : list) {
	                if (plotId == sample.getPlotId() && wantedSpecimenNumber==sample.getSpecimenNumber()) {
	                    calc = sample;
	                    break;
	                }
	            }
	        }
	    }

        CurationCellId ccid = curationCellIdFactory.getCurationCellId(pos, ti);

        Pair<EditState,KdxSample> pair = CurationCellValue.getEditStateAndSample(
                edited.getSampleMeasurement(ccid),
                getRawSampleMeasurements(ccid),
                database.getSampleMeasurement(ccid),
                calc);

        result = pair.second;
		return result;
	}

   public Map<Plot,Map<Integer,KdxSample>> getEditStateSamplesByPlot(TraitInstance ti, Predicate<Plot> plotFilter) {

        Map<Plot,Map<Integer,KdxSample>> result = new HashMap<>(plots.size());
        for (Plot plot : plots) {
            if (plotFilter == null || plotFilter.evaluate(plot)) {
                for (Integer psnum : plot.getSpecimenNumbers(PlotOrSpecimen.INCLUDE_PLOT)) {
                    PlotOrSpecimen pos = plot.getPlotOrSpecimen(psnum);
                    KdxSample sm = getEditStateSampleFor(ti, pos);

                    if (sm != null) {
                        Map<Integer, KdxSample> map = result.get(plot);
                        if (map == null) {
                            map = new HashMap<>();
                            result.put(plot, map);
                        }
                        map.put(psnum, sm);
                    }
                }
            }
        }
        return result;
    }


	public Map<PlotOrSpecimen,KdxSample> getEditStateSamplesByPlotOrSpecimen(TraitInstance ti, Predicate<Plot> plotFilter) {
		Map<PlotOrSpecimen,KdxSample> result = new HashMap<>(plots.size());
		for (Plot plot : plots) {
			if (plotFilter == null || plotFilter.evaluate(plot)) {
			    for (Integer psnum : plot.getSpecimenNumbers(PlotOrSpecimen.INCLUDE_PLOT)) {
	                PlotOrSpecimen pos = plot.getPlotOrSpecimen(psnum);
	                KdxSample sm = getEditStateSampleFor(ti, pos);
	                if (sm != null) {
	                    result.put(pos, sm);
	                }
			    }
			}
		}
		return result;
	}

	public int getPlotCount() {
		return plots.size();
	}

	private void setEditedSample(CurationCellId ccid, KdxSample sm) {
		edited.putSampleMeasurement(ccid, sm);
	}

	public KdxSample getEditedSample(CurationCellId ccid) {
		return edited.getSampleMeasurement(ccid);
	}

	// - - - - - - - -

	@Override
	public List<TraitInstance> getTraitInstances() {
		return Collections.unmodifiableList(traitInstances);
	}

	@Override
	public List<Plot> getPlots() {
		return Collections.unmodifiableList(plots);
	}

	@Override
	public Iterable<? extends KdxSample> getSamplesForCurationCellId(CurationCellId ccid) {
		List<KdxSample> list = samplesByCurationCellId.get(ccid);
		return (list != null) ? list: Collections.emptyList();
	}

	@Override
	public void visitSamplesForPlotOrSpecimen(PlotOrSpecimen pos, Consumer<KdxSample> visitor) {
	    int plotId = pos.getPlotId();
	    for (CurationCellId ccid : samplesByCurationCellId.keySet()) {
	        if (ccid.plotId == plotId) {
	            for (KdxSample s : samplesByCurationCellId.get(ccid)) {
	                visitor.accept(s);
	            }
	        }
	    }
	}

	@Override
	public Sample getSampleForTraitInstance(PlotOrSpecimen pos, TraitInstance ti) {
		return getEditStateSampleFor(ti, pos);
	}

	public TraitInstanceValueRetriever<?> getTraitInstanceValueRetriever(Sample sm) {
		TraitInstance ti = getTraitInstanceForSample(sm);
		return ti==null ? null : tiValueRetrieverByTi.get(ti);
	}


	@Override
	public TraitInstance getTraitInstanceForSample(Sample sample) {
		return traitInstanceByIdentifier.get(InstanceIdentifierUtil.getInstanceIdentifier(sample));
	}

	private void buildDeviceSamplesBySampleGroup() {
		if (deviceSamplesBySampleGroup == null) {
			synchronized (this) {
				if (deviceSamplesBySampleGroup == null) {
					deviceSamplesBySampleGroup = new HashMap<>();

					for (List<SampleGroup> csList : sampleGroupsByDeviceId.values()) {
						Collections.sort(csList, MOST_RECENT_DATE_LOADED_COMPARATOR);
						for (SampleGroup sg : csList) {
							SampleMeasurementStore store = new SampleMeasurementStore(this,
									sg.getStoreIdentifier(),
									DeviceType.KDSMART);
							store.setSampleMeasurementData(sg);
							deviceSamplesBySampleGroup.put(sg, store);
						}
					}
				}
			}
		}
	}

	// We sort the KDSmart samples with most recent first
	private static final Comparator<Pair<SampleGroup,KdxSample>> MOST_RECENT_SG_SAMPLE_COMPARATOR = new Comparator<Pair<SampleGroup,KdxSample>>() {
		@Override
		public int compare(Pair<SampleGroup,KdxSample> p1, Pair<SampleGroup,KdxSample> p2) {
			Date d1 = p1.second.getMeasureDateTime();
			if (d1 == null) {
				d1 = p1.first.getDateLoaded();
			}
			Date d2 = p2.second.getMeasureDateTime();
			if (d2 == null) {
				d2 = p2.first.getDateLoaded();
			}
			// NOTE: reversal to get MOST RECENT first
			return ObjectUtil.safeCompare(d2, d1);
		}
	};


	public List<KdxSample> getRawSampleMeasurements(CurationCellId ccid) {

		buildDeviceSamplesBySampleGroup();

		List<Pair<SampleGroup,KdxSample>> tmp = new ArrayList<>();

		for (SampleGroup sg : deviceSamplesBySampleGroup.keySet()) {
			SampleMeasurementStore store = deviceSamplesBySampleGroup.get(sg);
			KdxSample sm = store.getSampleMeasurement(ccid);
			if (sm != null && sm.hasBeenScored()) {
			    // NOTE: only use scored samples
				tmp.add(new Pair<>(sg,sm));
			}
		}
		if (tmp.size() > 1) {
			Collections.sort(tmp, MOST_RECENT_SG_SAMPLE_COMPARATOR);
		}

		List<KdxSample> result = new ArrayList<>(tmp.size());
		for (Pair<SampleGroup,KdxSample> pair : tmp) {
			result.add(pair.second);
		}

		return result;
	}

	private static final Comparator<SampleGroup> MOST_RECENT_DATE_LOADED_COMPARATOR = new Comparator<SampleGroup>() {
		@Override
		public int compare(SampleGroup o1, SampleGroup o2) {
			return o2.getDateLoaded().compareTo(o1.getDateLoaded());
		}
	};

	private static final String TAG = CurationData.class.getSimpleName();

	private static boolean DEBUG_CALC = Boolean.getBoolean("CurationData.DEBUG_CALC"); //$NON-NLS-1$

	// ===============
	// PlotInfoProvider


	@Override
	public void changePlotsActivation(boolean activate, List<Plot> plots) {
		if (activate) {
			activatePlots(plots);
		}
		else {
			deactivatePlots(plots);
		}
	}

	// PlotInfoProvider
    // CalcContextDataProvider
	@Override
	public Plot getPlotByPlotId(int plotId) {
		return plotByPlotId.get(plotId);
	}

	@Override
	public Set<Plot> getPlotsForPlotSpecimens(Collection<PlotOrSpecimen> plotSpecimens) {
	    Set<Plot> result = new HashSet<>();
	    plotSpecimens.stream().map(pos -> plotByPlotId.get(pos.getPlotId()))
	        .filter(p -> p != null)
	        .collect(Collectors.toSet());
	    return result;
	}

	@Override
	public String getPlotAttributeValue(int plotId, String attributeName) {
		String result = null;
		Plot plot = getPlotByPlotId(plotId);
		if (plot != null) {
			for (PlotAttributeValue pav : plot.plotAttributeValues) {
				PlotAttribute pa = plotAttributeById.get(pav.getAttributeId());
				if (pa != null && attributeName.equals(pa.getPlotAttributeName())) {
					result = pav.getAttributeValue();
					break;
				}
			}
		}
		return result;
	}

	// CalcContextDataProvider, PlotInfoProvider
	@Override
	public Map<String,String> getPlotAttributeValues(int plotId) {
		Map<String,String> result = null;

		Plot plot = getPlotByPlotId(plotId);
		if (plot != null) {
			result = new HashMap<>();
			for (PlotAttributeValue pav : plot.plotAttributeValues) {
				PlotAttribute pa = plotAttributeById.get(pav.getAttributeId());
				if (pa != null) {
					result.put(pa.getPlotAttributeName(), pav.getAttributeValue());
				}
			}
		}

		return result;
	}

	@Override
	public Iterator<String> getPlotAttributeValuesIterator(String attributeName) {
		return new PlotAttributeValuesIterator(attributeName, plotByPlotId.values());
	}

	// =============== Sample Creation
	// EditedSampleFactory
	@Override
	public KdxSample createCalcSampleMeasurement(
	        CurationCellId ccid,
			String traitValue,
			Date dateTimeStamp)
	{
		return createEditedSampleMeasurement(ccid,
		        traitValue,
		        dateTimeStamp,
				null, // sampleType
				null, // suppressReason
				calcSampleGroup);
	}

	public 	KdxSample createEditedSampleMeasurement(
			CurationCellValue ccv,
			String traitValue,
			Date dateTimeStamp,
			SampleType sampleType,
			String suppressReason)
	{
		return createEditedSampleMeasurement(ccv.getCurationCellId(),
		        traitValue,
		        dateTimeStamp,
		        sampleType,
		        suppressReason,
		        null /*SampleGroup*/);
	}

	private KdxSample createEditedSampleMeasurement(
	        CurationCellId ccid,
			String traitValue,
			Date dateTimeStamp,
			SampleType sampleType,
			String suppressReason,
			SampleGroup forSampleGroup)
	{
		KdxSample sm = new KdxSample();
		sm.needsToBeSaved = true;

		sm.setSampleGroupId(forSampleGroup==null
				? editedSampleGroup.getSampleGroupId()
				: forSampleGroup.getSampleGroupId());
		sm.setTrialId(getTrialId());

		sm.setPlotId(ccid.plotId);
		sm.setTraitId(ccid.traitId);
		sm.setTraitInstanceNumber(ccid.instanceNumber);
		sm.setSpecimenNumber(ccid.specimenNumber);

		sm.setTraitValue(traitValue);
		sm.setMeasureDateTime(dateTimeStamp);
		if (sampleType != null) {
			sm.setSampleTypeId(sampleType.getTypeId());
		}

		sm.setSuppressedReason(suppressReason);
		return sm;
	}

	public void addUnsavedChangesListener(UnsavedChangesListener l) {
		listenerList.add(UnsavedChangesListener.class, l);
	}

	public void removeUnsavedChangesListener(UnsavedChangesListener l) {
		listenerList.remove(UnsavedChangesListener.class, l);
	}

	public void fireUnsavedChanges() {
		int nChanges = getUnsavedChangesCount();
		for (UnsavedChangesListener l : listenerList.getListeners(UnsavedChangesListener.class)) {
			l.unsavedChangesExist(this, nChanges);
		}
	}

	public int getUnsavedChangesCount() {
		return changeManager.getUndoCount();
	}

	/**
	 * returns the number of Samples saved
	 * @return
	 * @throws IOException
	 */
	public int saveEditedSampleChanges() throws IOException {

		List<KdxSample> samplesToSave = new ArrayList<>();
		for (KdxSample s : edited.getSamples()) {
			if (s.needsToBeSaved) {
				samplesToSave.add(s);
			}
		}

		Set<Integer> sampleGroupIds = new HashSet<>();
		if (! samplesToSave.isEmpty()) {
			kdsmartDatabase.saveMultipleSamples(
					samplesToSave,
					KDSmartDatabase.DONT_UPDATE_KDS_DATE_CHANGED);

			for (KdxSample s : samplesToSave) {
				sampleGroupIds.add(s.getSampleGroupId());
				s.needsToBeSaved = false;
			}
		}

		Set<Plot> changedPlots = new HashSet<>();
		for (Changeable<PlotAndSampleChanger> ch : changeManager.getUndoChangeables()) {
			if (ch instanceof PlotActivationChangeable) {
				changedPlots.addAll( ((PlotActivationChangeable) ch).getPlots());
			}
		}

		if (! changedPlots.isEmpty()) {
			for (Plot plot : changedPlots) {
				kdsmartDatabase.savePlot(plot, false);
			}
		}
		changeManager.clear();

		if (! sampleGroupIds.isEmpty() || ! changedPlots.isEmpty()) {
			fireSamplesSaved(sampleGroupIds, changedPlots);
		}

		return samplesToSave.size() + changedPlots.size();
	}

	// ==== Manage Changeables =====
	// And we manage the Changeables too

	public void addChangeable(Changeable<PlotAndSampleChanger> change) {
		changeManager.addChangeable(change);
	}

	public void addChanges(List<Changeable<PlotAndSampleChanger>> changeList) {
		switch (changeList.size()) {
		case 0:
			break;
		case 1:
			changeManager.addChangeable(changeList.get(0));
			break;
		default:
			EditedValueChangeableGroup changeableGroup = new EditedValueChangeableGroup(changeList);
			changeManager.addChangeable(changeableGroup);
			break;
		}
	}

	public void addSamplesSavedListener(SamplesSavedListener l) {
		listenerList.add(SamplesSavedListener.class, l);
	}

	public void removeSamplesSavedListener(SamplesSavedListener l) {
		listenerList.remove(SamplesSavedListener.class, l);
	}

	protected void fireSamplesSaved(Set<Integer> sampleGroupIds, Set<Plot> changedPlots) {
		int[] sgids = sampleGroupIds.stream().mapToInt(Integer::valueOf).toArray();
		Plot[] plots = changedPlots.toArray(new Plot[changedPlots.size()]);
		for (SamplesSavedListener l : listenerList.getListeners(SamplesSavedListener.class)) {
			l.samplesSaved(this, trial, sgids, plots);
		}
	}

	@Override
	public void addCurationDataChangeListener(CurationDataChangeListener l) {
		listenerList.add(CurationDataChangeListener.class, l);
	}

	@Override
	public void removeCurationDataChangeListener(CurationDataChangeListener l) {
		listenerList.remove(CurationDataChangeListener.class, l);
	}

	private final CalcSamplesConsumer samplesConsumer = new CalcSamplesConsumer() {

		@Override
		public void handleCalcSamplesChanged(TraitInstance traitInstance,
				List<KdxSample> samples)
		{
		    calculatedSamplesByTraitInstance.put(traitInstance,
		            Collections.unmodifiableList(samples));

		    if (DEBUG_CALC) {
    			System.err.println("/////////////////////////////////"); //$NON-NLS-1$
    			System.err.println("///   handleCalcSamplesChanged: "+ traitInstance.trait.getTraitName()); //$NON-NLS-1$
    			System.err.println("/////////////////////////////////"); //$NON-NLS-1$
    			// TODO tell the TraitInstanceStatsTable model (via an EventListener) that the traitInstance has changed
		    }
		}
	};


	protected void fireEditedSamplesChanged(List<CurationCellId> curationCellIds) {

		redoCalculatedTraitInstances(Either.left(curationCellIds));

		for (CurationDataChangeListener l : listenerList.getListeners(CurationDataChangeListener.class)) {
			l.editedSamplesChanged(this, curationCellIds);
		}
		fireUnsavedChanges();
	}

	private void redoCalculatedTraitInstances(Either<List<CurationCellId>, List<Plot>>  changedCellIdsOrPlots) {

		List<CurationCellId> changedCellIds = null;
		List<Plot> changedPlots = null;

		List<TraitInstance> calcsAffected;
		if (changedCellIdsOrPlots.isLeft()) {
			changedCellIds = changedCellIdsOrPlots.left();
			if (DEBUG_CALC) {
				String msg = changedCellIds.stream()
				        .map(CurationCellId::toString)
				        .collect(Collectors.joining("\n", "redoCalc - CCIds:", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				Shared.Log.d(TAG, msg);
			}

			calcsAffected = changedCellIds.stream()
			    .flatMap(ccid -> calcContext.getCalculatedForDependent(ccid.traitInstance)
			            .stream())
			            .collect(Collectors.toList());


		}
		else {
			changedPlots = changedCellIdsOrPlots.right();
			if (DEBUG_CALC) {
				String msg = changedPlots.stream()
				        .map(Plot::toString)
				        .collect(Collectors.joining("\n", "redoCalc - CCIds:", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Shared.Log.d(TAG, msg);
			}
			// Need to do ALL of the calculations for which the plot contains a scored sample for the dependents

			Set<TraitInstance> calcs = new HashSet<>();
			for (Plot plot : changedPlots) {
			    for (CurationCellId ccid : samplesByCurationCellId.keySet()) {
			        if (ccid.plotId == plot.getPlotId()) {
		                for (KdxSample sample : getSamplesForCurationCellId(ccid)) {
		                    if (sample.hasBeenScored()) {
		                        calcs.addAll(calcContext.getCalculatedForDependents(sample));
		                    }
		                }
			        }
			    }
			}

			calcsAffected = new ArrayList<>(calcs);
		}

		if (! calcsAffected.isEmpty()) {
		    if (DEBUG_CALC) {
    		    System.out.println("--- CalcsAffected: " + calcsAffected.size()); //$NON-NLS-1$
    		    calcsAffected.stream()
    		        .forEach(item -> System.out.println("Calc: " + item)); //$NON-NLS-1$
		    }
		    Set<TraitInstance> dependents = calcContext.getDependents(calcsAffected);

		    if (DEBUG_CALC) {
    		    System.out.println("--- Dependents: " + dependents.size()); //$NON-NLS-1$
    		    dependents.stream()
    		        .forEach(item -> System.out.println("Dependent: " + item)); //$NON-NLS-1$
		    }
		    generateCalcSamples();

		    // TODO first, if any of the TraitInstances are used in CALC TraitInstances then
	        //      need to recompute all of the entries and let listeners know.
	        //  calculatedTraitsChanged(List<TraitInstance>)
		}
	}

	private void generateCalcSamples() {
       if (calcContext != null) {
            Map<TraitInstance, String> errorByTraitInstance = new HashMap<>();
            calcContext.generateSamples(samplesConsumer, errorByTraitInstance);
            if (! errorByTraitInstance.isEmpty()) {
                List<String> details = new ArrayList<>();
                String title = "redoCalculatedTraitInstances: Problems while doing CALCs: " //$NON-NLS-1$
                        + errorByTraitInstance.size();
                Shared.Log.w(TAG, title );
                for (TraitInstance ti : errorByTraitInstance.keySet()) {
                    String error = errorByTraitInstance.get(ti);
                    String msg = String.format("\t%s: %s", ti.trait.getTraitName(), error); //$NON-NLS-1$
                    Shared.Log.w(TAG, msg);
                    details.add(msg);
                }
                Shared.Log.w(TAG, "- - - - - - - - - - - - - - -"); //$NON-NLS-1$
                MsgBox.warn(null, String.join("\n", details), title);
            }
        }
	}

	// Some of these may have null oldSample, and some null newSample
	public void registerChangedSamples(List<EditedSampleInfo> infoList) {

		List<CurationCellId> curationCellIds = new ArrayList<>();

		List<Changeable<PlotAndSampleChanger>> changeList = new ArrayList<>();

		for (EditedSampleInfo info : infoList) {
			curationCellIds.add(info.ccid);

			EditedValueChangeable evc = new EditedValueChangeable(
					info.oldEditedSample,
					info.newEditedSample,
					info.ccid);

			changeList.add(evc);

			setEditedSample(info.ccid, info.newEditedSample);
		}

		addChanges(changeList);

		fireEditedSamplesChanged(curationCellIds);
	}

	public void registerChangeablesForNewEditedSamples(List<EditedSampleInfo> infoList) {

		List<Changeable<PlotAndSampleChanger>> changeList = new ArrayList<>();

		List<CurationCellId> curationCellIds = new ArrayList<>(infoList.size());
		for (EditedSampleInfo info : infoList) {
			setEditedSample(info.ccid, info.newEditedSample);

			EditedValueChangeable evc = new EditedValueChangeable(
					info.oldEditedSample,
					info.newEditedSample,
					info.ccid);

			changeList.add(evc);
			curationCellIds.add(info.ccid);
		}

		switch (changeList.size()) {
		case 0:
			break;
		case 1:
			addChangeable(changeList.get(0));
			break;
		default:
			EditedValueChangeableGroup changeableGroup = new EditedValueChangeableGroup(changeList);
			addChangeable(changeableGroup);
			break;
		}

		fireEditedSamplesChanged(curationCellIds);
	}

	public void registerRejectedSampleInfo(List<EditedSampleInfo> infoList, String reason) {
		registerAcceptRejectSampleInfo(infoList, reason);
	}

	public void registerAcceptedSampleInfo(List<EditedSampleInfo> infoList) {
		registerAcceptRejectSampleInfo(infoList, null);
	}

	private void registerAcceptRejectSampleInfo(List<EditedSampleInfo> infoList, String reason) {
		List<SuppressChangeable> changeList = new ArrayList<>();

		List<CurationCellId> curationCellIds = new ArrayList<>(infoList.size());

		for (EditedSampleInfo info : infoList) {
			String oldReason = info.oldEditedSample == null ? null : info.oldEditedSample.getSuppressedReason();

			SuppressChangeable changeable = new SuppressChangeable(
					info, oldReason, reason);
			changeList.add(changeable);

			curationCellIds.add(info.ccid);

			setEditedSample(info.ccid, info.newEditedSample);
		}
		addSuppressedToChangeManager(changeList, reason);

		fireEditedSamplesChanged(curationCellIds);
	}

	private void addSuppressedToChangeManager(
			List<SuppressChangeable> changeList,
			String newReason)
	{
		switch (changeList.size()) {
		case 0:
			break;
		case 1:
			changeManager.addChangeable(changeList.get(0));
			break;
		default:
			SuppressChangeableGroup changeableGroup = new SuppressChangeableGroup(changeList, newReason);
			changeManager.addChangeable(changeableGroup);
			break;
		}
	}

	static public final String PROPERTY_N_STD_DEV_FOR_OUTLIER = "numberOfStdDevForOutlier"; //$NON-NLS-1$

	// TODO make this configurable
	// null means use the Q1, Q3
	// lowerThreshold = Q1 - 1.5 * (Q3-Q1)
	// upperThreshold = Q3 + 1.5 * (Q3-Q1)

	// non-null means
	// lowerThreshold = mean - N * stddev
	// upperThreshold = mean + N * stddev
	private Integer numberOfStdDevForOutlier = null;

	public Integer getNumberOfStdDevForOutlier() {
		return numberOfStdDevForOutlier;
	}

	public void setNumberOfStdDevForOutlier(Integer n) {
		Integer old = numberOfStdDevForOutlier;
		numberOfStdDevForOutlier = n;
		support.firePropertyChange(PROPERTY_N_STD_DEV_FOR_OUTLIER, old, numberOfStdDevForOutlier);
	}

	private PropertyChangeSupport support = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		support.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		support.removePropertyChangeListener(propertyName, listener);
	}

	private final EventListenerList listenerList = new EventListenerList();

	private CalcContext calcContext;

	protected void firePlotActivationChanged(boolean activated, Collection<Plot> coll) {

	    List<Plot> plots = new ArrayList<>(coll);
		redoCalculatedTraitInstances(Either.right(plots));

		if (! plots.isEmpty()) {
			for (CurationDataChangeListener l : listenerList.getListeners(CurationDataChangeListener.class)) {
				l.plotActivationChanged(this, activated, plots);
			}
		}
		fireUnsavedChanges();
	}

	public void activatePlots(Collection<Plot> plots) {

		PlotActivationChangeable changeable = new PlotActivationChangeable(true);
		for (Plot p : plots) {
			changeable.addPlot(p);
			p.setWhenDeactivated(null);
		}

		addChangeable(changeable);

		firePlotActivationChanged(true, plots);
	}

	public void deactivatePlots(Collection<Plot> plots) {

		PlotActivationChangeable changeable = new PlotActivationChangeable(false);
		for (Plot p : plots) {
			changeable.addPlot(p);
			p.setWhenDeactivated(changeable.date);
		}

		addChangeable(changeable);

		firePlotActivationChanged(false, plots);
	}

	public String undoChanges() {
		PlotAndSampleChanger pasc = getPlotAndSampleChanger();
		String msg = changeManager.undo(pasc);
		handleUndoRedoResult(pasc);
		return msg;
	}

	public String redoChanges() {
		PlotAndSampleChanger pasc = getPlotAndSampleChanger();
		String msg = changeManager.redo(pasc);
		handleUndoRedoResult(pasc);
		return msg;
	}

	private void handleUndoRedoResult(PlotAndSampleChanger pasc) {

		Map<Boolean,List<Plot>> plotsByActivation =
				pasc.getPlotActivationChanges();

		if (! plotsByActivation.isEmpty()) {
			for (Boolean activated : plotsByActivation.keySet()) {
				List<Plot> plotList = plotsByActivation.get(activated);
				if (plotList != null && ! plotList.isEmpty()) {
					firePlotActivationChanged(activated, plotList);
				}
			}
		}

		List<CurationCellId> curationCellIds = pasc.getEditedCurationCellIds();
		if (! curationCellIds.isEmpty()) {
			fireEditedSamplesChanged(curationCellIds);
		}
	}

	public StatsData getStatsData(DeviceType deviceTypeForSamples) {

		StatsData result = new StatsData(numberOfStdDevForOutlier, trial);

		Transformer<TraitInstance, List<KdxSample>> sampleProvider = new Transformer<TraitInstance, List<KdxSample>>() {
			@Override
			public List<KdxSample> transform(TraitInstance ti) {
				return getSampleMeasurements(ti, deviceTypeForSamples);
			}
		};

		result.setTraitInstances(traitInstances, sampleProvider);

		return result;
	}

   static public boolean FULL_DETAILS = true;

    public static CurationData create(Trial trial, boolean fullDetails, KdxploreDatabase kdxploreDatabase) throws IOException, KdxploreConfigException {
        CurationData cd = new CurationData(trial);

        CurationDataCollector curationDataCollector = new CurationDataCollector(
                kdxploreDatabase,
                trial,
                fullDetails);

        cd.setPlotData(curationDataCollector);

        return cd;
    }

}
