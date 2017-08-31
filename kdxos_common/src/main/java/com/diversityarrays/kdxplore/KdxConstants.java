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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

import com.diversityarrays.util.Either;
import com.diversityarrays.util.JarUtil;
import com.diversityarrays.util.MsgBox;

@SuppressWarnings("nls")
public class KdxConstants {

    static private final String EXPIRY_YMD = "2017-02-15";

    static private long VERSION_INFO = 1483102800000L;
    static public long getVersionInfo() {
        return VERSION_INFO;
    }

    static private long VERSION_SUBINFO;

    static public long getVersionSubinfo() {
        return VERSION_SUBINFO;
    }

    // ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **
    // ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **

    // true to disable alpha/beta expiry checking
    static public final boolean PRODUCTION_MODE;

    static public final int VERSION_CODE = 46;
    static public final String VERSION = "2.0.46"; // NOTE: 2.0.xx is reserved for CIMMYT source

    static {
        int pos = VERSION.indexOf('.');
        if (pos <= 0) {
            throw new RuntimeException("Invalid VERSION: " + VERSION);
        }

        boolean prodMode = false;
        try {
            int major = Integer.parseInt(VERSION.substring(0, pos));
            if (0 == (major & 1)) {
                // even numbers are PRODUCTION
                prodMode = true;
            }
        }
        catch (NumberFormatException e) {
            throw new RuntimeException("No leading integer in VERSION: " + VERSION);
        }
        PRODUCTION_MODE = prodMode;

        // ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **
        // ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **

        long tmp = 0;
        long tmp2 = Long.MAX_VALUE;

        boolean exitAfterPrinting = false;
        if (! PRODUCTION_MODE) {
            try {
                Date when;
                if (VERSION_INFO == 0) {
                    // To
                    when = new SimpleDateFormat("yyyy-MM-dd").parse(EXPIRY_YMD);
                    tmp = when.getTime();
                    exitAfterPrinting = true;
                } else {
                    tmp = VERSION_INFO;
                    when = new Date(VERSION_INFO);
                }
                tmp2 = 1 + ChronoUnit.DAYS.between(new Date().toInstant(), when.toInstant());
            } catch (ParseException e) {
            }
        }
        String versionLine = "VERSION_INFO=" + (tmp > 0 ? "-" + tmp : "") + "[" + tmp2 + "]";
        if (exitAfterPrinting) {
            System.err.println(versionLine);
            runStaticInitChecks(false, false);
            System.exit(1);
        }
        System.out.println(versionLine);
        VERSION_INFO = tmp;
        VERSION_SUBINFO = tmp2;
    }

    static public void runStaticInitChecks(boolean errorIsFatal, boolean quiet) {

        checkStatsUtil(errorIsFatal, quiet);

        checkWorkPackageReader(errorIsFatal, quiet);

        checkMsgClasses(errorIsFatal, quiet);
    }

    private static void checkStatsUtil(boolean errorIsFatal, boolean quiet) {
        try {
            Class<?> statsUtil = Class.forName("com.diversityarrays.kdxplore.stats.StatsUtil");
            Method initCheck = statsUtil.getMethod("initCheck");
            if (Modifier.isStatic(initCheck.getModifiers())) {
                try {
                    initCheck.invoke(null);
                    if (! quiet) {
                        System.out.println("******************************");
                        System.out.println("SUCCESS: StatsUtil.initCheck()");
                        System.out.println("******************************");
                    }
                }
                catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                    reportCheckProblem(errorIsFatal, "%error on StatsUtil.initCheck()", e);
                }
            }
            else {
                System.err.println("%StatsUtil.initCheck(int) is not static");
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            System.err.println("%StatsUtil.initCheck: " + e.getClass().getSimpleName()+"=" + e.getMessage());
        }
    }

