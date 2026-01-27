package com.example.explicit_litert.Overlays;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.explicit_litert.Types.DetectionResult;
import com.example.explicit_litert.Types.ModelTypes;

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
    private static final float MODEL1_SCALE = 1.9f;
    Context mcontext;
    int screenWidth;
    int screenHeight;
    WindowManager wm;

    FrameLayout.LayoutParams labelParams;
    List<TrackedBox> previousDetections = new ArrayList<>();
    Map<TrackedBox, View> overlayMap = new HashMap<>();

    long DETECTION_PERSIST_MS = 2000;
    int MAX_MISSES = 5;
    float IOU_THRESHOLD = 0.6f;

    String imageModelName;
    String textModelName;


//    private View colorSquare;
//    private WindowManager.LayoutParams squareParams;
//    private final int[] colors = {
//            Color.RED,
//            Color.GREEN,
//            Color.BLUE,
//            Color.YELLOW,
//            Color.MAGENTA,
//            Color.CYAN
//    };
//    private int colorIndex = 0;

    public DynamicView(Context mcontext, WindowManager wm, String imageModelName, String textModelName) {
        this.mcontext = mcontext;
        this.wm = wm;
        this.imageModelName = imageModelName;
        this.textModelName = textModelName;
        WindowMetrics metrics = wm.getCurrentWindowMetrics();
        Rect bounds = metrics.getBounds();
        this.screenWidth = bounds.width();
        this.screenHeight = bounds.height();

        labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START
        );
//        createColorSquare();

    }
    //    private void createColorSquare() {
//        colorSquare = new View(mcontext);
//
//        int sizePx = 40; // square size
//        colorSquare.setBackgroundColor(colors[0]);
//
//        squareParams = new WindowManager.LayoutParams(
//                sizePx,
//                sizePx,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//                PixelFormat.TRANSLUCENT
//        );
//
//        squareParams.gravity = Gravity.TOP | Gravity.START;
//        squareParams.x = screenWidth / 2 - sizePx / 2;
//        squareParams.y = screenHeight / 2 - sizePx / 2;
//
//
//        wm.addView(colorSquare, squareParams);
//        startColorRandomizer();
//
//    }
//    private Handler colorHandler = new Handler(Looper.getMainLooper());
//    private Runnable colorRunnable;
//    private final Random random = new Random();
//    private void startColorRandomizer() {
//        colorRunnable = new Runnable() {
//            @Override
//            public void run() {
//                int color = Color.rgb(
//                        random.nextInt(256),
//                        random.nextInt(256),
//                        random.nextInt(256)
//                );
//
//                if (colorSquare != null) {
//                    colorSquare.setBackgroundColor(color);
//                    colorHandler.postDelayed(this, 300); // change every 300ms
//                }
//            }
//        };
//
//        colorHandler.post(colorRunnable);
//    }
//    move all previous detection's view upward or downard depending on the Y-offset:
    public void moveDetections(float offset){
//        iterate all existing overlays
        for (Map.Entry<TrackedBox, View> ovl: overlayMap.entrySet()){
            View v = ovl.getValue();
            v.setTranslationY(v.getTranslationY() + offset);

        }
    }

    public void updateDetections(List<DetectionResult> newDetections) {
        long now = System.currentTimeMillis();

        // update tracking (existing + new + removed)
        List<DetectionResult> tracked = updateTracking(newDetections, now);

        // create or update overlays for current tracked boxes
        for (TrackedBox tb : previousDetections) {
            if (!overlayMap.containsKey(tb)) {
                // new tracked box to create overlay
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
                View v = entry.getValue();

                // Clear all references before removal
                if (v instanceof FrameLayout) {
                    FrameLayout frame = (FrameLayout) v;
                    frame.removeAllViews();
                    frame.setBackground(null);
                }

                try {
                    wm.removeView(v);
                } catch (IllegalArgumentException e) {
                    // Already removed
                    Log.i("dynamicview", "already removed wm");
                }

                return true;
            }
            return false;
        });
        Log.i("updateDetection", "updateDetections: "+ (System.currentTimeMillis()-now)+"ms");
    }

    // initialize text design
    private TextView setText(DetectionResult dr){
        TextView labelView = new TextView(mcontext);
        labelView.setText(dr.label + " " + String.format("%.2f", dr.confidence));
        labelView.setTextColor(Color.WHITE);
        labelView.setTextSize(12);
        labelView.setPadding(6, 4, 6, 4);
        labelView.setBackgroundColor(Color.argb(180, 30, 30, 30)); // black bg
        return labelView;
    }
    private View createOverlayView(DetectionResult dr) {
        // Outer container (handles position and border)
        FrameLayout boxContainer = new FrameLayout(mcontext);
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(Color.argb(200, 0, 0, 0));
        boxBg.setStroke(4, Color.RED);
        boxContainer.setBackground(boxBg);
//        boxContainer.setBackground(boxBackground);

        TextView labelView = setText(dr);

        // add text in box
        boxContainer.addView(labelView, labelParams);

        // coordinates
        float width = (dr.right - dr.left) * screenWidth;
        float height = (dr.bottom - dr.top) * screenHeight;
        float x = dr.left * screenWidth;
        float y = dr.top * screenHeight;

//        if (imageModelName == ModelTypes.MOBILENET_SSD) {
//
//            float leftPx   = dr.left   * screenWidth;
//            float topPx    = dr.top    * screenHeight;
//            float rightPx  = dr.right  * screenWidth;
//            float bottomPx = dr.bottom * screenHeight;
//
//            width  = rightPx - leftPx;
//            height = bottomPx - topPx;
//            x = leftPx;
//            y = topPx;
//        }

        // enlarge if modelType == 1
//        if (dr.modelType == 1) {
//            float extraH = (height * (MODEL1_SCALE - 1)) / 2;
//            y -= extraH;
//            height *= MODEL1_SCALE;
//        }

        // relocate box
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int) width,
                (int) height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE
        );

        params.x = (int) x;
        params.y = (int) y;
        params.gravity = Gravity.TOP | Gravity.START;
        // show
        wm.addView(boxContainer, params);
        return boxContainer;
    }


    private void updateOverlayView(TrackedBox tb) {
        View v = overlayMap.get(tb);
        if (v == null) return;
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) v.getLayoutParams();
        DetectionResult dr = tb.dr;

        float width = (dr.right - dr.left) * screenWidth;
        float height = (dr.bottom - dr.top) * screenHeight;
        float x = dr.left * screenWidth;
        float y = dr.top * screenHeight;

        // make box taller if modelType == 1
