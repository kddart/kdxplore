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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;

import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithPlotAttributesOption;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithTraitOption;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Specimen;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.util.SampleOrder;
import com.diversityarrays.kdsmart.db.util.TrialExportHelper;
import com.diversityarrays.kdsmart.db.util.WhyMissing;
import com.diversityarrays.kdsmart.kdxs.TrialInfo;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

class MyTrialExportHelper implements TrialExportHelper {

	/**
	 *
	 */
	private final SampleGroupExportDialog sampleGroupExportDialog;
	private final KdxploreDatabase kdxploreDatabase;
	private final KDSmartDatabase kdsmartDatabase;

	private final int targetDatabaseVersion;
	private final int sampleGroupId;
	private final boolean wantMediaFiles;
	private final int kdsmartVersionCode;
	private final Set<Integer> traitIds;

	MyTrialExportHelper(SampleGroupExportDialog sampleGroupExportDialog, KdxploreDatabase kdxdb,
			int targetDatabaseVersion,
			SampleGroup sampleGroup,
			boolean wantMediaFiles)
	{
		this.sampleGroupExportDialog = sampleGroupExportDialog;
		kdxploreDatabase = kdxdb;
		kdsmartDatabase = kdxdb.getKDXploreKSmartDatabase();

		this.kdsmartVersionCode = kdxploreDatabase.getKDXploreKSmartDatabase().getKdsmartVersionCode();
		this.targetDatabaseVersion = targetDatabaseVersion;
		this.sampleGroupId = sampleGroup.getSampleGroupId();
		this.wantMediaFiles = wantMediaFiles;

		traitIds = sampleGroup.getSamples()
		    .stream()
		    .map(s -> s.getTraitId())
		    .collect(Collectors.toSet());
	}

	@Override
	public void visitTraitInstancesForTrial(int trialId, boolean withTraits,
	        Predicate<TraitInstance> traitInstanceVisitor)
	throws IOException {
		WithTraitOption withTraitOption = withTraits
				? WithTraitOption.ALL_WITH_TRAITS
				: WithTraitOption.ALL_WITHOUT_TRAITS;
		Predicate<TraitInstance> visitor = new Predicate<TraitInstance>() {
            @Override
            public boolean evaluate(TraitInstance ti) {
                Boolean result = Boolean.TRUE;
                if (traitIds.contains(ti.getTraitId())) {
                    result = traitInstanceVisitor.evaluate(ti);
                }
                return result;
            }
        };
        kdsmartDatabase.visitTraitInstancesForTrial(trialId,
				withTraitOption,
				visitor);
	}

	@Override
	public void visitSpecimensForTrial(int trialId, Predicate<Specimen> visitor)
	throws IOException {
		kdsmartDatabase.visitSpecimensForTrial(trialId, visitor);
	}

	@Override
	public void visitSamplesForTrial(
	        SampleGroupChoice sampleGroupChoice,
	        int trialId,
			SampleOrder sampleOrder,
			TrialItemVisitor<Sample> sampleTrialItemVisitor)
	throws IOException
	{
        KdxSampleVisitor kdxSampleVisitor = new KdxSampleVisitor(traitIds, sampleTrialItemVisitor);

        kdsmartDatabase.visitSamplesForTrial(
                sampleGroupChoice,
                trialId,
                sampleOrder,
                kdxSampleVisitor);
	}

	@Override
	public Either<? extends Throwable, Boolean> visitPlotsForTrial(int trialId,
			TrialItemVisitor<Plot> visitor)
	{
		kdsmartDatabase.visitPlotsForTrial(trialId,
		        SampleGroupChoice.create(this.sampleGroupExportDialog.sampleGroup.getSampleGroupId()),
				WithPlotAttributesOption.WITH_PLOT_ATTRIBUTES,
				visitor);
		return Either.right(Boolean.TRUE);
	}

	@Override
	public Either<? extends Throwable, Boolean> visitPlotAttributeValuesForTrial(
			int trialId, TrialItemVisitor<PlotAttributeValue> trialPlotVisitor)
	{
		kdsmartDatabase.visitPlotAttributeValuesForTrial(trialId, trialPlotVisitor);
		return Either.right(Boolean.TRUE);
	}

	@Override
	public void visitComments(Closure<Tag> commentVisitor) throws IOException {
		for (Tag tag : kdsmartDatabase.getAllTags()) {
			commentVisitor.execute(tag);
		}
	}

	@Override
	public TrialInfo getTrialInfo(int trialId) throws IOException {
		return kdsmartDatabase.getTrialInfo(trialId, true /*wantLatestSampleDate*/);
	}

	@Override
	public Iterable<Trait> getTraits() throws IOException {
		return kdxploreDatabase.getAllTraits();
	}

	@Override
	public Map<Integer, List<File>> getMediaFilesBySpecimenNumber(
	        SampleGroupChoice sampleGroupChoice,
	        int trialId,
			Plot plot, Closure<Pair<WhyMissing, String>> missingFileConsumer)
	throws IOException {
		if  (wantMediaFiles) {
			return kdsmartDatabase.getMediaFilesBySpecimenNumber(sampleGroupChoice, trialId, plot, missingFileConsumer);
		}
		else {
			return Collections.emptyMap();
		}
	}

	@Override
	public int getDatabaseVersion() {
		return targetDatabaseVersion;
	}
	@Override
	public int getKdsmartVersionCode() {
		return kdsmartVersionCode;
	}
}