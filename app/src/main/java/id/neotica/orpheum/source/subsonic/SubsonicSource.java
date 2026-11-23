package id.neotica.orpheum.source.subsonic;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourcePrefs;
import id.neotica.orpheum.source.domain.AlbumDetailData;
import id.neotica.orpheum.source.domain.Feed;
import id.neotica.orpheum.source.domain.HomeFeed;
import id.neotica.orpheum.utils.JsonHelper;

public class SubsonicSource implements MusicSource {

    private static final int HOME_TRACK_LIMIT = 10;
    private static final int HOME_TRACK_ALBUMS = 3;
    private static final int TRACK_ALBUM_BATCH = 10;

    private final SubsonicConfig config;
    private final SubsonicClient client;
    private final Object cacheLock = new Object();
    private final ArrayList<Track> trackCache = new ArrayList<Track>();

    private int albumOffset;
    private boolean albumsExhausted;

    public SubsonicSource(Context context) {
        this.config = new SubsonicConfig(context);
        this.client = new SubsonicClient(config);
    }

    @Override
    public void loadHome(Context uiContext, final int limit, final SourceCallback<HomeFeed> callback) {
        if (!isLoggedIn()) {
            callback.onError("No Subsonic server configured.");
            return;
        }
        runTask(uiContext, null, new Worker<HomeFeed>() {
            @Override
            public HomeFeed run() throws Exception {
                List<Album> albums = fetchAlbums(0, limit);
                List<Track> tracks = new ArrayList<Track>();
                int count = Math.min(HOME_TRACK_ALBUMS, albums.size());
                for (int i = 0; i < count; i++) {
                    AlbumDetailData detail = fetchAlbumDetail(albums.get(i).id);
                    tracks.addAll(detail.tracks);
                }
                if (tracks.size() > HOME_TRACK_LIMIT) {
                    tracks = new ArrayList<Track>(tracks.subList(0, HOME_TRACK_LIMIT));
                }
                return new HomeFeed(albums, tracks);
            }
        }, callback);
    }

    @Override
    public void loadTracks(Context uiContext, final int page, final int limit,
                           final SourceCallback<Feed<Track>> callback) {
        if (!isLoggedIn()) {
            callback.onError("No Subsonic server configured.");
            return;
        }
        final int requestedPage = page < 1 ? 1 : page;
        if (requestedPage == 1) {
            synchronized (cacheLock) {
                trackCache.clear();
                albumOffset = 0;
                albumsExhausted = false;
            }
        }
        runTask(uiContext, "Loading tracks...", new Worker<Feed<Track>>() {
            @Override
            public Feed<Track> run() throws Exception {
                synchronized (cacheLock) {
                    int required = requestedPage * limit;
                    while (trackCache.size() < required && !albumsExhausted) {
                        fetchNextAlbumBatch();
                    }
                    int start = Math.min((requestedPage - 1) * limit, trackCache.size());
                    int end = Math.min(start + limit, trackCache.size());
                    List<Track> items = new ArrayList<Track>(trackCache.subList(start, end));
                    boolean hasMore = end < trackCache.size() || !albumsExhausted;
                    return new Feed<Track>(items, requestedPage, hasMore);
                }
            }
        }, callback);
    }

    @Override
    public void loadAlbums(Context uiContext, final int page, final int limit,
                           final SourceCallback<Feed<Album>> callback) {
        if (!isLoggedIn()) {
            callback.onError("No Subsonic server configured.");
            return;
        }
        final int requestedPage = page < 1 ? 1 : page;
        runTask(uiContext, "Loading albums...", new Worker<Feed<Album>>() {
            @Override
            public Feed<Album> run() throws Exception {
                List<Album> albums = fetchAlbums((requestedPage - 1) * limit, limit);
                return new Feed<Album>(albums, requestedPage, albums.size() == limit);
            }
        }, callback);
    }

