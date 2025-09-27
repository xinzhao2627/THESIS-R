package com.example.explicitapp3.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.Detectors.YoloV10Detector;
import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.ModelTypes;
import java.io.IOException;

public class ImageModel {
    private static final String TAG = "ImageModel";
    YoloV10Detector yoloV10Detector;
    Context mcontext;
    String selectedModel;
    public ImageModel (Context mcontext, String name) throws IOException {
        this.mcontext = mcontext;
        selectedModel = name;

        if (name.equals(ModelTypes.YOLO_V10_F16)){
            yoloV10Detector = new YoloV10Detector(
                    mcontext,
                    ModelTypes.YOLO_V10_F16 + "/" + ModelTypes.YOLO_V10_F16_MODEL,
                    ModelTypes.YOLO_V10_F16 + "/" + "labels.txt"
            );
            Log.i(TAG, "f16");
        } else if (name.equals(ModelTypes.YOLO_V10_F32)){
            yoloV10Detector = new YoloV10Detector(
                    mcontext,
                    ModelTypes.YOLO_V10_F32 + "/" + ModelTypes.YOLO_V10_F32_MODEL,
                    ModelTypes.YOLO_V10_F32 + "/" + "labels.txt"
            );
            Log.i(TAG, name);
        }
    }

    public ClassifyResults detect(Bitmap bitmap){
        Log.i(TAG, "detecting image");
        return yoloV10Detector.detect(bitmap);
    }

    public void cleanup(){
        if (yoloV10Detector != null){
            yoloV10Detector.cleanup();
            yoloV10Detector = null;
        }

    }

}
