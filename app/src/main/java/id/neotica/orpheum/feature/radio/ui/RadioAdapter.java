package id.neotica.orpheum.feature.radio.ui;

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
import id.neotica.orpheum.feature.radio.FavoriteStationStore;
import id.neotica.orpheum.feature.radio.domain.RadioItem;
import id.neotica.orpheum.utils.ImageUrlHelper;

public class RadioAdapter extends ArrayAdapter<RadioItem> {

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
    public View getView(int position, View convertView, ViewGroup parent) {
        RadioItem item = getItem(position);

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

            boolean favorite = item.type == RadioItem.TYPE_STATION
                    && FavoriteStationStore.isFavorite(item.guideId);
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
