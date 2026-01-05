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
import android.os.Looper;
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
    Handler handler; //backgrund
    Handler mainHandler; // ui
    HandlerThread handlerThread;
    DynamicOverlay dynamicOverlay;
    TextModel textModel;
    private static final String TAG = "OverlayFunctions";
    List<TrackedBox> previousDetections = new ArrayList<>();


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
        mainHandler = new Handler(Looper.getMainLooper());
        int[] boundsRes = getBounds();
        imageReader = ImageReader.newInstance(
                boundsRes[0],
                boundsRes[1],
                PixelFormat.RGBA_8888,
                3
        );
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = imageToBitmap(image);
                    processImage(image,bitmap);
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
    }

    private void processImage(Image image ,Bitmap bitmap) {
        List<DetectionResult> dt = new ArrayList<>();
        if (imageModel != null) {
            ClassifyResults res = imageModel.detect(bitmap);
            dt.addAll(res.detectionResults);
        }
        image.close();
        bitmap.recycle();
        mainHandler.post(()-> handleUI(dt));
    }
    private void handleUI(List<DetectionResult> dt) {
            if (dynamicView != null){
//                    only output 4 boxes for now
                while (dt.size() > 4) {
                    dt.remove(dt.size() - 1);
                }
                dynamicView.updateDetections(dt);
            }

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
