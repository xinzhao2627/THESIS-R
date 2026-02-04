package com.example.finalthesisapp.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.finalthesisapp.Detectors.LSTM_Detector;
import com.example.finalthesisapp.Types.DetectionResult;
import com.example.finalthesisapp.Types.ModelTypes;

import java.util.ArrayList;
import java.util.List;

public class TextModel {
    LSTM_Detector lstmDetector;

    Context mcontext;
    String selectedModel;
    int statusBarHeight = 0;
    public static final String TAG = "TEXTMODEL";
    int etn = 60;

    public TextModel(Context mcontext, String name, int etn) {
        this.mcontext = mcontext;
        this.etn = etn;
        selectedModel = name;
        Log.i(TAG, "TextModel: the selected model is " + name);
        if (name.equals(ModelTypes.LSTM)) {
            lstmDetector = new LSTM_Detector(mcontext, etn, ModelTypes.LSTM);
        } else if (name.equals(ModelTypes.BILSTM)) {
            lstmDetector = new LSTM_Detector(mcontext, etn, ModelTypes.BILSTM);
        }
//            else if (name.equals(ModelTypes.LogisticRegression)) {
//            logisticRegressionDetector = new LogisticRegression_Detector(mcontext);
//        } else if (name.equals(ModelTypes.SVM)) {
//            svmDetector = new SVM_Detector(mcontext);
//        } else if (name.equals(ModelTypes.NaiveBayes)) {
//            naiveBayesDetector = new NaiveBayes_Detector(mcontext);
//        }
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
//        try {
        switch (selectedModel) {
            case ModelTypes.LSTM:
//                    return null;
                return lstmDetector.detect(bitmap);
            case ModelTypes.BILSTM:
                return lstmDetector.detect(bitmap);
            default:
                return new ArrayList<>();
        }
//        }

    }

    public void cleanup() {
        if (lstmDetector != null) {
            lstmDetector.cleanup();
            lstmDetector = null;
        }
//        if (svmDetector != null) {
//            svmDetector.cleanup();
//            svmDetector = null;
//        }
//        if (naiveBayesDetector != null) {
//            naiveBayesDetector.cleanup();
//            naiveBayesDetector = null;
//        }
//        if (logisticRegressionDetector != null) {
//            logisticRegressionDetector.cleanup();
//            logisticRegressionDetector = null;
//        }

    }

}

