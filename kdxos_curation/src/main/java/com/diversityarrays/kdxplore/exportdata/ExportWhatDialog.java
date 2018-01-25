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
package com.diversityarrays.kdxplore.exportdata;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import com.diversityarrays.kdsmart.db.csvio.CsvImportDefinition;
import com.diversityarrays.kdsmart.db.entities.PlotIdentSummary;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.Shared.For;
import com.diversityarrays.kdxplore.exportdata.ExportOptions.PlotAttrAsNumber;
import com.diversityarrays.kdxplore.importdata.bms.BmsConstant;
import com.diversityarrays.kdxplore.prefs.ExportFileType;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.kdxplore.trialmgr.TrialManagerPreferences;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.RunMode;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.GBH;
import net.pearcan.util.StringUtil;

// TODO i18n
@SuppressWarnings("nls")
public class ExportWhatDialog extends JDialog {
	
	static enum HowToExportInactivePlots {
		DONT_EXPORT("Don't export"),
		AS_MISSING(TraitValue.EXPORT_VALUE_MISSING),
		AS_NA(TraitValue.EXPORT_VALUE_NA),
		AS_STRING("Specify:");
		
		public final String display;
		HowToExportInactivePlots(String d) {
			this.display = d;
		}
	}
	
	private static final String PLOTATTR_TTT = "<HTML>Determines which <i>Plot Attribute</i> values will be output as Excel numbers";
	private final List<Integer> selectedModelRows;
	private final int rowCount;
	
	private final FileDrop fileDrop = new FileDrop() {		
		@Override
		public void dropFiles(Component component, List<File> files, DropLocationInfo dli) {
			File foundFile = null;
			File foundDir = null;
			for (File f : files) {
				if (f.isDirectory()) {
					if (foundDir == null) {
						foundDir = f;
					}
				}
				else if (f.isFile()) {
					if (foundFile == null) {
						String loname = f.getName().toLowerCase();
						for (ExportFileType eft : ExportFileType.values()) {
							if (loname.endsWith(eft.suffix)) {
								foundFile = f;
								break;
							}
						}
					}
				}
			}

			if (foundFile != null) {
				outputPath.setText(foundFile.getPath());
			}
			else if (foundDir != null) {
				outputPath.setText(new File(foundDir, "export.csv").getPath());
			}
		}
	};
	
