package id.neotica.orpheum.feature.player.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import java.lang.reflect.Method;

import id.neotica.orpheum.feature.player.PlaybackManager;
import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.SdkCompat;

/**
 * Foreground service keeping playback alive with a notification.
 * Uses reflection so it also runs on API 3-4, where
 * {@code startForeground}/{@code stopForeground} do not exist.
 */
public class AudioService extends Service implements PlaybackManager.PlaybackListener {

    private static final int NOTIFICATION_ID = 1000;

    public static final String ACTION_PLAY_PAUSE = "id.neotica.orpheum.action.PLAY_PAUSE";
    public static final String ACTION_NEXT = "id.neotica.orpheum.action.NEXT";

    private static final Class<?>[] START_FOREGROUND_SIG = new Class[] { int.class, Notification.class };
    private static final Class<?>[] STOP_FOREGROUND_SIG = new Class[] { boolean.class };
    private static final Class<?>[] SET_FOREGROUND_SIG = new Class[] { boolean.class };

    private Method mStartForeground;
    private Method mStopForeground;
    private Method mSetForeground;

    private final Object[] mStartForegroundArgs = new Object[2];
    private final Object[] mStopForegroundArgs = new Object[1];
    private final Object[] mSetForegroundArgs = new Object[1];

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mStartForeground = getClass().getMethod("startForeground", START_FOREGROUND_SIG);
            mStopForeground = getClass().getMethod("stopForeground", STOP_FOREGROUND_SIG);
        } catch (NoSuchMethodException e) {
            mStartForeground = null;
            mStopForeground = null;
        }

        try {
            mSetForeground = getClass().getMethod("setForeground", SET_FOREGROUND_SIG);
        } catch (NoSuchMethodException e) {
            mSetForeground = null;
        }

        PlaybackManager.addListener(this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handleStart();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleStart();
        return START_STICKY;
    }

    private void handleStart() {
        if (PlaybackManager.hasTrack()) {
            compatStartForeground(NOTIFICATION_ID, buildNotification());
        } else {
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        PlaybackManager.removeListener(this);
        compatStopForeground();
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(NOTIFICATION_ID);
        } catch (Throwable t) {
            // Ignore notification cleanup errors
        }
        super.onDestroy();
    }

    @Override
    public void onTrackChanged() {
        updateNotification();
    }

    @Override
    public void onPlaybackStateChanged() {
        updateNotification();
    }

    @Override
    public void onPrepared() {
        updateNotification();
    }

    @Override
    public void onCompletion() {
        updateNotification();
    }

    @Override
    public void onError(String error) {
    }

    private Notification buildNotification() {
        Track track = PlaybackManager.getCurrentTrack();
        String title = track != null ? track.title : "Orpheum";
        String artist = (track != null && !TextUtils.isEmpty(track.artistName)) ? track.artistName : "";

        Intent openIntent = new Intent(this, PlayerActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPending = PendingIntent.getActivity(this, 0, openIntent, 0);

        if (SdkCompat.atLeast(11)) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setContentIntent(contentPending)
                    .setOngoing(true);

            if (SdkCompat.atLeast(16)) {
                Intent playPauseIntent = new Intent(ACTION_PLAY_PAUSE);
                PendingIntent playPausePending = PendingIntent.getBroadcast(this, 1, playPauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(
                        PlaybackManager.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        PlaybackManager.isPlaying() ? "Pause" : "Play",
                        playPausePending);

                Intent nextIntent = new Intent(ACTION_NEXT);
                PendingIntent nextPending = PendingIntent.getBroadcast(this, 2, nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(android.R.drawable.ic_media_next, "Next", nextPending);
            }

            return builder.getNotification();
        } else {
            Notification notification = new Notification(
                    android.R.drawable.ic_media_play,
                    title,
                    System.currentTimeMillis());
            notification.setLatestEventInfo(this, title, artist, contentPending);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            return notification;
        }
    }

    private void updateNotification() {
        if (PlaybackManager.hasTrack()) {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.notify(NOTIFICATION_ID, buildNotification());
            } catch (Throwable t) {
                // Ignore notification update errors
            }
        }
    }

    private void compatStartForeground(int id, Notification notification) {
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
                return;
            } catch (Exception e) {
                // Fall through to the legacy path
            }
        }

        if (mSetForeground != null) {
            try {
                mSetForegroundArgs[0] = Boolean.TRUE;
                mSetForeground.invoke(this, mSetForegroundArgs);
            } catch (Exception e) {
                // Ignore
            }
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private void compatStopForeground() {
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
                return;
            } catch (Exception e) {
                // Fall through to the legacy path
            }
        }

        if (mSetForeground != null) {
            try {
                mSetForegroundArgs[0] = Boolean.FALSE;
                mSetForeground.invoke(this, mSetForegroundArgs);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
