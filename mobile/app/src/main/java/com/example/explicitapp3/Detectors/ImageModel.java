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
import com.example.explicitapp3.MainActivity;
import com.example.explicitapp3.Types.ResizeResult;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
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
import java.util.List;

/**
 * IMAGE MODEL DOCUMENTATION
 * <p> This is the image function for the overlay foreground
 * Example
 * {@code
 * ImageModel imageModel = new ImageModel();
 * imageModel.initModel();
 * imageModel.destroy();
 * }
 *
 * @author xinzhao2627 (R. Montaniel)
 * @see MainActivity
 */
public class ImageModel {
    Interpreter interpreter;
    int tensorWidth = 0;
    int tensorHeight = 0;
    int numChannel = 0;
    int numElements = 0;
    float scale = 0f;
    int zeroPoint = 0;

    List<String> labels;
    ImageProcessor imageProcessor;

    public final String TAG = "ImageModel";
    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final float CONFIDENCE_THRESHOLD = 0.1f;
    private static final float IOU_THRESHOLD = 0.5f;

    private static final DataType INPUT_IMAGE_TYPE = DataType.UINT8;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.UINT8;

    public ImageModel(Context context, String modelPath, String labelPath) throws IOException {
        labels = new ArrayList<>();

        ByteBuffer model = FileUtil.loadMappedFile(context, modelPath);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(model, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();

        // 320
        tensorWidth = inputShape[1];

        // 320
        tensorHeight = inputShape[2];

        // this is 6300
        numElements = outputShape[1];

        // this is 85
        numChannel = outputShape[2];

        Tensor outputTensor = interpreter.getOutputTensor(0);
        DataType dtype = outputTensor.dataType();

        // the output params
        Tensor.QuantizationParams qParams = outputTensor.quantizationParams();
        scale = qParams.getScale();
        zeroPoint = qParams.getZeroPoint();

        // logs
        Log.i(TAG, "Output datatype: " + dtype);
        Log.i(TAG, "Output scale: " + qParams.getScale() + ", zeroPoint: " + qParams.getZeroPoint());
        // input params
        Tensor.QuantizationParams iparam = interpreter.getInputTensor(0).quantizationParams();
        Log.i(TAG, "Input scale: " + iparam.getScale() + ", zerepoint: " + iparam.getZeroPoint());

        // get the labels.txt and add them to the array list
        try (InputStream inputStream = context.getAssets().open(labelPath)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            while (line != null && !line.isEmpty()) {
                labels.add(line);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
        }
    }

    // the interface that will be returned (buffer and the bitmap)
    // the returned resized bitmap is just for viewing purposes only


    // resize the image to fit in the interpreter model
    public ResizeResult resize(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        tensorImage.load(bitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);

        // this is just for displaying resized bitmap
        ImageProcessor resizeOnlyProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
                .build();
        TensorImage resizedImage = resizeOnlyProcessor.process(tensorImage);
        Bitmap displayBitmap = resizedImage.getBitmap();


        return new ResizeResult(processedImage.getBuffer(), displayBitmap);

    }

    public ClassifyResults classify(Bitmap bitmap) {

        int BITMAP_WIDTH = bitmap.getWidth();
        int BITMAP_HEIGHT = bitmap.getHeight();

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, 255))
                //THIS IS uint8
                .add(new CastOp(INPUT_IMAGE_TYPE))
                .build();

        ResizeResult elements = resize(bitmap);
        // why ditch v5? because it has different numchannels  and numelements output
        TensorBuffer output = TensorBuffer.createFixedSize(
                //this is {1, 6300, 85}
                new int[]{1, numElements, numChannel},
                // this is uint8
                OUTPUT_IMAGE_TYPE
        );
        interpreter.run(elements.buffer, output.getBuffer());

        TensorProcessor tensorProcessor = new TensorProcessor.Builder()
                .add(new DequantizeOp(zeroPoint, scale))
                .build();
        output = tensorProcessor.process(output);

        // I believe the prediction results would shape like:
        // [x, y, w, h, confidence (or objectness), class1, class2, class3...]
        float[] predictions = output.getFloatArray();

        // store here all the detected objects with high confidence
        List<DetectionResult> detectionResults = new ArrayList<>();

        for (int i = 0; i < numElements; i++) {

            // offset since the prediction array is flattened
            int offset = i * numChannel;
            float x = predictions[offset] * BITMAP_WIDTH;
            float y = predictions[offset + 1] * BITMAP_HEIGHT;
            float w = predictions[offset + 2] * BITMAP_WIDTH;
            float h = predictions[offset + 3] * BITMAP_HEIGHT;


            // get the regions
            int left = (int) Math.max(0, (x - w / 2));
            int top = (int) Math.max(0, (y - h / 2));
            int right = (int) Math.min(320, (x + w / 2.));
            int bottom = (int) Math.min(320, (y + h / 2.));

            float[] classScores = Arrays.copyOfRange(predictions, 5 + offset, numChannel + offset);

            // find the label of the highest interval
            float maxClassScore = -1f;
            int labelId = 0;
            for (int j = 0; j < classScores.length; j++) {
                if (classScores[j] > maxClassScore) {
                    maxClassScore = classScores[j];
                    labelId = j;
                }
            }


            float confidence = predictions[offset + 4];
            float finalConfidence = confidence;


            // check if the confidence  is high then put it in the results list
            if (finalConfidence > CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "x: " + x + " y: " + y + " w: " + w + " h: " + h + " label: " + labels.get(labelId) + " confidence: " + confidence);
                detectionResults.add(new DetectionResult(labelId, finalConfidence, left, top, right, bottom, labels.get(labelId)));
            }
        }

        return new ClassifyResults(elements.resizedBitmap, detectionResults);
    }

    public Bitmap drawBoxes(Bitmap bitmap, List<DetectionResult> detectionResults) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(40f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        for (DetectionResult dr : detectionResults) {
            RectF rectF = new RectF(
                    dr.left * mutableBitmap.getWidth(),
                    dr.top * mutableBitmap.getHeight(),
                    dr.right * mutableBitmap.getWidth(),
                    dr.bottom * mutableBitmap.getHeight()
            );
            canvas.drawRect(rectF, paint);
            canvas.drawText("", rectF.left, rectF.top, textPaint);
        }
        return mutableBitmap;
    }


    public void destroy() {
    }

}
