package com.example.explicitapp3.Detectors;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.ToolsNLP.Tokenizers.LSTM_tokenizer;
import com.example.explicitapp3.ToolsNLP.Recognizer;
import com.example.explicitapp3.Types.DetectionResult;
import com.example.explicitapp3.Types.ModelTypes;
import com.example.explicitapp3.Types.TextResults;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.task.core.TaskJniUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LSTM_Detector {
    private static final String TAG = "LSTM_TAGALOG";
    private Context mcontext;
    Interpreter interpreter;
    Recognizer recognizer;
    LSTM_tokenizer tokenizer;

    public static final String[] LABELS = ModelTypes.LSTM_LABELARRAY;
    public static final String tokenizerPath = ModelTypes.LSTM_TOKENIZER;
    public static final String modelPath = ModelTypes.LSTM_MODEL;
    int[][] ids;
    int[][] mask;
    float[][] outputs;

    public LSTM_Detector(Context context) {
        mcontext = context;
        try {
            recognizer = new Recognizer(context);

            this.mcontext = context;
            ByteBuffer modelBuffer_base = TaskJniUtils.loadMappedFile(context, modelPath);
            Interpreter.Options options = new Interpreter.Options();
            Log.w(TAG, "GPU NOT SUPPORTED");
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
            options.setUseXNNPACK(true);
//            options.setUseNNAPI(true);
            interpreter = new Interpreter(modelBuffer_base, options);

            int inputCount = interpreter.getInputTensorCount();
            for (int i = 0; i < inputCount; i++) {
                int[] shape = interpreter.getInputTensor(i).shape();
                String name = interpreter.getInputTensor(i).name();
                Log.w(TAG, "Input " + i + " (" + name + ") shape: " + Arrays.toString(shape));
            }
            initBuffers();
            InputStream inputStream = mcontext.getAssets().open(tokenizerPath);
            tokenizer = new LSTM_tokenizer(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> detectionResultList = new ArrayList<>();
        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        long startTime = System.currentTimeMillis();
        for (TextResults t : textResults) {
            String text = t.textContent.toLowerCase().trim();
            LSTM_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            long[] inputIds = encoding.inputIds;
            long[] attentionMask = encoding.attentionMask;

            // if theres no tokens found just continue
            if (inputIds.length < 1) continue;
            // print statements
//            debugInput(t.textContent, inputIds, attentionMask);
            float[][] output = runInference(inputIds, attentionMask);
            float max_cfs = output[0][0];
            String l = max_cfs > 0.5 ? LABELS[1] : LABELS[0];
//            Log.i(TAG, "left: " + t.left + " top: " + t.top + " right: " + t.right + " bottom:" + t.bottom);
//            Log.i(TAG, "label: " + l + "  max cfs: " + max_cfs);
//            Log.i(TAG, "\n");
            if (l.equals(LABELS[1])) {
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


        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        return detectionResultList;
    }

    public void initBuffers() {
        ids = new int[1][ModelTypes.LSTM_SEQ_LEN];
        mask = new int[1][ModelTypes.LSTM_SEQ_LEN];
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        Log.i(TAG, "initBuffers: output shape[0]" + outputShape[0] + " outputshape[1]: " + outputShape[1]);
        outputs = new float[outputShape[0]][outputShape[1]];
    }

    public float[][] runInference(long[] inputIds, long[] attentionMask) {
        long startTime = System.currentTimeMillis();
        int seqLen = inputIds.length;

        float[][] newInput = new float[1][ModelTypes.LSTM_SEQ_LEN];

        for (int i = 0; i < ModelTypes.LSTM_SEQ_LEN; i++) {
            if (i < seqLen) {
                newInput[0][i] = (float) inputIds[i];
            } else {
                newInput[0][i] = 0.0f;
            }
        }

        interpreter.run(newInput, outputs);
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

//    private void debugInput(String raw, long[] ids, long[] mask) {
//        int nonPad = 0;
//        for (int i = 0; i < ids.length; i++) if (ids[i] != 0) nonPad++;
//        Log.i(TAG, "RAW: '" + raw + "'");
//        Log.i(TAG, "IDS: " + Arrays.toString(Arrays.copyOf(ids, 16)) + " ...");
//        Log.i(TAG, "MASK: " + Arrays.toString(Arrays.copyOf(mask, 16)) + " ... nonPad=" + nonPad);
//    }

}
