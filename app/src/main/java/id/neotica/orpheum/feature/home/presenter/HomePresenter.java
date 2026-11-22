package id.neotica.orpheum.feature.home.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.home.contract.HomeView;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.AuthManager;
import id.neotica.orpheum.utils.Constants;

/**
 * MVP presenter owning the home screen: the auth gate plus the {@code GET /home} feed.
 * Rendering is delegated to a {@link HomeView}; networking stays here.
 */
public class HomePresenter {

    private final Context context;
    private final AuthManager authManager;

    private HomeView view;

    public HomePresenter(Context context) {
        this.context = context;
        this.authManager = new AuthManager(context);
    }

    public void attach(HomeView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        if (view == null) {
            return;
        }
        if (!authManager.isLoggedIn()) {
            view.renderLoggedOut();
            return;
        }
        String username = authManager.getUsernameFromToken();
        view.renderLoggedIn(username != null ? username : "User");
        fetchHome();
    }

    private void fetchHome() {
        requestHome(new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);

                    List<Album> albums = new ArrayList<Album>();
                    JSONArray albumsArray = root.optJSONArray("recent_albums");
                    if (albumsArray != null) {
                        for (int i = 0; i < albumsArray.length(); i++) {
                            Album album = Album.fromJson(albumsArray.optJSONObject(i));
                            if (album != null) {
                                albums.add(album);
                            }
                        }
                    }

                    List<Track> tracks = new ArrayList<Track>();
                    JSONArray tracksArray = root.optJSONArray("recent_tracks");
                    if (tracksArray != null) {
                        for (int i = 0; i < tracksArray.length(); i++) {
                            Track track = Track.fromJson(tracksArray.optJSONObject(i));
                            if (track != null) {
                                tracks.add(track);
                            }
                        }
                    }

                    if (view != null) {
                        view.renderRecentAlbums(albums);
                        view.renderRecentTracks(tracks);
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

    void requestHome(ApiCallback callback) {
        new ApiTask(context, "GET", Constants.HOME_URL + "?limit=10", null, null, callback).execute();
    }
}
