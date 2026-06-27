package id.neotica.orpheum.model;

import java.io.Serializable;

public class Track implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String albumId;
    public String title;
    public int durationSeconds;
    public String fileUrl;
    public int trackNumber;
    public String artistName;

    public Track(String id, String albumId, String title, int durationSeconds, String fileUrl, int trackNumber, String artistName) {
        this.id = id;
        this.albumId = albumId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.fileUrl = fileUrl;
        this.trackNumber = trackNumber;
        this.artistName = artistName;
    }
}
