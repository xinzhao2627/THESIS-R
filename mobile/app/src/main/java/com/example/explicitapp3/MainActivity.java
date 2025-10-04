package com.example.explicitapp3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.example.explicitapp3.Types.ModelTypes;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String YOLO_V10_F16_MODEL = "yolov10_16/yolov10n_float16.tflite";
    public static final String YOLO_V10_F16_LABELS = "yolov10_16/labels.txt";

    public static final String YOLO_V10_F32_MODEL = "yolov10_32/yolov10n_float32.tflite";
    public static final String YOLO_V10_F32_LABELS = "yolov10_32/labels.txt";

    public static final String DistilBert_Tagalog_MODEL = "distilbert_tagalog/distilbert_tagalog_classification_model.tflite";
    public static final String xtremedistil_MODEL = "xtremedisti/xtremedistil_nsfw_safe_model.tflite";
    public static final String dost_robert_MODEL = "dost_robert/nsfw_model.tflite";
    public static final String xlm_roberta_MODEL = "xlm_roberta/xlm_roberta_classification_model.tflite";
    public static final String roberta_tagalog_MODEL = "roberta_tagalog/roberta_tagalog_nsfw_model.tflite";


//    String chosen_text_model = "";
//    String[] chosen_image_model = {"", ""};
    String chosen_model = "";

    Button runAppButton;
    Button overlayPermissionButton;

    Button textModel1;
    Button textModel2;
    Button textModel3;
    Button textModel4;
    Button textModel5;
    Button textModel6;
//    ArrayList<Button> textButtons;
    ArrayList<Button> buttons;

    Button imageModel1;
    Button imageModel2;
//    ArrayList<Button> imageButtons;

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

//        imageButtons = new ArrayList<>();
//        textButtons = new ArrayList<>();
        buttons = new ArrayList<>();

        textModel1 = findViewById(R.id.textmodelButton1);
//        textButtons.add(textModel1);
        buttons.add(textModel1);
        textModel2 = findViewById(R.id.textmodelButton2);
//        textButtons.add(textModel2);
        buttons.add(textModel2);
        textModel3 = findViewById(R.id.textmodelButton3);
//        textButtons.add(textModel3);
        buttons.add(textModel3);
        textModel4 = findViewById(R.id.textmodelButton4);
//        textButtons.add(textModel4);
        buttons.add(textModel4);
        textModel5 = findViewById(R.id.textmodelButton5);
//        textButtons.add(textModel5);
        buttons.add(textModel5);

        textModel6 = findViewById(R.id.textmodelButton6);
        buttons.add(textModel6);
        imageModel1 = findViewById(R.id.imagemodelButton1);
        imageModel2 = findViewById(R.id.imagemodelButton2);

//        imageButtons.add(imageModel1);
//        imageButtons.add(imageModel2);
        buttons.add(imageModel1);
        buttons.add(imageModel2);

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
//                    if (chosen_image_model[0].isEmpty() || chosen_image_model[1].isEmpty() || chosen_text_model.isEmpty()) {
//                        Toast.makeText(this, "Please choose image and text models", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
                    if (chosen_model.isEmpty()) {
                        Toast.makeText(this, "Please choose image and text models", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent serviceIntent = new Intent(this, OverlayService.class);
                    serviceIntent.putExtra("resultCode", resultCode);
                    serviceIntent.putExtra("data", data);

//                    serviceIntent.putExtra("chosen_image_model_model", chosen_image_model[0]);
//                    serviceIntent.putExtra("chosen_image_model_label", chosen_image_model[1]);
//                    serviceIntent.putExtra("chosen_text_model", chosen_text_model);
                    serviceIntent.putExtra("chosen_model", chosen_model);

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

        textModel1.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("DistilBERT Tagalog")) {
//                    chosen_text_model = DistilBert_Tagalog_MODEL;
                    chosen_model = ModelTypes.DISTILBERT_TAGALOG;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
        textModel2.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("MSX DistilBERT")) {
//                    chosen_text_model = xtremedistil_MODEL;
                    chosen_model = ModelTypes.DISTILBERT_TAGALOG;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
        textModel3.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("DOST RoBERTa")) {
//                    chosen_text_model = dost_robert_MODEL;
                    chosen_model = ModelTypes.DISTILBERT_TAGALOG;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
        textModel4.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("XLM RoBERTa")) {
//                    chosen_text_model = xlm_roberta_MODEL;
                    chosen_model = ModelTypes.DISTILBERT_TAGALOG;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
        textModel5.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("RoBERTa Tagalog")) {
//                    chosen_text_model = roberta_tagalog_MODEL;
                    chosen_model = ModelTypes.DISTILBERT_TAGALOG;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
        textModel6.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("LSTM")) {
//                    chosen_text_model = roberta_tagalog_MODEL;
                    chosen_model = ModelTypes.LSTM;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });

        imageModel1.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("Yolov10n_32f")) {
//                    chosen_image_model = new String[]{YOLO_V10_F32_MODEL, YOLO_V10_F32_LABELS};
                    chosen_model = ModelTypes.YOLO_V10_F32;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
        imageModel2.setOnClickListener(l -> {
            for (Button b : buttons) {
                String s = b.getText().toString();
                if (s.equals("Yolov10n_16f")) {
//                    chosen_image_model = new String[]{YOLO_V10_F16_MODEL, YOLO_V10_F16_LABELS};
                    chosen_model = ModelTypes.YOLO_V10_F16;
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.buttonSelected));
                    b.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                    b.setTextColor(Color.parseColor("#002D8B"));
                }
            }
        });
    }

    private void stopOverlayService() {
        Intent serviceIntent = new Intent(this, OverlayService.class);
        serviceIntent.setAction("STOP_SERVICE");
        startService(serviceIntent);

        isServiceRunning = false;
        runAppButton.setText("Start");

    }


}