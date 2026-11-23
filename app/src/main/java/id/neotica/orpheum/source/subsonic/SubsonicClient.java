package id.neotica.orpheum.source.subsonic;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Random;

import id.neotica.orpheum.utils.JsonHelper;

public class SubsonicClient {

    public static final String API_VERSION = "1.16.1";
    public static final String CLIENT_NAME = "Orpheum";

    private static final Random RANDOM = new Random();

    private final String base;
    private final String username;
    private final String salt;
    private final String token;

    public SubsonicClient(SubsonicConfig config) {
        this.base = config.getBase();
        this.username = config.getUsername();
        this.salt = config.getSalt();
        this.token = config.getToken();
    }

    public static String normalizeBase(String raw) {
        if (raw == null) {
            return "";
        }
        String base = raw.trim();
        if (base.length() == 0) {
            return "";
        }
        if (!base.contains("://")) {
            base = "http://" + base;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/rest")) {
            base = base.substring(0, base.length() - "/rest".length());
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
        }
        return base;
    }

    public static String randomSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return hex(bytes);
    }

    public static String token(String password, String salt) {
        if (password == null) {
            password = "";
        }
        if (salt == null) {
            salt = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return hex(digest.digest((password + salt).getBytes("UTF-8")));
        } catch (Throwable t) {
            return "";
        }
    }

    public static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return hex(digest.digest(input.getBytes("UTF-8")));
        } catch (Throwable t) {
            return "";
        }
    }

    public static String pingUrl(String base, String username, String salt, String token) {
        return buildUrl(base, "ping", username, salt, token, null);
    }

    public static String buildUrl(String base, String method, String username, String salt,
                                  String token, Map<String, String> extra) {
        StringBuilder url = new StringBuilder(normalizeBase(base));
        url.append("/rest/").append(method).append(".view");
        url.append("?u=").append(encode(username));
        url.append("&t=").append(encode(token));
        url.append("&s=").append(encode(salt));
        url.append("&v=").append(encode(API_VERSION));
        url.append("&c=").append(encode(CLIENT_NAME));
        url.append("&f=json");
        if (extra != null) {
            for (Map.Entry<String, String> entry : extra.entrySet()) {
                url.append('&').append(encode(entry.getKey())).append('=')
                        .append(encode(entry.getValue()));
            }
        }
        return url.toString();
    }

    public String get(String method, Map<String, String> extra) throws IOException, SubsonicException {
        String url = buildUrl(base, method, username, salt, token, extra);
        String body = read(url);
        rethrowIfFailed(body);
        return body;
    }

    public String streamUrl(String songId) {
        StringBuilder extra = new StringBuilder();
        extra.append("&id=").append(encode(songId));
        extra.append("&format=mp3");
        extra.append("&maxBitRate=192");
        return buildUrl(base, "stream", username, salt, token, null) + extra;
    }

    public String coverUrl(String coverArtId) {
        return buildUrl(base, "getCoverArt", username, salt, token, null)
                + "&id=" + encode(coverArtId) + "&size=600";
    }

    public String getBase() {
        return base;
    }

    public String getUsername() {
        return username;
    }

    public static String errorMessage(String body) {
        try {
            JSONObject root = new JSONObject(body).optJSONObject("subsonic-response");
            JSONObject error = root != null ? root.optJSONObject("error") : null;
            String message = JsonHelper.getString(error, "message");
            if (message.length() > 0) {
                return message;
            }
        } catch (Throwable t) {
            // Fall through
        }
        return "Subsonic request failed";
    }

    public static boolean isOk(String body) {
        try {
            JSONObject root = new JSONObject(body).optJSONObject("subsonic-response");
            return root != null && "ok".equals(JsonHelper.getString(root, "status"));
        } catch (Throwable t) {
            return false;
        }
    }

    private static void rethrowIfFailed(String body) throws SubsonicException {
        try {
            JSONObject root = new JSONObject(body).optJSONObject("subsonic-response");
            if (root == null) {
                throw new SubsonicException("Unexpected Subsonic response");
            }
            if (!"ok".equals(JsonHelper.getString(root, "status"))) {
                throw new SubsonicException(errorMessage(body));
            }
        } catch (SubsonicException e) {
            throw e;
        } catch (Throwable t) {
            throw new SubsonicException("Error parsing server data.");
        }
    }

    private static String read(String urlString) throws IOException {
        HttpURLConnection connection = null;
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            in = code >= 200 && code <= 299 ? connection.getInputStream() : connection.getErrorStream();
            String body = readStream(in);
            if (code < 200 || code > 299) {
                throw new IOException("HTTP " + code);
            }
            return body;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                    // Ignore
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        reader.close();
        return body.toString();
    }

    private static String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Throwable t) {
            return value;
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            if (value < 16) {
                hex.append('0');
            }
            hex.append(Integer.toHexString(value));
        }
        return hex.toString();
    }

    public static class SubsonicException extends Exception {
        public SubsonicException(String message) {
            super(message);
        }
    }
}
