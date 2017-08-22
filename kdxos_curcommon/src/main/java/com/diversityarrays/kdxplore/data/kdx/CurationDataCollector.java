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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.javase.JavaseContext;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.csvio.ImportError;
import com.diversityarrays.kdsmart.db.csvio.PlotIdentCollector;
import com.diversityarrays.kdsmart.db.entities.MediaFileRecord;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.entities.TrialAttribute;
import com.diversityarrays.kdsmart.db.util.WhyMissing;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.jdbc.KdxploreConfigException;
import com.diversityarrays.kdxplore.data.util.DatabaseUtil;
import com.diversityarrays.util.Pair;

import android.content.Context;

public class CurationDataCollector {

    private static final String TAG = "CurationDataCollector"; //$NON-NLS-1$

    private final PlotPositionIdentifier plotNameIdentifier = new PlotPositionIdentifier() {
        @Override
        public String getDisplayName() {
            return trial.getNameForPlot();
        }

        @Override
        public Integer getPositionValue(Plot plot) {
            return plot.getUserPlotId();
        }

        @Override
        public String getRangeDescription(PlotIdentSummary pis) {
            return pis.plotIdentRange.isEmpty() ? "" : pis.plotIdentRange.getDescription(); //$NON-NLS-1$
        }
    };

    private final PlotPositionIdentifier xNameIdentifier = new PlotPositionIdentifier() {
        @Override
        public String getDisplayName() {
            return trial.getNameForColumn();
        }

        @Override
        public Integer getPositionValue(Plot plot) {
            return plot.getPlotColumn();
        }

        @Override
        public String getRangeDescription(PlotIdentSummary pis) {
            return pis.xColumnRange.isEmpty() ? "" : pis.xColumnRange.getDescription(); //$NON-NLS-1$
        }
    };

    private final PlotPositionIdentifier yNameIdentifier = new PlotPositionIdentifier() {
        @Override
        public String getDisplayName() {
            return trial.getNameForRow();
        }

        @Override
        public Integer getPositionValue(Plot plot) {
            return plot.getPlotRow();
        }

        @Override
        public String getRangeDescription(PlotIdentSummary pis) {
            return pis.yRowRange.isEmpty() ? "" : pis.yRowRange.getDescription(); //$NON-NLS-1$
        }
    };

    public final List<PlotPositionIdentifier> ppIdentifiers = new ArrayList<>();

    private Trial trial;
    
    public final List<Pair<SampleGroup, DeviceIdentifier>> sampleGroupData = new ArrayList<>();

    public final List<PlotAttribute> plotAttributes;
    public final List<TraitInstance> traitInstances;
    
    
    public final List<DeviceIdentifier> deviceIdentifiers;

    public final List<TrialAttribute> trialAttributes;

    public final List<SampleGroup> sampleGroups = new ArrayList<>();
    public SampleGroup databaseSampleGroup = null;
    public SampleGroup editedSampleGroup = null;

    public final List<Plot> plots;
    
    private final Map<Integer,DeviceIdentifier> deviceIdentifierById = new HashMap<>();

    public final Map<String,Integer> countByTagLabel;

//    public final Map<Integer,Tag> tagById = new HashMap<>();

//    public final Map<Integer,Map<Integer,TagPlotUse[]>> plotTagsBySampleGroupId = new HashMap<>();
    
//    public final Map<Integer,MediaFileRecord[]> mfrByPlotId = new HashMap<>();

