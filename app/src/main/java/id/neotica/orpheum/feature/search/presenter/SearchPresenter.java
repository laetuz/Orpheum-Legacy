package id.neotica.orpheum.feature.search.presenter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.search.contract.SearchView;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter owning track search: query state, paging and parsing.
 * Rendering is delegated to a {@link SearchView}; networking stays here.
 */
public class SearchPresenter {

    private final Context context;

    private SearchView view;
    private String query = "";
    private int page = 1;
    private int totalPages = 1;

    public SearchPresenter(Context context) {
        this.context = context;
    }

    public void attach(SearchView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void search(String query) {
        this.query = query == null ? "" : query.trim();
        this.page = 1;
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
        requestSearch(url, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);
                    page = JsonHelper.getInt(root, "page", page);
                    totalPages = JsonHelper.getInt(root, "total_pages", 1);

                    List<Track> results = new ArrayList<Track>();
                    JSONArray data = root.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            Track track = Track.fromJson(data.optJSONObject(i));
                            if (track != null) {
                                results.add(track);
                            }
                        }
                    }

                    if (view != null) {
                        view.renderResults(results, append);
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
        String encoded;
        try {
            encoded = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encoded = query;
        }
        return Constants.SEARCH_URL + "?q=" + encoded + "&page=" + page + "&limit=20";
    }

    // --- I/O seam: overridden in tests to run synchronously ---

    void requestSearch(String url, ApiCallback callback) {
        new ApiTask(context, "GET", url, null, "Searching...", callback).execute();
    }
}
