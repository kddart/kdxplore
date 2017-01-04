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

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.diversityarrays.kdxplore.KdxConstants;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.MsgBox.ProblemReporter;

@SuppressWarnings("nls")
public class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler, ProblemReporter {

    private static final String END_OF_STACKTRACE = "-- End of Stacktrace --";
    
    static private final String[] PROPERTY_NAMES = {
            "java.version",
            "java.vendor",
            "java.home",
            "java.class.version",
            "os.name",
            "os.arch",
            "java.class.path",
    };
    
    private final JFrame frame;
    private final String baseFilename;
    private final String emailTo;
    private final String softwareVersion;

    public DefaultUncaughtExceptionHandler(JFrame frame, String baseFilename, String emailTo, String softwareVersion) {
        this.frame = frame;
        this.baseFilename = baseFilename;
        this.emailTo = emailTo;
        this.softwareVersion = softwareVersion;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        error.printStackTrace();
        MsgBox.showThrowable(JOptionPane.ERROR_MESSAGE, 
                frame, 
                "Uncaught Exception: Please email to " + emailTo,
                error, 
                this);
    }

    // ===== ProblemReporter =====
    @Override
    public String getActionLabel() {
        return "Create Report";
    }

    @Override
    public void reportProblem(String title, Throwable error, Thread thread) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File file = new File(System.getProperty("user.home"), 
                baseFilename +"_" + timeStamp + ".txt");
        
        PrintWriter pw;
        try {
            pw = new PrintWriter(file);
        }
        catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(frame, 
                    "Unable to create error report\n" + e.getMessage(),
                    title, 
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add(System.getProperty("user.home"));
        
        pw.println("Error Report in " + file.getPath());
        pw.println();
        if (emailTo != null && ! emailTo.isEmpty()) {
            pw.println("Please send this file to " + emailTo);
            pw.println();
        }
        pw.println("Environment Information:");
        for (String propertyName : PROPERTY_NAMES) {
            pw.println(propertyName + ":\t" + System.getProperty(propertyName));
        }
        pw.println();
        pw.println("Error Title: " + title);
        pw.println("Software Version: " + softwareVersion);
        pw.println("Build-Date: " + KdxConstants.getKdxploreBuildDate());
        
        if (thread == null) {
            pw.println("StackTrace Follows:");
            error.printStackTrace(pw);
            pw.println(END_OF_STACKTRACE);
        }
        else {
            pw.println(thread.getName());
            try {
                StackTraceElement[] stackTrace = thread.getStackTrace();
                pw.println("StackTrace: depth=" + stackTrace.length);
                for (int index = 0; index < stackTrace.length; ++index) {
                    StackTraceElement ste = stackTrace[index];
                    pw.println("[" + index + "] " + ste.toString());
                }
                pw.println(END_OF_STACKTRACE);
            }
            catch (SecurityException e) {
                pw.println("StackTrace: can't get thread stacktrace: " + e.getMessage());
                error.printStackTrace(pw);
                pw.println(END_OF_STACKTRACE);
            }
        }

        pw.close();
        
        String message = "Error report is in\n" + file.getPath();
        if (Desktop.isDesktopSupported()) {
            String openFolderOption = "Open Folder";
            String[] options = {
                    "Close",
                    openFolderOption,
                    "Open Report File"
            };
            
            int choice = JOptionPane.showOptionDialog(frame, 
                    message, 
                    title, 
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, 
                    options, openFolderOption);
            if (choice == 1 || choice == 2) {
                try {
                    File openThis = choice==1 ? file.getParentFile() : file;
                    Desktop.getDesktop().open(openThis);
                }
                catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, 
                            "Unable to open folder", title, 
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(frame, 
                    message, title, 
                    JOptionPane.INFORMATION_MESSAGE);
        }
        

    }

}
