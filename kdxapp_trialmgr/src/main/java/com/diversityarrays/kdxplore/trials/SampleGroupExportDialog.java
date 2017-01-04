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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;

import com.diversityarrays.kdsmart.db.FileUtility;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithPlotAttributesOption;
import com.diversityarrays.kdsmart.db.KDSmartDatabase.WithTraitOption;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotAttributeValue;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.SampleImpl;
import com.diversityarrays.kdsmart.db.entities.Specimen;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.ormlite.KDSmartDatabaseUpgrader;
import com.diversityarrays.kdsmart.db.util.CsvWriter;
import com.diversityarrays.kdsmart.db.util.CsvWriterImpl;
import com.diversityarrays.kdsmart.db.util.ExportFor;
import com.diversityarrays.kdsmart.db.util.ExportOutputHelper;
import com.diversityarrays.kdsmart.db.util.JsonUtil;
import com.diversityarrays.kdsmart.db.util.SampleOrder;
import com.diversityarrays.kdsmart.db.util.TrialDetailsExporter;
import com.diversityarrays.kdsmart.db.util.TrialExportHelper;
import com.diversityarrays.kdsmart.db.util.UnscoredSampleEmitter;
import com.diversityarrays.kdsmart.db.util.Util;
import com.diversityarrays.kdsmart.db.util.WhyMissing;
import com.diversityarrays.kdsmart.kdxs.TrialInfo;
import com.diversityarrays.kdsmart.kdxs.WorkPackage;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.Shared.For;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.kdxplore.data.kdx.SampleGroup;
import com.diversityarrays.kdxplore.data.util.DefaultUnscoredSampleEmitter;
import com.diversityarrays.kdxplore.data.util.WorkPackageFactory;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.ui.HelpUtils;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;
import com.google.gson.Gson;

import net.pearcan.ui.DefaultBackgroundRunner;
import net.pearcan.ui.GuiUtil;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.GBH;

public class SampleGroupExportDialog extends JDialog {

	enum OutputOption {
		KDX(ExportFor.KDXPLORE, "KDX (for KDSmart)"), 
		CSV_FULL(ExportFor.KDSMART_CSV_FULL, "CSV (Plots & Individuals)"),
		CSV_PLOTS(ExportFor.KDSMART_CSV_PLOTS_ONLY, "CSV"),
		JSON(null, "JSON"),
		ZIP(ExportFor.KDSMART_ZIP, "ZIP")
		;
	
		public final ExportFor exportFor;
		public final String displayName;
		OutputOption(ExportFor ef, String s) {
			exportFor = ef;
			displayName = s;
		}
	}
	
	static public String createTitle(DeviceType deviceType, Trial trial) {
       String title;
        switch (deviceType) {
        case DATABASE:
            title = "Database ";
            break;
        case EDITED:
            title = "Curated ";
            break;
        case KDSMART:
            title = "Collected ";
            break;
        case FOR_SCORING:
            title = "Scoring ";
            break;
        default:
            title = "";
            break;
        }
        title = title + "Samples for " + trial.getTrialName();
        return title;
	}
	
