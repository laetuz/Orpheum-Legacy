package id.neotica.orpheum.ui.player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.PlaybackManager;

public class PlayerActivity extends Activity {

    private static final String KEY_TRACKS = "tracks";
    private static final String KEY_INDEX = "index";
    private static final int PROGRESS_INTERVAL = 500;

    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageButton btnPrev;
    private ImageButton btnNext;

    private Handler handler;
    private Runnable progressRunnable;

    private boolean userIsSeeking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_player);
        CrashCatcher.showCrashLogIfAny(this);

        tvTitle = (TextView) findViewById(R.id.tv_track_title);
        tvArtist = (TextView) findViewById(R.id.tv_track_artist);
        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);
        tvTotalTime = (TextView) findViewById(R.id.tv_total_time);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        btnPlayPause = (ImageButton) findViewById(R.id.btn_play_pause);
        btnPrev = (ImageButton) findViewById(R.id.btn_prev);
        btnNext = (ImageButton) findViewById(R.id.btn_next);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(KEY_TRACKS)) {
            ArrayList<Track> tracks = (ArrayList<Track>) intent.getSerializableExtra(KEY_TRACKS);
            int index = intent.getIntExtra(KEY_INDEX, 0);
            PlaybackManager.init(tracks, index);
        }

        if (!PlaybackManager.hasTrack()) {
            Toast.makeText(this, "No tracks to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handler = new Handler();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                if (fromUser) {
                    PlaybackManager.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userIsSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userIsSeeking = false;
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackManager.toggle();
                updatePlayPauseIcon();
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackManager.previous();
                updateTrackInfo();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackManager.next();
                updateTrackInfo();
            }
        });

        PlaybackManager.setListener(new PlaybackManager.PlaybackListener() {
            @Override
            public void onTrackChanged() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTrackInfo();
                    }
                });
            }

            @Override
            public void onPrepared() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updatePlayPauseIcon();
                        startProgressUpdater();
                    }
                });
            }

            @Override
            public void onCompletion() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTrackInfo();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    }
                });
            }
        });

        updateTrackInfo();
        if (PlaybackManager.isPrepared()) {
            updatePlayPauseIcon();
            startProgressUpdater();
        }
    }

    private void updateTrackInfo() {
        Track track = PlaybackManager.getCurrentTrack();
        if (track == null) return;

        tvTitle.setText(track.title);
        tvArtist.setText(!TextUtils.isEmpty(track.artistName) ? track.artistName : "");
        tvTotalTime.setText(formatDuration(track.durationSeconds));
        tvCurrentTime.setText("0:00");
        seekBar.setProgress(0);
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (PlaybackManager.isPlaying()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void startProgressUpdater() {
        stopProgressUpdater();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!userIsSeeking && PlaybackManager.isPrepared()) {
                    int pos = PlaybackManager.getCurrentPosition();
                    int dur = PlaybackManager.getDuration();
                    if (dur > 0) {
                        seekBar.setProgress((int) ((long) pos * 100 / dur));
                        tvCurrentTime.setText(formatDuration(pos / 1000));
                    }
                    handler.postDelayed(this, PROGRESS_INTERVAL);
                } else if (PlaybackManager.isPrepared()) {
                    handler.postDelayed(this, PROGRESS_INTERVAL);
                }
            }
        };
        handler.postDelayed(progressRunnable, PROGRESS_INTERVAL);
    }

    private void stopProgressUpdater() {
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    private static String formatDuration(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        StringBuilder sb = new StringBuilder();
        sb.append(min);
        sb.append(":");
        if (sec < 10) sb.append("0");
        sb.append(sec);
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlaybackManager.removeListener();
        stopProgressUpdater();
    }

    public static Intent createIntent(Activity from, ArrayList<Track> tracks, int index) {
        Intent intent = new Intent(from, PlayerActivity.class);
        intent.putExtra(KEY_TRACKS, tracks);
        intent.putExtra(KEY_INDEX, index);
        return intent;
    }
}
