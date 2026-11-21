package id.neotica.orpheum.feature.settings.presenter;

import android.content.Context;

import id.neotica.orpheum.feature.settings.contract.SettingsView;
import id.neotica.orpheum.utils.AuthManager;

/**
 * MVP presenter owning the settings screen: profile and logout.
 * Rendering is delegated to a {@link SettingsView}.
 */
public class SettingsPresenter {

    private final AuthManager authManager;

    private SettingsView view;

    public SettingsPresenter(Context context) {
        this.authManager = new AuthManager(context);
    }

    public void attach(SettingsView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        String username = authManager.getUsernameFromToken();
        if (view != null) {
            view.renderProfile(username != null ? username : "User");
        }
    }

    public void logout() {
        authManager.clear();
        if (view != null) {
            view.showMessage("Logged out");
            view.navigateToMain();
        }
    }
}
