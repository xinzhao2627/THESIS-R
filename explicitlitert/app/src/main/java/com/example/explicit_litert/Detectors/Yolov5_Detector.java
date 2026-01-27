package com.example.explicit_litert.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.explicit_litert.Types.ClassifyResults;
import com.example.explicit_litert.Types.DetectionResult;
import com.google.ai.edge.litert.Accelerator;
import com.google.ai.edge.litert.CompiledModel;
import com.google.ai.edge.litert.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Yolov5_Detector {
    Context context;
    public static final String TAG = "YoloV5Detector";
    String MODEL_PATH;
    private static final float CONFIDENCE_THRESHOLD = 0.25f;
    List<String> labels;
    int numChannel = 7;
//    int numElements = 25200;
//    int img_size = 640;
    int numElements = 6300;
    int img_size = 320;

    List<TensorBuffer> inputBuffer;
    List<TensorBuffer> outputBuffer;
    CompiledModel model;

    public Yolov5_Detector(Context context, String chosen_image_model) throws IOException {
        labels = new ArrayList<>();
        labels.add("nsfw");
        labels.add("safe");
        MODEL_PATH = chosen_image_model;
        String modelFilePath = copyAssetToFile(context, chosen_image_model);

        Log.i(TAG, "Yolov5_Detector: MODEL PATH: " + MODEL_PATH);
        try {
            model = CompiledModel.create(modelFilePath, new CompiledModel.Options(Accelerator.GPU));
            inputBuffer = model.createInputBuffers();

            outputBuffer = model.createOutputBuffers();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String copyAssetToFile(Context context, String assetPath) throws IOException {
        File outFile = new File(context.getFilesDir(), assetPath);
        outFile.getParentFile().mkdirs();

        if (outFile.exists()) return outFile.getAbsolutePath();

        try (InputStream is = context.getAssets().open(assetPath);
             FileOutputStream fos = new FileOutputStream(outFile)) {

            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        return outFile.getAbsolutePath();
    }

    public static float[] normalize(Bitmap image, float mean, float stddev) {
        long now = System.currentTimeMillis();
        int width = image.getWidth();
        int height = image.getHeight();
        int numPixels = width * height;

        int[] pixelsIntArray = new int[numPixels];
        float[] outputFloatArray = new float[numPixels * 3]; // 3 channels (R, G, B)

        image.getPixels(pixelsIntArray, 0, width, 0, 0, width, height);

        for (int i = 0; i < numPixels; i++) {
            int pixel = pixelsIntArray[i];

            float r = (float) Color.red(pixel);
            float g = (float) Color.green(pixel);
            float b = (float) Color.blue(pixel);

            int outputBaseIndex = i * 3;
            outputFloatArray[outputBaseIndex] = (r - mean) / stddev; // red
            outputFloatArray[outputBaseIndex + 1] = (g - mean) / stddev; // green
            outputFloatArray[outputBaseIndex + 2] = (b - mean) / stddev; // blue
        }
        Log.i(TAG, "normalize(): " + (System.currentTimeMillis() - now));
        return outputFloatArray;
    }

    // run the interpreter
    public ClassifyResults detect(Bitmap bitmap) {
        Log.i(TAG, "\n Function time in milliseconds (YOLOV10) size widht: " + bitmap.getWidth() + " height: " + bitmap.getHeight());
        Bitmap image = Bitmap.createScaledBitmap(bitmap, img_size, img_size, true);
        float[] inputFloatArray = normalize(image, 0f, 255f);
        try {
            long now = System.currentTimeMillis();
            inputBuffer.get(0).writeFloat(inputFloatArray);
            model.run(inputBuffer, outputBuffer);
            float[] predictions = outputBuffer.get(0).readFloat();
            Log.i(TAG, "model.run: " + (System.currentTimeMillis() - now));

            return new ClassifyResults(null, getBoundsList(predictions));
//            Log.i(TAG, "detect: a: "+ a.length);
        } catch (Exception e) {
            Log.i(TAG, "detect: error" + e.getMessage());
        }

        return new ClassifyResults(null, new ArrayList<>());
    }

    //    get the coordinates and label
    public List<DetectionResult> getBoundsList(float[] predictions) {
        long now = System.currentTimeMillis();

        List<DetectionResult> results = new ArrayList<>();
        int numBoxes = numElements;   // 25200
        int numChannels = numChannel; // 7
        int numClasses = numChannels - 5;

        for (int i = 0; i < numBoxes; i++) {

            int offset = i * numChannels;

            float cx = predictions[offset + 0];
            float cy = predictions[offset + 1];
            float w  = predictions[offset + 2];
            float h  = predictions[offset + 3];
            float objectness = predictions[offset + 4];

            int bestClass = -1;
            float bestScore = 0f;

            for (int c = 0; c < numClasses; c++) {
                float score = predictions[offset + 5 + c];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }
            float confidence = bestScore * objectness;
            if (confidence < CONFIDENCE_THRESHOLD) continue;
//            Log.i(TAG, "getBoundsList: best class is: " + bestClass );

            float x1 = cx - w / 2f;
            float y1 = cy - h / 2f;
            float x2 = cx + w / 2f;
            float y2 = cy + h / 2f;
            Log.i(TAG, "getBoundsList: inti is: " + bestClass);

            String l = labels.get(bestClass);
            if (l.equals("safe")) continue;
            Log.i("YOLO_BOX",
                    "i=" + i +
                            " label=" + l +
                            " score=" + bestScore +
                            " x1=" + x1 +
                            " y1=" + y1 +
                            " x2=" + x2 +
                            " y2=" + y2
            );
            results.add(new DetectionResult(
                    bestClass,
                    confidence,
                    x1, y1, x2, y2,
                    l,
                    0
            ));
        }
        Log.i(TAG, "getBoundsList(): " + (System.currentTimeMillis() - now));
        return results;
    }

    //
//    //    close the model, this runs one time
    public void cleanup() {
        try {
            // Close the compiled model
            if (model != null) {
                model.close();
                model = null;
            }

            // Clear input/output buffers
            if (inputBuffer != null) {
                inputBuffer.clear();
                inputBuffer = null;
            }

            if (outputBuffer != null) {
                outputBuffer.clear();
                outputBuffer = null;
            }

            // Clear labels
            if (labels != null) {
                labels.clear();
                labels = null;
            }
            Log.i(TAG, "cleanup(): Model resources released successfully");

        } catch (Exception e) {
            Log.e(TAG, "cleanup(): Error releasing resources: " + e.getMessage());
        }
    }
}
