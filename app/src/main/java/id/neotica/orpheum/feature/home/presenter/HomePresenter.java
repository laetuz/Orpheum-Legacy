package id.neotica.orpheum.feature.home.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.home.contract.HomeView;
import id.neotica.orpheum.utils.AuthManager;

/**
 * MVP presenter owning the home screen state: the auth gate.
 * Rendering is delegated to a {@link HomeView}.
 */
public class HomePresenter {

    private final AuthManager authManager;

    private HomeView view;

    public HomePresenter(Context context) {
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
        if (authManager.isLoggedIn()) {
            String username = authManager.getUsernameFromToken();
            view.renderLoggedIn(username != null ? username : "User");
        } else {
            view.renderLoggedOut();
        }
    }
}
