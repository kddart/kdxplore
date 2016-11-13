package com.diversityarrays.util;
// NOTE: This file used "as-is" except for the package change and isolation
//       from the "AppContext" usage to ensure that it is available and 
//       because of the comment below about:
//          "You should not rely on this class even existing"

/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.awt.Color;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

/**
 * DefaultLookup provides a way to customize the lookup done by the
 * UIManager. The default implementation of DefaultLookup forwards
 * the call to the UIManager.
 * <p>
 * <b>WARNING:</b> While this class is public, it should not be treated as
 * public API and its API may change in incompatable ways between dot dot
 * releases and even patch releases. You should not rely on this class even
 * existing.
 *
 * @author Scott Violet
 */
public class SunSwingDefaultLookup {
    /**
     * Key used to store DefaultLookup for AppContext.
     */
    private static final Object DEFAULT_LOOKUP_KEY = 
    		new StringBuffer("SunSwingDefaultLookup"); //$NON-NLS-1$
    
    static private final String SUN_AWT_APPCONTEXT = "sun.awt.AppContext"; //$NON-NLS-1$
    private static Object appContext;
    private static Method getMethod;
    private static Method putMethod;
    static {
        try {
            Class<?> clazz = Class.forName(SUN_AWT_APPCONTEXT);
            
            Method getAppContext;
            try {
                getAppContext = clazz.getMethod("getAppContext"); //$NON-NLS-1$
                if (! Modifier.isStatic(getAppContext.getModifiers())) {
                    throw new RuntimeException("Not a class method: " + getAppContext.toString()); //$NON-NLS-1$
                }
            }
            catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Can't find method "  //$NON-NLS-1$
                        + SUN_AWT_APPCONTEXT + ".getAppContext()"); //$NON-NLS-1$
            }
            
            try {
                getMethod = clazz.getMethod("get", Object.class); //$NON-NLS-1$
            }
            catch (NoSuchMethodException | SecurityException e1) {
                throw new RuntimeException("Can't find method " + SUN_AWT_APPCONTEXT + ".get(Object)"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            
            try {
                putMethod = clazz.getMethod("put", Object.class, Object.class); //$NON-NLS-1$
            }
            catch (NoSuchMethodException | SecurityException e1) {
                throw new RuntimeException("Can't find method " //$NON-NLS-1$ 
                        + SUN_AWT_APPCONTEXT + ".get(Object, Object)"); //$NON-NLS-1$
            }
            
            try {
                appContext = getAppContext.invoke(clazz);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(SUN_AWT_APPCONTEXT + ".getAppContext() failed"); //$NON-NLS-1$
            }
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find class " + SUN_AWT_APPCONTEXT); //$NON-NLS-1$
        }
    }
    
