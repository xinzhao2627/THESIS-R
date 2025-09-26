package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import com.example.explicitapp3.ToolsNLP.Recognizer;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;
import com.example.explicitapp3.Types.TextResults;

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
public class Scrapped_distilbert_tagalog_detector {
    private static final String TAG = "DistilBERT_tagalog_model";
    BertNLClassifier classifier;
    View view;
    Recognizer recognizer;
    private Context mcontext;

    /**
     * Initializes the text classification model that will be used for detecting NSFW
     *
     * @param context       The context (or activity) of the app
     *                      <p>example usage:
     *                      <pre>{@code
     *                                                                initTextModel(getApplicationContext(), "assets/path/to/model.tflite")
     *                                                                }</pre></p>
     */
    public Scrapped_distilbert_tagalog_detector (Context context){
        try {
            this.mcontext = context;
            BertNLClassifier.BertNLClassifierOptions options = BertNLClassifier.BertNLClassifierOptions.builder().build();
            ByteBuffer modelBuffer_base = TaskJniUtils.loadMappedFile(context, ModelTypes.DISTILBERT_TAGALOG_MODEL);
            classifier = BertNLClassifier.createFromBufferAndOptions(modelBuffer_base, options);
            recognizer = new Recognizer(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    /**
     * Analyzes and classifies the text
     * <p>
     * The bitmap of the projected view
     */
    public List<DetectionResult>  detect(Bitmap bitmap) {
        if (classifier == null) {
            Log.e(TAG, "Text classifier not initialized");
            return new ArrayList<>();
        }
        try {
            List<DetectionResult> detectionResultList = new ArrayList<>();
            List<TextResults> textResults = recognizer.textRecognition(bitmap);
            for (TextResults t : textResults) {
                String textContent = t.textContent;
                List<Category> results = classifier.classify(textContent);
                runInference(results, detectionResultList, t);
            }
            return detectionResultList;
        } catch (Exception e) {
            Log.e(TAG, "Text analysis failed: " + e.getMessage());
        }
        return new ArrayList<>();
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
    public void runInference(List<Category> results,List<DetectionResult>detectionResultList, TextResults textResult) {
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
            Log.i(TAG, "Label: " + label + ", Score: " + score);
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