package id.neotica.orpheum.model;

import org.json.JSONObject;

import java.io.Serializable;

import id.neotica.orpheum.utils.JsonHelper;

public class Track implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String albumId;
    public String title;
    public int durationSeconds;
    public String fileUrl;
    public int trackNumber;
    public String artistName;
    public String albumCover;
    public String localPath;
    public String streamUrl;
    public String source;
    public String sourceId;
    public boolean live;

    public Track(String id, String albumId, String title, int durationSeconds, String fileUrl, int trackNumber, String artistName, String albumCover) {
        this.id = id;
        this.albumId = albumId;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.fileUrl = fileUrl;
        this.trackNumber = trackNumber;
        this.artistName = artistName;
        this.albumCover = albumCover;
    }

    public static Track fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        return new Track(
                JsonHelper.getString(obj, "id"),
                JsonHelper.getString(obj, "album_id"),
                JsonHelper.getString(obj, "title"),
                JsonHelper.getInt(obj, "duration_seconds", 0),
                JsonHelper.getString(obj, "file_url"),
                JsonHelper.getInt(obj, "track_number", 0),
                JsonHelper.getString(obj, "artist_name"),
                JsonHelper.getString(obj, "album_cover")
        );
    }
}
