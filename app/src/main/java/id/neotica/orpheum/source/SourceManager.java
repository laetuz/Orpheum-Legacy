package id.neotica.orpheum.source;

import android.content.Context;

import id.neotica.orpheum.source.orpheum.OrpheumSource;
import id.neotica.orpheum.source.subsonic.SubsonicConfig;
import id.neotica.orpheum.source.subsonic.SubsonicSource;
import id.neotica.orpheum.utils.AuthManager;

public final class SourceManager {

    public static final String SOURCE_ORPHEUM = SourcePrefs.SOURCE_ORPHEUM;
    public static final String SOURCE_SUBSONIC = SourcePrefs.SOURCE_SUBSONIC;

    private static MusicSource cachedSource;
    private static String cachedId;

    private SourceManager() { }

    public static synchronized MusicSource get(Context context) {
        Context app = context.getApplicationContext();
        String id = SourcePrefs.getActive(app);
        if (cachedSource == null || !id.equals(cachedId)) {
            if (SOURCE_SUBSONIC.equals(id)) {
                cachedSource = new SubsonicSource(app);
            } else {
                cachedSource = new OrpheumSource(app);
            }
            cachedId = id;
        }
        return cachedSource;
    }

    public static synchronized void setActive(Context context, String source) {
        Context app = context.getApplicationContext();
        SourcePrefs.setActive(app, source);
        reset();
    }

    public static synchronized void reset() {
        cachedSource = null;
        cachedId = null;
    }

    public static boolean isLoggedIn(Context context) {
        return get(context).isLoggedIn();
    }

    public static void logout(Context context) {
        Context app = context.getApplicationContext();
        String active = SourcePrefs.getActive(app);
        get(app).logout();

        boolean orpheumAvailable = new AuthManager(app).isLoggedIn();
        boolean subsonicAvailable = new SubsonicConfig(app).isConfigured();
        if (SOURCE_ORPHEUM.equals(active)) {
            if (subsonicAvailable) {
                setActive(app, SOURCE_SUBSONIC);
            } else {
                reset();
            }
        } else {
            if (orpheumAvailable) {
                setActive(app, SOURCE_ORPHEUM);
            } else {
                reset();
            }
        }
    }
}
