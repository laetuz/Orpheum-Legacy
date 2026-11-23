package id.neotica.orpheum.feature.home.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.home.contract.HomeView;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceCallback;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.domain.HomeFeed;

/**
 * MVP presenter owning the home screen: the auth gate plus the home feed.
 * Rendering is delegated to a {@link HomeView}; networking stays in the source layer.
 */
public class HomePresenter {

    private final Context context;

    private HomeView view;

    public HomePresenter(Context context) {
        this.context = context;
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
        MusicSource source = SourceManager.get(context);
        if (!source.isLoggedIn()) {
            view.renderLoggedOut();
            return;
        }
        view.renderLoggedIn(source.getDisplayName());
        fetchHome(source);
    }

    private void fetchHome(MusicSource source) {
        source.loadHome(context, 10, new SourceCallback<HomeFeed>() {
            @Override
            public void onSuccess(HomeFeed data) {
                if (view != null) {
                    view.renderRecentAlbums(data.albums);
                    view.renderRecentTracks(data.tracks);
                }
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
