package com.example.explicitapp3.Overlays;

import static androidx.appcompat.content.res.AppCompatResources.getDrawable;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.example.explicitapp3.R;
import com.example.explicitapp3.Types.DetectionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Dynamic overlay inherits views. For every N detected objects in the image,
 * there would be N instance of Canvas created, with designated coordinates of each object
 */
public class DynamicView {
    private static class TrackedBox {
        DetectionResult dr;
        long lastSeen;
        int misses;

        TrackedBox(DetectionResult dr, long t) {
            this.dr = dr;
            this.lastSeen = t;
            this.misses = 0;
        }
    }

    Context mcontext;
    int screenWidth;
    int screenHeight;
    WindowManager wm;

    List<TrackedBox> previousDetections = new ArrayList<>();
    Map<TrackedBox, View> overlayMap = new HashMap<>();

    long DETECTION_PERSIST_MS = 2000;
    int MAX_MISSES = 20;
    float IOU_THRESHOLD = 0.45f;

    public DynamicView(Context mcontext, WindowManager wm) {
        this.mcontext = mcontext;
        this.wm = wm;

        WindowMetrics metrics = wm.getCurrentWindowMetrics();
        Rect bounds = metrics.getBounds();
        this.screenWidth = bounds.width();
        this.screenHeight = bounds.height();
    }

    public void updateDetections(List<DetectionResult> newDetections) {
        long now = System.currentTimeMillis();

        // update tracking (existing + new + removed)
        List<DetectionResult> tracked = updateTracking(newDetections, now);

        // create or update overlays for current tracked boxes
        for (TrackedBox tb : previousDetections) {
            if (!overlayMap.containsKey(tb)) {
                // new tracked box → create overlay
                View boxView = createOverlayView(tb.dr);
                overlayMap.put(tb, boxView);
            } else {
                // update existing overlay position
                updateOverlayView(tb);
            }
        }

        // remove overlays for boxes that no longer exist
        overlayMap.entrySet().removeIf(entry -> {
            TrackedBox tb = entry.getKey();
            if (!previousDetections.contains(tb)) {
                wm.removeView(entry.getValue());
                return true; // remove from map
            }
            return false;
        });
    }

    private View createOverlayView(DetectionResult dr) {
        View boxView = new View(mcontext);
        boxView.setBackgroundColor(Color.argb(200, 0, 0, 0)); // solid black block

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int) ((dr.right - dr.left) * screenWidth),
                (int) ((dr.bottom - dr.top) * screenHeight),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE
        );

        params.x = (int) (dr.left * screenWidth);
        params.y = (int) (dr.top * screenHeight);
        params.gravity = Gravity.TOP | Gravity.START;

        // make only that area untouchable
        boxView.setOnTouchListener((v, event) -> true);

        wm.addView(boxView, params);
        return boxView;
    }

    private void updateOverlayView(TrackedBox tb) {
        View v = overlayMap.get(tb);
        if (v == null) return;

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();
        DetectionResult dr = tb.dr;

        params.x = (int) (dr.left * screenWidth);
        params.y = (int) (dr.top * screenHeight);
        params.width = (int) ((dr.right - dr.left) * screenWidth);
        params.height = (int) ((dr.bottom - dr.top) * screenHeight);

        wm.updateViewLayout(v, params);
    }

    private List<DetectionResult> updateTracking(List<DetectionResult> dt, long now) {
        if (!dt.isEmpty()) {
            boolean[] matched = new boolean[dt.size()];

            for (TrackedBox tb : previousDetections) {
                int bestIndex = -1;
                float bestIou = 0f;

                for (int i = 0; i < dt.size(); i++) {
                    if (matched[i]) continue;
                    DetectionResult c = dt.get(i);
                    if (tb.dr.modelType != c.modelType) continue;

                    float iou = iou(tb.dr, c);
                    if (iou > bestIou) {
                        bestIou = iou;
                        bestIndex = i;
                    }
                }

                if (bestIndex >= 0 && bestIou >= IOU_THRESHOLD) {
                    tb.dr = dt.get(bestIndex);
                    tb.lastSeen = now;
                    tb.misses = 0;
                    matched[bestIndex] = true;
                } else {
                    tb.misses++;
                }
            }

            for (int i = 0; i < dt.size(); i++) {
                if (!matched[i]) {
                    previousDetections.add(new TrackedBox(dt.get(i), now));
                }
            }

        } else {
            for (TrackedBox b : previousDetections) {
                b.misses++;
            }
        }

        Iterator<TrackedBox> it = previousDetections.iterator();
        while (it.hasNext()) {
            TrackedBox b = it.next();
            long age = now - b.lastSeen;
            if (b.misses > MAX_MISSES || age > DETECTION_PERSIST_MS) {
                // remove overlay immediately when tracking is lost
                if (overlayMap.containsKey(b)) {
                    wm.removeView(overlayMap.get(b));
                    overlayMap.remove(b);
                }
                it.remove();
            }
        }

        List<DetectionResult> res = new ArrayList<>();
        for (TrackedBox tb : previousDetections) res.add(tb.dr);
        return res;
    }

    private float iou(DetectionResult a, DetectionResult b) {
        float interLeft = Math.max(a.left, b.left);
        float interTop = Math.max(a.top, b.top);
        float interRight = Math.min(a.right, b.right);
        float interBottom = Math.min(a.bottom, b.bottom);
        float interW = Math.max(0f, interRight - interLeft);
        float interH = Math.max(0f, interBottom - interTop);
        float interArea = interW * interH;
        if (interArea <= 0f) return 0f;

        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);
        return interArea / (areaA + areaB - interArea + 1e-6f);
    }
    public void clearDetectionOverlays(){
        for (View v : overlayMap.values()){
            wm.removeView(v);
        }
    }
}

