package id.neotica.orpheum.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

import java.lang.reflect.Method;

/**
 * API 11+ notification builder. Services load this class by name through
 * reflection so their own classes stay verify-safe on API 3.
 */
public final class NotificationApi11 {

    private NotificationApi11() { }

    public static Notification buildPlayback(Context context, String title, String artist,
                                             PendingIntent content, boolean playing,
                                             PendingIntent playPause, PendingIntent next) {
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(content)
                .setOngoing(true);

        addAction(builder,
                playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                playing ? "Pause" : "Play",
                playPause);
        addAction(builder, android.R.drawable.ic_media_next, "Next", next);
        return builder.getNotification();
    }

    public static Notification buildDownload(Context context, String title, String text,
                                             PendingIntent content, int done, int total,
                                             boolean failed) {
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setOngoing(true);

        if (!failed && total > 0) {
            builder.setProgress(total, done, false);
        }
        return builder.getNotification();
    }

    private static void addAction(Notification.Builder builder, int icon,
                                  CharSequence title, PendingIntent intent) {
        if (intent == null) {
            return;
        }
        try {
            Method addAction = builder.getClass().getMethod("addAction",
                    int.class, CharSequence.class, PendingIntent.class);
            addAction.invoke(builder, Integer.valueOf(icon), title, intent);
        } catch (Throwable t) {
            // API 11-15 have no addAction; show the basic notification instead.
        }
    }
}
