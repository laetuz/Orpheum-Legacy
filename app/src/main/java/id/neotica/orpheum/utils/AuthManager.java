package id.neotica.orpheum.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {

    private static final String PREF_NAME = "NeostoreAuth";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRATION_TIME = "expiration_time";
    private static final String KEY_USERNAME = "username";

    private SharedPreferences prefs;

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).commit();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveRefreshToken(String refreshToken) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).commit();
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void saveExpirationTime(long expirationTime) {
        prefs.edit().putLong(KEY_EXPIRATION_TIME, expirationTime).commit();
    }

    public long getExpirationTime() {
        return prefs.getLong(KEY_EXPIRATION_TIME, 0);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public String getAuthorizationHeader() {
        String token = getToken();
        if (token != null) {
            return "Bearer " + token;
        }
        return null;
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).commit();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_EXPIRATION_TIME)
                .remove(KEY_USERNAME)
                .commit();
    }

    public Map<String, String> getAuthHeaders() {
        String token = getToken();
        if (token != null) {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + token);
            return headers;
        }
        return null;
    }

    public String getUsernameFromToken() {
        String stored = getUsername();
        if (stored != null) return stored;
        String token = getToken();
        if (token == null) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            byte[] decoded = base64UrlDecode(parts[1]);
            if (decoded == null) return null;
            String json = new String(decoded, "UTF-8");
            JSONObject payload = new JSONObject(json);
            if (payload.has("sub")) return payload.optString("sub");
            if (payload.has("username")) return payload.optString("username");
            if (payload.has("name")) return payload.optString("name");
            if (payload.has("preferred_username")) return payload.optString("preferred_username");
            if (payload.has("email")) return payload.optString("email");
            if (payload.has("id")) return payload.optString("id");
            return "User";
        } catch (Throwable e) {
            return null;
        }
    }

    private static byte[] base64UrlDecode(String input) {
        try {
            String base64 = input.replace('-', '+').replace('_', '/');
            int padding = 4 - base64.length() % 4;
            if (padding != 4) {
                base64 += "====".substring(0, padding);
            }
            return base64Decode(base64);
        } catch (Throwable e) {
            return null;
        }
    }

    private static byte[] base64Decode(String s) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        s = s.replaceAll("[^" + chars + "=]", "");
        if (s.length() == 0) return new byte[0];
        int pad = s.endsWith("==") ? 2 : s.endsWith("=") ? 1 : 0;
        byte[] out = new byte[s.length() * 3 / 4 - pad];
        int i = 0, o = 0;
        while (i < s.length()) {
            int a = chars.indexOf(s.charAt(i++));
            int b = chars.indexOf(s.charAt(i++));
            int c = i < s.length() ? chars.indexOf(s.charAt(i++)) : 64;
            int d = i < s.length() ? chars.indexOf(s.charAt(i++)) : 64;
            out[o++] = (byte) ((a << 2) | (b >> 4));
            if (c != 64) out[o++] = (byte) ((b << 4) | (c >> 2));
            if (d != 64) out[o++] = (byte) ((c << 6) | d);
        }
        return out;
    }
}
