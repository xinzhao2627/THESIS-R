package com.example.explicitapp3.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicitapp3.Detectors.Mobilenet_ssd_Detector;
import com.example.explicitapp3.Detectors.Mobilenet_ssd_Detector2;
import com.example.explicitapp3.Detectors.YoloV10Detector;
import com.example.explicitapp3.Detectors.Yolov11n_Detector;
import com.example.explicitapp3.Detectors.Yolov5_Detector;
import com.example.explicitapp3.Types.ClassifyResults;
import com.example.explicitapp3.Types.ModelTypes;

import java.io.IOException;

public class ImageModel {
    private static final String TAG = "ImageModel";
    YoloV10Detector yoloV10Detector;
    Yolov11n_Detector yolov11nDetector;
    Yolov5_Detector yolov5Detector;

    Mobilenet_ssd_Detector2 mobilenetSsdDetector;
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
            mobilenetSsdDetector = new Mobilenet_ssd_Detector2(mcontext, ModelTypes.MOBILENET_SSD + "/" + ModelTypes.MOBILENET_SSD_MODEL,
                    ModelTypes.MOBILENET_SSD + "/" + "labels.txt");
//            yoloV10Detector = new YoloV10Detector(
//                    mcontext,
//                    ModelTypes.YOLO_V10_F32 + "/" + ModelTypes.YOLO_V10_F32_MODEL,
//                    ModelTypes.YOLO_V10_F32 + "/" + "labels.txt"
//            );
            Log.i(TAG, name);
        } else if (name.equals(ModelTypes.YOLO_V11N)) {
            yolov11nDetector = new Yolov11n_Detector(mcontext, ModelTypes.YOLO_V11N + "/" + ModelTypes.YOLO_V11N_MODEL,
                    ModelTypes.YOLO_V11N + "/" + "labels.txt");
            Log.i(TAG, name);
        }else if (name.equals(ModelTypes.YOLO_V5)) {
            yolov5Detector = new Yolov5_Detector(mcontext, ModelTypes.YOLO_V5 + "/" + ModelTypes.YOLO_V5_MODEL,
                    ModelTypes.YOLO_V5 + "/" + "labels.txt");
            Log.i(TAG, name);
        }
    }

    public ClassifyResults detect(Bitmap bitmap) {
        if (selectedModel.equals(ModelTypes.YOLO_V10_F32) || selectedModel.equals(ModelTypes.YOLO_V10_F16)) {
            return yoloV10Detector.detect(bitmap);
        } else if (selectedModel.equals(ModelTypes.MOBILENET_SSD)) {
            return mobilenetSsdDetector.detect(bitmap);
        } else if (selectedModel.equals(ModelTypes.YOLO_V11N)) {
            return yolov11nDetector.detect(bitmap);
        } else if (selectedModel.equals(ModelTypes.YOLO_V5)){
            return yolov5Detector.detect(bitmap);
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
        } else if (yolov11nDetector != null) {
            yolov11nDetector.cleanup();
            yolov11nDetector = null;
        } else if (yolov5Detector != null) {
            yolov5Detector.cleanup();
            yolov5Detector = null;
        }

    }

}
