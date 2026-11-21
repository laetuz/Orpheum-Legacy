package id.neotica.orpheum.feature.player.presenter;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;

import id.neotica.orpheum.feature.player.PlaybackManager;
import id.neotica.orpheum.feature.player.contract.PlayerView;
import id.neotica.orpheum.model.Track;

/**
 * MVP presenter owning the player screen: playback commands, state and the
 * progress ticker. Rendering is delegated to a {@link PlayerView}.
 */
public class PlayerPresenter implements PlaybackManager.PlaybackListener {

    private static final int PROGRESS_INTERVAL = 500;

    private final Context context;
    private final Handler handler = new Handler();

    private PlayerView view;
    private boolean userSeeking;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (view == null) {
                return;
            }
            if (PlaybackManager.isPrepared()) {
                if (!userSeeking) {
                    view.renderProgress(PlaybackManager.getCurrentPosition(), PlaybackManager.getDuration());
                }
                handler.postDelayed(this, PROGRESS_INTERVAL);
            }
        }
    };

    public PlayerPresenter(Context context) {
        this.context = context;
    }

    public void attach(PlayerView view) {
        this.view = view;
        PlaybackManager.addListener(this);
        refresh();
    }

    public void detach() {
        PlaybackManager.removeListener(this);
        stopProgress();
        this.view = null;
    }

    public void init(ArrayList<Track> tracks, int index) {
        PlaybackManager.init(tracks, index);
    }

    public boolean hasTrack() {
        return PlaybackManager.hasTrack();
    }

    public void refresh() {
        pushTrack();
        pushState();
        if (PlaybackManager.isPrepared()) {
            startProgress();
        }
    }

    public void togglePlayPause() {
        PlaybackManager.toggle();
        pushState();
    }

    public void next() {
        PlaybackManager.next();
    }

    public void previous() {
        PlaybackManager.previous();
    }

    public void seekTo(int percent) {
        PlaybackManager.seekTo(percent);
    }

    public void setUserSeeking(boolean seeking) {
        this.userSeeking = seeking;
    }

    @Override
    public void onTrackChanged() {
        pushTrack();
    }

    @Override
    public void onPlaybackStateChanged() {
        pushState();
    }

    @Override
    public void onPrepared() {
        pushState();
        startProgress();
    }

    @Override
    public void onCompletion() {
        pushTrack();
    }

    @Override
    public void onError(String error) {
        if (view != null) {
            view.showError(error);
        }
    }

    private void pushTrack() {
        Track track = PlaybackManager.getCurrentTrack();
        if (view != null && track != null) {
            view.renderTrack(track);
        }
    }

    private void pushState() {
        if (view != null) {
            view.renderPlaybackState(PlaybackManager.isPlaying());
        }
    }

    private void startProgress() {
        stopProgress();
        handler.postDelayed(progressRunnable, PROGRESS_INTERVAL);
    }

    private void stopProgress() {
        handler.removeCallbacks(progressRunnable);
    }
}
