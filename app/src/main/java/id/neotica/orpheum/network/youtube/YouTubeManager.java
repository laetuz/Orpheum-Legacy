package id.neotica.orpheum.network.youtube;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.JsonHelper;
import id.neotica.orpheum.utils.SdkCompat;

public final class YouTubeManager {

    private static final int TYPE_INVIDIOUS = 1;
    private static final int TYPE_PIPED = 2;
    private static final int RESULT_LIMIT = 20;
    private static final int MAX_DOWNLOAD_BYTES = 80 * 1024 * 1024;
    private static final long MAX_CACHE_BYTES = 100L * 1024L * 1024L;
    private static final int MAX_CACHE_FILES = 20;
    private static final long PREPARED_MAX_AGE_MS = 20L * 60L * 1000L;
    private static final int MAX_RESOLVE_ATTEMPTS = 12;

    private static YouTubeManager instance;

    private final Context context;
    private final YouTubeConfig config;
    private final Object lock = new Object();
    private final ArrayList<Instance> instances = new ArrayList<Instance>();
    private final HashMap<String, ArrayList<Candidate>> candidates =
            new HashMap<String, ArrayList<Candidate>>();
    private final HashMap<String, Integer> cursors = new HashMap<String, Integer>();
    private final HashMap<String, Prepared> prepared = new HashMap<String, Prepared>();
    private final HashMap<String, ArrayList<Candidate>> muxedCandidates =
            new HashMap<String, ArrayList<Candidate>>();
    private final HashMap<String, HashSet<String>> failedInstances =
            new HashMap<String, HashSet<String>>();
    private final Random random = new Random();

    private boolean loaded;

    private YouTubeManager(Context context) {
        this.context = context.getApplicationContext();
        this.config = new YouTubeConfig(this.context);
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
        final String videoId = track.sourceId;

        synchronized (lock) {
            Prepared cached = prepared.get(videoId);
            if (cached != null && System.currentTimeMillis() - cached.at < PREPARED_MAX_AGE_MS
                    && (cached.filePath == null || new File(cached.filePath).exists())) {
                return cached.source;
            }
            prepared.remove(videoId);
        }

        ensureInstances();
        YouTubeDiagnostics.start(videoId, track.title);

        HashSet<String> attempted = new HashSet<String>();
        synchronized (lock) {
            HashSet<String> failed = failedInstances.get(videoId);
            if (failed != null) {
                attempted.addAll(failed);
            }
        }

        IOException lastError = null;
        for (int attempt = 0; attempt < MAX_RESOLVE_ATTEMPTS; attempt++) {
            Candidate candidate = takeCandidate(videoId, attempted);
            if (candidate == null) {
                break;
            }
            try {
                Prepared result = prepare(videoId, candidate);
                synchronized (lock) {
                    result.at = System.currentTimeMillis();
                    prepared.put(videoId, result);
                }
                return result.source;
            } catch (Throwable t) {
                lastError = t instanceof IOException
                        ? (IOException) t
                        : new IOException("No playable audio stream");
                YouTubeDiagnostics.append("Failed " + candidate.instance + " "
                        + candidate.format + " -> " + message(t));
            }
        }
        throw lastError != null
                ? lastError
                : new IOException("No playable YouTube audio for this device");
    }

    public boolean onPlaybackError(Track track) {
        if (track == null || track.sourceId == null) {
            return false;
        }
        final String videoId = track.sourceId;
        synchronized (lock) {
            Prepared current = prepared.remove(videoId);
            if (current != null) {
                if (current.filePath != null) {
                    new File(current.filePath).delete();
                }
                if (current.instanceKey != null) {
                    HashSet<String> failed = failedInstances.get(videoId);
                    if (failed == null) {
                        failed = new HashSet<String>();
                        failedInstances.put(videoId, failed);
                    }
                    failed.add(current.instanceKey);
                }
            }
            ArrayList<Candidate> list = candidates.get(videoId);
            int cursor = cursors.containsKey(videoId) ? cursors.get(videoId) : 0;
            if (list != null && cursor < list.size()) {
                return true;
            }
            for (int i = 0; i < instances.size(); i++) {
                HashSet<String> failed = failedInstances.get(videoId);
                if (failed == null || !failed.contains(instances.get(i).key)) {
                    return true;
                }
            }
            candidates.remove(videoId);
            cursors.remove(videoId);
            muxedCandidates.remove(videoId);
            failedInstances.remove(videoId);
            return false;
        }
    }

    private Candidate takeCandidate(String videoId, HashSet<String> attempted) throws IOException {
        synchronized (lock) {
            ArrayList<Candidate> list = candidates.get(videoId);
            int cursor = cursors.containsKey(videoId) ? cursors.get(videoId) : 0;
            if (list == null || cursor >= list.size()) {
                list = loadCandidatesLocked(videoId, attempted);
                candidates.put(videoId, list);
                cursor = 0;
            }
            if (list.isEmpty() || cursor >= list.size()) {
                return null;
            }
            cursors.put(videoId, cursor + 1);
            return list.get(cursor);
        }
    }

