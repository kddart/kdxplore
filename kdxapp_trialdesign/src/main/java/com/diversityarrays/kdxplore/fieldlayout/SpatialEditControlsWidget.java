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
package com.diversityarrays.kdxplore.fieldlayout;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.fielddesign.EditModeWidget;
import com.diversityarrays.kdxplore.fielddesign.FieldLayoutEditPanel;
import com.diversityarrays.kdxplore.fielddesign.alg.CombinedAlg;
import com.diversityarrays.kdxplore.fielddesign.alg.FarthestFirst;
import com.diversityarrays.kdxplore.fielddesign.alg.IterableAlg;
import com.diversityarrays.kdxplore.fielddesign.alg.StepState;
import com.diversityarrays.kdxplore.fieldlayout.ReplicateDetailsPanel.EditWhat;
import com.diversityarrays.kdxplore.trialdesign.TrialDesignPreferences;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.GBH;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class SpatialEditControlsWidget {

    private static final boolean ALLOW_SPATIALS_STEP = Boolean.getBoolean("ALLOW_SPATIALS_STEP");

    private final ReplicateDetailsModel replicateDetailsModel;
    private final FieldLayoutEditPanel<ReplicateCellContent> editPanel;

    private IterableAlg iterableAlg;

    private boolean justDoneInit = false;
    private final Action assignSpatialsAction = new AbstractAction(">" + UnicodeChars.ELLIPSIS) {
        @Override
        public void actionPerformed(ActionEvent e) {

            PlantingBlock<ReplicateCellContent> pb = replicateDetailsModel.getPlantingBlock();
            if (pb.getContentCount() > 0) {
                if (! justDoneInit) {
                    if (JOptionPane.YES_NO_OPTION
                            !=
                        JOptionPane.showConfirmDialog(widgetComponent,
                                "Clear them all and continue?",
                                "Some Entries already present",
                                JOptionPane.YES_NO_OPTION))
                    {
                        return;
                    }
                }

                replicateDetailsModel.clearPlantingBlockContent();
                assignToOthersAction.setEnabled(false);

                replicateDetailsModel.updateEntryTypeCounts();
            }

            int nSpatials = replicateDetailsModel.getSpatialCheckCountRequired();
            if (nSpatials <= 0) {
                MsgBox.info(widgetComponent, "?? Required count = " + nSpatials, "Start Spatial Placement");
                return;
            }

            justDoneInit = true;
            // TODO in future, allow leaving existing spatials - but first
            //      must fix the bug in the IterableAlg for processing that case

            CombinedAlg combinedAlg = new CombinedAlg(
                    pb.getColumnCount(),
                    pb.getRowCount(),
                    System.currentTimeMillis());
            int inset = insetSpinner.isVisible()
                    ? insetNumberModel.getNumber().intValue()
                    : 0;
            combinedAlg.setFarthestFirstInset(inset);

            iterableAlg = combinedAlg;

            try {
                StepState stepState = iterableAlg.start(nSpatials);
                stepSpatialsAction.setEnabled(ALLOW_SPATIALS_STEP && true);
                stepToEndAction.setEnabled(ALLOW_SPATIALS_STEP && true);

                showSpatials(stepState);
            }
            catch (RuntimeException re) {
                MsgBox.error(widgetComponent, re, "Internal Error");
            }
        }
    };

    private final Action stepSpatialsAction = new AbstractAction(">|") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (iterableAlg == null) {
                new Toast(stepSpatialsButton, "No Algorithm Assigned", Toast.SHORT).show();
                return;
            }

            justDoneInit = false;
            Optional<StepState> optional = iterableAlg.step();
            if (optional.isPresent()) {
                showSpatials(optional.get());
            }
            else {
                assignSpatialsAction.setEnabled(true);
                stepSpatialsAction.setEnabled(false);
                stepToEndAction.setEnabled(false);

                assignToOthersAction.setEnabled(true);
                new Toast(stepSpatialsButton, "Stable position reached", Toast.SHORT).show();
            }
        }

    };
    private final JButton stepSpatialsButton = new JButton(stepSpatialsAction);

    static class StepUntilStableResult {
        public final StepState stepState;
        public final int iterations;
        public final boolean cancelled;

        public StepUntilStableResult(StepState ss, int n, boolean cancelled) {
            stepState = ss;
            iterations = n;
            this.cancelled = cancelled;
        }
    }

    static class StepUntilStable extends JDialog {

        private boolean keepStepping;
        private final IterableAlg iterableAlg;

        private Action cancelAction = new AbstractAction(UnicodeChars.PLAY_STOP) {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        private final JProgressBar progressBar = new JProgressBar();

        private final Consumer<Either<Throwable, StepUntilStableResult>> onStopped;
        private final boolean delaying;
        private final Consumer<StepState> showStepState;

        StepUntilStable(Window owner,
                IterableAlg iterableAlg,
                boolean delaying,
                Consumer<StepState> showStepState,
                Consumer<Either<Throwable, StepUntilStableResult>> onStopped)
        {
            super(owner,
                    "Searching for Stable Distribution" + UnicodeChars.ELLIPSIS,
                    ModalityType.APPLICATION_MODAL);

            this.iterableAlg = iterableAlg;
            this.showStepState = showStepState;
            this.onStopped = onStopped;
            this.delaying = delaying;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            progressBar.setIndeterminate(true);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    startStepping();
                }
                @Override
                public void windowClosed(WindowEvent e) {
                    keepStepping = false;
                    dispose();
                }
            });

            Box box = Box.createVerticalBox();
            box.add(progressBar);
            box.add(new JButton(cancelAction));

            pack();
        }

        protected void startStepping() {
            SwingWorker<StepUntilStableResult, StepState> worker = new SwingWorker<StepUntilStableResult, StepState>() {
                @Override
                protected StepUntilStableResult doInBackground() throws Exception {
                    keepStepping = true;
                    Optional<StepState> optional = iterableAlg.step();
                    int iterations = 1;
                    while (optional.isPresent() && keepStepping) {
                        publish(optional.get());
                        if (delaying) {
                            try { Thread.sleep(500); }
                            catch (InterruptedException ignore) { }
                        }
                        optional = iterableAlg.step();
                        ++iterations;
                    }

                    if (optional.isPresent()) {
                        return new StepUntilStableResult(optional.get(),
                                iterations,
                                ! keepStepping);
                    }
                    return new StepUntilStableResult(null,
                            iterations,
                            ! keepStepping);
                }

                @Override
                protected void process(List<StepState> chunks) {
                    if (! chunks.isEmpty()) {
                        showStepState.accept(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    try {
                        onStopped.accept(Either.right(get()));
                    }
                    catch (InterruptedException e) {
                        onStopped.accept(Either.left(e));
                    }
                    catch (ExecutionException e) {
                        onStopped.accept(Either.left(e.getCause()));
                    }
                    // TODO Auto-generated method stub
                    super.done();
                }

            };

            worker.execute();
        }

    }

    private final Action stepToEndAction = new AbstractAction(">>|") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (iterableAlg == null) {
                new Toast(stepSpatialsButton, "No Algorithm Assigned", Toast.SHORT).show();
                return;
            }

            boolean delaying = 0 != (ActionEvent.SHIFT_MASK & e.getModifiers());

            Consumer<Either<Throwable, StepUntilStableResult>> onStopped = new Consumer<Either<Throwable, StepUntilStableResult>>() {
                @Override
                public void accept(Either<Throwable, StepUntilStableResult> either) {

                    if (either.isLeft()) {
                        MsgBox.error(widgetComponent, either.left(), "Error");
                        return;
                    }

                    StepUntilStableResult result = either.right();
                    if (result.cancelled) {
                        new Toast(stepToEndButton,
                                String.format("Stopped after %d iterations", result.iterations),
                                Toast.SHORT).show();
                    }
                    else {
                        assignSpatialsAction.setEnabled(true);
                        stepSpatialsAction.setEnabled(false);
                        stepToEndAction.setEnabled(false);

                        assignToOthersAction.setEnabled(true);
                        new Toast(stepToEndButton,
                                String.format("Stable position reached after %d iterations",
                                        result.iterations),
                                Toast.SHORT).show();
                    }
                }
            };

            Consumer<StepState> showStepState = new Consumer<StepState>() {
                @Override
                public void accept(StepState t) {
                    showSpatials(t);
                }

            };
            StepUntilStable dlg = new StepUntilStable(GuiUtil.getOwnerWindow(widgetComponent),
                    iterableAlg,
                    delaying,
                    showStepState,
                    onStopped);
            dlg.setVisible(true);
        }

    };
    private final JButton stepToEndButton = new JButton(stepToEndAction);

    private final Action assignToOthersAction = new AbstractAction("Assign to Others") {
        @Override
        public void actionPerformed(ActionEvent e) {
            MsgBox.error(widgetComponent, "Not yet Available", "Assign to Others");
        }
    };

    private final SpinnerNumberModel insetNumberModel = new SpinnerNumberModel(1, 0, 5, 1);
    private final JSpinner insetSpinner = new JSpinner(insetNumberModel);

    private final JComponent widgetComponent;

    private final String instructionsHtml;

    private Color spatialsColor;

    private final Container insetsContainer = Box.createHorizontalBox();

    public SpatialEditControlsWidget(ReplicateDetailsModel rdm,
            FieldLayoutEditPanel<ReplicateCellContent> editPanel,
            Function<EntryType, Color> entryTypeColorSupplier)
    {
        this.replicateDetailsModel = rdm;
        this.editPanel = editPanel;

        spatialsColor = Color.GREEN;
        Either<String, Optional<EntryType>> either = rdm.getSpatialEntryType();
        if (either.isRight()) {
            Optional<EntryType> opt = either.right();
            if (opt.isPresent()) {
                spatialsColor = entryTypeColorSupplier.apply(opt.get()).darker();
            }
        }


        replicateDetailsModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                assignToOthersAction.setEnabled(
                        replicateDetailsModel.getSpatialCheckCountDefined() <= 0);
                initInsetsModel();
            }
        });
        initInsetsModel();

        if (ALLOW_SPATIALS_STEP) {
            instructionsHtml = readInstructionsHtml();
        }
        else {
            String spatialName = StringUtil.htmlEscape(replicateDetailsModel.getSpatialChecksName());

            instructionsHtml = String.format("<HTML>Use the button to randomly distribute %s checks."
                    + "<BR>Repeat until you get a distribution that looks good."
                    + "<HR>"
                    + "If you get close you can tweak the result:<ol>"
                    + "<li>Select the %s option in the <i>Edit</i> options on the left</li>"
                    + "<li>Switch to <i>%s</i> and select the <i>%s</i> checkbox</li>"
                    + "</ol>",
                    spatialName,
                    EditModeWidget.EditMode.REPLICATE_CONTENT.asBoldHtml(),
                    EditWhat.NON_SPATIAL.visible,
                    spatialName);
        }

        insetsContainer.add(new JLabel("Exclude Edge"));
        insetsContainer.add(insetSpinner);
        Arrays.asList(insetsContainer.getComponents()).stream()
            .forEach(c -> {
                if (c instanceof JComponent) {
                    ((JComponent) c).setToolTipText("Don't assign spatials this close to the edge");
                    }
                });


        JPanel panel = new JPanel();
        GBH gbh = new GBH(panel);
        int y = 0;

        gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.SW, new JButton(assignSpatialsAction));
        gbh.add(1,y, 1,2, GBH.BOTH, 2,3, GBH.CENTER, new JLabel(instructionsHtml));
        ++y;

        gbh.add(0,y, 1,1, GBH.NONE, 1.5,1, GBH.NW, insetsContainer);
        ++y;

        if (ALLOW_SPATIALS_STEP) {
            gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.WEST, stepSpatialsButton);
            ++y;

            Box last = Box.createHorizontalBox();
            last.add(stepToEndButton);
            last.add(new JButton(assignToOthersAction));
            gbh.add(0,y, 2,1, GBH.NONE, 1,1, GBH.WEST, last);
            ++y;
        }

        assignToOthersAction.setEnabled(false);

        panel.setBorder(new LineBorder(spatialsColor));
        widgetComponent = panel;
    }

    private void initInsetsModel() {
        int nSpatialsRequired = replicateDetailsModel.getSpatialCheckCountRequired();
        int maxInset = 0;
        if (nSpatialsRequired > 0) {
            Dimension sz = replicateDetailsModel.getPlantingBlockSize();
            maxInset = Math.min(sz.width, sz.height);

            int nPoints = FarthestFirst.getPointCountAvailable(sz, maxInset);
            while (nPoints <= nSpatialsRequired && maxInset >= 0) {
                --maxInset;
                nPoints = FarthestFirst.getPointCountAvailable(sz, maxInset);
            }
        }

        if (maxInset > 0) {
            insetSpinner.setVisible(true);
            insetNumberModel.setMaximum(maxInset);
        }
        else {
            insetSpinner.setVisible(false);
        }
    }

    private String readInstructionsHtml() {
        InputStream is = getClass().getResourceAsStream("spatial-instructions.html");
        String html = null;
        if (is != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int nb;
            try {
                while ((nb = is.read(buf)) > 0) {
                    baos.write(buf, 0, nb);
                }
                baos.close();
                html = baos.toString();
            }
            catch (IOException e1) {
                Shared.Log.w(SpatialEditControlsWidget.class.getSimpleName(), e1);
            }
            finally {
                try { is.close(); }
                catch (IOException ignore) {}
            }
        }
        if (Check.isEmpty(html)) {
            String spatialName = TrialDesignPreferences.getInstance().getSpatialEntryName();
            if (Check.isEmpty(spatialName)) {
                spatialName = "Spatial";
            }
            html = String.format(
                    "<HTML>Use the buttons to generate locations for <i>%s</i> Checks",
                    spatialName);
        }
        return html;
    }

    public JComponent getWidgetComponent() {
        return widgetComponent;
    }

    public void initialise(boolean b) {
        iterableAlg = null;
        assignSpatialsAction.setEnabled(true);
        stepSpatialsAction.setEnabled(false);
    }

    private void showSpatials(StepState stepState) {
        Either<String, Optional<EntryType>> either = replicateDetailsModel.getSpatialEntryType();
        if (either.isLeft()) {
            MsgBox.error(widgetComponent, either.left(), "Internal Error");
            return;
        }

        Optional<EntryType> opt = either.right();
        if (! opt.isPresent()) {
            MsgBox.error(widgetComponent, "Missing Spatial Check Type", "Internal Error");
            return;
        }

        EntryType spatialCheck = opt.get();
        List<Point> points = Arrays.asList(stepState.spatials).stream()
            .map(np -> np.point)
            .collect(Collectors.toList());

        replicateDetailsModel.clearPlantingBlockContent();

        ReplicateCellContent newContent = new ReplicateCellContent(spatialCheck);
        replicateDetailsModel.setContentAtPoints(newContent, points.toArray(new Point[points.size()]));

        editPanel.repaint();
    }

    public Color getSpatialsColor() {
        return spatialsColor;
    }

}
