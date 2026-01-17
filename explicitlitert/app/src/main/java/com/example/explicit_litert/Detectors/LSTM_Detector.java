package com.example.explicit_litert.Detectors;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicit_litert.ToolsNLP.Recognizer;
import com.example.explicit_litert.ToolsNLP.Tokenizers.LSTM_tokenizer;
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
            MappedByteBuffer modelBuffer = loadModelFile(context, modelPath);
//            ByteBuffer modelBuffer_base = null;
            Interpreter.Options options = new Interpreter.Options();
            Log.w(TAG, "GPU NOT SUPPORTED");
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
            options.setUseXNNPACK(true);
//            options.setUseNNAPI(true);
            interpreter = new Interpreter(modelBuffer, options);

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
    private static MappedByteBuffer loadModelFile(Context context, String assetPath)
            throws IOException {

        AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();

        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public void initBuffers() {
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        Log.i(TAG, "initBuffers: shape: "+outputShape[0] + " and " + outputShape[1]);
        outputs = new float[outputShape[0]][outputShape[1]];
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> detectionResultList = new ArrayList<>();
        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        for (TextResults t : textResults) {
            String text = t.textContent.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
            text = text.toLowerCase().trim();
            if (text.length() < 3) continue;

            LSTM_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
//            Log.i(TAG, "text heyys: " + text);
            long[] inputIds = encoding.inputIds;
            long[] attentionMask = encoding.attentionMask;
            // if theres no tokens found just continue
            if (inputIds.length < 1) continue;

            float[][] output = runInference(inputIds, attentionMask);
            float max_cfs = output[0][0];
            Log.i(TAG, "cfs: " + max_cfs);

            String l = max_cfs > 0.5 ? LABELS[1] : LABELS[0];
            if (l.equals("safe")) continue;
            Log.i(TAG, "word: "+t.textContent+" label: " + l + "  max cfs: " + max_cfs);

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
        return detectionResultList;
    }
    public float[][] runInference(long[] inputIds, long[] attentionMask) {
        long startTime = System.currentTimeMillis();
        int seqLen = inputIds.length;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (long id : inputIds) {
            max = Math.max(max, (int) id);
            min = Math.min(min, (int) id);
        }
        Log.i(TAG, "token range: min=" + min + " max=" + max);

        float[][] newInput = new float[1][ModelTypes.LSTM_SEQ_LEN];
        Log.i(TAG, "runInference: newinput: " +newInput.length + " seqlen: " + seqLen);
        for (int i = 0; i < ModelTypes.LSTM_SEQ_LEN; i++) {
            if (i < seqLen) {
                newInput[0][i] = (float) inputIds[i];
            } else {
                newInput[0][i] = 0.0f;
            }
        }
        for (int i = 0; i < 10; i++) {
            Log.i(TAG, "token[" + i + "] = " + newInput[0][i]);
        }
        interpreter.run(newInput, outputs);
        Log.i(TAG, "runInference: newinpuut done: " +newInput.length);

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
