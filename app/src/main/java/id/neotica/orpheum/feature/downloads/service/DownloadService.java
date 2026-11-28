package id.neotica.orpheum.feature.downloads.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.ImageUrlHelper;
import id.neotica.orpheum.utils.SdkCompat;

/**
 * Sequential album downloader. Keeps itself in the foreground while jobs are
 * queued and writes track files + album covers into {@code DownloadStore}.
 */
public class DownloadService extends Service {

    public static final String ACTION_START = "id.neotica.orpheum.action.DOWNLOAD_START";
    public static final String ACTION_CANCEL = "id.neotica.orpheum.action.DOWNLOAD_CANCEL";
    public static final String ACTION_STATE = "id.neotica.orpheum.action.DOWNLOAD_STATE";

    public static final String EXTRA_ALBUM_ID = "album_id";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_DONE = "done";
    public static final String EXTRA_TOTAL = "total";

    private static final int NOTIFICATION_ID = 2000;

    private static final String DOWNLOADS_ACTIVITY =
            "id.neotica.orpheum.feature.downloads.ui.DownloadsActivity";
    private static final String NOTIFICATION_HELPER =
            "id.neotica.orpheum.utils.NotificationApi11";

    private static final Class<?>[] START_FOREGROUND_SIG = new Class[] { int.class, Notification.class };
    private static final Class<?>[] STOP_FOREGROUND_SIG = new Class[] { boolean.class };
    private static final Class<?>[] SET_FOREGROUND_SIG = new Class[] { boolean.class };

    private Method mStartForeground;
    private Method mStopForeground;
    private Method mSetForeground;

    private final Object[] mStartForegroundArgs = new Object[2];
    private final Object[] mStopForegroundArgs = new Object[1];
    private final Object[] mSetForegroundArgs = new Object[1];

    private final Set<String> cancelRequested = Collections.synchronizedSet(new HashSet<String>());

