package id.neotica.orpheum.feature.auth.login.presenter;

import android.content.Context;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import id.neotica.orpheum.feature.auth.login.contract.LoginView;
import id.neotica.orpheum.feature.auth.login.domain.LoginResult;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.utils.AuthManager;
import id.neotica.orpheum.utils.Constants;

/**
 * MVP presenter owning the login flow: validation, token exchange and username fetch.
 * Rendering is delegated to a {@link LoginView}; networking stays here.
 */
public class LoginPresenter {

    private final Context context;
    private final AuthManager authManager;

    private LoginView view;

    public LoginPresenter(Context context) {
        this.context = context;
        this.authManager = new AuthManager(context);
    }

    public void attach(LoginView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void login(String username, String password) {
        if (username == null || username.length() == 0) {
            if (view != null) {
                view.showUsernameError();
            }
            return;
        }
        if (password == null || password.length() == 0) {
            if (view != null) {
                view.showPasswordError();
            }
            return;
        }

        requestLogin(username, password, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                LoginResult result;
                try {
                    result = LoginResult.fromJson(new JSONObject(response));
                } catch (Exception e) {
                    if (view != null) {
                        view.showMessage("Error parsing response");
                    }
                    return;
                }

                if (result == null || result.token == null) {
                    if (view != null) {
                        view.showMessage("Login failed: no token in response");
                    }
                    return;
                }

                authManager.saveToken(result.token);
                authManager.saveRefreshToken(result.refreshToken);
                authManager.saveExpirationTime(result.expirationTime);
                SourceManager.setActive(context, SourceManager.SOURCE_ORPHEUM);

                fetchUsername();
            }

            @Override
            public void onError(String errorMessage) {
                if (view != null) {
                    view.showMessage("Login failed: " + errorMessage);
                }
            }
        });
    }

    private void fetchUsername() {
        requestUsername(new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (response != null && response.trim().length() > 0) {
                    authManager.saveUsername(response.trim());
                }
                if (view != null) {
                    view.navigateToMain();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (view != null) {
                    view.navigateToMain();
                }
            }
        });
    }

    // --- I/O seams: overridden in tests to run synchronously ---

    void requestLogin(String username, String password, ApiCallback callback) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("username", username);
        headers.put("password", password);

        new ApiTask(context, "GET", Constants.AUTH_BASE_URL + "/auth/login", null,
                "Logging in...", callback, headers).execute();
    }

    void requestUsername(ApiCallback callback) {
        new ApiTask(context, "GET", Constants.AUTH_BASE_URL + "/auth/user/username", null,
                null, callback, authManager.getAuthHeaders()).execute();
    }
}
