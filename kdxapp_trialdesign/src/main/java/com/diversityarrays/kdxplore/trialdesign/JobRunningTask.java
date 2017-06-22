/**
 *
 */
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
package com.diversityarrays.kdxplore.trialdesign;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdxplore.trialdesign.algorithms.Algorithms;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;

class JobRunningTask extends BackgroundTask<Either<String, AlgorithmRunResult>, Void> {

	private String[] command;

    private final List<TrialEntry> userTrialEntries;

    private File algorithmFolder;

    private final BiConsumer<AlgorithmRunResult, File> onSuccess;

    private final BackgroundRunner backgroundRunner;

    private final File kdxploreOutputFile;

    private final String algorithmName;

    private final Consumer<Either<Throwable, String>> onError;

    JobRunningTask(
            String algorithmName,
    		List<TrialEntry> userTrialEntries,
    		String[] command,
    		File algorithmFolder,
    		BiConsumer<AlgorithmRunResult, File> onSuccess,
    		Consumer<Either<Throwable, String>> onError,
    		BackgroundRunner runner)
    {
        super("Running " + algorithmName + UnicodeChars.ELLIPSIS, true);

        this.algorithmName = algorithmName;
        this.userTrialEntries = userTrialEntries;
        this.command = command;
        this.algorithmFolder = algorithmFolder;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.backgroundRunner = runner;

        this.kdxploreOutputFile = new File(new File(algorithmFolder, "4KDXplore"), "plots_expdesign.csv");
    }

    @Override
    public Either<String, AlgorithmRunResult> generateResult(Closure<Void> arg0) throws Exception {

    	AlgorithmRunResult result = new AlgorithmRunResult(algorithmName, algorithmFolder);
        ProcessBuilder pb = new ProcessBuilder(command);

        File tempAlgorithmOutputFile = new File(algorithmFolder, "stdout.txt");
        File tempAlgorithmErrorFile = new File(algorithmFolder, "stderr.txt");

        //pb.redirectErrorStream(true);
        tempAlgorithmErrorFile.createNewFile();
        tempAlgorithmOutputFile.createNewFile();

        pb.redirectOutput(tempAlgorithmOutputFile);
        pb.redirectError(tempAlgorithmErrorFile);

        Process process = pb.start();

        while (! process.waitFor(1000, TimeUnit.MILLISECONDS)) {
            if (backgroundRunner.isCancelRequested()) {
                process.destroy();
                throw new CancellationException();
            }
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errtxt = Algorithms.readContent("Error Output: (code=" + exitCode + ")",
                    new FileInputStream(tempAlgorithmErrorFile));
            return Either.left(errtxt);
        }

        if (! kdxploreOutputFile.exists()) {
            return Either.left("Missing output file: " + kdxploreOutputFile.getPath());
        }

        result.addTrialEntries(kdxploreOutputFile, userTrialEntries);

        return Either.right(result);
    }

    @Override
    public void onCancel(CancellationException ce) {
        onError.accept(Either.left(ce));
    }

    @Override
    public void onException(Throwable t) {
        onError.accept(Either.left(t));
    }

    @Override
    public void onTaskComplete(Either<String, AlgorithmRunResult> either) {
    	if (either.isLeft()) {
    	    onError.accept(Either.right(either.left()));
    	}
    	else {
    		onSuccess.accept(either.right(), kdxploreOutputFile);
    	}
    }
}
