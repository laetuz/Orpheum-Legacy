package id.neotica.orpheum.feature.youtube;

import android.content.Context;

import java.io.IOException;

import id.neotica.orpheum.feature.player.PlaybackManager;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.youtube.YouTubeDiagnostics;
import id.neotica.orpheum.network.youtube.YouTubeManager;

public class YouTubeStreamResolver implements PlaybackManager.StreamUrlResolver {

    private final Context context;

    public YouTubeStreamResolver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String resolve(Track track) throws Exception {
        if (track == null || !"youtube".equals(track.source)) {
            throw new IOException("Not a YouTube track");
        }
        return YouTubeManager.get(context).resolveAudio(track);
    }

    @Override
    public boolean onPlaybackError(Track track, int what, int extra) {
        YouTubeDiagnostics.append(what < 0
                ? "Player prepare timed out"
                : "Player error: what=" + what + " extra=" + extra);
        return YouTubeManager.get(context).onPlaybackError(track);
    }
}
