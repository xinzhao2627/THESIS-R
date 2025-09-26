package com.example.explicitapp3.Parent;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.explicitapp3.Detectors.DistilBERT_tagalog_Detector;
import com.example.explicitapp3.Detectors.Roberta_tagalog_Detector;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;

import java.util.List;

public class TextModel {
    Roberta_tagalog_Detector robertaTagalogDetector;
    DistilBERT_tagalog_Detector distilBERTDetector;
    Context mcontext;
    String selectedModel;

    public TextModel(Context mcontext, String name)  {
        this.mcontext = mcontext;
        selectedModel = name;

        if (name.equals(ModelTypes.ROBERTA_TAGALOG)) {
            robertaTagalogDetector = new Roberta_tagalog_Detector(mcontext);
        } else if (name.equals(ModelTypes.DISTILBERT_TAGALOG)) {
            distilBERTDetector = new DistilBERT_tagalog_Detector(mcontext);
        }
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        if (selectedModel.equals(ModelTypes.ROBERTA_TAGALOG)) {
            return robertaTagalogDetector.detect(bitmap);
        } else if (selectedModel.equals(ModelTypes.DISTILBERT_TAGALOG)) {
            return distilBERTDetector.detect(bitmap);
        } else {
            return robertaTagalogDetector.detect(bitmap);
        }
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

    }

}

