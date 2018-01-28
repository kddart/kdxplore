/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.field;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.Sample;
import com.diversityarrays.kdsmart.db.entities.Specimen;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.SampleOrder;
import com.diversityarrays.kdsmart.db.util.Util;
import com.diversityarrays.kdsmart.scoring.NextUnscoredSampleRowFinder;
import com.diversityarrays.kdsmart.scoring.NextUnscoredSampleRowFinder.UnscoredSampleSearchResult;
import com.diversityarrays.kdsmart.scoring.PlotVisitGroup;
import com.diversityarrays.kdsmart.scoring.PlotVisitList;
import com.diversityarrays.kdxplore.field.FieldViewPanel.SeparatorVisibilityOption;
import com.diversityarrays.kdxplore.ui.CellSelectableTable;
import com.diversityarrays.kdxplore.ui.Toast;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.XYPos;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.NumberSpinner;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class FieldViewDialog extends JDialog {

	class SampleCounts {
		public int scored = 0;
		public int unscored = 0;
	}

    private Plot lastSelectedPlot;

    private JCheckBox autoAdvanceOption = new JCheckBox("Auto Advance");
    
    private final Action autoAdvanceAction = new AbstractAction("Adv") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doAutoAdvance();
        }
    };
    private final Action advanceAction = new AbstractAction("Forw") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doAdvance();
        }
    };
    private final Action retreatAction = new AbstractAction("Back") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doRetreat();
        }
    };

	private final Action showInfoAction = new AbstractAction("Info") {
		@Override
		public void actionPerformed(ActionEvent e) {
			showTrialInfo(trial, fieldViewPanel);
		}
	};

	private final Trial trial;

	private final FieldViewPanel fieldViewPanel;

	//		private HTMLDocument htmlDocument = new HTMLDocument();
	private final JLabel infoTextArea = new JLabel("Click Plots to see Trait scores");

	private final CellSelectableTable fieldLayoutTable;

	private final Map<Integer,Trait> traitMap;

	private final Map<Integer,SampleCounts> countsByTraitId = new HashMap<>();

	private final Transformer<Plot, Point> xyProvider;

	private final KDSmartDatabase database;
	
	private final FileDrop fileDrop = new FileDrop() {
        @Override
        public void dropFiles(Component component, List<File> files, DropLocationInfo dli) {            
            SwingUtilities.invokeLater(() -> handleFileDrop(component, files, dli));
        }
	};
	
	private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

    private final SampleGroupChoice sampleGroupChoiceForSamples;

    private final SampleGroupChoice sampleGroupChoiceForNewMedia;

    private Box advanceRetreatControls;

    private Box autoAdvanceControls;
    
    private final SpinnerNumberModel fontSizeModel;

	public FieldViewDialog(Window owner, String title, 
	        SampleGroupChoice sgcSamples, 
	        Trial trial, 
	        SampleGroupChoice sgcNewMedia,
            KDSmartDatabase db) 
	throws IOException
	{
		super(owner, title, ModalityType.MODELESS);
		
		advanceRetreatControls = Box.createHorizontalBox();
		advanceRetreatControls.add(new JButton(retreatAction));
		advanceRetreatControls.add(new JButton(advanceAction));
	        
		autoAdvanceControls = Box.createHorizontalBox();
		autoAdvanceControls.add(new JButton(autoAdvanceAction));

		autoAdvanceOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMovementControls();
            }
        });
		

		this.database = db;
        this.sampleGroupChoiceForSamples = sgcSamples;
        this.sampleGroupChoiceForNewMedia= sgcNewMedia;
        
        NumberSpinner fontSpinner = new NumberSpinner(new SpinnerNumberModel(), "0.00");
        
		this.fieldViewPanel = FieldViewPanel.create(database,
		        trial,
		        SeparatorVisibilityOption.VISIBLE,
		        null, 
		        Box.createHorizontalGlue(),
		        new JButton(showInfoAction),
                Box.createHorizontalGlue(),
                new JLabel("Font Size:"),
                fontSpinner,
                Box.createHorizontalGlue(),
		        advanceRetreatControls,
                autoAdvanceOption,
		        autoAdvanceControls);

		initialiseAction(advanceAction, "ic_object_advance_black.png", "Auto-Advance");
		
		this.xyProvider = fieldViewPanel.getXYprovider();
		this.traitMap = fieldViewPanel.getTraitMap();

		fieldLayoutTable = fieldViewPanel.getFieldLayoutTable();
		
		JScrollPane scrollPane = fieldViewPanel.getFieldTableScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
	    fieldLayoutTable.setTransferHandler(flth);
	    fieldLayoutTable.setDropMode(DropMode.ON);

	    fieldLayoutTable.addMouseListener(new MouseAdapter() {
	        JPopupMenu popupMenu;
            @Override
            public void mouseClicked(MouseEvent e) {
                if (! SwingUtilities.isRightMouseButton(e) || 1 != e.getClickCount()) {
                    return;
                }
                Point pt = e.getPoint();
                int row = fieldLayoutTable.rowAtPoint(pt);
                if (row >= 0) {
                    int col = fieldLayoutTable.columnAtPoint(pt);
                    if  (col >= 0) {
                        Plot plot = fieldViewPanel.getPlotAt(col, row);
                        if (plot != null) {
                            if (popupMenu == null) {
                                popupMenu = new JPopupMenu("View Attachments");
                            }
                            popupMenu.removeAll();

                            Set<File> set = plot.getMediaFiles();
                            if (Check.isEmpty(set)) {
                                popupMenu.add(new JMenuItem("No Attachments available"));
                            }
                            else {
                                for (File file : set) {
                                    Action a = new AbstractAction(file.getName()) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                Desktop.getDesktop().browse(file.toURI());
                                            }
                                            catch (IOException e1) {
                                                MsgBox.warn(FieldViewDialog.this, 
                                                        e1,
                                                        file.getName());
                                            }
                                        }
                                    };
                                    popupMenu.add(new JMenuItem(a));
                                }
                            }
                            popupMenu.show(fieldLayoutTable, pt.x, pt.y);
                        }
                    }
                }                
            }
	        
	    });
        Font font = fieldLayoutTable.getFont();
        float fontSize = font.getSize2D();

        fontSizeModel = new SpinnerNumberModel(fontSize, fontSize, 50.0, 1.0);
        fontSpinner.setModel(fontSizeModel);
        fontSizeModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float fsize = fontSizeModel.getNumber().floatValue();
                System.out.println("Using fontSize=" + fsize);
                Font font = fieldLayoutTable.getFont().deriveFont(fsize);
                fieldLayoutTable.setFont(font);
                FontMetrics fm = fieldLayoutTable.getFontMetrics(font);
                int lineHeight = fm.getMaxAscent() + fm.getMaxDescent();
                fieldLayoutTable.setRowHeight(4*lineHeight);
                