    public CurationDataCollector(KdxploreDatabase kdxdb,
            Trial trial, boolean fullDetails) 
    throws IOException, KdxploreConfigException
    {
        this.trial = trial;

        int trialId = trial.getTrialId();
        this.deviceIdentifiers = kdxdb.getDeviceIdentifiers();
        
        deviceIdentifierById.clear();
        for (DeviceIdentifier d : deviceIdentifiers) {
            deviceIdentifierById.put(d.getDeviceIdentifierId(), d);
        }
        
//        ProgressUpdater progressUpdater;
//        kdxdb.getKDXploreKSmartDatabase().collectPlotsFor(trial, 
//                SampleGroupChoice.ANY_SAMPLE_GROUP, 
//                KDSmartDatabase.WithPlotAttributesOption.WITH_PLOT_ATTRIBUTES,
//                KDSmartDatabase.WithTraitOption.ALL_WITH_TRAITS,
//                progressUpdater);
//        
//        this.plots = kdxdb.getPlots(trial, SampleGroupChoice.ANY_SAMPLE_GROUP,
//                KDSmartDatabase.WithPlotAttributesOption.WITH_PLOT_ATTRIBUTES);

        Closure<Pair<WhyMissing, MediaFileRecord>> reportMissing = new Closure<Pair<WhyMissing,MediaFileRecord>>() {
            @Override
            public void execute(Pair<WhyMissing, MediaFileRecord> arg0) {
                // TODO Auto-generated method stub
                
            }
        };
        Map<Integer, Plot> plotById = DatabaseUtil.collectPlotsIncludingMediaFiles(
                kdxdb.getKDXploreKSmartDatabase(),
                trialId, 
                "CurationDataCollector", 
                SampleGroupChoice.ANY_SAMPLE_GROUP,
                reportMissing);
        this.plots = new ArrayList<>(plotById.values());

        this.countByTagLabel = Collections.unmodifiableMap(
                plots.stream()
                .flatMap(plot -> plot.getTagsBySampleGroup().entrySet().stream())
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.groupingBy(Tag::getLabel, 
                            Collectors.reducing(0, e -> 1, Integer::sum))));
        
        collectPlotPositionIdentifiers(plots);

        trialAttributes = kdxdb.getTrialAttributes(trialId);
        
        KDSmartDatabase kdsdb = kdxdb.getKDXploreKSmartDatabase();
        
        plotAttributes = kdsdb.getAllPlotAttributesForTrial(trialId);
        traitInstances = kdsdb.getTraitInstances(trialId, KDSmartDatabase.WithTraitOption.ALL_WITH_TRAITS);
        
//        tagById.clear();
//        for (Tag tag : kdsdb.getAllTags()) {
//            tagById.put(tag.getTagId(), tag);
//        }
        
//        collectTagPlotUsage(trialId, kdsdb);
        
//        collectMediaFileRecords(trialId, kdsdb);
        
        loadSampleGroups(kdxdb, trialId, fullDetails);
    }

//    private void collectTagPlotUsage(int trialId, KDSmartDatabase kdsdb) throws IOException {
//        Map<Integer,Map<Integer,List<TagPlotUse>>> tmp = new HashMap<>();
//        
//        Predicate<TagPlotUse> tpuVisitor = new Predicate<TagPlotUse>() {
//            @Override
//            public boolean evaluate(TagPlotUse tpu) {
//                int sampleGroupId = tpu.getSampleGroupId();
//                Map<Integer, List<TagPlotUse>> map = tmp.get(sampleGroupId);
//                if (map == null) {
//                    map = new HashMap<>();
//                    tmp.put(sampleGroupId, map);
//                }
//                List<TagPlotUse> list = map.get(tpu.getPlotId());
//                if (list == null) {
//                    list = new ArrayList<>(1);
//                    map.put(tpu.getPlotId(), list);
//                }
//                list.add(tpu);
//                return true;
//            }
//        };
//        kdsdb.visitTagsForTrial(trialId, tpuVisitor);
//        // Now convert to a more efficient form
//        plotTagsBySampleGroupId.clear();
//        for (Integer sampleGroupId : tmp.keySet()) {
//            Map<Integer, List<TagPlotUse>> mapOfList = tmp.get(sampleGroupId);
//            Map<Integer,TagPlotUse[]> mapOfArray = new HashMap<>();
//            for (Integer plotId : mapOfList.keySet()) {
//                List<TagPlotUse> list = mapOfList.get(plotId);
//                mapOfArray.put(plotId, list.toArray(new TagPlotUse[list.size()]));
//            }
//            plotTagsBySampleGroupId.put(sampleGroupId, mapOfArray);
//        }
//    }

