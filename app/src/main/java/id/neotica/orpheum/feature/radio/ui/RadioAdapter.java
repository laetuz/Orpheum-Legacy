package id.neotica.orpheum.feature.radio.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import android.text.TextUtils;

import java.util.List;

import id.neotica.orpheum.R;
import id.neotica.orpheum.feature.radio.FavoriteStationStore;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.utils.ImageUrlHelper;

public class RadioAdapter extends ArrayAdapter<RadioItem> {

    private static final int TYPE_ROW = 0;
    private static final int TYPE_HEADER = 1;

    public RadioAdapter(Context context, List<RadioItem> items) {
        super(context, 0, items);
    }

    private static class ViewHolder {
        ImageView ivLogo;
        ImageView ivFavorite;
        TextView tvName;
        TextView tvDescription;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        RadioItem item = getItem(position);
        return item != null && item.type == RadioItem.TYPE_HEADER ? TYPE_HEADER : TYPE_ROW;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RadioItem item = getItem(position);

        if (item != null && item.type == RadioItem.TYPE_HEADER) {
            TextView header = (TextView) convertView;
            if (header == null) {
                header = (TextView) LayoutInflater.from(getContext())
                        .inflate(R.layout.item_radio_header, parent, false);
            }
            header.setText(item.name);
            return header;
        }

        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_radio, parent, false);
            vh.ivLogo = (ImageView) convertView.findViewById(R.id.iv_radio_logo);
            vh.ivFavorite = (ImageView) convertView.findViewById(R.id.iv_radio_favorite);
            vh.tvName = (TextView) convertView.findViewById(R.id.tv_radio_name);
            vh.tvDescription = (TextView) convertView.findViewById(R.id.tv_radio_description);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        if (item != null) {
            vh.tvName.setText(item.name);
            vh.tvDescription.setText(!TextUtils.isEmpty(item.description) ? item.description : "");

            boolean favoritable = (item.type == RadioItem.TYPE_STATION && item.live)
                    || item.type == RadioItem.TYPE_SHOW;
            boolean favorite = favoritable && FavoriteStationStore.isFavorite(item.guideId);
            vh.ivFavorite.setVisibility(favorite ? View.VISIBLE : View.GONE);

            if (!TextUtils.isEmpty(item.imageUrl)) {
                ImageLoader.getInstance().displayImage(ImageUrlHelper.build(item.imageUrl), vh.ivLogo);
            } else {
                ImageLoader.getInstance().cancelDisplayTask(vh.ivLogo);
                vh.ivLogo.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        return convertView;
    }
}
