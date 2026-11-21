package id.neotica.orpheum.feature.albumdetail.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.albumdetail.contract.AlbumDetailView;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter owning the album detail screen: album header and track list.
 * Rendering is delegated to an {@link AlbumDetailView}; networking stays here.
 */
public class AlbumDetailPresenter {

    private final Context context;

    private AlbumDetailView view;

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
        request(Constants.ALBUM_DETAIL_URL + albumId, albumId);
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

                    if (view != null) {
                        view.renderTracks(tracks);
                    }
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
