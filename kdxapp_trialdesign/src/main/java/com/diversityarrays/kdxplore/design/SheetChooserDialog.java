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
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;

import com.diversityarrays.util.Check;

import net.pearcan.util.BackgroundRunner;

@SuppressWarnings("nls")
public class SheetChooserDialog extends JDialog {
    
    public String selectedSheetName;
    
    private final Action okAction = new AbstractAction("Import") {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };
    private final Action cancelAction = new AbstractAction("Import") {
        @Override
        public void actionPerformed(ActionEvent e) {
            selectedSheetName = null;
            dispose();
        }
    };
    
    
    private SheetChooserPanel sheetChooserPanel;

    private Consumer<String> onSheetChosen = new Consumer<String>() {
        @Override
        public void accept(String t) {
            selectedSheetName = t;
            okAction.setEnabled(! Check.isEmpty(t));
        }
    };
    
    public SheetChooserDialog(Window owner, String title, File excelFile, BackgroundRunner br) throws FileNotFoundException, IOException {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        
        sheetChooserPanel = new SheetChooserPanel(br, onSheetChosen);

        Box btns = Box.createHorizontalBox();
        btns.add(Box.createHorizontalGlue());
        btns.add(new JButton(cancelAction));
        btns.add(new JButton(okAction));
        
        Container cp = getContentPane();
        cp.add(sheetChooserPanel, BorderLayout.CENTER);
        cp.add(btns, BorderLayout.SOUTH);
        pack();
        
        updateOkAction();
        
        sheetChooserPanel.loadExcelFile(excelFile);
    }

    protected void updateOkAction() {
        okAction.setEnabled(! Check.isEmpty(selectedSheetName));
    }
}
