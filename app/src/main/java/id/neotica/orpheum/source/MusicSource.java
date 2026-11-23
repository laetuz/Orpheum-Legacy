package id.neotica.orpheum.source;

import android.content.Context;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.source.domain.AlbumDetailData;
import id.neotica.orpheum.source.domain.Feed;
import id.neotica.orpheum.source.domain.HomeFeed;

public interface MusicSource {

    void loadHome(Context uiContext, int limit, SourceCallback<HomeFeed> callback);

    void loadTracks(Context uiContext, int page, int limit, SourceCallback<Feed<Track>> callback);

    void loadAlbums(Context uiContext, int page, int limit, SourceCallback<Feed<Album>> callback);

    void search(Context uiContext, String query, int page, int limit, SourceCallback<Feed<Track>> callback);

    void loadAlbum(Context uiContext, String albumId, SourceCallback<AlbumDetailData> callback);

    boolean isLoggedIn();

    String getDisplayName();

    String getDescription();

    String getId();

    void logout();
}
