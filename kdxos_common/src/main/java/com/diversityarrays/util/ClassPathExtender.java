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
package com.diversityarrays.util;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

@SuppressWarnings("nls")
public class ClassPathExtender {
	
	static public boolean VERBOSE = false;
	
	private static final FileFilter JAR_OR_PROPERTIES = new FileFilter() {
		@Override
		public boolean accept(File file) {
			boolean result = false;
			if (file.isFile()) {
				if (file.canRead()) {
					String loname = file.getName().toLowerCase();
					if (loname.endsWith(".jar") || loname.endsWith(".properties")) {
						result = true;
					}
				}
			}
			return result;
		}
	};
	

	public static void appendToClassPath(File baseDir, String dirnames, Consumer<File> jarChecker) {
		appendToClassPath(baseDir, dirnames, jarChecker, null);
	}
	
	public static void appendToClassPath(File baseDir, String dirnames, Consumer<File> jarChecker, Log logger) {
		List<File> dirs = new ArrayList<File>();

		for (String dirname : dirnames.split(",")) {
			File dir = new File(baseDir, dirname);
			if (dir.isDirectory()) {
				dirs.add(dir);
			}
		}
		addDirectoryJarsToClassPath(jarChecker, dirs, logger);
	}

	public static void addDirectoryJarsToClassPath(Consumer<File> jarChecker, List<File> dirs) {
		addDirectoryJarsToClassPath(jarChecker, dirs, null);
	}
	
	public static void addDirectoryJarsToClassPath(Consumer<File> jarChecker, List<File> dirs, Log logger) {
		if (! dirs.isEmpty()) {
			addDirectoryJarsToClassPath(logger, jarChecker, dirs.toArray(new File[dirs.size()]));
		}
	}

	public static void addDirectoryJarsToClassPath(Consumer<File> jarChecker, File ... dirs) {
		addDirectoryJarsToClassPath(null, jarChecker, dirs);
	}

	public static void addDirectoryJarsToClassPath(
			Log logger, Consumer<File> jarChecker, File ... dirs) 
	{
		if (dirs==null || dirs.length <= 0) {
			if (logger != null) {
				logger.info("No directories provided for class path");
			}
			return;
		}
		
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl instanceof java.net.URLClassLoader) {
			try {
				Method m = java.net.URLClassLoader.class.getDeclaredMethod("addURL", URL.class );
				m.setAccessible(true);
				
				Set<URL> currentUrls = new HashSet<URL>(Arrays.asList(((URLClassLoader) ccl).getURLs()));
				if (VERBOSE) {
					info(logger, "=== Current URLS in ClassLoader: " + currentUrls.size());
					for (URL u : currentUrls) {
						info(logger, "\t" + u.toString());
					}
				}
				
				for (File dir : dirs) {
					if (dir.isDirectory()) {
						for (File f : dir.listFiles(JAR_OR_PROPERTIES)) {
							try {
								URL u = f.toURI().toURL();
								if (! currentUrls.contains(u)) {
									m.invoke(ccl, u);
									if (VERBOSE) {
										info(logger, "[Added " + u + "] to CLASSPATH"); 
									}
									if (jarChecker != null) {
										jarChecker.accept(f);
									}
								}
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException e) {
								warn(logger,
										"%Unable to add " + f.getPath() + " to CLASSPATH (" + e.getMessage() + ")");
							}
						}
					}
				}
			} catch (NoSuchMethodException e) {
				warn(logger, "%No method: "+java.net.URLClassLoader.class.getName()+".addURL(URL)");
			}
			
		}
		else {
			warn(logger, "%currentThread.contextClassLoader is not an instance of " + java.net.URLClassLoader.class.getName());
		}
	}

	private static void info(Log logger, Object msg) {
		if (logger!=null) {
			logger.info(msg);
		}
		else {
			System.out.println(msg);
		}
	}
	
	private static void warn(Log logger, Object msg) {
		if (logger!=null) {
			logger.warn(msg);
		}
		else {
			System.err.println(msg);
		}
	}
}