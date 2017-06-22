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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.Shared.For;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.design.PlantingBlockFactory;
import com.diversityarrays.kdxplore.design.ReplicateDetailsModel;
import com.diversityarrays.kdxplore.fieldlayout.DesignParams;
import com.diversityarrays.kdxplore.fieldlayout.LocationEditPanel;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.ReplicateCellContent;
import com.diversityarrays.kdxplore.fieldlayout.SiteLocation;
import com.diversityarrays.kdxplore.fieldlayout.TrialEntryAssignmentDataProvider;
import com.diversityarrays.kdxplore.trialdesign.TrialEntryFile;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;

import au.com.bytecode.opencsv.CSVWriter;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class FieldLayoutEditFrame extends JFrame {

    private final LocationEditPanel locationEditPanel;

    private static final Function<SiteLocation, String> TITLE_FACTORY = new Function<SiteLocation, String>() {
        @Override
        public String apply(SiteLocation t) {
            return "Edit " + t.name; //$NON-NLS-1$
        }
    };

    protected static final int MAX_FILENAME_LEN = 100;

    private final JCheckBox alwaysOnTop = new JCheckBox("Always on Top", true);

    private final Action saveAction = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {

            JFileChooser chooser = Shared.getFileChooser(For.DIR_SAVE);
            if (JFileChooser.APPROVE_OPTION != chooser.showSaveDialog(FieldLayoutEditFrame.this)) {
                return;
            }

            File saveDir = chooser.getSelectedFile();
            SiteLocation siteLocation = locationEditPanel.getSiteLocation();

            String filename = normaliseName.apply(siteLocation.name);
            File outdir = new File(saveDir, filename);

            if (! outdir.isDirectory()) {
                if (! outdir.mkdirs()) {
                    MsgBox.warn(FieldLayoutEditFrame.this,
                            "Unable to create folder for output:\n" + outdir.getPath(),
                            "Save Design");
                    return;
                }
            }

            List<PlantingBlock<ReplicateCellContent>> list = new ArrayList<>(locationEditPanel.getPlantingBlocks());
            Collections.sort(list);

            List<File> successes = new ArrayList<>();
            List<Pair<File, IOException>> errors = new ArrayList<>();
            for (PlantingBlock<ReplicateCellContent> pb : list) {
                doOneFile(outdir, pb, successes, errors);
            }
            StringBuilder sb = new StringBuilder("<HTML>");
            if (! successes.isEmpty()) {
                sb.append("Saved in ")
                    .append(StringUtil.htmlEscape(outdir.getPath()))
                    .append(":<ul>");
                for (File f : successes) {
                    sb.append("<li>")
                        .append(StringUtil.htmlEscape(f.getName()))
                        .append("</li>");
                }
                sb.append("</ul>");
            }
            if (! errors.isEmpty()) {
                sb.append("<HR>Problems:<ul>");
                for (Pair<File, IOException> pair : errors) {
                    sb.append("<li><b>")
                        .append(StringUtil.htmlEscape(pair.first.getName()));
                    sb.append("</b>: ")
                        .append(StringUtil.htmlEscape(pair.second.getMessage()));
                    sb.append("</li>");
                }
                sb.append("</ul>");
            }
            sb.append("<hr>Do you want to view the output folder?");
            JLabel label = new JLabel(sb.toString());

            int answer = JOptionPane.showConfirmDialog(FieldLayoutEditFrame.this,
                    new JScrollPane(label), "Design Save Results",
                    JOptionPane.YES_NO_OPTION);
            if (JOptionPane.YES_OPTION == answer) {
                try {
                    Desktop.getDesktop().open(outdir);
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private Function<String, String> normaliseName = new Function<String, String>() {
            @Override
            public String apply(String t) {
                String s = t.replaceAll("[^-.0-9A-Za-z]", "_");
                if (s.length() > MAX_FILENAME_LEN) {
                    s = s.substring(0, MAX_FILENAME_LEN);
                }
                return s;
            }
        };


        private Comparator<Point> pointComparator = new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                int diff = Integer.compare(o1.y, o2.y);
                if (diff == 0) {
                    diff = Integer.compare(o1.x, o2.x);
                }
                return diff;
            }
        };

        private void doOneFile(File outdir,
                PlantingBlock<ReplicateCellContent> pb,
                List<File> successes,
                List<Pair<File, IOException>> errors)
        {
            Map<Point, ReplicateCellContent> contentByPoint = pb.getContentByPoint();
            List<Point> points = new ArrayList<>(contentByPoint.keySet());
            Collections.sort(points, pointComparator);

            String pbname = normaliseName.apply(pb.getName());
            File outfile = new File(outdir, pbname + ".csv");

            CSVWriter writer  = null;
            boolean success = false;
            try {
                writer = new CSVWriter(new FileWriter(outfile));

                String[] line = new String[5];
                line[0] = "EntryId";
                line[1] = "EntryName";
                line[2] = "X";
                line[3] = "Y";
                line[4] = "EntryType";
                writer.writeNext(line);

                for (Point pt : points) {
                    ReplicateCellContent rcc = contentByPoint.get(pt);

                    EntryType entryType = rcc.entryType;

                    line[0] = Integer.toString(rcc.trialEntry.getEntryId());
                    line[2] = Integer.toString(pt.x);
                    line[3] = Integer.toString(pt.y);
                    line[4] = entryType.getName();

                    switch (entryType.variant) {
                    case CHECK:
                    case ENTRY:
                        line[1] = rcc.trialEntry.getEntryName();
                        break;
                    case SPATIAL:
                        line[1] = "-spatial-";
                        break;
                    default:
                        line[1] = "??" + entryType.variant.name();
                        break;
                    }
                    writer.writeNext(line);
                }

                success = true;
            }
            catch (IOException e1) {
                errors.add(new Pair<>(outfile, e1));
            }
            finally {
                if (writer != null) {
                    try { writer.close(); }
                    catch (IOException ignore) { }
                }

                if (success) {
                    successes.add(outfile);
                }
            }
        }
    };



    public FieldLayoutEditFrame(
            GraphicsConfiguration gc,
            SiteLocation location,
            Set<ReplicateDetailsModel> replicateModels,
            PlantingBlockFactory<ReplicateCellContent> blockFactory,
            Function<EntryType, Color> entryTypeColorSupplier,
            Map<EntryType, Integer> countByEntryTypes,
            Supplier<TrialEntryFile> trialEntryFileSupplier,
            Consumer<String> messagePrinter)
    {
        super(TITLE_FACTORY.apply(location), gc);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setAlwaysOnTop(alwaysOnTop.isSelected());

        KDClientUtils.initAction(ImageId.SAVE_24, saveAction, "Save Design");
        alwaysOnTop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAlwaysOnTop(alwaysOnTop.isSelected());
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.add(saveAction);
        toolBar.add(alwaysOnTop);

        TrialEntryAssignmentDataProvider dataProvider = new TrialEntryAssignmentDataProvider() {

            @Override
            public TrialEntryFile getTrialEntryFile() {
                return trialEntryFileSupplier.get();
            }

            @Override
            public Collection<ReplicateDetailsModel> getAllReplicateDetailModels() {
                return replicateModels;
            }
        };
        locationEditPanel = new LocationEditPanel(
                (s) -> { setTitle(TITLE_FACTORY.apply(s)); },
                location,
                replicateModels,
                countByEntryTypes,
                entryTypeColorSupplier,
                dataProvider,
                messagePrinter);

        Container cp = getContentPane();
        cp.add(toolBar, BorderLayout.NORTH);
        cp.add(locationEditPanel, BorderLayout.CENTER);

        pack();

        addWindowListener(new WindowAdapter() {
            boolean once = false;
            @Override
            public void windowOpened(WindowEvent e) {
                if (! once) {
                    once = true; // to be sure, to be sure
                    doPostOpenInit();
                }
            }
            @Override
            public void windowClosing(WindowEvent e) {
                // TODO - make frame non-closable and then check for unsaved changes
            }
        });
    }

    public void doPostOpenInit() {
        // TODO anything we need ???
    }

    public SiteLocation getSiteLocation() {
        return locationEditPanel.getSiteLocation();
    }

    public void onDesignParamsChanged(DesignParams dp) {
        locationEditPanel.setDesignParams(dp);

        new Toast(FieldLayoutEditFrame.this, "Design changed", 1000).show();
//        MsgBox.info(FieldLayoutEditFrame.this,
//                "You may wish to close and re-open this editor",
//                "Design params changed");

//        Set<WhatChanged> changed = locationEditPanel.setDesignParams(dp);
//        if (! changed.isEmpty()) {
//            String msg = changed.stream()
//                        .map(wc -> wc.name())
//                        .collect(Collectors.joining(", "));
//            System.out.println("onDesignParamsChanged: " + getSiteLocation().name + ": " + msg);
//            MsgBox.info(FieldLayoutEditFrame.this,
//                    "You may wish to close and re-open this editor",
//                    "Design params changed");
//        }
    }
}
