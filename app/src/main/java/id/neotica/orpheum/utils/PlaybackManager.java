package id.neotica.orpheum.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;

import id.neotica.orpheum.BuildConfig;
import id.neotica.orpheum.model.Track;

public class PlaybackManager {

    private static MediaPlayer mediaPlayer;
    private static ArrayList<Track> queue;
    private static int currentIndex;
    private static boolean isPrepared;
    private static PlaybackListener listener;

    public interface PlaybackListener {
        void onTrackChanged();
        void onPrepared();
        void onCompletion();
        void onError(String error);
    }

    public static void init(ArrayList<Track> tracks, int index) {
        if (tracks == null || tracks.isEmpty() || index < 0 || index >= tracks.size()) return;

        queue = new ArrayList<Track>(tracks);
        currentIndex = index;
        isPrepared = false;

        releaseMediaPlayer();
        playTrack();
    }

    private static void playTrack() {
        if (queue == null || currentIndex < 0 || currentIndex >= queue.size()) return;

        Track track = queue.get(currentIndex);
        if (track == null || TextUtils.isEmpty(track.id)) return;

        releaseMediaPlayer();

        String url = BuildConfig.BASE_URL + "/orpheum/stream/" + track.id;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isPrepared = true;
                    if (!mp.isPlaying()) {
                        mp.start();
                    }
                    if (listener != null) listener.onPrepared();
                }
            });

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    next();
                    if (listener != null) listener.onCompletion();
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    isPrepared = false;
                    if (listener != null) listener.onError("Error playing track");
                    return true;
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            if (listener != null) listener.onError("Error loading track");
        }

        if (listener != null) listener.onTrackChanged();
    }

    public static void toggle() {
        if (mediaPlayer == null || !isPrepared) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    public static boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public static void next() {
        if (queue == null || currentIndex + 1 >= queue.size()) return;
        currentIndex++;
        playTrack();
    }

    public static void previous() {
        if (queue == null) return;
        if (currentIndex - 1 >= 0) {
            currentIndex--;
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

    public static boolean isPrepared() {
        return isPrepared;
    }

    public static void setListener(PlaybackListener l) {
        listener = l;
    }

    public static void removeListener() {
        listener = null;
    }

    public static void release() {
        releaseMediaPlayer();
        queue = null;
        currentIndex = 0;
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
