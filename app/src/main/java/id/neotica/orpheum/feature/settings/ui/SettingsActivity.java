package id.neotica.orpheum.feature.settings.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.home.ui.MainActivity;
import id.neotica.orpheum.feature.settings.contract.SettingsView;
import id.neotica.orpheum.feature.settings.presenter.SettingsPresenter;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.SizeFormatHelper;
import id.neotica.orpheum.utils.TopBarHelper;

public class SettingsActivity extends Activity implements SettingsView {

    private TextView tvUsername;
    private TextView tvVersion;
    private TextView tvDownloads;

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
        tvVersion = (TextView) findViewById(R.id.tv_settings_version);
        tvDownloads = (TextView) findViewById(R.id.tv_settings_downloads);
        Button btnClearCache = (Button) findViewById(R.id.btn_clear_cover_cache);
        Button btnLogout = (Button) findViewById(R.id.btn_logout);

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
    public void renderVersion(String versionName, int versionCode) {
        tvVersion.setText(getString(R.string.settings_version, versionName, versionCode));
    }

    @Override
    public void renderDownloadsSummary(int albumCount, long totalBytes) {
        if (albumCount <= 0) {
            tvDownloads.setText(R.string.settings_no_downloads);
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
