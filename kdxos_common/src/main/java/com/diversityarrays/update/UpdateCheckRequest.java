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
package com.diversityarrays.update;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.KDXploreUpdate;
import com.diversityarrays.util.Msg;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;
import com.google.gson.Gson;

import net.pearcan.util.StringUtil;

public class UpdateCheckRequest {

    private static final String TAG = "UpdateCheckRequest"; //$NON-NLS-1$
    static private boolean VERBOSE = Boolean.getBoolean("VERBOSE_UPDATE_CHECK_REQUEST"); //$NON-NLS-1$
    static public enum CheckStatus {
        NOT_CHECKED,
        USER_CHECK,
        ERROR,
        UNKNOWN_HOST,
        UP_TO_DATE,
        UPDATE_AVAILABLE
    }

	public final boolean userCheck;
	public final int versionCode;
	public final String versionName;
	public final Window owner;
    public final String updateBaseUrl;

    public final String updateUrlUsed;
    public final String updateSite;

    public CheckStatus checkStatus = CheckStatus.NOT_CHECKED;
    public KDXploreUpdate kdxploreUpdate;

	public UpdateCheckRequest(Window w, int vcode, String vname,
	        boolean userCheck,
	        String url)
	{
		this.owner = w;
		this.versionCode = vcode;
		this.versionName = vname;
		this.userCheck = userCheck;
		this.updateBaseUrl = url;

        StringBuilder sb = new StringBuilder(updateBaseUrl);
        sb.append(versionCode);
        sb.append(".json"); //$NON-NLS-1$

        // updateUrlUsed = "NONEXISTENT"; // Uncomment to check error
        updateUrlUsed = sb.toString();

        String site = null;
        int spos = updateUrlUsed.indexOf("://"); //$NON-NLS-1$
        if (spos > 0) {
            int epos = updateUrlUsed.indexOf('/', spos+1);
            if (epos > 0) {
                site = updateUrlUsed.substring(0, epos);
            }
        }
        updateSite = Check.isEmpty(site) ? updateUrlUsed : site;

        if (VERBOSE) {
            Shared.Log.d(TAG, "using url=" + updateUrlUsed); //$NON-NLS-1$
        }
	}

	public boolean isUpdateAvailable() {
	    if (kdxploreUpdate==null) {
	        return false;
	    }
	    return kdxploreUpdate.versionCode > versionCode;
	}

	public boolean isNotError() {
	    return kdxploreUpdate!=null && ! kdxploreUpdate.isError();
	}

