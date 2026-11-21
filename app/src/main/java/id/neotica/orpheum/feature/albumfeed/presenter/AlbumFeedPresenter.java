package id.neotica.orpheum.feature.albumfeed.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.albumfeed.contract.AlbumFeedView;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter owning the paginated new-releases feed: paging state and parsing.
 * Rendering is delegated to an {@link AlbumFeedView}; networking stays here.
 */
public class AlbumFeedPresenter {

    private final Context context;

    private AlbumFeedView view;
    private int page = 1;
    private int totalPages = 1;

    public AlbumFeedPresenter(Context context) {
        this.context = context;
    }

    public void attach(AlbumFeedView view) {
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

                    List<Album> albums = new ArrayList<Album>();
                    JSONArray data = root.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            Album album = Album.fromJson(data.optJSONObject(i));
                            if (album != null) {
                                albums.add(album);
                            }
                        }
                    }

                    if (view != null) {
                        view.renderAlbums(albums, append);
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
        return Constants.ALBUM_FEED_URL + "?page=" + page + "&limit=20";
    }

    // --- I/O seam: overridden in tests to run synchronously ---

    void requestFeed(String url, ApiCallback callback) {
        new ApiTask(context, "GET", url, null, "Loading albums...", callback).execute();
    }
}
