package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ResizeResult;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
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
import java.util.List;

import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

// The total time complexity of this class is:
//
public class Yolov11n_Detector {
    Context context;
    public final String TAG = "YoloV11Detector";
    String MODEL_PATH;
    String LABELS_PATH;
    DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final float CONFIDENCE_THRESHOLD = 0.25f;

    List<String> labels;
    Interpreter interpreter;
    int tensorWidth = 0;
    int tensorHeight = 0;
    int numChannel = 0;
    int numElements = 0;
    GpuDelegate gpuDelegate;
    ImageProcessor imageProcessor;

    //    new
    TensorImage tensorImage;
    TensorBuffer output;
    Bitmap resizedBitmap;

    //    initialize, this runs one time
    public Yolov11n_Detector(Context context, String chosen_image_model, String chosen_image_label) throws IOException {
        MODEL_PATH = chosen_image_model;
        LABELS_PATH = chosen_image_label;
        Log.w(TAG, "YoloV10Detector: Yolo initialized \n \n");
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(0f, 255f))
                .add(new CastOp(INPUT_IMAGE_TYPE))
                .build();
        ByteBuffer model = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatibilityList = new CompatibilityList();
        if (compatibilityList.isDelegateSupportedOnThisDevice()) {

            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
            gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
        }
        NnApiDelegate nnApi = new NnApiDelegate();
        options.addDelegate(nnApi);


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
        int[] outputShape = interpreter.getOutputTensor(0).shape();

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
        if (outputShape != null) {
            numChannel = outputShape[1];
            numElements = outputShape[2];
        }
        Log.w(TAG, "Numchannel: " + numChannel);
        Log.w(TAG, "Numelements: " + numElements);
        Log.w(TAG, "tensorWidth: " + tensorWidth);
        Log.w(TAG, "tensorHeight: " + tensorHeight);

        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in YoloV10Detector(): " + (memEnd - memStart) + " bytes");

//        new
        tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        output = TensorBuffer.createFixedSize(
                new int[]{1, numElements, numChannel},
                OUTPUT_IMAGE_TYPE
        );
        resizedBitmap = Bitmap.createBitmap(
                tensorWidth,
                tensorHeight,
                Bitmap.Config.ARGB_8888
        );
    }


    //    public ResizeResult resize(Bitmap bitmap) {
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false);
//        Canvas canvas = new Canvas(resizedBitmap);
//        canvas.drawBitmap(bitmap, null,
//                new Rect(0, 0, tensorWidth, tensorHeight),
//                null);
//        tensorImage.load(resizedBitmap);
//        TensorImage processedImage = imageProcessor.process(tensorImage);
//        return new ResizeResult(processedImage.getBuffer(), resizedBitmap);
//    }
    private void resizeInto(Bitmap src, Bitmap dst) {
        Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, null,
                new Rect(0, 0, tensorWidth, tensorHeight),
                null);
    }

    // run the interpreter
    public ClassifyResults detect(Bitmap bitmap) {
        Log.i(TAG, "\n Function time in milliseconds (YOLOV10)");
        long now = System.currentTimeMillis();
//        ResizeResult resizeResult = resize(bitmap);
        resizeInto(bitmap, resizedBitmap);
        Log.i(TAG, "(bitmap resizing, scaling) resizeInto(): " + (System.currentTimeMillis() - now));

        tensorImage.load(resizedBitmap);
        Log.i(TAG, "tensorImage.load(): " + (System.currentTimeMillis() - now));
        TensorImage processedImage = imageProcessor.process(tensorImage);
        Log.i(TAG, "imageProcessor.process(): " + (System.currentTimeMillis() - now));
        Object input = processedImage.getBuffer();
        Object outputb = output.getBuffer();
        interpreter.run(input, outputb);
        Log.i(TAG, "interpreter.run() (oneline of code): " + (System.currentTimeMillis() - now));
        List<DetectionResult> detectionResultList = getBoundsList(bitmap, output.getFloatArray());
        return new ClassifyResults(null, detectionResultList);
    }

    //    get the coordinates and label
    public List<DetectionResult> getBoundsList(Bitmap bitmap, float[] predictions) {
        long now = System.currentTimeMillis();

        List<DetectionResult> results = new ArrayList<>();

        for (int c = 0; c < numElements; c++) {

            float maxConf = CONFIDENCE_THRESHOLD;
            int maxIdx = -1;

            int j = 4;
            int arrayIdx = c + numElements * j;

            while (j < numChannel) {
                if (predictions[arrayIdx] > maxConf) {
                    maxConf = predictions[arrayIdx];
                    maxIdx = j - 4;
                }
                j++;
                arrayIdx += numElements;
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {

                String clsName = labels.get(maxIdx);

                float cx = predictions[c];
                float cy = predictions[c + numElements];
                float w = predictions[c + numElements * 2];
                float h = predictions[c + numElements * 3];
                float x1 = cx - w / 2f;
                float y1 = cy - h / 2f;
                float x2 = cx + w / 2f;
                float y2 = cy + h / 2f;
                if (x1 < 0F || x1 > 1F) continue;
                if (y1 < 0F || y1 > 1F) continue;
                if (x2 < 0F || x2 > 1F) continue;
                if (y2 < 0F || y2 > 1F) continue;
                Log.i(TAG, String.format("Box: cx=%.1f cy=%.1f w=%.1f h=%.1f",
                        cx, cy, w, h));
                results.add(new DetectionResult(
                        maxIdx,
                        maxConf,
                        x1,
                        y1,
                        x2,
                        y2,
                        clsName,
                        0
                ));
            }
        }
        Log.i(TAG, "getBoundsList(): " + (System.currentTimeMillis() - now));
        return results;
    }


    //    close the model, this runs one time
    public void cleanup() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }

        if (labels != null) {
            labels.clear();
            labels = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        imageProcessor = null;
    }
}
