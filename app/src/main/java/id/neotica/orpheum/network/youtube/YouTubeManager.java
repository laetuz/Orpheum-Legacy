package id.neotica.orpheum.network.youtube;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.JsonHelper;

public final class YouTubeManager {

    private static final int TYPE_INVIDIOUS = 1;
    private static final int TYPE_PIPED = 2;
    private static final int RESULT_LIMIT = 20;
    private static final long AUDIO_CACHE_MS = 20L * 60L * 1000L;

    private static YouTubeManager instance;

    private final YouTubeConfig config;
    private final Object lock = new Object();
    private final ArrayList<Instance> instances = new ArrayList<Instance>();
    private final HashMap<String, CachedAudio> audioCache = new HashMap<String, CachedAudio>();
    private final Random random = new Random();

    private boolean loaded;

    private YouTubeManager(Context context) {
        this.config = new YouTubeConfig(context);
    }

    public static synchronized YouTubeManager get(Context context) {
        if (instance == null) {
            instance = new YouTubeManager(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }

    public String getListUrl() {
        return config.getListUrl();
    }

    public String getInstanceSummary() {
        synchronized (lock) {
            ArrayList<Instance> list = new ArrayList<Instance>(instances);
            if (list.isEmpty()) {
                list = parseInstances(config.getCachedInstances());
            }
            int invidious = 0;
            int piped = 0;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).type == TYPE_INVIDIOUS) {
                    invidious++;
                } else {
                    piped++;
                }
            }
            if (invidious == 0 && piped == 0) {
                return "Not loaded";
            }
            return invidious + " Invidious, " + piped + " Piped";
        }
    }

    public List<Track> search(String query) throws IOException {
        ensureInstances();
        ArrayList<Instance> pool = snapshot();
        Collections.shuffle(pool, random);

        boolean responded = false;
        IOException lastError = null;
        for (int i = 0; i < pool.size(); i++) {
            Instance current = pool.get(i);
            try {
                List<Track> tracks = current.type == TYPE_INVIDIOUS
                        ? searchInvidious(current, query)
                        : searchPiped(current, query);
                responded = true;
                if (!tracks.isEmpty()) {
                    return tracks;
                }
            } catch (Throwable t) {
                if (isDeadError(t)) {
                    removeInstance(current);
                }
                lastError = t instanceof IOException
                        ? (IOException) t
                        : new IOException("YouTube search failed");
            }
        }
        if (responded) {
            return new ArrayList<Track>();
        }
        throw lastError != null ? lastError : new IOException("YouTube search failed");
    }

    public String resolveAudio(Track track) throws IOException {
        if (track == null || track.sourceId == null || track.sourceId.length() == 0) {
            throw new IOException("No YouTube video id");
        }
        synchronized (lock) {
            CachedAudio cached = audioCache.get(track.sourceId);
            if (cached != null && System.currentTimeMillis() - cached.at < AUDIO_CACHE_MS) {
                return cached.url;
            }
        }

        ensureInstances();
        ArrayList<Instance> pool = snapshot();
        Collections.shuffle(pool, random);

        IOException lastError = null;
        for (int i = 0; i < pool.size(); i++) {
            Instance current = pool.get(i);
            try {
                String url = current.type == TYPE_INVIDIOUS
                        ? audioInvidious(current, track.sourceId)
                        : audioPiped(current, track.sourceId);
                if (url != null && url.length() > 0) {
                    synchronized (lock) {
                        CachedAudio cached = new CachedAudio();
                        cached.url = url;
                        cached.at = System.currentTimeMillis();
                        audioCache.put(track.sourceId, cached);
                    }
                    return url;
                }
            } catch (Throwable t) {
                if (isDeadError(t)) {
                    removeInstance(current);
                }
                lastError = t instanceof IOException
                        ? (IOException) t
                        : new IOException("YouTube audio unavailable");
            }
        }
        throw lastError != null ? lastError : new IOException("No compatible audio stream");
    }

    private void ensureInstances() throws IOException {
        synchronized (lock) {
            if (loaded && !instances.isEmpty()) {
                return;
            }

            ArrayList<Instance> cached = parseInstances(config.getCachedInstances());
            if (!cached.isEmpty() && !config.isCacheStale()) {
                instances.clear();
                instances.addAll(cached);
                loaded = true;
                return;
            }

            IOException lastError = null;
            try {
                String raw = YouTubeHttp.get(config.getListUrl(), 15000);
                ArrayList<Instance> fresh = parseInstances(raw);
                if (fresh.isEmpty()) {
                    throw new IOException("No YouTube instances in list");
                }
                config.saveInstances(raw);
                instances.clear();
                instances.addAll(fresh);
                loaded = true;
                return;
            } catch (IOException e) {
                lastError = e;
            }

            if (!cached.isEmpty()) {
                instances.clear();
                instances.addAll(cached);
                loaded = true;
                return;
            }
            throw new IOException("YouTube instances unavailable. Check Settings."
                    + (lastError != null && lastError.getMessage() != null
                    ? " " + lastError.getMessage() : ""));
        }
    }

    private ArrayList<Instance> parseInstances(String raw) {
        ArrayList<Instance> result = new ArrayList<Instance>();
        if (raw == null || raw.length() == 0) {
            return result;
        }
        try {
            JSONObject root = new JSONObject(raw);
            HashSet<String> seen = new HashSet<String>();

            JSONArray invidious = root.optJSONArray("invidious");
            if (invidious != null) {
                for (int i = 0; i < invidious.length(); i++) {
                    String base = YouTubeUrlHelper.normalizeBase(invidious.optString(i));
                    if (base.length() > 0 && seen.add("i:" + base)) {
                        result.add(new Instance(TYPE_INVIDIOUS, base, base));
                    }
                }
            }

            JSONArray piped = root.optJSONArray("piped");
            if (piped != null) {
                for (int i = 0; i < piped.length(); i++) {
                    String entry = piped.optString(i);
                    String[] parts = entry.split(",", 2);
                    String base = YouTubeUrlHelper.normalizeBase(parts[0]);
                    String proxy = parts.length > 1
                            ? YouTubeUrlHelper.normalizeBase(parts[1])
                            : base;
                    if (proxy.length() == 0) {
                        proxy = base;
                    }
                    if (base.length() > 0 && seen.add("p:" + base)) {
                        result.add(new Instance(TYPE_PIPED, base, proxy));
                    }
                }
            }
        } catch (Throwable t) {
            // Invalid instance list; callers decide whether to fall back
        }
        return result;
    }

    private ArrayList<Instance> snapshot() {
        synchronized (lock) {
            return new ArrayList<Instance>(instances);
        }
    }

    private void removeInstance(Instance instance) {
        synchronized (lock) {
            instances.remove(instance);
        }
    }

    private boolean isDeadError(Throwable t) {
        if (t instanceof UnknownHostException
                || t instanceof ConnectException
                || t instanceof SocketTimeoutException) {
            return true;
        }
        String message = t != null ? t.getMessage() : null;
        if (message == null) {
            return false;
        }
        return message.indexOf("HTTP 5") >= 0
                || message.indexOf("HTTP 429") >= 0
                || message.indexOf("timed out") >= 0
                || message.indexOf("Connection refused") >= 0;
    }

    private List<Track> searchInvidious(Instance instance, String query) throws Exception {
        String url = instance.base + "/api/v1/search?q="
                + YouTubeUrlHelper.encode(query) + "&type=video";
        JSONArray array = new JSONArray(YouTubeHttp.get(url, 20000));
        List<Track> tracks = new ArrayList<Track>();
        for (int i = 0; i < array.length() && tracks.size() < RESULT_LIMIT; i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null || !"video".equals(JsonHelper.getString(item, "type"))) {
                continue;
            }
            if (item.optBoolean("liveNow") || item.optBoolean("isUpcoming")) {
                continue;
            }
            String videoId = JsonHelper.getString(item, "videoId");
            int duration = JsonHelper.getInt(item, "lengthSeconds", 0);
            if (videoId.length() == 0 || duration <= 0) {
                continue;
            }
            String title = JsonHelper.getString(item, "title");
            String artist = JsonHelper.getString(item, "author");
            String cover = "";
            JSONArray thumbnails = item.optJSONArray("videoThumbnails");
            if (thumbnails != null && thumbnails.length() > 0) {
                JSONObject thumb = thumbnails.optJSONObject(
                        Math.min(4, thumbnails.length() - 1));
                cover = YouTubeUrlHelper.mapThrough(instance.base,
                        JsonHelper.getString(thumb, "url"));
            }
            if (cover.length() == 0) {
                cover = instance.base + "/vi/" + videoId + "/mqdefault.jpg";
            }
            tracks.add(createTrack(videoId, title, artist, duration, cover,
                    tracks.size() + 1));
        }
        return tracks;
    }

    private List<Track> searchPiped(Instance instance, String query) throws Exception {
        String url = instance.base + "/search?q="
                + YouTubeUrlHelper.encode(query) + "&filter=videos";
        JSONObject root = new JSONObject(YouTubeHttp.get(url, 20000));
        JSONArray items = root.optJSONArray("items");
        List<Track> tracks = new ArrayList<Track>();
        if (items == null) {
            String error = JsonHelper.getString(root, "error");
            if (error.length() > 0) {
                throw new IOException(error);
            }
            return tracks;
        }
        for (int i = 0; i < items.length() && tracks.size() < RESULT_LIMIT; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String videoId = YouTubeUrlHelper.videoIdFromUrl(
                    JsonHelper.getString(item, "url"));
            int duration = JsonHelper.getInt(item, "duration", 0);
            if (videoId.length() == 0 || duration <= 0) {
                continue;
            }
            String cover = instance.proxy + "/vi/" + videoId
                    + "/mqdefault.jpg?host=i.ytimg.com&rewrite=false";
            tracks.add(createTrack(
                    videoId,
                    JsonHelper.getString(item, "title"),
                    JsonHelper.getString(item, "uploaderName"),
                    duration,
                    cover,
                    tracks.size() + 1));
        }
        return tracks;
    }

    private String audioInvidious(Instance instance, String videoId) throws Exception {
        String url = instance.base + "/api/v1/videos/"
                + YouTubeUrlHelper.encode(videoId) + "?local=true";
        JSONObject root = new JSONObject(YouTubeHttp.get(url, 20000));
        JSONArray formats = root.optJSONArray("adaptiveFormats");
        if (formats == null || formats.length() == 0) {
            throw new IOException("No compatible audio stream");
        }
        String[] mimeTypes = new String[formats.length()];
        String[] codecs = new String[formats.length()];
        int[] bitrates = new int[formats.length()];
        String[] urls = new String[formats.length()];
        for (int i = 0; i < formats.length(); i++) {
            JSONObject format = formats.optJSONObject(i);
            if (format == null) {
                continue;
            }
            String type = JsonHelper.getString(format, "type");
            mimeTypes[i] = type;
            codecs[i] = type;
            bitrates[i] = parseInt(JsonHelper.getString(format, "bitrate"));
            urls[i] = JsonHelper.getString(format, "url");
            if (urls[i].length() == 0) {
                mimeTypes[i] = "";
                codecs[i] = "";
                bitrates[i] = -1;
            }
        }
        int index = YouTubeAudioSelector.chooseBest(mimeTypes, null, codecs, bitrates);
        if (index < 0 || urls[index].length() == 0) {
            throw new IOException("No compatible audio stream");
        }
        return YouTubeUrlHelper.mapThrough(instance.base, urls[index]);
    }

    private String audioPiped(Instance instance, String videoId) throws Exception {
        String url = instance.base + "/streams/" + YouTubeUrlHelper.encode(videoId);
        JSONObject root = new JSONObject(YouTubeHttp.get(url, 20000));
        JSONArray streams = root.optJSONArray("audioStreams");
        if (streams == null || streams.length() == 0) {
            String error = JsonHelper.getString(root, "error");
            throw new IOException(error.length() > 0 ? error : "No compatible audio stream");
        }
        String[] mimeTypes = new String[streams.length()];
        String[] formats = new String[streams.length()];
        String[] codecs = new String[streams.length()];
        int[] bitrates = new int[streams.length()];
        String[] urls = new String[streams.length()];
        for (int i = 0; i < streams.length(); i++) {
            JSONObject stream = streams.optJSONObject(i);
            if (stream == null) {
                continue;
            }
            mimeTypes[i] = JsonHelper.getString(stream, "mimeType");
            formats[i] = JsonHelper.getString(stream, "format");
            codecs[i] = JsonHelper.getString(stream, "codec");
            bitrates[i] = JsonHelper.getInt(stream, "bitrate", 0);
            urls[i] = JsonHelper.getString(stream, "url");
            if (urls[i].length() == 0) {
                mimeTypes[i] = "";
                formats[i] = "";
                codecs[i] = "";
                bitrates[i] = -1;
            }
        }
        int index = YouTubeAudioSelector.chooseBest(mimeTypes, formats, codecs, bitrates);
        if (index < 0 || urls[index].length() == 0) {
            throw new IOException("No compatible audio stream");
        }
        return YouTubeUrlHelper.mapThrough(instance.proxy, urls[index]);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static Track createTrack(String videoId, String title, String artist,
                                     int duration, String cover, int number) {
        Track track = new Track("yt:" + videoId, "", title, duration, "", number,
                artist, cover);
        track.source = "youtube";
        track.sourceId = videoId;
        return track;
    }

    private static class Instance {
        final int type;
        final String base;
        final String proxy;

        Instance(int type, String base, String proxy) {
            this.type = type;
            this.base = base;
            this.proxy = proxy;
        }
    }

    private static class CachedAudio {
        String url;
        long at;
    }
}
