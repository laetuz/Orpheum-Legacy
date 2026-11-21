package id.neotica.orpheum.ui.components;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Track;
import id.neotica.orpheum.utils.ImageUrlHelper;
import id.neotica.orpheum.utils.TimeFormatHelper;

public class TrackAdapter extends ArrayAdapter<Track> {

    public TrackAdapter(Context context, List<Track> tracks) {
        super(context, 0, tracks);
    }

    private static class ViewHolder {
        ImageView ivCover;
        TextView tvTrackNumber;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvDuration;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Track track = getItem(position);

        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_track, parent, false);
            vh.ivCover = (ImageView) convertView.findViewById(R.id.iv_track_cover);
            vh.tvTrackNumber = (TextView) convertView.findViewById(R.id.tv_track_number);
            vh.tvTitle = (TextView) convertView.findViewById(R.id.tv_track_title);
            vh.tvArtist = (TextView) convertView.findViewById(R.id.tv_track_artist);
            vh.tvDuration = (TextView) convertView.findViewById(R.id.tv_track_duration);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        if (track != null) {
            vh.tvTrackNumber.setText(String.valueOf(track.trackNumber));
            vh.tvTitle.setText(track.title);
            if (!TextUtils.isEmpty(track.artistName)) {
                vh.tvArtist.setText(track.artistName);
            } else {
                vh.tvArtist.setText("");
            }
            vh.tvDuration.setText(TimeFormatHelper.formatDuration(track.durationSeconds));

            if (!TextUtils.isEmpty(track.albumCover)) {
                ImageLoader.getInstance().displayImage(ImageUrlHelper.build(track.albumCover), vh.ivCover);
            } else {
                ImageLoader.getInstance().cancelDisplayTask(vh.ivCover);
                vh.ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        return convertView;
    }
}
