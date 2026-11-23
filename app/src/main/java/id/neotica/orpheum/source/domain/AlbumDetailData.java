package id.neotica.orpheum.source.domain;

import java.util.List;

import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;

public class AlbumDetailData {

    public final Album album;
    public final List<Track> tracks;

    public AlbumDetailData(Album album, List<Track> tracks) {
        this.album = album;
        this.tracks = tracks;
    }
}
