package com.example.explicitapp3.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.Detectors.DistilBERT_tagalog_Detector;
import com.example.explicitapp3.Detectors.LSTM_Detector;
import com.example.explicitapp3.Detectors.LogisticRegression_Detector;
import com.example.explicitapp3.Detectors.NaiveBayes_Detector;
import com.example.explicitapp3.Detectors.Roberta_tagalog_Detector;
import com.example.explicitapp3.Detectors.SVM_Detector;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;

import java.util.ArrayList;
import java.util.List;

public class TextModel {
    Roberta_tagalog_Detector robertaTagalogDetector;
    DistilBERT_tagalog_Detector distilBERTDetector;
    LSTM_Detector lstmDetector;
    LogisticRegression_Detector logisticRegressionDetector;
    SVM_Detector svmDetector;
    NaiveBayes_Detector naiveBayesDetector;
    Context mcontext;
    String selectedModel;
    int statusBarHeight = 0;
    public static final String TAG = "TEXTMODEL";

    public TextModel(Context mcontext, String name) {
        this.mcontext = mcontext;
        selectedModel = name;
        Log.i(TAG, "TextModel: the selected model is " + name);
        if (name.equals(ModelTypes.ROBERTA_TAGALOG)) {
            robertaTagalogDetector = new Roberta_tagalog_Detector(mcontext);
        } else if (name.equals(ModelTypes.DISTILBERT_TAGALOG)) {
            distilBERTDetector = new DistilBERT_tagalog_Detector(mcontext);
        } else if (name.equals(ModelTypes.LSTM)) {
            lstmDetector = new LSTM_Detector(mcontext);
        } else if (name.equals(ModelTypes.LogisticRegression)) {
            logisticRegressionDetector = new LogisticRegression_Detector(mcontext);
        } else if (name.equals(ModelTypes.SVM)) {
            svmDetector = new SVM_Detector(mcontext);
        } else if (name.equals(ModelTypes.NaiveBayes)) {
            naiveBayesDetector = new NaiveBayes_Detector(mcontext);
        }
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
//        try {
            switch (selectedModel) {
                case ModelTypes.ROBERTA_TAGALOG:
                    return robertaTagalogDetector.detect(bitmap);
                case ModelTypes.DISTILBERT_TAGALOG:
                    return distilBERTDetector.detect(bitmap);
                case ModelTypes.LSTM:
                    return lstmDetector.detect(bitmap);
                case ModelTypes.LogisticRegression:
                    return logisticRegressionDetector.detect(bitmap);
                case ModelTypes.SVM:
                    return svmDetector.detect(bitmap);
                case ModelTypes.NaiveBayes:
                    return naiveBayesDetector.detect(bitmap);
                default:
                    return new ArrayList<>();
            }
//        }

    }

    public void cleanup() {
        if (robertaTagalogDetector != null) {
            robertaTagalogDetector.cleanup();
            robertaTagalogDetector = null;
        }
        if (distilBERTDetector != null) {
            distilBERTDetector.cleanup();
            distilBERTDetector = null;
        }
        if (lstmDetector != null) {
            lstmDetector.cleanup();
            lstmDetector = null;
        }
        if (svmDetector != null) {
            svmDetector.cleanup();
            svmDetector = null;
        }
        if (naiveBayesDetector != null) {
            naiveBayesDetector.cleanup();
            naiveBayesDetector = null;
        }
        if (logisticRegressionDetector != null) {
            logisticRegressionDetector.cleanup();
            logisticRegressionDetector = null;
        }

    }

}

