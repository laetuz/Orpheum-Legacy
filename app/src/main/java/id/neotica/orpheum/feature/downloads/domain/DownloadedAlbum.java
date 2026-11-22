package id.neotica.orpheum.feature.downloads.domain;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.model.Track;

/**
 * Persisted download entry for an album. Track entries keep only API metadata;
 * local file paths are resolved by {@code DownloadStore} on demand.
 */
public class DownloadedAlbum {

    public String id;
    public String title;
    public String artistName;
    public int releaseYear;
    public String coverUrl;
    public String status;
    public int done;
    public int total;
    public long totalBytes;
    public long addedAt;
    public List<Track> tracks = new ArrayList<Track>();
}
