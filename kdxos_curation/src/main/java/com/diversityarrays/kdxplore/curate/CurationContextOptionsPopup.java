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
package com.diversityarrays.kdxplore.curate;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

@SuppressWarnings("nls")
public class CurationContextOptionsPopup extends JPopupMenu {

    JRadioButtonMenuItem useNameOption = new JRadioButtonMenuItem("Show Trait Name");
    JRadioButtonMenuItem useAliasOption = new JRadioButtonMenuItem("Show Trait Alias (when available)");
    JCheckBoxMenuItem includeLevelOption = new JCheckBoxMenuItem("Show Trait Level");
    
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (useNameOption == src || useAliasOption == src) {
                curationContext.setShowAliasForTraits(useAliasOption.isSelected());
            }
            else if (includeLevelOption == src) {
                curationContext.setShowTraitLevelPrefix(includeLevelOption.isSelected());
            }
        }
    };
    
    private final CurationContext curationContext;
    private final Component component;
    
    private final MouseListener mouseListener = new MouseAdapter() {   
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (2 == e.getClickCount()) {
                e.consume();
                curationContext.setShowAliasForTraits(! curationContext.getShowAliasForTraits());
            }
            else if (SwingUtilities.isRightMouseButton(e)) {
                Point pt = e.getPoint();
                show(component, pt.x, pt.y);
            }
        }
    };

    public CurationContextOptionsPopup(CurationContext ctx, Component comp) {
        this.curationContext = ctx;
        this.component = comp;
        
        if (component instanceof JComponent) {
            ((JComponent) component).setToolTipText("<HTML>Double-click to toggle showing <i>Trait Name</i> vs <i>Trait Alias</i> (when available)"
                + "<br>Right-click for menu of options");
        }
        
        component.addMouseListener(mouseListener);
        
        add(useNameOption);
        add(useAliasOption);
        addSeparator();
        add(includeLevelOption);

        ButtonGroup bg = new ButtonGroup();
        bg.add(useNameOption);
        bg.add(useAliasOption);

        useNameOption.addActionListener(actionListener);
        useAliasOption.addActionListener(actionListener);
        includeLevelOption.addActionListener(actionListener);
    }
    
    @Override
    public void show(Component c, int x, int y) {
        useAliasOption.setSelected(curationContext.getShowAliasForTraits());
        useNameOption.setSelected(! useAliasOption.isSelected());                
        includeLevelOption.setSelected(curationContext.getShowTraitLevelPrefix());
        
        super.show(c, x, y);
    }
}
