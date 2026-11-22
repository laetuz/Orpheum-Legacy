package id.neotica.orpheum.feature.radio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import id.neotica.orpheum.OrpheumApp;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * Local favorite-station index (SharedPreferences JSON). Stations are stored by
 * guide id and re-tuned when played, since TuneIn stream URLs rotate.
 */
public class FavoriteStationStore {

    private static final String PREFS_NAME = "OrpheumRadioFavorites";
    private static final String KEY_STATIONS = "stations";
    private static final Object LOCK = new Object();

    private static Set<String> favoriteIds;

    private FavoriteStationStore() { }

    public static List<RadioItem> getAll() {
        synchronized (LOCK) {
            List<RadioItem> result = new ArrayList<RadioItem>();
            JSONArray array = loadArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String guideId = JsonHelper.getString(obj, "guide_id");
                if (guideId.length() == 0) {
                    continue;
                }
                result.add(RadioItem.station(
                        guideId,
                        JsonHelper.getString(obj, "name"),
                        JsonHelper.getString(obj, "description"),
                        JsonHelper.getString(obj, "image_url")));
            }
            return result;
        }
    }

    public static boolean isFavorite(String guideId) {
        if (guideId == null || guideId.length() == 0) {
            return false;
        }
        synchronized (LOCK) {
            ensureFavoriteIdsLocked();
            return favoriteIds.contains(guideId);
        }
    }

    /**
     * Adds or removes the station. Returns true when it is now a favorite.
     */
    public static boolean toggle(RadioItem item) {
        if (item == null || item.guideId == null || item.guideId.length() == 0) {
            return false;
        }
        synchronized (LOCK) {
            JSONArray array = loadArray();
            int index = indexOf(array, item.guideId);
            if (index >= 0) {
                saveArrayLocked(removeAt(array, index));
                return false;
            }

            JSONArray updated = new JSONArray();
            updated.put(itemToJson(item));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    updated.put(obj);
                }
            }
            saveArrayLocked(updated);
            return true;
        }
    }

    public static void remove(String guideId) {
        if (guideId == null) {
            return;
        }
        synchronized (LOCK) {
            JSONArray array = loadArray();
            int index = indexOf(array, guideId);
            if (index >= 0) {
                saveArrayLocked(removeAt(array, index));
            }
        }
    }

    private static boolean exists(JSONArray array, String guideId) {
        return indexOf(array, guideId) >= 0;
    }

    private static int indexOf(JSONArray array, String guideId) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && guideId.equals(JsonHelper.getString(obj, "guide_id"))) {
                return i;
            }
        }
        return -1;
    }

    private static JSONArray removeAt(JSONArray source, int index) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            if (i != index) {
                JSONObject obj = source.optJSONObject(i);
                if (obj != null) {
                    result.put(obj);
                }
            }
        }
        return result;
    }

    private static JSONObject itemToJson(RadioItem item) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("guide_id", item.guideId);
            obj.put("name", item.name != null ? item.name : "");
            obj.put("description", item.description != null ? item.description : "");
            obj.put("image_url", item.imageUrl != null ? item.imageUrl : "");
            obj.put("added_at", System.currentTimeMillis());
        } catch (Throwable t) {
            // Keep whatever was written
        }
        return obj;
    }

    private static void ensureFavoriteIdsLocked() {
        if (favoriteIds != null) {
            return;
        }
        Set<String> ids = new HashSet<String>();
        JSONArray array = loadArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                String guideId = JsonHelper.getString(obj, "guide_id");
                if (guideId.length() > 0) {
                    ids.add(guideId);
                }
            }
        }
        favoriteIds = ids;
    }

    private static SharedPreferences prefs() {
        return OrpheumApp.getInstance().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static JSONArray loadArray() {
        String raw = prefs().getString(KEY_STATIONS, null);
        if (raw == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (Throwable t) {
            return new JSONArray();
        }
    }

    private static void saveArrayLocked(JSONArray array) {
        prefs().edit().putString(KEY_STATIONS, array.toString()).commit();
        favoriteIds = null;
    }
}
