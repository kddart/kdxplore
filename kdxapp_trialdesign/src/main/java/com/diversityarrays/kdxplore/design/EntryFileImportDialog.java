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
package com.diversityarrays.kdxplore.design;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.excel.ExcelUtil;
import net.pearcan.excel.ExcelUtil.SheetInfo;
import net.pearcan.ui.DefaultBackgroundRunner;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.BackgroundTask;

/**
 * If Excel file:
 * <pre>
 * +---------------------------------+
 * | #Rows: [nnn]     Sheet: [    v] |
 * | --------------------------------|
 * |    data preview shows here      |
 * |                                 |
 * |                                 |
 * | - - - - - - - - - - - - ---- - -|
 * |        Role Assignment here     |
 * |                                 |
 * | --------------------------------|
 * </pre>
 *
 * If CSV file: then Sheet dropdown is hidden
 * </pre>
 *
 * @author brianp
 */
@SuppressWarnings("nls")
public abstract class EntryFileImportDialog<EFile,Role >extends JDialog {

    private static final String CARD_ERROR = "cardError";
    private static final String CARD_DATA = "cardData";

    public EFile entryFile;

    private RowDataProvider rowDataProvider;

    private SpinnerNumberModel previewRowCountSpinnerModel = new SpinnerNumberModel(8, 2, 100, 1);

    private final DefaultComboBoxModel<String> sheetNamesModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> sheetNamesComboBox = new JComboBox<>(sheetNamesModel);
    /**
     * To protect against looping calls to updatePreviewData() when initialising the content
     * of the sheetNamesModel and setting initial selection
     */
    private boolean initialisingSheetNames;
    private ActionListener sheetNamesActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object item = sheetNamesComboBox.getSelectedItem();
            if (item == null) {
                selectedSheetName = null;
            }
            else {
                selectedSheetName = sheetNamesComboBox.getSelectedItem().toString();
            }

            // If we were called because the UpdateDataPreviewTask
            // is populating the combobox model then don't recurse.
            if (! initialisingSheetNames) {
                updateDataPreview(previewRowCountSpinnerModel.getNumber().intValue());
            }
        }
    };

    private final DataPreviewTableModel dataPreviewTableModel = new DataPreviewTableModel();
    private final JTable dataPreviewTable = new JTable(dataPreviewTableModel);
    private final PromptScrollPane dataPreviewScrollPane = new PromptScrollPane(dataPreviewTable, "");

    private final Action acceptAction = new AbstractAction("Accept") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                rowDataProvider.reset();
                entryFile = createEntryFile(rowDataProvider, headingRoleTableModel.getRoleByHeading());
                dispose();
            }
            catch (IOException | EntryFileException e1) {
                MsgBox.error(EntryFileImportDialog.this, e1, getTitle() + " - Error");
            }
        }
    };

    private final Action cancelAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
        @Override
        public void actionPerformed(ActionEvent e) {
            entryFile = null;
            dispose();
        }
    };

    private final HeadingRoleTableModel<Role> headingRoleTableModel;
    private final HeadingRoleTable<Role> headingRoleTable;
    private final JScrollPane headingTableScrollPane;

    private final JLabel headingWarning = new JLabel();
    private final ChangeListener headingRoleChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            Map<String,Role> roleByHeading = headingRoleTableModel.getRoleByHeading();

            Either<String,Set<String>> either = checkValidity(roleByHeading);
            if (either.isLeft()) {
                headingWarning.setText(either.left());
                acceptAction.setEnabled(false);
            }
            else {
                headingWarning.setText("");
                acceptAction.setEnabled(true);
            }
        }
    };

    private final JCheckBox useScrollBarOption = new JCheckBox("Use Scrollbar");

    private final FileDrop fileDrop = new FileDrop() {
        @Override
        public void dropFiles(Component c, List<File> list, DropLocationInfo dli) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setFile(list.get(0));
                }
            });
        }
    };
    private FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

    private final DefaultBackgroundRunner backgroundRunner = new DefaultBackgroundRunner();

    private final JTextArea errorMessage = new JTextArea();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private File file;
    private FileType fileType;

    //For Excel files
    private String selectedSheetName;


    public EntryFileImportDialog(Window owner, String title, File inputFile) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setGlassPane(backgroundRunner.getBlockingPane());

        useScrollBarOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDataPreviewScrolling();
            }
        });


        headingRoleTableModel = createHeadingRoleTableModel();
        headingRoleTable = new HeadingRoleTable<>(headingRoleTableModel);
        headingTableScrollPane = new JScrollPane(headingRoleTable);
        GuiUtil.setVisibleRowCount(headingRoleTable, 10);

        JPanel roleAssignmentPanel = new JPanel(new BorderLayout());
        roleAssignmentPanel.add(headingTableScrollPane, BorderLayout.CENTER);

        headingRoleTable.setTransferHandler(flth);
        headingRoleTableModel.addChangeListener(headingRoleChangeListener);

        GuiUtil.setVisibleRowCount(dataPreviewTable, 10);
        dataPreviewTable.setTransferHandler(flth);
        dataPreviewScrollPane.setTransferHandler(flth);
        updateDataPreviewScrolling();

        dataPreviewScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