    private ArrayList<Candidate> loadCandidatesLocked(String videoId,
                                                       HashSet<String> attempted) throws IOException {
        ArrayList<Instance> pool = orderedInstances();
        IOException lastError = null;
        for (int i = 0; i < pool.size(); i++) {
            Instance current = pool.get(i);
            if (attempted.contains(current.key)) {
                continue;
            }
            attempted.add(current.key);
            try {
                CandidateSet set = current.type == TYPE_INVIDIOUS
                        ? candidatesInvidious(current, videoId)
                        : candidatesPiped(current, videoId);
                if (!SdkCompat.atLeast(12)) {
                    if (!set.muxed.isEmpty()) {
                        YouTubeDiagnostics.append("Instance " + current.label
                                + ": " + set.muxed.size()
                                + " progressive muxed candidate(s)");
                        return set.muxed;
                    }
                    YouTubeDiagnostics.append("Instance " + current.label
                            + ": no progressive muxed MP4");
                    continue;
                }
                if (SdkCompat.atLeast(12) && !set.muxed.isEmpty()) {
                    ArrayList<Candidate> pending = muxedCandidates.get(videoId);
                    if (pending == null) {
                        pending = new ArrayList<Candidate>();
                        muxedCandidates.put(videoId, pending);
                    }
                    pending.addAll(set.muxed);
                }
                if (!set.audio.isEmpty()) {
                    YouTubeDiagnostics.append("Instance " + current.label
                            + ": " + set.audio.size() + " audio candidate(s)");
                    return set.audio;
                }
                YouTubeDiagnostics.append("Instance " + current.label
                        + ": no audio-only M4A"
                        + (set.muxed.isEmpty() ? "" : " (" + set.muxed.size()
                        + " muxed candidate(s))"));
            } catch (Throwable t) {
                if (isDeadError(t)) {
                    removeInstance(current);
                }
                lastError = t instanceof IOException
                        ? (IOException) t
                        : new IOException("No playable audio stream");
                YouTubeDiagnostics.append("Instance " + current.label + ": " + message(t));
            }
        }

        if (SdkCompat.atLeast(12)) {
            ArrayList<Candidate> pending = muxedCandidates.remove(videoId);
            if (pending != null && !pending.isEmpty()) {
                return pending;
            }
        }
        throw lastError != null
                ? lastError
                : new IOException("No playable audio-only stream for this device");
    }

    private ArrayList<Instance> orderedInstances() {
        ArrayList<Instance> piped = new ArrayList<Instance>();
        ArrayList<Instance> invidious = new ArrayList<Instance>();
        for (int i = 0; i < instances.size(); i++) {
            Instance instance = instances.get(i);
            if (instance.type == TYPE_PIPED) {
                piped.add(instance);
            } else {
                invidious.add(instance);
            }
        }
        Collections.shuffle(piped, random);
        Collections.shuffle(invidious, random);
        piped.addAll(invidious);
        return piped;
    }

    private Prepared prepare(String videoId, Candidate candidate) throws IOException {
        YouTubeDiagnostics.append("Candidate: " + candidate.instance
                + " | " + candidate.format
                + " | " + YouTubeUrlHelper.safeUrl(candidate.url));

        Prepared result = new Prepared();
        result.instanceKey = candidate.instanceKey;

        if (!SdkCompat.atLeast(12)) {
            return downloadCandidate(videoId, candidate, result);
        }

        try {
            YouTubeHttp.ProbeResult probe = YouTubeHttp.probe(candidate.url, 20000);
            result.source = probe.url;
            result.filePath = null;
            YouTubeDiagnostics.append("Stream: HTTP " + probe.status + " "
                    + (probe.contentType == null ? "" : probe.contentType));
            return result;
        } catch (Throwable t) {
            YouTubeDiagnostics.append("Stream probe failed: " + message(t)
                    + " (trying local preparation)");
        }
        return downloadCandidate(videoId, candidate, result);
    }