	private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);
	
	private JFileChooser chooser;
	private final PromptTextField outputPath = new PromptTextField("Output File (drag/drop file or folder)");
	
	private final FileFilter DIR_CSV_OR_XLS = new FileFilter() {
        @Override
        public String getDescription() {
            return "CSV files or Excel Files";
        }

        @Override
        public boolean accept(File f) {
            if (! f.isFile()) {
                return true;
            }
            String loname = f.getName().toLowerCase();
            for (ExportFileType eft : ExportFileType.values()) {
                if (loname.endsWith(eft.suffix)) {
                    return true;
                }
            }
            return false;
        }
	};

	private final Action browseAction = new AbstractAction("Choose...") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (chooser == null) {
				chooser = Shared.getFileChooser(For.LOAD_FILE_OR_DIR, DIR_CSV_OR_XLS);
			}

			if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(ExportWhatDialog.this)) {
				File file = chooser.getSelectedFile();
				if (file != null) {
					if (file.isDirectory()) {
						String trialName = trial.getTrialName();
						String filename = com.diversityarrays.kdsmart.db.util.Util.getTimestampedOutputFileName(trialName, "");
//						String filename = com.diversityarrays.kdsmart.db.util.Util.removeInvalidFilenameCharacters(trialName);
						
						ExportFileType eft = ( excelFormatOption.isEnabled() && excelFormatOption.isSelected() )
								? ExportFileType.XLS // BMS only does XLS, not XLSX
								: ExportFileType.CSV;
						outputPath.setText(new File(file, filename + eft.suffix).getPath());
					}
					else {
						outputPath.setText(file.getPath());
					}
				}
			}
		}		
	};
	
	private final Action okAction = new AbstractAction("Save") {

		@Override
		public void actionPerformed(ActionEvent e) {
			exportOptions = new ExportOptions();
			exportOptions.file = new File(outputPath.getText().trim());
			
			exportOptions.colourOutputCells = cboxColourOutputCells.isSelected();
			exportOptions.includeDateTimeMeasured = cboxIncludeDateTime.isSelected();
			
			switch (howToExportInactive) {
			case AS_MISSING:
				exportOptions.exportInactiveTraitValue = TraitValue.EXPORT_VALUE_MISSING;
				break;
			case AS_NA:
				exportOptions.exportInactiveTraitValue = TraitValue.EXPORT_VALUE_NA;
				break;
			case AS_STRING:
				exportOptions.exportInactiveTraitValue = valueForInactivePlotSamples.getText();
				break;
			case DONT_EXPORT:
				exportOptions.exportInactiveTraitValue = null;
				break;
			default:
				break;			
			}
			
			exportOptions.allPlotAttributes = rbAllPlotAttributes.isSelected();
			exportOptions.whichTraitInstances = whichTraitInstances;

			exportOptions.modelRows = getModelRows(rbExportAllPlots.isSelected());
			
			if (nameForColumn != null) {
				exportOptions.nameForColumn = nameForColumn.getText().trim();
			}
			if (nameForRow != null) {
				exportOptions.nameForRow = nameForRow.getText().trim();
			}
			
			exportOptions.excelFormat = excelFormatOption.isEnabled() && excelFormatOption.isSelected();
			exportOptions.plotAttributeAsNumber = (PlotAttrAsNumber) plotAttrAsNumberCombo.getSelectedItem();
			exportOptions.showTrialName = showTrialNameOption.isSelected();
			
			exportOptions.unscoredValueString = unscoredValue.getText().trim();
			exportOptions.naValueString = naValue.getText().trim();
			exportOptions.missingValueString = missingValue.getText().trim();
			exportOptions.suppressedValueString = suppressedValue.getText().trim();
			
			// TODO move this to KdxplorePreferences
			KdxplorePreferences.getInstance().saveOutputDirectory(exportOptions.file.getParentFile());
			String filename = exportOptions.file.getName();
			int pos = filename.lastIndexOf('.');
			if (pos > 0) {
				String sfx = filename.substring(pos);
				ExportFileType eft = ExportFileType.lookupBySuffix(sfx);
				if (eft != null) {
					TrialManagerPreferences.getInstance().saveLastExportFileType(eft);
				}
			}
			dispose();
		}
		
	};
	
	private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {

		@Override
		public void actionPerformed(ActionEvent e) {
			exportOptions = null;
			dispose();
		}
		
	};
	
	private WhichTraitInstances whichTraitInstances = WhichTraitInstances.ALL_WITH_DATA;
	public ExportOptions exportOptions;
	
	private final JCheckBox showTrialNameOption = new JCheckBox("Column for Trial Name");
	private final JCheckBox excelFormatOption = new JCheckBox("Excel Format", false);
	
	private final JComboBox<ExportOptions.PlotAttrAsNumber> plotAttrAsNumberCombo = 
			new JComboBox<ExportOptions.PlotAttrAsNumber>(PlotAttrAsNumber.values());

	private final JRadioButton rbExportAllPlots = new JRadioButton("All Plots", true);
	private final JRadioButton rbExportSelectedPlots = new JRadioButton("Selected Plots");
	
	private final JCheckBox cboxColourOutputCells = new JCheckBox("Colour Excel Output");
	private final JCheckBox cboxIncludeDateTime = new JCheckBox("Output Date/Time Measured");

	private final JRadioButton rbAllPlotAttributes = new JRadioButton("All", true);
	private final JRadioButton rbSelectedPlotAttributes = new JRadioButton("Only Selected");

	private final PromptTextField unscoredValue = new PromptTextField("Value for unscored data");
	private final PromptTextField naValue = new PromptTextField("Value for NA");
	private final PromptTextField missingValue = new PromptTextField("Value for missing data");
	private final PromptTextField suppressedValue= new PromptTextField("Value for suppressed data");
	
	private final Trial trial;
	private final Action resetAction = new AbstractAction("Use/Show Defaults") {
		@Override
		public void actionPerformed(ActionEvent e) {
			unscoredValue.setText(TraitValue.EXPORT_VALUE_UNSCORED);
			naValue.setText(TraitValue.EXPORT_VALUE_NA);
			missingValue.setText(TraitValue.EXPORT_VALUE_MISSING);
			suppressedValue.setText("");
		}		
	};
	private PromptTextField nameForColumn;
	private PromptTextField nameForRow;
	private Action trialNamesAction = new AbstractAction("Use Names from Trial") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (nameForColumn != null) {
				nameForColumn.setText(trial.getNameForColumn());
			}
			if (nameForRow != null) {
				nameForRow.setText(trial.getNameForRow());
			}
		}
		
	};
	private DocumentListener nameForRowColListener = new DocumentListener() {
		@Override
		public void removeUpdate(DocumentEvent e) {
			checkNameForRowOrColumn(null);
		}		
		@Override
		public void insertUpdate(DocumentEvent e) {
			checkNameForRowOrColumn(null);
		}
		@Override
		public void changedUpdate(DocumentEvent e) {
			checkNameForRowOrColumn(null);
		}
	};
	
	private final JLabel warning = new JLabel();
	
	private String previousOutputFilePath;
	
	private HowToExportInactivePlots howToExportInactive = HowToExportInactivePlots.DONT_EXPORT;
	private final PromptTextField valueForInactivePlotSamples = new PromptTextField("Enter value");

	public ExportWhatDialog(Window owner, String title, Trial trial, 
			int rowCount, 
			List<Integer> selectedModelRows,
			int selectedTraitInstanceCount,
			int selectedPlotAttributeCount) 
	{
		super(owner, title, ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.trial = trial;
		this.rowCount = rowCount;
		this.selectedModelRows = selectedModelRows;
		Collections.sort(selectedModelRows);
		
		outputPath.setTransferHandler(flth);
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(rbExportAllPlots);
		bg.add(rbExportSelectedPlots);

		bg = new ButtonGroup();
		bg.add(rbAllPlotAttributes);
		bg.add(rbSelectedPlotAttributes);

		Map<JRadioButton,WhichTraitInstances> whichByRb = new LinkedHashMap<>();
		ActionListener whichRbListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                whichTraitInstances = whichByRb.get(e.getSource());
            }
        };
        bg = new ButtonGroup();

        Box traits = Box.createHorizontalBox();        
		for (WhichTraitInstances wti : WhichTraitInstances.values()) {
		    if (selectedTraitInstanceCount <= 0 && WhichTraitInstances.SELECTED == wti) {
		        continue;
		    }
		    JRadioButton rb = new JRadioButton(wti.toString(), wti == whichTraitInstances);
		    rb.addActionListener(whichRbListener);
		    bg.add(rb);
		    
		    traits.add(rb);
		}
		
		outputPath.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				previousOutputFilePath = null;
				updateOkAction();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				previousOutputFilePath = null;
				updateOkAction();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				previousOutputFilePath = null;
				updateOkAction();
			}
		});
		updateOkAction();
		
		TrialManagerPreferences preferences = TrialManagerPreferences.getInstance();
		
		ExportFileType eft = preferences.getLastExportFileType();
		if (eft == null) {
		    eft = preferences.getLastExportFileTypeDefaultValue();
		}
        String filename = com.diversityarrays.kdsmart.db.util.Util.getTimestampedOutputFileName(trial.getTrialName(), null)
        		+ eft.suffix;
        File outdir = KdxplorePreferences.getInstance().getOutputDirectory();
        if (outdir == null) {
        	outdir = FileSystemView.getFileSystemView().getDefaultDirectory();
        }
        outputPath.setText(new File(outdir, filename).getPath());
		
		JPanel panel = new JPanel();
		GBH gbh = new GBH(panel, 2,4,2,4);
		int y = 0;
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Save To:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, outputPath);
		gbh.add(2,y, 1,1, GBH.NONE, 0,1, GBH.WEST, new JButton(browseAction));
		++y;

		showTrialNameOption.setToolTipText("The first column in Measurements will be the Trial Name");
		excelFormatOption.setSelected(false);

		Box optionsBox = Box.createHorizontalBox();
		optionsBox.add(showTrialNameOption);
		
		JLabel lbl = new JLabel("Plot Attributes as Number:");
		lbl.setToolTipText(PLOTATTR_TTT);
		plotAttrAsNumberCombo.setToolTipText(PLOTATTR_TTT);
		optionsBox.add(Box.createHorizontalStrut(10));
		optionsBox.add(lbl);
		optionsBox.add(plotAttrAsNumberCombo);
		// NOTE: forcing false
		if (RunMode.getRunMode().isDeveloper()) {
			optionsBox.add(Box.createHorizontalStrut(10));
			optionsBox.add(excelFormatOption);
			excelFormatOption.setToolTipText("<HTML>" + "a KDXplore Excel file");
			excelFormatOption.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					handleExcelFormatOptionFormatChanged();;
				}
			});
		}
		optionsBox.add(Box.createHorizontalGlue());
		gbh.add(0,y, 3,1, GBH.NONE, 1,1, GBH.WEST, optionsBox);
		++y;
		
		if (RunMode.getRunMode().isDeveloper()) {
			Box two = Box.createHorizontalBox();
			two.add(cboxColourOutputCells);
			two.add(cboxIncludeDateTime);
			gbh.add(0,y, 3,1, GBH.NONE, 1,1, GBH.WEST, two);
			++y;
		}
		
		PlotIdentSummary plotIdentSummary = trial.getPlotIdentSummary();
		boolean hasX = ! plotIdentSummary.xColumnRange.isEmpty();
		boolean hasY = ! plotIdentSummary.yRowRange.isEmpty();
		if (hasX) {
			nameForColumn = new PromptTextField("default is '" + CsvImportDefinition.HEADING_COLUMNX + "'");
			nameForColumn.getDocument().addDocumentListener(nameForRowColListener);
			
			gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Heading for '" + trial.getNameForPlot() + "' X axis:");
			gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, nameForColumn);
			
			gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, new JButton(trialNamesAction));
			++y;
		}
		if (hasY) {
			nameForRow = new PromptTextField("default is '" + trial.getNameForRow() + "'");
			nameForRow.getDocument().addDocumentListener(nameForRowColListener);

			gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Heading for '" + trial.getNameForPlot() + "' Y axis:");
			gbh.add(1,y, 1,1, GBH.HORZ, 2,1, GBH.CENTER, nameForRow);

			if (! hasX) {
				gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.CENTER, new JButton(trialNamesAction));
			}
			++y;
		}

		rbExportSelectedPlots.setEnabled(! selectedModelRows.isEmpty());
		if (! selectedModelRows.isEmpty()) {
			rbExportSelectedPlots.setText(MessageFormat.format("Selected Plots ({0,number,integer})", selectedModelRows.size()));
		}
		// This lets you just output data for selected plots
		Box plots = Box.createHorizontalBox();
		plots.add(rbExportAllPlots);
		plots.add(rbExportSelectedPlots);
		plots.add(Box.createHorizontalGlue());
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Plots:");
		gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, plots);
		++y;
		
		// - - - - -

		valueForInactivePlotSamples.setEnabled(false);
		Map<JRadioButton, HowToExportInactivePlots> howtoByRadioButton = new HashMap<>();
		ActionListener rbListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				howToExportInactive = howtoByRadioButton.get(e.getSource());		
				valueForInactivePlotSamples.setEnabled(HowToExportInactivePlots.AS_STRING==howToExportInactive);
			}
		};
		
		ButtonGroup ipo_bg = new ButtonGroup();
		Box inactivePlotOptions = Box.createHorizontalBox();
		for (HowToExportInactivePlots how : HowToExportInactivePlots.values()) {
			JRadioButton rb = new JRadioButton(how.display, how == howToExportInactive);
			ipo_bg.add(rb);
			rb.addActionListener(rbListener);
			howtoByRadioButton.put(rb, how);
			
			inactivePlotOptions.add(rb);
		}
		inactivePlotOptions.add(valueForInactivePlotSamples);
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Inactive Plot Values:");
		gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, inactivePlotOptions);
		++y;
		// - - - - -
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Plot Attributes:");
		if (selectedPlotAttributeCount <= 0) {
			gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, "ALL");
		}
		else {
			Box plotAtts = Box.createHorizontalBox();
			plotAtts.add(rbAllPlotAttributes);
			plotAtts.add(rbSelectedPlotAttributes);
			plotAtts.add(Box.createHorizontalGlue());
			
			gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, plotAtts);
		}
		++y;

		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Trait Instances:");
        gbh.add(1,y, 2,1, GBH.HORZ, 1,1, GBH.CENTER, traits);
		++y;
		
		gbh.add(1,y, 1,1, GBH.HORZ, 0,1, GBH.CENTER, GuiUtil.createLabelSeparator("Show Scores As:"));
		gbh.add(2,y, 1,1, GBH.NONE, 1,1, GBH.WEST, new JButton(resetAction));
		++y;
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Unscored values:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, unscoredValue);
		++y;
		
		gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "NA values:");
		gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, naValue);
		++y;
		
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Missing values:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, missingValue);
        ++y;
        
        gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Suppressed values:");
        gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, suppressedValue);
        ++y;
        
		warning.setForeground(Color.RED);
		gbh.add(0,y, 3,1, GBH.HORZ, 1,1, GBH.CENTER, warning);
		++y;
		
		Box buttons = Box.createHorizontalBox();
		buttons.add(new JButton(cancelAction));
		buttons.add(new JButton(okAction));
		buttons.add(Box.createHorizontalGlue());
		
		Container cp = getContentPane();
		cp.add(panel, BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);
		pack();
		
		setSize(800, 480);
	}
	

	private void handleExcelFormatOptionFormatChanged() {
		String defaultNameForColumn = CsvImportDefinition.HEADING_COLUMNX;
		String defaultNameForRow = CsvImportDefinition.HEADING_ROWY;
		
		if (excelFormatOption.isSelected()) {
//			namesPanel.setVisible(false);
			new Toast(excelFormatOption, "Excel Format not yet enabled completely", Toast.SHORT).show();
			defaultNameForColumn = BmsConstant.XLSHDG_FIELDMAP_COLUMN;
			defaultNameForRow = BmsConstant.XLSHDG_FIELDMAP_RANGE;
			
			String path = outputPath.getText().trim();
			if (! path.isEmpty()) {
				if (previousOutputFilePath == null) {
					previousOutputFilePath = path;
				}
				File tmp = new File(path);
				String loname = tmp.getName().toLowerCase();
				int pos = loname.lastIndexOf('.');
				if (pos > 0) {
					String sfx = loname.substring(pos);
					if (! ".xls".equals(sfx)) {
						tmp = new File(tmp.getParentFile(), loname.substring(0, pos) + ".xls");
						outputPath.setText(tmp.getPath());
					}
				}
				else {
					outputPath.setText(path + ".xls");
				}
			}
		}
		else if (previousOutputFilePath != null) {
//			namesPanel.setVisible(true);

			outputPath.setText(previousOutputFilePath);
			previousOutputFilePath = null;
		}
		
		if (nameForColumn != null) {
			nameForColumn.setPrompt("default is '" + defaultNameForColumn + "'");
		}
		if (nameForRow != null) {
			nameForRow.setPrompt("default is '" + defaultNameForRow + "'");
		}
	}

	private void checkNameForRowOrColumn(String s) {
		List<String> lines = new ArrayList<>();
		if (s != null) {
			lines.add(s);
		}
		
		boolean n4cmsg = false;
		if (nameForColumn != null) {
			if (! nameForColumn.getText().trim().isEmpty()) {
				n4cmsg = true;
				lines.add("Non-default name for Column may require manual import");
			}
		}
		if (nameForRow != null && ! n4cmsg) {
			if (! nameForRow.getText().trim().isEmpty()) {
				lines.add("Non-default name for Row may require manual import");
			}
		}		

		if (lines.isEmpty()) {
			warning.setText("");
		}
		else {
			warning.setText(StringUtil.join("\n", lines));
		}
	}

	protected void updateOkAction() {
		String s = outputPath.getText().trim();
		if (s.isEmpty()) {
			checkNameForRowOrColumn("Please provide an output file");
			okAction.setEnabled(false);
		}
		else if (excelFormatOption.isSelected() && ! endsWithExcelSuffix(s)) {
			okAction.setEnabled(false);
			checkNameForRowOrColumn("Excel format requires .xls or .xlsx files");
		}
		else {
			checkNameForRowOrColumn(null);
			okAction.setEnabled(true);
		}
	}

//	public boolean getAllTraits() {
//		return allTraits.isSelected();
//	}
	
	private boolean endsWithExcelSuffix(String s) {
        String low_s = s.toLowerCase();
        return low_s.endsWith(".xls") || low_s.endsWith(".xlsx");
    }


    private Iterable<Integer> getModelRows(boolean all) {
		if (all) {
			return new Iterable<Integer>() {
				final int nRows = rowCount;
				@Override
				public Iterator<Integer> iterator() {
					return new Iterator<Integer>() {
						int row = 0;

						@Override
						public Integer next() {
							if (row < nRows) {
								return row++;
							}
							throw new NoSuchElementException();
						}
						
						@Override
						public boolean hasNext() {
							return row < nRows;
						}
					};
				}
			};
		}
		return selectedModelRows;
	}
}
