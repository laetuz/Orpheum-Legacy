package id.neotica.orpheum.network.youtube;

import android.content.Context;
import android.content.SharedPreferences;

public final class YouTubeConfig {

    public static final String DEFAULT_LIST_URL = "http://144.31.189.129/notPipe.json";

    private static final String PREFS_NAME = "OrpheumYouTube";
    private static final String KEY_LIST_URL = "list_url";
    private static final String KEY_INSTANCES = "instances";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final long CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L;

    private final SharedPreferences prefs;

    public YouTubeConfig(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getListUrl() {
        String url = prefs.getString(KEY_LIST_URL, "");
        return url.length() > 0 ? url : DEFAULT_LIST_URL;
    }

    public void setListUrl(String url) {
        prefs.edit().putString(KEY_LIST_URL, url == null ? "" : url.trim()).commit();
    }

    public String getCachedInstances() {
        return prefs.getString(KEY_INSTANCES, "");
    }

    public void saveInstances(String raw) {
        prefs.edit()
                .putString(KEY_INSTANCES, raw)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .commit();
    }

    public long getLastUpdate() {
        return prefs.getLong(KEY_LAST_UPDATE, 0L);
    }

    public boolean isCacheStale() {
        long last = getLastUpdate();
        return last <= 0L || System.currentTimeMillis() - last > CACHE_MAX_AGE_MS;
    }

    public void clearCache() {
        prefs.edit()
                .remove(KEY_INSTANCES)
                .remove(KEY_LAST_UPDATE)
                .commit();
    }
}
