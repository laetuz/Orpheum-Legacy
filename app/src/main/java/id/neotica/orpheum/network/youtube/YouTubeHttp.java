package id.neotica.orpheum.network.youtube;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

    public static ProbeResult probe(String urlString, int timeout) throws IOException {
        String current = urlString;
        for (int redirect = 0; redirect < 4; redirect++) {
            HttpURLConnection connection = open(current, timeout, false);
            try {
                connection.setRequestProperty("Range", "bytes=0-1");
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.length() == 0) {
                        throw new IOException("HTTP " + code + " without Location");
                    }
                    current = new URL(new URL(current), location).toString();
                    continue;
                }
                if (code < 200 || code > 299) {
                    throw new IOException("HTTP " + code);
                }
                String contentType = connection.getContentType();
                if (isHtml(contentType)) {
                    throw new IOException("HTML response");
                }
                return new ProbeResult(current, code, contentType);
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many redirects");
    }

    public static DownloadResult download(String urlString, File target,
                                          int maxBytes, int timeout) throws IOException {
        String current = urlString;
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create cache directory");
        }
        File temp = new File(target.getAbsolutePath() + ".part");
        for (int redirect = 0; redirect < 4; redirect++) {
            HttpURLConnection connection = open(current, timeout, false);
            InputStream in = null;
            OutputStream out = null;
            try {
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.length() == 0) {
                        throw new IOException("HTTP " + code + " without Location");
                    }
                    current = new URL(new URL(current), location).toString();
                    continue;
                }
                if (code < 200 || code > 299) {
                    throw new IOException("HTTP " + code);
                }
                String contentType = connection.getContentType();
                if (isHtml(contentType)) {
                    throw new IOException("HTML response");
                }
                long length = connection.getContentLength();
                if (length > maxBytes) {
                    throw new IOException("Audio file too large");
                }
                in = new BufferedInputStream(connection.getInputStream());
                out = new FileOutputStream(temp);
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > maxBytes) {
                        throw new IOException("Audio file too large");
                    }
                    out.write(buffer, 0, read);
                }
                out.flush();
                out.close();
                out = null;
                in.close();
                in = null;
                if (target.exists()) {
                    target.delete();
                }
                if (!temp.renameTo(target)) {
                    throw new IOException("Cannot save audio file");
                }
                return new DownloadResult(target, total, contentType, current);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                connection.disconnect();
                if (temp.exists()) {
                    temp.delete();
                }
            }
        }
        throw new IOException("Too many redirects");
    }

    private static HttpURLConnection open(String urlString, int timeout,
                                          boolean followRedirects) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setInstanceFollowRedirects(followRedirects);
        connection.setRequestProperty("User-Agent",
                "Orpheum/" + BuildConfig.VERSION_NAME + " (YouTube audio)");
        return connection;
    }

    private static boolean isHtml(String contentType) {
        if (contentType == null) {
            return false;
        }
        String value = contentType.toLowerCase();
        return value.indexOf("text/html") >= 0 || value.indexOf("text/plain") >= 0;
    }

    public static class ProbeResult {
        public final String url;
        public final int status;
        public final String contentType;

        ProbeResult(String url, int status, String contentType) {
            this.url = url;
            this.status = status;
            this.contentType = contentType;
        }
    }

    public static class DownloadResult {
        public final File file;
        public final int bytes;
        public final String contentType;
        public final String url;

        DownloadResult(File file, int bytes, String contentType, String url) {
            this.file = file;
            this.bytes = bytes;
            this.contentType = contentType;
            this.url = url;
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
