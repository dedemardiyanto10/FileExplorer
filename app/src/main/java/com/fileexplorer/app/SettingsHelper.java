package com.fileexplorer.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsetsController;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsHelper {

    private static final String PREFS_NAME = "AppSettings";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_AMOLED_MODE = "amoled_mode";
    public static final String KEY_SHOW_HIDDEN = "show_hidden_files";
    public static final String KEY_IGNORE_NOMEDIA = "ignore_nomedia";
    public static final String KEY_RV_ANIMATION = "recyclerview_animation";
    public static final String KEY_RV_ANIMATION_TYPE = "recyclerview_animation_type";
    public static final String KEY_SHOW_BREADCRUMB = "show_breadcrumb";
    public static final String KEY_SHOW_CATEGORY = "show_category";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ==================== THEME ====================
    
    public static void applyThemeOnStartup(Context context) {
        int themeMode = getPrefs(context).getInt(KEY_THEME_MODE, 2);
        switch (themeMode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int getThemeMode(Context context) {
        return getPrefs(context).getInt(KEY_THEME_MODE, 2);
    }

    public static boolean isAmoledMode(Context context) {
        int themeMode = getPrefs(context).getInt(KEY_THEME_MODE, 2);
        if (themeMode != 1) return false;
        return getPrefs(context).getBoolean(KEY_AMOLED_MODE, false);
    }

    public static boolean isDarkModeEnabled(Context context) {
        int themeMode = getPrefs(context).getInt(KEY_THEME_MODE, 2);
        if (themeMode == 1) return true;
        if (themeMode == 0) return false;
        int nightMode = context.getResources().getConfiguration().uiMode 
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // ==================== FILE SETTINGS ====================
    
    public static boolean isShowHiddenFiles(Context context) {
        return getPrefs(context).getBoolean(KEY_SHOW_HIDDEN, false);
    }

    public static boolean isIgnoreNomedia(Context context) {
        return getPrefs(context).getBoolean(KEY_IGNORE_NOMEDIA, false);
    }

    // ==================== ANIMATION ====================
    
    public static boolean isRecyclerViewAnimationEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_RV_ANIMATION, true);
    }

    public static int getRecyclerViewAnimationType(Context context) {
        return getPrefs(context).getInt(KEY_RV_ANIMATION_TYPE, 0);
    }

    // ==================== UI ====================
    
    public static boolean isShowBreadcrumb(Context context) {
        return getPrefs(context).getBoolean(KEY_SHOW_BREADCRUMB, false);
    }

    public static boolean isShowCategory(Context context) {
        return getPrefs(context).getBoolean(KEY_SHOW_CATEGORY, true);
    }

    // ==================== APPLY AMOLED (MainActivity) ====================
    
    public static void applyAmoledMode(Context context, View rootView, AppBarLayout appBarLayout,
                                      MaterialToolbar topAppBar, RecyclerView recyclerView) {
        if (context == null) return;

        boolean isAmoled = isAmoledMode(context);
        boolean isDarkMode = isDarkModeEnabled(context);

        if (isDarkMode && isAmoled) {
            int blackColor = Color.BLACK;
            if (rootView != null) rootView.setBackgroundColor(blackColor);
            if (appBarLayout != null) appBarLayout.setBackgroundColor(blackColor);
            if (topAppBar != null) topAppBar.setBackgroundColor(blackColor);
            if (recyclerView != null) recyclerView.setBackgroundColor(blackColor);
        }
    }

    // ==================== APPLY INSTANT VISUALS (SettingsActivity) ====================
    
    public static int getTargetBackgroundColor(Context context) {
        int themeMode = getPrefs(context).getInt(KEY_THEME_MODE, 2);
        boolean isAmoled = getPrefs(context).getBoolean(KEY_AMOLED_MODE, false);

        boolean isNight;
        if (themeMode == 1) {
            isNight = true;
        } else if (themeMode == 0) {
            isNight = false;
        } else {
            int nightMask = context.getResources().getConfiguration().uiMode 
                    & Configuration.UI_MODE_NIGHT_MASK;
            isNight = (nightMask == Configuration.UI_MODE_NIGHT_YES);
        }

        if (isNight && isAmoled) {
            return Color.BLACK;
        } else {
            android.util.TypedValue tv = new android.util.TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurface, tv, true);
            return tv.data != 0 ? tv.data : Color.WHITE;
        }
    }

    public static void applyInstantVisuals(Activity activity, View rootView, MaterialToolbar topAppBar) {
        int targetColor = getTargetBackgroundColor(activity);
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(targetColor));
        rootView.setBackgroundColor(targetColor);
        topAppBar.setBackgroundColor(targetColor);
    }

    // ==================== SETUP STATUS BAR (SettingsActivity) ====================
    
    public static void setupStatusBar(Activity activity) {
        Context context = activity;
        boolean isNight = (context.getResources().getConfiguration().uiMode 
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean isAmoled = getPrefs(context).getBoolean(KEY_AMOLED_MODE, false);
        int themeMode = getPrefs(context).getInt(KEY_THEME_MODE, 2);

        boolean currentDark = (themeMode == 1) || (themeMode == 2 && isNight);

        int statusBarColor;
        if (currentDark && isAmoled) {
            statusBarColor = Color.BLACK;
        } else {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorSurface, typedValue, true);
            statusBarColor = typedValue.data;
        }

        activity.getWindow().setStatusBarColor(statusBarColor);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int appearanceMask = currentDark ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            if (activity.getWindow().getInsetsController() != null) {
                activity.getWindow().getInsetsController().setSystemBarsAppearance(
                        appearanceMask, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        }
    }

    // ==================== SAVE SETTINGS ====================
    
    public static void saveThemeMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public static void saveAmoledMode(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_AMOLED_MODE, enabled).apply();
    }
}