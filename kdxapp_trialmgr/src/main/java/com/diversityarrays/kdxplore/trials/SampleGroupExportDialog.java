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
import java.io.IOException;
import java.io.PrintWriter;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitLevel;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.ormlite.KDSmartDatabaseUpgrader;
import com.diversityarrays.kdsmart.db.util.ExportFor;
import com.diversityarrays.kdsmart.db.util.JsonUtil;
import com.diversityarrays.kdsmart.db.util.OmittedEntities;
import com.diversityarrays.kdsmart.db.util.SampleOrder;
import com.diversityarrays.kdsmart.db.util.TrialDetailsExporter;
import com.diversityarrays.kdsmart.db.util.TrialExportHelper;
import com.diversityarrays.kdsmart.db.util.UnscoredSampleEmitter;
import com.diversityarrays.kdsmart.db.util.Util;
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
import com.diversityarrays.util.BoxBuilder;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.StrikeThroughCheckBox;
import com.google.gson.Gson;

import net.pearcan.ui.DefaultBackgroundRunner;
import net.pearcan.ui.GuiUtil;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.GBH;

public class SampleGroupExportDialog extends JDialog {

    private static final String TTT_DATABASE_VERSION_FOR_EXPORT = "Choose version for KDSmart compatibility";

    private static final int MIN_DB_VERSION_FOR_KDSMART_3 = 30;
    
	enum OutputOption {
		KDX(ExportFor.KDXPLORE, "KDX (for KDSmart)"),
		ZIP(ExportFor.KDSMART_ZIP, "ZIP"),
		CSV_FULL(ExportFor.KDSMART_CSV_FULL, "CSV (Plots & Individuals)"),
		CSV_PLOTS(ExportFor.KDSMART_CSV_PLOTS_ONLY, "CSV"),
		JSON(null, "JSON");

		public final ExportFor exportFor;
		public final String displayName;
		OutputOption(ExportFor ef, String s) {
			exportFor = ef;
			displayName = s;
		}

		public boolean supportsMediaFiles() {
		    return (this==KDX) || (this==ZIP);
		}

        public boolean usesWorkPackage() {
            return this==KDX;
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

	private final DefaultComboBoxModel<Integer> dbVersionModel = new DefaultComboBoxModel<>(
            new Integer[] { WorkPackageFactory.MAX_DBVERSION_FOR_KDSMART_2 });
    private final JComboBox<Integer> databaseVersionChoices = new JComboBox<>(dbVersionModel);
    private final JLabel dbVersionLabel = new JLabel("DB Version");

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

	private final JCheckBox wantMediaFilesOption = new StrikeThroughCheckBox("Include Media Files", true);
	private final JCheckBox oldKdsmartOption = new StrikeThroughCheckBox("Old KDSmart Compat", false);
    private final JCheckBox kdsmartVersion3option = new StrikeThroughCheckBox("For KDSmart-v3", false);

	private final List<JComponent> exportForOptions = Arrays.asList(
	        kdsmartVersion3option, wantMediaFilesOption, oldKdsmartOption);

	private final Trial trial;
	private final KdxploreDatabase kdxploreDatabase;
	private boolean haveCollectedSamples;
	final SampleGroup sampleGroup;

	private final DefaultBackgroundRunner backgroundRunner = new DefaultBackgroundRunner();
	protected OutputOption selectedOutputOption;

	private Set<Integer> excludeTheseTraitIds;
	
	private final Set<Integer> excludeTheseSampleIds = new HashSet<>();
	
	private final Set<Integer> excludeThesePlotIds;

	private final Map<Integer,Trait> allTraits;
	
	public SampleGroupExportDialog(Window owner,
	        String title,
			Trial trial,
			KdxploreDatabase kdxploreDatabase,
			DeviceType deviceType, DeviceIdentifier devid,
			SampleGroup sampleGroup,
			Set<Integer> excludeTheseTraitIds,
			Map<Integer, Trait> allTraitIds, 
			Set<Integer> excludeThesePlotIds
			)
	{
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setGlassPane(backgroundRunner.getBlockingPane());

		this.allTraits = allTraitIds;
		this.trial = trial;
		this.kdxploreDatabase = kdxploreDatabase;
		this.sampleGroup = sampleGroup;
		this.excludeTheseTraitIds = excludeTheseTraitIds;
		this.excludeThesePlotIds = excludeThesePlotIds;

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
				    if (comp == wantMediaFilesOption) {
				        comp.setEnabled(enb && selectedOutputOption.supportsMediaFiles());
				    }
				    else if (comp == kdsmartVersion3option) {
				        comp.setEnabled(enb && selectedOutputOption.usesWorkPackage());
				    }
				    else {
				        comp.setEnabled(enb);
				    }
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
        	if (OutputOption.KDX==oo) {
        		kdxExportButton = rb;
        	}
        	
        	if (OutputOption.ZIP==oo) {
        		zipExportButton = rb;
        	}
        	
        	rb.addActionListener(new ActionListener() {
        		@Override
        		public void actionPerformed(ActionEvent e) {
        			exportExclusionBox.selectAndDeactivateButtons(kdxExportButton.isSelected() 
        					|| zipExportButton.isSelected());
        		}					
        	});
			
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

		Box additionalOptionsBox = Box.createHorizontalBox();
		additionalOptionsBox.add(this.wantMediaFilesOption);
		additionalOptionsBox.add(this.kdsmartVersion3option);
        
		dbVersionLabel.setToolTipText(TTT_DATABASE_VERSION_FOR_EXPORT);
		databaseVersionChoices.setToolTipText(TTT_DATABASE_VERSION_FOR_EXPORT);

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
		
		gbh.add(0,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, exportExclusionBox);
		++y;

		gbh.add(0,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, additionalOptionsBox);
		++y;
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, dbVersionLabel);
		gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, BoxBuilder.horizontal().add(databaseVersionChoices).get());

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

