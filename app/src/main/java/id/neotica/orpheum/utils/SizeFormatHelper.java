package id.neotica.orpheum.utils;

public final class SizeFormatHelper {

    private SizeFormatHelper() { }

    public static String format(long bytes) {
        if (bytes <= 0) {
            return "0 KB";
        }
        long kb = Math.round(bytes / 1024.0);
        if (kb < 1024) {
            return kb + " KB";
        }
        long tenths = Math.round(bytes / (1024.0 * 1024.0) * 10);
        return (tenths / 10) + "." + (tenths % 10) + " MB";
    }
}
