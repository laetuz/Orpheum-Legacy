package id.neotica.orpheum.feature.downloads;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.neotica.orpheum.OrpheumApp;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * SharedPreferences-backed index of downloaded albums plus the filesystem layout
 * ({@code <external>/Orpheum/downloads/<albumId>/<trackId>.mp3}, internal fallback).
 */
public class DownloadStore {

    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_DOWNLOADING = "DOWNLOADING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private static final String PREFS_NAME = "OrpheumDownloads";
    private static final String KEY_ALBUMS = "albums";
    private static final String EXTERNAL_DIR = "Orpheum/downloads";
    private static final Object LOCK = new Object();

    private static Map<String, String> trackFileCache;

    private DownloadStore() { }

    private static final Comparator<DownloadedAlbum> BY_ADDED_DESC = new Comparator<DownloadedAlbum>() {
        @Override
        public int compare(DownloadedAlbum a, DownloadedAlbum b) {
            if (a.addedAt == b.addedAt) {
                return 0;
            }
            return a.addedAt > b.addedAt ? -1 : 1;
        }
    };

    public static File getDownloadRoot() {
        Context context = OrpheumApp.getInstance();
        File root = null;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File candidate = new File(Environment.getExternalStorageDirectory(), EXTERNAL_DIR);
                if ((candidate.exists() || candidate.mkdirs()) && candidate.canWrite()) {
                    root = candidate;
                }
            }
        } catch (Throwable t) {
            root = null;
        }
        if (root == null && context != null) {
            root = new File(context.getFilesDir(), "downloads");
            if (!root.exists()) {
                root.mkdirs();
            }
        }
        return root;
    }

    public static File getAlbumDir(String albumId) {
        return new File(getDownloadRoot(), albumId);
    }

    public static File getTrackFile(String albumId, String trackId) {
        return new File(getAlbumDir(albumId), trackId + ".mp3");
    }

    public static File getCoverFile(String albumId) {
        return new File(getAlbumDir(albumId), "cover.img");
    }

    public static List<DownloadedAlbum> getAll() {
        synchronized (LOCK) {
            return loadAllLocked();
        }
    }

    public static int getCompletedCount() {
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            int count = 0;
            for (int i = 0; i < all.size(); i++) {
                if (STATUS_COMPLETED.equals(all.get(i).status)) {
                    count++;
                }
            }
            return count;
        }
    }

    public static long getStorageBytes() {
        synchronized (LOCK) {
            return sizeOf(getDownloadRoot());
        }
    }

    public static DownloadedAlbum getAlbum(String albumId) {
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id.equals(albumId)) {
                    return all.get(i);
                }
            }
            return null;
        }
    }

    public static void enqueue(Album album, List<Track> tracks) {
        if (album == null || album.id == null) {
            return;
        }
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            DownloadedAlbum entry = null;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id.equals(album.id)) {
                    entry = all.get(i);
                    break;
                }
            }
            if (entry == null) {
                entry = new DownloadedAlbum();
                entry.id = album.id;
                entry.addedAt = System.currentTimeMillis();
                all.add(entry);
            }
            entry.title = album.title;
            entry.artistName = album.artistName;
            entry.releaseYear = album.releaseYear;
            entry.coverUrl = album.coverUrl;
            entry.status = STATUS_QUEUED;
            entry.done = 0;
            entry.total = tracks != null ? tracks.size() : 0;
            entry.totalBytes = 0;
            entry.tracks = new ArrayList<Track>();
            if (tracks != null) {
                for (int i = 0; i < tracks.size(); i++) {
                    entry.tracks.add(copyMetadata(tracks.get(i)));
                }
            }
            saveAllLocked(all);
        }
    }

    public static void setStatus(String albumId, String status, int done, int total) {
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            DownloadedAlbum entry = find(all, albumId);
            if (entry == null) {
                return;
            }
            entry.status = status;
            entry.done = done;
            entry.total = total;
            saveAllLocked(all);
        }
    }

    public static void markCompleted(String albumId, long totalBytes) {
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            DownloadedAlbum entry = find(all, albumId);
            if (entry == null) {
                return;
            }
            entry.status = STATUS_COMPLETED;
            entry.done = entry.total;
            entry.totalBytes = totalBytes;
            saveAllLocked(all);
        }
    }

    public static void markFailed(String albumId) {
        synchronized (LOCK) {
            deleteRecursive(getAlbumDir(albumId));
            List<DownloadedAlbum> all = loadAllLocked();
            DownloadedAlbum entry = find(all, albumId);
            if (entry == null) {
                return;
            }
            entry.status = STATUS_FAILED;
            entry.done = 0;
            entry.totalBytes = 0;
            saveAllLocked(all);
        }
    }

    public static void remove(String albumId) {
        synchronized (LOCK) {
            deleteRecursive(getAlbumDir(albumId));
            List<DownloadedAlbum> all = loadAllLocked();
            for (int i = all.size() - 1; i >= 0; i--) {
                if (all.get(i).id.equals(albumId)) {
                    all.remove(i);
                }
            }
            saveAllLocked(all);
        }
    }

    public static DownloadedAlbum getNextQueued() {
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            DownloadedAlbum next = null;
            for (int i = 0; i < all.size(); i++) {
                DownloadedAlbum candidate = all.get(i);
                if (STATUS_QUEUED.equals(candidate.status)
                        && (next == null || candidate.addedAt < next.addedAt)) {
                    next = candidate;
                }
            }
            return next;
        }
    }

    public static String findTrackFile(String trackId) {
        synchronized (LOCK) {
            if (trackFileCache == null) {
                buildTrackFileCacheLocked();
            }
            return trackFileCache.get(trackId);
        }
    }

    public static List<Track> getLocalTracks(String albumId) {
        synchronized (LOCK) {
            DownloadedAlbum entry = getAlbum(albumId);
            List<Track> result = new ArrayList<Track>();
            if (entry == null || !STATUS_COMPLETED.equals(entry.status)) {
                return result;
            }
            File cover = getCoverFile(albumId);
            String coverUri = cover.exists() ? "file://" + cover.getAbsolutePath() : null;
            for (int i = 0; i < entry.tracks.size(); i++) {
                Track meta = entry.tracks.get(i);
                File file = getTrackFile(albumId, meta.id);
                if (!file.exists()) {
                    continue;
                }
                Track track = new Track(meta.id, meta.albumId, meta.title, meta.durationSeconds,
                        meta.fileUrl, meta.trackNumber, meta.artistName, coverUri);
                track.localPath = file.getAbsolutePath();
                result.add(track);
            }
            return result;
        }
    }

    public static Album getLocalAlbum(String albumId) {
        synchronized (LOCK) {
            DownloadedAlbum entry = getAlbum(albumId);
            if (entry == null) {
                return null;
            }
            File cover = getCoverFile(albumId);
            String coverUri = cover.exists() ? "file://" + cover.getAbsolutePath() : null;
            return new Album(entry.id, "", entry.title, entry.releaseYear, coverUri, entry.artistName, "");
        }
    }

    public static void recoverInterrupted() {
        synchronized (LOCK) {
            List<DownloadedAlbum> all = loadAllLocked();
            boolean changed = false;
            for (int i = 0; i < all.size(); i++) {
                DownloadedAlbum entry = all.get(i);
                if (STATUS_DOWNLOADING.equals(entry.status) || STATUS_QUEUED.equals(entry.status)) {
                    deleteRecursive(getAlbumDir(entry.id));
                    entry.status = STATUS_FAILED;
                    entry.done = 0;
                    entry.totalBytes = 0;
                    changed = true;
                }
            }
            if (changed) {
                saveAllLocked(all);
            }
        }
    }

    private static DownloadedAlbum find(List<DownloadedAlbum> all, String albumId) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(albumId)) {
                return all.get(i);
            }
        }
        return null;
    }

    private static List<DownloadedAlbum> loadAllLocked() {
        List<DownloadedAlbum> result = new ArrayList<DownloadedAlbum>();
        JSONArray array = loadArray();
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            DownloadedAlbum entry = albumFromJson(array.optJSONObject(i));
            if (entry == null) {
                changed = true;
                continue;
            }
            if (STATUS_COMPLETED.equals(entry.status) && !allTracksExist(entry)) {
                deleteRecursive(getAlbumDir(entry.id));
                changed = true;
                continue;
            }
            result.add(entry);
        }
        if (changed) {
            saveAllLocked(result);
        }
        Collections.sort(result, BY_ADDED_DESC);
        return result;
    }

    private static boolean allTracksExist(DownloadedAlbum entry) {
        if (entry.tracks == null || entry.tracks.isEmpty()) {
            return false;
        }
        for (int i = 0; i < entry.tracks.size(); i++) {
            if (!getTrackFile(entry.id, entry.tracks.get(i).id).exists()) {
                return false;
            }
        }
        return true;
    }

    private static void buildTrackFileCacheLocked() {
        Map<String, String> map = new HashMap<String, String>();
        List<DownloadedAlbum> all = loadAllLocked();
        for (int i = 0; i < all.size(); i++) {
            DownloadedAlbum entry = all.get(i);
            if (!STATUS_COMPLETED.equals(entry.status)) {
                continue;
            }
            for (int j = 0; j < entry.tracks.size(); j++) {
                Track track = entry.tracks.get(j);
                File file = getTrackFile(entry.id, track.id);
                if (file.exists()) {
                    map.put(track.id, file.getAbsolutePath());
                }
            }
        }
        trackFileCache = map;
    }

    private static SharedPreferences prefs() {
        return OrpheumApp.getInstance().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static JSONArray loadArray() {
        String raw = prefs().getString(KEY_ALBUMS, null);
        if (raw == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (Throwable t) {
            return new JSONArray();
        }
    }

    private static void saveAllLocked(List<DownloadedAlbum> albums) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < albums.size(); i++) {
            array.put(albumToJson(albums.get(i)));
        }
        prefs().edit().putString(KEY_ALBUMS, array.toString()).commit();
        trackFileCache = null;
    }

    private static JSONObject albumToJson(DownloadedAlbum entry) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", entry.id);
            obj.put("title", entry.title);
            obj.put("artist_name", entry.artistName);
            obj.put("release_year", entry.releaseYear);
            obj.put("cover_url", entry.coverUrl);
            obj.put("status", entry.status);
            obj.put("done", entry.done);
            obj.put("total", entry.total);
            obj.put("total_bytes", entry.totalBytes);
            obj.put("added_at", entry.addedAt);

            JSONArray tracks = new JSONArray();
            for (int i = 0; i < entry.tracks.size(); i++) {
                Track track = entry.tracks.get(i);
                JSONObject trackObj = new JSONObject();
                trackObj.put("id", track.id);
                trackObj.put("album_id", track.albumId);
                trackObj.put("title", track.title);
                trackObj.put("duration_seconds", track.durationSeconds);
                trackObj.put("file_url", track.fileUrl);
                trackObj.put("track_number", track.trackNumber);
                trackObj.put("artist_name", track.artistName);
                tracks.put(trackObj);
            }
            obj.put("tracks", tracks);
        } catch (Throwable t) {
            // Keep whatever was written
        }
        return obj;
    }

    private static DownloadedAlbum albumFromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        String id = JsonHelper.getString(obj, "id");
        if (id.length() == 0) {
            return null;
        }
        DownloadedAlbum entry = new DownloadedAlbum();
        entry.id = id;
        entry.title = JsonHelper.getString(obj, "title");
        entry.artistName = JsonHelper.getString(obj, "artist_name");
        entry.releaseYear = JsonHelper.getInt(obj, "release_year", 0);
        entry.coverUrl = JsonHelper.getString(obj, "cover_url");
        entry.status = JsonHelper.getString(obj, "status");
        entry.done = JsonHelper.getInt(obj, "done", 0);
        entry.total = JsonHelper.getInt(obj, "total", 0);
        entry.totalBytes = JsonHelper.getLong(obj, "total_bytes", 0);
        entry.addedAt = JsonHelper.getLong(obj, "added_at", 0);

        JSONArray tracks = obj.optJSONArray("tracks");
        if (tracks != null) {
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject trackObj = tracks.optJSONObject(i);
                if (trackObj == null) {
                    continue;
                }
                entry.tracks.add(new Track(
                        JsonHelper.getString(trackObj, "id"),
                        JsonHelper.getString(trackObj, "album_id"),
                        JsonHelper.getString(trackObj, "title"),
                        JsonHelper.getInt(trackObj, "duration_seconds", 0),
                        JsonHelper.getString(trackObj, "file_url"),
                        JsonHelper.getInt(trackObj, "track_number", 0),
                        JsonHelper.getString(trackObj, "artist_name"),
                        null));
            }
        }
        return entry;
    }

    private static Track copyMetadata(Track source) {
        if (source == null) {
            return null;
        }
        return new Track(source.id, source.albumId, source.title, source.durationSeconds,
                source.fileUrl, source.trackNumber, source.artistName, source.albumCover);
    }

    private static long sizeOf(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return file.length();
        }
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                total += sizeOf(children[i]);
            }
        }
        return total;
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                deleteRecursive(children[i]);
            }
        }
        file.delete();
    }
}
