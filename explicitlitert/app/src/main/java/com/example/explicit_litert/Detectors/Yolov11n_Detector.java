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

public class Yolov11n_Detector {
    Context context;
    public static final String TAG = "YoloV11nDetector";
    String MODEL_PATH;
    private static final float CONFIDENCE_THRESHOLD = 0.25f;
    List<String> labels;
    int numChannel = 6;

//  int numElements = 12096;
//  int img_size = 768;

//    int numElements = 8400;
//    int img_size = 640;

    int numElements = 2100;
    int img_size = 320;
    List<TensorBuffer> inputBuffer;
    List<TensorBuffer> outputBuffer;
    CompiledModel model;

    public Yolov11n_Detector(Context context, String chosen_image_model) throws IOException {
        if (
                chosen_image_model.contains("_320")
        ) {
            Log.i(TAG, "Yolo_Detector: using " + 320 + " modelpath: " + chosen_image_model);

        } else if (
                chosen_image_model.contains("_640")
        ) {
            Log.i(TAG, "Yolo_Detector: using " + 640 + " modelpath: " + chosen_image_model);

             numElements = 8400;
             img_size = 640;

        } else if (
                chosen_image_model.contains("_768")
        ) {
            Log.i(TAG, "Yolo_Detector: using " + 768 + " modelpath: " + chosen_image_model);

             numElements = 12096;
             img_size = 768;
        }
        labels = new ArrayList<>();
        labels.add("nsfw");
        labels.add("safe");
        MODEL_PATH = chosen_image_model;
        String modelFilePath = copyAssetToFile(context, chosen_image_model);

        Log.i(TAG, "Yolov11n_Detector: MODEL PATH: " + MODEL_PATH + " filemodelpath: " + modelFilePath);
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
//        Log.i(TAG, "normalize(): " + (System.currentTimeMillis() - now));
        return outputFloatArray;
    }

    // run the interpreter
    public ClassifyResults detect(Bitmap bitmap) {
//        Log.i(TAG, "\n Function time in milliseconds (YOLOV10) size widht: " + bitmap.getWidth() + " height: " + bitmap.getHeight());

        try {
            Log.i("gpteam", "new yolov11or12_"+img_size);

            long now = System.currentTimeMillis();
            Bitmap image = Bitmap.createScaledBitmap(bitmap, img_size, img_size, false);
            Log.i("gpteam", "scale: " + (System.currentTimeMillis() - now));

            now = System.currentTimeMillis();
            float[] inputFloatArray = normalize(image, 0f, 255f);
            Log.i("gpteam", "normalize: " + (System.currentTimeMillis() - now));

            now = System.currentTimeMillis();
            inputBuffer.get(0).writeFloat(inputFloatArray);
            Log.i("gpteam", "inputbuffer: " + (System.currentTimeMillis() - now));

            now = System.currentTimeMillis();
            model.run(inputBuffer, outputBuffer);
            Log.i("gpteam", "model.run: " + (System.currentTimeMillis() - now));

            now = System.currentTimeMillis();
            float[] predictions = outputBuffer.get(0).readFloat();
            Log.i("gpteam", "outputbuffer: " + (System.currentTimeMillis() - now));

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
        int numBoxes = numElements;   // 8400
        int numChannels = numChannel; // 6 (2 classes then 4 bounding box)
        int numClasses = numChannels - 4;

        for (int i = 0; i < numBoxes; i++) {

            float cx = predictions[0 * numBoxes + i];
            float cy = predictions[1 * numBoxes + i];
            float w = predictions[2 * numBoxes + i];
            float h = predictions[3 * numBoxes + i];

            int bestClass = -1;
            float bestScore = 0f;

            for (int c = 0; c < numClasses; c++) {
                float score = predictions[(4 + c) * numBoxes + i];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }

            if (bestScore < CONFIDENCE_THRESHOLD) continue;
//            Log.i(TAG, "getBoundsList: best class is: " + bestClass );

            float x1 = cx - w / 2f;
            float y1 = cy - h / 2f;
            float x2 = cx + w / 2f;
            float y2 = cy + h / 2f;
//            Log.i(TAG, "getBoundsList: inti is: " + bestClass);

            String l = labels.get(bestClass);
            if (l.equals("safe")) continue;
//            Log.i("YOLO_BOX",
//                    "i=" + i +
//                            " label=" + l +
//                            " score=" + bestScore +
//                            " x1=" + x1 +
//                            " y1=" + y1 +
//                            " x2=" + x2 +
//                            " y2=" + y2
//            );
            results.add(new DetectionResult(
                    bestClass,
                    bestScore,
                    x1, y1, x2, y2,
                    l,
                    0
            ));
        }
//        Log.i(TAG, "getBoundsList(): " + (System.currentTimeMillis() - now));
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
