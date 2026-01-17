package com.example.explicit_litert.Types;

public class TextResults {
    public float left, top, right, bottom;
    public int modelType;
    public String textContent;
    public TextResults(float left, float top, float right, float bottom, int modelType, String textContent) {

        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.modelType = modelType;
        this.textContent = textContent;
    }
}
