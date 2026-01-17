package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.ToolsNLP.Recognizer;
import com.example.explicitapp3.ToolsNLP.Tokenizers.Roberta_tagalog_tokenizer;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;
import com.example.explicitapp3.Types.TextResults;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.task.core.TaskJniUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Roberta_tagalog_Detector {
    private static final String TAG = "ROBERTA";
    private Context mcontext;
    Interpreter interpreter;
    Recognizer recognizer;
    Roberta_tagalog_tokenizer tokenizer;
    public static final String[] LABELS = ModelTypes.ROBERTA_TAGALOG_LABELARRAY;
    public static final String tokenizerPath = ModelTypes.ROBERTA_TAGALOG_TOKENIZER;
    public static final String modelPath = ModelTypes.ROBERTA_TAGALOG_MODEL;
    public Roberta_tagalog_Detector(Context context) {
        mcontext = context;
        try {
            recognizer = new Recognizer(context);
            this.mcontext = context;
            ByteBuffer modelBuffer_base = TaskJniUtils.loadMappedFile(context, modelPath);
//            ByteBuffer modelBuffer_base = null;
            Interpreter.Options options = new Interpreter.Options();
            CompatibilityList compatibilityList = new CompatibilityList();
//            if (compatibilityList.isDelegateSupportedOnThisDevice()) {
//                Log.w(TAG, "GPU SUPPORTED");
//                GpuDelegate.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
//                GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
//                options.addDelegate(gpuDelegate);
//            } else {
            Log.w(TAG, "GPU NOT SUPPORTED");
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
//            }//
            interpreter = new Interpreter(modelBuffer_base, options);

            int inputCount = interpreter.getInputTensorCount();
            for (int i = 0; i < inputCount; i++) {
                int[] shape = interpreter.getInputTensor(i).shape();
                String name = interpreter.getInputTensor(i).name();
                Log.w(TAG, "Input " + i + " (" + name + ") shape: " + Arrays.toString(shape));
            }
            InputStream inputStream = mcontext.getAssets().open(tokenizerPath);
            tokenizer = new Roberta_tagalog_tokenizer(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> detectionResultList = new ArrayList<>();

        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        long startTime = System.currentTimeMillis();
        for (TextResults t : textResults) {
            String text = t.textContent.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
            text = text.toLowerCase().trim();
            if (text.length() < 3) continue;
            Roberta_tagalog_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            long[] inputIds = encoding.inputIds;
            long[] attentionMask = encoding.attentionMask;
            if (inputIds.length < 1) continue;

            debugInput(t.textContent, inputIds, attentionMask);
//            ensureInputOrder(); // see below
            float[][] output = runInference(inputIds, attentionMask);
            float[] probabilities = softmax(output[0]);
//            for (float[] o : output) Log.i(TAG, "output[]: " + Arrays.toString(o));

//            Log.i(TAG, "output length: " + output.length);
//            Log.i(TAG, "output array: " + Arrays.toString(output[0]));
            float max_cfs = -100f;
            String l = "";

            for (int i = 0; i < probabilities.length; i++) {
                if (probabilities[i] > max_cfs) {
//                    Log.i(TAG, LABELS[i] + "prob: " + probabilities[i]);
                    max_cfs = probabilities[i];
                    l = LABELS[i];
                }
            }
            if (l.equals("safe")) continue;
            Log.i(TAG, "word: "+t.textContent+" label: " + l + "  max cfs: " + max_cfs);
            detectionResultList.add(new DetectionResult(
                    0,
                    max_cfs,
                    t.left / bitmap.getWidth(),
                    t.top / bitmap.getHeight(),
                    t.right / bitmap.getWidth(),
                    t.bottom / bitmap.getHeight(),
                    l,
                    1
            ));
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
//        Log.i(TAG, "this for loop took: " + duration + " ms");
        return detectionResultList;
    }

    public float[][] runInference(long[] inputIds, long[] attentionMask) {
        long startTime = System.currentTimeMillis();

        int batchSize = 1;
        int seqLen = inputIds.length;
//        Log.i(TAG, "Input IDs: " + Arrays.toString(inputIds));
//        Log.i(TAG, "Attention Mask: " + Arrays.toString(attentionMask));
        int[][] ids = new int[batchSize][seqLen];
        int[][] mask = new int[batchSize][seqLen];

        for (int i = 0; i < seqLen; i++) {
            ids[0][i] = (int) inputIds[i];
            mask[0][i] = (int) attentionMask[i];
        }

//        int[] inputShape0 = interpreter.getInputTensor(0).shape();
//        int[] inputShape1 = interpreter.getInputTensor(1).shape();

        // 1 batch size 2 labels (safe/nsfw)
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        float[][] outputs = new float[outputShape[0]][outputShape[1]];

        Object[] inputArray = new Object[]{mask, ids};
        Map<Integer, Object> outputsMap = new HashMap<>();
        outputsMap.put(0, outputs);

        interpreter.runForMultipleInputsOutputs(inputArray, outputsMap);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
//        Log.i(TAG, "inference function took: " + duration + " ms");
        return outputs;
    }

    public void cleanup() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
    private void debugInput(String raw, long[] ids, long[] mask) {
        int nonPad = 0;
        for (int i = 0; i < ids.length; i++) if (ids[i] != 1) nonPad++;
        Log.i(TAG, "RAW: '" + raw + "'");
        Log.i(TAG, "IDS: " + Arrays.toString(Arrays.copyOf(ids, 16)) + " ...");
        Log.i(TAG, "MASK: " + Arrays.toString(Arrays.copyOf(mask, 16)) + " ... nonPad=" + nonPad);
    }
    private void ensureInputOrder() {
        String in0 = interpreter.getInputTensor(0).name();
        String in1 = interpreter.getInputTensor(1).name();
//        Log.i(TAG, "in0: "+in0 + "in1: "+in1);
        if (!in0.contains("input") && in1.contains("input")) {
            Log.w(TAG, "Input order suspicious: swapping not implemented, check model export.");
        }
    }
    private float[] softmax(float[] logits) {
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
