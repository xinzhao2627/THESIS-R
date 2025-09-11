package com.example.explicitapp3;

import static android.content.Context.WINDOW_SERVICE;
import static androidx.appcompat.content.res.AppCompatResources.getDrawable;
import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class OverlayFunctions {
    private MediaProjectionManager mediaProjectionManager;
    WindowManager wm;
    private MediaProjection mediaProjection;
    public Notification notification;
    Surface surface;

    TextureView screenMirror;
    VirtualDisplay mVirtualDisplay;
    Context mcontext;
    int dpi;
    Interpreter tflite;
    // these are for screenshot
    ImageReader imageReader;
    Handler handler;
    HandlerThread handlerThread;
    TextModel textModel;
    private static final String TAG = "OverlayFunctions";

    /**
     * Sets the media projection manager for screen capture operations.
     *
     * @param mediaProjectionManager The system media projection manager
     * @return The same media projection manager for method chaining
     */
    public MediaProjectionManager setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        this.mediaProjectionManager = mediaProjectionManager;
        return this.mediaProjectionManager;
    }
    /**
     * Sets the active media projection instance for screen capture.
     *
     * @param mediaProjection The media projection instance obtained from user permission
     * @see MediaProjectionManager#getMediaProjection(int, Intent)
     */
    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }
    /**
     * Sets the display density for virtual display configuration.
     * The dpi is the quality of the virtual display, higher dpi has higher resolution
     * but also higher memory consumption
     *
     * @param dpi Display density in dots per inch
     * @see DisplayMetrics#densityDpi
     */
    public void setDpi(int dpi) {
        this.dpi = dpi;
    }


    public void setWindowManager(WindowManager windowManager) {
        this.wm = windowManager;
    }
    /**
     * Initializes and configures the foreground service notification.
     * Also initializes the text model for content analysis.
     *
     * @param context Application context {@code Context context = getApplicationContext()}
     *                You cannot fetch context on a extend service class as to why you need to import it
     * @return The created notification instance
     * @throws IOException If text model initialization fails
     *
     * @see NotificationHelper#createNotificationChannel(Context)
     * @see TextModel#initTextModel(Context, String)
     */
    public Notification setNotification(Context context) throws IOException {
        NotificationHelper.createNotificationChannel(context);
        notification = NotificationHelper.createNotification(context);
        mcontext = context;
        textModel = new TextModel();
//
        return notification;
    }

    /**
     * It may initialize the TensorFlow Lite model.
     * NOTE: this may fail if you did not create an asset folder for models
     * @throws IOException If the model file cannot be loaded or is corrupted
     * @apiNote Currently uses the exported NSFW model with metadata
     */
    public void initModel() throws IOException {
//        textModel.initTextModel(mcontext, "try_model/quantized/model.tflite");
        textModel.initTextModel(mcontext, "exporter_model/exporter_nsfw_model_metadata.tflite");
    }

    /**
     * Starts the screen capture process using the configured media projection.
     * For the meantime, this will not display the code
     * <p>The method:
     * <ul>
     *   <li>Registers a callback to handle projection stop events</li>
     *   <li>Releases any existing virtual display (if any to avoid error)</li>
     *   <li>Creates a fresh virtual display with current screen dimensions</li>
     *   <li>Uses {@code AUTO_MIRROR} flag for automatic content mirroring</li>
     * </ul>
     *
     * @throws IllegalStateException If media projection is not initialized
     * @see #getBounds()
     * @see MediaProjection#createVirtualDisplay(String, int, int, int, int, Surface, VirtualDisplay.Callback, Handler)
     */
    public void startScreenCapture() {
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
    /**
     * Creates and displays a toggleable overlay view for blurred blocking.
     *
     * @param lf Layout inflater for creating the overlay view
     * @throws RuntimeException If overlay cannot be added to window manager
     *
     * @see WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY
     * @see View#setVisibility(int)
     */
    public void setupToggleableOverlay(LayoutInflater lf) {
        Drawable drawable = getDrawable(mcontext, R.drawable.window_background);
        WindowManager windowManager = mcontext.getSystemService(WindowManager.class);
        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // this is the overlay view
        View view = lf.inflate(R.layout.activity_overlay, null);
        Button b = view.findViewById(R.id.tohomescreenButton);
        if (textModel != null) {
            textModel.setView(view);
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "onClick: Im clicked..." );
                view.setVisibility(View.GONE);
                Intent homeintent = new Intent(Intent.ACTION_MAIN);
                homeintent.addCategory(Intent.CATEGORY_HOME);
                homeintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mcontext.startActivity(homeintent);

            }
        });

        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |  // WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                ,
                PixelFormat.TRANSLUCENT
        );
        params.setBlurBehindRadius(50);

        // init the imageview
        wm.addView(view, params);
    }

