package id.neotica.orpheum.model;

import org.json.JSONObject;

import id.neotica.orpheum.utils.JsonHelper;

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

    public static Album fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        return new Album(
                JsonHelper.getString(obj, "id"),
                JsonHelper.getString(obj, "artist_id"),
                JsonHelper.getString(obj, "title"),
                JsonHelper.getInt(obj, "release_year", 0),
                JsonHelper.getString(obj, "cover_url"),
                JsonHelper.getString(obj, "artist_name"),
                JsonHelper.getString(obj, "created_at")
        );
    }
}
