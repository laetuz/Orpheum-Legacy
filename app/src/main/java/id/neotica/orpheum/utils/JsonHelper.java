package id.neotica.orpheum.utils;

import org.json.JSONObject;

/**
 * API 1-safe JSONObject accessors. The two-argument
 * {@code optString/optInt/optLong(name, fallback)} overloads are API 9+.
 */
public class JsonHelper {

    private JsonHelper() { }

    public static String getString(JSONObject obj, String key) {
        if (obj == null || obj.isNull(key)) {
            return "";
        }
        return obj.optString(key);
    }

    public static String getStringOrNull(JSONObject obj, String key) {
        if (obj == null || obj.isNull(key)) {
            return null;
        }
        return obj.optString(key);
    }

    public static int getInt(JSONObject obj, String key, int fallback) {
        if (obj == null || obj.isNull(key)) {
            return fallback;
        }
        return obj.optInt(key);
    }

    public static long getLong(JSONObject obj, String key, long fallback) {
        if (obj == null || obj.isNull(key)) {
            return fallback;
        }
        return obj.optLong(key);
    }
}
