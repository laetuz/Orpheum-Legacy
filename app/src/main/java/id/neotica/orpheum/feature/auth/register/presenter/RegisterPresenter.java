package id.neotica.orpheum.feature.auth.register.presenter;

import android.content.Context;

import org.json.JSONObject;

import id.neotica.orpheum.feature.auth.register.contract.RegisterView;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter owning the registration flow: validation and account creation.
 * Rendering is delegated to a {@link RegisterView}; networking stays here.
 */
public class RegisterPresenter {

    private final Context context;

    private RegisterView view;

    public RegisterPresenter(Context context) {
        this.context = context;
    }

    public void attach(RegisterView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void register(String username, String email, String password) {
        if (username == null || username.length() == 0) {
            if (view != null) {
                view.showUsernameError();
            }
            return;
        }
        if (email == null || email.length() == 0) {
            if (view != null) {
                view.showEmailError();
            }
            return;
        }
        if (password == null || password.length() == 0) {
            if (view != null) {
                view.showPasswordError();
            }
            return;
        }

        String payload;
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);
            json.put("email", email);
            payload = json.toString();
        } catch (Exception e) {
            if (view != null) {
                view.showMessage("Error creating request");
            }
            return;
        }

        requestRegister(payload, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    String message = JsonHelper.getString(json, "message");
                    if (message.length() == 0) {
                        message = "Account created.";
                    }
                    if (view != null) {
                        view.showMessage(message);
                        view.finishScreen();
                    }
                } catch (Exception e) {
                    if (view != null) {
                        view.showMessage("Error parsing response");
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (view != null) {
                    view.showMessage("Registration failed: " + errorMessage);
                }
            }
        });
    }

    // --- I/O seam: overridden in tests to run synchronously ---

    void requestRegister(String payload, ApiCallback callback) {
        new ApiTask(context, "POST", Constants.AUTH_BASE_URL + "/auth/register", payload,
                "Registering...", callback).execute();
    }
}
