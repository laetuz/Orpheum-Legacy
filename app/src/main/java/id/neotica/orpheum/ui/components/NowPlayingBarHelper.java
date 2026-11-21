package id.neotica.orpheum.ui.components;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.player.PlaybackManager;
import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.model.Track;

/**
 * Wires the shared now-playing bar included at the bottom of feed screens.
 */
public class NowPlayingBarHelper {

    private NowPlayingBarHelper() { }

    public static void setup(final Activity activity) {
        View bar = activity.findViewById(R.id.now_playing_bar);
        ImageButton npPlayPause = (ImageButton) activity.findViewById(R.id.np_play_pause);
        if (bar == null || npPlayPause == null) {
            return;
        }

        bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.startActivity(new Intent(activity, PlayerActivity.class));
            }
        });

        npPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackManager.toggle();
                refresh(activity);
            }
        });
    }

    public static void refresh(Activity activity) {
        View bar = activity.findViewById(R.id.now_playing_bar);
        if (bar == null) {
            return;
        }

        if (PlaybackManager.hasTrack()) {
            bar.setVisibility(View.VISIBLE);

            TextView npTitle = (TextView) activity.findViewById(R.id.np_title);
            TextView npArtist = (TextView) activity.findViewById(R.id.np_artist);
            ImageButton npPlayPause = (ImageButton) activity.findViewById(R.id.np_play_pause);

            Track track = PlaybackManager.getCurrentTrack();
            if (track != null && npTitle != null) {
                npTitle.setText(track.title);
                if (npArtist != null) {
                    npArtist.setText(!TextUtils.isEmpty(track.artistName) ? track.artistName : "");
                }
            }
            if (npPlayPause != null) {
                npPlayPause.setImageResource(PlaybackManager.isPlaying()
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play);
            }
        } else {
            bar.setVisibility(View.GONE);
        }
    }
}
