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
package com.diversityarrays.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;

public class ReportIssueAction extends AbstractAction {

    static public final String REPORT_ISSUE_NAME = "Report Issue (on GitHub)";

    static private final String REPORT_ISSUE_URL = "https://github.com/kddart/kdxplore/issues/new";

    private final String url;
    private final Component comp;

    public ReportIssueAction(Component comp) {
        this(REPORT_ISSUE_NAME, comp, REPORT_ISSUE_URL);
    }

    public ReportIssueAction(String name, Component comp) {
        this(name, comp, REPORT_ISSUE_URL);
    }

    public ReportIssueAction(String name, Component comp, String url) {
        super(name);
        this.comp = comp;
        this.url = url;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        }
        catch (IOException | URISyntaxException e1) {
            MsgBox.warn(comp, 
                    e1, "Unable to open " + url);
            e1.printStackTrace();
        }
    }
}
