package com.example.explicit_litert.ToolsNLP;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import com.example.explicit_litert.Types.TextResults;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.List;

public class Recognizer {
    Context mcontext;
    private static final String TAG = "Recognizer";
    int etn = 60;
    TextRecognizer textRecognizer;
    public Recognizer(Context context, int etn){
        mcontext = context;
        this.etn = etn;
        Log.i(TAG, "Recognizer: etn is: "+etn);
        textRecognizer = new TextRecognizer.Builder(mcontext).build();
    }
    public List<TextResults> textRecognition(Bitmap bitmap) {
        List<TextResults> textList = new ArrayList<>();

        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, skipping text recognition");
            return textList;
        }
        try {

            Frame frameimage = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlockSparseArray = textRecognizer.detect(frameimage);
            Log.w(TAG, "textRecognition: hihi");
            // loops through all detected text (textblock) (usually this is just 1 length)
            for (int i = 0; i < textBlockSparseArray.size(); i++) {
                TextBlock textBlock = textBlockSparseArray.get(textBlockSparseArray.keyAt(i));
                for (Text t : textBlock.getComponents()) {
//                    Log.w(TAG, i + " textRecognition: " + t.getValue());
                    Rect rect = t.getBoundingBox();
//                    int offset = (int) (rect.height() * 0.1);
                    int top = rect.top - etn;
                    int bottom = rect.bottom - etn;

                    // Prevent clipping above screen
                    if (top < 0) {
                        top = 0;
                    }
                    if (bottom < 0){
                        bottom = 0;
                    }
                    String text = t.getValue().trim().toLowerCase();
                    if (text.isEmpty()) continue;
                    if (!text.matches(".*[a-zA-Z].*")) continue;
//                    Log.w(TAG, "left: " + rect.left + " right: " + rect.right + " top: " + rect.top + " bottom: " + rect.bottom);
                    textList.add(new TextResults(rect.left, top, rect.right, bottom, 1, text));
                }
            }
        }catch (Exception e) {
            Log.e(TAG, "Text recognition failed: " + e.getMessage());
        }
        return textList;
    }
}