    private Prepared downloadCandidate(String videoId, Candidate candidate,
                                       Prepared result) throws IOException {
        File dir = new File(context.getCacheDir(), "youtube");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create audio cache");
        }
        String name = videoId + "-" + Integer.toHexString(candidate.url.hashCode())
                + "." + candidate.ext;
        File target = new File(dir, name);
        if (!target.exists() || target.length() == 0) {
            long started = System.currentTimeMillis();
            YouTubeHttp.DownloadResult download = YouTubeHttp.download(
                    candidate.url, target, MAX_DOWNLOAD_BYTES, 30000);
            YouTubeDiagnostics.append("Downloaded " + download.bytes + " bytes ("
                    + (download.contentType == null ? "" : download.contentType)
                    + ") in " + (System.currentTimeMillis() - started) + " ms");
        } else {
            YouTubeDiagnostics.append("Using cached " + target.getName()
                    + " (" + target.length() + " bytes)");
        }
        if (candidate.muxed) {
            try {
                Mp4AudioOnlyStripper.stripVideoTrack(target);
                YouTubeDiagnostics.append("Removed video track; audio-only MP4 ready");
            } catch (Throwable t) {
                target.delete();
                throw new IOException("Cannot extract audio track: " + message(t));
            }
        }
        result.source = target.getAbsolutePath();
        result.filePath = target.getAbsolutePath();
        pruneCache(dir);
        return result;
    }

    private void pruneCache(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                if (a.lastModified() == b.lastModified()) {
                    return 0;
                }
                return a.lastModified() < b.lastModified() ? -1 : 1;
            }
        });
        long total = 0;
        for (int i = 0; i < files.length; i++) {
            total += files[i].length();
        }
        int index = 0;
        while (files.length - index > MAX_CACHE_FILES || total > MAX_CACHE_BYTES) {
            if (index >= files.length) {
                break;
            }
            total -= files[index].length();
            files[index].delete();
            index++;
        }
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

    private CandidateSet candidatesInvidious(Instance instance,
                                             String videoId) throws Exception {
        String url = instance.base + "/api/v1/videos/"
                + YouTubeUrlHelper.encode(videoId) + "?local=true";
        JSONObject root = new JSONObject(YouTubeHttp.get(url, 20000));
        String error = JsonHelper.getString(root, "error");
        if (error.length() > 0) {
            throw new IOException(error);
        }

        CandidateSet result = new CandidateSet();
        HashSet<String> seen = new HashSet<String>();
        JSONArray formats = root.optJSONArray("adaptiveFormats");
        if (formats != null) {
            for (int i = 0; i < formats.length(); i++) {
                JSONObject format = formats.optJSONObject(i);
                if (format == null) {
                    continue;
                }
                String type = JsonHelper.getString(format, "type");
                if (type.indexOf("audio/mp4") < 0 && type.indexOf("mp4a") < 0) {
                    continue;
                }
                String mediaUrl = JsonHelper.getString(format, "url");
                if (mediaUrl.length() == 0) {
                    continue;
                }
                int bitrate = parseInt(JsonHelper.getString(format, "bitrate"));
                addAudioCandidate(result.audio, seen, instance,
                        mediaUrl, instance.base,
                        "audio/mp4 " + bitrate + " bits/s", "m4a", bitrate);
            }
        }
        Collections.sort(result.audio, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.rank == b.rank) {
                    return 0;
                }
                return a.rank > b.rank ? -1 : 1;
            }
        });

        JSONArray streamFormats = root.optJSONArray("formatStreams");
        if (streamFormats != null) {
            for (int i = 0; i < streamFormats.length(); i++) {
                JSONObject format = streamFormats.optJSONObject(i);
                if (format == null) {
                    continue;
                }
                String type = JsonHelper.getString(format, "type");
                if (type.indexOf("mp4a") < 0 && type.indexOf("aac") < 0) {
                    continue;
                }
                String mediaUrl = JsonHelper.getString(format, "url");
                if (mediaUrl.length() == 0) {
                    continue;
                }
                mediaUrl = YouTubeUrlHelper.mapThrough(instance.base, mediaUrl);
                if (mediaUrl.length() == 0 || !seen.add(mediaUrl)) {
                    continue;
                }
                int height = parseHeight(JsonHelper.getString(format, "resolution"));
                String quality = JsonHelper.getString(format, "quality");
                result.muxed.add(new Candidate(
                        mediaUrl,
                        instance.label,
                        instance.key,
                        "muxed " + (quality.length() > 0 ? quality : height + "p"),
                        "mp4",
                        height,
                        true));
            }
        }
        Collections.sort(result.muxed, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.rank == b.rank) {
                    return 0;
                }
                return a.rank < b.rank ? -1 : 1;
            }
        });
        return result;
    }

    private CandidateSet candidatesPiped(Instance instance,
                                         String videoId) throws Exception {
        String url = instance.base + "/streams/" + YouTubeUrlHelper.encode(videoId);
        JSONObject root = new JSONObject(YouTubeHttp.get(url, 20000));
        String error = JsonHelper.getString(root, "error");
        if (error.length() > 0) {
            throw new IOException(error);
        }

        CandidateSet result = new CandidateSet();
        HashSet<String> seen = new HashSet<String>();
        JSONArray streams = root.optJSONArray("audioStreams");
        if (streams != null) {
            for (int i = 0; i < streams.length(); i++) {
                JSONObject stream = streams.optJSONObject(i);
                if (stream == null) {
                    continue;
                }
                String mime = JsonHelper.getString(stream, "mimeType");
                String format = JsonHelper.getString(stream, "format");
                String codec = JsonHelper.getString(stream, "codec");
                if (mime.indexOf("audio/mp4") < 0
                        && !"M4A".equalsIgnoreCase(format)
                        && !codec.startsWith("mp4a")) {
                    continue;
                }
                String mediaUrl = JsonHelper.getString(stream, "url");
                if (mediaUrl.length() == 0) {
                    continue;
                }
                int bitrate = JsonHelper.getInt(stream, "bitrate", 0);
                addAudioCandidate(result.audio, seen, instance,
                        mediaUrl, instance.proxy,
                        "audio/mp4 " + bitrate + " bits/s", "m4a", bitrate);
            }
        }
        Collections.sort(result.audio, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.rank == b.rank) {
                    return 0;
                }
                return a.rank > b.rank ? -1 : 1;
            }
        });

        JSONArray videoStreams = root.optJSONArray("videoStreams");
        if (videoStreams != null) {
            for (int i = 0; i < videoStreams.length(); i++) {
                JSONObject stream = videoStreams.optJSONObject(i);
                if (stream == null || stream.optBoolean("videoOnly")) {
                    continue;
                }
                String mime = JsonHelper.getString(stream, "mimeType");
                if (mime.indexOf("video/mp4") < 0) {
                    continue;
                }
                String mediaUrl = JsonHelper.getString(stream, "url");
                if (mediaUrl.length() == 0) {
                    continue;
                }
                mediaUrl = YouTubeUrlHelper.mapThrough(instance.proxy, mediaUrl);
                if (mediaUrl.length() == 0 || !seen.add(mediaUrl)) {
                    continue;
                }
                int height = JsonHelper.getInt(stream, "height", 0);
                result.muxed.add(new Candidate(
                        mediaUrl,
                        instance.label,
                        instance.key,
                        "muxed mp4 " + height + "p",
                        "mp4",
                        height,
                        true));
            }
        }
        Collections.sort(result.muxed, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate a, Candidate b) {
                if (a.rank == b.rank) {
                    return 0;
                }
                return a.rank < b.rank ? -1 : 1;
            }
        });
        return result;
    }

    private void addAudioCandidate(ArrayList<Candidate> audio, HashSet<String> seen,
                                   Instance instance, String rawUrl, String proxyBase,
                                   String format, String ext, int bitrate) {
        String proxied = YouTubeUrlHelper.mapThrough(proxyBase, rawUrl);
        if (proxied.length() > 0 && seen.add(proxied)) {
            audio.add(new Candidate(proxied, instance.label, instance.key,
                    format, ext, bitrate, false));
        }
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            if (!rawUrl.equals(proxied) && seen.add(rawUrl)) {
                audio.add(new Candidate(rawUrl, instance.label + " direct",
                        instance.key, format + " (direct)", ext, bitrate, false));
            }
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int parseHeight(String resolution) {
        if (resolution == null) {
            return 0;
        }
        int value = 0;
        for (int i = 0; i < resolution.length(); i++) {
            char c = resolution.charAt(i);
            if (c >= '0' && c <= '9') {
                value = value * 10 + (c - '0');
            } else if (value > 0) {
                break;
            }
        }
        return value;
    }

    private static String message(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        String message = t.getMessage();
        return message == null || message.length() == 0
                ? t.getClass().getSimpleName()
                : message;
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
        final String key;
        final String label;

        Instance(int type, String base, String proxy) {
            this.type = type;
            this.base = base;
            this.proxy = proxy;
            this.key = (type == TYPE_INVIDIOUS ? "i:" : "p:") + base;
            this.label = (type == TYPE_INVIDIOUS ? "Invidious " : "Piped ")
                    + base.replace("http://", "").replace("https://", "");
        }
    }

    private static class CandidateSet {
        final ArrayList<Candidate> audio = new ArrayList<Candidate>();
        final ArrayList<Candidate> muxed = new ArrayList<Candidate>();
    }

    private static class Candidate {
        final String url;
        final String instance;
        final String instanceKey;
        final String format;
        final String ext;
        final int rank;
        final boolean muxed;

        Candidate(String url, String instance, String instanceKey,
                  String format, String ext, int rank, boolean muxed) {
            this.url = url;
            this.instance = instance;
            this.instanceKey = instanceKey;
            this.format = format;
            this.ext = ext;
            this.rank = rank;
            this.muxed = muxed;
        }
    }

    private static class Prepared {
        String source;
        String filePath;
        String instanceKey;
        long at;
    }
}
