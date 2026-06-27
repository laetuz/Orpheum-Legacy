package id.neotica.orpheum.model;

public class Album {

    public String id;
    public String artistId;
    public String title;
    public int releaseYear;
    public String coverUrl;
    public String artistName;
    public String createdAt;

    public Album(String id, String artistId, String title, int releaseYear, String coverUrl, String artistName, String createdAt) {
        this.id = id;
        this.artistId = artistId;
        this.title = title;
        this.releaseYear = releaseYear;
        this.coverUrl = coverUrl;
        this.artistName = artistName;
        this.createdAt = createdAt;
    }
}
