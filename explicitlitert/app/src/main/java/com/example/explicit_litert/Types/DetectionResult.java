package com.example.explicit_litert.Types;

public class DetectionResult {
    public int classId;
    public float confidence;
    public float left, top, right, bottom;
    public String label;
    public int modelType;
    public DetectionResult(int classId, float confidence, float left, float top, float right, float bottom, String label, int modelType) {
        this.classId = classId;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.label = label;
        this.modelType = modelType;
    }
}