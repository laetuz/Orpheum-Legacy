package id.neotica.orpheum.feature.albumfeed.contract;

import java.util.List;

import id.neotica.orpheum.model.Album;

/**
 * View contract for the new-releases feed, implemented by the feed Activity.
 * {@code AlbumFeedPresenter} drives all rendering through this interface.
 */
public interface AlbumFeedView {

    void renderAlbums(List<Album> albums, boolean append);

    void showLoadMore(boolean hasMore);

    void showError(String message);
}
