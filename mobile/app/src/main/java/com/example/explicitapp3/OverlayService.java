package com.example.explicitapp3;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.explicitapp3.OverlayFunctions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
/**
 * OverlayService is a foreground service that came from the main activity.
 * This service runs continuously /silently in the background to capture
 * screen content, analyze and process the image/text, and display overlays when needed (on basis.
 *
 * <p>The service implements several Android system services and APIs:
 * <ul>
 *   <li><strong>MediaProjection API</strong> - screen content capture</li>
 *   <li><strong>WindowManager</strong> - system overlays display</li>
 *   <li><strong>Foreground Service</strong> - persistency on background operation</li>
 *   <li><strong>TensorFlow Lite</strong> - classification and analysis</li>
 * </ul>
 *
 *
 * <p>Required permissions and declarations in AndroidManifest.xml:
 * <pre>{@code
 * <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
 *
 * <service
 *     android:name=".OverlayService"
 *     android:enabled="true"
 *     android:exported="false"
 *     android:foregroundServiceType="mediaProjection" />
 * }</pre>
 *
 * <p>Usage from MainActivity:
 * <pre>{@code
 * Intent serviceIntent = new Intent(this, OverlayService.class);
 * serviceIntent.putExtra("resultCode", resultCode);
 * serviceIntent.putExtra("data", mediaProjectionIntent);
 * ContextCompat.startForegroundService(this, serviceIntent);
 * }</pre>
 *
 * @author xinzhao2627 (R. Montaniel)
 * @version 1.0
 * @since API level 21
 * @see OverlayFunctions
 * @see MainActivity
 * @see MediaProjection
 */
public class OverlayService extends Service {
    public static final String TAG = "OverlayService";
    OverlayFunctions overlayFunctions;
    public static int SERVICE_ID = 1667;
    private MediaProjection mediaProjection;
    Notification notification;
    MediaProjectionManager mediaProjectionManager;
    WindowManager wm;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int rc = intent.getIntExtra("resultCode", -1);
        Intent d = intent.getParcelableExtra("data");
        Log.w(TAG, "onStartCommand: Media projection will now start capturing");

        if (rc == -1 && d != null) {
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (overlayFunctions == null) overlayFunctions = new OverlayFunctions();

            overlayFunctions.setWindowManager(wm);
            overlayFunctions.setMediaProjectionManager(mediaProjectionManager);
            overlayFunctions.setDpi(getResources().getDisplayMetrics().densityDpi);
            try {
                notification = overlayFunctions.setNotification(getApplicationContext());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);

            if (mediaProjection == null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(rc, d);
                overlayFunctions.setMediaProjection(mediaProjection);
            }

            try {
                overlayFunctions.initModel();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            overlayFunctions.setupToggleableOverlay(
                    (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)
            );
            overlayFunctions.setupOverlayScreenshot();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //
        overlayFunctions.destroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}