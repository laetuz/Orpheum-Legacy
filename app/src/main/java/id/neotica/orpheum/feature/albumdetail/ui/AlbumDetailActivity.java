package id.neotica.orpheum.feature.albumdetail.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.albumdetail.contract.AlbumDetailView;
import id.neotica.orpheum.feature.albumdetail.presenter.AlbumDetailPresenter;
import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.ui.components.TrackAdapter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.ImageUrlHelper;
import id.neotica.orpheum.utils.TopBarHelper;

public class AlbumDetailActivity extends Activity implements AlbumDetailView {

    public static final String EXTRA_ALBUM_ID = "album_id";

    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvYear;
    private ListView listView;
    private TrackAdapter adapter;
    private ArrayList<Track> trackList;

    private AlbumDetailPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_album_detail);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.album_detail), true);

        presenter = new AlbumDetailPresenter(this);
        presenter.attach(this);

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

        NowPlayingBarHelper.setup(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ALBUM_ID)) {
            presenter.load(intent.getStringExtra(EXTRA_ALBUM_ID));
        } else {
            Toast.makeText(this, "No album ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowPlayingBarHelper.refresh(this);
    }

    @Override
    public void renderAlbum(Album album) {
        tvTitle.setText(album.title);
        tvArtist.setText(!TextUtils.isEmpty(album.artistName) ? album.artistName : "");
        tvYear.setText(album.releaseYear > 0 ? String.valueOf(album.releaseYear) : "");

        if (!TextUtils.isEmpty(album.coverUrl)) {
            ImageLoader.getInstance().displayImage(ImageUrlHelper.build(album.coverUrl), ivCover);
        } else {
            ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    @Override
    public void renderTracks(List<Track> tracks) {
        adapter.clear();
        for (int i = 0; i < tracks.size(); i++) {
            adapter.add(tracks.get(i));
        }
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
