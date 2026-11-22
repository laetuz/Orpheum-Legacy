package id.neotica.orpheum.feature.radio.ui;

import android.app.Activity;
import android.text.TextUtils;

import java.util.ArrayList;

import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.model.Track;

/**
 * Builds a one-station playback queue and opens the player.
 */
public final class RadioPlaybackHelper {

    private RadioPlaybackHelper() { }

    public static void play(Activity activity, RadioItem station, String streamUrl) {
        if (activity == null || station == null || TextUtils.isEmpty(streamUrl)) {
            return;
        }

        Track track = new Track(station.guideId, "", station.name, 0, "", 0,
                !TextUtils.isEmpty(station.description) ? station.description : "",
                station.imageUrl);
        track.streamUrl = streamUrl;

        ArrayList<Track> queue = new ArrayList<Track>();
        queue.add(track);
        activity.startActivity(PlayerActivity.createIntent(activity, queue, 0));
    }
}
