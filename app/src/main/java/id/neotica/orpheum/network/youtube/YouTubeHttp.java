package id.neotica.orpheum.network.youtube;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import id.neotica.orpheum.BuildConfig;

public final class YouTubeHttp {

    private static final int DEFAULT_TIMEOUT = 15000;

    private YouTubeHttp() { }

    public static String get(String urlString) throws IOException {
        return get(urlString, DEFAULT_TIMEOUT);
    }

    public static String get(String urlString, int timeout) throws IOException {
        HttpURLConnection connection = null;
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("User-Agent",
                    "Orpheum/" + BuildConfig.VERSION_NAME + " (YouTube audio)");
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            in = code >= 200 && code <= 299
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = read(in);
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

    private static String read(InputStream in) throws IOException {
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
}