	public SwingWorker<String, Void> createWorker(
	        PrintStream ps, Consumer<String> onReceiveComplete)
	{
        final ProgressMonitor progressMonitor;
        if (owner == null) {
            progressMonitor = null;
        }
        else {
            progressMonitor = new ProgressMonitor(
                owner, Msg.PROGRESS_CHECKING(), null, 0, 0);
        }

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {

                // We're starting, lets make sure we start from a known state.
                kdxploreUpdate = null;

                // Thread.sleep(3000); // Uncomment to check delay

                BufferedReader reader = null;
                StringBuffer buffer = new StringBuffer();

                try {
                    URL url = new URL(updateUrlUsed);

                    if (VERBOSE) {
                        Shared.Log.d(TAG, "Reading from: " + url); //$NON-NLS-1$
                    }

                    reader = new BufferedReader(new InputStreamReader(
                            url.openStream()));
                    int read;
                    char[] chars = new char[1024];
                    while ((read = reader.read(chars)) != -1) {
                        if (progressMonitor!=null && progressMonitor.isCanceled()) {
                            cancel(true);
                            return null;
                        }
                        buffer.append(chars, 0, read);
                    }
//                } catch (IOException e) {
//                    Shared.Log.w(TAG, "Error reading from " + updateUrlUsed, e); //$NON-NLS-1$
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }

                return buffer.toString();
            }

            @Override
            protected void done() {
                try {
                    String json = get();
                    Gson gson = new Gson();
                    kdxploreUpdate = gson.fromJson(json, KDXploreUpdate.class);
                    if (VERBOSE) {
                        Shared.Log.d(TAG, "received:\n========\n" + json + "\n----------\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    doOnReceiveComplete(onReceiveComplete);
                } catch (CancellationException ignore) {
                    if (VERBOSE) {
                        Shared.Log.d(TAG, "Cancelled"); //$NON-NLS-1$
                    }
                    doOnReceiveComplete(onReceiveComplete);
                } catch (InterruptedException ignore) {
                    if (VERBOSE) {
                        Shared.Log.d(TAG, "Interrupted"); //$NON-NLS-1$
                    }
                    doOnReceiveComplete(onReceiveComplete);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof UnknownHostException) {
                        ps.println(Msg.ERRMSG_UNABLE_TO_CONTACT_UPDATE_SITE(updateSite));
                    }
                    else if (cause instanceof FileNotFoundException) {
                        String msg;
                        FileNotFoundException fnf = (FileNotFoundException) cause;
                        if (updateUrlUsed.equals(fnf.getMessage())) {
                            msg = "Maybe someone forgot to create the file!"; //$NON-NLS-1$
                            if (VERBOSE) {
                                Shared.Log.d(TAG, msg + ":" + fnf.getMessage()); //$NON-NLS-1$
                            }
                            else {
                                // Well maybe someone forgot to create the file on
                                // the server!
                                System.err.println(msg);
                                System.err.println(fnf.getMessage());
                            }

                            String errmsg = Msg.ERRMSG_PROBLEMS_CONTACTING_UPDATE_SERVER_1();
                            kdxploreUpdate = new KDXploreUpdate(errmsg);

                            doOnReceiveComplete(onReceiveComplete);

                            // We're done if the URL we tried for matched the message
                            return;
                        }
                    }
                    else {
                        cause.printStackTrace(ps);
                        if (VERBOSE) {
                            Shared.Log.d(TAG, "Errored", cause); //$NON-NLS-1$
                        }
                    }

                    String msg = Msg.HTML_PROBLEMS_CONTACTING_UPDATE_2(
                            StringUtil.htmlEscape(updateUrlUsed),
                            cause.getClass().getSimpleName(),
                            StringUtil.htmlEscape(cause.getMessage()));
                    kdxploreUpdate = new KDXploreUpdate(msg);
                    kdxploreUpdate.unknownHost = (cause instanceof UnknownHostException);

                    doOnReceiveComplete(onReceiveComplete);
                }
            }


        };

        return worker;
    }

	private void doOnReceiveComplete(Consumer<String> onReceiveComplete) {
        Pair<CheckStatus,String> pair = postReceiveProcess();
        checkStatus = pair.first;
        if (VERBOSE) {
            Shared.Log.d(TAG, "CheckStatus=" + checkStatus + " for " + updateUrlUsed);
        }
        onReceiveComplete.accept(pair.second);
    }

    private Pair<CheckStatus,String> postReceiveProcess() {
       if (kdxploreUpdate == null) {
           String msg;
            if (RunMode.getRunMode().isDeveloper()) {
                msg = "<html>Unable to read update information:<br>" + updateUrlUsed; //$NON-NLS-1$
            }
            else {
                msg = Msg.ERRMSG_UNABLE_TO_READ_UPDATE_INFO();
            }
            return new Pair<>(CheckStatus.USER_CHECK, msg);
        }

        if (kdxploreUpdate.isError()) {
            if (! userCheck && kdxploreUpdate.unknownHost) {
                return new Pair<>(CheckStatus.UNKNOWN_HOST, null);
            }
            return new Pair<>(CheckStatus.ERROR, kdxploreUpdate.errorMessage);
        }

        // User is checking or we have an update.

        if (!userCheck) {
            // Auto check ...
            if (kdxploreUpdate.versionCode <= versionCode) {
                // We have the latest
                return new Pair<>(CheckStatus.UP_TO_DATE, Msg.YOUR_VERSION_IS_THE_LATEST());
            }
        }

        return new Pair<>(CheckStatus.UPDATE_AVAILABLE, null);
	}
}