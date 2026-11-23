package id.neotica.orpheum.feature.radio.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.radio.FavoriteStationStore;
import id.neotica.orpheum.feature.radio.contract.RadioView;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.feature.radio.presenter.RadioPresenter;
import id.neotica.orpheum.ui.components.DarkDialogHelper;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class FavoritesActivity extends Activity implements RadioView {

    private ListView listView;
    private View emptyView;
    private RadioAdapter adapter;
    private RadioPresenter presenter;
    private final ArrayList<RadioItem> favorites = new ArrayList<RadioItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_favorites);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.radio_favorites_title), true);

        presenter = new RadioPresenter(this);
        presenter.attach(this);

        listView = (ListView) findViewById(R.id.lv_favorites);
        emptyView = findViewById(R.id.tv_favorites_empty);

        adapter = new RadioAdapter(this, favorites);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RadioItem item = adapter.getItem(position);
                if (item == null) {
                    return;
                }
                if (item.type == RadioItem.TYPE_SHOW) {
                    openShow(item);
                } else {
                    presenter.tune(item);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                RadioItem item = adapter.getItem(position);
                if (item != null) {
                    showRemoveMenu(item);
                    return true;
                }
                return false;
            }
        });

        NowPlayingBarHelper.setup(this);

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        NowPlayingBarHelper.refresh(this);
    }

    private void reload() {
        List<RadioItem> items = FavoriteStationStore.getAll();
        favorites.clear();
        favorites.addAll(items);
        adapter.notifyDataSetChanged();

        boolean empty = favorites.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openShow(RadioItem item) {
        Intent intent = new Intent(this, RadioActivity.class);
        intent.putExtra(RadioActivity.EXTRA_PAGE_URL, item.pageUrl);
        intent.putExtra(RadioActivity.EXTRA_TITLE, item.name);
        intent.putExtra(RadioActivity.EXTRA_SHOW_ITEM, item);
        startActivity(intent);
    }

    private void showRemoveMenu(final RadioItem item) {
        DarkDialogHelper.showList(this, item.name,
                new String[] { getString(R.string.radio_remove_from_favorites) },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FavoriteStationStore.remove(item.guideId);
                        reload();
                        Toast.makeText(FavoritesActivity.this, R.string.radio_favorite_removed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void renderItems(List<RadioItem> items) {
    }

    @Override
    public void playStation(RadioItem station, String streamUrl) {
        RadioPlaybackHelper.play(this, station, streamUrl);
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
