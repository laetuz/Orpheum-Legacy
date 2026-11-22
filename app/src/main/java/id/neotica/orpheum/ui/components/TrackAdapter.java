package id.neotica.orpheum.ui.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Track;

public class TrackAdapter extends ArrayAdapter<Track> {

    public TrackAdapter(Context context, List<Track> tracks) {
        super(context, 0, tracks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(getContext()).inflate(R.layout.item_track, parent, false);
        }
        Track track = getItem(position);
        if (track != null) {
            TrackRow.from(row).bind(track);
        }
        return row;
    }
}
