package com.example.explicit_litert.Types;

import java.util.ArrayList;
import java.util.List;

//EXPERIMENTAL
//EXPERIMENTAL
public class AnchorGenerator {

    private final float baseScale;
    private final float[] scales = {
            1.0f,
            (float) Math.pow(2, 1.0 / 3.0),
            (float) Math.pow(2, 2.0 / 3.0)
    };
    private final float[] ratios = {0.5f, 1.0f, 2.0f};

    public AnchorGenerator() {
        this(4.0f);
    }

    public AnchorGenerator(float baseScale) {
        this.baseScale = baseScale;
    }
    public float[][] generateAnchors(int[][] featureSizes) {
        int[] strides = {8, 16, 32};
        List<float[]> allAnchors = new ArrayList<>();

        for (int level = 0; level < featureSizes.length; level++) {
            int h = featureSizes[level][0];
            int w = featureSizes[level][1];
            int stride = strides[level];

            float baseAnchorSize = baseScale * stride;

            // 9 cell anchors
            float[][] cellAnchors = new float[scales.length * ratios.length][4];
            int idx = 0;

            for (float scale : scales) {
                for (float ratio : ratios) {
                    float wa = (float) (baseAnchorSize * scale * Math.sqrt(ratio));
                    float ha = (float) (baseAnchorSize * scale / Math.sqrt(ratio));

                    cellAnchors[idx][0] = -wa / 2f;
                    cellAnchors[idx][1] = -ha / 2f;
                    cellAnchors[idx][2] =  wa / 2f;
                    cellAnchors[idx][3] =  ha / 2f;
                    idx++;
                }
            }

            // grid
            for (int y = 0; y < h; y++) {
                float cy = (y + 0.5f) * stride;
                for (int x = 0; x < w; x++) {
                    float cx = (x + 0.5f) * stride;

                    for (float[] a : cellAnchors) {
                        float[] anchor = new float[4];
                        anchor[0] = cx + a[0];
                        anchor[1] = cy + a[1];
                        anchor[2] = cx + a[2];
                        anchor[3] = cy + a[3];
                        allAnchors.add(anchor);
                    }
                }
            }
        }

        return allAnchors.toArray(new float[0][0]);
    }

    public float[][] decodeBoxes(float[][] boxPreds, float[][] anchors) {
        int n = boxPreds.length;
        float[][] out = new float[n][4];

        for (int i = 0; i < n; i++) {
            float ax1 = anchors[i][0];
            float ay1 = anchors[i][1];
            float ax2 = anchors[i][2];
            float ay2 = anchors[i][3];

            float aw = ax2 - ax1;
            float ah = ay2 - ay1;
            float actx = ax1 + 0.5f * aw;
            float acty = ay1 + 0.5f * ah;

            float dx = boxPreds[i][0];
            float dy = boxPreds[i][1];
            float dw = boxPreds[i][2];
            float dh = boxPreds[i][3];

            float pctx = dx * aw + actx;
            float pcty = dy * ah + acty;
            float pw = (float) Math.exp(dw) * aw;
            float ph = (float) Math.exp(dh) * ah;

            out[i][0] = pctx - 0.5f * pw; // x1
            out[i][1] = pcty - 0.5f * ph; // y1
            out[i][2] = pctx + 0.5f * pw; // x2
            out[i][3] = pcty + 0.5f * ph; // y2
        }
        return out;
    }
}
