package id.neotica.orpheum.feature.youtube.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import id.neotica.orpheum.feature.youtube.contract.YouTubeView;
import id.neotica.orpheum.feature.youtube.presenter.YouTubePresenter;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.ui.components.TrackAdapter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class YouTubeActivity extends Activity implements YouTubeView {

    private EditText etSearch;
    private ListView listView;
    private TextView tvEmpty;
    private TrackAdapter adapter;
    private ArrayList<Track> trackList;

    private ProgressDialog searchDialog;
    private ProgressDialog prepareDialog;

    private YouTubePresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_youtube);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.youtube_title), true);

        presenter = new YouTubePresenter(this);
        presenter.attach(this);

        etSearch = (EditText) findViewById(R.id.et_youtube_search);
        Button btnSearch = (Button) findViewById(R.id.btn_youtube_search);
        listView = (ListView) findViewById(R.id.lv_youtube);
        tvEmpty = (TextView) findViewById(R.id.tv_youtube_empty);

        trackList = new ArrayList<Track>();
        adapter = new TrackAdapter(this, trackList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.getItem(position) != null) {
                    presenter.preparePlayback(trackList, position);
                }
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.search(etSearch.getText().toString());
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    presenter.search(etSearch.getText().toString());
                    return true;
                }
                return false;
            }
        });

        NowPlayingBarHelper.setup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NowPlayingBarHelper.refresh(this);
    }

    @Override
    public void renderResults(List<Track> tracks) {
        trackList.clear();
        for (int i = 0; i < tracks.size(); i++) {
            trackList.add(tracks.get(i));
        }
        adapter.notifyDataSetChanged();
        if (tracks.isEmpty()) {
            tvEmpty.setText(R.string.youtube_no_results);
            tvEmpty.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showSearching() {
        if (searchDialog == null) {
            try {
                searchDialog = ProgressDialog.show(this, "",
                        getString(R.string.youtube_searching), true);
            } catch (Throwable t) {
                searchDialog = null;
            }
        }
    }

    @Override
    public void hideSearching() {
        if (searchDialog != null) {
            try {
                searchDialog.dismiss();
            } catch (Throwable t) {
                // Ignore window errors
            }
            searchDialog = null;
        }
    }

    @Override
    public void showPreparing() {
        if (prepareDialog == null) {
            try {
                prepareDialog = ProgressDialog.show(this, "",
                        getString(R.string.youtube_preparing), true);
            } catch (Throwable t) {
                prepareDialog = null;
            }
        }
    }

    @Override
    public void hidePreparing() {
        if (prepareDialog != null) {
            try {
                prepareDialog.dismiss();
            } catch (Throwable t) {
                // Ignore window errors
            }
            prepareDialog = null;
        }
    }

    @Override
    public void openPlayer(ArrayList<Track> tracks, int index) {
        startActivity(PlayerActivity.createIntent(this, tracks, index));
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        hideSearching();
        hidePreparing();
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }
}