	private JRadioButton kdxExportButton;
	
	private JRadioButton zipExportButton;
	
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

	    String filename = filepathText.getText().trim();

		OmittedEntities entities = exportExclusionBox.createOmittedObject(
				this.excludeThesePlotIds,
				this.excludeTheseTraitIds, 
				this.excludeTheseSampleIds);
	    
		this.excludeTheseTraitIds = entities.omittedTraits;
		
	    final Integer maxDatabaseVersionForWorkPackage = kdsmartVersion3option.isSelected() ? 
	    		WorkPackageFactory.MAX_DBVERSION_FOR_KDSMART_2 :
	    	MIN_DB_VERSION_FOR_KDSMART_3;

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
	                        	 boolean suppressed = ((KdxSample) s).isSuppressed();
	                        	 if (suppressed) {
	                        		 excludeTheseSampleIds.add(s.getSampleId());
	                        		 return true;
	                        	 }
	                        	
	                            if (! excludeTheseTraitIds.contains(s.getTraitId()) 
	                            		&& ! excludeThesePlotIds.contains(s.getPlotId())) {
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
			    
			    
			    return doExport(filename, maxDatabaseVersionForWorkPackage, entities);
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

	private final ExportExclusionBox exportExclusionBox = new ExportExclusionBox();

	private File doExport(String filename, Integer maxDatabaseVersionForWorkPackage, OmittedEntities entities) {

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
				targetDatabaseVersion = ! this.kdsmartVersion3option.isSelected() ? 
						(int) databaseVersionChoices.getSelectedItem() : 
							kdxploreDatabase.getKDXploreKSmartDatabase().getDatabaseVersion();
			}

			boolean wantMediaFiles = wantMediaFilesOption.isSelected();
			
			successIfNotNull = doExportAsNonJsonFile(sampleGroup,
					targetDatabaseVersion,
					wantMediaFiles,
					exportFor,
					outfile,
					entities);
		}
		else {
			// Ensure .json suffix
			String suffix = ".json";
			if (! filename.toLowerCase().endsWith(suffix)) {
				outfile = new File(filename + suffix);
			}

			boolean needElapsedDaysAsDate = compatibleWithOldKdsmart;

			int databaseVersionForExport = (Integer) databaseVersionChoices.getSelectedItem();

            successIfNotNull = doExportAsWorkpackageJson(
			        databaseVersionForExport,
			        needElapsedDaysAsDate, sampleGroup, filename, outfile,
			        maxDatabaseVersionForWorkPackage);
		}

		return successIfNotNull;
	}

	private File doExportAsNonJsonFile(SampleGroup sampleGroup,
			int targetDatabaseVersion,
			boolean wantMediaFiles,
			ExportFor exportFor,
			File outputFile, 
			OmittedEntities entities)
	{
	    File exported = null;
		try {
			TrialExportHelper exportHelper = new MyTrialExportHelper(
					this, kdxploreDatabase,
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
					exportHelper,
					entities);

			SimpleFileExportOutputHelper exportOutputHelper = new SimpleFileExportOutputHelper(outfile);

			UnscoredSampleEmitter unscoredSampleEmitter = new DefaultUnscoredSampleEmitter();

			Pair<File, Long> exportResult = exporter.exportTrialDetails(
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
	        int databaseVersionForExport,
			boolean needElapsedDaysAsDate,
			SampleGroup sampleGroup,
			String filename,
			File outfile,
			Integer maxDbVersionForWorkPackage)
	{
		try {
			WorkPackage wp = WorkPackageFactory.createWorkPackageForSampleGroup(
			        databaseVersionForExport,
					kdxploreDatabase,
					needElapsedDaysAsDate,
					trial.getTrialId(),
					sampleGroup.getSampleGroupId(),
					false /* wantMediaFiles */,
					maxDbVersionForWorkPackage);

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
}
