package com.example.explicitapp3;

public class DetectionResult {
    public int classId;
    public float confidence;
    public float left, top, right, bottom;
    public String label;

    public DetectionResult(int classId, float confidence, float left, float top, float right, float bottom, String label) {
        this.classId = classId;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.label = label;
    }
}