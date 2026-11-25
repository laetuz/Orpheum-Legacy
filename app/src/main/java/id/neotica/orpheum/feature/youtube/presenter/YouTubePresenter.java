package id.neotica.orpheum.feature.youtube.presenter;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.feature.player.PlaybackManager;
import id.neotica.orpheum.feature.youtube.YouTubeStreamResolver;
import id.neotica.orpheum.feature.youtube.contract.YouTubeView;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.youtube.YouTubeManager;

public class YouTubePresenter {

    private final Context context;

    private YouTubeView view;
    private ArrayList<Track> results = new ArrayList<Track>();

    public YouTubePresenter(Context context) {
        this.context = context;
    }

    public void attach(YouTubeView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void search(final String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() == 0) {
            results = new ArrayList<Track>();
            if (view != null) {
                view.renderResults(results);
            }
            return;
        }
        final String searchQuery = trimmed;

        if (view != null) {
            view.showSearching();
        }
        new AsyncTask<Void, Void, List<Track>>() {
            private String error;

            @Override
            protected List<Track> doInBackground(Void... voids) {
                try {
                    return YouTubeManager.get(context).search(searchQuery);
                } catch (Throwable t) {
                    error = t.getMessage();
                    if (error == null || error.length() == 0) {
                        error = "YouTube search failed";
                    }
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Track> tracks) {
                if (view != null) {
                    view.hideSearching();
                }
                if (tracks == null) {
                    if (view != null) {
                        view.showMessage(error);
                    }
                    return;
                }
                results = new ArrayList<Track>(tracks);
                if (view != null) {
                    view.renderResults(results);
                }
            }
        }.execute();
    }

    public void preparePlayback(final ArrayList<Track> queue, final int index) {
        if (queue == null || queue.isEmpty() || index < 0 || index >= queue.size()) {
            return;
        }
        final ArrayList<Track> playbackQueue = new ArrayList<Track>(queue);
        if (view != null) {
            view.showPreparing();
        }
        new AsyncTask<Void, Void, String>() {
            private String error;

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return YouTubeManager.get(context).resolveAudio(playbackQueue.get(index));
                } catch (Throwable t) {
                    error = t.getMessage();
                    if (error == null || error.length() == 0) {
                        error = "Could not prepare audio";
                    }
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String url) {
                if (view != null) {
                    view.hidePreparing();
                }
                if (url == null || url.length() == 0) {
                    if (view != null) {
                        view.showMessage(error);
                    }
                    return;
                }
                playbackQueue.get(index).streamUrl = url;
                PlaybackManager.setStreamUrlResolver(new YouTubeStreamResolver(context));
                if (view != null) {
                    view.openPlayer(playbackQueue, index);
                }
            }
        }.execute();
    }
}
