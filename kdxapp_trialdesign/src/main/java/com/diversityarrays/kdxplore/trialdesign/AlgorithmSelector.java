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
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.diversityarrays.kdcompute.db.Knob;
import com.diversityarrays.kdcompute.db.KnobBinding;
import com.diversityarrays.kdcompute.db.Plugin;
import com.diversityarrays.kdcompute.db.PluginCollection;
import com.diversityarrays.kdcompute.db.RunBinding;
import com.diversityarrays.kdcompute.db.TrialDesignOutput;
import com.diversityarrays.kdcompute.designer.RunBindingPanel;
import com.diversityarrays.kdxplore.design.AlgorithmParam;
import com.diversityarrays.kdxplore.trialdesign.algorithms.Algorithms;
import com.diversityarrays.kdxplore.trialdesign.algorithms.RunSpec;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ConfirmDialog;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ListByOne;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.UnicodeChars;
import com.diversityarrays.util.VerticalLabelUI;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.StringUtil;
import net.pearcan.util.Util;

@SuppressWarnings("nls")
public class AlgorithmSelector extends JPanel {

    private List<TrialEntry> entries = new ArrayList<>();

    private final RunBindingPanel runBindingPanel = new RunBindingPanel(false);

    private final JLabel algorithmName = new JLabel();

    private final JPanel bindingPanel = new JPanel(new BorderLayout());

    private final JLabel instructions = new JLabel("", JLabel.CENTER);

    private final DesignPerLocationOptions designPerLocationOptions = new DesignPerLocationOptions();

    private BackgroundRunner backgroundRunner;

    private final Algorithms algorithms;

    private String scriptPath;

    public AlgorithmSelector(BackgroundRunner br) {
        super(new BorderLayout());

        this.backgroundRunner = br;

        algorithms = new Algorithms();

        scriptPath = TrialDesignPreferences.getInstance().getRscriptPath();

        Either<Exception, PluginCollection> either = algorithms.getPluginCollection();
        if (either.isLeft()) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Unable to load algorithms");
            either.left().printStackTrace(pw);
            pw.close();

            add(new JLabel(sw.toString()), BorderLayout.CENTER);
            return;
        }

        PluginCollection pluginCollection = either.right();
        if (pluginCollection.plugins==null || pluginCollection.plugins.length <= 0) {
            add(new JLabel("No Trial Design Plugins found in '" + pluginCollection.pluginGroup + "'"),
                    BorderLayout.CENTER);
            return;
        }

        ListByOne<Algorithms.ErrorType, String> errors = algorithms.getErrors();

        if (algorithms.getAlgorithmCount() <= 0) {
            String msg = "No Trial Design Plugins available (#2)";
            if (errors.isEmpty()) {
                add(new JLabel(msg), BorderLayout.CENTER);
            }
            else {
                StringBuilder sb = createErrorsLabel(errors);
                sb.append("<br><hr><b>").append(msg).append("</b>");
                add(new JLabel(sb.toString()), BorderLayout.CENTER);
            }
            return;
        }

        algorithmTableModel.replaceWith(algorithms.getSortedPlugins());

        algorithmsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        algorithmsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    int vrow = algorithmsTable.getSelectedRow();
                    if  (vrow >= 0) {
                        int mrow = algorithmsTable.convertRowIndexToModel(vrow);
                        if (mrow >= 0) {
                            setPlugin(algorithmTableModel.get(mrow));
                        }
                    }
                    else {
                        setPlugin(null);
                    }
                }
            }
        });

        algorithmsTable.setToolTipText("Double-click to open documentation URL");
        algorithmsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2 && SwingUtilities.isLeftMouseButton(e)) {
                    Point pt = e.getPoint();
                    int vrow = algorithmsTable.rowAtPoint(pt);
                    if (vrow >= 0) {
                        int mrow = algorithmsTable.convertRowIndexToModel(vrow);
                        if (mrow >= 0) {
                            Plugin  a = algorithmTableModel.get(mrow);
                            String desc = a.getDescription();
                            if (! Check.isEmpty(desc)) {
                                JTextArea ta = new JTextArea(desc, 10, 40);
                                Font f = ta.getFont();
                                ta.setFont(f.deriveFont(f.getSize2D() * 1.5f));
                                ta.setLineWrap(true);
                                ta.setWrapStyleWord(true);
                                ta.setEditable(false);
                                JScrollPane scrollPane = new JScrollPane(ta);
                                MsgBox.info(AlgorithmSelector.this, scrollPane, a.getAlgorithmName());
                            }
                            else {
                                String docUrl = a.getDocUrl();
                                if (! Check.isEmpty(docUrl)) {
                                    try {
                                        Util.openUrl(docUrl);
                                    }
                                    catch (IOException e1) {
                                        MsgBox.error(AlgorithmSelector.this,
                                                docUrl + "\n" + e1.getMessage(),
                                                "Unable to open URL");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        if (algorithmTableModel.getRowCount() > 0) {
            setPlugin(algorithmTableModel.get(0));
        }

        runBindingPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateRunAction();
            }
        });
        updateRunAction();

        runAlgorithmAction.putValue(Action.SHORT_DESCRIPTION, "Use SHIFT to change Rscript");

        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(new JButton(runAlgorithmAction));

        algorithmName.setFont(algorithmName.getFont().deriveFont(Font.BOLD));
        bindingPanel.add(algorithmName, BorderLayout.NORTH);
        bindingPanel.add(runBindingPanel, BorderLayout.CENTER);
        bindingPanel.add(buttons, BorderLayout.SOUTH);


        JComponent rightComponent;
        if (errors.isEmpty()) {
            rightComponent = bindingPanel;
        }
        else {
            JLabel errorsLabel = new JLabel(createErrorsLabel(errors).toString());
            JScrollPane errorsScroll = new JScrollPane(errorsLabel);

            JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
            boolean macos = Util.isMacOS();
            addTab(macos, tabbedPane, "Bindings", bindingPanel);
            addTab(macos, tabbedPane, "Warnings", errorsScroll);

            rightComponent = tabbedPane;
        }
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(algorithmsTable),
                rightComponent);
        splitPane.setResizeWeight(0.6);

        instructions.setText("<HTML>"
        		+ "<table>"
        		+ "<tr><td valign='top'><b>1)</b> Provide your Entry List on the left</td>"
        		+ "<td valign='top'><b>3)</b> Provide values for the parameters</td></tr>"

        		+ "<tr><td valign='top'><b>2)</b> Select an Algorithm from the list above</td>"
        		+ "<td valign='top'><b>4)</b> Choose whether each Location uses<br>the same design output</td></tr>"
        		+ "</table>"
                + "<HR>"
                );

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(instructions, BorderLayout.NORTH);
        bottom.add(designPerLocationOptions, BorderLayout.CENTER);

        add(splitPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addTab(boolean macos, JTabbedPane tabbedPane, String title, Component c) {
    	tabbedPane.addTab(title, c);
    	int index = tabbedPane.indexOfTab(title);
    	if (! macos) {
        	JLabel label = new JLabel(title);
        	label.setBorder(new EmptyBorder(2,2,2,2));
    		label.setUI(new VerticalLabelUI(VerticalLabelUI.UPWARDS));
    		tabbedPane.setTabComponentAt(index, label);
    	}
    }

    private StringBuilder createErrorsLabel(ListByOne<Algorithms.ErrorType, String> errors) {
        StringBuilder sb = new StringBuilder(
                "<HTML>Click on <i>Bindings</i> Tab to continue<BR>Problems loading Plugins:");
        for (Algorithms.ErrorType et : errors.keySet()) {
            List<String> details = errors.getValue(et);
            sb.append("<br><B>").append(et.description).append("</b>");
            if (details.size() == 1) {
                sb.append(": ").append(StringUtil.htmlEscape(details.get(0)));
            }
            else {
                sb.append("<br><ul>");
                for (String s : details) {
                    sb.append("<li>").append(StringUtil.htmlEscape(s)).append("</li>");
                }
                sb.append("</ul>");
            }
        }
        return sb;
    }

    /**
     * The Run button!
     */
    private final Action runAlgorithmAction = new AbstractAction("Run") {
        @Override
        public void actionPerformed(ActionEvent e) {

            boolean shifted = 0 != (ActionEvent.SHIFT_MASK & e.getModifiers());
            if (Check.isEmpty(scriptPath) || shifted) {
                showScriptFinderDialog();
                return;
            }

            RunBinding rb = runBindingPanel.getRunBinding();
            try {
                Either<String, RunSpec> either = algorithms.createRunSpec(rb, entries);
                if (either.isLeft()) {
                    MsgBox.error(AlgorithmSelector.this,
                            rb.getPlugin().getPluginName() + "\n" + either.left(),
                            "Error Preparing Algorithm");
                }
                else {
                    RunSpec runSpec = either.right();

                    String commandRun = runSpec.constructRunCommand(rb, scriptPath);

                    // FIXME this converting spaces to commas and then later splitting on commas is ridiculous

                    String[] command = commandRun.split(",");

                    JobRunningTask task = new JobRunningTask(
                    		rb.getPlugin().getAlgorithmName(),
                    		entries,
                    		command,
                    		runSpec.outputFolder,
                    		(arr, f) -> handleAlgorithmRunResult(arr, f),
                    		(eith) -> handleAlgorithmError(command, runSpec.outputFolder, eith),
                    		backgroundRunner);

                    backgroundRunner.runBackgroundTask(task);
                }

            }
            catch (IOException e1) {
                MsgBox.error(AlgorithmSelector.this, e1, "Error Running Algorithm");
                return;
            }
        }
    };

    private void handleAlgorithmError(String[] command, File algorithmFolder, Either<Throwable, String> either) {
        if (either.isLeft()) {
            MsgBox.error(this, either.left().getMessage(), "Cancelled");
            return;
        }

        String errmsg = either.right();

        String[] lines = errmsg.split("\n");
        Pattern pattern = Pattern.compile("^Error in library\\(\\\"([^\\\"]*)\\\"\\) :.*", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(lines[0]);

        if (m.matches()) {
            if (lines.length > 1) {
                errmsg = lines[1];
            }
        }
        if (RunMode.getRunMode().isDeveloper()) {
            JScrollPane scrollPane = new JScrollPane(new JLabel("<HTML><PRE>"
                    + StringUtil.htmlEscape(errmsg).replaceAll("\n", "<BR>")
            + "</PRE><HR>Output Folder is:<BR>"
            + algorithmFolder.getPath()));
            ConfirmDialog dlg = new ConfirmDialog(GuiUtil.getOwnerWindow(
                    this),
                    "Error Encountered",
                    "Open Output Folder", UnicodeChars.CANCEL_CROSS)
            {
                @Override
                protected Component createMainPanel() {
                    return scrollPane;
                }
            };
            dlg.setVisible(true);
            if (dlg.confirmed) {
                try {
                    PrintWriter pw = new PrintWriter(new FileWriter(new File(algorithmFolder, "command.txt")));
                    Arrays.asList(command).forEach(s -> pw.println(s));
                    pw.close();
                    Desktop.getDesktop().open(algorithmFolder);
                }
                catch (IOException e) {
                    MsgBox.error(this, e, "Unable to open");
                }
            }
        }
        else {
            MsgBox.error(this, errmsg, "Problem Encountered");
        }
    }

    private void handleAlgorithmRunResult(AlgorithmRunResult arr, File kdxploreOutputFile) {
        TrialDesignOutput tdo = arr.getTrialDesignOutput();

        Optional<String> opt = tdo.getWhyOutputNotUsable();
        if (opt.isPresent()) {
            MsgBox.error(this, opt.get(), "Output File Error");
            removeAlgorithmFolder(arr.getAlgorithmFolder());
        } else {
//            DesignParams<TrialEntry> designParams = new DesignParams<>(arr.getOutputEntries());
//
//            if (RunMode.getRunMode().isDeveloper()) {
//                onDesignParametersChanged.accept(designParams);
//            }

            if (kdxploreOutputFile.exists()) {
                AlgorithmOutputViewer output = new AlgorithmOutputViewer(
                        arr, kdxploreOutputFile);
                output.pack();
                output.setLocationRelativeTo(this);
                output.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        removeAlgorithmFolder(arr.getAlgorithmFolder());
                    }
                });
                output.setVisible(true);
            }
            else {
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                        kdxploreOutputFile.getPath() +
                        "\n\nDo you want to view the output folder?",
                        "Missing Output File",
                        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE))
                {
                    if (RunMode.getRunMode().isDeveloper()) {
                        try {
                            Desktop.getDesktop().open(arr.getAlgorithmFolder());
                        }
                        catch (IOException e) {
                            MsgBox.warn(this, e, "Error opening output folder");
                        }
                    }
                }
                else {
                    removeAlgorithmFolder(arr.getAlgorithmFolder());
                }
            }
        }
    }

    private void removeAlgorithmFolder(File algorithmFolder) {
        Optional<IOException> opterr = deleteTempFolder(algorithmFolder);
        if (opterr.isPresent()) {
            MsgBox.warn(this, opterr.get(), "Error removing output folder");
        }
    }

    private Optional<IOException> deleteTempFolder(File tempAlgorithmOutputFile) {

        if (tempAlgorithmOutputFile.isDirectory()) {
            for (File f : tempAlgorithmOutputFile.listFiles()) {
                deleteTempFolder(f);
            }
        }
        else {
            if (! tempAlgorithmOutputFile.delete()) {
                return Optional.of(new IOException("Could not delete file" +  tempAlgorithmOutputFile.getAbsolutePath()));
            }
        }
        return Optional.empty();
    }


    private void showScriptFinderDialog() {
        RscriptFinderPanel[] panel = new RscriptFinderPanel[1];
        JDialog[] dialog = new JDialog[1];

        Consumer<Either<Throwable, String>> onScriptPathChecked = new Consumer<Either<Throwable,String>>() {
            @Override
            public void accept(Either<Throwable, String> either) {
                if (either.isLeft()) {
                    Throwable err = either.left();
                    if (err instanceof CancellationException) {
                        MsgBox.error(AlgorithmSelector.this, scriptPath, "Cancelled");
                    }
                    else {
                        MsgBox.error(AlgorithmSelector.this, err, "Error while Checking");
                    }
                }
                else {
                    String path = either.right();
                    String versionNumber = panel[0].getVersionNumber();
                    int answer = JOptionPane.showConfirmDialog(AlgorithmSelector.this,
                            path + "\n" + versionNumber,
                            "Do you want to use this script?",
                            JOptionPane.YES_NO_OPTION);
                    if (JOptionPane.YES_OPTION == answer) {
                        scriptPath = path;
                        TrialDesignPreferences.getInstance().setRscriptPath(scriptPath);
                        dialog[0].dispose();
                    }
                }
            }
        };

        JLabel label = new JLabel();
        if (Check.isEmpty(scriptPath)) {

        }
        else {
            label.setText("Amend the script command ");
        }
        panel[0] = new RscriptFinderPanel(backgroundRunner,
                    label,
                    scriptPath,
                    onScriptPathChecked);

        JDialog dlg = new JDialog(GuiUtil.getOwnerWindow(AlgorithmSelector.this),
                "Check Rscript Path",
                ModalityType.APPLICATION_MODAL);
        dialog[0] = dlg;

        dlg.setContentPane(panel[0]);
        dlg.pack();
        dlg.setSize(320, 240);
        dlg.setLocationRelativeTo(AlgorithmSelector.this);
        dlg.setVisible(true);
    }

    private final AlgorithmTableModel algorithmTableModel = new AlgorithmTableModel();
    private final JTable algorithmsTable = new JTable(algorithmTableModel) {

        @Override
        public String getToolTipText(MouseEvent me) {
            String result = null;
            Point pt = me.getPoint();
            int vrow  = rowAtPoint(pt);
            if (vrow >= 0) {
                int mrow = convertRowIndexToModel(vrow);
                if (mrow >= 0) {
                    Plugin plugin = algorithmTableModel.get(mrow);

                    StringBuilder sb = new StringBuilder("<HTML>");
                    String s = plugin.getScriptTemplateFilename();
                    if (! Check.isEmpty(s)) {
                        sb.append(StringUtil.htmlEscape(s));
                    }

                    s = algorithms.getRscriptCommand(plugin);
                    if (s!= null  && ! Check.isEmpty(s)) {
                        sb.append("<br>").append(StringUtil.htmlEscape(s));
                    }
                    result = sb.toString();
                }
            }
            return result;
        }

    };

    private void setPlugin(Plugin a) {
        if (a == null) {
            algorithmName.setText("");
            runBindingPanel.setData(null, null);
        }
        else {
            algorithmName.setText(a.getAlgorithmName());
            runBindingPanel.setData(a, null);
        }
    }

    private void updateRunAction() {
        String msg = runBindingPanel.getIncompleteMessage("Please select an Algorithm");
        runAlgorithmAction.setEnabled(msg.isEmpty());
    }

    public void setLocationCount(int locationCount) {
        designPerLocationOptions.setLocationCount(locationCount);
    }

    public void setEntries(List<TrialEntry> entries) {

        this.entries.clear();
        this.entries.addAll(entries);

    }

    public <T> Optional<T> getParameterValue(AlgorithmParam<T> param) {
        Optional<T> result = Optional.empty();

        Predicate<Knob> isReplicate = new Predicate<Knob>() {
            @Override
            public boolean test(Knob knob) {
                if (param.strings.contains(knob.getDescription().toLowerCase())) {
                    return true;
                }
                if (param.strings.contains(knob.getKnobName().toLowerCase())) {
                    return true;
                }
                return false;
            }
        };

        RunBinding rb = runBindingPanel.getRunBinding();
        for (KnobBinding kb : rb.getKnobBindings()) {
            Knob knob = kb.getKnob();
            if (isReplicate.test(knob)) {
                Optional<T> opt = param.getValue(kb);
                if (opt.isPresent()) {
                    result = opt;
                    break;
                }
            }
        }
        return result;
    }
}
