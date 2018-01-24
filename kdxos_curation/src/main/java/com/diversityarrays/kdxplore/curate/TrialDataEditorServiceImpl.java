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

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.services.TrialDataEditorService;
import com.diversityarrays.util.Either;

import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;

public class TrialDataEditorServiceImpl implements TrialDataEditorService {

    @Override
    public void createUserInterface(BackgroundRunner backgroundRunner, CurationParams params, Consumer<Either<InitError,TrialDataEditorResult>> onComplete) {
        BackgroundTask<CurationData, Void> task = 
                new BackgroundTask<CurationData, Void>("Collecting Trial Data...", false) 
        {

            @Override
            public void onTaskComplete(CurationData cd) {

                PlotIdentSummary pis = cd.getTrial().getPlotIdentSummary();

                if (pis==null) {
                    onComplete.accept(Either.left(InitError.NO_PLOTIDENT_SUMMARY));
                    return;
                }

                if (! pis.hasXandY() && pis.plotIdentRange.isEmpty()) {
                    onComplete.accept(Either.left(InitError.NO_ID_X_OR_Y));
                    return;
                }

                if (PlotIdentOption.NO_X_Y_OR_PLOT_ID==cd.getTrial().getPlotIdentOption()) {
                    onComplete.accept(Either.left(InitError.NO_IDENT_SPECIFIED));
                    return;
                }

                try {
                    TrialDataEditor editor = TrialDataEditor.createTrialDataEditor(
                            params.offlineData, 
                            cd, params.windowOpener, 
                            params.messageLogger);

                    TrialDataEditorResult result = new TrialDataEditorResult();
                    result.curationData = cd;
                    result.frame = params.windowOpener.addDesktopObject(editor);
                    result.onFrameClosed = new Closure<Void>() {
                        @Override
                        public void execute(Void arg0) {
                            editor.pushDownAllGraphAndPlotFrames();
                        }
                    };

                    onComplete.accept(Either.right(result));
                }
                catch (IOException e) {
                    onComplete.accept(Either.left(InitError.error(e)));
                }
            }

            @Override
            public void onException(Throwable error) {
                onComplete.accept(Either.left(InitError.error(error)));
            }

            @Override
            public void onCancel(CancellationException ce) {
                onComplete.accept(Either.left(InitError.error(ce)));
            }

            @Override
            public CurationData generateResult(Closure<Void> arg0) throws Exception {
                CurationData curationData = CurationData.create(
                        params.trial,
                        CurationData.FULL_DETAILS,
                        params.offlineData.getKdxploreDatabase());

                return curationData;
            }
        };

        backgroundRunner.runBackgroundTask(task);
    }
}