    private static void checkWorkPackageReader(boolean errorIsFatal, boolean quiet) {
        try {
            Class<?> kdsdbu = Class.forName("com.diversityarrays.kdsmart.db.ormlite.KDSmartDatabaseUpgrader");
            Field dbVersion = kdsdbu.getField("DATABASE_VERSION");
            if (Modifier.isStatic(dbVersion.getModifiers())) {
                if (int.class == dbVersion.getType()) {
                    int dbVersionValue = dbVersion.getInt(null);
                    System.out.println("KDSmart.DATABASE_VERSION=" + dbVersionValue);
                    Class<?> wpr = Class.forName("com.diversityarrays.kdsmart.kdxs.WorkPackageReader");
                    Method initCheck = wpr.getMethod("initCheck", int.class);
                    if (Modifier.isStatic(initCheck.getModifiers())) {
                        try {
                            initCheck.invoke(wpr, dbVersionValue);
                            if (! quiet) {
                                System.out.println("*******************************");
                                System.out.println("WorkPackageReader.initCheck(..)");
                                System.out.println("*******************************");
                            }
                        }
                        catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                            reportCheckProblem(errorIsFatal,
                                    "%error on WorkPackageReader.initCheck(" + dbVersionValue + ")",
                                    e);
                        }
                    }
                    else {
                        System.err.println("%WorkPackageReader.initCheck(int) is not static");
                    }
                }
                else {
                    System.err.println("%KDSmartDatabaseUpgrader.DATABASE_VERSION is not int");
                }
            }
            else {
                System.err.println("%KDSmartDatabaseUpgrader.DATABASE_VERSION is not static");
            }
        }
        catch (ClassNotFoundException
                | NoSuchFieldException
                | SecurityException
                | IllegalArgumentException
                | IllegalAccessException
                | NoSuchMethodException e)
        {
            System.err.println("%WorkPackageReader.initCheck: " + e.getMessage());
        }
    }

    static private void checkMsgClasses(boolean errorIsFatal, boolean quiet) {

        if (! quiet) {
            System.out.println("***********************************************************");
            System.out.println("Checking Msg classes: Bundles, String resources and formats");
        }

        Map<String,List<String>> errorsByClassName = new LinkedHashMap<>();

        for (MsgCheck msgCheck : getMsgClassNames()) {
            String className = msgCheck.className;
            List<String> errors = new ArrayList<>();
            try {
                Class<?> clz = Class.forName(className);
                try {
                    ResourceBundle bundle = ResourceBundle.getBundle(className);
                    errors.addAll(doBundle(msgCheck, clz, bundle));
                }
                catch (MissingResourceException e) {
                    errors.add("Missing Bundle for " + className);
                }
            }
            catch (ClassNotFoundException e) {
                if (! msgCheck.optional) {
                    errors.add("Missing class: " + className + ", optional=" + msgCheck.optional);
                }
            }

            if (errors.isEmpty()) {
                if (! quiet) {
                    System.out.println("Messages ok for " + className);
                }
            }
            else {
                errorsByClassName.put(className, errors);
            }
        }

        if (! quiet) {
            System.out.println("***********************************************************");
        }

        if (! errorsByClassName.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String className : errorsByClassName.keySet()) {
                sb.append(className).append(": ");
                for (String error : errorsByClassName.get(className)) {
                    sb.append("\n  ").append(error);
                }
                sb.append('\n');
            }
            RuntimeException e = new RuntimeException(sb.toString());

            reportCheckProblem(errorIsFatal, "Some Msg classes have problems", e);
        }
    }

    static private List<String> doBundle(MsgCheck msgCheck, Class<?> clz, ResourceBundle bundle) {
        List<String> errors = new ArrayList<>();

        for (Method m : clz.getDeclaredMethods()) {

            if (msgCheck.shouldExcludeMethod(m.getName())) {
                continue;
            }

            if (Modifier.isStatic(m.getModifiers())) {
                if (String.class.equals(m.getReturnType())) {
                    String fullMethodName = msgCheck.className + "." + m.getName();
                    errors.addAll(doMethod(bundle, m, fullMethodName));
                }
            }
        }

        return errors;
    }

    /**
     * Check that the name of the method exists as a value in the Bundle.
     * If it doesn't then this is a problem - either a mis-spelling or someone forgot to create
     * the entry in the corresponding Msg.properties file.
     * <p>
     * Note: Special handling exists for methods that have a single parameter that is an Enum;
     * in this case the enum values are enumerated and the <code>name()</code> is appended to
     * the name of the method (with an underscore separator) to derive the message key.
     * <p>
     * If you write a method that is of this type it is your responsibility to make the code look like:
     * <pre>
     * public class Msg extends com.diversityarrays.util.AbstractMsg {
     *
     *     static private final Msg i = new Msg();
     *
     *     public static String METHOD_NAME(EnumClass value) {
     *         return i.getString("METHOD_NAME_" + value.name());
     *     }
     * }
     * </pre>
     * @param bundle
     * @param m
     * @param fullMethodName
     * @return
     */
    static private List<String> doMethod(ResourceBundle bundle, Method m, String fullMethodName) {
        List<String> errors = new ArrayList<>();
        String bundleName = m.getName();
        try {
            if (m.getParameterCount()==1) {
                Class<?> paramType = m.getParameterTypes()[0];
                // Single Enum parameter means we will enumerate all of the values
                if (paramType.isEnum()) {
                    Object[] enumValues = paramType.getEnumConstants();
                    for (Object enumValue : enumValues) {
                        String enumName = ((Enum<?>) enumValue).name();
                        bundleName = m.getName() + "_" + enumName;
                        String fmt = bundle.getString(bundleName);
                        try {
                            MessageFormat mfmt = new MessageFormat(fmt);
                            Format[] formats = mfmt.getFormats();
                            if (formats.length != 0) {
                                errors.add("parameter count: " + bundleName + " (should be zero)");
                            }
                        }
                        catch (IllegalArgumentException e) {
                            errors.add("bad MessageFormat for " + bundleName + ": " + fmt);
                            System.err.println("Invalid format: " + fullMethodName
                                    + "\n\t=[" + fmt + "]");
                        }
                    }

                    return errors;
                }
            }

            String fmt = bundle.getString(bundleName);
            try {
                MessageFormat mfmt = new MessageFormat(fmt);
                Format[] formats = mfmt.getFormats();
                if (formats.length != m.getParameterCount()) {
                    errors.add("parameter count: " + bundleName);
                    System.err.println(String.format(
                            "Format count(%d) <> parameterCount(%d): %s",
                            formats.length, m.getParameterCount(), fullMethodName));
                }
            }
            catch (IllegalArgumentException e) {
                errors.add("bad MessageFormat for " + bundleName + ": " + fmt);
                System.err.println("Invalid format: " + fullMethodName
                        + "\n\t=[" + fmt + "]");
            }
        }
        catch (MissingResourceException mre) {
            if (! bundleName.startsWith("raw_")) {
                errors.add("missing resource: " + bundleName);
                System.err.println("Missing resource: " + fullMethodName);
            }
        }
        catch (ClassCastException cce) {
            errors.add("Class Cast: " + m.getName());
            System.err.println("Not a String resource: " + fullMethodName);
        }
        return errors;
    }


    static private void reportCheckProblem(boolean errorIsFatal, String msg, Throwable t) {
        Throwable cause = t;
        if (cause instanceof InvocationTargetException) {
            cause = cause.getCause();
        }
        System.err.println(msg);
        if (cause != null) {
            System.err.println(cause.getClass().getName());
            System.err.println(cause.getMessage());
        }

        if (cause != null && errorIsFatal) {
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }


    static public final String BUILT_BY = "Built-By"; //$NON-NLS-1$
    static public final String BUILD_DATE = "Build-Date"; //$NON-NLS-1$

    static public String getKdxploreBuildDate() {
        return getKdxploreManifestAttributes().get(BUILD_DATE);
    }

    static public Map<String, String> getKdxploreManifestAttributes() {

        Map<String,String> result = new LinkedHashMap<>();

        BiConsumer<URL, Either<IOException,Attributes>> consumer = new BiConsumer<URL, Either<IOException,Attributes>>() {
            @Override
            public void accept(URL url, Either<IOException,Attributes> either) {
                if (either.isRight()) {
                    Attributes mainAttributes = either.right();
                    System.out.println("Manifest: " + url); //$NON-NLS-1$
                    for (Object key : mainAttributes.keySet()) {
                        Object value = mainAttributes.get(key);
                        result.put(key.toString(), value==null ? "" : value.toString()); //$NON-NLS-1$
                    }
                }
                else {
                    System.err.println("Manifest Error: " + url);
                    System.err.println(either.left().getMessage());
                }
            }
        };
        try {
            JarUtil.getManifestAttributes(KdxConstants.class.getClassLoader(),
                    (url) -> url.toString().contains("/kdxplore_os.jar!"),
                    consumer);
        } catch (IOException e) {
            Shared.Log.w("KdxConstants", "Failed to get MANIFESTs: " + e.getMessage()); //$NON-NLS-1$
        }

        return result;
    }

    static class MsgCheck {
        public final String className;
        public final boolean optional;
        private Set<String> excludeMethodNames = null;

        MsgCheck(String cn, boolean b) {
            className = cn;
            optional = b;
        }

        @Override
        public String toString() {
            return (optional?"?":"") + className
                    + (excludeMethodNames==null
                        ? ""
                        : excludeMethodNames.stream().collect(Collectors.joining(",")));
        }

        public void addExcludeMethodName(String methodName) {
            if (excludeMethodNames==null) {
                excludeMethodNames = new HashSet<>();
            }
            excludeMethodNames.add(methodName);
        }

        public boolean shouldExcludeMethod(String methodName) {
            if (excludeMethodNames==null) {
                return false;
            }
            return excludeMethodNames.contains(methodName);
        }
    }

    static private List<MsgCheck> getMsgClassNames() {

        Set<String> duplicates = new HashSet<>();
        Set<String> seen = new HashSet<>();
        List<MsgCheck> list = new ArrayList<>();
        InputStream is = KdxConstants.class.getResourceAsStream("msg-classes");
        if (is == null) {
            System.err.println("No Msg checking: missing resource 'msg-classes'");
        }
        else {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                String line;
                MsgCheck msgCheck = null;
                while (null != (line = br.readLine())) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (line.startsWith(":")) {
                        if (msgCheck != null) {
                            msgCheck.addExcludeMethodName(line.substring(1));
                        }
                    }
                    else {
                        boolean isOptional = false;
                        if (line.startsWith("?")) {
                            isOptional = true;
                            line = line.substring(1).trim();
                        }
                        if (seen.contains(line)) {
                            duplicates.add(line);
                        }
                        else {
                            seen.add(line);
                            msgCheck = new MsgCheck(line, isOptional);
                            list.add(msgCheck);
                        }
                    }
                }
            }
            catch (IOException ignore) {

            }
            finally {
                if (br != null) {
                    try { br.close(); } catch (IOException ignore) {}
                }
            }

            if (! duplicates.isEmpty()) {
                String msg = duplicates.stream().collect(Collectors.joining("\n", "Duplicates in msg-classes:\n", ""));
                MsgBox.warn(null, msg, "Packaging Warning");
            }
        }
        return list;
    }
}
