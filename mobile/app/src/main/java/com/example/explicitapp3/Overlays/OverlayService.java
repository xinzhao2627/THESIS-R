package com.example.explicitapp3.Overlays;


import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.example.explicitapp3.MainActivity;

import java.io.IOException;

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
 * @see OverlayFunctions
 * @see MainActivity
 * @see MediaProjection
 * @since API level 21
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
    public int onStartCommand(Intent intent, int flags, int startId)  {
        if (intent != null && "STOP_SERVICE".equals(intent.getAction())) {
            Log.w(TAG, "Stop command received, shutting down service");
            stopSelf();
            return START_NOT_STICKY;
        }


        int rc = intent.getIntExtra("resultCode", -1);
        Intent d = intent.getParcelableExtra("data");

//        String chosen_image_model_path = intent.getStringExtra("chosen_image_model_model");
//        String chosen_image_model_labels = intent.getStringExtra("chosen_image_model_label");
//        String chosen_text_model = intent.getStringExtra("chosen_text_model");
        String chosen_model = intent.getStringExtra("chosen_model");
//        Log.w(TAG, "Received models - Image: " + chosen_image_model_path);
//        Log.w(TAG, "Received labels: " + chosen_image_model_labels);
//        Log.w(TAG, "Received text model: " + chosen_text_model);

        Log.w(TAG, "onStartCommand: Media projection will now start capturing");

        if (rc == -1 && d != null) {
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (overlayFunctions == null) overlayFunctions = new OverlayFunctions();
            try {
                notification = overlayFunctions.setNotification(getApplicationContext());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            try {
                overlayFunctions.initModel(chosen_model);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (mediaProjection == null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(rc, d);
                overlayFunctions.setup(wm, getResources().getDisplayMetrics().densityDpi, mediaProjection, mediaProjectionManager);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (overlayFunctions != null) {
            overlayFunctions.destroy();
            overlayFunctions = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}