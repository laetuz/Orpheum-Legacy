package id.neotica.orpheum.feature.search.ui;

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
import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.feature.search.contract.SearchView;
import id.neotica.orpheum.feature.search.presenter.SearchPresenter;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.ui.components.TrackAdapter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class SearchActivity extends Activity implements SearchView {

    public static final String EXTRA_SEARCH_QUERY = "search_query";

    private EditText etSearch;
    private ListView listView;
    private TrackAdapter adapter;
    private ArrayList<Track> trackList;
    private View footerView;
    private Button btnLoadMore;

    private SearchPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_search);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.search), true);

        presenter = new SearchPresenter(this);
        presenter.attach(this);

        etSearch = (EditText) findViewById(R.id.et_search);
        Button btnSearch = (Button) findViewById(R.id.btn_search);

        listView = (ListView) findViewById(R.id.lv_results);
        trackList = new ArrayList<Track>();

        footerView = getLayoutInflater().inflate(R.layout.footer_load_more, null);
        btnLoadMore = (Button) footerView.findViewById(R.id.btn_load_more);
        listView.addFooterView(footerView);

        adapter = new TrackAdapter(this, trackList);
        listView.setAdapter(adapter);

        btnLoadMore.setVisibility(View.GONE);

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
                Track track = adapter.getItem(position);
                if (track != null) {
                    startActivity(PlayerActivity.createIntent(SearchActivity.this, trackList, position));
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

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_SEARCH_QUERY)) {
            String initialQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY);
            if (!TextUtils.isEmpty(initialQuery)) {
                etSearch.setText(initialQuery);
                performSearch();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowPlayingBarHelper.refresh(this);
    }

    private void performSearch() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            return;
        }
        presenter.search(query);
    }

    @Override
    public void renderResults(List<Track> results, boolean append) {
        if (!append) {
            adapter.clear();
        }
        for (int i = 0; i < results.size(); i++) {
            adapter.add(results.get(i));
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
