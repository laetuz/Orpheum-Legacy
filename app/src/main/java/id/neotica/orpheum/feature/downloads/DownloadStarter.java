package id.neotica.orpheum.feature.downloads;

import android.content.Context;
import android.content.Intent;

import java.util.List;

import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.feature.downloads.service.DownloadService;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;

/**
 * Enqueues album downloads with {@link DownloadService}.
 */
public final class DownloadStarter {

    private DownloadStarter() { }

    public static void start(Context context, Album album, List<Track> tracks) {
        if (album == null || album.id == null || tracks == null || tracks.isEmpty()) {
            return;
        }
        DownloadStore.enqueue(album, tracks);

        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(DownloadService.ACTION_START);
        intent.putExtra(DownloadService.EXTRA_ALBUM_ID, album.id);
        context.startService(intent);
    }

    public static void cancel(Context context, String albumId) {
        if (albumId == null) {
            return;
        }
        DownloadedAlbum album = DownloadStore.getAlbum(albumId);
        if (album == null) {
            return;
        }
        if (DownloadStore.STATUS_QUEUED.equals(album.status)) {
            DownloadStore.remove(albumId);
            return;
        }
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(DownloadService.ACTION_CANCEL);
        intent.putExtra(DownloadService.EXTRA_ALBUM_ID, albumId);
        context.startService(intent);
    }
}
