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
package com.diversityarrays.kdxplore;

import java.awt.Component;
import java.io.File;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;

import net.pearcan.io.SuffixFileFilter;
import net.pearcan.ui.FileChooserFactory;

/**
 * <b>File Choosers</b>
 * <p>
 * Usage Pattern:<br>
 * <pre>
 *    JFileChooser fc = Shared.getFileChooser( For.xyz [, optional FileFilters ] );
 *    if (JFileChooser.APPROVE_OPTION == fc.showXxx()) {
 *        File chosenFile = fc.getSelectedFile();
 *        // If you want the chosen directory to be preserved in this run...
 *        Shared.setCurrentChooserDir(chosenFile.getParentFile());
 *        // If you want the chosen directory to be preserved in future runs...
 *        Shared.setCurrentChooserDir(chosenFile.getParentFile());
 *        
 *        ... use <i>chosenFile</i> ...
 *    }
 * </pre>
 * @author brianp
 */
@SuppressWarnings("nls")
public class Shared {
    
    public static final String SUFFIX_JSON = ".json";
    public static final String SUFFIX_TXT = ".txt";
    static public final String SUFFIX_XLSX = ".xlsx";
    static public final String SUFFIX_XLS = ".xls";

    /**
     * An instance of FileFilter which accepts Excel files (*.xls).
     */
    static public final SuffixFileFilter XLS_FILE_FILTER = new SuffixFileFilter(SUFFIX_XLS, "Excel (*.xls)");

    /**
     *  An instance of FileFilter which accepts only Excel files (*.xlsx).
     */
    static public final SuffixFileFilter XLSX_FILE_FILTER = new SuffixFileFilter( SUFFIX_XLSX , "Excel file");

    /**
     * An instance of FileFilter which accepts Any Excel files (*.xl?).
     */
    static public final SuffixFileFilter OLD_EXCEL_FILE_FILTER = new SuffixFileFilter(
            new String[] { ".xls", ".xlt", ".xla" }, 
            "Excel (*.xls, *.xlt, *.xla)");

    /**
     *  An instance of FileFilter which accepts both new and old format Excel files (*.xls?).
     */
    static public final SuffixFileFilter NEW_EXCEL_FILE_FILTER = new SuffixFileFilter(
            new String[] { ".xls", SUFFIX_XLSX, ".xlsm", ".xla", ".xlam", ".xtl", ".xltm" }, 
            "Excel files");

    /**
     * An instance of FileFilter which accepts Text files (*.txt).
     */
    static public final SuffixFileFilter TXT_FILE_FILTER = new SuffixFileFilter(SUFFIX_TXT, "Text files");
    
    /**
     * An instance of FileFilter which accepts JSON files (*.json).
     */
    static public final SuffixFileFilter JSON_FILE_FILTER = new SuffixFileFilter(SUFFIX_JSON, "JSON files");

    /**
     * An instance of FileFilter which accepts CSV files (*.csv).
     */
    static public final SuffixFileFilter CSV_FILE_FILTER = new SuffixFileFilter(".csv", "CSV files");
    
    /**
     * An instance of FileFilter which accepts ZIP files (*.zip).
     */
    static public final SuffixFileFilter ZIP_FILTER = new SuffixFileFilter(".zip", "ZIP files");

    /**
     * An instance of FileFilter which accepts KDXchange files (*.kdx).
     */
    static public final SuffixFileFilter KDX_FILTER = new SuffixFileFilter(".kdx", "KDXchange files");

	
	static public enum For {
	    MULTI_FILE_LOAD,
		FILE_LOAD,
		LOAD_FILE_OR_DIR,
		
	    FILE_SAVE,
	    DIR_SAVE, 
	    
	    SAVE_FILE_OR_DIR, 
	    ;

		public boolean isForInput() {
			return this==FILE_LOAD || this==MULTI_FILE_LOAD || this==For.LOAD_FILE_OR_DIR;
		}
		
		public boolean isForDirectory() {
			return this==DIR_SAVE;
		}
	}

	static private FileChooserFactory fileChooserFactory;

	static public JFileChooser getFileChooser(For forWhat, FileFilter ... fileFilters) {
		FileChooserFactory fcf = getFileChooserFactory();

		JFileChooser fc;
		switch (forWhat) {
        case DIR_SAVE:
            fc = fcf.getDirectoryChooser(forWhat.name());
            fc.setMultiSelectionEnabled(false);
            break;

        case FILE_LOAD:
            fc = fcf.getInputFileChooser(forWhat.name(), fileFilters);
            break;

        case MULTI_FILE_LOAD:
            fc = fcf.getInputFileChooser(forWhat.name(), fileFilters);
            fc.setMultiSelectionEnabled(true);
            break;

        case LOAD_FILE_OR_DIR:
            fc = fcf.getInputFileChooser(forWhat.name(), fileFilters);
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setMultiSelectionEnabled(false);
            break;
            
        case SAVE_FILE_OR_DIR:
            fc = fcf.getOutputFileChooser(forWhat.name(), fileFilters);
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setMultiSelectionEnabled(false);
            break;

        case FILE_SAVE:
            fc = fcf.getOutputFileChooser(forWhat.name(), fileFilters);
            fc.setMultiSelectionEnabled(false);
            break;

        default:
            throw new RuntimeException("Unhandled: For." + forWhat.name()); //$NON-NLS-1$
		}

		return fc;
	}
	
