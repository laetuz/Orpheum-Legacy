package id.neotica.orpheum.source.subsonic;

import android.content.Context;
import android.content.SharedPreferences;

public class SubsonicConfig {

    private static final String PREFS_NAME = "OrpheumSubsonic";
    private static final String KEY_BASE = "base";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_SALT = "salt";
    private static final String KEY_TOKEN = "token";

    private final SharedPreferences prefs;

    public SubsonicConfig(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isConfigured() {
        return getBase().length() > 0
                && getUsername().length() > 0
                && getSalt().length() > 0
                && getToken().length() > 0;
    }

    public void save(String base, String username, String salt, String token) {
        prefs.edit()
                .putString(KEY_BASE, base)
                .putString(KEY_USERNAME, username)
                .putString(KEY_SALT, salt)
                .putString(KEY_TOKEN, token)
                .commit();
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_BASE)
                .remove(KEY_USERNAME)
                .remove(KEY_SALT)
                .remove(KEY_TOKEN)
                .commit();
    }

    public String getBase() {
        return prefs.getString(KEY_BASE, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getSalt() {
        return prefs.getString(KEY_SALT, "");
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }
}
