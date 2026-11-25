package id.neotica.orpheum.network.youtube;

public final class YouTubeDiagnostics {

    private static String report = "No YouTube playback attempt yet.";
    private static String currentTrack = "";

    private YouTubeDiagnostics() { }

    public static synchronized void start(String trackKey, String title) {
        if (trackKey == null) {
            trackKey = "";
        }
        if (!trackKey.equals(currentTrack)) {
            currentTrack = trackKey;
            report = "Track: " + (title == null ? trackKey : title);
        } else {
            report = report + "\n--- resolution retry ---";
        }
    }

    public static synchronized void set(String value) {
        report = value == null || value.length() == 0
                ? "No YouTube playback attempt yet."
                : value;
    }

    public static synchronized void append(String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        report = report + "\n" + value;
    }

    public static synchronized String getReport() {
        return report;
    }
}
