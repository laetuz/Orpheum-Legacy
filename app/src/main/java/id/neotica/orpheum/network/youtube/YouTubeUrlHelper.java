package id.neotica.orpheum.network.youtube;

import java.net.URLEncoder;

public final class YouTubeUrlHelper {

    private YouTubeUrlHelper() { }

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
        return base;
    }

    public static String encode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Throwable t) {
            return value;
        }
    }

    public static String mapThrough(String base, String url) {
        String normalizedBase = normalizeBase(base);
        if (normalizedBase.length() == 0 || url == null) {
            return "";
        }
        String value = url.trim();
        if (value.length() == 0) {
            return "";
        }
        if (value.startsWith("//")) {
            value = (normalizedBase.startsWith("https://") ? "https:" : "http:") + value;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int hostEnd = value.indexOf("://") + 3;
            int pathStart = value.indexOf('/', hostEnd);
            return pathStart == -1 ? normalizedBase : normalizedBase + value.substring(pathStart);
        }
        if (value.startsWith("/")) {
            return normalizedBase + value;
        }
        int slash = value.indexOf('/');
        if (slash > 0) {
            String host = value.substring(0, slash);
            if (host.indexOf('.') >= 0 || host.indexOf(':') >= 0) {
                return mapThrough(normalizedBase, "http://" + value);
            }
        }
        return normalizedBase + "/" + value;
    }

    public static String safeUrl(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        try {
            java.net.URL url = new java.net.URL(value);
            String path = url.getPath();
            int port = url.getPort();
            return url.getProtocol() + "://" + url.getHost()
                    + (port > 0 ? ":" + port : "")
                    + (path == null ? "" : path);
        } catch (Throwable t) {
            return "";
        }
    }

    public static String videoIdFromUrl(String url) {
        if (url == null) {
            return "";
        }
        int query = url.indexOf("v=");
        if (query >= 0) {
            String rest = url.substring(query + 2);
            int amp = rest.indexOf('&');
            return amp >= 0 ? rest.substring(0, amp) : rest;
        }
        int youtu = url.indexOf("youtu.be/");
        if (youtu >= 0) {
            String rest = url.substring(youtu + "youtu.be/".length());
            int slash = rest.indexOf('/');
            return slash >= 0 ? rest.substring(0, slash) : rest;
        }
        return url.length() == 11 ? url : "";
    }
}
