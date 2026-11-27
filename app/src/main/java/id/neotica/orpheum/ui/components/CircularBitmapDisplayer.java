package id.neotica.orpheum.ui.components;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.display.BitmapDisplayer;

/**
 * Draws the loaded bitmap as a circle pinned to a fixed intrinsic size so an
 * {@code ImageView} with {@code scaleType="matrix"} keeps its bounds (and can
 * rotate it). The bitmap itself is center-cropped into those bounds, so a
 * decoded image larger than the view no longer overflows.
 */
public class CircularBitmapDisplayer implements BitmapDisplayer {

    private final int targetSizePx;

    public CircularBitmapDisplayer(int targetSizePx) {
        this.targetSizePx = targetSizePx;
    }

    @Override
    public Bitmap display(Bitmap bitmap, ImageView imageView) {
        Bitmap circle = toCircle(bitmap, targetSizePx);
        imageView.setImageBitmap(circle);
        return circle;
    }

    public static Bitmap toCircle(Bitmap bitmap, int targetSizePx) {
        Bitmap output = Bitmap.createBitmap(targetSizePx, targetSizePx,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);

        float scale = Math.max(
                (float) targetSizePx / bitmap.getWidth(),
                (float) targetSizePx / bitmap.getHeight());
        float dx = (targetSizePx - bitmap.getWidth() * scale) / 2f;
        float dy = (targetSizePx - bitmap.getHeight() * scale) / 2f;

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);

        float radius = targetSizePx / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }
}
