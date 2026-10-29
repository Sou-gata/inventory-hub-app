package in.gbtsolutions.inventoryhub.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScannerOverlayView extends View {
    public static final int MODE_QR = 0;
    public static final int MODE_BARCODE = 1;

    private int mode = MODE_QR;
    private Paint backgroundPaint;
    private Paint borderPaint;
    private Paint laserPaint;
    private Path overlayPath;
    private float cornerRadius;

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#99000000")); // semi transparent black
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#3B82F6")); // Blue border
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setAntiAlias(true);

        laserPaint = new Paint();
        laserPaint.setColor(Color.parseColor("#EF4444")); // Red laser line
        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setStrokeWidth(4f);
        laserPaint.setAntiAlias(true);

        overlayPath = new Path();

        cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f,
                context.getResources().getDisplayMetrics());
    }

    public void setMode(int mode) {
        if (this.mode != mode) {
            this.mode = mode;
            invalidate();
        }
    }

    public int getMode() {
        return mode;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float left, top, right, bottom;

        if (mode == MODE_BARCODE) {
            float rectWidth = width * 0.88f;
            float rectHeight = Math.min(height * 0.32f, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 170f, getResources().getDisplayMetrics()));
            left = (width - rectWidth) / 2f;
            top = (height - rectHeight) / 2f;
            right = left + rectWidth;
            bottom = top + rectHeight;
        } else {
            int boxSize = (int) (Math.min(width, height) * 0.72);
            left = (width - boxSize) / 2f;
            top = (height - boxSize) / 2f;
            right = left + boxSize;
            bottom = top + boxSize;
        }

        overlayPath.reset();
        overlayPath.addRect(0, 0, width, height, Path.Direction.CW);
        overlayPath.addRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, Path.Direction.CCW);

        canvas.drawPath(overlayPath, backgroundPaint);
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, borderPaint);

        if (mode == MODE_BARCODE) {
            // Draw center laser guide line for 1D barcode alignment
            float centerY = (top + bottom) / 2f;
            canvas.drawLine(left + 24, centerY, right - 24, centerY, laserPaint);
        }
    }
}
