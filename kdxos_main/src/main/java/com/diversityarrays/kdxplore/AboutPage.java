/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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
package com.diversityarrays.kdxplore;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.io.IOUtil;
import net.pearcan.util.StringTemplate;
import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class AboutPage extends JDialog {
    
    static enum Licence {
        APACHE_2("Apache License Version 2.0",
                "collections-generic-4.01.jar",
                "commons-codec-1.6.jar",
                "commons-logging-1.2.jar",
                "dalclient-core.jar", // Github kddart/libJava-DAL
                "dalclient-javase.jar", // Github kddart/libJava-DAL
                "gson-2.3.1.jar",
                "httpclient-4.3.jar",
                "httpclient-cache-4.3.jar",
                "httpcore-4.3.jar",
                "httpmime-4.3.jar",
                "l2fprod-common-all.jar",
                "opencsv-2.3.jar"
                ),

        EPL_1_0("Eclipse Public License 1.0", // MPL_2.0 (Mozilla)
                "javax.persistence-2.0.0.jar"),

        GLGPL_2_1("Gnu Lesser GPL 2.1",
                "swingx-all-1.6.4.jar"
                ),
        ;

        final String licenceFileName;
        final String[] jarNames;
        Licence(String lfn, String ... jars) {
            licenceFileName = lfn;
            jarNames = jars;
        }
    }
    
    static private final String ABOUT_HTML = "about.html";
    
    private final Action closeAction = new AbstractAction(UnicodeChars.CANCEL_CROSS) {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };
    private final Action licencesAction = new AbstractAction("Show Licences") {
        @Override
        public void actionPerformed(ActionEvent e) {
            MsgBox.info(AboutPage.this, new JScrollPane(licences), getTitle());
        }
    };

    private final JLabel licences;
    
    AboutPage(String title, Window owner, String appName, String displayVersion) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        
        Map<String, String> tokenValues = getTokenValues(displayVersion);

        final JLabel label = getAboutFromTemplate(tokenValues);
        licences = setSmallFont(new JLabel(getLicencesHtmlList(appName)));

        Box buttons = Box.createHorizontalBox();
        buttons.add(new JButton(licencesAction));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(new JButton(closeAction));
        
        Container cp = getContentPane();
        cp.add(label, BorderLayout.CENTER);
        cp.add(buttons, BorderLayout.SOUTH);
        pack();
        
    }
    
    public JLabel getAboutFromTemplate(Map<String, String> tokenValues) {

        JLabel label = null;

        InputStream is = getClass().getResourceAsStream(ABOUT_HTML);
        if (is != null) {
            try {
                StringTemplate template = new StringTemplate(IOUtil.readAsString(is));
                label = new JLabel(template.replaceTokens(tokenValues));
            }
            catch (IOException | RuntimeException e) {
            }
        }
        
        if (label == null) {
            StringBuilder sb = new StringBuilder("<HTML>KDXplore");
            sb.append("<BR>");
            sb.append("Copyright (c) 2015, 2016 Diversity Arrays Technology (DArT).");
            sb.append("<BR>");
            
            for (String key : tokenValues.keySet()) {
                sb.append("<BR><B>").append(StringUtil.htmlEscape(key))
                .append("</B>: ")
                .append(StringUtil.htmlEscape(tokenValues.get(key)));
            }
            label = new JLabel(sb.toString());
        }

        return setSmallFont(label);
    }
    
    private JLabel setSmallFont(JLabel label) {
        label.setBorder(new EmptyBorder(2, 4, 2, 4));
        label.setFont(label.getFont().deriveFont(10f));
        return label;
    }

    public Map<String, String> getTokenValues(String displayVersion) {
        Map<String,String> tokenValues = new LinkedHashMap<>();
        tokenValues.put("displayVersion", displayVersion);

        Map<String, String> attrs = KdxConstants.getKdxploreManifestAttributes();
        // default some values
        tokenValues.put(KdxConstants.BUILD_DATE, "not-available");
        tokenValues.put(KdxConstants.BUILT_BY, "not-available");
        for (String key : attrs.keySet()) {
            if (KdxConstants.BUILD_DATE.equals(key) || KdxConstants.BUILT_BY.equals(key)) {
                tokenValues.put(key, attrs.get(key));
            }
        }
        
        return tokenValues;
    }
    
    private String getLicencesHtmlList(String appName) {
        StringBuilder sb = new StringBuilder("<HTML>");
        
        sb.append("Libraries used by " + appName + ":<hr>");
        sb.append("<p>");

        sb.append("Libraries by Licence:");
        sb.append("<UL>");
        for (Licence lic : Licence.values()) {
            sb.append("<li>").append(StringUtil.htmlEscape(lic.licenceFileName));
            if (lic.jarNames.length == 1) {
                sb.append("<br>").append(getBasename(lic.jarNames[0]));
            }
            else {
                sb.append("<br>");
                String sep = "";
                int length = 0;
                for (String jar : lic.jarNames) {
                    String name = getBasename(jar);
                    sb.append(sep).append(name);
                    length += name.length();
                    if (length > 60) {
                        sep = "<br>";
                        length = 0;
                    }
                    else {
                        sep = ", ";
                    }
                }
            }
            
            sb.append("</li>");
        }
        
        sb.append("</UL>");
        sb.append("<b>NOTE</b>");
        sb.append("<br>The full text of individual Licences may be found in the directory");
        sb.append(" of that name in the <i>" + appName + "</i> distribution.");
        return sb.toString();
    }

    private String getBasename(String jar) {
        Pattern basePattern = Pattern.compile("^(.*)-[\\.0-9]+\\.jar$");
        
        String result = jar;
        Matcher m = basePattern.matcher(jar);
        if (m.matches()) {
            Pattern p2 = Pattern.compile("^(.*)-[\\.0-9]+$");
            while (m.matches()) {
                result = m.group(1);
                m = p2.matcher(result);
            }
        }
        else if (jar.endsWith(".jar")) {
            result = result.substring(0, result.length()-4);
        }
        return result;
    }
}