    static private void putToAppContext(Object key, Object value) {
        try {
            putMethod.invoke(appContext, key, value);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    static private Object getFromAppContext(Object key) {
        try {
            return getMethod.invoke(appContext, key);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Thread that last asked for a default.
     */
    private static Thread currentDefaultThread;
    /**
     * DefaultLookup for last thread.
     */
    private static SunSwingDefaultLookup currentDefaultLookup;

    /**
     * If true, a custom DefaultLookup has been set.
     */
    private static boolean isLookupSet;


    /**
     * Sets the DefaultLookup instance to use for the current
     * <code>AppContext</code>. Null implies the UIManager should be
     * used.
     */
    public static void setDefaultLookup(SunSwingDefaultLookup lookup) {
        synchronized(SunSwingDefaultLookup.class) {
            if (!isLookupSet && lookup == null) {
                // Null was passed in, and no one has invoked setDefaultLookup
                // with a non-null value, we don't need to do anything.
                return;
            }
            else if (lookup == null) {
                // null was passed in, but someone has invoked setDefaultLookup
                // with a non-null value, use an instance of DefautLookup
                // which will fallback to UIManager.
                lookup = new SunSwingDefaultLookup();
            }
            isLookupSet = true;
            putToAppContext(DEFAULT_LOOKUP_KEY, lookup);
//            AppContext.getAppContext().put(DEFAULT_LOOKUP_KEY, lookup);
            currentDefaultThread = Thread.currentThread();
            currentDefaultLookup = lookup;
        }
    }

    public static Object get(JComponent c, ComponentUI ui, String key) {
        boolean lookupSet;
        synchronized(SunSwingDefaultLookup.class) {
            lookupSet = isLookupSet;
        }
        if (!lookupSet) {
            // No one has set a valid DefaultLookup, use UIManager.
            return UIManager.get(key, c.getLocale());
        }
        Thread thisThread = Thread.currentThread();
        SunSwingDefaultLookup lookup;
        synchronized(SunSwingDefaultLookup.class) {
            // See if we've already cached the DefaultLookup for this thread,
            // and use it if we have.
            if (thisThread == currentDefaultThread) {
                // It is cached, use it.
                lookup = currentDefaultLookup;
            }
            else {
                // Not cached, get the DefaultLookup to use from the AppContext
                lookup = (SunSwingDefaultLookup) getFromAppContext(DEFAULT_LOOKUP_KEY);
//                lookup = (SunSwingDefaultLookup)AppContext.getAppContext().get(DEFAULT_LOOKUP_KEY);
                if (lookup == null) {
                    // Fallback to DefaultLookup, which will redirect to the
                    // UIManager.
                    lookup = new SunSwingDefaultLookup();
                    putToAppContext(DEFAULT_LOOKUP_KEY, lookup);
//                    AppContext.getAppContext().put(DEFAULT_LOOKUP_KEY, lookup);
                }
                // Cache the values to make the next lookup easier.
                currentDefaultThread = thisThread;
                currentDefaultLookup = lookup;
            }
        }
        return lookup.getDefault(c, ui, key);
    }

    //
    // The following are convenience method that all use getDefault.
    //
    public static int getInt(JComponent c, ComponentUI ui, String key,
                             int defaultValue) {
        Object iValue = get(c, ui, key);

        if (iValue == null || !(iValue instanceof Number)) {
            return defaultValue;
        }
        return ((Number)iValue).intValue();
    }

    public static int getInt(JComponent c, ComponentUI ui, String key) {
        return getInt(c, ui, key, -1);
    }

    public static Insets getInsets(JComponent c, ComponentUI ui, String key,
                                   Insets defaultValue) {
        Object iValue = get(c, ui, key);

        if (iValue == null || !(iValue instanceof Insets)) {
            return defaultValue;
        }
        return (Insets)iValue;
    }

    public static Insets getInsets(JComponent c, ComponentUI ui, String key) {
        return getInsets(c, ui, key, null);
    }

    public static boolean getBoolean(JComponent c, ComponentUI ui, String key,
                                     boolean defaultValue) {
        Object iValue = get(c, ui, key);

        if (iValue == null || !(iValue instanceof Boolean)) {
            return defaultValue;
        }
        return ((Boolean)iValue).booleanValue();
    }

    public static boolean getBoolean(JComponent c, ComponentUI ui, String key) {
        return getBoolean(c, ui, key, false);
    }

    public static Color getColor(JComponent c, ComponentUI ui, String key,
                                 Color defaultValue) {
        Object iValue = get(c, ui, key);

        if (iValue == null || !(iValue instanceof Color)) {
            return defaultValue;
        }
        return (Color)iValue;
    }

    public static Color getColor(JComponent c, ComponentUI ui, String key) {
        return getColor(c, ui, key, null);
    }

    public static Icon getIcon(JComponent c, ComponentUI ui, String key,
            Icon defaultValue) {
        Object iValue = get(c, ui, key);
        if (iValue == null || !(iValue instanceof Icon)) {
            return defaultValue;
        }
        return (Icon)iValue;
    }

    public static Icon getIcon(JComponent c, ComponentUI ui, String key) {
        return getIcon(c, ui, key, null);
    }

    public static Border getBorder(JComponent c, ComponentUI ui, String key,
            Border defaultValue) {
        Object iValue = get(c, ui, key);
        if (iValue == null || !(iValue instanceof Border)) {
            return defaultValue;
        }
        return (Border)iValue;
    }

    public static Border getBorder(JComponent c, ComponentUI ui, String key) {
        return getBorder(c, ui, key, null);
    }

    public Object getDefault(JComponent c, ComponentUI ui, String key) {
        // basic
        return UIManager.get(key, c.getLocale());
    }
}
