package com.example.explicitapp3;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
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

public class MainActivity extends AppCompatActivity {
    Button runAppButton;
    Button overlayPermissionButton;
    MediaProjectionManager mediaProjectionManager;
    ActivityResultLauncher<Intent> overlayPermissionLauncher;
    ActivityResultLauncher<Intent> mediaProjectionLauncher;
    public static final String TAG = "MainActivity";
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

                Intent i = mediaProjectionManager.createScreenCaptureIntent();
                mediaProjectionLauncher.launch(i);

            } else {
                Toast.makeText(this, "Please enable overlay permission", Toast.LENGTH_SHORT).show();
            }
        });
    }


}