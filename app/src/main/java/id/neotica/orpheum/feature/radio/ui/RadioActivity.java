package id.neotica.orpheum.feature.radio.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.radio.FavoriteStationStore;
import id.neotica.orpheum.feature.radio.contract.RadioView;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.feature.radio.presenter.RadioPresenter;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.utils.Constants;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class RadioActivity extends Activity implements RadioView {

    public static final String EXTRA_PAGE_URL = "radio_page_url";
    public static final String EXTRA_TITLE = "radio_page_title";

    private EditText etSearch;
    private RadioAdapter adapter;
    private RadioPresenter presenter;
    private String pageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_radio);
        CrashCatcher.showCrashLogIfAny(this);

        Intent intent = getIntent();
        String title = intent != null ? intent.getStringExtra(EXTRA_TITLE) : null;
        pageUrl = intent != null ? intent.getStringExtra(EXTRA_PAGE_URL) : null;
        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.radio_title);
        }
        TopBarHelper.setup(this, title, true, android.R.drawable.btn_star_big_on,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(RadioActivity.this, FavoritesActivity.class));
                    }
                });

        presenter = new RadioPresenter(this);
        presenter.attach(this);

        etSearch = (EditText) findViewById(R.id.et_radio_search);
        Button btnSearch = (Button) findViewById(R.id.btn_radio_search);
        ListView listView = (ListView) findViewById(R.id.lv_radio);

        adapter = new RadioAdapter(this, new ArrayList<RadioItem>());
        listView.setAdapter(adapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performSearch();
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RadioItem item = adapter.getItem(position);
                if (item == null) {
                    return;
                }
                if (item.type == RadioItem.TYPE_LINK) {
                    openLink(item);
                } else {
                    presenter.tune(item);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                RadioItem item = adapter.getItem(position);
                if (item == null || item.type != RadioItem.TYPE_STATION) {
                    return false;
                }
                boolean favorite = FavoriteStationStore.toggle(item);
                Toast.makeText(RadioActivity.this,
                        favorite ? R.string.radio_favorite_added : R.string.radio_favorite_removed,
                        Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        NowPlayingBarHelper.setup(this);

        presenter.load(!TextUtils.isEmpty(pageUrl) ? pageUrl : Constants.TUNEIN_BROWSE_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        NowPlayingBarHelper.refresh(this);
    }

    private void performSearch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, R.string.radio_no_results, Toast.LENGTH_SHORT).show();
            return;
        }
        presenter.search(query);
    }

    private void openLink(RadioItem item) {
        Intent intent = new Intent(this, RadioActivity.class);
        intent.putExtra(EXTRA_PAGE_URL, item.pageUrl);
        intent.putExtra(EXTRA_TITLE, item.name);
        startActivity(intent);
    }

    @Override
    public void renderItems(List<RadioItem> items) {
        adapter.clear();
        for (int i = 0; i < items.size(); i++) {
            adapter.add(items.get(i));
        }
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
