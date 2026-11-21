package id.neotica.orpheum.feature.home.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.albumfeed.ui.AlbumFeedActivity;
import id.neotica.orpheum.feature.auth.login.ui.LoginActivity;
import id.neotica.orpheum.feature.auth.register.ui.RegisterActivity;
import id.neotica.orpheum.feature.home.contract.HomeView;
import id.neotica.orpheum.feature.home.presenter.HomePresenter;
import id.neotica.orpheum.feature.search.ui.SearchActivity;
import id.neotica.orpheum.feature.settings.ui.SettingsActivity;
import id.neotica.orpheum.feature.trackfeed.ui.TrackFeedActivity;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TopBarHelper;

public class MainActivity extends Activity implements HomeView {

    private TextView tvTitle;
    private Button btnLogin;
    private Button btnRegister;
    private Button btnTracks;
    private Button btnAlbums;
    private Button btnSearch;

    private HomePresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_main);
        CrashCatcher.showCrashLogIfAny(this);

        tvTitle = (TextView) findViewById(R.id.tv_title);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnRegister = (Button) findViewById(R.id.btn_register);
        btnTracks = (Button) findViewById(R.id.btn_tracks);
        btnAlbums = (Button) findViewById(R.id.btn_albums);
        btnSearch = (Button) findViewById(R.id.btn_search);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        btnTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, TrackFeedActivity.class));
            }
        });

        btnAlbums.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AlbumFeedActivity.class));
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });

        presenter = new HomePresenter(this);
        presenter.attach(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.load();
        }
    }

    @Override
    public void renderLoggedOut() {
        TopBarHelper.setup(this, getString(R.string.app_name), false);
        tvTitle.setVisibility(View.GONE);
        btnLogin.setVisibility(View.VISIBLE);
        btnRegister.setVisibility(View.VISIBLE);
        btnTracks.setVisibility(View.GONE);
        btnAlbums.setVisibility(View.GONE);
        btnSearch.setVisibility(View.GONE);
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
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText("Welcome " + username + "!");
        btnLogin.setVisibility(View.GONE);
        btnRegister.setVisibility(View.GONE);
        btnTracks.setVisibility(View.VISIBLE);
        btnAlbums.setVisibility(View.VISIBLE);
        btnSearch.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }
}
