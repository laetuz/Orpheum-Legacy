package id.neotica.orpheum.feature.auth.login.domain;

import org.json.JSONObject;

import id.neotica.orpheum.utils.JsonHelper;

/**
 * Parsed login response ({@code GET /auth/login}).
 */
public class LoginResult {

    public final String token;
    public final String refreshToken;
    public final long expirationTime;

    public LoginResult(String token, String refreshToken, long expirationTime) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
    }

    public static LoginResult fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        return new LoginResult(
                JsonHelper.getStringOrNull(obj, "token"),
                JsonHelper.getStringOrNull(obj, "refreshToken"),
                JsonHelper.getLong(obj, "expirationTime", 0)
        );
    }
}
