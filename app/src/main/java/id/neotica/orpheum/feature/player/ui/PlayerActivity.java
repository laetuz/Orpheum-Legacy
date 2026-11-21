package id.neotica.orpheum.feature.player.ui;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.player.contract.PlayerView;
import id.neotica.orpheum.feature.player.presenter.PlayerPresenter;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.TimeFormatHelper;
import id.neotica.orpheum.utils.TopBarHelper;

public class PlayerActivity extends Activity implements PlayerView {

    private static final String KEY_TRACKS = "tracks";
    private static final String KEY_INDEX = "index";

    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;

    private PlayerPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_player);
        CrashCatcher.showCrashLogIfAny(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        TopBarHelper.setup(this, getString(R.string.player_title), true);

        tvTitle = (TextView) findViewById(R.id.tv_track_title);
        tvArtist = (TextView) findViewById(R.id.tv_track_artist);
        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);
        tvTotalTime = (TextView) findViewById(R.id.tv_total_time);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        btnPlayPause = (ImageButton) findViewById(R.id.btn_play_pause);
        ImageButton btnPrev = (ImageButton) findViewById(R.id.btn_prev);
        ImageButton btnNext = (ImageButton) findViewById(R.id.btn_next);

        presenter = new PlayerPresenter(this);
        presenter.attach(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(KEY_TRACKS)) {
            @SuppressWarnings("unchecked")
            ArrayList<Track> tracks = (ArrayList<Track>) intent.getSerializableExtra(KEY_TRACKS);
            int index = intent.getIntExtra(KEY_INDEX, 0);
            presenter.init(tracks, index);
        }

        if (!presenter.hasTrack()) {
            Toast.makeText(this, "No tracks to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    presenter.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                presenter.setUserSeeking(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                presenter.setUserSeeking(false);
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.togglePlayPause();
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.previous();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.next();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) {
            presenter.refresh();
        }
    }

    @Override
    public void renderTrack(Track track) {
        tvTitle.setText(track.title);
        tvArtist.setText(!TextUtils.isEmpty(track.artistName) ? track.artistName : "");
        tvTotalTime.setText(TimeFormatHelper.formatDuration(track.durationSeconds));
        tvCurrentTime.setText("0:00");
        seekBar.setProgress(0);
    }

    @Override
    public void renderPlaybackState(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
    }

    @Override
    public void renderProgress(int positionMs, int durationMs) {
        if (durationMs > 0) {
            seekBar.setProgress((int) ((long) positionMs * 100 / durationMs));
            tvTotalTime.setText(TimeFormatHelper.formatDuration(durationMs / 1000));
        }
        tvCurrentTime.setText(TimeFormatHelper.formatDuration(positionMs / 1000));
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }

    public static Intent createIntent(Activity from, ArrayList<Track> tracks, int index) {
        Intent intent = new Intent(from, PlayerActivity.class);
        intent.putExtra(KEY_TRACKS, tracks);
        intent.putExtra(KEY_INDEX, index);
        return intent;
    }
}
