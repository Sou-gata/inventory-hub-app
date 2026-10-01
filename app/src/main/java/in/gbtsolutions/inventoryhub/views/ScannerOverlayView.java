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
    private Paint backgroundPaint;
    private Paint borderPaint;
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
        borderPaint.setColor(Color.RED);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8f); // Thickness of the border
        borderPaint.setAntiAlias(true);

        overlayPath = new Path();

        cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f, // Change this value to make corners rounder or sharper
                context.getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int boxSize = (int) (Math.min(width, height) * 0.70);

        float left = (width - boxSize) / 2f;
        float top = (height - boxSize) / 2f;
        float right = left + boxSize;
        float bottom = top + boxSize;

        overlayPath.reset();

        overlayPath.addRect(0, 0, width, height, Path.Direction.CW);

        overlayPath.addRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, Path.Direction.CCW);

        canvas.drawPath(overlayPath, backgroundPaint);

        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, borderPaint);
    }
}

