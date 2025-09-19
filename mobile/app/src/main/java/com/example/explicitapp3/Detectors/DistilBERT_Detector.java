package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.TextResults;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.TaskJniUtils;
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * TextModel class provides text recognition and classification functionality using Google Text recognizer
 * and TensorFlow Lite BERT classifier. It can detect text in images and classify whether the content
 * should be blocked based on NSFW (Not Safe For Work) criteria.
 *
 * <p>This class integrates:
 * <ul>
 *   <li>Google Vision Text Recognition API for extracting text from bitmaps</li>
 *   <li>TensorFlow Lite BERT NL Classifier for content classification</li>
 *   <li>Content filtering based on confidence thresholds</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * TextModel textModel = new TextModel();
 * textModel.initTextModel(context, "model.tflite");
 * textModel.setView(overlayView);
 * textModel.textRecognition(bitmap);
 * }</pre>
 *
 * @author xinzhao2627 (R. Montaniel)
 * @version 1.0
 * @since 9/11/25
 */
public class DistilBERT_Detector {
    private static final String TAG = "TextModel";

    /**
     *
     */
    BertNLClassifier classifier;
    View view;
    private Context mcontext;

    /**
     * Initializes the text classification model that will be used for detecting NSFW
     *
     * @param context       The context (or activity) of the app
     * @param textModelName The file path for the tflite model (please be sure to include the metadata)
     *                      <p>example usage:
     *                      <pre>{@code
     *                                                                initTextModel(getApplicationContext(), "assets/path/to/model.tflite")
     *                                                                }</pre></p>
     */
    public DistilBERT_Detector(Context context, String textModelName) throws IOException {
        this.mcontext = context;
        BertNLClassifier.BertNLClassifierOptions options = BertNLClassifier.BertNLClassifierOptions.builder().build();

        ByteBuffer modelBuffer_base = TaskJniUtils.loadMappedFile(context, textModelName);
        classifier = BertNLClassifier.createFromBufferAndOptions(modelBuffer_base, options);

    }

    /**
     * Setup a view to allow TextModel.java
     * to configure the popup visibility, a null indicates that
     * there is no censorship mechanism
     *
     * @param view The popup view that is visible when it detects a censorship
     */
    public void setView(View view) {
        this.view = view;
    }

    /**
     * Accepts a bitmap that contains the frame of the projected view (app content), it automatically analyzes the text
     *
     * @param bitmap The bitmap of the projected view
     */
    public List<DetectionResult> textRecognition(Bitmap bitmap) {

        List<DetectionResult> detectionResultList = new ArrayList<>();
        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, skipping text recognition");
            return detectionResultList ;
        }
        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(mcontext).build();
            Frame frameimage = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlockSparseArray = textRecognizer.detect(frameimage);
            List<TextResults> textList = new ArrayList<>();
            Log.w(TAG, "textRecognition: hihi");
            // loops through all detected text (textblock) (usually this is just 1 length)
            for (int i = 0; i < textBlockSparseArray.size(); i++) {
                TextBlock textBlock = textBlockSparseArray.get(textBlockSparseArray.keyAt(i));
                // get the
//                String textb  = textBlock.getValue();
//                Log.w(TAG, i+" "+textb);
                for (Text t : textBlock.getComponents()) {
//                    Log.w(TAG, i + " textRecognition: " + t.getValue());
                    Rect rect = t.getBoundingBox();
//                    Log.w(TAG, "left: " + rect.left + " right: " + rect.right + " top: " + rect.top + " bottom: " + rect.bottom);

                    textList.add(new TextResults(rect.left, rect.top, rect.right, rect.bottom, 1, t.getValue()));
                    detectionResultList.add(new DetectionResult(
                            0,
                            0,
                            (float) rect.left,
                            (float) rect.top,
                            (float) rect.right,
                            (float) rect.bottom,
                            "t",
                            1
                    ));
                }
            }
//            Log.w(TAG, "textRecognition: TEXT IS: " + ftext);

//            analyzeText(textList, detectionResultList);
            textRecognizer.release();

        } catch (Exception e) {
            Log.e(TAG, "Text recognition failed: " + e.getMessage());
        }
        return detectionResultList;
    }

    /**
     * Analyzes and classifies the text
     * <p>
     * The bitmap of the projected view
     */
    public void analyzeText(List<TextResults> textList, List<DetectionResult> detectionResultList) {
        if (classifier == null) {
            Log.e(TAG, "Text classifier not initialized");
            return;
        }

        try {
            for (TextResults t : textList) {
                String textContent = t.textContent;
                List<Category> results = classifier.classify(textContent);
                toBlock(results, detectionResultList, t);
            }

        } catch (Exception e) {
            Log.e(TAG, "Text analysis failed: " + e.getMessage());
        }
    }

    /**
     * It determines whether content should be blocked based on classification res
     * Checks if any category is labeled as "NSFW" with a confidence score above the threshold.
     *
     * @param results List of classification categories with confidence scores
     * @return {@code true} if content should be blocked (NSFW content detected with high confidence),
     * {@code false} otherwise
     * @implNote Current confidence threshold is set to 0.9f (90%)
     * @todo Consider making the number of categories and threshold configurable
     * @see Category
     */
    public boolean toBlock(List<Category> results, List<DetectionResult> detectionResultList, TextResults textResult) {
        float CONFIDENCE_THRESHOLD = 0.9f;
        float cfs = 0;
        String l = "";
        boolean isblocked = false;
        for (Category result : results) {
            String label = result.getLabel();
            float score = result.getScore();
            if (label.equals("NSFW") && score >= CONFIDENCE_THRESHOLD) {
                isblocked = true;
                cfs = score;
                l = label;
            }
//            Log.i(TAG, "Text Classification - Label: " + label + ", Score: " + score);
        }
        if (isblocked) {
            detectionResultList.add(new DetectionResult(
                    0,
                    cfs,
                    textResult.left,
                    textResult.top,
                    textResult.right,
                    textResult.bottom,
                    l,
                    1
            ));
        }
        return isblocked;
    }

    /**
     * Cleans up resources by closing the classifier and releasing memory.
     * Can be called when the TextModel instance is no longer needed to prevent memory leaks.
     *
     * @apiNote This method is safe to call multiple times
     * @see BertNLClassifier#close()
     */
    public void cleanup() {
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
    }
}