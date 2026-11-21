package id.neotica.orpheum.feature.trackfeed.contract;

import java.util.List;

import id.neotica.orpheum.model.Track;

/**
 * View contract for the new-tracks feed, implemented by the feed Activity.
 * {@code TrackFeedPresenter} drives all rendering through this interface.
 */
public interface TrackFeedView {

    void renderTracks(List<Track> tracks, boolean append);

    void showLoadMore(boolean hasMore);

    void showError(String message);
}
