package id.neotica.orpheum.feature.settings.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.nostra13.universalimageloader.core.ImageLoader;

import id.neotica.orpheum.BuildConfig;
import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.settings.contract.SettingsView;
import id.neotica.orpheum.source.MusicSource;
import id.neotica.orpheum.source.SourceManager;

/**
 * MVP presenter owning the settings screen: profile, active source, version,
 * downloaded storage, cover-cache clearing, and logout.
 */
public class SettingsPresenter {

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SettingsView view;

    public SettingsPresenter(Context context) {
        this.context = context;
    }

    public void attach(SettingsView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        MusicSource source = SourceManager.get(context);
        boolean subsonic = SourceManager.SOURCE_SUBSONIC.equals(source.getId());
        String label = context.getString(subsonic
                ? R.string.settings_source_subsonic
                : R.string.settings_source_orpheum);
        String detail;
        String profile;
        if (source.isLoggedIn()) {
            profile = source.getDisplayName();
            detail = subsonic ? source.getDescription() : profile;
        } else {
            profile = context.getString(R.string.settings_source_none);
            detail = context.getString(R.string.settings_source_none);
        }

        if (view != null) {
            view.renderProfile(profile);
            view.renderSource(label, detail, subsonic);
            view.renderVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int albumCount = DownloadStore.getCompletedCount();
                final long totalBytes = DownloadStore.getStorageBytes();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (view != null) {
                            view.renderDownloadsSummary(albumCount, totalBytes);
                        }
                    }
                });
            }
        }).start();
    }

    public void switchSource(String sourceId) {
        SourceManager.setActive(context, sourceId);
        if (view != null) {
            view.showMessage(context.getString(R.string.settings_source_changed));
            view.navigateToMain();
        }
    }

    public void clearCoverCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                try {
                    ImageLoader loader = ImageLoader.getInstance();
                    if (!loader.isInited()) {
                        success = false;
                    } else {
                        loader.clearMemoryCache();
                        loader.clearDiskCache();
                    }
                } catch (Throwable t) {
                    success = false;
                }
                final boolean cleared = success;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (view != null) {
                            view.showMessage(context.getString(cleared
                                    ? R.string.settings_cover_cache_cleared
                                    : R.string.settings_cover_cache_failed));
                        }
                    }
                });
            }
        }).start();
    }

    public void logout() {
        boolean subsonic = SourceManager.SOURCE_SUBSONIC.equals(
                SourceManager.get(context).getId());
        SourceManager.logout(context);
        if (view != null) {
            view.showMessage(context.getString(subsonic
                    ? R.string.subsonic_disconnected
                    : R.string.settings_logged_out));
            view.navigateToMain();
        }
    }
}