//    private void collectMediaFileRecords(int trialId, KDSmartDatabase kdsdb) throws IOException {
//        Map<Integer,List<MediaFileRecord>> tmp = new HashMap<>();
//        Predicate<MediaFileRecord> mfrVisitor = new Predicate<MediaFileRecord>() {
//            @Override
//            public boolean evaluate(MediaFileRecord mfr) {
//                List<MediaFileRecord> list = tmp.get(mfr.plotId);
//                if (list == null) {
//                    list = new ArrayList<>(1);
//                    tmp.put(mfr.plotId, list);
//                }
//                list.add(mfr);
//                return true;
//            }
//        };
//        kdsdb.visitTrialMediaFileRecords(trialId,mfrVisitor);
//        // Convert to more efficient form
//        mfrByPlotId.clear();
//        for (Integer plotId : tmp.keySet()) {
//            List<MediaFileRecord> list = tmp.get(plotId);
//            mfrByPlotId.put(plotId, list.toArray(new MediaFileRecord[list.size()]));
//        }
//    }
    
    private void loadSampleGroups(KdxploreDatabase kdxdb, int trialId, boolean fullDetails)
    throws IOException, KdxploreConfigException {
        int databaseDeviceIdentifierId = KdxploreDatabase.DEVICE_ID_NOT_FOUND;
        int editedDeviceIdentifierId = KdxploreDatabase.DEVICE_ID_NOT_FOUND;
        for (DeviceIdentifier devid : kdxdb.getDeviceIdentifiers()) {
            switch (devid.getDeviceType()) {
            case DATABASE:
                databaseDeviceIdentifierId = devid.getDeviceIdentifierId();
                break;
            case EDITED:
                editedDeviceIdentifierId = devid.getDeviceIdentifierId();
                break;
            case KDSMART:
                break;
            case FOR_SCORING:
                break;
            default:
                break;
            }
        }
        if (databaseDeviceIdentifierId == KdxploreDatabase.DEVICE_ID_NOT_FOUND) {
            throw new KdxploreConfigException("No Device Identifier for DATABASE");
        }
        
        if (editedDeviceIdentifierId == KdxploreDatabase.DEVICE_ID_NOT_FOUND) {
            throw new KdxploreConfigException("No Device Identifier for EDITED");
        }
        
        databaseSampleGroup = null;
        editedSampleGroup = null;
        sampleGroups.clear();
        
        final int f_databaseDeviceIdentifierId = databaseDeviceIdentifierId;
        final int f_editedDeviceIdentifierId = editedDeviceIdentifierId;

        Transformer<SampleGroup, KdxploreConfigException> sampleGroupVisitor = new Transformer<SampleGroup, KdxploreConfigException>() {
            @Override
            public KdxploreConfigException transform(SampleGroup sg) {
                addSampleGroup(sg, 
                        fullDetails,
                        kdxdb,
                        f_databaseDeviceIdentifierId, 
                        f_editedDeviceIdentifierId);

                return null;
            }
        };

        KdxploreConfigException error = kdxdb.visitSampleGroups(trialId, sampleGroupVisitor);
        if (error != null) {
            throw error;
        }
        
        Date now = new Date();
        if (databaseSampleGroup == null) {
            // We need one.
            SampleGroup sampleGroup = new SampleGroup();
            sampleGroup.setTrialId(trialId);
            sampleGroup.setDeviceIdentifierId(databaseDeviceIdentifierId);
            sampleGroup.setDateLoaded(now);
            kdxdb.saveSampleGroup(sampleGroup);
            
            addSampleGroup(sampleGroup, 
                    fullDetails, kdxdb, 
                    f_databaseDeviceIdentifierId, f_editedDeviceIdentifierId);
        }

        if (editedSampleGroup == null) {
            // We need one
            SampleGroup sampleGroup = new SampleGroup();
            sampleGroup.setTrialId(trialId);
            sampleGroup.setDeviceIdentifierId(editedDeviceIdentifierId);
            sampleGroup.setDateLoaded(now);
            kdxdb.saveSampleGroup(sampleGroup);
            
            addSampleGroup(sampleGroup, 
                    fullDetails, kdxdb, 
                    f_databaseDeviceIdentifierId, f_editedDeviceIdentifierId);
        }
    }
    

    private KdxploreConfigException addSampleGroup(SampleGroup sg,
            boolean fullDetails, KdxploreDatabase kdxdb,
            int f_databaseDeviceIdentifierId,
            int f_editedDeviceIdentifierId) 
    {
        int did = sg.getDeviceIdentifierId();
        if (did == f_editedDeviceIdentifierId) {
            if (editedSampleGroup != null) {
                return new KdxploreConfigException("Duplicate EDITED Sample Group: "
                        + editedSampleGroup.getSampleGroupId() + " <> " + sg.getSampleGroupId());
            }
            editedSampleGroup = sg;
        }
        else if (did == f_databaseDeviceIdentifierId) {
            if (databaseSampleGroup != null) {
                return new KdxploreConfigException(
                        "Duplicate DATABASE Sample Group: " 
                        + databaseSampleGroup.getSampleGroupId() + " <> " + sg.getSampleGroupId());
            }
            databaseSampleGroup = sg;
        }
        
        DeviceIdentifier deviceIdentifier = deviceIdentifierById.get(sg.getDeviceIdentifierId());
        if (deviceIdentifier == null) {
            Shared.Log.e(TAG, "No DeviceIdentifier for id=" + sg.getDeviceIdentifierId());
            // TODO handle unknown devices
        }
        else if (DeviceType.FOR_SCORING != deviceIdentifier.getDeviceType()) {
            
            if (fullDetails) {
                try {
                    List<KdxSample> samples = kdxdb.getSampleGroupSamples(sg);
                    sg.setSamples(samples);
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            sampleGroupData.add(new Pair<>(sg, deviceIdentifier));
            sampleGroups.add(sg);
        }

        
        
        return null;
    }

    private void collectPlotPositionIdentifiers(List<Plot> plots) throws IOException {
        Context context = JavaseContext.getInstance();

        ppIdentifiers.clear();

        PlotIdentCollector pic = new PlotIdentCollector(context);
        switch (trial.getPlotIdentOption()) {
        case NO_X_Y_OR_PLOT_ID:
            break;
        case PLOT_ID:
            ppIdentifiers.add(plotNameIdentifier);
            pic.setUsingPlotId();
            break;
        case PLOT_ID_THEN_X:
            ppIdentifiers.add(plotNameIdentifier);
            ppIdentifiers.add(xNameIdentifier);

            pic.setUsingPlotId();
            pic.setUsingPlotX();
            break;
        case PLOT_ID_THEN_XY:
            ppIdentifiers.add(plotNameIdentifier);
            ppIdentifiers.add(xNameIdentifier);
            ppIdentifiers.add(yNameIdentifier);

            pic.setUsingPlotId();
            pic.setUsingPlotX();
            pic.setUsingPlotY();
            break;
        case PLOT_ID_THEN_Y:
            ppIdentifiers.add(plotNameIdentifier);
            ppIdentifiers.add(yNameIdentifier);

            pic.setUsingPlotId();
            pic.setUsingPlotY();
            break;
        case PLOT_ID_THEN_YX:
            ppIdentifiers.add(plotNameIdentifier);
            ppIdentifiers.add(yNameIdentifier);
            ppIdentifiers.add(xNameIdentifier);

            pic.setUsingPlotId();
            pic.setUsingPlotY();
            pic.setUsingPlotX();
            break;
        case X_THEN_Y:
            ppIdentifiers.add(xNameIdentifier);
            ppIdentifiers.add(yNameIdentifier);

            pic.setUsingPlotX();
            pic.setUsingPlotY();
            break;
        case Y_THEN_X:
            ppIdentifiers.add(yNameIdentifier);
            ppIdentifiers.add(xNameIdentifier);

            pic.setUsingPlotY();
            pic.setUsingPlotX();
            break;
        default:
            break;              
        }
        
        int lineNumber = 0;
        for (Plot plot : plots) {
            ImportError err = pic.collectPlotIdentifiers(++lineNumber, plot);
            if (err != null) {
                throw new IOException(err.message);
            }
        }

        trial.setPlotIdentSummary(pic.getPlotIdentSummary());
    }  
}
