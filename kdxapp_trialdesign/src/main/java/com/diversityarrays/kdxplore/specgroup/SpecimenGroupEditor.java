/**
 * 
 */
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
package com.diversityarrays.kdxplore.specgroup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import javax.persistence.Column;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.dalclient.DalResponseException;
import com.diversityarrays.dalclient.DalResponseRecord;
import com.diversityarrays.daldb.core.BreedingMethod;
import com.diversityarrays.daldb.core.Genotype;
import com.diversityarrays.daldb.core.Schema;
import com.diversityarrays.daldb.core.Specimen;
import com.diversityarrays.daldb.core.SpecimenGroup;
import com.diversityarrays.db.DartEntityBuilder;
import com.diversityarrays.db.DartEntityVisitor;
import com.diversityarrays.db.DartSchemaHelper;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.Shared.For;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.refdata.KddartReferenceData;
import com.diversityarrays.kdxplore.demoapp.KdxGenotypeSpecimen;
import com.diversityarrays.kdxplore.gtools.dnd.GenotypeDndSourceSink;
import com.diversityarrays.kdxplore.gtools.dnd.GenotypeTransferHandler;
import com.diversityarrays.kdxplore.gtools.searches.DatabaseSearcher;
import com.diversityarrays.kdxplore.gtools.searches.SearchResultConsumer;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.UnicodeChars;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.DefaultBackgroundRunner;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.ui.widget.PromptTextField;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;

/**
 * @author alexs
 *
 */
@SuppressWarnings("nls")
public class SpecimenGroupEditor extends JSplitPane {

	private static final String BREEDING_METHOD_NAME = "BreedingMethodName";

//    private PromptTextField matchSpecimenName = new PromptTextField("Download matching Specimen Names");
    
