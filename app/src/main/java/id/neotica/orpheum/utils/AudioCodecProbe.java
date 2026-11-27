package id.neotica.orpheum.utils;

import android.content.Context;
import android.media.MediaPlayer;

import id.neotica.orpheum.R;

public final class AudioCodecProbe {

    private AudioCodecProbe() { }

    public static String probe(Context context) {
        StringBuilder result = new StringBuilder();
        result.append("MP3: ").append(probeFormat(context, R.raw.codec_test_mp3));
        result.append("\nAAC-LC: ").append(probeFormat(context, R.raw.codec_test_aac_lc));
        result.append("\nHE-AAC: ").append(probeFormat(context, R.raw.codec_test_he_aac));
        return result.toString();
    }

    private static String probeFormat(Context context, int resourceId) {
        MediaPlayer player = null;
        try {
            player = MediaPlayer.create(context, resourceId);
            return player != null ? "OK" : "FAILED";
        } catch (Throwable t) {
            return "FAILED (" + t.getClass().getSimpleName() + ")";
        } finally {
            if (player != null) {
                try {
                    player.release();
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }
    }
}
