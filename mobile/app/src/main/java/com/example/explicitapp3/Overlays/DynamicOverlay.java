package com.example.explicitapp3.Overlays;

import static androidx.appcompat.content.res.AppCompatResources.getDrawable;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.example.explicitapp3.R;
import com.example.explicitapp3.Types.DetectionResult;

import java.util.List;

/**
 * Dynamic overlay inherits views. For every N detected objects in the image,
 * there would be N instance of Canvas created, with designated coordinates of each object
 */
public class DynamicOverlay extends View {
    Context mcontext;

    Paint paint;
    Paint textPaint;
    Paint blurPaint;
    List<DetectionResult> detectionResultList;

    public DynamicOverlay(Context mcontext) {
        super(mcontext);
        this.mcontext = mcontext;
        init();
    }

    public DynamicOverlay(Context mcontext, AttributeSet attributeSet) {
        super(mcontext, attributeSet);
        this.mcontext = mcontext;
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(40f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        blurPaint = new Paint();
        blurPaint.setColor(Color.BLACK);
        blurPaint.setAlpha(200);

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setResults(List<DetectionResult> detectionResultList) {
        this.detectionResultList = detectionResultList;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (detectionResultList == null) return;
        int padding = 10;

        for (DetectionResult dr : detectionResultList) {
            float l = 0f - padding;
            float t = 0f - padding;
            float r = 0f + padding;
            float b = 0f + padding;

            if (dr.modelType == 0) {
                l += dr.left * getWidth();
                t += dr.top * getHeight();
                r += dr.right * getWidth();
                b += dr.bottom * getHeight();
            } else {
                l += dr.left * getWidth();
                t += dr.top * getHeight() - 90;
                r += dr.right * getWidth();
                b += dr.bottom * getHeight() - 30;
            }
            RectF rectF = new RectF(l, t, r, b);

            drawBlur(canvas, rectF);
            canvas.drawRect(rectF, paint);

            String tt = dr.modelType == 0 ? dr.label + " " + String.format("%.2f", dr.confidence) : ".";
            canvas.drawText(tt, rectF.left, rectF.top, textPaint);
        }
    }

    private void drawBlur(Canvas canvas, RectF rectF) {
        // Simple overlay blur
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.BLACK);
        overlayPaint.setAlpha(180);
        canvas.drawRect(rectF, overlayPaint);

        float pixelSize = 15f;
        Paint pixelPaint = new Paint();

        for (float x = rectF.left; x < rectF.right; x += pixelSize) {
            for (float y = rectF.top; y < rectF.bottom; y += pixelSize) {
                int grayValue = (int) (Math.random() * 100) + 100;
                pixelPaint.setColor(Color.rgb(grayValue, grayValue, grayValue));
                pixelPaint.setAlpha(120);

                canvas.drawRect(x, y,
                        Math.min(x + pixelSize, rectF.right),
                        Math.min(y + pixelSize, rectF.bottom),
                        pixelPaint);
            }
        }
    }
}
