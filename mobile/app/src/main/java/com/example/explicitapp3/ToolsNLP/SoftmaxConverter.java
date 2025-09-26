package com.example.explicitapp3.ToolsNLP;

public class SoftmaxConverter {
    public float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (logit > max) {
                max = logit;
            }
        }

        // Subtract max for numerical stability
        float sum = 0f;
        float[] exps = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = (float) Math.exp(logits[i] - max);
            sum += exps[i];
        }

        // Normalize
        for (int i = 0; i < logits.length; i++) {
            exps[i] /= sum;
        }

        return exps;
    }
}
