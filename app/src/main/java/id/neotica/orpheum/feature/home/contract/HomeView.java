package id.neotica.orpheum.feature.home.contract;

/**
 * View contract for the home screen, implemented by the home Activity.
 * {@code HomePresenter} drives all rendering through this interface.
 */
public interface HomeView {

    void renderLoggedOut();

    void renderLoggedIn(String username);
}
