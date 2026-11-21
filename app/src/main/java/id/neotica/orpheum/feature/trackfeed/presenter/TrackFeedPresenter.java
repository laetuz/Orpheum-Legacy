package id.neotica.orpheum.feature.trackfeed.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.trackfeed.contract.TrackFeedView;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter owning the paginated new-tracks feed: paging state and parsing.
 * Rendering is delegated to a {@link TrackFeedView}; networking stays here.
 */
public class TrackFeedPresenter {

    private final Context context;

    private TrackFeedView view;
    private int page = 1;
    private int totalPages = 1;

    public TrackFeedPresenter(Context context) {
        this.context = context;
    }

    public void attach(TrackFeedView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        page = 1;
        request(buildUrl(page), false);
    }

    public void loadMore() {
        if (page >= totalPages) {
            return;
        }
        page++;
        request(buildUrl(page), true);
    }

    private void request(String url, final boolean append) {
        requestFeed(url, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    page = JsonHelper.getInt(root, "page", page);
                    totalPages = JsonHelper.getInt(root, "total_pages", 1);

                    List<Track> tracks = new ArrayList<Track>();
                    JSONArray data = root.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            Track track = Track.fromJson(data.optJSONObject(i));
                            if (track != null) {
                                tracks.add(track);
                            }
                        }
                    }

                    if (view != null) {
                        view.renderTracks(tracks, append);
                        view.showLoadMore(page < totalPages);
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
                if (append && page > 1) {
                    page--;
                }
                if (view != null) {
                    view.showError(errorMessage);
                }
            }
        });
    }

    private String buildUrl(int page) {
        return Constants.TRACK_FEED_URL + "?page=" + page + "&limit=20";
    }

    // --- I/O seam: overridden in tests to run synchronously ---

    void requestFeed(String url, ApiCallback callback) {
        new ApiTask(context, "GET", url, null, "Loading tracks...", callback).execute();
    }
}
