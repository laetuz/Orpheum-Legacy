package id.neotica.orpheum.feature.albumfeed.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.albumdetail.ui.AlbumDetailActivity;
import id.neotica.orpheum.feature.albumfeed.contract.AlbumFeedView;
import id.neotica.orpheum.feature.albumfeed.presenter.AlbumFeedPresenter;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class AlbumFeedActivity extends Activity implements AlbumFeedView {

    private ListView listView;
    private AlbumAdapter adapter;
    private ArrayList<Album> albumList;
    private View footerView;
    private Button btnLoadMore;

    private AlbumFeedPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_album_feed);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.new_releases), true);

        presenter = new AlbumFeedPresenter(this);
        presenter.attach(this);

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
                    intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id);
                    startActivity(intent);
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
    public void renderAlbums(List<Album> albums, boolean append) {
        if (!append) {
            adapter.clear();
        }
        for (int i = 0; i < albums.size(); i++) {
            adapter.add(albums.get(i));
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
