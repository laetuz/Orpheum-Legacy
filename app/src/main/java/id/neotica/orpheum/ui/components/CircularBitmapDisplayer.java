package id.neotica.orpheum.ui.components;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

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
    public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
        imageAware.setImageDrawable(new CircleDrawable(bitmap, targetSizePx));
    }

    private static class CircleDrawable extends BitmapDrawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final BitmapShader shader;
        private final int targetSizePx;

        CircleDrawable(Bitmap bitmap, int targetSizePx) {
            super(bitmap);
            this.targetSizePx = targetSizePx;
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);
        }

        @Override
        public int getIntrinsicWidth() {
            return targetSizePx;
        }

        @Override
        public int getIntrinsicHeight() {
            return targetSizePx;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            Bitmap bitmap = getBitmap();
            if (bitmap == null || bounds.width() <= 0 || bounds.height() <= 0) {
                return;
            }

            float scale = Math.max(
                    (float) bounds.width() / bitmap.getWidth(),
                    (float) bounds.height() / bitmap.getHeight());
            float dx = bounds.left + (bounds.width() - bitmap.getWidth() * scale) / 2f;
            float dy = bounds.top + (bounds.height() - bitmap.getHeight() * scale) / 2f;

            Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            shader.setLocalMatrix(matrix);
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