public void setupDynamicOverlayNonBlur(LayoutInflater lf, int widthR, int heightR) {
        Drawable drawable = getDrawable(mcontext, R.drawable.window_background);
        WindowManager windowManager = mcontext.getSystemService(WindowManager.class);

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // this is the linearview (drawable)
        LinearView view = lf.inflate(R.layout.LinearView, null);
        WindowManager.LayoutParams params = new

view.setBackground(Color.parseColor("#FF0000"));
 WindowManager.LayoutParams(               widthR,
               heightR,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );
        
        // init the imageview
        wm.addView(view, params);
    }
    /**
     * Sets up the screenshot capture system using ImageReader for real-time screen analysis.
     * Creates a background thread for image processing and configures automated screenshot saving.
     *
     * <p>The system:
     * <ul>
     *   <li>image processing - converting image to bitmap</li>
     *   <li>Implements automatic text recognition and prediction on captured frames</li>
     *   <li>Optional: Saves screenshots to device gallery for debugging/logging</li>
     *   <li>Handles proper resource cleanup for images and bitmaps (every read update)</li>
     * </ul>
     *
     * @throws RuntimeException If ImageReader setup fails or background thread cannot be created
     * @see ImageReader#newInstance(int, int, int, int)
     * @see #imageToBitmap(Image)
     * @see TextModel#textRecognition(Bitmap)
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setupOverlayScreenshot() {
        handlerThread = new HandlerThread("ScreenCapture");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        int[] boundsRes = getBounds();
        imageReader = ImageReader.newInstance(
                boundsRes[0],
                boundsRes[1],
                PixelFormat.RGBA_8888,
                2
        );

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    // save the image for now, deal with it l;ater
                    Bitmap bitmap = imageToBitmap(image);

                    // TODO fit the image/bitmap into the trained model, (e.g yuvImage & tflite interpreter
                    // TODO here:
                    // scale the bitmap, then normalize the rgb channels (float conversion) then put it into model
                    // TODO link: https://firebase.google.com/docs/ml/android/use-custom-models#java_3


                    //TODO recognize text here:
                    textModel.textRecognition(bitmap);


                    // OPTIONAL (SAVE TO GALLERY)
                    saveToGalleryBitmap(bitmap);
                    image.close();
                    bitmap.recycle();
                    // TODO open later
//                    Log.w(TAG, "onImageAvailable: Running imagee");
                }
            }
        }, handler);
        surface = imageReader.getSurface();
        if (mediaProjection != null) startScreenCapture();
    }
    /**
     * Converts an Android Image object to a Bitmap for processing.
     * Reconstruict the image using pixelstride.
     *
     * @param image The source Image from ImageReader
     * @return Bitmap of the image
     *
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
     *
     * @see MediaStore.Images.Media#EXTERNAL_CONTENT_URI
     * @see ContentResolver#insert(Uri, ContentValues)
     */
    public void saveToGalleryBitmap(Bitmap bitmap) {
        // can configure the bitmap here
        if (bitmap != null) {
            // save to gallery
            Bitmap res = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(res);
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(0x55FF0000, PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(res, 0, 0, paint);

            try {
//                File dir = mcontext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//                File file = new File(dir, "screenshot_"+ System.currentTimeMillis() +".png");
//                FileOutputStream fos = new FileOutputStream(file);
//                bitmap.compress(Bitmap.CompressFormat.PNG,100, fos);
//                fos.close();
//                Log.w(TAG, "onSurfaceTextureUpdated: running..." );
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
            } //catch (FileNotFoundException e) {
//                Log.e(TAG, "onSurfaceTextureUpdated: NotFoundError " + e.getMessage() );
//                throw new RuntimeException(e);
//            }
            catch (IOException e) {
                Log.e(TAG, "onSurfaceTextureUpdated: IOError " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                res.recycle();
            }

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
     * @apiNote This method is safe to call multiple times
     * @see #stopScreenCapture()
     * @see HandlerThread#quitSafely()
     * @see TextModel#cleanup()
     */
    public void destroy() {
        stopScreenCapture();
        if (wm != null && screenMirror != null) {
            try {
                wm.removeView((View) screenMirror.getParent());
            } catch (Exception e) {
                Log.e("OverlayService", "Error removing overlay view: " + e.getMessage());
            }
            wm = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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

    }

    public Surface getSurface() {
        return surface;
    }
}
