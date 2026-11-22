package id.neotica.orpheum.ui.components;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

/**
 * Draws the loaded bitmap as a circle while keeping a real intrinsic size
 * ({@link BitmapDrawable} derives it from the bitmap). That lets an
 * {@code ImageView} with {@code scaleType="matrix"} apply rotations — UIL's
 * built-in rounded drawable has no intrinsic size, so the matrix is ignored.
 */
public class CircularBitmapDisplayer implements BitmapDisplayer {

    public CircularBitmapDisplayer() { }

    @Override
    public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
        imageAware.setImageDrawable(new CircleDrawable(bitmap));
    }

    private static class CircleDrawable extends BitmapDrawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        CircleDrawable(Bitmap bitmap) {
            super(bitmap);
            paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float centerX = (bounds.left + bounds.right) / 2f;
            float centerY = (bounds.top + bounds.bottom) / 2f;
            float radius = Math.min(bounds.width(), bounds.height()) / 2f;
            canvas.drawCircle(centerX, centerY, radius, paint);
        }
    }
}
