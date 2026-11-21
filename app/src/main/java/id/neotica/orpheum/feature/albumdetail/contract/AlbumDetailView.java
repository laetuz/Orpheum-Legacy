package id.neotica.orpheum.feature.albumdetail.contract;

import java.util.List;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;

/**
 * View contract for the album detail screen, implemented by the detail Activity.
 * {@code AlbumDetailPresenter} drives all rendering through this interface.
 */
public interface AlbumDetailView {

    void renderAlbum(Album album);

    void renderTracks(List<Track> tracks);

    void showError(String message);
}
