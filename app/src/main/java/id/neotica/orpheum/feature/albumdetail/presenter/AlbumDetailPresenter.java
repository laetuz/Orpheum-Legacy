package id.neotica.orpheum.feature.albumdetail.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.albumdetail.contract.AlbumDetailView;
import id.neotica.orpheum.feature.downloads.DownloadStarter;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;

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

        request(Constants.ALBUM_DETAIL_URL + albumId, albumId);
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

    private void request(String url, final String albumId) {
        requestAlbum(url, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);

                    JSONObject albumObj = root.optJSONObject("album");
                    if (albumObj != null) {
                        Album album = Album.fromJson(albumObj);
                        if (album != null) {
                            currentAlbum = album;
                        }
                        if (view != null && album != null) {
                            view.renderAlbum(album);
                        }
                    } else if (view != null) {
                        view.showError("Album not found.");
                    }

                    List<Track> tracks = new ArrayList<Track>();
                    JSONArray tracksArray = root.optJSONArray("tracks");
                    if (tracksArray != null) {
                        for (int i = 0; i < tracksArray.length(); i++) {
                            Track track = Track.fromJson(tracksArray.optJSONObject(i));
                            if (track != null) {
                                tracks.add(track);
                            }
                        }
                    }
                    currentTracks = new ArrayList<Track>(tracks);

                    if (view != null) {
                        view.renderTracks(tracks);
                    }
                    renderDownloadState(DownloadStore.getAlbum(albumId));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (view != null) {
                        view.showError("Error parsing server data.");
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (view != null) {
                    view.showError(errorMessage);
                }
            }
        });
    }

    // --- I/O seam: overridden in tests to run synchronously ---

    void requestAlbum(String url, ApiCallback callback) {
        new ApiTask(context, "GET", url, null, "Loading album...", callback).execute();
    }
}
