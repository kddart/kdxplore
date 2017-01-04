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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Closure;
import org.apache.poi.ss.usermodel.Workbook;

import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;

import net.pearcan.excel.ExcelUtil;
import net.pearcan.excel.ExcelUtil.SheetInfo;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;

@SuppressWarnings("nls")
public class SheetChooserPanel extends JPanel {
    
    private static final int DEFAULT_VISIBLE_ROWCOUNT = 8;
    
    private final DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> sheetComboBox = new JComboBox<>(cbModel);

    private Workbook workbook;
    
    private String selectedSheetName;

    private final DataPreviewTableModel dataPreviewTableModel = new DataPreviewTableModel(true);
    private final JTable dataPreviewTable = new JTable(dataPreviewTableModel);
    private final PromptScrollPane dataPreviewScrollPane = new PromptScrollPane(dataPreviewTable, "");
    private File excelFile;
    private final Consumer<String> onChoice;
    private final BackgroundRunner backgroundRunner;
    
    private final Action useAction = new AbstractAction("Use") {
        @Override
        public void actionPerformed(ActionEvent e) {
            onChoice.accept(selectedSheetName);
        }
    };

    private SpinnerNumberModel spinnerModel = new SpinnerNumberModel(8, 2, 20, 1);

    public SheetChooserPanel(BackgroundRunner br, Consumer<String> onSheetChosen /*, Component ... more */) {
        super(new BorderLayout());

        this.backgroundRunner = br;
        this.onChoice = onSheetChosen;
        sheetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object item = sheetComboBox.getSelectedItem();
                if (item == null) {
                    selectedSheetName = null;
                }
                else {
                    selectedSheetName = sheetComboBox.getSelectedItem().toString();
                }
                updateUsingSelected();
            }
        });
        spinnerModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateUsingSelected();
            }
        });
        
        GuiUtil.setVisibleRowCount(dataPreviewTable, DEFAULT_VISIBLE_ROWCOUNT);

        Box top = Box.createHorizontalBox();
        top.add(new JLabel("Select Sheet: "));
        top.add(sheetComboBox);
        top.add(Box.createHorizontalGlue());
        top.add(new JLabel("Preview:"));
        top.add(new JSpinner(spinnerModel));
        top.add(Box.createHorizontalGlue());
        top.add(new JButton(useAction));
//        if (more != null) {
//            for (Component c : more) {
//                top.add(c);
//            }
//        }

        add(top, BorderLayout.NORTH);
        add(dataPreviewScrollPane, BorderLayout.CENTER);
        
        useAction.setEnabled(false);
    }
    
    public void loadExcelFile(File file) throws IOException {
        this.excelFile = file;
        workbook = ExcelUtil.getWorkbook(excelFile.getName(), excelFile);
        
        List<SheetInfo> sheetInfos = ExcelUtil.getSheetInfoForAllSheets(workbook);
        for (SheetInfo si : sheetInfos) {
            cbModel.addElement(si.name);
        }
        sheetComboBox.setSelectedIndex(0);
    }

    protected void updateUsingSelected() {
        if (Check.isEmpty(selectedSheetName)) {
            dataPreviewTableModel.clear();
        }
        else {
            BackgroundTask<Either<IOException,List<String[]>>,Void> task = new BackgroundTask<Either<IOException,List<String[]>>,Void>("Loading...", false) {
                @Override
                public Either<IOException,List<String[]>> generateResult(Closure<Void> arg0) throws Exception {
                    List<String[]> result = new ArrayList<>();
                    try {
                        ExcelRowDataProvider rdp = new ExcelRowDataProvider(excelFile, selectedSheetName);
                        try {
                            Optional<String[]> opt = rdp.getHeadings();
                            if (opt.isPresent()) {
                                result.add(opt.get());
                            }
                            for (int i = spinnerModel.getNumber().intValue(); --i >= 0; ) {
                                String[] rd = rdp.getNextRowData();
                                if (rd == null) {
                                    break;
                                }
                                result.add(rd);
                            }
                        } finally {
                            rdp.close();
                        }
                        return Either.right(result);
                    }
                    catch (IOException e1) {
                        return Either.left(e1);
                    }
                }

                @Override
                public void onCancel(CancellationException arg0) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onException(Throwable ex) {
                    MsgBox.warn(SheetChooserPanel.this, ex, "Error Getting Sheet Data");
                }

                @Override
                public void onTaskComplete(Either<IOException,List<String[]>> either) {
                    if (either.isRight()) {
                        dataPreviewTableModel.setData(either.right());
                        int rowCount = dataPreviewTableModel.getRowCount();
                        if (rowCount > 2) {
                            GuiUtil.setVisibleRowCount(dataPreviewTable, Math.min(DEFAULT_VISIBLE_ROWCOUNT, rowCount));
                        }
                        useAction.setEnabled(true);
                    }
                    else {
                        dataPreviewTableModel.clear();
                        dataPreviewScrollPane.setPrompt(either.left().getMessage());   
                        useAction.setEnabled(false);
                    }
                }
            };
            
            backgroundRunner.runBackgroundTask(task);
        }
    }
}
