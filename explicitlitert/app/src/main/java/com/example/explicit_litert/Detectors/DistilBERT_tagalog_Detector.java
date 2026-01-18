package com.example.explicit_litert.Detectors;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicit_litert.ToolsNLP.Recognizer;
import com.example.explicit_litert.ToolsNLP.SoftmaxConverter;
import com.example.explicit_litert.ToolsNLP.Tokenizers.Distilbert_tagalog_tokenizer;
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

// IN THIS CODE YOU CAN RUN: TINYBERT/DISTILBERT JUST REPLACE THE MODEL AND TOKENIZER FILEPATH IN THE MODELTYPES.JAVA
public class DistilBERT_tagalog_Detector {
    private static final String TAG = "DISTILBERT_TAGALOG";
    private Context mcontext;
    Interpreter interpreter;
    Recognizer recognizer;
    Distilbert_tagalog_tokenizer tokenizer;
    SoftmaxConverter softmaxConverter;
    public static final String[] LABELS = ModelTypes.DISTILBERT_TAGALOG_LABELARRAY;
    public static final String tokenizerPath = ModelTypes.DISTILBERT_TAGALOG_TOKENIZER;
    public static final String modelPath = ModelTypes.DISTILBERT_TAGALOG_MODEL;
    int[][] ids;
    int[][] mask;
    float[][] outputs;

    public DistilBERT_tagalog_Detector(Context context) {
        mcontext = context;
        try {
            recognizer = new Recognizer(context);
            softmaxConverter = new SoftmaxConverter();

            this.mcontext = context;
            ByteBuffer modelBuffer_base = loadModelFile(context, modelPath);
            Interpreter.Options options = new Interpreter.Options();

            Log.w(TAG, "GPU NOT SUPPORTED");
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
            options.setUseXNNPACK(true);
//            }//
            interpreter = new Interpreter(modelBuffer_base, options);

            int inputCount = interpreter.getInputTensorCount();
            for (int i = 0; i < inputCount; i++) {
                int[] shape = interpreter.getInputTensor(i).shape();
                String name = interpreter.getInputTensor(i).name();
                Log.w(TAG, "Input " + i + " (" + name + ") shape: " + Arrays.toString(shape));
            }
            initBuffers();
            InputStream inputStream = mcontext.getAssets().open(tokenizerPath);
            tokenizer = new Distilbert_tagalog_tokenizer(inputStream);
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
        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        long startTime = System.currentTimeMillis();
        for (TextResults t : textResults) {
            String text = t.textContent.replaceAll("[^a-z\\s]", "").replaceAll("\\s+", " ").trim();
            text = text.toLowerCase().trim();
            if (text.length() < 3) continue;
            Distilbert_tagalog_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            long[] inputIds = encoding.inputIds;
            long[] attentionMask = encoding.attentionMask;
            if (inputIds.length < 1) continue;
//            ensureInputOrder(); // see below
            float[][] output = runInference(inputIds, attentionMask);
            float[] probabilities = softmaxConverter.softmax(output[0]);
            float max_cfs = -100f;
            String l = "";

            for (int i = 0; i < probabilities.length; i++) {
                if (probabilities[i] > max_cfs) {
//                    Log.i(TAG, LABELS[i] + "prob: " + probabilities[i]);
                    max_cfs = probabilities[i];
                    l = LABELS[i];
                }
            }
////            for (float[] o : output) Log.i(TAG, "output[]: " + Arrays.toString(o));
//            Log.i(TAG, "output length: " + output.length);
//            Log.i(TAG, "output array: " + Arrays.toString(output[0]));

            Log.i(TAG, "\nSTART");

            Log.i(TAG, "heence word: "+text + "  output[0][0]: "+output[0][0] + " | output[0][1]: "+ output[0][1] + " softmax[0]: " +probabilities[0] + " softmax[1]: "+ probabilities[1] + " label: " +l);
            debugInput(text, inputIds, attentionMask);
//            int predClass = output[0][0] > output[0][1] ? 0 : 1;
//            if (predClass == 1) continue;
//            Log.i(TAG, "left: " + t.left + " top: " + t.top + " right: " + t.right + " bottom:" + t.bottom);


            if (l.equals("nsfw") ) {
                detectionResultList.add(new DetectionResult(
                        0,
                        max_cfs,
                        t.left / bitmap.getWidth(),
                        t.top / bitmap.getHeight(),
                        t.right / bitmap.getWidth(),
                        t.bottom / bitmap.getHeight(),
                        "nsfw",
                        1
                ));
            }

            Log.i(TAG, "\n");
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
//        Log.i(TAG, "this for loop took: " + duration + " ms");
        return detectionResultList;
    }

    public void initBuffers() {
        ids = new int[1][ModelTypes.DISTILBERT_TAGALOG_SEQ_LEN];
        mask = new int[1][ModelTypes.DISTILBERT_TAGALOG_SEQ_LEN];
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        outputs = new float[outputShape[0]][outputShape[1]];
    }

    public float[][] runInference(long[] inputIds, long[] attentionMask) {
        long startTime = System.currentTimeMillis();
        int seqLen = inputIds.length;
        for (int i = 0; i < seqLen; i++) {
            ids[0][i] = (int) inputIds[i];
            mask[0][i] = (int) attentionMask[i];
        }

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
        for (int i = 0; i < ids.length; i++) if (ids[i] != 0) nonPad++;
        Log.i(TAG, "RAW: '" + raw + "'");
        Log.i(TAG, "heence IDS: " + Arrays.toString(Arrays.copyOf(ids, 16)) + " ...");
        Log.i(TAG, "heence MASK: " + Arrays.toString(Arrays.copyOf(mask, 16)) + " ... nonPad=" + nonPad);
    }

}
