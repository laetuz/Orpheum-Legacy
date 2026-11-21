package id.neotica.orpheum.feature.auth.register.contract;

/**
 * View contract for the register screen, implemented by the register Activity.
 * {@code RegisterPresenter} drives all rendering through this interface.
 */
public interface RegisterView {

    void showUsernameError();

    void showEmailError();

    void showPasswordError();

    void showMessage(String message);

    void finishScreen();
}
