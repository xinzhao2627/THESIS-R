package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.ToolsNLP.Handlers.SVM_Weight_Handler;
import com.example.explicitapp3.ToolsNLP.Recognizer;
import com.example.explicitapp3.ToolsNLP.Tokenizers.SVM_tokenizer;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;
import com.example.explicitapp3.Types.TextResults;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SVM_Detector {
    private static final String TAG = "SVM";
    private Context mcontext;
    Recognizer recognizer;
    SVM_tokenizer tokenizer;
    SVM_Weight_Handler SVMWeightHandler;
    public static final String[] LABELS = ModelTypes.SVM_LABELARRAY;
    public static final String tokenizerPath = ModelTypes.SVM_TOKENIZER;
    double[] coefficients;
    double intercept;
    public SVM_Detector(Context context) {
        mcontext = context;
        try {
            recognizer = new Recognizer(context);
            this.mcontext = context;
            InputStream inputStream1 = mcontext.getAssets().open(tokenizerPath);
            InputStream inputStream2 = mcontext.getAssets().open(tokenizerPath);
            tokenizer = new SVM_tokenizer(inputStream1);
            SVMWeightHandler = new SVM_Weight_Handler();
            SVMWeightHandler.loadWeights(inputStream2);
            coefficients = SVMWeightHandler.getCoefficients();
            intercept = SVMWeightHandler.getIntercept();
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
            SVM_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            long[] inputIds = encoding.inputIds;

            // if theres no tokens found just continue
            if (inputIds.length < 1) continue;
            double max_cfs = runInference(inputIds);
            String l = max_cfs > 0.5 ? LABELS[1] : LABELS[0];
            Log.i(TAG, "left: " + t.left + " top: " + t.top + " right: " + t.right + " bottom:" + t.bottom);
            Log.i(TAG, "text: "+ "'"+text+"'" + " label: " + l + "  max cfs: " + max_cfs);
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

    public double runInference(long[] inputIds) {
        long startTime = System.currentTimeMillis();
        int seqLen = inputIds.length;

        double[] features = new double[seqLen];

        for (int i = 0; i < seqLen; i++) {
            features[i] = (double) inputIds[i];
        }

        double z = intercept;
        for (int i = 0; i < features.length; i++) {
            z += features[i] * coefficients[i];
        }
        return z;
    }

    public void cleanup() {

    }
}
