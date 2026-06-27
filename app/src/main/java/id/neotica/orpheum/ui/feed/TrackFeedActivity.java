package id.neotica.orpheum.ui.feed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.ui.player.PlayerActivity;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.PlaybackManager;

public class TrackFeedActivity extends Activity {

    private ListView listView;
    private TrackAdapter adapter;
    private ArrayList<Track> trackList;

    private int currentPage = 1;
    private int totalPages = 1;
    private View footerView;
    private Button btnLoadMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_track_feed);
        CrashCatcher.showCrashLogIfAny(this);

        listView = (ListView) findViewById(R.id.lv_tracks);
        trackList = new ArrayList<Track>();

        footerView = getLayoutInflater().inflate(R.layout.footer_load_more, null);
        btnLoadMore = (Button) footerView.findViewById(R.id.btn_load_more);
        listView.addFooterView(footerView);

        adapter = new TrackAdapter(this, trackList);
        listView.setAdapter(adapter);

        btnLoadMore.setVisibility(View.GONE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track track = adapter.getItem(position);
                if (track != null) {
                    startActivity(PlayerActivity.createIntent(TrackFeedActivity.this, trackList, position));
                }
            }
        });

        btnLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPage < totalPages) {
                    currentPage++;
                    fetchTracks(currentPage);
                }
            }
        });

        setupNowPlayingBar();

        fetchTracks(currentPage);
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
                startActivity(new Intent(TrackFeedActivity.this, PlayerActivity.class));
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

    private void fetchTracks(final int pageToLoad) {
        String url = Constants.TRACK_FEED_URL + "?page=" + pageToLoad + "&limit=20";

        new ApiTask(this, "GET", url, null, "Loading tracks...", new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject root = new JSONObject(response);

                    currentPage = root.optInt("page", 1);
                    totalPages = root.optInt("total_pages", 1);

                    JSONArray dataArray = root.optJSONArray("data");

                    if (pageToLoad == 1) adapter.clear();

                    if (dataArray != null) {
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);

                            String id = obj.optString("id", "");
                            String albumId = obj.optString("album_id", "");
                            String title = obj.optString("title", "");
                            int durationSeconds = obj.optInt("duration_seconds", 0);
                            String fileUrl = obj.optString("file_url", "");
                            int trackNumber = obj.optInt("track_number", 0);
                            String artistName = obj.optString("artist_name", "");

                            adapter.add(new Track(id, albumId, title, durationSeconds, fileUrl, trackNumber, artistName));
                        }
                    }

                    if (currentPage >= totalPages) {
                        btnLoadMore.setVisibility(View.GONE);
                    } else {
                        btnLoadMore.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(TrackFeedActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(TrackFeedActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                if (pageToLoad > 1) {
                    currentPage--;
                }
            }
        }).execute();
    }
}
