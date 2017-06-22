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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;

import com.diversityarrays.kdxplore.Shared;

public class MsgBox {

    static public interface ProblemReporter {
        /**
         * The text for the button to invoke the <code>reportProblem</code> method.
         * @return
         */
        String getActionLabel();

        /**
         * Do what is needed to report the problem.
         * @param title
         * @param error
         * @param thread
         */
        void reportProblem(String title, Throwable error, Thread thread);
    }

    /**
     * Set this somewhere else to get standardised error reporting.
     */
    static public ProblemReporter DEFAULT_PROBLEM_REPORTER = null;

    public static void info(Component parentComponent, Object msg, String title) {
        info(parentComponent, msg, title, null);
    }

    public static void info(Component parentComponent, Object msg, String title,
            ProblemReporter problemReporter)
    {
        if (msg instanceof Throwable) {
            Shared.Log.i("MESSAGE", title, (Throwable) msg); //$NON-NLS-1$
            showThrowable(JOptionPane.INFORMATION_MESSAGE,
                    parentComponent,
                    title,
                    (Throwable) msg,
                    problemReporter);
        }
        else {
            Shared.Log.i("MESSAGE", title + ": " + msg); //$NON-NLS-1$  //$NON-NLS-2$
        }
        JOptionPane.showMessageDialog(parentComponent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parentComponent, Object msg, String title) {
        warn(parentComponent, msg, title, null);
    }

    public static void warn(Component parentComponent, Object msg, String title,
            ProblemReporter problemReporter)
    {
        if (msg instanceof Throwable) {
            Shared.Log.e("WARN", title, (Throwable) msg); //$NON-NLS-1$

            showThrowable(JOptionPane.WARNING_MESSAGE,
                    parentComponent,
                    title,
                    (Throwable) msg,
                    problemReporter);
        }
        else {
            Shared.Log.e("WARN", title + ": " + msg); //$NON-NLS-1$  //$NON-NLS-2$
        }
        JOptionPane.showMessageDialog(parentComponent, msg, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parentComponent, Object msg, String title) {
        error(parentComponent, msg, title, null);
    }

    public static void error(Component parentComponent, Object msg, String title,
            ProblemReporter problemReporter)
    {
        if (msg instanceof Throwable) {
            Shared.Log.e("ERROR", title, (Throwable) msg); //$NON-NLS-1$

            showThrowable(JOptionPane.ERROR_MESSAGE,
                    parentComponent,
                    title,
                    (Throwable) msg,
                    problemReporter);
        }
        else {
        	Shared.Log.e("ERROR", title + ": " + msg); //$NON-NLS-1$  //$NON-NLS-2$
            JOptionPane.showMessageDialog(parentComponent, msg, title,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showThrowable(int messageType,
            Component parentComponent,
            String title,
            Throwable t,
            ProblemReporter reporter)
    {
        showThrowable(messageType, parentComponent, title, t, reporter, null);
    }

    public static void increaseJTextPaneFont(JEditorPane editorPane, float multiplier) {

        Document doc = editorPane.getDocument();
        if (doc instanceof HTMLDocument) {
            HTMLDocument hdoc = (HTMLDocument) doc;
            Font font = editorPane.getFont();
            font = font.deriveFont(font.getSize2D() * multiplier);
            String bodyRule = String.format(
                    "body { font-family: %s; font-size: %dpt; }", //$NON-NLS-1$
                    font.getFamily(),
                    font.getSize());
            hdoc.getStyleSheet().addRule(bodyRule);
        }

    }

    static private final String[] PROPERTY_NAMES = {
            "java.version", //$NON-NLS-1$
            "java.vendor", //$NON-NLS-1$
            "java.home", //$NON-NLS-1$
            "java.class.version", //$NON-NLS-1$
            "os.name", //$NON-NLS-1$
            "os.arch", //$NON-NLS-1$
//            "java.class.path", //$NON-NLS-1$
    };

    public static void showThrowable(int messageType,
            Component parentComponent,
            String title,
            Throwable error,
            ProblemReporter reporter,
            Thread thread)
    {

        if (reporter == null) {
            reporter = DEFAULT_PROBLEM_REPORTER;
        }
        ProblemReporter problemReporter = reporter;

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        for (String propertyName : PROPERTY_NAMES) {
            pw.println(propertyName + ":\t" + System.getProperty(propertyName)); //$NON-NLS-1$
        }
        pw.println();
        error.printStackTrace(pw);
        pw.close();

        JTextArea stacktrace = new JTextArea(sw.toString(), 20, 0);
        stacktrace.setEditable(false);
        JScrollPane sp = new JScrollPane(stacktrace);
        sp.setVisible(false);


        Action submitReport = null;
        if (problemReporter != null) {
            submitReport = new AbstractAction(problemReporter.getActionLabel()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    problemReporter.reportProblem(title, error, thread);
                }
            };
        }

        /*
           +------------------------------------------+
           | [Throwable.getMessage()                  |
           | (Error class) [ ] details       (submit) |
           | .______________________________________. |
           | |  stackTrace shows here               | |
           | |                                      | |
           | |                                      | |
           | |______________________________________| |
           |                                          |
           +------------------------------------------+
         */

        String tmsg = error.getMessage();
        JLabel msgLabel = new JLabel(tmsg==null ? "": tmsg, SwingConstants.LEFT); //$NON-NLS-1$
        msgLabel.setForeground(Color.RED);

        JLabel clsLabel = new JLabel(error.getClass().getName(), SwingConstants.LEFT);
        JCheckBox showDetails = new JCheckBox(Msg.ERROR_DETAILS());

        Box classAndCheckbox = Box.createHorizontalBox();
        classAndCheckbox.add(clsLabel);
        classAndCheckbox.add(showDetails);
        classAndCheckbox.add(Box.createHorizontalGlue());
        if (submitReport != null) {
            classAndCheckbox.add(new JButton(submitReport));
        }

        JPanel panel1 = new JPanel(new BorderLayout());
        panel1.add(classAndCheckbox, BorderLayout.NORTH);
        panel1.add(sp, BorderLayout.CENTER);

        JPanel panel2 = new JPanel(new BorderLayout());
        panel2.add(msgLabel, BorderLayout.NORTH);
        panel2.add(panel1, BorderLayout.CENTER);


        JOptionPane optPane = new JOptionPane(panel2, messageType);
        JDialog dlg = optPane.createDialog(parentComponent, title);
        showDetails.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sp.setVisible(showDetails.isSelected());
                dlg.pack();
            }
        });

        dlg.setResizable(true);
        dlg.setVisible(true);
    }

    private MsgBox() {}

}