	private Action loadAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
		    KdxplorePreferences prefs = KdxplorePreferences.getInstance();
		    File file = prefs.askInputFile(SpecimenGroupEditor.this, 
		            "Choose Specimen List File to Load", 
		            Shared.CSV_FILE_FILTER, Shared.TXT_FILE_FILTER);
		    if (file != null) {
		        doImportFile(file);
		    }
		}		
	};
	
	private final DatabaseSearcher searcher;

	private final GenotypeDndSourceSink srcSink = new GenotypeDndSourceSink() {

        @Override
        public List<Genotype> getGenotypes(JComponent c) {

            List<Genotype> genotypes = new ArrayList<Genotype>();
            List<Integer> genotypeIds = new ArrayList<Integer>();

            for (Specimen specimen : SpecimenGroupEditor.this.getSelectedSpecimen()) {
                genotypeIds.add(specimen.getSpecimenId());
                searcher.getGenotypesForSpecimen(specimen.getSpecimenId(), new SearchResultConsumer() {

                    @Override
                    public void consume(List<KdxGenotypeSpecimen> genos) {
                        genotypes.addAll(genos);
                    }

                    @Override
                    public void handleError(Throwable error) {
                        error.printStackTrace();
                    }       
                });
            }

            return genotypes;
        }

        @Override
        public void handleGenotypeDrop(Component component, List<Genotype> genotypes, DropLocationInfo dli) {
            
            backgroundRunner.runBackgroundTask(new BackgroundTask<List<Specimen>,Boolean>("Converting Specimen..", true) {

                @Override
                public List<Specimen> generateResult(Closure<Boolean> arg0) throws Exception {
                    
                    List<Specimen> specimen = new ArrayList<Specimen>();
                    List<Integer> genotypeIds = new ArrayList<Integer>();
                    for (Genotype genotype : genotypes) {
                        genotypeIds.add(genotype.getGenotypeId());
                        specimen.addAll(searcher.getSpecimen(genotype.getGenotypeId()));
                    }
                    
                    return specimen;
                }

                @Override
                public void onCancel(CancellationException arg0) {
                    arg0.printStackTrace();                     
                }

                @Override
                public void onException(Throwable arg0) {
                    arg0.printStackTrace();                                             
                }

                @Override
                public void onTaskComplete(List<Specimen> arg0) {
                    SpecimenGroupEditor.this.specimenTableModel.addSpecimens(arg0); 
                }
                
            }); 
        }

        @Override
        public List<Specimen> getSpecimens(JComponent c) {
            List<Integer> rows = GuiUtil.getSelectedModelRows(specimenTable);
            List<Specimen> specimens = new ArrayList<Specimen>();
            
            for (Integer row : rows) {
                specimens.add(specimenTableModel.getSpecimenAt(row));
            }
            
            return specimens;
        }

        @Override
        public void handleSpecimenDrop(Component component, List<Specimen> specimens, DropLocationInfo dli) {
            specimenTableModel.addSpecimens(specimens);             
        }

        @Override
        public boolean isGenotypeOutput() {
            return false;
        }
    };

    private final BackgroundRunner backgroundRunner;
    
    private final FileDrop fileDrop = new FileDrop() {
        @Override
        public void dropFiles(Component arg0, List<File> files, DropLocationInfo arg2) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    File file = files.get(0);
                    String loname = file.getName().toLowerCase();
                    if (loname.endsWith(".txt") || loname.endsWith(".csv")) {
                        doImportFile(file);
                    }
                    else {
                        MsgBox.error(SpecimenGroupEditor.this, "Only CSV and TXT files supported", 
                                "Drop Failed");
                    }
                }
            });
        }
    };
    
    private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

    private final OfflineData offlineData;
	
	public SpecimenGroupEditor(OfflineData offlineData, DatabaseSearcher s, BackgroundRunner runner, Action action) {
	    super(JSplitPane.VERTICAL_SPLIT);
	    
	    this.offlineData = offlineData;
		this.searcher = s;
		if (runner == null) {
		    backgroundRunner = new DefaultBackgroundRunner();
		}
		else {
		    backgroundRunner = runner;
		}
		genotypeTransferHandler = new GenotypeTransferHandler(srcSink);
		

		KDClientUtils.initAction(ImageId.TRASH_24, deleteSpecimenFromList, "Delete Specimen from list");
		KDClientUtils.initAction(ImageId.DOWNLOAD_24, downloadSpecimens, "Download Specimens");
		KDClientUtils.initAction(ImageId.DOWNLOAD_24, downloadSpecimenGroups, "Download Specimen Groups");
		KDClientUtils.initAction(ImageId.EXPORT_24, exportSpecimenList, "Export specimen list");
		//	      KDClientUtils.initAction(ImageId.SAVE_24, saveAction, "Save specimen list");
		KDClientUtils.initAction(ImageId.ADD_CSV_24, loadAction, "Load specimen list");
		//	      KDClientUtils.initAction(ImageId.REFRESH_24, refreshSpecimenList, "Refresh specimen lists");

		setLeftComponent(createTopComponent(action));
		setRightComponent(createBottomComponent());

		setResizeWeight(0.2);
	}
			
	private void doImportFile(File file) {
	    BackgroundTask<List<Specimen>, Void> task = new BackgroundTask<List<Specimen>, Void>("Loading " + file.getName(), true) {

            @Override
            public List<Specimen> generateResult(Closure<Void> arg) throws Exception {
                return importFile(file);
            }

            @Override
            public void onCancel(CancellationException e) {
                MsgBox.info(SpecimenGroupEditor.this, e.getMessage(), title);
            }

            @Override
            public void onException(Throwable e) {
                MsgBox.error(SpecimenGroupEditor.this, e, title);
            }

            @Override
            public void onTaskComplete(List<Specimen> specs) {
                specimenTableModel.setData(specs);
            }
        };
        backgroundRunner.runBackgroundTask(task);
    }

    private List<Specimen> importFile(File file) throws IOException {

        List<Specimen> specimens = new ArrayList<>();

        Map<String, BreedingMethod> methodByLowcaseName = new HashMap<>();
        if (offlineData != null) {
            KddartReferenceData refdata = offlineData.getKddartReferenceData();
            if (refdata != null) {
                for (BreedingMethod bm : refdata.getBreedingMethods()) {
                    methodByLowcaseName.put(bm.getBreedingMethodName().toLowerCase(), bm);
                }
            }
        }
        
        SpecimenColumnHelper sch = new SpecimenColumnHelper();
        
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            @SuppressWarnings("resource")
            CSVReader csvReader = new CSVReader(reader);
            
            String[] lineFields;
            Map<Integer,Column> columnByInputIndex = null;
            int lineNumber = 0;
            while (null != (lineFields = csvReader.readNext())) {
                if (! Check.isEmpty(lineFields[0])) {
                    ++lineNumber;
                    
                    // Line1 Column1 == "SpecimenName"
                    // else we only do the first column as the specimen name
                    if (lineNumber == 1) {
                        if (lineFields[0].equals(sch.specimenNameColumn.name())) {
                            
                            columnByInputIndex = new HashMap<>();
                            // Heading row
                            columnByInputIndex.put(0, sch.specimenNameColumn);
                            for (int inputIndex = 1; inputIndex < lineFields.length; ++inputIndex) {
                                String heading = lineFields[inputIndex];
                                for (Column c : sch.columns) {
                                    if (c.name().equals(heading)) {
                                        columnByInputIndex.put(inputIndex, c);
                                        break;
                                    }
                                }
                            }
                            // Skip to next line
                            continue;
                        }
                    }
                    
                    Specimen s = new Specimen();
                    if (columnByInputIndex == null) {
                        s.setSpecimenName(lineFields[0]);                        
                    }
                    else {
                        for (int index = lineFields.length; --index >= 0; ) {
                            String columnValue = lineFields[index];

                            Column c = columnByInputIndex.get(index);
                            if (c != null) {
                                Field fld = sch.fieldByColumn.get(c);
                                if (fld != null) {
                                    if (sch.breedingMethodIdColumn == c) {
                                        BreedingMethod bm = methodByLowcaseName.get(columnValue);
                                        if (bm != null) {
                                            s.setExtraData(BREEDING_METHOD_NAME, bm.getBreedingMethodName());
                                            s.setBreedingMethodId(bm.getBreedingMethodId());
                                        }
                                        else {
                                            s.setExtraData(BREEDING_METHOD_NAME, columnValue);
                                        }
                                    }
                                    else {
                                        Object value = null;
                                        if (Integer.class == fld.getType()) {
                                            if (! Check.isEmpty(columnValue)) {
                                                try {
                                                    Integer i = Integer.valueOf(columnValue);
                                                    value = i;
                                                }
                                                catch (NumberFormatException ignore) {
                                                }
                                                
                                            }
                                        }
                                        else if (String.class == fld.getType()) {
                                            value = columnValue;
                                        }
                                        
                                        if (value != null) {
                                            try {
                                                fld.set(s, value);
                                            }
                                            catch (IllegalArgumentException 
                                                    | IllegalAccessException ignore) { }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    specimens.add(s);
                }
            }
            return specimens;
        }
        finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignore) {}
            }
        }
    }

    private GenotypeTransferHandler genotypeTransferHandler;
	
	private SpecimenTableModel specimenTableModel;
	private JTable specimenTable;

    private final DartSchemaHelper dsh = new DartSchemaHelper(new Schema());

    class AskSearchParamsDialog extends JDialog {
        
        private final PromptTextField matchField;

        public String matchParam;
        Action okAction = new AbstractAction("Search") {
            @Override
            public void actionPerformed(ActionEvent e) {
                matchParam = matchField.getText().trim();;
                dispose();
            }
        };
        private JButton okButton = new JButton(okAction);

        Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
            @Override
            public void actionPerformed(ActionEvent e) {
                matchParam = null;
                dispose();
            }
        };
        AskSearchParamsDialog(String title, String prompt) {
            super(ownerFrame, title, ModalityType.APPLICATION_MODAL);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            matchField = new PromptTextField(prompt, 40);
            
            Box btns = Box.createHorizontalBox();
            btns.add(new JLabel("Leave 'Match' blank to retreive all"));
            btns.add(Box.createHorizontalGlue());
            btns.add(new JButton(cancelAction));
            btns.add(okButton);
            
            Box top = Box.createHorizontalBox();
            top.add(new JLabel("Match:"));
            top.add(matchField);
//            JPanel main = new JPanel();
//            GBH gbh = new GBH(main, 2,2,2,2);
//            int y = 0;
//            gbh.add(0,y, 1,1, GBH.NONE, 0,1, GBH.EAST, "Match:");
//            gbh.add(1,y, 1,1, GBH.HORZ, 1,1, GBH.CENTER, matchField);
//            ++y;
            
            Container cp = getContentPane();
            cp.add(top,  BorderLayout.NORTH);
            cp.add(btns, BorderLayout.SOUTH);
            
            getRootPane().setDefaultButton(okButton);
            pack();
        }
    }
    
    /**
     * Return null if don't want to do it
     * @param title
     * @param matchPrompt
     * @param relativeTo 
     * @return
     */
    private Pair<String,DALClient> askSearchParams(String title, String matchPrompt, Component relativeTo) {
        AskSearchParamsDialog dlg = new AskSearchParamsDialog(title, matchPrompt);
        dlg.setLocationRelativeTo(relativeTo);
        dlg.setVisible(true);
        if (dlg.matchParam == null) {
            return null;
        }

        Boolean alwaysOnTop = null;
        if (ownerFrame != null) {
            alwaysOnTop = ownerFrame.isAlwaysOnTop();
            if (alwaysOnTop) {
                ownerFrame.toBack();
            }
        }
        DALClient client = searcher.getClient();
        if (ownerFrame != null) {
            ownerFrame.toFront();
            if (alwaysOnTop != null) {
                ownerFrame.setAlwaysOnTop(alwaysOnTop);
            }
        }
        if (client == null) {
            return null;
        }

        String match = dlg.matchParam;
        return new Pair<>(match, client);
    }
    
    private final Action downloadSpecimenGroups = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            
            Pair<String, DALClient> pair = askSearchParams("Find Specimen Groups", 
                    "Enter Group name portion to match",
                    downloadGroupsButton);
            if (pair == null) {
                return;
            }
            
            String match = pair.first;
            DALClient client = pair.second;

            boolean useMatch = ! match.isEmpty();

            DartEntityBuilder<Specimen> specimenBuilder = new DartEntityBuilder<>(Specimen.class, null);

            BackgroundTask<Map<SpecimenGroup,List<Specimen>>, Void> task = new BackgroundTask<Map<SpecimenGroup,List<Specimen>>, Void>("Downloading...", true) {

                @Override
                public Map<SpecimenGroup,List<Specimen>> generateResult(Closure<Void> arg0) throws Exception {
                    
                    Map<SpecimenGroup,List<Specimen>> result = new HashMap<>();
                    
                    DartEntityVisitor<SpecimenGroup> sgVisitor = new DartEntityVisitor<SpecimenGroup>() {
                        
                        @Override
                        public boolean visitDartEntity(SpecimenGroup sg, Map<String, String> unusedData, DalResponseRecord record) {
                            if (! useMatch || sg.getSpecimenGroupName().toLowerCase().contains(match)) {
                                List<Specimen> specimens = result.get(sg);
                                if (specimens == null) {
                                    specimens = new ArrayList<>();
                                    result.put(sg, specimens);
                                }
                                List<Map<String, String>> more = record.nestedData.get("Specimen");
                                if (more != null) {
                                    for (Map<String,String> map : more) {
                                        Specimen spec = specimenBuilder.build(map);
                                        specimens.add(spec);
                                    }
                                }
                            }
                            return ! backgroundRunner.isCancelRequested();
                        }

                        // Override required but we don't use it
                        @Override
                        public boolean visitDartEntity(SpecimenGroup entity, Map<String, String> unusedData) {
                            // method above does the work
                            return true;
                        }
                    };
                    try {
                        dsh.visitDartEntities(client, SpecimenGroup.class, sgVisitor);
                    }
                    catch (DalResponseException | IOException e2) {
                        MsgBox.error(SpecimenGroupEditor.this, e2, title);
                    }
                    return result;
                }

                @Override
                public void onCancel(CancellationException arg0) {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void onException(Throwable arg0) {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void onTaskComplete(Map<SpecimenGroup,List<Specimen>> result) {
                    specimenGroupTableModel.setData(result);
                    System.out.println("Downloaded " + result.size());
                    if (ownerFrame != null) {
                        ownerFrame.toFront();
                    }
                }
                
            };
            backgroundRunner.runBackgroundTask(task);
        }
    };
    private final JButton downloadGroupsButton = new JButton(downloadSpecimenGroups);
	
    private final Action downloadSpecimens = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {

		    Pair<String, DALClient> pair = askSearchParams("Find Specimens", 
		            "Enter Specimen name portion to match",
		            downloadSpecimensButton);
		    if (pair == null) {
		        return;
		    }

		    String match = pair.first;
		    DALClient client = pair.second;
            
		    boolean useMatch = ! match.isEmpty();
		    
		    BackgroundTask<List<Specimen>, Void> task = new BackgroundTask<List<Specimen>, Void>("Downloading...", true) {
                @Override
                public List<Specimen> generateResult(Closure<Void> arg0) throws Exception {
                    List<Specimen> specs = new ArrayList<>();
                    DartEntityVisitor<Specimen> visitor = new DartEntityVisitor<Specimen>() {
                        @Override
                        public boolean visitDartEntity(Specimen s, Map<String, String> unusedData) {
                            if (! useMatch || s.getSpecimenName().contains(match)) {
                                specs.add(s);
                            }
                            return ! backgroundRunner.isCancelRequested();
                        }
                    };
                    dsh.visitDartEntities(client, Specimen.class, visitor);
                    return specs;
                }

                @Override
                public void onCancel(CancellationException arg0) {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void onException(Throwable e) {
                    MsgBox.error(SpecimenGroupEditor.this, 1, title);
                }

                @Override
                public void onTaskComplete(List<Specimen> specs) {
                    specimenTableModel.setData(specs);
                    System.out.println("Found " + specs.size());
                }
		        
		    };
		    
		    backgroundRunner.runBackgroundTask(task);
	
		}
	};
	private final JButton downloadSpecimensButton = new JButton(downloadSpecimens);
	
	// This is a hack because we can't seem to use ChainingTransferHandler for the
	// combination of FLTH, GenotypeDropHandler and TableTransferHandler
	private final JLabel fileDropTarget = new JLabel("drop CSV files here");

	static class SpecimenColumnHelper {
	    
	    static private final String FIELD_NAME_SPECIMEN_NAME = "specimenName";
	    static private final String FIELD_NAME_BMETHOD_ID = "breedingMethodId";
	    
        public final Column specimenNameColumn;;
        public final Column breedingMethodIdColumn;
        public final List<Column> columns = new ArrayList<>();
        public final Map<Column,Field> fieldByColumn = new HashMap<>();

	    public SpecimenColumnHelper() throws IOException {

	        Column snameColumn = null;
	        Column bmidColumn = null;
            for (Field fld : Specimen.class.getDeclaredFields()) {
                if (Modifier.isStatic(fld.getModifiers())) {
                    continue;
                }
                Column column = fld.getAnnotation(Column.class);
                if (column != null) {
                   columns.add(column);
                   fld.setAccessible(true);
                   
                   if (Integer.class!=fld.getType() && String.class!=fld.getType()) {
                       throw new IOException(
                               "Unexpected class for " + fld.getName() + ": " + fld.getType().getName());
                   }
                   
                   fieldByColumn.put(column, fld);
                   
                   if (FIELD_NAME_SPECIMEN_NAME.equals(fld.getName())) {
                       snameColumn = column;
                   }
                   else if (FIELD_NAME_BMETHOD_ID.equals(fld.getName())) {
                       bmidColumn = column;
                   }
                }
            }
            
            if (snameColumn == null) {
                throw new IOException("Missing Column for SpecimenName");
            }

            specimenNameColumn = snameColumn;
            breedingMethodIdColumn = bmidColumn;

            columns.remove(specimenNameColumn);
            columns.add(0, specimenNameColumn);
	    }
	}
	private Action exportSpecimenList = new AbstractAction("Export") {
		@Override
		public void actionPerformed(ActionEvent e) {
		    KdxplorePreferences prefs = KdxplorePreferences.getInstance();

		    JFileChooser chooser = Shared.getFileChooser(For.FILE_SAVE, Shared.CSV_FILE_FILTER);
            File outdir = prefs.getOutputDirectory();
            if (outdir != null && outdir.isDirectory()) {
                chooser.setCurrentDirectory(outdir);
            }
            if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(SpecimenGroupEditor.this)) {
                File outfile = chooser.getSelectedFile();
                outfile = Shared.ensureSuffix(outfile, ".csv");
                
                CSVWriter writer = null;
                try {
                    // SpecimenId
                    // SpecimenName
                    // SpecimenBarcode
                    // IsActive
                    // Pedigree
                    // SelectionHistory
                    
                    SpecimenColumnHelper sch = new SpecimenColumnHelper();

                    String[] output = new String[sch.columns.size()];

                    writer = new CSVWriter(new FileWriter(outfile));
                    
                    int ci = -1;
                    for (Column column : sch.columns) {
                        ++ci;
                        if (sch.breedingMethodIdColumn == column) {
                            output[ci] = "BreedingMethodName";
                        }
                        else {
                            output[ci] = column.name();
                        }
                    }
                    writer.writeNext(output);
                    
                    int[] vrows = specimenTable.getSelectedRows();
                    if (vrows == null || vrows.length<=0) {
                        int nRows = specimenTable.getRowCount();
                        vrows = new int[nRows];
                        for (int i = nRows; --i>=0; ) {
                            vrows[i] = i;
                        }
                    }
                    for (int vrow : vrows) {
                        int mrow = specimenTable.convertRowIndexToModel(vrow);
                        if (mrow < 0) {
                            continue;
                        }
                        Specimen s = specimenTableModel.getSpecimenAt(mrow);
                        collectOutput(s, sch, output);
                        writer.writeNext(output);
                    }
                }
                catch (IOException e1) {
                    MsgBox.error(SpecimenGroupEditor.this, e1, "Export Failed");
                }
                finally {
                    if (writer != null) {
                        try { writer.close(); } catch (IOException ignore) {}
                    }
                }
            }
		}

        private void collectOutput(Specimen s, SpecimenColumnHelper sch, String[] output) {
            String bmName = s.getExtraData(BREEDING_METHOD_NAME);
            
            int i = -1;
            for (Column column : sch.columns) {
                ++i;
                if (sch.breedingMethodIdColumn == column) {
                    output[i] = bmName==null ? "" : bmName;
                }
                else {
                    Field f = sch.fieldByColumn.get(column);
                    if (f == null) {
                        // SNH
                        output[i] = "";
                    }
                    else {
                        String v = "";
                        try {
                            Object vv = f.get(s);
                            if (vv != null) {
                                v = vv.toString();
                            }
                        }
                        catch (IllegalArgumentException | IllegalAccessException ignore) { }
                        output[i] = v;
                    }
                }
            }
        }
	};
	
	private Action deleteSpecimenFromList = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			List<Integer> rows = GuiUtil.getSelectedModelRows(specimenTable);	
			List<Specimen> specimens = new ArrayList<Specimen>();

			if (!rows.isEmpty()) {
				for (Integer row : rows) {
					specimens.add(specimenTableModel.getSpecimenAt(row));
				}
			}
			
			specimenTableModel.removeSpecimens(specimens);
			specimenTableModel.fireTableDataChanged();
		}
	};
	
	private Action clearSpecimenList = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
		    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(SpecimenGroupEditor.this, "Confirm - Clear All Specimens?", title, JOptionPane.YES_NO_OPTION)) {
		        specimenTableModel.clear();
		    }
		}
	};

	private SpecimenGroupTableModel specimenGroupTableModel;

    private String title;

    private JFrame ownerFrame;

    private JTable specimenGroupTable;
	
	private JComponent createTopComponent(Action extraAction) {

//		refreshSpecimenList.setEnabled(false);
		loadAction.setEnabled(true);
		
		Box specimenGroupControls = Box.createHorizontalBox();
		specimenGroupControls.add(downloadGroupsButton);
		if (extraAction != null) {
		    specimenGroupControls.add(new JButton(extraAction));
	    }
		specimenGroupControls.add(new JSeparator(JSeparator.VERTICAL));
//		specimenGroupControls.add(new JButton(refreshSpecimenList));
		specimenGroupControls.add(new JButton(loadAction));
		
		specimenGroupTableModel = new SpecimenGroupTableModel();
		specimenGroupTable = new JTable(specimenGroupTableModel);
		specimenGroupTable.setAutoCreateRowSorter(true);
		TableTransferHandler tth = TableTransferHandler.initialiseForCopySelectAll(specimenGroupTable, true);
		specimenGroupTable.setTransferHandler(tth);

		PromptScrollPane pane = new PromptScrollPane("No Specimen Groups Found");
		pane.add(specimenGroupTable);
		specimenGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		specimenGroupTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    List<Specimen> list = null;
                    int vrow = specimenGroupTable.getSelectedRow();
                    if (vrow >= 0) {
                        int mrow = specimenGroupTable.convertRowIndexToModel(vrow);
                        if (mrow >= 0) {
                            list = specimenGroupTableModel.getSpecimensAt(mrow);
                        }
                    }
                    specimenTableModel.setData(list);
                }
            }
        });
		
		JPanel containerPanel = new JPanel(new BorderLayout());
		containerPanel.add(specimenGroupControls, BorderLayout.NORTH);
		containerPanel.add(new TitledTablePanel("Specimen Groups", specimenGroupTable, "No Specimen Groups Loaded"), BorderLayout.CENTER);
		
		return containerPanel;
	}
	
	private JComponent createBottomComponent() {
		// =========
		specimenTableModel = new SpecimenTableModel(null);
		specimenTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                exportSpecimenList.setEnabled(specimenTableModel.getRowCount() > 0);
            }
        });
		specimenTable = new JTable(specimenTableModel);
		
		specimenTable.setAutoCreateRowSorter(true);
		specimenTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! e.getValueIsAdjusting()) {
                    deleteSpecimenFromList.setEnabled(specimenTable.getSelectedRowCount() > 0);
                }
            }
        });
