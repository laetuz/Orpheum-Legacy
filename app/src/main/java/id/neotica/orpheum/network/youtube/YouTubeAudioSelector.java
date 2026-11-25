package id.neotica.orpheum.network.youtube;

public final class YouTubeAudioSelector {

    private YouTubeAudioSelector() { }

    public static int chooseBest(String[] mimeTypes, String[] formats, String[] codecs,
                                 int[] bitrates) {
        int best = -1;
        int bestBitrate = -1;
        for (int i = 0; i < mimeTypes.length; i++) {
            String mime = lower(mimeTypes[i]);
            String format = lower(formats != null && i < formats.length ? formats[i] : null);
            String codec = lower(codecs != null && i < codecs.length ? codecs[i] : null);

            if (mime.indexOf("webm") >= 0 || format.indexOf("webm") >= 0
                    || codec.startsWith("opus") || codec.startsWith("vorbis")) {
                continue;
            }
            boolean mp4 = mime.indexOf("audio/mp4") >= 0
                    || "m4a".equals(format)
                    || (codec != null && codec.startsWith("mp4a"));
            if (!mp4) {
                continue;
            }
            int bitrate = bitrates != null && i < bitrates.length ? bitrates[i] : 0;
            if (bitrate > bestBitrate) {
                best = i;
                bestBitrate = bitrate;
            }
        }
        return best;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
