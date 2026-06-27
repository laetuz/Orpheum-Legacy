package id.neotica.orpheum.ui.feed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import id.neotica.orpheum.BuildConfig;
import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.ui.player.PlayerActivity;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.PlaybackManager;

public class AlbumDetailActivity extends Activity {

    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvYear;
    private ListView listView;
    private TrackAdapter adapter;
    private ArrayList<Track> trackList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_album_detail);
        CrashCatcher.showCrashLogIfAny(this);

        ivCover = (ImageView) findViewById(R.id.iv_album_cover);
        tvTitle = (TextView) findViewById(R.id.tv_album_title);
        tvArtist = (TextView) findViewById(R.id.tv_album_artist);
        tvYear = (TextView) findViewById(R.id.tv_album_year);
        listView = (ListView) findViewById(R.id.lv_tracks);

        trackList = new ArrayList<Track>();
        adapter = new TrackAdapter(this, trackList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track track = adapter.getItem(position);
                if (track != null) {
                    startActivity(PlayerActivity.createIntent(AlbumDetailActivity.this, trackList, position));
                }
            }
        });

        setupNowPlayingBar();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("album_id")) {
            String albumId = intent.getStringExtra("album_id");
            fetchAlbumDetail(albumId);
        } else {
            Toast.makeText(this, "No album ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchAlbumDetail(String albumId) {
        String url = Constants.ALBUM_DETAIL_URL + albumId;

        new ApiTask(this, "GET", url, null, "Loading album...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);

                    JSONObject albumObj = root.optJSONObject("album");
                    if (albumObj != null) {
                        String id = albumObj.optString("id", "");
                        String artistId = albumObj.optString("artist_id", "");
                        String title = albumObj.optString("title", "");
                        int releaseYear = albumObj.optInt("release_year", 0);
                        String coverUrl = albumObj.isNull("cover_url") ? "" : albumObj.optString("cover_url", "");
                        String artistName = albumObj.optString("artist_name", "");
                        String createdAt = albumObj.isNull("created_at") ? "" : albumObj.optString("created_at", "");

                        setAlbumInfo(new Album(id, artistId, title, releaseYear, coverUrl, artistName, createdAt));
                    }

                    JSONArray tracksArray = root.optJSONArray("tracks");
                    if (tracksArray != null) {
                        for (int i = 0; i < tracksArray.length(); i++) {
                            JSONObject obj = tracksArray.getJSONObject(i);

                            String trackId = obj.optString("id", "");
                            String albumId = obj.optString("album_id", "");
                            String trackTitle = obj.optString("title", "");
                            int durationSeconds = obj.optInt("duration_seconds", 0);
                            String fileUrl = obj.optString("file_url", "");
                            int trackNumber = obj.optInt("track_number", 0);
                            String artistName = obj.optString("artist_name", "");

                            adapter.add(new Track(trackId, albumId, trackTitle, durationSeconds, fileUrl, trackNumber, artistName));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(AlbumDetailActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AlbumDetailActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }).execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNowPlayingBar();
    }

    private void setupNowPlayingBar() {
        final View bar = findViewById(R.id.now_playing_bar);
        final ImageButton npPlayPause = (ImageButton) findViewById(R.id.np_play_pause);

        bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(AlbumDetailActivity.this, PlayerActivity.class));
            }
        });

        npPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackManager.toggle();
                refreshNowPlayingBar();
            }
        });
    }

    private void refreshNowPlayingBar() {
        View bar = findViewById(R.id.now_playing_bar);
        if (bar == null) return;

        if (PlaybackManager.hasTrack()) {
            bar.setVisibility(View.VISIBLE);
            TextView npTitle = (TextView) findViewById(R.id.np_title);
            TextView npArtist = (TextView) findViewById(R.id.np_artist);
            ImageButton npPlayPause = (ImageButton) findViewById(R.id.np_play_pause);

            Track track = PlaybackManager.getCurrentTrack();
            if (track != null) {
                npTitle.setText(track.title);
                npArtist.setText(!TextUtils.isEmpty(track.artistName) ? track.artistName : "");
            }
            npPlayPause.setImageResource(
                PlaybackManager.isPlaying()
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play
            );
        } else {
            bar.setVisibility(View.GONE);
        }
    }

    private void setAlbumInfo(Album album) {
        tvTitle.setText(album.title);
        if (!TextUtils.isEmpty(album.artistName)) {
            tvArtist.setText(album.artistName);
        } else {
            tvArtist.setText("");
        }
        tvYear.setText(String.valueOf(album.releaseYear));

        if (!TextUtils.isEmpty(album.coverUrl)) {
            String fullUrl = BuildConfig.FILE_BASE_URL + "/buckets/" + album.coverUrl;
            ImageLoader.getInstance().displayImage(fullUrl, ivCover);
        } else {
            ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }
}