//                boolean useScrollBar = useScrollBarOption.isSelected();
                GuiUtil.initialiseTableColumnWidths(dataPreviewTable, true);
            }
        });

        Box top = Box.createHorizontalBox();
        top.add(new JLabel("# rows to preview: "));
        top.add(new JSpinner(previewRowCountSpinnerModel));
        top.add(Box.createHorizontalGlue());
        top.add(useScrollBarOption);

        JPanel dataPreviewPanel = new JPanel(new BorderLayout());
        dataPreviewPanel.add(GuiUtil.createLabelSeparator("Data Preview", top), BorderLayout.NORTH);
        dataPreviewPanel.add(dataPreviewScrollPane, BorderLayout.CENTER);

        headingWarning.setForeground(Color.RED);
        JLabel instructions = new JLabel("<HTML>Put instructions on usage here");
        instructions.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane instScroll = new JScrollPane(instructions);
        JSplitPane headingsAndInstructions = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                roleAssignmentPanel, instScroll);

        JPanel headingPanel = new JPanel(new BorderLayout());
        headingPanel.add(GuiUtil.createLabelSeparator("Assign Roles for Headings"), BorderLayout.NORTH);
        headingPanel.add(headingsAndInstructions, BorderLayout.CENTER);
        headingPanel.add(headingWarning, BorderLayout.SOUTH);

        errorMessage.setEditable(false);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                dataPreviewPanel,
                headingPanel);
        splitPane.setResizeWeight(0.5);

        cardPanel.add(new JScrollPane(errorMessage), CARD_ERROR);
        cardPanel.add(splitPane, CARD_DATA);

        Box bot = Box.createHorizontalBox();
        bot.add(Box.createHorizontalGlue());
        bot.add(new JButton(cancelAction));
        bot.add(new JButton(acceptAction));
        acceptAction.setEnabled(false);

        Container cp = getContentPane();
        cp.add(cardPanel, BorderLayout.CENTER);
        cp.add(bot, BorderLayout.SOUTH);
        pack();

        sheetNamesComboBox.addActionListener(sheetNamesActionListener);

        Timer timer = new Timer(true);

        previewRowCountSpinnerModel.addChangeListener(new ChangeListener() {
            int nPreview;
            TimerTask timerTask = null;
            @Override
            public void stateChanged(ChangeEvent e) {
                nPreview = previewRowCountSpinnerModel.getNumber().intValue();

                if (timerTask == null) {
                    timerTask = new TimerTask() {
                        int lastPreviewCount = nPreview;
                        @Override
                        public void run() {
                            if (lastPreviewCount == nPreview) {
                                System.err.println("Stable at " + lastPreviewCount);
                                // No change, do it now
                                cancel();
                                try {
                                    updateDataPreview(lastPreviewCount);
                                }
                                finally {
                                    timerTask = null;
                                }
                            }
                            else {
                                System.err.println("Changing from " + lastPreviewCount + " to " + nPreview);
                                lastPreviewCount = nPreview;
                            }
                        }
                    };
                    timer.scheduleAtFixedRate(timerTask, 500, 100);
                }
            }
        });

        sheetNamesComboBox.setVisible(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                removeWindowListener(this);
                setFile(inputFile);
            }
        });
    }


    private void updateDataPreviewScrolling() {
        boolean useScrollBar = useScrollBarOption.isSelected();
        dataPreviewTable.setAutoResizeMode(
                useScrollBar
                ? JTable.AUTO_RESIZE_OFF
                : JTable.AUTO_RESIZE_ALL_COLUMNS);
        if (! useScrollBar) {
//            System.out.println("initTableCOlumns: useScrollBar=" + useScrollBar);
            GuiUtil.initialiseTableColumnWidths(dataPreviewTable, true);
        }
    }

    private void setFile(File f) {
        setTitle(f.getName());

        dataPreviewTableModel.clear();

        this.file = f;
        if (Shared.NEW_EXCEL_FILE_FILTER.accept(file)) {
            fileType = FileType.EXCEL;
        }
        else if (Shared.CSV_FILE_FILTER.accept(file) || Shared.TXT_FILE_FILTER.accept(file)) {
            fileType = FileType.CSV_OR_TXT;
        }
        else {
            fileType = FileType.OTHER;
        }

        dataPreviewTableModel.setUseLettersForColumnNames(FileType.EXCEL == fileType);

        updateDataPreview(previewRowCountSpinnerModel.getNumber().intValue());
    }

    private void updateDataPreview(int nPreview) {
        UpdateDataPreviewTask task = new UpdateDataPreviewTask(nPreview);
        backgroundRunner.runBackgroundTask(task);
    }

    class UpdateDataPreviewTask extends BackgroundTask<Pair<String,Exception>,Void> {

        private int nPreviewRows;

        private String[] headingRow;
        private List<String[]> previewRows = new ArrayList<>();

        private List<String> sheetNames;

        UpdateDataPreviewTask(int nPreviewRows) {
            super("Loading...", false);

            this.nPreviewRows = nPreviewRows;
        }

        @Override
        public Pair<String,Exception> generateResult(Closure<Void> arg0) throws Exception {

            switch (fileType) {
            case CSV_OR_TXT:
                try {
                    rowDataProvider = new CsvRowDataProvider(file);
                }
                catch (IOException e) {
                    return new Pair<>("Error reading CSV/Text", e);
                }
                break;

            case EXCEL:
                try {
                    List<String> sheetNames = loadExcelFile();
                    if (sheetNames.isEmpty()) {
                    }
                    else {
                        selectedSheetName = sheetNames.get(0);
                        try {
                            rowDataProvider = new ExcelRowDataProvider(file, selectedSheetName);
                        }
                        catch (IOException e) {
                            return new Pair<>("Error reading Excel", e);
                        }
                    }
                }
                catch (IOException e) {
                    return new Pair<>("Unable to read Excel file", e);
                }
                break;

            default:
                throw new IllegalStateException("fileType=" + fileType);
            }

            Optional<String[]> opt = rowDataProvider.getHeadings();
            if (opt.isPresent()) {

                headingRow = opt.get();

                try {
                    for (int i = nPreviewRows; --i >= 0; ) {
                        String[] rd = rowDataProvider.getNextRowData();
                        if (rd == null) {
                            break;
                        }
                        previewRows.add(rd);
                    }
                }
                catch (IOException e) {
                    return new Pair<>("Problem reading preview data", e);
                }
            }

            return null;
        }

        @Override
        public void onCancel(CancellationException ignore) {
        }

        @Override
        public void onException(Throwable e) {
            initUIwithException("Problem Initialising", e);
        }

        @Override
        public void onTaskComplete(Pair<String,Exception> pair) {
            dataPreviewTableModel.clear();
            headingRoleTableModel.setHeadingsAndData(null, null);

            if (pair == null) {
                if (previewRows.isEmpty()) {
                    // no first data row
                    headingRoleTableModel.setHeadingsAndData(headingRow, null);
                    errorMessage.setText("No Data Rows");
                    cardLayout.show(cardPanel, CARD_ERROR);
                }
                else {
                    if (! Check.isEmpty(sheetNames)) {
                        initialisingSheetNames = true;
                        try {
                            sheetNamesModel.removeAllElements();
                            for (String s : sheetNames) {
                                sheetNamesModel.addElement(s);
                            }
                            sheetNamesComboBox.setSelectedIndex(0);
                        }
                        finally {
                            initialisingSheetNames = false;
                        }
                    }

                    headingRoleTableModel.setHeadingsAndData(headingRow, previewRows.get(0));
                    previewRows.add(0, headingRow);
                    dataPreviewTableModel.setData(previewRows);
                    GuiUtil.initialiseTableColumnWidths(dataPreviewTable);
                    cardLayout.show(cardPanel, CARD_DATA);
                }
            }
            else if (pair.second == null) {
                errorMessage.setText(pair.first);
                cardLayout.show(cardPanel, CARD_ERROR);
            }
            else {
                initUIwithException(pair.first, pair.second);
            }
        }
    }

    private void initUIwithException(String hdg, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(hdg);
        e.printStackTrace(pw);
        pw.close();

        errorMessage.setText(sw.toString());
        cardLayout.show(cardPanel, CARD_ERROR);
    }

    private List<String> loadExcelFile() throws IOException {
      List<String> sheetNames = new ArrayList<>();
      for (SheetInfo si : ExcelUtil.getSheetInfoForAllSheets(file)) {
          sheetNames.add(si.name);
      }
      return sheetNames;
  }

    abstract protected Either<String, Set<String>> checkValidity(Map<String,Role> rbh);
    abstract protected HeadingRoleTableModel<Role> createHeadingRoleTableModel();

    abstract protected EFile createEntryFile(RowDataProvider rdp, Map<String,Role> roleByHeading)
            throws IOException, EntryFileException;
}
