package id.neotica.orpheum.feature.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.albumdetail.ui.AlbumDetailActivity;
import id.neotica.orpheum.feature.downloads.contract.DownloadsView;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.feature.downloads.presenter.DownloadsPresenter;
import id.neotica.orpheum.feature.downloads.service.DownloadService;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class DownloadsActivity extends Activity implements DownloadsView {

    private ListView listView;
    private View emptyView;
    private DownloadsAdapter adapter;
    private DownloadsPresenter presenter;
    private boolean receiverRegistered;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.ACTION_STATE.equals(intent.getAction()) && presenter != null) {
                presenter.load();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_downloads);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.downloads_title), true);

        presenter = new DownloadsPresenter(this);
        presenter.attach(this);

        listView = (ListView) findViewById(R.id.lv_downloads);
        emptyView = findViewById(R.id.tv_downloads_empty);
        adapter = new DownloadsAdapter(this, new ArrayList<DownloadedAlbum>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DownloadedAlbum album = adapter.getItem(position);
                if (album != null) {
                    Intent intent = new Intent(DownloadsActivity.this, AlbumDetailActivity.class);
                    intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id);
                    startActivity(intent);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                DownloadedAlbum album = adapter.getItem(position);
                if (album != null) {
                    confirmDelete(album);
                    return true;
                }
                return false;
            }
        });

        NowPlayingBarHelper.setup(this);

        presenter.load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            registerReceiver(stateReceiver, new IntentFilter(DownloadService.ACTION_STATE));
            receiverRegistered = true;
        }
        if (presenter != null) {
            presenter.load();
        }
        NowPlayingBarHelper.refresh(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver);
            receiverRegistered = false;
        }
    }

    private void confirmDelete(final DownloadedAlbum album) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.downloads_delete)
                .setMessage(getString(R.string.downloads_delete_confirm, album.title))
                .setPositiveButton(R.string.common_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.delete(album.id);
                    }
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    @Override
    public void renderDownloads(List<DownloadedAlbum> albums) {
        adapter.clear();
        for (int i = 0; i < albums.size(); i++) {
            adapter.add(albums.get(i));
        }

        boolean empty = albums.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }
}
