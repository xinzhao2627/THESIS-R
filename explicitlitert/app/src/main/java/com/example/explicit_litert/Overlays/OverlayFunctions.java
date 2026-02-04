package com.example.explicit_litert.Overlays;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.example.explicit_litert.NotificationHelper;
//import com.example.explicit_litert.Detectors.DistilBERT_tagalog_Detector;
import com.example.explicit_litert.Parent.ImageModel;
import com.example.explicit_litert.Parent.TextModel;
import com.example.explicit_litert.Types.ClassifyResults;
import com.example.explicit_litert.Types.DetectionResult;
import com.example.explicit_litert.Types.ModelTypes;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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
    Handler handler; //backgrund
    Handler mainHandler; // ui
    HandlerThread handlerThread;
    TextModel textModel;
    private static final String TAG = "OverlayFunctions";
    List<TrackedBox> previousDetections = new ArrayList<>();

    String imageModelName = "";
    String textModelName = "";
    DynamicView dynamicView;
    View overlayView;

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
        final Handler handlerCB = new Handler(Looper.getMainLooper());
        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d("OverlayService", "MediaProjection stopped");
                super.onStop();
                stopScreenCapture();
            }
            @Override
            public void onCapturedContentVisibilityChanged(boolean isVisible) {
                super.onCapturedContentVisibilityChanged(isVisible);
                Log.d(TAG, "Captured content visibility changed: " + isVisible);
                // called when the captured app becomes visible/invisible
                // reset view
                if (!isVisible){
                    handlerCB.postDelayed(()-> {
                        if (dynamicView != null) {
                            dynamicView.clearDetectionOverlays();
                        }

                    }, 1000);
                }
            }

            @Override
            public void onCapturedContentResize(int width, int height) {
                super.onCapturedContentResize(width, height);
                Log.d(TAG, "Captured content resized: " + width + "x" + height);
                // Called when the captured app's size changes
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
        mainHandler = new Handler(Looper.getMainLooper());
        int[] boundsRes = getBounds();
        ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();
        AtomicBoolean isProcessing = new AtomicBoolean(false);
        imageReader = ImageReader.newInstance(
                boundsRes[0],
                boundsRes[1],
                PixelFormat.RGBA_8888,
                2
        );

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                long now = System.currentTimeMillis();
                if (!isProcessing.compareAndSet(false, true)) {
                    // frame skip
                    Image skip = reader.acquireLatestImage();
                    if (skip != null) skip.close();
                    return;
                }

                Image image = reader.acquireLatestImage();
                if (image == null) {
                    isProcessing.set(false);
                    return;
                }


                Bitmap bitmap = imageToBitmap(image);
                image.close();

                inferenceExecutor.execute(() -> {
                    processImage(bitmap);
                    isProcessing.set(false);
                });
            }

        }, handler);

        surface = imageReader.getSurface();
        initRecorder();
    }

    static final float SCROLL_EPS = 1f;
    private Bitmap previousScrollFrame = null;

    private void processImage(Bitmap bitmap) {
////        FOR INDICATION ONLY
//        if (previousScrollFrame != null) {
////            float deltaY = estimateScrollDelta(previousScrollFrame, bitmap);
//            if (isScrolling(previousScrollFrame, bitmap)){
//                Log.i(TAG, "processImage: scrolling...");
//            } else {
//                Log.i(TAG, "processImage: not scrolling..." );
//            }
//            previousScrollFrame.recycle();
//        }
//        // now update the previous
//        previousScrollFrame = bitmap.copy(Bitmap.Config.ARGB_8888, false);

//        Log.i(TAG, "time: \n");
        long now = System.currentTimeMillis();
        List<DetectionResult> dt = new ArrayList<>();
//        Log.i("gpteam", "new");
        if (imageModel != null) {
            ClassifyResults res = imageModel.detect(bitmap);
//            Log.i("gpteam", "processImage: MODELONLY: "+imageModelName+ " : " + (System.currentTimeMillis()-now)+"ms");

//            Log.i(TAG, "model time: " + (System.currentTimeMillis() - now));
            dt.addAll(res.detectionResults);
        }

        if (textModel != null) {
            List<DetectionResult> dt_text = textModel.detect(bitmap);
            dt.addAll(dt_text);
        }
        // show the boxes
        mainHandler.post(() -> handleUI(dt));
        bitmap.recycle();
//        Log.i(TAG, "processImage time: "+ (System.currentTimeMillis() - now));
    }

    private float estimateScrollDelta(Bitmap prev, Bitmap curr) {
        long now = System.currentTimeMillis();
        int w = prev.getWidth();
        int h = prev.getHeight();

        int stride = 2;
        int maxShift = 20;

        int[] r1 = new int[w];
        int[] r2 = new int[w];

        long bestCost = Long.MAX_VALUE;
        int bestShift = 0;

        for (int shift = -maxShift; shift <= maxShift; shift++) {
            long cost = 0;

            for (int y = h / 4; y < h * 3 / 4; y += stride) {
                int y2 = y + shift;
                if (y2 < 0 || y2 >= h) continue;

                prev.getPixels(r1, 0, w, 0, y, w, 1);
                curr.getPixels(r2, 0, w, 0, y2, w, 1);

                for (int x = 0; x < w; x += stride) {
                    int a = (r1[x] >> 16) & 0xFF;
                    int b = (r2[x] >> 16) & 0xFF;
                    cost += Math.abs(a - b);
                }
            }

            if (cost < bestCost) {
                bestCost = cost;
                bestShift = shift;
            }
        }
        Log.i(TAG, "scrolling time: " + (System.currentTimeMillis() - now));

        return bestShift;
    }

    private boolean isScrolling(Bitmap prev, Bitmap curr) {
        long now = System.currentTimeMillis();

        int w = prev.getWidth();
        int h = prev.getHeight();

        int[] a = new int[w];
        int[] b = new int[w];

        int y = h / 2;

        prev.getPixels(a, 0, w, 0, y, w, 1);
        curr.getPixels(b, 0, w, 0, y + 5, w, 1);

        long diff = 0;
        for (int i = 0; i < w; i += 8) {
            diff += Math.abs(((a[i] >> 16) & 0xFF) - ((b[i] >> 16) & 0xFF));
        }
        Log.i(TAG, "scrolling time: " + (System.currentTimeMillis() - now) + " val: " + diff);

        return diff > 10000;
    }

    private void handleUI(List<DetectionResult> dt) {
        if (dynamicView != null) {
            long now = System.currentTimeMillis();
            dynamicView.updateDetections(dt);
//            Log.i(TAG, "processImage: uionly: "+imageModelName+ " : " + (System.currentTimeMillis()-now)+"ms");

        }

    }

    public void setupDynamicOverlay() {
        if (dynamicView == null) {
            dynamicView = new DynamicView(mcontext, wm, imageModelName, textModelName);
        }
    }


    // PixelFormat.RGBA_8888 image to bitmap
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
//        int width = image.getWidth();
//        int height = image.getHeight();
//        final Image.Plane[] planes = image.getPlanes();
//        final ByteBuffer buffer = planes[0].getBuffer();
//
//        int pixelStride = planes[0].getPixelStride();
//
//        int rowStride = planes[0].getRowStride();
//        int rowPadding = rowStride - pixelStride * width;
//        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444);
//        bitmap.copyPixelsFromBuffer(buffer);
//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
//        try {
//            saveBitmapToGallery(mcontext, bitmap, "capture_" + System.currentTimeMillis());
//
//        } catch (Exception e) {
//            Log.i(TAG, "imageToBitmap: exception" + e.getMessage());
//        }
//        return bitmap;
    }

    public static void saveBitmapToGallery(
            Context context,
            Bitmap bitmap,
            String fileName
    ) throws IOException {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/MyApp");

        Uri uri = context.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        try (OutputStream out =
                     context.getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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
     */
    public void destroy() {
        stopScreenCapture();
        if (wm != null) {
//            try {
//                if (dynamicOverlay != null) {
//                    wm.removeView(dynamicOverlay);
//                    dynamicOverlay = null;
//                }
//            } catch (Exception e) {
//                Log.e("OverlayService", "Error removing overlay view: " + e.getMessage());
//            }
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

    public void initModel(String textDetector, String imageDetector, int etn) throws IOException {
//        boolean isID = imageDetector.equals(ModelTypes.YOLO_V10_F32) || imageDetector.equals(ModelTypes.YOLO_V10_F16) || imageDetector.equals(ModelTypes.MOBILENET_SSD);
//
//        boolean isTD = textDetector.equals(ModelTypes.LSTM)
//                || textDetector.equals(ModelTypes.NaiveBayes)
//                || textDetector.equals(ModelTypes.LogisticRegression)
//                || textDetector.equals(ModelTypes.ROBERTA_TAGALOG)
//                || textDetector.equals(ModelTypes.DISTILBERT_TAGALOG)
//                || textDetector.equals(ModelTypes.SVM);
        if (!imageDetector.isEmpty() && !imageDetector.equals("none")) {
            imageModel = new ImageModel(mcontext, imageDetector);
            imageModelName = imageDetector;
        }
        if (!textDetector.isEmpty() && !textDetector.equals("none")) {
            textModel = new TextModel(mcontext, textDetector, etn);
            textModelName = textDetector;

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
