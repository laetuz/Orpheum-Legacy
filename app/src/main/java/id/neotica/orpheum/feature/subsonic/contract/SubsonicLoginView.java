package id.neotica.orpheum.feature.subsonic.contract;

public interface SubsonicLoginView {

    void showServerError();

    void showInvalidServerError();

    void showUsernameError();

    void showPasswordError();

    void showMessage(String message);

    void navigateToMain();
}
