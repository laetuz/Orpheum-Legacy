package id.neotica.orpheum.feature.search.contract;

import java.util.List;

import id.neotica.orpheum.model.Track;

/**
 * View contract for the search screen, implemented by the search Activity.
 * {@code SearchPresenter} drives all rendering through this interface.
 */
public interface SearchView {

    void renderResults(List<Track> results, boolean append);

    void showLoadMore(boolean hasMore);

    void showError(String message);
}