//		specimenTableModel.addTableModelListener(new TableModelListener() {
//            @Override
//            public void tableChanged(TableModelEvent e) {
//                fileDropTarget.setVisible(specimenTableModel.getRowCount() > 0);
//            }
//        });
		CompoundBorder border = new CompoundBorder(new LineBorder(Color.DARK_GRAY), new EmptyBorder(4, 2, 4, 2));
		fileDropTarget.setBorder(border);
		fileDropTarget.setBackground(Color.LIGHT_GRAY);
//		fileDropTarget.setVisible(false);
		deleteSpecimenFromList.setEnabled(false);
				
		TitledTablePanel specimenTablePanel = new TitledTablePanel(
		        "Specimen", 
		        specimenTable,
		        "Download from database,\n  Drag/drop Specimens or Drag/drop CSV File above");
		
		
//		saveAction.setEnabled(false);
//		downloadSpecimenList.setEnabled(false);
		exportSpecimenList.setEnabled(false);

		Box otherBox = Box.createHorizontalBox();
        otherBox.add(downloadSpecimensButton);
        otherBox.add(fileDropTarget);
		otherBox.add(Box.createHorizontalGlue());
        
		otherBox.add(new JButton(exportSpecimenList));
        otherBox.add(Box.createHorizontalGlue());
        otherBox.add(new JLabel("(local)"));
		otherBox.add(new JButton(deleteSpecimenFromList));
		
		JPanel containerPanel2 = new JPanel(new BorderLayout());
		containerPanel2.add(otherBox, BorderLayout.NORTH);
		containerPanel2.add(specimenTablePanel, BorderLayout.CENTER);
		
		fileDropTarget.setTransferHandler(flth);

