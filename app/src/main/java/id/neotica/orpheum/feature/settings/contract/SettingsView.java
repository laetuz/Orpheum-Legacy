package id.neotica.orpheum.feature.settings.contract;

/**
 * View contract for the settings screen, implemented by the settings Activity.
 * {@code SettingsPresenter} drives all rendering through this interface.
 */
public interface SettingsView {

    void renderProfile(String username);

    void showMessage(String message);

    void navigateToMain();
}
