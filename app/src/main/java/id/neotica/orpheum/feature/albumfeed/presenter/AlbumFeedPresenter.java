package id.neotica.orpheum.feature.albumfeed.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.albumfeed.contract.AlbumFeedView;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.domain.Feed;

/**
 * MVP presenter owning the paginated new-releases feed: paging state.
 * Rendering is delegated to an {@link AlbumFeedView}; networking stays in the source layer.
 */
public class AlbumFeedPresenter {

    private final Context context;

    private AlbumFeedView view;
    private int page = 1;
    private boolean hasMore;

    public AlbumFeedPresenter(Context context) {
        this.context = context;
    }

    public void attach(AlbumFeedView view) {
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
        source.loadAlbums(context, page, 20, new SourceCallback<Feed<Album>>() {
            @Override
            public void onSuccess(Feed<Album> feed) {
                page = feed.page;
                hasMore = feed.hasMore;
                if (view != null) {
                    view.renderAlbums(feed.items, append);
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
