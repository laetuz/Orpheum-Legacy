package id.neotica.orpheum.feature.settings.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.nostra13.universalimageloader.core.ImageLoader;

import id.neotica.orpheum.BuildConfig;
import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.settings.contract.SettingsView;
import id.neotica.orpheum.utils.AuthManager;

/**
 * MVP presenter owning the settings screen: profile, version, downloaded storage,
 * cover-cache clearing, and logout. Rendering is delegated to a {@link SettingsView}.
 */
public class SettingsPresenter {

    private final Context context;
    private final AuthManager authManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SettingsView view;

    public SettingsPresenter(Context context) {
        this.context = context;
        this.authManager = new AuthManager(context);
    }

    public void attach(SettingsView view) {
        this.view = view;
    }

    public void detach() {
        this.view = null;
    }

    public void load() {
        String username = authManager.getUsernameFromToken();
        if (view != null) {
            view.renderProfile(username != null ? username : "User");
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
        authManager.clear();
        if (view != null) {
            view.showMessage("Logged out");
            view.navigateToMain();
        }
    }
}
