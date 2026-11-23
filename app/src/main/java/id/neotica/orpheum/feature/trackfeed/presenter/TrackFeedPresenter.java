package id.neotica.orpheum.feature.trackfeed.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.trackfeed.contract.TrackFeedView;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.domain.Feed;

/**
 * MVP presenter owning the paginated new-tracks feed: paging state.
 * Rendering is delegated to a {@link TrackFeedView}; networking stays in the source layer.
 */
public class TrackFeedPresenter {

    private final Context context;

    private TrackFeedView view;
    private int page = 1;
    private boolean hasMore;

    public TrackFeedPresenter(Context context) {
        this.context = context;
    }

    public void attach(TrackFeedView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        page = 1;
        request(false);
    }

    public void loadMore() {
        if (!hasMore) {
            return;
        }
        page++;
        request(true);
    }

    private void request(final boolean append) {
        MusicSource source = SourceManager.get(context);
        source.loadTracks(context, page, 20, new SourceCallback<Feed<Track>>() {
            @Override
            public void onSuccess(Feed<Track> feed) {
                page = feed.page;
                hasMore = feed.hasMore;
                if (view != null) {
                    view.renderTracks(feed.items, append);
                    view.showLoadMore(hasMore);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (append && page > 1) {
                    page--;
                }
                if (view != null) {
                    view.showError(errorMessage);
                }
            }
        });
    }
}
