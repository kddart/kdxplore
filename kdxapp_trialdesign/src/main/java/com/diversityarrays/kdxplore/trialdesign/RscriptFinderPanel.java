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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdxplore.trialdesign.algorithms.Algorithms;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.Util;

@SuppressWarnings("nls")
public class RscriptFinderPanel extends JPanel {

    public enum RscriptOption {
        LINUX("/usr/bin/Rscript", true),
        MACOS("/usr/local/bin/Rscript", true),
        WINDOWS("C:\\Program Files\\R\\R-#.#.#\\bin\\RScript.exe", false),
        ;
        public final String location;
        public final boolean unix;
        RscriptOption(String loc, boolean unix) {
            location = loc;
            this.unix = unix;
        }
        @Override
        public String toString() {
            return "use: " + location;
        }
    }



    private PromptTextField scriptPathField = new PromptTextField("Enter Rscript Path (Double Click for Help)");

    private MouseListener clickAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {

                List<RscriptOption> optionList = new ArrayList<>();
                if (Util.isLinux()) {
                    Collections.addAll(optionList, RscriptOption.LINUX, RscriptOption.MACOS);
                }
                else if (Util.isMacOS()) {
                    Collections.addAll(optionList, RscriptOption.MACOS, RscriptOption.LINUX);
                }
                else if (Util.isMicrosoftWindows()) {
                    Collections.addAll(optionList, RscriptOption.WINDOWS);
                }

                StringBuilder sb = new StringBuilder("<HTML>");
                sb.append("This field identifies the location of the Rscript executable on your system");
                if (optionList.isEmpty()) {

                }
                else {
                    sb.append(" It may be at ");
                    sb.append(optionList.stream().map(o -> o.location).collect(Collectors.joining(" or<BR>")));
                }

                if (Util.isLinux() || Util.isMacOS()) {
                    sb.append("You could use one of the commands:<UL>");
                    sb.append("<LI>locate Rscript; or</LI>");
                    sb.append("<LI>which Rscript</LI>");
                    sb.append("</UL>in a command line window to locate the <code>Rscript</code> executable");
                }

                if (optionList.isEmpty()) {
                    MsgBox.info(RscriptFinderPanel.this, sb.toString(), "Rscript Path Help");
                }
                else {
                    RscriptOption[] options = optionList.toArray(new RscriptOption[optionList.size()]);
                    Object result = JOptionPane.showInputDialog(RscriptFinderPanel.this,
                            sb.toString(),
                            "Rscript Path Help",
                            JOptionPane.QUESTION_MESSAGE, null, options, null);
                    if (result instanceof RscriptOption) {
                        RscriptOption option = (RscriptOption) result;
                        scriptPathField.setText(option.location);
                    }
                }
            }
        }
    };

    private final Action checkScriptPath = new AbstractAction("Test") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doCheckScriptPath();
        }
    };

    private final BackgroundRunner backgroundRunner;

    private final Consumer<Either<Throwable, String>> onScriptPathChecked;

    protected String checkOutput;

    private String errorOutput;

    private String versionNumber;

    public RscriptFinderPanel(
            BackgroundRunner br,
            Component component,
            String initialScriptPath,
            Consumer<Either<Throwable, String>> onScriptPathChecked)
    {
        super(new BorderLayout());

        backgroundRunner = br;
        this.onScriptPathChecked = onScriptPathChecked;

        scriptPathField.setText(initialScriptPath);
        scriptPathField.addMouseListener(clickAdapter);

        Box scriptPathBox = Box.createHorizontalBox();

        Consumer<Void> updateFindScriptButton = new Consumer<Void>() {
            @Override
            public void accept(Void t) {
                checkScriptPath.setEnabled(! scriptPathField.getText().trim().isEmpty());
            }
        };

        scriptPathBox.add(scriptPathField);
        scriptPathBox.add(new JButton(checkScriptPath));
        scriptPathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFindScriptButton.accept(null);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFindScriptButton.accept(null);
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFindScriptButton.accept(null);
            }
        });
        updateFindScriptButton.accept(null);

        add(scriptPathBox, BorderLayout.NORTH);
        if (component != null) {
            add(component, BorderLayout.CENTER);
        }
    }

    public String getOutput() {
        return checkOutput;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    private void doCheckScriptPath() {

        String scriptPath = scriptPathField.getText().trim();

        BackgroundTask<Either<String,String>, Void> task = new BackgroundTask<Either<String,String>, Void>("Checking...", true) {
            @Override
            public Either<String,String> generateResult(Closure<Void> arg0) throws Exception {

                ProcessBuilder findRScript  = new ProcessBuilder(scriptPath, "--version");

                Process p = findRScript.start();

                while (! p.waitFor(1000, TimeUnit.MILLISECONDS)) {
                    if (backgroundRunner.isCancelRequested()) {
                        p.destroy();
                        throw new CancellationException();
                    }
                }

                if (0 == p.exitValue()) {
                    String output = Algorithms.readContent(null, p.getInputStream());
                    versionNumber = Algorithms.readContent(null, p.getErrorStream());
                    return Either.right(output);
                }

                errorOutput = Algorithms.readContent("Error Output:", p.getErrorStream());
                if (errorOutput.isEmpty()) {
                    errorOutput = "No error output available";
                    return Either.left(errorOutput);
                }
                return Either.left(errorOutput);
            }

            @Override
            public void onException(Throwable t) {
                onScriptPathChecked.accept(Either.left(t));
            }

            @Override
            public void onCancel(CancellationException ce) {
                onScriptPathChecked.accept(Either.left(ce));
            }

            @Override
            public void onTaskComplete(Either<String,String> either) {
                if (either.isLeft()) {
                    MsgBox.error(RscriptFinderPanel.this, either.left(), "Error Output");
                }
                else {
                    TrialDesignPreferences.getInstance().setRscriptPath(scriptPath);
                    onScriptPathChecked.accept(Either.right(scriptPath));
                    checkOutput = either.right();
                }
            }
        };

        backgroundRunner.runBackgroundTask(task);
    }
}
