package id.neotica.orpheum.utils;

import id.neotica.orpheum.BuildConfig;

public class Constants {
    private Constants() { }

    public static final String BASE_URL = normalizeBase(BuildConfig.BASE_URL);
    public static final String FILE_BASE_URL = BuildConfig.FILE_BASE_URL;
    public static final String AUTH_BASE_URL = BuildConfig.AUTH_BASE_URL;

    public static final String ORPHEUM_BASE = BASE_URL + "/orpheum";
    public static final String TRACK_FEED_URL = ORPHEUM_BASE + "/catalog/tracks/new";
    public static final String ALBUM_FEED_URL = ORPHEUM_BASE + "/catalog/new-releases";
    public static final String SEARCH_URL = ORPHEUM_BASE + "/catalog/search";
    public static final String ALBUM_DETAIL_URL = ORPHEUM_BASE + "/albums/";
    public static final String HOME_URL = ORPHEUM_BASE + "/home";

    private static String normalizeBase(String url) {
        if (url == null) {
            return "";
        }
        url = url.trim();
        url = stripTrailingSlashes(url);
        if (url.endsWith("/orpheum")) {
            url = url.substring(0, url.length() - "/orpheum".length());
            url = stripTrailingSlashes(url);
        }
        return url;
    }

    private static String stripTrailingSlashes(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
