package id.neotica.orpheum.feature.trackfeed.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.feature.trackfeed.contract.TrackFeedView;
import id.neotica.orpheum.feature.trackfeed.presenter.TrackFeedPresenter;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.ui.components.TrackAdapter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class TrackFeedActivity extends Activity implements TrackFeedView {

    private ListView listView;
    private TrackAdapter adapter;
    private ArrayList<Track> trackList;
    private View footerView;
    private Button btnLoadMore;

    private TrackFeedPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_track_feed);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.new_tracks), true);

        presenter = new TrackFeedPresenter(this);
        presenter.attach(this);

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
                presenter.loadMore();
            }
        });

        NowPlayingBarHelper.setup(this);

        presenter.load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowPlayingBarHelper.refresh(this);
    }

    @Override
    public void renderTracks(List<Track> tracks, boolean append) {
        if (!append) {
            adapter.clear();
        }
        for (int i = 0; i < tracks.size(); i++) {
            adapter.add(tracks.get(i));
        }
    }

    @Override
    public void showLoadMore(boolean hasMore) {
        btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }
}
