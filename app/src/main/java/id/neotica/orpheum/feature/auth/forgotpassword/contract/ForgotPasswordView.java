package id.neotica.orpheum.feature.auth.forgotpassword.contract;

/**
 * View contract for the forgot-password screen, implemented by the Activity.
 * {@code ForgotPasswordPresenter} drives all rendering through this interface.
 */
public interface ForgotPasswordView {

    void showEmailError();

    void showMessage(String message);

    void finishScreen();
}