	static public File chooseFileToOpen(Component parent, FileFilter ... fileFilters) {
		JFileChooser fc = getFileChooser(For.FILE_LOAD, fileFilters);
		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(parent)) {
			return fc.getSelectedFile();
		}
		return null;
	}
	
	static public void setCurrentChooserDir(File dir) {
		if (dir != null) {
			getFileChooserFactory().setCurrentDir(dir);
		}
	}

	public static FileChooserFactory getFileChooserFactory() {
		if (fileChooserFactory == null) {
			fileChooserFactory = new FileChooserFactory();
		}
		return fileChooserFactory;
	}
	

    public static class Log {
        
        public static final int VERBOSE = 99;
        public static final int DEBUG = Integer.MAX_VALUE;

        private static Level LOGGING_LEVEL = Level.INFO;

        static public void setLoggingLevel(Level level) {
            LOGGING_LEVEL = level==null ? Level.FINE : level;
            if (logger != null) {
                logger.setLevel(LOGGING_LEVEL);
            }
        }
        
        private static Logger logger;
        
        static public void setLogger(Logger l) {
            logger = l;
            if (logger != null) {
                logger.setLevel(LOGGING_LEVEL);
            }
        }
        
        public static Logger getLogger() {
            return logger;
        }

    //  public static boolean isLoggable(String tag, int level) {
//          // TODO Auto-generated method stub
//          return true;
    //  }


        static public void w(String tag, String message) {
            w(tag, message, null);
        }

        static public void w(String tag, Throwable t) {
            w(tag, "", t); //$NON-NLS-1$
        }

        public static void w(String tag, String message, Throwable t) {
            if (LOGGING_LEVEL.intValue() <= Level.WARNING.intValue()) {
                System.err.println("WARNING:\t"+tag+'\t'+message); //$NON-NLS-1$
                if (t != null) {
                    t.printStackTrace(System.err);
                }
            }
            
            if (logger != null) {
                logger.log(Level.WARNING, tag + '\t' + message, t);
            }
        }

        static public void i(String tag, String message) {
            i(tag, message, null);      
        }
        
        static public void i(String tag, String message, Throwable t) {
            if (logger != null) {
                logger.log(Level.INFO, tag + '\t' + message, t);
            }
            else if (LOGGING_LEVEL.intValue() <= Level.INFO.intValue()) {
                System.err.println("INFO:\t"+tag+'\t'+message); //$NON-NLS-1$
                if (t != null) {
                    t.printStackTrace(System.err);
                }
            }
        }

        static public void e(String tag, String message) {
            e(tag, message, null);
        }
        
        static public void e(String tag, String message, Throwable t) {
            if (logger != null) {
                logger.log(Level.SEVERE, tag + '\t' + message, t);
            }
            else if (LOGGING_LEVEL.intValue() <= Level.SEVERE.intValue()) {
                System.err.println("ERROR:\t"+tag+'\t'+message); //$NON-NLS-1$
                if (t != null) {
                    t.printStackTrace(System.err);
                }
            }
        }

        public static void d(String tag, String message) {
            d(tag, message, null);
        }

        public static void d(String tag, String message, Throwable t) {
            if (logger != null) {
                logger.log(Level.FINE, tag + '\t' + message, t);
            }
            else if (LOGGING_LEVEL.intValue() <= Level.FINEST.intValue()) {
                System.err.println("DEBUG:\t"+tag+'\t'+message); //$NON-NLS-1$
                if (t != null) {
                    t.printStackTrace(System.err);
                }
            }
        }

        public static void wtf(String tag, String message, Throwable t) {
            if (logger != null) {
                logger.log(Level.ALL, tag + '\t' + message, t);
            }
            else {
                System.err.println("WTF:\t"+tag+'\t'+message); //$NON-NLS-1$
            }
            if (t != null) {
                t.printStackTrace(System.err);
            }
        }


        public static void wtf(String tag, String message) {
            wtf(tag, message, null);
        }


        public static void v(String tag, String message) {
            if (logger != null) {
                logger.log(Level.FINE, tag + '\t' + message);
            }
            else {
                System.err.println("VERBOSE:\t"+tag+'\t'+message); //$NON-NLS-1$
            }
        }
    }
    
    /**
     * Look for services.
     * @param serviceClass required Service class
     * @param onServiceFound called with each discovered service, return false to terminate early
     * @return number of services found
     */
    static public <T> int detectServices(Class<T> serviceClass, Predicate<T> onServiceFound) {
        return detectServices(serviceClass, onServiceFound, null);
    }
    
    static public enum Mode {
        /**
         * Default is to use the ServiceLoader until onServiceFound() returns false
         */
        SERVICE_LOADER_THEN_CLASSNAME,

        /**
         * Another option is to try the className first (if provided) 
         */
        CLASSNAME_FIRST_SINGLESHOT,
        CLASSNAME_THEN_LOADER,
    }
    static public Mode DETECT_MODE = Mode.SERVICE_LOADER_THEN_CLASSNAME;
    
    /**
     * Look for services.
     * @param serviceClass required Service class
     * @param onServiceFound called with each discovered service, return false to terminate early
     * @param classNameForDev if non-null and non-empty is the className to instantiate if ServiceLoader finds nothing
     * @return number of services found
     */
    @SuppressWarnings("unchecked")
    static public <T> int detectServices(Class<T> serviceClass, Predicate<T> onServiceFound, String classNameForDev) {

        int count = 0;
        
        if (Mode.SERVICE_LOADER_THEN_CLASSNAME != DETECT_MODE && ! Check.isEmpty(classNameForDev)) {
            try {
                Class<?> clazz = Class.forName(classNameForDev);
                if (serviceClass.isAssignableFrom(clazz)) {
                    T service = (T) clazz.newInstance();
                    count++;
                    if (! onServiceFound.test(service)) {
                        return count;
                    }
                    if (Mode.CLASSNAME_FIRST_SINGLESHOT == DETECT_MODE) {
                        return count;
                    }
                }
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Log.w("Shared", "detectFirstService: " + classNameForDev);
            }
        }

        ServiceLoader<T> loader =  ServiceLoader.load(serviceClass);
        Iterator<T> iter = loader.iterator();
        while (iter.hasNext()) {
            try {
                T service = iter.next();
                count ++;
                if (! onServiceFound.test(service)) {
                    break;
                }
            }
            catch (ServiceConfigurationError error) {
                Log.w("Shared", "detectFirstService", error);
            }
        }

        if (count <= 0 && ! Check.isEmpty(classNameForDev)) {
            try {
                Class<?> clazz = Class.forName(classNameForDev);
                if (serviceClass.isAssignableFrom(clazz)) {
                    T service = (T) clazz.newInstance();
                    count++;
                    onServiceFound.test(service);
                }
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                Log.w("Shared", "detectFirstService: " + classNameForDev);
            }
        }
        
        return count;
    }
    
    /**
     * Alternative form that uses Either to return the result.
     * @param serviceClass desired Service class if classNamesForDev is null/empty 
     * @param onServiceFound callback on discovery (note that Either.right() may be null)
     * @param classNamesForDev if provided is checked first
     */
    @SuppressWarnings("unchecked")
    static public <T> void detectServices(Class<T> serviceClass, 
            BiConsumer<String, Either<Throwable,T>> onServiceFound, String ... classNamesForDev)
    {
        int count = 0;
        if (classNamesForDev != null && classNamesForDev.length > 0) {

            for (String classNameForDev : classNamesForDev) {
                Throwable error = null;
                if (! Check.isEmpty(classNameForDev)) {
                    try {
                        Class<?> clazz = Class.forName(classNameForDev);
                        if (serviceClass.isAssignableFrom(clazz)) {
                            T service = (T) clazz.newInstance();
                            ++count;
                            onServiceFound.accept(classNameForDev, Either.right(service));
                        }
                        else {
                            error = new IllegalArgumentException(
                                    "Not a " + serviceClass.getName() + ": " + clazz.getName()); //$NON-NLS-1$//$NON-NLS-2$
                        }
                    }
                    catch (ClassNotFoundException e) {
                        error = new ClassNotFoundException("Missing class: " + classNameForDev); //$NON-NLS-1$
                    }
                    catch (InstantiationException | IllegalAccessException e) {
                        error = e;
                    }
                }

                if (error != null) {
                    onServiceFound.accept(classNameForDev, Either.left(error)); 
                }
            }
            
        }
        
        if (count <= 0) {
            ServiceLoader<T> loader =  ServiceLoader.load(serviceClass);
            
            Iterator<T> iter = loader.iterator();
            while (iter.hasNext()) {
                T service = iter.next();
                onServiceFound.accept(service.getClass().getName(), Either.right(service));
            }
        }
    }

    public static File ensureSuffix(File in, String suffix) {
        File outfile = in;
        if (! outfile.getName().toLowerCase().endsWith(suffix)) {
            outfile = new File(outfile.getParentFile(), outfile.getName() + suffix);
        }
        return outfile;
    }

}
