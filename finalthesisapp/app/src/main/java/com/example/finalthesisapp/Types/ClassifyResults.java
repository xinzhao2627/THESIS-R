package com.example.finalthesisapp.Types;

import android.graphics.Bitmap;

import java.util.List;

public class ClassifyResults {
    public Bitmap resized_bitmap;
    public List<DetectionResult> detectionResults;

    public ClassifyResults(Bitmap bitmap, List<DetectionResult> detectionResultList){
        resized_bitmap = bitmap;
        detectionResults = detectionResultList;
    }
}
