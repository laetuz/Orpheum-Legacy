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
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.network.ApiCallback;
import id.neotica.orpheum.network.ApiTask;
import id.neotica.orpheum.ui.player.PlayerActivity;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.PlaybackManager;

public class AlbumFeedActivity extends Activity {

    private ListView listView;
    private AlbumAdapter adapter;
    private ArrayList<Album> albumList;

    private int currentPage = 1;
    private int totalPages = 1;
    private View footerView;
    private Button btnLoadMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_album_feed);
        CrashCatcher.showCrashLogIfAny(this);

        listView = (ListView) findViewById(R.id.lv_albums);
        albumList = new ArrayList<Album>();

        footerView = getLayoutInflater().inflate(R.layout.footer_load_more, null);
        btnLoadMore = (Button) footerView.findViewById(R.id.btn_load_more);
        listView.addFooterView(footerView);

        adapter = new AlbumAdapter(this, albumList);
        listView.setAdapter(adapter);

        btnLoadMore.setVisibility(View.GONE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Album album = adapter.getItem(position);
                if (album != null) {
                    Intent intent = new Intent(AlbumFeedActivity.this, AlbumDetailActivity.class);
                    intent.putExtra("album_id", album.id);
                    startActivity(intent);
                }
            }
        });

        btnLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPage < totalPages) {
                    currentPage++;
                    fetchAlbums(currentPage);
                }
            }
        });

        setupNowPlayingBar();

        fetchAlbums(currentPage);
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
                startActivity(new Intent(AlbumFeedActivity.this, PlayerActivity.class));
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

    private void fetchAlbums(final int pageToLoad) {
        String url = Constants.ALBUM_FEED_URL + "?page=" + pageToLoad + "&limit=20";

        new ApiTask(this, "GET", url, null, "Loading albums...", new ApiCallback() {
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
                            String artistId = obj.optString("artist_id", "");
                            String title = obj.optString("title", "");
                            int releaseYear = obj.optInt("release_year", 0);
                            String coverUrl = obj.isNull("cover_url") ? "" : obj.optString("cover_url", "");
                            String artistName = obj.optString("artist_name", "");
                            String createdAt = obj.isNull("created_at") ? "" : obj.optString("created_at", "");

                            adapter.add(new Album(id, artistId, title, releaseYear, coverUrl, artistName, createdAt));
                        }
                    }

                    if (currentPage >= totalPages) {
                        btnLoadMore.setVisibility(View.GONE);
                    } else {
                        btnLoadMore.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(AlbumFeedActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AlbumFeedActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                if (pageToLoad > 1) {
                    currentPage--;
                }
            }
        }).execute();
    }
}
