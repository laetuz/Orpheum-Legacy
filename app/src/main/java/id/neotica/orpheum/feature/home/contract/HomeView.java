package id.neotica.orpheum.feature.home.contract;

import java.util.List;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;

/**
 * View contract for the home screen, implemented by the home Activity.
 * {@code HomePresenter} drives all rendering through this interface.
 */
public interface HomeView {

    void renderLoggedOut();

    void renderLoggedIn(String username);

    void renderRecentAlbums(List<Album> albums);

    void renderRecentTracks(List<Track> tracks);

    void showError(String message);
}
