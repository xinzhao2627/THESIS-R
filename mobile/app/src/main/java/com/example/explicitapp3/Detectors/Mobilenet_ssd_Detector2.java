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
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

// The total time complexity of this class is:
//
public class Mobilenet_ssd_Detector2 {
    public final String TAG = "MobileNetDetector";
    String MODEL_PATH;
    String LABELS_PATH;
    DataType INPUT_IMAGE_TYPE = DataType.UINT8;
    private static final float CONFIDENCE_THRESHOLD = 0.7f;

    List<String> labels;
    Interpreter interpreter;

    int tensorWidth = 0;
    int tensorHeight = 0;
    int maxDetections = 0;
    int numClasses = 0;

    ImageProcessor imageProcessor;

    public Mobilenet_ssd_Detector2(Context context, String chosen_image_model, String chosen_image_label) throws IOException {
        MODEL_PATH = chosen_image_model;
        LABELS_PATH = chosen_image_label;

        Log.w(TAG, "Mobilenet Detector: initialized: " + MODEL_PATH);
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(0f, 1f))
                .add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
//                .add(new CastOp(INPUT_IMAGE_TYPE))
                .build();
        ByteBuffer model = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatibilityList = new CompatibilityList();
        if (compatibilityList.isDelegateSupportedOnThisDevice()) {
            Log.w(TAG, "GPU SUPPORTED");
            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            //        options.setNumThreads(4);
            Log.w(TAG, "GPU NOT SUPPORTED");
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
            Log.w(TAG, "init input shape length: " + inputShape.length);

            // the width and height of the input
            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];
            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                Log.w(TAG, "init is 3: " + inputShape.length);

                tensorWidth = inputShape[2];
                tensorHeight = inputShape[3];
            }
        }
//        maxDetections = inputShape[1];
        Log.w(TAG, "init inputshape[0]: " + inputShape[0]);
        Log.w(TAG, "init inputshape[1]: " + inputShape[1]);
        Log.w(TAG, "init inputshape[2]: " + inputShape[2]);
        Log.w(TAG, "init inputshape[3]: " + inputShape[3]);
        maxDetections = interpreter.getOutputTensor(0).shape()[1];
        Log.w(TAG, "init tensorWidth: " + tensorWidth);
        Log.w(TAG, "init tensorHeight: " + tensorHeight);
        Log.w(TAG, "init maxDetections: " + maxDetections);
        Log.w(TAG, "init numClasses: " + numClasses);
        Log.w(TAG, "init Scores shape: " + Arrays.toString(interpreter.getOutputTensor(0).shape()));
        Log.w(TAG, "init Boxes shape: " + Arrays.toString(interpreter.getOutputTensor(1).shape()));
        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in YoloV10Detector(): " + (memEnd - memStart) + " bytes");
    }

    public ResizeResult resize(Bitmap bitmap) {
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false);
        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        tensorImage.load(resizedBitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);
        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in resize(): " + (memEnd - memStart) + " bytes");
        return new ResizeResult(processedImage.getBuffer(), resizedBitmap);
    }

    public ClassifyResults detect(Bitmap bitmap) {
        ResizeResult resizeResult = resize(bitmap);

        float[][][] boxes = new float[1][maxDetections][4];
        float[][] classes = new float[1][maxDetections];
        float[][] scores = new float[1][maxDetections];
        float[] numDetections = new float[1];

        Map<Integer, Object> output = new HashMap<>();
        output.put(0, boxes);
        output.put(1, classes);
        output.put(2, scores);
        output.put(3, numDetections);

        interpreter.runForMultipleInputsOutputs(
                new Object[]{ resizeResult.buffer },
                output
        );

        List<DetectionResult> results = new ArrayList<>();
        int detections = (int) numDetections[0];

        for (int i = 0; i < detections; i++) {
            float score = scores[0][i];
            if (score < CONFIDENCE_THRESHOLD) continue;

            int classId = Math.max(0, Math.min((int) classes[0][i], labels.size() - 1));

            float ymin = boxes[0][i][0];
            float xmin = boxes[0][i][1];
            float ymax = boxes[0][i][2];
            float xmax = boxes[0][i][3];

            String label = classId < labels.size() ? labels.get(classId) : "Unknown";
            Log.w(TAG,
                    "RAW BOX i=" + i +
                            " ymin=" + ymin +
                            " xmin=" + xmin +
                            " ymax=" + ymax +
                            " xmax=" + xmax +
                            " score=" + score
            );
            results.add(new DetectionResult(
                    classId, score, xmin, ymin, xmax, ymax, label, 0
            ));
        }

        return new ClassifyResults(resizeResult.resizedBitmap, results);
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
        imageProcessor = null;
    }
}