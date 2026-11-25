package id.neotica.orpheum.feature.youtube.contract;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.model.Track;

public interface YouTubeView {

    void renderResults(List<Track> tracks);

    void showSearching();

    void hideSearching();

    void showPreparing();

    void hidePreparing();

    void openPlayer(ArrayList<Track> tracks, int index);

    void showMessage(String message);
}