//                GuiUtil.initialiseTableColumnWidths(fieldLayoutTable, false);
                
                fieldLayoutTable.repaint();
            }
        });
        
        fieldLayoutTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fieldLayoutTable.setResizable(true, true);
        fieldLayoutTable.getTableColumnResizer().setResizeAllColumns(true);

	    advanceAction.setEnabled(false);
		fieldLayoutTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					handlePlotSelection();
				}
			}
		});
		TableColumnModel columnModel = fieldLayoutTable.getColumnModel();
        columnModel.addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					handlePlotSelection();
				}
			}
			
			@Override
			public void columnRemoved(TableColumnModelEvent e) { }
			
			@Override
			public void columnMoved(TableColumnModelEvent e) { }
			
			@Override
			public void columnMarginChanged(ChangeEvent e) { }
			
			@Override
			public void columnAdded(TableColumnModelEvent e) { }
		});
        
        
        PropertyChangeListener listener = new PropertyChangeListener() {
            // Use a timer and redisplay other columns when delay is GT 100 ms
            
            Timer timer = new Timer(true);
            TimerTask timerTask;
            long lastActive;
            boolean busy = false;
            private int eventColumnWidth;
            private TableColumn eventColumn;
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (busy) {
                    return;
                }

                if (evt.getSource() instanceof TableColumn && "width".equals(evt.getPropertyName())) {

                    eventColumn = (TableColumn) evt.getSource();
                    eventColumnWidth = eventColumn.getWidth();

                    lastActive = System.currentTimeMillis();
                    if (timerTask == null) {
                        timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                if (System.currentTimeMillis() - lastActive > 200) {
                                    timerTask.cancel();
                                    timerTask = null;
                                    
                                    busy = true;
                                    try {
                                        for (Enumeration<TableColumn> en = columnModel.getColumns(); en.hasMoreElements(); ) {
                                            TableColumn tc = en.nextElement();
                                            if (tc != eventColumn) {
                                                tc.setWidth(eventColumnWidth);
                                            }
                                        }
                                    }
                                    finally {
                                        busy = false;
                                    }
                                }
                            }
                        };
                        timer.scheduleAtFixedRate(timerTask, 100, 150);
                    }
                }
            }
        };
        for (Enumeration<TableColumn> en = columnModel.getColumns(); en.hasMoreElements(); ) {
            TableColumn tc = en.nextElement();
            tc.addPropertyChangeListener(listener);
        }
		
		Map<Integer, Plot> plotById = new HashMap<>();
		for (Plot plot : fieldViewPanel.getFieldLayout()) {
			plotById.put(plot.getPlotId(), plot);
		}

		TrialItemVisitor<Sample> sampleVisitor = new TrialItemVisitor<Sample>() {
			@Override
			public void setExpectedItemCount(int count) { }

			@Override
			public boolean consumeItem(Sample sample) throws IOException {
				
				Plot plot = plotById.get(sample.getPlotId());
				if (plot == null) {
					throw new IOException("Missing plot for plotId=" + sample.getPlotId()
						+ " sampleIdent=" + Util.createUniqueSampleKey(sample));
				}
				plot.addSample(sample);

				SampleCounts counts = countsByTraitId.get(sample.getTraitId());
				if (counts == null) {
					counts = new SampleCounts();
					countsByTraitId.put(sample.getTraitId(), counts);
				}
				if (sample.hasBeenScored()) {
					++counts.scored;
				}
				else {
					++counts.unscored;
				}
				return true;
			}
		};
		database.visitSamplesForTrial(
		        sampleGroupChoiceForSamples,
		        trial.getTrialId(), 
				SampleOrder.ALL_BY_PLOT_ID_THEN_TRAIT_ID_THEN_INSTANCE_NUMBER_ORDER_THEN_SPECIMEN_NUMBER,
				sampleVisitor);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.trial = trial;

		KDClientUtils.initAction(ImageId.SETTINGS_24, showInfoAction, "Trial Summary");

		Action clear = new AbstractAction("Clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                infoTextArea.setText("");
            }
		};
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(GuiUtil.createLabelSeparator("Plot Details", new JButton(clear)), BorderLayout.NORTH);
		bottom.add(new JScrollPane(infoTextArea), BorderLayout.CENTER);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
				fieldViewPanel, 
				new JScrollPane(infoTextArea));
		splitPane.setResizeWeight(0.0);
		splitPane.setOneTouchExpandable(true);

		setContentPane(splitPane);

		updateMovementControls();
		pack();
	}

	private void initialiseAction(Action action, String resourceName, String tooltip) {
        action.putValue(Action.SHORT_DESCRIPTION, tooltip);
        
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is != null) {
            try {
                BufferedImage img = ImageIO.read(is);
                action.putValue(Action.SMALL_ICON, new ImageIcon(img));
            }
            catch (IOException ignore) {}
        }
    }

	private void doAdvance() {
	    
	}
	
	private void doRetreat() {
	    
	}
	
    private void doAutoAdvance() {
	    Sample lastSample = null;
	    Sample lastUnscoredSample = null;
	    for (Sample sample : lastSelectedPlot) {
	        lastSample = sample;
	        if (! sample.hasBeenScored()) {
	            lastUnscoredSample = sample;
	        }
	    }

	    NextUnscoredSampleRowFinder finder = new NextUnscoredSampleRowFinder(fieldViewPanel.getPlotVisitList());
	    UnscoredSampleSearchResult searchResult = finder.findNextScoreRequiredSample(lastSample, null);
	    if (searchResult == null) {
	        MsgBox.info(FieldViewDialog.this, "No next unscored", "Find Next Unscored");
	    }
	    else {
	        XYPos xy = fieldViewPanel.getFieldLayoutTableModel().getXYForPlot(searchResult.sample.getPlotId());
	        if (xy != null) {
	            List<Point> points = Arrays.asList(new Point(xy.x, xy.y));
	            fieldLayoutTable.setSelectedPoints(points);
//	            Plot plot = fieldViewPanel.getFieldLayoutTableModel().getPlot(xy.y, xy.x);
	        }
	    }
        
    }

    private void handleFileDrop(Component component, List<File> files, DropLocationInfo dli) {
	    Integer column = dli.column;
	    Integer row = dli.rowOrIndex;

	    List<Plot> plots =  fieldViewPanel.getFieldLayout().getItemsAt(Arrays.asList(new Point(column, row)));
	    
	    System.out.println("Dropped on: col,row=" + column + "," + row);
	    
	    List<File> filesAdded = new ArrayList<>();
        for (Plot plot : plots) {            
            System.out.println("plot: " + plot.getPlotId() + ": " + plot.getPlotColumn() + "," + plot.getPlotRow());
            
            List<Specimen> specimens = plot.getSpecimens();
            if (specimens==null || specimens.isEmpty()) {
                
                try {
                    for (File file : files) {
                        File storedFile = copyToMediaStoreDir(file, plot, PlotOrSpecimen.ORGANISM_NUMBER_IS_PLOT);
                        database.addMediaFile(sampleGroupChoiceForNewMedia, trial, plot, PlotOrSpecimen.ORGANISM_NUMBER_IS_PLOT, storedFile);
                        filesAdded.add(file);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(FieldViewDialog.this, 
                            e, 
                            "Database Error", 
                            JOptionPane.ERROR_MESSAGE);
                    break;
                }
            }
            else {
                List<String> plotOrSpecimen = new ArrayList<>();
                Map<String, PlotOrSpecimen> specimenByIdent = new HashMap<>();
                String plotIdent = trial.getPlotIdentOption().createPlotIdentString(plot, ",");
                plotOrSpecimen.add(plotIdent);
                specimenByIdent.put(plotIdent, plot);
                for (Specimen s : specimens) {
                    if (PlotOrSpecimen.isSpecimenNumberForSpecimen(s.getSpecimenNumber())) {
                        String ident = "Individual#" + s.getSpecimenNumber();
                        plotOrSpecimen.add(ident);
                        specimenByIdent.put(ident, s);
                    }
                }
                Object[] selectionValues = plotOrSpecimen.toArray(new String[plotOrSpecimen.size()]);
                Object initialSelectionValue = null;
                Object chosen = JOptionPane.showInputDialog(FieldViewDialog.this, "Select Plot or Specimen", 
                        "Add Attachment", JOptionPane.OK_CANCEL_OPTION, 
                        null,
                        selectionValues, initialSelectionValue);
                if (chosen != null) {
                    PlotOrSpecimen choice = specimenByIdent.get(chosen);
                    if (choice != null) {
                        try {
                            for (File file : files) {
                                File storedFile = copyToMediaStoreDir(file, plot, choice.getSpecimenNumber());
                                database.addMediaFile(sampleGroupChoiceForNewMedia, trial, plot, choice.getSpecimenNumber(), storedFile);
                                filesAdded.add(file);
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(FieldViewDialog.this, 
                                    e, 
                                    "Database Error", 
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        }
	    
        if (! filesAdded.isEmpty()) {
            String message = filesAdded.stream().map(File::getName).collect(Collectors.joining("\n"));
            JOptionPane.showMessageDialog(FieldViewDialog.this, message, "Attachments Added", JOptionPane.INFORMATION_MESSAGE);
        }
        
    }

	static private Locale NO_LOCALE = null;
    private File copyToMediaStoreDir(File file, Plot plot, int specimenNumber) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        
        int dotpos = file.getName().lastIndexOf('.');
        String suffix = dotpos <= 0 ? "" : file.getName().substring(dotpos);
        
        String nameTrial_trialId = String.format(NO_LOCALE, "Trial_%d", trial.getTrialId());
        Integer userPlotId = plot.getUserPlotId();
        String plotIdent;
        if (userPlotId == null) {
            plotIdent = String.format(NO_LOCALE, "%s-Plot_%d,%d",
                    nameTrial_trialId, plot.getPlotColumn(), plot.getPlotRow());
        } else {
            plotIdent = String.format(NO_LOCALE, "%s-Plot_%d",
                    nameTrial_trialId, userPlotId);
        }

        String fileName;
        if (PlotOrSpecimen.isSpecimenNumberForPlot(specimenNumber)) {
            fileName = String.format(NO_LOCALE, "%s-%s",
                    plotIdent, timeStamp);
        } else {
            fileName = String.format(NO_LOCALE, "%s-%d-%s",
                    plotIdent, specimenNumber, timeStamp);
        }
        
        File mediaStorageDir = database.getMediaStorageDir();
        File result = new File(mediaStorageDir, fileName + suffix);
        
        int unique = 0;
        while (result.exists()) {
            result = new File(mediaStorageDir, fileName + " (" + (++unique) + ")" + suffix);
        }
        
        try (FileInputStream is = new FileInputStream(file);
             FileOutputStream os = new FileOutputStream(result))
        {
            byte[] buf = new byte[8096];
            int nb;
            while (-1 != (nb = is.read(buf))) {
                os.write(buf, 0, nb);
            }
        }
        finally { }
        
        return result;
    }

    private void showTrialInfo(Trial trial, Component component) {

		List<Trait> traits = new ArrayList<>(traitMap.values());
		Collections.sort(traits);

		StringBuilder html = new StringBuilder("<HTML>");
		html.append("Trial has samples for ")
		.append(countsByTraitId.size())
		.append(" Traits:");

		html.append("<UL>");
		for (Trait trait : traits) {
			SampleCounts counts = countsByTraitId.get(trait.getTraitId());
			if (counts != null) {
				html.append("<LI>")
				.append(StringUtil.htmlEscape(String.format("%20s: ", trait.getTraitName())))
				.append(counts.scored)
				.append(" / ")
				.append(counts.scored + counts.unscored)
				.append("</LI>");
			}
		}
		html.append("</UL>");

		JOptionPane.showMessageDialog(component, 
				new JLabel(html.toString()), 
				trial.getTrialName(), 
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void handlePlotSelection() {

		StringBuilder html = new StringBuilder("<HTML><DL>\n");

		int nSelected = 0;
		
		List<Point> viewPoints = fieldLayoutTable.getSelectedPoints();
		for (Point pt : viewPoints) {
		    Plot plot = fieldViewPanel.getPlotAt(pt.x, pt.y);
            if (plot != null) {
                ++nSelected;
                lastSelectedPlot = plot;
                appendPlotDetails(html, plot);
            }
		}
		html.append("</DL>\n");
		infoTextArea.setText(html.toString());

		boolean onlyOne = nSelected == 1;
        autoAdvanceAction.setEnabled(onlyOne);
        advanceAction.setEnabled(onlyOne);
        retreatAction.setEnabled(onlyOne);

        if (nSelected == 1) {
		    autoAdvanceAction.setEnabled(true);
		    advanceAction.setEnabled(true);
		    retreatAction.setEnabled(true);
		    
		    PlotVisitList pvl = fieldViewPanel.getPlotVisitList();
		    PlotVisitGroup pvg = pvl.getPlotVisitGroup(lastSelectedPlot);
		    pvl.changePlotVisitPositionToStartOf(pvg.getGroupIndex());
//            PlotVisitPosition pvp = new PlotVisitPosition(groupIndex, plotIndexInGroup);
//            pvl.changePlotVisitPositionTo(pvp);
		}
	}

	private void appendPlotDetails(StringBuilder html, Plot plot) {
		Point xy = xyProvider.transform(plot);

		html.append("<DT><B>");
		if (xy != null) {
			//				int xcoord = FieldLayoutUtil.convertColumnIndexToXCoord(xy.x, trial, fieldLayout);
			//				int ycoord = FieldLayoutUtil.convertRowIndexToYCoord(xy.y, trial, fieldLayout);
			html.append("X: ").append(xy.x)
			.append("  Y: ").append(xy.y);
		}
		Integer userPlotId = plot.getUserPlotId();
		if (userPlotId != null) {
			html.append(" [").append(userPlotId).append("]");
		}
		html.append("</B></DT>\n");

		html.append("<DD>");
		appendDD(html, plot);
		html.append("</DD><HR>");
	}

    private void appendDD(StringBuilder html, Plot plot) {
        
        Map<Integer, List<Tag>> map = plot.getTagsBySampleGroup();
        if (! map.isEmpty()) {
            Set<String> tagLabels = map.entrySet().stream()
                    .flatMap(e -> e.getValue().stream())
                    .map(Tag::getLabel)
                    .collect(Collectors.toSet());

                if (! tagLabels.isEmpty()) {
                    List<String> list = new ArrayList<>(tagLabels);
                    Collections.sort(list);
                    html.append("Tags:");
                    String sep = " ";
                    for (String tag : list) {
                        html.append(sep).append(tag);
                        sep = ", ";
                    }
                }
        }
		html.append("\n");

		int sampleCount = 0;
		int specimenNumber = -1;
		for (Sample sample : plot) {
			if (1 == ++sampleCount) {
				html.append("<UL>\n");
			}
			
			html.append("<LI>");
			
			if (specimenNumber != sample.getSpecimenNumber()) {
				int previousSpecimenNumber = specimenNumber;
				specimenNumber = sample.getSpecimenNumber();
				if (PlotOrSpecimen.isSpecimenNumberForPlot(specimenNumber)) {
					if (previousSpecimenNumber != -1) {
						html.append("<B>Plot:</B><BR>\n");
					}
				}
				else {
					html.append("<HR>\n<B>Individual#")
						.append(specimenNumber)
						.append("</B><BR>\n");
				}
			}
			
		     TraitNameStyle traitNameStyle = trial.getTraitNameStyle();


			Trait trait = traitMap.get(sample.getTraitId());
			String traitName = trait==null
					? "Trait#" + sample.getTraitId()
					: trait.getTraitName();
					String tiname = traitNameStyle.makeTraitInstanceName(traitName, sample.getTraitInstanceNumber());

			html.append(tiname).append(": ");
			switch (TraitValue.classify(sample.getTraitValue())) {
			case MISSING:
				html.append(TraitValue.EXPORT_VALUE_MISSING);
				break;
			case NA:
				html.append(TraitValue.EXPORT_VALUE_NA);
				break;
			case SET:
				html.append(sample.getTraitValue());
				break;
			case UNSET:
				//html.append("");
				break;
			default:
				break;
			}
			html.append("</LI>\n");
		}
		
		if (sampleCount == 0) {
			html.append("No Samples");
		}
		else {
			html.append("</UL>\n")
				.append("<B>Sample Count</B>: ").append(sampleCount);
		}
    }

    protected void updateMovementControls() {
        boolean auto = autoAdvanceOption.isSelected();

        updateControls(! auto, advanceRetreatControls);
        updateControls(auto && (lastSelectedPlot != null), autoAdvanceControls);
    }
    
    private void updateControls(boolean enable, Container cont) {
        for (int i = cont.getComponentCount(); --i >= 0; ) {
            Component c = cont.getComponent(i);
            if (c instanceof JButton) {
                c.setEnabled(enable);
            }
        }
    }
}
