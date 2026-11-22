package id.neotica.orpheum.ui.components;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.ImageUrlHelper;
import id.neotica.orpheum.utils.TimeFormatHelper;

/**
 * ViewHolder-style binder for {@code item_track} rows, shared by the list adapters
 * and the home screen. Stores itself in the row's tag so recycling stays cheap.
 */
public final class TrackRow {

    private final ImageView ivCover;
    private final TextView tvNumber;
    private final TextView tvTitle;
    private final TextView tvArtist;
    private final TextView tvDuration;

    private TrackRow(View row) {
        ivCover = (ImageView) row.findViewById(R.id.iv_track_cover);
        tvNumber = (TextView) row.findViewById(R.id.tv_track_number);
        tvTitle = (TextView) row.findViewById(R.id.tv_track_title);
        tvArtist = (TextView) row.findViewById(R.id.tv_track_artist);
        tvDuration = (TextView) row.findViewById(R.id.tv_track_duration);
    }

    public static TrackRow from(View row) {
        Object tag = row.getTag();
        if (tag instanceof TrackRow) {
            return (TrackRow) tag;
        }
        TrackRow trackRow = new TrackRow(row);
        row.setTag(trackRow);
        return trackRow;
    }

    public void bind(Track track) {
        if (track == null) {
            return;
        }
        tvNumber.setText(String.valueOf(track.trackNumber));
        tvTitle.setText(track.title);
        tvArtist.setText(!TextUtils.isEmpty(track.artistName) ? track.artistName : "");
        tvDuration.setText(TimeFormatHelper.formatDuration(track.durationSeconds));

        if (!TextUtils.isEmpty(track.albumCover)) {
            ImageLoader.getInstance().displayImage(ImageUrlHelper.build(track.albumCover), ivCover);
        } else {
            ImageLoader.getInstance().cancelDisplayTask(ivCover);
            ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }
}
