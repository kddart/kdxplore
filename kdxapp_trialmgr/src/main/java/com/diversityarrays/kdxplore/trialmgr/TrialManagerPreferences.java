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
package com.diversityarrays.kdxplore.trialmgr;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diversityarrays.kdxplore.prefs.ExportFileType;
import com.diversityarrays.kdxplore.prefs.KdxAppPreferences;
import com.diversityarrays.kdxplore.prefs.KdxPreference;
import com.diversityarrays.kdxplore.prefs.MessageId;
import com.diversityarrays.kdxplore.prefs.PreferenceCollection;
import com.diversityarrays.kdxplore.services.KdxApp;

@SuppressWarnings("nls")
public class TrialManagerPreferences extends KdxAppPreferences {

    static private final TrialManagerPreferences singleton = new TrialManagerPreferences();
    
    static public final Map<String, String> BRANCH_NAME_BY_PATH_COMPONENT;
    static {
        Map<String, String> map = new HashMap<>();
        
        map.put("export", Msg.GROUP_EXPORT()); //$NON-NLS-1$
        map.put("traits", "Traits"); //$NON-NLS-1$
        map.put("tips", "Tips");
//        map.put(TrialManagerPreferences.class.getName(), Msg.APPNAME_TRIAL_MANAGER()); //$NON-NLS-1$

        BRANCH_NAME_BY_PATH_COMPONENT = Collections.unmodifiableMap(map);
    }

    static public TrialManagerPreferences getInstance() {
        return singleton;
    }
    
    // kdx_trialmgr/curation
    private final KdxPreference<String> SAMPLE_MEASUREMENTS_SHEET_NAME = new KdxPreference<String>(
            TrialManagerPreferences.class, String.class,
            new MessageId("Excel Sheet Name for exporting SampleMeasurements"),
            "export/sampleMeasurementSheetName", "Sample Measurements", //$NON-NLS-1$//$NON-NLS-2$
            SHEET_NAME_VALIDATOR);

    static public final KdxPreference<ExportFileType> LAST_EXPORT_FILE_TYPE = new KdxPreference<ExportFileType>(
            TrialManagerPreferences.class, ExportFileType.class,
            new MessageId("Last export file type"),
            "export/lastExportFileType", ExportFileType.CSV); //$NON-NLS-1$

    static public final KdxPreference<Color> BAD_FOR_CALC = new KdxPreference<Color>(
            TrialManagerPreferences.class, Color.class,
            new MessageId("Colour when Trait is invalid for CALC"),
            "traits/invalidForCalcColour", Color.decode("#0099cc")); //$NON-NLS-1$ //$NON-NLS-2$

    static public final KdxPreference<Boolean> WARN_MULTI_TRIAL_PREVIEW= new KdxPreference<Boolean>(
    	TrialManagerPreferences.class, Boolean.class,
    	new MessageId("Warn Multi-Trial Layout Preview"),
    	"tips/warnMultiTrialPreview", Boolean.TRUE);
    
    static private final KdxPreference<Boolean> SHOW_EDIT_TRAIT_WARNING= new KdxPreference<Boolean>(
        	TrialManagerPreferences.class, Boolean.class,
        	new MessageId("Show Warning When Editing Traits"),
        	"traits/warnEditTraitPreview", Boolean.TRUE);
    
    static private final KdxPreference<Boolean> SHOW_IF_SUBPLOT_SCORED_SAMPLES = new KdxPreference<Boolean>(
            TrialManagerPreferences.class, Boolean.class,
            new MessageId("Show if Sub-Plot scored Samples exist"),
            "tips/showSubplotScoredSamples", Boolean.FALSE);
    
    private TrialManagerPreferences() {
    	super();
    }
    
    public boolean getShowIfSubplotScoredSamplesExist() {
        Boolean b = getPreferenceValue(SHOW_IF_SUBPLOT_SCORED_SAMPLES);
        return b==null ? false : b.booleanValue();
    }
    
    // kdx_curation (data export)
    public String getSampleMeasurementsSheetName() {
        return getPreferenceValue(SAMPLE_MEASUREMENTS_SHEET_NAME);
    }

    // kdxapp_trialmgr
    public Color getBadForCalcColor() {
        return getPreferenceValue(BAD_FOR_CALC);
    }

    // kdxapp_trialmgr
    public boolean getShowMultiTrialLayoutPreviewWarning() {
    	Boolean b = getPreferenceValue(WARN_MULTI_TRIAL_PREVIEW);
    	return b==null ? false : b.booleanValue();
    }

    public void setShowMultiTrialLayoutPreviewWarning(boolean b) {
        savePreferenceValue(WARN_MULTI_TRIAL_PREVIEW, b);
    }
    
    // kdxapp_trialmgr
    public boolean getShowEditTraitWarning() {
    	Boolean b = getPreferenceValue(SHOW_EDIT_TRAIT_WARNING);
    	return b==null ? SHOW_EDIT_TRAIT_WARNING.defaultValue : b.booleanValue();
    }
    public void setShowEditTraitWarning(boolean b) {
        savePreferenceValue(SHOW_EDIT_TRAIT_WARNING, b);
    }

    // kdx_curation
    public ExportFileType getLastExportFileType() {
        return getPreferenceValue(LAST_EXPORT_FILE_TYPE);
    }

    // kdx_curation
    public void saveLastExportFileType(ExportFileType eft) {
        savePreferenceValue(LAST_EXPORT_FILE_TYPE, eft);
    }

    public ExportFileType getLastExportFileTypeDefaultValue() {
        return LAST_EXPORT_FILE_TYPE.defaultValue;
    }
    
    @Override
    public PreferenceCollection getPreferenceCollection(KdxApp app, String appGroupName) {
    	PreferenceCollection pc = new PreferenceCollection(app, 
    			addGroupNameTo(BRANCH_NAME_BY_PATH_COMPONENT, app, appGroupName));
        List<KdxPreference<?>> list = KdxPreference.getKdxPreferences(TrialManagerPreferences.class);
		pc.addAll(list);
        return pc;
    }
}
