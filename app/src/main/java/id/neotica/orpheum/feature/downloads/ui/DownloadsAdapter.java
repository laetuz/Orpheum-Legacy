package id.neotica.orpheum.feature.downloads.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.downloads.DownloadStore;
import id.neotica.orpheum.feature.downloads.domain.DownloadedAlbum;
import id.neotica.orpheum.utils.ImageUrlHelper;

public class DownloadsAdapter extends ArrayAdapter<DownloadedAlbum> {

    public DownloadsAdapter(Context context, List<DownloadedAlbum> albums) {
        super(context, 0, albums);
    }

    private static class ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvStatus;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DownloadedAlbum album = getItem(position);

        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_download_album, parent, false);
            vh.ivCover = (ImageView) convertView.findViewById(R.id.iv_download_cover);
            vh.tvTitle = (TextView) convertView.findViewById(R.id.tv_download_title);
            vh.tvArtist = (TextView) convertView.findViewById(R.id.tv_download_artist);
            vh.tvStatus = (TextView) convertView.findViewById(R.id.tv_download_status);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        if (album != null) {
            vh.tvTitle.setText(album.title);
            vh.tvArtist.setText(!TextUtils.isEmpty(album.artistName) ? album.artistName : "");
            vh.tvStatus.setText(statusText(album));

            File cover = DownloadStore.getCoverFile(album.id);
            if (cover.exists()) {
                ImageLoader.getInstance().displayImage("file://" + cover.getAbsolutePath(), vh.ivCover);
            } else if (!TextUtils.isEmpty(album.coverUrl)) {
                ImageLoader.getInstance().displayImage(ImageUrlHelper.build(album.coverUrl), vh.ivCover);
            } else {
                ImageLoader.getInstance().cancelDisplayTask(vh.ivCover);
                vh.ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        return convertView;
    }

    private String statusText(DownloadedAlbum album) {
        Context context = getContext();
        if (DownloadStore.STATUS_COMPLETED.equals(album.status)) {
            return context.getString(R.string.downloads_summary,
                    album.tracks.size(), formatSize(album.totalBytes));
        }
        if (DownloadStore.STATUS_DOWNLOADING.equals(album.status)) {
            return context.getString(R.string.downloads_progress, album.done, album.total);
        }
        if (DownloadStore.STATUS_QUEUED.equals(album.status)) {
            return context.getString(R.string.downloads_queued);
        }
        return context.getString(R.string.downloads_failed);
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) {
            return "0 KB";
        }
        long kb = Math.round(bytes / 1024.0);
        if (kb < 1024) {
            return kb + " KB";
        }
        long tenths = Math.round(bytes / (1024.0 * 1024.0) * 10);
        return (tenths / 10) + "." + (tenths % 10) + " MB";
    }
}
