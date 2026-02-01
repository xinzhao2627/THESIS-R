package com.example.explicit_litert.Detectors;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicit_litert.ToolsNLP.Recognizer;
import com.example.explicit_litert.ToolsNLP.Tokenizers.Roberta_tagalog_tokenizer;
import com.example.explicit_litert.Types.DetectionResult;
import com.example.explicit_litert.Types.ModelTypes;
import com.example.explicit_litert.Types.TextResults;

import org.tensorflow.lite.Interpreter;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
    public Roberta_tagalog_Detector(Context context, int etn) {
        mcontext = context;
        try {
            recognizer = new Recognizer(context, etn);
            this.mcontext = context;
            ByteBuffer modelBuffer_base = loadModelFile(context, modelPath);
//            ByteBuffer modelBuffer_base = null;
            Interpreter.Options options = new Interpreter.Options();
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
    private static MappedByteBuffer loadModelFile(Context context, String assetPath)
            throws IOException {

        AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();

        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> detectionResultList = new ArrayList<>();

        long nnn = System.currentTimeMillis();
        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        Log.i("recognizering", "recognizer_ms dostroberta: "+(System.currentTimeMillis()-nnn));
        long startTime = System.currentTimeMillis();
        for (TextResults t : textResults) {
            String text = t.textContent.replaceAll("[^a-z\\s]", " ").replaceAll("\\s+", " ").trim();
            text = text.toLowerCase().trim();
            if (text.length() < 3) continue;
            Log.i("gpteam", "new robertatagalogordostroberta");

            long now = System.currentTimeMillis();
            Roberta_tagalog_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            Log.i("gpteam", "encode "+(System.currentTimeMillis()-now));

            long[] inputIds = encoding.inputIds;
            long[] attentionMask = encoding.attentionMask;
            if (inputIds.length < 1) continue;

//            debugInput(t.textContent, inputIds, attentionMask);
//            ensureInputOrder(); // see below
            float[][] output = runInference(attentionMask, inputIds);

            now = System.currentTimeMillis();
            float[] probabilities = softmax(output[0]);
            Log.i("gpteam", "softmax "+(System.currentTimeMillis()-now));

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
//             && max_cfs > 0.5
            if (l.equals("nsfw")){
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
//            Log.i(TAG, "word: "+t.textContent+" label: " + l + "  max cfs: " + max_cfs);

        }
//        Log.i(TAG, "this for loop took: " + duration + " ms");
        return detectionResultList;
    }

    public float[][] runInference(long[] attentionMask, long[] inputIds) {
        if (interpreter == null) return new float[][]{};
        try {
            long now = System.currentTimeMillis();

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
            Object[] inputArray = new Object[]{mask, ids};
            Log.i("gpteam", "inputbuffer "+(System.currentTimeMillis()-now));

            now = System.currentTimeMillis();
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            float[][] outputs = new float[outputShape[0]][outputShape[1]];
            Map<Integer, Object> outputsMap = new HashMap<>();
            outputsMap.put(0, outputs);
            Log.i("gpteam", "outputbuffer "+(System.currentTimeMillis()-now));

            now = System.currentTimeMillis();
            interpreter.runForMultipleInputsOutputs(inputArray, outputsMap);
            Log.i("gpteam", "model.run "+(System.currentTimeMillis()-now));

//        Log.i(TAG, "inference function took: " + duration + " ms");
            return outputs;
        } catch (Exception e) {
            Log.i(TAG, "runInference error: " +e.getMessage());
            return new float[][]{};
        }


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
