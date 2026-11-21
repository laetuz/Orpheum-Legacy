package id.neotica.orpheum.feature.auth.forgotpassword.presenter;

import android.content.Context;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import id.neotica.orpheum.feature.auth.forgotpassword.contract.ForgotPasswordView;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.JsonHelper;

/**
 * MVP presenter owning the password-reset flow: validation and reset request.
 * Rendering is delegated to a {@link ForgotPasswordView}; networking stays here.
 */
public class ForgotPasswordPresenter {

    private final Context context;

    private ForgotPasswordView view;

    public ForgotPasswordPresenter(Context context) {
        this.context = context;
    }

    public void attach(ForgotPasswordView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void submit(String email) {
        if (email == null || email.length() == 0) {
            if (view != null) {
                view.showEmailError();
            }
            return;
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("email", email);

        requestForgotPassword(headers, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    String message = JsonHelper.getString(json, "message");
                    if (message.length() == 0) {
                        message = "If the email exists, a reset link has been sent.";
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
                    view.showMessage("Request failed: " + errorMessage);
                }
            }
        });
    }

    // --- I/O seam: overridden in tests to run synchronously ---

    void requestForgotPassword(Map<String, String> headers, ApiCallback callback) {
        new ApiTask(context, "POST", Constants.AUTH_BASE_URL + "/auth/forgot-password", null,
                "Sending...", callback, headers).execute();
    }
}