    private volatile boolean workerRunning;
    private volatile boolean stopping;
    private volatile String currentAlbumId;
    private boolean foregroundStarted;
    private Thread worker;

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
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopping = true;
        if (worker != null) {
            worker.interrupt();
        }
        stopForegroundCompat();
        cancelNotification();
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            ensureWorker();
            return;
        }

        String action = intent.getAction();
        String albumId = intent.getStringExtra(EXTRA_ALBUM_ID);

        if (ACTION_CANCEL.equals(action)) {
            if (albumId != null) {
                cancelRequested.add(albumId);
                DownloadedAlbum album = DownloadStore.getAlbum(albumId);
                if (album == null || DownloadStore.STATUS_QUEUED.equals(album.status)) {
                    DownloadStore.remove(albumId);
                    sendState(albumId, DownloadStore.STATUS_FAILED, 0, 0);
                }
            }
            return;
        }

        if (albumId != null) {
            cancelRequested.remove(albumId);
        }
        ensureWorker();
    }

    private void ensureWorker() {
        if (workerRunning || stopping) {
            return;
        }
        workerRunning = true;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                processQueue();
            }
        });
        worker.start();
    }

    private void processQueue() {
        try {
            while (!stopping) {
                DownloadedAlbum album = DownloadStore.getNextQueued();
                if (album == null) {
                    break;
                }

                currentAlbumId = album.id;
                if (cancelRequested.contains(album.id)) {
                    DownloadStore.remove(album.id);
                    sendState(album.id, DownloadStore.STATUS_FAILED, 0, 0);
                    currentAlbumId = null;
                    continue;
                }

                boolean ok = downloadAlbum(album);
                if (!ok && cancelRequested.contains(album.id)) {
                    DownloadStore.remove(album.id);
                    sendState(album.id, DownloadStore.STATUS_FAILED, 0, 0);
                }
                currentAlbumId = null;
            }
        } finally {
            workerRunning = false;
            currentAlbumId = null;
            stopForegroundCompat();
            cancelNotification();
            stopSelf();
        }
    }

    private boolean downloadAlbum(DownloadedAlbum album) {
        int total = album.tracks.size();
        DownloadStore.setStatus(album.id, DownloadStore.STATUS_DOWNLOADING, 0, total);
        sendState(album.id, DownloadStore.STATUS_DOWNLOADING, 0, total);
        showProgress(album, 0, total, false);

        File dir = DownloadStore.getAlbumDir(album.id);
        if (!dir.exists() && !dir.mkdirs()) {
            DownloadStore.markFailed(album.id);
            sendState(album.id, DownloadStore.STATUS_FAILED, 0, total);
            showProgress(album, 0, total, true);
            return false;
        }

        int done = 0;
        for (int i = 0; i < total; i++) {
            Track track = album.tracks.get(i);
            if (stopping || cancelRequested.contains(album.id)) {
                DownloadStore.markFailed(album.id);
                sendState(album.id, DownloadStore.STATUS_FAILED, done, total);
                return false;
            }

            File target = DownloadStore.getTrackFile(album.id, track.id);
            try {
                String source = track.streamUrl;
                if (TextUtils.isEmpty(source)) {
                    source = Constants.ORPHEUM_BASE + "/stream/" + track.id;
                }
                downloadFile(source, target, album.id);
            } catch (IOException e) {
                DownloadStore.markFailed(album.id);
                sendState(album.id, DownloadStore.STATUS_FAILED, done, total);
                showProgress(album, done, total, true);
                return false;
            }

            done++;
            DownloadStore.setStatus(album.id, DownloadStore.STATUS_DOWNLOADING, done, total);
            sendState(album.id, DownloadStore.STATUS_DOWNLOADING, done, total);
            showProgress(album, done, total, false);
        }

        if (!TextUtils.isEmpty(album.coverUrl)) {
            try {
                downloadFile(ImageUrlHelper.build(album.coverUrl),
                        DownloadStore.getCoverFile(album.id), album.id);
            } catch (IOException e) {
                if (stopping || cancelRequested.contains(album.id)) {
                    DownloadStore.markFailed(album.id);
                    sendState(album.id, DownloadStore.STATUS_FAILED, done, total);
                    return false;
                }
                // Cover art is optional; continue without it.
            }
        }

        long bytes = 0;
        for (int i = 0; i < album.tracks.size(); i++) {
            bytes += DownloadStore.getTrackFile(album.id, album.tracks.get(i).id).length();
        }

        DownloadStore.markCompleted(album.id, bytes);
        sendState(album.id, DownloadStore.STATUS_COMPLETED, total, total);
        showProgress(album, total, total, false);
        return true;
    }

    private void downloadFile(String urlString, File target, String albumId) throws IOException {
        File tmp = new File(target.getAbsolutePath() + ".part");
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new IOException("Unsupported URL scheme: " + protocol);
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            int code = connection.getResponseCode();
            if (code < 200 || code > 299) {
                throw new IOException("HTTP " + code);
            }

            in = connection.getInputStream();
            out = new FileOutputStream(tmp);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                if (stopping || cancelRequested.contains(albumId)) {
                    throw new IOException("Cancelled");
                }
                out.write(buffer, 0, length);
            }

            out.flush();
            out.close();
            out = null;
            in.close();
            in = null;

            if (target.exists()) {
                target.delete();
            }
            if (!tmp.renameTo(target)) {
                throw new IOException("Could not move downloaded file");
            }
        } finally {
            closeQuietly(out);
            closeQuietly(in);
            if (connection != null) {
                connection.disconnect();
            }
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    private void showProgress(DownloadedAlbum album, int done, int total, boolean failed) {
        Notification notification = buildNotification(album, done, total, failed);
        if (!foregroundStarted) {
            compatStartForeground(NOTIFICATION_ID, notification);
            foregroundStarted = true;
            return;
        }
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, notification);
        } catch (Throwable t) {
            // Ignore notification update errors
        }
    }

    private Notification buildNotification(DownloadedAlbum album, int done, int total, boolean failed) {
        String title = album != null && !TextUtils.isEmpty(album.title)
                ? album.title
                : getString(R.string.downloads_title);

        String text;
        if (failed) {
            text = getString(R.string.downloads_failed);
        } else if (total > 0 && done >= total) {
            text = getString(R.string.downloads_complete);
        } else {
            text = getString(R.string.downloads_progress, done, total);
        }

        Intent openIntent = new Intent();
        openIntent.setClassName(this, DOWNLOADS_ACTIVITY);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, 0);

        if (SdkCompat.atLeast(11)) {
            try {
                Class<?> clazz = Class.forName(NOTIFICATION_HELPER);
                Method build = clazz.getMethod("buildDownload",
                        Context.class, String.class, String.class, PendingIntent.class,
                        int.class, int.class, boolean.class);
                return (Notification) build.invoke(null, this, title, text, contentIntent,
                        Integer.valueOf(done), Integer.valueOf(total), Boolean.valueOf(failed));
            } catch (Throwable t) {
                // Fall back to the legacy notification below
            }
        }

        Notification notification = new Notification(
                android.R.drawable.stat_sys_download, title, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, text, contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    private void sendState(String albumId, String status, int done, int total) {
        Intent intent = new Intent(ACTION_STATE);
        intent.putExtra(EXTRA_ALBUM_ID, albumId);
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_DONE, done);
        intent.putExtra(EXTRA_TOTAL, total);
        sendBroadcast(intent);
    }

    private void stopForegroundCompat() {
        if (!foregroundStarted) {
            return;
        }
        foregroundStarted = false;
        compatStopForeground();
    }

    private void cancelNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(NOTIFICATION_ID);
        } catch (Throwable t) {
            // Ignore notification cleanup errors
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

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable t) {
                // Ignore
            }
        }
    }
}
