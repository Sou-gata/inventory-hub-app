package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import in.gbtsolutions.inventoryhub.Configurations;

public class ThemeManager {
    private static final String KEY_THEME_MODE = "app_theme_mode";

    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                Configurations.PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    /**
     * Gets the saved theme mode preference. Defaults to system default.
     */
    public static int getSavedThemeMode(Context context) {
        return getPreferences(context).getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * Persists the chosen theme mode and applies it immediately across the app.
     */
    public static void setSavedThemeMode(Context context, int mode) {
        getPreferences(context)
                .edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Applies the saved theme mode on application or activity startup.
     */
    public static void applyTheme(Context context) {
        int mode = getSavedThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Checks if the active configuration or preference resolves to dark mode.
     */
    public static boolean isDarkMode(Context context) {
        int mode = getSavedThemeMode(context);
        if (mode == MODE_DARK) {
            return true;
        } else if (mode == MODE_LIGHT) {
            return false;
        } else {
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    /**
     * Toggles between dark and light modes, saves the preference, and applies the change.
     */
    public static void toggleTheme(Context context) {
        boolean currentlyDark = isDarkMode(context);
        int newMode = currentlyDark ? MODE_LIGHT : MODE_DARK;
        setSavedThemeMode(context, newMode);
    }
}
