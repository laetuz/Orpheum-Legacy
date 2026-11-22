package id.neotica.orpheum.feature.downloads.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.downloads.DownloadStarter;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.downloads.contract.DownloadsView;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;

/**
 * MVP presenter owning the downloads screen: downloaded album list, deletion and
 * cancellation of active downloads.
 */
public class DownloadsPresenter {

    private final Context context;

    private DownloadsView view;

    public DownloadsPresenter(Context context) {
        this.context = context;
    }

    public void attach(DownloadsView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        if (view != null) {
            view.renderDownloads(DownloadStore.getAll());
        }
    }

    public void delete(String albumId) {
        DownloadedAlbum album = DownloadStore.getAlbum(albumId);
        if (album == null) {
            load();
            return;
        }
        if (DownloadStore.STATUS_DOWNLOADING.equals(album.status)
                || DownloadStore.STATUS_QUEUED.equals(album.status)) {
            DownloadStarter.cancel(context, albumId);
        } else {
            DownloadStore.remove(albumId);
        }
        load();
    }
}
