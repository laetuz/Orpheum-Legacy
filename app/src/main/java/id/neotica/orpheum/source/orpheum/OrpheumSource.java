package id.neotica.orpheum.source.orpheum;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourcePrefs;
import id.neotica.orpheum.source.domain.AlbumDetailData;
import id.neotica.orpheum.source.domain.Feed;
import id.neotica.orpheum.source.domain.HomeFeed;
import id.neotica.orpheum.utils.AuthManager;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

public class OrpheumSource implements MusicSource {

    private final Context context;
    private final AuthManager authManager;

    public OrpheumSource(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = new AuthManager(this.context);
    }

    @Override
    public void loadHome(Context uiContext, int limit, SourceCallback<HomeFeed> callback) {
        final SourceCallback<HomeFeed> cb = callback;
        new ApiTask(uiContext, "GET", Constants.HOME_URL + "?limit=" + limit, null, null,
                new ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            JSONObject root = new JSONObject(response);
                            List<Album> albums = new ArrayList<Album>();
                            JSONArray albumsArray = root.optJSONArray("recent_albums");
                            if (albumsArray != null) {
                                for (int i = 0; i < albumsArray.length(); i++) {
                                    Album album = Album.fromJson(albumsArray.optJSONObject(i));
                                    if (album != null) {
                                        albums.add(album);
                                    }
                                }
                            }
                            List<Track> tracks = new ArrayList<Track>();
                            JSONArray tracksArray = root.optJSONArray("recent_tracks");
                            if (tracksArray != null) {
                                for (int i = 0; i < tracksArray.length(); i++) {
                                    Track track = Track.fromJson(tracksArray.optJSONObject(i));
                                    if (track != null) {
                                        tracks.add(track);
                                    }
                                }
                            }
                            cb.onSuccess(new HomeFeed(albums, tracks));
                        } catch (Exception e) {
                            cb.onError("Error parsing server data.");
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        cb.onError(errorMessage);
                    }
                }).execute();
    }

    @Override
    public void loadTracks(Context uiContext, final int page, int limit,
                           final SourceCallback<Feed<Track>> callback) {
        String url = Constants.TRACK_FEED_URL + "?page=" + page + "&limit=" + limit;
        new ApiTask(uiContext, "GET", url, null, "Loading tracks...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    int resultPage = JsonHelper.getInt(root, "page", page);
                    int totalPages = JsonHelper.getInt(root, "total_pages", 1);
                    List<Track> tracks = new ArrayList<Track>();
                    JSONArray data = root.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            Track track = Track.fromJson(data.optJSONObject(i));
                            if (track != null) {
                                tracks.add(track);
                            }
                        }
                    }
                    callback.onSuccess(new Feed<Track>(tracks, resultPage, resultPage < totalPages));
                } catch (Exception e) {
                    callback.onError("Error parsing server data.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        }).execute();
    }

    @Override
    public void loadAlbums(Context uiContext, final int page, int limit,
                           final SourceCallback<Feed<Album>> callback) {
        String url = Constants.ALBUM_FEED_URL + "?page=" + page + "&limit=" + limit;
        new ApiTask(uiContext, "GET", url, null, "Loading albums...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    int resultPage = JsonHelper.getInt(root, "page", page);
                    int totalPages = JsonHelper.getInt(root, "total_pages", 1);
                    List<Album> albums = new ArrayList<Album>();
                    JSONArray data = root.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            Album album = Album.fromJson(data.optJSONObject(i));
                            if (album != null) {
                                albums.add(album);
                            }
                        }
                    }
                    callback.onSuccess(new Feed<Album>(albums, resultPage, resultPage < totalPages));
                } catch (Exception e) {
                    callback.onError("Error parsing server data.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        }).execute();
    }

    @Override
    public void search(Context uiContext, String query, final int page, int limit,
                       final SourceCallback<Feed<Track>> callback) {
        String encoded;
        try {
            encoded = URLEncoder.encode(query == null ? "" : query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encoded = query == null ? "" : query;
        }
        String url = Constants.SEARCH_URL + "?q=" + encoded + "&page=" + page + "&limit=" + limit;
        new ApiTask(uiContext, "GET", url, null, "Searching...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    int resultPage = JsonHelper.getInt(root, "page", page);
                    int totalPages = JsonHelper.getInt(root, "total_pages", 1);
                    List<Track> tracks = new ArrayList<Track>();
                    JSONArray data = root.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            Track track = Track.fromJson(data.optJSONObject(i));
                            if (track != null) {
                                tracks.add(track);
                            }
                        }
                    }
                    callback.onSuccess(new Feed<Track>(tracks, resultPage, resultPage < totalPages));
                } catch (Exception e) {
                    callback.onError("Error parsing server data.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        }).execute();
    }

    @Override
    public void loadAlbum(Context uiContext, String albumId, final SourceCallback<AlbumDetailData> callback) {
        String url = Constants.ALBUM_DETAIL_URL + albumId;
        new ApiTask(uiContext, "GET", url, null, "Loading album...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    JSONObject albumObj = root.optJSONObject("album");
                    if (albumObj == null) {
                        callback.onError("Album not found.");
                        return;
                    }
                    Album album = Album.fromJson(albumObj);
                    List<Track> tracks = new ArrayList<Track>();
                    JSONArray tracksArray = root.optJSONArray("tracks");
                    if (tracksArray != null) {
                        for (int i = 0; i < tracksArray.length(); i++) {
                            Track track = Track.fromJson(tracksArray.optJSONObject(i));
                            if (track != null) {
                                tracks.add(track);
                            }
                        }
                    }
                    callback.onSuccess(new AlbumDetailData(album, tracks));
                } catch (Exception e) {
                    callback.onError("Error parsing server data.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        }).execute();
    }

    @Override
    public boolean isLoggedIn() {
        return authManager.isLoggedIn();
    }

    @Override
    public String getDisplayName() {
        String username = authManager.getUsernameFromToken();
        return username != null ? username : "User";
    }

    @Override
    public String getDescription() {
        return "Orpheum account";
    }

    @Override
    public String getId() {
        return SourcePrefs.SOURCE_ORPHEUM;
    }

    @Override
    public void logout() {
        authManager.clear();
    }
}
