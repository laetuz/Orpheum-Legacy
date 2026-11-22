package id.neotica.orpheum.utils;

import android.text.TextUtils;

import id.neotica.orpheum.BuildConfig;

public class ImageUrlHelper {

    private ImageUrlHelper() { }

    public static String build(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        if (path.startsWith("file://")) {
            return path;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("/media/")) {
            return Constants.ORPHEUM_BASE + path;
        }
        if (path.startsWith("/buckets")) {
            return BuildConfig.FILE_BASE_URL + path;
        }
        if (path.startsWith("/")) {
            return BuildConfig.FILE_BASE_URL + "/buckets" + path;
        }
        return BuildConfig.FILE_BASE_URL + "/buckets/" + path;
    }
}
