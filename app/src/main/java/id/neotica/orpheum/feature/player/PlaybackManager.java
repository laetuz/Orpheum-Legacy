package id.neotica.orpheum.feature.player;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import id.neotica.orpheum.OrpheumApp;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.player.service.AudioService;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.Constants;

/**
 * Static playback facade: owns the MediaPlayer, the queue and the listener list.
 * The foreground {@link AudioService} observes this state for its notification.
 */
public class PlaybackManager {

    private static MediaPlayer mediaPlayer;
    private static ArrayList<Track> queue;
    private static int currentIndex;
    private static boolean isPrepared;
    private static int resolveGeneration;
    private static StreamUrlResolver streamUrlResolver;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final CopyOnWriteArrayList<PlaybackListener> listeners =
            new CopyOnWriteArrayList<PlaybackListener>();

    public interface PlaybackListener {
        void onTrackChanged();
        void onPlaybackStateChanged();
        void onPrepared();
        void onCompletion();
        void onError(String error);
    }

    public interface StreamUrlResolver {
        String resolve(Track track) throws Exception;

        boolean onPlaybackError(Track track, int what, int extra);
    }

    public static void setStreamUrlResolver(StreamUrlResolver resolver) {
        streamUrlResolver = resolver;
    }

    public static void init(ArrayList<Track> tracks, int index) {
        if (tracks == null || tracks.isEmpty() || index < 0 || index >= tracks.size()) {
            return;
        }

        queue = new ArrayList<Track>(tracks);
        currentIndex = index;
        isPrepared = false;
        resolveGeneration++;

        releaseMediaPlayer();
        playTrack();
        startService();
    }

    private static void playTrack() {
        if (queue == null || currentIndex < 0 || currentIndex >= queue.size()) {
            return;
        }

        final Track track = queue.get(currentIndex);
        if (track == null || TextUtils.isEmpty(track.id)) {
            return;
        }

        releaseMediaPlayer();

        boolean youtube = "youtube".equals(track.source);
        String source = track.localPath;
        if (!TextUtils.isEmpty(source) && !new File(source).exists()) {
            source = null;
        }
        if (TextUtils.isEmpty(source)) {
            source = track.streamUrl;
        }
        if (TextUtils.isEmpty(source) && !youtube) {
            source = DownloadStore.findTrackFile(track.id);
        }
        if (TextUtils.isEmpty(source) && !youtube) {
            source = Constants.ORPHEUM_BASE + "/stream/" + track.id;
        }
        if (TextUtils.isEmpty(source)) {
            if (youtube) {
                resolveAndPlay(track);
            } else {
                notifyError("Could not prepare audio");
                notifyPlaybackStateChanged();
            }
            return;
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(source);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isPrepared = true;
                    if (!mp.isPlaying()) {
                        mp.start();
                    }
                    notifyPrepared();
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    notifyCompletion();
                    if (queue != null && currentIndex + 1 < queue.size()) {
                        currentIndex++;
                        resolveGeneration++;
                        playTrack();
                    } else {
                        notifyPlaybackStateChanged();
                    }
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    isPrepared = false;
                    if ("youtube".equals(track.source)) {
                        track.streamUrl = null;
                        if (streamUrlResolver != null
                                && streamUrlResolver.onPlaybackError(track, what, extra)) {
                            resolveAndPlay(track);
                            return true;
                        }
                    }
                    notifyError("Error playing track");
                    notifyPlaybackStateChanged();
                    return true;
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            notifyError("Error loading track");
        }

        notifyTrackChanged();
    }

