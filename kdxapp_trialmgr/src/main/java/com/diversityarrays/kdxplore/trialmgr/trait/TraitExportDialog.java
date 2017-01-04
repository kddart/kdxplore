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
package com.diversityarrays.kdxplore.trialmgr.trait;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.csvio.CsvDumper;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.util.CsvWriter;
import com.diversityarrays.kdsmart.db.util.CsvWriterImpl;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.trials.SampleGroupExportDialog;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.DefaultBackgroundRunner;
import net.pearcan.ui.GuiUtil;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.GBH;

public class TraitExportDialog extends JDialog {

    private final DefaultBackgroundRunner backgroundRunner = new DefaultBackgroundRunner();

    private final JTextField filepathField = new JTextField();

    private final Action browseAction = new AbstractAction("Browse...") {
        @Override
        public void actionPerformed(ActionEvent e) {

            JFileChooser chooser = KdxplorePreferences.getInstance()
                .getOutputFileChooser("Choose Output file", JFileChooser.FILES_ONLY, false,
                        com.diversityarrays.kdxplore.Shared.CSV_FILE_FILTER);

            if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(TraitExportDialog.this)) {
                File f = chooser.getSelectedFile();
                String path = f.getPath();
                if (! path.toLowerCase().endsWith(".csv")) {
                    path = path + ".csv";
                }
                filepathField.setText(path);
            }
        }
    };
    
    private final JCheckBox openAfterExport = new JCheckBox("Open file after export");

    private final Action exportAction = new AbstractAction("Export") {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleExportAction();
        }
    };

    private final Action cancelAction = new AbstractAction("Close") {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };

    private List<Trait> traits;
    
    public TraitExportDialog(Window owner, List<Trait> traits) {
        super(owner, "Export Traits", ModalityType.APPLICATION_MODAL);
        
        this.traits = traits;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        setGlassPane(backgroundRunner.getBlockingPane());
        
        JPanel mainPanel = new JPanel();
        GBH gbh = new GBH(mainPanel, 2,1,2,1);
        int y = 0;
        gbh.add(0,y, 1,1, GBH.NONE, 1,1, GBH.EAST, "CSV file:");
        gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, filepathField);
        gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new JButton(browseAction));
        ++y;
        
        exportAction.setEnabled(false);
        filepathField.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateExportAction();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateExportAction();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateExportAction();
            }

            private void updateExportAction() {
                exportAction.setEnabled(! Check.isEmpty(filepathField.getText().trim()));
            }
        });
        File outdir = KdxplorePreferences.getInstance().getOutputDirectory();
        if (outdir != null) {
            File outfile = new File(outdir, "traits.csv");
            filepathField.setText(outfile.getPath());
        }
        
        Box buttons = Box.createHorizontalBox();
        buttons.add(openAfterExport);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(new JButton(cancelAction));
        buttons.add(new JButton(exportAction));
        
        Container cp = getContentPane();
        cp.add(mainPanel, BorderLayout.CENTER);
        cp.add(buttons, BorderLayout.SOUTH);
        pack();
        
        Dimension sz = getSize();
        setSize(600, sz.height);
    }

    private void handleExportAction() {
        String path = filepathField.getText();
        if (! path.toLowerCase().endsWith(".csv")) {
            path = path + ".csv";
        }
        final File file = new File(path);
        final boolean openAfterExport = this.openAfterExport.isSelected();

        BackgroundTask<Void, Void> task = new BackgroundTask<Void, Void>("Exporting...", false) {

            @Override
            public Void generateResult(Closure<Void> arg0) throws Exception {

                CsvDumper<Trait> dumper = new CsvDumper<>(Trait.class);
                try {
                    CsvWriter csvWriter = new CsvWriterImpl(new FileWriter(file));
                    dumper.setCsvWriter(csvWriter);
                    dumper.emitHeaderLine();

                    for (Trait trait : traits) {
                        dumper.emitDataLine(trait);
                    }
                }
                finally {
                    dumper.close();
                }
                return null;
            }

            @Override
            public void onCancel(CancellationException ce) {
                onException(ce);
            }

            @Override
            public void onException(Throwable t) {
                MsgBox.error(TraitExportDialog.this, t, "Error - " + getTitle());
            }

            @Override
            public void onTaskComplete(Void v) {
                dispose();
                if (openAfterExport) {
                    try {
                        net.pearcan.util.Util.openFile(file);
                    } catch (IOException e) {
                        MsgBox.error(null, 
                                e.getMessage(),
                                "Failed: " + getTitle());
                    } 
                }
            }
        };
        
        backgroundRunner.runBackgroundTask(task);
    }

}
