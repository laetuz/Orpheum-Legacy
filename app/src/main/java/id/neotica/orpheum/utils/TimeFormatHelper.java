package id.neotica.orpheum.utils;

public class TimeFormatHelper {

    private TimeFormatHelper() { }

    public static String formatDuration(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int min = seconds / 60;
        int sec = seconds % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(min);
        sb.append(":");
        if (sec < 10) {
            sb.append("0");
        }
        sb.append(sec);
        return sb.toString();
    }
}
