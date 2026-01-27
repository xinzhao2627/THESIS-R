package com.example.explicit_litert.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.explicit_litert.Types.AnchorGenerator;
import com.example.explicit_litert.Types.ClassifyResults;
import com.example.explicit_litert.Types.DetectionResult;
import com.google.ai.edge.litert.Accelerator;
import com.google.ai.edge.litert.CompiledModel;
import com.google.ai.edge.litert.TensorBuffer;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Efficientdet_Detector {
    public static final String TAG = "efficientdet";
    String MODEL_PATH;
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    List<String> labels;
    List<TensorBuffer> inputBuffer;
    List<TensorBuffer> outputBuffer;
    CompiledModel model;

    AnchorGenerator anchorGenerator;
    float[][] anchor;

    public Efficientdet_Detector(Context context, String chosen_image_model) throws IOException {
        anchorGenerator = new AnchorGenerator();
        anchor = anchorGenerator.generateAnchors(new int[][]{
                {64, 64},
                {32, 32},
                {16, 16}
        });
        Log.i(TAG, "anchorlen: "+ anchor.length + " anchor[1]:" + Arrays.toString(anchor[1]));

        labels = new ArrayList<>();
        labels.add("gore");
        labels.add("nsfw");
        labels.add("safe");
        MODEL_PATH = chosen_image_model;
        String modelFilePath = copyAssetToFile(context, chosen_image_model);
        Log.i(TAG, "Efficientdet_Detector: MODEL PATH: " + MODEL_PATH);
        try {
            model = CompiledModel.create(modelFilePath, new CompiledModel.Options(Accelerator.GPU));
            inputBuffer = model.createInputBuffers();
            outputBuffer = model.createOutputBuffers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public ClassifyResults detect(Bitmap bitmap) {
        Bitmap image = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
        float[] inputFloatArray = normalize(image, 0f, 255f);
        try {
            long now = System.currentTimeMillis();
            inputBuffer.get(0).writeFloat(inputFloatArray);
            model.run(inputBuffer, outputBuffer);

//           prediction results (scores)
            float[] predictions = outputBuffer.get(0).readFloat();
//            box results (coordinates)
            float[] boxes = outputBuffer.get(1).readFloat();
            Log.i(TAG, "detect: boxes.length = " + boxes.length);
            int N = boxes.length / 4;
            float[][] boxPreds = new float[N][4];
            for (int i = 0; i < N; i++) {
                boxPreds[i][0] = boxes[i * 4];
                boxPreds[i][1] = boxes[i * 4 + 1];
                boxPreds[i][2] = boxes[i * 4 + 2];
                boxPreds[i][3] = boxes[i * 4 + 3];
            }
            float[][] out = anchorGenerator.decodeBoxes(boxPreds, anchor);
            Log.i(TAG, "outlength: " + out.length + " anchorlength: " + anchor.length + " boxeslength: " + boxes.length + " boxpredslength: "+boxPreds.length);
            Log.i(TAG, "model.run: " + (System.currentTimeMillis() - now));

            return new ClassifyResults(null, getBoundsList(predictions, out));
//            Log.i(TAG, "detect: a: "+ a.length);
        } catch (Exception e) {
            Log.e(TAG, "detect: error", e);
        }

        return new ClassifyResults(null, new ArrayList<>());
    }

    public List<DetectionResult> getBoundsList(float[] predictions, float[][] out) {
        long now = System.currentTimeMillis();

        List<DetectionResult> results = new ArrayList<>();
        int numBoxes = predictions.length / 4;
        int numClasses = 3;

        for (int i = 0; i < numBoxes; i++) {
            int clsOffset = i * numClasses;
            float maxScore = -Float.MAX_VALUE;
            int classId = -1;

            for (int c = 0; c < numClasses; c++){
                float score = predictions[clsOffset + c];
                if (score > maxScore){
                    maxScore = score;
                    classId = c;
                }
            }

            if (maxScore < CONFIDENCE_THRESHOLD) continue;
            String l = labels.get(classId);
            if (l.equals("safe")) continue;
            float x1 = out[i][0];
            float y1 = out[i][1];
            float x2 = out[i][2];
            float y2 = out[i][3];
            x1 = Math.max(0f, Math.min(x1, 512));
            y1 = Math.max(0f, Math.min(y1, 512));
            x2 = Math.max(0f, Math.min(x2, 512));
            y2 = Math.max(0f, Math.min(y2, 512));
            float left   = x1 / 512;
            float top    = y1 / 512;
            float right  = x2 / 512;
            float bottom = y2 / 512;
            Log.i("DECODED_BOX",
                    "i=" + i +
                            " label=" + l +
                            " score=" + maxScore +
                            " left=" + left +
                            " top=" + top +
                            " right=" + right +
                            " bottom=" + bottom
            );
            results.add(new DetectionResult(
                    classId,
                    maxScore,
                    left, top, right, bottom,
                    l,
                    0
            ));



        }
        Log.i(TAG, "getBoundsList(): " + (System.currentTimeMillis() - now));
        return results;
    }

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

            // Clear anchor data
            if (anchor != null) {
                anchor = null;
            }

            if (anchorGenerator != null) {
                anchorGenerator = null;
            }

            Log.i(TAG, "cleanup(): Model resources released successfully");

        } catch (Exception e) {
            Log.e(TAG, "cleanup(): Error releasing resources: " + e.getMessage());
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
}