    @Override
    public void search(Context uiContext, final String query, final int page, final int limit,
                       final SourceCallback<Feed<Track>> callback) {
        if (!isLoggedIn()) {
            callback.onError("No Subsonic server configured.");
            return;
        }
        final int requestedPage = page < 1 ? 1 : page;
        runTask(uiContext, "Searching...", new Worker<Feed<Track>>() {
            @Override
            public Feed<Track> run() throws Exception {
                List<Track> empty = new ArrayList<Track>();
                String trimmed = query == null ? "" : query.trim();
                if (trimmed.length() == 0) {
                    return new Feed<Track>(empty, requestedPage, false);
                }
                Map<String, String> params = new HashMap<String, String>();
                params.put("query", trimmed);
                params.put("songCount", String.valueOf(limit));
                params.put("songOffset", String.valueOf((requestedPage - 1) * limit));
                params.put("albumCount", "0");
                params.put("artistCount", "0");

                String response;
                try {
                    response = client.get("search3", params);
                } catch (SubsonicClient.SubsonicException e) {
                    response = client.get("search2", params);
                }
                JSONObject root = root(response);
                JSONObject result = root.optJSONObject("searchResult3");
                if (result == null) {
                    result = root.optJSONObject("searchResult2");
                }
                List<Track> tracks = parseSongs(result, "song", "");
                return new Feed<Track>(tracks, requestedPage, tracks.size() == limit);
            }
        }, callback);
    }

    @Override
    public void loadAlbum(Context uiContext, String albumId, final SourceCallback<AlbumDetailData> callback) {
        if (!isLoggedIn()) {
            callback.onError("No Subsonic server configured.");
            return;
        }
        final String id = albumId;
        runTask(uiContext, "Loading album...", new Worker<AlbumDetailData>() {
            @Override
            public AlbumDetailData run() throws Exception {
                return fetchAlbumDetail(id);
            }
        }, callback);
    }

    @Override
    public boolean isLoggedIn() {
        return config.isConfigured();
    }

    @Override
    public String getDisplayName() {
        String username = config.getUsername();
        return username.length() > 0 ? username : "User";
    }

    @Override
    public String getDescription() {
        return config.getBase();
    }

    @Override
    public String getId() {
        return SourcePrefs.SOURCE_SUBSONIC;
    }

    @Override
    public void logout() {
        config.clear();
    }

    private void fetchNextAlbumBatch() throws Exception {
        List<Album> albums = fetchAlbums(albumOffset, TRACK_ALBUM_BATCH);
        albumOffset += albums.size();
        if (albums.size() < TRACK_ALBUM_BATCH) {
            albumsExhausted = true;
        }
        List<Track> batch = new ArrayList<Track>();
        for (int i = 0; i < albums.size(); i++) {
            AlbumDetailData detail = fetchAlbumDetail(albums.get(i).id);
            batch.addAll(detail.tracks);
        }
        trackCache.addAll(batch);
    }

