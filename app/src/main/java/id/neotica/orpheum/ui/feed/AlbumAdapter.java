package id.neotica.orpheum.ui.feed;

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

import id.neotica.orpheum.BuildConfig;
import id.neotica.orpheum.R;
import id.neotica.orpheum.model.Album;

public class AlbumAdapter extends ArrayAdapter<Album> {

    public AlbumAdapter(Context context, List<Album> albums) {
        super(context, 0, albums);
    }

    private static class ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvYear;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Album album = getItem(position);

        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_album, parent, false);
            vh.ivCover = (ImageView) convertView.findViewById(R.id.iv_album_cover);
            vh.tvTitle = (TextView) convertView.findViewById(R.id.tv_album_title);
            vh.tvArtist = (TextView) convertView.findViewById(R.id.tv_album_artist);
            vh.tvYear = (TextView) convertView.findViewById(R.id.tv_album_year);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        if (album != null) {
            vh.tvTitle.setText(album.title);
            if (!TextUtils.isEmpty(album.artistName)) {
                vh.tvArtist.setText(album.artistName);
            } else {
                vh.tvArtist.setText("");
            }
            vh.tvYear.setText(String.valueOf(album.releaseYear));

            if (!TextUtils.isEmpty(album.coverUrl)) {
                String fullUrl = BuildConfig.FILE_BASE_URL + "/buckets/" + album.coverUrl;
                ImageLoader.getInstance().displayImage(fullUrl, vh.ivCover);
            } else {
                ImageLoader.getInstance().cancelDisplayTask(vh.ivCover);
                vh.ivCover.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        return convertView;
    }
}