	static public Set<Integer> getExcludedTraitIds(Component comp, String msgTitle, KdxploreDatabase kdxdb, SampleGroup sg) {
        Set<Integer> excludeTheseTraitIds = new HashSet<>();

        try {
            Set<Integer> traitIds = collectTraitIdsFromSamples(kdxdb, sg);
            List<Trait> undecidableTraits = new ArrayList<>();
            Set<Integer> missingTraitIds = new TreeSet<>();
            Map<Integer, Trait> traitMap = kdxdb.getKDXploreKSmartDatabase().getTraitMap();
            for (Integer traitId : traitIds) {
                Trait t = traitMap.get(traitId);
                if (t == null) {
                    missingTraitIds.add(traitId);
                }
                else if (TraitLevel.UNDECIDABLE == t.getTraitLevel()) {
                    undecidableTraits.add(t);
                }
            }
            
            if (! missingTraitIds.isEmpty()) {
                String msg = missingTraitIds.stream()
                        .map(i -> Integer.toString(i))
                        .collect(Collectors.joining(","));
                MsgBox.error(comp, msg, "Missing Trait IDs");
                return null;
            }
            
            if (! undecidableTraits.isEmpty()) {
                String msg = undecidableTraits.stream()
                        .map(Trait::getTraitName)
                        .collect(Collectors.joining("\n", 
                                "Traits that are neither Plot nor Sub-Plot:\n", 
                                "\nDo you want to continue and Exclude samples for those Traits?"));

                if (JOptionPane.YES_OPTION 
                        != 
                    JOptionPane.showConfirmDialog(comp, msg, msgTitle, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) 
                {
                    return null;
                }
                
                Set<Integer> tmp = undecidableTraits.stream()
                        .map(Trait::getTraitId)
                        .collect(Collectors.toSet());

                excludeTheseTraitIds.addAll(tmp);
            }
        }
        catch (IOException e) {
            MsgBox.error(comp, "Unable to read samples from database\n" + e.getMessage(), msgTitle);
            return null;
        }
        
        return excludeTheseTraitIds;
	}
	
	private final JTextField filepathText = new JTextField();
	private JFileChooser fileChooser;
	private final Action browseFileAction = new AbstractAction("Browse...") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (fileChooser == null) {
			    fileChooser = Shared.getFileChooser(For.FILE_SAVE);
			}
			
			File file = new File(filepathText.getText());
			
			fileChooser.setSelectedFile(file);
			if (JFileChooser.APPROVE_OPTION ==
					fileChooser.showSaveDialog(SampleGroupExportDialog.this))
			{
				filepathText.setText(fileChooser.getSelectedFile().getPath());
			}
		}
	};
	
	private final Action exportAndCloseAction = new AbstractAction("Export & Close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleExportAction(true);
		}
	};
	
	private final Action exportAction = new AbstractAction("Export") {
		@Override
		public void actionPerformed(ActionEvent e) {
			handleExportAction(false);
		}
	};

	private final Action cancelAction = new AbstractAction("Close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	};
	
	private final JCheckBox wantMediaFilesOption = new JCheckBox("Emit Media Files");
	private final JCheckBox oldKdsmartOption = new JCheckBox("Old KDSmart Compat");
	
	private final List<JComponent> exportForOptions = Arrays.asList(
			wantMediaFilesOption, oldKdsmartOption);

	private final Trial trial;
	private final KdxploreDatabase kdxploreDatabase;
	private boolean haveCollectedSamples;
	private final SampleGroup sampleGroup;
	
	private final DefaultBackgroundRunner backgroundRunner = new DefaultBackgroundRunner();
	protected OutputOption selectedOutputOption;

	private final Set<Integer> excludeTheseTraitIds;
	
	public SampleGroupExportDialog(Window owner,
	        String title,
			Trial trial, 
			KdxploreDatabase kdxploreDatabase,
			DeviceType deviceType, DeviceIdentifier devid,
			SampleGroup sampleGroup,
			Set<Integer> excludeTheseTraitIds
			) 
	{
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setGlassPane(backgroundRunner.getBlockingPane());
		
		this.trial = trial;
		this.kdxploreDatabase = kdxploreDatabase;
		this.sampleGroup = sampleGroup;
		this.excludeTheseTraitIds = excludeTheseTraitIds;

		String deviceName = devid==null ? "Unknown_" + deviceType.name() : devid.getDeviceName();
		
		if (DeviceType.FOR_SCORING.equals(deviceType)) {
		    if (! Check.isEmpty(sampleGroup.getOperatorName())) {
                deviceName = sampleGroup.getOperatorName();
            }
		}
		
		File directory = KdxplorePreferences.getInstance().getOutputDirectory();
		if (directory==null) {
			directory = new File(System.getProperty("user.home"));
		}
		String filename = Util.getTimestampedOutputFileName(trial.getTrialName(), deviceName);
		
		File outfile = new File(directory, filename);
		
		filepathText.setText(outfile.getPath());
		
		filepathText.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				updateButtons();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateButtons();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateButtons();
			}
		});
		updateButtons();

		boolean developer = RunMode.getRunMode().isDeveloper();
		oldKdsmartOption.setForeground(Color.BLUE);
		oldKdsmartOption.setToolTipText("For ElapsedDays value compatiblity with older versions of KDSmart");

		Box exportForOptionsBox = Box.createHorizontalBox();
		for (JComponent comp : exportForOptions) {
			if (developer || comp != oldKdsmartOption) {
				exportForOptionsBox.add(comp);
			}
		}
		
		Map<JRadioButton,OutputOption> optionByRb = new HashMap<>();
		ActionListener rbListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedOutputOption = optionByRb.get(e.getSource());
				boolean enb = selectedOutputOption.exportFor != null;
				for (JComponent comp : exportForOptions) {
					comp.setEnabled(enb);
				}
			}
		};

        boolean anySamplesForIndividuals = true;
        try {
            anySamplesForIndividuals = kdxploreDatabase.getAnySamplesForIndividuals(sampleGroup);
        }
        catch (IOException e) {
            Shared.Log.w("SampleGroupExportDialog", "getAnySamplesForIndividuals", e);
        }

		ButtonGroup bg = new ButtonGroup();
		Box radioButtons = Box.createHorizontalBox();
		
		List<OutputOption> options = new ArrayList<>();
		Collections.addAll(options, OutputOption.values());

        if (! anySamplesForIndividuals) {
            // No relevant samples so don't bother offering the option.
            options.remove(OutputOption.CSV_FULL);
        }

		for (OutputOption oo : options) {
			if (! developer && OutputOption.JSON==oo) {
				continue;
			}

			JRadioButton rb = new JRadioButton(oo.displayName);
			if (OutputOption.JSON==oo) {
				rb.setForeground(Color.BLUE);
				rb.setToolTipText("Developer Only");
			}
			bg.add(rb);
			optionByRb.put(rb, oo);
			radioButtons.add(rb);
			rb.addActionListener(rbListener);
			if (bg.getButtonCount() == 1) {
				rb.doClick();
			}
		}

		JPanel panel = new JPanel();
		GBH gbh = new GBH(panel);
		int y = 0;
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Output File:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, filepathText);
		gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new JButton(browseFileAction));
		++y;
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Options:");
		gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, radioButtons);
		++y;
		
		gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, exportForOptionsBox);
		++y;
		
		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalGlue());
		buttons.add(new JButton(cancelAction));
		buttons.add(new JButton(exportAction));
		buttons.add(new JButton(exportAndCloseAction));
		
		Container cp = getContentPane();
		
		cp.add(panel, BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);
		
		pack();
	}

	protected void updateButtons() {
		exportAndCloseAction.setEnabled(! filepathText.getText().trim().isEmpty());
		
	}
	
	static private Set<Integer> collectTraitIdsFromSamples(KdxploreDatabase kdxdb, SampleGroup sampleGroup) throws IOException {
	    Set<Integer> allTraitIds = new HashSet<>();
        TrialItemVisitor<Sample> visitor = new TrialItemVisitor<Sample>() {
            @Override
            public void setExpectedItemCount(int count) { }
            @Override
            public boolean consumeItem(Sample s) throws IOException {
                if (s instanceof KdxSample) {
                    allTraitIds.add(s.getTraitId());
                }
                return true;
            }
        };

        kdxdb.getKDXploreKSmartDatabase().visitSamplesForTrial(
                SampleGroupChoice.create(sampleGroup.getSampleGroupId()),
                sampleGroup.getTrialId(), 
                SampleOrder.ALL_UNORDERED, 
                visitor);
        
        return allTraitIds;
	}
	
	private void handleExportAction(boolean closeWhenDone) {
	    
		BackgroundTask<File, Void> task = new BackgroundTask<File,Void>("Exporting...", true) {

			@Override
			public File generateResult(Closure<Void> publishPartial) throws Exception {
			    if (! haveCollectedSamples) {

			        // Need the KdxSamples into the sampleGroup
	                TrialItemVisitor<Sample> visitor = new TrialItemVisitor<Sample>() {

	                    @Override
	                    public void setExpectedItemCount(int count) { }

	                    @Override
	                    public boolean consumeItem(Sample s) throws IOException {
	                        if (s instanceof KdxSample) {
	                            if (! excludeTheseTraitIds.contains(s.getTraitId())) {
	                                sampleGroup.addSample((KdxSample) s);
	                            }
	                        }
	                        return true;
	                    }
	                };

	                kdxploreDatabase.getKDXploreKSmartDatabase().visitSamplesForTrial(
	                        SampleGroupChoice.create(sampleGroup.getSampleGroupId()),
	                        sampleGroup.getTrialId(), 
	                        SampleOrder.ALL_UNORDERED, 
	                        visitor);


	                haveCollectedSamples = true;
			    }
			    return doExport();
			}

			@Override
			public void onCancel(CancellationException e) {
			}

			@Override
			public void onException(Throwable cause) {
				GuiUtil.errorMessage(SampleGroupExportDialog.this, cause, getTitle());
			}

			@Override
			public void onTaskComplete(File result) {
				if (result != null) {
					int answer = JOptionPane.showConfirmDialog(SampleGroupExportDialog.this, 
							"Saved: " + result.getPath() 
							+ "\nOpen containing directory?", 
							getTitle(), 
							JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE);
					
					if (answer == JOptionPane.YES_OPTION) {
						try {
							net.pearcan.util.Util.openFile(result.getParentFile());
						} catch (IOException e) {
							GuiUtil.errorMessage(SampleGroupExportDialog.this, 
									e.getMessage(),
									"Failed: " + getTitle());
						}
					}
					
					if (closeWhenDone) {
						dispose();
					}
				}
			}
		};

		backgroundRunner.runBackgroundTask(task);
		
	}

	private File doExport() {
		
		String filename = filepathText.getText().trim();

		File outfile = new File(filename);
		
		ExportFor exportFor = selectedOutputOption.exportFor;

		boolean compatibleWithOldKdsmart = oldKdsmartOption.isSelected();

		File successIfNotNull;
		if (exportFor != null) {
			int targetDatabaseVersion ;
			if (compatibleWithOldKdsmart) {
				targetDatabaseVersion = KDSmartDatabaseUpgrader.FIRST_VERSION_WITH_EDAYS_AS_INT - 1;
			}
			else {
				targetDatabaseVersion = kdxploreDatabase
						.getKDXploreKSmartDatabase().getDatabaseVersion();
			}

			boolean wantMediaFiles = wantMediaFilesOption.isSelected();
			
			successIfNotNull = doExportAsKdxFile(sampleGroup, 
					targetDatabaseVersion, 
					wantMediaFiles, 
					exportFor, 
					outfile);
		}
		else {
			// Ensure .json suffix
			String suffix = ".json";
			if (! filename.toLowerCase().endsWith(suffix)) {
				outfile = new File(filename + suffix);
			}
			
			boolean needElapsedDaysAsDate = compatibleWithOldKdsmart;
			successIfNotNull = doExportAsWorkpackageJson(needElapsedDaysAsDate, sampleGroup, filename, outfile);
		}
		
		return successIfNotNull;
	}
	
	private File doExportAsKdxFile(SampleGroup sampleGroup, 
			int targetDatabaseVersion,
			boolean wantMediaFiles,
			ExportFor exportFor,
			File outputFile) 
	{
	    File exported = null;
		try {
			TrialExportHelper exportHelper = new MyTrialExportHelper(
					kdxploreDatabase,
					targetDatabaseVersion,
					sampleGroup,
					wantMediaFiles
					);

			File outfile = removeSuffix(exportFor, outputFile);

			// TODO this should probably be an exportVersion check?
			boolean useISO8601dateFormat = targetDatabaseVersion >= 24;

			SampleGroupChoice sampleGroupChoice = SampleGroupChoice.create(sampleGroup.getSampleGroupId());
			TrialDetailsExporter exporter = new TrialDetailsExporter(
			        sampleGroupChoice,
					trial.getTrialId(), 
					trial.getTrialPlantingDate(), 
					outfile.getParentFile(), 
					outfile.getName(), 
					exportFor, 
					true, // emitDownloadedId
					useISO8601dateFormat, // useISOdateFormat 
					exportHelper);

			SimpleFileExportOutputHelper exportOutputHelper = new SimpleFileExportOutputHelper(outfile);

			UnscoredSampleEmitter unscoredSampleEmitter = new DefaultUnscoredSampleEmitter();

			android.util.Pair<File, Long> exportResult = exporter.exportTrialDetails(
			        exportOutputHelper, unscoredSampleEmitter);

			exported = exportResult.first;

			List<String> errorKeys = unscoredSampleEmitter.getMultiplyOccurringKeys();
			if (! errorKeys.isEmpty()) {
			    MsgBox.warn(SampleGroupExportDialog.this, 
	                    HelpUtils.makeListInScrollPane(errorKeys, Function.identity()),
	                    "Internal Error - Duplicate Samples");
			    exported = null;
			}
		} catch (IOException e) {
			GuiUtil.errorMessage(SampleGroupExportDialog.this, e);
		}
		return exported;
	}
	
	class SimpleFileExportOutputHelper implements ExportOutputHelper<File, File> {

		private final File outputFolder;
		private final String fileName;
		private File lastOutfile;

		SimpleFileExportOutputHelper(File outfile) {
			this.outputFolder = outfile.getParentFile();
			this.fileName = outfile.getName();
		}
		
		@Override
		public android.util.Pair<File, CsvWriter> createOutputCsvFileAndWriterForPlot() throws IOException {
	    	String name = fileName.toLowerCase().endsWith(".csv")
	    			? fileName
	    			: fileName + ".csv";
	        File outfile = new File(outputFolder, name);
	        CsvWriter csvWriter = new CsvWriterImpl(new FileWriter(outfile));
	        return new android.util.Pair<>(outfile,csvWriter);
		}

	    @Override
	    public android.util.Pair<File[], CsvWriter[]> createOutputCsvFileAndWriterForPlotAndSpecimen() throws IOException {
	        String plotFileName;
	        String specimenFileName;

	        if (fileName.toLowerCase().endsWith(".csv")) {
	            plotFileName = fileName;
	            specimenFileName = fileName.substring(0, fileName.length() - 4)  + SUB_PLOT_SUFFIX + ".csv";
	        }
	        else {
	            plotFileName = fileName + ".csv";
	            specimenFileName = fileName  + SUB_PLOT_SUFFIX + ".csv";
	        }

	        File plotOutputFile = FileUtility.constructOutputFileWithoutOverwriting(new File(outputFolder,plotFileName));
	        File specimenOutputFile = FileUtility.constructOutputFileWithoutOverwriting(new File(outputFolder, specimenFileName));

	        File files [] = new File[2];
	        files[0] = plotOutputFile;
	        files[1] = specimenOutputFile;

	        lastOutfile = plotOutputFile;
	        CsvWriter plotCsvWriter  = new CsvWriterImpl(new FileWriter(plotOutputFile));
	        CsvWriter specimenCsvWriter = new CsvWriterImpl(new FileWriter(specimenOutputFile));

	        CsvWriter [] csvWriters = new CsvWriter[2];
	        csvWriters[0]= plotCsvWriter;
	        csvWriters[1]= specimenCsvWriter;

	        android.util.Pair<File[], CsvWriter[]>  pair = new android.util.Pair<>(files,csvWriters);

	        return pair;
	    }

		@Override
		public File createOutputZipFile() throws IOException {
	        String zipFilename = fileName.toLowerCase().endsWith(".zip")
	        		? fileName
	        		: fileName + ".zip";
	        return new File(outputFolder, zipFilename);
		}

		@Override
		public File createOutputKdxchangeFile() {
	        String kdxFilename = fileName.toLowerCase().endsWith(ExportFor.KDX_SUFFIX)
	        		? fileName
	        		: fileName + ExportFor.KDX_SUFFIX;
	        return new File(outputFolder, kdxFilename);
		}

		@Override
		public OutputStream getOutputStream(File file) throws IOException {
	        return new FileOutputStream(file);
		}

		@Override
		public String getName(File docFile) {
	        return docFile.getName();
		}
		
		@Override
		public void close() throws IOException {
	        // No-op
		}
	}

	private File removeSuffix(ExportFor exportFor, File outfile) {
		// Remove suffix because it will be added later by the FileExportHelper
		String suffix = null;
		switch (exportFor) {
		case KDSMART_CSV_FULL:
		case KDSMART_CSV_PLOTS_ONLY:
			suffix = ".csv";
			break;
		case KDSMART_ZIP:
			suffix = ".zip";
			break;
		case KDXPLORE:
			suffix = ExportFor.KDX_SUFFIX;
			break;
		default:
			break;
		}
		
		if (suffix != null) {
			String filename = outfile.getName();
			if (filename.toLowerCase().endsWith(suffix)) {
				outfile = new File(outfile.getParentFile(),
						filename.substring(0, filename.length() - suffix.length()));
			}
		}
		
		return outfile;
	}
	
	private File doExportAsWorkpackageJson(
			boolean needElapsedDaysAsDate,
			SampleGroup sampleGroup,
			String filename, 
			File outfile) 
	{
		try {
			WorkPackage wp = WorkPackageFactory.createWorkPackageForSampleGroup(
					kdxploreDatabase, 
					needElapsedDaysAsDate,
					trial.getTrialId(), 
					sampleGroup.getSampleGroupId(),
					false /* wantMediaFiles */);

			Gson gson = JsonUtil.newGsonBuilder(TraitValue.MEASURED_DATE_TIME_ISO_8601_FORMAT)
					.setPrettyPrinting()
					.create();
			String json = gson.toJson(wp);
			
			PrintWriter pw = new PrintWriter(outfile);
			pw.println(json);
			pw.close();
			
			return outfile;
			
		} catch (IOException e1) {
		    e1.printStackTrace();
			MsgBox.error(SampleGroupExportDialog.this, 
					filename +  "\n" + e1.getMessage(),
					"Failed: " + getTitle());
		}
		
		return null;
	}


	class MyTrialExportHelper implements TrialExportHelper {
		
		private final KdxploreDatabase kdxploreDatabase;
		private final KDSmartDatabase kdsmartDatabase;

		private final int targetDatabaseVersion;
		private final int sampleGroupId;
		private final boolean wantMediaFiles;
		private final int kdsmartVersionCode;
		private final Set<Integer> traitIds;
		
		MyTrialExportHelper(KdxploreDatabase kdxdb, 
				int targetDatabaseVersion,
				SampleGroup sampleGroup, 
				boolean wantMediaFiles) 
		{
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
			        SampleGroupChoice.create(sampleGroup.getSampleGroupId()), 
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
				Plot plot, Closure<android.util.Pair<WhyMissing, String>> missingFileConsumer)
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
	
	static public class KdxSampleVisitor implements TrialItemVisitor<Sample> {

        private List<Pair<Field,Field>> fromTo;

	    private final TrialItemVisitor<Sample> wrapped;

        private final Set<Integer> traitIds;
	    
	    KdxSampleVisitor(Set<Integer> traitIds, TrialItemVisitor<Sample> wrapped) {
	        this.traitIds = traitIds;
	        this.wrapped = wrapped;
	    }

        @Override
        public void setExpectedItemCount(int count) {
            wrapped.setExpectedItemCount(count);
        }

        @Override
        public boolean consumeItem(Sample sample) throws IOException {
            boolean result = true;

            if (traitIds.contains(sample.getTraitId())) {
                
                SampleImpl tmp;
                
                if (sample instanceof SampleImpl) {
                    tmp = (SampleImpl) sample;
                }
                else {
                    if (fromTo==null) {
                        fromTo = buildFromTo(sample);
                    }

                    // The wrapped visitor expects a SampleImpl
                    tmp = new SampleImpl();
                    for (Pair<Field,Field> pair : fromTo) {
                        try {
                            Object value = pair.first.get(sample);
                            pair.second.set(tmp, value);
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            throw new IOException(e);
                        }                   
                    }
                }

                result = wrapped.consumeItem(tmp);
            }
            return result;
        }
	    
     // Can't build the map until we get the first concrete Sample so we then know the class.
        private List<Pair<Field, Field>> buildFromTo(Sample sample) {
            List<Pair<Field,Field>> fromTo = new ArrayList<>();

            Map<String,Field> toFieldByName = new HashMap<>();
            Class<?> to_cls = SampleImpl.class;
            while (to_cls != Object.class) {
                for (Field  to : to_cls.getDeclaredFields()) {
                    if (Modifier.isStatic(to.getModifiers())) {
                        continue;
                    }
                    toFieldByName.put(to.getName(), to);
                }
                to_cls = to_cls.getSuperclass();
            }
            
            Class<?> from_cls = sample.getClass();
            while (from_cls != Object.class) {
                for (Field from : from_cls.getDeclaredFields()) {
                    if (Modifier.isStatic(from.getModifiers())) {
                        continue;
                    }
                    
                    Field to = toFieldByName.get(from.getName());
                    if (to != null) {
                        from.setAccessible(true);
                        to.setAccessible(true);
                        
                        fromTo.add(new Pair<>(from, to));
                    }
                }               
                from_cls = from_cls.getSuperclass();
            }
            
            return fromTo;
        }
	}
}
