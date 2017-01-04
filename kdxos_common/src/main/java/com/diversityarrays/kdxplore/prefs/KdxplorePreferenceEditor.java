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
package com.diversityarrays.kdxplore.prefs;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.DesktopObject;

public class KdxplorePreferenceEditor extends JPanel implements DesktopObject {

    // TODO Try using PreferenceTreeTablePanel instead of this
	private final PreferenceTreePanel preferenceTreePanel = new PreferenceTreePanel();
    private final String title;

	public KdxplorePreferenceEditor(String title) {
		super(new BorderLayout());
		
		this.title = title;
		add(preferenceTreePanel, BorderLayout.CENTER);
	}

	@Override
	public JPanel getJPanel() {
		return this;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public JMenuBar getJMenuBar() {
		return null;
	}

	@Override
	public JToolBar getJToolBar() {
		return null;
	}

	@Override
	public void doPostOpenActions() {
		preferenceTreePanel.doPostOpenActions();
	}

	@Override
	public boolean canClose() {
		return true;
	}

	@Override
	public boolean isClosable() {
		return true;
	}

	@Override
	public Object getWindowIdentifier() {
		return KdxplorePreferences.class;
	}


    public static void startEditorDialog(JComponent comp, String title, KdxPreference<?> pref) {
        KdxplorePreferenceEditor editor = new KdxplorePreferenceEditor(title);
        editor.preferenceTreePanel.setInitialPreference(pref);
        JDialog dlg = new JDialog(GuiUtil.getOwnerWindow(comp),
                title, ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setContentPane(editor.getJPanel());
        dlg.pack();
        dlg.setVisible(true);
    }

	
	// = = = = = = =

}
