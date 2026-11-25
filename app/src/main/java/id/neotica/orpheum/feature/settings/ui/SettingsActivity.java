package id.neotica.orpheum.feature.settings.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.auth.login.ui.LoginActivity;
import id.neotica.orpheum.feature.home.ui.MainActivity;
import id.neotica.orpheum.feature.settings.contract.SettingsView;
import id.neotica.orpheum.feature.settings.presenter.SettingsPresenter;
import id.neotica.orpheum.feature.subsonic.ui.SubsonicLoginActivity;
import id.neotica.orpheum.source.SourceManager;
import id.neotica.orpheum.source.subsonic.SubsonicConfig;
import id.neotica.orpheum.ui.components.DarkDialogHelper;
import id.neotica.orpheum.utils.AuthManager;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.SizeFormatHelper;
import id.neotica.orpheum.utils.TopBarHelper;

public class SettingsActivity extends Activity implements SettingsView {

    private TextView tvUsername;
    private TextView tvSource;
    private TextView tvSourceDetail;
    private TextView tvYoutubeStatus;
    private EditText etYoutubeUrl;
    private TextView tvVersion;
    private TextView tvDownloads;
    private Button btnLogout;

    private SettingsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_settings);
        CrashCatcher.showCrashLogIfAny(this);

        TopBarHelper.setup(this, getString(R.string.settings_title), true);

        presenter = new SettingsPresenter(this);
        presenter.attach(this);

        tvUsername = (TextView) findViewById(R.id.tv_settings_username);
        tvSource = (TextView) findViewById(R.id.tv_settings_source);
        tvSourceDetail = (TextView) findViewById(R.id.tv_settings_source_detail);
        tvYoutubeStatus = (TextView) findViewById(R.id.tv_settings_youtube_status);
        etYoutubeUrl = (EditText) findViewById(R.id.et_settings_youtube_url);
        etYoutubeUrl.clearFocus();
        tvVersion = (TextView) findViewById(R.id.tv_settings_version);
        tvDownloads = (TextView) findViewById(R.id.tv_settings_downloads);
        Button btnSwitchSource = (Button) findViewById(R.id.btn_switch_source);
        Button btnSaveYoutube = (Button) findViewById(R.id.btn_settings_youtube_save);
        Button btnClearCache = (Button) findViewById(R.id.btn_clear_cover_cache);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        btnSwitchSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSourceDialog();
            }
        });

        btnSaveYoutube.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.saveYouTubeListUrl(etYoutubeUrl.getText().toString());
            }
        });

        btnClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.clearCoverCache();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.logout();
            }
        });
    }

    private void showSourceDialog() {
        final boolean orpheum = new AuthManager(this).isLoggedIn();
        final boolean subsonic = new SubsonicConfig(this).isConfigured();
        String[] items = new String[] {
                getString(orpheum ? R.string.settings_source_orpheum : R.string.settings_source_signin),
                getString(subsonic ? R.string.settings_source_subsonic : R.string.settings_source_connect)
        };
        DarkDialogHelper.showList(this, getString(R.string.settings_section_source), items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (orpheum) {
                                presenter.switchSource(SourceManager.SOURCE_ORPHEUM);
                            } else {
                                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                            }
                        } else {
                            if (subsonic) {
                                presenter.switchSource(SourceManager.SOURCE_SUBSONIC);
                            } else {
                                startActivity(new Intent(SettingsActivity.this, SubsonicLoginActivity.class));
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.load();
    }

    @Override
    public void renderProfile(String username) {
        tvUsername.setText(username);
    }

    @Override
    public void renderSource(String label, String detail, boolean subsonicActive) {
        tvSource.setText(label);
        tvSourceDetail.setText(detail);
        btnLogout.setText(subsonicActive ? R.string.subsonic_disconnect : R.string.common_logout);
    }

    @Override
    public void renderYouTube(String listUrl, String summary) {
        if (!etYoutubeUrl.hasFocus()) {
            etYoutubeUrl.setText(listUrl);
        }
        tvYoutubeStatus.setText(summary);
    }

    @Override
    public void renderVersion(String versionName, int versionCode) {
        tvVersion.setText(getString(R.string.settings_version, versionName, versionCode));
    }

    @Override
    public void renderDownloadsSummary(int albumCount, long totalBytes) {
        if (albumCount <= 0) {
            tvDownloads.setText(R.string.settings_no_downloads);
        } else if (albumCount == 1) {
            tvDownloads.setText(getString(R.string.settings_downloads_summary_one,
                    SizeFormatHelper.format(totalBytes)));
        } else {
            tvDownloads.setText(getString(R.string.settings_downloads_summary,
                    albumCount, SizeFormatHelper.format(totalBytes)));
        }
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToMain() {
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }
}
