package com.example.explicitapp3;

import android.graphics.Bitmap;

import java.util.List;

public class ClassifyResults {
    Bitmap resized_bitmap;
    List<DetectionResult> detectionResults;

    public ClassifyResults(Bitmap bitmap, List<DetectionResult> detectionResultList){
        resized_bitmap = bitmap;
        detectionResults = detectionResultList;
    }
}
