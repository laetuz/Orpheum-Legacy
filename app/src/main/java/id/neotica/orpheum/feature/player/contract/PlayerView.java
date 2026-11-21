package id.neotica.orpheum.feature.player.contract;

import id.neotica.orpheum.model.Track;

/**
 * View contract for the player screen, implemented by the player Activity.
 * {@code PlayerPresenter} drives all rendering through this interface.
 */
public interface PlayerView {

    void renderTrack(Track track);

    void renderPlaybackState(boolean isPlaying);

    void renderProgress(int positionMs, int durationMs);

    void showError(String message);
}
