package id.neotica.orpheum.ui.feed;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Track;

public class TrackAdapter extends ArrayAdapter<Track> {

    public TrackAdapter(Context context, List<Track> tracks) {
        super(context, 0, tracks);
    }

    private static class ViewHolder {
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
            vh.tvDuration.setText(formatDuration(track.durationSeconds));
        }

        return convertView;
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
}
