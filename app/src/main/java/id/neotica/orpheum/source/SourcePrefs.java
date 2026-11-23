package id.neotica.orpheum.source;

import android.content.Context;
import android.content.SharedPreferences;

import id.neotica.orpheum.source.subsonic.SubsonicConfig;
import id.neotica.orpheum.utils.AuthManager;

public final class SourcePrefs {

    public static final String SOURCE_ORPHEUM = "orpheum";
    public static final String SOURCE_SUBSONIC = "subsonic";

    private static final String PREFS_NAME = "OrpheumSource";
    private static final String KEY_ACTIVE = "active";

    private SourcePrefs() { }

    public static String getActive(Context context) {
        SharedPreferences prefs = prefs(context);
        String active = prefs.getString(KEY_ACTIVE, null);
        if (SOURCE_SUBSONIC.equals(active) || SOURCE_ORPHEUM.equals(active)) {
            return active;
        }
        if (new AuthManager(context).isLoggedIn()) {
            return SOURCE_ORPHEUM;
        }
        if (new SubsonicConfig(context).isConfigured()) {
            return SOURCE_SUBSONIC;
        }
        return SOURCE_ORPHEUM;
    }

    public static void setActive(Context context, String source) {
        prefs(context).edit().putString(KEY_ACTIVE, source).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
