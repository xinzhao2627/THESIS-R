package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ResizeResult;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

// The total time complexity of this class is:
//
public class Mobilenet_ssd_Detector {
    Context context;
    public final String TAG = "YoloV10Detector";
    String MODEL_PATH;
    String LABELS_PATH;
    DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final float CONFIDENCE_THRESHOLD = 0.4f;

    List<String> labels;
    Interpreter interpreter;
    int tensorWidth = 0;
    int tensorHeight = 0;
    int numChannel = 0;
    int numElements = 0;
    public Mobilenet_ssd_Detector(Context context, String chosen_image_model, String chosen_image_label) throws IOException {
        MODEL_PATH = chosen_image_model;
        LABELS_PATH = chosen_image_label;
        Log.w(TAG, "Mobilenet Detector: Yolo initialized");
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ByteBuffer model = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatibilityList = new CompatibilityList();
        if (compatibilityList.isDelegateSupportedOnThisDevice()) {
            Log.w(TAG, "GPU SUPPORTED" );
            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            //        options.setNumThreads(4);
            Log.w(TAG, "GPU NOT SUPPORTED" );
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
        }

        interpreter = new Interpreter(model, options);

        // POPULATE LABELS LIST
        labels = new ArrayList<>();
        try (InputStream inputStream = context.getAssets().open(LABELS_PATH)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            while (line != null && !line.isEmpty()) {
                labels.add(line);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
        }
        int[] inputShape = interpreter.getInputTensor(0).shape();
        if (inputShape != null) {
            // the width and height of the input
            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];
            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2];
                tensorHeight = inputShape[3];
            }
        }
        Log.w(TAG, "tensorWidth: " + tensorWidth);
        Log.w(TAG, "tensorHeight: " + tensorHeight);

        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in YoloV10Detector(): " + (memEnd - memStart) + " bytes");
    }

    public ResizeResult resize(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false);
        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        tensorImage.load(resizedBitmap);
        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return new ResizeResult(tensorImage.getBuffer(), resizedBitmap);
    }


    public ClassifyResults detect(Bitmap bitmap) {
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        ResizeResult resizeResult = resize(bitmap);
        float[][][] boxes = new float[1][10][4];
        float[][] scores = new float[1][10];
        float[][] classes = new float[1][10];
        float[] numDetections = new float[1];
        Map<Integer, Object> output = new HashMap<>();
        output.put(0, boxes);
        output.put(1, classes);
        output.put(2, scores);
        output.put(3, numDetections);
        Object[] inputArray = { resizeResult.buffer };
        interpreter.runForMultipleInputsOutputs(inputArray, output);
        List<DetectionResult> detectionsResults = new ArrayList<>();
        int count = Math.min(10, (int) numDetections[0]);
        for (int i = 0; i < count; i++) {
            int score = (int) scores[0][i];
            if (score > CONFIDENCE_THRESHOLD) {
                int labelId = (int) classes[0][i];
                String label = labelId < labels.size() ? labels.get(labelId) : "Unknown";
                float ymin = boxes[0][i][0];
                float xmin = boxes[0][i][1];
                float ymax = boxes[0][i][2];
                float xmax = boxes[0][i][3];
                detectionsResults.add(new DetectionResult(labelId, score, xmin, ymin, xmax, ymax, label, 0));
            } 
        }
        return new ClassifyResults(resizeResult.resizedBitmap, detectionsResults);
    }

    public void cleanup() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }

        if (labels != null) {
            labels.clear();
            labels = null;
        }
    }
}