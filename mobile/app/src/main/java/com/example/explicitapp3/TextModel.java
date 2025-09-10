package com.example.explicitapp3;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.TaskJniUtils;
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TextModel {
    private static final String TAG = "TextModel";
    BertNLClassifier classifier;
    View view;
    private Context mcontext;

    public void initTextModel(Context context, String textModelName) throws IOException {
        this.mcontext = context;
        BertNLClassifier.BertNLClassifierOptions options =
                BertNLClassifier.BertNLClassifierOptions.builder().build();

        ByteBuffer modelBuffer_base = TaskJniUtils.loadMappedFile(context, textModelName);
        classifier = BertNLClassifier.createFromBufferAndOptions(modelBuffer_base, options);

    }

    public void setView(View view) {
        this.view = view;
    }

    public void textRecognition(Bitmap bitmap) {
        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, skipping text recognition");
            return;
        }

        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(mcontext).build();
            Frame frameimage = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlockSparseArray = textRecognizer.detect(frameimage);
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < textBlockSparseArray.size(); i++) {
                TextBlock textBlock = textBlockSparseArray.get(textBlockSparseArray.keyAt(i));
                text.append(" ").append(textBlock.getValue());
            }
            String ftext = text.toString().trim();
            Log.w(TAG, "textRecognition: TEXT IS: " + ftext);

            analyzeText(ftext);
            textRecognizer.release();

        } catch (Exception e) {
            Log.e(TAG, "Text recognition failed: " + e.getMessage());
        }
    }

    public void analyzeText(String text) {
        if (classifier == null) {
            Log.e(TAG, "Text classifier not initialized");
            return;
        }

        try {
            List<Category> results = classifier.classify(text);

            if (toBlock(results)){
                // TODO: open the blur screen
                if (view != null){

                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Text analysis failed: " + e.getMessage());
        }
    }

    // TODO: to change based on number of categories...
    public boolean toBlock(List<Category> results) {
        float CONFIDENCE_THRESHOLD = 0.9f;
        boolean isblocked = false;
        for (Category result : results) {
            String label = result.getLabel();
            float score = result.getScore();
            isblocked = label.equals("NSFW") && score >= CONFIDENCE_THRESHOLD || isblocked;
            Log.i(TAG, "Text Classification - Label: " + label + ", Score: " + score);
        }
        return isblocked;
    }

    public void cleanup() {
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
    }
}