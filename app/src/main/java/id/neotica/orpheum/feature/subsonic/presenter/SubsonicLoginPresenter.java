package id.neotica.orpheum.feature.subsonic.presenter;

import android.content.Context;

import java.net.URL;

import id.neotica.orpheum.feature.subsonic.contract.SubsonicLoginView;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.subsonic.SubsonicClient;
import id.neotica.orpheum.source.subsonic.SubsonicConfig;

/**
 * MVP presenter owning the Subsonic connection form: validation and ping.
 */
public class SubsonicLoginPresenter {

    private final Context context;

    private SubsonicLoginView view;

    public SubsonicLoginPresenter(Context context) {
        this.context = context;
    }

    public void attach(SubsonicLoginView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void connect(String server, String username, String password) {
        String rawServer = server == null ? "" : server.trim();
        final String user = username == null ? "" : username.trim();
        final String pass = password == null ? "" : password.trim();

        if (rawServer.length() == 0) {
            if (view != null) {
                view.showServerError();
            }
            return;
        }
        final String base = SubsonicClient.normalizeBase(rawServer);
        try {
            String scheme = base.toLowerCase();
            if (!scheme.startsWith("http://") && !scheme.startsWith("https://")) {
                if (view != null) {
                    view.showInvalidServerError();
                }
                return;
            }
            URL url = new URL(base);
            String host = url.getHost();
            if (host == null || host.length() == 0) {
                if (view != null) {
                    view.showInvalidServerError();
                }
                return;
            }
        } catch (Throwable t) {
            if (view != null) {
                view.showInvalidServerError();
            }
            return;
        }
        if (user.length() == 0) {
            if (view != null) {
                view.showUsernameError();
            }
            return;
        }
        if (pass.length() == 0) {
            if (view != null) {
                view.showPasswordError();
            }
            return;
        }

        final String salt = SubsonicClient.randomSalt();
        final String token = SubsonicClient.token(pass, salt);
        String url = SubsonicClient.pingUrl(base, user, salt, token);
        new ApiTask(context, "GET", url, null, "Connecting...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (!SubsonicClient.isOk(response)) {
                    if (view != null) {
                        view.showMessage(SubsonicClient.errorMessage(response));
                    }
                    return;
                }
                new SubsonicConfig(context).save(base, user, salt, token);
                SourceManager.setActive(context, SourceManager.SOURCE_SUBSONIC);
                if (view != null) {
                    view.navigateToMain();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (view != null) {
                    view.showMessage("Connection failed: " + errorMessage);
                }
            }
        }).execute();
    }
}
