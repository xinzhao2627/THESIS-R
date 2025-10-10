package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.ToolsNLP.Handlers.NaiveBayes_Weight_Handler;
import com.example.explicitapp3.ToolsNLP.Recognizer;
import com.example.explicitapp3.ToolsNLP.Tokenizers.NaiveBayes_tokenizer;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;
import com.example.explicitapp3.Types.TextResults;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NaiveBayes_Detector {
    private static final String TAG = "NaiveBayes";
    private Context mcontext;
    Recognizer recognizer;
    NaiveBayes_tokenizer tokenizer;
    NaiveBayes_Weight_Handler naiveBayesWeightHandler;
    public static final String[] LABELS = ModelTypes.NaiveBayes_LABELARRAY;
    public static final String tokenizerPath = ModelTypes.NaiveBayes_TOKENIZER;
    double[][] feature_log_prob;
    double[] class_log_prior;

    public NaiveBayes_Detector(Context context) {
        mcontext = context;
        try {
            recognizer = new Recognizer(context);
            this.mcontext = context;
            InputStream inputStream1 = mcontext.getAssets().open(tokenizerPath);
            InputStream inputStream2 = mcontext.getAssets().open(tokenizerPath);
            tokenizer = new NaiveBayes_tokenizer(inputStream1);
            naiveBayesWeightHandler = new NaiveBayes_Weight_Handler();
            naiveBayesWeightHandler.loadWeights(inputStream2);

            feature_log_prob = naiveBayesWeightHandler.getFeature_log_prob();
            class_log_prior = naiveBayesWeightHandler.getClass_log_prior();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> detectionResultList = new ArrayList<>();
        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        long startTime = System.currentTimeMillis();
        for (TextResults t : textResults) {
            String text = t.textContent.toLowerCase().trim();
            if (text.isEmpty()) continue;
            NaiveBayes_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            long[] inputIds = encoding.inputIds;

            // if theres no tokens found just continue
            if (inputIds.length < 1) continue;
            double[] output = runInference(inputIds);
            double max_cfs = Math.max(output[0], output[1]);
            String l = output[0] > output[1] ? LABELS[0] : LABELS[1];
            Log.i(TAG, "left: " + t.left + " top: " + t.top + " right: " + t.right + " bottom:" + t.bottom);
            Log.i(TAG, "text: " + "'" + text + "'" + " label: " + l + "  max cfs: " + max_cfs);
            detectionResultList.add(new DetectionResult(
                    0,
                    (float) max_cfs,
                    t.left / bitmap.getWidth(),
                    t.top / bitmap.getHeight(),
                    t.right / bitmap.getWidth(),
                    t.bottom / bitmap.getHeight(),
                    l,
                    1
            ));
            Log.i(TAG, "\n");
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        return detectionResultList;
    }

    public double[] runInference(long[] inputIds) {
        // input id is a feature vector
        int seqLen = inputIds.length;

        // calculate prob result
        int n = feature_log_prob.length;
        double[] logprob = new double[n];

        for (int i = 0; i < n; i++) {
            double sum = class_log_prior[i];

            for (int j = 0; j < inputIds.length; j++) {
                // if theres a value on that particular bag of word feature index
                // then add it to sum based on the number of words it has on that idx
                if (inputIds[j] > 0) {
                    sum += feature_log_prob[i][j] * inputIds[j];
                }
            }
            logprob[i] = sum;
        }

        // logprob[0] for safe, then logprob[1] for nsfw's score
        return logprob;
    }

    public void cleanup() {

    }
}
