package id.neotica.orpheum.source.domain;

import java.util.List;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;

public class HomeFeed {

    public final List<Album> albums;
    public final List<Track> tracks;

    public HomeFeed(List<Album> albums, List<Track> tracks) {
        this.albums = albums;
        this.tracks = tracks;
    }
}
