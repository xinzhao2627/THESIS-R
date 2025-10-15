package com.example.explicitapp3.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.Detectors.Mobilenet_ssd_Detector;
import com.example.explicitapp3.Detectors.YoloV10Detector;
import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.ModelTypes;

import java.io.IOException;

public class ImageModel {
    private static final String TAG = "ImageModel";
    YoloV10Detector yoloV10Detector;
    Mobilenet_ssd_Detector mobilenetSsdDetector;
    Context mcontext;
    String selectedModel;

    public ImageModel(Context mcontext, String name) throws IOException {
        this.mcontext = mcontext;
        selectedModel = name;

        if (name.equals(ModelTypes.YOLO_V10_F16)) {
            yoloV10Detector = new YoloV10Detector(
                    mcontext,
                    ModelTypes.YOLO_V10_F16 + "/" + ModelTypes.YOLO_V10_F16_MODEL,
                    ModelTypes.YOLO_V10_F16 + "/" + "labels.txt"
            );
            Log.i(TAG, "f16");
        } else if (name.equals(ModelTypes.YOLO_V10_F32)) {
            yoloV10Detector = new YoloV10Detector(
                    mcontext,
                    ModelTypes.YOLO_V10_F32 + "/" + ModelTypes.YOLO_V10_F32_MODEL,
                    ModelTypes.YOLO_V10_F32 + "/" + "labels.txt"
            );
            Log.i(TAG, name);
        } else if (name.equals(ModelTypes.MOBILENET_SSD)) {
            mobilenetSsdDetector = new Mobilenet_ssd_Detector(mcontext, ModelTypes.MOBILENET_SSD + "/" + ModelTypes.MOBILENET_SSD_MODEL,
                    ModelTypes.MOBILENET_SSD + "/" + "labels.txt");
//            yoloV10Detector = new YoloV10Detector(
//                    mcontext,
//                    ModelTypes.YOLO_V10_F32 + "/" + ModelTypes.YOLO_V10_F32_MODEL,
//                    ModelTypes.YOLO_V10_F32 + "/" + "labels.txt"
//            );
        }
    }

    public ClassifyResults detect(Bitmap bitmap) {
        if (selectedModel.equals(ModelTypes.YOLO_V10_F32) || selectedModel.equals(ModelTypes.YOLO_V10_F16)) {
            return yoloV10Detector.detect(bitmap);
        } else if (selectedModel.equals(ModelTypes.MOBILENET_SSD)) {
            return mobilenetSsdDetector.detect(bitmap);
//            return yoloV10Detector.detect(bitmap);
        }
        return null;
    }

    public void cleanup() {
        if (yoloV10Detector != null) {
            yoloV10Detector.cleanup();
            yoloV10Detector = null;
        } else if (mobilenetSsdDetector != null) {
            mobilenetSsdDetector.cleanup();
            mobilenetSsdDetector = null;
        }

    }

}
