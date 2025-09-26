package com.example.explicitapp3.ToolsNLP;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import com.example.explicitapp3.Types.TextResults;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.List;

public class Recognizer {
    Context mcontext;
    private static final String TAG = "Recognizer";
    public Recognizer(Context context){
        mcontext = context;
    }
    public List<TextResults> textRecognition(Bitmap bitmap) {
        List<TextResults> textList = new ArrayList<>();

        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, skipping text recognition");
            return textList;
        }
        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(mcontext).build();
            Frame frameimage = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlockSparseArray = textRecognizer.detect(frameimage);
            Log.w(TAG, "textRecognition: hihi");
            // loops through all detected text (textblock) (usually this is just 1 length)
            for (int i = 0; i < textBlockSparseArray.size(); i++) {
                TextBlock textBlock = textBlockSparseArray.get(textBlockSparseArray.keyAt(i));
                for (Text t : textBlock.getComponents()) {
//                    Log.w(TAG, i + " textRecognition: " + t.getValue());
                    Rect rect = t.getBoundingBox();
//                    Log.w(TAG, "left: " + rect.left + " right: " + rect.right + " top: " + rect.top + " bottom: " + rect.bottom);
                    textList.add(new TextResults(rect.left, rect.top, rect.right, rect.bottom, 1, t.getValue()));
                }
            }
        }catch (Exception e) {
            Log.e(TAG, "Text recognition failed: " + e.getMessage());
        }
        return textList;
    }
}