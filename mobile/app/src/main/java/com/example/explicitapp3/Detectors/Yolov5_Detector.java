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
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.common.ops.QuantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.metadata.MetadataExtractor;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

// The total time complexity of this class is:
//
public class Yolov5_Detector {
    Context context;
    public final String TAG = "YoloV5Detector";
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
    TensorImage tensorImage;
    TensorBuffer output;
    Bitmap resizedBitmap;

    public Yolov5_Detector(Context context, String chosen_image_model, String chosen_image_label) throws IOException {
        MODEL_PATH = chosen_image_model;
        LABELS_PATH = chosen_image_label;


        ByteBuffer model = FileUtil.loadMappedFile(context, MODEL_PATH);
        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatibilityList = new CompatibilityList();
        if (compatibilityList.isDelegateSupportedOnThisDevice()) {
            Log.w(TAG, "GPU SUPPORTED");
            GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
            gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            Log.w(TAG, "GPU NOT SUPPORTED");
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
        }

        interpreter = new Interpreter(model, options);

        // POPULATE LABELS LIST
//        labels = new ArrayList<>();
//        try (InputStream inputStream = context.getAssets().open(LABELS_PATH)) {
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//            String line = bufferedReader.readLine();
//            while (line != null && !line.isEmpty()) {
//                labels.add(line);
//                line = bufferedReader.readLine();
//            }
//            bufferedReader.close();
//        }
        labels = FileUtil.loadLabels(context,LABELS_PATH);
        Log.i(TAG, "Success reading label: " + LABELS_PATH);

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

        tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        //        for yolov5nu new:
//        Numchannel: 84
//        Numelements: 8400

//        for yolov5s old:
//        Numchannel: 6300
//        Numelements: 85
//        output = TensorBuffer.createFixedSize(
//                new int[]{1, numChannel, numElements},
//                OUTPUT_IMAGE_TYPE
//        );

        //s
        output = TensorBuffer.createFixedSize(
                new int[]{1, numElements, numChannel},
                OUTPUT_IMAGE_TYPE
        );
        imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(0f, 255f))
                .add(new CastOp(INPUT_IMAGE_TYPE))
//                .add(new ResizeOp(tensorHeight, tensorWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build();
        resizedBitmap = Bitmap.createBitmap(
                tensorWidth,
                tensorHeight,
                Bitmap.Config.ARGB_8888
        );
    }

    private void resizeInto(Bitmap src, Bitmap dst) {
        Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, null,
                new Rect(0, 0, tensorWidth, tensorHeight),
                null);
    }

    // run the interpreter
    public ClassifyResults detect(Bitmap bitmap) {
        Log.i(TAG, "\n Function time in milliseconds (YOLOV10) size widht: " + bitmap.getWidth() + " height: " + bitmap.getHeight());
        long now = System.currentTimeMillis();

        resizeInto(bitmap, resizedBitmap);
//        Bitmap resized = Bitmap.createScaledBitmap(rgbBitmap, tensorWidth, tensorHeight, true);

        tensorImage.load(resizedBitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);

        ByteBuffer input = processedImage.getBuffer();
        input.rewind();
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
//
////
//
//
//        int numBoxes = numChannel;
//        int stride = numElements;
////        int numClasses = stride - 5;
//
//        for (int i = 0; i < numBoxes; i++) {
//
//            int offset = i * stride;
//
//            float cx = predictions[offset + 0];
//            float cy = predictions[offset + 1];
//            float w  = predictions[offset + 2];
//            float h  = predictions[offset + 3];
//
//            float objectness = predictions[offset + 4];
//            if (objectness < CONFIDENCE_THRESHOLD) continue;
//
//            int bestClass = -1;
//            float bestClassScore = 0f;
//            float[] classScores = Arrays.copyOfRange(predictions, 5 + offset, stride + offset);
//            for (int c = 0; c < classScores.length; c++) {
//                if (classScores[c] > bestClassScore) {
//                    bestClassScore = classScores[c];
//                    bestClass = c;
//                }
//            }
//
//            float confidence = objectness * bestClassScore;
//            if (confidence < CONFIDENCE_THRESHOLD) continue;
//
//            float x1 = cx - w / 2f;
//            float y1 = cy - h / 2f;
//            float x2 = cx + w / 2f;
//            float y2 = cy + h / 2f;
//
////            // clamp (YOLOv5 Python behavior)
////            x1 = Math.max(0f, Math.min(1f, x1));
////            y1 = Math.max(0f, Math.min(1f, y1));
////            x2 = Math.max(0f, Math.min(1f, x2));
////            y2 = Math.max(0f, Math.min(1f, y2));
//            Log.i(TAG, "getBoundsList: best class is: "+bestClass + " " +labels.get(bestClass));
//            results.add(new DetectionResult(
//                    bestClass,
//                    confidence,
//                    x1,
//                    y1,
//                    x2,
//                    y2,
//                    labels.get(bestClass),
//                    0
//            ));
//        }

//        for yolov5nu:
        int numBoxes = numElements;   // 8400
        int numChannels = numChannel; // 84
        int numClasses = numChannels - 4;

        for (int i = 0; i < numBoxes; i++) {

            float cx = predictions[0 * numBoxes + i];
            float cy = predictions[1 * numBoxes + i];
            float w  = predictions[2 * numBoxes + i];
            float h  = predictions[3 * numBoxes + i];

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

            float x1 = cx - w / 2f;
            float y1 = cy - h / 2f;
            float x2 = cx + w / 2f;
            float y2 = cy + h / 2f;
            Log.i(TAG, "getBoundsList: best class is: "+bestClass + " " +labels.get(bestClass));

            results.add(new DetectionResult(
                    bestClass,
                    bestScore,
                    x1, y1, x2, y2,
                    labels.get(bestClass),
                    0
            ));
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
