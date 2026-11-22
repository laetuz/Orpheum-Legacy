package id.neotica.orpheum.feature.player.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.player.contract.PlayerView;
import id.neotica.orpheum.feature.player.presenter.PlayerPresenter;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.ui.components.CircularBitmapDisplayer;
import id.neotica.orpheum.utils.CrashCatcher;
import id.neotica.orpheum.utils.ImageUrlHelper;
import id.neotica.orpheum.utils.TimeFormatHelper;
import id.neotica.orpheum.utils.TopBarHelper;

public class PlayerActivity extends Activity implements PlayerView {

    private static final String KEY_TRACKS = "tracks";
    private static final String KEY_INDEX = "index";

    private static final long SPIN_TICK_MS = 40L;
    private static final float DEGREES_PER_MS = 360f / 12000f;

    private TextView tvTitle;
    private TextView tvArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageView ivCover;
    private View seekRow;
    private boolean isLive;

    private int coverSizePx;
    private DisplayImageOptions coverOptions;
    private boolean coverAvailable;

    private final Handler spinHandler = new Handler();
    private final Matrix coverMatrix = new Matrix();
    private float coverAngle;
    private long lastSpinTick;
    private boolean spinning;

    private final Runnable spinRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (lastSpinTick != 0L) {
                coverAngle = (coverAngle + (now - lastSpinTick) * DEGREES_PER_MS) % 360f;
            }
            lastSpinTick = now;
            applyCoverRotation();
            spinHandler.postDelayed(this, SPIN_TICK_MS);
        }
    };

    private PlayerPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashCatcher.init(getApplicationContext());
        setContentView(R.layout.activity_player);
        CrashCatcher.showCrashLogIfAny(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        TopBarHelper.setup(this, getString(R.string.player_title), true,
                android.R.drawable.ic_menu_agenda, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showQueue();
                    }
                });

        tvTitle = (TextView) findViewById(R.id.tv_track_title);
        tvArtist = (TextView) findViewById(R.id.tv_track_artist);
        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);
        tvTotalTime = (TextView) findViewById(R.id.tv_total_time);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        btnPlayPause = (ImageButton) findViewById(R.id.btn_play_pause);
        ImageButton btnPrev = (ImageButton) findViewById(R.id.btn_prev);
        ImageButton btnNext = (ImageButton) findViewById(R.id.btn_next);
        ivCover = (ImageView) findViewById(R.id.iv_player_cover);
        seekRow = findViewById(R.id.player_seek_row);

        coverSizePx = getResources().getDimensionPixelSize(R.dimen.player_cover_size);
        coverOptions = new DisplayImageOptions.Builder()
                .displayer(new CircularBitmapDisplayer(coverSizePx))
                .cacheInMemory(true)
                .build();

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
    protected void onPause() {
        super.onPause();
        stopSpin();
    }

    @Override
    public void renderTrack(Track track) {
        isLive = !TextUtils.isEmpty(track.streamUrl);
        seekRow.setVisibility(isLive ? View.GONE : View.VISIBLE);

        tvTitle.setText(track.title);
        tvArtist.setText(!TextUtils.isEmpty(track.artistName) ? track.artistName : "");
        tvTotalTime.setText(TimeFormatHelper.formatDuration(track.durationSeconds));
        tvCurrentTime.setText("0:00");
        seekBar.setProgress(0);

        coverAvailable = !TextUtils.isEmpty(track.albumCover);
        ivCover.setImageDrawable(null);

        if (!coverAvailable) {
            stopSpin();
            coverAngle = 0f;
            coverMatrix.reset();
            ivCover.setImageMatrix(coverMatrix);
            return;
        }

        ImageLoader.getInstance().displayImage(
                ImageUrlHelper.build(track.albumCover),
                new ImageViewAware(ivCover),
                coverOptions,
                new ImageSize(coverSizePx, coverSizePx),
                new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        applyCoverRotation();
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        coverAvailable = false;
                        stopSpin();
                    }
                },
                null);
    }

    @Override
    public void renderPlaybackState(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
        if (isPlaying) {
            startSpin();
        } else {
            stopSpin();
        }
    }

    @Override
    public void renderProgress(int positionMs, int durationMs) {
        if (isLive) {
            return;
        }
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
        stopSpin();
        if (presenter != null) {
            presenter.detach();
        }
        super.onDestroy();
    }

    private void startSpin() {
        if (spinning || !coverAvailable) {
            return;
        }
        spinning = true;
        lastSpinTick = 0L;
        spinHandler.post(spinRunnable);
    }

    private void stopSpin() {
        spinning = false;
        spinHandler.removeCallbacks(spinRunnable);
        lastSpinTick = 0L;
    }

    private void applyCoverRotation() {
        coverMatrix.reset();
        coverMatrix.setRotate(coverAngle, coverSizePx / 2f, coverSizePx / 2f);
        ivCover.setImageMatrix(coverMatrix);
    }

    private void showQueue() {
        ArrayList<Track> queue = presenter.getQueue();
        if (queue.isEmpty()) {
            return;
        }

        int current = presenter.getCurrentIndex();
        String[] labels = new String[queue.size()];
        for (int i = 0; i < queue.size(); i++) {
            Track track = queue.get(i);
            String label = (i + 1) + ". " + track.title;
            if (i == current) {
                label = "> " + label;
            }
            labels[i] = label;
        }

        Context dialogContext = new ContextThemeWrapper(this, R.style.AppDialogTheme);
        new AlertDialog.Builder(dialogContext)
                .setTitle(R.string.player_queue)
                .setAdapter(
                        new ArrayAdapter<String>(dialogContext, android.R.layout.simple_list_item_1, labels),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                presenter.playAt(which);
                            }
                        })
                .show();
    }

    public static Intent createIntent(Activity from, ArrayList<Track> tracks, int index) {
        Intent intent = new Intent(from, PlayerActivity.class);
        intent.putExtra(KEY_TRACKS, tracks);
        intent.putExtra(KEY_INDEX, index);
        return intent;
    }
}
