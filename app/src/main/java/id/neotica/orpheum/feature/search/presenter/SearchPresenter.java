package id.neotica.orpheum.feature.search.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.search.contract.SearchView;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.domain.Feed;

/**
 * MVP presenter owning track search: query state and paging.
 * Rendering is delegated to a {@link SearchView}; networking stays in the source layer.
 */
public class SearchPresenter {

    private final Context context;

    private SearchView view;
    private String query = "";
    private int page = 1;
    private boolean hasMore;

    public SearchPresenter(Context context) {
        this.context = context;
    }

    public void attach(SearchView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void search(String query) {
        this.query = query == null ? "" : query.trim();
        this.page = 1;
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
        source.search(context, query, page, 20, new SourceCallback<Feed<Track>>() {
            @Override
            public void onSuccess(Feed<Track> feed) {
                page = feed.page;
                hasMore = feed.hasMore;
                if (view != null) {
                    view.renderResults(feed.items, append);
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