    private List<Album> fetchAlbums(int offset, int size) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", "newest");
        params.put("size", String.valueOf(size));
        params.put("offset", String.valueOf(offset));
        String response = client.get("getAlbumList2", params);
        JSONObject root = root(response);
        JSONObject list = root.optJSONObject("albumList2");
        if (list == null) {
            list = root.optJSONObject("albumList");
        }
        JSONArray array = asArray(list, "album");
        List<Album> albums = new ArrayList<Album>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Album album = albumFromJson(array.optJSONObject(i));
                if (album != null) {
                    albums.add(album);
                }
            }
        }
        return albums;
    }

    private AlbumDetailData fetchAlbumDetail(String albumId) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("id", albumId);
        String response = client.get("getAlbum", params);
        JSONObject root = root(response);
        JSONObject albumObj = root.optJSONObject("album");
        if (albumObj == null) {
            throw new SubsonicClient.SubsonicException("Album not found.");
        }
        Album album = albumFromJson(albumObj);
        List<Track> tracks = parseSongs(albumObj, "song", album != null ? album.coverUrl : "");
        Collections.sort(tracks, new Comparator<Track>() {
            @Override
            public int compare(Track a, Track b) {
                if (a.trackNumber == b.trackNumber) {
                    return 0;
                }
                return a.trackNumber < b.trackNumber ? -1 : 1;
            }
        });
        if (album == null) {
            throw new SubsonicClient.SubsonicException("Album not found.");
        }
        return new AlbumDetailData(album, tracks);
    }

    private List<Track> parseSongs(JSONObject container, String key, String fallbackCover) {
        List<Track> tracks = new ArrayList<Track>();
        JSONArray array = asArray(container, key);
        if (array == null) {
            return tracks;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject song = array.optJSONObject(i);
            if (song != null) {
                tracks.add(trackFromJson(song, fallbackCover));
            }
        }
        return tracks;
    }

    private Album albumFromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        String id = JsonHelper.getString(obj, "id");
        if (id.length() == 0) {
            return null;
        }
        String title = JsonHelper.getString(obj, "name");
        if (title.length() == 0) {
            title = JsonHelper.getString(obj, "title");
        }
        String coverArt = JsonHelper.getString(obj, "coverArt");
        String coverUrl = coverArt.length() > 0 ? client.coverUrl(coverArt) : "";
        return new Album(
                id,
                JsonHelper.getString(obj, "artistId"),
                title,
                JsonHelper.getInt(obj, "year", 0),
                coverUrl,
                JsonHelper.getString(obj, "artist"),
                JsonHelper.getString(obj, "created"));
    }

    private Track trackFromJson(JSONObject song, String fallbackCover) {
        String id = JsonHelper.getString(song, "id");
        String albumId = JsonHelper.getString(song, "albumId");
        if (albumId.length() == 0) {
            albumId = JsonHelper.getString(song, "parent");
        }
        String coverArt = JsonHelper.getString(song, "coverArt");
        String cover = coverArt.length() > 0 ? client.coverUrl(coverArt) : fallbackCover;
        Track track = new Track(
                id,
                albumId,
                JsonHelper.getString(song, "title"),
                JsonHelper.getInt(song, "duration", 0),
                "",
                JsonHelper.getInt(song, "track", 0),
                JsonHelper.getString(song, "artist"),
                cover);
        track.streamUrl = client.streamUrl(id);
        return track;
    }

    private static JSONObject root(String response) throws SubsonicClient.SubsonicException {
        try {
            JSONObject root = new JSONObject(response).optJSONObject("subsonic-response");
            if (root == null) {
                throw new SubsonicClient.SubsonicException("Unexpected Subsonic response");
            }
            return root;
        } catch (SubsonicClient.SubsonicException e) {
            throw e;
        } catch (Throwable t) {
            throw new SubsonicClient.SubsonicException("Error parsing server data.");
        }
    }

    private static JSONArray asArray(JSONObject parent, String key) {
        if (parent == null) {
            return null;
        }
        JSONArray array = parent.optJSONArray(key);
        if (array != null) {
            return array;
        }
        JSONObject single = parent.optJSONObject(key);
        if (single != null) {
            array = new JSONArray();
            array.put(single);
        }
        return array;
    }

    private interface Worker<T> {
        T run() throws Exception;
    }

    private <T> void runTask(final Context uiContext, final String message,
                             final Worker<T> worker, final SourceCallback<T> callback) {
        new AsyncTask<Void, Void, T>() {
            private ProgressDialog dialog;
            private String error;

            @Override
            protected void onPreExecute() {
                if (message != null && uiContext != null) {
                    try {
                        dialog = ProgressDialog.show(uiContext, "", message, true);
                    } catch (Throwable t) {
                        dialog = null;
                    }
                }
            }

            @Override
            protected T doInBackground(Void... voids) {
                try {
                    return worker.run();
                } catch (SubsonicClient.SubsonicException e) {
                    error = e.getMessage();
                } catch (IOException e) {
                    error = "Failed to connect. Check server URL and network.";
                } catch (Throwable t) {
                    error = "Error parsing server data.";
                }
                return null;
            }

            @Override
            protected void onPostExecute(T result) {
                if (dialog != null) {
                    try {
                        dialog.dismiss();
                    } catch (Throwable t) {
                        // Ignore window errors
                    }
                }
                if (error != null) {
                    callback.onError(error);
                } else if (result == null) {
                    callback.onError("Subsonic returned no data.");
                } else {
                    callback.onSuccess(result);
                }
            }
        }.execute();
    }
}
