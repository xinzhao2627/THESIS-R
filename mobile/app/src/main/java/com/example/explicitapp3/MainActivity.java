package com.example.explicitapp3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.explicitapp3.Overlays.OverlayService;

public class MainActivity extends AppCompatActivity {
    Button runAppButton;
    Button overlayPermissionButton;
    MediaProjectionManager mediaProjectionManager;
    ActivityResultLauncher<Intent> overlayPermissionLauncher;
    ActivityResultLauncher<Intent> mediaProjectionLauncher;
    public static final String TAG = "MainActivity";
    boolean isServiceRunning = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        runAppButton = findViewById(R.id.runAppButton);
        overlayPermissionButton = findViewById(R.id.overlayPermissionButton);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        /**
         * Configures the overlay permission launcher to handle user responses to overlay permission requests.
         * This launcher manages the flow when users grant or deny the SYSTEM_ALERT_WINDOW permission
         * required for displaying content over other applications.
         *
         * <p>The launcher:
         * <ul>
         *   <li>Checks if overlay permission was granted using {@link Settings#canDrawOverlays(Context)}</li>
         *   <li>Displays appropriate toast messages to inform the user</li>
         *   <li>Enables or disables functionality based on permission status</li>
         * </ul>
         *
         * @implNote Uses {@link ActivityResultContracts.StartActivityForResult} for modern permission handling
         * @see Settings#ACTION_MANAGE_OVERLAY_PERMISSION
         */
        overlayPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (Settings.canDrawOverlays(this))
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "You did not grant the permission.", Toast.LENGTH_SHORT).show();

        });
        overlayPermissionButton.setOnClickListener(l -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(i);
        });
        /**
         * Configures the media projection launcher to handle screen capture permission responses.
         * This launcher processes the result of media projection permission requests and starts
         * the overlay service if permission is granted.
         *
         * The reason this specific function uses foreground service is to comply to Android 15
         * intent service transfer
         *
         * <p>On successful permission grant:
         * <ul>
         *   <li>Extracts result code and intent data from the permission response</li>
         *   <li>Creates and starts {@link OverlayService} as a foreground service</li>
         *   <li>Passes permission data to the service for screen capture initialization</li>
         *   <li>Updates UI to reflect service running state</li>
         * </ul>
         * You will need to enable the following permission in manifest:
         * {@code
         *      <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
         *     <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
         * }
         * <p>On unsuccessful:
         * <ul>
         *   <li>Displays error message</li>
         * </ul>
         * @author xinzhao2627 (R. Montaniel)
         * @see OverlayService
         * @see ContextCompat#startForegroundService(Context, Intent)
         */
        mediaProjectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            int resultCode = r.getResultCode();
            Intent data = r.getData();

            Log.d(TAG, "MediaProjection result - resultCode: " + resultCode);
            Log.d(TAG, "MediaProjection result - data: " + data);

            if (resultCode == Activity.RESULT_OK) {
                // Permission granted
                try {
                    Intent serviceIntent = new Intent(this, OverlayService.class);
                    serviceIntent.putExtra("resultCode", resultCode);
                    serviceIntent.putExtra("data", data);


                    Log.d(TAG, "Starting service with resultCode: " + resultCode);
                    Log.d(TAG, "Starting service with data: " + data);
                    Log.d(TAG, "running...");

                    ContextCompat.startForegroundService(this, serviceIntent);
                    isServiceRunning = true;
                    runAppButton.setText("Stop");
                } catch (RuntimeException e) {
                    Log.w(TAG, "onCreate: Error on MediaProjectionInstance " + e.getMessage());
                }
            } else {
                // Permission denied or cancelled
                Log.w(TAG, "Screen capture permission denied. ResultCode: " + resultCode);
                Toast.makeText(this, "Screen sharing permission denied", Toast.LENGTH_SHORT).show();
            }
        });

        runAppButton.setOnClickListener((l) -> {
            if (Settings.canDrawOverlays(this)) {
                if (isServiceRunning) {
                    stopOverlayService();
                } else {
                    Intent i = mediaProjectionManager.createScreenCaptureIntent();
                    mediaProjectionLauncher.launch(i);
                }


            } else {
                Toast.makeText(this, "Please enable overlay permission", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void stopOverlayService(){
        Intent serviceIntent = new Intent(this, OverlayService.class);
        serviceIntent.setAction("STOP_SERVICE");
        startService(serviceIntent);

        isServiceRunning = false;
        runAppButton.setText("Start");

    }


}