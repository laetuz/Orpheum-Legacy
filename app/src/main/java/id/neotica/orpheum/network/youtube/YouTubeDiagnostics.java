package id.neotica.orpheum.network.youtube;

public final class YouTubeDiagnostics {

    private static String report = "No YouTube playback attempt yet.";

    private YouTubeDiagnostics() { }

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
