package id.neotica.orpheum.utils;

import android.os.Build;

/**
 * API 1-safe SDK level checks. {@code Build.VERSION.SDK_INT} is API 4+,
 * so the version string ({@code Build.VERSION.SDK}, API 1) is parsed instead.
 */
public class SdkCompat {

    private SdkCompat() { }

    public static final int SDK = parseSdk(Build.VERSION.SDK);

    public static boolean atLeast(int level) {
        return SDK >= level;
    }

    private static int parseSdk(String sdkString) {
        try {
            return Integer.parseInt(sdkString);
        } catch (Throwable t) {
            return 1;
        }
    }
}
