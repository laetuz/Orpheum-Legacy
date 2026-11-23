package id.neotica.orpheum.feature.home.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.albumdetail.ui.AlbumDetailActivity;
import id.neotica.orpheum.feature.albumfeed.ui.AlbumFeedActivity;
import id.neotica.orpheum.feature.auth.login.ui.LoginActivity;
import id.neotica.orpheum.feature.auth.register.ui.RegisterActivity;
import id.neotica.orpheum.feature.downloads.ui.DownloadsActivity;
import id.neotica.orpheum.feature.home.contract.HomeView;
import id.neotica.orpheum.feature.home.presenter.HomePresenter;
import id.neotica.orpheum.feature.player.ui.PlayerActivity;
import id.neotica.orpheum.feature.radio.ui.RadioActivity;
import id.neotica.orpheum.feature.search.ui.SearchActivity;
import id.neotica.orpheum.feature.settings.ui.SettingsActivity;
import id.neotica.orpheum.feature.subsonic.ui.SubsonicLoginActivity;
import id.neotica.orpheum.feature.trackfeed.ui.TrackFeedActivity;
import id.neotica.orpheum.model.Album;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.ui.components.NowPlayingBarHelper;
import id.neotica.orpheum.ui.components.TrackRow;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.ImageUrlHelper;
import id.neotica.orpheum.utils.TopBarHelper;

public class MainActivity extends Activity implements HomeView {

    private TextView tvTitle;
    private EditText etHomeSearch;
    private View layoutLoggedOut;
    private View scrollLoggedIn;
    private View sectionAlbums;
    private View sectionTracks;
    private LinearLayout llRecentAlbums;
    private LinearLayout llRecentTracks;

    private ArrayList<Track> recentTracks = new ArrayList<Track>();

    private HomePresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_main);
        CrashCatcher.showCrashLogIfAny(this);

        tvTitle = (TextView) findViewById(R.id.tv_title);
        etHomeSearch = (EditText) findViewById(R.id.et_home_search);
        layoutLoggedOut = findViewById(R.id.layout_logged_out);
        scrollLoggedIn = findViewById(R.id.scroll_logged_in);
        sectionAlbums = findViewById(R.id.section_albums);
        sectionTracks = findViewById(R.id.section_tracks);
        llRecentAlbums = (LinearLayout) findViewById(R.id.ll_recent_albums);
        llRecentTracks = (LinearLayout) findViewById(R.id.ll_recent_tracks);

        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        findViewById(R.id.btn_subsonic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SubsonicLoginActivity.class));
            }
        });

        findViewById(R.id.btn_tracks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TrackFeedActivity.class));
            }
        });

        findViewById(R.id.btn_albums).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AlbumFeedActivity.class));
            }
        });

        findViewById(R.id.btn_home_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSearch();
            }
        });

        findViewById(R.id.btn_downloads).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, DownloadsActivity.class));
            }
        });

        findViewById(R.id.btn_radio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RadioActivity.class));
            }
        });

        findViewById(R.id.tv_see_all_albums).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AlbumFeedActivity.class));
            }
        });

        findViewById(R.id.tv_see_all_tracks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TrackFeedActivity.class));
            }
        });

        etHomeSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    openSearch();
                    return true;
                }
                return false;
            }
        });

        NowPlayingBarHelper.setup(this);

        presenter = new HomePresenter(this);
        presenter.attach(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.load();
        }
        NowPlayingBarHelper.refresh(this);
    }

    private void openSearch() {
        String query = etHomeSearch.getText().toString().trim();
        Intent intent = new Intent(this, SearchActivity.class);
        if (!TextUtils.isEmpty(query)) {
            intent.putExtra(SearchActivity.EXTRA_SEARCH_QUERY, query);
        }
        startActivity(intent);
    }

    @Override
    public void renderLoggedOut() {
        TopBarHelper.setup(this, getString(R.string.app_name), false);
        layoutLoggedOut.setVisibility(View.VISIBLE);
        scrollLoggedIn.setVisibility(View.GONE);
    }

    @Override
    public void renderLoggedIn(String username) {
        TopBarHelper.setup(this, getString(R.string.app_name), false,
                android.R.drawable.ic_menu_preferences, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    }
                });
        layoutLoggedOut.setVisibility(View.GONE);
        scrollLoggedIn.setVisibility(View.VISIBLE);
        tvTitle.setText("Welcome " + username + "!");
    }

    @Override
    public void renderRecentAlbums(List<Album> albums) {
        llRecentAlbums.removeAllViews();

        if (albums == null || albums.isEmpty()) {
            sectionAlbums.setVisibility(View.GONE);
            return;
        }
        sectionAlbums.setVisibility(View.VISIBLE);

        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < albums.size(); i++) {
            final Album album = albums.get(i);
            View card = inflater.inflate(R.layout.item_home_album, llRecentAlbums, false);
            ImageView ivCover = (ImageView) card.findViewById(R.id.iv_home_album_cover);
            TextView tvAlbumTitle = (TextView) card.findViewById(R.id.tv_home_album_title);
            TextView tvAlbumArtist = (TextView) card.findViewById(R.id.tv_home_album_artist);

            tvAlbumTitle.setText(album.title);
            tvAlbumArtist.setText(!TextUtils.isEmpty(album.artistName) ? album.artistName : "");

            if (!TextUtils.isEmpty(album.coverUrl)) {
                ImageLoader.getInstance().displayImage(ImageUrlHelper.build(album.coverUrl), ivCover);
            } else {
                ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, AlbumDetailActivity.class);
                    intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id);
                    startActivity(intent);
                }
            });

            llRecentAlbums.addView(card);
        }
    }

    @Override
    public void renderRecentTracks(List<Track> tracks) {
        recentTracks = new ArrayList<Track>();
        llRecentTracks.removeAllViews();

        if (tracks == null || tracks.isEmpty()) {
            sectionTracks.setVisibility(View.GONE);
            return;
        }
        recentTracks.addAll(tracks);
        sectionTracks.setVisibility(View.VISIBLE);

        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < tracks.size(); i++) {
            final int index = i;
            View row = inflater.inflate(R.layout.item_track, llRecentTracks, false);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(PlayerActivity.createIntent(MainActivity.this, recentTracks, index));
                }
            });
            TrackRow.from(row).bind(tracks.get(i));
            llRecentTracks.addView(row);
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
