package id.neotica.orpheum.feature.downloads.contract;

import java.util.List;

import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;

/**
 * View contract for the downloads screen, implemented by the downloads Activity.
 * {@code DownloadsPresenter} drives all rendering through this interface.
 */
public interface DownloadsView {

    void renderDownloads(List<DownloadedAlbum> albums);

    void showMessage(String message);
}