//        if (dr.modelType == 1) {
//            float extraH = (height * (MODEL1_SCALE - 1)) / 2;
//            y -= extraH;
//            height *= MODEL1_SCALE;
//        }

        params.x = (int) x;
        params.y = (int) y;
        params.width = (int) width;
        params.height = (int) height;
        wm.updateViewLayout(v, params);
    }
    private void removeOverlay(TrackedBox tb) {
        View v = overlayMap.get(tb);
        if (v != null) {
            // Clear background to release Drawable
            if (v instanceof FrameLayout) {
                FrameLayout frame = (FrameLayout) v;
                if (frame.getChildCount() > 0) {
                    TextView label = (TextView) frame.getChildAt(0);
                    label.setBackground(null);
                }
                frame.setBackground(null);
            }
            wm.removeView(v);
        }
    }
    private List<DetectionResult> applyNMS(List<DetectionResult> detections, float nmsThreshold) {
        if (detections.size() <= 1) return detections;

        // Sort by confidence (highest first)
        List<DetectionResult> sorted = new ArrayList<>(detections);
        sorted.sort((a, b) -> Float.compare(b.confidence, a.confidence));

        List<DetectionResult> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (suppressed[i]) continue;

            DetectionResult current = sorted.get(i);
            kept.add(current);

            // Suppress overlapping boxes with lower confidence
            for (int j = i + 1; j < sorted.size(); j++) {
                if (suppressed[j]) continue;

                DetectionResult candidate = sorted.get(j);

                // Only suppress if same class (nsfw vs nsfw, safe vs safe)
                if (current.classId == candidate.classId) {
                    float overlap = iou(current, candidate);
                    if (overlap > nmsThreshold) {
                        suppressed[j] = true;
                        Log.d("NMS", String.format("Suppressed box %s (%.2f) due to overlap %.2f with %s (%.2f)",
                                candidate.label, candidate.confidence, overlap,
                                current.label, current.confidence));
                    }
                }
            }
        }

        Log.i("NMS", String.format("Filtered %d → %d boxes", detections.size(), kept.size()));
        return kept;
    }
    //    dt is the current list of detection results
//    now is just milliseconds tracker
    private List<DetectionResult> updateTracking(List<DetectionResult> dt, long now) {
        if (!dt.isEmpty()) {
//            prevent box overlay through nms
            dt = applyNMS(dt, 0.5f);

//          this is just the iou,
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

//          if a detection result is new, add it in the previous detections
            Log.i("heyheyy", "updateTracking: current prevdec size: " + dt.size()+previousDetections.size());
            for (int i = 0; i < dt.size(); i++) {
                if (!matched[i]) {
//                    previousDetections.add(new TrackedBox(dt.get(i), now));

                    //new
                    DetectionResult newBox = dt.get(i);

                    // Check if this new box overlaps with ANY existing tracked box
                    if (!overlapsWithExisting(newBox)) {
                        previousDetections.add(new TrackedBox(newBox, now));
                        Log.d("Tracking", String.format("Added new box: %s (%.2f)",
                                newBox.label, newBox.confidence));
                    } else {
                        Log.d("Tracking", String.format("Rejected box %s (%.2f) - overlaps with tracked box",
                                newBox.label, newBox.confidence));
                    }
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
//                    wm.removeView(overlayMap.get(b));
                    removeOverlay(b);
                    overlayMap.remove(b);
                }
                it.remove();
            }
        }

        List<DetectionResult> res = new ArrayList<>();
//        for (TrackedBox tb : previousDetections) res.add(tb.dr);
        return res;
    }
    private boolean overlapsWithExisting(DetectionResult newBox) {
        float overlapThreshold = 0.3f; // Lower than IOU_THRESHOLD to be more strict

        for (TrackedBox tracked : previousDetections) {
            // Skip if different model types
            if (tracked.dr.modelType != newBox.modelType) continue;

            float overlap = iou(tracked.dr, newBox);

            if (overlap > overlapThreshold) {
                Log.d("Overlap", String.format("New %s overlaps %.2f with tracked %s",
                        newBox.label, overlap, tracked.dr.label));
                return true;
            }
        }

        return false;
    }
    // n^m
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
            try {
                wm.removeView(v);

            } catch (Exception e) {
                Log.e("DynamicView", "view already removed");
            }
        }
        overlayMap.clear();
        previousDetections.clear();

//        if (colorSquare != null) {
//            wm.removeView(colorSquare);
//            colorSquare = null;
//        }
    }
}
