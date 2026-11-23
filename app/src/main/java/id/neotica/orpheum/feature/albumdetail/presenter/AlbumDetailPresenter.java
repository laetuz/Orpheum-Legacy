package id.neotica.orpheum.feature.albumdetail.presenter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.albumdetail.contract.AlbumDetailView;
import id.neotica.orpheum.feature.downloads.DownloadStarter;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.domain.AlbumDetailData;

/**
 * MVP presenter owning the album detail screen: album header, track list and
 * the download lifecycle. Downloaded albums render from local files first.
 */
public class AlbumDetailPresenter {

    private final Context context;

    private AlbumDetailView view;
    private String albumId;
    private Album currentAlbum;
    private ArrayList<Track> currentTracks = new ArrayList<Track>();

    public AlbumDetailPresenter(Context context) {
        this.context = context;
    }

    public void attach(AlbumDetailView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load(String albumId) {
        this.albumId = albumId;

        DownloadedAlbum local = DownloadStore.getAlbum(albumId);
        if (local != null && DownloadStore.STATUS_COMPLETED.equals(local.status)) {
            Album album = DownloadStore.getLocalAlbum(albumId);
            List<Track> tracks = DownloadStore.getLocalTracks(albumId);
            if (album != null && !tracks.isEmpty()) {
                currentAlbum = album;
                currentTracks = new ArrayList<Track>(tracks);
                if (view != null) {
                    view.renderAlbum(album);
                    view.renderTracks(tracks);
                }
                renderDownloadState(local);
                return;
            }
        }

        request(albumId);
    }

    public void refreshDownloadState() {
        renderDownloadState(albumId != null ? DownloadStore.getAlbum(albumId) : null);
    }

    public void startDownload() {
        if (currentAlbum == null || currentTracks.isEmpty()) {
            return;
        }
        DownloadStarter.start(context, currentAlbum, currentTracks);
        refreshDownloadState();
    }

    public void deleteDownload() {
        if (albumId == null) {
            return;
        }
        DownloadedAlbum local = DownloadStore.getAlbum(albumId);
        if (local != null && (DownloadStore.STATUS_DOWNLOADING.equals(local.status)
                || DownloadStore.STATUS_QUEUED.equals(local.status))) {
            DownloadStarter.cancel(context, albumId);
        } else {
            DownloadStore.remove(albumId);
        }
        refreshDownloadState();
    }

    private void renderDownloadState(DownloadedAlbum local) {
        if (view == null) {
            return;
        }
        boolean downloaded = local != null && DownloadStore.STATUS_COMPLETED.equals(local.status);
        boolean downloading = local != null && (DownloadStore.STATUS_DOWNLOADING.equals(local.status)
                || DownloadStore.STATUS_QUEUED.equals(local.status));
        int done = local != null ? local.done : 0;
        int total = local != null ? local.total : currentTracks.size();
        view.renderDownloadAction(downloaded, downloading, done, total);
    }

    private void request(final String albumId) {
        MusicSource source = SourceManager.get(context);
        source.loadAlbum(context, albumId, new SourceCallback<AlbumDetailData>() {
            @Override
            public void onSuccess(AlbumDetailData data) {
                currentAlbum = data.album;
                currentTracks = new ArrayList<Track>(data.tracks);
                if (view != null) {
                    view.renderAlbum(data.album);
                    view.renderTracks(data.tracks);
                }
                renderDownloadState(DownloadStore.getAlbum(albumId));
            }

            @Override
            public void onError(String message) {
                if (view != null) {
                    view.showError(message);
                }
            }
        });
    }
}
