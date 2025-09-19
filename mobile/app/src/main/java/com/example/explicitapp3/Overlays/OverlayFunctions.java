package com.example.explicitapp3.Overlays;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.example.explicitapp3.Detectors.ImageModel;
import com.example.explicitapp3.Detectors.YoloV10Detector;
import com.example.explicitapp3.NotificationHelper;
import com.example.explicitapp3.Detectors.DistilBERT_Detector;
import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.DetectionResult;
import com.google.android.gms.common.util.ArrayUtils;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverlayFunctions {
    private MediaProjectionManager mediaProjectionManager;
    WindowManager wm;
    private MediaProjection mediaProjection;
    public Notification notification;
    Surface surface;
    Bitmap mbitmap;
    VirtualDisplay mVirtualDisplay;
    Context mcontext;
    int dpi;
    int frameCounter = 0;
    int frameSkip = 2;
    // these are for screenshot
    ImageReader imageReader;
    Handler handler;
    HandlerThread handlerThread;
    DistilBERT_Detector distilBERTDetector;
    YoloV10Detector yoloV10Detector;
    DynamicOverlay dynamicOverlay;
    OpenGlRecorder openGlRecorder;
    private static final String TAG = "OverlayFunctions";

    /**
     * Initializes and configures the foreground service notification.
     * Also initializes the text model for content analysis.
     *
     * @param context Application context {@code Context context = getApplicationContext()}
     *                You cannot fetch context on a extend service class as to why you need to import it
     * @return The created notification instance
     * @throws IOException If text model initialization fails
     * @see NotificationHelper#createNotificationChannel(Context)
     * @see DistilBERT_Detector (Context, String)
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
//        iniOpenGlRecorder();

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
//                ImageFormat.YUV_420_888,
                PixelFormat.RGBA_8888,
                56
        );

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
//                frameCounter++;
//                if (frameCounter % frameSkip != 0) {
//                    Image image = reader.acquireLatestImage();
//                    if (image != null) image.close();
//                }
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = imageToBitmap(image);
                    processImageAsync(bitmap);
                    image.close();
//                    saveToGalleryBitmap(bitmap);
                    bitmap.recycle();
                }
            }
        }, handler);
        surface = imageReader.getSurface();
        initRecorder();
    }
    public void iniOpenGlRecorder(){
        int[] boundsRes = getBounds();
        int width = boundsRes[0];
        int height = boundsRes[1];
        openGlRecorder = new OpenGlRecorder(mediaProjection, width, height, dpi, mcontext);

        openGlRecorder.start();

    }

    public void setupDynamicOverlay() {
        dynamicOverlay = new DynamicOverlay(mcontext);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        wm.addView(dynamicOverlay, params);
    }

    private void processImageAsync(Bitmap bitmap) {
        // get all result for yolo
        ClassifyResults res = yoloV10Detector.detect(bitmap);

        // then for bert
        List<DetectionResult> dt = distilBERTDetector.textRecognition(bitmap);

        // combine
        dt.addAll(res.detectionResults);


        Log.w(TAG, "res length: "+res.detectionResults.size() + " dt length: " + dt.size() );

        // display
        if (dynamicOverlay != null) {
            new Handler(mcontext.getMainLooper()).post(() -> {
                dynamicOverlay.setResults(dt);
            });
        }
    }

    /**
     * Converts an Android Image object to a Bitmap for processing.
     * Reconstruict the image using pixelstride.
     * ルビーちゃん, はいい??
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
     * @see DistilBERT_Detector#cleanup()
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
//                throw new RuntimeException(e);
            }
            handlerThread = null;
            handler = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (distilBERTDetector != null) {
            distilBERTDetector.cleanup();
            distilBERTDetector = null;
        }

        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (yoloV10Detector != null) {
            yoloV10Detector.cleanup();
            yoloV10Detector = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
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
    public void initModel() throws IOException {
        distilBERTDetector = new DistilBERT_Detector(mcontext, "exporter_model/exporter_nsfw_model_metadata.tflite");
        yoloV10Detector = new YoloV10Detector(mcontext);
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
