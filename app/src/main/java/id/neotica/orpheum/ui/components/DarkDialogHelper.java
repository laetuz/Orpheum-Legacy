package id.neotica.orpheum.ui.components;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import id.neotica.orpheum.R;

/**
 * Fully self-contained dark dialog (no framework alert-dialog theming), used for
 * the player queue and favorite-removal menus so OEM light dialog themes cannot
 * make the text unreadable.
 */
public final class DarkDialogHelper {

    private DarkDialogHelper() { }

    public static Dialog showList(final Activity activity, String title, String[] items,
                                  final DialogInterface.OnClickListener listener) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_dark_list);
        dialog.setCancelable(true);

        TextView tvTitle = (TextView) dialog.findViewById(R.id.tv_dialog_title);
        tvTitle.setText(title);

        ListView list = (ListView) dialog.findViewById(R.id.lv_dialog_items);
        list.setCacheColorHint(0);
        list.setAdapter(new ArrayAdapter<String>(
                activity, R.layout.item_dialog_dark, R.id.tv_dialog_item, items));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onClick(dialog, position);
                }
            }
        });

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int rowHeight = (int) (48 * metrics.density);
        int maxHeight = (int) (metrics.heightPixels * 0.55f);
        list.getLayoutParams().height = Math.min(Math.max(items.length, 1) * rowHeight, maxHeight);

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout((int) (metrics.widthPixels * 0.9f),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }
}
