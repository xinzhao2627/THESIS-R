package com.example.explicit_litert.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicit_litert.Detectors.Mobilebert_Detector;
import com.example.explicit_litert.Detectors.Roberta_tagalog_Detector;
import com.example.explicit_litert.Detectors.DistilBERT_tagalog_Detector;
import com.example.explicit_litert.Detectors.LSTM_Detector;
//import com.example.explicit_litert.Detectors.LogisticRegression_Detector;
//import com.example.explicit_litert.Detectors.NaiveBayes_Detector;

//import com.example.explicit_litert.Detectors.SVM_Detector;
import com.example.explicit_litert.Types.DetectionResult;
import com.example.explicit_litert.Types.ModelTypes;

import java.util.ArrayList;
import java.util.List;

public class TextModel {
    Roberta_tagalog_Detector robertaTagalogDetector;
    DistilBERT_tagalog_Detector distilBERTDetector;
    LSTM_Detector lstmDetector;
    Mobilebert_Detector mobilebertDetector;
    //    LogisticRegression_Detector logisticRegressionDetector;
//    SVM_Detector svmDetector;
//    NaiveBayes_Detector naiveBayesDetector;
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
        if (name.equals(ModelTypes.ROBERTA_TAGALOG)) {
            robertaTagalogDetector = new Roberta_tagalog_Detector(mcontext, etn);
        } else if (name.equals(ModelTypes.DISTILBERT_TAGALOG) || name.equals(ModelTypes.TINYBERT)) {
            distilBERTDetector = new DistilBERT_tagalog_Detector(mcontext, etn, name);
        } else if (name.equals(ModelTypes.LSTM)) {
            lstmDetector = new LSTM_Detector(mcontext, etn, ModelTypes.LSTM);
        } else if (name.equals(ModelTypes.MOBILEBERT)) {
            mobilebertDetector = new Mobilebert_Detector(mcontext, etn);
        } else if (name.equals(ModelTypes.BILSTM)){
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
            case ModelTypes.ROBERTA_TAGALOG:
//                    return null;
                return robertaTagalogDetector.detect(bitmap);
            case ModelTypes.DISTILBERT_TAGALOG:
//                    return null;
                return distilBERTDetector.detect(bitmap);
            case ModelTypes.TINYBERT:
//                    return null;
                return distilBERTDetector.detect(bitmap);
            case ModelTypes.MOBILEBERT:
                return mobilebertDetector.detect(bitmap);
            case ModelTypes.LSTM:
//                    return null;
                return lstmDetector.detect(bitmap);
            case ModelTypes.BILSTM:
                return lstmDetector.detect(bitmap);
            case ModelTypes.LogisticRegression:
                return null;
//                    return logisticRegressionDetector.detect(bitmap);
            case ModelTypes.SVM:
                return null;
//                    return svmDetector.detect(bitmap);
            case ModelTypes.NaiveBayes:
                return null;
//                    return naiveBayesDetector.detect(bitmap);
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
        if (mobilebertDetector != null) {
            mobilebertDetector.cleanup();
            mobilebertDetector = null;
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