    private static void resolveAndPlay(final Track track) {
        final StreamUrlResolver resolver = streamUrlResolver;
        if (resolver == null) {
            notifyError("Could not prepare audio");
            notifyPlaybackStateChanged();
            return;
        }

        final int generation = resolveGeneration;
        final int index = currentIndex;
        notifyTrackChanged();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String resolved = null;
                String error = null;
                try {
                    resolved = resolver.resolve(track);
                } catch (Throwable t) {
                    error = t.getMessage();
                }
                final String url = resolved;
                final String failure = error;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != resolveGeneration || queue == null
                                || currentIndex != index || queue.get(index) != track) {
                            return;
                        }
                        if (!TextUtils.isEmpty(url)) {
                            track.streamUrl = url;
                            playTrack();
                        } else {
                            isPrepared = false;
                            notifyError(failure != null && failure.length() > 0
                                    ? failure
                                    : "Could not prepare audio");
                            notifyPlaybackStateChanged();
                        }
                    }
                });
            }
        }).start();
    }

    public static void toggle() {
        if (mediaPlayer == null || !isPrepared) {
            return;
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        notifyPlaybackStateChanged();
    }

    public static boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public static void next() {
        if (queue == null || currentIndex + 1 >= queue.size()) {
            return;
        }
        currentIndex++;
        resolveGeneration++;
        playTrack();
    }

    public static void previous() {
        if (queue == null) {
            return;
        }
        if (currentIndex - 1 >= 0) {
            currentIndex--;
            resolveGeneration++;
            playTrack();
        } else if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(0);
        }
    }

    public static void seekTo(int percent) {
        if (mediaPlayer != null && isPrepared) {
            int pos = (int) ((long) percent * mediaPlayer.getDuration() / 100);
            mediaPlayer.seekTo(pos);
        }
    }

    public static int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public static int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public static boolean hasTrack() {
        return queue != null && !queue.isEmpty() && currentIndex >= 0 && currentIndex < queue.size();
    }

    public static Track getCurrentTrack() {
        if (hasTrack()) {
            return queue.get(currentIndex);
        }
        return null;
    }

    public static ArrayList<Track> getQueue() {
        if (queue == null) {
            return new ArrayList<Track>();
        }
        return new ArrayList<Track>(queue);
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static void playAt(int index) {
        if (queue == null || index < 0 || index >= queue.size()) {
            return;
        }
        currentIndex = index;
        resolveGeneration++;
        playTrack();
    }

    public static boolean isPrepared() {
        return isPrepared;
    }

    public static void addListener(PlaybackListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    public static void release() {
        resolveGeneration++;
        releaseMediaPlayer();
        queue = null;
        currentIndex = 0;
        stopService();
    }

    private static void notifyTrackChanged() {
        for (PlaybackListener listener : listeners) {
            try {
                listener.onTrackChanged();
            } catch (Throwable t) {
                // Ignore listener failures
            }
        }
    }

    private static void notifyPlaybackStateChanged() {
        for (PlaybackListener listener : listeners) {
            try {
                listener.onPlaybackStateChanged();
            } catch (Throwable t) {
                // Ignore listener failures
            }
        }
    }

    private static void notifyPrepared() {
        for (PlaybackListener listener : listeners) {
            try {
                listener.onPrepared();
            } catch (Throwable t) {
                // Ignore listener failures
            }
        }
    }

    private static void notifyCompletion() {
        for (PlaybackListener listener : listeners) {
            try {
                listener.onCompletion();
            } catch (Throwable t) {
                // Ignore listener failures
            }
        }
    }

    private static void notifyError(String error) {
        for (PlaybackListener listener : listeners) {
            try {
                listener.onError(error);
            } catch (Throwable t) {
                // Ignore listener failures
            }
        }
    }

    private static void startService() {
        try {
            Context ctx = OrpheumApp.getInstance();
            if (ctx != null) {
                ctx.startService(new Intent(ctx, AudioService.class));
            }
        } catch (Throwable t) {
            // Ignore service start errors
        }
    }

    private static void stopService() {
        try {
            Context ctx = OrpheumApp.getInstance();
            if (ctx != null) {
                ctx.stopService(new Intent(ctx, AudioService.class));
            }
        } catch (Throwable t) {
            // Ignore service stop errors
        }
    }

    private static void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Throwable t) {
                // Ignore release errors
            }
            mediaPlayer = null;
            isPrepared = false;
        }
    }
}
