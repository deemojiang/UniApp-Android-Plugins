package io.huiy.deemo.printer.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public final class Utils {

    @SuppressLint("StaticFieldLeak")
    private static Application sApp;

    private Utils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * Init utils.
     * <p>Init it in the class of UtilsFileProvider.</p>
     *
     * @param app application
     */
    public static void init(final Application app) {
        if (app == null) {
            Log.e("Utils", "app is null.");
            return;
        }
        if (sApp == null) {
            sApp = app;
        }
    }

    /**
     * Return the Application object.
     * <p>Main process get app by UtilsFileProvider,
     * and other process get app by reflect.</p>
     *
     * @return the Application object
     */
    public static Application getApp() {
        if (sApp != null) return sApp;
        init(getCurApplication());
        if (sApp == null) throw new NullPointerException("reflect failed.");
        Log.i("Utils", " reflect app success.");
        return sApp;
    }

    public static Application getCurApplication() {
        Application application = null;
        try {
            Class atClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = atClass.getDeclaredMethod("currentApplication");
            currentApplicationMethod.setAccessible(true);
            application = (Application) currentApplicationMethod.invoke(null);
            Log.d("fw_create", "curApp class1:" + application);
        } catch (Exception e) {
            Log.d("fw_create", "e:" + e.toString());
        }

        if (application != null)
            return application;

        try {
            Class atClass = Class.forName("android.app.AppGlobals");
            Method currentApplicationMethod = atClass.getDeclaredMethod("getInitialApplication");
            currentApplicationMethod.setAccessible(true);
            application = (Application) currentApplicationMethod.invoke(null);
            Log.d("fw_create", "curApp class2:" + application);
        } catch (Exception e) {
            Log.d("fw_create", "e:" + e.toString());
        }

        return application;
    }

    public static void showLong(String str) {
        Toast.makeText(getApp(), str, Toast.LENGTH_LONG).show();
    }

    public static void showLong(int strId) {
        Toast.makeText(getApp(), strId, Toast.LENGTH_LONG).show();
    }
}
