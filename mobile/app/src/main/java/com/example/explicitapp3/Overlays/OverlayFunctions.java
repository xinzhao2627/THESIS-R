package com.example.explicitapp3.Overlays;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.example.explicitapp3.NotificationHelper;
import com.example.explicitapp3.Detectors.DistilBERT_tagalog_Detector;
import com.example.explicitapp3.Parent.ImageModel;
import com.example.explicitapp3.Parent.TextModel;
import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OverlayFunctions {
    private MediaProjectionManager mediaProjectionManager;
    WindowManager wm;
    private MediaProjection mediaProjection;
    public Notification notification;
    Surface surface;
    VirtualDisplay mVirtualDisplay;
    Context mcontext;
    int dpi;
    private long lastProcessTime = 0;
    private static final long PROCESS_INTERVAL_MS = 180;
    ImageModel imageModel;
    // these are for screenshot
    ImageReader imageReader;
    Handler handler;
    HandlerThread handlerThread;
    DynamicOverlay dynamicOverlay;
    TextModel textModel;
    private static final String TAG = "OverlayFunctions";

    List<TrackedBox> previousDetections = new ArrayList<>();
    long DETECTION_PERSIST_MS = 1000;
    int MAX_MISSES = 20;
    float IOU_THRESHOLD = 0.45f;

    DynamicView dynamicView;
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

    /**
     * Initializes and configures the foreground service notification.
     * Also initializes the text model for content analysis.
     *
     * @param context Application context {@code Context context = getApplicationContext()}
     *                You cannot fetch context on a extend service class as to why you need to import it
     * @return The created notification instance
     * @throws IOException If text model initialization fails
     * @see NotificationHelper#createNotificationChannel(Context)
     * @see DistilBERT_tagalog_Detector (Context, String)
     */
    public Notification setNotification(Context context) throws IOException {
        NotificationHelper.createNotificationChannel(context);
        notification = NotificationHelper.createNotification(context);
        mcontext = context;
        return notification;
    }

    public void setup(WindowManager windowManager, int idpi, MediaProjection mp, MediaProjectionManager mpm) {
        this.wm = windowManager;
        this.dpi = idpi;
        this.mediaProjection = mp;
        this.mediaProjectionManager = mpm;
        initImageReader();
    }

    public void initRecorder() {
        setupDynamicOverlay();
        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopScreenCapture();
            }
        };
        mediaProjection.registerCallback(callback, null);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        int[] boundsRes = getBounds();
        int width = boundsRes[0];
        int height = boundsRes[1];

        mVirtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width == 0 ? DisplayMetrics.DENSITY_DEFAULT : width,
                height == 0 ? DisplayMetrics.DENSITY_DEFAULT : height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                handler
        );

    }

    public void initImageReader() {
        handlerThread = new HandlerThread("ImageProcessor");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        int[] boundsRes = getBounds();
        imageReader = ImageReader.newInstance(
                boundsRes[0],
                boundsRes[1],
                PixelFormat.RGBA_8888,
                56
        );
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = imageToBitmap(image);
                    processImageAsync(bitmap);
                    image.close();
                    bitmap.recycle();
                }
            }
        }, handler);
        surface = imageReader.getSurface();
        initRecorder();
    }

    public void setupDynamicOverlay() {
        if (dynamicView == null){
            dynamicView = new DynamicView(mcontext, wm);
        }

//        dynamicOverlay = new DynamicOverlay(mcontext);
//        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//                ,
//                PixelFormat.TRANSLUCENT
//        );
//        wm.addView(dynamicOverlay, params);
    }

    private void processImageAsync(Bitmap bitmap) {
        long startTime = System.currentTimeMillis();
        List<DetectionResult> dt = new ArrayList<>();
        if (imageModel != null) {
            ClassifyResults res = imageModel.detect(bitmap);
            dt.addAll(res.detectionResults);
        }
        if (textModel != null) {
            List<DetectionResult> dt_text = textModel.detect(bitmap);
            dt.addAll(dt_text);
        }
//        List<DetectionResult> toBlur = updateTracking(dt, startTime);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // display faster??
//        if (dynamicOverlay != null) {
//            new Handler(mcontext.getMainLooper()).post(() -> {
//                dynamicOverlay.setResults(toBlur);
//            });
//        }

        if (dynamicView != null) {
            new Handler(mcontext.getMainLooper()).post(() -> {

                if (dynamicView != null){
                    while (dt.size() > 4) {
                        dt.remove(dt.size() - 1);
                    }
                    dynamicView.updateDetections(dt);
                }
//                dynamicView.clearDetectionOverlays();
//                dynamicView.setAllView();
            });
        }
    }

    private List<DetectionResult> updateTracking(List<DetectionResult> dt, long now) {

        if (!dt.isEmpty()) {
            // this var tracks all the new overlaps, we dont include overlaps
            boolean[] matched = new boolean[dt.size()];

            for (TrackedBox tb : previousDetections) {
                int bestIndex = -1;
                float bestIou = 0f;

                // loop all the new objects and find the highest iou
                for (int i = 0; i < dt.size(); i++) {
                    if (matched[i]) continue;
                    DetectionResult c = dt.get(i);
                    // skip if we are comparing image to text
                    if (tb.dr.modelType != c.modelType) continue;

                    // a high iou means one of the old object
                    // is similar to the new one
                    float iou = iou(tb.dr, c);
                    if (iou > bestIou) {
                        bestIou = iou;
                        bestIndex = i;
                    }
                }
                // check the highest iou if it overlaps with one
                // of the existing objects
                if (bestIndex >= 0 && bestIou >= IOU_THRESHOLD) {
                    // if it is then replace the existing
                    tb.dr = dt.get(bestIndex);
                    tb.lastSeen = now;
                    tb.misses = 0;
                    // then mark that object, meaning it has already been integrated
                    matched[bestIndex] = true;
                } else {
                    tb.misses++;
                }
            }


            for (int i = 0; i < dt.size(); i++) {
                // add only new detections if it does not overlap on previous ones
                if (!matched[i]) {
                    previousDetections.add(new TrackedBox(dt.get(i), now));
                }
            }

        } else {
            // else just update the misses
            for (TrackedBox b : previousDetections) {
                b.misses++;
            }
        }

        // final check for outdated objects
        Iterator<TrackedBox> it = previousDetections.iterator();
        while (it.hasNext()) {
            TrackedBox b = it.next();
            long age = now - b.lastSeen;
            // if object last long enough remove it
            if (b.misses > MAX_MISSES || age > DETECTION_PERSIST_MS) {
                it.remove();
            }
        }


        // the list of all detected objects that remained from previous
        List<DetectionResult> res = new ArrayList<>();
        for (TrackedBox tb : previousDetections) {
            res.add(tb.dr);
        }
        return res;
    }

    private float iou(DetectionResult a, DetectionResult b) {
        float ax1 = a.left;
        float ay1 = a.top;
        float ax2 = a.right;
        float ay2 = a.bottom;

        float bx1 = b.left;
        float by1 = b.top;
        float bx2 = b.right;
        float by2 = b.bottom;

        float interLeft = Math.max(ax1, bx1);
        float interTop = Math.max(ay1, by1);
        float interRight = Math.min(ax2, bx2);
        float interBottom = Math.min(ay2, by2);
        float interW = Math.max(0f, interRight - interLeft);
        float interH = Math.max(0f, interBottom - interTop);
        float interArea = interW * interH;
        if (interArea <= 0f) return 0f;

        float areaA = (ax2 - ax1) * (ay2 - ay1);
        float areaB = (bx2 - bx1) * (by2 - by1);
        return interArea / (areaA + areaB - interArea + 1e-6f);
    }

    /**
     * Converts an Android Image object to a Bitmap for processing.
     * Reconstruict the image using pixelstride.
     *
     * @param image The source Image from ImageReader
     * @return Bitmap of the image
     * @see Image.Plane
     */
    public Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        int bitmapWidth = image.getWidth() + rowPadding / pixelStride;
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    /**
     * Saves a bitmap image to the device gallery using the MediaStore API.
     *
     * @param bitmap The bitmap to save to gallery
     * @throws RuntimeException If save operation fails
     * @see MediaStore.Images.Media#EXTERNAL_CONTENT_URI
     * @see ContentResolver#insert(Uri, ContentValues)
     */
    public void saveToGalleryBitmap(Bitmap bitmap) {
        // can configure the bitmap here
        if (bitmap != null) {
            try {
                // you need content resolver in android 15
                ContentResolver resolver = mcontext.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "as_screenshot_" + System.currentTimeMillis());
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                        if (outputStream == null) {
                            throw new IOException("Failed to open output stream");
                        }
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                            throw new IOException("Failed to save bitmap");
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "onSurfaceTextureUpdated: IOError " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Performs cleanup of all resources used by OverlayFunctions and OverlayService.
     * This method should be called when the service is stopping to prevent memory leaks.
     *
     * <p>The operation itself consist of the following:
     * <ul>
     *   <li>Stops screen capture and releases virtual display</li>
     *   <li>Removes overlay views from window manager</li>
     *   <li>Shut down background threads</li>
     *   <li>Closes ImageReader and releases surface</li>
     *   <li>Cleans up text model resources</li>
     * </ul>
     * This needs to be doe to avoid memory leaks on app onClose and onShutdown
     *
     * @apiNote This method is safe to call multiple times
     * @see #stopScreenCapture()
     * @see HandlerThread#quitSafely()
     * @see DistilBERT_tagalog_Detector#cleanup()
     */
    public void destroy() {
        stopScreenCapture();
        if (wm != null) {
            try {
                if (dynamicOverlay != null) {
                    wm.removeView(dynamicOverlay);
                    dynamicOverlay = null;
                }
            } catch (Exception e) {
                Log.e("OverlayService", "Error removing overlay view: " + e.getMessage());
            }
            wm = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handlerThread = null;
            handler = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (textModel != null) {
            textModel.cleanup();
            textModel = null;
        }

        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (imageModel != null) {
            imageModel.cleanup();
            imageModel = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (previousDetections != null) {
            previousDetections.clear();
        }

        if (dynamicView != null) {
            dynamicView.clearDetectionOverlays();
            dynamicView = null;
        }
    }

    public void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    /**
     * It may initialize the TensorFlow Lite model.
     * NOTE: this may fail if you did not create an asset folder for models
     *
     * @throws IOException If the model file cannot be loaded or is corrupted
     * @apiNote Currently uses the exported NSFW model with metadata
     */

    public void initModel(String textDetector, String imageDetector) throws IOException {
        boolean isID = imageDetector.equals(ModelTypes.YOLO_V10_F32) || imageDetector.equals(ModelTypes.YOLO_V10_F16) || imageDetector.equals(ModelTypes.MOBILENET_SSD);

        boolean isTD = textDetector.equals(ModelTypes.LSTM)
                || textDetector.equals(ModelTypes.NaiveBayes)
                || textDetector.equals(ModelTypes.LogisticRegression)
                || textDetector.equals(ModelTypes.ROBERTA_TAGALOG)
                || textDetector.equals(ModelTypes.DISTILBERT_TAGALOG)
                || textDetector.equals(ModelTypes.SVM);
        if (isID){
            imageModel = new ImageModel(mcontext, imageDetector);
        }
        if (isTD){
            textModel = new TextModel(mcontext, textDetector);

        }
    }

    /**
     * Retrieves the dimensions that will be used to set the regions for the virtual display.
     *
     * @return Array containing [width, height] of the current display
     * @see WindowManager#getCurrentWindowMetrics()
     */
    public int[] getBounds() {
        WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
        Rect bounds = windowMetrics.getBounds();
        int width = bounds.width();
        int height = bounds.height();
        return new int[]{width, height};
    }
}
