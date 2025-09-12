package com.example.explicitapp3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
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
import java.nio.ByteOrder;
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
    List<String> labels;
    ImageProcessor imageProcessor;
    public final String TAG = "ImageModel";
    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final float CONFIDENCE_THRESHOLD = 0.1f;
    private static final float IOU_THRESHOLD = 0.5f;
    float scale = 0f;
    int zeroPoint = 0;
    private static final DataType INPUT_IMAGE_TYPE = DataType.UINT8;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.UINT8;
    MetadataExtractor.QuantizationParams inputQuantizeParam = new MetadataExtractor.QuantizationParams(0.003921568859368563f, 0);
//    MetadataExtractor.QuantizationParams outputQuantizeParam = new MetadataExtractor.QuantizationParams(0.00739737693220377f, 2);
MetadataExtractor.QuantizationParams outputQuantizeParam = new MetadataExtractor.QuantizationParams(0.006305381190031767f, 5);

    public ImageModel(Context context, String modelPath, String labelPath) throws IOException {
        labels = new ArrayList<>();

        ByteBuffer model = FileUtil.loadMappedFile(context, modelPath);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(model, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        int[] outputShape = interpreter.getOutputTensor(0).shape();

        tensorWidth = inputShape[1];
        tensorHeight = inputShape[2];
        numElements = outputShape[1];
        numChannel = outputShape[2];

        Tensor outputTensor = interpreter.getOutputTensor(0);
        DataType dtype = outputTensor.dataType();
        Tensor.QuantizationParams qParams = outputTensor.quantizationParams();
        Log.i(TAG, "Output datatype: " + dtype);
        Log.i(TAG, "Output scale: " + qParams.getScale() + ", zeroPoint: " + qParams.getZeroPoint());
        Tensor.QuantizationParams iparam = interpreter.getInputTensor(0).quantizationParams();
        Log.i(TAG, "Input scale: " + iparam.getScale() + ", zerepoint: " + iparam.getZeroPoint());
        scale = qParams.getScale();
        zeroPoint = qParams.getZeroPoint();
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
    public class ResizeResult {
        public ByteBuffer buffer;
        public Bitmap resizedBitmap;

        public ResizeResult(ByteBuffer buffer, Bitmap resizedBitmap) {
            this.buffer = buffer;
            this.resizedBitmap = resizedBitmap;
        }
    }
    public ResizeResult resize(Bitmap bitmap) {
        // Create separate processor for just resizing (for display)
        ImageProcessor resizeOnlyProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        tensorImage.load(bitmap);
        TensorImage resizedImage = resizeOnlyProcessor.process(tensorImage);
        Bitmap displayBitmap = resizedImage.getBitmap();

        TensorImage processedImage = imageProcessor.process(tensorImage);
        return new ResizeResult(processedImage.getBuffer(), displayBitmap);

    }

    public ClassifyResults classify(Bitmap bitmap) {

        int BITMAP_WIDTH = bitmap.getWidth();
        int BITMAP_HEIGHT = bitmap.getHeight();
        Log.i(TAG, "Model expects: " + tensorWidth + "x" + tensorHeight);
        Log.i(TAG, "Bitmap size: " + BITMAP_WIDTH + "x" + BITMAP_HEIGHT);
        Log.i(TAG, "Expected buffer size: " + (tensorWidth * tensorHeight * 3)); // RGB channels
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, 255))
                .add(new CastOp(INPUT_IMAGE_TYPE))
                .build();
        ResizeResult elements = resize(bitmap);
        Log.w(TAG, "NumElement: " + numElements + " numChannel: " + numChannel);
        TensorBuffer output = TensorBuffer.createFixedSize(
                new int[]{1, numElements, numChannel},
                OUTPUT_IMAGE_TYPE
        );


        interpreter.run(elements.buffer, output.getBuffer());
        TensorProcessor tensorProcessor = new TensorProcessor.Builder()
                .add(new DequantizeOp(outputQuantizeParam.getZeroPoint(), outputQuantizeParam.getScale()))
                .build();
        output = tensorProcessor.process(output);

        // [x, y, w, h, confidence, class1, class2, class3...]
        // THERE ARE PROBALBLY FCKING 600K ROWS HERE (NUMCHANNEL * NUMELEMENT)
        // so every 85th index indicates a new object
        float[] predictions = output.getFloatArray();
        List<DetectionResult> detectionResults = new ArrayList<>();

        for (int i = 0; i < numElements; i++) {
            int confidentScoreIndex = 4;
            int offset = i * numChannel;
            float x = predictions[offset] * BITMAP_WIDTH;
            float y = predictions[offset + 1] * BITMAP_HEIGHT;
            float w = predictions[offset + 2] * BITMAP_WIDTH;
            float h = predictions[offset + 3] * BITMAP_HEIGHT;
            float confidence = predictions[offset + 4];

            int left = (int) Math.max(0, (x - w / 2));
            int top = (int) Math.max(0, (y - h / 2));
            int right = (int) Math.max(BITMAP_WIDTH, (x + w / 2.));
            int bottom = (int) Math.max(BITMAP_HEIGHT, (y + h / 2.));
//            float objectness = (predictions[offset + 4] - zeroPoint) * scale;

            /** copy all the confident scores of the object of a single detection
             * why +5? the first 4 are x y w h, the 5th starts the object..
             * so From there until the end of the detection (e.g 5 + 85 upto 85 + 85 would be 90 -> 170)
             */
            float[] classScores = Arrays.copyOfRange(predictions, 5 + offset, numChannel + offset);

            float maxClassScore = -1f;
            int labelId = 0;
            for (int j = 0; j < classScores.length; j++) {
                float individual_objectness = (classScores[j] - zeroPoint) * scale;
                if (individual_objectness > maxClassScore) {
                    maxClassScore = individual_objectness;
                    labelId = j;
                }
            }
//            Log.i(TAG, "raw x,y,w,h: " + predictions[offset] + "," +
//                    predictions[offset+1] + "," +
//                    predictions[offset+2] + "," +
//                    predictions[offset+3]);
//            Log.w(TAG, "x: " + x + " y: " + y + " w: " + w + " h: " + h + " label: " + labels.get(labelId) + " confidence: " + confidence);
            if (confidence > 0.0){
                Log.w(TAG+ " Confi", labels.get(labelId) + " confidence: " + confidence);

            }

            if (confidence > CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "x: " + x + " y: " + y + " w: " + w + " h: " + h + " label: " + labels.get(labelId) + " confidence: " + confidence);

                detectionResults.add(new DetectionResult(labelId, confidence, left, top, right, bottom, labels.get(labelId)));
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
