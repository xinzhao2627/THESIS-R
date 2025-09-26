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
import java.util.List;

import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

// The total time complexity of this class is:
//
public class YoloV10Detector {
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

    ImageProcessor imageProcessor;

    /*
     * The final time complexity of YoloV10Detector is:
     * = O(L + R + B + D)
     * = O(R + B) since L < N | L < B && D < N | D < B
     * = O((w * h) + N)
     *
     * Where:
     * N = numElements
     * h = tensorHeight
     * w = tensorWidth
     * B = detect() func
     * L = YoloV10Detector() func
     * D = drawBoxes() func
     * R = resize() func
     * */

    // this is O(L) where L is the set of labels
    // but L is guaranteed to be less than the time complexity of resize()
    // so the time complexity here is omitted in O(L + R + B) as L < R & L < B
    public YoloV10Detector(Context context, String chosen_image_model, String chosen_image_label) throws IOException {
        MODEL_PATH = chosen_image_model;
        LABELS_PATH = chosen_image_label;
        Log.w(TAG, "YoloV10Detector: Yolo initialized");
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(0f, 255f))
                //THIS IS Float32
                .add(new CastOp(INPUT_IMAGE_TYPE))
                .build();

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
            numElements = outputShape[1];
            numChannel = outputShape[2];
        }
        Log.w(TAG, "Numchannel: " + numChannel);
        Log.w(TAG, "Numelements: " + numElements);
        Log.w(TAG, "tensorWidth: " + tensorWidth);
        Log.w(TAG, "tensorHeight: " + tensorHeight);

        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in YoloV10Detector(): " + (memEnd - memStart) + " bytes");
    }

    // resize the image to fit in the interpreter model
    // TIME COMPLEXITY: O(WxH) or O(640x640) = constant = O(R)
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

    // O(R + I + B)
    // R = Resize function
    // I = interpreter
    // B = getBoundsList
    public ClassifyResults detect(Bitmap bitmap) {
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ResizeResult resizeResult = resize(bitmap);
        TensorBuffer output = TensorBuffer.createFixedSize(
                new int[]{1, numElements, numChannel},
                // this is float32
                OUTPUT_IMAGE_TYPE
        );
//        Log.i(TAG, "getBoundsList: predicting...");
        interpreter.run(resizeResult.buffer, output.getBuffer());
        List<DetectionResult> detectionResultList = getBoundsList(bitmap, output.getFloatArray());


        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in detect(): " + (memEnd - memStart) + " bytes");
        return new ClassifyResults(resizeResult.resizedBitmap, detectionResultList);


    }


    // O(N) = B
    // N = loop of numElements, usually between 3 to 3 digits
    public List<DetectionResult> getBoundsList(Bitmap bitmap, float[] predictions) {
        Log.w(TAG, "getBoundsList: getting bounds" );
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        List<DetectionResult> detectionResults = new ArrayList<>();

        for (int i = 0; i < numElements; i++) {
            int offset = i * numChannel;
            float confidence = predictions[offset + 4];
            if (confidence > CONFIDENCE_THRESHOLD) {
                float x = predictions[offset];
                float y = predictions[offset + 1];
                float w = predictions[offset + 2];
                float h = predictions[offset + 3];

                int labelId = (int) predictions[offset + 5];
                String label = labels.get(labelId);
                Log.w(TAG, "label: " + label + " confidence: " + confidence);
                detectionResults.add(new DetectionResult(
                        labelId, confidence,
                        x, y, w, h,
                        label,
                        0
                ));
            }
        }
        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in getBoundsList(): " + (memEnd - memStart) + " bytes");
        return detectionResults;
    }

    // The time complexity of drawBoxes() O(D) depends on the detectionResults, which came
    // from the getsBoundsList, therefore O(D) is omitted in N + D since D < B
    public Bitmap drawBoxes(Bitmap bitmap, List<DetectionResult> detectionResults) {
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(40f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        Log.w(TAG, "bitmap width: " + bitmap.getWidth() + " bitmap height: " + bitmap.getHeight());
        int padding = 5;
        for (DetectionResult dr : detectionResults) {
            float l = dr.left * bitmap.getWidth() - padding;
            float t = dr.top * bitmap.getHeight() - padding;
            float r = dr.right * bitmap.getWidth() + padding;
            float b = dr.bottom * bitmap.getHeight() + padding;

            RectF rectF = new RectF(l, t, r, b);

            canvas.drawRect(rectF, paint);
            String tt = dr.label + " " + String.format("%.2f", dr.confidence);
            canvas.drawText(tt, rectF.left, rectF.top, textPaint);
        }
        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory allocated in getBoundsList(): " + (memEnd - memStart) + " bytes");
        return mutableBitmap;
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