//        TableTransferHandler tth2 = TableTransferHandler.initialiseForCopySelectAll(specimenTable, true);
//        ChainingTransferHandler cth = new ChainingTransferHandler(flth, genotypeTransferHandler, tth2);
        
        TransferHandler cth = genotypeTransferHandler;
//        ChainingTH cth = new ChainingTH(genotypeTransferHandler, tth2);

        specimenTablePanel.scrollPane.setTransferHandler(cth);

		specimenTablePanel.setTransferHandler(cth);
		specimenTable.setTransferHandler(cth);	
		specimenTable.setDragEnabled(true);

		return containerPanel2;
	}
	
	public void setOwnerWindow(JFrame frame) {
	    if (ownerFrame != null) {
	        throw new IllegalStateException("Already have an ownerFrame");
	    }
	    ownerFrame = frame;
	    if (backgroundRunner instanceof DefaultBackgroundRunner) {
	        DefaultBackgroundRunner dbr = (DefaultBackgroundRunner) backgroundRunner;
	        ownerFrame.setGlassPane(dbr.getBlockingPane());
	    }
	    title = frame.getTitle();
	    frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                setDividerLocation(0.3);
                searcher.addPropertyChangeListener(DatabaseSearcher.PROP_CLIENT_CHANGED, clientChangedListener);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                searcher.removePropertyChangeListener(DatabaseSearcher.PROP_CLIENT_CHANGED, clientChangedListener);
            }   
	    });
	}

    private PropertyChangeListener clientChangedListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            boolean b = searcher.hasClient();
            loadAction.setEnabled(b);
            downloadSpecimens.setEnabled(b);
        }
    };


	/**
	 * @return
	 */
	public List<Specimen> getSelectedSpecimen() {
		
		List<Integer> rows = GuiUtil.getSelectedModelRows(specimenTable);	
		List<Specimen> specimens = new ArrayList<Specimen>();

		if (!rows.isEmpty()) {
			for (Integer row : rows) {
				specimens.add(specimenTableModel.getSpecimenAt(row));
			}
		}		
		return specimens;
	}

	/**
	 * @param specimensTakeFromPlots
	 */
	public void addSpecimens(List<? extends Specimen> specimensTakeFromPlots) {
		specimenTableModel.addSpecimens(specimensTakeFromPlots);
	}

	/**
	 * 
	 */
	public void updateTable() {
		specimenTableModel.fireTableDataChanged();
	}
	
	static public class TitledTablePanel extends JPanel {

		public final JTable table;
		public final PromptScrollPane scrollPane;

		public TitledTablePanel(String title, JTable table, String emptyMessage) {
			super(new BorderLayout());

			this.table = table;
			this.scrollPane = new PromptScrollPane(table, emptyMessage);

			add(GuiUtil.createLabelSeparator(title), BorderLayout.NORTH);
			add(scrollPane, BorderLayout.CENTER);
		}
		
		public PromptScrollPane getScrollPane() {
			return this.scrollPane;
		}
	}
}
